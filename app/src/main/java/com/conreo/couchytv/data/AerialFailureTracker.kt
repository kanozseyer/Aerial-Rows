package com.conreo.couchytv.data

/**
 * Small in-process failure policy for streamed Aerial clips.
 *
 * A transient failure does not immediately blacklist a clip. Repeated failures
 * temporarily quarantine it, preventing a dead CDN object from being retried on
 * every shuffle cycle. State is intentionally session-local: process restart is
 * a clean retry and no persistence/background service is required.
 */
class AerialFailureTracker(
    private val failuresBeforeQuarantine: Int = 2,
    private val quarantineMs: Long = 30 * 60_000L,
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private data class FailureState(
        val failures: Int,
        val retryAfterMs: Long? = null,
    )

    private val states = mutableMapOf<String, FailureState>()

    init {
        require(failuresBeforeQuarantine > 0)
        require(quarantineMs > 0)
    }

    /** Returns only clips that are eligible right now, expiring old quarantines lazily. */
    fun eligible(items: List<AerialItem>): List<AerialItem> {
        val now = clockMs()
        return items.filter { item ->
            val state = states[item.id] ?: return@filter true
            val retryAt = state.retryAfterMs ?: return@filter true
            if (now >= retryAt) {
                states.remove(item.id)
                true
            } else {
                false
            }
        }
    }

    /** Records one playback failure and returns true when that clip became quarantined. */
    fun recordFailure(itemId: String): Boolean {
        if (itemId.isBlank()) return false
        val current = states[itemId]
        if (current?.retryAfterMs != null && clockMs() < current.retryAfterMs) return true

        val failures = (current?.failures ?: 0) + 1
        val quarantined = failures >= failuresBeforeQuarantine
        states[itemId] = if (quarantined) {
            FailureState(failures = failures, retryAfterMs = clockMs() + quarantineMs)
        } else {
            FailureState(failures = failures)
        }
        return quarantined
    }

    /** A clip that plays through successfully is healthy again immediately. */
    fun recordSuccess(itemId: String) {
        states.remove(itemId)
    }

    /**
     * If every current item is quarantined, returns the delay until the earliest
     * one becomes eligible. Returns null when at least one item can play now.
     */
    fun retryDelayMs(items: List<AerialItem>): Long? {
        if (items.isEmpty()) return null
        val now = clockMs()
        val delays = items.map { item ->
            val retryAt = states[item.id]?.retryAfterMs ?: return null
            (retryAt - now).coerceAtLeast(0L)
        }
        return delays.minOrNull()
    }

    /** Prevent stale catalogue IDs from accumulating during a long launcher session. */
    fun retain(itemIds: Set<String>) {
        states.keys.retainAll(itemIds)
    }
}
