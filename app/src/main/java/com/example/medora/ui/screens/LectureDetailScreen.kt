package com.example.medora.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R

// Shows one lecture. Text is split into lines so very long files stay smooth.
@Composable
fun LectureDetailScreen(id: Long, vm: LibraryViewModel = viewModel()) {
    val lecture by remember(id) { vm.lecture(id) }.collectAsState(initial = null)
    val current = lecture ?: return
    val lines = remember(current.content) { current.content.split("\n") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
                if (current.content.isBlank()) {
                    Text(
                        text = stringResource(R.string.no_content),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        if (current.content.isNotBlank()) {
            itemsIndexed(lines) { _, line ->
                Text(text = line, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
