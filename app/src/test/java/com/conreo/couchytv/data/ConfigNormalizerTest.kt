package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigNormalizerTest {
    private val fallbackCategories = listOf(
        CategoryCfg("streaming", "Streaming fallback"),
        CategoryCfg("apps", "Apps fallback"),
    )

    @Test
    fun emptyCategoriesAreRepairedFromCanonicalFallback() {
        val normalized = ConfigNormalizer.normalize(
            LauncherConfig(categories = emptyList()),
            fallbackCategories,
        )

        assertEquals(fallbackCategories, normalized.categories)
    }

    @Test
    fun invalidAndDuplicateCategoriesAreRemovedWithoutLosingValidRows() {
        val normalized = ConfigNormalizer.normalize(
            LauncherConfig(
                categories = listOf(
                    CategoryCfg("games", "Games"),
                    CategoryCfg("games", "Duplicate Games"),
                    CategoryCfg("", "Invalid"),
                    CategoryCfg("music", ""),
                    CategoryCfg("apps", "Apps"),
                ),
            ),
            fallbackCategories,
        )

        assertEquals(
            listOf(CategoryCfg("games", "Games"), CategoryCfg("apps", "Apps")),
            normalized.categories,
        )
    }

    @Test
    fun invalidAerialSelectionFallsBackToAllPlayableCollections() {
        val normalized = ConfigNormalizer.normalize(
            LauncherConfig(builtinSources = setOf("bad", "also-bad")),
            fallbackCategories,
        )

        assertEquals(ALL_AERIAL_SOURCES, normalized.builtinSources)
    }

    @Test
    fun rangesAndDuplicateRowIdsAreRepaired() {
        val normalized = ConfigNormalizer.normalize(
            LauncherConfig(
                rowsTopMarginPercent = 99,
                channelItemLimit = 1,
                channelItemLimits = mapOf("a" to 2, "b" to 99),
                homeRowOrder = listOf("apps", "watch_next", "apps"),
            ),
            fallbackCategories,
        )

        assertEquals(50, normalized.rowsTopMarginPercent)
        assertEquals(4, normalized.channelItemLimit)
        assertEquals(mapOf("a" to 4, "b" to 30), normalized.channelItemLimits)
        assertEquals(listOf("apps", "watch_next"), normalized.homeRowOrder)
        assertTrue(normalized.categories.isNotEmpty())
    }
}
