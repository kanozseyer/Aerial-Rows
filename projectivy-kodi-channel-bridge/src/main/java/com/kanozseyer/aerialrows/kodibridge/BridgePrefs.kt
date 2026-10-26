package com.kanozseyer.aerialrows.kodibridge

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BridgeChannel(
    val id: String,
    val title: String,
    val sourceUri: String,
    val channelId: Long = 0L,
)

object BridgePrefs {
    private const val FILE = "kodi_bridge"
    private const val KEY_KODI_PACKAGE = "kodi_package"
    private const val KEY_CHANNELS = "channels"
    const val DEFAULT_KODI_PACKAGE = "org.xbmc.kodi"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var Context.kodiPackage: String
        get() = prefs(this).getString(KEY_KODI_PACKAGE, DEFAULT_KODI_PACKAGE).orEmpty().ifBlank { DEFAULT_KODI_PACKAGE }
        set(value) { prefs(this).edit().putString(KEY_KODI_PACKAGE, value.trim().ifBlank { DEFAULT_KODI_PACKAGE }).apply() }

    fun channels(context: Context): List<BridgeChannel> = runCatching {
        val array = JSONArray(prefs(context).getString(KEY_CHANNELS, "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    BridgeChannel(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        sourceUri = obj.getString("sourceUri"),
                        channelId = obj.optLong("channelId", 0L),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun find(context: Context, id: String): BridgeChannel? = channels(context).firstOrNull { it.id == id }

    fun add(context: Context, title: String, sourceUri: String): BridgeChannel {
        val existing = channels(context).firstOrNull { it.sourceUri == sourceUri }
        if (existing != null) return existing
        val channel = BridgeChannel(UUID.randomUUID().toString(), title, sourceUri)
        save(context, channels(context) + channel)
        return channel
    }

    fun update(context: Context, channel: BridgeChannel) {
        save(context, channels(context).map { if (it.id == channel.id) channel else it })
    }

    fun remove(context: Context, id: String) {
        save(context, channels(context).filterNot { it.id == id })
    }

    private fun save(context: Context, channels: List<BridgeChannel>) {
        val array = JSONArray()
        channels.forEach { channel ->
            array.put(JSONObject().apply {
                put("id", channel.id)
                put("title", channel.title)
                put("sourceUri", channel.sourceUri)
                put("channelId", channel.channelId)
            })
        }
        prefs(context).edit().putString(KEY_CHANNELS, array.toString()).apply()
    }
}
