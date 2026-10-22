package com.conreo.couchytv.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Built-in aerial-video wallpaper sources kept by Aerial Rows:
 * Apple and Amazon Fire TV only. Community collections from Couchy's bundled
 * manifest are deliberately excluded and are never returned to the player.
 */
object BuiltinAerials {

    /** Source filter values (config.builtinSource): All, Apple, Amazon. */
    val SOURCES = listOf("", "A", "Z") // "" = Apple + Amazon

    @Serializable
    private data class Entry(val t: String = "", val u: String, val s: String = "A")

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
                .filter { it.s == "A" || it.s == "Z" }
                .map { it.s to it.u }
                .toList()
        }.getOrDefault(emptyList())
        cache = list
        return list
    }

    /** Video URIs for a collection. [sourceIndex] indexes SOURCES; 0 = Apple + Amazon. */
    fun load(context: Context, sourceIndex: Int = 0): List<String> {
        val key = SOURCES.getOrElse(sourceIndex) { "" }
        val all = loadAll(context)
        return if (key.isEmpty()) all.map { it.second }
        else all.filter { it.first == key }.map { it.second }
    }
}
