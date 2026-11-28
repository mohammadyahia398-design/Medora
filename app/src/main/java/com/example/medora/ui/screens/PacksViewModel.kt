package com.example.medora.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.Card
import com.example.medora.data.Lecture
import com.example.medora.data.MedoraDatabase
import com.example.medora.data.StudyPack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Study packs: groups of lectures shown and exported together.
class PacksViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)
    private val dao = db.studyPackDao()

    val packs: StateFlow<List<StudyPack>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lectures: StateFlow<List<Lecture>> =
        db.lectureDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cards: StateFlow<List<Card>> =
        db.cardDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pack(id: Long): Flow<StudyPack?> = dao.observe(id)

    fun create(name: String, ids: Collection<Long>) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { dao.insert(StudyPack(name = clean, lectureIds = StudyPack.join(ids))) }
    }

    fun update(id: Long, name: String, ids: Collection<Long>) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { dao.update(id, clean, StudyPack.join(ids)) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }
}
