package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.Card
import com.example.medora.data.MedoraDatabase
import com.example.medora.data.Mistake
import com.example.medora.data.QuizResult
import com.example.medora.domain.PageFinder
import kotlinx.coroutines.launch
import java.util.Locale

const val PRACTICE_TF = "tf"
const val PRACTICE_SHORT = "short"
const val PRACTICE_WEAK = "weak"

private const val MAX_ITEMS = 10

// One practice item, always made from one of the user's own cards.
// tf: statement shown with the card front; correct = statement is the card's answer.
// short: user types the answer. weak: multiple choice from forgotten cards.
class PracticeItem(
    val card: Card,
    val statement: String = "",
    val isTrue: Boolean = true,
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val page: Int? = null,
)

class PracticeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)

    var mode by mutableStateOf("")
        private set
    var loaded by mutableStateOf(false)
        private set
    var items by mutableStateOf<List<PracticeItem>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var answered by mutableStateOf(false)
        private set
    var lastCorrect by mutableStateOf(false)
        private set
    var chosen by mutableStateOf<Int?>(null)
        private set
    var score by mutableIntStateOf(0)
        private set

    // A wrong typed answer waits here, because the user can still count it as correct.
    private var pending: Mistake? = null

    fun start(newMode: String) {
        if (newMode == mode && loaded) return
        mode = newMode
        load()
    }

    private fun load() {
        loaded = false
        viewModelScope.launch {
            val cards = db.cardDao().getAll()
            val contents = db.lectureDao().getAll().associate { it.id to it.content }
            items = when (mode) {
                PRACTICE_TF -> trueFalse(cards, contents)
                PRACTICE_SHORT -> cards.shuffled().take(MAX_ITEMS).map { PracticeItem(it, page = pageOf(it, contents)) }
                else -> weak(cards, contents)
            }
            pending = null
            index = 0
            score = 0
            answered = false
            chosen = null
            loaded = true
        }
    }

    private fun pageOf(c: Card, contents: Map<Long, String>) =
        PageFinder.pageOf(contents[c.lectureId], c.source, c.back, c.front)

    private fun trueFalse(cards: List<Card>, contents: Map<Long, String>): List<PracticeItem> {
        val answers = cards.map { it.back }.distinct()
        if (answers.size < 2) return emptyList()
        return cards.shuffled().take(MAX_ITEMS).map { c ->
            val showTrue = Math.random() < 0.5
            val statement = if (showTrue) c.back else answers.filter { it != c.back }.random()
            PracticeItem(c, statement = statement, isTrue = showTrue, page = pageOf(c, contents))
        }
    }

    // Weak = cards rated "Again" at least once in the review history (same rule as Progress).
    private suspend fun weak(cards: List<Card>, contents: Map<Long, String>): List<PracticeItem> {
        val forgotten = db.statsDao().getAllLogs().filter { it.rating == 1 }.map { it.cardId }.toSet()
        return cards.filter { it.id in forgotten }.shuffled().take(MAX_ITEMS).mapNotNull { c ->
            val wrong = cards.filter { it.back != c.back }.map { it.back }.distinct().shuffled().take(3)
            if (wrong.size < 2) {
                null
            } else {
                val options = (wrong + c.back).shuffled()
                PracticeItem(c, options = options, correctIndex = options.indexOf(c.back), page = pageOf(c, contents))
            }
        }
    }

    fun answerTrueFalse(saidTrue: Boolean) {
        if (answered) return
        val item = items[index]
        val ok = saidTrue == item.isTrue
        mark(ok)
        chosen = if (saidTrue) 0 else 1
        if (!ok) save(item, if (saidTrue) item.statement else "")
    }

    fun answerChoice(i: Int) {
        if (answered) return
        chosen = i
        val item = items[index]
        val ok = i == item.correctIndex
        mark(ok)
        if (!ok) save(item, item.options.getOrElse(i) { "" })
    }

    fun answerShort(text: String) {
        if (answered || text.isBlank()) return
        val item = items[index]
        mark(matches(text, item.card.back))
        if (!lastCorrect) {
            pending = Mistake(
                lectureId = item.card.lectureId,
                kind = "practice",
                question = item.card.front,
                chosen = text.trim(),
                correct = item.card.back,
                source = item.card.source,
            )
        }
    }

    private fun save(item: PracticeItem, chosenText: String) {
        val m = Mistake(
            lectureId = item.card.lectureId,
            kind = "practice",
            question = item.card.front,
            chosen = chosenText,
            correct = item.card.back,
            source = item.card.source,
        )
        viewModelScope.launch { db.mistakeDao().insertAll(listOf(m)) }
    }

    // The user can overrule the automatic check of a typed answer.
    fun countAsCorrect() {
        if (!answered || lastCorrect) return
        lastCorrect = true
        score += 1
        pending = null
    }

    private fun mark(ok: Boolean) {
        answered = true
        lastCorrect = ok
        if (ok) score += 1
    }

    fun next() {
        pending?.let { m -> viewModelScope.launch { db.mistakeDao().insertAll(listOf(m)) } }
        pending = null
        answered = false
        chosen = null
        index += 1
        if (index == items.size && items.isNotEmpty()) {
            val result = QuizResult(score = score, total = items.size)
            viewModelScope.launch { db.statsDao().insertQuiz(result) }
        }
    }

    fun restart() = load()

    companion object {
        private fun norm(s: String) = s.lowercase(Locale.ROOT)
            .replace(Regex("[\\p{Punct}\\s]+"), " ")
            .trim()

        // Exact match after cleanup, or the typed text covers most of the answer.
        fun matches(typed: String, answer: String): Boolean {
            val a = norm(typed)
            val b = norm(answer)
            if (a.isEmpty() || b.isEmpty()) return false
            if (a == b) return true
            val shorter = minOf(a.length, b.length).toDouble()
            val longer = maxOf(a.length, b.length).toDouble()
            return (a.contains(b) || b.contains(a)) && shorter / longer >= 0.6
        }
    }
}
