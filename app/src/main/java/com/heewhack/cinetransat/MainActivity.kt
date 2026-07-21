package com.heewhack.cinetransat

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.heewhack.cinetransat.data.FestivalZone
import com.heewhack.cinetransat.ui.LocalWatchListStatsRepository
import com.heewhack.cinetransat.notifications.CancellationNotificationManager
import com.heewhack.cinetransat.ui.AppReviewPromptDialog
import com.heewhack.cinetransat.ui.LocalFestivalImageLoader
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.LocalAppLanguageRepository
import com.heewhack.cinetransat.ui.LocalComponentActivity
import com.heewhack.cinetransat.ui.LocalProgramWeekRepository
import com.heewhack.cinetransat.ui.LocalWatchListRepository
import com.heewhack.cinetransat.ui.LocalRattrapageVotesRepository
import com.heewhack.cinetransat.ui.ProvideAppLocale
import com.heewhack.cinetransat.ui.CineTransatApp
import com.heewhack.cinetransat.ui.splash.FestivalSplashScreen
import com.heewhack.cinetransat.ui.theme.CineTransatTheme
import com.heewhack.cinetransat.calendar.ScreeningCalendarService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingScreeningId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingScreeningId.value = intent.screeningIdFromNotification()
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
        val app = application as CineTransatApplication
        val watchListRepository = app.watchListRepository
        val watchListStatsRepository = app.watchListStatsRepository
        val rattrapageVotesRepository = app.rattrapageVotesRepository
        val programStore = app.programStore
        val notificationManager = app.cancellationNotificationManager
        val appLanguageRepository = app.appLanguageRepository
        val programWeekRepository = app.programWeekRepository
        val reviewPrompt = app.appReviewPromptController
        reviewPrompt.bind(this)

        setContent {
            val scope = rememberCoroutineScope()
            val appLanguage by appLanguageRepository.language.collectAsStateWithLifecycle()
            var showSplash by remember { mutableStateOf(true) }
            var launchIsLoading by remember { mutableStateOf(false) }
            var postLaunchStarted by remember { mutableStateOf(false) }
            var pendingSeasonYear by remember { mutableStateOf<Int?>(null) }
            var pendingSettingsEnable by remember { mutableStateOf(false) }
            var pendingCalendarPermissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
            var showRattrapageVotingOpenAlert by remember { mutableStateOf(false) }
            val screeningIdFromNotification by pendingScreeningId

            val permissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    val seasonYear = pendingSeasonYear ?: programStore.state.value.publicConfig.currentSeasonYear
                    scope.launch {
                        if (pendingSettingsEnable) {
                            notificationManager.completePermissionResult(seasonYear, granted)
                            pendingSettingsEnable = false
                        } else {
                            notificationManager.completeFirstLaunchPrompt(seasonYear, granted)
                        }
                        pendingSeasonYear = null
                    }
                }

            val calendarPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { grants ->
                    pendingCalendarPermissionCallback?.invoke(grants.values.all { it })
                    pendingCalendarPermissionCallback = null
                }

            val requestCalendarPermissions: (onResult: (Boolean) -> Unit) -> Unit = { onResult ->
                if (ScreeningCalendarService.hasCalendarPermissions(this@MainActivity)) {
                    onResult(true)
                } else {
                    pendingCalendarPermissionCallback = onResult
                    calendarPermissionLauncher.launch(ScreeningCalendarService.calendarPermissions)
                }
            }

            CompositionLocalProvider(
                LocalComponentActivity provides this@MainActivity,
                LocalFestivalImageLoader provides imageLoader,
                LocalWatchListRepository provides watchListRepository,
                LocalWatchListStatsRepository provides watchListStatsRepository,
                LocalRattrapageVotesRepository provides rattrapageVotesRepository,
                LocalFestivalProgramStore provides programStore,
                LocalAppLanguageRepository provides appLanguageRepository,
                LocalProgramWeekRepository provides programWeekRepository,
            ) {
                ProvideAppLocale(appLanguage) {
                    CineTransatTheme(dynamicColor = false) {
                        val presentingReview by reviewPrompt.isPresentingPrompt.collectAsStateWithLifecycle()
                        val pendingReviewFeedback by reviewPrompt.pendingFeedback.collectAsStateWithLifecycle()

                        if (showSplash) {
                            FestivalSplashScreen(
                                isLoading = launchIsLoading,
                                onAnimationComplete = {
                                    if (postLaunchStarted) return@FestivalSplashScreen
                                    postLaunchStarted = true
                                    launchIsLoading = true
                                    scope.launch {
                                        programStore.completePostLaunchSetup()
                                        val seasonYear = programStore.state.value.publicConfig.currentSeasonYear
                                        watchListRepository.syncWithFirestore()
                                        showSplash = false
                                        reviewPrompt.setMainUiVisible(true)
                                        if (notificationManager.shouldShowFirstLaunchPrompt()) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                pendingSeasonYear = seasonYear
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                notificationManager.handleFirstLaunchPrompt(seasonYear) { true }
                                            }
                                        }
                                        if (shouldShowRattrapageVotingOpenPrompt(seasonYear, programStore.state.value.publicConfig.rattrapageVotingOpen)) {
                                            showRattrapageVotingOpenAlert = true
                                        }
                                    }
                                },
                            )
                        } else {
                            CineTransatApp(
                                pendingScreeningId = screeningIdFromNotification,
                                onPendingScreeningHandled = { pendingScreeningId.value = null },
                                onRequestCalendarPermissions = requestCalendarPermissions,
                                onRequestNotificationPermission = { seasonYear ->
                                    pendingSeasonYear = seasonYear
                                    pendingSettingsEnable = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        scope.launch {
                                            notificationManager.completePermissionResult(seasonYear, true)
                                            pendingSettingsEnable = false
                                            pendingSeasonYear = null
                                        }
                                    }
                                },
                            )
                        }

                        if (presentingReview) {
                            AppReviewPromptDialog(
                                onSubmit = reviewPrompt::submitRating,
                                onNotNow = {
                                    reviewPrompt.dismissWithoutAction()
                                },
                                onDismissRequest = {
                                    reviewPrompt.dismissWithoutAction()
                                },
                            )
                        }

                        if (showRattrapageVotingOpenAlert) {
                            AlertDialog(
                                onDismissRequest = { showRattrapageVotingOpenAlert = false },
                                title = { Text(stringResource(R.string.rattrapage_voting_open_title)) },
                                text = { Text(stringResource(R.string.rattrapage_voting_open_message)) },
                                confirmButton = {
                                    TextButton(onClick = { showRattrapageVotingOpenAlert = false }) {
                                        Text(stringResource(R.string.rattrapage_voting_open_ok))
                                    }
                                },
                            )
                        }

                        LaunchedEffect(pendingReviewFeedback) {
                            if (!pendingReviewFeedback) return@LaunchedEffect
                            reviewPrompt.clearPendingFeedback()
                            val seasonYear = programStore.state.value.publicConfig.currentSeasonYear
                            runCatching {
                                AppSupport.openFeedback(this@MainActivity, seasonYear)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as CineTransatApplication
        app.appReviewPromptController.onForeground()
        app.programStore.ensureListening()
        lifecycleScope.launch {
            app.watchListStatsRepository.flushPendingDeltas()
            app.watchListRepository.syncWithFirestore()
        }
    }

    override fun onPause() {
        (application as CineTransatApplication).appReviewPromptController.onBackground()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingScreeningId.value = intent.screeningIdFromNotification()
    }

    private fun Intent.screeningIdFromNotification(): String? =
        getStringExtra(CancellationNotificationManager.EXTRA_SCREENING_ID)
            ?: getStringExtra("screeningId")

    private fun shouldShowRattrapageVotingOpenPrompt(seasonYear: Int, votingOpen: Boolean): Boolean {
        if (!votingOpen) return false
        val prefs = getSharedPreferences(RATTRAPAGE_VOTING_PROMPT_PREFS, MODE_PRIVATE)
        val today = LocalDate.now(FestivalZone).format(DateTimeFormatter.BASIC_ISO_DATE)
        val key = "shown.$seasonYear"
        if (prefs.getString(key, null) == today) return false
        prefs.edit().putString(key, today).apply()
        return true
    }

    companion object {
        private const val RATTRAPAGE_VOTING_PROMPT_PREFS = "rattrapage_voting_open_prompt"
    }
}
