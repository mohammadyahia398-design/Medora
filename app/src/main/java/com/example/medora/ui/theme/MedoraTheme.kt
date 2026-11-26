package com.example.medora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

private val Scheme = lightColorScheme(
    primary = MedoraColors.Ink,
    onPrimary = Color.White,
    secondary = MedoraColors.Purple,
    background = MedoraColors.Background,
    onBackground = MedoraColors.Ink,
    surface = MedoraColors.Surface,
    onSurface = MedoraColors.Ink,
)

private val MedoraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun MedoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        shapes = MedoraShapes,
        content = content,
    )
}
