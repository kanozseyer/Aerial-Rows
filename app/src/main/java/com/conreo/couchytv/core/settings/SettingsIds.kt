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

/** Stable IDs for the first Display controls migrated to semantic focus/policy. */
object DisplaySettingIds {
    val Layout = SettingId("display.layout")
    val SectionNames = SettingId("display.section_names")
    val UiScale = SettingId("display.ui_scale")
    val Roundness = SettingId("display.roundness")
    val IconSize = SettingId("display.icon_size")
    val Spacing = SettingId("display.spacing")

    val all: List<SettingId> = listOf(
        Layout,
        SectionNames,
        UiScale,
        Roundness,
        IconSize,
        Spacing,
    )
}

/** Stable IDs for wallpaper controls whose focus should survive dialogs/re-entry. */
object WallpaperSettingIds {
    val Dimming = SettingId("wallpaper.dimming")
    val GradientMode = SettingId("wallpaper.gradient_mode")
    val GradientPreset = SettingId("wallpaper.gradient_preset")
    val Photo = SettingId("wallpaper.photo")
    val Aerials = SettingId("wallpaper.aerials")
    val AerialApple = SettingId("wallpaper.aerial.apple")
    val AerialAmazon = SettingId("wallpaper.aerial.amazon")
    val AerialCommunity1 = SettingId("wallpaper.aerial.community_1")
    val AerialCommunity2 = SettingId("wallpaper.aerial.community_2")
    val Video = SettingId("wallpaper.video")
    val VideoSpeed = SettingId("wallpaper.video_speed")

    val all: List<SettingId> = listOf(
        Dimming,
        GradientMode,
        GradientPreset,
        Photo,
        Aerials,
        AerialApple,
        AerialAmazon,
        AerialCommunity1,
        AerialCommunity2,
        Video,
        VideoSpeed,
    )
}
