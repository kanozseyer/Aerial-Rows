package com.kanozseyer.aerialrows.kodibridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kanozseyer.aerialrows.kodibridge.BridgePrefs.kodiPackage

data class KodiEntry(
    val name: String,
    val category: String?,
    val uri: String,
    val mediaType: String?,
) {
    val isDirectory: Boolean
        get() = category.equals("directory", true) ||
            category.equals("folder", true) ||
            mediaType.equals("directory", true)
}

object KodiClient {
    const val VIDEO_ADDONS_ROOT = "addons://sources/video/"
    const val RED_LIGHT_NAME = "Red Light"

    fun isInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getApplicationInfo(context.kodiPackage, 0)
    }.isSuccess

    /**
     * Kodi's exported file provider wraps native Kodi routes as
     * content://org.xbmc.kodi.file#plugin://... . Always keep the native route in
     * our database so menu traversal and Projectivy launches survive provider/path
     * changes and do not recursively wrap content:// URIs.
     */
    fun rawUri(context: Context, value: String): String {
        if (value.isBlank()) return value
        return runCatching {
            val parsed = Uri.parse(value)
            if (parsed.scheme.equals("content", true) &&
                parsed.authority.equals("${context.kodiPackage}.file", true) &&
                !parsed.fragment.isNullOrBlank()
            ) parsed.fragment!! else value
        }.getOrDefault(value)
    }

    /** Null means Kodi/provider is unavailable. Empty means the directory returned no rows. */
    fun list(context: Context, sourceUri: String): List<KodiEntry>? {
        val authority = "${context.kodiPackage}.file"
        val nativeSource = rawUri(context, sourceUri)
        val queryUri = Uri.Builder()
            .scheme("content")
            .authority(authority)
            .fragment(nativeSource)
            .build()

        return runCatching {
            context.contentResolver.query(queryUri, null, null, null, null)?.use { cursor ->
                val name = cursor.getColumnIndex("name")
                val category = cursor.getColumnIndex("category")
                val uri = cursor.getColumnIndex("uri")
                val mediaType = cursor.getColumnIndex("mediatype")
                buildList {
                    while (cursor.moveToNext()) {
                        val wrappedUri = if (uri >= 0 && !cursor.isNull(uri)) cursor.getString(uri) else null
                        if (wrappedUri.isNullOrBlank()) continue
                        val itemUri = rawUri(context, wrappedUri)
                        add(
                            KodiEntry(
                                name = if (name >= 0 && !cursor.isNull(name)) cursor.getString(name) else itemUri,
                                category = if (category >= 0 && !cursor.isNull(category)) cursor.getString(category) else null,
                                uri = itemUri,
                                mediaType = if (mediaType >= 0 && !cursor.isNull(mediaType)) cursor.getString(mediaType) else null,
                            ),
                        )
                    }
                }
            }
        }.getOrNull()
    }

    /** Find the installed Red Light video add-on by its Kodi-visible display name. */
    fun redLightRoot(context: Context): KodiEntry? {
        val addons = list(context, VIDEO_ADDONS_ROOT) ?: return null
        return addons.firstOrNull { it.name.trim().equals(RED_LIGHT_NAME, ignoreCase = true) }
            ?: addons.firstOrNull { it.name.contains(RED_LIGHT_NAME, ignoreCase = true) }
            ?: addons.firstOrNull { rawUri(context, it.uri).contains("plugin.video.redlight", ignoreCase = true) }
    }

    fun intent(context: Context, kodiUri: String? = null): Intent {
        val pkg = context.kodiPackage
        val nativeUri = kodiUri?.let { rawUri(context, it) }
        return Intent(if (nativeUri.isNullOrBlank()) Intent.ACTION_MAIN else Intent.ACTION_VIEW).apply {
            component = ComponentName(pkg, "$pkg.Splash")
            if (!nativeUri.isNullOrBlank()) data = Uri.parse(nativeUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun open(context: Context, kodiUri: String? = null): Boolean = runCatching {
        context.startActivity(intent(context, kodiUri))
        true
    }.getOrElse {
        val fallback = context.packageManager.getLaunchIntentForPackage(context.kodiPackage)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return false
        runCatching { context.startActivity(fallback); true }.getOrDefault(false)
    }
}
