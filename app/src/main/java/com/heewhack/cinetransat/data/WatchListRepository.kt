package com.heewhack.cinetransat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.util.Log
import kotlinx.coroutines.launch

private val Context.watchListDataStore by preferencesDataStore(name = "watch_list")

private val ScreeningIdsKey = stringSetPreferencesKey("screening_ids")

class WatchListRepository(
    private val context: Context,
    private val statsRepository: WatchListStatsRepository,
    private val applicationScope: CoroutineScope,
) {
    val screeningIds: Flow<Set<String>> =
        context.watchListDataStore.data.map { prefs ->
            prefs[ScreeningIdsKey] ?: emptySet()
        }

    suspend fun toggle(
        screeningId: String,
        seasonYear: Int,
    ) {
        val delta = updateLocalWatchList(screeningId) ?: return
        enqueueStatsDelta(screeningId, seasonYear, delta)
    }

    suspend fun setInList(
        screeningId: String,
        seasonYear: Int,
        inList: Boolean,
    ) {
        var delta: Int? = null
        context.watchListDataStore.edit { prefs ->
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
        }
        delta?.let { enqueueStatsDelta(screeningId, seasonYear, it) }
    }

    suspend fun syncAnonymousStatsWithLocalWatchList(seasonYear: Int) {
        val ids = screeningIds.first()
        statsRepository.syncWithLocalWatchList(ids, seasonYear)
    }

    private suspend fun updateLocalWatchList(screeningId: String): Int? {
        var delta: Int? = null
        context.watchListDataStore.edit { prefs ->
            val current = prefs[ScreeningIdsKey]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(screeningId)) {
                current.remove(screeningId)
                delta = -1
            } else {
                delta = 1
            }
            prefs[ScreeningIdsKey] = current
        }
        return delta
    }

    private fun enqueueStatsDelta(
        screeningId: String,
        seasonYear: Int,
        delta: Int,
    ) {
        Log.i("WatchListStats", "enqueue Δ$delta for $screeningId season=$seasonYear")
        applicationScope.launch {
            statsRepository.recordDelta(screeningId, seasonYear, delta)
        }
    }
}
