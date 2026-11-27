package com.example.medora.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.medora.data.NoteImages
import com.example.medora.ui.theme.LocalMedoraDark
import com.example.medora.ui.theme.MedoraColors
import com.example.medora.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Shared pieces for showing a note: styled text, tables and images.
// Note marks: **bold**  ==highlight==  # heading  - bullet  | table |  ![image](...)

private val MARK = Regex("""\*\*(.+?)\*\*|==(.+?)==""")

@Composable
fun highlightColor(): Color =
    if (LocalMedoraDark.current) MedoraColors.Orange.copy(alpha = 0.45f) else MedoraColors.Orange.copy(alpha = 0.6f)

fun noteStyled(text: String, highlight: Color): AnnotatedString = buildAnnotatedString {
    var last = 0
    MARK.findAll(text).forEach { m ->
        append(text.substring(last, m.range.first))
        val bold = m.groupValues[1]
        if (bold.isNotEmpty()) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
        } else {
            withStyle(SpanStyle(background = highlight)) { append(m.groupValues[2]) }
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

// The text without ** and == marks (for cards, PDF and PowerPoint).
fun plainNote(text: String): String = MARK.replace(text) { it.groupValues[1].ifEmpty { it.groupValues[2] } }

fun parseCells(line: String): List<String> =
    line.trim().trim('|').split('|').map { it.trim() }

fun isTableSeparator(line: String): Boolean =
    parseCells(line).all { c -> c.isNotEmpty() && c.all { it == '-' || it == ':' } }

@Composable
fun NoteTableRow(cells: List<String>, header: Boolean) {
    val hl = highlightColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (header) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
    ) {
        cells.forEach { cell ->
            Text(
                text = noteStyled(cell, hl),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (header) FontWeight.Bold else null,
                color = if (header) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    .padding(Spacing.s),
            )
        }
    }
}

// One image from the note, loaded off the main thread.
@Composable
fun NoteImage(path: String) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { NoteImages.load(context, path, 1200) }
    }
    val b = bitmap
    if (b != null) {
        Image(
            bitmap = b.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.s)
                .clip(MaterialTheme.shapes.medium),
        )
    } else {
        Spacer(Modifier.height(Spacing.s))
    }
}

// Whole note shown as it will look (used by the editor preview).
@Composable
fun NotePreview(text: String, modifier: Modifier = Modifier) {
    val hl = highlightColor()
    val lines = text.split("\n")
    Column(modifier) {
        lines.forEachIndexed { i, line ->
            val row = line.trim()
            val img = NoteImages.pathOf(line)
            when {
                img != null -> NoteImage(img)
                row.startsWith("|") -> if (!isTableSeparator(row)) {
                    val next = lines.getOrNull(i + 1)?.trim().orEmpty()
                    NoteTableRow(parseCells(row), header = next.startsWith("|") && isTableSeparator(next))
                }
                row.startsWith("# ") -> Text(
                    noteStyled(row.removePrefix("# "), hl),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Spacing.m, bottom = Spacing.xs),
                )
                else -> Text(
                    noteStyled(if (row.startsWith("- ")) "• " + row.removePrefix("- ") else line, hl),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = Spacing.xs),
                )
            }
        }
    }
}
