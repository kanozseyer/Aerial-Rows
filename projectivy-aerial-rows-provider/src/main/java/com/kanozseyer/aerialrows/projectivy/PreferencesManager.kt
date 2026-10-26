package com.kanozseyer.aerialrows.projectivy

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object PreferencesManager {
    const val SOURCE_APPLE = "A"
    const val SOURCE_AMAZON = "Z"
    const val SOURCE_COMMUNITY_1 = "C1"
    const val SOURCE_COMMUNITY_2 = "C2"
    const val SOURCE_CUSTOM = "CUSTOM"

    val DEFAULT_SOURCES = setOf(SOURCE_APPLE, SOURCE_AMAZON, SOURCE_COMMUNITY_1, SOURCE_COMMUNITY_2)
    const val DEFAULT_APPLE_CATALOGUE_URL =
        "https://raw.githubusercontent.com/spocky/projectivy-plugin-wallpaper-overflight/main/videos.json"
    const val DEFAULT_CACHE_DURATION_HOURS = 24

    private const val KEY_SOURCES = "sources"
    private const val KEY_PREFER_4K = "prefer_4k"
    private const val KEY_PREFER_HDR = "prefer_hdr"
    private const val KEY_FALLBACK = "fallback"
    private const val KEY_CUSTOM_URL = "custom_url"
    private const val KEY_CACHE_HOURS = "cache_hours"
    private const val KEY_FILTER_KEYWORDS = "filter_keywords"

    private lateinit var preferences: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun init(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    var enabledSources: Set<String>
        get() = preferences.getStringSet(KEY_SOURCES, DEFAULT_SOURCES)?.toSet() ?: DEFAULT_SOURCES
        set(value) = preferences.edit().putStringSet(KEY_SOURCES, value.toSet()).apply()

    fun setSourceEnabled(source: String, enabled: Boolean) {
        val next = enabledSources.toMutableSet()
        if (enabled) next += source else next -= source
        enabledSources = next
    }

    var prefer4k: Boolean
        get() = preferences.getBoolean(KEY_PREFER_4K, true)
        set(value) = preferences.edit().putBoolean(KEY_PREFER_4K, value).apply()

    var preferHdr: Boolean
        get() = preferences.getBoolean(KEY_PREFER_HDR, true)
        set(value) = preferences.edit().putBoolean(KEY_PREFER_HDR, value).apply()

    var fallback: Boolean
        get() = preferences.getBoolean(KEY_FALLBACK, true)
        set(value) = preferences.edit().putBoolean(KEY_FALLBACK, value).apply()

    var customSourceUrl: String
        get() = preferences.getString(KEY_CUSTOM_URL, "").orEmpty().trim()
        set(value) = preferences.edit().putString(KEY_CUSTOM_URL, value.trim()).apply()

    var cacheDurationHours: Int
        get() = preferences.getInt(KEY_CACHE_HOURS, DEFAULT_CACHE_DURATION_HOURS).coerceAtLeast(0)
        set(value) = preferences.edit().putInt(KEY_CACHE_HOURS, value.coerceAtLeast(0)).apply()

    var filterKeywords: String
        get() = preferences.getString(KEY_FILTER_KEYWORDS, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_FILTER_KEYWORDS, value).apply()

    fun export(): String = buildJsonObject {
        preferences.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> put(key, JsonPrimitive(value))
                is Int -> put(key, JsonPrimitive(value))
                is Long -> put(key, JsonPrimitive(value))
                is Float -> put(key, JsonPrimitive(value))
                is String -> put(key, JsonPrimitive(value))
                is Set<*> -> put(key, JsonArray(value.filterIsInstance<String>().sorted().map(::JsonPrimitive)))
            }
        }
    }.toString()

    fun import(raw: String): Boolean = runCatching {
        val obj = json.parseToJsonElement(raw) as? JsonObject
            ?: error("Expected JSON object")
        val editor = preferences.edit().clear()
        obj.forEach { (key, value) ->
            when (value) {
                is JsonArray -> editor.putStringSet(
                    key,
                    value.mapNotNull { element -> element.jsonPrimitive.contentOrNull }.toSet(),
                )
                is JsonPrimitive -> when {
                    value.isString -> editor.putString(key, value.content)
                    value.booleanOrNull != null -> editor.putBoolean(key, value.booleanOrNull!!)
                    value.intOrNull != null -> editor.putInt(key, value.intOrNull!!)
                    else -> Unit
                }
                else -> Unit
            }
        }
        editor.apply()
    }.isSuccess
}
