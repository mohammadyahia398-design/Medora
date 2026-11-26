package com.example.medora.export

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.medora.data.Card

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
            val w = PageWriter(doc)
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
                summary.split("\n").forEach { w.block(it, bodyP, 4f) }
                w.block(labels.disclaimer, smallP, 14f)
            }

            if (cards.isNotEmpty()) {
                w.block(labels.cards, headP, 8f)
                cards.forEach { c ->
                    w.block(c.front, boldP, 2f)
                    c.back.split("\n").forEach { w.block(it, bodyP, 2f) }
                    w.gap(10f)
                }
            }

            if (summary.isBlank() && cards.isEmpty()) {
                content.split("\n").forEach { w.block(it, bodyP, 4f) }
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

    private class PageWriter(private val doc: PdfDocument) {
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

        fun finish() {
            page?.let { doc.finishPage(it) }
            page = null
        }
    }
}
