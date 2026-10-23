package com.conreo.couchytv.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DynamicSettingsIdsTest {
    @Test
    fun appIdentityUsesPackageAndComponent() {
        val first = AppSettingsIds.app("com.example", "com.example.Main")
        val same = AppSettingsIds.app("com.example", "com.example.Main")
        val otherActivity = AppSettingsIds.app("com.example", "com.example.Tv")

        assertEquals(first, same)
        assertNotEquals(first, otherActivity)
    }

    @Test
    fun channelIdentityUsesSemanticProviderKey() {
        assertEquals(
            ChannelSettingsIds.channel("provider:stable-key"),
            ChannelSettingsIds.channel("provider:stable-key"),
        )
        assertNotEquals(
            ChannelSettingsIds.channel("provider:one"),
            ChannelSettingsIds.channel("provider:two"),
        )
    }

    @Test
    fun categoryIdentityDoesNotDependOnVisibleNameOrPosition() {
        assertEquals(
            SettingId("categories.category:games"),
            CategorySettingsIds.category("games"),
        )
    }
}
