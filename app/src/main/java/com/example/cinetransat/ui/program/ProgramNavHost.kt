package com.example.cinetransat.ui.program

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cinetransat.data.FestivalProgramData
import com.example.cinetransat.ui.detail.MovieDetailScreen

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ProgramNavHost(modifier: Modifier = Modifier) {
    val activity = LocalContext.current as ComponentActivity
    val windowSizeClass = calculateWindowSizeClass(activity = activity)
    val navController = rememberNavController()

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
                )
            } else {
                ProgramTabletScreen(
                    onScreeningClick = { _, screening -> onScreeningClick(screening.id) },
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
            val screenings = FestivalProgramData.weeks.flatMap { it.orderedScreenings }
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
