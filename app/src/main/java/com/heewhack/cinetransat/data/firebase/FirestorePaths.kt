package com.heewhack.cinetransat.data.firebase

object FirestorePaths {
    const val SEASONS_COLLECTION = "seasons"
    const val PUBLIC_CONFIG = "cinetransat/publicConfig"
    const val WATCHLIST_STATS_COLLECTION = "watchlistStats"

    fun season(year: Int): String = "$SEASONS_COLLECTION/$year"

    fun watchlistStat(screeningId: String): String = "$WATCHLIST_STATS_COLLECTION/$screeningId"
}
