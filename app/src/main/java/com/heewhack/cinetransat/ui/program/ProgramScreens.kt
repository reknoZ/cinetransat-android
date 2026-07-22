package com.heewhack.cinetransat.ui.program

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heewhack.cinetransat.data.AppLanguage
import com.heewhack.cinetransat.data.FestivalWeek
import com.heewhack.cinetransat.data.Screening
import com.heewhack.cinetransat.data.indexOfWeekFor
import com.heewhack.cinetransat.data.localizedTitle
import com.heewhack.cinetransat.data.rememberAppLanguage
import com.heewhack.cinetransat.data.rememberFestivalLocale
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.LocalProgramWeekRepository
import com.heewhack.cinetransat.ui.LocalWatchListRepository
import com.heewhack.cinetransat.ui.components.MoviePosterCard
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProgramPhoneScreen(
    onScreeningClick: (FestivalWeek, Screening) -> Unit,
    programFocusGeneration: Int = 0,
    modifier: Modifier = Modifier,
) {
    val programStore = LocalFestivalProgramStore.current
    val programWeekRepository = LocalProgramWeekRepository.current
    val programState by programStore.state.collectAsStateWithLifecycle()
    val weeks = programState.weeks

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            programState.isLoading && weeks.isEmpty() -> {
                Box(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            weeks.isEmpty() -> {
                ProgramMessage(
                    modifier = Modifier.padding(innerPadding),
                    message = programState.lastErrorMessage ?: stringResource(R.string.program_unavailable),
                    onRetry = { programStore.completePostLaunchSetup() },
                )
            }
            else -> {
                val seasonYear = programState.seasonYear
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                ) {
                    programState.lastErrorMessage?.let { error ->
                        ProgramErrorBanner(
                            message = error,
                            onDismiss = programStore::clearError,
                        )
                    }
                    key(seasonYear) {
                    val savedPageIndex =
                        remember(weeks) {
                            programWeekRepository.weekPageIndex(seasonYear, weeks)
                        }
                    val pagerState =
                        rememberPagerState(
                            initialPage = savedPageIndex.coerceIn(0, weeks.lastIndex.coerceAtLeast(0)),
                            pageCount = { weeks.size },
                        )
                    val pagerScope = rememberCoroutineScope()
                    LaunchedEffect(pagerState, seasonYear, weeks) {
                        snapshotFlow { pagerState.currentPage }.collect { page ->
                            weeks.getOrNull(page)?.let { week ->
                                programWeekRepository.saveSelectedWeek(seasonYear, week)
                            }
                        }
                    }
                    LaunchedEffect(programFocusGeneration, seasonYear, weeks) {
                        if (programFocusGeneration == 0 || weeks.isEmpty()) return@LaunchedEffect
                        val targetPage = weeks.indexOfWeekFor().coerceIn(0, weeks.lastIndex)
                        if (pagerState.currentPage != targetPage) {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }

                    val currentWeek = weeks.getOrNull(pagerState.currentPage)
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProgramSeasonNavigator(
                            seasonYear = programState.seasonYear,
                            availableYears = programState.availableSeasonYears,
                            onSelectSeason = programStore::selectSeason,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (currentWeek != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = weekHeaderText(currentWeek),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        beyondViewportPageCount = 1,
                    ) { page ->
                        val week = weeks[page]
                        WeekGrid(
                            week = week,
                            compact = true,
                            showWeekHeader = false,
                            onScreeningClick = onScreeningClick,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                        )
                    }
                    PagerDots(
                        pageCount = weeks.size,
                        currentPage = pagerState.currentPage,
                        onPageSelected = { index ->
                            pagerScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 14.dp),
                    )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramTabletScreen(
    onScreeningClick: (FestivalWeek, Screening) -> Unit,
    programFocusGeneration: Int = 0,
    modifier: Modifier = Modifier,
) {
    val programStore = LocalFestivalProgramStore.current
    val programWeekRepository = LocalProgramWeekRepository.current
    val programState by programStore.state.collectAsStateWithLifecycle()
    val weeks = programState.weeks
    val seasonYear = programState.seasonYear
    var selectedWeek by remember(seasonYear, weeks) {
        mutableStateOf(programWeekRepository.selectedWeek(weeks, seasonYear))
    }

    LaunchedEffect(weeks, seasonYear) {
        if (selectedWeek == null || weeks.none { it.id == selectedWeek?.id }) {
            selectedWeek = programWeekRepository.selectedWeek(weeks, seasonYear)
        }
    }

    LaunchedEffect(selectedWeek, seasonYear) {
        val week = selectedWeek ?: return@LaunchedEffect
        programWeekRepository.saveSelectedWeek(seasonYear, week)
    }

    LaunchedEffect(programFocusGeneration, weeks, seasonYear) {
        if (programFocusGeneration == 0 || weeks.isEmpty()) return@LaunchedEffect
        selectedWeek = weeks[weeks.indexOfWeekFor()]
    }

    if (programState.isLoading && weeks.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (weeks.isEmpty() || selectedWeek == null) {
        ProgramMessage(
            modifier = modifier,
            message = programState.lastErrorMessage ?: stringResource(R.string.program_unavailable),
            onRetry = { programStore.completePostLaunchSetup() },
        )
        return
    }

    Row(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier
                    .widthIn(min = 260.dp, max = 360.dp)
                    .fillMaxHeight()
                    .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(weeks, key = { it.id }) { week ->
                val selected = week.id == selectedWeek?.id
                Card(
                    onClick = { selectedWeek = week },
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                        ),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.program_week_short, week.weekNumber),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = week.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ProgramSeasonNavigator(
                                seasonYear = programState.seasonYear,
                                availableYears = programState.availableSeasonYears,
                                onSelectSeason = programStore::selectSeason,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = weekHeaderText(selectedWeek!!),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    },
                    windowInsets = WindowInsets(0),
                    expandedHeight = 48.dp,
                )
            },
        ) { innerPadding ->
            WeekGrid(
                week = selectedWeek!!,
                compact = false,
                showWeekHeader = false,
                onScreeningClick = onScreeningClick,
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun PagerDots(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val weekLabel = stringResource(R.string.program_week_short, index + 1)
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .semantics { contentDescription = weekLabel }
                        .clickable { onPageSelected(index) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .then(
                                if (selected) {
                                    Modifier.size(22.dp, 10.dp)
                                } else {
                                    Modifier.size(10.dp)
                                },
                            )
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                },
                            ),
                )
            }
        }
    }
}

@Composable
private fun WeekGrid(
    week: FestivalWeek,
    compact: Boolean,
    onScreeningClick: (FestivalWeek, Screening) -> Unit,
    modifier: Modifier = Modifier,
    showWeekHeader: Boolean = true,
) {
    val screenings = week.orderedScreenings
    if (screenings.size != 4) return
    val textMeasurer = rememberTextMeasurer()
    val repo = LocalWatchListRepository.current
    val programStore = LocalFestivalProgramStore.current
    val programState by programStore.state.collectAsStateWithLifecycle()
    val seasonYear = programState.publicConfig.currentSeasonYear
    val watchIds by repo.screeningIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    val appLanguage = rememberAppLanguage()

    val titleStyle =
        if (compact) {
            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 12.sp)
        } else {
            MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 13.sp)
        }

    Column(modifier = modifier.fillMaxSize()) {
        if (showWeekHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                ) {
                    Text(
                        text = weekHeaderText(week),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(modifier.height(10.dp))
        } else {
            Spacer(modifier.height(4.dp))
        }

        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            val density = LocalDensity.current
            val minGap = if (compact) 7.dp else 10.dp
            val posterTitleGap = 2.dp
            // 0.88 was too small; 1.0 can clip titles — ~0.94 is a middle ground.
            val posterWidthScale = if (compact) 0.94f else 0.92f

            fun rowTitleHeight(width: androidx.compose.ui.unit.Dp, a: Int, b: Int): androidx.compose.ui.unit.Dp {
                val wpx = with(density) { width.roundToPx() }
                val aPx =
                    textMeasurer.measure(
                        text = screenings[a].localizedTitle(appLanguage),
                        style = titleStyle,
                        constraints = Constraints(maxWidth = wpx),
                    ).size.height
                val bPx =
                    textMeasurer.measure(
                        text = screenings[b].localizedTitle(appLanguage),
                        style = titleStyle,
                        constraints = Constraints(maxWidth = wpx),
                    ).size.height
                return with(density) { maxOf(aPx, bPx).toDp() } + 1.dp
            }

            val widthCap = (maxWidth - (minGap * 2f)) / 2f
            val minPosterW = 40.dp
            val posterW =
                remember(maxWidth, maxHeight, screenings, titleStyle, appLanguage, compact) {
                    var candidate = widthCap
                    while (candidate > minPosterW) {
                        val row1Title = rowTitleHeight(candidate, 0, 1)
                        val row2Title = rowTitleHeight(candidate, 2, 3)
                        val posterH = candidate * 3f / 2f
                        val requiredHeight =
                            (posterH * 2f) + (minGap * 3f) + (posterTitleGap * 2f) + row1Title + row2Title
                        if (requiredHeight <= maxHeight) break
                        candidate -= 0.5.dp
                    }
                    (candidate * posterWidthScale).coerceAtLeast(minPosterW)
                }

            val row1Title = rowTitleHeight(posterW, 0, 1)
            val row2Title = rowTitleHeight(posterW, 2, 3)
            val posterH = posterW * 3f / 2f
            val row1Height = posterH + posterTitleGap + row1Title
            val row2Height = posterH + posterTitleGap + row2Title

            val horizontalSlack = (maxWidth - (posterW * 2f)).coerceAtLeast(0.dp)
            val sideGap = horizontalSlack / 3f
            val colGap = horizontalSlack / 3f

            val verticalSlack = (maxHeight - row1Height - row2Height).coerceAtLeast(0.dp)
            val topGap = verticalSlack / 3f
            val rowGap = verticalSlack / 3f
            val bottomGap = verticalSlack / 3f

            val posterSize = DpSize(posterW, posterH)

            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topGap))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(row1Height)
                            .padding(horizontal = sideGap),
                    horizontalArrangement = Arrangement.spacedBy(colGap),
                ) {
                    PosterCell(
                        screening = screenings[0],
                        appLanguage = appLanguage,
                        compact = compact,
                        posterSize = posterSize,
                        titleStyle = titleStyle,
                        inWatchlist = screenings[0].id in watchIds,
                        onToggleWatch = { scope.launch { repo.toggle(screenings[0].id, seasonYear) } },
                        onOpenDetail = { onScreeningClick(week, screenings[0]) },
                        modifier = Modifier.width(posterW).fillMaxHeight(),
                    )
                    PosterCell(
                        screening = screenings[1],
                        appLanguage = appLanguage,
                        compact = compact,
                        posterSize = posterSize,
                        titleStyle = titleStyle,
                        inWatchlist = screenings[1].id in watchIds,
                        onToggleWatch = { scope.launch { repo.toggle(screenings[1].id, seasonYear) } },
                        onOpenDetail = { onScreeningClick(week, screenings[1]) },
                        modifier = Modifier.width(posterW).fillMaxHeight(),
                    )
                }
                Spacer(Modifier.height(rowGap))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(row2Height)
                            .padding(horizontal = sideGap),
                    horizontalArrangement = Arrangement.spacedBy(colGap),
                ) {
                    PosterCell(
                        screening = screenings[2],
                        appLanguage = appLanguage,
                        compact = compact,
                        posterSize = posterSize,
                        titleStyle = titleStyle,
                        inWatchlist = screenings[2].id in watchIds,
                        onToggleWatch = { scope.launch { repo.toggle(screenings[2].id, seasonYear) } },
                        onOpenDetail = { onScreeningClick(week, screenings[2]) },
                        modifier = Modifier.width(posterW).fillMaxHeight(),
                    )
                    PosterCell(
                        screening = screenings[3],
                        appLanguage = appLanguage,
                        compact = compact,
                        posterSize = posterSize,
                        titleStyle = titleStyle,
                        inWatchlist = screenings[3].id in watchIds,
                        onToggleWatch = { scope.launch { repo.toggle(screenings[3].id, seasonYear) } },
                        onOpenDetail = { onScreeningClick(week, screenings[3]) },
                        modifier = Modifier.width(posterW).fillMaxHeight(),
                    )
                }
                Spacer(Modifier.height(bottomGap))
            }
        }
    }
}

