package com.conreo.couchytv.core.settings

import com.conreo.couchytv.core.model.AppId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun dynamicAppIdsUseSemanticAppIdentity() {
        val first = DynamicSettingIds.app(
            AppId("example.pkg", "example.pkg/.MainActivity"),
        )
        val same = DynamicSettingIds.app(
            AppId("example.pkg", "example.pkg/.MainActivity"),
        )
        val otherComponent = DynamicSettingIds.app(
            AppId("example.pkg", "example.pkg/.TvActivity"),
        )

        assertEquals(first, same)
        assertNotEquals(first, otherComponent)
    }

    @Test
    fun stableRoutesAreUnique() {
        assertEquals(SettingsRoutes.all.size, SettingsRoutes.all.map { it.value }.toSet().size)
    }
}
