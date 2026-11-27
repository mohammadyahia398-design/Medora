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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.medora.export.PdfExporter
import com.example.medora.export.PdfLabels
import com.example.medora.export.PptxExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    aiVm: AiViewModel = viewModel(),
    onEdit: () -> Unit = {},
) {
    val lecture by remember(id) { vm.lecture(id) }.collectAsState(initial = null)
    val cards by remember(id) { cardsVm.cardsOf(id) }.collectAsState(initial = emptyList())
    val lines = remember(lecture?.content) { lecture?.content.orEmpty().split("\n") }
    var draft by remember { mutableStateOf<CardDraft?>(null) }
    var suggestions by remember { mutableStateOf<List<CardSuggester.Suggestion>?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfLabels = PdfLabels(
        summary = stringResource(R.string.summary),
        cards = stringResource(R.string.cards_count, cards.size),
        disclaimer = stringResource(R.string.ai_disclaimer),
    )
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val l = lecture
        if (uri != null && l != null) {
            val snapshot = cards
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        PdfExporter.write(context, uri, l.title, l.subject, l.summary, l.content, snapshot, pdfLabels)
                    }.isSuccess
                }
                Toast.makeText(
                    context,
                    if (ok) R.string.pdf_saved else R.string.pdf_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    val pptxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
    ) { uri ->
        val l = lecture
        if (uri != null && l != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        PptxExporter.write(context, uri, l.title, l.subject, l.slidesJson)
                    }.isSuccess
                }
                Toast.makeText(
                    context,
                    if (ok) R.string.pptx_saved else R.string.pptx_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
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
                MedoraButton(
                    text = stringResource(R.string.edit_note),
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
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
                if (current.content.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    if (aiVm.busy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.ai_working))
                        }
                    } else {
                        MedoraButton(
                            text = stringResource(R.string.ai_generate),
                            onClick = {
                                aiVm.generate(
                                    current.id,
                                    current.title,
                                    current.content,
                                    cards.map { it.front }.toSet(),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                MedoraButton(
                    text = stringResource(R.string.export_pdf),
                    onClick = {
                        val name = current.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(40).ifBlank { "lecture" }
                        pdfLauncher.launch("$name.pdf")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (current.slidesJson.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    MedoraButton(
                        text = stringResource(R.string.export_pptx),
                        onClick = {
                            val name = current.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(40).ifBlank { "lecture" }
                            pptxLauncher.launch("$name.pptx")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (current.summary.isNotBlank()) {
                    MedoraCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.summary),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(text = current.summary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ai_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
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
                val t = line.trimStart()
                val isHeading = t.startsWith("# ")
                val shown = when {
                    isHeading -> t.removePrefix("# ")
                    t.startsWith("- ") -> "\u2022 " + t.removePrefix("- ")
                    else -> line
                }
                Text(
                    text = shown,
                    style = if (isHeading) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isHeading) FontWeight.Bold else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = line.isNotBlank()) {
                            draft = CardDraft("", shown.trim(), shown.trim())
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

    aiVm.newCards?.let { list ->
        SuggestionsDialog(
            items = list,
            onDismiss = { aiVm.clearCards() },
            onAdd = { chosen ->
                cardsVm.addSuggestions(id, chosen)
                aiVm.clearCards()
            },
        )
    }

    if (aiVm.noKey || aiVm.error != null) {
        AlertDialog(
            onDismissRequest = { aiVm.clearError() },
            title = { Text(stringResource(R.string.ai_failed)) },
            text = {
                Text(if (aiVm.noKey) stringResource(R.string.ai_no_key) else aiVm.error.orEmpty())
            },
            confirmButton = {
                TextButton(onClick = { aiVm.clearError() }) { Text(stringResource(R.string.ok)) }
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
