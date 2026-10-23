package com.conreo.couchytv.feature.wallpaper

import com.conreo.couchytv.data.LauncherConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperPlanTest {
    @Test
    fun richerSourcesKeepCurrentPriorityAndPresetAlwaysTerminatesPlan() {
        val config = LauncherConfig(
            useBuiltinAerials = true,
            builtinSources = linkedSetOf("A", "C1"),
            useVideoWallpaper = true,
            videoUri = "content://wallpaper/video",
            useCustomWallpaper = true,
            wallpaper = 3,
            videoSpeed = 4,
        )

        val plan = WallpaperPlanResolver.from(config)

        assertEquals(
            listOf(
                WallpaperCandidate.BuiltinAerials(linkedSetOf("A", "C1")),
                WallpaperCandidate.LocalVideo("content://wallpaper/video"),
                WallpaperCandidate.CustomPhoto,
                WallpaperCandidate.Preset(3),
            ),
            plan.candidates,
        )
        assertEquals(4, plan.videoSpeedIndex)
    }

    @Test
    fun disabledOrInvalidRichSourcesAreSkipped() {
        val config = LauncherConfig(
            useBuiltinAerials = true,
            builtinSources = setOf("UNKNOWN"),
            useVideoWallpaper = true,
            videoUri = "   ",
            useCustomWallpaper = false,
            wallpaper = 2,
        )

        val plan = WallpaperPlanResolver.from(config)

        assertEquals(listOf(WallpaperCandidate.Preset(2)), plan.candidates)
    }

    @Test
    fun localVideoUriIsTrimmedAndCanFallbackToPhotoThenPreset() {
        val config = LauncherConfig(
            useBuiltinAerials = false,
            useVideoWallpaper = true,
            videoUri = "  content://wallpaper/local  ",
            useCustomWallpaper = true,
            wallpaper = 1,
        )

        val plan = WallpaperPlanResolver.from(config)

        assertEquals(
            listOf(
                WallpaperCandidate.LocalVideo("content://wallpaper/local"),
                WallpaperCandidate.CustomPhoto,
                WallpaperCandidate.Preset(1),
            ),
            plan.candidates,
        )
        assertTrue(plan.candidates.last() is WallpaperCandidate.Preset)
    }
}
