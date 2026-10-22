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

    // Release signing key, supplied by CI via environment variables (from GitHub
    // secrets). Absent locally and on F-Droid, so this config stays inert there.
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
            // Signing priority:
            //  • CI (KEYSTORE_FILE env set)  → your real release key, from secrets.
            //  • -PlocalSign                 → the auto-generated debug key (local test).
            //  • otherwise (incl. F-Droid)   → UNSIGNED; F-Droid signs with its own key.
            signingConfig = when {
                ciKeystore != null -> signingConfigs.getByName("ci")
                project.hasProperty("localSign") -> signingConfigs.getByName("debug")
                else -> null
            }
        }
    }

    // Reproducibility for F-Droid: don't embed the Google-signed dependency
    // metadata block in the artifact.
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
 * Aerial Rows keeps Couchy's large upstream LauncherScreen/SettingsSheet files
 * intact in Git history while applying a tiny, idempotent integration patch
 * before Android compiles them. This makes upstream rebases much easier: the
 * custom SlidingRows renderer lives in its own file and these replacements are
 * deliberately narrow and fail loudly if Couchy's surrounding code changes.
 */
val applyAerialRowsSourcePatches = tasks.register("applyAerialRowsSourcePatches") {
    doLast {
        val settings = file("src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt")
        var s = settings.readText()
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
            s = s.replace("steps = 3,\n            current = config.layout.coerceIn(0, 2),", "steps = 4,\n            current = config.layout.coerceIn(0, 3),")
            check(s.contains("mode_sliding_rows")) { "Aerial Rows: failed to patch SettingsSheet view-mode selector" }
            settings.writeText(s)
        }

        val launcher = file("src/main/java/com/conreo/couchytv/ui/LauncherScreen.kt")
        var l = launcher.readText()
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
            launcher.writeText(l)
        }

        val strings = file("src/main/res/values/strings.xml")
        var x = strings.readText()
        if (!x.contains("name=\"mode_sliding_rows\"")) {
            val anchor = "    <string name=\"mode_dock_desc\">Floating dock at the bottom — Down expands over a blurred wallpaper</string>"
            val extra = anchor + "\n    <string name=\"mode_sliding_rows\">Sliding Rows</string>\n    <string name=\"mode_sliding_rows_desc\">Projectivy-style horizontal rows — selection moves naturally and Left reveals the navigation pane</string>"
            check(x.contains(anchor)) { "Aerial Rows: strings.xml layout anchor was not found" }
            x = x.replace(anchor, extra)
            strings.writeText(x)
        }
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
    // iOS-style continuous (squircle) corners — perceptually smoother than the
    // circular-arc RoundedCornerShape.
    implementation("androidx.graphics:graphics-shapes:1.0.1")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.annotation:annotation:1.8.2")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Applies the bundled baseline profile (src/main/baseline-prof.txt) so the
    // startup + scroll paths are AOT-compiled on first run instead of JIT'd —
    // the big cold-start "screen shows but not smooth yet" win.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    // Video wallpaper playback — plays Apple .mov aerials that MediaPlayer can't
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    // OkHttp data source: lets us stream aerials through a trust-all TLS client
    // so Apple's sylvan.apple.com (cert chain many TV trust stores reject) works.
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1")
}
