package com.example.medora.ai

import com.example.medora.domain.CardSuggester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AiQuestionDraft(val question: String, val options: List<String>, val correctIndex: Int, val source: String)

// Everything one AI request returns for one lecture.
class AiPack(
    val summary: String,
    val cards: List<CardSuggester.Suggestion>,
    val questions: List<AiQuestionDraft>,
    val slidesJson: String,
)

class NoAiKeyException : Exception("no key")

// One request per lecture: Gemini first, OpenRouter if Gemini fails.
// Uses only Android built-ins (HttpURLConnection + org.json). No extra library.
class AiClient(private val settings: AiSettings) {

    suspend fun generate(title: String, content: String): AiPack = withContext(Dispatchers.IO) {
        if (!settings.hasAnyKey) throw NoAiKeyException()
        val prompt = buildPrompt(title, content.take(MAX_CHARS))
        val errors = mutableListOf<String>()

        val gKey = settings.geminiKey
        if (gKey.isNotBlank()) {
            try {
                return@withContext parse(callGemini(gKey, settings.geminiModel, prompt))
            } catch (e: Exception) {
                errors += "Gemini: " + (e.message ?: e.javaClass.simpleName)
            }
        }
        val oKey = settings.openRouterKey
        if (oKey.isNotBlank()) {
            try {
                return@withContext parse(callOpenRouter(oKey, settings.openRouterModel, prompt))
            } catch (e: Exception) {
                errors += "OpenRouter: " + (e.message ?: e.javaClass.simpleName)
            }
        }
        throw IOException(errors.joinToString("\n"))
    }

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
            if (code !in 200..299) throw IOException("HTTP $code " + errorMessage(text))
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
