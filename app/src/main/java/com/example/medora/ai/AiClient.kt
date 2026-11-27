package com.example.medora.ai

import com.example.medora.domain.CardSuggester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

class AiQuestionDraft(val question: String, val options: List<String>, val correctIndex: Int, val source: String)

// Everything one AI request returns for one lecture.
class AiPack(
    val summary: String,
    val cards: List<CardSuggester.Suggestion>,
    val questions: List<AiQuestionDraft>,
    val slidesJson: String,
)

class NoAiKeyException : Exception("no key")

// Why the AI request failed, so the screen can show a clear message.
enum class AiFailKind { NO_INTERNET, INVALID_KEY, RATE_LIMIT, DAILY_LIMIT, SERVER, BAD_ANSWER, OTHER }

class AiFailure(val kind: AiFailKind, detail: String) : IOException(detail)

private class HttpError(val code: Int, val retryAfter: Long, msg: String) : IOException(msg)

// One request per lecture: Gemini first, OpenRouter if Gemini fails.
// Provider routing: a provider that is down or rate-limited is skipped for a while,
// short failures are retried once, and each provider has a local daily limit.
// Uses only Android built-ins (HttpURLConnection + org.json). No extra library.
class AiClient(private val settings: AiSettings, private val guard: ProviderGuard? = null) {

    private class Provider(val name: String, val call: (String) -> String)

    suspend fun generate(title: String, content: String): AiPack = withContext(Dispatchers.IO) {
        if (!settings.hasAnyKey) throw NoAiKeyException()
        val prompt = buildPrompt(title, content.take(MAX_CHARS))

        val all = mutableListOf<Provider>()
        val gKey = settings.geminiKey
        val gModel = settings.geminiModel
        if (gKey.isNotBlank()) all += Provider(GEMINI) { callGemini(gKey, gModel, it) }
        val oKey = settings.openRouterKey
        val oModel = settings.openRouterModel
        if (oKey.isNotBlank()) all += Provider(OPENROUTER) { callOpenRouter(oKey, oModel, it) }

        // Healthy providers first; a cooling-down provider is only tried as a last resort.
        val ordered = all.filter { guard?.isCooling(it.name) != true } +
            all.filter { guard?.isCooling(it.name) == true }

        val errors = mutableListOf<String>()
        var kind = AiFailKind.OTHER
        for (p in ordered) {
            if (guard?.reachedDailyLimit(p.name) == true) {
                errors += "${p.name}: daily limit ${ProviderGuard.DAILY_LIMIT}"
                kind = worse(kind, AiFailKind.DAILY_LIMIT)
                continue
            }
            try {
                val pack = tryProvider(p, prompt)
                guard?.markSuccess(p.name)
                return@withContext pack
            } catch (e: AiFailure) {
                errors += "${p.name}: " + (e.message ?: "")
                kind = worse(kind, e.kind)
                if (e.kind == AiFailKind.NO_INTERNET) break
            }
        }
        throw AiFailure(kind, errors.joinToString("\n"))
    }

    // At most two attempts per provider, and only for failures that may pass quickly.
    private suspend fun tryProvider(p: Provider, prompt: String): AiPack {
        var attempt = 0
        while (true) {
            attempt++
            guard?.countRequest(p.name)
            var fail = AiFailure(AiFailKind.OTHER, "")
            var wait = 0L
            var cool = 0L
            try {
                return parse(p.call(prompt))
            } catch (e: HttpError) {
                val msg = "HTTP ${e.code} " + (e.message ?: "")
                when {
                    e.code == 401 || e.code == 403 -> fail = AiFailure(AiFailKind.INVALID_KEY, msg)
                    e.code == 429 -> {
                        fail = AiFailure(AiFailKind.RATE_LIMIT, msg)
                        cool = if (e.retryAfter > 0) e.retryAfter else 60L
                        if (e.retryAfter in 1..10) wait = e.retryAfter * 1000L
                    }
                    e.code == 408 || e.code >= 500 -> {
                        fail = AiFailure(AiFailKind.SERVER, msg)
                        wait = 2_000L
                        cool = 120L
                    }
                    else -> fail = AiFailure(AiFailKind.OTHER, msg)
                }
            } catch (e: UnknownHostException) {
                throw AiFailure(AiFailKind.NO_INTERNET, "no internet")
            } catch (e: SocketTimeoutException) {
                fail = AiFailure(AiFailKind.SERVER, "timeout")
                wait = 2_000L
                cool = 120L
            } catch (e: IOException) {
                val m = e.message ?: e.javaClass.simpleName
                if (m.startsWith("bad AI answer") || m.startsWith("empty AI answer")) {
                    fail = AiFailure(AiFailKind.BAD_ANSWER, m)
                    wait = 1_000L
                } else {
                    fail = AiFailure(AiFailKind.OTHER, m)
                }
            } catch (e: Exception) {
                // JSON shape changed or similar: retry once, the model may answer better.
                fail = AiFailure(AiFailKind.BAD_ANSWER, e.message ?: e.javaClass.simpleName)
                wait = 1_000L
            }
            if (attempt >= 2 || wait == 0L || guard?.reachedDailyLimit(p.name) == true) {
                guard?.coolDown(p.name, cool)
                throw fail
            }
            delay(wait)
        }
    }

