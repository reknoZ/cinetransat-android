package com.heewhack.cinetransat.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.heewhack.cinetransat.CineTransatApplication
import com.heewhack.cinetransat.data.firebase.FirestorePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class WatchListStatsRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val db = FirebaseFirestore.getInstance()
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val flushMutex = Mutex()

    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    private val listeners = mutableMapOf<String, ListenerRegistration>()

    init {
        migrateLedgerIfNeeded()
        registerNetworkCallback()
    }

    fun count(screeningId: String): Int? = _counts.value[screeningId]

    fun startObserving(screeningId: String) {
        if (listeners.containsKey(screeningId)) return
        val ref = db.document(FirestorePaths.watchlistStat(screeningId))
        val registration =
            ref.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listener error ($screeningId): ${error.message}")
                    return@addSnapshotListener
                }
                val count = snapshot?.readCount() ?: 0
                _counts.update { current -> current + (screeningId to maxOf(0, count)) }
            }
        listeners[screeningId] = registration
        scope.launch { fetchCountFromServer(ref, screeningId) }
    }

    fun stopObserving(screeningId: String) {
        listeners.remove(screeningId)?.remove()
    }

    fun syncObservations(screeningIds: Set<String>) {
        val stale = listeners.keys.toSet() - screeningIds
        stale.forEach(::stopObserving)
        screeningIds.forEach(::startObserving)
    }

    fun stopAllObservations() {
        listeners.keys.toList().forEach(::stopObserving)
    }

    fun onFirestoreServerReachable(seasonYear: Int) {
        scope.launch {
            runCatching { db.enableNetwork().await() }
            flushPendingDeltas(seasonYear)
        }
    }

    suspend fun recordDelta(
        screeningId: String,
        seasonYear: Int,
        delta: Int,
    ): Boolean {
        if (delta != 1 && delta != -1) return false
        Log.i(TAG, "recordDelta requested $screeningId Δ$delta season=$seasonYear")
        val success = attemptServerDelta(screeningId, seasonYear, delta)
        if (!success) {
            enqueuePendingDelta(screeningId, delta)
        }
        return success
    }

    suspend fun syncWithLocalWatchList(
        screeningIds: Set<String>,
        seasonYear: Int,
    ) {
        syncMutex.withLock {
            Log.i(TAG, "syncWithLocalWatchList ${screeningIds.size} items season=$seasonYear")

            for (id in contributedIds(seasonYear) - screeningIds) {
                recordDelta(id, seasonYear, -1)
            }

            for (id in screeningIds) {
                if (contributedIds(seasonYear).contains(id)) {
                    Log.d(TAG, "Skip sync for $id (already contributed on this device)")
                    continue
                }
                recordDelta(id, seasonYear, 1)
            }
        }
        flushPendingDeltas(seasonYear)
    }

    suspend fun flushPendingDeltas(seasonYear: Int) {
        flushMutex.withLock {
            runCatching { db.enableNetwork().await() }

            val pending = loadPendingDeltas()
            if (pending.isEmpty()) return

            Log.i(TAG, "Flushing ${pending.size} pending watchlist stat write(s)")
            val remaining = mutableListOf<PendingDelta>()
            for (entry in pending) {
                val success = attemptServerDelta(entry.screeningId, seasonYear, entry.delta)
                if (!success) {
                    remaining += entry
                }
            }
            savePendingDeltas(remaining)
        }
    }

    private suspend fun attemptServerDelta(
        screeningId: String,
        seasonYear: Int,
        delta: Int,
    ): Boolean {
        val ref = db.document(FirestorePaths.watchlistStat(screeningId))
        val path = FirestorePaths.watchlistStat(screeningId)

        return try {
            val nextCount =
                withContext(Dispatchers.IO) {
                    val snapshot = ref.get(Source.SERVER).await()
                    val current = snapshot.readCount()
                    val next = current + delta
                    if (next < 0) {
                        throw FirebaseFirestoreException(
                            "Count would become negative",
                            FirebaseFirestoreException.Code.ABORTED,
                        )
                    }
                    ref.set(mapOf(COUNT_FIELD to next), SetOptions.merge()).await()
                    next
                }
            _counts.update { current -> current + (screeningId to maxOf(0, nextCount)) }
            if (delta == 1) {
                markContributed(seasonYear, screeningId)
            } else {
                unmarkContributed(seasonYear, screeningId)
            }
            removePendingDelta(screeningId, delta)
            Log.i(TAG, "Recorded Δ$delta for $screeningId at $path → $nextCount")
            true
        } catch (error: Exception) {
            val code = (error as? FirebaseFirestoreException)?.code
            Log.e(TAG, "Write failed $screeningId Δ$delta at $path ($code): ${error.message}")
            false
        }
    }

    private suspend fun fetchCountFromServer(
        ref: com.google.firebase.firestore.DocumentReference,
        screeningId: String,
    ) {
        runCatching {
            val count = ref.get(Source.SERVER).await().readCount()
            _counts.update { current -> current + (screeningId to maxOf(0, count)) }
        }.onFailure { error ->
            Log.w(TAG, "Initial fetch failed ($screeningId): ${error.message}")
        }
    }

    private fun registerNetworkCallback() {
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch {
                        runCatching { db.enableNetwork().await() }
                        flushPendingDeltas(currentSeasonYear())
                    }
                }
            },
        )
    }

    private fun currentSeasonYear(): Int {
        val app = appContext as? CineTransatApplication
        return app?.programStore?.state?.value?.publicConfig?.currentSeasonYear
            ?: FestivalPublicConfig.DEFAULT_SEASON_YEAR
    }

    private data class PendingDelta(
        val screeningId: String,
        val delta: Int,
    )

    private fun enqueuePendingDelta(
        screeningId: String,
        delta: Int,
    ) {
        val pending = loadPendingDeltas().toMutableList()
        pending.removeAll { it.screeningId == screeningId && it.delta == -delta }
        pending += PendingDelta(screeningId, delta)
        savePendingDeltas(pending)
        Log.i(TAG, "Queued pending Δ$delta for $screeningId (count=${pending.size})")
    }

    private fun removePendingDelta(
        screeningId: String,
        delta: Int,
    ) {
        val pending = loadPendingDeltas().toMutableList()
        pending.removeAll { it.screeningId == screeningId && it.delta == delta }
        savePendingDeltas(pending)
    }

    private fun loadPendingDeltas(): List<PendingDelta> {
        val raw = prefs.getString(KEY_PENDING_DELTAS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        PendingDelta(
                            screeningId = item.getString("screeningId"),
                            delta = item.getInt("delta"),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun savePendingDeltas(deltas: List<PendingDelta>) {
        val array = JSONArray()
        deltas.forEach { entry ->
            array.put(
                JSONObject()
                    .put("screeningId", entry.screeningId)
                    .put("delta", entry.delta),
            )
        }
        prefs.edit().putString(KEY_PENDING_DELTAS, array.toString()).apply()
    }

    private fun migrateLedgerIfNeeded() {
        if (prefs.getInt(KEY_LEDGER_MIGRATION, 0) >= LEDGER_VERSION) return
        prefs.edit().apply {
            prefs.all.keys
                .filter { it.startsWith("watchlistStatsContributed_") }
                .forEach(::remove)
            remove(KEY_PENDING_DELTAS)
            putInt(KEY_LEDGER_MIGRATION, LEDGER_VERSION)
        }.apply()
        Log.i(TAG, "Reset watchlist stats ledger (migration v$LEDGER_VERSION)")
    }

    private fun contributedKey(seasonYear: Int): String = "watchlistStatsContributed_v${LEDGER_VERSION}_$seasonYear"

    private fun contributedIds(seasonYear: Int): Set<String> =
        prefs.getStringSet(contributedKey(seasonYear), emptySet()) ?: emptySet()

    private fun markContributed(
        seasonYear: Int,
        screeningId: String,
    ) {
        val ids = contributedIds(seasonYear).toMutableSet()
        ids.add(screeningId)
        prefs.edit().putStringSet(contributedKey(seasonYear), ids).apply()
    }

    private fun unmarkContributed(
        seasonYear: Int,
        screeningId: String,
    ) {
        val ids = contributedIds(seasonYear).toMutableSet()
        ids.remove(screeningId)
        prefs.edit().putStringSet(contributedKey(seasonYear), ids).apply()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.readCount(): Int {
        if (!exists()) return 0
        val data = data ?: return 0
        return when (val value = data[COUNT_FIELD]) {
            is Number -> value.toInt()
            else -> 0
        }
    }

    companion object {
        private const val TAG = "WatchListStats"
        private const val PREFS_NAME = "watchlist_stats"
        private const val COUNT_FIELD = "count"
        private const val KEY_PENDING_DELTAS = "pending_deltas"
        private const val KEY_LEDGER_MIGRATION = "ledger_migration"
        private const val LEDGER_VERSION = 3

        fun othersCount(
            total: Int,
            inWatchList: Boolean,
        ): Int {
            if (total <= 0) return 0
            return if (inWatchList) maxOf(0, total - 1) else total
        }
    }
}