@Composable
private fun PosterCell(
    screening: Screening,
    appLanguage: AppLanguage,
    compact: Boolean,
    posterSize: DpSize,
    titleStyle: TextStyle,
    inWatchlist: Boolean,
    onToggleWatch: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val watchListInteractive = !screening.hasPassed
    val showWatchListControl = watchListInteractive || inWatchlist

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.width(posterSize.width),
            contentAlignment = Alignment.BottomEnd,
        ) {
            MoviePosterCard(
                screening = screening,
                modifier = Modifier.clickable(onClick = onOpenDetail),
                compact = compact,
                fixedPosterSize = posterSize,
            )
            if (showWatchListControl) {
                Surface(
                    modifier =
                        Modifier
                            .padding(4.dp)
                            .size(32.dp)
                            .graphicsLayer { alpha = if (watchListInteractive) 1f else 0.45f },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 2.dp,
                ) {
                    IconButton(
                        onClick = onToggleWatch,
                        enabled = watchListInteractive,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (inWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription =
                                if (inWatchlist) {
                                    stringResource(R.string.watchlist_remove)
                                } else {
                                    stringResource(R.string.watchlist_add)
                                },
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        Text(
            text = screening.localizedTitle(appLanguage),
            style = titleStyle,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            softWrap = true,
            color =
                if (screening.hasPassed) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
            modifier =
                Modifier
                    .width(posterSize.width)
                    .graphicsLayer { alpha = if (screening.hasPassed) 0.8f else 1f }
                    .clickable(onClick = onOpenDetail),
        )
    }
}

@Composable
private fun ProgramSeasonNavigator(
    seasonYear: Int,
    availableYears: List<Int>,
    onSelectSeason: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val years = availableYears.ifEmpty { listOf(seasonYear) }
    val index = years.indexOf(seasonYear)
    val olderYear = years.getOrNull(if (index >= 0) index + 1 else -1)
    val newerYear = years.getOrNull(if (index > 0) index - 1 else -1)
    val showsChevrons = years.size > 1

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        if (showsChevrons) {
            IconButton(
                onClick = { olderYear?.let(onSelectSeason) },
                enabled = olderYear != null,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(R.string.program_season_older),
                    tint =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (olderYear != null) 1f else 0.28f,
                        ),
                )
            }
        }
        Text(
            text = seasonYear.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 56.dp),
            textAlign = TextAlign.Center,
        )
        if (showsChevrons) {
            IconButton(
                onClick = { newerYear?.let(onSelectSeason) },
                enabled = newerYear != null,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = stringResource(R.string.program_season_newer),
                    tint =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (newerYear != null) 1f else 0.28f,
                        ),
                )
            }
        }
    }
}

@Composable
private fun ProgramMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
private fun ProgramErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        }
    }
}

@Composable
private fun weekHeaderText(week: FestivalWeek): String {
    val locale = rememberFestivalLocale()
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM", locale) }
    val first = week.orderedScreenings.firstOrNull()?.startsAt?.toLocalDate()
    val last = week.orderedScreenings.lastOrNull()?.startsAt?.toLocalDate()
    val range =
        if (first != null && last != null) {
            if (first.month == last.month) {
                "${first.dayOfMonth}-${last.dayOfMonth} ${monthFormatter.format(first)}"
            } else {
                "${first.dayOfMonth} ${monthFormatter.format(first)}-${last.dayOfMonth} ${monthFormatter.format(last)}"
            }
        } else {
            week.label
        }
    return stringResource(R.string.program_week_header, week.weekNumber, range)
}
