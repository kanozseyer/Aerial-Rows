package com.conreo.aerialrows.wallpaper

import android.content.Context
import org.json.JSONArray

internal data class AerialSource(
    val id: String,
    val label: String,
)

internal data class AerialEntry(
    val title: String,
    val uri: String,
    val sourceId: String,
)

internal object AerialCatalogue {
    val sources = listOf(
        AerialSource("A", "Apple"),
        AerialSource("Z", "Amazon"),
        AerialSource("C1", "Community 1"),
        AerialSource("C2", "Community 2"),
    )
    val sourceIds: Set<String> = sources.mapTo(linkedSetOf()) { it.id }

    @Volatile
    private var cache: List<AerialEntry>? = null

    fun loadAll(context: Context): List<AerialEntry> {
        cache?.let { return it }
        return synchronized(this) {
            cache?.let { return@synchronized it }
            val raw = context.assets.open("aerials.json").bufferedReader().use { it.readText() }
            val json = JSONArray(raw)
            val parsed = buildList {
                for (index in 0 until json.length()) {
                    val item = json.optJSONObject(index) ?: continue
                    val sourceId = item.optString("s").trim()
                    val uri = item.optString("u").trim()
                    if (sourceId !in sourceIds || uri.isEmpty()) continue
                    add(
                        AerialEntry(
                            title = item.optString("t").trim().ifBlank { "Aerial" },
                            uri = uri,
                            sourceId = sourceId,
                        ),
                    )
                }
            }.distinctBy { "${it.sourceId}:${it.uri}" }
            cache = parsed
            parsed
        }
    }

    fun loadSelected(context: Context, selectedSources: Set<String>): List<AerialEntry> {
        if (selectedSources.isEmpty()) return emptyList()
        val selected = selectedSources.intersect(sourceIds)
        return loadAll(context).filter { it.sourceId in selected }
    }

    fun counts(context: Context): Map<String, Int> =
        loadAll(context).groupingBy(AerialEntry::sourceId).eachCount()

    fun sourceLabel(sourceId: String): String =
        sources.firstOrNull { it.id == sourceId }?.label ?: sourceId
}
