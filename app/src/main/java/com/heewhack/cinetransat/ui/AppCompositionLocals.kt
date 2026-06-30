package com.heewhack.cinetransat.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.staticCompositionLocalOf
import coil.ImageLoader
import com.heewhack.cinetransat.data.AppLanguage
import com.heewhack.cinetransat.data.AppLanguageRepository
import com.heewhack.cinetransat.data.FestivalProgramStore
import com.heewhack.cinetransat.data.WatchListRepository
import com.heewhack.cinetransat.data.WatchListStatsRepository

val LocalComponentActivity =
    staticCompositionLocalOf<ComponentActivity> {
        error("ComponentActivity not provided")
    }

val LocalAppLanguage =
    staticCompositionLocalOf<AppLanguage> {
        error("AppLanguage not provided")
    }

val LocalAppLanguageRepository =
    staticCompositionLocalOf<AppLanguageRepository> {
        error("AppLanguageRepository not provided")
    }

val LocalWatchListStatsRepository =
    staticCompositionLocalOf<WatchListStatsRepository> {
        error("WatchListStatsRepository not provided")
    }

val LocalWatchListRepository =
    staticCompositionLocalOf<WatchListRepository> {
        error("WatchListRepository not provided")
    }

val LocalFestivalProgramStore =
    staticCompositionLocalOf<FestivalProgramStore> {
        error("FestivalProgramStore not provided")
    }

val LocalFestivalImageLoader =
    staticCompositionLocalOf<ImageLoader> {
        error("ImageLoader not provided")
    }
