package com.conreo.couchytv.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.conreo.couchytv.Actions
import com.conreo.couchytv.core.launch.LaunchDispatcher
import com.conreo.couchytv.core.launch.LaunchOrigin
import com.conreo.couchytv.core.launch.LaunchRequest
import com.conreo.couchytv.core.launch.LaunchTarget
import com.conreo.couchytv.core.model.AppId
import com.conreo.couchytv.core.settings.DisplaySettingDescriptors
import com.conreo.couchytv.core.settings.DisplaySettingIds
import com.conreo.couchytv.core.settings.DynamicSettingIds
import com.conreo.couchytv.core.settings.MainSettingIds
import com.conreo.couchytv.core.settings.LauncherSettingIds
import com.conreo.couchytv.core.settings.StatusBarSettingDescriptors
import com.conreo.couchytv.core.settings.StatusBarSettingIds
import com.conreo.couchytv.core.settings.WallpaperSettingDescriptors
import com.conreo.couchytv.core.settings.WallpaperSettingIds
import com.conreo.couchytv.core.settings.SettingId
import com.conreo.couchytv.core.settings.SettingsNavigationState
import com.conreo.couchytv.core.settings.SettingsRoutes
import com.conreo.couchytv.R
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.AppRepository
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.ConfigStore
import com.conreo.couchytv.data.DATE_FORMATS
import com.conreo.couchytv.data.LAYOUT_DOCK
import com.conreo.couchytv.data.LAYOUT_GRID
import com.conreo.couchytv.data.LAYOUT_SLIDING_ROWS
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.PANEL_CLEAR
import com.conreo.couchytv.data.PANEL_LIQUID_GLASS
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.serialization.json.Json

private val format = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

/**
 * Settings panel built entirely from focusable rows and buttons — every
 * element is reachable with the D-pad alone. Text entry only ever happens
 * inside a dedicated dialog (opened with the pencil button).
 */
@Composable
fun SettingsSheet(
    config: LauncherConfig,
    apps: List<AppEntry>,
    store: ConfigStore,
    launchDispatcher: LaunchDispatcher,
    onDismiss: () -> Unit,
    onWallpaperChanged: () -> Unit,
    onRerunWizard: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val debouncedWriter = remember(store, scope) { DebouncedSettingsWriter(store, scope) }
    var navigation by remember { mutableStateOf(SettingsNavigationState()) }

    fun closeSettings() {
        scope.launch {
            debouncedWriter.flush()
            onDismiss()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, debouncedWriter) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && debouncedWriter.hasPending) {
                scope.launch { debouncedWriter.flush() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Same document-picker flow as videos: opens the system file manager,
    // works with USB drives and network storage providers.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        File(context.filesDir, "wallpaper.jpg").outputStream().use { out ->
                            input.copyTo(out)
                        }
                    }
                }
                store.update {
                    it.copy(
                        useCustomWallpaper = true,
                        useVideoWallpaper = false,
                        useBuiltinAerials = false,
                    )
                }
                onWallpaperChanged()
            }
        }
    }

    // Video wallpaper: keep a persistable read grant and stream in place —
    // aerial files are hundreds of MB, never copied.
    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch {
                store.update {
                    it.copy(
                        videoUri = uri.toString(),
                        useVideoWallpaper = true,
                        useCustomWallpaper = false,
                        useBuiltinAerials = false,
                    )
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (navigation.canGoBack) navigation = navigation.back() else closeSettings()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Force the dialog window to fill the screen — Compose dialogs otherwise
        // keep a small platform margin, so the panel wouldn't sit flush against
        // the right edge.
        val dialogView = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.SideEffect {
            ((dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window)
                ?.setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                )
        }
        // A Dialog spawns its own window that re-provides the platform density,
        // so the launcher's ScaledUi doesn't reach here — re-apply it. The
        // dialog's LocalConfiguration is the real (unscaled) device width.
        ScaledUi(uiScaleFactor(config.uiScale, androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .width(440.dp)
                    .fillMaxHeight(),
            ) {
                // Slide left when entering a sub-screen, right when going back
                // to Main (the root); cross-fade so it never looks abrupt.
                androidx.compose.animation.AnimatedContent(
                    targetState = navigation.currentRoute,
                    transitionSpec = {
                        val forward = targetState != SettingsRoutes.Main
                        val dir = if (forward) 1 else -1
                        (androidx.compose.animation.slideInHorizontally(tween(260)) { w -> dir * w } +
                            androidx.compose.animation.fadeIn(tween(260))) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(tween(260)) { w -> -dir * w } +
                                androidx.compose.animation.fadeOut(tween(260)))
                    },
                    label = "settingsScreen",
                ) { target ->
                when (target) {
                    SettingsRoutes.Main -> MainScreen(
                        rememberedFocus = navigation.rememberedFocus(SettingsRoutes.Main) ?: MainSettingIds.Apps,
                        onFocus = { id -> navigation = navigation.rememberFocus(SettingsRoutes.Main, id) },
                        onWallpaper = { navigation = navigation.open(SettingsRoutes.Wallpaper) },
                        onDisplay = { navigation = navigation.open(SettingsRoutes.Display) },
                        onStatusBar = { navigation = navigation.open(SettingsRoutes.StatusBar) },
                        onApps = { navigation = navigation.open(SettingsRoutes.Apps) },
                        onChannels = { navigation = navigation.open(SettingsRoutes.Channels) },
                        onCategories = { navigation = navigation.open(SettingsRoutes.Categories) },
                        onLauncher = { navigation = navigation.open(SettingsRoutes.Launcher) },
                        onAndroidSettings = { Actions.openSystemSettings(context) },
                        onAbout = { navigation = navigation.open(SettingsRoutes.About) },
                    )
                    SettingsRoutes.Wallpaper -> WallpaperScreen(
                        config = config,
                        rememberedFocus = navigation.rememberedFocus(SettingsRoutes.Wallpaper),
                        onFocus = { id -> navigation = navigation.rememberFocus(SettingsRoutes.Wallpaper, id) },
                        onSelectBuiltinAerials = {
                            scope.launch {
                                store.update {
                                    it.copy(
                                        useBuiltinAerials = true,
                                        useVideoWallpaper = false,
                                        useCustomWallpaper = false,
                                    )
                                }
                            }
                        },
                        onSelectBuiltinSource = { v ->
                            val key = listOf("A", "Z", "C1", "C2").getOrNull(v - 1)
                            if (key != null) scope.launch {
                                store.update { cfg ->
                                    val next = if (key in cfg.builtinSources) cfg.builtinSources - key
                                    else cfg.builtinSources + key
                                    if (next.isEmpty()) cfg else cfg.copy(builtinSources = next)
                                }
                            }
                        },
                        onSetScrim = { v ->
                            val value = WallpaperSettingDescriptors.Scrim.requireValid(v)
                            debouncedWriter.submit(WallpaperSettingDescriptors.Scrim.id) {
                                it.copy(scrimMode = value)
                            }
                        },
                        onSetSpeed = { v ->
                            val value = WallpaperSettingDescriptors.VideoSpeed.requireValid(v)
                            debouncedWriter.submit(WallpaperSettingDescriptors.VideoSpeed.id) {
                                it.copy(videoSpeed = value)
                            }
                        },
                        onPreset = { i ->
                            scope.launch {
                                store.update {
                                    it.copy(
                                        wallpaper = i,
                                        useCustomWallpaper = false,
                                        useVideoWallpaper = false,
                                        useBuiltinAerials = false,
                                    )
                                }
                            }
                        },
                        onPickPhoto = {
                            runCatching { photoPicker.launch(arrayOf("image/*")) }
                                .onFailure { Actions.toast(context, context.getString(R.string.toast_no_picker)) }
                        },
                        onPickVideo = {
                            runCatching { videoPicker.launch(arrayOf("video/*")) }
                                .onFailure { Actions.toast(context, context.getString(R.string.toast_no_picker)) }
                        },
                    )
                    SettingsRoutes.Display -> DisplayScreen(
                        config = config,
                        rememberedFocus = navigation.rememberedFocus(SettingsRoutes.Display),
                        onFocus = { id -> navigation = navigation.rememberFocus(SettingsRoutes.Display, id) },
                        store = store,
                        debouncedWriter = debouncedWriter,
                        onBack = { navigation = navigation.back() },
                    )
                    SettingsRoutes.StatusBar -> StatusBarScreen(
                        config = config,
                        apps = apps,
                        store = store,
                        debouncedWriter = debouncedWriter,
                        rememberedFocus = navigation.rememberedFocus(SettingsRoutes.StatusBar),
                        onFocus = { id -> navigation = navigation.rememberFocus(SettingsRoutes.StatusBar, id) },
                        onBack = { navigation = navigation.back() },
                    )
                    SettingsRoutes.Apps -> AppsScreen(
                        apps = apps,
                        config = config,
                        rememberedFocus = navigation.rememberedFocus(SettingsRoutes.Apps),
                        onFocus = { id -> navigation = navigation.rememberFocus(SettingsRoutes.Apps, id) },
                        onBack = { navigation = navigation.back() },
                        onLaunch = { pkg ->
                            launchDispatcher.dispatch(
                                LaunchRequest(
                                    target = LaunchTarget.App(AppId(packageName = pkg)),
                                    origin = LaunchOrigin.SETTINGS,
                                ),
                            )
                        },
                        onAppInfo = { pkg -> Actions.openAppInfo(context, pkg) },
                        onUninstall = { pkg -> Actions.uninstall(context, pkg) },
                        onToggleHide = { pkg ->
                            scope.launch {
                                store.update { c ->
                                    c.copy(hidden = if (pkg in c.hidden) c.hidden - pkg else c.hidden + pkg)
                                }
                            }
                        },
                    )
                    SettingsRoutes.Channels -> ChannelsSettingsScreen(
                        config = config,
                        store = store,
                    )
                    SettingsRoutes.Categories -> CategoriesScreen(
                        config = config,
                        apps = apps,
                        store = store,
                        onBack = { navigation = navigation.back() },
                    )
                    SettingsRoutes.About -> AboutScreen(
                        onBack = { navigation = navigation.back() },
                    )
                    SettingsRoutes.Launcher -> LauncherSettingsSubscreen(
                        config = config,
                        store = store,
                        rememberedFocus = navigation.rememberedFocus(SettingsRoutes.Launcher),
                        onFocus = { id -> navigation = navigation.rememberFocus(SettingsRoutes.Launcher, id) },
                        onBack = { navigation = navigation.back() },
                        onRerunWizard = onRerunWizard,
                    )
                }
                }
            }
        }
        }
    }
}

