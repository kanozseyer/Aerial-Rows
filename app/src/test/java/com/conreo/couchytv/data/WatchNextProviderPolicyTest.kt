package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchNextProviderPolicyTest {
    @Test
    fun oneProviderCannotExceedConfiguredCap() {
        val limiter = WatchNextProviderLimiter(
            WatchNextProviderPolicy(
                maxItemsPerProvider = 2,
                excludedPackages = emptySet(),
            ),
        )

        assertTrue(limiter.allow("com.example.video"))
        assertTrue(limiter.allow("com.example.video"))
        assertFalse(limiter.allow("com.example.video"))
        assertTrue(limiter.allow("com.example.other"))
    }

    @Test
    fun excludedProviderNeverConsumesCapacity() {
        val limiter = WatchNextProviderLimiter(
            WatchNextProviderPolicy(
                maxItemsPerProvider = 2,
                excludedPackages = setOf("com.example.hidden"),
            ),
        )

        assertFalse(limiter.allow("com.example.hidden"))
        assertTrue(limiter.allow("com.example.visible"))
    }

    @Test
    fun unknownProviderIsStillBoundedAsOneProvider() {
        val limiter = WatchNextProviderLimiter(
            WatchNextProviderPolicy(
                maxItemsPerProvider = 1,
                excludedPackages = emptySet(),
            ),
        )

        assertTrue(limiter.allow(""))
        assertFalse(limiter.allow(""))
    }

    @Test
    fun normalizerRepairsProviderPolicyValues() {
        val fallback = listOf(CategoryCfg("apps", "Apps"))
        val normalized = ConfigNormalizer.normalize(
            LauncherConfig(
                watchNextPerProviderLimit = 99,
                excludedWatchNextProviders = setOf(" com.example.one ", "", "   "),
            ),
            fallback,
        )

        assertEquals(30, normalized.watchNextPerProviderLimit)
        assertEquals(setOf("com.example.one"), normalized.excludedWatchNextProviders)
    }
}
