package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvProgramCustomizationTest {
    private fun item(
        id: Long,
        intent: String,
        owner: String = "provider.pkg",
        title: String = "Item $id",
    ) = TvProgramItem(
        id = id,
        title = title,
        subtitle = "",
        artworkUri = "",
        intentUri = intent,
        packageName = owner,
        durationMs = 0L,
        playbackPositionMs = 0L,
    )

    @Test
    fun stableKeyPrefersIntentUriOverProviderRowId() {
        val first = item(id = 1L, intent = "app://watch/episode-7")
        val recreated = item(id = 999L, intent = "app://watch/episode-7")
        assertEquals(tvProgramStableKey(first), tvProgramStableKey(recreated))
    }

    @Test
    fun stableKeyFallsBackToOwnerAndNumericId() {
        val value = item(id = 42L, intent = "", owner = "")
        assertEquals("fallback.pkg|id:42", tvProgramStableKey(value, "fallback.pkg"))
    }

    @Test
    fun visibleProgramsFiltersHiddenBeforeApplyingLimit() {
        val programs = (1L..6L).map { item(it, "app://watch/$it") }
        val hidden = setOf(tvProgramStableKey(programs[0]), tvProgramStableKey(programs[1]))
        val visible = visibleTvPrograms(programs, hidden, "provider.pkg", limit = 4)
        assertEquals(listOf(3L, 4L, 5L, 6L), visible.map { it.id })
        assertTrue(visible.none { tvProgramStableKey(it) in hidden })
    }
}
