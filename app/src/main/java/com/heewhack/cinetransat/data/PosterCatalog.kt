package com.heewhack.cinetransat.data

object PosterCatalog {
    private val stemsByTitle: Map<String, String> =
        mapOf(
            "Les Bronzés font du ski" to "les-bronzes-font-du-ski",
            "Le Vieux qui ne voulait pas fêter son anniversaire" to "le-vieux-qui-ne-voulait-pas-feter-son-anniversaire",
            "Shaun of the Dead" to "shaun-of-the-dead",
            "Bottoms" to "bottoms",
            "E.T. l'extra-terrestre" to "e.t.-the-extraterrestial",
            "Les Mitchell contre les machines" to "the-mitchells-vs-the-machines",
            "Soirée choréoké" to "soirée-choréoké",
            "Au revoir là-haut" to "au-revoir-la-haut",
            "Marinette" to "marinette",
            "Ninjababy" to "ninjababy",
            "Paddington 2" to "paddington-2",
            "Soirée courts-métrages" to "soirée-court-métrages",
            "The Holiday" to "the-holiday",
            "Ma vie de Courgette" to "ma-vie-de-courgette",
            "Terminator 2 : Le Jugement dernier" to "terminator-2-judgment-day",
            "La Famille Asada" to "la-famille-asada",
            "Puan" to "puan",
            "Lost in Translation" to "lost-in-translation",
            "Pulp Fiction" to "pulp-fiction",
            "North by Northwest" to "north-by-northwest",
            "Soirée rattrapage" to "soirée-rattrapage",
            "Everything Everywhere All at Once" to "everything-everywhere-all-at-once",
            "Bãhubali : The Beginning" to "bahubali-the-beginning",
            "Bãhubali: The Beginning" to "bahubali-the-beginning",
            "Le Fabuleux Destin d'Amélie Poulain" to "le-fabuleux-destin-d'amelie-poulain",
        )

    private val releaseYearByPosterKey: Map<String, Int> =
        mapOf(
            "back-to-the-future" to 1985,
            "la-famille-belier" to 2014,
            "billy-elliot" to 2000,
            "flow" to 2024,
            "sauvages" to 2023,
            "love-and-other-disasters" to 2006,
            "paddington" to 2014,
            "i-am-not-a-witch" to 2017,
            "singin-in-the-rain" to 1952,
            "wadjda" to 2012,
            "the-girl-who-leapt-through-time" to 2006,
            "jumanji-welcome-to-the-jungle" to 2017,
            "bon-schuur-ticino" to 2024,
            "portrait-de-la-jeune-fille-en-feu" to 2019,
            "blackkklansman" to 2018,
            "much-ado-about-nothing" to 1993,
            "the-mummy" to 1999,
            "lo-que-quisimos-ser" to 2024,
            "oceans-eleven" to 2001,
            "everything-everywhere-all-at-once" to 2022,
            "baahubali-2-the-conclusion" to 2017,
            "intouchables" to 2011,
            "les-bronzes-font-du-ski" to 1979,
            "e.t.-the-extraterrestial" to 1982,
            "paddington-2" to 2017,
            "pulp-fiction" to 1994,
            "shaun-of-the-dead" to 2004,
            "bottoms" to 2023,
            "le-vieux-qui-ne-voulait-pas-feter-son-anniversaire" to 2013,
            "the-mitchells-vs-the-machines" to 2021,
            "marinette" to 2023,
            "ninjababy" to 2021,
            "ma-vie-de-courgette" to 2016,
            "terminator-2-judgment-day" to 1991,
            "la-famille-asada" to 2020,
            "puan" to 2023,
            "lost-in-translation" to 2003,
            "north-by-northwest" to 1959,
            "bahubali-the-beginning" to 2015,
            "baahubali-the-beginning" to 2015,
            "le-fabuleux-destin-d'amelie-poulain" to 2001,
            "the-holiday" to 2006,
            "au-revoir-la-haut" to 2017,
        )

    fun releaseYear(posterKey: String): Int? = releaseYearByPosterKey[posterKey]

    fun stem(
        displayTitle: String,
        searchTitle: String? = null,
        explicitPosterKey: String? = null,
        legacyPosterAssetName: String? = null,
    ): String {
        if (!explicitPosterKey.isNullOrBlank()) return explicitPosterKey
        val legacy =
            legacyPosterAssetName?.takeUnless {
                it.length == 8 && it.all(Char::isDigit)
            }
        if (!legacy.isNullOrBlank()) return legacy
        stemsByTitle[displayTitle]?.let { return it }
        return PosterKey.slug(searchTitle ?: displayTitle)
    }
}

object PosterUrlEncoding {
    fun pathComponent(raw: String): String {
        val allowed = BooleanArray(256)
        fun allow(chars: String) {
            for (ch in chars) {
                if (ch.code in allowed.indices) allowed[ch.code] = true
            }
        }
        allow("-._~!$&'()*+,;=:@")
        for (ch in 'A'..'Z') allowed[ch.code] = true
        for (ch in 'a'..'z') allowed[ch.code] = true
        for (ch in '0'..'9') allowed[ch.code] = true
        return buildString {
            for (ch in raw) {
                if (ch.code < 256 && allowed[ch.code]) {
                    append(ch)
                } else {
                    val bytes = ch.toString().toByteArray(Charsets.UTF_8)
                    for (byte in bytes) {
                        append('%')
                        append(byte.toUByte().toString(16).uppercase())
                    }
                }
            }
        }
    }
}

fun Screening.remotePosterUrl(posterBaseURL: String?): String? {
    if (!posterURL.isNullOrBlank()) return posterURL
    if (usesTBDPlaceholderPoster) return null
    val template = posterBaseURL?.takeIf { it.isNotBlank() } ?: return null
    if (template.contains("{posterKey}")) {
        return template.replace("{posterKey}", PosterUrlEncoding.pathComponent(posterKey))
    }
    if (template.contains("{id}")) {
        return template.replace("{id}", id)
    }
    return null
}
