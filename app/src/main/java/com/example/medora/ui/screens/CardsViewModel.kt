package com.example.medora.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.Card
import com.example.medora.data.CardRepository
import com.example.medora.data.MedoraDatabase
import com.example.medora.domain.CardSuggester
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)
    private val repo = CardRepository(db.cardDao())

    val cards: StateFlow<List<Card>> =
        repo.cards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueCount: StateFlow<Int> =
        repo.observeDueCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val aiQuestionCount: StateFlow<Int> =
        db.aiQuestionDao().observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val mistakeCount: StateFlow<Int> =
        db.mistakeDao().observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun cardsOf(lectureId: Long): Flow<List<Card>> = repo.observeByLecture(lectureId)

    fun add(lectureId: Long, front: String, back: String, source: String) {
        viewModelScope.launch { repo.add(lectureId, front, back, source) }
    }

    fun addSuggestions(lectureId: Long, items: List<CardSuggester.Suggestion>) {
        viewModelScope.launch {
            repo.addAll(items.map { Card(lectureId = lectureId, front = it.front, back = it.back, source = it.source) })
        }
    }

    fun delete(card: Card) {
        viewModelScope.launch { repo.delete(card) }
    }
}
