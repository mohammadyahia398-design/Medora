package com.example.medora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.medora.ui.theme.Spacing

// Our own card. Wraps Material inside, so we can swap the engine later.
// Children get an even gap. Pass spaced = false when the content sets its own gaps.
@Composable
fun MedoraCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    spaced: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.surface else containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.card),
            verticalArrangement = if (spaced) Arrangement.spacedBy(Spacing.s) else Arrangement.Top,
            content = content,
        )
    }
}
