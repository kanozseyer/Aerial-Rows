package com.kanozseyer.aerialrows.projectivy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperType

class MediaPolicyTest {
    private val media = VariantMedia(
        title = "Test",
        url_img = "image.jpg",
        url_1080p = "1080.mp4",
        url_1080p_hdr = "1080-hdr.mp4",
        url_4k = "4k.mp4",
        url_4k_hdr = "4k-hdr.mp4",
    )

    @Test
    fun prefers4kHdrWhenRequested() {
        val chosen = chooseMedia(media, MediaPreference(prefer4k = true, preferHdr = true, fallback = true))
        assertEquals("4k-hdr.mp4", chosen?.uri)
        assertEquals(WallpaperType.VIDEO, chosen?.type)
    }

    @Test
    fun hdrFallbackKeepsHdrBeforeSdr() {
        val partial = media.copy(url_4k_hdr = "")
        val chosen = chooseMedia(partial, MediaPreference(prefer4k = true, preferHdr = true, fallback = true))
        assertEquals("4k.mp4", chosen?.uri)
    }

    @Test
    fun noFallbackRejectsMissingExactVariant() {
        val partial = media.copy(url_4k_hdr = "")
        val chosen = chooseMedia(partial, MediaPreference(prefer4k = true, preferHdr = true, fallback = false))
        assertNull(chosen)
    }

    @Test
    fun imageIsLastResortWhenFallbackEnabled() {
        val imageOnly = VariantMedia(title = "Image", url_img = "image.jpg")
        val chosen = chooseMedia(imageOnly, MediaPreference(prefer4k = false, preferHdr = false, fallback = true))
        assertEquals("image.jpg", chosen?.uri)
        assertEquals(WallpaperType.IMAGE, chosen?.type)
    }
}
