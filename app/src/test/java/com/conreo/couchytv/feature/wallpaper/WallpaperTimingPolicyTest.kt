package com.conreo.couchytv.feature.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperTimingPolicyTest {
    private val policy = WallpaperTimingPolicy()

    @Test
    fun networkRetryUsesConnectivityAwareDelay() {
        assertEquals(2_500L, policy.networkRetryDelayMs(connected = true))
        assertEquals(6_000L, policy.networkRetryDelayMs(connected = false))
    }

    @Test
    fun providerRetryNeverSpinsBelowMinimum() {
        assertEquals(1_000L, policy.providerRetryDelayMs(0L))
        assertEquals(1_000L, policy.providerRetryDelayMs(500L))
        assertEquals(4_000L, policy.providerRetryDelayMs(4_000L))
    }
}
