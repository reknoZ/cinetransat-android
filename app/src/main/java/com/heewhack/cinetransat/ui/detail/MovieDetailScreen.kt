package com.heewhack.cinetransat.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.data.AppLanguage
import com.heewhack.cinetransat.data.ExternalFilmLinks
import com.heewhack.cinetransat.data.Screening
import com.heewhack.cinetransat.data.localizedSynopsis
import com.heewhack.cinetransat.data.localizedTitle
import com.heewhack.cinetransat.data.WatchListStatsRepository
import com.heewhack.cinetransat.data.rememberAppLanguage
import com.heewhack.cinetransat.data.rememberFestivalLocale
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.LocalWatchListRepository
import com.heewhack.cinetransat.ui.LocalWatchListStatsRepository
import com.heewhack.cinetransat.ui.watchlistInterestLabel
import com.heewhack.cinetransat.ui.watchlistOthersLabel
import com.heewhack.cinetransat.ui.components.MoviePosterCard
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
private fun rememberDayFormatter(): DateTimeFormatter {
    val locale = rememberFestivalLocale()
    return remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }
}

@Composable
private fun rememberTimeFormatter(): DateTimeFormatter {
    val locale = rememberFestivalLocale()
    return remember(locale) { DateTimeFormatter.ofPattern("HH:mm").withLocale(locale) }
}

private data class ScreeningFact(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val muted: Boolean = false,
)

@Composable
private fun ScreeningFactsRow(
    screening: Screening,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
) {
    val durationValue: String
    val durationMuted: Boolean
    when (val minutes = screening.runtimeMinutes) {
        null -> {
            durationValue = stringResource(R.string.detail_duration_variable)
            durationMuted = true
        }
        0 -> {
            durationValue = "—"
            durationMuted = true
        }
        else -> {
            durationValue = "$minutes min"
            durationMuted = false
        }
    }

    val facts =
        listOf(
            ScreeningFact(
                icon = Icons.Filled.WbTwilight,
                label = stringResource(R.string.detail_sunset),
                value = timeFormatter.format(screening.sunsetAt),
            ),
            ScreeningFact(
                icon = Icons.Filled.Theaters,
                label = stringResource(R.string.detail_start),
                value = timeFormatter.format(screening.startsAt),
            ),
            ScreeningFact(
                icon = Icons.Filled.HourglassTop,
                label = stringResource(R.string.detail_duration),
                value = durationValue,
                muted = durationMuted,
            ),
            ScreeningFact(
                icon = Icons.Filled.PanTool,
                label = stringResource(R.string.detail_legal_age),
                value = screening.legalAge?.let { "$it+" } ?: "—",
                muted = screening.legalAge == null,
            ),
            ScreeningFact(
                icon = Icons.Filled.FamilyRestroom,
                label = stringResource(R.string.detail_recommended_age),
                value = screening.recommendedAge?.let { "$it+" } ?: "—",
                muted = screening.recommendedAge == null,
            ),
        )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        facts.forEach { fact ->
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "${fact.label}, ${fact.value}"
                        },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = fact.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = fact.value,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color =
                        if (fact.muted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    screenings: List<Screening>,
    initialScreeningId: String,
    onNavigateUp: () -> Unit,
) {
    if (screenings.isEmpty()) return

    val uriHandler = LocalUriHandler.current
    val watchRepo = LocalWatchListRepository.current
    val programStore = LocalFestivalProgramStore.current
    val programState by programStore.state.collectAsStateWithLifecycle()
    val seasonYear = programState.publicConfig.currentSeasonYear
    val watchIds by watchRepo.screeningIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    val initialPage = remember(initialScreeningId, screenings) {
        screenings.indexOfFirst { it.id == initialScreeningId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { screenings.size })
    val screening = screenings[pagerState.currentPage]
    val appLanguage = rememberAppLanguage()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = screening.localizedTitle(appLanguage),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding),
        ) { page ->
            val item = screenings[page]
            DetailBody(
                screening = item,
                appLanguage = appLanguage,
                onOpenImdb = { uriHandler.openUri(ExternalFilmLinks.imdbSearchUri(item.searchTitle).toString()) },
                onOpenAllocine = { uriHandler.openUri(ExternalFilmLinks.allocineSearchUri(item.searchTitle).toString()) },
                pagerState = pagerState,
                inWatchList = item.id in watchIds,
                onToggleWatch = {
                    scope.launch { watchRepo.toggle(item.id, seasonYear) }
                },
            )
        }
    }
}

@Composable
private fun DetailBody(
    screening: Screening,
    appLanguage: AppLanguage,
    onOpenImdb: () -> Unit,
    onOpenAllocine: () -> Unit,
    pagerState: PagerState,
    inWatchList: Boolean,
    onToggleWatch: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val statsRepo = LocalWatchListStatsRepository.current
    val counts by statsRepo.counts.collectAsStateWithLifecycle()
    val dayFormatter = rememberDayFormatter()
    val timeFormatter = rememberTimeFormatter()
    val contentModifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()

    DisposableEffect(screening.id) {
        statsRepo.startObserving(screening.id)
        onDispose { statsRepo.stopObserving(screening.id) }
    }

    val totalCount = counts[screening.id] ?: 0
    val othersCount = WatchListStatsRepository.othersCount(totalCount, inWatchList)

    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = contentModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
                enabled = pagerState.currentPage > 0,
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Film précédent",
                    modifier = Modifier.size(28.dp),
                )
            }

            Box(contentAlignment = Alignment.BottomEnd) {
                MoviePosterCard(
                    screening = screening,
                    compact = false,
                    modifier = Modifier.widthIn(max = 238.dp),
                )
                Surface(
                    modifier =
                        Modifier
                            .padding(4.dp)
                            .size(32.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 2.dp,
                ) {
                    IconButton(
                        onClick = onToggleWatch,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (inWatchList) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (inWatchList) "Retirer de la liste" else "Ajouter à la liste",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    if (pagerState.currentPage < pagerState.pageCount - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                enabled = pagerState.currentPage < pagerState.pageCount - 1,
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Film suivant",
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (screening.isCanceled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                    )
                    Text(
                        text = stringResource(R.string.detail_screening_canceled),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFFF9800),
                    )
                }
            }

            Text(
                text = dayFormatter.format(screening.startsAt),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val interestLabel =
                when {
                    othersCount > 0 -> watchlistOthersLabel(othersCount, detail = true)
                    totalCount > 0 -> watchlistInterestLabel(totalCount)
                    else -> null
                }
            if (interestLabel != null) {
                Text(
                    text = interestLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ScreeningFactsRow(screening = screening, timeFormatter = timeFormatter)

            Text(
                text = screening.localizedSynopsis(appLanguage),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.detail_references),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            TextButton(onClick = onOpenImdb) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Public, contentDescription = null)
                    Text(stringResource(R.string.detail_search_imdb))
                }
            }
            TextButton(onClick = onOpenAllocine) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🍿")
                    Text(stringResource(R.string.detail_search_allocine))
                }
            }
        }
    }
}
