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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.Lecture
import com.example.medora.ui.components.MedoraCard
import java.text.DateFormat
import java.util.Date

@Composable
fun LibraryScreen(onOpenLecture: (Long) -> Unit, vm: LibraryViewModel = viewModel()) {
    val lectures by vm.lectures.collectAsState()
    val importFailed by vm.importFailed.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importText(uri)
    }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Lecture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (lectures.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(lectures, key = { it.id }) { lecture ->
                    LectureItem(lecture, onOpen = { onOpenLecture(lecture.id) }, onDelete = { toDelete = lecture })
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExtendedFloatingActionButton(onClick = { picker.launch(arrayOf("text/*")) }) {
                Text(stringResource(R.string.import_txt))
            }
            FloatingActionButton(onClick = { showAdd = true }) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }

    if (importFailed) {
        AlertDialog(
            onDismissRequest = { vm.dismissImportError() },
            text = { Text(stringResource(R.string.import_failed)) },
            confirmButton = {
                TextButton(onClick = { vm.dismissImportError() }) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    if (showAdd) {
        AddLectureDialog(
            onDismiss = { showAdd = false },
            onAdd = { title, subject ->
                vm.add(title, subject)
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
private fun LectureItem(lecture: Lecture, onOpen: () -> Unit, onDelete: () -> Unit) {
    MedoraCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = DateFormat.getDateInstance().format(Date(lecture.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun AddLectureDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_lecture)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title, subject) },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
