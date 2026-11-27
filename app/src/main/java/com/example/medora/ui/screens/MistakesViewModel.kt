package com.example.medora.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.MedoraDatabase
import com.example.medora.data.Mistake
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MistakesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)

    val mistakes: StateFlow<List<Mistake>> =
        db.mistakeDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val titles: StateFlow<Map<Long, String>> =
        db.lectureDao().observeAll()
            .map { list -> list.associate { it.id to it.title } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun remove(id: Long) {
        viewModelScope.launch { db.mistakeDao().delete(id) }
    }
}
