package com.example.medora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.medora.ui.theme.MedoraColors
import com.example.medora.ui.theme.MedoraTheme
import com.kyant.backdrop.backdrops.LayerBackdrop

// Full-screen dark scene for the study screens (cards, quiz). A navy gradient with soft
// color glows: this is what the frosted glass cards blur and show through.
// Always dark, whatever the app theme. The content gets the backdrop to pass to GlassBox.
@Composable
fun MedoraImmersive(content: @Composable BoxScope.(LayerBackdrop) -> Unit) {
    val backdrop = rememberGlassBackdrop()
    MedoraTheme(darkTheme = true) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().glassSource(backdrop)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Brush.verticalGradient(listOf(Color(0xFF070A24), Color(0xFF0A0F3D))))
                        fun glow(color: Color, alpha: Float, x: Float, y: Float, r: Float) {
                            val c = Offset(size.width * x, size.height * y)
                            drawCircle(
                                brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), c, size.width * r),
                                radius = size.width * r,
                                center = c,
                            )
                        }
                        glow(MedoraColors.Blue, 0.60f, 0.15f, 0.28f, 0.75f)
                        glow(MedoraColors.Sky, 0.30f, 0.95f, 0.55f, 0.60f)
                        glow(MedoraColors.Teal, 0.28f, 0.30f, 0.95f, 0.70f)
                    }
                }
                content(backdrop)
            }
        }
    }
}

// Colors shared by the glass pieces on the dark scene.
object ImmersiveGlass {
    val frost = Color.White.copy(alpha = 0.14f)
    val frostSolid = Color(0xFF1B2250).copy(alpha = 0.94f)
    val line = Color.White.copy(alpha = 0.22f)
}
