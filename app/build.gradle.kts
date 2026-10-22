plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.conreo.couchytv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.conreo.couchytv"
        minSdk = 21
        targetSdk = 34
        versionCode = 7
        versionName = "1.0.6"
    }

    val ciKeystore = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (ciKeystore != null) {
            create("ci") {
                storeFile = file(ciKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = when {
                ciKeystore != null -> signingConfigs.getByName("ci")
                project.hasProperty("localSign") -> signingConfigs.getByName("debug")
                else -> null
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

/*
 * Aerial Rows keeps Couchy's two large upstream UI files close to upstream and
 * applies narrow, idempotent integration hooks before compilation. Each feature
 * has its own guard so an already-built local checkout can receive newer hooks.
 */
val applyAerialRowsSourcePatches = tasks.register("applyAerialRowsSourcePatches") {
    doLast {
        val settings = file("src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt")
        var s = settings.readText()

        // 1) Fourth Display > Layout > View mode: Sliding Rows.
        if (!s.contains("LAYOUT_SLIDING_ROWS")) {
            s = s.replace(
                "import com.conreo.couchytv.data.LAYOUT_GRID\n",
                "import com.conreo.couchytv.data.LAYOUT_GRID\nimport com.conreo.couchytv.data.LAYOUT_SLIDING_ROWS\n",
            )
            s = s.replace(
                "value = listOf(stringResource(R.string.mode_carousel), stringResource(R.string.mode_grid), stringResource(R.string.mode_dock))[config.layout.coerceIn(0, 2)],",
                "value = listOf(stringResource(R.string.mode_carousel), stringResource(R.string.mode_grid), stringResource(R.string.mode_dock), stringResource(R.string.mode_sliding_rows))[config.layout.coerceIn(0, 3)],",
            )
            s = s.replace(
                "LAYOUT_DOCK -> stringResource(R.string.mode_dock_desc)\n                else -> stringResource(R.string.mode_carousel_desc)",
                "LAYOUT_DOCK -> stringResource(R.string.mode_dock_desc)\n                LAYOUT_SLIDING_ROWS -> stringResource(R.string.mode_sliding_rows_desc)\n                else -> stringResource(R.string.mode_carousel_desc)",
            )
            s = s.replace(
                "steps = 3,\n            current = config.layout.coerceIn(0, 2),",
                "steps = 4,\n            current = config.layout.coerceIn(0, 3),",
            )
            check(s.contains("mode_sliding_rows")) { "Aerial Rows: failed to patch SettingsSheet view-mode selector" }
        }

        // 2) Content > Channels settings page.
        if (!s.contains("SettingsScreen.Channels")) {
            s = s.replace(
                "private enum class SettingsScreen { Main, Wallpaper, Display, StatusBar, Apps, Categories, Launcher, About }",
                "private enum class SettingsScreen { Main, Wallpaper, Display, StatusBar, Apps, Channels, Categories, Launcher, About }",
            )
            s = s.replace(
                "                        onApps = { screen = SettingsScreen.Apps },\n                        onCategories = { screen = SettingsScreen.Categories },",
                "                        onApps = { screen = SettingsScreen.Apps },\n                        onChannels = { screen = SettingsScreen.Channels },\n                        onCategories = { screen = SettingsScreen.Categories },",
            )
            s = s.replace(
                "                    SettingsScreen.Categories -> CategoriesScreen(",
                "                    SettingsScreen.Channels -> ChannelsSettingsScreen(\n                        config = config,\n                        store = store,\n                    )\n                    SettingsScreen.Categories -> CategoriesScreen(",
            )
            s = s.replace(
                "    onApps: () -> Unit,\n    onCategories: () -> Unit,",
                "    onApps: () -> Unit,\n    onChannels: () -> Unit,\n    onCategories: () -> Unit,",
            )
            val sectionsItem = """        SettingsItem(
            selected = false, onClick = onCategories,
            headlineContent = { Text(stringResource(R.string.item_sections)) },
            supportingContent = { Text(stringResource(R.string.item_sections_sub)) },
            leadingContent = { Icon(AppIcons.Folder, contentDescription = null) },
        )"""
            val channelsItem = sectionsItem + """
        SettingsItem(
            selected = false, onClick = onChannels,
            headlineContent = { Text("Channels") },
            supportingContent = { Text("Watch Next and Android TV app channels") },
            leadingContent = { Icon(AppIcons.Play, contentDescription = null) },
        )"""
            check(s.contains(sectionsItem)) { "Aerial Rows: Settings main-menu section hook not found" }
            s = s.replace(sectionsItem, channelsItem)
            check(s.contains("ChannelsSettingsScreen")) { "Aerial Rows: channel settings hook failed" }
        }

        // 3) Display > Row & Dock Background controls.
        if (!s.contains("PanelAppearanceSettings(config = config, store = store)")) {
            val spacingBlock = """        SelectorRow(
            label = stringResource(R.string.spacing),
            value = listOf(stringResource(R.string.size_xs), stringResource(R.string.size_s), stringResource(R.string.size_n), stringResource(R.string.size_l), stringResource(R.string.size_xl))[config.spacing.coerceIn(0, 4)],
            steps = 5,
            current = config.spacing.coerceIn(0, 4),
            onSelect = { v -> update { it.copy(spacing = v) } },
        )"""
            check(s.contains(spacingBlock)) { "Aerial Rows: Display spacing hook not found" }
            s = s.replace(
                spacingBlock,
                spacingBlock + "\n        PanelAppearanceSettings(config = config, store = store)",
            )
        }

        // 4) Aerial Rows uses only Apple + Amazon. Remove Couchy's community
        // collections from the selector as well as the runtime loader.
        if (s.contains("R.string.aerial_src_comm1") || s.contains("R.string.aerial_src_comm2")) {
            val oldSources = """            val sourceNames = listOf(
                stringResource(R.string.aerial_src_all),
                stringResource(R.string.aerial_src_apple),
                stringResource(R.string.aerial_src_amazon),
                stringResource(R.string.aerial_src_comm1),
                stringResource(R.string.aerial_src_comm2),
            )"""
            val newSources = """            val sourceNames = listOf(
                stringResource(R.string.aerial_src_all),
                stringResource(R.string.aerial_src_apple),
                stringResource(R.string.aerial_src_amazon),
            )"""
            check(s.contains(oldSources)) { "Aerial Rows: aerial source selector hook not found" }
            s = s.replace(oldSources, newSources)
            s = s.replace(
                "val idx = config.builtinSource.coerceIn(0, sourceNames.size - 1)",
                "val idx = if (config.builtinSource in sourceNames.indices) config.builtinSource else 0",
            )
        }
        settings.writeText(s)

        val launcher = file("src/main/java/com/conreo/couchytv/ui/LauncherScreen.kt")
        var l = launcher.readText()

        // 5) Actual Sliding Rows renderer selection.
        if (!l.contains("LAYOUT_SLIDING_ROWS")) {
            l = l.replace(
                "import com.conreo.couchytv.data.LAYOUT_GRID\n",
                "import com.conreo.couchytv.data.LAYOUT_GRID\nimport com.conreo.couchytv.data.LAYOUT_SLIDING_ROWS\n",
            )
            val hook = """                when (config.layout) {
                    LAYOUT_DOCK -> DockArea("""
            val replacement = """                when (config.layout) {
                    LAYOUT_SLIDING_ROWS -> SlidingRowsArea(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        categorized = categorized,
                        config = config,
                        accent = accent,
                        movePkg = movePkg,
                        moveFocus = moveFocus,
                        cardWidth = cardWidth,
                        gap = gap,
                        onLaunch = { app -> if (movePkg == null) Actions.launchApp(context, app.pkg) },
                        onMenu = { app -> if (movePkg == null) menuFor = app },
                        onSettings = { showSettings = true },
                        onAndroidSettings = { Actions.openSystemSettings(context) },
                    )
                    LAYOUT_DOCK -> DockArea("""
            check(l.contains(hook)) { "Aerial Rows: LauncherScreen layout hook was not found" }
            l = l.replace(hook, replacement)
        }

        // 6) Make Couchy's Dock use the same Clear / Solid / Liquid Glass panel.
        if (!l.contains("// Aerial Rows customizable dock panel")) {
            val oldDockPanel = """            Box(
                Modifier
                    .fillMaxSize()
                    .clip(SmoothCornerShape(LocalCornerRadius.current + 14.dp))
                    .background(Color(0xB3121418))
                    .background(Color.White.copy(alpha = 0.06f))
            )"""
            val newDockPanel = """            // Aerial Rows customizable dock panel
            AerialPanel(
                config = config,
                shape = SmoothCornerShape(LocalCornerRadius.current + 14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {}"""
            check(l.contains(oldDockPanel)) { "Aerial Rows: Dock background hook not found" }
            l = l.replace(oldDockPanel, newDockPanel)
        }
        launcher.writeText(l)

        val strings = file("src/main/res/values/strings.xml")
        var x = strings.readText()
        if (!x.contains("name=\"mode_sliding_rows\"")) {
            val anchor = "    <string name=\"mode_dock_desc\">Floating dock at the bottom — Down expands over a blurred wallpaper</string>"
            val extra = anchor + "\n    <string name=\"mode_sliding_rows\">Sliding Rows</string>\n    <string name=\"mode_sliding_rows_desc\">Projectivy-style horizontal rows — selection moves naturally and Left reveals the navigation pane</string>"
            check(x.contains(anchor)) { "Aerial Rows: strings.xml layout anchor was not found" }
            x = x.replace(anchor, extra)
        }
        strings.writeText(x)
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(applyAerialRowsSourcePatches)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("androidx.graphics:graphics-shapes:1.0.1")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.annotation:annotation:1.8.2")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1")
}
