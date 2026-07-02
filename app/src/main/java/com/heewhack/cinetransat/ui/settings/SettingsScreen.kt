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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            if (notificationsDenied) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.settings_notifications_denied),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            } else {
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                if (notificationsEnabled) {
                                    stringResource(R.string.settings_cancellation_alerts_on)
                                } else {
                                    stringResource(R.string.settings_cancellation_alerts_off)
                                },
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.settings_notifications_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            )
                        },
                    )
                }
            }

            item { HorizontalDivider() }

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
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_about_version_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = AppSupport.versionName(context),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_copyright),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { HorizontalDivider() }

            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = {
                            runCatching {
                                AppSupport.openFeedback(context, seasonYear)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_send_feedback))
                    }
                    TextButton(
                        onClick = { openStoreListing(context) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_rate_this_app))
                    }
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

