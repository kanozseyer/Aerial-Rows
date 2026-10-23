package com.conreo.couchytv.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingMutationBufferTest {
    private data class Sample(val size: Int = 0, val spacing: Int = 0, val external: Int = 0)

    @Test
    fun latestMutationForSameSettingWins() {
        val buffer = SettingMutationBuffer<Sample>()
        val size = SettingId("display.icon_size")

        buffer.put(size) { it.copy(size = 1) }
        buffer.put(size) { it.copy(size = 4) }

        assertEquals(1, buffer.size)
        assertEquals(4, buffer.applyTo(Sample()).size)
    }

    @Test
    fun differentSettingsComposeWithoutDroppingEachOther() {
        val buffer = SettingMutationBuffer<Sample>()
        buffer.put(SettingId("display.icon_size")) { it.copy(size = 3) }
        buffer.put(SettingId("display.spacing")) { it.copy(spacing = 2) }

        assertEquals(Sample(size = 3, spacing = 2), buffer.applyTo(Sample()))
    }

    @Test
    fun delayedBatchAppliesOverFreshExternalBase() {
        val buffer = SettingMutationBuffer<Sample>()
        buffer.put(SettingId("display.icon_size")) { it.copy(size = 2) }
        val batch = requireNotNull(buffer.drain())

        val externallyChanged = Sample(external = 9)
        assertEquals(Sample(size = 2, external = 9), batch(externallyChanged))
        assertTrue(buffer.isEmpty)
    }

    @Test
    fun drainingIsolatedFromLaterMutations() {
        val buffer = SettingMutationBuffer<Sample>()
        val size = SettingId("display.icon_size")
        buffer.put(size) { it.copy(size = 1) }
        val first = requireNotNull(buffer.drain())

        buffer.put(size) { it.copy(size = 4) }

        assertEquals(1, first(Sample()).size)
        assertEquals(4, buffer.applyTo(Sample()).size)
        assertFalse(buffer.isEmpty)
    }
}
