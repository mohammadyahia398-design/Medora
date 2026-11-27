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

// Our own icons, drawn in code. One style for all: a dark rounded outline
// and a single soft color on the main shape. Grid: 24 x 24, stroke 1.8.

private const val STROKE = 1.8f

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
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

// A dark outline. If tint is given, the shape is filled with that soft color.
private fun ImageVector.Builder.outline(
    tint: Color?,
    width: Float = STROKE,
    block: PathBuilder.() -> Unit,
) {
    path(
        fill = tint?.let { SolidColor(it) },
        stroke = SolidColor(MedoraColors.Ink),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}

object MedoraIcons {
    val Home: ImageVector by lazy {
        icon("Home") {
            outline(MedoraColors.Purple) {
                moveTo(4f, 10.5f); lineTo(12f, 4f); lineTo(20f, 10.5f)
                lineTo(20f, 18.5f); quadTo(20f, 20f, 18.5f, 20f)
                lineTo(5.5f, 20f); quadTo(4f, 20f, 4f, 18.5f); close()
            }
            outline(null) {
                moveTo(10f, 20f); lineTo(10f, 15f)
                quadTo(10f, 13f, 12f, 13f); quadTo(14f, 13f, 14f, 15f)
                lineTo(14f, 20f)
            }
        }
    }

    val Library: ImageVector by lazy {
        icon("Library") {
            outline(MedoraColors.Orange) { rrect(5f, 3.5f, 14f, 17f, 2.5f) }
            outline(null) { moveTo(9.5f, 3.5f); lineTo(9.5f, 20.5f) }
            outline(null) {
                moveTo(12.5f, 8f); lineTo(16f, 8f)
                moveTo(12.5f, 11.5f); lineTo(15f, 11.5f)
            }
        }
    }

    val Study: ImageVector by lazy {
        icon("Study") {
            outline(MedoraColors.Green) {
                moveTo(12f, 5f); lineTo(21f, 9.5f); lineTo(12f, 14f); lineTo(3f, 9.5f); close()
            }
            outline(null) {
                moveTo(7f, 12f); lineTo(7f, 15.5f)
                quadTo(12f, 19f, 17f, 15.5f); lineTo(17f, 12f)
            }
            outline(null) { moveTo(21f, 9.5f); lineTo(21f, 14.5f) }
        }
    }

    val Progress: ImageVector by lazy {
        icon("Progress") {
            outline(MedoraColors.Purple) { rrect(4f, 12f, 4.5f, 8f, 1.5f) }
            outline(MedoraColors.Purple) { rrect(9.75f, 8f, 4.5f, 12f, 1.5f) }
            outline(MedoraColors.Orange) { rrect(15.5f, 4f, 4.5f, 16f, 1.5f) }
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            outline(null) { moveTo(4f, 8f); lineTo(20f, 8f) }
            outline(MedoraColors.Coral) { circle(15f, 8f, 2.6f) }
            outline(null) { moveTo(4f, 16f); lineTo(20f, 16f) }
            outline(MedoraColors.Coral) { circle(9f, 16f, 2.6f) }
        }
    }

    val Back: ImageVector by lazy {
        ImageVector.Builder(
            name = "Back",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true,
        ).apply {
            outline(null, 2.2f) {
                moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f)
            }
        }.build()
    }
}
