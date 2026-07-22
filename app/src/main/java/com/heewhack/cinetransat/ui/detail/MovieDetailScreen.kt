package com.heewhack.cinetransat.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.data.AppLanguage
import com.heewhack.cinetransat.data.ExternalFilmLinks
import com.heewhack.cinetransat.data.Screening
import com.heewhack.cinetransat.data.canceledForRattrapage
import com.heewhack.cinetransat.data.localizedSynopsis
import com.heewhack.cinetransat.data.localizedTitle
import com.heewhack.cinetransat.data.localizedAudioLanguage
import com.heewhack.cinetransat.data.localizedSubtitleLanguage
import com.heewhack.cinetransat.data.rememberAppLanguage
import com.heewhack.cinetransat.data.rememberFestivalLocale
import com.heewhack.cinetransat.calendar.ScreeningCalendarService
import com.heewhack.cinetransat.ui.LocalComponentActivity
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.LocalRattrapageVotesRepository
import com.heewhack.cinetransat.ui.LocalWatchListRepository
import com.heewhack.cinetransat.ui.LocalWatchListStatsRepository
import com.heewhack.cinetransat.ui.watchlistInterestLabel
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
            durationValue = stringResource(R.string.detail_duration_minutes, minutes)
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

@Composable
private fun ScreeningLanguageRow(
    screening: Screening,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val audio = screening.localizedAudioLanguage(appLanguage) ?: stringResource(R.string.detail_language_unspecified)
    val subtitles = screening.localizedSubtitleLanguage(appLanguage) ?: stringResource(R.string.detail_language_unspecified)

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        LanguageFactColumn(
            icon = Icons.Filled.RecordVoiceOver,
            label = stringResource(R.string.detail_audio_language),
            value = audio,
            modifier = Modifier.weight(1f),
        )
        LanguageFactColumn(
            icon = Icons.Filled.Subtitles,
            label = stringResource(R.string.detail_subtitles),
            value = subtitles,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LanguageFactColumn(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.semantics {
                contentDescription = "$label, $value"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    screenings: List<Screening>,
    initialScreeningId: String,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    showUpNavigation: Boolean = true,
    showNavButtons: Boolean = true,
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
        modifier = modifier,
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
                    if (showUpNavigation) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.detail_back),
                            )
                        }
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
            key(item.id, watchIds.contains(item.id)) {
                    DetailBody(
                    screening = item,
                    appLanguage = appLanguage,
                    lineupScreenings = screenings,
                    onOpenImdb = { uriHandler.openUri(ExternalFilmLinks.imdbSearchUri(item.searchTitle).toString()) },
                    onOpenAllocine = { uriHandler.openUri(ExternalFilmLinks.allocineSearchUri(item.searchTitle).toString()) },
                    pagerState = pagerState,
                    showNavButtons = showNavButtons,
                    onToggleWatch = {
                        if (!item.hasPassed) {
                            scope.launch { watchRepo.toggle(item.id, seasonYear) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailBody(
    screening: Screening,
    appLanguage: AppLanguage,
    lineupScreenings: List<Screening>,
    onOpenImdb: () -> Unit,
    onOpenAllocine: () -> Unit,
    pagerState: PagerState,
    showNavButtons: Boolean = true,
    onToggleWatch: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val watchRepo = LocalWatchListRepository.current
    val watchIds by watchRepo.screeningIds.collectAsStateWithLifecycle()
    val inWatchList = screening.id in watchIds
    val statsRepo = LocalWatchListStatsRepository.current
    val counts by statsRepo.counts.collectAsStateWithLifecycle()
    val dayFormatter = rememberDayFormatter()
    val timeFormatter = rememberTimeFormatter()
    val contentModifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()
    val activity = LocalComponentActivity.current
    val programStore = LocalFestivalProgramStore.current
    val programState by programStore.state.collectAsStateWithLifecycle()
    val seasonYear = programState.seasonYear

    DisposableEffect(screening.id) {
        statsRepo.startObserving(screening.id)
        onDispose { statsRepo.stopObserving(screening.id) }
    }

    val totalCount = counts[screening.id] ?: 0

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
            if (showNavButtons) {
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
                        contentDescription = stringResource(R.string.detail_previous_film),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Box(contentAlignment = Alignment.BottomEnd) {
                MoviePosterCard(
                    screening = screening,
                    compact = false,
                    modifier = Modifier.widthIn(max = 238.dp),
                )
                val watchListInteractive = !screening.hasPassed
                if (watchListInteractive || inWatchList) {
                    Surface(
                        modifier =
                            Modifier
                                .padding(4.dp)
                                .size(32.dp)
                                .graphicsLayer { alpha = if (watchListInteractive) 1f else 0.45f },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 2.dp,
                    ) {
                        IconButton(
                            onClick = onToggleWatch,
                            enabled = watchListInteractive,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (inWatchList) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription =
                                    if (inWatchList) {
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

            if (showNavButtons) {
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
                        contentDescription = stringResource(R.string.detail_next_film),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
            } else if (screening.hasPassed) {
                Text(
                    text = stringResource(R.string.screening_passed),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = dayFormatter.format(screening.startsAt),
                style = MaterialTheme.typography.titleMedium,
                color =
                    if (screening.hasPassed) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )

            val interestLabel =
                if (totalCount > 0) {
                    watchlistInterestLabel(totalCount)
                } else {
                    null
                }
            if (interestLabel != null) {
                Text(
                    text = interestLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ScreeningFactsRow(screening = screening, timeFormatter = timeFormatter)

            if (!screening.isRattrapageEvening) {
                ScreeningLanguageRow(
                    screening = screening,
                    appLanguage = appLanguage,
                    modifier = contentModifier,
                )
            }

            if (!screening.isCanceled) {
                OutlinedButton(
                    onClick = {
                        val intent =
                            ScreeningCalendarService.insertIntent(
                                context = activity,
                                screening = screening,
                                language = appLanguage,
                            )
                        activity.startActivity(intent)
                    },
                    modifier = contentModifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Event,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.calendar_add_one))
                }
            }

            Text(
                text = screening.localizedSynopsis(appLanguage),
                style = MaterialTheme.typography.bodyLarge,
                modifier = contentModifier,
            )

            if (screening.isRattrapageEvening) {
                val canceled =
                    programState.allScreenings.canceledForRattrapage(
                        excludingRattrapageId = screening.id,
                    )
                if (canceled.isNotEmpty()) {
                    RattrapageDetailSection(
                        canceledScreenings = canceled,
                        seasonYear = seasonYear,
                        votingOpen = programState.publicConfig.rattrapageVotingOpen,
                        appLanguage = appLanguage,
                        onOpenScreening = { canceledId ->
                            val index = lineupScreenings.indexOfFirst { it.id == canceledId }
                            if (index >= 0) {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        },
                        modifier = contentModifier,
                    )
                }
            }

            if (screening.externalSearchLinksEnabled || screening.releaseYear != null) {
                ExternalLinksRow(
                    modifier = contentModifier,
                    linksEnabled = screening.externalSearchLinksEnabled,
                    releaseYear = screening.releaseYear,
                    onOpenImdb = onOpenImdb,
                    onOpenAllocine = onOpenAllocine,
                )
            }
        }
    }
}

@Composable
private fun RattrapageDetailSection(
    canceledScreenings: List<Screening>,
    seasonYear: Int,
    votingOpen: Boolean,
    appLanguage: AppLanguage,
    onOpenScreening: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val votesRepo = LocalRattrapageVotesRepository.current
    val votedIds by votesRepo.votedScreeningIds.collectAsStateWithLifecycle()
    val voteCounts by votesRepo.voteCounts.collectAsStateWithLifecycle()
    val totalVotes = voteCounts.values.sum()

    LaunchedEffect(canceledScreenings.map { it.id }, seasonYear, votingOpen) {
        if (votingOpen) {
            votesRepo.startObserving(
                screeningIds = canceledScreenings.map { it.id },
                seasonYear = seasonYear,
            )
        } else {
            votesRepo.stopAllObservations()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text =
                if (canceledScreenings.size == 1) {
                    stringResource(R.string.rattrapage_canceled_one)
                } else {
                    stringResource(R.string.rattrapage_canceled_many)
                },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (votingOpen) {
            Text(
                text = stringResource(R.string.rattrapage_canceled_heading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            canceledScreenings.forEach { canceled ->
                val voted = canceled.id in votedIds
                val count = voteCounts[canceled.id] ?: 0
                val share = if (totalVotes > 0) count.toFloat() / totalVotes.toFloat() else 0f
                RattrapagePollOptionRow(
                    title = canceled.localizedTitle(appLanguage),
                    voted = voted,
                    voteCount = count,
                    share = share,
                    onToggleVote = { votesRepo.toggleVote(canceled.id, seasonYear) },
                    onOpenDetail = { onOpenScreening(canceled.id) },
                )
            }
            if (totalVotes > 0) {
                Text(
                    text =
                        if (totalVotes == 1) {
                            stringResource(R.string.rattrapage_votes_one)
                        } else {
                            stringResource(R.string.rattrapage_votes_many, totalVotes)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            canceledScreenings.forEach { canceled ->
                Text(
                    text = canceled.localizedTitle(appLanguage),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenScreening(canceled.id) }
                            .padding(vertical = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.rattrapage_voting_closed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RattrapagePollOptionRow(
    title: String,
    voted: Boolean,
    voteCount: Int,
    share: Float,
    onToggleVote: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val animatedShare by animateFloatAsState(
        targetValue = share.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
        label = "rattrapageVoteShare",
    )
    val shape = RoundedCornerShape(10.dp)
    val fillColor =
        if (voted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .clickable(onClick = onToggleVote),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedShare)
                        .background(fillColor),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector =
                        if (voted) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.RadioButtonUnchecked
                        },
                    contentDescription =
                        stringResource(
                            if (voted) {
                                R.string.rattrapage_vote_remove
                            } else {
                                R.string.rattrapage_vote_add
                            },
                        ),
                    tint =
                        if (voted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$voteCount",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color =
                        if (voted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
        IconButton(onClick = onOpenDetail) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun ExternalLinksRow(
    linksEnabled: Boolean,
    releaseYear: Int?,
    onOpenImdb: () -> Unit,
    onOpenAllocine: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!linksEnabled && releaseYear == null) return

    val linkColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (linksEnabled) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExternalLinkChip(
                    enabled = true,
                    onClick = onOpenImdb,
                    linkColor = linkColor,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MovieCreation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = linkColor,
                    )
                    Text(
                        text = stringResource(R.string.detail_search_imdb),
                        style = MaterialTheme.typography.bodyMedium,
                        color = linkColor,
                    )
                }
                ExternalLinkChip(
                    enabled = true,
                    onClick = onOpenAllocine,
                    linkColor = linkColor,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Theaters,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = linkColor,
                    )
                    Text(
                        text = stringResource(R.string.detail_search_allocine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = linkColor,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (releaseYear != null) {
            Text(
                text = releaseYear.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExternalLinkChip(
    enabled: Boolean,
    onClick: () -> Unit,
    linkColor: Color,
    content: @Composable () -> Unit,
) {
    val row =
        @Composable {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    if (enabled) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = linkColor),
        ) {
            row()
        }
    } else {
        row()
    }
}
