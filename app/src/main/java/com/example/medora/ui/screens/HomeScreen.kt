package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
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
import com.example.medora.ui.theme.Spacing
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
    val itemShape = MaterialTheme.shapes.small
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        MedoraCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            spaced = false,
        ) {
            SectionTitle(stringResource(R.string.home_today))
            Spacer(Modifier.height(Spacing.m))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                StatBlock(
                    value = s.dueNow.toString(),
                    label = stringResource(R.string.home_stat_due),
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    value = s.reviewsToday.toString(),
                    label = stringResource(R.string.home_stat_reviews),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.l))
            if (s.dueNow > 0) {
                MedoraButton(
                    text = stringResource(R.string.start_review),
                    onClick = onReview,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = stringResource(if (s.totalCards > 0) R.string.no_due_cards else R.string.study_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
            SectionTitle(stringResource(R.string.home_progress))
            Spacer(Modifier.height(Spacing.m))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                StatBlock(
                    value = s.completionPercent.toString() + "%",
                    label = stringResource(R.string.home_stat_completion),
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    value = s.studied.toString() + " / " + s.totalCards.toString(),
                    label = stringResource(R.string.home_stat_studied),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.m))
            ProgressBar(s.completionPercent / 100f)
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(R.string.home_completion_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
            SectionTitle(stringResource(R.string.home_recent))
            if (s.recent.isEmpty()) {
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = stringResource(R.string.home_recent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            s.recent.forEachIndexed { index, l ->
                Spacer(Modifier.height(if (index == 0) Spacing.m else Spacing.s))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(itemShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onOpenLecture(l.id) }
                        .padding(Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = l.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (l.subject.isNotBlank()) {
                        Text(
                            text = l.subject,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = if (l.cardCount > 0) {
                            stringResource(R.string.home_lecture_progress, l.studiedCount, l.cardCount)
                        } else {
                            stringResource(R.string.home_no_cards)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

// A big number with a small label under it.
@Composable
private fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.copy(alpha = 0.72f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progress",
    )
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
