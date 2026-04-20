package com.example.cinetransat.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.cinetransat.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val selectedLanguageTag = remember { mutableStateOf(currentLanguageTag()) }
    val languageMenuExpanded = remember { mutableStateOf(false) }
    val languageOptions =
        listOf(
            "fr" to stringResource(R.string.language_french),
            "en" to stringResource(R.string.language_english),
        )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    trailingContent = {
                        TextButton(onClick = { languageMenuExpanded.value = true }) {
                            val selectedLabel =
                                languageOptions.firstOrNull { it.first == selectedLanguageTag.value }?.second
                                    ?: stringResource(R.string.language_french)
                            Text(selectedLabel)
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = languageMenuExpanded.value,
                            onDismissRequest = { languageMenuExpanded.value = false },
                        ) {
                            languageOptions.forEach { (tag, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedLanguageTag.value = tag
                                        AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags(tag),
                                        )
                                        languageMenuExpanded.value = false
                                    },
                                )
                            }
                        }
                    },
                )
            }

            item { HorizontalDivider() }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about_app)) },
                    supportingContent = { Text(stringResource(R.string.settings_about_app_body)) },
                )
            }

            item { HorizontalDivider() }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_rate_this_app)) },
                    supportingContent = { Text(stringResource(R.string.settings_rate_this_app_hint)) },
                    trailingContent = {
                        TextButton(onClick = { openStoreListing(context) }) {
                            Text(stringResource(R.string.settings_rate_now))
                        }
                    },
                )
            }
        }
    }
}

private fun openStoreListing(context: android.content.Context) {
    val packageName = context.packageName
    val marketIntent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val webIntent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(webIntent)
    }
}

private fun currentLanguageTag(): String {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    val chosen = appLocales[0]
    return when (chosen?.language ?: Locale.getDefault().language) {
        "en" -> "en"
        else -> "fr"
    }
}
