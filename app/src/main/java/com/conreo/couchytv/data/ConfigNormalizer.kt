package com.conreo.couchytv.data

/** Pure configuration repair/validation used after decode and before persistence. */
object ConfigNormalizer {
    fun normalize(
        config: LauncherConfig,
        fallbackCategories: List<CategoryCfg>,
    ): LauncherConfig {
        val validSources = config.builtinSources.filterTo(linkedSetOf()) { it in ALL_AERIAL_SOURCES }
        val sources = if (validSources.isEmpty()) ALL_AERIAL_SOURCES else validSources
        val onlineWallpaperSource = config.onlineWallpaperSource
            .takeIf { it in ALL_ONLINE_WALLPAPER_SOURCES }
            ?: ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS

        val categories = config.categories
            .asSequence()
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { it.id }
            .toList()
            .ifEmpty { fallbackCategories }

        val excludedWatchNextProviders = config.excludedWatchNextProviders
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toCollection(linkedSetOf())
        val hiddenTvPrograms = config.hiddenTvPrograms
            .asSequence()
            .mapNotNull { (channelKey, programKeys) ->
                val key = channelKey.trim()
                val values = programKeys.asSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toCollection(linkedSetOf())
                if (key.isEmpty() || values.isEmpty()) null else key to values
            }
            .toMap()

        return config.copy(
            categories = categories,
            builtinSources = sources,
            onlineWallpaperSource = onlineWallpaperSource,
            onlineWallpaperRevision = config.onlineWallpaperRevision.coerceAtLeast(0),
            rowsTopMarginPercent = config.rowsTopMarginPercent.coerceIn(0, 50),
            watchNextPerProviderLimit = config.watchNextPerProviderLimit.coerceIn(1, 30),
            excludedWatchNextProviders = excludedWatchNextProviders,
            channelItemLimit = config.channelItemLimit.coerceIn(4, 30),
            channelItemLimits = config.channelItemLimits.mapValues { (_, value) -> value.coerceIn(4, 30) },
            hiddenTvPrograms = hiddenTvPrograms,
            homeRowOrder = config.homeRowOrder.distinct(),
        )
    }
}
