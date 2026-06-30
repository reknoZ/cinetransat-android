package com.heewhack.cinetransat.data

data class FestivalPublicConfig(
    val currentSeasonYear: Int,
    val websiteURL: String,
    val practicalInfoURL: String,
    val contactEmail: String,
    val facebookURL: String?,
    val instagramURL: String?,
    val posterBaseURL: String?,
) {
    companion object {
        const val DEFAULT_SEASON_YEAR: Int = 2026

        val defaults =
            FestivalPublicConfig(
                currentSeasonYear = DEFAULT_SEASON_YEAR,
                websiteURL = "https://www.cinetransat.ch/",
                practicalInfoURL = "https://www.cinetransat.ch/infos-pratiques",
                contactEmail = "info@cinetransat.ch",
                facebookURL = "https://www.facebook.com/cinetransat",
                instagramURL = "https://www.instagram.com/cinetransat",
                posterBaseURL = "https://cinetransat-497ce.web.app/posters/{posterKey}.jpg",
            )
    }
}

internal fun FestivalPublicConfig.Companion.decodeFromFirestore(data: Map<String, Any?>): FestivalPublicConfig {
    fun string(key: String, fallback: String): String =
        (data[key] as? String)?.takeIf { it.isNotBlank() } ?: fallback

    val year =
        (data["currentSeasonYear"] as? Number)?.toInt()
            ?: (data["seasonYear"] as? Number)?.toInt()
            ?: defaults.currentSeasonYear

    return FestivalPublicConfig(
        currentSeasonYear = year,
        websiteURL = string("websiteURL", defaults.websiteURL),
        practicalInfoURL = string("practicalInfoURL", defaults.practicalInfoURL),
        contactEmail = string("contactEmail", defaults.contactEmail),
        facebookURL = (data["facebookURL"] as? String)?.takeIf { it.isNotBlank() },
        instagramURL = (data["instagramURL"] as? String)?.takeIf { it.isNotBlank() },
        posterBaseURL = (data["posterBaseURL"] as? String)?.takeIf { it.isNotBlank() },
    )
}
