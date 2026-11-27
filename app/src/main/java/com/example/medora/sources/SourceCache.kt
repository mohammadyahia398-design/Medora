package com.example.medora.sources

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CachedResult(val items: List<SourceItem>, val accessed: String, val fromCache: Boolean)

// Keeps search results in the app cache folder for 7 days. Saves internet and
// lets old results open offline. Android may clear this folder at any time.
object SourceCache {
    private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    private const val MAX_FILES = 50

    suspend fun search(context: Context, query: String): CachedResult = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "sources").apply { mkdirs() }
        val file = File(dir, key(query) + ".json")
        val now = System.currentTimeMillis()
        if (file.exists() && now - file.lastModified() < MAX_AGE_MS) {
            readOrNull(file)?.let { return@withContext CachedResult(it, dateOf(file.lastModified()), true) }
        }
        try {
            val raw = EuropePmcClient.fetchRaw(query)
            val items = EuropePmcClient.parse(raw)
            try {
                file.writeText(raw, Charsets.UTF_8)
                trim(dir)
            } catch (e: Exception) {
                // Cache is optional.
            }
            CachedResult(items, dateOf(now), false)
        } catch (e: Exception) {
            // Offline: fall back to an old copy if we have one.
            val old = if (file.exists()) readOrNull(file) else null
            if (old != null) CachedResult(old, dateOf(file.lastModified()), true) else throw e
        }
    }

    private fun readOrNull(file: File): List<SourceItem>? = try {
        EuropePmcClient.parse(file.readText(Charsets.UTF_8))
    } catch (e: Exception) {
        null
    }

    private fun trim(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_FILES).forEach { it.delete() }
    }

    private fun dateOf(ms: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))

    private fun key(query: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(query.trim().lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }
}
