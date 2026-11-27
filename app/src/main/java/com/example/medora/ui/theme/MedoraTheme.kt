package com.example.medora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp

// Single place for the whole look. Change here, the app changes everywhere.
object MedoraColors {
    val Ink = Color(0xFF2B1650)
    val Purple = Color(0xFFC9A0F0)
    val Orange = Color(0xFFFFB255)
    val Green = Color(0xFF8EF0B4)
    val Coral = Color(0xFFFF8A80)
    val Background = Color(0xFFFBFAFD)
    val Surface = Color(0xFFFFFFFF)
}

// True when the dark theme is on. Read it where a color is fixed in code.
val LocalMedoraDark = compositionLocalOf { false }

private val Scheme = lightColorScheme(
    primary = MedoraColors.Ink,
    onPrimary = Color.White,
    secondary = MedoraColors.Purple,
    background = MedoraColors.Background,
    onBackground = MedoraColors.Ink,
    surface = MedoraColors.Surface,
    onSurface = MedoraColors.Ink,
)

private val DarkScheme = darkColorScheme(
    primary = MedoraColors.Purple,
    onPrimary = MedoraColors.Ink,
    secondary = MedoraColors.Purple,
    background = Color(0xFF15101F),
    onBackground = Color(0xFFF1E9FB),
    surface = Color(0xFF211A30),
    onSurface = Color(0xFFF1E9FB),
    surfaceVariant = Color(0xFF352B49),
    onSurfaceVariant = Color(0xFFD9CEEA),
)

private val MedoraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

// Our icons are drawn in dark ink. In the dark theme they are tinted light.
@Composable
fun iconTint(): ColorFilter? =
    if (LocalMedoraDark.current) ColorFilter.tint(DarkScheme.onBackground) else null

@Composable
fun MedoraTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMedoraDark provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else Scheme,
            shapes = MedoraShapes,
            content = content,
        )
    }
}
