package com.example.medora.ui.components

import android.os.Build
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.medora.ui.glass.LiquidButton
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

// Our own button. Android 13+: Kyant liquid button (stretches with the finger).
// Older phones: the normal Material button.
@Composable
fun MedoraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val primary = MaterialTheme.colorScheme.primary
        val light = lerp(primary, Color.White, 0.25f)
        // The glass refracts this gradient, so the edges look like liquid.
        val backdrop = rememberCanvasBackdrop {
            drawRect(Brush.horizontalGradient(listOf(primary, light, primary)))
        }
        LiquidButton(onClick = onClick, backdrop = backdrop, modifier = modifier) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text)
        }
    }
}
