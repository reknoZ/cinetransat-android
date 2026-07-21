package com.heewhack.cinetransat.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.heewhack.cinetransat.data.firebase.FirestorePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Soirée Rattrapage votes live on each screening inside `seasons/{year}`:
 * `weeks[i].screenings[j].votes` = [anonymousDeviceUuid, …]
 */
class RattrapageVotesRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _votedScreeningIds = MutableStateFlow<Set<String>>(emptySet())
    val votedScreeningIds: StateFlow<Set<String>> = _votedScreeningIds.asStateFlow()

    private val _voteCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val voteCounts: StateFlow<Map<String, Int>> = _voteCounts.asStateFlow()

    private var seasonListener: ListenerRegistration? = null
    private var observedSeasonYear: Int? = null
    private var observedScreeningIds: Set<String> = emptySet()

    fun hasVoted(screeningId: String): Boolean = screeningId in _votedScreeningIds.value

    fun voteCount(screeningId: String): Int = _voteCounts.value[screeningId] ?: 0

    fun voteShare(screeningId: String): Float {
        val total = _voteCounts.value.values.sum()
        if (total <= 0) return 0f
        return voteCount(screeningId).toFloat() / total.toFloat()
    }

    val totalVoteCount: Int
        get() = _voteCounts.value.values.sum()

    fun startObserving(
        screeningIds: List<String>,
        seasonYear: Int,
    ) {
        val wanted = screeningIds.toSet()
        if (observedSeasonYear == seasonYear &&
            observedScreeningIds == wanted &&
            seasonListener != null
        ) {
            return
        }

        observedSeasonYear = seasonYear
        observedScreeningIds = wanted
        _votedScreeningIds.value = _votedScreeningIds.value.intersect(wanted)
        _voteCounts.value =
            wanted.associateWith { id -> _voteCounts.value[id] ?: 0 }

        seasonListener?.remove()
        val ref = db.document(FirestorePaths.season(seasonYear))
        seasonListener =
            ref.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Season votes listener: ${error.message}")
                    return@addSnapshotListener
                }
                val data = snapshot?.data ?: return@addSnapshotListener
                applyVotes(data, AnonymousDeviceIdentity.deviceId(appContext))
            }
    }

    fun stopAllObservations() {
        seasonListener?.remove()
        seasonListener = null
        observedSeasonYear = null
        observedScreeningIds = emptySet()
    }

    fun toggleVote(
        screeningId: String,
        seasonYear: Int,
    ) {
        val currentlyVoted = screeningId in _votedScreeningIds.value
        _votedScreeningIds.value =
            if (currentlyVoted) {
                _votedScreeningIds.value - screeningId
            } else {
                _votedScreeningIds.value + screeningId
            }
        val currentCount = voteCount(screeningId)
        _voteCounts.value =
            _voteCounts.value + (
                screeningId to
                    if (currentlyVoted) {
                        maxOf(0, currentCount - 1)
                    } else {
                        currentCount + 1
                    }
            )
        scope.launch {
            val success = writeVote(screeningId, seasonYear, add = !currentlyVoted)
            if (!success) {
                _votedScreeningIds.value =
                    if (currentlyVoted) {
                        _votedScreeningIds.value + screeningId
                    } else {
                        _votedScreeningIds.value - screeningId
                    }
                val reverted = voteCount(screeningId)
                _voteCounts.value =
                    _voteCounts.value + (
                        screeningId to
                            if (currentlyVoted) {
                                reverted + 1
                            } else {
                                maxOf(0, reverted - 1)
                            }
                    )
            }
        }
    }

    private fun applyVotes(
        seasonData: Map<String, Any>,
        deviceId: String,
    ) {
        val nextVoted = mutableSetOf<String>()
        val nextCounts = observedScreeningIds.associateWith { 0 }.toMutableMap()
        for (screening in screeningsIn(seasonData)) {
            val id = screening["id"] as? String ?: continue
            if (id !in observedScreeningIds) continue
            val votes = stringList(screening[VOTES_FIELD])
            nextCounts[id] = votes.size
            if (deviceId in votes) {
                nextVoted += id
            }
        }
        _votedScreeningIds.value = nextVoted
        _voteCounts.value = nextCounts
    }

    private suspend fun writeVote(
        screeningId: String,
        seasonYear: Int,
        add: Boolean,
    ): Boolean {
        val deviceId = AnonymousDeviceIdentity.deviceId(appContext)
        val ref = db.document(FirestorePaths.season(seasonYear))
        return try {
            withContext(Dispatchers.IO) {
                db.runTransaction { transaction ->
                    val snap = transaction.get(ref)
                    val data =
                        deepMutableMap(snap.data)
                            ?: throw IllegalStateException("Missing season document")
                    if (!applyVoteMutation(data, screeningId, deviceId, add)) {
                        throw IllegalStateException("Screening $screeningId not found")
                    }
                    transaction.set(ref, data)
                    null
                }.await()
            }
            Log.i(
                TAG,
                "Vote ${if (add) "+" else "−"} for $screeningId on seasons/$seasonYear (…${deviceId.takeLast(6)})",
            )
            true
        } catch (error: Exception) {
            Log.e(TAG, "Vote write failed $screeningId: ${error.message}")
            false
        }
    }

    companion object {
        private const val TAG = "RattrapageVotes"
        private const val VOTES_FIELD = "votes"

        /** Mutates `weeks[*].screenings[*].votes` for the matching screening id. */
        @Suppress("UNCHECKED_CAST")
        fun applyVoteMutation(
            seasonData: MutableMap<String, Any?>,
            screeningId: String,
            deviceId: String,
            add: Boolean,
        ): Boolean {
            val weeks = seasonData["weeks"] as? MutableList<MutableMap<String, Any?>> ?: return false
            for (week in weeks) {
                val screenings = week["screenings"] as? MutableList<MutableMap<String, Any?>> ?: continue
                for (screening in screenings) {
                    if (screening["id"] as? String != screeningId) continue
                    val votes = stringList(screening[VOTES_FIELD]).toMutableList()
                    if (add) {
                        if (deviceId !in votes) votes += deviceId
                    } else {
                        votes.removeAll { it == deviceId }
                    }
                    screening[VOTES_FIELD] = votes
                    return true
                }
            }
            return false
        }

        @Suppress("UNCHECKED_CAST")
        private fun screeningsIn(seasonData: Map<String, Any>): List<Map<String, Any>> {
            val weeks = seasonData["weeks"] as? List<*> ?: return emptyList()
            return weeks.flatMap { week ->
                val map = week as? Map<*, *> ?: return@flatMap emptyList()
                val screenings = map["screenings"] as? List<*> ?: return@flatMap emptyList()
                screenings.mapNotNull { it as? Map<String, Any> }
            }
        }

        private fun stringList(value: Any?): List<String> =
            (value as? List<*>)?.mapNotNull { it as? String }.orEmpty()

        /** Deep-copy Firestore maps/lists into mutable structures for in-place vote edits. */
        @Suppress("UNCHECKED_CAST")
        private fun deepMutableMap(data: Map<String, Any>?): MutableMap<String, Any?>? {
            if (data == null) return null
            val out = mutableMapOf<String, Any?>()
            for ((key, value) in data) {
                out[key] = deepMutableValue(value)
            }
            return out
        }

        @Suppress("UNCHECKED_CAST")
        private fun deepMutableValue(value: Any?): Any? =
            when (value) {
                is Map<*, *> -> {
                    val map = mutableMapOf<String, Any?>()
                    for ((k, v) in value) {
                        if (k is String) map[k] = deepMutableValue(v)
                    }
                    map
                }
                is List<*> -> value.map { deepMutableValue(it) }.toMutableList()
                else -> value
            }
    }
}
