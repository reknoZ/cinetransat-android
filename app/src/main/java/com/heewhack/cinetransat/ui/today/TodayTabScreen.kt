package com.heewhack.cinetransat.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heewhack.cinetransat.data.screeningsToday
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.detail.MovieDetailScreen

@Composable
fun TodayTabScreen(
    modifier: Modifier = Modifier,
) {
    val programStore = LocalFestivalProgramStore.current
    val programState by programStore.state.collectAsStateWithLifecycle()
    val todayScreenings = programState.allScreenings.screeningsToday()
    if (todayScreenings.isEmpty()) return

    MovieDetailScreen(
        modifier = modifier,
        screenings = todayScreenings,
        initialScreeningId = todayScreenings.first().id,
        onNavigateUp = {},
        showUpNavigation = false,
        showNavButtons = false,
    )
}
