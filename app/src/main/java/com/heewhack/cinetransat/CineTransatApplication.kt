package com.heewhack.cinetransat

import android.app.Application
import com.heewhack.cinetransat.data.AppLanguageRepository
import com.heewhack.cinetransat.data.FestivalProgramStore
import com.heewhack.cinetransat.data.ProgramWeekRepository
import com.heewhack.cinetransat.data.WatchListRepository
import com.heewhack.cinetransat.data.WatchListStatsRepository
import com.heewhack.cinetransat.data.RattrapageVotesRepository
import com.heewhack.cinetransat.notifications.CancellationNotificationManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CineTransatApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var programStore: FestivalProgramStore
        private set

    lateinit var cancellationNotificationManager: CancellationNotificationManager
        private set

    lateinit var appLanguageRepository: AppLanguageRepository
        private set

    lateinit var watchListStatsRepository: WatchListStatsRepository
        private set

    lateinit var watchListRepository: WatchListRepository
        private set

    lateinit var rattrapageVotesRepository: RattrapageVotesRepository
        private set

    lateinit var programWeekRepository: ProgramWeekRepository
        private set

    lateinit var appReviewPromptController: AppReviewPromptController
        private set

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        cancellationNotificationManager = CancellationNotificationManager.getInstance(this)
        cancellationNotificationManager.ensureNotificationChannelReady()
        cancellationNotificationManager.startupSyncSubscription()
        programStore = FestivalProgramStore(applicationContext)
        watchListStatsRepository = WatchListStatsRepository(applicationContext)
        rattrapageVotesRepository = RattrapageVotesRepository(applicationContext)
        appLanguageRepository = AppLanguageRepository(applicationContext)
        programWeekRepository = ProgramWeekRepository(applicationContext)
        appReviewPromptController = AppReviewPromptController(applicationContext)
        watchListRepository =
            WatchListRepository(
                applicationContext,
                watchListStatsRepository,
                applicationScope,
            )
    }
}
