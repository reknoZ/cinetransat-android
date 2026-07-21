package com.heewhack.cinetransat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

object AppSupport {
    const val FEEDBACK_EMAIL = "feedback@heewhack.com"

    private fun packageInfo(context: Context) =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        }.getOrNull()

    fun versionName(context: Context): String = packageInfo(context)?.versionName ?: "—"

    fun versionCode(context: Context): Long {
        val info = packageInfo(context) ?: return 0L
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    fun versionLabel(context: Context): String = "${versionName(context)} (${versionCode(context)})"

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
                append("CinéTransat ${versionLabel(context)}")
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

    fun openPlayStoreListing(context: Context) {
        val packageName = context.packageName
        val marketIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val webIntent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        runCatching { context.startActivity(marketIntent) }
            .onFailure { context.startActivity(webIntent) }
    }
}
