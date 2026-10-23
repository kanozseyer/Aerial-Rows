package com.conreo.couchytv.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherConfigTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun defaultsPreferSlidingRowsAndAllAerialCollections() {
        val config = LauncherConfig()

        assertEquals(LAYOUT_SLIDING_ROWS, config.layout)
        assertEquals(ALL_AERIAL_SOURCES, config.builtinSources)
    }

    @Test
    fun configRoundTripPreservesHomeAndTvState() {
        val original = LauncherConfig(
            homeRowOrder = listOf("watch_next", "category:games"),
            hiddenTvChannels = setOf("provider:channel"),
            channelItemLimits = mapOf("provider:channel" to 18),
            builtinSources = setOf("A", "C1"),
            rowsTopMarginPercent = 12,
        )

        val decoded = json.decodeFromString<LauncherConfig>(json.encodeToString(original))

        assertEquals(original, decoded)
    }

    @Test
    fun legacyConfigWithoutNewFieldsStillDecodesWithSafeDefaults() {
        val legacy = """
            {
              "builtinSource": 1,
              "layout": 2,
              "categories": [{"id":"apps","name":"Apps"}]
            }
        """.trimIndent()

        val decoded = json.decodeFromString<LauncherConfig>(legacy)

        assertEquals(2, decoded.layout)
        assertEquals(ALL_AERIAL_SOURCES, decoded.builtinSources)
        assertTrue(decoded.homeRowOrder.isEmpty())
    }
}
