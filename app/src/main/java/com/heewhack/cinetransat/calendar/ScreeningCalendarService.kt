package com.heewhack.cinetransat.calendar

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.data.AppLanguage
import com.heewhack.cinetransat.data.Screening
import com.heewhack.cinetransat.data.localizedTitle
import java.time.ZonedDateTime

object ScreeningCalendarService {
    private const val DEFAULT_DURATION_MINUTES = 120
    private const val SCREENING_ID_NOTE_PREFIX = "cinetransat-screening:"

    sealed class BatchAddResult {
        data class Added(val count: Int) : BatchAddResult()

        data class Partial(val added: Int, val failed: Int) : BatchAddResult()

        data object Denied : BatchAddResult()

        data object Empty : BatchAddResult()
    }

    val calendarPermissions: Array<String> =
        arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
        )

    fun location(language: AppLanguage): String =
        when (language) {
            AppLanguage.Fr -> "Parc de la Perle du Lac, rue de Lausanne, 1202 Genève"
            AppLanguage.En -> "La Perle du Lac park, rue de Lausanne, 1202 Geneva"
        }

    fun insertIntent(
        context: Context,
        screening: Screening,
        language: AppLanguage,
    ): Intent {
        val startMillis = screening.startsAt.toInstant().toEpochMilli()
        val endMillis = endDate(screening).toInstant().toEpochMilli()
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, eventTitle(context, screening, language))
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location(language))
            putExtra(CalendarContract.Events.DESCRIPTION, eventNotes(screening, language))
            putExtra(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Zurich")
        }
    }

    fun hasCalendarPermissions(context: Context): Boolean =
        calendarPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun addUpcomingScreenings(
        context: Context,
        screenings: List<Screening>,
        language: AppLanguage,
    ): BatchAddResult {
        val eligible = screenings.filter { !it.isCanceled && !it.hasPassed }
        if (eligible.isEmpty()) return BatchAddResult.Empty
        if (!hasCalendarPermissions(context)) return BatchAddResult.Denied

        val calendarId = defaultCalendarId(context) ?: return BatchAddResult.Partial(added = 0, failed = eligible.size)

        var added = 0
        var failed = 0
        for (screening in eligible) {
            if (insertEvent(context, screening, language, calendarId)) {
                added++
            } else {
                failed++
            }
        }
        return if (failed == 0) {
            BatchAddResult.Added(added)
        } else {
            BatchAddResult.Partial(added = added, failed = failed)
        }
    }

    private fun insertEvent(
        context: Context,
        screening: Screening,
        language: AppLanguage,
        calendarId: Long,
    ): Boolean {
        val values =
            ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, eventTitle(context, screening, language))
                put(CalendarContract.Events.EVENT_LOCATION, location(language))
                put(CalendarContract.Events.DESCRIPTION, eventNotes(screening, language))
                put(CalendarContract.Events.DTSTART, screening.startsAt.toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, endDate(screening).toInstant().toEpochMilli())
                put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Zurich")
                put(CalendarContract.Events.HAS_ALARM, 0)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            }
        return context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) != null
    }

    private fun defaultCalendarId(context: Context): Long? {
        val projection =
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.IS_PRIMARY,
            )
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            null,
        )?.use { cursor ->
            var fallbackId: Long? = null
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val isPrimary = cursor.getInt(1) == 1
                if (isPrimary) return id
                if (fallbackId == null) fallbackId = id
            }
            return fallbackId
        }
        return null
    }

    private fun endDate(screening: Screening): ZonedDateTime {
        val minutes = screening.runtimeMinutes?.takeIf { it > 0 } ?: DEFAULT_DURATION_MINUTES
        return screening.startsAt.plusMinutes(minutes.toLong())
    }

    private fun eventTitle(
        context: Context,
        screening: Screening,
        language: AppLanguage,
    ): String {
        val base = "CinéTransat — ${screening.localizedTitle(language)}"
        return if (screening.isCanceled) {
            val badge = context.getString(R.string.screening_canceled_badge)
            "[$badge] $base"
        } else {
            base
        }
    }

    private fun eventNotes(screening: Screening, language: AppLanguage): String {
        val freeLine =
            when (language) {
                AppLanguage.Fr -> "Projection gratuite en plein air."
                AppLanguage.En -> "Free open-air screening."
            }
        return """
            $freeLine
            $SCREENING_ID_NOTE_PREFIX${screening.id}
            """.trimIndent()
    }
}
