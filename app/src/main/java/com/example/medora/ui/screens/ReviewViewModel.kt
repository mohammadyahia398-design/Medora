package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.Card
import com.example.medora.data.CardRepository
import com.example.medora.data.MedoraDatabase
import com.example.medora.data.ReviewLog
import com.example.medora.domain.Fsrs
import kotlinx.coroutines.launch

// One review session over the cards that are due now. Each answer is saved
// (new FSRS schedule + a review log). Lives in the ViewModel so rotation does not reset it.
class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)
    private val repo = CardRepository(db.cardDao())
    private val stats = db.statsDao()

    var loaded by mutableStateOf(false)
        private set
    var deck by mutableStateOf<List<Card>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var revealed by mutableStateOf(false)
        private set
    var knew by mutableIntStateOf(0)
        private set

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            deck = repo.getDue()
            index = 0
            knew = 0
            revealed = false
            loaded = true
        }
    }

    fun reveal() {
        revealed = true
    }

    fun answer(rating: Fsrs.Rating) {
        val card = deck.getOrNull(index) ?: return
        if (rating != Fsrs.Rating.Again) knew += 1
        revealed = false
        index += 1
        viewModelScope.launch {
            repo.update(Fsrs.schedule(card, rating))
            stats.insertLog(ReviewLog(cardId = card.id, rating = rating.value))
        }
    }

    fun restart() {
        loaded = false
        load()
    }
}
