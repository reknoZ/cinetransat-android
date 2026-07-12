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
import com.heewhack.cinetransat.ui.today.TodayTabScreen
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
    val showTodayTab = programState.allScreenings.screeningsToday().isNotEmpty()

    val programTabIndex = if (showTodayTab) 1 else 0
    val watchlistTabIndex = programTabIndex + 1
    val infoTabIndex = watchlistTabIndex + 1
    val settingsTabIndex = infoTabIndex + 1

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var programFocusGeneration by rememberSaveable { mutableIntStateOf(0) }
    var previousShowTodayTab by remember { mutableStateOf(showTodayTab) }

    LaunchedEffect(showTodayTab) {
        if (previousShowTodayTab && !showTodayTab && selectedTab > 0) {
            selectedTab--
        }
        previousShowTodayTab = showTodayTab
    }

    LaunchedEffect(selectedTab, programTabIndex) {
        if (selectedTab == programTabIndex) {
            programFocusGeneration++
        }
    }

    LaunchedEffect(pendingScreeningId, programTabIndex) {
        if (pendingScreeningId != null) {
            selectedTab = programTabIndex
        }
    }

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
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
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
                    selected = selectedTab == programTabIndex,
                    onClick = { selectedTab = programTabIndex },
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
                    selected = selectedTab == watchlistTabIndex,
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
                    selected = selectedTab == infoTabIndex,
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
                    selected = selectedTab == settingsTabIndex,
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
                0 ->
                    if (showTodayTab) {
                        TodayTabScreen()
                    } else {
                        ProgramNavHost(
                            pendingScreeningId = pendingScreeningId,
                            onPendingScreeningHandled = onPendingScreeningHandled,
                            programFocusGeneration = programFocusGeneration,
                        )
                    }
                programTabIndex ->
                    ProgramNavHost(
                        pendingScreeningId = pendingScreeningId,
                        onPendingScreeningHandled = onPendingScreeningHandled,
                        programFocusGeneration = programFocusGeneration,
                    )
                watchlistTabIndex ->
                    WatchListNavHost(onRequestCalendarPermissions = onRequestCalendarPermissions)
                infoTabIndex -> UsefulInfoScreen()
                settingsTabIndex ->
                    SettingsScreen(onRequestNotificationPermission = onRequestNotificationPermission)
            }
        }
    }
}
