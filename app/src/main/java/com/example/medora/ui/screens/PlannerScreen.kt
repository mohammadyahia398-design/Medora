package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.MedoraDatabase
import com.example.medora.data.StudyTask
import com.example.medora.ui.components.MedoraScreenTitle
import com.example.medora.ui.components.RoundIconButton
import com.example.medora.ui.icons.MedoraIcons
import com.example.medora.ui.icons.themed
import com.example.medora.ui.theme.Dimens
import com.example.medora.ui.theme.MedoraPastel
import com.example.medora.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

private const val DAY_MS = 86_400_000L
private val MINUTE_CHOICES = listOf(15, 25, 45, 60)

// Start of the day (local time) for a moment.
internal fun dayOf(t: Long): Long = Calendar.getInstance().apply {
    timeInMillis = t
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

// Next day start. Calendar handles summer time (23/25 hour days).
private fun nextDay(day: Long, by: Int = 1): Long = Calendar.getInstance().apply {
    timeInMillis = day
    add(Calendar.DAY_OF_YEAR, by)
}.timeInMillis

private fun weekStart(day: Long): Long = Calendar.getInstance().apply {
    timeInMillis = day
    val back = Math.floorMod(get(Calendar.DAY_OF_WEEK) - firstDayOfWeek, 7)
    add(Calendar.DAY_OF_YEAR, -back)
}.timeInMillis

class PlannerViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = MedoraDatabase.get(app).taskDao()

    val tasks: StateFlow<List<StudyTask>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, subject: String, day: Long, minutes: Int) {
        val t = title.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { dao.insert(StudyTask(title = t, subject = subject.trim(), day = day, minutes = minutes)) }
    }

    fun toggle(task: StudyTask) {
        viewModelScope.launch {
            dao.setDone(task.id, !task.done, if (task.done) 0L else System.currentTimeMillis())
        }
    }

    fun delete(task: StudyTask) {
        viewModelScope.launch { dao.delete(task.id) }
    }
}

// Planner tab, like the lavender planner reference: big title, capsule tabs, a week strip of
// circles, a weekly progress card with a ring, and one soft pastel card per task.
@Composable
fun PlannerScreen(onFocus: (StudyTask) -> Unit, vm: PlannerViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsState()
    val today = remember { dayOf(System.currentTimeMillis()) }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableLongStateOf(today) }
    var adding by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<StudyTask?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    val dayTitle = remember(locale) { SimpleDateFormat("EEEE d MMM", locale) }

    val week0 = weekStart(today)
    val weekEnd = nextDay(week0, 7)
    val weekTasks = tasks.filter { it.day in week0 until weekEnd }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MedoraScreenTitle(stringResource(R.string.b5_title), modifier = Modifier.weight(1f))
            RoundIconButton(MedoraIcons.Plus, stringResource(R.string.b5_add), onClick = { adding = true })
        }
        PillTabs(
            titles = listOf(stringResource(R.string.b5_tab_day), stringResource(R.string.b5_tab_week), stringResource(R.string.b5_tab_all)),
            selected = tab,
            onSelect = { tab = it },
        )
        WeekStrip(
            start = week0,
            today = today,
            selected = selected,
            busyDays = tasks.map { it.day }.toSet(),
            onSelect = { selected = it; tab = 0 },
        )
        WeekProgressCard(done = weekTasks.count { it.done }, total = weekTasks.size)

        when (tab) {
            0 -> {
                val overdue = if (selected == today) tasks.filter { !it.done && it.day < today } else emptyList()
                val list = tasks.filter { it.day == selected }
                PlanSectionTitle(dayTitle.format(Date(selected)))
                if (overdue.isEmpty() && list.isEmpty()) EmptyHint()
                if (overdue.isNotEmpty()) {
                    Text(
                        stringResource(R.string.b5_overdue),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    overdue.forEach { t -> TaskCard(t, { vm.toggle(t) }, { onFocus(t) }, { toDelete = t }) }
                }
                list.forEach { t -> TaskCard(t, { vm.toggle(t) }, { onFocus(t) }, { toDelete = t }) }
            }
            else -> {
                val list = if (tab == 1) weekTasks else tasks
                if (list.isEmpty()) EmptyHint()
                list.groupBy { it.day }.forEach { (day, items) ->
                    PlanSectionTitle(dayTitle.format(Date(day)))
                    items.forEach { t -> TaskCard(t, { vm.toggle(t) }, { onFocus(t) }, { toDelete = t }) }
                }
            }
        }
    }

    if (adding) {
        AddTaskDialog(
            selected = selected,
            today = today,
            dayLabel = { dayTitle.format(Date(it)) },
            onDismiss = { adding = false },
            onAdd = { title, subject, day, minutes ->
                vm.add(title, subject, day, minutes)
                adding = false
            },
        )
    }
    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.b5_delete_title)) },
            text = { Text(t.title) },
            confirmButton = {
                TextButton(onClick = { vm.delete(t); toDelete = null }) {
                    Text(stringResource(R.string.b5_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.b5_cancel)) } },
        )
    }
}

@Composable
private fun PlanSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = Spacing.s),
    )
}

@Composable
private fun EmptyHint() {
    Text(
        text = stringResource(R.string.b5_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.card),
    )
}

