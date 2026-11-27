package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.MedoraDatabase
import com.example.medora.settings.AppPrefs
import com.example.medora.ui.components.MedoraCard
import com.example.medora.ui.components.MedoraSlider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import kotlin.math.roundToInt

// Last 7 days (oldest first), streak of days in a row with reviews, and reviews today.
data class StudyWeek(val days: List<Int> = List(7) { 0 }, val streak: Int = 0, val today: Int = 0)

class StudyWeekViewModel(app: Application) : AndroidViewModel(app) {
    private val dayMs = 86_400_000L

    private fun dayStart(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Day number (local time). Calendar handles summer time; rounding fixes 23/25 h days.
    private fun dayIndex(t: Long, today: Long): Int = ((today - dayStart(t)).toDouble() / dayMs).roundToInt()

    val state: StateFlow<StudyWeek> = MedoraDatabase.get(app).statsDao()
        .observeReviewTimesSince(0L)
        .map { times ->
            val today = dayStart(System.currentTimeMillis())
            val ago = times.map { dayIndex(it, today) }.filter { it >= 0 }.toSet()
            val counts = IntArray(7)
            times.forEach { t -> val d = dayIndex(t, today); if (d in 0..6) counts[6 - d]++ }
            // Streak counts from today, or from yesterday if nothing yet today.
            var start = if (0 in ago) 0 else 1
            var streak = 0
            while (start in ago) { streak++; start++ }
            StudyWeek(counts.toList(), streak, counts[6])
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyWeek())
}

@Composable
fun StudyWeekCard(vm: StudyWeekViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    var goal by remember { mutableIntStateOf(AppPrefs.dailyGoal(context)) }
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    MedoraCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.stat_week_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
            val max = (s.days.maxOrNull() ?: 0).coerceAtLeast(goal).coerceAtLeast(1)
            val slot = size.width / 7f
            val w = slot * 0.6f
            s.days.forEachIndexed { i, n ->
                val col = if (rtl) 6 - i else i
                val x = col * slot + (slot - w) / 2f
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 2f))
                val h = size.height * n / max
                if (h > 0f) drawRoundRect(bar, Offset(x, size.height - h), Size(w, h), CornerRadius(w / 2f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.stat_streak), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.stat_streak_days, s.streak), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.stat_goal_today), style = MaterialTheme.typography.titleMedium)
            Text("${s.today} / $goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (s.today.toFloat() / goal).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.stat_goal_set, goal), style = MaterialTheme.typography.bodySmall)
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
