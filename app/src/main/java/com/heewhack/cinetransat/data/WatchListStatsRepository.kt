package com.heewhack.cinetransat.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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

/**
 * Watch list popularity = `watchlistDevices/{screeningId}.devices`.size.
 *
 * On app open, [syncWithLocalWatchList] reconciles local storage with Firestore:
 * 1. Query Firestore for screenings that already contain this install's anonymous ID.
 * 2. If local watch list is empty but Firestore has entries → reinstall restore.
 * 3. Otherwise local is source of truth: register missing IDs, drop stale remote entries.
 */
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
    private val devicesCountByScreeningId = mutableMapOf<String, Int>()

    init {
        registerNetworkCallback()
    }

    fun count(screeningId: String): Int? = _counts.value[screeningId]

    fun applyOptimisticDelta(
        screeningId: String,
        delta: Int,
    ) {
        if (delta != 1 && delta != -1) return
        _counts.update { current ->
            val next = maxOf(0, (current[screeningId] ?: 0) + delta)
            current + (screeningId to next)
        }
    }

    fun startObserving(screeningId: String) {
        if (listeners.containsKey(screeningId)) return
        val ref = db.document(FirestorePaths.watchlistDevices(screeningId))
        listeners[screeningId] =
            ref.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Devices listener ($screeningId): ${error.message}")
                    return@addSnapshotListener
                }
                devicesCountByScreeningId[screeningId] = deviceCount(snapshot?.data)
                publishCount(screeningId)
            }
    }

    fun stopObserving(screeningId: String) {
        listeners.remove(screeningId)?.remove()
        devicesCountByScreeningId.remove(screeningId)
    }

    fun syncObservations(screeningIds: Set<String>) {
        val stale = listeners.keys.toSet() - screeningIds
        stale.forEach(::stopObserving)
        screeningIds.forEach(::startObserving)
    }

    fun stopAllObservations() {
        listeners.keys.toList().forEach(::stopObserving)
    }

    fun onFirestoreServerReachable() {
        scope.launch {
            runCatching { db.enableNetwork().await() }
            flushPendingDeltas()
        }
    }

    suspend fun recordDelta(
        screeningId: String,
        delta: Int,
    ): Boolean {
        if (delta != 1 && delta != -1) return false
        val success = attemptServerDelta(screeningId, delta)
        if (!success) {
            enqueuePendingDelta(screeningId, delta)
        }
        return success
    }

    /**
     * Reconcile local watch list with Firestore for this install.
     *
     * @return The watch list IDs that should be stored locally (unchanged, or restored on reinstall).
     */
    suspend fun syncWithLocalWatchList(localIds: Set<String>): Set<String> {
        val result =
            syncMutex.withLock {
                runCatching { db.enableNetwork().await() }

                val remoteIds = fetchContributedScreeningIds()
                Log.i(
                    TAG,
                    "syncWithLocalWatchList local=${localIds.size} remote=${remoteIds.size}",
                )

                if (localIds.isEmpty() && remoteIds.isNotEmpty()) {
                    Log.i(TAG, "Reinstall restore — ${remoteIds.size} screening(s) from Firestore")
                    remoteIds
                } else {
                    for (id in localIds - remoteIds) {
                        registerDevice(id)
                    }
                    for (id in remoteIds - localIds) {
                        unregisterDevice(id)
                    }
                    localIds
                }
            }
        flushPendingDeltas()
        return result
    }

    suspend fun flushPendingDeltas() {
        flushMutex.withLock {
            runCatching { db.enableNetwork().await() }

            val pending = loadPendingDeltas()
            if (pending.isEmpty()) return

            Log.i(TAG, "Flushing ${pending.size} pending watchlist write(s)")
            val remaining = mutableListOf<PendingDelta>()
            for (entry in pending) {
                val success = attemptServerDelta(entry.screeningId, entry.delta)
                if (!success) {
                    remaining += entry
                }
            }
            savePendingDeltas(remaining)
        }
    }

    private suspend fun registerDevice(screeningId: String) {
        attemptServerDelta(screeningId, 1)
    }

    private suspend fun unregisterDevice(screeningId: String) {
        attemptServerDelta(screeningId, -1)
    }

    private suspend fun attemptServerDelta(
        screeningId: String,
        delta: Int,
    ): Boolean {
        val deviceId = AnonymousDeviceIdentity.deviceId(appContext)
        val ref = db.document(FirestorePaths.watchlistDevices(screeningId))

        return try {
            withContext(Dispatchers.IO) {
                if (delta == 1) {
                    val snap = ref.get().await()
                    if (devicesFrom(snap.data).contains(deviceId)) {
                        return@withContext true
                    }
                    ref.set(
                        mapOf(DEVICES_FIELD to FieldValue.arrayUnion(deviceId)),
                        SetOptions.merge(),
                    ).await()
                } else {
                    val snap = ref.get().await()
                    if (!devicesFrom(snap.data).contains(deviceId)) {
                        return@withContext true
                    }
                    ref.update(DEVICES_FIELD, FieldValue.arrayRemove(deviceId)).await()
                }
            }
            removePendingDelta(screeningId, delta)
            Log.i(
                TAG,
                "Devices ${if (delta == 1) "+" else "−"}1 for $screeningId (…${deviceId.takeLast(6)})",
            )
            true
        } catch (error: Exception) {
            val code = (error as? FirebaseFirestoreException)?.code
            Log.e(TAG, "Write failed $screeningId Δ$delta ($code): ${error.message}")
            false
        }
    }

    private suspend fun fetchContributedScreeningIds(): Set<String> {
        val deviceId = AnonymousDeviceIdentity.deviceId(appContext)
        val snapshot =
            runCatching {
                db.collection(FirestorePaths.WATCHLIST_DEVICES_COLLECTION)
                    .whereArrayContains(DEVICES_FIELD, deviceId)
                    .get()
                    .await()
            }.getOrElse { error ->
                Log.w(TAG, "Contributed IDs query failed: ${error.message}")
                return emptySet()
            }

        return snapshot.documents.map { it.id }.toSet()
    }

    private fun publishCount(screeningId: String) {
        val count = devicesCountByScreeningId[screeningId] ?: 0
        _counts.update { current ->
            if (current[screeningId] == count) current else current + (screeningId to count)
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
                override fun onAvailable(network: android.net.Network) {
                    scope.launch {
                        runCatching { db.enableNetwork().await() }
                        flushPendingDeltas()
                    }
                }
            },
        )
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

    private fun devicesFrom(data: Map<String, Any>?): List<String> {
        if (data == null) return emptyList()
        @Suppress("UNCHECKED_CAST")
        return (data[DEVICES_FIELD] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    }

    private fun deviceCount(data: Map<String, Any>?): Int = devicesFrom(data).size

    companion object {
        private const val TAG = "WatchListStats"
        private const val PREFS_NAME = "watchlist_stats"
        private const val DEVICES_FIELD = "devices"
        private const val KEY_PENDING_DELTAS = "pending_deltas"

        fun othersCount(
            total: Int,
            inWatchList: Boolean,
        ): Int {
            if (total <= 0) return 0
            return if (inWatchList) maxOf(0, total - 1) else total
        }
    }
}