/* ------------------------------ main menu ------------------------------ */

@Composable
private fun MainScreen(
    rememberedFocus: SettingId,
    onFocus: (SettingId) -> Unit,
    onWallpaper: () -> Unit,
    onDisplay: () -> Unit,
    onStatusBar: () -> Unit,
    onApps: () -> Unit,
    onChannels: () -> Unit,
    onCategories: () -> Unit,
    onLauncher: () -> Unit,
    onAndroidSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
        )
        val focusRequesters = remember {
            MainSettingIds.all.associateWith { FocusRequester() }
        }
        val focusTarget = rememberedFocus.takeIf { it in focusRequesters } ?: MainSettingIds.Apps
        LaunchedEffect(focusTarget) {
            repeat(30) {
                if (runCatching { focusRequesters.getValue(focusTarget).requestFocus() }.isSuccess) {
                    return@LaunchedEffect
                }
                withFrameNanos {}
            }
        }
        fun focusModifier(id: SettingId): Modifier = Modifier
            .focusRequester(focusRequesters.getValue(id))
            .onFocusChanged { state -> if (state.isFocused) onFocus(id) }

        // ---- Content: what shows on the home screen ----
        SectionLabel(stringResource(R.string.group_content))
        SettingsItem(
            selected = false, onClick = onApps,
            headlineContent = { Text(stringResource(R.string.item_app)) },
            supportingContent = { Text(stringResource(R.string.item_app_sub)) },
            leadingContent = { Icon(AppIcons.Apps, contentDescription = null) },
            modifier = focusModifier(MainSettingIds.Apps),
        )
        SettingsItem(
            selected = false, onClick = onCategories,
            modifier = focusModifier(MainSettingIds.Categories),
            headlineContent = { Text(stringResource(R.string.item_sections)) },
            supportingContent = { Text(stringResource(R.string.item_sections_sub)) },
            leadingContent = { Icon(AppIcons.Folder, contentDescription = null) },
        )
        SettingsItem(
            selected = false, onClick = onChannels,
            modifier = focusModifier(MainSettingIds.Channels),
            headlineContent = { Text("Channels") },
            supportingContent = { Text("Watch Next and Android TV app channels") },
            leadingContent = { Icon(AppIcons.Play, contentDescription = null) },
        )

        // ---- Appearance: how it looks ----
        SectionLabel(stringResource(R.string.group_appearance))
        SettingsItem(
            selected = false, onClick = onDisplay,
            modifier = focusModifier(MainSettingIds.Display),
            headlineContent = { Text(stringResource(R.string.item_display)) },
            supportingContent = { Text(stringResource(R.string.item_display_sub)) },
            leadingContent = { Icon(AppIcons.Display, contentDescription = null) },
        )
        SettingsItem(
            selected = false, onClick = onWallpaper,
            modifier = focusModifier(MainSettingIds.Wallpaper),
            headlineContent = { Text(stringResource(R.string.item_wallpaper)) },
            supportingContent = { Text(stringResource(R.string.item_wallpaper_sub)) },
            leadingContent = { Icon(AppIcons.Image, contentDescription = null) },
        )
        SettingsItem(
            selected = false, onClick = onStatusBar,
            modifier = focusModifier(MainSettingIds.StatusBar),
            headlineContent = { Text(stringResource(R.string.item_statusbar)) },
            supportingContent = { Text(stringResource(R.string.item_statusbar_sub)) },
            leadingContent = { Icon(AppIcons.Wifi, contentDescription = null) },
        )

        // ---- System ----
        SectionLabel(stringResource(R.string.group_system))
        SettingsItem(
            selected = false, onClick = onLauncher,
            modifier = focusModifier(MainSettingIds.Launcher),
            headlineContent = { Text(stringResource(R.string.item_launcher_settings)) },
            supportingContent = { Text(stringResource(R.string.item_launcher_settings_sub)) },
            leadingContent = { Icon(painter = androidx.compose.ui.res.painterResource(R.drawable.ic_couch), contentDescription = null, modifier = Modifier.size(24.dp)) },
        )
        SettingsItem(
            selected = false, onClick = onAndroidSettings,
            modifier = focusModifier(MainSettingIds.AndroidSettings),
            headlineContent = { Text(stringResource(R.string.item_android_settings)) },
            supportingContent = { Text(stringResource(R.string.item_android_settings_sub)) },
            leadingContent = { Icon(AppIcons.Gear, contentDescription = null) },
        )
        SettingsItem(
            selected = false, onClick = onAbout,
            modifier = focusModifier(MainSettingIds.About),
            headlineContent = { Text(stringResource(R.string.item_about)) },
            leadingContent = { Icon(AppIcons.Info, contentDescription = null) },
        )
    }
}

