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
    val semanticKey: String = "",
    val missingCount: Int = 0,
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
                        semanticKey = obj.optString("semanticKey", ""),
                        missingCount = obj.optInt("missingCount", 0),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun find(context: Context, id: String): BridgeChannel? = channels(context).firstOrNull { it.id == id }

    fun add(context: Context, title: String, sourceUri: String): BridgeChannel =
        upsert(context, "", title, sourceUri)

    /**
     * Keep the Android TV channel identity stable while Red Light moves or rewrites
     * the underlying plugin route. Matching order deliberately supports migration
     * from older bridge builds that only stored title/sourceUri.
     */
    fun upsert(context: Context, semanticKey: String, title: String, sourceUri: String): BridgeChannel {
        val all = channels(context).toMutableList()
        val index = all.indexOfFirst { semanticKey.isNotBlank() && it.semanticKey == semanticKey }
            .takeIf { it >= 0 }
            ?: all.indexOfFirst { it.title.equals(title, ignoreCase = true) }.takeIf { it >= 0 }
            ?: all.indexOfFirst { it.sourceUri == sourceUri }.takeIf { it >= 0 }

        val updated = if (index != null) {
            all[index].copy(
                title = title,
                sourceUri = sourceUri,
                semanticKey = semanticKey.ifBlank { all[index].semanticKey },
                missingCount = 0,
            ).also { all[index] = it }
        } else {
            BridgeChannel(
                id = UUID.randomUUID().toString(),
                title = title,
                sourceUri = sourceUri,
                semanticKey = semanticKey,
            ).also { all += it }
        }
        save(context, all)
        return updated
    }

    fun update(context: Context, channel: BridgeChannel) {
        save(context, channels(context).map { if (it.id == channel.id) channel else it })
    }

    fun remove(context: Context, id: String) {
        save(context, channels(context).filterNot { it.id == id })
    }

    /**
     * A Red Light row is only considered gone after two successful discovery
     * passes. This prevents a temporary Kodi/provider/API failure from deleting
     * the user's Projectivy channel and its cached cards.
     */
    fun markMissingManaged(context: Context, seenSemanticKeys: Set<String>): List<BridgeChannel> {
        val stale = mutableListOf<BridgeChannel>()
        val updated = channels(context).map { channel ->
            if (!channel.semanticKey.startsWith("redlight:")) return@map channel
            if (channel.semanticKey in seenSemanticKeys) return@map channel.copy(missingCount = 0)
            val next = channel.copy(missingCount = channel.missingCount + 1)
            if (next.missingCount >= 2) stale += next
            next
        }
        save(context, updated)
        return stale
    }

    private fun save(context: Context, channels: List<BridgeChannel>) {
        val array = JSONArray()
        channels.forEach { channel ->
            array.put(JSONObject().apply {
                put("id", channel.id)
                put("title", channel.title)
                put("sourceUri", channel.sourceUri)
                put("channelId", channel.channelId)
                put("semanticKey", channel.semanticKey)
                put("missingCount", channel.missingCount)
            })
        }
        prefs(context).edit().putString(KEY_CHANNELS, array.toString()).apply()
    }
}
