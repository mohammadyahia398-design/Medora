package com.example.medora.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.medora.R
import com.example.medora.ui.components.GlassBar
import com.example.medora.ui.theme.LocalMedoraDark
import com.example.medora.ui.theme.MedoraColors
import com.example.medora.ui.theme.iconTint
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.medora.ui.icons.MedoraIcons
import com.example.medora.ui.screens.ExamScreen
import com.example.medora.ui.screens.HomeScreen
import com.example.medora.ui.screens.LectureDetailScreen
import com.example.medora.ui.screens.LibraryScreen
import com.example.medora.ui.screens.NoteEditorScreen
import com.example.medora.ui.screens.PracticeScreen
import com.example.medora.ui.screens.ProgressScreen
import com.example.medora.ui.screens.QuizScreen
import com.example.medora.ui.screens.ReviewScreen
import com.example.medora.ui.screens.SettingsScreen
import com.example.medora.ui.screens.SourcesScreen
import com.example.medora.ui.screens.StudyScreen

private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_LECTURE = "lecture/{id}"
private const val ROUTE_REVIEW = "review"
private const val ROUTE_QUIZ = "quiz"
private const val ROUTE_NOTE = "note/{id}"
private const val ROUTE_EXAM = "exam"
private const val ROUTE_SOURCES = "sources"
private const val ROUTE_PRACTICE = "practice/{mode}"

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Home("home", R.string.tab_home, MedoraIcons.Home),
    Library("library", R.string.tab_library, MedoraIcons.Library),
    Study("study", R.string.tab_study, MedoraIcons.Study),
    Progress("progress", R.string.tab_progress, MedoraIcons.Progress),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedoraApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val tab = Tab.values().firstOrNull { it.route == route }
    val isSettings = route == ROUTE_SETTINGS
    val isLecture = route == ROUTE_LECTURE
    val isReview = route == ROUTE_REVIEW
    val isQuiz = route == ROUTE_QUIZ
    val isNote = route == ROUTE_NOTE
    val isExam = route == ROUTE_EXAM
    val isSources = route == ROUTE_SOURCES
    val isPractice = route == ROUTE_PRACTICE
    val isSub = isSettings || isLecture || isReview || isQuiz || isNote || isExam || isSources || isPractice

    val titleRes = when {
        isSettings -> R.string.settings
        isLecture -> R.string.lecture_details
        isReview -> R.string.review_title
        isQuiz -> R.string.quiz_title
        isNote -> R.string.edit_note
        isExam -> R.string.exam_title
        isSources -> R.string.sources_title
        isPractice -> R.string.practice_title
        tab != null -> tab.label
        else -> R.string.app_name
    }

    val backdrop = rememberLayerBackdrop()
    val dark = LocalMedoraDark.current

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            if (dark) Color(0xFF1D1530) else Color(0xFFF1E6FB),
                        ),
                    ),
                ),
            )
            Scaffold(
                containerColor = Color.Transparent,
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = Tab.Home.route,
                    modifier = Modifier
                        .padding(padding)
                        .padding(top = 64.dp, bottom = if (tab != null) 88.dp else 0.dp),
                ) {
                    composable(Tab.Home.route) {
                        HomeScreen(
                            onReview = { navController.navigate(ROUTE_REVIEW) },
                            onOpenLecture = { navController.navigate("lecture/$it") },
                        )
                    }
                    composable(Tab.Library.route) {
                        LibraryScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(Tab.Study.route) {
                        StudyScreen(
                            onReview = { navController.navigate(ROUTE_REVIEW) },
                            onQuiz = { navController.navigate(ROUTE_QUIZ) },
                            onExam = { navController.navigate(ROUTE_EXAM) },
                            onSources = { navController.navigate(ROUTE_SOURCES) },
                            onPractice = { navController.navigate("practice/$it") },
                        )
                    }
                    composable(Tab.Progress.route) {
                        ProgressScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(ROUTE_SETTINGS) { SettingsScreen() }
                    composable(ROUTE_REVIEW) {
                        ReviewScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(ROUTE_EXAM) {
                        ExamScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(ROUTE_SOURCES) { SourcesScreen() }
                    composable(
                        ROUTE_PRACTICE,
                        arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                    ) { entry ->
                        PracticeScreen(
                            mode = entry.arguments?.getString("mode").orEmpty(),
                            onOpenLecture = { navController.navigate("lecture/$it") },
                        )
                    }
                    composable(ROUTE_QUIZ) {
                        QuizScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(
                        ROUTE_LECTURE,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val lid = entry.arguments?.getLong("id") ?: 0L
                        LectureDetailScreen(id = lid, onEdit = { navController.navigate("note/$lid") })
                    }
                    composable(
                        ROUTE_NOTE,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        NoteEditorScreen(
                            id = entry.arguments?.getLong("id") ?: 0L,
                            onDone = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
        GlassBar(
            backdrop = backdrop,
            height = 56.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isSub) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Image(
                            imageVector = MedoraIcons.Back,
                            contentDescription = stringResource(R.string.back),
                            colorFilter = iconTint(),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (!isSub) {
                    IconButton(onClick = { navController.navigate(ROUTE_SETTINGS) }) {
                        Image(
                            imageVector = MedoraIcons.Settings,
                            contentDescription = stringResource(R.string.settings),
                            colorFilter = iconTint(),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
        if (tab != null) {
            GlassBar(
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            ) {
                Tab.values().forEach { item ->
                    GlassTabItem(
                        tab = item,
                        selected = item == tab,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.GlassTabItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // 0 -> 1 when selected. The bouncy spring makes the pill and the icon "pop".
    val sel by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "tabSelected",
    )
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tabPress",
    )

    // A short wiggle when the tab becomes selected (not on first start).
    val wiggle = remember { Animatable(0f) }
    val firstRun = remember { booleanArrayOf(true) }
    LaunchedEffect(selected) {
        if (firstRun[0]) {
            firstRun[0] = false
            return@LaunchedEffect
        }
        if (selected) {
            wiggle.animateTo(-14f, tween(90))
            wiggle.animateTo(10f, tween(110))
            wiggle.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            )
        }
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(width = 56.dp, height = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 0.3f + 0.7f * sel
                        alpha = sel.coerceIn(0f, 1f)
                    }
                    .background(MedoraColors.Purple.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            )
            Image(
                imageVector = tab.icon,
                contentDescription = null,
                colorFilter = iconTint(),
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        val scale = (1f + 0.15f * sel) * press
                        scaleX = scale
                        scaleY = scale
                        translationY = -2.dp.toPx() * sel
                        rotationZ = wiggle.value
                        alpha = (0.55f + 0.45f * sel).coerceIn(0f, 1f)
                    },
            )
        }
        Text(
            text = stringResource(tab.label),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.graphicsLayer {
                alpha = (0.55f + 0.45f * sel).coerceIn(0f, 1f)
            },
        )
    }
}
