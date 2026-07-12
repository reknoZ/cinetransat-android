package com.heewhack.cinetransat.ui.program

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.heewhack.cinetransat.ui.LocalComponentActivity
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.detail.MovieDetailScreen

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ProgramNavHost(
    modifier: Modifier = Modifier,
    pendingScreeningId: String? = null,
    onPendingScreeningHandled: () -> Unit = {},
    programFocusGeneration: Int = 0,
) {
    val activity = LocalComponentActivity.current
    val windowSizeClass = calculateWindowSizeClass(activity = activity)
    val navController = rememberNavController()
    val programStore = LocalFestivalProgramStore.current
    val programState by programStore.state.collectAsStateWithLifecycle()

    LaunchedEffect(pendingScreeningId, programState.allScreenings) {
        val id = pendingScreeningId ?: return@LaunchedEffect
        if (programState.allScreenings.any { it.id == id }) {
            navController.navigate("detail/$id") {
                launchSingleTop = true
            }
            onPendingScreeningHandled()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "program",
        modifier = modifier.fillMaxSize(),
    ) {
        composable("program") {
            val onScreeningClick = { screeningId: String ->
                navController.navigate("detail/$screeningId")
            }
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                ProgramPhoneScreen(
                    onScreeningClick = { _, screening -> onScreeningClick(screening.id) },
                    programFocusGeneration = programFocusGeneration,
                )
            } else {
                ProgramTabletScreen(
                    onScreeningClick = { _, screening -> onScreeningClick(screening.id) },
                    programFocusGeneration = programFocusGeneration,
                )
            }
        }
        composable(
            route = "detail/{screeningId}",
            arguments =
                listOf(
                    navArgument("screeningId") { type = NavType.StringType },
                ),
        ) { entry ->
            val id = entry.arguments?.getString("screeningId").orEmpty()
            val screenings = programState.allScreenings
            if (screenings.isEmpty()) {
                LaunchedEffect(Unit) {
                    navController.navigateUp()
                }
                return@composable
            }
            MovieDetailScreen(
                screenings = screenings,
                initialScreeningId = id,
                onNavigateUp = { navController.navigateUp() },
            )
        }
    }
}
