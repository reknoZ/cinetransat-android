package com.example.cinetransat.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cinetransat.data.ExternalFilmLinks
import com.example.cinetransat.data.FestivalLocale
import com.example.cinetransat.data.Screening
import com.example.cinetransat.ui.LocalWatchListRepository
import com.example.cinetransat.ui.components.MoviePosterCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(FestivalLocale)

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(FestivalLocale)

@Composable
private fun InfoLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.25f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            modifier = Modifier.weight(0.75f),
            textAlign = TextAlign.End,
        )
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
    val watchIds by watchRepo.screeningIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    val initialPage = remember(initialScreeningId, screenings) {
        screenings.indexOfFirst { it.id == initialScreeningId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { screenings.size })
    val screening = screenings[pagerState.currentPage]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = screening.title,
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
                onOpenImdb = { uriHandler.openUri(ExternalFilmLinks.imdbSearchUri(item.searchTitle).toString()) },
                onOpenAllocine = { uriHandler.openUri(ExternalFilmLinks.allocineSearchUri(item.searchTitle).toString()) },
                pagerState = pagerState,
                inWatchList = item.id in watchIds,
                onToggleWatch = {
                    scope.launch { watchRepo.toggle(item.id) }
                },
            )
        }
    }
}

@Composable
private fun DetailBody(
    screening: Screening,
    onOpenImdb: () -> Unit,
    onOpenAllocine: () -> Unit,
    pagerState: PagerState,
    inWatchList: Boolean,
    onToggleWatch: () -> Unit,
) {
    val scope = rememberCoroutineScope()
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
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
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

            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.BottomEnd,
            ) {
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

        Text(
            text = screening.synopsis,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
        )

        Column(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                        text = "Séance annulée",
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

            InfoLine(
                label = "Coucher du soleil (indicatif)",
                value = timeFormatter.format(screening.sunsetAt),
            )

            InfoLine(
                label = "Début de la projection",
                value = timeFormatter.format(screening.startsAt),
            )

            val runtime = screening.runtimeMinutes
            if (runtime != null) {
                InfoLine(label = "Durée", value = "$runtime min")
            } else {
                InfoLine(
                    label = "Durée",
                    value = "Variable",
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Références",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            TextButton(onClick = onOpenImdb) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Public, contentDescription = null)
                    Text("Recherche sur IMDb")
                }
            }
            TextButton(onClick = onOpenAllocine) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🍿")
                    Text("Recherche sur Allociné")
                }
            }
        }
    }
}
