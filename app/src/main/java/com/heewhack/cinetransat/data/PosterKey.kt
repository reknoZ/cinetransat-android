package com.heewhack.cinetransat.data

import java.text.Normalizer
import java.util.Locale

object PosterKey {
    fun slug(title: String): String {
        val folded =
            Normalizer.normalize(title, Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
                .lowercase(Locale.FRENCH)
        val slug =
            buildString {
                var lastWasHyphen = false
                for (ch in folded) {
                    if (ch.isLetterOrDigit()) {
                        append(ch)
                        lastWasHyphen = false
                    } else if (!lastWasHyphen && isNotEmpty()) {
                        append('-')
                        lastWasHyphen = true
                    }
                }
            }.trimEnd('-')
        return slug.ifEmpty { "unknown" }
    }
}
