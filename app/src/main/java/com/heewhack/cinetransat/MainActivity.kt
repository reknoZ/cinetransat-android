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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.heewhack.cinetransat.ui.LocalWatchListStatsRepository
import com.heewhack.cinetransat.notifications.CancellationNotificationManager
import com.heewhack.cinetransat.ui.LocalFestivalImageLoader
import com.heewhack.cinetransat.ui.LocalFestivalProgramStore
import com.heewhack.cinetransat.ui.LocalAppLanguageRepository
import com.heewhack.cinetransat.ui.LocalComponentActivity
import com.heewhack.cinetransat.ui.LocalWatchListRepository
import com.heewhack.cinetransat.ui.ProvideAppLocale
import com.heewhack.cinetransat.ui.CineTransatApp
import com.heewhack.cinetransat.ui.splash.FestivalSplashScreen
import com.heewhack.cinetransat.ui.theme.CineTransatTheme
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
        val programStore = app.programStore
        val notificationManager = app.cancellationNotificationManager
        val appLanguageRepository = app.appLanguageRepository

        setContent {
            val scope = rememberCoroutineScope()
            val appLanguage by appLanguageRepository.language.collectAsStateWithLifecycle()
            var showSplash by remember { mutableStateOf(true) }
            var postLaunchStarted by remember { mutableStateOf(false) }
            var pendingSeasonYear by remember { mutableStateOf<Int?>(null) }
            var pendingSettingsEnable by remember { mutableStateOf(false) }
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

            CompositionLocalProvider(
                LocalComponentActivity provides this@MainActivity,
                LocalFestivalImageLoader provides imageLoader,
                LocalWatchListRepository provides watchListRepository,
                LocalWatchListStatsRepository provides watchListStatsRepository,
                LocalFestivalProgramStore provides programStore,
                LocalAppLanguageRepository provides appLanguageRepository,
            ) {
                ProvideAppLocale(appLanguage) {
                    CineTransatTheme(dynamicColor = false) {
                        if (showSplash) {
                            FestivalSplashScreen(onFinished = { showSplash = false })
                        } else {
                            LaunchedEffect(postLaunchStarted) {
                                if (postLaunchStarted) return@LaunchedEffect
                                postLaunchStarted = true
                                programStore.completePostLaunchSetup()
                                val seasonYear = programStore.state.value.publicConfig.currentSeasonYear
                                scope.launch {
                                    watchListRepository.syncAnonymousStatsWithLocalWatchList(seasonYear)
                                }
                                if (!notificationManager.shouldShowFirstLaunchPrompt()) return@LaunchedEffect
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingSeasonYear = seasonYear
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationManager.handleFirstLaunchPrompt(seasonYear) { true }
                                }
                            }
                            CineTransatApp(
                                pendingScreeningId = screeningIdFromNotification,
                                onPendingScreeningHandled = { pendingScreeningId.value = null },
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
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as CineTransatApplication
        app.programStore.ensureListening()
        lifecycleScope.launch {
            val seasonYear = app.programStore.state.value.publicConfig.currentSeasonYear
            app.watchListStatsRepository.flushPendingDeltas(seasonYear)
            app.watchListRepository.syncAnonymousStatsWithLocalWatchList(seasonYear)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingScreeningId.value = intent.screeningIdFromNotification()
    }

    private fun Intent.screeningIdFromNotification(): String? =
        getStringExtra(CancellationNotificationManager.EXTRA_SCREENING_ID)
            ?: getStringExtra("screeningId")
}
