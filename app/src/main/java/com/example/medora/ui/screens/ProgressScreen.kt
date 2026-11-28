package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.CardRepository
import com.example.medora.data.MedoraDatabase
import com.example.medora.settings.AppPrefs
import com.example.medora.ui.components.MedoraCard
import com.example.medora.ui.components.MedoraScreenTitle
import com.example.medora.ui.components.MedoraSlider
import com.example.medora.ui.icons.MedoraIcons
import com.example.medora.ui.icons.themed
import com.example.medora.ui.theme.LocalMedoraDark
import com.example.medora.ui.theme.MedoraColors
import com.example.medora.ui.theme.MedoraPastel
import com.example.medora.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.roundToInt

data class ProgressStats(
    val totalCards: Int = 0,
    val dueNow: Int = 0,
    val learned: Int = 0,
    val reviewsToday: Int = 0,
    val reviewsWeek: Int = 0,
    val lastQuiz: String = "-",
    val quizAverage: String = "-",
)

// Review activity for the charts. week: last 7 days, oldest first.
// month: reviews per day of this month (index 0 = day 1). firstColumn: empty cells before day 1.
data class Activity(
    val total: Int = 0,
    val week: List<Int> = List(7) { 0 },
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val month: List<Int> = emptyList(),
    val firstColumn: Int = 0,
    val today: Int = 1,
)

class ProgressViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)
    private val cards = CardRepository(db.cardDao())
    private val stats = db.statsDao()
    private val dayMs = 86_400_000L

    private fun dayStart(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val today = dayStart(System.currentTimeMillis())
    private val weekAgo = today - 6 * dayMs

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

    val activity: StateFlow<Activity> = stats.observeReviewTimesSince(0L)
        .map { times -> buildActivity(times) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Activity())

    private fun buildActivity(times: List<Long>): Activity {
        val now = System.currentTimeMillis()
        val todayStart = dayStart(now)
        // Days ago (local time). Rounding fixes 23/25 hour days around summer time.
        val ago = times.map { ((todayStart - dayStart(it)).toDouble() / dayMs).roundToInt() }.filter { it >= 0 }
        val week = IntArray(7)
        ago.forEach { d -> if (d in 0..6) week[6 - d]++ }
        val days = ago.toSortedSet()

        var start = if (0 in days) 0 else 1
        var streak = 0
        while (start in days) { streak++; start++ }

        var best = 0
        var run = 0
        var prev = Int.MIN_VALUE
        days.forEach { d ->
            run = if (d == prev + 1) run + 1 else 1
            if (run > best) best = run
            prev = d
        }

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val todayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val month = IntArray(daysInMonth)
        val monthStart = todayStart - (todayOfMonth - 1) * dayMs
        val c = Calendar.getInstance()
        times.forEach { t ->
            if (t >= monthStart - dayMs) {
                c.timeInMillis = t
                if (c.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && c.get(Calendar.YEAR) == cal.get(Calendar.YEAR)) {
                    month[c.get(Calendar.DAY_OF_MONTH) - 1]++
                }
            }
        }
        val first = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstColumn = Math.floorMod(first.get(Calendar.DAY_OF_WEEK) - first.firstDayOfWeek, 7)

        return Activity(times.size, week.toList(), streak, best, month.toList(), firstColumn, todayOfMonth)
    }
}

// Progress tab = Analytics, like the BONT reference: soft white cards, fully rounded bars,
// a goal bar that fills up, and a month heatmap made of circles.
@Composable
fun ProgressScreen(onOpenLecture: (Long) -> Unit = {}, vm: ProgressViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val a by vm.activity.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        MedoraScreenTitle(stringResource(R.string.b4_an_title))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            BigNumberCard(a.total.toString(), stringResource(R.string.b4_an_total), Modifier.weight(1f))
            BigNumberCard(a.bestStreak.toString(), stringResource(R.string.b4_an_best), Modifier.weight(1f))
        }
        GoalBar(today = s.reviewsToday)
        WeekBarsCard(a)
        HeatmapCard(a)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            MiniStat(s.dueNow.toString(), stringResource(R.string.stat_due_now), 3, Modifier.weight(1f))
            MiniStat(s.totalCards.toString(), stringResource(R.string.stat_total_cards), 0, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            MiniStat(s.learned.toString(), stringResource(R.string.stat_learned), 1, Modifier.weight(1f))
            MiniStat(s.reviewsWeek.toString(), stringResource(R.string.stat_reviews_week), 2, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            MiniStat(s.lastQuiz, stringResource(R.string.stat_last_quiz), 0, Modifier.weight(1f))
            MiniStat(s.quizAverage, stringResource(R.string.stat_quiz_average), 3, Modifier.weight(1f))
        }
        WeakPointsSection(onOpenLecture = onOpenLecture)
    }
}

