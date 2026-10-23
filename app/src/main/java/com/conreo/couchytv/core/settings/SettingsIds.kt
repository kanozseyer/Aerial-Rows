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

/** Stable identities for the display screen and its high-frequency selectors. */
object DisplaySettingIds {
    val Layout = SettingId("display.layout")
    val CategoryNames = SettingId("display.category_names")
    val UiScale = SettingId("display.ui_scale")
    val CornerRadius = SettingId("display.corner_radius")
    val IconScale = SettingId("display.icon_scale")
    val Spacing = SettingId("display.spacing")
    val RowsTopMargin = SettingId("display.rows_top_margin")
    val PanelStyle = SettingId("display.panel_style")
    val PanelOpacity = SettingId("display.panel_opacity")
    val PanelTintHue = SettingId("display.panel_tint_hue")
    val PanelTintSaturation = SettingId("display.panel_tint_saturation")
    val PanelEffectStrength = SettingId("display.panel_effect_strength")

    val all: List<SettingId> = listOf(
        Layout,
        CategoryNames,
        UiScale,
        CornerRadius,
        IconScale,
        Spacing,
        RowsTopMargin,
        PanelStyle,
        PanelOpacity,
        PanelTintHue,
        PanelTintSaturation,
        PanelEffectStrength,
    )
}

/** Stable identities for wallpaper source/appearance controls. */
object WallpaperSettingIds {
    val Scrim = SettingId("wallpaper.scrim")
    val GradientMode = SettingId("wallpaper.gradient_mode")
    val GradientPreset = SettingId("wallpaper.gradient_preset")
    val CustomPhoto = SettingId("wallpaper.custom_photo")
    val BuiltinAerials = SettingId("wallpaper.builtin_aerials")
    val AerialApple = SettingId("wallpaper.aerial.apple")
    val AerialAmazon = SettingId("wallpaper.aerial.amazon")
    val AerialCommunity1 = SettingId("wallpaper.aerial.community_1")
    val AerialCommunity2 = SettingId("wallpaper.aerial.community_2")
    val LocalVideo = SettingId("wallpaper.local_video")
    val VideoSpeed = SettingId("wallpaper.video_speed")

    val all: List<SettingId> = listOf(
        Scrim,
        GradientMode,
        GradientPreset,
        CustomPhoto,
        BuiltinAerials,
        AerialApple,
        AerialAmazon,
        AerialCommunity1,
        AerialCommunity2,
        LocalVideo,
        VideoSpeed,
    )
}

/** Stable identities for status-bar controls and the conditional VPN target row. */
object StatusBarSettingIds {
    val Enabled = SettingId("status.enabled")
    val Glass = SettingId("status.glass")
    val Clock24h = SettingId("status.clock_24h")
    val DateFormat = SettingId("status.date_format")
    val VpnButton = SettingId("status.vpn_button")
    val VpnApp = SettingId("status.vpn_app")

    val all: List<SettingId> = listOf(
        Enabled,
        Glass,
        Clock24h,
        DateFormat,
        VpnButton,
        VpnApp,
    )
}

/** Stable identities for launcher-management actions. */
object LauncherSettingIds {
    val Language = SettingId("launcher.language")
    val SaveConfig = SettingId("launcher.save_config")
    val LoadConfig = SettingId("launcher.load_config")
    val RerunWizard = SettingId("launcher.rerun_wizard")

    val all: List<SettingId> = listOf(
        Language,
        SaveConfig,
        LoadConfig,
        RerunWizard,
    )
}
