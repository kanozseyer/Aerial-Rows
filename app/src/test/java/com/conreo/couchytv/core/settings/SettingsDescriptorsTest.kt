package com.conreo.couchytv.core.settings

import com.conreo.couchytv.data.DATE_FORMATS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDescriptorsTest {
    @Test
    fun rapidDisplaySelectorsAreDebounced() {
        val descriptors = listOf(
            DisplaySettingDescriptors.UiScale,
            DisplaySettingDescriptors.CornerRadius,
            DisplaySettingDescriptors.IconScale,
            DisplaySettingDescriptors.Spacing,
            DisplaySettingDescriptors.RowsTopMargin,
            DisplaySettingDescriptors.PanelOpacity,
            DisplaySettingDescriptors.PanelTintHue,
            DisplaySettingDescriptors.PanelTintSaturation,
            DisplaySettingDescriptors.PanelEffectStrength,
        )

        descriptors.forEach { descriptor ->
            assertEquals(PersistencePolicy.DEBOUNCED, descriptor.persistence)
            assertEquals(SettingEffect.RECOMPOSE, descriptor.effect)
        }
    }

    @Test
    fun panelStyleIsImmediateBecauseItChangesVisibleControls() {
        assertEquals(PersistencePolicy.IMMEDIATE, DisplaySettingDescriptors.PanelStyle.persistence)
        assertEquals(SettingEffect.RECOMPOSE, DisplaySettingDescriptors.PanelStyle.effect)
    }

    @Test
    fun rowsTopMarginAcceptsOnlyConfiguredSteps() {
        assertTrue(DisplaySettingDescriptors.RowsTopMargin.accepts(0))
        assertTrue(DisplaySettingDescriptors.RowsTopMargin.accepts(25))
        assertTrue(DisplaySettingDescriptors.RowsTopMargin.accepts(50))
        assertFalse(DisplaySettingDescriptors.RowsTopMargin.accepts(7))
        assertFalse(DisplaySettingDescriptors.RowsTopMargin.accepts(51))
    }

    @Test
    fun wallpaperSpeedAndPresetReloadWallpaperAfterDebounce() {
        listOf(
            WallpaperSettingDescriptors.GradientPreset,
            WallpaperSettingDescriptors.VideoSpeed,
        ).forEach { descriptor ->
            assertEquals(PersistencePolicy.DEBOUNCED, descriptor.persistence)
            assertEquals(SettingEffect.RELOAD_WALLPAPER, descriptor.effect)
        }
    }

    @Test
    fun statusDateFormatTracksConfiguredFormatCountAndDebounces() {
        DATE_FORMATS.indices.forEach { index ->
            assertTrue(StatusBarSettingDescriptors.DateFormat.accepts(index))
        }
        assertFalse(StatusBarSettingDescriptors.DateFormat.accepts(-1))
        assertFalse(StatusBarSettingDescriptors.DateFormat.accepts(DATE_FORMATS.size))
        assertEquals(PersistencePolicy.DEBOUNCED, StatusBarSettingDescriptors.DateFormat.persistence)
        assertEquals(SettingEffect.RECOMPOSE, StatusBarSettingDescriptors.DateFormat.effect)
    }

    @Test
    fun statusVisibilityTogglesStayImmediate() {
        listOf(
            StatusBarSettingDescriptors.Enabled,
            StatusBarSettingDescriptors.Glass,
            StatusBarSettingDescriptors.Clock24h,
            StatusBarSettingDescriptors.VpnButton,
        ).forEach { descriptor ->
            assertEquals(PersistencePolicy.IMMEDIATE, descriptor.persistence)
            assertEquals(SettingEffect.RECOMPOSE, descriptor.effect)
        }
    }

    @Test
    fun descriptorRangesRejectInvalidPersistedValues() {
        assertTrue(DisplaySettingDescriptors.IconScale.accepts(4))
        assertFalse(DisplaySettingDescriptors.IconScale.accepts(5))
        assertTrue(WallpaperSettingDescriptors.VideoSpeed.accepts(3))
        assertFalse(WallpaperSettingDescriptors.VideoSpeed.accepts(-1))
    }
}
