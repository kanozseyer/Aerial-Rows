package com.kanozseyer.aerialrows.projectivy

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperDisplayMode
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperType
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.max

@Serializable
internal data class VariantMedia(
    val location: String = "",
    val title: String = "",
    val url_img: String = "",
    val url_1080p: String = "",
    val url_1080p_hdr: String = "",
    val url_4k: String = "",
    val url_4k_hdr: String = "",
    val author: String = "",
)

@Serializable
internal data class CompactMedia(
    val t: String = "",
    val u: String = "",
    val s: String = PreferencesManager.SOURCE_APPLE,
)

internal data class MediaPreference(
    val prefer4k: Boolean,
    val preferHdr: Boolean,
    val fallback: Boolean,
)

internal data class ChosenMedia(val uri: String, val type: Int)

internal fun chooseMedia(media: VariantMedia, preference: MediaPreference): ChosenMedia? {
    val exact = when {
        preference.prefer4k && preference.preferHdr -> media.url_4k_hdr
        preference.prefer4k -> media.url_4k
        preference.preferHdr -> media.url_1080p_hdr
        else -> media.url_1080p
    }.trim()

    if (!preference.fallback) {
        return exact.takeIf { it.isNotEmpty() }?.let { ChosenMedia(it, WallpaperType.VIDEO) }
    }

    val candidates = when {
        preference.prefer4k && preference.preferHdr -> listOf(
            media.url_4k_hdr to WallpaperType.VIDEO,
            media.url_4k to WallpaperType.VIDEO,
            media.url_1080p_hdr to WallpaperType.VIDEO,
            media.url_1080p to WallpaperType.VIDEO,
            media.url_img to WallpaperType.IMAGE,
        )
        preference.prefer4k -> listOf(
            media.url_4k to WallpaperType.VIDEO,
            media.url_1080p to WallpaperType.VIDEO,
            media.url_img to WallpaperType.IMAGE,
        )
        preference.preferHdr -> listOf(
            media.url_1080p_hdr to WallpaperType.VIDEO,
            media.url_1080p to WallpaperType.VIDEO,
            media.url_img to WallpaperType.IMAGE,
        )
        else -> listOf(
            media.url_1080p to WallpaperType.VIDEO,
            media.url_img to WallpaperType.IMAGE,
        )
    }

    return candidates.firstNotNullOfOrNull { (uri, type) ->
        uri.trim().takeIf { it.isNotEmpty() }?.let { ChosenMedia(it, type) }
    }
}

