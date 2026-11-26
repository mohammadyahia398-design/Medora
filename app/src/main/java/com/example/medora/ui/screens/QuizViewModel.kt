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
import kotlinx.coroutines.launch

class QuizQuestion(val card: Card, val options: List<String>, val correctIndex: Int)

private const val MAX_QUESTIONS = 10

// Multiple-choice quiz built from the user's own cards. The wrong options are
// the answers of other cards. No AI.
class QuizViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CardRepository(MedoraDatabase.get(app).cardDao())

    var loaded by mutableStateOf(false)
        private set
    var questions by mutableStateOf<List<QuizQuestion>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var selected by mutableStateOf<Int?>(null)
        private set
    var score by mutableIntStateOf(0)
        private set

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            questions = build(repo.getAll())
            index = 0
            score = 0
            selected = null
            loaded = true
        }
    }

    private fun build(cards: List<Card>): List<QuizQuestion> {
        return cards.shuffled().take(MAX_QUESTIONS).mapNotNull { card ->
            val wrong = cards
                .filter { it.id != card.id && it.back != card.back }
                .map { it.back }
                .distinct()
                .shuffled()
                .take(3)
            if (wrong.size < 2) {
                null
            } else {
                val options = (wrong + card.back).shuffled()
                QuizQuestion(card, options, options.indexOf(card.back))
            }
        }
    }

    fun select(option: Int) {
        if (selected != null) return
        selected = option
        if (option == questions[index].correctIndex) score += 1
    }

    fun next() {
        selected = null
        index += 1
    }

    fun restart() {
        loaded = false
        load()
    }
}