@Composable
private fun BigNumberCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = Spacing.l, horizontal = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// The whole card is the progress bar. Tap it to change the daily goal.
@Composable
private fun GoalBar(today: Int) {
    val context = LocalContext.current
    var goal by remember { mutableIntStateOf(AppPrefs.dailyGoal(context)) }
    var editing by remember { mutableStateOf(false) }
    val tone = MedoraPastel.tone(1)
    val fraction = (today.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val left = (goal - today).coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(tone.container)
                .clickable(role = Role.Button) { editing = !editing },
        ) {
            if (fraction > 0f) {
                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(MaterialTheme.shapes.medium)
                            .background(tone.strong),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircle(MedoraIcons.Progress, MaterialTheme.colorScheme.surface)
                Spacer(Modifier.width(Spacing.m))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.stat_goal_today), style = MaterialTheme.typography.titleSmall, color = tone.content)
                    Text(
                        stringResource(R.string.b4_an_goal_progress, today, goal),
                        style = MaterialTheme.typography.labelMedium,
                        color = tone.content.copy(alpha = 0.75f),
                    )
                }
                Text(
                    text = if (left == 0) stringResource(R.string.b4_an_goal_done) else stringResource(R.string.b4_an_goal_left, left),
                    style = MaterialTheme.typography.labelMedium,
                    color = tone.content,
                )
            }
        }
        if (editing) {
            MedoraCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stat_goal_set, goal), style = MaterialTheme.typography.bodyMedium)
                MedoraSlider(
                    value = goal.toFloat(),
                    onValueChange = {
                        val g = (it / 5f).roundToInt().times(5).coerceIn(5, 100)
                        if (g != goal) {
                            goal = g
                            AppPrefs.setDailyGoal(context, g)
                        }
                    },
                    valueRange = 5f..100f,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                )
            }
        }
    }
}

@Composable
private fun IconCircle(icon: ImageVector, bg: Color) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Image(imageVector = icon.themed(), contentDescription = null, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun CardHeader(icon: ImageVector, toneIndex: Int, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconCircle(icon, MedoraPastel.tone(toneIndex).container)
        Spacer(Modifier.width(Spacing.m))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Short weekday names. Arabic names are long, so they use the one-letter form.
@Composable
private fun weekdayFormat(): SimpleDateFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { SimpleDateFormat(if (locale.language == "ar") "EEEEE" else "EEE", locale) }
}

@Composable
private fun WeekBarsCard(a: Activity) {
    val format = weekdayFormat()
    val labels = remember(format) {
        (6 downTo 0).map { back ->
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -back)
            format.format(c.time)
        }
    }
    val tone = MedoraPastel.tone(0)
    val max = (a.week.maxOrNull() ?: 0).coerceAtLeast(1)
    MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
        CardHeader(MedoraIcons.Study, 0, stringResource(R.string.b4_an_week_title), stringResource(R.string.stat_week_title))
        Spacer(Modifier.height(Spacing.l))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            a.week.forEachIndexed { i, n ->
                val isToday = i == a.week.lastIndex
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        labels[i],
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(Spacing.s))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (n > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight((n.toFloat() / max).coerceAtLeast(0.2f))
                                    .clip(CircleShape)
                                    .background(tone.strong),
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        n.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// One circle per day of this month. Deeper orange = more reviews. Today has a ring.
@Composable
private fun HeatmapCard(a: Activity) {
    val format = weekdayFormat()
    val headers = remember(format) {
        val c = Calendar.getInstance()
        val firstDay = c.firstDayOfWeek
        (0 until 7).map { i ->
            c.set(Calendar.DAY_OF_WEEK, ((firstDay - 1 + i) % 7) + 1)
            format.format(c.time)
        }
    }
    val dark = LocalMedoraDark.current
    val max = (a.month.maxOrNull() ?: 0).coerceAtLeast(1)
    val cells = a.firstColumn + a.month.size
    val rows = (cells + 6) / 7

    MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
        CardHeader(MedoraIcons.Progress, 2, stringResource(R.string.b4_an_heat_title), stringResource(R.string.b4_an_heat_sub))
        Spacer(Modifier.height(Spacing.l))
        Row(modifier = Modifier.fillMaxWidth()) {
            headers.forEach { h ->
                Text(
                    h,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(Spacing.s))
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = r * 7 + col - a.firstColumn + 1
                    Box(modifier = Modifier.weight(1f).padding(Spacing.xs).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day in 1..a.month.size) {
                            val n = a.month[day - 1]
                            val future = day > a.today
                            val level = when {
                                n == 0 -> 0
                                n * 3 <= max -> 1
                                n * 3 <= max * 2 -> 2
                                else -> 3
                            }
                            val bg = when (level) {
                                0 -> if (future) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
                                1 -> MedoraColors.Orange.copy(alpha = 0.35f)
                                2 -> MedoraColors.Orange.copy(alpha = 0.65f)
                                else -> MedoraColors.Orange
                            }
                            val ink = when {
                                level >= 2 -> MedoraColors.Ink
                                future -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                level == 1 && !dark -> MedoraColors.Ink
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(bg)
                                    .then(
                                        if (day == a.today) {
                                            Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (day == a.today) FontWeight.Bold else FontWeight.Normal,
                                    color = ink,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, toneIndex: Int, modifier: Modifier = Modifier) {
    val tone = MedoraPastel.tone(toneIndex)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(tone.container)
            .padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = tone.content, maxLines = 1)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = tone.content.copy(alpha = 0.72f),
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
