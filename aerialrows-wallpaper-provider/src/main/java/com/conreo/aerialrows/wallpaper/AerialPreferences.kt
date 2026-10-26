package com.conreo.aerialrows.wallpaper

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal object AerialPreferences {
    private const val PREFS_NAME = "aerial_rows_wallpaper"
    private const val KEY_SELECTED_SOURCES = "selected_sources"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    var selectedSources: Set<String>
        get() {
            val stored = prefs.getStringSet(KEY_SELECTED_SOURCES, null)
            return if (stored == null) {
                AerialCatalogue.sourceIds
            } else {
                stored.intersect(AerialCatalogue.sourceIds)
            }
        }
        set(value) {
            prefs.edit()
                .putStringSet(KEY_SELECTED_SOURCES, value.intersect(AerialCatalogue.sourceIds).toSet())
                .apply()
        }

    fun export(): String = JSONObject().apply {
        put("selectedSources", JSONArray(selectedSources.sorted()))
    }.toString()

    fun import(raw: String) {
        val json = JSONObject(raw)
        val array = json.optJSONArray("selectedSources") ?: return
        val selected = buildSet {
            for (index in 0 until array.length()) {
                val id = array.optString(index)
                if (id in AerialCatalogue.sourceIds) add(id)
            }
        }
        selectedSources = selected
    }
}
