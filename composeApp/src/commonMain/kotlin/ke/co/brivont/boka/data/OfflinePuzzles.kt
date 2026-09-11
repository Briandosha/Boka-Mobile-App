package ke.co.brivont.boka.data

import ke.co.brivont.boka.core.json
import ke.co.brivont.boka.core.provideStore
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Offline puzzle support (the data-light / offline mode).
 *
 * A small cache of prefetched puzzles lives in the key-value store so the trainer
 * keeps working with no signal — important where data is costly. It tops itself up
 * from the server whenever we're online and running low. Attempts made offline are
 * queued and flushed the next time a request succeeds, so ratings and streaks stay
 * correct. Lessons are already bundled in the app, so they're offline by default.
 */
object OfflinePuzzles {
    private const val CACHE_KEY = "puzzle_cache_v1"
    private const val PENDING_KEY = "puzzle_pending_v1"
    private const val LOW_WATER = 8      // refill when fewer than this remain
    private const val REFILL = 30        // how many to fetch per top-up

    private val store by lazy { provideStore() }

    // ---- cache ----
    private fun readCache(): MutableList<PuzzleDto> =
        store.get(CACHE_KEY)
            ?.let { runCatching { json.decodeFromString<List<PuzzleDto>>(it) }.getOrNull() }
            ?.toMutableList() ?: mutableListOf()

    private fun writeCache(list: List<PuzzleDto>) =
        store.put(CACHE_KEY, if (list.isEmpty()) null else json.encodeToString(list))

    fun cachedCount(): Int = readCache().size

    /** Take one saved puzzle (null if none). */
    fun pop(): PuzzleDto? {
        val list = readCache()
        if (list.isEmpty()) return null
        val p = list.removeAt(0)
        writeCache(list)
        return p
    }

    /** Top the cache up from the server when low. Silently no-ops when offline. */
    suspend fun refillIfLow() {
        val list = readCache()
        if (list.size >= LOW_WATER) return
        val fresh = PuzzleApi.batch(REFILL) ?: return
        val have = list.map { it.id }.toHashSet()
        list += fresh.filter { it.id !in have }
        writeCache(list)
    }

    // ---- pending attempts ----
    private fun readPending(): MutableList<AttemptBody> =
        store.get(PENDING_KEY)
            ?.let { runCatching { json.decodeFromString<List<AttemptBody>>(it) }.getOrNull() }
            ?.toMutableList() ?: mutableListOf()

    private fun writePending(list: List<AttemptBody>) =
        store.put(PENDING_KEY, if (list.isEmpty()) null else json.encodeToString(list))

    fun enqueue(a: AttemptBody) { val l = readPending(); l += a; writePending(l) }
    fun pendingCount(): Int = readPending().size

    /** Send queued attempts in order; stops at the first failure (still offline). Returns how many synced. */
    suspend fun flushPending(): Int {
        val list = readPending()
        var sent = 0
        while (list.isNotEmpty()) {
            val a = list.first()
            if (PuzzleApi.attempt(a.puzzleId, a.solved) == null) break
            list.removeAt(0); sent++
        }
        writePending(list)
        return sent
    }
}
