package com.example.medora.ui.screens

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.Lecture
import com.example.medora.data.LectureRepository
import com.example.medora.data.MedoraDatabase
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

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LectureRepository(MedoraDatabase.get(app).lectureDao())

    val lectures: StateFlow<List<Lecture>> =
        repo.lectures.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importFailed = MutableStateFlow(false)
    val importFailed: StateFlow<Boolean> = _importFailed

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
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { readTextFile(uri) }
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
            val isPdf = resolver.getType(uri) == "application/pdf" ||
                fileName.endsWith(".pdf", ignoreCase = true)
            if (isPdf) {
                if (size > MAX_PDF_BYTES) return null
                val pdfText = readPdf(uri)
                return if (pdfText == null) null else title to pdfText
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

    // Extracts selectable text from a PDF, page by page, with "[Page N]" markers.
    // Scanned PDFs (images only) have no text and return null.
    private fun readPdf(uri: Uri): String? {
        return try {
            val app = getApplication<Application>()
            PDFBoxResourceLoader.init(app)
            val input = app.contentResolver.openInputStream(uri) ?: return null
            input.use { stream ->
                PDDocument.load(stream).use { doc ->
                    val stripper = PDFTextStripper()
                    val sb = StringBuilder()
                    for (page in 1..doc.numberOfPages) {
                        stripper.setStartPage(page)
                        stripper.setEndPage(page)
                        val text = stripper.getText(doc).trim()
                        if (text.isNotEmpty()) {
                            sb.append("[Page ").append(page).append("]\n").append(text).append("\n\n")
                        }
                        if (sb.length > MAX_PDF_CHARS) break
                    }
                    sb.toString().trim().ifEmpty { null }
                }
            }
        } catch (e: Throwable) {
            null
        }
    }
}
