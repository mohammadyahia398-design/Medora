package com.example.medora.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.settings.AppPrefs
import com.example.medora.ui.icons.themed
import com.example.medora.ui.theme.Dimens
import com.example.medora.ui.theme.MedoraColors
import com.example.medora.ui.theme.MedoraPastel
import com.example.medora.ui.theme.Spacing

// Building blocks for the Settings screen. Same idea as the reference (stats on top,
// grouped rows with round icon badges), in MEDORA's own look: fully round corners,
// one blue, pastel only to tell things apart.
private val SettingsShape = RoundedCornerShape(28.dp)

// Three small pastel capsules at the top: streak, reviews today, daily goal.
@Composable
internal fun SettingsStatsRow(vm: StudyWeekViewModel = viewModel()) {
    val week by vm.state.collectAsState()
    val goal = AppPrefs.dailyGoal(LocalContext.current)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SettingsStat(week.streak.toString(), stringResource(R.string.stat_streak), 0, Modifier.weight(1f))
        SettingsStat(week.today.toString(), stringResource(R.string.b9_today), 1, Modifier.weight(1f))
        SettingsStat(goal.toString(), stringResource(R.string.b9_goal), 2, Modifier.weight(1f))
    }
}

@Composable
private fun SettingsStat(value: String, label: String, toneIndex: Int, modifier: Modifier = Modifier) {
    val tone = MedoraPastel.tone(toneIndex)
    Column(
        modifier = modifier
            .clip(SettingsShape)
            .background(tone.container)
            .padding(vertical = Spacing.l, horizontal = Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = tone.content,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = tone.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// A title, then one white fully rounded card that holds the rows.
@Composable
internal fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.xs),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SettingsShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Spacing.card, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            content = content,
        )
    }
}

// One row: round pastel badge with an icon, the title, and an optional control at the end.
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    toneIndex: Int,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val container = if (danger) MedoraColors.Coral.copy(alpha = 0.35f) else MedoraPastel.tone(toneIndex).container
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(Dimens.roundButton).clip(CircleShape).background(container),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = icon.themed(),
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconM),
            )
        }
        Spacer(Modifier.width(Spacing.m))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

// Thin line between rows.
@Composable
internal fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}
