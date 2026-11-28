package com.example.medora.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.medora.R
import com.example.medora.reminder.ReminderScheduler
import com.example.medora.ui.icons.MedoraIcons
import com.example.medora.ui.components.MedoraSwitch
import com.example.medora.ui.theme.Spacing

// Settings card: daily review reminder on/off and hour (0-23).
@Composable
fun ReminderSettingsCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(ReminderScheduler.isEnabled(context)) }
    var hour by remember { mutableStateOf(ReminderScheduler.getHour(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            ReminderScheduler.enable(context, hour)
            enabled = true
        } else {
            enabled = false
        }
    }

    fun turnOn() {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ReminderScheduler.enable(context, hour)
            enabled = true
        }
    }

    SettingsGroup(title = stringResource(R.string.reminder_section)) {
        Text(stringResource(R.string.reminder_hint), style = MaterialTheme.typography.bodySmall)
        SettingsRow(
            icon = MedoraIcons.Planner,
            title = stringResource(R.string.reminder_enable),
            toneIndex = 1,
            trailing = {
                MedoraSwitch(
                    checked = enabled,
                    onCheckedChange = { on ->
                        if (on) {
                            turnOn()
                        } else {
                            ReminderScheduler.disable(context)
                            enabled = false
                        }
                    },
                )
            },
        )
        SettingsDivider()
        SettingsRow(
            icon = MedoraIcons.Study,
            title = stringResource(R.string.reminder_time),
            toneIndex = 2,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    TextButton(onClick = {
                        hour = (hour + 23) % 24
                        ReminderScheduler.changeHour(context, hour)
                    }) { Text("\u2212") }
                    Text(
                        text = String.format(java.util.Locale.US, "%02d:00", hour),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = {
                        hour = (hour + 1) % 24
                        ReminderScheduler.changeHour(context, hour)
                    }) { Text("+") }
                }
            },
        )
    }
}
