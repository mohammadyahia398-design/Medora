package com.example.medora.ui.components

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.example.medora.ui.glass.LiquidSlider
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

// Slider. Android 13+: Kyant liquid slider. Older phones: Material slider.
@Composable
fun MedoraSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val current = rememberUpdatedState(value)
        val surface = MaterialTheme.colorScheme.surface
        val backdrop = rememberCanvasBackdrop { drawRect(surface) }
        LiquidSlider(
            value = { current.value },
            onValueChange = onValueChange,
            valueRange = valueRange,
            visibilityThreshold = 0.01f,
            backdrop = backdrop,
            modifier = modifier,
        )
    } else {
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = modifier)
    }
}
