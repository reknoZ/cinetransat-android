package com.example.cinetransat.ui

import androidx.compose.runtime.staticCompositionLocalOf
import coil.ImageLoader
import com.example.cinetransat.data.WatchListRepository

val LocalWatchListRepository =
    staticCompositionLocalOf<WatchListRepository> {
        error("WatchListRepository not provided")
    }

val LocalFestivalImageLoader =
    staticCompositionLocalOf<ImageLoader> {
        error("ImageLoader not provided")
    }
