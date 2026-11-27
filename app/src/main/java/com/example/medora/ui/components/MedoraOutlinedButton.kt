package com.example.medora.ui.components

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.medora.ui.glass.LiquidButton
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.shapes.Capsule

// Secondary button. Same height as MedoraButton. Android 13+: clear liquid glass.
@Composable
fun MedoraOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val surface = MaterialTheme.colorScheme.surface
        val backdrop = rememberCanvasBackdrop { drawRect(surface) }
        LiquidButton(
            onClick = onClick,
            backdrop = backdrop,
            modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, Capsule()),
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text)
        }
    }
}
