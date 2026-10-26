from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"pattern not found in {path}: {old[:140]!r}")
    if text.count(old) != 1:
        raise SystemExit(f"pattern not unique in {path}: count={text.count(old)} {old[:140]!r}")
    p.write_text(text.replace(old, new, 1))


# Config: additive online-photo mode. Sources are deliberately public-domain
# Wikimedia Featured collections so Aerial Rows does not need commercial API
# keys or impose attribution-heavy wallpaper-service terms on the user.
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/Config.kt",
    '''    val useCustomWallpaper: Boolean = false,
    /** video wallpaper: persisted content-URI of a local video, looped muted */
''',
    '''    val useCustomWallpaper: Boolean = false,
    /** Static online wallpaper downloaded/cached on demand. */
    val useOnlineWallpaper: Boolean = false,
    /** Extensible provider/source ID; invalid values normalize to the photo collection. */
    val onlineWallpaperSource: String = ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS,
    /** Incremented by the user-facing Next photo action; persisted so refresh survives recreation. */
    val onlineWallpaperRevision: Int = 0,
    /** video wallpaper: persisted content-URI of a local video, looped muted */
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/Config.kt",
    '''val ALL_AERIAL_SOURCES = setOf("A", "Z", "C1", "C2")

/** Status bar date formats''',
    '''val ALL_AERIAL_SOURCES = setOf("A", "Z", "C1", "C2")

const val ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS = "wikimedia_pd_photos"
const val ONLINE_WALLPAPER_WIKIMEDIA_PD_ART = "wikimedia_pd_art"
val ALL_ONLINE_WALLPAPER_SOURCES = setOf(
    ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS,
    ONLINE_WALLPAPER_WIKIMEDIA_PD_ART,
)

/** Status bar date formats''',
)

replace_once(
    "app/src/main/java/com/conreo/couchytv/data/ConfigNormalizer.kt",
    '''        val sources = if (validSources.isEmpty()) ALL_AERIAL_SOURCES else validSources

        val categories = config.categories
''',
    '''        val sources = if (validSources.isEmpty()) ALL_AERIAL_SOURCES else validSources
        val onlineWallpaperSource = config.onlineWallpaperSource
            .takeIf { it in ALL_ONLINE_WALLPAPER_SOURCES }
            ?: ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS

        val categories = config.categories
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/ConfigNormalizer.kt",
    '''            builtinSources = sources,
            rowsTopMarginPercent = config.rowsTopMarginPercent.coerceIn(0, 50),
''',
    '''            builtinSources = sources,
            onlineWallpaperSource = onlineWallpaperSource,
            onlineWallpaperRevision = config.onlineWallpaperRevision.coerceAtLeast(0),
            rowsTopMarginPercent = config.rowsTopMarginPercent.coerceIn(0, 50),
''',
)

# Stable settings identities for the online-photo mode and its conditional controls.
replace_once(
    "app/src/main/java/com/conreo/couchytv/core/settings/SettingsIds.kt",
    '''    val CustomPhoto = SettingId("wallpaper.custom_photo")
    val BuiltinAerials = SettingId("wallpaper.builtin_aerials")
''',
    '''    val CustomPhoto = SettingId("wallpaper.custom_photo")
    val OnlinePhoto = SettingId("wallpaper.online_photo")
    val OnlinePhotoSource = SettingId("wallpaper.online_photo.source")
    val OnlinePhotoNext = SettingId("wallpaper.online_photo.next")
    val BuiltinAerials = SettingId("wallpaper.builtin_aerials")
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/core/settings/SettingsIds.kt",
    '''        CustomPhoto,
        BuiltinAerials,
''',
    '''        CustomPhoto,
        OnlinePhoto,
        OnlinePhotoSource,
        OnlinePhotoNext,
        BuiltinAerials,
''',
)

# Pure wallpaper plan: online photos are a first-class candidate and still end
# in the built-in preset fallback, so network/cache failures can never blank HOME.
replace_once(
    "app/src/main/java/com/conreo/couchytv/feature/wallpaper/WallpaperPlan.kt",
    '''import com.conreo.couchytv.data.ALL_AERIAL_SOURCES
import com.conreo.couchytv.data.LauncherConfig
''',
    '''import com.conreo.couchytv.data.ALL_AERIAL_SOURCES
import com.conreo.couchytv.data.ALL_ONLINE_WALLPAPER_SOURCES
import com.conreo.couchytv.data.LauncherConfig
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/feature/wallpaper/WallpaperPlan.kt",
    '''    /** App-private wallpaper.jpg. Availability/decoding is checked by the adapter. */
    data object CustomPhoto : WallpaperCandidate

    /** Built-in gradient/preset index. Always the final fallback. */
