package com.heewhack.cinetransat.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.heewhack.cinetransat.CineTransatApplication
import com.heewhack.cinetransat.MainActivity
import com.heewhack.cinetransat.R
import android.util.Log
import com.heewhack.cinetransat.data.AppLanguage
import com.heewhack.cinetransat.data.Screening
import com.heewhack.cinetransat.data.localizedTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Cancellation alerts via FCM topic push (app closed). Local notifications are only a
 * fallback when Firestore updates while the app is already running.
 */
class CancellationNotificationManager private constructor(
    private val appContext: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val notificationId = AtomicInteger(0)

    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _lastTopicSubscribeError = MutableStateFlow<String?>(null)
    val lastTopicSubscribeError: StateFlow<String?> = _lastTopicSubscribeError.asStateFlow()

    private val _notificationsDenied = MutableStateFlow(!canPostNotifications())
    val notificationsDenied: StateFlow<Boolean> = _notificationsDenied.asStateFlow()

    init {
        ensureNotificationChannel()
        _isEnabled.value = prefs.getBoolean(KEY_ENABLED, false)
    }

    /** Call from [Application.onCreate] so the FCM channel exists before any push arrives. */
    fun ensureNotificationChannelReady() {
        ensureNotificationChannel()
    }

    fun startupSyncSubscription() {
        if (!prefs.getBoolean(KEY_ENABLED, false) || !canPostNotifications()) return
        val year = prefs.getInt(KEY_SUBSCRIBED_TOPIC_YEAR, -1).takeIf { it > 0 }
            ?: return
        scope.launch { subscribeToCancellationTopic(year) }
    }

    fun refreshPermissionStatus() {
        _notificationsDenied.value = !canPostNotifications()
    }

    suspend fun completeFirstLaunchPrompt(
        seasonYear: Int,
        granted: Boolean,
    ) {
        prefs.edit().putBoolean(KEY_INITIAL_PROMPT_COMPLETED, true).apply()
        completePermissionResult(seasonYear, granted)
    }

    suspend fun handleFirstLaunchPrompt(
        seasonYear: Int,
        requestPermission: suspend () -> Boolean,
    ) {
        if (prefs.getBoolean(KEY_INITIAL_PROMPT_COMPLETED, false)) return
        prefs.edit().putBoolean(KEY_INITIAL_PROMPT_COMPLETED, true).apply()

        val granted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications()) {
                requestPermission()
            } else {
                canPostNotifications()
            }
        completePermissionResult(seasonYear, granted)
    }

    suspend fun completePermissionResult(
        seasonYear: Int,
        granted: Boolean,
    ) {
        refreshPermissionStatus()
        if (granted) {
            enableNotifications(seasonYear, skipPermissionCheck = true)
        } else {
            setEnabled(false)
        }
    }

    fun shouldShowFirstLaunchPrompt(): Boolean =
        !prefs.getBoolean(KEY_INITIAL_PROMPT_COMPLETED, false)

    suspend fun enableNotifications(
        seasonYear: Int,
        requestPermission: suspend () -> Boolean = { canPostNotifications() },
        skipPermissionCheck: Boolean = false,
    ): Boolean {
        val granted =
            if (skipPermissionCheck || canPostNotifications()) {
                true
            } else {
                requestPermission()
            }
        refreshPermissionStatus()
        if (!granted) {
            setEnabled(false)
            return false
        }

        setEnabled(true)
        subscribeToCancellationTopic(seasonYear)
        return true
    }

    suspend fun disableNotifications() {
        setEnabled(false)
        val year = prefs.getInt(KEY_SUBSCRIBED_TOPIC_YEAR, -1)
        if (year > 0) {
            unsubscribeFromCancellationTopic(year)
        }
        prefs.edit().remove(KEY_SUBSCRIBED_TOPIC_YEAR).apply()
    }

    suspend fun syncSubscriptionIfNeeded(seasonYear: Int) {
        if (!_isEnabled.value || !canPostNotifications()) return
        subscribeToCancellationTopic(seasonYear)
    }

    fun onNewFcmToken(token: String) {
        Log.d(TAG, "New FCM token ${token.take(12)}…")
        if (!prefs.getBoolean(KEY_ENABLED, false)) return
        val year = prefs.getInt(KEY_SUBSCRIBED_TOPIC_YEAR, -1)
        if (year > 0) {
            scope.launch { subscribeToCancellationTopic(year) }
        }
    }

    fun handleNewlyCanceled(
        screenings: List<Screening>,
        seasonYear: Int,
    ) {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        if (enabled != _isEnabled.value) {
            _isEnabled.value = enabled
        }
        refreshPermissionStatus()
        if (!enabled || screenings.isEmpty() || !canPostNotifications()) {
            Log.d(
                TAG,
                "Skip local cancel alert enabled=$enabled count=${screenings.size} canPost=${canPostNotifications()}",
            )
            return
        }
        val language = currentAppLanguage()
        for (screening in screenings) {
            showScreeningCanceledNotification(screening, seasonYear, language)
        }
    }

    fun showRemoteNotification(
        title: String,
        body: String,
        screeningId: String?,
        seasonYear: Int?,
    ) {
        if (!prefs.getBoolean(KEY_ENABLED, false) || !canPostNotifications()) return
        postNotification(
            title = title,
            body = body,
            screeningId = screeningId,
            seasonYear = seasonYear,
        )
    }

    private fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private suspend fun subscribeToCancellationTopic(seasonYear: Int) {
        if (!prefs.getBoolean(KEY_ENABLED, false)) return
        val topic = cancellationTopic(seasonYear)
        val previousYear = prefs.getInt(KEY_SUBSCRIBED_TOPIC_YEAR, -1)
        if (previousYear > 0 && previousYear != seasonYear) {
            unsubscribeFromCancellationTopic(previousYear)
        }
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM token ready (${token.take(12)}…), subscribing to $topic")
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            _lastTopicSubscribeError.value = null
            prefs.edit().putInt(KEY_SUBSCRIBED_TOPIC_YEAR, seasonYear).apply()
            Log.d(TAG, "Subscribed to FCM topic $topic")
        } catch (error: Exception) {
            _lastTopicSubscribeError.value = error.localizedMessage
            Log.w(TAG, "FCM subscribe failed for $topic: ${error.message}")
        }
    }

    private suspend fun unsubscribeFromCancellationTopic(seasonYear: Int) {
        runCatching {
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(cancellationTopic(seasonYear))
                .await()
        }
    }

    private fun showScreeningCanceledNotification(
        screening: Screening,
        seasonYear: Int,
        language: AppLanguage,
    ) {
        val title = appContext.getString(R.string.notification_cancel_title)
        val date = formattedScreeningDate(screening, language.locale)
        val body =
            appContext.getString(
                R.string.notification_cancel_body,
                screening.localizedTitle(language),
                date,
            )
        postNotification(
            title = title,
            body = body,
            screeningId = screening.id,
            seasonYear = seasonYear,
        )
    }

    private fun postNotification(
        title: String,
        body: String,
        screeningId: String?,
        seasonYear: Int?,
    ) {
        if (!canPostNotifications()) return

        val launchIntent =
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                screeningId?.let { putExtra(EXTRA_SCREENING_ID, it) }
                seasonYear?.let { putExtra(EXTRA_SEASON_YEAR, it) }
            }
        val pendingIntent =
            PendingIntent.getActivity(
                appContext,
                notificationId.incrementAndGet(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        NotificationManagerCompat.from(appContext).notify(
            screeningId?.hashCode() ?: notificationId.incrementAndGet(),
            notification,
        )
        Log.d(TAG, "Posted cancellation notification screeningId=$screeningId")
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_cancellations),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = appContext.getString(R.string.notification_channel_cancellations_desc)
            }
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        }

    private fun formattedScreeningDate(
        screening: Screening,
        locale: Locale,
    ): String {
        val formatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(locale)
        return formatter.format(screening.startsAt)
    }

    private fun currentAppLanguage(): AppLanguage {
        val app = appContext.applicationContext as CineTransatApplication
        return app.appLanguageRepository.currentLanguage()
    }

    companion object {
        const val CHANNEL_ID = "cancellations"
        const val EXTRA_SCREENING_ID = "screeningId"
        const val EXTRA_SEASON_YEAR = "seasonYear"

        private const val PREFS_NAME = "cancellation_notifications"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SUBSCRIBED_TOPIC_YEAR = "subscribed_topic_year"
        private const val KEY_INITIAL_PROMPT_COMPLETED = "initial_prompt_completed"

        @Volatile
        private var instance: CancellationNotificationManager? = null

        fun getInstance(context: Context): CancellationNotificationManager =
            instance ?: synchronized(this) {
                instance ?: CancellationNotificationManager(context.applicationContext).also {
                    instance = it
                }
            }

        fun cancellationTopic(seasonYear: Int): String = "season_${seasonYear}_cancellations"

        private const val TAG = "CineTransatNotify"
    }
}
