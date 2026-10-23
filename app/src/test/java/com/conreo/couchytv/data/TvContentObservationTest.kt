package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvContentObservationTest {
    @Test
    fun disabledTvRowsObserveNothing() {
        assertTrue(
            tvContentObservationTargets(
                showWatchNext = false,
                showPreviewChannels = false,
            ).isEmpty(),
        )
    }

    @Test
    fun watchNextOnlyObservesWatchNextProvider() {
        assertEquals(
            setOf(TvContentObservation.WATCH_NEXT),
            tvContentObservationTargets(
                showWatchNext = true,
                showPreviewChannels = false,
            ),
        )
    }

    @Test
    fun previewChannelsObserveChannelMetadataAndPrograms() {
        assertEquals(
            setOf(
                TvContentObservation.CHANNELS,
                TvContentObservation.PREVIEW_PROGRAMS,
            ),
            tvContentObservationTargets(
                showWatchNext = false,
                showPreviewChannels = true,
            ),
        )
    }

    @Test
    fun allTvRowsObserveExactlyThreeProviderSurfaces() {
        assertEquals(
            setOf(
                TvContentObservation.WATCH_NEXT,
                TvContentObservation.CHANNELS,
                TvContentObservation.PREVIEW_PROGRAMS,
            ),
            tvContentObservationTargets(
                showWatchNext = true,
                showPreviewChannels = true,
            ),
        )
    }
}
