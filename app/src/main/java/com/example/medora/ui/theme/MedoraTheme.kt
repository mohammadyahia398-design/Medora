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
import androidx.compose.ui.unit.dp

// Single place for the whole look. Change here, the app changes everywhere.
// Palette: White, Sky, Blue, Navy. Light theme is mostly white, dark theme mostly black.
object MedoraColors {
    val White = Color(0xFFFFFFFF)
    val Sky = Color(0xFF90E0EF)
    val Blue = Color(0xFF0077B6)
    val Navy = Color(0xFF03045E)
    // Blue mixed with white.
    val BlueSoft = Color(0xFF73B4D7)

    // Soft fills for the icons. Details on them are drawn in Ink.
    val Teal = Color(0xFF7ADCCB)

    // Meaning colors. Keep them: right answer, wrong answer, AI marker.
    val Orange = Color(0xFFFFB84D)
    val Green = Color(0xFF8FE3A8)
    val GreenDeep = Color(0xFF1B7A3D)
    val Coral = Color(0xFFFF9A8F)

    // Details on soft fills (and icon outlines) are drawn in Ink.
    val Ink = Navy

    // Glass tokens (used by GlassBar.kt).
    val GlassLight = Color.White.copy(alpha = 0.35f)
    val GlassDark = Color(0xFF05061A).copy(alpha = 0.5f)
    val GlassSolidLight = Color.White.copy(alpha = 0.96f)
    val GlassSolidDark = Color(0xFF0A0B1C).copy(alpha = 0.96f)
    val GlassBorderLight = Color(0x1A03045E)
    val GlassBorderDark = Color(0x33FFFFFF)
}

// True when the dark theme is on. Read it where a color is fixed in code.
val LocalMedoraDark = compositionLocalOf { false }

// Every role is set on purpose. A role left out falls back to Material's
// baseline purple, which is where the stray purple used to come from.
private val Scheme = lightColorScheme(
    primary = MedoraColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F4FA),
    onPrimaryContainer = MedoraColors.Navy,
    secondary = MedoraColors.Navy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F1F8),
    onSecondaryContainer = MedoraColors.Navy,
    tertiary = Color(0xFF8F5B00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE3B8),
    onTertiaryContainer = Color(0xFF3A2400),
    background = MedoraColors.White,
    onBackground = MedoraColors.Ink,
    surface = Color(0xFFF2FAFD),
    onSurface = MedoraColors.Ink,
    surfaceVariant = Color(0xFFE4F3F8),
    onSurfaceVariant = Color(0xFF3F4F73),
    surfaceTint = MedoraColors.Blue,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FCFE),
    surfaceContainer = Color(0xFFEFF8FC),
    surfaceContainerHigh = Color(0xFFE8F4F9),
    surfaceContainerHighest = Color(0xFFE1EFF6),
    inverseSurface = MedoraColors.Navy,
    inverseOnSurface = Color.White,
    inversePrimary = MedoraColors.Sky,
    outline = Color(0xFFB4CFDD),
    outlineVariant = Color(0xFFD7E8F0),
)

private val DarkScheme = darkColorScheme(
    primary = MedoraColors.Sky,
    onPrimary = MedoraColors.Navy,
    primaryContainer = MedoraColors.Navy,
    onPrimaryContainer = MedoraColors.Sky,
    secondary = MedoraColors.BlueSoft,
    onSecondary = MedoraColors.Navy,
    secondaryContainer = Color(0xFF0A1B45),
    onSecondaryContainer = MedoraColors.Sky,
    tertiary = MedoraColors.Orange,
    onTertiary = Color(0xFF3A2400),
    tertiaryContainer = Color(0xFF5A3B00),
    onTertiaryContainer = Color(0xFFFFE3B8),
    background = Color.Black,
    onBackground = Color(0xFFEAF6FA),
    surface = Color(0xFF0A0B1C),
    onSurface = Color(0xFFEAF6FA),
    surfaceVariant = Color(0xFF161933),
    onSurfaceVariant = Color(0xFFB4C6D6),
    surfaceTint = MedoraColors.Sky,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF07081A),
    surfaceContainer = Color(0xFF0D0E26),
    surfaceContainerHigh = Color(0xFF13153A),
    surfaceContainerHighest = Color(0xFF1A1D45),
    inverseSurface = Color(0xFFEAF6FA),
    inverseOnSurface = MedoraColors.Ink,
    inversePrimary = MedoraColors.Blue,
    outline = Color(0xFF3A4A78),
    outlineVariant = Color(0xFF1F2850),
)

private val MedoraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun MedoraTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMedoraDark provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else Scheme,
            shapes = MedoraShapes,
            typography = MedoraTypography,
            content = content,
        )
    }
}
