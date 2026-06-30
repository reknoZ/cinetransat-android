package com.heewhack.cinetransat.data

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Official screening start times for CinéTransat 2026.
 * Firestore seed data still carries a placeholder [startsAt] of 21:45 on every night;
 * projection actually begins a few minutes after sunset (or at a fixed time for special nights).
 */
object FestivalProjectionStart {
  private val startsByScreeningId2026: Map<String, LocalTime> =
      mapOf(
          // Week 1
          "20260709" to LocalTime.of(21, 44),
          "20260710" to LocalTime.of(21, 43),
          "20260711" to LocalTime.of(21, 43),
          "20260712" to LocalTime.of(21, 42),
          // Week 2
          "20260716" to LocalTime.of(21, 39),
          "20260717" to LocalTime.of(21, 38),
          "20260718" to LocalTime.of(21, 37),
          "20260719" to LocalTime.of(21, 35),
          // Week 3
          "20260723" to LocalTime.of(21, 31),
          "20260724" to LocalTime.of(21, 30),
          "20260725" to LocalTime.of(21, 29),
          "20260726" to LocalTime.of(21, 28),
          // Week 4
          "20260730" to LocalTime.of(21, 0),
          "20260731" to LocalTime.of(21, 21),
          "20260801" to LocalTime.of(21, 20),
          "20260802" to LocalTime.of(21, 19),
          // Week 5
          "20260806" to LocalTime.of(21, 13),
          "20260807" to LocalTime.of(21, 11),
          "20260808" to LocalTime.of(21, 10),
          "20260809" to LocalTime.of(21, 8),
          // Week 6
          "20260813" to LocalTime.of(21, 1),
          "20260814" to LocalTime.of(21, 0),
          "20260815" to LocalTime.of(20, 58),
          "20260816" to LocalTime.of(20, 56),
      )

  fun resolve(
      screeningId: String,
      seasonYear: Int,
      sunsetAt: ZonedDateTime,
      parsedStartsAt: ZonedDateTime,
  ): ZonedDateTime {
    if (seasonYear != 2026) {
      return parsedStartsAt
    }
    val override = startsByScreeningId2026[screeningId] ?: return parsedStartsAt
    return ZonedDateTime.of(sunsetAt.toLocalDate(), override, FestivalZone)
  }
}
