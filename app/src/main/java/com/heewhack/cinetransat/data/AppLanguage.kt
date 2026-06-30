package com.heewhack.cinetransat.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.heewhack.cinetransat.ui.LocalAppLanguage
import java.util.Locale

enum class AppLanguage {
    Fr,
    En,
    ;

    val locale: Locale
        get() =
            when (this) {
                Fr -> Locale.forLanguageTag("fr-CH")
                En -> Locale.ENGLISH
            }
}

@Composable
fun rememberAppLanguage(): AppLanguage = LocalAppLanguage.current

@Composable
fun rememberFestivalLocale(): Locale {
    val appLanguage = rememberAppLanguage()
    return remember(appLanguage) { appLanguage.locale }
}

fun Screening.localizedSynopsis(language: AppLanguage): String =
    when (language) {
        AppLanguage.Fr -> synopsis
        AppLanguage.En -> synopsisEn?.takeIf { it.isNotBlank() } ?: synopsis
    }

fun Screening.localizedTitle(language: AppLanguage): String {
    if (language == AppLanguage.Fr) return title
    return when (title) {
        "Soirée choréoké" -> "Choréoké Night"
        "Soirées courts-métrages", "Soirée courts-métrages" -> "Short Film Night"
        "Soirée rattrapage" -> "Make Up Night"
        else -> title
    }
}
