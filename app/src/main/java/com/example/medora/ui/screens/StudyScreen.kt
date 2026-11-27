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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard
import com.example.medora.ui.components.MedoraOutlinedButton
import com.example.medora.ui.theme.Spacing

private const val MIN_CARDS_FOR_QUIZ = 4

@Composable
fun StudyScreen(
    onReview: () -> Unit,
    onQuiz: () -> Unit,
    onExam: () -> Unit = {},
    onSources: () -> Unit = {},
    onPractice: (String) -> Unit = {},
    onMistakes: () -> Unit = {},
    vm: CardsViewModel = viewModel(),
) {
    val cards by vm.cards.collectAsState()
    val due by vm.dueCount.collectAsState()
    val aiQuestions by vm.aiQuestionCount.collectAsState()
    val mistakes by vm.mistakeCount.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (cards.isEmpty() && aiQuestions == 0) {
                    Text(
                        text = stringResource(R.string.study_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.cards_total, cards.size),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.due_now, due),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    if (due > 0) {
                        MedoraButton(
                            text = stringResource(R.string.start_review),
                            onClick = onReview,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else if (cards.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.no_due_cards),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (cards.size >= MIN_CARDS_FOR_QUIZ || aiQuestions > 0) {
                        MedoraButton(
                            text = stringResource(R.string.start_quiz),
                            onClick = onQuiz,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        MedoraButton(
                            text = stringResource(R.string.start_exam),
                            onClick = onExam,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.quiz_need_cards),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (cards.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MedoraOutlinedButton(
                                text = stringResource(R.string.practice_tf_title),
                                onClick = { onPractice(PRACTICE_TF) },
                                modifier = Modifier.weight(1f),
                            )
                            MedoraOutlinedButton(
                                text = stringResource(R.string.practice_short_title),
                                onClick = { onPractice(PRACTICE_SHORT) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        MedoraOutlinedButton(
                            text = stringResource(R.string.practice_weak_title),
                            onClick = { onPractice(PRACTICE_WEAK) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        if (mistakes > 0) {
            Spacer(Modifier.height(Spacing.m))
            MedoraButton(
                text = stringResource(R.string.start_mistakes, mistakes),
                onClick = onMistakes,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        MedoraButton(
            text = stringResource(R.string.start_sources),
            onClick = onSources,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
