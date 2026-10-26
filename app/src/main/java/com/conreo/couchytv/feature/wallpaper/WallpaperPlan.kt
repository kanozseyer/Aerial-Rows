package com.conreo.couchytv.feature.wallpaper

import com.conreo.couchytv.data.ALL_AERIAL_SOURCES
import com.conreo.couchytv.data.ALL_ONLINE_WALLPAPER_SOURCES
import com.conreo.couchytv.data.LauncherConfig

/**
 * Renderer-independent description of the launcher's wallpaper preference and
 * fallback order. Candidates are ordered from highest to lowest priority; the
 * preset is always last so a failed/missing richer source can never leave HOME
 * without a background.
 */
data class WallpaperPlan(
    val candidates: List<WallpaperCandidate>,
    val videoSpeedIndex: Int,
) {
    init {
        require(candidates.isNotEmpty())
        require(candidates.last() is WallpaperCandidate.Preset)
    }
}

sealed interface WallpaperCandidate {
    /** Built-in streamed Aerial collections, identified by stable source IDs. */
    data class BuiltinAerials(val sourceIds: Set<String>) : WallpaperCandidate {
        init {
            require(sourceIds.isNotEmpty())
            require(sourceIds.all { it in ALL_AERIAL_SOURCES })
        }
    }

    /** Persisted content URI chosen by the user. */
    data class LocalVideo(val uri: String) : WallpaperCandidate {
        init {
            require(uri.isNotBlank())
        }
    }

    /** Cached on-demand online still image. Revision changes only for explicit Next. */
    data class OnlinePhoto(val sourceId: String, val revision: Int) : WallpaperCandidate {
        init {
            require(sourceId in ALL_ONLINE_WALLPAPER_SOURCES)
            require(revision >= 0)
        }
    }

    /** App-private wallpaper.jpg. Availability/decoding is checked by the adapter. */
    data object CustomPhoto : WallpaperCandidate

    /** Built-in gradient/preset index. Always the final fallback. */
    data class Preset(val index: Int) : WallpaperCandidate
}

object WallpaperPlanResolver {
    fun from(config: LauncherConfig): WallpaperPlan {
        val candidates = buildList {
            val aerialSources = config.builtinSources
                .asSequence()
                .filter { it in ALL_AERIAL_SOURCES }
                .toCollection(linkedSetOf())
            if (config.useBuiltinAerials && aerialSources.isNotEmpty()) {
                add(WallpaperCandidate.BuiltinAerials(aerialSources))
            }

            if (config.useVideoWallpaper && config.videoUri.isNotBlank()) {
                add(WallpaperCandidate.LocalVideo(config.videoUri.trim()))
            }

            val onlineSource = config.onlineWallpaperSource
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
        }

        return WallpaperPlan(
            candidates = candidates,
            videoSpeedIndex = config.videoSpeed,
        )
    }
}
