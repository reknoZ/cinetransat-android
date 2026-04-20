package com.example.cinetransat.data

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
    val runtimeMinutes: Int?,
    val searchTitle: String,
)

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

object FestivalProgramData {
    const val DEMO_YEAR: Int = 2025

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

    private fun screening(
        id: String,
        title: String,
        start: ZonedDateTime,
        sunset: ZonedDateTime,
        canceled: Boolean = false,
        minutes: Int?,
        synopsis: String,
        searchTitle: String? = null,
    ): Screening =
        Screening(
            id = id,
            title = title,
            startsAt = start,
            sunsetAt = sunset,
            isCanceled = canceled,
            synopsis = synopsis,
            runtimeMinutes = minutes,
            searchTitle = searchTitle ?: title,
        )

    val weeks: List<FestivalWeek> = listOf(
        FestivalWeek(
            weekNumber = 1,
            id = "2025-w1",
            label = "10–13 juillet",
            screenings = listOf(
                screening(
                    "2025-w1-0",
                    "Les Bronzés font du ski",
                    zoned(DEMO_YEAR, 7, 10),
                    sunset(DEMO_YEAR, 7, 10),
                    minutes = 90,
                    synopsis = "Comédie culte des Bronzés coincés à la montagne.",
                ),
                screening(
                    "2025-w1-1",
                    "Le Vieux qui ne voulait pas fêter son anniversaire",
                    zoned(DEMO_YEAR, 7, 11),
                    sunset(DEMO_YEAR, 7, 11),
                    minutes = 114,
                    synopsis = "Road movie absurde et tendre entre Suède et explosion.",
                ),
                screening(
                    "2025-w1-2",
                    "Shaun of the Dead",
                    zoned(DEMO_YEAR, 7, 12),
                    sunset(DEMO_YEAR, 7, 12),
                    minutes = 99,
                    synopsis = "Zombie comedy britannique iconique.",
                ),
                screening(
                    "2025-w1-3",
                    "Bottoms",
                    zoned(DEMO_YEAR, 7, 13),
                    sunset(DEMO_YEAR, 7, 13),
                    minutes = 91,
                    synopsis = "Comédie déjantée de lycée et club de combat improbable.",
                ),
            ),
        ),
        FestivalWeek(
            weekNumber = 2,
            id = "2025-w2",
            label = "17–20 juillet",
            screenings = listOf(
                screening(
                    "2025-w2-0",
                    "E.T. l'extra-terrestre",
                    zoned(DEMO_YEAR, 7, 17),
                    sunset(DEMO_YEAR, 7, 17),
                    minutes = 115,
                    synopsis = "Le classique Spielberg sur l'amitié et le retour à la maison.",
                ),
                screening(
                    "2025-w2-1",
                    "Les Mitchell contre les machines",
                    zoned(DEMO_YEAR, 7, 18),
                    sunset(DEMO_YEAR, 7, 18),
                    minutes = 114,
                    synopsis = "Road trip familial face à une révolte des robots.",
                ),
                screening(
                    "2025-w2-2",
                    "Soirée choréoké",
                    zoned(DEMO_YEAR, 7, 19),
                    sunset(DEMO_YEAR, 7, 19),
                    canceled = true,
                    minutes = null,
                    synopsis = "Soirée spéciale — annulée en raison des conditions.",
                ),
                screening(
                    "2025-w2-3",
                    "Au revoir là-haut",
                    zoned(DEMO_YEAR, 7, 20),
                    sunset(DEMO_YEAR, 7, 20),
                    canceled = true,
                    minutes = 117,
                    synopsis = "Drame poétique post-Grande Guerre — séance annulée.",
                ),
            ),
        ),
        FestivalWeek(
            weekNumber = 3,
            id = "2025-w3",
            label = "24–27 juillet",
            screenings = listOf(
                screening(
                    "2025-w3-0",
                    "Marinette",
                    zoned(DEMO_YEAR, 7, 24),
                    sunset(DEMO_YEAR, 7, 24),
                    minutes = 95,
                    synopsis = "Biopic sportif sur la footballeuse Marinette Pichon.",
                ),
                screening(
                    "2025-w3-1",
                    "Ninjababy",
                    zoned(DEMO_YEAR, 7, 25),
                    sunset(DEMO_YEAR, 7, 25),
                    minutes = 103,
                    synopsis = "Comédie norvégienne d'une grossesse dessinée en ninja.",
                ),
                screening(
                    "2025-w3-2",
                    "Paddington 2",
                    zoned(DEMO_YEAR, 7, 26),
                    sunset(DEMO_YEAR, 7, 26),
                    canceled = true,
                    minutes = 103,
                    synopsis = "Aventures de l'ours le plus aimable de Londres — séance annulée.",
                ),
                screening(
                    "2025-w3-3",
                    "Soirée courts-métrages",
                    zoned(DEMO_YEAR, 7, 27),
                    sunset(DEMO_YEAR, 7, 27),
                    canceled = true,
                    minutes = null,
                    synopsis = "Programme de courts — annulé.",
                ),
            ),
        ),
        FestivalWeek(
            weekNumber = 4,
            id = "2025-w4",
            label = "31 juillet – 3 août",
            screenings = listOf(
                screening(
                    "2025-w4-0",
                    "The Holiday",
                    zoned(DEMO_YEAR, 7, 31),
                    sunset(DEMO_YEAR, 7, 31),
                    minutes = 136,
                    synopsis = "Romance hivernale entre Los Angeles et la campagne anglaise.",
                ),
                screening(
                    "2025-w4-1",
                    "Ma vie de Courgette",
                    zoned(DEMO_YEAR, 8, 1),
                    sunset(DEMO_YEAR, 8, 1),
                    minutes = 66,
                    synopsis = "Stop-motion délicat sur l'enfance et la résilience.",
                ),
                screening(
                    "2025-w4-2",
                    "Terminator 2 : Le Jugement dernier",
                    zoned(DEMO_YEAR, 8, 2),
                    sunset(DEMO_YEAR, 8, 2),
                    minutes = 137,
                    synopsis = "Science-fiction d'action avec Schwarzenegger.",
                ),
                screening(
                    "2025-w4-3",
                    "La Famille Asada",
                    zoned(DEMO_YEAR, 8, 3),
                    sunset(DEMO_YEAR, 8, 3),
                    minutes = 127,
                    synopsis = "Drame familial japonais autour d'un restaurant et des liens.",
                ),
            ),
        ),
        FestivalWeek(
            weekNumber = 5,
            id = "2025-w5",
            label = "7–10 août",
            screenings = listOf(
                screening(
                    "2025-w5-0",
                    "Puan",
                    zoned(DEMO_YEAR, 8, 7),
                    sunset(DEMO_YEAR, 8, 7),
                    minutes = 110,
                    synopsis = "Comédie argentine sur l'université, la politique et l'amitié.",
                ),
                screening(
                    "2025-w5-1",
                    "Lost in Translation",
                    zoned(DEMO_YEAR, 8, 8),
                    sunset(DEMO_YEAR, 8, 8),
                    minutes = 102,
                    synopsis = "Rencontre fugace à Tokyo entre deux âmes en décalage.",
                ),
                screening(
                    "2025-w5-2",
                    "Pulp Fiction",
                    zoned(DEMO_YEAR, 8, 9),
                    sunset(DEMO_YEAR, 8, 9),
                    minutes = 154,
                    synopsis = "Anthologie criminelle signée Tarantino.",
                ),
                screening(
                    "2025-w5-3",
                    "North by Northwest",
                    zoned(DEMO_YEAR, 8, 10),
                    sunset(DEMO_YEAR, 8, 10),
                    minutes = 136,
                    synopsis = "Thriller hitchcockien à travers les États-Unis.",
                ),
            ),
        ),
        FestivalWeek(
            weekNumber = 6,
            id = "2025-w6",
            label = "14–17 août",
            screenings = listOf(
                screening(
                    "2025-w6-0",
                    "Soirée rattrapage",
                    zoned(DEMO_YEAR, 8, 14),
                    sunset(DEMO_YEAR, 8, 14),
                    minutes = null,
                    synopsis = "Programme variable : films manqués ou invités de la saison.",
                ),
                screening(
                    "2025-w6-1",
                    "Everything Everywhere All at Once",
                    zoned(DEMO_YEAR, 8, 15),
                    sunset(DEMO_YEAR, 8, 15),
                    minutes = 139,
                    synopsis = "Multivers délirant et émouvant sur les choix de vie.",
                ),
                screening(
                    "2025-w6-2",
                    "Bãhubali : The Beginning",
                    zoned(DEMO_YEAR, 8, 16),
                    sunset(DEMO_YEAR, 8, 16),
                    minutes = 159,
                    synopsis = "Épopée indienne grand spectacle.",
                    searchTitle = "Baahubali The Beginning",
                ),
                screening(
                    "2025-w6-3",
                    "Le Fabuleux Destin d'Amélie Poulain",
                    zoned(DEMO_YEAR, 8, 17),
                    sunset(DEMO_YEAR, 8, 17),
                    minutes = 122,
                    synopsis = "Paris poétique et jeux du hasard.",
                ),
            ),
        ),
    )

    private val screeningById: Map<String, Screening> =
        weeks.flatMap { it.screenings }.associateBy { it.id }

    fun screeningOrNull(id: String): Screening? = screeningById[id]
}
