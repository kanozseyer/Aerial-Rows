package com.conreo.couchytv.feature.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperEngineTest {
    private val aerials = WallpaperCandidate.BuiltinAerials(setOf("A"))
    private val video = WallpaperCandidate.LocalVideo("content://video")
    private val preset = WallpaperCandidate.Preset(0)
    private val plan = WallpaperPlan(
        candidates = listOf(aerials, video, preset),
        videoSpeedIndex = 2,
    )

    @Test
    fun semanticEventsDriveOneSelectionPath() {
        val engine = WallpaperEngine(plan)
        assertEquals(aerials, engine.selection.candidate)

        engine.markUnavailable(aerials, WallpaperUnavailableReason.NETWORK)
        assertEquals(video, engine.selection.candidate)

        engine.requestRefresh(video)
        assertEquals(1L, engine.selection.revision)

        engine.markRecovered(aerials)
        assertEquals(aerials, engine.selection.candidate)
    }

    @Test
    fun planUpdatePrunesRemovedUnavailableState() {
        val engine = WallpaperEngine(plan)
        engine.markUnavailable(aerials, WallpaperUnavailableReason.QUARANTINED)

        val replacement = WallpaperPlan(
            candidates = listOf(video, WallpaperCandidate.Preset(3)),
            videoSpeedIndex = 1,
        )
        engine.updatePlan(replacement)

        assertEquals(video, engine.selection.candidate)
        assertEquals(emptyMap<WallpaperCandidate, WallpaperUnavailableReason>(), engine.state.unavailable)
    }
}
