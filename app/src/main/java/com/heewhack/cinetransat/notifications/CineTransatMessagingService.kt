package com.heewhack.cinetransat.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.heewhack.cinetransat.R

class CineTransatMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        CancellationNotificationManager.getInstance(this).ensureNotificationChannelReady()
        CancellationNotificationManager.getInstance(this).onNewFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        CancellationNotificationManager.getInstance(this).ensureNotificationChannelReady()
        val manager = CancellationNotificationManager.getInstance(this)
        val data = message.data
        Log.d(
            TAG,
            "FCM message from=${message.from} data=$data notification=${message.notification?.title}",
        )
        val title =
            message.notification?.title
                ?: data["title"]
                ?: getString(R.string.notification_cancel_title)
        val body =
            message.notification?.body
                ?: data["body"]
                ?: data["screeningTitle"]?.let { screeningTitle ->
                    getString(R.string.notification_cancel_body_remote, screeningTitle)
                }
                ?: return
        manager.showRemoteNotification(
            title = title,
            body = body,
            screeningId = data["screeningId"],
            seasonYear = data["seasonYear"]?.toIntOrNull(),
        )
    }

    companion object {
        private const val TAG = "CineTransatFCM"
    }
}
