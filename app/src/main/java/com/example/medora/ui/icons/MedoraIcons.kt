package com.example.medora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.medora.ui.theme.MedoraColors

// Our own icons, drawn in code. Thick rounded shapes + one dark detail each.
// Grid: 48 x 48.

private fun PathBuilder.rrect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    moveTo(x + r, y)
    lineTo(x + w - r, y)
    quadTo(x + w, y, x + w, y + r)
    lineTo(x + w, y + h - r)
    quadTo(x + w, y + h, x + w - r, y + h)
    lineTo(x + r, y + h)
    quadTo(x, y + h, x, y + h - r)
    lineTo(x, y + r)
    quadTo(x, y, x + r, y)
    close()
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, false, cx + r, cy)
    arcTo(r, r, 0f, true, false, cx - r, cy)
    close()
}

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 48.dp,
        defaultHeight = 48.dp,
        viewportWidth = 48f,
        viewportHeight = 48f,
    ).apply(block).build()

private fun ImageVector.Builder.fill(color: Color, block: PathBuilder.() -> Unit) {
    path(fill = SolidColor(color), pathBuilder = block)
}

private fun ImageVector.Builder.softFill(color: Color, width: Float, block: PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(color),
        stroke = SolidColor(color),
        strokeLineWidth = width,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}

object MedoraIcons {
    private val ink = MedoraColors.Ink

    val Home: ImageVector by lazy {
        icon("Home") {
            softFill(MedoraColors.Purple, 5f) {
                moveTo(9f, 22f); lineTo(24f, 9f); lineTo(39f, 22f); close()
            }
            fill(MedoraColors.Purple) { rrect(10f, 21f, 28f, 20f, 6f) }
            fill(ink) {
                moveTo(20f, 41f); lineTo(20f, 32f)
                quadTo(20f, 28f, 24f, 28f)
                quadTo(28f, 28f, 28f, 32f)
                lineTo(28f, 41f); close()
            }
        }
    }

    val Library: ImageVector by lazy {
        icon("Library") {
            fill(MedoraColors.Orange) { rrect(8f, 7f, 32f, 35f, 7f) }
            fill(ink) { rrect(8f, 7f, 10f, 35f, 5f) }
            fill(ink) { rrect(23f, 16f, 12f, 4f, 2f) }
        }
    }

    val Study: ImageVector by lazy {
        icon("Study") {
            softFill(MedoraColors.Green, 5f) {
                moveTo(24f, 10f); lineTo(41f, 19f); lineTo(24f, 28f); lineTo(7f, 19f); close()
            }
            fill(ink) {
                moveTo(14f, 28f); lineTo(14f, 34f)
                quadTo(24f, 42f, 34f, 34f)
                lineTo(34f, 28f); lineTo(24f, 33f); close()
            }
            fill(ink) { rrect(39f, 19f, 4f, 15f, 2f) }
        }
    }

    val Progress: ImageVector by lazy {
        icon("Progress") {
            fill(MedoraColors.Purple) { rrect(6f, 28f, 11f, 14f, 4f) }
            fill(MedoraColors.Orange) { rrect(19f, 18f, 11f, 24f, 4f) }
            fill(ink) { rrect(32f, 6f, 11f, 36f, 4f) }
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            fill(MedoraColors.Coral) { rrect(5f, 8f, 38f, 15f, 7.5f) }
            fill(ink) { circle(35.5f, 15.5f, 4.5f) }
            fill(MedoraColors.Purple) { rrect(5f, 26f, 38f, 15f, 7.5f) }
            fill(ink) { circle(12.5f, 33.5f, 4.5f) }
        }
    }

    val Back: ImageVector by lazy {
        ImageVector.Builder(
            name = "Back",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
            autoMirror = true,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(ink),
                strokeLineWidth = 6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(30f, 10f); lineTo(16f, 24f); lineTo(30f, 38f)
            }
        }.build()
    }
}
