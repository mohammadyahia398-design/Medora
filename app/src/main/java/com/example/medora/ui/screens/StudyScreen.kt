package com.example.medora.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

private const val MIN_CARDS_FOR_QUIZ = 4

@Composable
fun StudyScreen(onReview: () -> Unit, onQuiz: () -> Unit, vm: CardsViewModel = viewModel()) {
    val cards by vm.cards.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedoraCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (cards.isEmpty()) {
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
                    Spacer(Modifier.height(16.dp))
                    MedoraButton(
                        text = stringResource(R.string.start_review),
                        onClick = onReview,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    if (cards.size >= MIN_CARDS_FOR_QUIZ) {
                        MedoraButton(
                            text = stringResource(R.string.start_quiz),
                            onClick = onQuiz,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.quiz_need_cards),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
