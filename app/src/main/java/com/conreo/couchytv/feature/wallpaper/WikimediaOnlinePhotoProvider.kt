package com.conreo.couchytv.feature.wallpaper

import android.content.Context
import com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_ART
import com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

private const val WIKIMEDIA_API = "https://commons.wikimedia.org/w/api.php"
private const val ONLINE_THUMB_WIDTH = 2560
private const val MAX_DOWNLOAD_BYTES = 16L * 1024L * 1024L
private const val CONNECT_TIMEOUT_MS = 7_000
private const val READ_TIMEOUT_MS = 12_000
private const val USER_AGENT = "Aerial-Rows/1.1 (+https://github.com/kanozseyer/Aerial-Rows)"

internal data class WikimediaPhotoCandidate(
    val title: String,
    val imageUrl: String,
    val width: Int,
    val height: Int,
)

internal fun isTvWallpaperCandidate(width: Int, height: Int): Boolean {
    if (width < 1600 || height <= 0) return false
    val aspect = width.toDouble() / height.toDouble()
    return aspect in 1.45..2.40
}

internal fun wikimediaCategoryForSource(sourceId: String): String = when (sourceId) {
    ONLINE_WALLPAPER_WIKIMEDIA_PD_ART -> "Featured pictures in the public domain"
    else -> "Featured photographs in the public domain"
}

class WikimediaOnlinePhotoProvider(private val context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(sourceId: String, revision: Int, networkConnected: Boolean): PhotoWallpaperAsset? =
        withContext(Dispatchers.IO) {
            val cache = cacheFile(sourceId)
            val cachedRevision = revisionFile(sourceId).takeIf(File::exists)?.readText()?.trim()?.toIntOrNull()

            if (cache.exists() && cachedRevision == revision) {
                return@withContext LocalPhotoProvider(cache).load()
            }

            if (networkConnected) {
                val downloaded = runCatching {
                    val candidate = fetchCandidate(sourceId) ?: return@runCatching false
                    downloadAtomically(candidate.imageUrl, cache)
                }.getOrDefault(false)
                if (downloaded) {
                    revisionFile(sourceId).writeText(revision.toString())
                    return@withContext LocalPhotoProvider(cache).load()
                }
            }

            // Offline/provider failure: retaining the last valid cached frame is
            // better than a flash or blank. If no cache exists, WallpaperHost
            // marks the candidate unavailable and the engine selects Preset.
            if (cache.exists()) LocalPhotoProvider(cache).load() else null
        }

    private fun cacheFile(sourceId: String) = File(appContext.filesDir, "online_wallpaper_${safeId(sourceId)}.img")
    private fun revisionFile(sourceId: String) = File(appContext.filesDir, "online_wallpaper_${safeId(sourceId)}.rev")

    private fun safeId(sourceId: String): String = sourceId.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    private fun fetchCandidate(sourceId: String): WikimediaPhotoCandidate? {
        val category = wikimediaCategoryForSource(sourceId)
        // Vary the category window on explicit refresh while keeping requests
        // compact. If the random window yields no landscape image, retry from
        // the category start rather than failing the whole wallpaper source.
        val start = ('A'.code + Random.nextInt(26)).toChar().toString()
        return queryCategory(category, start) ?: queryCategory(category, null)
    }

    private fun queryCategory(category: String, startPrefix: String?): WikimediaPhotoCandidate? {
        val query = buildString {
            append("?action=query&format=json&formatversion=2")
            append("&generator=categorymembers")
            append("&gcmtitle=").append(enc("Category:$category"))
            append("&gcmtype=file&gcmlimit=120")
            if (startPrefix != null) append("&gcmstartsortkeyprefix=").append(enc(startPrefix))
            append("&prop=imageinfo&iiprop=url%7Csize&iiurlwidth=$ONLINE_THUMB_WIDTH")
        }
        val connection = (URL(WIKIMEDIA_API + query).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            instanceFollowRedirects = true
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val pages = JSONObject(body).optJSONObject("query")?.optJSONArray("pages") ?: return null
            val candidates = buildList {
                for (index in 0 until pages.length()) {
                    val page = pages.optJSONObject(index) ?: continue
                    val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                    val width = info.optInt("width", 0)
                    val height = info.optInt("height", 0)
                    if (!isTvWallpaperCandidate(width, height)) continue
                    val url = info.optString("thumburl").ifBlank { info.optString("url") }
                    if (!url.startsWith("https://")) continue
                    add(
                        WikimediaPhotoCandidate(
                            title = page.optString("title"),
                            imageUrl = url,
                            width = width,
                            height = height,
                        )
                    )
                }
            }
            candidates.randomOrNull()
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadAtomically(url: String, destination: File): Boolean {
        val temp = File(destination.parentFile, destination.name + ".tmp")
        temp.delete()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode !in 200..299) return false
            val declared = connection.contentLengthLong
            if (declared > MAX_DOWNLOAD_BYTES) return false
            var total = 0L
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_DOWNLOAD_BYTES) {
                            temp.delete()
                            return false
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (total == 0L) return false
            if (destination.exists() && !destination.delete()) return false
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }
            return destination.exists() && destination.length() > 0L
        } finally {
            connection.disconnect()
            if (temp.exists()) temp.delete()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}

fun createWikimediaOnlinePhotoProvider(context: Context): WikimediaOnlinePhotoProvider =
    WikimediaOnlinePhotoProvider(context.applicationContext)