/* ------------------------------ about ------------------------------ */

@Composable
private fun AboutScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.item_about),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_couch),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
            Column {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "v" + (runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrDefault("?")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stringResource(R.string.about_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 12.dp),
        )

        SectionLabel(stringResource(R.string.about_foss))
        Text(
            stringResource(R.string.about_license),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
        // Source code
        Text(
            stringResource(R.string.about_source) + "  ·  " + stringResource(R.string.github_url),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        )

        // Support — scannable QR to Buy me a coffee (Cookie script font; brand name, not localized)
        Text(
            stringResource(R.string.about_coffee),
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.cookie)
            ),
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 14.dp),
        )
        Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.qr_coffee),
            contentDescription = stringResource(R.string.about_coffee),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp)
                .size(150.dp)
                .clip(RoundedCornerShape(10.dp)),
        )

        Text(
            stringResource(R.string.about_thirdparty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 8.dp),
        )
    }
}

/* ------------------------------ display ------------------------------ */

@Composable
private fun DisplayScreen(
    config: LauncherConfig,
    store: ConfigStore,
    debouncedWriter: DebouncedSettingsWriter,
    rememberedFocus: SettingId?,
    onFocus: (SettingId) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    fun update(transform: (LauncherConfig) -> LauncherConfig) {
        scope.launch { store.update(transform) }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.item_display),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
        )
        SectionLabel(stringResource(R.string.section_layout))
        val displayFocusIds = remember(config.layout, config.panelStyle) {
            buildList {
                add(DisplaySettingIds.Layout)
                if (config.layout != LAYOUT_DOCK) add(DisplaySettingIds.CategoryNames)
                add(DisplaySettingIds.UiScale)
                add(DisplaySettingIds.CornerRadius)
                add(DisplaySettingIds.IconScale)
                add(DisplaySettingIds.Spacing)
                add(DisplaySettingIds.RowsTopMargin)
                add(DisplaySettingIds.PanelStyle)
                if (config.panelStyle != PANEL_CLEAR) {
                    add(DisplaySettingIds.PanelOpacity)
                    add(DisplaySettingIds.PanelTintHue)
                    add(DisplaySettingIds.PanelTintSaturation)
                    if (config.panelStyle == PANEL_LIQUID_GLASS) {
                        add(DisplaySettingIds.PanelEffectStrength)
                    }
                }
            }
        }
        val focus = rememberSettingsFocusBindings(
            ids = displayFocusIds,
            rememberedFocus = rememberedFocus,
            defaultFocus = DisplaySettingIds.Layout,
            onFocused = onFocus,
        )
        SelectorRow(
            modifier = focus.modifier(DisplaySettingIds.Layout),
            label = stringResource(R.string.view_mode),
            value = listOf(stringResource(R.string.mode_carousel), stringResource(R.string.mode_grid), stringResource(R.string.mode_dock), stringResource(R.string.mode_sliding_rows))[config.layout.coerceIn(0, 3)],
            options = listOf(stringResource(R.string.mode_carousel), stringResource(R.string.mode_grid), stringResource(R.string.mode_dock), stringResource(R.string.mode_sliding_rows)),
            description = when (config.layout) {
                LAYOUT_GRID -> stringResource(R.string.mode_grid_desc)
                LAYOUT_DOCK -> stringResource(R.string.mode_dock_desc)
                LAYOUT_SLIDING_ROWS -> stringResource(R.string.mode_sliding_rows_desc)
                else -> stringResource(R.string.mode_carousel_desc)
            },
            steps = 4,
            current = config.layout.coerceIn(0, 3),
            onSelect = { v -> update { it.copy(layout = v) } },
        )
        // Section names are only drawn in carousel/grid — hide the toggle in dock.
        if (config.layout != LAYOUT_DOCK) {
            SectionLabel(stringResource(R.string.section_elements))
            SettingsItem(
                selected = false,
                onClick = { update { it.copy(showCategoryNames = !it.showCategoryNames) } },
                modifier = focus.modifier(DisplaySettingIds.CategoryNames),
                headlineContent = { Text(stringResource(R.string.section_names_label)) },
                trailingContent = { CheckMark(checked = config.showCategoryNames) },
            )
        }
        SectionLabel(stringResource(R.string.section_size))
        run {
            val labels = listOf(stringResource(R.string.scale_auto), "75%", "90%", "100%", "115%", "130%")
            val idx = config.uiScale.coerceIn(0, labels.size - 1)
            SelectorRow(
                modifier = focus.modifier(DisplaySettingIds.UiScale),
                label = stringResource(R.string.ui_scale),
                value = labels[idx],
                options = labels,
                description = if (idx == 0) stringResource(R.string.ui_scale_auto_sub) else null,
                steps = labels.size,
                current = idx,
                onSelect = { v ->
                    val value = DisplaySettingDescriptors.UiScale.requireValid(v)
                    debouncedWriter.submit(DisplaySettingDescriptors.UiScale.id) { it.copy(uiScale = value) }
                },
            )
        }
        SelectorRow(
            modifier = focus.modifier(DisplaySettingIds.CornerRadius),
            label = stringResource(R.string.roundness),
            value = listOf(stringResource(R.string.size_xs), stringResource(R.string.size_s), stringResource(R.string.size_n), stringResource(R.string.size_l), stringResource(R.string.size_xl))[config.cornerRadius.coerceIn(0, 4)],
            options = listOf(stringResource(R.string.size_xs), stringResource(R.string.size_s), stringResource(R.string.size_n), stringResource(R.string.size_l), stringResource(R.string.size_xl)),
            steps = 5,
            current = config.cornerRadius.coerceIn(0, 4),
            onSelect = { v ->
                    val value = DisplaySettingDescriptors.CornerRadius.requireValid(v)
                    debouncedWriter.submit(DisplaySettingDescriptors.CornerRadius.id) { it.copy(cornerRadius = value) }
                },
        )
        SelectorRow(
            modifier = focus.modifier(DisplaySettingIds.IconScale),
            label = stringResource(R.string.icon_size),
            value = listOf(stringResource(R.string.size_xs), stringResource(R.string.size_s), stringResource(R.string.size_n), stringResource(R.string.size_l), stringResource(R.string.size_xl))[config.iconScale.coerceIn(0, 4)],
            options = listOf(stringResource(R.string.size_xs), stringResource(R.string.size_s), stringResource(R.string.size_n), stringResource(R.string.size_l), stringResource(R.string.size_xl)),
            steps = 5,
            current = config.iconScale.coerceIn(0, 4),
            onSelect = { v ->
                    val value = DisplaySettingDescriptors.IconScale.requireValid(v)
                    debouncedWriter.submit(DisplaySettingDescriptors.IconScale.id) { it.copy(iconScale = value) }
                },
        )
        SelectorRow(
            modifier = focus.modifier(DisplaySettingIds.Spacing),
            label = stringResource(R.string.spacing),
            value = listOf(stringResource(R.string.size_xs), stringResource(R.string.size_s), stringResource(R.string.size_n), stringResource(R.string.size_l), stringResource(R.string.size_xl))[config.spacing.coerceIn(0, 4)],
            options = listOf(stringResource(R.string.size_xs), stringResource(R.string.size_s), stringResource(R.string.size_n), stringResource(R.string.size_l), stringResource(R.string.size_xl)),
            steps = 5,
            current = config.spacing.coerceIn(0, 4),
            onSelect = { v ->
                    val value = DisplaySettingDescriptors.Spacing.requireValid(v)
                    debouncedWriter.submit(DisplaySettingDescriptors.Spacing.id) { it.copy(spacing = value) }
                },
        )
        PanelAppearanceSettings(
            config = config,
            store = store,
            debouncedWriter = debouncedWriter,
            focus = focus,
        )
    }
}

