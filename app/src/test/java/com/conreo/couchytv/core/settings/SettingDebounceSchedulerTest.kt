package com.conreo.couchytv.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingDebounceSchedulerTest {
    private data class Sample(val size: Int = 0, val spacing: Int = 0, val external: Int = 0)

    @Test
    fun latestInputPushesDeadlineForward() {
        val scheduler = SettingDebounceScheduler<Sample>(delayMs = 220L)
        val size = SettingId("display.icon_size")

        scheduler.put(size, nowMs = 1_000L) { it.copy(size = 1) }
        scheduler.put(size, nowMs = 1_100L) { it.copy(size = 4) }

        assertNull(scheduler.drainIfDue(1_319L))
        val batch = requireNotNull(scheduler.drainIfDue(1_320L))
        assertEquals(4, batch(Sample()).size)
    }

    @Test
    fun previewAppliesPendingBatchOverFreshBase() {
        val scheduler = SettingDebounceScheduler<Sample>()
        scheduler.put(SettingId("display.icon_size"), 0L) { it.copy(size = 3) }
        scheduler.put(SettingId("display.spacing"), 10L) { it.copy(spacing = 2) }

        assertEquals(
            Sample(size = 3, spacing = 2, external = 9),
            scheduler.preview(Sample(external = 9)),
        )
    }

    @Test
    fun flushIgnoresDeadlineAndClearsBatch() {
        val scheduler = SettingDebounceScheduler<Sample>()
        scheduler.put(SettingId("display.icon_size"), 10_000L) { it.copy(size = 2) }

        val batch = requireNotNull(scheduler.flush())
        assertEquals(2, batch(Sample()).size)
        assertTrue(scheduler.isEmpty)
        assertNull(scheduler.deadlineMs)
    }
}
