package com.example.medora.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.NoteImages
import com.example.medora.ui.components.MedoraButton
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

// Simple note editor: title, text, format buttons, table and image. Plain text inside.
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    Column(
        modifier = Modifier.fillMaxSize().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.lecture_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MedoraButton(
                text = stringResource(R.string.note_heading),
                onClick = { body = togglePrefix(body, "# ") },
                modifier = Modifier.weight(1f),
            )
            MedoraButton(
                text = stringResource(R.string.note_bullet),
                onClick = { body = togglePrefix(body, "- ") },
                modifier = Modifier.weight(1f),
            )
            MedoraButton(
                text = stringResource(R.string.note_numbered),
                onClick = { body = togglePrefix(body, "1. ") },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MedoraButton(
                text = stringResource(R.string.note_table),
                onClick = { body = insertTable(body) },
                modifier = Modifier.weight(1f),
            )
            MedoraButton(
                text = stringResource(R.string.note_image),
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        MedoraButton(
            text = stringResource(R.string.save),
            onClick = {
                if (title.isNotBlank()) {
                    vm.saveNote(id, title, body.text)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
