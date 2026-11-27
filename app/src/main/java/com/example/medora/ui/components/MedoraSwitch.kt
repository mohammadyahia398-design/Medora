package com.example.medora.ui.components

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.example.medora.ui.glass.LiquidToggle
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

// On/off switch. Android 13+: Kyant liquid toggle. Older phones: Material switch.
@Composable
fun MedoraSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val current = rememberUpdatedState(checked)
        val surface = MaterialTheme.colorScheme.surface
        val backdrop = rememberCanvasBackdrop { drawRect(surface) }
        LiquidToggle(
            selected = { current.value },
            onSelect = { if (it != current.value) onCheckedChange(it) },
            backdrop = backdrop,
            modifier = modifier,
        )
    } else {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    }
}
