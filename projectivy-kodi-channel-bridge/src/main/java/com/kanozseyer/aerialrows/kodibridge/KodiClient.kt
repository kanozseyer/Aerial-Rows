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

    fun isInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getApplicationInfo(context.kodiPackage, 0)
    }.isSuccess

    /** Null means Kodi/provider is unavailable. Empty means the directory returned no rows. */
    fun list(context: Context, sourceUri: String): List<KodiEntry>? {
        val authority = "${context.kodiPackage}.file"
        val queryUri = Uri.Builder()
            .scheme("content")
            .authority(authority)
            .fragment(sourceUri)
            .build()

        return runCatching {
            context.contentResolver.query(queryUri, null, null, null, null)?.use { cursor ->
                val name = cursor.getColumnIndex("name")
                val category = cursor.getColumnIndex("category")
                val uri = cursor.getColumnIndex("uri")
                val mediaType = cursor.getColumnIndex("mediatype")
                buildList {
                    while (cursor.moveToNext()) {
                        val itemUri = if (uri >= 0 && !cursor.isNull(uri)) cursor.getString(uri) else null
                        if (itemUri.isNullOrBlank()) continue
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

    fun intent(context: Context, kodiUri: String? = null): Intent {
        val pkg = context.kodiPackage
        return Intent(if (kodiUri.isNullOrBlank()) Intent.ACTION_MAIN else Intent.ACTION_GET_CONTENT).apply {
            component = ComponentName(pkg, "$pkg.Splash")
            if (!kodiUri.isNullOrBlank()) data = Uri.parse(kodiUri)
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
