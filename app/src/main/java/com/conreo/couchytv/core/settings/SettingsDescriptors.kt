package com.conreo.couchytv.core.settings

/** Typed policy for the high-frequency Display settings. */
object DisplaySettingDescriptors {
    val Layout = SettingDescriptor(
        id = DisplaySettingIds.Layout,
        route = SettingsRoutes.Display,
        persistence = PersistencePolicy.IMMEDIATE,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in 0..3 },
    )
    val UiScale = intRange(DisplaySettingIds.UiScale, 0..5)
    val CornerRadius = intRange(DisplaySettingIds.CornerRadius, 0..4)
    val IconScale = intRange(DisplaySettingIds.IconScale, 0..4)
    val Spacing = intRange(DisplaySettingIds.Spacing, 0..4)
    val RowsTopMargin = allowedValues(
        DisplaySettingIds.RowsTopMargin,
        setOf(0, 3, 6, 9, 12, 15, 20, 25, 30, 40, 50),
    )
    val PanelStyle = SettingDescriptor(
        id = DisplaySettingIds.PanelStyle,
        route = SettingsRoutes.Display,
        persistence = PersistencePolicy.IMMEDIATE,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in 0..2 },
    )
    val PanelOpacity = intRange(DisplaySettingIds.PanelOpacity, 0..10)
    val PanelTintHue = intRange(DisplaySettingIds.PanelTintHue, 0..12)
    val PanelTintSaturation = intRange(DisplaySettingIds.PanelTintSaturation, 0..5)
    val PanelEffectStrength = intRange(DisplaySettingIds.PanelEffectStrength, 0..5)

    private fun intRange(id: SettingId, range: IntRange) = SettingDescriptor(
        id = id,
        route = SettingsRoutes.Display,
        persistence = PersistencePolicy.DEBOUNCED,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in range },
    )

    private fun allowedValues(id: SettingId, values: Set<Int>) = SettingDescriptor(
        id = id,
        route = SettingsRoutes.Display,
        persistence = PersistencePolicy.DEBOUNCED,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in values },
    )
}

/** Typed policy for wallpaper controls that can be stepped rapidly. */
object WallpaperSettingDescriptors {
    val Scrim = intRange(WallpaperSettingIds.Scrim, 0..4, SettingEffect.RECOMPOSE)
    val GradientPreset = intRange(
        WallpaperSettingIds.GradientPreset,
        0..4,
        SettingEffect.RELOAD_WALLPAPER,
    )
    val VideoSpeed = intRange(
        WallpaperSettingIds.VideoSpeed,
        0..3,
        SettingEffect.RELOAD_WALLPAPER,
    )

    private fun intRange(
        id: SettingId,
        range: IntRange,
        effect: SettingEffect,
    ) = SettingDescriptor(
        id = id,
        route = SettingsRoutes.Wallpaper,
        persistence = PersistencePolicy.DEBOUNCED,
        effect = effect,
        validator = { value: Int -> value in range },
    )
}

/** Status controls are cheap toggles except date format, which is commonly stepped rapidly. */
object StatusBarSettingDescriptors {
    val Enabled = toggle(StatusBarSettingIds.Enabled)
    val Glass = toggle(StatusBarSettingIds.Glass)
    val Clock24h = toggle(StatusBarSettingIds.Clock24h)
    val DateFormat = SettingDescriptor(
        id = StatusBarSettingIds.DateFormat,
        route = SettingsRoutes.StatusBar,
        persistence = PersistencePolicy.DEBOUNCED,
        effect = SettingEffect.RECOMPOSE,
        // Mirrors the seven stable DATE_FORMATS entries (0 = off).
        validator = { value: Int -> value in 0..6 },
    )
    val VpnButton = toggle(StatusBarSettingIds.VpnButton)
    val VpnApp = SettingDescriptor<String>(
        id = StatusBarSettingIds.VpnApp,
        route = SettingsRoutes.StatusBar,
        persistence = PersistencePolicy.IMMEDIATE,
        effect = SettingEffect.RECOMPOSE,
    )

    private fun toggle(id: SettingId) = SettingDescriptor<Boolean>(
        id = id,
        route = SettingsRoutes.StatusBar,
        persistence = PersistencePolicy.IMMEDIATE,
        effect = SettingEffect.RECOMPOSE,
    )
}
