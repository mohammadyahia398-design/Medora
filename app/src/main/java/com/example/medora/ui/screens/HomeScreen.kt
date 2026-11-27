package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.CardRepository
import com.example.medora.data.MedoraDatabase
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

private const val RECENT_COUNT = 3

class HomeLecture(
    val id: Long,
    val title: String,
    val subject: String,
    val cardCount: Int,
    val studiedCount: Int,
)

class HomeState(
    val dueNow: Int = 0,
    val totalCards: Int = 0,
    val studied: Int = 0,
    val reviewsToday: Int = 0,
    val recent: List<HomeLecture> = emptyList(),
) {
    // Share of all cards that were studied at least once.
    val completionPercent: Int
        get() = if (totalCards > 0) studied * 100 / totalCards else 0
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)
    private val cardRepo = CardRepository(db.cardDao())

    private val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val state: StateFlow<HomeState> = combine(
        db.lectureDao().observeAll(),
        cardRepo.cards,
        cardRepo.observeDueCount(),
        db.statsDao().observeReviewsSince(today),
    ) { lectures, cards, due, reviews ->
        val byLecture = cards.groupBy { it.lectureId }
        HomeState(
            dueNow = due,
            totalCards = cards.size,
            studied = cards.count { it.reps > 0 },
            reviewsToday = reviews,
            recent = lectures.take(RECENT_COUNT).map { l ->
                val lectureCards = byLecture[l.id].orEmpty()
                HomeLecture(l.id, l.title, l.subject, lectureCards.size, lectureCards.count { it.reps > 0 })
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
}

@Composable
fun HomeScreen(
    onReview: () -> Unit = {},
    onOpenLecture: (Long) -> Unit = {},
    vm: HomeViewModel = viewModel(),
) {
    val s by vm.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MedoraCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_today),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.due_now, s.dueNow),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.home_reviews_today, s.reviewsToday),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            if (s.dueNow > 0) {
                MedoraButton(
                    text = stringResource(R.string.start_review),
                    onClick = onReview,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (s.totalCards > 0) {
                Text(
                    text = stringResource(R.string.no_due_cards),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = stringResource(R.string.study_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        MedoraCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_progress),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_cards_studied, s.studied, s.totalCards),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.home_completion, s.completionPercent.toString() + "%"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            ProgressBar(s.completionPercent / 100f)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_completion_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        MedoraCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_recent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (s.recent.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_recent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            s.recent.forEach { l ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenLecture(l.id) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = l.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (l.subject.isNotBlank()) {
                        Text(text = l.subject, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = if (l.cardCount > 0) {
                            stringResource(R.string.home_lecture_progress, l.studiedCount, l.cardCount)
                        } else {
                            stringResource(R.string.home_no_cards)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
