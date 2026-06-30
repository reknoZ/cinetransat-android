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
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
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
import com.heewhack.cinetransat.data.localizedTitle
import com.heewhack.cinetransat.data.rememberAppLanguage
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.LocalWatchListRepository
import com.heewhack.cinetransat.ui.components.MoviePosterCard
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProgramPhoneScreen(
    onScreeningClick: (FestivalWeek, Screening) -> Unit,
    modifier: Modifier = Modifier,
) {
    val programStore = LocalFestivalProgramStore.current
    val programState by programStore.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        programNavigationTitle(programState.seasonYear),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                windowInsets = WindowInsets(0),
                expandedHeight = 44.dp,
            )
        },
    ) { innerPadding ->
        val weeks = programState.weeks

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
                    message = programState.lastErrorMessage ?: "Programme indisponible.",
                    onRetry = { programStore.completePostLaunchSetup() },
                )
            }
            else -> {
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
                    val pagerState = rememberPagerState(pageCount = { weeks.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        beyondViewportPageCount = 1,
                    ) { page ->
                        val week = weeks[page]
                        WeekGrid(
                            week = week,
                            compact = true,
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
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramTabletScreen(
    onScreeningClick: (FestivalWeek, Screening) -> Unit,
    modifier: Modifier = Modifier,
) {
    val programStore = LocalFestivalProgramStore.current
    val programState by programStore.state.collectAsStateWithLifecycle()
    val weeks = programState.weeks
    var selectedWeek by remember(weeks) { mutableStateOf(weeks.firstOrNull()) }

    if (programState.isLoading && weeks.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (weeks.isEmpty() || selectedWeek == null) {
        ProgramMessage(
            modifier = modifier,
            message = programState.lastErrorMessage ?: "Programme indisponible.",
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
                            text = "Week ${week.weekNumber}",
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
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            programNavigationTitle(programState.seasonYear),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    windowInsets = WindowInsets(0),
                    expandedHeight = 44.dp,
                )
            },
        ) { innerPadding ->
            WeekGrid(
                week = selectedWeek!!,
                compact = false,
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            },
                        ),
            )
        }
    }
}

@Composable
private fun WeekGrid(
    week: FestivalWeek,
    compact: Boolean,
    onScreeningClick: (FestivalWeek, Screening) -> Unit,
    modifier: Modifier = Modifier,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
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
        Spacer(Modifier.height(10.dp))

        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            val density = LocalDensity.current
            val minGap = if (compact) 8.dp else 10.dp
            val posterTitleGap = 2.dp
            val posterWidthScale = if (compact) 0.88f else 0.9f

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

            val widthCap = (maxWidth - (minGap * 3f)) / 2f
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
            Surface(
                modifier =
                    Modifier
                        .padding(4.dp)
                        .size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 2.dp,
            ) {
                IconButton(
                    onClick = onToggleWatch,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (inWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (inWatchlist) "Retirer de la liste" else "Ajouter à la liste",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Text(
            text = screening.localizedTitle(appLanguage),
            style = titleStyle,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            softWrap = true,
            modifier =
                Modifier
                    .width(posterSize.width)
                    .clickable(onClick = onOpenDetail),
        )
    }
}

private fun programNavigationTitle(seasonYear: Int): String = "Programme $seasonYear"

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
            Text("Réessayer")
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
                Text("OK")
            }
        }
    }
}

private val englishMonth: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

private fun weekHeaderText(week: FestivalWeek): String {
    val first = week.orderedScreenings.firstOrNull()?.startsAt?.toLocalDate()
    val last = week.orderedScreenings.lastOrNull()?.startsAt?.toLocalDate()
    val range =
        if (first != null && last != null) {
            if (first.month == last.month) {
                "${first.dayOfMonth}-${last.dayOfMonth} ${englishMonth.format(first)}"
            } else {
                "${first.dayOfMonth} ${englishMonth.format(first)}-${last.dayOfMonth} ${englishMonth.format(last)}"
            }
        } else {
            week.label
        }
    return "Week ${week.weekNumber} • $range"
}
