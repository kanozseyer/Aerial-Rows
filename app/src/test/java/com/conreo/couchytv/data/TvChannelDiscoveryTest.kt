package com.conreo.couchytv.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvChannelDiscoveryTest {
    @Test
    fun homeRequiresBrowsableChannelWithPrograms() {
        assertTrue(
            shouldIncludeTvChannel(
                mode = TvChannelQueryMode.HOME,
                browsable = true,
                hasPrograms = true,
            ),
        )
        assertFalse(
            shouldIncludeTvChannel(
                mode = TvChannelQueryMode.HOME,
                browsable = false,
                hasPrograms = true,
            ),
        )
        assertFalse(
            shouldIncludeTvChannel(
                mode = TvChannelQueryMode.HOME,
                browsable = true,
                hasPrograms = false,
            ),
        )
    }

    @Test
    fun settingsDiscoveryKeepsNonBrowsableAndEmptyChannels() {
        assertTrue(
            shouldIncludeTvChannel(
                mode = TvChannelQueryMode.DISCOVERY,
                browsable = false,
                hasPrograms = false,
            ),
        )
        assertTrue(
            shouldIncludeTvChannel(
                mode = TvChannelQueryMode.DISCOVERY,
                browsable = false,
                hasPrograms = true,
            ),
        )
        assertTrue(
            shouldIncludeTvChannel(
                mode = TvChannelQueryMode.DISCOVERY,
                browsable = true,
                hasPrograms = false,
            ),
        )
    }
}
