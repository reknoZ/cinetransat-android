package com.heewhack.cinetransat.data

import android.net.Uri
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

val FestivalZone: ZoneId = ZoneId.of("Europe/Zurich")
val FestivalLocale: Locale = Locale.forLanguageTag("fr-CH")

data class FestivalWeek(
    /** 1-based week index (shown as "Week 1", … in the UI). */
    val weekNumber: Int,
    val id: String,
    val label: String,
    val screenings: List<Screening>,
) {
    val orderedScreenings: List<Screening>
        get() = screenings.sortedBy { it.startsAt }
}

data class Screening(
    val id: String,
    val title: String,
    val startsAt: ZonedDateTime,
    val sunsetAt: ZonedDateTime,
    val isCanceled: Boolean,
    val synopsis: String,
    val synopsisEn: String? = null,
    val runtimeMinutes: Int?,
    val legalAge: Int? = null,
    val recommendedAge: Int? = null,
    val searchTitle: String,
    val posterURL: String? = null,
    val posterKey: String,
    val audioLanguage: String? = null,
    val audioLanguageEn: String? = null,
    val subtitleLanguage: String? = null,
    val subtitleLanguageEn: String? = null,
) {
    val usesTBDPlaceholderPoster: Boolean
        get() = posterKey == "tbd"

    val isProgramAnnounced: Boolean
        get() = !usesTBDPlaceholderPoster

    val externalSearchLinksEnabled: Boolean
        get() = isProgramAnnounced && searchTitle.isNotBlank()

    val releaseYear: Int?
        get() = PosterCatalog.releaseYear(posterKey)

    /** Screening day (Geneva) is before today — shown dimmed with a “past” badge in the UI. */
    val hasPassed: Boolean
        get() {
            val today = LocalDate.now(FestivalZone)
            val screeningDay = startsAt.withZoneSameInstant(FestivalZone).toLocalDate()
            return screeningDay < today
        }

    val festivalDay: LocalDate
        get() = startsAt.withZoneSameInstant(FestivalZone).toLocalDate()
}

/** Screenings scheduled on [date] in Geneva (includes canceled). */
fun List<Screening>.screeningsOn(date: LocalDate): List<Screening> =
    filter { it.festivalDay == date }
        .sortedBy { it.startsAt }

fun List<Screening>.screeningsToday(): List<Screening> =
    screeningsOn(LocalDate.now(FestivalZone))

/** Week pager index for [today] — current week, next upcoming, or last week if the festival ended. */
fun List<FestivalWeek>.indexOfWeekFor(date: LocalDate = LocalDate.now(FestivalZone)): Int {
    if (isEmpty()) return 0
    indexOfFirst { week -> week.orderedScreenings.any { it.festivalDay == date } }
        .takeIf { it >= 0 }
        ?.let { return it }
    indexOfFirst { week -> week.orderedScreenings.any { it.festivalDay >= date } }
        .takeIf { it >= 0 }
        ?.let { return it }
    return lastIndex
}

object ExternalFilmLinks {
    fun imdbSearchUri(title: String): Uri =
        Uri.parse("https://www.imdb.com/find/").buildUpon()
            .appendQueryParameter("q", title)
            .appendQueryParameter("s", "tt")
            .appendQueryParameter("ttype", "ft")
            .build()

    fun allocineSearchUri(title: String): Uri =
        Uri.parse("https://www.allocine.fr/recherche/").buildUpon()
            .appendQueryParameter("q", title)
            .build()
}

/** Bundled fallback when Firestore is unavailable on first launch. */
object FestivalProgramBootstrap {
    const val seasonYear: Int = 2025

