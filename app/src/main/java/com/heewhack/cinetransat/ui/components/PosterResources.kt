package com.heewhack.cinetransat.ui.components

import android.content.Context
import com.heewhack.cinetransat.data.Screening
import java.time.format.DateTimeFormatter

private val posterDateKey: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

/**
 * iOS stores one poster per screening date (`YYYYMMDD` in Assets); Android uses
 * `res/drawable-nodpi/poster_<yyyyMMdd>.jpg` copied from those imagesets.
 */
fun Context.posterDrawableId(screening: Screening): Int {
    val key =
        if (screening.id.length == 8 && screening.id.all(Char::isDigit)) {
            screening.id
        } else {
            posterDateKey.format(screening.startsAt.toLocalDate())
        }
    return resources.getIdentifier("poster_$key", "drawable", packageName)
}
