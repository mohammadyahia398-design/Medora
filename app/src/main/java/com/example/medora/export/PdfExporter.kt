package com.example.medora.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.medora.data.Card
import com.example.medora.data.NoteImages
import com.example.medora.ui.components.plainNote

data class PdfLabels(val summary: String, val cards: String, val disclaimer: String)

// Writes a study PDF with the built-in PdfDocument (no library).
// Text is drawn with StaticLayout so Arabic (RTL) and English both work.
object PdfExporter {
    private const val W = 595
    private const val H = 842
    private const val M = 40

    fun write(
        context: Context,
        uri: Uri,
        title: String,
        subject: String,
        summary: String,
        content: String,
        cards: List<Card>,
        labels: PdfLabels,
    ) {
        val doc = PdfDocument()
        try {
            val w = PageWriter(doc, context)
            w.newPage()

            val titleP = paint(20f, bold = true)
            val subjectP = paint(12f, color = Color.DKGRAY)
            val headP = paint(15f, bold = true)
            val bodyP = paint(12f)
            val boldP = paint(12f, bold = true)
            val smallP = paint(10f, color = Color.GRAY)

            w.block(title, titleP, 4f)
            w.block(subject, subjectP, 12f)

            if (summary.isNotBlank()) {
                w.block(labels.summary, headP, 6f)
                w.lines(summary.split("\n"), bodyP, boldP, 4f)
                w.block(labels.disclaimer, smallP, 14f)
            }

            if (cards.isNotEmpty()) {
                w.block(labels.cards, headP, 8f)
                cards.forEach { c ->
                    w.block(c.front, boldP, 2f)
                    w.lines(c.back.split("\n"), bodyP, boldP, 2f)
                    w.gap(10f)
                }
            }

            if (summary.isBlank() && cards.isEmpty()) {
                w.lines(content.split("\n"), bodyP, boldP, 4f)
            }

            w.finish()
            context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                ?: throw IllegalStateException("Cannot open output")
        } finally {
            doc.close()
        }
    }

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK): TextPaint =
        TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private class PageWriter(private val doc: PdfDocument, private val context: Context) {
        private var page: PdfDocument.Page? = null
        private var y = M.toFloat()
        private var number = 0
        private val width = W - 2 * M

        fun newPage() {
            page?.let { doc.finishPage(it) }
            number++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, number).create())
            y = M.toFloat()
        }

        fun gap(h: Float) {
            y += h
        }

        fun block(text: String, paint: TextPaint, gapAfter: Float) {
            if (text.isBlank()) return
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setLineSpacing(0f, 1.2f)
                .build()
            if (y + layout.height > H - M && y > M) newPage()
            val canvas = page!!.canvas
            canvas.save()
            canvas.translate(M.toFloat(), y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + gapAfter
        }

        private fun cells(line: String): List<String> =
            line.trim().trim('|').split('|').map { it.trim() }

        private fun isSep(line: String): Boolean =
            cells(line).all { c -> c.isNotEmpty() && c.all { it == '-' || it == ':' } }

        fun lines(all: List<String>, body: TextPaint, bold: TextPaint, gapAfter: Float) {
            var i = 0
            while (i < all.size) {
                if (all[i].trim().startsWith("|")) {
                    val start = i
                    while (i < all.size && all[i].trim().startsWith("|")) i++
                    table(all.subList(start, i), body, bold)
                    gap(gapAfter + 6f)
                } else {
                    val path = NoteImages.pathOf(all[i])
                    val t = all[i].trimStart()
                    when {
                        path != null -> image(path, gapAfter)
                        // "# Heading" lines (notes and study packs) are printed bold.
                        t.startsWith("# ") -> block(plainNote(t.removePrefix("# ")), bold, gapAfter + 2f)
                        else -> block(plainNote(all[i]), body, gapAfter)
                    }
                    i++
                }
            }
        }

        // A note image, scaled to the page width (at most half a page high).
        private fun image(path: String, gapAfter: Float) {
            val bmp = NoteImages.load(context, path, 1200) ?: return
            try {
                var w = width.toFloat()
                var h = w * bmp.height / bmp.width
                val maxH = (H - 2 * M) / 2f
                if (h > maxH) {
                    w = w * maxH / h
                    h = maxH
                }
                if (y + h > H - M && y > M) newPage()
                val left = M + (width - w) / 2f
                page!!.canvas.drawBitmap(bmp, null, RectF(left, y, left + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
                y += h + gapAfter
            } finally {
                bmp.recycle()
            }
        }

        private fun table(raw: List<String>, body: TextPaint, bold: TextPaint) {
            val rows = raw.filter { !isSep(it) }.map { cells(it) }
            if (rows.isEmpty()) return
            val hasHeader = raw.size > 1 && isSep(raw[1])
            val cols = rows.maxOf { it.size }.coerceAtLeast(1)
            val colW = width / cols
            val pad = 4
            val border = Paint().apply { style = Paint.Style.STROKE; color = Color.GRAY; strokeWidth = 0.8f }
            val fill = Paint().apply { style = Paint.Style.FILL; color = Color.rgb(235, 235, 235) }
            rows.forEachIndexed { r, row ->
                val paint = if (hasHeader && r == 0) bold else body
                val layouts = (0 until cols).map { c ->
                    val t = plainNote(row.getOrElse(c) { "" }).ifEmpty { " " }
                    StaticLayout.Builder.obtain(t, 0, t.length, paint, colW - 2 * pad)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                        .build()
                }
                val h = layouts.maxOf { it.height } + 2f * pad
                if (y + h > H - M && y > M) newPage()
                val canvas = page!!.canvas
                layouts.forEachIndexed { c, l ->
                    val x = M + c * colW.toFloat()
                    if (hasHeader && r == 0) canvas.drawRect(x, y, x + colW, y + h, fill)
                    canvas.drawRect(x, y, x + colW, y + h, border)
                    canvas.save()
                    canvas.translate(x + pad, y + pad)
                    l.draw(canvas)
                    canvas.restore()
                }
                y += h
            }
        }

        fun finish() {
            page?.let { doc.finishPage(it) }
            page = null
        }
    }
}
