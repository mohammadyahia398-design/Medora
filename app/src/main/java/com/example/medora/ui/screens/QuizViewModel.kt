package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.AiQuestion
import com.example.medora.data.Card
import com.example.medora.data.CardRepository
import com.example.medora.data.MedoraDatabase
import com.example.medora.data.QuizResult
import kotlinx.coroutines.launch

class QuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val source: String,
    val lectureId: Long,
)

private const val MAX_QUESTIONS = 10

// Multiple-choice quiz from the user's own cards (wrong options = other cards' answers)
// plus any AI questions. The final score is saved.
class QuizViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)
    private val repo = CardRepository(db.cardDao())
    private val aiDao = db.aiQuestionDao()
    private val stats = db.statsDao()

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
            val fromCards = fromCards(repo.getAll())
            val fromAi = aiDao.getAll().mapNotNull(::fromAi)
            questions = (fromCards + fromAi).shuffled().take(MAX_QUESTIONS)
            index = 0
            score = 0
            selected = null
            loaded = true
        }
    }

    private fun fromCards(cards: List<Card>): List<QuizQuestion> {
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
                val source = if (card.source != card.back) card.source else ""
                QuizQuestion(card.front, options, options.indexOf(card.back), source, card.lectureId)
            }
        }
    }

    private fun fromAi(q: AiQuestion): QuizQuestion? {
        val options = q.options.split("\n").filter { it.isNotBlank() }
        if (options.size < 2 || q.correctIndex !in options.indices) return null
        val correct = options[q.correctIndex]
        val shuffled = options.shuffled()
        return QuizQuestion(q.question, shuffled, shuffled.indexOf(correct), q.source, q.lectureId)
    }

    fun select(option: Int) {
        if (selected != null) return
        selected = option
        if (option == questions[index].correctIndex) score += 1
    }

    fun next() {
        selected = null
        index += 1
        if (index == questions.size) {
            val result = QuizResult(score = score, total = questions.size)
            viewModelScope.launch { stats.insertQuiz(result) }
        }
    }

    fun restart() {
        loaded = false
        load()
    }
}
