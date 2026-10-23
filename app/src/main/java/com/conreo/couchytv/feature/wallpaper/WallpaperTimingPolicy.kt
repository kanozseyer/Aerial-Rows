package com.conreo.couchytv.feature.wallpaper

/**
 * Timing rules for wallpaper rotation/recovery. Kept outside Compose so providers
 * and the eventual runtime engine share one deterministic policy.
 */
data class WallpaperTimingPolicy(
    val aerialSafetyCapMs: Long = 10 * 60_000L,
    val connectedRetryMs: Long = 2_500L,
    val disconnectedRetryMs: Long = 6_000L,
    val minimumProviderRetryMs: Long = 1_000L,
) {
    init {
        require(aerialSafetyCapMs > 0)
        require(connectedRetryMs > 0)
        require(disconnectedRetryMs > 0)
        require(minimumProviderRetryMs > 0)
    }

    fun networkRetryDelayMs(connected: Boolean): Long =
        if (connected) connectedRetryMs else disconnectedRetryMs

    fun providerRetryDelayMs(requestedMs: Long): Long =
        requestedMs.coerceAtLeast(minimumProviderRetryMs)
}
