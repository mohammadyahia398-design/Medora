package com.example.medora.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.NoteImages
import com.example.medora.data.Sticker
import com.example.medora.data.StickerLibrary
import com.example.medora.data.Stickers
import com.example.medora.ui.components.NotePreview
import com.example.medora.ui.components.StickerBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TABLE_TEMPLATE = "| A | B |\n|---|---|\n|  |  |\n"

// Puts an empty table at the cursor, on its own lines.
private fun insertTable(v: TextFieldValue): TextFieldValue = insertBlock(v, TABLE_TEMPLATE)

// Puts a block (table or image line) at the cursor, on its own lines.
private fun insertBlock(v: TextFieldValue, blockText: String): TextFieldValue {
    val text = v.text
    val cursor = v.selection.max.coerceIn(0, text.length)
    val lead = if (cursor == 0 || text[cursor - 1] == '\n') "" else "\n"
    val block = lead + blockText
    return TextFieldValue(
        text.substring(0, cursor) + block + text.substring(cursor),
        TextRange(cursor + block.length),
    )
}

// Wraps the selected text with a mark like ** or ==. No selection: puts the two marks
// with the cursor between them.
private fun wrapSelection(v: TextFieldValue, mark: String): TextFieldValue {
    val text = v.text
    val a = v.selection.min.coerceIn(0, text.length)
    val b = v.selection.max.coerceIn(0, text.length)
    val inner = text.substring(a, b)
    val out = text.substring(0, a) + mark + inner + mark + text.substring(b)
    return TextFieldValue(
        out,
        if (a == b) TextRange(a + mark.length) else TextRange(a + mark.length, b + mark.length),
    )
}

