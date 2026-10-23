package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigTransferTest {
    private val fallback = listOf(CategoryCfg("apps", "Apps"))

    @Test
    fun encodedConfigRoundTripsThroughEnvelopeAndPreservesLiveAppBaseline() {
        val source = LauncherConfig(
            categories = listOf(CategoryCfg("games", "Games")),
            knownApps = setOf("old.app"),
            setupDone = false,
            rowsTopMarginPercent = 999,
        )

        val result = ConfigTransfer.decodeForRestore(
            raw = ConfigTransfer.encode(source),
            fallbackCategories = fallback,
            knownApps = setOf("live.app"),
        )

        assertTrue(result is ConfigImportResult.Success)
        val restored = (result as ConfigImportResult.Success).config
        assertEquals(setOf("live.app"), restored.knownApps)
        assertTrue(restored.setupDone)
        assertEquals(50, restored.rowsTopMarginPercent)
    }

    @Test
    fun legacyRawConfigStillImports() {
        val legacy = kotlinx.serialization.json.Json.encodeToString(
            LauncherConfig.serializer(),
            LauncherConfig(categories = listOf(CategoryCfg("music", "Music"))),
        )

        val result = ConfigTransfer.decodeForRestore(
            raw = legacy,
            fallbackCategories = fallback,
            knownApps = emptySet(),
        )

        assertTrue(result is ConfigImportResult.Success)
    }

    @Test
    fun malformedPayloadIsRejected() {
        assertEquals(
            ConfigImportResult.Invalid,
            ConfigTransfer.decodeForRestore("not-json", fallback, emptySet()),
        )
    }
}
