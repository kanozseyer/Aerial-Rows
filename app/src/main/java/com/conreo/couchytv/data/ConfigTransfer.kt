package com.conreo.couchytv.data

sealed interface ConfigImportResult {
    data class Success(val config: LauncherConfig) : ConfigImportResult
    data class UnsupportedSchema(val schemaVersion: Int) : ConfigImportResult
    data object Invalid : ConfigImportResult
}

/**
 * File-transfer boundary for the lightweight Settings > Save/Load configuration flow.
 * Full asset-aware transactional backup remains a later BackupEngine concern.
 */
object ConfigTransfer {
    fun encode(config: LauncherConfig): String = ConfigCodec.encode(config)

    fun decodeForRestore(
        raw: String,
        fallbackCategories: List<CategoryCfg>,
        knownApps: Set<String>,
    ): ConfigImportResult = when (val decoded = ConfigCodec.decode(raw)) {
        is ConfigDecodeResult.Success -> {
            val normalized = ConfigNormalizer.normalize(
                config = decoded.envelope.launcherConfig,
                fallbackCategories = fallbackCategories,
            )
            ConfigImportResult.Success(
                normalized.copy(
                    knownApps = knownApps,
                    setupDone = true,
                ),
            )
        }
        is ConfigDecodeResult.UnsupportedSchema ->
            ConfigImportResult.UnsupportedSchema(decoded.schemaVersion)
        is ConfigDecodeResult.Failure -> ConfigImportResult.Invalid
    }
}
