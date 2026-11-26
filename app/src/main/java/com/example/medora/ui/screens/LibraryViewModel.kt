package com.example.medora.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.Lecture
import com.example.medora.data.LectureRepository
import com.example.medora.data.MedoraDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LectureRepository(MedoraDatabase.get(app).lectureDao())

    val lectures: StateFlow<List<Lecture>> =
        repo.lectures.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, subject: String) {
        viewModelScope.launch { repo.add(title, subject) }
    }

    fun delete(lecture: Lecture) {
        viewModelScope.launch { repo.delete(lecture) }
    }
}
