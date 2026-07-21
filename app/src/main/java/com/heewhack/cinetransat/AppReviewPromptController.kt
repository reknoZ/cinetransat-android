package com.heewhack.cinetransat

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Soft “Like this app?” prompt after enough cumulative foreground use.
 * Star sheet → Play In-App Review (4–5★) or feedback email (1–3★).
 */
class AppReviewPromptController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private var tickJob: Job? = null
    private var sessionStartedAtMs: Long? = null
    private var promptedThisSession = false
    private var hostActivity: WeakReference<Activity>? = null
    private var mainUiVisible = false

    private val _isPresentingPrompt = MutableStateFlow(false)
    val isPresentingPrompt: StateFlow<Boolean> = _isPresentingPrompt.asStateFlow()

    private val _pendingFeedback = MutableStateFlow(false)
    val pendingFeedback: StateFlow<Boolean> = _pendingFeedback.asStateFlow()

    fun bind(activity: Activity) {
        hostActivity = WeakReference(activity)
    }

    fun setMainUiVisible(visible: Boolean) {
        mainUiVisible = visible
        if (visible) {
            evaluatePromptIfNeeded()
        }
    }

    fun onForeground() {
        sessionStartedAtMs = System.currentTimeMillis()
        tickJob?.cancel()
        tickJob =
            scope.launch {
                while (isActive) {
                    delay(15_000)
                    accumulateActiveTime()
                    evaluatePromptIfNeeded()
                }
            }
        evaluatePromptIfNeeded()
    }

    fun onBackground() {
        accumulateActiveTime()
        tickJob?.cancel()
        tickJob = null
        sessionStartedAtMs = null
    }

    /** Call from Settings when the user opens rating / review themselves. */
    fun markReviewActionCompleted() {
        prefs.edit()
            .putBoolean(KEY_COMPLETED_REVIEW, true)
            .putLong(KEY_LAST_PROMPT_MS, System.currentTimeMillis())
            .apply()
    }

    fun dismissWithoutAction() {
        if (!_isPresentingPrompt.value) return
        recordPromptShown()
        _isPresentingPrompt.value = false
    }

    fun submitRating(stars: Int) {
        if (stars <= 0) return
        markReviewActionCompleted()
        _isPresentingPrompt.value = false

        val activity = hostActivity?.get()
        if (stars >= STORE_REVIEW_MIN_STARS && activity != null && !activity.isFinishing) {
            launchInAppReview(activity)
        } else {
            _pendingFeedback.value = true
        }
    }

    fun clearPendingFeedback() {
        _pendingFeedback.value = false
    }

    private fun evaluatePromptIfNeeded() {
        val activity = hostActivity?.get() ?: return
        if (!mainUiVisible || promptedThisSession || activity.isFinishing) return
        if (_isPresentingPrompt.value) return
        if (prefs.getBoolean(KEY_COMPLETED_REVIEW, false)) return

        accumulateActiveTime()
        val total = prefs.getLong(KEY_ACTIVE_MS, 0L)
        if (total < MIN_ACTIVE_MS) return

        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT_MS, 0L)
        if (lastPrompt > 0L) {
            val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastPrompt)
            if (days < MIN_DAYS_BETWEEN_PROMPTS) return
        }

        promptedThisSession = true
        _isPresentingPrompt.value = true
    }

    private fun launchInAppReview(activity: Activity) {
        scope.launch {
            runCatching {
                val manager = ReviewManagerFactory.create(activity)
                val request = manager.requestReviewFlow().await()
                manager.launchReviewFlow(activity, request).await()
            }.onFailure {
                AppSupport.openPlayStoreListing(activity)
            }
        }
    }

    private fun accumulateActiveTime() {
        val started = sessionStartedAtMs ?: return
        val now = System.currentTimeMillis()
        val delta = now - started
        sessionStartedAtMs = now
        if (delta <= 0L || delta > 120_000L) return
        prefs.edit().putLong(KEY_ACTIVE_MS, prefs.getLong(KEY_ACTIVE_MS, 0L) + delta).apply()
    }

    private fun recordPromptShown() {
        prefs.edit().putLong(KEY_LAST_PROMPT_MS, System.currentTimeMillis()).apply()
    }

    companion object {
        private const val PREFS = "app_review_prompt"
        private const val KEY_ACTIVE_MS = "total_active_ms"
        private const val KEY_LAST_PROMPT_MS = "last_prompt_ms"
        private const val KEY_COMPLETED_REVIEW = "completed_review_action"
        private val MIN_ACTIVE_MS = TimeUnit.MINUTES.toMillis(8)
        private const val MIN_DAYS_BETWEEN_PROMPTS = 7L
        private const val STORE_REVIEW_MIN_STARS = 4
    }
}
