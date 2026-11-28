package com.example.medora.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

// App-level choices: language and theme. Saved on the phone only.
// Language is applied by wrapping the activity context (see MainActivity.attachBaseContext).
object AppPrefs {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
    const val LANG_AR = "ar"
    const val LANG_EN = "en"

    private const val FILE = "medora_app_prefs"
    private const val KEY_THEME = "theme"
    private const val KEY_LANG = "language"
    private const val KEY_GOAL = "daily_goal"
    private const val KEY_ONBOARDED = "onboarded"
    const val DEFAULT_GOAL = 20

    val theme: MutableState<String> = mutableStateOf(SYSTEM)

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun init(context: Context) {
        theme.value = prefs(context).getString(KEY_THEME, SYSTEM) ?: SYSTEM
    }

    fun setTheme(context: Context, value: String) {
        prefs(context).edit().putString(KEY_THEME, value).apply()
        theme.value = value
    }

    // Daily review goal (cards per day).
    fun dailyGoal(context: Context): Int = prefs(context).getInt(KEY_GOAL, DEFAULT_GOAL)

    fun setDailyGoal(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_GOAL, value).apply()
    }

    // True after the first-run intro (onboarding + goal) is finished.
    fun onboarded(context: Context): Boolean = prefs(context).getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDED, true).apply()
    }

    fun language(context: Context): String =
        prefs(context).getString(KEY_LANG, SYSTEM) ?: SYSTEM

    fun setLanguage(context: Context, value: String) {
        prefs(context).edit().putString(KEY_LANG, value).apply()
    }

    fun wrap(base: Context): Context {
        val lang = language(base)
        if (lang == SYSTEM) return base
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
