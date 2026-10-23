package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TvProgramOrderingTest {
    @Test
    fun watchNextPrefersMostRecentlyEngagedContent() {
        assertEquals(
            "last_engagement_time_utc_millis DESC, _id DESC",
            tvProgramSortOrder(TvProgramQueryKind.WATCH_NEXT),
        )
    }

    @Test
    fun previewChannelsPreferProviderWeightWithStableTieBreak() {
        assertEquals(
            "weight DESC, _id ASC",
            tvProgramSortOrder(TvProgramQueryKind.PREVIEW_CHANNEL),
        )
    }
}
