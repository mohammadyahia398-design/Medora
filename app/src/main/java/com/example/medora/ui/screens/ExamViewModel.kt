package com.example.medora.ui.screens

import android.app.Application
import android.os.SystemClock
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
import com.example.medora.domain.PageFinder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ExamPhase { Setup, Running, Result }

private const val POOL_MAX = 40
private const val SECONDS_PER_QUESTION = 60
private const val MIN_EXAM_QUESTIONS = 4

// Timed exam: questions from the user's cards + AI questions, one minute per question.
// No feedback until the end. The final score is saved with the other quiz results.
class ExamViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)
    private val repo = CardRepository(db.cardDao())
    private val aiDao = db.aiQuestionDao()
    private val stats = db.statsDao()

    private var pool: List<QuizQuestion> = emptyList()
    private var timerJob: Job? = null

    var loaded by mutableStateOf(false)
        private set
    var phase by mutableStateOf(ExamPhase.Setup)
        private set
    var available by mutableIntStateOf(0)
        private set
    var chosenCount by mutableIntStateOf(0)
        private set
    var questions by mutableStateOf<List<QuizQuestion>>(emptyList())
        private set
    var answers by mutableStateOf<List<Int?>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var remaining by mutableIntStateOf(0)
        private set
    var usedSeconds by mutableIntStateOf(0)
        private set
    var score by mutableIntStateOf(0)
        private set
    var timedOut by mutableStateOf(false)
        private set

    val countOptions: List<Int>
        get() = if (available < MIN_EXAM_QUESTIONS) {
            emptyList()
        } else {
            (listOf(10, 20, 40).filter { it <= available } + available).distinct().sorted()
        }

    val unanswered: Int
        get() = answers.count { it == null }

    private val totalSeconds: Int
        get() = questions.size * SECONDS_PER_QUESTION

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val contents = db.lectureDao().getAll().associate { it.id to it.content }
            val fromCards = cardQuestions(repo.getAll(), contents)
            val fromAi = aiDao.getAll().mapNotNull { aiQuestion(it, contents) }
            pool = (fromCards + fromAi).shuffled().take(POOL_MAX)
            available = pool.size
            val options = countOptions
            chosenCount = options.lastOrNull { it <= 20 } ?: options.firstOrNull() ?: 0
            phase = ExamPhase.Setup
            loaded = true
        }
    }

    private fun cardQuestions(cards: List<Card>, contents: Map<Long, String>): List<QuizQuestion> {
        return cards.shuffled().take(POOL_MAX).mapNotNull { card ->
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
                val page = PageFinder.pageOf(contents[card.lectureId], card.source, card.back, card.front)
                QuizQuestion(card.front, options, options.indexOf(card.back), source, card.lectureId, page)
            }
        }
    }

    private fun aiQuestion(q: AiQuestion, contents: Map<Long, String>): QuizQuestion? {
        val options = q.options.split("\n").filter { it.isNotBlank() }
        if (options.size < 2 || q.correctIndex !in options.indices) return null
        val correct = options[q.correctIndex]
        val shuffled = options.shuffled()
        val page = PageFinder.pageOf(contents[q.lectureId], q.source)
        return QuizQuestion(q.question, shuffled, shuffled.indexOf(correct), q.source, q.lectureId, page)
    }

    fun chooseCount(count: Int) {
        if (count in countOptions) chosenCount = count
    }

    fun start() {
        if (chosenCount < MIN_EXAM_QUESTIONS || phase != ExamPhase.Setup) return
        val picked = pool.shuffled().take(chosenCount)
        questions = picked
        answers = List<Int?>(picked.size) { null }
        index = 0
        timedOut = false
        val total = picked.size * SECONDS_PER_QUESTION
        remaining = total
        val endAt = SystemClock.elapsedRealtime() + total * 1000L
        phase = ExamPhase.Running
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val left = ((endAt - SystemClock.elapsedRealtime() + 999) / 1000).toInt()
                remaining = left.coerceAtLeast(0)
                if (left <= 0) {
                    timedOut = true
                    finish()
                    break
                }
            }
        }
    }

    fun select(option: Int) {
        if (phase != ExamPhase.Running) return
        val updated = answers.toMutableList()
        updated[index] = option
        answers = updated
    }

    fun goTo(i: Int) {
        if (phase == ExamPhase.Running && i in questions.indices) index = i
    }

    fun finish() {
        if (phase != ExamPhase.Running) return
        timerJob?.cancel()
        timerJob = null
        usedSeconds = totalSeconds - remaining
        score = questions.indices.count { answers[it] == questions[it].correctIndex }
        phase = ExamPhase.Result
        val result = QuizResult(score = score, total = questions.size)
        viewModelScope.launch { stats.insertQuiz(result) }
    }

    fun retry() {
        timerJob?.cancel()
        timerJob = null
        loaded = false
        load()
    }
}
