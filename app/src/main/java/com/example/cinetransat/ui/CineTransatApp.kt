package com.example.cinetransat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cinetransat.R
import com.example.cinetransat.ui.about.AboutFestivalScreen
import com.example.cinetransat.ui.info.UsefulInfoScreen
import com.example.cinetransat.ui.program.ProgramNavHost
import com.example.cinetransat.ui.settings.SettingsScreen
import com.example.cinetransat.ui.watchlist.WatchListNavHost

@Composable
fun CineTransatApp() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(stringResource(R.string.tab_program), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Bookmarks, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(stringResource(R.string.tab_watchlist), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(stringResource(R.string.tab_info), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(stringResource(R.string.tab_festival), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
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
                0 -> ProgramNavHost()
                1 -> WatchListNavHost()
                2 -> UsefulInfoScreen()
                3 -> AboutFestivalScreen()
                4 -> SettingsScreen()
            }
        }
    }
}
