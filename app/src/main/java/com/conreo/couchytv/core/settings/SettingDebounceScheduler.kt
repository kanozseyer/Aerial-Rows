package com.conreo.couchytv.core.settings

/**
 * Pure debounce policy around [SettingMutationBuffer]. The Android/Compose adapter
 * supplies monotonic time and decides how to sleep; this class owns only the
 * deterministic batching/deadline rules and is therefore trivial to unit test.
 */
class SettingDebounceScheduler<T>(
    val delayMs: Long = 220L,
) {
    init {
        require(delayMs > 0L) { "Debounce delay must be positive" }
    }

    private val buffer = SettingMutationBuffer<T>()

    var deadlineMs: Long? = null
        private set

    val isEmpty: Boolean
        get() = buffer.isEmpty

    val pendingIds: Set<SettingId>
        get() = buffer.ids

    fun put(
        id: SettingId,
        nowMs: Long,
        transform: (T) -> T,
    ) {
        buffer.put(id, transform)
        deadlineMs = nowMs + delayMs
    }

    /** Preview pending changes over the newest externally supplied base value. */
    fun preview(base: T): T = buffer.applyTo(base)

    /** Returns one immutable batch only after the latest debounce deadline. */
    fun drainIfDue(nowMs: Long): ((T) -> T)? {
        val deadline = deadlineMs ?: return null
        if (nowMs < deadline) return null
        deadlineMs = null
        return buffer.drain()
    }

    /** Flushes immediately, used when closing/confirming a settings session. */
    fun flush(): ((T) -> T)? {
        deadlineMs = null
        return buffer.drain()
    }

    fun clear() {
        deadlineMs = null
        buffer.clear()
    }
}
