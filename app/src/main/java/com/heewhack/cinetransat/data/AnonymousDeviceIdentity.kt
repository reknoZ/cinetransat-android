package com.heewhack.cinetransat.data

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Anonymous ID for watch list deduplication — not tied to account or advertising IDs.
 *
 * Stored in SharedPreferences (included in Auto Backup). New installs use a stable
 * app-scoped ID derived from [Settings.Secure.ANDROID_ID] so the same device keeps
 * the same ID after reinstall even when cloud backup is unavailable.
 *
 * Installs that already saved a random UUID keep using it.
 */
object AnonymousDeviceIdentity {
    private const val PREFS_NAME = "anonymous_device_identity"
    private const val KEY_DEVICE_ID = "device_id"

    fun deviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val id = stableDeviceId(context.applicationContext)
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    /** App-scoped, stable across reinstall on the same device (API 26+). */
    private fun stableDeviceId(context: Context): String {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown"
        val material = "${context.packageName}:$androidId"
        return UUID.nameUUIDFromBytes(material.toByteArray(Charsets.UTF_8)).toString()
    }
}
