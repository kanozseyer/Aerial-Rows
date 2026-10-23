package com.conreo.couchytv.core.settings

object SettingsDescriptors {
    val DisplayLayout = SettingDescriptor(
        id = DisplaySettingIds.Layout,
        route = SettingsRoutes.Display,
        persistence = PersistencePolicy.IMMEDIATE,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in 0..3 },
    )

    val DisplayUiScale = SettingDescriptor(
        id = DisplaySettingIds.UiScale,
        route = SettingsRoutes.Display,
        persistence = PersistencePolicy.DEBOUNCED,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in 0..5 },
    )

    val DisplayRoundness = SettingDescriptor(
        id = DisplaySettingIds.Roundness,
        route = SettingsRoutes.Display,
        persistence = PersistencePolicy.DEBOUNCED,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in 0..4 },
    )

    val DisplayIconSize = SettingDescriptor(
        id = DisplaySettingIds.IconSize,
        route = SettingsRoutes.Display,
        persistence = PersistencePolicy.DEBOUNCED,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in 0..4 },
    )

    val DisplaySpacing = SettingDescriptor(
        id = DisplaySettingIds.Spacing,
        route = SettingsRoutes.Display,
        persistence = PersistencePolicy.DEBOUNCED,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in 0..4 },
    )

    val WallpaperDimming = SettingDescriptor(
        id = WallpaperSettingIds.Dimming,
        route = SettingsRoutes.Wallpaper,
        persistence = PersistencePolicy.IMMEDIATE,
        effect = SettingEffect.RECOMPOSE,
        validator = { value: Int -> value in 0..4 },
    )

    val WallpaperGradientPreset = SettingDescriptor(
        id = WallpaperSettingIds.GradientPreset,
        route = SettingsRoutes.Wallpaper,
        persistence = PersistencePolicy.IMMEDIATE,
        effect = SettingEffect.RELOAD_WALLPAPER,
        validator = { value: Int -> value in 0..4 },
    )

    val WallpaperVideoSpeed = SettingDescriptor(
        id = WallpaperSettingIds.VideoSpeed,
        route = SettingsRoutes.Wallpaper,
        persistence = PersistencePolicy.DEBOUNCED,
        effect = SettingEffect.RELOAD_WALLPAPER,
        validator = { value: Int -> value in 0..3 },
    )

    val migratedIntegerChoices: List<SettingDescriptor<Int>> = listOf(
        DisplayLayout,
        DisplayUiScale,
        DisplayRoundness,
        DisplayIconSize,
        DisplaySpacing,
        WallpaperDimming,
        WallpaperGradientPreset,
        WallpaperVideoSpeed,
    )
}
