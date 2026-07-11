package com.heewhack.cinetransat.data.firebase

object FirestorePaths {
    const val SEASONS_COLLECTION = "seasons"
    const val PUBLIC_CONFIG = "cinetransat/publicConfig"

    /** One doc per screening (`yyyyMMdd`), `devices` = [uuid, …]. */
    const val WATCHLIST_DEVICES_COLLECTION = "watchlistDevices"

    fun season(year: Int): String = "$SEASONS_COLLECTION/$year"

    fun watchlistDevices(screeningId: String): String = "$WATCHLIST_DEVICES_COLLECTION/$screeningId"
}
