package com.conreo.couchytv.feature.wallpaper

import com.conreo.couchytv.data.AerialFailureTracker
import com.conreo.couchytv.data.AerialItem
import com.conreo.couchytv.data.AerialShuffleBag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinAerialProviderTest {
    private fun item(id: String) = AerialItem(
        id = id,
        title = id,
        sourceId = "A",
        uri = "https://example.test/$id.mov",
    )

    @Test
    fun providerUsesFairBagOverCatalogue() = runBlocking {
        val a = item("a")
        val b = item("b")
        val provider = BuiltinAerialProvider(
            catalogue = AerialCatalogue { listOf(a, b) },
            shuffleBag = AerialShuffleBag { it },
        )

        assertEquals(AerialProviderResult.Ready(a), provider.next(setOf("A")))
        assertEquals(AerialProviderResult.Ready(b), provider.next(setOf("A")))
    }

    @Test
    fun quarantinedOnlyClipReturnsRetryThenRecovers() = runBlocking {
        var now = 1_000L
        val clip = item("a")
        val provider = BuiltinAerialProvider(
            catalogue = AerialCatalogue { listOf(clip) },
            shuffleBag = AerialShuffleBag { it },
            failures = AerialFailureTracker(
                failuresBeforeQuarantine = 1,
                quarantineMs = 30_000L,
                clockMs = { now },
            ),
        )

        assertEquals(AerialProviderResult.Ready(clip), provider.next(setOf("A")))
        assertTrue(provider.recordFailure(clip.id))
        assertEquals(AerialProviderResult.RetryAfter(30_000L), provider.next(setOf("A")))

        now += 30_000L
        assertEquals(AerialProviderResult.Ready(clip), provider.next(setOf("A")))
    }

    @Test
    fun emptyCatalogueIsExplicit() = runBlocking {
        val provider = BuiltinAerialProvider(
            catalogue = AerialCatalogue { emptyList() },
            shuffleBag = AerialShuffleBag { it },
        )

        assertEquals(AerialProviderResult.Empty, provider.next(setOf("A")))
    }

    @Test
    fun successfulPlaybackClearsFailureBeforeThreshold() = runBlocking {
        val clip = item("a")
        val provider = BuiltinAerialProvider(
            catalogue = AerialCatalogue { listOf(clip) },
            shuffleBag = AerialShuffleBag { it },
            failures = AerialFailureTracker(failuresBeforeQuarantine = 2),
        )

        provider.recordFailure(clip.id)
        provider.recordSuccess(clip.id)
        assertTrue(!provider.recordFailure(clip.id))
    }
}
