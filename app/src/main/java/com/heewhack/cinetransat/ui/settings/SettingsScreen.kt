package com.heewhack.cinetransat.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heewhack.cinetransat.AppSupport
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.data.AppLanguage
import com.heewhack.cinetransat.data.rememberAppLanguage
import com.heewhack.cinetransat.notifications.CancellationNotificationManager
import com.heewhack.cinetransat.ui.LocalAppLanguageRepository
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onRequestNotificationPermission: (seasonYear: Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val languageRepository = LocalAppLanguageRepository.current
    val programStore = LocalFestivalProgramStore.current
    val programState by programStore.state.collectAsStateWithLifecycle()
    val notificationManager =
        remember {
            CancellationNotificationManager.getInstance(context.applicationContext)
        }
    val notificationsEnabled by notificationManager.isEnabled.collectAsStateWithLifecycle()
    val notificationsDenied by notificationManager.notificationsDenied.collectAsStateWithLifecycle()
    val appLanguage = rememberAppLanguage()
    val selectedLanguageTag =
        when (appLanguage) {
            AppLanguage.En -> "en"
            AppLanguage.Fr -> "fr"
        }
    val seasonYear = programState.publicConfig.currentSeasonYear
    val languageOptions =
        listOf(
            "fr" to stringResource(R.string.language_french),
            "en" to stringResource(R.string.language_english),
        )

    LaunchedEffect(Unit) {
        notificationManager.refreshPermissionStatus()
    }

    val pink = MaterialTheme.colorScheme.primary
    val mutedPink = pink.copy(alpha = 0.8f)
    val textButtonColors = ButtonDefaults.textButtonColors(contentColor = pink)
    val segmentedColors =
        SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
            activeContentColor = pink,
            inactiveContainerColor = MaterialTheme.colorScheme.surface,
            inactiveContentColor = mutedPink,
        )
    val switchColors =
        SwitchDefaults.colors(
            checkedThumbColor = pink,
            checkedTrackColor = pink.copy(alpha = 0.45f),
            uncheckedThumbColor = mutedPink,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = pink.copy(alpha = 0.35f),
        )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = pink,
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleSmall,
                        color = pink,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        languageOptions.forEachIndexed { index, (tag, label) ->
                            SegmentedButton(
                                selected = selectedLanguageTag == tag,
                                onClick = {
                                    if (selectedLanguageTag != tag) {
                                        scope.launch {
                                            languageRepository.setLanguage(
                                                if (tag == "en") AppLanguage.En else AppLanguage.Fr,
                                            )
                                        }
                                    }
                                },
                                shape =
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = languageOptions.size,
                                    ),
                                colors = segmentedColors,
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(color = pink.copy(alpha = 0.15f)) }

            if (notificationsDenied) {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.settings_notifications_denied),
                                style = MaterialTheme.typography.bodyMedium,
                                color = mutedPink,
                            )
                        },
                    )
                }
            } else {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        headlineContent = {
                            Text(
                                text =
                                    if (notificationsEnabled) {
                                        stringResource(R.string.settings_cancellation_alerts_on)
                                    } else {
                                        stringResource(R.string.settings_cancellation_alerts_off)
                                    },
                                color = pink,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.settings_notifications_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedPink,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        if (enabled) {
                                            val granted =
                                                notificationManager.enableNotifications(
                                                    seasonYear = seasonYear,
                                                )
                                            if (!granted) {
                                                onRequestNotificationPermission(seasonYear)
                                            }
                                        } else {
                                            notificationManager.disableNotifications()
                                        }
                                    }
                                },
                                colors = switchColors,
                            )
                        },
                    )
                }
            }

            item { HorizontalDivider(color = pink.copy(alpha = 0.15f)) }

            item {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        TextButton(
                            onClick = {
                                runCatching {
                                    AppSupport.openFeedback(context, seasonYear)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            colors = textButtonColors,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_send_feedback),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                            )
                        }
                        TextButton(
                            onClick = { openStoreListing(context) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            colors = textButtonColors,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_rate_this_app),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            item { HorizontalDivider(color = pink.copy(alpha = 0.15f)) }

            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_about_app),
                        style = MaterialTheme.typography.titleSmall,
                        color = pink,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_about_version_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = pink,
                        )
                        Text(
                            text = AppSupport.versionLabel(context),
                            style = MaterialTheme.typography.bodyMedium,
                            color = mutedPink,
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_copyright),
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedPink,
                    )
                }
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

