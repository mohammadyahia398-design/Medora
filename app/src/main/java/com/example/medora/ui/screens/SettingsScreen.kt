package com.example.medora.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import android.net.Uri
import com.example.medora.data.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.medora.R
import com.example.medora.ai.AiSettings
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard
import com.example.medora.ui.theme.Spacing

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
    val scope = rememberCoroutineScope()
    var backupMessage by remember { mutableStateOf<Int?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { BackupManager.export(context, uri) }
                backupMessage = if (ok) R.string.backup_done else R.string.backup_failed
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingRestore = uri }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_title)) },
            text = { Text(stringResource(R.string.restore_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { BackupManager.restore(context, uri) }
                        if (ok) BackupManager.restartApp(context) else backupMessage = R.string.restore_failed
                    }
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        AppearanceSettingsCard()

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

        ReminderSettingsCard()

        Text(
            text = stringResource(R.string.backup_section),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        MedoraCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodySmall)
            MedoraButton(
                text = stringResource(R.string.backup_export),
                onClick = { exportLauncher.launch("medora-backup.db") },
                modifier = Modifier.fillMaxWidth(),
            )
            MedoraButton(
                text = stringResource(R.string.backup_restore),
                onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            )
            backupMessage?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium)
            }
        }

        DeleteDataCard()
    }
}
