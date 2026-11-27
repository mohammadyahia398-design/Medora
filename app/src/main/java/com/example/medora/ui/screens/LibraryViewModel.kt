package com.example.medora.ui.screens

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.Lecture
import com.example.medora.data.LectureRepository
import com.example.medora.data.MedoraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_BYTES = 5_000_000L

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

    fun add(title: String, subject: String) {
        viewModelScope.launch { repo.add(title, subject) }
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
            if (size > MAX_BYTES) return null
            val fileName = name ?: "Lecture"
            val title = fileName.substringBeforeLast('.').ifBlank { fileName }
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
}
