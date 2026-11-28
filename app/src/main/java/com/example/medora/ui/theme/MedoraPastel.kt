package com.example.medora.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Soft fills used only to tell cards apart (subjects, small stat cards).
// The main color stays MEDORA blue. Text on a pastel uses its own content color.
object MedoraPastel {
    // strong = a deeper shade of the same pastel, for filled parts (bars, goal fill).
    class Tone(val container: Color, val content: Color, val strong: Color)

    private val light = listOf(
        Tone(Color(0xFFD6F5E3), MedoraColors.Ink, Color(0xFF8FE3A8)), // mint
        Tone(Color(0xFFE6E0FA), MedoraColors.Ink, Color(0xFFBBAAF2)), // lilac
        Tone(Color(0xFFFFF0C2), MedoraColors.Ink, Color(0xFFFFD36B)), // butter
        Tone(Color(0xFFD9F4FA), MedoraColors.Ink, Color(0xFF90E0EF)), // sky
    )
    private val dark = listOf(
        Tone(Color(0xFF123B2B), Color(0xFFEAF6FA), Color(0xFF2F8F62)),
        Tone(Color(0xFF2A2350), Color(0xFFEAF6FA), Color(0xFF5C4CB8)),
        Tone(Color(0xFF4A3B10), Color(0xFFEAF6FA), Color(0xFF9C7B1F)),
        Tone(Color(0xFF0F3140), Color(0xFFEAF6FA), Color(0xFF1F7391)),
    )

    // Same index gives the same tone, so a card keeps its color.
    @Composable
    fun tone(index: Int): Tone {
        val list = if (LocalMedoraDark.current) dark else light
        return list[Math.floorMod(index, list.size)]
    }
}