/**
 * D-pad selector row: Left/Right steps immediately; OK opens a choice dialog
 * when named options are supplied, otherwise it retains legacy cycle behavior.
 */
@Composable
private fun SelectorRow(
    label: String,
    value: String,
    steps: Int,
    current: Int,
    onSelect: (Int) -> Unit,
    description: String? = null,
    modifier: Modifier = Modifier,
    options: List<String>? = null,
) {
    var showChoices by remember { mutableStateOf(false) }
    var optimisticCurrent by remember { mutableIntStateOf(current) }
    LaunchedEffect(current) { optimisticCurrent = current }
    val validOptions = options?.takeIf { it.size == steps }
    val displayedCurrent = optimisticCurrent.coerceIn(0, steps - 1)
    val displayedValue = validOptions?.getOrNull(displayedCurrent) ?: value
    SettingsItem(
        selected = false,
        onClick = {
            if (validOptions != null) showChoices = true
            else onSelect((current + 1) % steps)
        },
        headlineContent = { Text(label) },
        supportingContent = { if (description != null) Text(description) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("◄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(displayedValue)
                Text(if (validOptions != null) "▾" else "►", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = modifier.onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (e.key) {
                Key.DirectionLeft -> {
                    val next = (displayedCurrent - 1 + steps) % steps
                    optimisticCurrent = next
                    onSelect(next)
                    true
                }
                Key.DirectionRight -> {
                    val next = (displayedCurrent + 1) % steps
                    optimisticCurrent = next
                    onSelect(next)
                    true
                }
                else -> false
            }
        },
    )
    if (showChoices && validOptions != null) {
        SettingsChoiceDialog(
            title = label,
            options = validOptions,
            current = displayedCurrent,
            onSelect = { index ->
                optimisticCurrent = index
                onSelect(index)
                showChoices = false
            },
            onDismiss = { showChoices = false },
        )
    }
}

/* --------------- app list: icon + name launches, ⓘ = app info --------------- */

@Composable
private fun AppsScreen(
    apps: List<AppEntry>,
    config: LauncherConfig,
    rememberedFocus: SettingId?,
    onFocus: (SettingId) -> Unit,
    onBack: () -> Unit,
    onLaunch: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onToggleHide: (String) -> Unit,
) {
    val catNames = remember(config.categories) { config.categories.associate { it.id to it.name } }
    val appSettingIds = remember(apps) {
        apps.map { app ->
            DynamicSettingIds.app(
                AppId(
                    packageName = app.pkg,
                    componentName = app.componentName,
                ),
            )
        }
    }
    val listState = rememberLazyListState()
    val requesters = remember(appSettingIds) {
        appSettingIds.associateWith { FocusRequester() }
    }
    // Capture the remembered target only when this Apps screen/app-set enters
    // composition. Normal row-to-row focus updates must not re-run scrollToItem,
    // otherwise D-pad navigation snaps each newly focused row back to the top.
    val restoreTargetId = remember(appSettingIds) {
        rememberedFocus?.takeIf { it in requesters } ?: appSettingIds.firstOrNull()
    }

    LaunchedEffect(appSettingIds, restoreTargetId) {
        val targetId = restoreTargetId ?: return@LaunchedEffect
        val targetIndex = appSettingIds.indexOf(targetId).takeIf { it >= 0 } ?: return@LaunchedEffect
        listState.scrollToItem(targetIndex)
        withFrameNanos { }
        repeat(30) {
            if (runCatching { requesters.getValue(targetId).requestFocus() }.isSuccess) {
                return@LaunchedEffect
            }
            withFrameNanos { }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            stringResource(R.string.item_app),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp, start = 8.dp),
        )
        Text(
            stringResource(R.string.app_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(apps.size, key = { apps[it].pkg }) { i ->
                val app = apps[i]
                val appSettingId = appSettingIds[i]
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        SettingsItem(
                            selected = false,
                            onClick = { onLaunch(app.pkg) },
                            modifier = Modifier
                                .focusRequester(requesters.getValue(appSettingId))
                                .onFocusChanged { state ->
                                    if (state.isFocused) onFocus(appSettingId)
                                },
                            headlineContent = { Text(app.label) },
                            supportingContent = {
                                val names = AppRepository.sectionsOf(app, config)
                                    .mapNotNull { catNames[it] }
                                val suffix = if (app.pkg in config.hidden) " · " + stringResource(R.string.hidden_tag) else ""
                                Text(names.joinToString(" · ").ifEmpty { stringResource(R.string.no_section) } + suffix)
                            },
                            leadingContent = { AppThumb(app) },
                        )
                    }
                    val isHidden = app.pkg in config.hidden
                    SmallIconButton(
                        if (isHidden) AppIcons.Hide else AppIcons.Show,
                        stringResource(if (isHidden) R.string.menu_unhide else R.string.menu_hide),
                    ) { onToggleHide(app.pkg) }
                    SmallIconButton(AppIcons.Info, stringResource(R.string.menu_app_info)) { onAppInfo(app.pkg) }
                    SmallIconButton(AppIcons.Delete, stringResource(R.string.menu_uninstall)) { onUninstall(app.pkg) }
                }
            }
        }
    }
}

