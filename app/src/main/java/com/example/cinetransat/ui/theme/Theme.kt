package com.example.cinetransat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DeepNavy = Color(0xFF0D111A)
private val NightBlue = Color(0xFF141A2E)
private val AccentBlue = Color(0xFF5B7CFF)

private val DarkColors =
    darkColorScheme(
        primary = AccentBlue,
        onPrimary = Color.White,
        primaryContainer = NightBlue,
        onPrimaryContainer = Color(0xFFE8ECFF),
        secondary = Color(0xFF9AA8D4),
        onSecondary = DeepNavy,
        background = DeepNavy,
        onBackground = Color(0xFFE8ECF5),
        surface = NightBlue,
        onSurface = Color(0xFFE8ECF5),
        surfaceVariant = Color(0xFF252B3D),
        onSurfaceVariant = Color(0xFFB4BCCF),
    )

private val FestivalLightColors =
    lightColorScheme(
        primary = Color(0xFF2F4FA0),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8EEFF),
        onPrimaryContainer = Color(0xFF0F1F4A),
        secondary = Color(0xFF5D4037),
        onSecondary = Color.White,
        background = FestivalYellow,
        onBackground = FestivalInk,
        surface = FestivalYellowSurface,
        onSurface = FestivalInk,
        surfaceVariant = Color(0xFFFFF0B0),
        onSurfaceVariant = Color(0xFF3E3E30),
        tertiary = Color(0xFFC62828),
        onTertiary = Color.White,
    )

@Composable
fun CineTransatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Festival UI uses fixed yellow; keep `false` for parity with iOS. */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColors
            else -> FestivalLightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
