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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.domain.Fsrs
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard

@Composable
fun ReviewScreen(onOpenLecture: (Long) -> Unit, vm: ReviewViewModel = viewModel()) {
    val deck = vm.deck
    when {
        !vm.loaded -> Unit
        deck.isEmpty() -> CenterText(stringResource(R.string.no_due_cards))
        vm.index >= deck.size -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.done),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.knew_count, vm.knew, deck.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(24.dp))
            MedoraButton(text = stringResource(R.string.restart), onClick = { vm.restart() })
        }
        else -> {
            val card = deck[vm.index]
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.progress_of, vm.index + 1, deck.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                MedoraCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = card.front,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (vm.revealed) {
                        Spacer(Modifier.height(16.dp))
                        Text(text = card.back, style = MaterialTheme.typography.bodyLarge)
                        if (card.source.isNotBlank() && card.source != card.back) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.source_label) + ": " + card.source,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        vm.pages[card.id]?.let { page ->
                            Text(
                                text = stringResource(R.string.page_label, page),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = { onOpenLecture(card.lectureId) }) {
                            Text(stringResource(R.string.open_lecture))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (!vm.revealed) {
                    MedoraButton(
                        text = stringResource(R.string.show_answer),
                        onClick = { vm.reveal() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MedoraButton(
                            text = stringResource(R.string.rate_again),
                            onClick = { vm.answer(Fsrs.Rating.Again) },
                            modifier = Modifier.weight(1f),
                        )
                        MedoraButton(
                            text = stringResource(R.string.rate_hard),
                            onClick = { vm.answer(Fsrs.Rating.Hard) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MedoraButton(
                            text = stringResource(R.string.rate_good),
                            onClick = { vm.answer(Fsrs.Rating.Good) },
                            modifier = Modifier.weight(1f),
                        )
                        MedoraButton(
                            text = stringResource(R.string.rate_easy),
                            onClick = { vm.answer(Fsrs.Rating.Easy) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CenterText(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
