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
