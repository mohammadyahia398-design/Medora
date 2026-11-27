package com.example.medora.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.Mistake
import com.example.medora.ui.components.MedoraCard
import com.example.medora.ui.theme.LocalMedoraDark
import com.example.medora.ui.theme.MedoraColors
import com.example.medora.ui.theme.Spacing

private const val KIND_ALL = ""
private const val KIND_QUIZ = "quiz"
private const val KIND_EXAM = "exam"
private const val KIND_PRACTICE = "practice"

// Saved wrong answers. Filter by where they came from; "Got it" removes one.
@Composable
fun MistakesScreen(onOpenLecture: (Long) -> Unit, vm: MistakesViewModel = viewModel()) {
    val all by vm.mistakes.collectAsState()
    val titles by vm.titles.collectAsState()
    var kind by rememberSaveable { mutableStateOf(KIND_ALL) }
    val shown = if (kind == KIND_ALL) all else all.filter { it.kind == kind }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            KindChip(R.string.mistakes_filter_all, kind == KIND_ALL) { kind = KIND_ALL }
            KindChip(R.string.mistakes_kind_quiz, kind == KIND_QUIZ) { kind = KIND_QUIZ }
            KindChip(R.string.mistakes_kind_exam, kind == KIND_EXAM) { kind = KIND_EXAM }
            KindChip(R.string.mistakes_kind_practice, kind == KIND_PRACTICE) { kind = KIND_PRACTICE }
        }
        if (shown.isEmpty()) {
            CenterText(stringResource(R.string.mistakes_empty))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                items(shown, key = { it.id }) { m ->
                    MistakeCard(
                        m = m,
                        lectureTitle = titles[m.lectureId].orEmpty(),
                        onOpenLecture = onOpenLecture,
                        onRemove = { vm.remove(m.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun KindChip(@StringRes label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(label)) },
    )
}

@Composable
private fun MistakeCard(
    m: Mistake,
    lectureTitle: String,
    onOpenLecture: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    val dark = LocalMedoraDark.current
    val kindRes = when (m.kind) {
        KIND_QUIZ -> R.string.mistakes_kind_quiz
        KIND_EXAM -> R.string.mistakes_kind_exam
        else -> R.string.mistakes_kind_practice
    }
    val header = stringResource(kindRes) + if (lectureTitle.isNotBlank()) " - $lectureTitle" else ""
    MedoraCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = m.question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (m.chosen.isNotBlank()) {
            Text(
                text = stringResource(R.string.mistakes_your_answer, m.chosen),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.mistakes_correct_answer, m.correct),
            style = MaterialTheme.typography.bodyMedium,
            color = if (dark) MedoraColors.Green else MedoraColors.GreenDeep,
        )
        if (m.source.isNotBlank() && m.source != m.correct) {
            Text(
                text = stringResource(R.string.source_label) + ": " + m.source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { onOpenLecture(m.lectureId) }) {
                Text(stringResource(R.string.open_lecture))
            }
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.mistakes_got_it))
            }
        }
    }
}
