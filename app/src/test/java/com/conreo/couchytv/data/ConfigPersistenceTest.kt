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
    fun v1EnvelopeWithoutPolicyMigratesToBehaviorPreservingDefault() {
        val v1 = """
            {
              "schemaVersion": 1,
              "launcherConfig": {
                "layout": $LAYOUT_SLIDING_ROWS,
                "knownApps": ["com.example.old"]
              }
            }
        """.trimIndent()

        val result = ConfigCodec.decode(v1)

        assertTrue(result is ConfigDecodeResult.Success)
        result as ConfigDecodeResult.Success
        assertEquals(CURRENT_CONFIG_SCHEMA_VERSION, result.envelope.schemaVersion)
        assertEquals(NewInstallPolicy.FIRST_SECTION, result.envelope.launcherConfig.newInstallPolicy)
        assertEquals(setOf("com.example.old"), result.envelope.launcherConfig.knownApps)
    }

    @Test
    fun v2EnvelopeMigratesToWatchNextProviderDefaults() {
        val v2 = """
            {
              "schemaVersion": 2,
              "launcherConfig": {
                "layout": $LAYOUT_SLIDING_ROWS
              }
            }
        """.trimIndent()

        val result = ConfigCodec.decode(v2)

        assertTrue(result is ConfigDecodeResult.Success)
        result as ConfigDecodeResult.Success
        assertEquals(CURRENT_CONFIG_SCHEMA_VERSION, result.envelope.schemaVersion)
        assertEquals(4, result.envelope.launcherConfig.watchNextPerProviderLimit)
        assertTrue(result.envelope.launcherConfig.excludedWatchNextProviders.isEmpty())
    }

    @Test
    fun encodedConfigRoundTripsInsideVersionedEnvelope() {
        val original = LauncherConfig(
            homeRowOrder = listOf("watch_next", "apps"),
            channelItemLimit = 20,
            newInstallPolicy = NewInstallPolicy.AUTO_CATEGORY,
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
