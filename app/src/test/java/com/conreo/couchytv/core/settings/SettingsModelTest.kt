package com.conreo.couchytv.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsModelTest {
    @Test
    fun semanticIdsRejectBlankValues() {
        assertThrows(IllegalArgumentException::class.java) { SettingId("  ") }
        assertThrows(IllegalArgumentException::class.java) { SettingsRoute("") }
    }

    @Test
    fun descriptorCarriesPersistenceEffectAndValidation() {
        val descriptor = SettingDescriptor(
            id = SettingId("wallpaper.video.speed"),
            route = SettingsRoute("wallpaper"),
            persistence = PersistencePolicy.DEBOUNCED,
            effect = SettingEffect.RELOAD_WALLPAPER,
            validator = { value: Int -> value in 0..3 },
        )

        assertTrue(descriptor.accepts(2))
        assertFalse(descriptor.accepts(4))
        assertEquals(2, descriptor.requireValid(2))
        assertThrows(IllegalArgumentException::class.java) { descriptor.requireValid(4) }
    }

    @Test
    fun routeStackBackRestoresRememberedFocusBySemanticId() {
        val wallpaper = SettingsRoutes.Wallpaper
        val source = SettingId("wallpaper.source")
        val speed = SettingId("wallpaper.video.speed")

        var state = SettingsNavigationState()
            .rememberFocus(settingId = source)
            .open(wallpaper)
            .rememberFocus(settingId = speed)

        assertEquals(wallpaper, state.currentRoute)
        assertEquals(speed, state.rememberedFocus())
        assertTrue(state.canGoBack)

        state = state.back()
        assertEquals(SettingsRoutes.Main, state.currentRoute)
        assertEquals(source, state.rememberedFocus())
        assertFalse(state.canGoBack)
    }

    @Test
    fun openingCurrentRouteAndBackingAtRootAreNoOps() {
        val root = SettingsNavigationState()
        assertSame(root, root.open(SettingsRoutes.Main))
        assertSame(root, root.back())
    }

    @Test
    fun routeAndRootSettingCatalogsUseUniqueSemanticIds() {
        assertEquals(SettingsRoutes.all.size, SettingsRoutes.all.map { it.value }.toSet().size)
        assertEquals(MainSettingIds.all.size, MainSettingIds.all.map { it.value }.toSet().size)
    }
}
