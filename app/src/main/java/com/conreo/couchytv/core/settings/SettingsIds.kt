package com.conreo.couchytv.core.settings

object SettingsRoutes {
    val Main = SettingsRoute.Root
    val Wallpaper = SettingsRoute("wallpaper")
    val Display = SettingsRoute("display")
    val StatusBar = SettingsRoute("status_bar")
    val Apps = SettingsRoute("apps")
    val Channels = SettingsRoute("channels")
    val Categories = SettingsRoute("categories")
    val Launcher = SettingsRoute("launcher")
    val About = SettingsRoute("about")

    val all: List<SettingsRoute> = listOf(
        Main,
        Wallpaper,
        Display,
        StatusBar,
        Apps,
        Channels,
        Categories,
        Launcher,
        About,
    )
}

/** Stable IDs for the root rows. Labels/locales can change without changing focus identity. */
object MainSettingIds {
    val Apps = SettingId("main.apps")
    val Categories = SettingId("main.categories")
    val Channels = SettingId("main.channels")
    val Display = SettingId("main.display")
    val Wallpaper = SettingId("main.wallpaper")
    val StatusBar = SettingId("main.status_bar")
    val Launcher = SettingId("main.launcher")
    val AndroidSettings = SettingId("main.android_settings")
    val About = SettingId("main.about")

    val all: List<SettingId> = listOf(
        Apps,
        Categories,
        Channels,
        Display,
        Wallpaper,
        StatusBar,
        Launcher,
        AndroidSettings,
        About,
    )
}
