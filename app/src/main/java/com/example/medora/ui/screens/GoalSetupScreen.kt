package com.example.medora.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.medora.R
import com.example.medora.settings.AppPrefs
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.theme.Dimens
import com.example.medora.ui.theme.MedoraShapesSet
import com.example.medora.ui.theme.Spacing

private val goals = listOf(10, 20, 30, 50)

// Pick the daily card goal. Saved on Continue; can be changed later in Progress.
@Composable
fun GoalSetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var goal by remember {
        mutableIntStateOf(AppPrefs.dailyGoal(context).let { if (it in goals) it else AppPrefs.DEFAULT_GOAL })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Dimens.screenPadding),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = stringResource(R.string.goal_setup_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.goal_setup_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.m, bottom = Dimens.sectionGap),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
                GoalOption(goals[0], goal == goals[0]) { goal = goals[0] }
                GoalOption(goals[1], goal == goals[1]) { goal = goals[1] }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.blockGap),
                modifier = Modifier.padding(top = Dimens.blockGap),
            ) {
                GoalOption(goals[2], goal == goals[2]) { goal = goals[2] }
                GoalOption(goals[3], goal == goals[3]) { goal = goals[3] }
            }
        }
        MedoraButton(
            text = stringResource(R.string.goal_setup_continue),
            onClick = {
                AppPrefs.setDailyGoal(context, goal)
                AppPrefs.setOnboarded(context)
                onDone()
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.sectionGap),
        )
    }
}

@Composable
private fun RowScope.GoalOption(value: Int, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 96.dp)
            .clip(MedoraShapesSet.card)
            .background(if (selected) scheme.primaryContainer else scheme.surface)
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
                MedoraShapesSet.card,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = scheme.onSurface,
        )
        Text(
            text = stringResource(R.string.goal_setup_cards),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
    }
}
