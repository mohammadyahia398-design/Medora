package com.example.medora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Small white circle with a forward arrow (points left in right-to-left languages).
// Only a visual hint: the card around it is the tap target.
@Composable
fun MedoraArrowButton(modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val ink = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.dp.toPx()
            fun x(f: Float) = if (rtl) w - f else f
            val tip = Offset(x(w * 0.9f), h / 2f)
            drawLine(ink, Offset(x(w * 0.1f), h / 2f), tip, stroke, StrokeCap.Round)
            drawLine(ink, Offset(x(w * 0.55f), h * 0.15f), tip, stroke, StrokeCap.Round)
            drawLine(ink, Offset(x(w * 0.55f), h * 0.85f), tip, stroke, StrokeCap.Round)
        }
    }
}
