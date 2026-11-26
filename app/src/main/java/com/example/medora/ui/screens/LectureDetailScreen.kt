package com.example.medora.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.Card
import com.example.medora.domain.CardSuggester
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard

private data class CardDraft(val front: String, val back: String, val source: String)

// One lecture: its cards on top, then its text. Tap a line to make a card from it.
@Composable
fun LectureDetailScreen(
    id: Long,
    vm: LibraryViewModel = viewModel(),
    cardsVm: CardsViewModel = viewModel(),
) {
    val lecture by remember(id) { vm.lecture(id) }.collectAsState(initial = null)
    val cards by remember(id) { cardsVm.cardsOf(id) }.collectAsState(initial = emptyList())
    val lines = remember(lecture?.content) { lecture?.content.orEmpty().split("\n") }
    var draft by remember { mutableStateOf<CardDraft?>(null) }
    var suggestions by remember { mutableStateOf<List<CardSuggester.Suggestion>?>(null) }
    val current = lecture ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (current.subject.isNotBlank()) {
                    Text(text = current.subject, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedoraButton(
                        text = stringResource(R.string.suggest_cards),
                        onClick = {
                            val existing = cards.map { it.front }.toSet()
                            suggestions = CardSuggester.suggest(current.content, existing)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    MedoraButton(
                        text = stringResource(R.string.add_card),
                        onClick = { draft = CardDraft("", "", "") },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        if (cards.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.cards_count, cards.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(cards, key = { it.id }) { card ->
                CardRow(card, onDelete = { cardsVm.delete(card) })
                Spacer(Modifier.height(8.dp))
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        if (current.content.isBlank()) {
            item { Text(text = stringResource(R.string.no_content), style = MaterialTheme.typography.bodyMedium) }
        } else {
            item {
                Text(
                    text = stringResource(R.string.tap_line_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
            }
            itemsIndexed(lines) { _, line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = line.isNotBlank()) {
                            draft = CardDraft("", line.trim(), line.trim())
                        }
                        .padding(vertical = 4.dp),
                )
            }
        }
    }

    draft?.let { d ->
        CardEditDialog(
            draft = d,
            onDismiss = { draft = null },
            onSave = { front, back ->
                cardsVm.add(id, front, back, d.source)
                draft = null
            },
        )
    }

    suggestions?.let { list ->
        SuggestionsDialog(
            items = list,
            onDismiss = { suggestions = null },
            onAdd = { chosen ->
                cardsVm.addSuggestions(id, chosen)
                suggestions = null
            },
        )
    }
}

@Composable
private fun CardRow(card: Card, onDelete: () -> Unit) {
    MedoraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(card.front, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(card.back, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun CardEditDialog(draft: CardDraft, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var front by remember { mutableStateOf(draft.front) }
    var back by remember { mutableStateOf(draft.back) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_card)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text(stringResource(R.string.card_front)) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text(stringResource(R.string.card_back)) },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(front.trim(), back.trim()) },
                enabled = front.isNotBlank() && back.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun SuggestionsDialog(
    items: List<CardSuggester.Suggestion>,
    onDismiss: () -> Unit,
    onAdd: (List<CardSuggester.Suggestion>) -> Unit,
) {
    val selected = remember { mutableStateListOf<Int>().apply { addAll(items.indices) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.suggestions_title)) },
        text = {
            if (items.isEmpty()) {
                Text(stringResource(R.string.suggestions_empty))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    itemsIndexed(items) { i, s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (i in selected) selected.remove(i) else selected.add(i) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = i in selected, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(s.front, fontWeight = FontWeight.Bold)
                                Text(
                                    s.back,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (items.isEmpty()) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            } else {
                TextButton(
                    onClick = { onAdd(items.filterIndexed { i, _ -> i in selected }) },
                    enabled = selected.isNotEmpty(),
                ) { Text(stringResource(R.string.add_selected, selected.size)) }
            }
        },
        dismissButton = {
            if (items.isNotEmpty()) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
