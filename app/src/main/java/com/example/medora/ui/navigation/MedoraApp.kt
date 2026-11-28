package com.example.medora.ui.navigation

import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.medora.settings.AppPrefs
import com.example.medora.ui.screens.GoalSetupScreen
import com.example.medora.ui.screens.OnboardingScreen
import com.example.medora.ui.screens.SplashScreen
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.medora.R
import com.example.medora.ui.components.MedoraBottomBar
import com.example.medora.ui.components.MedoraBottomItem
import com.example.medora.ui.components.MedoraTopBar
import com.example.medora.ui.theme.Dimens
import com.example.medora.ui.glass.LiquidBottomTab
import com.example.medora.ui.glass.LiquidBottomTabs
import com.example.medora.ui.theme.LocalMedoraDark
import com.example.medora.ui.theme.MedoraColors
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.medora.ui.icons.MedoraIcons
import com.example.medora.ui.icons.themed
import com.example.medora.ui.screens.ExamScreen
import com.example.medora.ui.screens.HomeScreen
import com.example.medora.ui.screens.LectureDetailScreen
import com.example.medora.ui.screens.LibraryScreen
import com.example.medora.ui.screens.MistakesScreen
import com.example.medora.ui.screens.NoteEditorScreen
import com.example.medora.ui.screens.PackDetailScreen
import com.example.medora.ui.screens.PacksScreen
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
private const val ROUTE_MISTAKES = "mistakes"
private const val ROUTE_PRACTICE = "practice/{mode}"
private const val ROUTE_PACKS = "packs"
private const val ROUTE_PACK = "pack/{id}"
private const val ROUTE_SPLASH = "splash"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_GOAL = "goal"

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
    val isMistakes = route == ROUTE_MISTAKES
    val isPacks = route == ROUTE_PACKS || route == ROUTE_PACK
    // Splash, onboarding, goal, and the dark study screens (cards, quiz) draw their own top area.
    val isFlow = route == ROUTE_SPLASH || route == ROUTE_ONBOARDING || route == ROUTE_GOAL || isReview || isQuiz
    // The Focus screen (Study tab) has its own capsules on top, so no top bar there.
    val noTopBar = isFlow || tab == Tab.Study
    val isSub = isSettings || isLecture || isReview || isQuiz || isNote || isExam || isSources || isPractice || isMistakes || isPacks

    val titleRes = when {
        isSettings -> R.string.settings
        isLecture -> R.string.lecture_details
        isReview -> R.string.review_title
        isQuiz -> R.string.quiz_title
        isNote -> R.string.edit_note
        isExam -> R.string.exam_title
        isSources -> R.string.sources_title
        isPractice -> R.string.practice_title
        isMistakes -> R.string.mistakes_title
        isPacks -> R.string.packs_title
        tab != null -> tab.label
        else -> R.string.app_name
    }

    val backdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_SPLASH,
                    modifier = Modifier
                        .padding(padding)
                        .padding(top = if (noTopBar) 0.dp else Dimens.topBarSpace, bottom = if (tab != null) Dimens.bottomBarSpace else 0.dp),
                ) {
                    composable(ROUTE_SPLASH) {
                        val ctx = LocalContext.current
                        SplashScreen(onDone = {
                            val next = if (AppPrefs.onboarded(ctx)) Tab.Home.route else ROUTE_ONBOARDING
                            navController.navigate(next) { popUpTo(ROUTE_SPLASH) { inclusive = true } }
                        })
                    }
                    composable(ROUTE_ONBOARDING) {
                        OnboardingScreen(onFinish = { navController.navigate(ROUTE_GOAL) })
                    }
                    composable(ROUTE_GOAL) {
                        GoalSetupScreen(onDone = {
                            navController.navigate(Tab.Home.route) {
                                popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                            }
                        })
                    }
                    composable(Tab.Home.route) {
                        HomeScreen(
                            onReview = { navController.navigate(ROUTE_REVIEW) },
                            onOpenLecture = { navController.navigate("lecture/$it") },
                        )
                    }
                    composable(Tab.Library.route) {
                        LibraryScreen(
                            onOpenLecture = { navController.navigate("lecture/$it") },
                            onOpenPacks = { navController.navigate(ROUTE_PACKS) },
                        )
                    }
                    composable(Tab.Study.route) {
                        StudyScreen(
                            onReview = { navController.navigate(ROUTE_REVIEW) },
                            onQuiz = { navController.navigate(ROUTE_QUIZ) },
                            onExam = { navController.navigate(ROUTE_EXAM) },
                            onSources = { navController.navigate(ROUTE_SOURCES) },
                            onPractice = { navController.navigate("practice/$it") },
                            onMistakes = { navController.navigate(ROUTE_MISTAKES) },
                        )
                    }
                    composable(Tab.Progress.route) {
                        ProgressScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(ROUTE_SETTINGS) { SettingsScreen() }
                    composable(ROUTE_REVIEW) {
                        ReviewScreen(
                            onOpenLecture = { navController.navigate("lecture/$it") },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(ROUTE_EXAM) {
                        ExamScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(ROUTE_SOURCES) { SourcesScreen() }
                    composable(ROUTE_MISTAKES) {
                        MistakesScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(
                        ROUTE_PRACTICE,
                        arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                    ) { entry ->
                        PracticeScreen(
                            mode = entry.arguments?.getString("mode").orEmpty(),
                            onOpenLecture = { navController.navigate("lecture/$it") },
                        )
                    }
                    composable(ROUTE_PACKS) {
                        PacksScreen(onOpenPack = { navController.navigate("pack/$it") })
                    }
                    composable(
                        ROUTE_PACK,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        PackDetailScreen(
                            id = entry.arguments?.getLong("id") ?: 0L,
                            onOpenLecture = { navController.navigate("lecture/$it") },
                            onDeleted = { navController.popBackStack() },
                        )
                    }
                    composable(ROUTE_QUIZ) {
                        QuizScreen(
                            onOpenLecture = { navController.navigate("lecture/$it") },
                            onBack = { navController.popBackStack() },
                        )
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
        if (!noTopBar) MedoraTopBar(
            title = if (tab == Tab.Home || tab == Tab.Library || tab == Tab.Progress) "" else stringResource(titleRes),
            modifier = Modifier.align(Alignment.TopCenter),
            startIcon = if (isSub) MedoraIcons.Back else null,
            startDescription = stringResource(R.string.back),
            onStart = { navController.popBackStack() },
            endIcon = if (tab == Tab.Home) MedoraIcons.Settings else null,
            endDescription = stringResource(R.string.settings),
            onEnd = { navController.navigate(ROUTE_SETTINGS) },
        )
        if (tab != null) {
            val goTab: (Tab) -> Unit = { item ->
                navController.navigate(item.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            MedoraBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = Dimens.screenPadding, end = Dimens.screenPadding, bottom = 12.dp),
            ) {
                Tab.values().forEach { item ->
                    MedoraBottomItem(
                        icon = item.icon,
                        label = stringResource(item.label),
                        selected = item == tab,
                        onClick = { goTab(item) },
                    )
                }
            }
        }
    }
}
