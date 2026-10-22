package com.conreo.couchytv.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Built-in aerial-video wallpaper sources bundled with Couchy/Aerial Rows.
 * Collections can be combined freely: Apple, Amazon Fire TV, Community 1 and
 * Community 2. The player receives one merged list from the selected sources.
 */
object BuiltinAerials {

    val SOURCE_APPLE = "A"
    val SOURCE_AMAZON = "Z"
    val SOURCE_COMMUNITY_1 = "C1"
    val SOURCE_COMMUNITY_2 = "C2"
    val SOURCES = listOf(SOURCE_APPLE, SOURCE_AMAZON, SOURCE_COMMUNITY_1, SOURCE_COMMUNITY_2)

    @Serializable
    private data class Entry(val t: String = "", val u: String, val s: String = SOURCE_APPLE)

    // source-key -> video URI
    @Volatile
    private var cache: List<Pair<String, String>>? = null

    private fun loadAll(context: Context): List<Pair<String, String>> {
        cache?.let { return it }
        val list = runCatching {
            val json = context.assets.open("aerials.json")
                .bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }
                .decodeFromString<List<Entry>>(json)
                .asSequence()
                .filter { it.s in SOURCES }
                .map { it.s to it.u }
                .toList()
        }.getOrDefault(emptyList())
        cache = list
        return list
    }

    /** Returns a merged playlist from every enabled source. */
    fun load(context: Context, selectedSources: Set<String>): List<String> {
        val selected = selectedSources.filter { it in SOURCES }.toSet()
        if (selected.isEmpty()) return emptyList()
        return loadAll(context)
            .asSequence()
            .filter { it.first in selected }
            .map { it.second }
            .toList()
    }

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
