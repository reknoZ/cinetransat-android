package com.heewhack.cinetransat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
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
private val WeekNumberKey = intPreferencesKey("week_number")

data class ProgramWeekSelection(
    val seasonYear: Int,
    val weekId: String,
    val weekNumber: Int,
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

    /**
     * Page to show for [seasonYear]:
     * - Season switch → same week number if present, else last week
     * - Same season → restore saved week unless that week is fully in the past, then current/upcoming
     */
    fun weekPageIndex(
        seasonYear: Int,
        weeks: List<FestivalWeek>,
    ): Int {
        if (weeks.isEmpty()) return 0
        val currentIdx = weeks.indexOfWeekFor().coerceIn(0, weeks.lastIndex)
        val saved = _selection.value ?: return currentIdx

        if (saved.seasonYear != seasonYear) {
            weeks.indexOfFirst { it.weekNumber == saved.weekNumber }.takeIf { it >= 0 }?.let { return it }
            return weeks.lastIndex
        }

        val savedIdx =
            weeks.indexOfFirst { it.id == saved.weekId }.takeIf { it >= 0 }
                ?: weeks.indexOfFirst { it.weekNumber == saved.weekNumber }.takeIf { it >= 0 }
                ?: return currentIdx

        return if (weeks[savedIdx].isFullyPassed()) currentIdx else savedIdx
    }

    fun selectedWeek(
        weeks: List<FestivalWeek>,
        seasonYear: Int,
    ): FestivalWeek? {
        if (weeks.isEmpty()) return null
        return weeks[weekPageIndex(seasonYear, weeks)]
    }

    suspend fun saveSelectedWeek(
        seasonYear: Int,
        week: FestivalWeek,
    ) {
        appContext.programWeekDataStore.edit { prefs ->
            prefs[SeasonYearKey] = seasonYear
            prefs[WeekIdKey] = week.id
            prefs[WeekNumberKey] = week.weekNumber
        }
    }

    private fun loadInitialSelection(): ProgramWeekSelection? =
        runBlocking {
            appContext.programWeekDataStore.data.first().toSelection()
        }

    private fun androidx.datastore.preferences.core.Preferences.toSelection(): ProgramWeekSelection? {
        val year = this[SeasonYearKey] ?: return null
        val weekId = this[WeekIdKey] ?: return null
        val weekNumber = this[WeekNumberKey] ?: weekNumberFromId(weekId) ?: return null
        return ProgramWeekSelection(seasonYear = year, weekId = weekId, weekNumber = weekNumber)
    }

    companion object {
        private val WeekNumberInId = Regex("""-w(\d+)$""", RegexOption.IGNORE_CASE)

        fun weekNumberFromId(weekId: String): Int? =
            WeekNumberInId.find(weekId)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
}

private fun FestivalWeek.isFullyPassed(today: LocalDate = LocalDate.now(FestivalZone)): Boolean {
    val days = orderedScreenings.map { it.festivalDay }
    return days.isNotEmpty() && days.all { it < today }
}
