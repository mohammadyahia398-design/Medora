package com.example.medora.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.medora.ui.theme.LocalMedoraDark

// Medora icons, drawn in code. One style for all: two tones, no outlines.
// ink  = the main mark (near-black in light theme, white in dark theme)
// soft = the quiet body behind it (light gray in light theme, dark gray in dark theme)
// Every corner is rounded, every line has round caps, so all edges are soft.
// Grid: 24 x 24.

private class Pal(val ink: Color, val soft: Color)

private val LightPal = Pal(ink = Color(0xFF111114), soft = Color(0xFFC6C6CC))
private val DarkPal = Pal(ink = Color(0xFFFFFFFF), soft = Color(0xFF55555B))

private fun icon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> Unit,
): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).apply(block).build()

// A filled shape. The thin same-color stroke with round joins softens every sharp corner.
private fun ImageVector.Builder.fill(
    color: Color,
    soften: Float = 0.8f,
    block: PathBuilder.() -> Unit,
) {
    path(
        fill = SolidColor(color),
        stroke = SolidColor(color),
        strokeLineWidth = soften,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}

// A line with round ends and round bends.
private fun ImageVector.Builder.line(
    color: Color,
    width: Float,
    block: PathBuilder.() -> Unit,
) {
    path(
        stroke = SolidColor(color),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}

// A rounded rectangle with a gentle, continuous corner (softer than a plain circle arc).
private fun PathBuilder.soft(x: Float, y: Float, w: Float, h: Float, r: Float) {
    val s = minOf(r * 1.3f, minOf(w, h) / 2f)
    val f = 0.3f * s
    moveTo(x + s, y)
    lineTo(x + w - s, y)
    curveTo(x + w - f, y, x + w, y + f, x + w, y + s)
    lineTo(x + w, y + h - s)
    curveTo(x + w, y + h - f, x + w - f, y + h, x + w - s, y + h)
    lineTo(x + s, y + h)
    curveTo(x + f, y + h, x, y + h - f, x, y + h - s)
    lineTo(x, y + s)
    curveTo(x, y + f, x + f, y, x + s, y)
    close()
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, false, cx + r, cy)
    arcTo(r, r, 0f, true, false, cx - r, cy)
    close()
}

// Home: two stacked cards, the top one is the strong one.
private fun home(p: Pal) = icon("Home") {
    fill(p.soft) { soft(4f, 13.2f, 16f, 6.8f, 3f) }
    fill(p.ink) { soft(3.5f, 4f, 17f, 7.6f, 3.2f) }
}

// Library: a folder with a small dark tab.
private fun library(p: Pal) = icon("Library") {
    fill(p.soft) { soft(3f, 8.2f, 18f, 12.3f, 3.6f) }
    fill(p.ink) {
        moveTo(3.2f, 9.4f)
        lineTo(3.2f, 7.8f)
        quadTo(3.2f, 5.4f, 5.6f, 5.4f)
        lineTo(8.6f, 5.4f)
        quadTo(9.8f, 5.4f, 10.5f, 6.4f)
        lineTo(11.4f, 7.7f)
        lineTo(11.4f, 9.4f)
        close()
    }
}

// Study: a bookmark ribbon with a dark cap.
private fun study(p: Pal) = icon("Study") {
    fill(p.soft) {
        moveTo(5.6f, 6.2f)
        lineTo(18.4f, 6.2f)
        lineTo(18.4f, 19.6f)
        quadTo(18.4f, 20.9f, 17.2f, 20.2f)
        lineTo(12f, 16.9f)
        lineTo(6.8f, 20.2f)
        quadTo(5.6f, 20.9f, 5.6f, 19.6f)
        close()
    }
    fill(p.ink) {
        moveTo(5.6f, 7.6f)
        lineTo(5.6f, 6.2f)
        quadTo(5.6f, 3.4f, 8.4f, 3.4f)
        lineTo(15.6f, 3.4f)
        quadTo(18.4f, 3.4f, 18.4f, 6.2f)
        lineTo(18.4f, 7.6f)
        quadTo(12f, 5.4f, 5.6f, 7.6f)
        close()
    }
}

// Progress: a histogram. Three soft bars, the tallest one is dark.
private fun progress(p: Pal) = icon("Progress") {
    fill(p.soft) { soft(3.5f, 12.5f, 4.6f, 8f, 1.9f) }
    fill(p.soft) { soft(9.7f, 8f, 4.6f, 12.5f, 1.9f) }
    fill(p.ink) { soft(15.9f, 3.5f, 4.6f, 17f, 1.9f) }
}

// Settings: two switches. Thick, so it reads well on small buttons.
private fun settings(p: Pal) = icon("Settings") {
    fill(p.soft) { soft(3f, 3.8f, 18f, 7.8f, 3.9f) }
    fill(p.ink) { circle(16.9f, 7.7f, 2.6f) }
    fill(p.soft) { soft(3f, 12.4f, 18f, 7.8f, 3.9f) }
    fill(p.ink) { circle(7.1f, 16.3f, 2.6f) }
}

// Back: a soft square tile with a thick chevron.
private fun back(p: Pal) = icon("Back", autoMirror = true) {
    fill(p.soft) { soft(3f, 3f, 18f, 18f, 6f) }
    line(p.ink, 2.5f) {
        moveTo(13.7f, 8.2f)
        lineTo(9.9f, 12f)
        lineTo(13.7f, 15.8f)
    }
}

private fun plus(p: Pal) = icon("Plus") {
    fill(p.soft) { soft(3f, 3f, 18f, 18f, 6f) }
    line(p.ink, 2.5f) { moveTo(12f, 7.6f); lineTo(12f, 16.4f) }
    line(p.ink, 2.5f) { moveTo(7.6f, 12f); lineTo(16.4f, 12f) }
}

private fun minus(p: Pal) = icon("Minus") {
    fill(p.soft) { soft(3f, 3f, 18f, 18f, 6f) }
    line(p.ink, 2.5f) { moveTo(7.6f, 12f); lineTo(16.4f, 12f) }
}

private fun lock(p: Pal) = icon("Lock") {
    line(p.ink, 2.4f) {
        moveTo(8.6f, 10.8f)
        lineTo(8.6f, 8.2f)
        arcTo(3.4f, 3.4f, 0f, false, true, 15.4f, 8.2f)
        lineTo(15.4f, 10.8f)
    }
    fill(p.soft) { soft(5f, 10.6f, 14f, 10f, 4f) }
    fill(p.ink) { circle(12f, 14.6f, 1.5f) }
    line(p.ink, 1.9f) { moveTo(12f, 15.2f); lineTo(12f, 17.6f) }
}

private fun share(p: Pal) = icon("Share") {
    fill(p.soft) { soft(4.5f, 10.6f, 15f, 10f, 3.6f) }
    line(p.ink, 2.4f) { moveTo(12f, 15.6f); lineTo(12f, 4f) }
    line(p.ink, 2.4f) {
        moveTo(7.9f, 8f)
        lineTo(12f, 3.9f)
        lineTo(16.1f, 8f)
    }
}

private fun exit(p: Pal) = icon("Exit", autoMirror = true) {
    fill(p.soft) { soft(11f, 4.5f, 8.6f, 15f, 3f) }
    line(p.ink, 2.4f) { moveTo(15f, 12f); lineTo(4.4f, 12f) }
    line(p.ink, 2.4f) {
        moveTo(8.6f, 7.8f)
        lineTo(4.4f, 12f)
        lineTo(8.6f, 16.2f)
    }
}

private fun sliders(p: Pal) = icon("Sliders") {
    line(p.soft, 2f) { moveTo(3.6f, 8f); lineTo(20.4f, 8f) }
    line(p.soft, 2f) { moveTo(3.6f, 16f); lineTo(20.4f, 16f) }
    fill(p.ink) { circle(8.8f, 8f, 3f) }
    fill(p.ink) { circle(15.2f, 16f, 3f) }
}

// Planner: a calendar page with a dark top band and two rings. New icon, same style.
private fun planner(p: Pal) = icon("Planner") {
    fill(p.soft) { soft(3.5f, 5f, 17f, 15.5f, 3.6f) }
    fill(p.ink) {
        moveTo(3.5f, 10f)
        lineTo(3.5f, 8.6f)
        quadTo(3.5f, 5f, 7.1f, 5f)
        lineTo(16.9f, 5f)
        quadTo(20.5f, 5f, 20.5f, 8.6f)
        lineTo(20.5f, 10f)
        close()
    }
    line(p.ink, 2.2f) {
        moveTo(8.2f, 3f)
        lineTo(8.2f, 6.2f)
        moveTo(15.8f, 3f)
        lineTo(15.8f, 6.2f)
    }
    fill(p.ink) { soft(13.6f, 13.6f, 3.8f, 3.8f, 1.2f) }
}

object MedoraIcons {
    val Home: ImageVector by lazy { home(LightPal) }
    val Library: ImageVector by lazy { library(LightPal) }
    val Study: ImageVector by lazy { study(LightPal) }
    val Progress: ImageVector by lazy { progress(LightPal) }
    val Settings: ImageVector by lazy { settings(LightPal) }
    val Back: ImageVector by lazy { back(LightPal) }
    val Plus: ImageVector by lazy { plus(LightPal) }
    val Minus: ImageVector by lazy { minus(LightPal) }
    val Lock: ImageVector by lazy { lock(LightPal) }
    val Share: ImageVector by lazy { share(LightPal) }
    val Exit: ImageVector by lazy { exit(LightPal) }
    val Sliders: ImageVector by lazy { sliders(LightPal) }
    val Planner: ImageVector by lazy { planner(LightPal) }

    // The same icons for the dark theme.
    internal val dark: Map<ImageVector, ImageVector> by lazy {
        mapOf(
            Home to home(DarkPal),
            Library to library(DarkPal),
            Study to study(DarkPal),
            Progress to progress(DarkPal),
            Settings to settings(DarkPal),
            Back to back(DarkPal),
            Plus to plus(DarkPal),
            Minus to minus(DarkPal),
            Lock to lock(DarkPal),
            Share to share(DarkPal),
            Exit to exit(DarkPal),
            Sliders to sliders(DarkPal),
            Planner to planner(DarkPal),
        )
    }
}

// Use on every icon: gives the dark-theme version when the dark theme is on.
@Composable
fun ImageVector.themed(): ImageVector =
    if (LocalMedoraDark.current) MedoraIcons.dark[this] ?: this else this
