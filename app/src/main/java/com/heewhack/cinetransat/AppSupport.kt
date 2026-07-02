package com.heewhack.cinetransat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object AppSupport {
    const val FEEDBACK_EMAIL = "feedback@heewhack.com"

    fun versionName(context: Context): String {
        val packageInfo =
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
        return packageInfo?.versionName ?: "—"
    }

    fun feedbackMailtoUri(
        context: Context,
        seasonYear: Int,
    ): Uri {
        val subject = context.getString(R.string.settings_feedback_subject)
        val body =
            buildString {
                appendLine()
                appendLine()
                appendLine("---")
                append("CinéTransat ${versionName(context)}")
                appendLine()
                append("Android ${Build.VERSION.RELEASE} · ${Build.MODEL}")
                append(context.getString(R.string.settings_feedback_season, seasonYear))
            }
        return Uri.parse(
            "mailto:$FEEDBACK_EMAIL?" +
                "subject=${Uri.encode(subject)}&body=${Uri.encode(body)}",
        )
    }

    fun openFeedback(
        context: Context,
        seasonYear: Int,
    ) {
        val intent =
            Intent(Intent.ACTION_SENDTO, feedbackMailtoUri(context, seasonYear)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }
}
