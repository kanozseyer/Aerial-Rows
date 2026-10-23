package com.conreo.couchytv.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDescriptorsTest {
    @Test
    fun migratedDescriptorsHaveUniqueIdsAndExpectedRoutes() {
        val descriptors = SettingsDescriptors.migratedIntegerChoices
        assertEquals(descriptors.size, descriptors.map { it.id }.toSet().size)
        assertTrue(descriptors.filter { it.route == SettingsRoutes.Display }.isNotEmpty())
        assertTrue(descriptors.filter { it.route == SettingsRoutes.Wallpaper }.isNotEmpty())
    }

    @Test
    fun rapidVisualChoicesUseDebouncedPersistence() {
        assertEquals(PersistencePolicy.DEBOUNCED, SettingsDescriptors.DisplayUiScale.persistence)
        assertEquals(PersistencePolicy.DEBOUNCED, SettingsDescriptors.DisplayRoundness.persistence)
        assertEquals(PersistencePolicy.DEBOUNCED, SettingsDescriptors.DisplayIconSize.persistence)
        assertEquals(PersistencePolicy.DEBOUNCED, SettingsDescriptors.DisplaySpacing.persistence)
        assertEquals(PersistencePolicy.DEBOUNCED, SettingsDescriptors.WallpaperVideoSpeed.persistence)
    }

    @Test
    fun discreteModeChoicesStayImmediate() {
        assertEquals(PersistencePolicy.IMMEDIATE, SettingsDescriptors.DisplayLayout.persistence)
        assertEquals(PersistencePolicy.IMMEDIATE, SettingsDescriptors.WallpaperDimming.persistence)
        assertEquals(PersistencePolicy.IMMEDIATE, SettingsDescriptors.WallpaperGradientPreset.persistence)
    }
}
