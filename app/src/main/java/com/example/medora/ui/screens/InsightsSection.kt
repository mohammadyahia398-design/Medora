package com.example.medora.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.medora.R
import com.example.medora.domain.Insights
import com.example.medora.ui.components.AiBadge
import com.example.medora.ui.components.MedoraCard
import com.example.medora.ui.components.NoteTableRow
import com.example.medora.ui.theme.Spacing

// AI terms, comparison tables and classifications of one lecture (always marked as AI).
@Composable
fun InsightsSection(insights: Insights) {
    if (insights.isEmpty) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        if (insights.terms.isNotEmpty()) {
            MedoraCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.insights_terms))
                insights.terms.forEach { t ->
                    Text(t.term, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(t.definition, style = MaterialTheme.typography.bodyMedium)
                    if (t.source.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.insights_source, t.source),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (insights.tables.isNotEmpty()) {
            MedoraCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.insights_tables))
                insights.tables.forEach { table ->
                    if (table.title.isNotBlank()) {
                        Text(table.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        NoteTableRow(table.headers, header = true)
                        table.rows.forEach { NoteTableRow(it, header = false) }
                    }
                    Spacer(Modifier.height(Spacing.s))
                }
            }
        }
        if (insights.classifications.isNotEmpty()) {
            MedoraCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.insights_classes))
                insights.classifications.forEach { c ->
                    if (c.title.isNotBlank()) {
                        Text(c.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    c.groups.forEach { g ->
                        Text("• " + g.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        if (g.items.isNotEmpty()) {
                            Text("    " + g.items.joinToString("، "), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(Spacing.s))
                }
            }
        }
        Text(
            text = stringResource(R.string.ai_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    AiBadge()
}

// Edit subject, folder and tags of a lecture.
@Composable
fun OrganizeDialog(
    subject: String,
    folder: String,
    tags: String,
    onDismiss: () -> Unit,
    onSave: (subject: String, folder: String, tags: String) -> Unit,
) {
    var s by remember { mutableStateOf(subject) }
    var f by remember { mutableStateOf(folder) }
    var t by remember { mutableStateOf(tags) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.organize)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                OutlinedTextField(
                    value = s,
                    onValueChange = { s = it },
                    label = { Text(stringResource(R.string.organize_subject)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = f,
                    onValueChange = { f = it },
                    label = { Text(stringResource(R.string.organize_folder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = t,
                    onValueChange = { t = it },
                    label = { Text(stringResource(R.string.organize_tags)) },
                    supportingText = { Text(stringResource(R.string.organize_tags_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(s, f, t) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
