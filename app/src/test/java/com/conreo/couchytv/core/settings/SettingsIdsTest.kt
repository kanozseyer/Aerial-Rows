package com.conreo.couchytv.core.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsIdsTest {
    @Test
    fun stableSettingIdsAreGloballyUnique() {
        val ids = MainSettingIds.all +
            DisplaySettingIds.all +
            WallpaperSettingIds.all +
            StatusBarSettingIds.all +
            LauncherSettingIds.all
        assertEquals(ids.size, ids.map { it.value }.toSet().size)
    }

    @Test
    fun stableRoutesAreUnique() {
        assertEquals(SettingsRoutes.all.size, SettingsRoutes.all.map { it.value }.toSet().size)
    }
}
