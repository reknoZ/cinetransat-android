package com.heewhack.cinetransat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.data.Screening
import com.heewhack.cinetransat.data.localizedTitle
import com.heewhack.cinetransat.data.rememberAppLanguage
import com.heewhack.cinetransat.data.rememberFestivalLocale
import com.heewhack.cinetransat.data.remotePosterUrl
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.theme.FestivalAccent
import java.time.format.DateTimeFormatter

@Composable
fun MoviePosterCard(
    screening: Screening,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    fixedPosterSize: DpSize? = null,
    showDateBadge: Boolean = true,
    showBorderAndShadow: Boolean = true,
) {
    val context = LocalContext.current
    val appLanguage = rememberAppLanguage()
    val locale = rememberFestivalLocale()
    val dateBadgeFormatter = remember(locale) { DateTimeFormatter.ofPattern("dd MMM", locale) }
    val programState = LocalFestivalProgramStore.current.state.collectAsStateWithLifecycle().value
    val displayTitle = screening.localizedTitle(appLanguage)
    val remotePoster = screening.remotePosterUrl(programState.publicConfig.posterBaseURL)
    val posterId = remember(screening.id) { context.posterDrawableId(screening) }
    val corner = if (compact) 12.dp else 16.dp
    val shape = RoundedCornerShape(corner)
    val gradient =
        Brush.linearGradient(
            colors =
                listOf(
                    Color(0xFF1F2E52),
                    Color(0xFF121E3F),
                ),
        )
    val watermarkSize = if (compact) 36.dp else 52.dp
    val dateBadge = dateBadgeFormatter.format(screening.startsAt).replaceFirstChar { c ->
        if (c.isLowerCase()) c.titlecase(locale) else c.toString()
    }

    val chipRound = if (compact) 6.dp else 8.dp
    val chipShape = RoundedCornerShape(chipRound)
    val chipPadH = if (compact) 6.dp else 8.dp
    val chipPadV = if (compact) 4.dp else 6.dp
    val hasPassed = screening.hasPassed
    val showPassedBadge = hasPassed && !screening.isCanceled
    val shadowElevation = if (compact) 7.dp else 10.dp

    val frameModifier =
        if (fixedPosterSize != null) {
            modifier.size(fixedPosterSize.width, fixedPosterSize.height)
        } else {
            modifier.aspectRatio(2f / 3f)
        }

    val chromeModifier =
        if (showBorderAndShadow) {
            Modifier
                .shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.35f),
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                )
                .shadow(
                    elevation = if (compact) 4.dp else 6.dp,
                    shape = shape,
                    clip = false,
                    spotColor = FestivalAccent.copy(alpha = 0.15f),
                    ambientColor = FestivalAccent.copy(alpha = 0.08f),
                )
                .border(1.dp, MaterialTheme.colorScheme.primary, shape)
        } else {
            Modifier
        }

    Box(
        modifier = frameModifier.then(chromeModifier),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .alpha(if (hasPassed) 0.72f else 1f),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(gradient),
            )

            when {
                remotePoster != null -> {
                    AsyncImage(
                        model =
                            ImageRequest.Builder(context)
                                .data(remotePoster)
                                .crossfade(200)
                                .build(),
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                posterId != 0 -> {
                    AsyncImage(
                        model =
                            ImageRequest.Builder(context)
                                .data(posterId)
                                .crossfade(200)
                                .build(),
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(watermarkSize),
                        tint = Color.White.copy(alpha = 0.12f),
                    )
                }
            }

            if (screening.isCanceled) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Thunderstorm,
                            contentDescription = null,
                            modifier = Modifier.size(if (compact) 28.dp else 36.dp),
                            tint = Color.White,
                        )
                        Text(
                            text = stringResource(R.string.detail_screening_canceled),
                            style =
                                if (compact) {
                                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                } else {
                                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                },
                            color = Color.White,
                        )
                    }
                }
            }

            if (showDateBadge) {
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(if (compact) 6.dp else 8.dp),
                    shape = chipShape,
                    color = Color.Black.copy(alpha = if (hasPassed) 0.55f else 0.9f),
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = dateBadge,
                        modifier = Modifier.padding(horizontal = chipPadH, vertical = chipPadV),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.02.sp,
                            ),
                        color = Color.White.copy(alpha = if (hasPassed) 0.85f else 1f),
                    )
                }
            }

            if (hasPassed && !screening.isCanceled) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF9E9E9E).copy(alpha = 0.22f)),
                )
            }

            if (showPassedBadge) {
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp),
                    shape = chipShape,
                    color = Color.White.copy(alpha = 0.92f),
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = stringResource(R.string.screening_passed),
                        modifier = Modifier.padding(horizontal = chipPadH, vertical = chipPadV),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.02.sp,
                            ),
                        color = Color(0xFF383838),
                    )
                }
            }
        }
    }
}
