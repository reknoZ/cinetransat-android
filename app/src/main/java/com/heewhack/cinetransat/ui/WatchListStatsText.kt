package com.heewhack.cinetransat.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.heewhack.cinetransat.R

@Composable
fun watchlistInterestLabel(count: Int): String {
    if (count <= 0) return ""
    return if (count == 1) {
        stringResource(R.string.detail_watchlist_interest_one)
    } else {
        stringResource(R.string.detail_watchlist_interest_many, count)
    }
}

@Composable
fun watchlistOthersLabel(
    count: Int,
    detail: Boolean = false,
): String {
    if (count <= 0) return ""
    return if (count == 1) {
        stringResource(
            if (detail) {
                R.string.detail_watchlist_others_one
            } else {
                R.string.watchlist_others_one
            },
        )
    } else {
        stringResource(
            if (detail) {
                R.string.detail_watchlist_others_many
            } else {
                R.string.watchlist_others_many
            },
            count,
        )
    }
}
