package com.example.medora.ai

import android.content.Context

// The user's own AI keys. Saved in this app's private storage on the phone only
// (backup is off in the manifest). Never put keys in the repository.
class AiSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

    var geminiKey: String
        get() = prefs.getString("gemini_key", "").orEmpty()
        set(value) = prefs.edit().putString("gemini_key", value.trim()).apply()

    var geminiModel: String
        get() = prefs.getString("gemini_model", null)?.ifBlank { null } ?: DEFAULT_GEMINI_MODEL
        set(value) = prefs.edit().putString("gemini_model", value.trim()).apply()

    var openRouterKey: String
        get() = prefs.getString("openrouter_key", "").orEmpty()
        set(value) = prefs.edit().putString("openrouter_key", value.trim()).apply()

    var openRouterModel: String
        get() = prefs.getString("openrouter_model", null)?.ifBlank { null } ?: DEFAULT_OPENROUTER_MODEL
        set(value) = prefs.edit().putString("openrouter_model", value.trim()).apply()

    val hasAnyKey: Boolean get() = geminiKey.isNotBlank() || openRouterKey.isNotBlank()

    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-flash-latest"
        const val DEFAULT_OPENROUTER_MODEL = "openrouter/free"
    }
}