''',
    '''    /** Cached on-demand online still image. Revision changes only for explicit Next. */
    data class OnlinePhoto(val sourceId: String, val revision: Int) : WallpaperCandidate {
        init {
            require(sourceId in ALL_ONLINE_WALLPAPER_SOURCES)
            require(revision >= 0)
        }
    }

    /** App-private wallpaper.jpg. Availability/decoding is checked by the adapter. */
    data object CustomPhoto : WallpaperCandidate

    /** Built-in gradient/preset index. Always the final fallback. */
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/feature/wallpaper/WallpaperPlan.kt",
    '''            if (config.useCustomWallpaper) {
                add(WallpaperCandidate.CustomPhoto)
            }

            add(WallpaperCandidate.Preset(config.wallpaper))
''',
    '''            val onlineSource = config.onlineWallpaperSource
                .takeIf { it in ALL_ONLINE_WALLPAPER_SOURCES }
            if (config.useOnlineWallpaper && onlineSource != null) {
                add(
                    WallpaperCandidate.OnlinePhoto(
                        sourceId = onlineSource,
                        revision = config.onlineWallpaperRevision.coerceAtLeast(0),
                    )
                )
            }

            if (config.useCustomWallpaper) {
                add(WallpaperCandidate.CustomPhoto)
            }

            add(WallpaperCandidate.Preset(config.wallpaper))
''',
)

# Android provider: only called when the online candidate is enabled. It queries
# Wikimedia's public MediaWiki API with an identifiable User-Agent, requests a
# 2560px thumbnail rather than huge originals, filters to landscape TV-friendly
# public-domain Featured media, atomically caches the result, and can use stale
# cache when offline.
Path("app/src/main/java/com/conreo/couchytv/feature/wallpaper/WikimediaOnlinePhotoProvider.kt").write_text(r'''package com.conreo.couchytv.feature.wallpaper

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
''')

