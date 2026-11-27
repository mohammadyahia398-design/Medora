package com.example.medora.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SourceItem(
    val title: String,
    val authors: String,
    val journal: String,
    val year: String,
    val pmid: String,
    val doi: String,
    val url: String,
)

// Searches PubMed records through the free Europe PMC REST API (no key needed).
// Only Android built-ins are used (HttpURLConnection + org.json). Returns references only.
object EuropePmcClient {
    private const val BASE = "https://www.ebi.ac.uk/europepmc/webservices/rest/search"
    private const val PAGE_SIZE = 10

    suspend fun search(query: String): List<SourceItem> = withContext(Dispatchers.IO) {
        val q = "($query) AND SRC:MED"
        val url = BASE + "?query=" + URLEncoder.encode(q, "UTF-8") +
            "&format=json&resultType=lite&pageSize=" + PAGE_SIZE
        parse(get(url))
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "GET"
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.setRequestProperty("Accept", "application/json")
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("HTTP $code")
            return text
        } finally {
            c.disconnect()
        }
    }

    private fun JSONObject.str(key: String): String {
        val v = optString(key).trim()
        return if (v == "null") "" else v
    }

    fun parse(raw: String): List<SourceItem> {
        val arr = JSONObject(raw).optJSONObject("resultList")?.optJSONArray("result") ?: return emptyList()
        val tags = Regex("<[^>]*>")
        val out = mutableListOf<SourceItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.str("title").replace(tags, "").trim().trimEnd('.')
            val id = o.str("id")
            if (title.isEmpty() || id.isEmpty()) continue
            val pmid = o.str("pmid")
            val source = o.str("source").ifEmpty { "MED" }
            val link = if (pmid.isNotEmpty()) {
                "https://pubmed.ncbi.nlm.nih.gov/$pmid/"
            } else {
                "https://europepmc.org/article/$source/$id"
            }
            out += SourceItem(
                title = title,
                authors = o.str("authorString").trimEnd('.'),
                journal = o.str("journalTitle"),
                year = o.str("pubYear"),
                pmid = pmid,
                doi = o.str("doi"),
                url = link,
            )
        }
        return out
    }
}
