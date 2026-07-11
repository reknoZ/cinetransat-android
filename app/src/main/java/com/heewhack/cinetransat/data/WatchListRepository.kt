package com.heewhack.cinetransat.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.watchListDataStore by preferencesDataStore(name = "watch_list")

private val ScreeningIdsKey = stringSetPreferencesKey("screening_ids")

class WatchListRepository(
    context: Context,
    private val statsRepository: WatchListStatsRepository,
    private val applicationScope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val _screeningIds = MutableStateFlow(loadInitialIds())

    val screeningIds: StateFlow<Set<String>> = _screeningIds.asStateFlow()

    init {
        applicationScope.launch {
            appContext.watchListDataStore.data.collect { prefs ->
                _screeningIds.value = prefs[ScreeningIdsKey] ?: emptySet()
            }
        }
    }

    suspend fun toggle(
        screeningId: String,
        seasonYear: Int,
    ) {
        val delta = updateLocalWatchList(screeningId) ?: return
        enqueueStatsDelta(screeningId, delta)
    }

    suspend fun setInList(
        screeningId: String,
        seasonYear: Int,
        inList: Boolean,
    ) {
        var delta: Int? = null
        appContext.watchListDataStore.edit { prefs ->
            val current = prefs[ScreeningIdsKey]?.toMutableSet() ?: mutableSetOf()
            val wasInList = screeningId in current
            if (inList) {
                if (!wasInList) {
                    current.add(screeningId)
                    delta = 1
                }
            } else if (wasInList) {
                current.remove(screeningId)
                delta = -1
            }
            prefs[ScreeningIdsKey] = current
            _screeningIds.value = current.toSet()
        }
        delta?.let { enqueueStatsDelta(screeningId, it) }
    }

    suspend fun syncWithFirestore() {
        val reconciled = statsRepository.syncWithLocalWatchList(_screeningIds.value)
        if (reconciled != _screeningIds.value) {
            replaceLocalWatchList(reconciled)
        }
    }

    private suspend fun replaceLocalWatchList(ids: Set<String>) {
        appContext.watchListDataStore.edit { prefs ->
            prefs[ScreeningIdsKey] = ids
        }
        _screeningIds.value = ids
        Log.i("WatchList", "Restored ${ids.size} screening(s) from Firestore")
    }

    private suspend fun updateLocalWatchList(screeningId: String): Int? {
        var delta: Int? = null
        appContext.watchListDataStore.edit { prefs ->
            val current = prefs[ScreeningIdsKey]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(screeningId)) {
                current.remove(screeningId)
                delta = -1
            } else {
                delta = 1
            }
            prefs[ScreeningIdsKey] = current
            _screeningIds.value = current.toSet()
        }
        return delta
    }

    private fun loadInitialIds(): Set<String> =
        runBlocking {
            appContext.watchListDataStore.data.first()[ScreeningIdsKey] ?: emptySet()
        }

    private fun enqueueStatsDelta(
        screeningId: String,
        delta: Int,
    ) {
        Log.i("WatchListStats", "enqueue Δ$delta for $screeningId")
        statsRepository.applyOptimisticDelta(screeningId, delta)
        applicationScope.launch {
            statsRepository.recordDelta(screeningId, delta)
        }
    }
}
