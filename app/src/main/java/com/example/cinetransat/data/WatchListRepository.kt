package com.example.cinetransat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.watchListDataStore by preferencesDataStore(name = "watch_list")

private val ScreeningIdsKey = stringSetPreferencesKey("screening_ids")

class WatchListRepository(
    private val context: Context,
) {
    val screeningIds: Flow<Set<String>> =
        context.watchListDataStore.data.map { prefs ->
            prefs[ScreeningIdsKey] ?: emptySet()
        }

    suspend fun toggle(screeningId: String) {
        context.watchListDataStore.edit { prefs ->
            val current = prefs[ScreeningIdsKey]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(screeningId)) {
                current.remove(screeningId)
            }
            prefs[ScreeningIdsKey] = current
        }
    }

    suspend fun setInList(
        screeningId: String,
        inList: Boolean,
    ) {
        context.watchListDataStore.edit { prefs ->
            val current = prefs[ScreeningIdsKey]?.toMutableSet() ?: mutableSetOf()
            if (inList) {
                current.add(screeningId)
            } else {
                current.remove(screeningId)
            }
            prefs[ScreeningIdsKey] = current
        }
    }
}
