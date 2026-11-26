package com.example.medora.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

@Composable
fun QuizScreen(onOpenLecture: (Long) -> Unit, vm: QuizViewModel = viewModel()) {
    val questions = vm.questions
    when {
        !vm.loaded -> Unit
        questions.isEmpty() -> CenterText(stringResource(R.string.quiz_need_cards))
        vm.index >= questions.size -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.score_of, vm.score, questions.size),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(24.dp))
            MedoraButton(text = stringResource(R.string.restart), onClick = { vm.restart() })
        }
        else -> {
            val q = questions[vm.index]
            val selected = vm.selected
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.progress_of, vm.index + 1, questions.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                MedoraCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = q.card.front,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(16.dp))
                q.options.forEachIndexed { i, option ->
                    val colors = when {
                        selected == null -> ButtonDefaults.buttonColors()
                        i == q.correctIndex -> ButtonDefaults.buttonColors(
                            containerColor = MedoraColors.Green,
                            contentColor = MedoraColors.Ink,
                        )
                        i == selected -> ButtonDefaults.buttonColors(
                            containerColor = MedoraColors.Coral,
                            contentColor = MedoraColors.Ink,
                        )
                        else -> ButtonDefaults.buttonColors(
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
                    Spacer(Modifier.height(8.dp))
                }
                if (selected != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            if (selected == q.correctIndex) R.string.correct else R.string.wrong
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (q.card.source.isNotBlank() && q.card.source != q.card.back) {
                        Text(
                            text = stringResource(R.string.source_label) + ": " + q.card.source,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { onOpenLecture(q.card.lectureId) }) {
                        Text(stringResource(R.string.open_lecture))
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
    }
}