/** Small leading artwork for list rows: banner if the app ships one, else icon. */
@Composable
private fun AppThumb(app: AppEntry) {
    when {
        app.banner != null -> Image(
            bitmap = app.banner,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 52.dp, height = 30.dp)
                .clip(RoundedCornerShape(5.dp)),
        )
        app.icon != null -> Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
        else -> Box(
            Modifier
                .size(width = 52.dp, height = 30.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
        )
    }
}

/* ---------- section apps picker: vertical grid, checked = included ---------- */

@Composable
private fun SectionAppsDialog(
    cat: CategoryCfg,
    apps: List<AppEntry>,
    config: LauncherConfig,
    onToggle: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.width(720.dp).height(520.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text(cat.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.section_apps_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                val f = initialFocus()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(apps.size, key = { apps[it].pkg }) { i ->
                        val app = apps[i]
                        val inSection = cat.id in AppRepository.sectionsOf(app, config)
                        Surface(
                            onClick = { onToggle(app) },
                            modifier = if (i == 0) Modifier.focusRequester(f) else Modifier,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.06f),
                                focusedContainerColor = Color.White.copy(alpha = 0.18f),
                            ),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                        ) {
                            Box(Modifier.fillMaxWidth().padding(8.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (app.banner != null) {
                                        Image(
                                            bitmap = app.banner,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(6.dp)),
                                        )
                                    } else {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (app.icon != null) {
                                                Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        app.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                                Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                    CheckMark(checked = inSection)
                                }
                            }
                        }
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}


/* ------------------------------ launcher settings ------------------------------ */

@Composable
private fun LauncherSettingsSubscreen(
    config: LauncherConfig,
    store: ConfigStore,
    rememberedFocus: SettingId?,
    onFocus: (SettingId) -> Unit,
    onBack: () -> Unit,
    onRerunWizard: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var langScreen by remember { mutableStateOf(false) }

    if (langScreen) {
        LanguageScreen(config = config, store = store, onBack = { langScreen = false })
        return
    }

    fun saveConfig() {
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    dir.mkdirs()
                    File(dir, "CouchyBackup.json").writeText(format.encodeToString(LauncherConfig.serializer(), config))
                }
            }
            Actions.toast(context, "Saved to Downloads/CouchyBackup.json")
        }
    }

    val focus = rememberSettingsFocusBindings(
        ids = LauncherSettingIds.all,
        rememberedFocus = rememberedFocus,
        defaultFocus = LauncherSettingIds.Language,
        onFocused = onFocus,
    )

    val loadPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            format.decodeFromString(LauncherConfig.serializer(), String(input.readBytes()))
                        }
                    }.getOrNull()
                }
                if (loaded != null) {
                    store.update { loaded.copy(knownApps = config.knownApps, setupDone = true) }
                    Actions.toast(context, context.getString(R.string.toast_config_loaded))
                } else {
                    Actions.toast(context, context.getString(R.string.toast_config_bad))
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.item_launcher_settings),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
        )
        SettingsItem(
            selected = false,
            onClick = { langScreen = true },
            modifier = focus.modifier(LauncherSettingIds.Language),
            headlineContent = { Text(stringResource(R.string.item_language)) },
            supportingContent = { Text(stringResource(R.string.item_language_sub)) },
            leadingContent = { Icon(AppIcons.Language, contentDescription = null) },
        )
        SettingsItem(
            selected = false,
            onClick = { saveConfig() },
            modifier = focus.modifier(LauncherSettingIds.SaveConfig),
            headlineContent = { Text(stringResource(R.string.item_save_config)) },
            supportingContent = { Text(stringResource(R.string.item_save_config_sub)) },
            leadingContent = { Icon(AppIcons.Save, contentDescription = null) },
        )
        SettingsItem(
            selected = false,
            onClick = { runCatching { loadPicker.launch(arrayOf("application/json", "*/*")) }.onFailure { Actions.toast(context, context.getString(R.string.toast_no_picker)) } },
            modifier = focus.modifier(LauncherSettingIds.LoadConfig),
            headlineContent = { Text(stringResource(R.string.item_load_config)) },
            supportingContent = { Text(stringResource(R.string.item_load_config_sub)) },
            leadingContent = { Icon(AppIcons.Folder, contentDescription = null) },
        )
        SettingsItem(
            selected = false,
            onClick = onRerunWizard,
            modifier = focus.modifier(LauncherSettingIds.RerunWizard),
            headlineContent = { Text(stringResource(R.string.rerun_wizard)) },
            supportingContent = { Text(stringResource(R.string.rerun_wizard_sub)) },
            leadingContent = { Icon(AppIcons.Play, contentDescription = null) },
        )
    }
}

/* ------------------------------ language ------------------------------ */

