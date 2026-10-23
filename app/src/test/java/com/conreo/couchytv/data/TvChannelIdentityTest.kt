package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvChannelIdentityTest {
    @Test
    fun providerIdCreatesStableKeyWhileKeepingLegacyAlias() {
        val identity = tvChannelIdentity(
            packageName = "com.example.provider",
            internalProviderId = "featured",
            channelId = 42L,
        )

        assertEquals("com.example.provider:provider:featured", identity.key)
        assertEquals("com.example.provider:42", identity.legacyKey)
    }

    @Test
    fun missingProviderIdFallsBackToLegacyKey() {
        val identity = tvChannelIdentity(
            packageName = "com.example.provider",
            internalProviderId = "  ",
            channelId = 42L,
        )

        assertEquals("com.example.provider:42", identity.key)
        assertEquals(identity.key, identity.legacyKey)
    }

    @Test
    fun channelReadsOldHiddenAndLimitKeysWithoutLosingNewIdentity() {
        val channel = TvChannelRow(
            id = 99L,
            key = "com.example.provider:provider:featured",
            legacyKey = "com.example.provider:42",
            name = "Featured",
            packageName = "com.example.provider",
            browsable = true,
            programs = emptyList(),
        )

        assertTrue(channel.matchesStoredKey("com.example.provider:provider:featured"))
        assertTrue(channel.matchesStoredKey("com.example.provider:42"))
        assertFalse(channel.matchesStoredKey("com.example.provider:other"))
        assertTrue(channel.isHiddenBy(setOf("com.example.provider:42")))
        assertEquals(
            8,
            channel.configuredItemLimit(
                perChannelLimits = mapOf("com.example.provider:42" to 8),
                defaultLimit = 12,
            ),
        )
    }

    @Test
    fun stableLimitWinsWhenBothStableAndLegacyKeysExist() {
        val channel = TvChannelRow(
            id = 99L,
            key = "com.example.provider:provider:featured",
            legacyKey = "com.example.provider:42",
            name = "Featured",
            packageName = "com.example.provider",
            browsable = true,
            programs = emptyList(),
        )

        assertEquals(
            16,
            channel.configuredItemLimit(
                perChannelLimits = mapOf(
                    "com.example.provider:provider:featured" to 16,
                    "com.example.provider:42" to 8,
                ),
                defaultLimit = 12,
            ),
        )
    }
}
