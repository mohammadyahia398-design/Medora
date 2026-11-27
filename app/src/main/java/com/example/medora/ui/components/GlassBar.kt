package com.example.medora.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.medora.ui.theme.LocalMedoraDark
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

// Glass bar. Android 12+ gets blur, Android 13+ also gets refraction.
// Older phones get a solid look instead.
@Composable
fun GlassBar(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(32.dp)
    val dark = LocalMedoraDark.current
    val look = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(8.dp.toPx())
                lens(12.dp.toPx(), 24.dp.toPx())
            },
            onDrawSurface = {
                drawRect(if (dark) Color(0xFF15101F).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.35f))
            },
        )
    } else {
        Modifier
            .background(if (dark) Color(0xFF211A30).copy(alpha = 0.96f) else Color.White.copy(alpha = 0.96f), shape)
            .border(1.dp, if (dark) Color(0x33FFFFFF) else Color(0x1A2B1650), shape)
    }
    Row(
        modifier = modifier.fillMaxWidth().height(height).then(look),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
