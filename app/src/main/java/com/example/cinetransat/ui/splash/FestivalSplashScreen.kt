package com.example.cinetransat.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinetransat.data.FestivalProgramData
import com.example.cinetransat.ui.LocalFestivalImageLoader
import com.example.cinetransat.ui.theme.FestivalInk
import com.example.cinetransat.ui.theme.FestivalYellow
import kotlinx.coroutines.delay

@Composable
fun FestivalSplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val imageLoader = LocalFestivalImageLoader.current
    val scale = remember { Animatable(0.02f) }
    var showYear by remember { mutableStateOf(false) }
    val splashYear = 2025

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
        )
        showYear = true
        delay(1200)
        onFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(FestivalYellow),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        .scale(scale.value)
                        .size(280.dp)
                        .padding(16.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "$splashYear",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = FestivalInk.copy(alpha = if (showYear) 1f else 0f),
                fontSize = 44.sp,
            )
        }
    }
}