    private fun worse(a: AiFailKind, b: AiFailKind): AiFailKind = if (b.ordinal < a.ordinal) b else a

    private fun callGemini(key: String, model: String, prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
            URLEncoder.encode(model, "UTF-8") + ":generateContent"
        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))),
            )
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
        val res = JSONObject(post(url, mapOf("x-goog-api-key" to key), body.toString()))
        val parts = res.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts")
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            if (!p.optBoolean("thought", false)) sb.append(p.optString("text"))
        }
        return sb.toString()
    }

    private fun callOpenRouter(key: String, model: String, prompt: String): String {
        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        val res = JSONObject(
            post(
                "https://openrouter.ai/api/v1/chat/completions",
                mapOf("Authorization" to "Bearer $key", "X-Title" to "Medora"),
                body.toString(),
            )
        )
        return res.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content")
    }

    private fun post(url: String, headers: Map<String, String>, body: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = 20_000
            c.readTimeout = 180_000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val retry = c.getHeaderField("Retry-After")?.trim()?.toLongOrNull() ?: 0L
                throw HttpError(code, retry, errorMessage(text))
            }
            return text
        } finally {
            c.disconnect()
        }
    }

    private fun errorMessage(text: String): String = try {
        JSONObject(text).optJSONObject("error")?.optString("message").orEmpty().take(200)
    } catch (e: Exception) {
        ""
    }

    private fun buildPrompt(title: String, content: String): String = """
You are a study assistant for medical students.
Use ONLY the lecture text below. Do not add facts that are not in the text.
Write in the same language as the lecture.
Return ONLY one JSON object (no markdown, no extra text) with exactly this shape:
{"summary":"...","cards":[{"front":"...","back":"...","source":"..."}],"questions":[{"question":"...","options":["...","...","...","..."],"answer":0,"source":"..."}],"slides":[{"title":"...","bullets":["..."]}]}
Rules:
- summary: 150 to 300 words, the key points.
- cards: 10 to 20 flashcards. front = question or term, back = short answer.
- questions: 5 to 10 multiple-choice questions, 4 options each, answer = index (0-3) of the correct option.
- slides: 5 to 10 slides, 3 to 6 short bullets each.
- source: a short exact quote from the lecture that supports the item.

Lecture title: $title
Lecture text:
<<<
$content
>>>
""".trim()

    companion object {
        private const val MAX_CHARS = 30_000
        private const val GEMINI = "Gemini"
        private const val OPENROUTER = "OpenRouter"

        fun parse(raw: String): AiPack {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start < 0 || end <= start) throw IOException("bad AI answer")
            val o = JSONObject(raw.substring(start, end + 1))

            val cards = mutableListOf<CardSuggester.Suggestion>()
            o.optJSONArray("cards")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i) ?: continue
                    val front = c.optString("front").trim()
                    val back = c.optString("back").trim()
                    if (front.isNotEmpty() && back.isNotEmpty()) {
                        cards += CardSuggester.Suggestion(front, back, c.optString("source").trim())
                    }
                }
            }

            val questions = mutableListOf<AiQuestionDraft>()
            o.optJSONArray("questions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val q = arr.optJSONObject(i) ?: continue
                    val text = q.optString("question").trim()
                    val optsArr = q.optJSONArray("options") ?: continue
                    val opts = (0 until optsArr.length())
                        .map { optsArr.optString(it).replace('\n', ' ').trim() }
                        .filter { it.isNotEmpty() }
                    val answer = q.optInt("answer", -1)
                    if (text.isNotEmpty() && opts.size >= 2 && opts.size == optsArr.length() && answer in opts.indices) {
                        questions += AiQuestionDraft(text, opts, answer, q.optString("source").trim())
                    }
                }
            }

            val summary = o.optString("summary").trim()
            val slides = o.optJSONArray("slides")?.toString().orEmpty()
            if (summary.isEmpty() && cards.isEmpty() && questions.isEmpty()) throw IOException("empty AI answer")
            return AiPack(summary, cards, questions, slides)
        }
    }
}
