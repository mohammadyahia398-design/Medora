package com.example.medora.ui.navigation

import androidx.annotation.StringRes
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
import com.example.medora.ui.theme.MedoraColors
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.medora.ui.icons.MedoraIcons
import com.example.medora.ui.screens.HomeScreen
import com.example.medora.ui.screens.LectureDetailScreen
import com.example.medora.ui.screens.LibraryScreen
import com.example.medora.ui.screens.ProgressScreen
import com.example.medora.ui.screens.QuizScreen
import com.example.medora.ui.screens.ReviewScreen
import com.example.medora.ui.screens.SettingsScreen
import com.example.medora.ui.screens.StudyScreen

private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_LECTURE = "lecture/{id}"
private const val ROUTE_REVIEW = "review"
private const val ROUTE_QUIZ = "quiz"

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
    val isSub = isSettings || isLecture || isReview || isQuiz

    val titleRes = when {
        isSettings -> R.string.settings
        isLecture -> R.string.lecture_details
        isReview -> R.string.review_title
        isQuiz -> R.string.quiz_title
        tab != null -> tab.label
        else -> R.string.app_name
    }

    val backdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(MedoraColors.Background, Color(0xFFF1E6FB))),
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
                    composable(Tab.Home.route) { HomeScreen() }
                    composable(Tab.Library.route) {
                        LibraryScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(Tab.Study.route) {
                        StudyScreen(
                            onReview = { navController.navigate(ROUTE_REVIEW) },
                            onQuiz = { navController.navigate(ROUTE_QUIZ) },
                        )
                    }
                    composable(Tab.Progress.route) { ProgressScreen() }
                    composable(ROUTE_SETTINGS) { SettingsScreen() }
                    composable(ROUTE_REVIEW) {
                        ReviewScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(ROUTE_QUIZ) {
                        QuizScreen(onOpenLecture = { navController.navigate("lecture/$it") })
                    }
                    composable(
                        ROUTE_LECTURE,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        LectureDetailScreen(id = entry.arguments?.getLong("id") ?: 0L)
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
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            imageVector = tab.icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp).alpha(if (selected) 1f else 0.5f),
        )
        Text(
            text = stringResource(tab.label),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.alpha(if (selected) 1f else 0.5f),
        )
    }
}
