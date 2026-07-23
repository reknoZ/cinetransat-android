package com.heewhack.cinetransat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.data.screeningsToday
import com.heewhack.cinetransat.ui.components.CalendarDayIcon
import com.heewhack.cinetransat.ui.info.UsefulInfoScreen
import com.heewhack.cinetransat.ui.program.ProgramNavHost
import com.heewhack.cinetransat.ui.settings.SettingsScreen
import com.heewhack.cinetransat.ui.watchlist.WatchListNavHost
import com.heewhack.cinetransat.ui.theme.FestivalAccentBright
import com.heewhack.cinetransat.ui.theme.FestivalProgramTitle

@Composable
fun CineTransatApp(
    pendingScreeningId: String? = null,
    onPendingScreeningHandled: () -> Unit = {},
    onRequestCalendarPermissions: (onResult: (Boolean) -> Unit) -> Unit = { it(true) },
    onRequestNotificationPermission: (seasonYear: Int) -> Unit = {},
) {
    val programStore = LocalFestivalProgramStore.current
    val programState by programStore.state.collectAsStateWithLifecycle()
    val todayScreenings = programState.allScreenings.screeningsToday()
    val showTodayTab = todayScreenings.isNotEmpty()
    val todaysScreeningId = todayScreenings.firstOrNull()?.id

    val programTabIndex = if (showTodayTab) 1 else 0
    val watchlistTabIndex = programTabIndex + 1
    val infoTabIndex = watchlistTabIndex + 1
    val settingsTabIndex = infoTabIndex + 1

    // Content host tab — Today is only a shortcut into Program, never its own screen.
    var selectedTab by rememberSaveable { mutableIntStateOf(programTabIndex) }
    var previousShowTodayTab by remember { mutableStateOf(showTodayTab) }
    var hasOpenedInitialToday by rememberSaveable { mutableStateOf(false) }
    var shortcutPendingScreeningId by remember { mutableStateOf<String?>(null) }
    var displayedProgramScreeningId by remember { mutableStateOf<String?>(null) }
    var programResetToken by rememberSaveable { mutableIntStateOf(0) }

    val isViewingTodaysScreening =
        showTodayTab &&
            todaysScreeningId != null &&
            displayedProgramScreeningId == todaysScreeningId

    val highlightedTab =
        if (isViewingTodaysScreening) {
            0
        } else {
            selectedTab
        }

    LaunchedEffect(showTodayTab) {
        if (previousShowTodayTab && !showTodayTab && selectedTab > 0) {
            selectedTab--
        }
        if (!showTodayTab && selectedTab == 0) {
            selectedTab = programTabIndex
        }
        previousShowTodayTab = showTodayTab
    }

    LaunchedEffect(showTodayTab, todaysScreeningId, programTabIndex) {
        if (!hasOpenedInitialToday && showTodayTab && todaysScreeningId != null) {
            shortcutPendingScreeningId = todaysScreeningId
            selectedTab = programTabIndex
            hasOpenedInitialToday = true
        }
    }

    LaunchedEffect(pendingScreeningId, programTabIndex) {
        if (pendingScreeningId != null) {
            selectedTab = programTabIndex
        }
    }

    fun openTodaysScreening() {
        val id = todaysScreeningId ?: return
        shortcutPendingScreeningId = id
        selectedTab = programTabIndex
    }

    fun showProgramGrid() {
        selectedTab = programTabIndex
        shortcutPendingScreeningId = null
        programResetToken++
    }

    val effectivePendingScreeningId = shortcutPendingScreeningId ?: pendingScreeningId

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
            ) {
                val tabColors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = FestivalProgramTitle,
                        selectedTextColor = FestivalProgramTitle,
                        unselectedIconColor = FestivalAccentBright,
                        unselectedTextColor = FestivalAccentBright,
                        indicatorColor = Color.Transparent,
                    )
                if (showTodayTab) {
                    NavigationBarItem(
                        selected = highlightedTab == 0,
                        onClick = { openTodaysScreening() },
                        colors = tabColors,
                        icon = {
                            CalendarDayIcon(
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.tab_today),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            )
                        },
                    )
                }
                NavigationBarItem(
                    selected = highlightedTab == programTabIndex,
                    onClick = { showProgramGrid() },
                    colors = tabColors,
                    icon = {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.tab_program),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        )
                    },
                )
                NavigationBarItem(
                    selected = highlightedTab == watchlistTabIndex,
                    onClick = { selectedTab = watchlistTabIndex },
                    colors = tabColors,
                    icon = {
                        Icon(
                            Icons.Filled.Bookmarks,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.tab_watchlist),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        )
                    },
                )
                NavigationBarItem(
                    selected = highlightedTab == infoTabIndex,
                    onClick = { selectedTab = infoTabIndex },
                    colors = tabColors,
                    icon = {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.tab_info),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        )
                    },
                )
                NavigationBarItem(
                    selected = highlightedTab == settingsTabIndex,
                    onClick = { selectedTab = settingsTabIndex },
                    colors = tabColors,
                    icon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.tab_settings),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (selectedTab) {
                programTabIndex ->
                    key(programResetToken) {
                        ProgramNavHost(
                            pendingScreeningId = effectivePendingScreeningId,
                            onPendingScreeningHandled = {
                                if (shortcutPendingScreeningId != null) {
                                    shortcutPendingScreeningId = null
                                } else {
                                    onPendingScreeningHandled()
                                }
                            },
                            onDisplayedScreeningIdChange = { displayedProgramScreeningId = it },
                        )
                    }
                watchlistTabIndex ->
                    WatchListNavHost(onRequestCalendarPermissions = onRequestCalendarPermissions)
                infoTabIndex -> UsefulInfoScreen()
                settingsTabIndex ->
                    SettingsScreen(onRequestNotificationPermission = onRequestNotificationPermission)
            }
        }
    }
}
