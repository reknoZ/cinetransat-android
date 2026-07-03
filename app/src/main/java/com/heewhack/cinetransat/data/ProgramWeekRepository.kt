package com.heewhack.cinetransat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.programWeekDataStore by preferencesDataStore(name = "program_week")

private val SeasonYearKey = intPreferencesKey("season_year")
private val WeekIdKey = stringPreferencesKey("week_id")

data class ProgramWeekSelection(
    val seasonYear: Int,
    val weekId: String,
)

class ProgramWeekRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _selection = MutableStateFlow(loadInitialSelection())

    val selection: StateFlow<ProgramWeekSelection?> = _selection.asStateFlow()

    init {
        scope.launch {
            appContext.programWeekDataStore.data.collect { prefs ->
                _selection.value = prefs.toSelection()
            }
        }
    }

    fun weekPageIndex(
        seasonYear: Int,
        weeks: List<FestivalWeek>,
    ): Int {
        val saved = _selection.value?.takeIf { it.seasonYear == seasonYear } ?: return 0
        return weeks.indexOfFirst { it.id == saved.weekId }.takeIf { it >= 0 } ?: 0
    }

    fun selectedWeek(
        weeks: List<FestivalWeek>,
        seasonYear: Int,
    ): FestivalWeek? {
        val saved = _selection.value?.takeIf { it.seasonYear == seasonYear }
        return weeks.find { it.id == saved?.weekId } ?: weeks.firstOrNull()
    }

    suspend fun saveSelectedWeek(
        seasonYear: Int,
        weekId: String,
    ) {
        appContext.programWeekDataStore.edit { prefs ->
            prefs[SeasonYearKey] = seasonYear
            prefs[WeekIdKey] = weekId
        }
    }

    private fun loadInitialSelection(): ProgramWeekSelection? =
        runBlocking {
            appContext.programWeekDataStore.data.first().toSelection()
        }

    private fun androidx.datastore.preferences.core.Preferences.toSelection(): ProgramWeekSelection? {
        val year = this[SeasonYearKey] ?: return null
        val weekId = this[WeekIdKey] ?: return null
        return ProgramWeekSelection(seasonYear = year, weekId = weekId)
    }
}
