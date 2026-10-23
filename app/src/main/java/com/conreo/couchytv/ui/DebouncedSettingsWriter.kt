package com.conreo.couchytv.ui

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.conreo.couchytv.core.settings.SettingDebounceScheduler
import com.conreo.couchytv.core.settings.SettingId
import com.conreo.couchytv.data.ConfigStore
import com.conreo.couchytv.data.LauncherConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Small UI adapter for rapid settings changes.
 *
 * The pure scheduler owns batching/deadlines; this adapter only sleeps and sends
 * one composed transform to ConfigStore. Because ConfigStore applies that
 * transform inside DataStore.edit, unrelated app/channel/config changes that
 * arrive during the debounce window are preserved.
 */
internal class DebouncedSettingsWriter(
    private val store: ConfigStore,
    private val scope: CoroutineScope,
    delayMs: Long = 220L,
    private val nowMs: () -> Long = SystemClock::elapsedRealtime,
) {
    private val scheduler = SettingDebounceScheduler<LauncherConfig>(delayMs)
    private var job: Job? = null

    // Read by preview() from composition so pending mutations can drive live
    // preview UI immediately without waiting for the DataStore round-trip.
    private var previewRevision by mutableIntStateOf(0)

    val hasPending: Boolean
        get() = !scheduler.isEmpty

    fun preview(base: LauncherConfig): LauncherConfig {
        previewRevision // snapshot read: invalidates callers when pending state changes
        return scheduler.preview(base)
    }

    fun submit(
        id: SettingId,
        transform: (LauncherConfig) -> LauncherConfig,
    ) {
        scheduler.put(id, nowMs(), transform)
        previewRevision++
        scheduleFlush()
    }

    private fun scheduleFlush() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                val deadline = scheduler.deadlineMs ?: return@launch
                delay((deadline - nowMs()).coerceAtLeast(0L))
                val batch = scheduler.drainIfDue(nowMs()) ?: continue
                persistCaptured(batch)
                // Do not invalidate preview here. ConfigStore's state update will
                // recompose with the persisted value; forcing an earlier snapshot
                // read could briefly render the old base value after the scheduler
                // has already drained its pending transform.
                return@launch
            }
        }
    }

    /**
     * Once a batch has been drained from the scheduler it has no second copy to
     * retry from. Finish that DataStore transaction even if a later input cancels
     * the old debounce job; only the pre-drain waiting period is cancellable.
     */
    private suspend fun persistCaptured(batch: (LauncherConfig) -> LauncherConfig) {
        withContext(NonCancellable) { store.update(batch) }
    }

    suspend fun flush() {
        job?.cancelAndJoin()
        job = null
        scheduler.flush()?.let { persistCaptured(it) }
    }

    fun clear() {
        job?.cancel()
        job = null
        scheduler.clear()
        previewRevision++
    }
}
