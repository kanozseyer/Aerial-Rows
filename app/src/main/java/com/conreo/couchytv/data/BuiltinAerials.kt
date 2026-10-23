package com.conreo.couchytv.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Stable typed metadata for one built-in Aerial clip. */
data class AerialItem(
    val id: String,
    val title: String,
    val sourceId: String,
    val uri: String,
)

/**
 * Build a typed Aerial item from the compact asset fields. The URI remains the
 * durable semantic identity inside its source collection; visible titles are
 * metadata only and can change without changing the clip identity.
 */
internal fun aerialItem(
    sourceId: String,
    title: String,
    uri: String,
): AerialItem? {
    val source = sourceId.trim()
    val mediaUri = uri.trim()
    if (source.isEmpty() || mediaUri.isEmpty()) return null
    return AerialItem(
        id = "$source:$mediaUri",
        title = title.trim().ifBlank { "Aerial" },
        sourceId = source,
        uri = mediaUri,
    )
}

/**
 * Built-in aerial-video wallpaper sources bundled with Couchy/Aerial Rows.
 * Collections can be combined freely: Apple, Amazon Fire TV, Community 1 and
 * Community 2. Typed items retain the title/source metadata from the asset;
 * legacy URL-only loaders remain available while playback migrates separately.
 */
object BuiltinAerials {

    val SOURCE_APPLE = "A"
    val SOURCE_AMAZON = "Z"
    val SOURCE_COMMUNITY_1 = "C1"
    val SOURCE_COMMUNITY_2 = "C2"
    val SOURCES = listOf(SOURCE_APPLE, SOURCE_AMAZON, SOURCE_COMMUNITY_1, SOURCE_COMMUNITY_2)

    @Serializable
    private data class Entry(val t: String = "", val u: String, val s: String = SOURCE_APPLE)

    @Volatile
    private var cache: List<AerialItem>? = null

    private fun loadAll(context: Context): List<AerialItem> {
        cache?.let { return it }
        val list = runCatching {
            val json = context.assets.open("aerials.json")
                .bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }
                .decodeFromString<List<Entry>>(json)
                .asSequence()
                .filter { it.s in SOURCES }
                .mapNotNull { aerialItem(sourceId = it.s, title = it.t, uri = it.u) }
                .toList()
        }.getOrDefault(emptyList())
        cache = list
        return list
    }

    /** Returns typed metadata for every clip from the enabled sources. */
    fun loadItems(context: Context, selectedSources: Set<String>): List<AerialItem> {
        val selected = selectedSources.filter { it in SOURCES }.toSet()
        if (selected.isEmpty()) return emptyList()
        return loadAll(context)
            .asSequence()
            .filter { it.sourceId in selected }
            .toList()
    }

    /** URL-only compatibility surface used by the current wallpaper player. */
    fun load(context: Context, selectedSources: Set<String>): List<String> =
        loadItems(context, selectedSources).map(AerialItem::uri)

    /** Legacy overload retained for compatibility with older code/backups. */
    fun load(context: Context, sourceIndex: Int = 0): List<String> {
        val selected = when (sourceIndex) {
            1 -> setOf(SOURCE_APPLE)
            2 -> setOf(SOURCE_AMAZON)
            3 -> setOf(SOURCE_COMMUNITY_1)
            4 -> setOf(SOURCE_COMMUNITY_2)
            else -> SOURCES.toSet()
        }
        return load(context, selected)
    }
}
