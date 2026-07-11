package com.heewhack.cinetransat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

/** Fixed 2026 navy programme palette (parity with iOS — always dark festival chrome). */
private val FestivalNavyColors =
    darkColorScheme(
        primary = FestivalAccentBright,
        onPrimary = Color.White,
        primaryContainer = FestivalProgramPagerTrack,
        onPrimaryContainer = FestivalProgramTitle,
        secondary = FestivalProgramTitleMuted,
        onSecondary = FestivalProgramBackground,
        background = FestivalProgramBackground,
        onBackground = FestivalProgramTitle,
        surface = FestivalProgramBackground,
        onSurface = FestivalProgramTitle,
        surfaceVariant = FestivalProgramPagerTrack,
        onSurfaceVariant = FestivalProgramTitleMuted,
        outline = FestivalProgramTitleMuted.copy(alpha = 0.35f),
        tertiary = Color(0xFFEF5350),
        onTertiary = Color.White,
    )

@Deprecated("Use FestivalNavyColors", ReplaceWith("FestivalNavyColors"))
private val FestivalDarkColors = FestivalNavyColors

@Deprecated("Use FestivalNavyColors", ReplaceWith("FestivalNavyColors"))
private val FestivalLightColors =
    lightColorScheme(
        primary = FestivalAccent,
        onPrimary = Color.White,
        primaryContainer = FestivalProgramPagerTrack,
        onPrimaryContainer = FestivalProgramTitle,
        secondary = FestivalProgramTitleMuted,
        onSecondary = FestivalProgramBackground,
        background = FestivalProgramBackground,
        onBackground = FestivalProgramTitle,
        surface = FestivalProgramBackground,
        onSurface = FestivalProgramTitle,
        surfaceVariant = FestivalProgramPagerTrack,
        onSurfaceVariant = FestivalProgramTitleMuted,
        outline = FestivalProgramTitleMuted.copy(alpha = 0.35f),
        tertiary = Color(0xFFC62828),
        onTertiary = Color.White,
    )

@Composable
fun CineTransatTheme(
    /** Festival UI always uses the navy palette (iOS parity). */
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalView.current.context
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> FestivalNavyColors
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
