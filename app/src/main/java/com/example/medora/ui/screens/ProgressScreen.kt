package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.CardRepository
import com.example.medora.data.MedoraDatabase
import com.example.medora.ui.components.MedoraCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class ProgressStats(
    val totalCards: Int = 0,
    val dueNow: Int = 0,
    val learned: Int = 0,
    val reviewsToday: Int = 0,
    val reviewsWeek: Int = 0,
    val lastQuiz: String = "-",
    val quizAverage: String = "-",
)

class ProgressViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)
    private val cards = CardRepository(db.cardDao())
    private val stats = db.statsDao()

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val today = startOfToday()
    private val weekAgo = today - 6 * 86_400_000L

    private val counts = combine(
        cards.cards,
        cards.observeDueCount(),
        cards.observeLearnedCount(),
    ) { all, due, learned -> Triple(all.size, due, learned) }

    val state: StateFlow<ProgressStats> = combine(
        counts,
        stats.observeReviewsSince(today),
        stats.observeReviewsSince(weekAgo),
        stats.observeRecentQuizzes(),
    ) { c, todayCount, weekCount, quizzes ->
        val last = quizzes.firstOrNull()
        val totalAsked = quizzes.sumOf { it.total }
        ProgressStats(
            totalCards = c.first,
            dueNow = c.second,
            learned = c.third,
            reviewsToday = todayCount,
            reviewsWeek = weekCount,
            lastQuiz = last?.let { "${it.score} / ${it.total}" } ?: "-",
            quizAverage = if (totalAsked > 0) "${quizzes.sumOf { it.score } * 100 / totalAsked}%" else "-",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressStats())
}

@Composable
fun ProgressScreen(vm: ProgressViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatRow(stringResource(R.string.stat_total_cards), s.totalCards.toString())
        StatRow(stringResource(R.string.stat_due_now), s.dueNow.toString())
        StatRow(stringResource(R.string.stat_learned), s.learned.toString())
        StatRow(stringResource(R.string.stat_reviews_today), s.reviewsToday.toString())
        StatRow(stringResource(R.string.stat_reviews_week), s.reviewsWeek.toString())
        StatRow(stringResource(R.string.stat_last_quiz), s.lastQuiz)
        StatRow(stringResource(R.string.stat_quiz_average), s.quizAverage)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    MedoraCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
