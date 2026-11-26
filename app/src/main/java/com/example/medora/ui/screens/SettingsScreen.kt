package com.example.medora.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.medora.R
import com.example.medora.ai.AiSettings
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard

// AI keys: one Gemini key (main) and one OpenRouter key (backup). Both optional.
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { AiSettings(context) }
    var geminiKey by remember { mutableStateOf(settings.geminiKey) }
    var geminiModel by remember { mutableStateOf(settings.geminiModel) }
    var orKey by remember { mutableStateOf(settings.openRouterKey) }
    var orModel by remember { mutableStateOf(settings.openRouterModel) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.ai_section),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(text = stringResource(R.string.ai_privacy), style = MaterialTheme.typography.bodySmall)

        MedoraCard(modifier = Modifier.fillMaxWidth()) {
            Text("Gemini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.gemini_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it; saved = false },
                label = { Text(stringResource(R.string.api_key)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = geminiModel,
                onValueChange = { geminiModel = it; saved = false },
                label = { Text(stringResource(R.string.model_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        MedoraCard(modifier = Modifier.fillMaxWidth()) {
            Text("OpenRouter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.openrouter_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = orKey,
                onValueChange = { orKey = it; saved = false },
                label = { Text(stringResource(R.string.api_key)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = orModel,
                onValueChange = { orModel = it; saved = false },
                label = { Text(stringResource(R.string.model_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        MedoraButton(
            text = stringResource(R.string.save),
            onClick = {
                settings.geminiKey = geminiKey
                settings.geminiModel = geminiModel
                settings.openRouterKey = orKey
                settings.openRouterModel = orModel
                saved = true
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (saved) {
            Text(stringResource(R.string.saved), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
