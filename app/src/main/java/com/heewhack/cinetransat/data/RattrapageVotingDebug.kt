package com.heewhack.cinetransat.data

/**
 * TEMPORARY — pretend cancellations / Rattrapage host for local voting UI tests.
 * Set [isEnabled] to false before release.
 */
object RattrapageVotingDebug {
    /** Flip to `true` only for local UI tests (fake cancellations / Rattrapage host). */
    const val isEnabled: Boolean = false

    /** Week 1 (9–12 Jul) + Thu 16 Jul. */
    val pretendCanceledIds: Set<String> =
        setOf(
            "20260709",
            "20260710",
            "20260711",
            "20260712",
            "20260716",
        )

    /** 2026 has no Soirée Rattrapage yet — host the poll on courts-métrages. */
    const val pretendRattrapageScreeningId: String = "20260719"

    fun isPretendCanceled(screeningId: String): Boolean =
        isEnabled && screeningId in pretendCanceledIds

    fun isPretendRattrapage(screeningId: String): Boolean =
        isEnabled && screeningId == pretendRattrapageScreeningId
}