class AerialRowsCatalogue(private val context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }
    private val remoteCache = RemoteSourceCache(appContext)

    suspend fun load(): List<Wallpaper> {
        val enabled = PreferencesManager.enabledSources
        val preference = MediaPreference(
            prefer4k = PreferencesManager.prefer4k,
            preferHdr = PreferencesManager.preferHdr,
            fallback = PreferencesManager.fallback,
        )
        val result = ArrayList<Wallpaper>(320)
        val bundled = loadBundledCompact()

        if (PreferencesManager.SOURCE_APPLE in enabled) {
            val remoteApple = remoteCache.load(
                PreferencesManager.DEFAULT_APPLE_CATALOGUE_URL,
                PreferencesManager.cacheDurationHours,
            )
            if (remoteApple != null) {
                result += parseVariantJson(remoteApple, preference, "Apple TV")
            } else {
                result += bundled
                    .asSequence()
                    .filter { it.s == PreferencesManager.SOURCE_APPLE }
                    .mapNotNull(::compactToWallpaper)
                    .toList()
            }
        }

        result += bundled
            .asSequence()
            .filter { compact -> compact.s != PreferencesManager.SOURCE_APPLE && compact.s in enabled }
            .mapNotNull(::compactToWallpaper)
            .toList()

        if (PreferencesManager.SOURCE_CUSTOM in enabled) {
            val customUrl = PreferencesManager.customSourceUrl
            if (customUrl.isNotBlank()) {
                remoteCache.load(customUrl, PreferencesManager.cacheDurationHours)
                    ?.let { body -> result += parseFlexible(body, preference, "Custom") }
            }
        }

        val keywords = PreferencesManager.filterKeywords
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)

        return result
            .asSequence()
            .filter { wallpaper -> wallpaper.matches(keywords) }
            .distinctBy(Wallpaper::uri)
            .take(MAX_WALLPAPERS)
            .toList()
    }

    private suspend fun loadBundledCompact(): List<CompactMedia> = withContext(Dispatchers.IO) {
        runCatching {
            appContext.assets.open("aerials.json").bufferedReader().use { reader ->
                json.decodeFromString<List<CompactMedia>>(reader.readText())
            }
        }.getOrDefault(emptyList())
    }

    internal fun parseFlexible(
        body: String,
        preference: MediaPreference,
        sourceLabel: String,
    ): List<Wallpaper> {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return emptyList()
        return when {
            trimmed.startsWith("#") -> parseM3u(trimmed, sourceLabel)
            trimmed.startsWith("[") -> {
                val variants = parseVariantJson(trimmed, preference, sourceLabel)
                if (variants.isNotEmpty()) variants else parseCompactJson(trimmed)
            }
            else -> emptyList()
        }
    }

    internal fun parseVariantJson(
        body: String,
        preference: MediaPreference,
        sourceLabel: String,
    ): List<Wallpaper> = runCatching {
        json.decodeFromString<List<VariantMedia>>(body)
            .mapNotNull { media ->
                val chosen = chooseMedia(media, preference) ?: return@mapNotNull null
                Wallpaper(
                    uri = chosen.uri,
                    type = chosen.type,
                    displayMode = WallpaperDisplayMode.DEFAULT,
                    title = media.title.ifBlank { "Aerial" },
                    source = media.location.ifBlank { sourceLabel },
                    author = media.author.trim().takeIf { it.isNotEmpty() },
                )
            }
    }.getOrDefault(emptyList())

    private fun parseCompactJson(body: String): List<Wallpaper> = runCatching {
        json.decodeFromString<List<CompactMedia>>(body).mapNotNull(::compactToWallpaper)
    }.getOrDefault(emptyList())

    private fun compactToWallpaper(media: CompactMedia): Wallpaper? {
        val uri = media.u.trim()
        if (uri.isEmpty()) return null
        return Wallpaper(
            uri = uri,
            type = if (uri.looksLikeVideo()) WallpaperType.VIDEO else WallpaperType.IMAGE,
            displayMode = WallpaperDisplayMode.DEFAULT,
            title = media.t.trim().ifBlank { "Aerial" },
            source = sourceName(media.s),
        )
    }

    internal fun parseM3u(body: String, fallbackSource: String): List<Wallpaper> {
        val wallpapers = mutableListOf<Wallpaper>()
        var title: String? = null
        var source: String? = null
        val groupRegex = Regex("group-title=\\\"([^\\\"]+)\\\"")

        body.lineSequence().forEach { raw ->
            if (wallpapers.size >= MAX_WALLPAPERS) return@forEach
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF:") -> {
                    title = line.substringAfterLast(',', "").trim().ifBlank { null }
                    source = groupRegex.find(line)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }
                }
                line.startsWith("#") -> Unit
                else -> {
                    wallpapers += Wallpaper(
                        uri = line,
                        type = if (line.looksLikeVideo()) WallpaperType.VIDEO else WallpaperType.IMAGE,
                        displayMode = WallpaperDisplayMode.DEFAULT,
                        title = title,
                        source = source ?: fallbackSource,
                    )
                    title = null
                    source = null
                }
            }
        }
        return wallpapers
    }

    private fun Wallpaper.matches(keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return true
        return keywords.any { keyword ->
            title?.contains(keyword, ignoreCase = true) == true ||
                source?.contains(keyword, ignoreCase = true) == true ||
                author?.contains(keyword, ignoreCase = true) == true
        }
    }

    private fun sourceName(id: String): String = when (id) {
        PreferencesManager.SOURCE_APPLE -> "Apple TV"
        PreferencesManager.SOURCE_AMAZON -> "Amazon Fire TV"
        PreferencesManager.SOURCE_COMMUNITY_1 -> "Community 1"
        PreferencesManager.SOURCE_COMMUNITY_2 -> "Community 2"
        else -> id.ifBlank { "Aerial Rows" }
    }

    private fun String.looksLikeVideo(): Boolean =
        Regex("\\.(mp4|mkv|webm|mov|m4v|3gp|ts|m3u8)(\\?.*)?$", RegexOption.IGNORE_CASE)
            .containsMatchIn(this)

    companion object {
        const val MAX_WALLPAPERS = 1000
    }
}

private class RemoteSourceCache(private val context: Context) {
    private val directory = File(context.cacheDir, "aerial_rows_catalogues").apply { mkdirs() }

    suspend fun load(url: String, maxAgeHours: Int): String? = withContext(Dispatchers.IO) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isEmpty()) return@withContext null

        val cacheFile = File(directory, normalizedUrl.sha256() + ".txt")
        val maxAgeMs = max(0, maxAgeHours).toLong() * 60L * 60L * 1000L
        val fresh = cacheFile.isFile && maxAgeMs > 0L &&
            System.currentTimeMillis() - cacheFile.lastModified() <= maxAgeMs

        if (fresh) return@withContext runCatching { cacheFile.readText() }.getOrNull()

        val downloaded = fetch(normalizedUrl)
        if (downloaded != null) {
            runCatching {
                val temp = File(directory, cacheFile.name + ".tmp")
                temp.writeText(downloaded)
                if (!temp.renameTo(cacheFile)) {
                    cacheFile.writeText(downloaded)
                    temp.delete()
                }
            }
            return@withContext downloaded
        }

        runCatching { cacheFile.takeIf { it.isFile }?.readText() }.getOrNull()
    }

    private fun fetch(url: String): String? {
        val connection = runCatching { URL(url).openConnection() as? HttpURLConnection }.getOrNull()
            ?: return null
        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", "Aerial-Rows-Projectivy/1.0")
            connection.connect()
            if (connection.responseCode !in 200..299) return null
            if (connection.contentLength > MAX_REMOTE_BYTES) return null

            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_REMOTE_BYTES) return null
                    output.write(buffer, 0, read)
                }
                output.toString(Charsets.UTF_8.name())
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        private const val MAX_REMOTE_BYTES = 6 * 1024 * 1024
    }
}
