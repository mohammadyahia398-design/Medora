package com.example.medora.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.ui.theme.Dimens
import com.example.medora.ui.theme.LocalMedoraDark
import com.example.medora.ui.theme.MedoraColors
import com.example.medora.ui.theme.Spacing
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MIN_CARDS_FOR_QUIZ = 4
private const val HOUR_MS = 3_600_000f

// Study tab = Focus mode (Pomodoro), like the Tiimo reference:
// two small capsules on top, a big title, one thick rounded ring, one main button.
// The old study actions (review, quiz, exam, practice, mistakes) live in the "Study tools" menu.
@Composable
fun StudyScreen(
    onReview: () -> Unit,
    onQuiz: () -> Unit,
    onExam: () -> Unit = {},
    onSources: () -> Unit = {},
    onPractice: (String) -> Unit = {},
    onMistakes: () -> Unit = {},
    vm: CardsViewModel = viewModel(),
    focus: FocusViewModel = viewModel(),
) {
    val cards by vm.cards.collectAsState()
    val due by vm.dueCount.collectAsState()
    val aiQuestions by vm.aiQuestionCount.collectAsState()
    val mistakes by vm.mistakeCount.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    val quizReady = cards.size >= MIN_CARDS_FOR_QUIZ || aiQuestions > 0
    val isBreak = focus.phase == FocusViewModel.Phase.Break

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenPadding, vertical = Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SoftPill(text = stringResource(R.string.start_sources), onClick = onSources)
            Box {
                SoftPill(text = stringResource(R.string.b4_tools), onClick = { menuOpen = true })
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    val go: (() -> Unit) -> Unit = { action -> menuOpen = false; action() }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.start_review) + if (due > 0) " ($due)" else "") },
                        onClick = { go(onReview) },
                        enabled = due > 0,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.start_quiz)) },
                        onClick = { go(onQuiz) },
                        enabled = quizReady,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.start_exam)) },
                        onClick = { go(onExam) },
                        enabled = quizReady,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.practice_tf_title)) },
                        onClick = { go { onPractice(PRACTICE_TF) } },
                        enabled = cards.isNotEmpty(),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.practice_short_title)) },
                        onClick = { go { onPractice(PRACTICE_SHORT) } },
                        enabled = cards.isNotEmpty(),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.practice_weak_title)) },
                        onClick = { go { onPractice(PRACTICE_WEAK) } },
                        enabled = cards.isNotEmpty(),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.start_mistakes, mistakes)) },
                        onClick = { go(onMistakes) },
                        enabled = mistakes > 0,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        Text(
            text = stringResource(if (isBreak) R.string.b4_break_title else R.string.b4_focus_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))

        FocusDial(focus = focus, modifier = Modifier.fillMaxWidth(0.82f))

        Spacer(Modifier.height(Spacing.s))
        Text(
            text = when {
                isBreak -> stringResource(R.string.b4_break_hint)
                focus.idle -> stringResource(R.string.b4_dial_hint)
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))

        StartButton(
            text = stringResource(
                when {
                    focus.running -> R.string.b4_pause
                    focus.idle -> R.string.b4_start
                    else -> R.string.b4_resume
                },
            ),
            running = focus.running,
            onClick = { focus.toggle() },
        )

        if (!focus.idle || isBreak) {
            Spacer(Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                TextButton(onClick = { focus.reset() }) { Text(stringResource(R.string.b4_reset)) }
                TextButton(onClick = { focus.finishPhase() }) { Text(stringResource(R.string.b4_skip)) }
            }
        }
        if (focus.sessionsDone > 0) {
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(R.string.b4_sessions, focus.sessionsDone),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

// The ring. Thick and fully rounded, with fine lines on the filled part (like the reference).
// Before start: drag or tap the ring to pick 5-60 minutes. While running: shows the time left.
// The clock face is not mirrored in right-to-left languages (60 stays on top, 15 on the right).
@Composable
private fun FocusDial(focus: FocusViewModel, modifier: Modifier = Modifier) {
    val isBreak = focus.phase == FocusViewModel.Phase.Break
    val dark = LocalMedoraDark.current
    val fill = if (isBreak) MedoraColors.Teal else MaterialTheme.colorScheme.primary
    val track = fill.copy(alpha = if (dark) 0.22f else 0.16f)
    val tick = if (dark) MedoraColors.Navy.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.45f)
    val knob = MaterialTheme.colorScheme.surface
    val labelStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val canEdit = focus.idle && !isBreak

    val fraction = (
        if (isBreak) focus.remainingMs.toFloat() / focus.totalMs.coerceAtLeast(1L).toFloat()
        else focus.remainingMs.toFloat() / HOUR_MS
        ).coerceIn(0f, 1f)

    val description = stringResource(R.string.b4_dial_desc, focus.focusMinutes)

    val pick: (Offset, Float, Float) -> Unit = { pos, w, h ->
        val dx = pos.x - w / 2f
        val dy = pos.y - h / 2f
        var deg = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat()
        if (deg < 0f) deg += 360f
        val raw = deg / 6f
        var m = (raw / FocusViewModel.STEP).roundToInt() * FocusViewModel.STEP
        if (m == 0) m = if (raw > 30f) FocusViewModel.MAX_FOCUS else FocusViewModel.MIN_FOCUS
        focus.setMinutes(m)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = description }
            .pointerInput(canEdit) {
                if (canEdit) {
                    detectTapGestures { pick(it, size.width.toFloat(), size.height.toFloat()) }
                }
            }
            .pointerInput(canEdit) {
                if (canEdit) {
                    detectDragGestures(
                        onDragStart = { pick(it, size.width.toFloat(), size.height.toFloat()) },
                    ) { change, _ ->
                        change.consume()
                        pick(change.position, size.width.toFloat(), size.height.toFloat())
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.13f
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            drawCircle(color = track, radius = radius, center = center, style = Stroke(width = stroke))
            val sweep = 360f * fraction
            if (sweep > 0f) {
                drawArc(
                    color = fill,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // Fine lines along the filled part, kept away from the rounded ends.
                val inner = radius - stroke * 0.22f
                val outer = radius + stroke * 0.22f
                var a = 6f
                while (a < sweep - 6f) {
                    val r = Math.toRadians((a - 90f).toDouble())
                    val c = cos(r).toFloat()
                    val s = sin(r).toFloat()
                    drawLine(
                        color = tick,
                        start = Offset(center.x + c * inner, center.y + s * inner),
                        end = Offset(center.x + c * outer, center.y + s * outer),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    a += 3.5f
                }
                if (canEdit) {
                    val r = Math.toRadians((sweep - 90f).toDouble())
                    drawCircle(
                        color = knob,
                        radius = stroke * 0.2f,
                        center = Offset(center.x + cos(r).toFloat() * radius, center.y + sin(r).toFloat() * radius),
                    )
                }
            }

            // 60 / 15 / 30 / 45 inside the ring.
            val labelR = radius - stroke / 2f - 20.dp.toPx()
            listOf("60" to -PI / 2.0, "15" to 0.0, "30" to PI / 2.0, "45" to PI).forEach { (t, ang) ->
                val layout = measurer.measure(t, labelStyle)
                val p = Offset(
                    center.x + cos(ang).toFloat() * labelR - layout.size.width / 2f,
                    center.y + sin(ang).toFloat() * labelR - layout.size.height / 2f,
                )
                drawText(textLayoutResult = layout, topLeft = p)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (canEdit) focus.focusMinutes.toString() else clock(focus.remainingMs),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    when {
                        canEdit -> R.string.b4_minutes
                        isBreak -> R.string.b4_break_title
                        else -> R.string.b4_focus_title
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

private fun clock(ms: Long): String {
    val total = ((ms + 999L) / 1000L).toInt()
    return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
}

// Small white capsule on top of the screen.
@Composable
private fun SoftPill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = Dimens.roundButton)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.l),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

// The one main button: soft capsule with a play or pause mark.
@Composable
private fun StartButton(text: String, running: Boolean, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier = Modifier
            .widthIn(min = 168.dp)
            .heightIn(min = Dimens.buttonHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = ink)
        Spacer(Modifier.width(Spacing.m))
        Canvas(modifier = Modifier.size(16.dp)) {
            if (running) {
                val w = size.width * 0.3f
                drawRoundRect(color = ink, topLeft = Offset(size.width * 0.1f, 0f), size = Size(w, size.height), cornerRadius = CornerRadius(w / 3f))
                drawRoundRect(color = ink, topLeft = Offset(size.width * 0.6f, 0f), size = Size(w, size.height), cornerRadius = CornerRadius(w / 3f))
            } else {
                val p = Path().apply {
                    moveTo(size.width * 0.15f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(size.width * 0.15f, size.height)
                    close()
                }
                drawPath(path = p, color = ink)
            }
        }
    }
}
