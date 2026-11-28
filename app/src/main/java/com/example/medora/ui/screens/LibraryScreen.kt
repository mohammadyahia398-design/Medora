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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.medora.ui.components.MedoraCard
import com.example.medora.ui.components.MedoraArrowButton
import com.example.medora.ui.components.MedoraScreenTitle
import com.example.medora.ui.components.MedoraTextTabs
import com.example.medora.ui.icons.MedoraIcons
import com.example.medora.ui.icons.themed
import com.example.medora.ui.theme.MedoraPastel
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
    var tab by remember { mutableStateOf(0) }
    var openGroup by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val allTags = remember(lectures) { lectures.flatMap { it.tagList() }.distinct().sortedBy { it.lowercase() } }
    val groups = remember(lectures, query, noSubject, tag) { groupLectures(lectures, query.trim(), noSubject, tag) }
    val shown = remember(groups, openGroup) { if (openGroup == null) groups else groups.filter { it.name == openGroup } }

    Box(modifier = Modifier.fillMaxSize()) {
        if (lectures.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(Spacing.xxl),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = Spacing.xs, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                item(key = "title") { MedoraScreenTitle(stringResource(R.string.tab_library)) }
                item(key = "tabs") {
                    MedoraTextTabs(
                        titles = listOf(
                            stringResource(R.string.library_tab_subjects),
                            stringResource(R.string.library_tab_lectures),
                        ),
                        selected = tab,
                        onSelect = {
                            tab = it
                            openGroup = null
                        },
                    )
                }
                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.library_search)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
                } else if (tab == 0) {
                    itemsIndexed(groups, key = { _, g -> "s_" + g.name }) { index, group ->
                        SubjectCard(
                            group = group,
                            toneIndex = index,
                            onClick = {
                                openGroup = group.name
                                tab = 1
                            },
                        )
                    }
                } else {
                    if (openGroup != null) {
                        item(key = "open_group") {
                            FilterChip(
                                selected = true,
                                onClick = { openGroup = null },
                                label = { Text(openGroup.orEmpty() + "  ×") },
                            )
                        }
                    }
                    shown.forEach { group ->
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

        // One round + button. It opens a short menu: add, import, study packs.
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.l)) {
            val addLabel = stringResource(R.string.add_lecture)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(role = Role.Button) { menuOpen = true }
                    .semantics { contentDescription = addLabel },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = MedoraIcons.Plus.themed(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_lecture)) },
                    onClick = {
                        menuOpen = false
                        showAdd = true
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.import_file)) },
                    onClick = {
                        menuOpen = false
                        picker.launch(arrayOf("text/*", "application/pdf", "image/*"))
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.packs_title)) },
                    onClick = {
                        menuOpen = false
                        onOpenPacks()
                    },
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

// One pastel card per subject, like the reference: icon in a white circle, a count pill,
// the name, and an arrow. The whole card opens the subject.
@Composable
private fun SubjectCard(group: LectureGroup, toneIndex: Int, onClick: () -> Unit) {
    val tone = MedoraPastel.tone(toneIndex)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(tone.container)
            .clickable(onClick = onClick)
            .padding(Spacing.card),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Image(imageVector = MedoraIcons.Library.themed(), contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.library_lecture_count, group.items.size),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.m, vertical = Spacing.xs + 2.dp),
            )
        }
        Spacer(Modifier.height(Spacing.l))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tone.content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.m))
            MedoraArrowButton()
        }
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