// Adds (or removes) a prefix such as "# " or "- " at the start of the cursor's line.
private fun togglePrefix(v: TextFieldValue, prefix: String): TextFieldValue {
    val text = v.text
    val cursor = v.selection.min.coerceIn(0, text.length)
    val start = text.lastIndexOf('\n', cursor - 1) + 1
    return if (text.startsWith(prefix, start)) {
        TextFieldValue(
            text.removeRange(start, start + prefix.length),
            TextRange((cursor - prefix.length).coerceAtLeast(start)),
        )
    } else {
        TextFieldValue(
            text.substring(0, start) + prefix + text.substring(start),
            TextRange(cursor + prefix.length),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
// Note editor, soft style: big borderless title, writing area, and a floating capsule of tools.
// The capsule opens a second capsule for text format and a rounded panel for stickers.
// Stickers sit in a layer over the writing area: one finger moves, two fingers resize and rotate.
@Composable
fun NoteEditorScreen(
    id: Long,
    onDone: () -> Unit,
    vm: LibraryViewModel = viewModel(),
) {
    val lecture by remember(id) { vm.lecture(id) }.collectAsState(initial = null)
    val loaded = lecture ?: return
    var title by remember(loaded.id) { mutableStateOf(loaded.title) }
    var body by remember(loaded.id) { mutableStateOf(TextFieldValue(loaded.content)) }
    val stickers = remember(loaded.id) { mutableStateListOf<Sticker>().apply { addAll(Stickers.parse(loaded.stickersJson)) } }
    var preview by remember(loaded.id) { mutableStateOf(false) }
    var showFormat by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }
    var library by remember { mutableStateOf(emptyList<String>()) }
    var libToDelete by remember { mutableStateOf<String?>(null) }
    var toRemove by remember { mutableStateOf<Sticker?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun addSticker(image: String, emoji: String) {
        val jitter = ((stickers.size % 5) - 2) * 0.06f
        stickers.add(Sticker(id = System.nanoTime(), image = image, emoji = emoji, x = 0.5f + jitter, y = 0.35f + jitter))
        showStickers = false
        showLibrary = false
    }

    // System photo picker: no storage permission needed.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val line = withContext(Dispatchers.IO) { NoteImages.save(context, uri) }
                if (line != null) {
                    body = insertBlock(body, line + "\n")
                } else {
                    Toast.makeText(context, R.string.note_image_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    // "My stickers": pick images once; each is saved with a white edge.
    val libraryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val failed = withContext(Dispatchers.IO) { uris.count { !StickerLibrary.add(context, it) } }
                library = withContext(Dispatchers.IO) { StickerLibrary.list(context) }
                if (failed > 0) Toast.makeText(context, R.string.note_image_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(showLibrary) {
        if (showLibrary) library = withContext(Dispatchers.IO) { StickerLibrary.list(context) }
    }
    val imageRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    val ink = MaterialTheme.colorScheme.onBackground

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = ink, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.lecture_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = ink.copy(alpha = 0.35f),
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(role = Role.Button) {
                            if (title.isNotBlank()) {
                                vm.saveNote(id, title, body.text, Stickers.toJson(stickers.toList()))
                                onDone()
                            }
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (preview) {
                    NotePreview(
                        text = body.text,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp),
                    )
                } else {
                    BasicTextField(
                        value = body,
                        onValueChange = { body = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxSize().padding(bottom = 110.dp),
                        decorationBox = { inner ->
                            Box {
                                if (body.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.b7_body_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = ink.copy(alpha = 0.35f),
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
                StickerBoard(
                    stickers = stickers,
                    modifier = Modifier.fillMaxSize(),
                    onChange = { n ->
                        val i = stickers.indexOfFirst { it.id == n.id }
                        if (i >= 0) stickers[i] = n
                    },
                    onLongPress = { toRemove = it },
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showStickers) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(Stickers.BUILT_IN, key = { it }) { e ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) { addSticker("", e) },
                                contentAlignment = Alignment.Center,
                            ) { Text(text = e, fontSize = 28.sp) }
                        }
                    }
                }
            }
            if (showLibrary) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item(key = "add") {
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable(role = Role.Button) {
                                        libraryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.b8_library_add),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                        if (library.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    text = stringResource(R.string.b8_library_empty),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.widthIn(max = 220.dp),
                                )
                            }
                        }
                        items(library, key = { it }) { path ->
                            LibraryThumb(
                                path = path,
                                onClick = {
                                    scope.launch {
                                        val copy = withContext(Dispatchers.IO) { StickerLibrary.useCopy(context, path) }
                                        if (copy != null) addSticker(copy, "")
                                    }
                                },
                                onLongClick = { libToDelete = path },
                            )
                        }
                    }
                }
            }
            if (showFormat) {
                CapsuleRow {
                    CapsuleItem(stringResource(R.string.note_heading)) { body = togglePrefix(body, "# ") }
                    CapsuleItem(stringResource(R.string.note_bullet)) { body = togglePrefix(body, "- ") }
                    CapsuleItem(stringResource(R.string.note_numbered)) { body = togglePrefix(body, "1. ") }
                    CapsuleItem(stringResource(R.string.note_bold)) { body = wrapSelection(body, "**") }
                    CapsuleItem(stringResource(R.string.note_highlight)) { body = wrapSelection(body, "==") }
                }
            }
            CapsuleRow {
                CapsuleItem(stringResource(R.string.b7_format), active = showFormat) { showFormat = !showFormat }
                CapsuleItem(stringResource(R.string.note_table)) { body = insertTable(body) }
                CapsuleItem(stringResource(R.string.note_image)) { imagePicker.launch(imageRequest) }
                CapsuleItem(stringResource(R.string.b7_sticker), active = showStickers) {
                    showStickers = !showStickers
                    if (showStickers) showLibrary = false
                }
                CapsuleItem(stringResource(R.string.b8_library), active = showLibrary) {
                    showLibrary = !showLibrary
                    if (showLibrary) showStickers = false
                }
                CapsuleItem(
                    stringResource(if (preview) R.string.note_edit else R.string.note_preview),
                    active = preview,
                ) { preview = !preview }
            }
        }
    }

    libToDelete?.let { path ->
        AlertDialog(
            onDismissRequest = { libToDelete = null },
            title = { Text(stringResource(R.string.b8_library_delete_title)) },
            confirmButton = {
                TextButton(onClick = {
                    libToDelete = null
                    scope.launch {
                        withContext(Dispatchers.IO) { StickerLibrary.delete(context, path) }
                        library = withContext(Dispatchers.IO) { StickerLibrary.list(context) }
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { libToDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    toRemove?.let { target ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            title = { Text(stringResource(R.string.b7_sticker_delete_title)) },
            confirmButton = {
                TextButton(onClick = {
                    stickers.removeAll { it.id == target.id }
                    toRemove = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toRemove = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

// Dark floating capsule. Wide content scrolls sideways inside it.
@Composable
private fun CapsuleRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.94f))
            .horizontalScroll(rememberScrollState())
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun CapsuleItem(text: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.inverseOnSurface else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            color = if (active) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}

// One saved sticker in the library panel. Tap puts it on the note, long press removes it.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryThumb(path: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { NoteImages.load(context, path, 256) }
    }
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(2.dp),
            )
        }
    }
}
