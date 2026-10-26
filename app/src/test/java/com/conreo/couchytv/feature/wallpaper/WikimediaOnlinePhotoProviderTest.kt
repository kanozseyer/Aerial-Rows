package com.conreo.couchytv.feature.wallpaper

import com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_ART
import com.conreo.couchytv.data.ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WikimediaOnlinePhotoProviderTest {
    @Test
    fun sourceIdsMapToPublicDomainFeaturedCollections() {
        assertEquals(
            "Featured photographs in the public domain",
            wikimediaCategoryForSource(ONLINE_WALLPAPER_WIKIMEDIA_PD_PHOTOS),
        )
        assertEquals(
            "Featured pictures in the public domain",
            wikimediaCategoryForSource(ONLINE_WALLPAPER_WIKIMEDIA_PD_ART),
        )
    }

    @Test
    fun tvCandidateRequiresLargeLandscapeImage() {
        assertTrue(isTvWallpaperCandidate(3840, 2160))
        assertTrue(isTvWallpaperCandidate(2560, 1600))
        assertFalse(isTvWallpaperCandidate(1200, 800))
        assertFalse(isTvWallpaperCandidate(2000, 3000))
        assertFalse(isTvWallpaperCandidate(5000, 1200))
    }
}
