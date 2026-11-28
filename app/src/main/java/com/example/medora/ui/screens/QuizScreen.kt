package com.example.medora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.ui.components.AiBadge
import com.example.medora.ui.components.GlassBox
import com.example.medora.ui.components.ImmersiveGlass
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraImmersive
import com.example.medora.ui.theme.MedoraColors
import com.example.medora.ui.theme.Spacing

// Quiz on the same dark scene as the cards. The question and its four options sit in
// one frosted glass card. Right answer green with a check, wrong answer coral with a cross.
@Composable
fun QuizScreen(
    onOpenLecture: (Long) -> Unit,
    onBack: () -> Unit = {},
    vm: QuizViewModel = viewModel(),
) {
    val questions = vm.questions
    MedoraImmersive { glass ->
        when {
            !vm.loaded -> Unit
            questions.isEmpty() -> StudyMessage(glass, onBack) {
                Text(
                    text = stringResource(R.string.quiz_need_cards),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            vm.index >= questions.size -> StudyMessage(glass, onBack) {
                Text(
                    text = stringResource(R.string.score_of, vm.score, questions.size),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xl))
                MedoraButton(text = stringResource(R.string.restart), onClick = { vm.restart() })
            }
            else -> {
                val q = questions[vm.index]
                val selected = vm.selected
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(Spacing.l),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassBackButton(glass, onBack)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.progress_of, vm.index + 1, questions.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(Spacing.l))
                    val done = vm.index + (if (selected != null) 1 else 0)
                    SceneProgress(done.toFloat() / questions.size, MedoraColors.Green)
                    Spacer(Modifier.height(Spacing.l))

                    GlassBox(
                        backdrop = glass,
                        shape = RoundedCornerShape(28.dp),
                        tint = ImmersiveGlass.frost,
                        fallback = ImmersiveGlass.frostSolid,
                        modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(Spacing.l),
                            verticalArrangement = Arrangement.spacedBy(Spacing.m),
                        ) {
                            if (q.fromAi) AiBadge()
                            Text(
                                text = q.prompt,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            q.options.forEachIndexed { i, option ->
                                OptionRow(
                                    text = option,
                                    state = when {
                                        selected == null -> OptionState.Idle
                                        i == q.correctIndex -> OptionState.Right
                                        i == selected -> OptionState.Wrong
                                        else -> OptionState.Dim
                                    },
                                    enabled = selected == null,
                                    onClick = { vm.select(i) },
                                )
                            }
                            if (selected != null) {
                                Text(
                                    text = stringResource(if (selected == q.correctIndex) R.string.correct else R.string.wrong),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected == q.correctIndex) MedoraColors.Green else MedoraColors.Coral,
                                )
                                if (selected != q.correctIndex) {
                                    Text(
                                        text = stringResource(R.string.exam_correct_answer, q.options[q.correctIndex]),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                if (q.source.isNotBlank()) {
                                    Text(
                                        text = stringResource(R.string.source_label) + ": " + q.source,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                q.page?.let { page ->
                                    Text(
                                        text = stringResource(R.string.page_label, page),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                TextButton(onClick = { onOpenLecture(q.lectureId) }) {
                                    Text(stringResource(R.string.open_lecture))
                                }
                            }
                        }
                    }

                    if (selected != null) {
                        Spacer(Modifier.height(Spacing.l))
                        MedoraButton(
                            text = stringResource(R.string.next),
                            onClick = { vm.next() },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MedoraColors.Green,
                            contentColor = MedoraColors.Ink,
                        )
                    }
                }
            }
        }
    }
}

private enum class OptionState { Idle, Right, Wrong, Dim }

// One answer as a capsule. A thin translucent fill, not another glass layer.
@Composable
private fun OptionRow(text: String, state: OptionState, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val fill = when (state) {
        OptionState.Right -> MedoraColors.Green.copy(alpha = 0.30f)
        OptionState.Wrong -> MedoraColors.Coral.copy(alpha = 0.30f)
        OptionState.Dim -> Color.White.copy(alpha = 0.05f)
        OptionState.Idle -> Color.White.copy(alpha = 0.10f)
    }
    val line = when (state) {
        OptionState.Right -> MedoraColors.Green
        OptionState.Wrong -> MedoraColors.Coral
        else -> ImmersiveGlass.line
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = if (state == OptionState.Dim) 0.6f else 1f),
            modifier = Modifier.weight(1f),
        )
        if (state == OptionState.Right || state == OptionState.Wrong) {
            Box(
                modifier = Modifier
                    .padding(start = Spacing.m)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (state == OptionState.Right) MedoraColors.Green else MedoraColors.Coral),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state == OptionState.Right) "✓" else "✕",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MedoraColors.Ink,
                )
            }
        }
    }
}
