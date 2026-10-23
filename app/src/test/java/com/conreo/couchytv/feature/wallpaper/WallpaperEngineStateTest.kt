package com.conreo.couchytv.feature.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperEngineStateTest {
    private val aerials = WallpaperCandidate.BuiltinAerials(setOf("A"))
    private val video = WallpaperCandidate.LocalVideo("content://video")
    private val photo = WallpaperCandidate.CustomPhoto
    private val preset = WallpaperCandidate.Preset(1)
    private val plan = WallpaperPlan(
        candidates = listOf(aerials, video, photo, preset),
        videoSpeedIndex = 3,
    )

    @Test
    fun unavailableSourcesFallThroughInPlanOrderAndPresetCannotBeDisabled() {
        var state = WallpaperEngineState(plan)
        assertEquals(WallpaperSelection(aerials, 0, 0), state.selection)

        state = state.reduce(
            WallpaperEvent.SourceUnavailable(aerials, WallpaperUnavailableReason.NETWORK)
        )
        assertEquals(video, state.selection.candidate)
        assertEquals(1, state.selection.fallbackDepth)

        state = state.reduce(
            WallpaperEvent.SourceUnavailable(video, WallpaperUnavailableReason.LOAD_FAILED)
        )
        assertEquals(photo, state.selection.candidate)

        state = state.reduce(
            WallpaperEvent.SourceUnavailable(photo, WallpaperUnavailableReason.MISSING_ASSET)
        )
        assertEquals(preset, state.selection.candidate)
        assertEquals(3, state.selection.fallbackDepth)

        val afterPresetFailure = state.reduce(
            WallpaperEvent.SourceUnavailable(preset, WallpaperUnavailableReason.LOAD_FAILED)
        )
        assertSame(state, afterPresetFailure)
        assertEquals(preset, afterPresetFailure.selection.candidate)
    }

    @Test
    fun recoveryRestoresHigherPrioritySource() {
        var state = WallpaperEngineState(plan)
            .reduce(WallpaperEvent.SourceUnavailable(aerials, WallpaperUnavailableReason.NETWORK))
        assertEquals(video, state.selection.candidate)

        state = state.reduce(WallpaperEvent.SourceRecovered(aerials))
        assertEquals(aerials, state.selection.candidate)
        assertEquals(0, state.selection.fallbackDepth)
    }

    @Test
    fun refreshChangesOnlyThatSourcesRevision() {
        var state = WallpaperEngineState(plan)
        state = state.reduce(WallpaperEvent.RefreshRequested(aerials))
        state = state.reduce(WallpaperEvent.RefreshRequested(aerials))
        state = state.reduce(WallpaperEvent.RefreshRequested(photo))

        assertEquals(2L, state.revisions[aerials])
        assertEquals(1L, state.revisions[photo])
        assertEquals(2L, state.selection.revision)

        val unchanged = state.reduce(
            WallpaperEvent.RefreshRequested(WallpaperCandidate.Preset(99))
        )
        assertSame(state, unchanged)
    }

    @Test
    fun planChangePrunesStateForRemovedCandidates() {
        var state = WallpaperEngineState(plan)
            .reduce(WallpaperEvent.SourceUnavailable(aerials, WallpaperUnavailableReason.QUARANTINED))
            .reduce(WallpaperEvent.RefreshRequested(aerials))
            .reduce(WallpaperEvent.RefreshRequested(video))

        val newPlan = WallpaperPlan(
            candidates = listOf(video, WallpaperCandidate.Preset(2)),
            videoSpeedIndex = 1,
        )
        state = state.reduce(WallpaperEvent.PlanChanged(newPlan))

        assertEquals(video, state.selection.candidate)
        assertFalse(aerials in state.unavailable)
        assertFalse(aerials in state.revisions)
        assertEquals(1L, state.revisions[video])
    }

    @Test
    fun removingThenReaddingFailedSourceClearsItsFailure() {
        var state = WallpaperEngineState(plan)
            .reduce(WallpaperEvent.SourceUnavailable(video, WallpaperUnavailableReason.LOAD_FAILED))
        assertTrue(video in state.unavailable)

        val withoutVideo = WallpaperPlan(
            candidates = listOf(photo, preset),
            videoSpeedIndex = 3,
        )
        state = state.reduce(WallpaperEvent.PlanChanged(withoutVideo))
        assertFalse(video in state.unavailable)

        state = state.reduce(WallpaperEvent.PlanChanged(plan))
        assertFalse(video in state.unavailable)
        assertEquals(aerials, state.selection.candidate)
    }

    @Test
    fun unchangedPlanKeepsFailureUntilExplicitRecovery() {
        val failed = WallpaperEngineState(plan)
            .reduce(WallpaperEvent.SourceUnavailable(video, WallpaperUnavailableReason.LOAD_FAILED))

        val samePlan = failed.reduce(WallpaperEvent.PlanChanged(plan))
        assertEquals(WallpaperUnavailableReason.LOAD_FAILED, samePlan.unavailable[video])

        val recovered = samePlan.reduce(WallpaperEvent.SourceRecovered(video))
        assertFalse(video in recovered.unavailable)
    }

    @Test
    fun eventsForCandidateOutsidePlanAreIgnored() {
        val otherVideo = WallpaperCandidate.LocalVideo("content://other")
        val state = WallpaperEngineState(plan)

        assertSame(
            state,
            state.reduce(
                WallpaperEvent.SourceUnavailable(otherVideo, WallpaperUnavailableReason.LOAD_FAILED)
            ),
        )
        assertSame(state, state.reduce(WallpaperEvent.RefreshRequested(otherVideo)))
    }
}