@Composable
private fun LanguageScreen(
    config: LauncherConfig,
    store: ConfigStore,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.item_language),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
        )
        val f = initialFocus()
        com.conreo.couchytv.data.LANGUAGES.forEachIndexed { i, code ->
            // Endonym: each language named in its own tongue (no per-language strings)
            val label = if (code.isEmpty()) stringResource(R.string.lang_system)
            else java.util.Locale(code).let { it.getDisplayName(it) }
                .replaceFirstChar { it.uppercase() }
            SettingsItem(
                selected = false,
                onClick = {
                    // Just persist it; LauncherApp mirrors it and recreates.
                    if (code != config.language) scope.launch {
                        store.update { it.copy(language = code) }
                    }
                },
                headlineContent = { Text(label) },
                trailingContent = { CheckMark(checked = config.language == code) },
                modifier = if (i == 0) Modifier.focusRequester(f) else Modifier,
            )
        }
    }
}

/* ------------------------------ status bar ------------------------------ */

@Composable
private fun StatusBarScreen(
    config: LauncherConfig,
    apps: List<AppEntry>,
    store: ConfigStore,
    debouncedWriter: DebouncedSettingsWriter,
    rememberedFocus: SettingId?,
    onFocus: (SettingId) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pickVpn by remember { mutableStateOf(false) }
    val vpnLabel = apps.firstOrNull { it.pkg == config.vpnApp }?.label ?: stringResource(R.string.vpn_system)
    val statusFocusIds = remember(config.showVpnButton) {
        buildList {
            add(StatusBarSettingIds.Enabled)
            add(StatusBarSettingIds.Glass)
            add(StatusBarSettingIds.Clock24h)
            add(StatusBarSettingIds.DateFormat)
            add(StatusBarSettingIds.VpnButton)
            if (config.showVpnButton) add(StatusBarSettingIds.VpnApp)
        }
    }
    val focus = rememberSettingsFocusBindings(
        ids = statusFocusIds,
        rememberedFocus = rememberedFocus,
        defaultFocus = StatusBarSettingIds.Enabled,
        onFocused = onFocus,
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.item_statusbar),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
        )
        SettingsItem(
            selected = false,
            onClick = { scope.launch { store.update { it.copy(showStatusBar = !it.showStatusBar) } } },
            modifier = focus.modifier(StatusBarSettingIds.Enabled),
            headlineContent = { Text(stringResource(R.string.show_statusbar)) },
            supportingContent = { Text(stringResource(R.string.show_statusbar_sub)) },
            trailingContent = { CheckMark(checked = config.showStatusBar) },
        )
        SettingsItem(
            selected = false,
            onClick = { scope.launch { store.update { it.copy(statusBarGlass = !it.statusBarGlass) } } },
            modifier = focus.modifier(StatusBarSettingIds.Glass),
            headlineContent = { Text(stringResource(R.string.statusbar_glass)) },
            supportingContent = { Text(stringResource(R.string.statusbar_glass_sub)) },
            trailingContent = { CheckMark(checked = config.statusBarGlass) },
        )
        SettingsItem(
            selected = false,
            onClick = { scope.launch { store.update { it.copy(h24 = !it.h24) } } },
            modifier = focus.modifier(StatusBarSettingIds.Clock24h),
            headlineContent = { Text(stringResource(R.string.clock_24h)) },
            trailingContent = { CheckMark(checked = config.h24) },
        )
        run {
            val idx = config.dateFormat.coerceIn(0, DATE_FORMATS.size - 1)
            val today = java.util.Date()
            val dateOptions = DATE_FORMATS.mapIndexed { optionIndex, pattern ->
                if (optionIndex == 0) stringResource(R.string.off)
                else java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(today)
            }
            SelectorRow(
                modifier = focus.modifier(StatusBarSettingIds.DateFormat),
                label = stringResource(R.string.date_label),
                value = dateOptions[idx],
                options = dateOptions,
                steps = DATE_FORMATS.size,
                current = idx,
                onSelect = { v ->
                    val value = StatusBarSettingDescriptors.DateFormat.requireValid(v)
                    debouncedWriter.submit(StatusBarSettingDescriptors.DateFormat.id) {
                        it.copy(dateFormat = value)
                    }
                },
            )
        }
        SettingsItem(
            selected = false,
            onClick = { scope.launch { store.update { it.copy(showVpnButton = !it.showVpnButton) } } },
            modifier = focus.modifier(StatusBarSettingIds.VpnButton),
            headlineContent = { Text(stringResource(R.string.show_vpn_button)) },
            leadingContent = { Icon(AppIcons.Vpn, contentDescription = null) },
            trailingContent = { CheckMark(checked = config.showVpnButton) },
        )
        // Grayed out (and not focusable) while the VPN button is hidden
        SettingsItem(
            selected = false,
            enabled = config.showVpnButton,
            onClick = { pickVpn = true },
            modifier = if (config.showVpnButton) focus.modifier(StatusBarSettingIds.VpnApp) else Modifier,
            headlineContent = { Text(stringResource(R.string.vpn_opens)) },
            supportingContent = { Text(vpnLabel) },
            leadingContent = { Icon(AppIcons.Vpn, contentDescription = null) },
        )
    }

    if (pickVpn) {
        Dialog(onDismissRequest = { pickVpn = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.width(380.dp).height(480.dp),
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        stringResource(R.string.vpn_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
                    )
                    val fv = initialFocus()
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item {
                            SettingsItem(
                                selected = false,
                                onClick = {
                                    scope.launch { store.update { it.copy(vpnApp = "") } }
                                    pickVpn = false
                                },
                                headlineContent = { Text(stringResource(R.string.vpn_system)) },
                                trailingContent = { CheckMark(checked = config.vpnApp.isEmpty()) },
                                modifier = Modifier.focusRequester(fv),
                            )
                        }
                        items(apps.size, key = { apps[it].pkg }) { i ->
                            val app = apps[i]
                            SettingsItem(
                                selected = false,
                                onClick = {
                                    scope.launch { store.update { it.copy(vpnApp = app.pkg) } }
                                    pickVpn = false
                                },
                                headlineContent = { Text(app.label) },
                                leadingContent = { AppThumb(app) },
                                trailingContent = { CheckMark(checked = config.vpnApp == app.pkg) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------ sections ------------------------------ */

@Composable
private fun CategoriesScreen(
    config: LauncherConfig,
    apps: List<AppEntry>,
    store: ConfigStore,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var renameFor by remember { mutableStateOf<CategoryCfg?>(null) }
    var appsFor by remember { mutableStateOf<CategoryCfg?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stringResource(R.string.item_sections),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
        )
        val f = initialFocus()
        // Auto-filled "All apps" section: a single toggle, no per-app assignment.
        run {
            val hasAll = config.categories.any { it.id == AppRepository.ALL_APPS_ID }
            val allName = stringResource(R.string.cat_all_apps)
            SettingsItem(
                selected = false,
                onClick = {
                    scope.launch {
                        store.update { cfg ->
                            val rest = cfg.categories.filter { it.id != AppRepository.ALL_APPS_ID }
                            cfg.copy(
                                categories = if (hasAll) rest
                                else rest + CategoryCfg(AppRepository.ALL_APPS_ID, allName),
                            )
                        }
                    }
                },
                modifier = Modifier.focusRequester(f),
                headlineContent = { Text(allName) },
                supportingContent = { Text(stringResource(R.string.all_apps_sub)) },
                leadingContent = { Icon(AppIcons.Apps, contentDescription = null) },
                trailingContent = { CheckMark(checked = hasAll) },
            )
        }
        config.categories.forEachIndexed { i, cat ->
            if (cat.id == AppRepository.ALL_APPS_ID) return@forEachIndexed
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    SettingsItem(
                        selected = false,
                        onClick = { appsFor = cat },
                        headlineContent = { Text(cat.name) },
                        supportingContent = { Text(stringResource(R.string.section_row_sub)) },
                        leadingContent = { Icon(AppIcons.Folder, contentDescription = null) },
                    )
                }
                SmallIconButton(AppIcons.Pencil, stringResource(R.string.rename_section)) { renameFor = cat }
                SmallIconButton(AppIcons.Up, stringResource(R.string.cd_move_up)) {
                    if (i > 0) scope.launch {
                        store.update { cfg ->
                            val l = cfg.categories.toMutableList()
                            val tmp = l[i - 1]; l[i - 1] = l[i]; l[i] = tmp
                            cfg.copy(categories = l)
                        }
                    }
                }
                SmallIconButton(AppIcons.Down, stringResource(R.string.cd_move_down)) {
                    if (i < config.categories.size - 1) scope.launch {
                        store.update { cfg ->
                            val l = cfg.categories.toMutableList()
                            val tmp = l[i + 1]; l[i + 1] = l[i]; l[i] = tmp
                            cfg.copy(categories = l)
                        }
                    }
                }
                if (config.categories.size > 1) {
                    SmallIconButton(AppIcons.Delete, stringResource(R.string.cd_delete_section)) {
                        scope.launch {
                            store.update { cfg ->
                                cfg.copy(categories = cfg.categories.filter { it.id != cat.id })
                            }
                        }
                    }
                }
            }
        }
        val newSectionName = stringResource(R.string.new_section)
        SettingsItem(
            selected = false,
            onClick = {
                scope.launch {
                    store.update {
                        it.copy(
                            categories = it.categories +
                                CategoryCfg("c${System.currentTimeMillis()}", newSectionName)
                        )
                    }
                }
            },
            headlineContent = { Text(stringResource(R.string.add_section)) },
            leadingContent = { Icon(AppIcons.Add, contentDescription = null) },
        )
    }

    appsFor?.let { cat ->
        // Keep the dialog's category object in sync with the live config
        val liveCat = config.categories.firstOrNull { it.id == cat.id } ?: cat
        SectionAppsDialog(
            cat = liveCat,
            apps = apps,
            config = config,
            onToggle = { app ->
                scope.launch {
                    store.update { cfg ->
                        val current = AppRepository.sectionsOf(app, cfg)
                        val next = if (liveCat.id in current) current - liveCat.id
                        else current + liveCat.id
                        cfg.copy(sections = cfg.sections + (app.pkg to next))
                    }
                }
            },
            onDismiss = { appsFor = null },
        )
    }

    renameFor?.let { cat ->
        RenameDialog(
            initial = cat.name,
            onSave = { newName ->
                scope.launch {
                    store.update { cfg ->
                        cfg.copy(categories = cfg.categories.map {
                            if (it.id == cat.id) it.copy(name = newName) else it
                        })
                    }
                }
                renameFor = null
            },
            onDismiss = { renameFor = null },
        )
    }
}

/** Pencil dialog: the ONLY place with a text field, so D-pad browsing never lands in an editor. */
@Composable
private fun RenameDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.width(340.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.rename_section), style = MaterialTheme.typography.titleMedium)
                val f = initialFocus()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { if (name.isNotBlank()) onSave(name.trim()) }
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { if (name.isNotBlank()) onSave(name.trim()) }) { Text(stringResource(R.string.save)) }
                    Button(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
}

/* ------------------------------ wallpaper ------------------------------ */

/**
 * ONE row per background mode, checkmark = active. Selecting any mode turns
 * the others off. Two groups: Static (color, photo) and Video (aerials, video).
 */
@Composable
private fun WallpaperScreen(
    config: LauncherConfig,
    rememberedFocus: SettingId?,
    onFocus: (SettingId) -> Unit,
    onSelectBuiltinAerials: () -> Unit,
    onSelectBuiltinSource: (Int) -> Unit,
    onSetScrim: (Int) -> Unit,
    onSetSpeed: (Int) -> Unit,
    onPreset: (Int) -> Unit,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.item_wallpaper),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
        )
        val staticActive = !config.useBuiltinAerials &&
            !config.useVideoWallpaper && !config.useCustomWallpaper
        val wallpaperFocusIds = remember(staticActive, config.useBuiltinAerials) {
            buildList {
                add(WallpaperSettingIds.Scrim)
                add(WallpaperSettingIds.GradientMode)
                if (staticActive) add(WallpaperSettingIds.GradientPreset)
                add(WallpaperSettingIds.CustomPhoto)
                add(WallpaperSettingIds.BuiltinAerials)
                if (config.useBuiltinAerials) {
                    add(WallpaperSettingIds.AerialApple)
                    add(WallpaperSettingIds.AerialAmazon)
                    add(WallpaperSettingIds.AerialCommunity1)
                    add(WallpaperSettingIds.AerialCommunity2)
                }
                add(WallpaperSettingIds.LocalVideo)
                add(WallpaperSettingIds.VideoSpeed)
            }
        }
        val focus = rememberSettingsFocusBindings(
            ids = wallpaperFocusIds,
            rememberedFocus = rememberedFocus,
            defaultFocus = WallpaperSettingIds.Scrim,
            onFocused = onFocus,
        )

        // Dimming — how much the wallpaper is darkened for icon readability.
        // Applies to any background; "Top & bottom" keeps the centre clear.
        run {
            val names = listOf(
                stringResource(R.string.dim_top_bottom),
                stringResource(R.string.dim_top),
                stringResource(R.string.dim_bottom),
                stringResource(R.string.dim_full),
                stringResource(R.string.dim_off),
            )
            val idx = config.scrimMode.coerceIn(0, names.size - 1)
            SelectorRow(
                modifier = focus.modifier(WallpaperSettingIds.Scrim),
                label = stringResource(R.string.dim_label),
                value = names[idx],
                options = names,
                steps = names.size,
                current = idx,
                onSelect = { v -> onSetScrim(v) },
            )
        }

        /* ---------------- STATIC: color gradient + your own photo ---------------- */
        SectionLabel(stringResource(R.string.wp_section_static))

        // Color gradient mode — checkmark like every other mode; the picker
        // below appears once it's active (mirrors the aerial collection row).
        val presetIdx = config.wallpaper.coerceIn(0, WALLPAPERS.size - 1)
        SettingsItem(
            selected = false, onClick = { onPreset(presetIdx) },
            modifier = focus.modifier(WallpaperSettingIds.GradientMode),
            headlineContent = { Text(stringResource(R.string.color_gradient)) },
            leadingContent = { Icon(AppIcons.Palette, contentDescription = null) },
            trailingContent = { CheckMark(checked = staticActive) },
        )
        if (staticActive) {
            val presetNames = listOf(
                stringResource(R.string.wp_midnight),
                stringResource(R.string.wp_aurora),
                stringResource(R.string.wp_sunset),
                stringResource(R.string.wp_deep),
                stringResource(R.string.wp_charcoal),
            )
            SelectorRow(
                modifier = focus.modifier(WallpaperSettingIds.GradientPreset),
                label = stringResource(R.string.preset_label),
                value = presetNames[presetIdx],
                options = presetNames,
                steps = WALLPAPERS.size,
                current = presetIdx,
                onSelect = { v -> onPreset(v) },
            )
        }

        // A photo of your own
        SettingsItem(
            selected = false, onClick = onPickPhoto,
            modifier = focus.modifier(WallpaperSettingIds.CustomPhoto),
            headlineContent = { Text(stringResource(R.string.pick_photo)) },
            supportingContent = { Text(stringResource(R.string.pick_photo_sub)) },
            leadingContent = { Icon(AppIcons.Image, contentDescription = null) },
            trailingContent = { CheckMark(checked = config.useCustomWallpaper) },
        )

        /* ---------------- VIDEO: built-in aerials + your own video ---------------- */
        SectionLabel(stringResource(R.string.wp_section_video))

        // Built-in aerial videos
        SettingsItem(
            selected = false, onClick = onSelectBuiltinAerials,
            modifier = focus.modifier(WallpaperSettingIds.BuiltinAerials),
            headlineContent = { Text(stringResource(R.string.builtin_aerials)) },
            supportingContent = { Text(stringResource(R.string.builtin_aerials_sub)) },
            leadingContent = { Icon(AppIcons.Play, contentDescription = null) },
            trailingContent = { CheckMark(checked = config.useBuiltinAerials) },
        )
        if (config.useBuiltinAerials) {
            Text(
                "AERIAL COLLECTIONS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
            )
            SettingsItem(
                selected = false,
                onClick = { onSelectBuiltinSource(1) },
                modifier = focus.modifier(WallpaperSettingIds.AerialApple),
                headlineContent = { Text(stringResource(R.string.aerial_src_apple)) },
                trailingContent = { CheckMark(checked = "A" in config.builtinSources) },
            )
            SettingsItem(
                selected = false,
                onClick = { onSelectBuiltinSource(2) },
                modifier = focus.modifier(WallpaperSettingIds.AerialAmazon),
                headlineContent = { Text(stringResource(R.string.aerial_src_amazon)) },
                trailingContent = { CheckMark(checked = "Z" in config.builtinSources) },
            )
            SettingsItem(
                selected = false,
                onClick = { onSelectBuiltinSource(3) },
                modifier = focus.modifier(WallpaperSettingIds.AerialCommunity1),
                headlineContent = { Text(stringResource(R.string.aerial_src_comm1)) },
                trailingContent = { CheckMark(checked = "C1" in config.builtinSources) },
            )
            SettingsItem(
                selected = false,
                onClick = { onSelectBuiltinSource(4) },
                modifier = focus.modifier(WallpaperSettingIds.AerialCommunity2),
                headlineContent = { Text(stringResource(R.string.aerial_src_comm2)) },
                trailingContent = { CheckMark(checked = "C2" in config.builtinSources) },
            )
        }

        // A video file of your own
        SettingsItem(
            selected = false, onClick = onPickVideo,
            modifier = focus.modifier(WallpaperSettingIds.LocalVideo),
            headlineContent = { Text(stringResource(R.string.pick_video)) },
            supportingContent = { Text(stringResource(R.string.pick_video_sub)) },
            leadingContent = { Icon(AppIcons.Image, contentDescription = null) },
            trailingContent = { CheckMark(checked = config.useVideoWallpaper) },
        )

        // Playback speed — applies to aerials and your own video.
        run {
            val labels = listOf("0.25×", "0.5×", "0.75×", "1×")
            val idx = config.videoSpeed.coerceIn(0, labels.size - 1)
            SelectorRow(
                modifier = focus.modifier(WallpaperSettingIds.VideoSpeed),
                label = stringResource(R.string.speed_label),
                value = labels[idx],
                options = labels,
                steps = labels.size,
                current = idx,
                onSelect = { v -> onSetSpeed(v) },
            )
        }
    }
}

