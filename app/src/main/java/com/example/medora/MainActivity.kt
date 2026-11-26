package com.example.medora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.medora.ui.navigation.MedoraApp
import com.example.medora.ui.theme.MedoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedoraTheme {
                MedoraApp()
            }
        }
    }
}
