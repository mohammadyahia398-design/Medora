package com.example.medora.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.Color
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
import com.example.medora.ui.components.MedoraOutlinedButton
import com.example.medora.ui.theme.MedoraPastel
import com.example.medora.ui.theme.Spacing

// AI keys: one Gemini key (main) and one OpenRouter key (backup). Both optional.
@Composable
fun SettingsScreen(onOpenNotifications: () -> Unit = {}) {
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
        ActivityResultContracts.CreateDocument("application/zip"),
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
        SettingsStatsRow()

        SettingsNotificationsGroup(onOpen = onOpenNotifications)

        AppearanceSettingsCard()

        Text(
            text = stringResource(R.string.ai_section),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(text = stringResource(R.string.ai_privacy), style = MaterialTheme.typography.bodySmall)

        SettingsSection(title = "Gemini", badge = "G", toneIndex = 3) {
            Text(stringResource(R.string.gemini_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it; saved = false },
                label = { Text(stringResource(R.string.api_key)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = CircleShape,
                colors = settingsFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = geminiModel,
                onValueChange = { geminiModel = it; saved = false },
                label = { Text(stringResource(R.string.model_name)) },
                singleLine = true,
                shape = CircleShape,
                colors = settingsFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SettingsSection(title = "OpenRouter", badge = "O", toneIndex = 1) {
            Text(stringResource(R.string.openrouter_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = orKey,
                onValueChange = { orKey = it; saved = false },
                label = { Text(stringResource(R.string.api_key)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = CircleShape,
                colors = settingsFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = orModel,
                onValueChange = { orModel = it; saved = false },
                label = { Text(stringResource(R.string.model_name)) },
                singleLine = true,
                shape = CircleShape,
                colors = settingsFieldColors(),
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

        SettingsSection(title = stringResource(R.string.backup_section), badge = "\u2601", toneIndex = 2) {
            Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodySmall)
            MedoraButton(
                text = stringResource(R.string.backup_export),
                onClick = { exportLauncher.launch("medora-backup.zip") },
                modifier = Modifier.fillMaxWidth(),
            )
            MedoraOutlinedButton(
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

// White rounded fields that sit nicely on a pastel card.
@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = Color.Transparent,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
)

// One pastel rounded block: a white round badge, the title, then the content.
@Composable
private fun SettingsSection(
    title: String,
    badge: String,
    toneIndex: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tone = MedoraPastel.tone(toneIndex)
    CompositionLocalProvider(LocalContentColor provides tone.content) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(tone.container)
                .padding(Spacing.card),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(Spacing.m))
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}
