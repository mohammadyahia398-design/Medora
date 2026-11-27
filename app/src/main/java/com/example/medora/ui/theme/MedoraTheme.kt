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
    // Soft flat fills for icons and badges. Details on them are drawn in Ink.
    val Teal = Color(0xFF7ADCCB)
    val TealDeep = Color(0xFF16857A)
    val Orange = Color(0xFFFFB84D)
    val Green = Color(0xFF8FE3A8)
    val Coral = Color(0xFFFF9A8F)
    val Ink = Color(0xFF14201F)
    val Background = Color(0xFFF3F7F6)
    val Surface = Color(0xFFFFFFFF)

    // Glass tokens (used by GlassBar.kt).
    val GlassLight = Color.White.copy(alpha = 0.35f)
    val GlassDark = Color(0xFF0F1514).copy(alpha = 0.45f)
    val GlassSolidLight = Color.White.copy(alpha = 0.96f)
    val GlassSolidDark = Color(0xFF18211F).copy(alpha = 0.96f)
    val GlassBorderLight = Color(0x1A14201F)
    val GlassBorderDark = Color(0x33FFFFFF)
}

// True when the dark theme is on. Read it where a color is fixed in code.
val LocalMedoraDark = compositionLocalOf { false }

// Every role is set on purpose. A role left out falls back to Material's
// baseline purple, which is where the stray purple used to come from.
private val Scheme = lightColorScheme(
    primary = MedoraColors.TealDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F3EE),
    onPrimaryContainer = Color(0xFF0B3B36),
    secondary = Color(0xFF4A7C77),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F0ED),
    onSecondaryContainer = MedoraColors.Ink,
    tertiary = Color(0xFF8F5B00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE3B8),
    onTertiaryContainer = Color(0xFF3A2400),
    background = MedoraColors.Background,
    onBackground = MedoraColors.Ink,
    surface = MedoraColors.Surface,
    onSurface = MedoraColors.Ink,
    surfaceVariant = Color(0xFFE6EFED),
    onSurfaceVariant = Color(0xFF47605C),
    surfaceTint = MedoraColors.TealDeep,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FAF9),
    surfaceContainer = Color(0xFFEEF4F2),
    surfaceContainerHigh = Color(0xFFE8F0EE),
    surfaceContainerHighest = Color(0xFFE1EBE9),
    inverseSurface = MedoraColors.Ink,
    inverseOnSurface = Color(0xFFF3F7F6),
    inversePrimary = MedoraColors.Teal,
    outline = Color(0xFFC3D3D0),
    outlineVariant = Color(0xFFDDE8E6),
)

private val DarkScheme = darkColorScheme(
    primary = MedoraColors.Teal,
    onPrimary = MedoraColors.Ink,
    primaryContainer = Color(0xFF1F4A44),
    onPrimaryContainer = Color(0xFFD3F3EE),
    secondary = Color(0xFF9FC4BE),
    onSecondary = Color(0xFF0F2623),
    secondaryContainer = Color(0xFF2B4440),
    onSecondaryContainer = Color(0xFFD3F3EE),
    tertiary = MedoraColors.Orange,
    onTertiary = Color(0xFF3A2400),
    tertiaryContainer = Color(0xFF5A3B00),
    onTertiaryContainer = Color(0xFFFFE3B8),
    background = Color(0xFF0F1514),
    onBackground = Color(0xFFE8F1EF),
    surface = Color(0xFF18211F),
    onSurface = Color(0xFFE8F1EF),
    surfaceVariant = Color(0xFF25322F),
    onSurfaceVariant = Color(0xFFB9CBC7),
    surfaceTint = MedoraColors.Teal,
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceContainerLow = Color(0xFF141C1A),
    surfaceContainer = Color(0xFF18211F),
    surfaceContainerHigh = Color(0xFF1E2926),
    surfaceContainerHighest = Color(0xFF25322F),
    inverseSurface = Color(0xFFE8F1EF),
    inverseOnSurface = MedoraColors.Ink,
    inversePrimary = MedoraColors.TealDeep,
    outline = Color(0xFF41524F),
    outlineVariant = Color(0xFF2E3C39),
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
            typography = MedoraTypography,
            content = content,
        )
    }
}
