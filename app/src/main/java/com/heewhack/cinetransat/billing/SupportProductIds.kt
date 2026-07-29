package com.heewhack.cinetransat.billing

object SupportProductIds {
    const val TIP_199 = "com.heewhack.cinetransat.tip.199"
    const val TIP_499 = "com.heewhack.cinetransat.tip.499"
    const val TIP_999 = "com.heewhack.cinetransat.tip.999"
    const val MONTHLY_099 = "com.heewhack.cinetransat.support.monthly.099"
    const val MONTHLY_199 = "com.heewhack.cinetransat.support.monthly.199"

    val oneTimeTips = listOf(TIP_199, TIP_499, TIP_999)
    val monthly = listOf(MONTHLY_099, MONTHLY_199)

    val tipNominalAmounts =
        mapOf(
            "1.99" to TIP_199,
            "4.99" to TIP_499,
            "9.99" to TIP_999,
        )
}
