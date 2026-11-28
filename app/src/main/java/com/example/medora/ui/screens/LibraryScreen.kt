package com.example.medora.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.medora.data.Lecture
import com.example.medora.ui.components.GlassBox
import com.example.medora.ui.components.MedoraCard
import com.example.medora.ui.components.glassSource
import com.example.medora.ui.components.rememberGlassBackdrop
import com.example.medora.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date

private const val MAX_PASTE_CHARS = 200_000
private const val SNIPPET_RADIUS = 50

private class LectureHit(val lecture: Lecture, val snippet: String?)
private class LectureGroup(val name: String, val items: List<LectureHit>)

// Filters by tag, then by title, subject, folder, tags or lecture text, then groups by
// folder (or subject when there is no folder), A-Z. A match inside the text shows a snippet.
private fun groupLectures(
    lectures: List<Lecture>,
    query: String,
    noSubject: String,
    tag: String?,
): List<LectureGroup> {
    val hits = lectures.filter { tag == null || tag in it.tagList() }.mapNotNull { l ->
        when {
            query.isEmpty() -> LectureHit(l, null)
            l.title.contains(query, true) || l.subject.contains(query, true) ||
                l.folder.contains(query, true) || l.tags.contains(query, true) -> LectureHit(l, null)
            else -> {
                val i = l.content.indexOf(query, ignoreCase = true)
                if (i < 0) {
                    null
                } else {
                    val start = (i - SNIPPET_RADIUS).coerceAtLeast(0)
                    val end = (i + query.length + SNIPPET_RADIUS).coerceAtMost(l.content.length)
                    val text = l.content.substring(start, end).replace('\n', ' ').trim()
                    LectureHit(l, (if (start > 0) "…" else "") + text + (if (end < l.content.length) "…" else ""))
                }
            }
        }
    }
    return hits
        .groupBy { it.lecture.folder.trim().ifEmpty { it.lecture.subject.trim() }.ifEmpty { noSubject } }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .map { (name, items) -> LectureGroup(name, items) }
}

@Composable
fun LibraryScreen(
    onOpenLecture: (Long) -> Unit,
    onOpenPacks: () -> Unit = {},
    vm: LibraryViewModel = viewModel(),
) {
    val lectures by vm.lectures.collectAsState()
    val importFailed by vm.importFailed.collectAsState()
    val importing by vm.importing.collectAsState()
    val ocrProgress by vm.ocrProgress.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importText(uri)
    }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Lecture?>(null) }
    var query by remember { mutableStateOf("") }
    val noSubject = stringResource(R.string.library_no_subject)
    var tag by remember { mutableStateOf<String?>(null) }
    val allTags = remember(lectures) { lectures.flatMap { it.tagList() }.distinct().sortedBy { it.lowercase() } }
    val groups = remember(lectures, query, noSubject, tag) { groupLectures(lectures, query.trim(), noSubject, tag) }

    val glass = rememberGlassBackdrop()
    val pill = RoundedCornerShape(28.dp)

    Box(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.fillMaxSize().glassSource(glass)) {
        if (lectures.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(Spacing.xxl),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = 84.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                if (allTags.isNotEmpty()) {
                    item(key = "tags") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            items(allTags, key = { "t_$it" }) { t ->
                                FilterChip(
                                    selected = tag == t,
                                    onClick = { tag = if (tag == t) null else t },
                                    label = { Text("#$t") },
                                )
                            }
                        }
                    }
                }
                if (groups.isEmpty()) {
                    item(key = "none") {
                        Text(
                            text = stringResource(R.string.library_no_match),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                groups.forEach { group ->
                    item(key = "h_" + group.name) {
                        Text(
                            text = stringResource(R.string.library_group, group.name, group.items.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                    }
                    items(group.items, key = { it.lecture.id }) { hit ->
                        val lecture = hit.lecture
                        LectureItem(
                            lecture,
                            snippet = hit.snippet,
                            onOpen = { onOpenLecture(lecture.id) },
                            onDelete = { toDelete = lecture },
                        )
                    }
                }
            }
        }

      }

        // Glass items float over the list (outside glassSource, so the list shows through).
        if (lectures.isNotEmpty()) {
            GlassBox(
                backdrop = glass,
                shape = pill,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = Spacing.l, end = Spacing.l, top = Spacing.s),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.library_search)) },
                    singleLine = true,
                    shape = pill,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.xl),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            GlassBox(
                backdrop = glass,
                shape = pill,
                onClick = onOpenPacks,
                modifier = Modifier.height(56.dp),
            ) {
                Text(
                    text = stringResource(R.string.packs_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            GlassBox(
                backdrop = glass,
                shape = pill,
                onClick = { picker.launch(arrayOf("text/*", "application/pdf", "image/*")) },
                modifier = Modifier.height(56.dp),
            ) {
                Text(
                    text = stringResource(R.string.import_file),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            GlassBox(
                backdrop = glass,
                shape = CircleShape,
                onClick = { showAdd = true },
                modifier = Modifier.size(56.dp),
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }

    if (importing) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.size(Spacing.l))
                    val (done, total) = ocrProgress
                    Text(
                        if (total > 0) stringResource(R.string.ocr_progress, done, total)
                        else stringResource(R.string.import_reading),
                    )
                }
            },
        )
    }

    if (importFailed) {
        AlertDialog(
            onDismissRequest = { vm.dismissImportError() },
            text = { Text(stringResource(R.string.import_failed_file)) },
            confirmButton = {
                TextButton(onClick = { vm.dismissImportError() }) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    if (showAdd) {
        AddLectureDialog(
            onDismiss = { showAdd = false },
            onAdd = { title, subject, content ->
                vm.add(title, subject, content)
                showAdd = false
            },
        )
    }

    toDelete?.let { lecture ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_confirm, lecture.title)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(lecture)
                    toDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun LectureItem(lecture: Lecture, snippet: String?, onOpen: () -> Unit, onDelete: () -> Unit) {
    MedoraCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), spaced = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lecture.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (lecture.subject.isNotBlank()) {
                    Text(text = lecture.subject, style = MaterialTheme.typography.bodyMedium)
                }
                val tags = lecture.tagList()
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.joinToString("  ") { "#$it" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (snippet != null) {
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = DateFormat.getDateInstance().format(Date(lecture.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun AddLectureDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_lecture)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.lecture_title)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(stringResource(R.string.lecture_subject)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= MAX_PASTE_CHARS) content = it },
                    label = { Text(stringResource(R.string.library_paste)) },
                    minLines = 3,
                    maxLines = 8,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title, subject, content.trim()) },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
