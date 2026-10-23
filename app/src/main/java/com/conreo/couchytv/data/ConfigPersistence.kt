package com.conreo.couchytv.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

const val CURRENT_CONFIG_SCHEMA_VERSION = 2

@Serializable
data class ConfigEnvelope(
    val schemaVersion: Int,
    val launcherConfig: LauncherConfig,
)

sealed interface ConfigDecodeResult {
    data class Success(val envelope: ConfigEnvelope) : ConfigDecodeResult
    data class UnsupportedSchema(val raw: String, val schemaVersion: Int) : ConfigDecodeResult
    data class Failure(val raw: String, val reason: String) : ConfigDecodeResult
}

interface ConfigMigration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(config: LauncherConfig): LauncherConfig
}

private object LegacyToV1Migration : ConfigMigration {
    override val fromVersion: Int = 0
    override val toVersion: Int = 1
    override fun migrate(config: LauncherConfig): LauncherConfig = config
}

/**
 * Schema v2 introduces the explicit new-install policy. LauncherConfig supplies
 * FIRST_SECTION as the serialized default, which preserves v1 behavior exactly.
 */
private object V1ToV2Migration : ConfigMigration {
    override val fromVersion: Int = 1
    override val toVersion: Int = 2
    override fun migrate(config: LauncherConfig): LauncherConfig = config
}

object MigrationRegistry {
    private val migrations: Map<Int, ConfigMigration> = listOf(
        LegacyToV1Migration,
        V1ToV2Migration,
    ).associateBy { it.fromVersion }

    fun migrate(envelope: ConfigEnvelope): ConfigEnvelope {
        require(envelope.schemaVersion <= CURRENT_CONFIG_SCHEMA_VERSION)

        var version = envelope.schemaVersion
        var config = envelope.launcherConfig
        while (version < CURRENT_CONFIG_SCHEMA_VERSION) {
            val migration = migrations[version]
                ?: error("No config migration registered from schema $version")
            check(migration.toVersion > version) {
                "Config migration from $version must advance the schema version"
            }
            config = migration.migrate(config)
            version = migration.toVersion
        }

        return ConfigEnvelope(
            schemaVersion = version,
            launcherConfig = config,
        )
    }
}

object ConfigCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(raw: String): ConfigDecodeResult = try {
        val root = json.parseToJsonElement(raw).jsonObject
        val envelope = if ("launcherConfig" in root) {
            json.decodeFromString<ConfigEnvelope>(raw)
        } else {
            ConfigEnvelope(
                schemaVersion = 0,
                launcherConfig = json.decodeFromString<LauncherConfig>(raw),
            )
        }

        if (envelope.schemaVersion > CURRENT_CONFIG_SCHEMA_VERSION) {
            ConfigDecodeResult.UnsupportedSchema(
                raw = raw,
                schemaVersion = envelope.schemaVersion,
            )
        } else {
            ConfigDecodeResult.Success(MigrationRegistry.migrate(envelope))
        }
    } catch (t: Throwable) {
        ConfigDecodeResult.Failure(
            raw = raw,
            reason = t.message ?: t::class.java.simpleName,
        )
    }

    fun encode(config: LauncherConfig): String = json.encodeToString(
        ConfigEnvelope(
            schemaVersion = CURRENT_CONFIG_SCHEMA_VERSION,
            launcherConfig = config,
        ),
    )
}
