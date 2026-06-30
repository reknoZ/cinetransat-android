package com.heewhack.cinetransat.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.heewhack.cinetransat.ui.LocalComponentActivity
import com.heewhack.cinetransat.data.AppLanguage

@Composable
fun ProvideAppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val activity = LocalComponentActivity.current
    val localizedContext =
        remember(language, activity) {
            val configuration = Configuration(activity.resources.configuration)
            configuration.setLocale(language.locale)
            activity.createConfigurationContext(configuration)
        }
    val localizedConfiguration =
        remember(localizedContext) {
            Configuration(localizedContext.resources.configuration)
        }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalAppLanguage provides language,
    ) {
        content()
    }
}
