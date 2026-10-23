package com.conreo.couchytv.feature.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalPhotoProviderTest {
    @Test
    fun downsampleKeepsPowerOfTwoNearRequestedWidth() {
        assertEquals(1, imageSampleSize(sourceWidth = 1920, maxWidth = 1280))
        assertEquals(2, imageSampleSize(sourceWidth = 3840, maxWidth = 1280))
        assertEquals(4, imageSampleSize(sourceWidth = 7680, maxWidth = 1280))
    }

    @Test
    fun tinyBlurTextureUsesSameSamplingRule() {
        assertEquals(16, imageSampleSize(sourceWidth = 1920, maxWidth = 96))
        assertEquals(32, imageSampleSize(sourceWidth = 3840, maxWidth = 96))
    }

    @Test
    fun invalidSourceWidthFallsBackToNoSampling() {
        assertEquals(1, imageSampleSize(sourceWidth = 0, maxWidth = 1280))
    }
}
