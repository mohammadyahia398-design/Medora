package com.example.medora.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// On-device OCR (ML Kit, bundled Latin model): works offline, nothing is uploaded.
// Reads English/Latin text only; Arabic script is not recognized.
// All functions block: call them from a background thread (Dispatchers.IO).
object OcrReader {
    const val MAX_OCR_PAGES = 150
    private const val MAX_SIDE = 2000

    // Reads text from a photo (jpg/png/webp). EXIF rotation is handled by ML Kit.
    fun fromImage(context: Context, uri: Uri): String? {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val image = InputImage.fromFilePath(context, uri)
            Tasks.await(recognizer.process(image)).text.trim().ifEmpty { null }
        } catch (e: Throwable) {
            null
        } finally {
            recognizer.close()
        }
    }

    // Renders the given 1-based PDF pages and reads their text.
    // Returns page number -> text (pages with no text are left out).
    fun fromPdfPages(
        context: Context,
        uri: Uri,
        pages: List<Int>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Map<Int, String> {
        val result = linkedMapOf<Int, String>()
        val wanted = pages.take(MAX_OCR_PAGES)
        if (wanted.isEmpty()) return result
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return result
            pfd.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    wanted.forEachIndexed { i, pageNo ->
                        onProgress(i, wanted.size)
                        if (pageNo in 1..renderer.pageCount) {
                            val text = readPage(renderer, pageNo - 1, recognizer)
                            if (!text.isNullOrBlank()) result[pageNo] = text
                        }
                    }
                    onProgress(wanted.size, wanted.size)
                }
            }
        } catch (e: Throwable) {
            // Keep whatever pages were read before the failure.
        } finally {
            recognizer.close()
        }
        return result
    }

    private fun readPage(
        renderer: PdfRenderer,
        index: Int,
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
    ): String? {
        var bitmap: Bitmap? = null
        return try {
            renderer.openPage(index).use { page ->
                // About 2x screen resolution, capped so big pages do not run out of memory.
                val scale = minOf(2f, MAX_SIDE.toFloat() / maxOf(page.width, page.height))
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmap = bmp
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            val image = InputImage.fromBitmap(bitmap!!, 0)
            Tasks.await(recognizer.process(image)).text.trim()
        } catch (e: Throwable) {
            null
        } finally {
            bitmap?.recycle()
        }
    }
}
