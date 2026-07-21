package com.heewhack.cinetransat.ui.watchlist

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.data.Screening
import com.heewhack.cinetransat.data.WatchListStatsRepository
import com.heewhack.cinetransat.data.rememberFestivalLocale
import com.heewhack.cinetransat.data.localizedTitle
import com.heewhack.cinetransat.data.rememberAppLanguage
import com.heewhack.cinetransat.calendar.ScreeningCalendarService
import com.heewhack.cinetransat.ui.LocalComponentActivity
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.LocalWatchListRepository
import com.heewhack.cinetransat.ui.LocalWatchListStatsRepository
import com.heewhack.cinetransat.ui.watchlistOthersLabel
import com.heewhack.cinetransat.ui.components.MoviePosterCard
import com.heewhack.cinetransat.ui.detail.MovieDetailScreen
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

@Composable
private fun rememberListDateTimeFormatter(): DateTimeFormatter {
    val locale = rememberFestivalLocale()
    return remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
    }
}

private val rowPosterSize = DpSize(91.dp, 137.dp)

private sealed class CalendarAlert {
    data object AccessDenied : CalendarAlert()

    data object NothingToAdd : CalendarAlert()

    data class AddedAll(val count: Int) : CalendarAlert()

    data class Partial(val added: Int, val failed: Int) : CalendarAlert()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchListNavHost(
    onRequestCalendarPermissions: (onResult: (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val programStore = LocalFestivalProgramStore.current
    NavHost(
        navController = navController,
        startDestination = "watch_list",
        modifier = modifier.fillMaxSize(),
    ) {
        composable("watch_list") {
            WatchListScreen(
                onRequestCalendarPermissions = onRequestCalendarPermissions,
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
private fun WatchListScreen(
    onRequestCalendarPermissions: (onResult: (Boolean) -> Unit) -> Unit,
    onOpenScreening: (String, List<String>) -> Unit,
) {
    val repo = LocalWatchListRepository.current
    val statsRepo = LocalWatchListStatsRepository.current
    val programStore = LocalFestivalProgramStore.current
    val activity = LocalComponentActivity.current
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
    var pendingRemovalId by remember { mutableStateOf<String?>(null) }
    var removalSecondsLeft by remember { mutableIntStateOf(5) }
    var calendarAlert by remember { mutableStateOf<CalendarAlert?>(null) }
    var isAddingAllToCalendar by remember { mutableStateOf(false) }

    fun showCalendarResult(result: ScreeningCalendarService.BatchAddResult) {
        calendarAlert =
            when (result) {
                ScreeningCalendarService.BatchAddResult.Denied -> CalendarAlert.AccessDenied
                ScreeningCalendarService.BatchAddResult.Empty -> CalendarAlert.NothingToAdd
                is ScreeningCalendarService.BatchAddResult.Added -> CalendarAlert.AddedAll(result.count)
                is ScreeningCalendarService.BatchAddResult.Partial ->
                    CalendarAlert.Partial(result.added, result.failed)
            }
    }

    fun addAllToCalendar() {
        if (isAddingAllToCalendar) return
        isAddingAllToCalendar = true
        showCalendarResult(
            ScreeningCalendarService.addUpcomingScreenings(
                context = activity,
                screenings = screenings,
                language = appLanguage,
            ),
        )
        isAddingAllToCalendar = false
    }

    LaunchedEffect(pendingRemovalId) {
        val id = pendingRemovalId ?: return@LaunchedEffect
        for (seconds in 5 downTo 1) {
            removalSecondsLeft = seconds
            delay(1_000)
        }
        if (pendingRemovalId == id) {
            repo.toggle(id, seasonYear)
            pendingRemovalId = null
        }
    }

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
                actions = {
                    if (screenings.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onRequestCalendarPermissions { granted ->
                                    if (granted) {
                                        addAllToCalendar()
                                    } else {
                                        calendarAlert = CalendarAlert.AccessDenied
                                    }
                                }
                            },
                            enabled = !isAddingAllToCalendar,
                        ) {
                            CalendarAddIcon(
                                contentDescription = stringResource(R.string.calendar_add_all),
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        calendarAlert?.let { alert ->
            val title = stringResource(R.string.calendar_add_all)
            val message =
                when (alert) {
                    CalendarAlert.AccessDenied -> stringResource(R.string.calendar_access_denied)
                    CalendarAlert.NothingToAdd -> stringResource(R.string.calendar_nothing_to_add)
                    is CalendarAlert.AddedAll ->
                        stringResource(R.string.calendar_added_all, alert.count)
                    is CalendarAlert.Partial ->
                        stringResource(R.string.calendar_added_partial, alert.added, alert.failed)
                }
            AlertDialog(
                onDismissRequest = { calendarAlert = null },
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { calendarAlert = null }) {
                        Text("OK")
                    }
                },
            )
        }
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
                    val isPendingRemoval = pendingRemovalId == screening.id
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
                            WatchListPosterCell(
                                screening = screening,
                                posterSize = rowPosterSize,
                                isPendingRemoval = isPendingRemoval,
                                removalSecondsLeft = removalSecondsLeft,
                                onOpenDetail = {
                                    onOpenScreening(screening.id, screenings.map { it.id })
                                },
                                onBookmarkClick =
                                    if (screening.hasPassed) {
                                        null
                                    } else {
                                        {
                                            if (isPendingRemoval) {
                                                pendingRemovalId = null
                                            } else {
                                                pendingRemovalId = screening.id
                                                removalSecondsLeft = 5
                                            }
                                        }
                                    },
                            )
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .clickable {
                                            onOpenScreening(screening.id, screenings.map { it.id })
                                        },
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
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarAddIcon(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            modifier
                .size(24.dp)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = tint,
        )
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
            tint = tint,
        )
    }
}

@Composable
private fun WatchListPosterCell(
    screening: Screening,
    posterSize: DpSize,
    isPendingRemoval: Boolean,
    removalSecondsLeft: Int,
    onOpenDetail: () -> Unit,
    onBookmarkClick: (() -> Unit)?,
) {
    val bookmarkInteractive = onBookmarkClick != null
    Box(
        modifier = Modifier.size(posterSize.width, posterSize.height),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(onClick = onOpenDetail),
        ) {
            MoviePosterCard(
                screening = screening,
                compact = true,
                fixedPosterSize = posterSize,
                showDateBadge = false,
                modifier = Modifier.fillMaxSize(),
            )
            if (isPendingRemoval) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.58f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.watchlist_remove_countdown, removalSecondsLeft),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
        Surface(
            modifier =
                Modifier
                    .padding(4.dp)
                    .size(32.dp)
                    .graphicsLayer { alpha = if (bookmarkInteractive) 1f else 0.45f },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
        ) {
            IconButton(
                onClick = { onBookmarkClick?.invoke() },
                enabled = bookmarkInteractive,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription =
                        if (bookmarkInteractive) {
                            stringResource(R.string.watchlist_remove)
                        } else {
                            null
                        },
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
