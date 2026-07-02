package com.heewhack.cinetransat.ui.watchlist

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.data.WatchListStatsRepository
import com.heewhack.cinetransat.data.rememberFestivalLocale
import com.heewhack.cinetransat.data.localizedTitle
import com.heewhack.cinetransat.data.rememberAppLanguage
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.LocalWatchListRepository
import com.heewhack.cinetransat.ui.LocalWatchListStatsRepository
import com.heewhack.cinetransat.ui.watchlistOthersLabel
import com.heewhack.cinetransat.ui.components.MoviePosterCard
import com.heewhack.cinetransat.ui.detail.MovieDetailScreen
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@Composable
private fun rememberListDateTimeFormatter(): DateTimeFormatter {
    val locale = rememberFestivalLocale()
    return remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
    }
}

private val rowPosterSize = DpSize(76.dp, 114.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchListNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val programStore = LocalFestivalProgramStore.current
    NavHost(
        navController = navController,
        startDestination = "watch_list",
        modifier = modifier.fillMaxSize(),
    ) {
        composable("watch_list") {
            WatchListScreen(
                onOpenScreening = { id, orderedIds ->
                    val encoded = Uri.encode(orderedIds.joinToString(separator = "|"))
                    navController.navigate("detail/$id?ids=$encoded")
                },
            )
        }
        composable(
            route = "detail/{screeningId}?ids={ids}",
            arguments =
                listOf(
                    navArgument("screeningId") { type = NavType.StringType },
                    navArgument("ids") { type = NavType.StringType; defaultValue = "" },
                ),
        ) { entry ->
            val id = entry.arguments?.getString("screeningId").orEmpty()
            val rawIds = entry.arguments?.getString("ids").orEmpty()
            val orderedIds = rawIds.split("|").filter { it.isNotBlank() }
            val screenings =
                if (orderedIds.isEmpty()) {
                    listOfNotNull(programStore.screeningOrNull(id))
                } else {
                    orderedIds.mapNotNull { programStore.screeningOrNull(it) }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchListScreen(onOpenScreening: (String, List<String>) -> Unit) {
    val repo = LocalWatchListRepository.current
    val statsRepo = LocalWatchListStatsRepository.current
    val programStore = LocalFestivalProgramStore.current
    val scope = rememberCoroutineScope()
    val appLanguage = rememberAppLanguage()
    val listDateTimeFormatter = rememberListDateTimeFormatter()
    val ids by repo.screeningIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val counts by statsRepo.counts.collectAsStateWithLifecycle()
    val programState by programStore.state.collectAsStateWithLifecycle()
    val seasonYear = programState.publicConfig.currentSeasonYear
    val screenings =
        programState.allScreenings
            .filter { it.id in ids }
            .sortedBy { it.startsAt }

    LaunchedEffect(ids) {
        statsRepo.syncObservations(ids)
    }

    DisposableEffect(Unit) {
        onDispose { statsRepo.stopAllObservations() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.watchlist_screen_title)) },
                windowInsets = WindowInsets(0),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (screenings.isEmpty()) {
            Column(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.watchlist_empty_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = stringResource(R.string.watchlist_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(screenings, key = { it.id }) { screening ->
                    val totalCount = counts[screening.id] ?: 0
                    val othersCount =
                        WatchListStatsRepository.othersCount(
                            total = totalCount,
                            inWatchList = true,
                        )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .clickable {
                                            onOpenScreening(screening.id, screenings.map { it.id })
                                        },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MoviePosterCard(
                                    screening = screening,
                                    compact = true,
                                    fixedPosterSize = rowPosterSize,
                                    showDateBadge = false,
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = screening.localizedTitle(appLanguage),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = listDateTimeFormatter.format(screening.startsAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (othersCount > 0) {
                                        Text(
                                            text = watchlistOthersLabel(othersCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = {
                                    scope.launch { repo.toggle(screening.id, seasonYear) }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.watchlist_remove),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
