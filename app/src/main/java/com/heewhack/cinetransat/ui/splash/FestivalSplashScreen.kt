package com.heewhack.cinetransat.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.heewhack.cinetransat.data.FestivalPublicConfig
import com.heewhack.cinetransat.ui.LocalFestivalImageLoader
import com.heewhack.cinetransat.ui.theme.FestivalAccentBright
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AnimateInDurationMillis = 500
private const val HoldAfterAnimateMillis = 150L

@Composable
fun FestivalSplashScreen(
    isLoading: Boolean,
    onAnimationComplete: () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = LocalFestivalImageLoader.current
    val logoScale = remember { Animatable(0.35f) }
    val logoOpacity = remember { Animatable(0.6f) }
    var didRunSequence by remember { mutableStateOf(false) }
    val splashYear = FestivalPublicConfig.DEFAULT_SEASON_YEAR

    LaunchedEffect(Unit) {
        if (didRunSequence) return@LaunchedEffect
        didRunSequence = true
        val animationSpec =
            tween<Float>(durationMillis = AnimateInDurationMillis, easing = FastOutSlowInEasing)
        kotlinx.coroutines.coroutineScope {
            launch { logoScale.animateTo(targetValue = 1f, animationSpec = animationSpec) }
            launch { logoOpacity.animateTo(targetValue = 1f, animationSpec = animationSpec) }
        }
        delay(HoldAfterAnimateMillis)
        onAnimationComplete()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = 36.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoOpacity.value
                        },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(context)
                            .data("file:///android_asset/festival_logo.svg")
                            .build(),
                    contentDescription = "CinéTransat",
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .size(width = 280.dp, height = 160.dp)
                            .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "$splashYear",
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                        ),
                    color = FestivalAccentBright,
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 52.dp)
                            .graphicsLayer {
                                scaleX = if (isLoading) 1f else 0.92f
                                alpha = if (isLoading) 1f else 0f
                            },
                    color = FestivalAccentBright,
                    strokeWidth = 3.dp,
                )
            }
        }
    }
}
