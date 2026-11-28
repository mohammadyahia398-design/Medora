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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.medora.ui.icons.MedoraIcons
import com.example.medora.ui.icons.themed
import com.example.medora.ui.theme.MedoraPastel
import com.example.medora.ui.theme.MedoraColors
import com.example.medora.ui.theme.LocalMedoraDark
import com.example.medora.ui.components.MedoraArrowButton
import com.example.medora.ui.components.MedoraScreenTitle
import com.example.medora.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    weekVm: StudyWeekViewModel = viewModel(),
) {
    val s by vm.state.collectAsState()
    val week by weekVm.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        MedoraScreenTitle(stringResource(R.string.home_title))
        HeroCard(s, onReview)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            StatCard(
                value = s.dueNow.toString(),
                label = stringResource(R.string.home_stat_due),
                toneIndex = 0,
                icon = MedoraIcons.Study,
                modifier = Modifier.weight(1f),
                onClick = onReview,
            )
            StatCard(
                value = s.reviewsToday.toString(),
                label = stringResource(R.string.home_stat_reviews),
                toneIndex = 1,
                icon = MedoraIcons.Progress,
                modifier = Modifier.weight(1f),
                onClick = null,
            )
        }

        WeekCard(week)

        Text(
            text = stringResource(R.string.home_recent),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (s.recent.isEmpty()) {
            MedoraCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_recent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                s.recent.forEach { l -> RecentLectureRow(l, onClick = { onOpenLecture(l.id) }) }
            }
        }
    }
}

// Main blue card: completion share, a progress bar and the one main button of the screen.
// The button is dark (navy), like the black capsule in the reference.
@Composable
private fun HeroCard(s: HomeState, onReview: () -> Unit) {
    val on = MaterialTheme.colorScheme.onPrimary
    val dark = LocalMedoraDark.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary)
            .padding(Spacing.card),
    ) {
        Text(
            text = stringResource(R.string.home_stat_completion),
            style = MaterialTheme.typography.labelLarge,
            color = on.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = s.completionPercent.toString() + "%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = on,
        )
        Text(
            text = s.studied.toString() + " / " + s.totalCards.toString() + "  " + stringResource(R.string.home_stat_studied),
            style = MaterialTheme.typography.labelMedium,
            color = on.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(Spacing.m))
        ProgressBar(s.completionPercent / 100f, track = on.copy(alpha = 0.25f), fill = on)
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(R.string.home_completion_hint),
            style = MaterialTheme.typography.bodySmall,
            color = on.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(Spacing.l))
        if (s.dueNow > 0) {
            MedoraButton(
                text = stringResource(R.string.start_review),
                onClick = onReview,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MedoraColors.Navy,
                contentColor = if (dark) MedoraColors.Sky else Color.White,
            )
        } else {
            Text(
                text = stringResource(if (s.totalCards > 0) R.string.no_due_cards else R.string.study_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = on,
            )
        }
    }
}

// Pastel card: icon in a white square on top, a big number, a label, and an arrow if it opens something.
@Composable
private fun StatCard(
    value: String,
    label: String,
    toneIndex: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
) {
    val tone = MedoraPastel.tone(toneIndex)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(tone.container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Image(imageVector = icon.themed(), contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(Spacing.m))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = tone.content,
                    maxLines = 1,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = tone.content.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onClick != null) MedoraArrowButton()
        }
    }
}

// White card with seven rounded bars (last 7 days, oldest first). Today is the strong one.
@Composable
private fun WeekCard(week: StudyWeek) {
    val bar = MaterialTheme.colorScheme.primary
    val max = (week.days.maxOrNull() ?: 0).coerceAtLeast(1)
    val labels = remember { weekdayLabels() }
    MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stat_week_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.stat_streak) + ": " + stringResource(R.string.stat_streak_days, week.streak),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.m))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            week.days.forEachIndexed { i, n ->
                val isToday = i == week.days.lastIndex
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                        if (isToday && n > 0) {
                            Text(
                                text = n.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.inverseSurface)
                                    .padding(horizontal = Spacing.s, vertical = 2.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(104.dp)
                            .clip(CircleShape)
                            .background(bar.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (n > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(n.toFloat() / max)
                                    .clip(CircleShape)
                                    .background(if (isToday) bar else bar.copy(alpha = 0.45f)),
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = labels[i],
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Short day names for the last 7 days, oldest first (same order as StudyWeek.days).
private fun weekdayLabels(): List<String> {
    val format = SimpleDateFormat("EEE", Locale.getDefault())
    return (6 downTo 0).map { ago ->
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -ago)
        format.format(c.time)
    }
}

@Composable
private fun RecentLectureRow(l: HomeLecture, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Image(imageVector = MedoraIcons.Library.themed(), contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(Spacing.m))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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

@Composable
private fun ProgressBar(fraction: Float, track: Color, fill: Color) {
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
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(shape)
                .background(fill),
        )
    }
}
