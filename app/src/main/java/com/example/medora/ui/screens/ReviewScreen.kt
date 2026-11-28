package com.example.medora.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.domain.Fsrs
import com.example.medora.ui.components.GlassBox
import com.example.medora.ui.components.ImmersiveGlass
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraImmersive
import com.example.medora.ui.theme.Spacing
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Flashcards on a dark scene with a frosted glass card.
// Show answer, then swipe right (Good) or left (Again). The four buttons stay for precise rating.
@Composable
fun ReviewScreen(
    onOpenLecture: (Long) -> Unit,
    onBack: () -> Unit = {},
    vm: ReviewViewModel = viewModel(),
) {
    val deck = vm.deck
    MedoraImmersive { glass ->
        when {
            !vm.loaded -> Unit
            deck.isEmpty() -> StudyMessage(glass, onBack) {
                Text(
                    text = stringResource(R.string.no_due_cards),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            vm.index >= deck.size -> StudyMessage(glass, onBack) {
                Text(
                    text = stringResource(R.string.done),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = stringResource(R.string.knew_count, vm.knew, deck.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xl))
                MedoraButton(text = stringResource(R.string.restart), onClick = { vm.restart() })
            }
            else -> CardSession(glass, vm, onOpenLecture, onBack)
        }
    }
}

@Composable
private fun CardSession(glass: LayerBackdrop, vm: ReviewViewModel, onOpenLecture: (Long) -> Unit, onBack: () -> Unit) {
    val deck = vm.deck
    val card = deck[vm.index]
    val scope = rememberCoroutineScope()
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val threshold = widthPx * 0.28f
    // New offset for every card, so the next card starts in the middle.
    val offsetX = remember(vm.index) { Animatable(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(Spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassBackButton(glass, onBack)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.progress_of, vm.index + 1, deck.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(Spacing.l))
        SceneProgress(vm.index.toFloat() / deck.size, Color(0xFF90E0EF))
        Spacer(Modifier.height(Spacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.review_reviewed, vm.index),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = stringResource(R.string.review_total, deck.size),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(Spacing.l))

        GlassBox(
            backdrop = glass,
            shape = RoundedCornerShape(28.dp),
            tint = ImmersiveGlass.frost,
            fallback = ImmersiveGlass.frostSolid,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer { alpha = 1f - (abs(offsetX.value) / widthPx).coerceIn(0f, 0.6f) }
                .pointerInput(vm.index, vm.revealed) {
                    if (vm.revealed) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val x = offsetX.value
                                    if (abs(x) > threshold) {
                                        offsetX.animateTo(if (x > 0) widthPx else -widthPx, tween(160))
                                        vm.answer(if (x > 0) Fsrs.Rating.Good else Fsrs.Rating.Again)
                                    } else {
                                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            },
                            onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                            onHorizontalDrag = { change, delta ->
                                change.consume()
                                scope.launch { offsetX.snapTo(offsetX.value + delta) }
                            },
                        )
                    }
                },
            onClick = if (vm.revealed) null else ({ vm.reveal() }),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = card.front,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (vm.revealed) {
                    Spacer(Modifier.height(Spacing.l))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(ImmersiveGlass.line))
                    Spacer(Modifier.height(Spacing.l))
                    Text(
                        text = card.back,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    if (card.source.isNotBlank() && card.source != card.back) {
                        Spacer(Modifier.height(Spacing.m))
                        Text(
                            text = stringResource(R.string.source_label) + ": " + card.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    vm.pages[card.id]?.let { page ->
                        Text(
                            text = stringResource(R.string.page_label, page),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                    TextButton(onClick = { onOpenLecture(card.lectureId) }) {
                        Text(stringResource(R.string.open_lecture))
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.l))
        if (!vm.revealed) {
            MedoraButton(
                text = stringResource(R.string.show_answer),
                onClick = { vm.reveal() },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = stringResource(R.string.review_swipe_hint),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.m))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                RatingChip(glass, stringResource(R.string.rate_again), Modifier.weight(1f)) { vm.answer(Fsrs.Rating.Again) }
                RatingChip(glass, stringResource(R.string.rate_hard), Modifier.weight(1f)) { vm.answer(Fsrs.Rating.Hard) }
                RatingChip(glass, stringResource(R.string.rate_good), Modifier.weight(1f)) { vm.answer(Fsrs.Rating.Good) }
                RatingChip(glass, stringResource(R.string.rate_easy), Modifier.weight(1f)) { vm.answer(Fsrs.Rating.Easy) }
            }
        }
    }
}

@Composable
private fun RatingChip(glass: LayerBackdrop, label: String, modifier: Modifier, onClick: () -> Unit) {
    GlassBox(
        backdrop = glass,
        shape = CircleShape,
        tint = ImmersiveGlass.frost,
        fallback = ImmersiveGlass.frostSolid,
        onClick = onClick,
        modifier = modifier.height(48.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

// Round glass back button with a plain chevron (flips in right-to-left languages).
@Composable
internal fun GlassBackButton(glass: LayerBackdrop, onBack: () -> Unit) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val label = stringResource(R.string.back)
    GlassBox(
        backdrop = glass,
        shape = CircleShape,
        tint = ImmersiveGlass.frost,
        fallback = ImmersiveGlass.frostSolid,
        onClick = onBack,
        modifier = Modifier.size(44.dp),
    ) {
        Text(
            text = if (rtl) "›" else "‹",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

// Thin rounded progress bar on the dark scene.
@Composable
internal fun SceneProgress(fraction: Float, fill: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(fill),
        )
    }
}

// Centered message (empty, finished) with a back button, on the dark scene.
@Composable
internal fun StudyMessage(glass: LayerBackdrop, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(Spacing.l)) {
        GlassBackButton(glass, onBack)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(Spacing.l),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

@Composable
internal fun CenterText(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
