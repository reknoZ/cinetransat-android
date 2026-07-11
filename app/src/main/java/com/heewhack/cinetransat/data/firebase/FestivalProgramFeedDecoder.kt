package com.heewhack.cinetransat.data.firebase

import com.heewhack.cinetransat.data.FestivalProjectionStart
import com.heewhack.cinetransat.data.FestivalWeek
import com.heewhack.cinetransat.data.FestivalZone
import com.heewhack.cinetransat.data.PosterCatalog
import com.heewhack.cinetransat.data.Screening
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

object FestivalProgramFeedDecoder {
    private val isoParser: DateTimeFormatter =
        DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HH:MM", "Z")
            .optionalEnd()
            .toFormatter()

    fun decodeProgram(data: Map<String, Any?>): DecodedProgram {
        val schemaVersion = (data["schemaVersion"] as? Number)?.toInt()
            ?: throw FestivalProgramFeedException("Missing schemaVersion")
        if (schemaVersion != 2 && schemaVersion != 3) {
            throw FestivalProgramFeedException("Unsupported schemaVersion $schemaVersion")
        }
        val seasonYear =
            (data["seasonYear"] as? Number)?.toInt()
                ?: throw FestivalProgramFeedException("Missing seasonYear")
        @Suppress("UNCHECKED_CAST")
        val weeksRaw = data["weeks"] as? List<Map<String, Any?>>
            ?: throw FestivalProgramFeedException("Missing weeks")

        val weeks =
            weeksRaw.mapIndexed { index, weekMap ->
                val id = weekMap["id"] as? String ?: "week-$index"
                val label = weekMap["label"] as? String ?: ""
                @Suppress("UNCHECKED_CAST")
                val screeningsRaw = weekMap["screenings"] as? List<Map<String, Any?>> ?: emptyList()
                FestivalWeek(
                    weekNumber = index + 1,
                    id = id,
                    label = label,
                    screenings = screeningsRaw.map { mapScreening(it, seasonYear) },
                )
            }
        return DecodedProgram(seasonYear = seasonYear, weeks = weeks)
    }

    private fun mapScreening(data: Map<String, Any?>, seasonYear: Int): Screening {
        val id = data["id"] as? String ?: throw FestivalProgramFeedException("Missing screening id")
        val title = data["title"] as? String ?: throw FestivalProgramFeedException("Missing title for $id")
        val startsAtRaw = data["startsAt"] as? String ?: throw FestivalProgramFeedException("Missing startsAt for $id")
        val sunsetRaw = data["sunset"] as? String ?: throw FestivalProgramFeedException("Missing sunset for $id")
        val sunsetAt = parseDate(sunsetRaw) ?: throw FestivalProgramFeedException("Invalid sunset for $id")
        val parsedStartsAt =
            parseDate(startsAtRaw) ?: throw FestivalProgramFeedException("Invalid startsAt for $id")
        val startsAt = FestivalProjectionStart.resolve(id, seasonYear, sunsetAt, parsedStartsAt)
        val isCanceled = parseIsCanceled(data["isCanceled"])
        val synopsis = data["synopsis"] as? String ?: ""
        val synopsisEn = data["synopsisEn"] as? String
        val runtimeMinutes = (data["runtimeMinutes"] as? Number)?.toInt()
        val legalAge = (data["legalAge"] as? Number)?.toInt()
        val recommendedAge = (data["recommendedAge"] as? Number)?.toInt()
        val searchTitleRaw = data["searchTitle"] as? String
        val posterURL = data["posterURL"] as? String
        val posterKey =
            PosterCatalog.stem(
                displayTitle = title,
                searchTitle = searchTitleRaw,
                explicitPosterKey = data["posterKey"] as? String,
                legacyPosterAssetName = data["posterAssetName"] as? String,
            )
        val searchTitle =
            when {
                !searchTitleRaw.isNullOrBlank() -> searchTitleRaw
                posterKey == "tbd" -> ""
                else -> title
            }
        val audioLanguage = data["audioLanguage"] as? String
        val audioLanguageEn = data["audioLanguageEn"] as? String
        val subtitleLanguage = data["subtitleLanguage"] as? String
        val subtitleLanguageEn = data["subtitleLanguageEn"] as? String
        return Screening(
            id = id,
            title = title,
            startsAt = startsAt,
            sunsetAt = sunsetAt,
            isCanceled = isCanceled,
            synopsis = synopsis,
            synopsisEn = synopsisEn,
            runtimeMinutes = runtimeMinutes,
            legalAge = legalAge,
            recommendedAge = recommendedAge,
            searchTitle = searchTitle,
            posterURL = posterURL,
            posterKey = posterKey,
            audioLanguage = audioLanguage,
            audioLanguageEn = audioLanguageEn,
            subtitleLanguage = subtitleLanguage,
            subtitleLanguageEn = subtitleLanguageEn,
        )
    }

    private fun parseIsCanceled(value: Any?): Boolean =
        when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> false
        }

    private fun parseDate(raw: String): ZonedDateTime? =
        runCatching { OffsetDateTime.parse(raw, isoParser).atZoneSameInstant(FestivalZone) }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(raw).withZoneSameInstant(FestivalZone) }.getOrNull()

    data class DecodedProgram(
        val seasonYear: Int,
        val weeks: List<FestivalWeek>,
    )
}

class FestivalProgramFeedException(message: String) : Exception(message)