# Runtime integration: demand-gated provider, cached/offline behavior, engine
# availability/fallback, and correct pseudo-blur for whichever static image is active.
replace_once(
    "app/src/main/java/com/conreo/couchytv/feature/wallpaper/WallpaperHost.kt",
    '''    val aerialCandidate = plan.candidates
        .filterIsInstance<WallpaperCandidate.BuiltinAerials>()
        .firstOrNull()
    val photoCandidate = WallpaperCandidate.CustomPhoto.takeIf { it in plan.candidates }

    val photoProvider = remember(context) { createLocalPhotoProvider(context) }
''',
    '''    val aerialCandidate = plan.candidates
        .filterIsInstance<WallpaperCandidate.BuiltinAerials>()
        .firstOrNull()
    val onlineCandidate = plan.candidates
        .filterIsInstance<WallpaperCandidate.OnlinePhoto>()
        .firstOrNull()
    val photoCandidate = WallpaperCandidate.CustomPhoto.takeIf { it in plan.candidates }

    val photoProvider = remember(context) { createLocalPhotoProvider(context) }
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/feature/wallpaper/WallpaperHost.kt",
    '''    val aerialProvider = remember(context) { createBuiltinAerialProvider(context) }
    var aerialItem by remember { mutableStateOf<AerialItem?>(null) }
''',
    '''    val onlineProvider = remember(context) { createWikimediaOnlinePhotoProvider(context) }
    var onlineAsset by remember { mutableStateOf<PhotoWallpaperAsset?>(null) }

    LaunchedEffect(onlineCandidate, networkConnected) {
        val candidate = onlineCandidate ?: run {
            onlineAsset = null
            return@LaunchedEffect
        }
        val asset = onlineProvider.load(
            sourceId = candidate.sourceId,
            revision = candidate.revision,
            networkConnected = networkConnected,
        )
        onlineAsset = asset
        engine = engine.reduce(
            if (asset != null) {
                WallpaperEvent.SourceRecovered(candidate)
            } else {
                WallpaperEvent.SourceUnavailable(
                    candidate,
                    if (networkConnected) WallpaperUnavailableReason.LOAD_FAILED
                    else WallpaperUnavailableReason.NETWORK,
                )
            }
        )
    }

    val aerialProvider = remember(context) { createBuiltinAerialProvider(context) }
    var aerialItem by remember { mutableStateOf<AerialItem?>(null) }
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/feature/wallpaper/WallpaperHost.kt",
    '''            WallpaperCandidate.CustomPhoto -> {
                val image = photoAsset?.sharp
                if (image != null) {
''',
    '''            is WallpaperCandidate.OnlinePhoto -> {
                val image = onlineAsset?.sharp
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(presetBrush))
                }
            }

            WallpaperCandidate.CustomPhoto -> {
                val image = photoAsset?.sharp
                if (image != null) {
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/feature/wallpaper/WallpaperHost.kt",
    '''        if (overlayAlpha > 0.01f) {
            photoAsset?.blurred?.let { blurred ->
''',
    '''        if (overlayAlpha > 0.01f) {
            val selectedBlurred = when (selected) {
                is WallpaperCandidate.OnlinePhoto -> onlineAsset?.blurred
                WallpaperCandidate.CustomPhoto -> photoAsset?.blurred
                else -> null
            }
            selectedBlurred?.let { blurred ->
''',
)

# Settings wiring: mode exclusivity, two public-domain Wikimedia collections,
# explicit Next (no background polling), and stable focus IDs.
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''import com.conreo.couchytv.data.PANEL_LIQUID_GLASS
import kotlinx.coroutines.Dispatchers
''',
    '''import com.conreo.couchytv.data.PANEL_LIQUID_GLASS
import com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_ART
import com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS
import kotlinx.coroutines.Dispatchers
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''                        useCustomWallpaper = true,
                        useVideoWallpaper = false,
                        useBuiltinAerials = false,
''',
    '''                        useCustomWallpaper = true,
                        useOnlineWallpaper = false,
                        useVideoWallpaper = false,
                        useBuiltinAerials = false,
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''                        useVideoWallpaper = true,
                        useCustomWallpaper = false,
                        useBuiltinAerials = false,
''',
    '''                        useVideoWallpaper = true,
                        useCustomWallpaper = false,
                        useOnlineWallpaper = false,
                        useBuiltinAerials = false,
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''                                        useBuiltinAerials = true,
                                        useVideoWallpaper = false,
                                        useCustomWallpaper = false,
''',
    '''                                        useBuiltinAerials = true,
                                        useVideoWallpaper = false,
                                        useCustomWallpaper = false,
                                        useOnlineWallpaper = false,
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''                        onSetScrim = { v ->
''',
    '''                        onSelectOnlinePhoto = {
                            scope.launch {
                                store.update {
                                    it.copy(
                                        useOnlineWallpaper = true,
                                        useCustomWallpaper = false,
                                        useVideoWallpaper = false,
                                        useBuiltinAerials = false,
                                    )
                                }
                            }
                        },
                        onSelectOnlineSource = { sourceId ->
                            scope.launch {
                                store.update {
                                    it.copy(
                                        onlineWallpaperSource = sourceId,
                                        onlineWallpaperRevision = it.onlineWallpaperRevision + 1,
                                        useOnlineWallpaper = true,
                                        useCustomWallpaper = false,
                                        useVideoWallpaper = false,
                                        useBuiltinAerials = false,
                                    )
                                }
                            }
                        },
                        onNextOnlinePhoto = {
                            scope.launch {
                                store.update {
                                    it.copy(
                                        onlineWallpaperRevision = it.onlineWallpaperRevision + 1,
                                        useOnlineWallpaper = true,
                                        useCustomWallpaper = false,
                                        useVideoWallpaper = false,
                                        useBuiltinAerials = false,
                                    )
                                }
                            }
                        },
                        onSetScrim = { v ->
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''                                        useCustomWallpaper = false,
                                        useVideoWallpaper = false,
                                        useBuiltinAerials = false,
''',
    '''                                        useCustomWallpaper = false,
                                        useOnlineWallpaper = false,
                                        useVideoWallpaper = false,
                                        useBuiltinAerials = false,
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''    onSelectBuiltinSource: (Int) -> Unit,
    onSetScrim: (Int) -> Unit,
''',
    '''    onSelectBuiltinSource: (Int) -> Unit,
    onSelectOnlinePhoto: () -> Unit,
    onSelectOnlineSource: (String) -> Unit,
    onNextOnlinePhoto: () -> Unit,
    onSetScrim: (Int) -> Unit,
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''        val staticActive = !config.useBuiltinAerials &&
            !config.useVideoWallpaper && !config.useCustomWallpaper
        val wallpaperFocusIds = remember(staticActive, config.useBuiltinAerials) {
''',
    '''        val staticActive = !config.useBuiltinAerials &&
            !config.useVideoWallpaper && !config.useCustomWallpaper && !config.useOnlineWallpaper
        val wallpaperFocusIds = remember(staticActive, config.useBuiltinAerials, config.useOnlineWallpaper) {
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''                add(WallpaperSettingIds.CustomPhoto)
                add(WallpaperSettingIds.BuiltinAerials)
''',
    '''                add(WallpaperSettingIds.CustomPhoto)
                add(WallpaperSettingIds.OnlinePhoto)
                if (config.useOnlineWallpaper) {
                    add(WallpaperSettingIds.OnlinePhotoSource)
                    add(WallpaperSettingIds.OnlinePhotoNext)
                }
                add(WallpaperSettingIds.BuiltinAerials)
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''        // A photo of your own
        SettingsItem(
            selected = false, onClick = onPickPhoto,
            modifier = focus.modifier(WallpaperSettingIds.CustomPhoto),
            headlineContent = { Text(stringResource(R.string.pick_photo)) },
            supportingContent = { Text(stringResource(R.string.pick_photo_sub)) },
            leadingContent = { Icon(AppIcons.Image, contentDescription = null) },
            trailingContent = { CheckMark(checked = config.useCustomWallpaper) },
        )

        /* ---------------- VIDEO: built-in aerials + your own video ---------------- */
''',
    '''        // A photo of your own
        SettingsItem(
            selected = false, onClick = onPickPhoto,
            modifier = focus.modifier(WallpaperSettingIds.CustomPhoto),
            headlineContent = { Text(stringResource(R.string.pick_photo)) },
            supportingContent = { Text(stringResource(R.string.pick_photo_sub)) },
            leadingContent = { Icon(AppIcons.Image, contentDescription = null) },
            trailingContent = { CheckMark(checked = config.useCustomWallpaper) },
        )

        SettingsItem(
            selected = false,
            onClick = onSelectOnlinePhoto,
            modifier = focus.modifier(WallpaperSettingIds.OnlinePhoto),
            headlineContent = { Text("Online featured photos") },
            supportingContent = { Text("Public-domain Wikimedia Commons images, downloaded only when selected and cached for offline use") },
            leadingContent = { Icon(AppIcons.Image, contentDescription = null) },
            trailingContent = { CheckMark(checked = config.useOnlineWallpaper) },
        )
        if (config.useOnlineWallpaper) {
            val onlineSources = listOf(
                ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS to "Featured photography",
                ONLINE_WALLPAPER_WIKIMEDIA_PD_ART to "Featured art & archives",
            )
            val onlineIndex = onlineSources.indexOfFirst { it.first == config.onlineWallpaperSource }
                .coerceAtLeast(0)
            SelectorRow(
                modifier = focus.modifier(WallpaperSettingIds.OnlinePhotoSource),
                label = "Online collection",
                value = onlineSources[onlineIndex].second,
                options = onlineSources.map { it.second },
                steps = onlineSources.size,
                current = onlineIndex,
                onSelect = { index -> onSelectOnlineSource(onlineSources[index].first) },
            )
            SettingsItem(
                selected = false,
                onClick = onNextOnlinePhoto,
                modifier = focus.modifier(WallpaperSettingIds.OnlinePhotoNext),
                headlineContent = { Text("Next online photo") },
                supportingContent = { Text("Fetch another landscape image from the selected collection") },
                leadingContent = { Icon(AppIcons.Play, contentDescription = null) },
            )
        }

        /* ---------------- VIDEO: built-in aerials + your own video ---------------- */
''',
)

# Plan/unit tests and pure provider-selection tests.
replace_once(
    "app/src/test/java/com/conreo/couchytv/feature/wallpaper/WallpaperPlanTest.kt",
    '''            useCustomWallpaper = true,
            wallpaper = 3,
''',
    '''            useOnlineWallpaper = true,
            onlineWallpaperSource = com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS,
            onlineWallpaperRevision = 7,
            useCustomWallpaper = true,
            wallpaper = 3,
''',
)
replace_once(
    "app/src/test/java/com/conreo/couchytv/feature/wallpaper/WallpaperPlanTest.kt",
    '''                WallpaperCandidate.LocalVideo("content://wallpaper/video"),
                WallpaperCandidate.CustomPhoto,
''',
    '''                WallpaperCandidate.LocalVideo("content://wallpaper/video"),
                WallpaperCandidate.OnlinePhoto(
                    com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS,
                    7,
                ),
                WallpaperCandidate.CustomPhoto,
''',
)
replace_once(
    "app/src/test/java/com/conreo/couchytv/feature/wallpaper/WallpaperPlanTest.kt",
    '''            useCustomWallpaper = false,
            wallpaper = 2,
''',
    '''            useOnlineWallpaper = true,
            onlineWallpaperSource = "invalid-online-source",
            useCustomWallpaper = false,
            wallpaper = 2,
''',
)
Path("app/src/test/java/com/conreo/couchytv/feature/wallpaper/WikimediaOnlinePhotoProviderTest.kt").write_text('''package com.conreo.couchytv.feature.wallpaper

import com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_ART
import com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WikimediaOnlinePhotoProviderTest {
    @Test
    fun sourceIdsMapToPublicDomainFeaturedCollections() {
        assertEquals(
            "Featured photographs in the public domain",
            wikimediaCategoryForSource(ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS),
        )
        assertEquals(
            "Featured pictures in the public domain",
            wikimediaCategoryForSource(ONLINE_WALLPAPER_WIKIMEDIA_PD_ART),
        )
    }

    @Test
    fun tvCandidateRequiresLargeLandscapeImage() {
        assertTrue(isTvWallpaperCandidate(3840, 2160))
        assertTrue(isTvWallpaperCandidate(2560, 1600))
        assertFalse(isTvWallpaperCandidate(1200, 800))
        assertFalse(isTvWallpaperCandidate(2000, 3000))
        assertFalse(isTvWallpaperCandidate(5000, 1200))
    }
}
''')

print("Online static wallpaper source materialized")
