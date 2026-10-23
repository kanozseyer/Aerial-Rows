package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AerialFailureTrackerTest {
    private fun item(id: String) = AerialItem(
        id = id,
        title = id,
        sourceId = "A",
        uri = "https://example.test/$id.mov",
    )

    @Test
    fun transientFailureDoesNotImmediatelyQuarantine() {
        var now = 1_000L
        val tracker = AerialFailureTracker(clockMs = { now })
        val clip = item("a")

        assertFalse(tracker.recordFailure(clip.id))
        assertEquals(listOf(clip), tracker.eligible(listOf(clip)))
        assertNull(tracker.retryDelayMs(listOf(clip)))
    }

    @Test
    fun repeatedFailureQuarantinesThenExpires() {
        var now = 5_000L
        val tracker = AerialFailureTracker(quarantineMs = 30_000L, clockMs = { now })
        val clip = item("a")

        tracker.recordFailure(clip.id)
        assertTrue(tracker.recordFailure(clip.id))
        assertTrue(tracker.eligible(listOf(clip)).isEmpty())
        assertEquals(30_000L, tracker.retryDelayMs(listOf(clip)))

        now += 30_000L
        assertEquals(listOf(clip), tracker.eligible(listOf(clip)))
        assertNull(tracker.retryDelayMs(listOf(clip)))
    }

    @Test
    fun successfulPlaybackClearsFailureHistory() {
        var now = 10_000L
        val tracker = AerialFailureTracker(clockMs = { now })
        val clip = item("a")

        tracker.recordFailure(clip.id)
        tracker.recordSuccess(clip.id)

        assertFalse(tracker.recordFailure(clip.id))
        assertEquals(listOf(clip), tracker.eligible(listOf(clip)))
    }

    @Test
    fun allQuarantinedUsesEarliestRetryAndRetainDropsOldIds() {
        var now = 20_000L
        val a = item("a")
        val b = item("b")
        val tracker = AerialFailureTracker(
            failuresBeforeQuarantine = 1,
            quarantineMs = 60_000L,
            clockMs = { now },
        )

        tracker.recordFailure(a.id)
        now += 10_000L
        tracker.recordFailure(b.id)
        assertEquals(50_000L, tracker.retryDelayMs(listOf(a, b)))

        tracker.retain(setOf(b.id))
        assertNull(tracker.retryDelayMs(listOf(a)))
        assertTrue(tracker.eligible(listOf(a)).contains(a))
    }
}