/* ------------------------------ widgets ------------------------------ */

/**
 * Every menu focuses its first item on entry (Back on the remote steps out,
 * so there are no on-screen Back buttons).
 */
@Composable
private fun initialFocus(): FocusRequester {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Claim focus the moment the first item's node is attached (retry per
        // frame, stop on the first success) instead of after a fixed 300ms delay.
        // A late request would yank focus back to item 1 if the user had already
        // navigated down — the "quick navigation resets to the top" bug.
        repeat(30) {
            if (runCatching { fr.requestFocus() }.isSuccess) return@LaunchedEffect
            withFrameNanos {}
        }
    }
    return fr
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, start = 8.dp, bottom = 4.dp),
    )
}

@Composable
internal fun CheckMark(checked: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .background(
                if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(5.dp),
            )
            .border(
                width = 2.dp,
                color = if (checked) MaterialTheme.colorScheme.primary
                else Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(5.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                AppIcons.Check,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * A settings row with NO focus scale. tv-material's default focusedScale
 * scales the whole row — including its already-rasterized text — which looks
 * blurry on TV; the focused container colour alone signals selection.
 */
@Composable
internal fun SettingsItem(
    onClick: () -> Unit,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    ListItem(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .padding(horizontal = 8.dp)
            .background(
                if (focused) Color(0xFFF1F2F4) else Color.Transparent,
                RoundedCornerShape(12.dp)
            ),
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = androidx.tv.material3.ListItemDefaults.colors(
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color(0xFF14151A),
        ),
    )
}

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = Color.White.copy(alpha = 0.25f),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            // label is the accessibility name announced by TalkBack.
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        }
    }
}
