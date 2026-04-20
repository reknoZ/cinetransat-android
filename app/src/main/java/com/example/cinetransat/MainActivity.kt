package com.example.cinetransat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.example.cinetransat.data.WatchListRepository
import com.example.cinetransat.ui.LocalFestivalImageLoader
import com.example.cinetransat.ui.LocalWatchListRepository
import com.example.cinetransat.ui.CineTransatApp
import com.example.cinetransat.ui.splash.FestivalSplashScreen
import com.example.cinetransat.ui.theme.CineTransatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val imageLoader =
            ImageLoader.Builder(this)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()
        val watchListRepository = WatchListRepository(applicationContext)

        setContent {
            CompositionLocalProvider(
                LocalFestivalImageLoader provides imageLoader,
                LocalWatchListRepository provides watchListRepository,
            ) {
                CineTransatTheme(dynamicColor = false) {
                    var showSplash by remember { mutableStateOf(true) }
                    if (showSplash) {
                        FestivalSplashScreen(onFinished = { showSplash = false })
                    } else {
                        CineTransatApp()
                    }
                }
            }
        }
    }
}