// Capsule tabs: the chosen one is filled with the main color.
@Composable
private fun PillTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        titles.forEachIndexed { i, title ->
            val on = i == selected
            Box(
                modifier = Modifier
                    .heightIn(min = Dimens.roundButton)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .padding(horizontal = Spacing.l),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

// This week as seven circles. Chosen day is filled, today has a ring, a dot marks days with tasks.
@Composable
private fun WeekStrip(start: Long, today: Long, selected: Long, busyDays: Set<Long>, onSelect: (Long) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val name = remember(locale) { SimpleDateFormat(if (locale.language == "ar") "EEEEE" else "EEE", locale) }
    val num = remember(locale) { SimpleDateFormat("d", locale) }
    val days = (0 until 7).map { nextDay(start, it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = Spacing.m, horizontal = Spacing.s),
    ) {
        days.forEach { d ->
            val on = d == selected
            Column(
                modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable { onSelect(d) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(name.format(Date(d)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .then(
                            if (d == today && !on) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CircleShape)
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        num.format(Date(d)),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (d in busyDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                )
            }
        }
    }
}

// Lilac card with a rounded ring showing how much of this week's plan is done.
@Composable
private fun WeekProgressCard(done: Int, total: Int) {
    val tone = MedoraPastel.tone(1)
    val fraction = if (total > 0) done.toFloat() / total else 0f
    val ring = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(tone.container)
            .padding(Spacing.card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.b5_week_progress), style = MaterialTheme.typography.titleMedium, color = tone.content)
            Text(
                stringResource(R.string.b5_week_count, done, total),
                style = MaterialTheme.typography.labelMedium,
                color = tone.content.copy(alpha = 0.75f),
            )
        }
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = 8.dp.toPx()
                val inset = w / 2f
                val arcSize = Size(size.width - w, size.height - w)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
                if (fraction > 0f) {
                    drawArc(ring, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
                }
            }
            Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = tone.content)
        }
    }
}

// One task: subject capsule and time on top, the title, then the done circle and a focus button.
// Long press deletes.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(task: StudyTask, onToggle: () -> Unit, onFocus: () -> Unit, onDelete: () -> Unit) {
    val tone = MedoraPastel.tone(Math.floorMod(task.id, 4L).toInt())
    val alpha = if (task.done) 0.6f else 1f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(tone.container)
            .combinedClickable(onClick = onToggle, onLongClick = onDelete)
            .padding(Spacing.card),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (task.subject.isNotBlank()) {
                Text(
                    task.subject,
                    style = MaterialTheme.typography.labelMedium,
                    color = tone.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = Spacing.m, vertical = Spacing.xs),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.b5_minutes, task.minutes) +
                    if (task.focusMinutes > 0) "  ·  " + stringResource(R.string.b5_focused, task.focusMinutes) else "",
                style = MaterialTheme.typography.labelMedium,
                color = tone.content.copy(alpha = 0.75f),
                maxLines = 1,
            )
        }
        Text(
            task.title,
            style = MaterialTheme.typography.titleMedium,
            color = tone.content.copy(alpha = alpha),
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(done = task.done, onClick = onToggle)
            Spacer(Modifier.width(Spacing.s))
            Text(
                stringResource(if (task.done) R.string.b5_done else R.string.b5_todo),
                style = MaterialTheme.typography.labelLarge,
                color = tone.content.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f),
            )
            if (!task.done) PlayCircle(onClick = onFocus)
        }
    }
}

@Composable
private fun CheckCircle(done: Boolean, onClick: () -> Unit) {
    val fill = MaterialTheme.colorScheme.primary
    val mark = MaterialTheme.colorScheme.onPrimary
    val ring = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(Dimens.roundButton)
            .clip(CircleShape)
            .background(if (done) fill else MaterialTheme.colorScheme.surface)
            .then(if (done) Modifier else Modifier.border(BorderStroke(1.5.dp, ring), CircleShape))
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val p = Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.52f)
                    lineTo(size.width * 0.42f, size.height * 0.78f)
                    lineTo(size.width * 0.88f, size.height * 0.25f)
                }
                drawPath(p, mark, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

// White circle with a play mark: starts the Focus timer on this task.
@Composable
private fun PlayCircle(onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    val description = stringResource(R.string.b5_start_focus)
    Box(
        modifier = Modifier
            .size(Dimens.roundButton)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val p = Path().apply {
                moveTo(size.width * 0.2f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(size.width * 0.2f, size.height)
                close()
            }
            drawPath(p, ink)
        }
    }
}

@Composable
private fun AddTaskDialog(
    selected: Long,
    today: Long,
    dayLabel: (Long) -> String,
    onDismiss: () -> Unit,
    onAdd: (String, String, Long, Int) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(25) }
    var day by remember { mutableLongStateOf(selected) }
    val dayChoices = listOf(today, nextDay(today), selected).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.b5_new_task)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.b5_field_title)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(stringResource(R.string.b5_field_subject)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.b5_field_time), style = MaterialTheme.typography.labelLarge)
                ChoiceRow(MINUTE_CHOICES.map { it to stringResource(R.string.b5_minutes, it) }, minutes) { minutes = it }
                Text(stringResource(R.string.b5_field_day), style = MaterialTheme.typography.labelLarge)
                ChoiceRow(
                    dayChoices.map { d ->
                        d to when (d) {
                            today -> stringResource(R.string.b5_today)
                            nextDay(today) -> stringResource(R.string.b5_tomorrow)
                            else -> dayLabel(d)
                        }
                    },
                    day,
                ) { day = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(title, subject, day, minutes) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.b5_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.b5_cancel)) } },
    )
}

// Small capsules to pick one value.
@Composable
private fun <T> ChoiceRow(items: List<Pair<T, String>>, chosen: T, onPick: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items.forEach { (value, label) ->
            val on = value == chosen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.touch - Spacing.s)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPick(value) }
                    .padding(horizontal = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
