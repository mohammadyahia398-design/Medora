package com.example.medora.ai

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Remembers, on this phone only, which AI provider is temporarily down or rate-limited,
// and how many requests each provider got today (local safety limit).
class ProviderGuard(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ai_provider_guard", Context.MODE_PRIVATE)

    fun isCooling(provider: String): Boolean =
        System.currentTimeMillis() < prefs.getLong("${provider}_until", 0L)

    fun coolDown(provider: String, seconds: Long) {
        if (seconds <= 0) return
        val until = System.currentTimeMillis() + seconds.coerceAtMost(MAX_COOL_SECONDS) * 1000
        prefs.edit().putLong("${provider}_until", until).apply()
    }

    fun markSuccess(provider: String) {
        prefs.edit().remove("${provider}_until").apply()
    }

    fun usedToday(provider: String): Int =
        if (prefs.getString("${provider}_day", null) == today()) prefs.getInt("${provider}_count", 0) else 0

    fun reachedDailyLimit(provider: String): Boolean = usedToday(provider) >= DAILY_LIMIT

    fun countRequest(provider: String) {
        val n = usedToday(provider) + 1
        prefs.edit().putString("${provider}_day", today()).putInt("${provider}_count", n).apply()
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        const val DAILY_LIMIT = 100
        private const val MAX_COOL_SECONDS = 3600L
    }
}
