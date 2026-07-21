package com.heewhack.cinetransat.data

import android.content.Context
import android.util.Log
import com.heewhack.cinetransat.CineTransatApplication
import com.heewhack.cinetransat.data.firebase.FestivalProgramFeedDecoder
import com.heewhack.cinetransat.data.firebase.FestivalProgramFeedException
import com.heewhack.cinetransat.data.firebase.FirestorePaths
import com.heewhack.cinetransat.notifications.CancellationNotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File

enum class FestivalDataSource {
    BUNDLED,
    FIRESTORE,
    CACHE,
}

data class FestivalProgramUiState(
    val weeks: List<FestivalWeek> = emptyList(),
    val seasonYear: Int = FestivalPublicConfig.DEFAULT_SEASON_YEAR,
    val availableSeasonYears: List<Int> = FestivalProgramStore.defaultAvailableSeasonYears,
    val publicConfig: FestivalPublicConfig = FestivalPublicConfig.defaults,
    val source: FestivalDataSource = FestivalDataSource.BUNDLED,
    val isLoading: Boolean = false,
    val lastErrorMessage: String? = null,
) {
    val allScreenings: List<Screening>
        get() = weeks.flatMap { it.orderedScreenings }
}

class FestivalProgramStore(
    context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cacheDir = File(appContext.filesDir, "FestivalData").apply { mkdirs() }
    private val knownCanceledPrefs =
        appContext.getSharedPreferences("known_canceled_screenings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(FestivalProgramUiState())
    val state: StateFlow<FestivalProgramUiState> = _state.asStateFlow()

    private var configListener: ListenerRegistration? = null
    private var seasonListener: ListenerRegistration? = null
    private var listeningSeasonYear: Int? = null
    private var postLaunchSetupComplete = false
    private val cancellationLock = Any()

    fun screeningOrNull(id: String): Screening? =
        _state.value.allScreenings.find { it.id == id }

    fun completePostLaunchSetup() {
        startListening()
        attachSeasonListener(_state.value.publicConfig.currentSeasonYear)
        scope.launch {
            refreshFromServer()
            refreshSeasonCatalog()
            CancellationNotificationManager.getInstance(appContext)
                .syncSubscriptionIfNeeded(_state.value.publicConfig.currentSeasonYear)
            postLaunchSetupComplete = true
        }
    }

    fun ensureListening() {
        if (!postLaunchSetupComplete) return
        if (configListener == null) {
            startListening()
        }
        val year = _state.value.seasonYear
        if (seasonListener == null || listeningSeasonYear != year) {
            attachSeasonListener(year)
        }
        scope.launch { refreshFromServer() }
    }

    fun selectSeason(year: Int) {
        if (_state.value.seasonYear == year) return
        attachSeasonListener(year)
        scope.launch {
            try {
                val seasonSnap =
                    firestore.document(FirestorePaths.season(year)).get(Source.SERVER).await()
                seasonSnap.data?.let { data ->
                    applyProgramDocument(data, FestivalDataSource.FIRESTORE, year)
                    cacheProgramDocument(data, year)
                }
            } catch (error: Exception) {
                Log.w(TAG, "Season $year fetch failed: ${error.message}")
            }
        }
    }

    suspend fun refreshFromServer() {
        try {
            val configSnap =
                firestore.document(FirestorePaths.PUBLIC_CONFIG).get(Source.SERVER).await()
            configSnap.data?.let { applyPublicConfigDocument(it, fromServer = true) }
        } catch (error: Exception) {
            Log.w(TAG, "publicConfig server fetch failed: ${error.message}")
        }

        try {
            refreshSeasonCatalog()
        } catch (error: Exception) {
            Log.w(TAG, "Season catalog fetch failed: ${error.message}")
        }

        try {
            val year = _state.value.seasonYear
            val seasonSnap =
                firestore.document(FirestorePaths.season(year)).get(Source.SERVER).await()
            seasonSnap.data?.let { data ->
                Log.d(TAG, "Season $year loaded from server")
                applyProgramDocument(data, FestivalDataSource.FIRESTORE, year)
                cacheProgramDocument(data, year)
                notifyWatchListStatsSync(year)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Season server fetch failed: ${error.message}")
        }
    }

    suspend fun refreshSeasonCatalog() {
        val snapshot = firestore.collection(FirestorePaths.SEASONS_COLLECTION).get().await()
        val years =
            snapshot.documents
                .mapNotNull { it.id.toIntOrNull() }
                .sortedDescending()
        if (years.isNotEmpty()) {
            _state.update { it.copy(availableSeasonYears = years) }
        }
    }

    fun startListening() {
        if (configListener != null) return
        Log.d(TAG, "Subscribing to ${FirestorePaths.PUBLIC_CONFIG}")
        configListener =
            firestore.document(FirestorePaths.PUBLIC_CONFIG).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "publicConfig listener error: ${error.message}")
                    _state.update { it.copy(lastErrorMessage = error.localizedMessage) }
                    return@addSnapshotListener
                }
                val data = snapshot?.data ?: return@addSnapshotListener
                applyPublicConfigDocument(data)
            }
    }

    fun clearError() {
        _state.update { it.copy(lastErrorMessage = null) }
    }

    private fun attachSeasonListener(year: Int) {
        if (listeningSeasonYear == year && seasonListener != null) {
            return
        }
        seasonListener?.remove()
        listeningSeasonYear = year
        _state.update { it.copy(isLoading = true, seasonYear = year) }

        if (_state.value.source != FestivalDataSource.FIRESTORE) {
            loadCachedSeasonIfAvailable(year)
        }

        Log.d(TAG, "Subscribing to ${FirestorePaths.season(year)}")
        seasonListener =
            firestore.document(FirestorePaths.season(year)).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Season $year listener error: ${error.message}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            lastErrorMessage = error.localizedMessage,
                        )
                    }
                    return@addSnapshotListener
                }
                val data = snapshot?.data ?: run {
                    _state.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                Log.d(
                    TAG,
                    "Season $year snapshot fromCache=${snapshot.metadata.isFromCache} " +
                        "pendingWrites=${snapshot.metadata.hasPendingWrites()}",
                )
                applyProgramDocument(data, FestivalDataSource.FIRESTORE, year)
                cacheProgramDocument(data, year)
            }
    }

    private fun applyProgramDocument(
        data: Map<String, Any?>,
        source: FestivalDataSource,
        expectedYear: Int,
    ) {
        if (source == FestivalDataSource.CACHE && _state.value.source == FestivalDataSource.FIRESTORE) {
            return
        }
        try {
            val decoded = FestivalProgramFeedDecoder.decodeProgram(data)
            val mismatch = decoded.seasonYear != expectedYear
            if (source == FestivalDataSource.FIRESTORE) {
                processNewlyCanceledScreenings(decoded.seasonYear, decoded.weeks)
            }
            _state.update {
                it.copy(
                    weeks = decoded.weeks,
                    seasonYear = decoded.seasonYear,
                    source = source,
                    isLoading = false,
                    lastErrorMessage =
                        if (mismatch) {
                            "Season document year ${decoded.seasonYear} does not match id $expectedYear."
                        } else {
                            null
                        },
                )
            }
        } catch (error: FestivalProgramFeedException) {
            _state.update {
                it.copy(
                    isLoading = false,
                    lastErrorMessage = error.localizedMessage,
                )
            }
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    lastErrorMessage = error.localizedMessage,
                )
            }
        }
    }

    private fun processNewlyCanceledScreenings(
        seasonYear: Int,
        weeks: List<FestivalWeek>,
    ) {
        // Past / archive seasons must never fire cancellation alerts.
        if (seasonYear != _state.value.publicConfig.currentSeasonYear) return

        synchronized(cancellationLock) {
            val canceledNow = weeks.flatMap { it.orderedScreenings }.filter { it.isCanceled }
            val canceledIdsNow = canceledNow.map { it.id }.toSet()
            val knownKey = knownCanceledKey(seasonYear)
            val baselineKey = baselineKey(seasonYear)

            if (!knownCanceledPrefs.getBoolean(baselineKey, false)) {
                val existingKnown = knownCanceledPrefs.getStringSet(knownKey, null)
                if (existingKnown != null) {
                    knownCanceledPrefs.edit().putBoolean(baselineKey, true).commit()
                    return
                }
                knownCanceledPrefs.edit()
                    .putStringSet(knownKey, HashSet(canceledIdsNow))
                    .putBoolean(baselineKey, true)
                    .commit()
                return
            }

            val known = knownCanceledPrefs.getStringSet(knownKey, null)?.toSet() ?: emptySet()
            // Only alert for films canceled on their screening day (Geneva calendar).
            val newlyCanceledToday =
                canceledNow.filter { it.id !in known && it.isFestivalDayToday }
            if (newlyCanceledToday.isNotEmpty()) {
                CancellationNotificationManager.getInstance(appContext)
                    .handleNewlyCanceled(newlyCanceledToday, seasonYear)
            }

            knownCanceledPrefs.edit()
                .putStringSet(knownKey, HashSet(canceledIdsNow))
                .commit()
        }
    }

    private fun knownCanceledKey(seasonYear: Int): String = "ids_$seasonYear"

    private fun baselineKey(seasonYear: Int): String = "baseline_$seasonYear"

    private fun notifyWatchListStatsSync(seasonYear: Int) {
        val app = appContext as? CineTransatApplication ?: return
        app.applicationScope.launch {
            app.watchListStatsRepository.onFirestoreServerReachable()
            app.watchListRepository.syncWithFirestore()
        }
    }

    private fun applyPublicConfigDocument(
        data: Map<String, Any?>,
        fromServer: Boolean = false,
    ) {
        try {
            val config = FestivalPublicConfig.decodeFromFirestore(data)
            val previousConfigYear = _state.value.publicConfig.currentSeasonYear
            _state.update { it.copy(publicConfig = config) }
            if (fromServer) {
                Log.d(TAG, "publicConfig loaded from server (season ${config.currentSeasonYear})")
                notifyWatchListStatsSync(config.currentSeasonYear)
            }
            if (listeningSeasonYear == null) {
                attachSeasonListener(config.currentSeasonYear)
            }
            if (config.currentSeasonYear != previousConfigYear) {
                scope.launch {
                    CancellationNotificationManager.getInstance(appContext)
                        .syncSubscriptionIfNeeded(config.currentSeasonYear)
                    refreshSeasonCatalog()
                }
            }
        } catch (error: Exception) {
            _state.update { it.copy(lastErrorMessage = error.localizedMessage) }
        }
    }

    private fun cacheProgramDocument(data: Map<String, Any?>, year: Int) {
        runCatching {
            @Suppress("UNCHECKED_CAST")
            val sanitized = sanitizeForJson(data) as Map<String, Any?>
            val json = JSONObject(sanitized).toString()
            File(cacheDir, "season-$year.json").writeText(json)
        }
    }

    private fun sanitizeForJson(value: Any?): Any? =
        when (value) {
            null -> null
            is Map<*, *> -> value.entries.associate { (key, entryValue) -> key.toString() to sanitizeForJson(entryValue) }
            is List<*> -> value.map { sanitizeForJson(it) }
            is Number, is Boolean, is String -> value
            else -> value.toString()
        }

    private fun loadCachedSeasonIfAvailable(year: Int) {
        val file = File(cacheDir, "season-$year.json")
        if (!file.exists()) return
        runCatching {
            val json = JSONObject(file.readText())
            @Suppress("UNCHECKED_CAST")
            val map = json.toMap() as Map<String, Any?>
            applyProgramDocument(map, FestivalDataSource.CACHE, year)
        }
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        keys().forEach { key ->
            result[key] = jsonValueToKotlin(get(key))
        }
        return result
    }

    private fun jsonValueToKotlin(value: Any?): Any? =
        when (value) {
            null, JSONObject.NULL -> null
            is JSONObject -> value.toMap()
            is org.json.JSONArray -> {
                List(value.length()) { index -> jsonValueToKotlin(value.get(index)) }
            }
            else -> value
        }

    companion object {
        private const val TAG = "FestivalProgramStore"

        val defaultAvailableSeasonYears: List<Int> =
            listOf(FestivalPublicConfig.DEFAULT_SEASON_YEAR, 2025)
                .distinct()
                .sortedDescending()
    }
}
