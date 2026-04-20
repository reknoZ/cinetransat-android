package com.example.cinetransat.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinetransat.data.FestivalLocale
import com.example.cinetransat.data.Screening
import com.example.cinetransat.ui.theme.FestivalInk
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateBadgeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM", FestivalLocale)

@Composable
fun MoviePosterCard(
    screening: Screening,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    fixedPosterSize: DpSize? = null,
    showDateBadge: Boolean = true,
) {
    val context = LocalContext.current
    val posterId = remember(screening.id) { context.posterDrawableId(screening) }
    val corner = if (compact) 12.dp else 16.dp
    val shape = RoundedCornerShape(corner)
    val gradient =
        Brush.linearGradient(
            colors =
                listOf(
                    Color(0xFF1F2437),
                    Color(0xFF0D0F1F),
                ),
        )
    val watermarkSize = if (compact) 36.dp else 52.dp
    val dateBadge = dateBadgeFormatter.format(screening.startsAt).replaceFirstChar { c ->
        if (c.isLowerCase()) c.titlecase(FestivalLocale) else c.toString()
    }

    val chipRound = if (compact) 6.dp else 8.dp
    val chipShape = RoundedCornerShape(chipRound)
    val chipPadH = if (compact) 6.dp else 8.dp
    val chipPadV = if (compact) 4.dp else 6.dp

    val frameModifier =
        if (fixedPosterSize != null) {
            modifier.size(fixedPosterSize.width, fixedPosterSize.height)
        } else {
            modifier.aspectRatio(2f / 3f)
        }

    Box(
        modifier = frameModifier.clip(shape),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(gradient),
        )

        if (posterId != 0) {
            AsyncImage(
                model =
                    ImageRequest.Builder(context)
                        .data(posterId)
                        .crossfade(200)
                        .build(),
                contentDescription = screening.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(watermarkSize),
                tint = Color.White.copy(alpha = 0.12f),
            )
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
                        text = "Annulé",
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
            // Keep date fully opaque even when the poster is darkened for cancellations.
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(if (compact) 6.dp else 8.dp),
                shape = chipShape,
                color = Color.Black.copy(alpha = 0.9f),
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
                    color = Color.White,
                )
            }
        }
    }
}
