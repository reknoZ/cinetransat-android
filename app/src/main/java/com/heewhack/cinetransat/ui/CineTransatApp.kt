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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heewhack.cinetransat.R
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(pendingScreeningId) {
        if (pendingScreeningId != null) {
            selectedTab = 0
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
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = tabColors,
                    icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(stringResource(R.string.tab_program), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = tabColors,
                    icon = { Icon(Icons.Filled.Bookmarks, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(stringResource(R.string.tab_watchlist), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = tabColors,
                    icon = { Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(stringResource(R.string.tab_info), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = tabColors,
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
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
                0 -> ProgramNavHost(
                    pendingScreeningId = pendingScreeningId,
                    onPendingScreeningHandled = onPendingScreeningHandled,
                )
                1 -> WatchListNavHost(onRequestCalendarPermissions = onRequestCalendarPermissions)
                2 -> UsefulInfoScreen()
                3 -> SettingsScreen(onRequestNotificationPermission = onRequestNotificationPermission)
            }
        }
    }
}
