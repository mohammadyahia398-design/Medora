package com.example.medora.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medora.R
import com.example.medora.data.BackupManager
import com.example.medora.data.DataEraser
import com.example.medora.settings.AppPrefs
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.medora.ui.theme.Spacing

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
private fun OptionRow(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        labels.forEachIndexed { i, label ->
            if (i == selectedIndex) {
                Button(
                    onClick = { onSelect(i) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) { Text(label, maxLines = 1) }
            } else {
                OutlinedButton(
                    onClick = { onSelect(i) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) { Text(label, maxLines = 1) }
            }
        }
    }
}

// Language and theme.
@Composable
fun AppearanceSettingsCard() {
    val context = LocalContext.current
    var lang by remember { mutableStateOf(AppPrefs.language(context)) }
    val theme = AppPrefs.theme.value

    val langCodes = listOf(AppPrefs.SYSTEM, AppPrefs.LANG_AR, AppPrefs.LANG_EN)
    val langLabels = listOf(stringResource(R.string.option_system), "العربية", "English")
    val themeCodes = listOf(AppPrefs.SYSTEM, AppPrefs.LIGHT, AppPrefs.DARK)
    val themeLabels = listOf(
        stringResource(R.string.option_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark),
    )

    MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
        Text(
            text = stringResource(R.string.appearance_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(Spacing.m))
        Text(stringResource(R.string.language_label), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.xs))
        OptionRow(langLabels, langCodes.indexOf(lang).coerceAtLeast(0)) { i ->
            val code = langCodes[i]
            if (code != lang) {
                lang = code
                AppPrefs.setLanguage(context, code)
                context.findActivity()?.recreate()
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Text(stringResource(R.string.theme_label), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.xs))
        OptionRow(themeLabels, themeCodes.indexOf(theme).coerceAtLeast(0)) { i ->
            AppPrefs.setTheme(context, themeCodes[i])
        }
    }
}

// Delete all user data from this phone (with a confirmation).
@Composable
fun DeleteDataCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    MedoraCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.data_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(stringResource(R.string.data_delete_hint), style = MaterialTheme.typography.bodySmall)
        MedoraButton(
            text = stringResource(R.string.data_delete),
            onClick = { confirm = true },
            modifier = Modifier.fillMaxWidth(),
        )
        if (failed) {
            Text(
                text = stringResource(R.string.data_delete_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.data_confirm_title)) },
            text = { Text(stringResource(R.string.data_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { DataEraser.eraseAll(context) }
                        if (ok) BackupManager.restartApp(context) else failed = true
                    }
                }) { Text(stringResource(R.string.data_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
