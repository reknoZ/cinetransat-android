package com.example.cinetransat.ui.components

import android.content.Context
import com.example.cinetransat.data.Screening
import java.time.format.DateTimeFormatter

private val posterDateKey: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

/**
 * iOS stores one poster per screening date (`YYYYMMDD` in Assets); Android uses
 * `res/drawable-nodpi/poster_<yyyyMMdd>.jpg` copied from those imagesets.
 */
fun Context.posterDrawableId(screening: Screening): Int {
    val key = posterDateKey.format(screening.startsAt.toLocalDate())
    return resources.getIdentifier("poster_$key", "drawable", packageName)
}
