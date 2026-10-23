package com.conreo.couchytv.core.settings

/**
 * Coalesces pending setting mutations by semantic [SettingId].
 *
 * The buffer stores transforms rather than whole snapshots so a delayed flush is
 * applied over the latest persisted value and cannot overwrite unrelated changes
 * that arrived while the user was rapidly adjusting another setting.
 */
class SettingMutationBuffer<T> {
    private val mutations = linkedMapOf<SettingId, (T) -> T>()

    val isEmpty: Boolean
        get() = mutations.isEmpty()

    val size: Int
        get() = mutations.size

    val ids: Set<SettingId>
        get() = mutations.keys.toSet()

    /** Latest change for one semantic setting wins while preserving batch order. */
    fun put(id: SettingId, transform: (T) -> T) {
        mutations[id] = transform
    }

    fun remove(id: SettingId) {
        mutations.remove(id)
    }

    fun clear() {
        mutations.clear()
    }

    /** Preview all currently pending mutations over an arbitrary fresh base. */
    fun applyTo(base: T): T = mutations.values.fold(base) { value, transform ->
        transform(value)
    }

    /**
     * Captures the current batch as one immutable transform and clears the buffer.
     * Later mutations belong to the next batch and cannot leak into an in-flight
     * persistence operation.
     */
    fun drain(): ((T) -> T)? {
        if (mutations.isEmpty()) return null
        val batch = mutations.values.toList()
        mutations.clear()
        return { base -> batch.fold(base) { value, transform -> transform(value) } }
    }
}