    private fun zoned(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 21,
        minute: Int = 45,
    ): ZonedDateTime =
        ZonedDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute), FestivalZone)

    private fun sunset(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 21,
        minute: Int = 18,
    ): ZonedDateTime =
        ZonedDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute), FestivalZone)

    private fun dayId(year: Int, month: Int, day: Int): String =
        String.format(Locale.ROOT, "%04d%02d%02d", year, month, day)

    private fun screening(
        year: Int,
        month: Int,
        day: Int,
        title: String,
        canceled: Boolean = false,
        minutes: Int?,
        synopsis: String,
        searchTitle: String? = null,
        posterKey: String? = null,
    ): Screening {
        val id = dayId(year, month, day)
        val resolvedSearchTitle = searchTitle ?: title
        val resolvedPosterKey =
            PosterCatalog.stem(
                displayTitle = title,
                searchTitle = searchTitle,
                explicitPosterKey = posterKey,
            )
        return Screening(
            id = id,
            title = title,
            startsAt = zoned(year, month, day),
            sunsetAt = sunset(year, month, day),
            isCanceled = canceled,
            synopsis = synopsis,
            runtimeMinutes = minutes,
            searchTitle = resolvedSearchTitle,
            posterKey = resolvedPosterKey,
        )
    }

    val weeks: List<FestivalWeek> =
        listOf(
            FestivalWeek(
                weekNumber = 1,
                id = "2025-w1",
                label = "10–13 juillet",
                screenings =
                    listOf(
                        screening(2025, 7, 10, "Les Bronzés font du ski", minutes = 90, synopsis = "Comédie culte des Bronzés coincés à la montagne."),
                        screening(2025, 7, 11, "Le Vieux qui ne voulait pas fêter son anniversaire", minutes = 114, synopsis = "Road movie absurde et tendre entre Suède et explosion."),
                        screening(2025, 7, 12, "Shaun of the Dead", minutes = 99, synopsis = "Zombie comedy britannique iconique."),
                        screening(2025, 7, 13, "Bottoms", minutes = 91, synopsis = "Comédie déjantée de lycée et club de combat improbable."),
                    ),
            ),
            FestivalWeek(
                weekNumber = 2,
                id = "2025-w2",
                label = "17–20 juillet",
                screenings =
                    listOf(
                        screening(2025, 7, 17, "E.T. l'extra-terrestre", minutes = 115, synopsis = "Le classique Spielberg sur l'amitié et le retour à la maison."),
                        screening(2025, 7, 18, "Les Mitchell contre les machines", minutes = 114, synopsis = "Road trip familial face à une révolte des robots."),
                        screening(2025, 7, 19, "Soirée choréoké", canceled = true, minutes = null, synopsis = "Soirée spéciale — annulée en raison des conditions."),
                        screening(2025, 7, 20, "Au revoir là-haut", canceled = true, minutes = 117, synopsis = "Drame poétique post-Grande Guerre — séance annulée."),
                    ),
            ),
            FestivalWeek(
                weekNumber = 3,
                id = "2025-w3",
                label = "24–27 juillet",
                screenings =
                    listOf(
                        screening(2025, 7, 24, "Marinette", minutes = 95, synopsis = "Biopic sportif sur la footballeuse Marinette Pichon."),
                        screening(2025, 7, 25, "Ninjababy", minutes = 103, synopsis = "Comédie norvégienne d'une grossesse dessinée en ninja."),
                        screening(2025, 7, 26, "Paddington 2", canceled = true, minutes = 103, synopsis = "Aventures de l'ours le plus aimable de Londres — séance annulée."),
                        screening(2025, 7, 27, "Soirée courts-métrages", canceled = true, minutes = null, synopsis = "Programme de courts — annulé."),
                    ),
            ),
            FestivalWeek(
                weekNumber = 4,
                id = "2025-w4",
                label = "31 juillet – 3 août",
                screenings =
                    listOf(
                        screening(2025, 7, 31, "The Holiday", minutes = 136, synopsis = "Romance hivernale entre Los Angeles et la campagne anglaise."),
                        screening(2025, 8, 1, "Ma vie de Courgette", minutes = 66, synopsis = "Stop-motion délicat sur l'enfance et la résilience."),
                        screening(2025, 8, 2, "Terminator 2 : Le Jugement dernier", minutes = 137, synopsis = "Science-fiction d'action avec Schwarzenegger."),
                        screening(2025, 8, 3, "La Famille Asada", minutes = 127, synopsis = "Drame familial japonais autour d'un restaurant et des liens."),
                    ),
            ),
            FestivalWeek(
                weekNumber = 5,
                id = "2025-w5",
                label = "7–10 août",
                screenings =
                    listOf(
                        screening(2025, 8, 7, "Puan", minutes = 110, synopsis = "Comédie argentine sur l'université, la politique et l'amitié."),
                        screening(2025, 8, 8, "Lost in Translation", minutes = 102, synopsis = "Rencontre fugace à Tokyo entre deux âmes en décalage."),
                        screening(2025, 8, 9, "Pulp Fiction", minutes = 154, synopsis = "Anthologie criminelle signée Tarantino."),
                        screening(2025, 8, 10, "North by Northwest", minutes = 136, synopsis = "Thriller hitchcockien à travers les États-Unis."),
                    ),
            ),
            FestivalWeek(
                weekNumber = 6,
                id = "2025-w6",
                label = "14–17 août",
                screenings =
                    listOf(
                        screening(2025, 8, 14, "Soirée rattrapage", minutes = null, synopsis = "Programme variable : films manqués ou invités de la saison."),
                        screening(2025, 8, 15, "Everything Everywhere All at Once", minutes = 139, synopsis = "Multivers délirant et émouvant sur les choix de vie."),
                        screening(2025, 8, 16, "Bãhubali : The Beginning", minutes = 159, synopsis = "Épopée indienne grand spectacle.", searchTitle = "Baahubali The Beginning"),
                        screening(2025, 8, 17, "Le Fabuleux Destin d'Amélie Poulain", minutes = 122, synopsis = "Paris poétique et jeux du hasard."),
                    ),
            ),
        )

    private val screeningById: Map<String, Screening> =
        weeks.flatMap { it.screenings }.associateBy { it.id }

    fun screeningOrNull(id: String): Screening? = screeningById[id]
}

/** @deprecated Use [FestivalProgramStore] — kept for transitional references. */
@Deprecated("Use FestivalProgramStore", ReplaceWith("FestivalProgramBootstrap"))
object FestivalProgramData {
    const val DEMO_YEAR: Int = FestivalProgramBootstrap.seasonYear
    val weeks: List<FestivalWeek> get() = FestivalProgramBootstrap.weeks
    fun screeningOrNull(id: String): Screening? = FestivalProgramBootstrap.screeningOrNull(id)
}
