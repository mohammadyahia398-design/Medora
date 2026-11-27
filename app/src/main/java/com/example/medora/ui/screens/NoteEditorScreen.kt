package com.example.medora.ui.screens

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.ui.components.MedoraButton

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

// Simple note editor: title, text, and three format buttons. Plain text inside.
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
