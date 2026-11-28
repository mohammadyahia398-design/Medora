package com.example.medora.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.Lecture
import com.example.medora.domain.Insights
import com.example.medora.export.PdfExporter
import com.example.medora.export.PdfLabels
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard
import com.example.medora.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// List of study packs + "New pack".
@Composable
fun PacksScreen(onOpenPack: (Long) -> Unit, vm: PacksViewModel = viewModel()) {
    val packs by vm.packs.collectAsState()
    val lectures by vm.lectures.collectAsState()
    val cards by vm.cards.collectAsState()
    var showNew by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item {
            Text(
                text = stringResource(R.string.packs_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            MedoraButton(
                text = stringResource(R.string.packs_new),
                onClick = { showNew = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (packs.isEmpty()) {
            item { Text(stringResource(R.string.packs_empty), style = MaterialTheme.typography.bodyLarge) }
        }
        items(packs, key = { it.id }) { pack ->
            val ids = pack.ids().toSet()
            val inPack = lectures.filter { it.id in ids }
            val cardCount = cards.count { it.lectureId in ids }
            MedoraCard(modifier = Modifier.fillMaxWidth().clickable { onOpenPack(pack.id) }) {
                Text(pack.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.packs_counts, inPack.size, cardCount),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (showNew) {
        PackEditDialog(
            title = stringResource(R.string.packs_new),
            name = "",
            selected = emptySet(),
            lectures = lectures,
            onDismiss = { showNew = false },
            onSave = { name, ids ->
                vm.create(name, ids)
                showNew = false
            },
        )
    }
}

// One pack: its lectures with summary and AI parts, edit, export as one PDF, delete.
@Composable
fun PackDetailScreen(
    id: Long,
    onOpenLecture: (Long) -> Unit,
    onDeleted: () -> Unit,
    vm: PacksViewModel = viewModel(),
) {
    val pack by remember(id) { vm.pack(id) }.collectAsState(initial = null)
    val lectures by vm.lectures.collectAsState()
    val cards by vm.cards.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val current = pack
    val ids = current?.ids().orEmpty()
    // Keep the pack's own order; deleted lectures are skipped.
    val inPack = remember(ids, lectures) { ids.mapNotNull { pid -> lectures.firstOrNull { it.id == pid } } }
    val packCards = remember(ids, cards) { cards.filter { it.lectureId in ids } }

    val termsLabel = stringResource(R.string.insights_terms)
    val tablesLabel = stringResource(R.string.insights_tables)
    val classesLabel = stringResource(R.string.insights_classes)
    val labels = PdfLabels(
        summary = stringResource(R.string.packs_contents),
        cards = stringResource(R.string.cards_count, packCards.size),
        disclaimer = stringResource(R.string.ai_disclaimer),
    )
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val p = pack
        if (uri != null && p != null) {
            val text = inPack.joinToString("\n\n") { l ->
                val parts = listOf(
                    "# " + l.title,
                    l.summary,
                    Insights.parse(l.insightsJson).toMarkdown(termsLabel, tablesLabel, classesLabel),
                )
                parts.filter { it.isNotBlank() }.joinToString("\n\n")
            }
            val snapshot = packCards
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching { PdfExporter.write(context, uri, p.name, "", text, "", snapshot, labels) }.isSuccess
                }
                Toast.makeText(context, if (ok) R.string.pdf_saved else R.string.pdf_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (current == null) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(current.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.packs_counts, inPack.size, packCards.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    MedoraButton(
                        text = stringResource(R.string.packs_edit),
                        onClick = { showEdit = true },
                        modifier = Modifier.weight(1f),
                    )
                    MedoraButton(
                        text = stringResource(R.string.export_pdf),
                        onClick = {
                            val name = current.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(40).ifBlank { "pack" }
                            pdfLauncher.launch("$name.pdf")
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.packs_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (inPack.isEmpty()) {
            item { Text(stringResource(R.string.packs_no_lectures), style = MaterialTheme.typography.bodyLarge) }
        }
        items(inPack, key = { it.id }) { l ->
            val insights = remember(l.insightsJson) { Insights.parse(l.insightsJson) }
            MedoraCard(modifier = Modifier.fillMaxWidth().clickable { onOpenLecture(l.id) }) {
                Text(l.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (l.subject.isNotBlank()) Text(l.subject, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = stringResource(
                        R.string.packs_lecture_parts,
                        packCards.count { it.lectureId == l.id },
                        insights.terms.size,
                        insights.tables.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (l.summary.isNotBlank()) {
                    Text(
                        text = l.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.packs_no_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showEdit) {
        PackEditDialog(
            title = stringResource(R.string.packs_edit),
            name = current.name,
            selected = ids.toSet(),
            lectures = lectures,
            onDismiss = { showEdit = false },
            onSave = { name, newIds ->
                vm.update(current.id, name, newIds)
                showEdit = false
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(stringResource(R.string.packs_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(current.id)
                    onDeleted()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

// Name + pick lectures. Lectures stay in the order they were picked.
@Composable
private fun PackEditDialog(
    title: String,
    name: String,
    selected: Set<Long>,
    lectures: List<Lecture>,
    onDismiss: () -> Unit,
    onSave: (String, List<Long>) -> Unit,
) {
    var packName by remember { mutableStateOf(name) }
    val chosen = remember { mutableStateListOf<Long>().apply { addAll(selected) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                OutlinedTextField(
                    value = packName,
                    onValueChange = { packName = it },
                    label = { Text(stringResource(R.string.packs_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (lectures.isEmpty()) {
                    Text(stringResource(R.string.library_empty))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(lectures, key = { it.id }) { l ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (l.id in chosen) chosen.remove(l.id) else chosen.add(l.id) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = l.id in chosen, onCheckedChange = null)
                                Spacer(Modifier.width(Spacing.s))
                                Column {
                                    Text(l.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val sub = l.folder.ifBlank { l.subject }
                                    if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(packName, chosen.toList()) },
                enabled = packName.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
