package com.conreo.couchytv.data

/** Pure configuration repair/validation used after decode and before persistence. */
object ConfigNormalizer {
    fun normalize(
        config: LauncherConfig,
        fallbackCategories: List<CategoryCfg>,
    ): LauncherConfig {
        val validSources = config.builtinSources.filterTo(linkedSetOf()) { it in ALL_AERIAL_SOURCES }
        val sources = if (validSources.isEmpty()) ALL_AERIAL_SOURCES else validSources

        val categories = config.categories
            .asSequence()
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { it.id }
            .toList()
            .ifEmpty { fallbackCategories }

        return config.copy(
            categories = categories,
            builtinSources = sources,
            rowsTopMarginPercent = config.rowsTopMarginPercent.coerceIn(0, 50),
            channelItemLimit = config.channelItemLimit.coerceIn(4, 30),
            channelItemLimits = config.channelItemLimits.mapValues { (_, value) -> value.coerceIn(4, 30) },
            homeRowOrder = config.homeRowOrder.distinct(),
        )
    }
}
