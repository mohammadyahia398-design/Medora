package com.example.medora.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard
import java.util.Locale
import com.example.medora.ui.theme.Spacing

private fun formatTime(totalSeconds: Int): String =
    String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

@Composable
fun ExamScreen(onOpenLecture: (Long) -> Unit, vm: ExamViewModel = viewModel()) {
    when {
        !vm.loaded -> Unit
        vm.phase == ExamPhase.Setup -> ExamSetup(vm)
        vm.phase == ExamPhase.Running -> ExamRunning(vm)
        else -> ExamResult(vm, onOpenLecture)
    }
}

@Composable
private fun ExamSetup(vm: ExamViewModel) {
    val options = vm.countOptions
    if (options.isEmpty()) {
        CenterText(stringResource(R.string.quiz_need_cards))
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
            Text(
                text = stringResource(R.string.exam_setup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(R.string.exam_setup_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = stringResource(R.string.exam_questions_count),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            options.forEach { n ->
                if (n == vm.chosenCount) {
                    Button(onClick = { vm.chooseCount(n) }) { Text(n.toString()) }
                } else {
                    OutlinedButton(onClick = { vm.chooseCount(n) }) { Text(n.toString()) }
                }
            }
        }
        Text(
            text = stringResource(R.string.exam_time_info, vm.chosenCount),
            style = MaterialTheme.typography.bodyMedium,
        )
        MedoraButton(
            text = stringResource(R.string.exam_start),
            onClick = { vm.start() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExamRunning(vm: ExamViewModel) {
    val questions = vm.questions
    val answers = vm.answers
    val index = vm.index
    if (index !in questions.indices || index !in answers.indices) return
    val q = questions[index]
    val selected = answers[index]
    var confirmFinish by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.progress_of, index + 1, questions.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = formatTime(vm.remaining),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (vm.remaining <= 60) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            )
        }
        Text(
            text = stringResource(R.string.exam_answered, questions.size - vm.unanswered, questions.size),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(Spacing.m))
        MedoraCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = q.prompt,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(Spacing.l))
        q.options.forEachIndexed { i, option ->
            val colors = if (selected == i) {
                ButtonDefaults.buttonColors()
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { vm.select(i) },
                modifier = Modifier.fillMaxWidth(),
                colors = colors,
            ) {
                Text(option)
            }
            Spacer(Modifier.height(Spacing.s))
        }
        Spacer(Modifier.height(Spacing.s))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedButton(
                onClick = { vm.goTo(index - 1) },
                enabled = index > 0,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.exam_prev))
            }
            Button(
                onClick = { vm.goTo(index + 1) },
                enabled = index < questions.size - 1,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.next))
            }
        }
        Spacer(Modifier.height(Spacing.m))
        MedoraButton(
            text = stringResource(R.string.exam_finish),
            onClick = { confirmFinish = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(stringResource(R.string.exam_finish_title)) },
            text = { Text(stringResource(R.string.exam_finish_text, vm.unanswered)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    vm.finish()
                }) {
                    Text(stringResource(R.string.exam_finish))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ExamResult(vm: ExamViewModel, onOpenLecture: (Long) -> Unit) {
    val questions = vm.questions
    val answers = vm.answers
    val total = questions.size
    val noAnswer = stringResource(R.string.exam_no_answer)
    val wrong = questions.indices.filter { answers.getOrNull(it) != questions[it].correctIndex }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.score_of, vm.score, total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (total > 0) {
                    Text(
                        text = (vm.score * 100 / total).toString() + "%",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = stringResource(R.string.exam_time_used, formatTime(vm.usedSeconds)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (vm.timedOut) {
                    Text(
                        text = stringResource(R.string.exam_time_up),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (wrong.isEmpty()) {
            Text(
                text = stringResource(R.string.exam_all_correct),
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            Text(
                text = stringResource(R.string.exam_mistakes, wrong.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            wrong.forEach { i ->
                val q = questions[i]
                val mine = answers.getOrNull(i)?.let { q.options.getOrNull(it) } ?: noAnswer
                MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
                    Text(
                        text = q.prompt,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = stringResource(R.string.exam_your_answer, mine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.exam_correct_answer, q.options[q.correctIndex]),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (q.source.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.source_label) + ": " + q.source,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    q.page?.let { page ->
                        Text(
                            text = stringResource(R.string.page_label, page),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { onOpenLecture(q.lectureId) }) {
                        Text(stringResource(R.string.open_lecture))
                    }
                }
            }
        }
        MedoraButton(
            text = stringResource(R.string.exam_retry),
            onClick = { vm.retry() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
