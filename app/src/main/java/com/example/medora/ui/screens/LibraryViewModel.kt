package com.example.medora.ui.screens

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.Lecture
import com.example.medora.data.LectureRepository
import com.example.medora.data.MedoraDatabase
import com.example.medora.ocr.OcrReader
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_BYTES = 5_000_000L
private const val MAX_PDF_BYTES = 20_000_000L
private const val MAX_PDF_CHARS = 2_000_000
private const val MAX_IMAGE_BYTES = 15_000_000L

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LectureRepository(MedoraDatabase.get(app).lectureDao())

    val lectures: StateFlow<List<Lecture>> =
        repo.lectures.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importFailed = MutableStateFlow(false)
    val importFailed: StateFlow<Boolean> = _importFailed

    // Shown while a long import runs. OCR progress is (done, total) pages; total 0 = no OCR yet.
    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing
    private val _ocrProgress = MutableStateFlow(0 to 0)
    val ocrProgress: StateFlow<Pair<Int, Int>> = _ocrProgress

    fun dismissImportError() {
        _importFailed.value = false
    }

    fun lecture(id: Long): Flow<Lecture?> = repo.observe(id)

    fun add(title: String, subject: String, content: String = "") {
        viewModelScope.launch { repo.add(title, subject, content) }
    }

    fun saveNote(id: Long, title: String, content: String) {
        viewModelScope.launch { repo.updateNote(id, title, content) }
    }

    fun delete(lecture: Lecture) {
        viewModelScope.launch { repo.delete(lecture) }
    }

    // Reads a UTF-8 text file the user picked and saves it as a lecture.
    fun importText(uri: Uri) {
        if (_importing.value) return
        viewModelScope.launch {
            _importing.value = true
            _ocrProgress.value = 0 to 0
            val result = try {
                withContext(Dispatchers.IO) { readTextFile(uri) }
            } finally {
                _importing.value = false
            }
            if (result == null) {
                _importFailed.value = true
            } else {
                repo.add(result.first, "", result.second)
            }
        }
    }

    private fun readTextFile(uri: Uri): Pair<String, String>? {
        return try {
            val resolver = getApplication<Application>().contentResolver
            var name: String? = null
            var size = 0L
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ni >= 0) name = c.getString(ni)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
            val fileName = name ?: "Lecture"
            val title = fileName.substringBeforeLast('.').ifBlank { fileName }
            val type = resolver.getType(uri).orEmpty()
            val isPdf = type == "application/pdf" ||
                fileName.endsWith(".pdf", ignoreCase = true)
            if (isPdf) {
                if (size > MAX_PDF_BYTES) return null
                val pdfText = readPdf(uri)
                return if (pdfText == null) null else title to pdfText
            }
            val isImage = type.startsWith("image/") ||
                Regex(".*\\.(jpe?g|png|webp|heic)$", RegexOption.IGNORE_CASE).matches(fileName)
            if (isImage) {
                if (size > MAX_IMAGE_BYTES) return null
                val imageText = OcrReader.fromImage(getApplication<Application>(), uri)
                return if (imageText == null) null else title to imageText
            }
            if (size > MAX_BYTES) return null
            val text = resolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?.removePrefix("\uFEFF")
                ?.trim()
            if (text.isNullOrEmpty()) null else title to text
        } catch (e: Exception) {
            null
        }
    }

    // Extracts text from a PDF, page by page, with "[Page N]" markers.
    // Pages without selectable text (scanned or image slides) are read with on-device OCR.
    private fun readPdf(uri: Uri): String? {
        val app = getApplication<Application>()
        val pages = linkedMapOf<Int, String>()
        var pageCount = 0
        var lastPage = 0
        try {
            PDFBoxResourceLoader.init(app)
            app.contentResolver.openInputStream(uri)?.use { stream ->
                PDDocument.load(stream).use { doc ->
                    pageCount = doc.numberOfPages
                    lastPage = pageCount
                    val stripper = PDFTextStripper()
                    var chars = 0
                    for (page in 1..pageCount) {
                        stripper.setStartPage(page)
                        stripper.setEndPage(page)
                        val text = stripper.getText(doc).trim()
                        if (text.isNotEmpty()) {
                            pages[page] = text
                            chars += text.length
                        }
                        if (chars > MAX_PDF_CHARS) {
                            lastPage = page
                            break
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            // PDFBox failed: OCR below may still read the pages.
        }
        if (pageCount == 0) {
            pageCount = try {
                app.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                    android.graphics.pdf.PdfRenderer(fd).use { it.pageCount }
                } ?: 0
            } catch (e: Throwable) {
                0
            }
            lastPage = pageCount
        }
        val missing = (1..lastPage).filter { it !in pages }
        if (missing.isNotEmpty()) {
            val ocr = OcrReader.fromPdfPages(app, uri, missing) { done, total ->
                _ocrProgress.value = done to total
            }
            pages.putAll(ocr)
        }
        val sb = StringBuilder()
        for (page in pages.keys.sorted()) {
            sb.append("[Page ").append(page).append("]\n").append(pages[page]).append("\n\n")
        }
        return sb.toString().trim().ifEmpty { null }
    }
}
