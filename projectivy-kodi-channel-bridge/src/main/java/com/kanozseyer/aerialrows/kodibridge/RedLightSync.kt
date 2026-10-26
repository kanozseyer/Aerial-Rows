package com.kanozseyer.aerialrows.kodibridge

import android.content.Context

data class RedLightSyncResult(
    val found: Boolean,
    val discoveredSections: Int,
    val syncedChannels: Int,
)

object RedLightSync {
    private const val MAX_TOP_LEVEL = 14
    private const val MAX_TRAKT_CHILDREN = 14

    /**
     * Discover Red Light by display name, then mirror useful Red Light sections as
     * Android TV channels. Trakt/My List style containers are expanded one level
     * so Projectivy can show Watchlist/Next Episodes/etc as their own rows.
     */
    fun discoverAndSync(context: Context): RedLightSyncResult {
        val root = KodiClient.redLightRoot(context) ?: return RedLightSyncResult(false, 0, 0)
        val top = KodiClient.list(context, root.uri) ?: return RedLightSyncResult(true, 0, 0)

        val sections = linkedMapOf<String, KodiEntry>()
        top.asSequence()
            .filter { it.isDirectory }
            .take(MAX_TOP_LEVEL)
            .forEach { entry ->
                val name = entry.name.trim()
                val lower = name.lowercase()
                val expand = lower.contains("trakt") || lower.contains("my list") || lower.contains("my lists")

                if (expand) {
                    val children = KodiClient.list(context, entry.uri)
                        .orEmpty()
                        .asSequence()
                        .filter { it.isDirectory }
                        .take(MAX_TRAKT_CHILDREN)
                        .toList()

                    if (children.isNotEmpty()) {
                        children.forEach { child ->
                            sections.putIfAbsent("Red Light · ${child.name.trim()}", child)
                        }
                    } else {
                        sections.putIfAbsent("Red Light · $name", entry)
                    }
                } else if (isUsefulTopLevel(lower)) {
                    sections.putIfAbsent("Red Light · $name", entry)
                }
            }

        var synced = 0
        sections.forEach { (title, entry) ->
            val channel = BridgePrefs.add(context, title, entry.uri)
            if (TvPublisher.sync(context, channel)) synced++
        }

        return RedLightSyncResult(true, sections.size, synced)
    }

    private fun isUsefulTopLevel(name: String): Boolean =
        name.contains("movie") ||
            name.contains("tv") ||
            name.contains("show") ||
            name.contains("anime") ||
            name.contains("discover") ||
            name.contains("favorite") ||
            name.contains("watch") ||
            name.contains("history") ||
            name.contains("collection") ||
            name.contains("recommend") ||
            name.contains("popular") ||
            name.contains("trending") ||
            name.contains("list")
}
