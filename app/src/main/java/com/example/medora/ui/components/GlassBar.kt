package com.example.medora.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.medora.ui.theme.LocalMedoraDark
import com.example.medora.ui.theme.MedoraColors
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
    Row(
        modifier = modifier.fillMaxWidth().height(height).then(glassLook(backdrop, shape)),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

// A backdrop for glass inside one screen. Put glassSource on the content that should
// show through the glass, and keep the glass items outside that content.
@Composable
fun rememberGlassBackdrop(): LayerBackdrop = rememberLayerBackdrop()

fun Modifier.glassSource(backdrop: LayerBackdrop): Modifier = this.layerBackdrop(backdrop)

// Glass container for floating items (buttons, search). Clickable when onClick is set.
@Composable
fun GlassBox(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    onClick: (() -> Unit)? = null,
    // Optional own frost color (and the solid color for phones without blur).
    tint: Color? = null,
    fallback: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val base = modifier.then(glassLook(backdrop, shape, tint, fallback)).clip(shape)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun glassLook(backdrop: LayerBackdrop, shape: Shape, tint: Color? = null, fallback: Color? = null): Modifier {
    val dark = LocalMedoraDark.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(8.dp.toPx())
                lens(12.dp.toPx(), 24.dp.toPx())
            },
            onDrawSurface = {
                drawRect(tint ?: if (dark) MedoraColors.GlassDark else MedoraColors.GlassLight)
            },
        )
    } else {
        Modifier
            .background(fallback ?: if (dark) MedoraColors.GlassSolidDark else MedoraColors.GlassSolidLight, shape)
            .border(1.dp, if (dark) MedoraColors.GlassBorderDark else MedoraColors.GlassBorderLight, shape)
    }
}
