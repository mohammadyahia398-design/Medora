package com.example.medora

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.medora.settings.AppPrefs
import com.example.medora.ui.navigation.MedoraApp
import com.example.medora.ui.theme.MedoraTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppPrefs.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPrefs.init(this)
        setContent {
            val dark = when (AppPrefs.theme.value) {
                AppPrefs.DARK -> true
                AppPrefs.LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            MedoraTheme(darkTheme = dark) {
                MedoraApp()
            }
        }
    }
}
