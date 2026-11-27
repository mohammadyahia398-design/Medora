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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.medora.ui.theme.MedoraColors

// True/False, short answer, and weak-points retest. Each answer is explained
// with the correct answer, the source line from the lecture and its page.
@Composable
fun PracticeScreen(mode: String, onOpenLecture: (Long) -> Unit, vm: PracticeViewModel = viewModel()) {
    LaunchedEffect(mode) { vm.start(mode) }
    val items = vm.items
    when {
        !vm.loaded -> Unit
        items.isEmpty() -> PracticeCenter(
            stringResource(if (mode == PRACTICE_WEAK) R.string.practice_weak_empty else R.string.practice_empty)
        )
        vm.index >= items.size -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.score_of, vm.score, items.size),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(24.dp))
            MedoraButton(text = stringResource(R.string.restart), onClick = { vm.restart() })
        }
        else -> PracticeQuestion(vm, mode, onOpenLecture)
    }
}

@Composable
private fun PracticeCenter(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun answerColors(answered: Boolean, isCorrect: Boolean, isChosen: Boolean): ButtonColors = when {
    !answered -> ButtonDefaults.buttonColors()
    isCorrect -> ButtonDefaults.buttonColors(containerColor = MedoraColors.Green, contentColor = MedoraColors.Ink)
    isChosen -> ButtonDefaults.buttonColors(containerColor = MedoraColors.Coral, contentColor = MedoraColors.Ink)
    else -> ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PracticeQuestion(vm: PracticeViewModel, mode: String, onOpenLecture: (Long) -> Unit) {
    val item = vm.items[vm.index]
    var typed by remember(vm.index) { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            text = stringResource(R.string.progress_of, vm.index + 1, vm.items.size),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
            Text(
                text = item.card.front,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (mode == PRACTICE_TF) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.practice_tf_statement, item.statement),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        when (mode) {
            PRACTICE_TF -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(true, false).forEachIndexed { i, value ->
                    Button(
                        onClick = { vm.answerTrueFalse(value) },
                        modifier = Modifier.weight(1f),
                        colors = answerColors(vm.answered, value == item.isTrue, vm.chosen == i),
                    ) {
                        Text(stringResource(if (value) R.string.practice_true else R.string.practice_false))
                    }
                }
            }
            PRACTICE_SHORT -> {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = { Text(stringResource(R.string.practice_type_answer)) },
                    enabled = !vm.answered,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (!vm.answered) {
                    MedoraButton(
                        text = stringResource(R.string.practice_check),
                        onClick = { vm.answerShort(typed) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            else -> item.options.forEachIndexed { i, option ->
                Button(
                    onClick = { vm.answerChoice(i) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = answerColors(vm.answered, i == item.correctIndex, vm.chosen == i),
                ) {
                    Text(option)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (vm.answered) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(if (vm.lastCorrect) R.string.correct else R.string.wrong),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (mode == PRACTICE_SHORT && !vm.lastCorrect) {
                TextButton(onClick = { vm.countAsCorrect() }) {
                    Text(stringResource(R.string.practice_count_correct))
                }
            }
            Spacer(Modifier.height(8.dp))
            MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
                Text(
                    text = stringResource(R.string.practice_explanation),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.exam_correct_answer, item.card.back),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val source = item.card.source
                if (source.isNotBlank() && source != item.card.back) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.source_label) + ": " + source,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                item.page?.let { page ->
                    Text(
                        text = stringResource(R.string.page_label, page),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { onOpenLecture(item.card.lectureId) }) {
                    Text(stringResource(R.string.open_lecture))
                }
            }
            Spacer(Modifier.height(8.dp))
            MedoraButton(
                text = stringResource(R.string.next),
                onClick = { vm.next() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
