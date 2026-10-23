package com.conreo.couchytv.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigPersistenceTest {
    @Test
    fun legacyLauncherConfigDecodesAndMigratesToCurrentSchema() {
        val legacy = Json.encodeToString(
            LauncherConfig(
                layout = LAYOUT_DOCK,
                builtinSources = setOf("A", "C2"),
            ),
        )

        val result = ConfigCodec.decode(legacy)

        assertTrue(result is ConfigDecodeResult.Success)
        result as ConfigDecodeResult.Success
        assertEquals(CURRENT_CONFIG_SCHEMA_VERSION, result.envelope.schemaVersion)
        assertEquals(LAYOUT_DOCK, result.envelope.launcherConfig.layout)
        assertEquals(setOf("A", "C2"), result.envelope.launcherConfig.builtinSources)
    }

    @Test
    fun encodedConfigRoundTripsInsideVersionedEnvelope() {
        val original = LauncherConfig(
            homeRowOrder = listOf("watch_next", "apps"),
            channelItemLimit = 20,
        )

        val result = ConfigCodec.decode(ConfigCodec.encode(original))

        assertTrue(result is ConfigDecodeResult.Success)
        result as ConfigDecodeResult.Success
        assertEquals(CURRENT_CONFIG_SCHEMA_VERSION, result.envelope.schemaVersion)
        assertEquals(original, result.envelope.launcherConfig)
    }

    @Test
    fun malformedJsonReturnsFailureWithoutInventingDefaults() {
        val raw = "{ definitely-not-json"

        val result = ConfigCodec.decode(raw)

        assertTrue(result is ConfigDecodeResult.Failure)
        assertEquals(raw, (result as ConfigDecodeResult.Failure).raw)
    }

    @Test
    fun newerSchemaIsRejectedWithoutBeingClassifiedAsCorruption() {
        val raw = """
            {
              "schemaVersion": ${CURRENT_CONFIG_SCHEMA_VERSION + 1},
              "launcherConfig": {}
            }
        """.trimIndent()

        val result = ConfigCodec.decode(raw)

        assertTrue(result is ConfigDecodeResult.UnsupportedSchema)
        result as ConfigDecodeResult.UnsupportedSchema
        assertEquals(CURRENT_CONFIG_SCHEMA_VERSION + 1, result.schemaVersion)
        assertEquals(raw, result.raw)
    }
}
