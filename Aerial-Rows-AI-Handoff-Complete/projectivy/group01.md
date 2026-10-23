# Projectivy 4.72 reverse engineering — Group 01: package/component/resource architecture

Status: **complete — reviewed and strengthened**
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
Analysis stack: REA 6.7.0 + bundled Node 22.20.0 + Java 21.0.12.1 + JADX MCP 0.7.1

## Purpose

This group establishes the static outer architecture before tracing feature implementations. It intentionally records declarations, entry points, resources and class/member inventories rather than copying decompiled method bodies. Later groups follow these named entry points into the obfuscated class graph.

## Package identity

- Package: `com.spocky.projengmenu`
- Version: `4.72`
- versionCode: `97`
- minSdk: `23`
- targetSdk: `37`
- JADX inventory: **7,395 classes**
- JADX resources: **1,171 files**
- Main application class: `com.spocky.projengmenu.PTApplication`
- Main HOME/launcher activity: `com.spocky.projengmenu.ui.home.MainActivity`
- Classes in the main `com.spocky.projengmenu` namespace: **90**

The small number of classes retaining the main Projectivy package name compared with the total class count confirms heavy shrinking/minification, but the total APK class count must **not** be interpreted as “Projectivy classes”. The APK also bundles AndroidX, Google, desugared Java (`j$.*`) and other third-party implementation code, and many classes are collapsed into `defpackage.*`. Therefore each obfuscated class must be ownership-classified from inheritance, callers, resources and behavior before being treated as Projectivy-specific logic. Named Projectivy classes are stable tracing anchors, not the full architecture.

## Application-level manifest facts

The application uses `@style/AppTheme`, hardware acceleration, RTL support, TV banner/icon resources, internal-only install location, `allowBackup=true`, `resizeableActivity=true`, `usesCleartextTraffic=true`, and `@xml/network_security_config`.

Declared platform requirements include Leanback as required and touchscreen/Wi-Fi as optional.

## Component graph

Manifest totals:

- 59 activities
- 13 services
- 16 receivers
- 4 providers

Projectivy-owned components:

- **55 activities**
- **2 services**
- **4 receivers**
- **1 provider**

### Primary launcher/home

`MainActivity` is exported and enabled and declares one MAIN intent filter containing:

- `android.intent.category.HOME`
- `android.intent.category.LAUNCHER`
- `android.intent.category.LEANBACK_LAUNCHER`
- `android.intent.category.LAUNCHER_APP`
- `android.intent.category.MONKEY`
- `fireos.intent.category.MAIN_HOME`

This is a direct Android HOME launcher implementation with Fire OS compatibility declared at the manifest boundary.

`res/layout/activity_main.xml` is a small shell containing:

- a full-screen `main_overlay` View
- a `FragmentContainerView`
- `com.spocky.projengmenu.ui.home.MainFragment`

Therefore the launcher activity delegates most home composition to `MainFragment` and related Leanback components rather than directly building rows in the activity.

### Settings / side panels / external actions

Externally reachable Projectivy actions include:

- `tv.projectivy.SETTINGS` → `SettingsActivity`
- `tv.projectivy.NOTIFICATIONS` → `NotificationsSidePanelActivity`
- `tv.projectivy.ALL_APPS` and Android `ALL_APPS` → `AllAppsSidePanelActivity`
- `APPLICATION_PREFERENCES` → `AppSettingsActivity`
- `tv.projectivy.APPLY_ICONPACK` / `com.spocky.projengmenu.APPLY_ICONPACK` → `ApplyIconPackActivity`
- `CONFIRM_PIN_SHORTCUT` → `PinShortcutActivity`
- VIEW/SEND image/video intents → `SetBackgroundActivity`
- `.plbackup` VIEW intents → `ImportSettingsActivity`
- `.pltheme` VIEW intents → `ImportThemeActivity`

This confirms that several Projectivy subsystems are deliberately exposed as reusable launcher actions rather than existing only inside one settings screen.

### Input/source trampoline activities

The APK declares launcher-visible but initially disabled activities for:

- HDMI 1
- HDMI 2
- HDMI 3
- HDMI 4
- AV
- Source popup
- Media Explorer

There is also a non-launcher `InternalTvActivity`.

These component toggles strongly support a design where Projectivy enables source shortcut activities only when a compatible source/provider is detected.

### Device/engineering activity families

Named guided-action activities cover:

- device settings adapters: Android, Xiaomi/MiTV, FengOS, Amlogic, MediaTek
- engineering menus: Fengmi/FengOS
- display tools: display profiles, input lag, zoom, 4K window-manager behaviour
- system/property overrides
- accessibility setup
- install/remove/cleanup utilities
- Google Smart Home, profile picker and media-explorer shortcuts
- about/premium/device/background pages

These will be treated as independent adapters/tools rather than launcher-core logic in later groups.

## Services

### `ProjectivyAccessibilityService`

Manifest:
- exported
- protected by `BIND_ACCESSIBILITY_SERVICE`
- enabled
- uses `res/xml/accessibilityservice.xml`

The accessibility config explicitly declares:
- `canRetrieveWindowContent=true`
- `canRequestFilterKeyEvents=true`

The class has 21 methods and 54 fields, including:
- `onAccessibilityEvent`
- `onKeyEvent`
- `onServiceConnected`
- `onInterrupt`
- lifecycle methods

This is a substantial subsystem, not a minimal HOME override shim. Group 9 will trace it separately.

### `NotificationListener`

Manifest:
- exported
- protected by `BIND_NOTIFICATION_LISTENER_SERVICE`

The class exposes notification listener lifecycle, posted/removed/ranking callbacks and both `getActiveNotifications()` overloads. This is a core anchor for notifications/Now Playing analysis in Group 5.

## Broadcast receivers

Projectivy owns:

- `com.spocky.projengmenu.services.StartUpBootReceiver` — `BOOT_COMPLETED`
- `com.spocky.projengmenu.services.LocaleChangedReceiver` — `LOCALE_CHANGED`
- `com.spocky.projengmenu.services.StorageBroadcastReceiver` — mounted/unmounted/removed/bad-removal plus USB attach/detach
- `com.spocky.projengmenu.receivers.WallpaperProviderUpdateReceiver` — `tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED`

The wallpaper update action confirms an external wallpaper-provider plugin contract exists at the manifest level.

## Content providers and file sharing

Projectivy provider:
- `ThemeContentProvider`
- authority `com.spocky.projengmenu.themeprovider`
- not exported
- grants URI permissions

It implements `openAssetFile`, `query`, `getType`, `insert`, `update`, and `delete`, so theme handling is backed by a real content-provider abstraction rather than only reading theme files directly.

The AndroidX `FileProvider` authority is `com.spocky.projengmenu.provider`. `provider_paths.xml` permits:
- external root
- `external-files/backups/`
- cache files

This is relevant to backup/theme/background import/export tracing.

## Package visibility queries

The manifest explicitly queries HOME, LAUNCHER and LEANBACK launch targets, icon-pack picker actions, Xiaomi screen-scale, Google TV Smart Home and:

`tv.projectivy.plugin.WALLPAPER_PROVIDER`

It also names packages associated with Play Store, Aptoide/Downloader, Xiaomi/Formovie/FengOS/Amlogic tools, projector motor service and media explorers.

This indicates device/vendor integration is designed around runtime package/intent discovery with explicit package visibility declarations.

## Permissions with architectural significance

Important permissions include:

- `RECEIVE_BOOT_COMPLETED`
- `REQUEST_INSTALL_PACKAGES`
- `REQUEST_DELETE_PACKAGES`
- `READ_TV_LISTINGS`
- `com.android.providers.tv.permission.READ_EPG_DATA`
- `com.android.providers.tv.permission.WRITE_EPG_DATA`
- media/image/video read permissions
- `INSTALL_SHORTCUT`
- `WAKE_LOCK`
- `FOREGROUND_SERVICE`
- Internet/network state
- Billing / Play install-referrer / ads attribution permissions

TV Provider and Watch Next/channel work therefore has first-class manifest support and is not merely a UI imitation.

## Resource architecture

Resource families among the 1,171 APK files include:

- 127 layouts
- 20 XML configuration files
- 39 raw assets
- 352 base drawables
- 184 colors
- 40 animations
- 39 animators
- 13 transitions
- 5 fonts

High-value Projectivy-specific layout/XML anchors include:

- `activity_main.xml`
- `row_header.xml`
- `hover_card_view.xml`
- `custom_browse_title.xml`
- `custom_browse_title_view.xml`
- `activity_parental_control.xml`
- `activity_color_picker.xml`
- `activity_image_picker.xml`
- `settings_main.xml`
- `settings_category.xml`
- `settings_category_apps.xml`
- `settings_category_content.xml`
- `settings_category_advanced.xml`
- `settings_channels.xml`
- `settings_notifications.xml`
- `settings_program_details.xml`
- `accessibilityservice.xml`
- `shortcuts.xml`
- `provider_paths.xml`

`row_header.xml` uses `IconHeaderImageView` plus Leanback `RowHeaderView`, confirming row headers have a dedicated icon+title representation.

`hover_card_view.xml` has optional artwork, title and up-to-three-line description. This provides a concrete resource-level anchor for selected-card metadata/hover-detail behaviour.

`custom_browse_title_view.xml` includes status-bar controls for wallpaper view, VPN/network, notifications, settings and battery plus `ClockView`. The top/title/status area is therefore a distinct UI subsystem rather than incidental row content.

## Settings resource model discovered in Group 1

The category settings XML already exposes a significant presentation model:

- editable category title
- change icon
- visibility
- multiline/grid mode
- background colour
- sliding single row
- card title visibility
- size/scale
- sort/order
- advanced category settings

Advanced category settings include:
- items per line
- max rows
- start margin
- end margin
- top margin

`settings_category_apps.xml` separates:
- TV apps
- mobile apps
- channels

This is an important design signal for Group 2/3/4: Projectivy's category/row model is not just a list of installed apps; it is a configurable presentation container that can incorporate channel content.

## Built-in raw functionality

The APK contains built-in `.pltheme` files and a large calibration/test-video set including SDR, HDR and Dolby Vision patterns. This independently confirms theme import/export and display-calibration tooling as real subsystems.

## Native code

The APK ships `libptutils.so` for arm64-v8a, armeabi-v7a, x86 and x86_64, alongside GIF decoder and DataStore shared-counter libraries.

`libptutils.so` is Projectivy-specific and must be treated as a separate native boundary. It is not required to understand the Java/Kotlin launcher structure, but later device/system groups should inventory its exports and callers before deciding whether any Aerial-Rows feature needs an equivalent.

## Important library/platform signals

Manifest components show use of:

- AndroidX Leanback
- AndroidX Preferences
- AndroidX WorkManager
- AndroidX Room
- AndroidX DataStore shared counter native support
- Media3
- Google Play Billing
- Firebase analytics/performance/crash/session/remote-config components
- Material components

Firebase collection flags are explicitly false in the base manifest for analytics, performance and crashlytics.

## Entry-point member inventories

- `PTApplication`: 17 methods / 13 fields; owns application initialization and SharedPreferences interception/access.
- `MainActivity`: 25 methods / 25 fields; explicit key/touch dispatch, HOME lifecycle, main layout/overlay management and user-interaction handling.
- `SettingsActivity`: small two-method named shell (`onKeyDown`, `onKeyUp`), implying settings implementation lives in base/fragment/obfuscated collaborators.
- `ProjectivyAccessibilityService`: 21 methods / 54 fields.
- `NotificationListener`: 18 methods / 15 fields.
- `ThemeContentProvider`: 9 methods / 3 fields.
- Boot/locale/storage/wallpaper receivers are small named entry points, ideal for xref tracing.

## Group 1 review additions (second pass)

A second pass was performed against the exact APK to strengthen the architectural baseline before proceeding to home-screen tracing.

### APK packaging / multidex

The APK is a **three-DEX multidex build**:

- `classes.dex`
- `classes2.dex`
- `classes3.dex`

It also contains ART baseline-profile assets (`assets/dexopt/baseline.prof`, `baseline.profm`, and `baseline.bin`). This matters operationally because implementation classes can be distributed across multiple DEX files; no later class inventory or xref pass should assume a single-DEX package.

### Application/runtime flags worth preserving as evidence

Additional manifest details with architectural or lifecycle relevance:

- `installLocation=internalOnly`
- `extractNativeLibs=false`
- `allowBackup=true`
- `backupInForeground=true`
- `hardwareAccelerated=true`
- `resizeableActivity=true`
- `usesCleartextTraffic=true`
- `networkSecurityConfig=@xml/network_security_config`
- `localeConfig=@xml/locale_config`
- `<profileable android:shell="true"/>`
- large/xlarge screens supported; Leanback required; touchscreen and Wi-Fi optional

The absence of a manifest `fullBackupContent` or `dataExtractionRules` attribute is also recorded; later backup analysis must therefore distinguish Projectivy's own `.plbackup` mechanism from Android platform backup.

### MainActivity task/lifecycle posture

The HOME activity has several non-default task flags that are relevant to faithfully reproducing launcher behaviour:

- `taskAffinity=""`
- `launchMode=singleTask`
- `clearTaskOnLaunch=true`
- `stateNotNeeded=true`
- `excludeFromRecents=true`
- `autoRemoveFromRecents=true`
- `resumeWhilePausing=true`

These are now part of the Group 1 baseline and should be checked when implementing the Aerial-Rows HOME activity rather than copied blindly.

### Exported surface / permission boundary

The Projectivy-owned exported surface includes the HOME activity, settings/all-apps/notifications actions, background/theme/settings import activities, icon-pack application, shortcut confirmation, source shortcut activities, accessibility service, notification listener, boot/locale/storage/wallpaper receivers and the application-preferences entry point. Most remaining Projectivy activities have no intent filters and are internal/non-exported by default.

Two custom permissions are particularly important:

- `tv.projectivy.plugin.permission.SEND_WALLPAPER_UPDATE` — protection level `normal`
- `com.spocky.projengmenu.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — protection level `signature`, also requested by the app

The wallpaper-update receiver itself is exported in the manifest and does not declare a receiver-level permission there, so later wallpaper-plugin analysis must inspect code-level validation/registration rather than assuming the custom permission protects every update broadcast.

### Native/JNI boundary made concrete

`libptutils.so` is present for arm64-v8a, armeabi-v7a, x86 and x86_64. The inspected arm64 library is a stripped Android ELF targeting API 23 and built by NDK r30.

The Projectivy application class has a declared native method:

- `PTApplication.vs(boolean): boolean`

`libptutils.so` exports the matching JNI entry point:

- `Java_com_spocky_projengmenu_PTApplication_vs`

It also exports `Java_com_spocky_projengmenu_PTApplication_vc`; the current parsed `PTApplication` member inventory does **not** expose a matching `vc` declaration, so this symbol should be treated as a native lead (possibly unused, removed/renamed at the managed layer, or reached by a mechanism not visible in the current class summary) rather than assumed active.

The library additionally contains ZIP/minizip-style helpers and several short exported helpers. Group 1 does not assign behaviour to those functions; Group 8/9/native tracing will follow actual Java/JNI callers before deciding whether any equivalent is required.

### APK identity/signing posture

The exact analyzed APK is signed with both APK Signature Scheme v1 and v2; v3/v3.1 are absent. The declared v1/v2 signer certificate fingerprint is:

`f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`

REA reports v1 and v2 as self-consistent and agreeing on the same signer, with v2 the strongest present scheme. This is retained only as artifact identity/provenance evidence; it is not used to infer implementation behaviour.

### Correction from the first pass

The first report stated `PTApplication` had 16 methods. The current class inventory shows **17 methods / 13 fields**. The corrected count is now canonical for this Group 1 baseline.

## Group 1 review additions (third pass, after Group 2)

A further review was performed after closing the home-screen graph. It produced two corrections and several architectural refinements that strengthen the baseline.

### Class ownership / minification boundary

The APK's 7,395-class inventory currently breaks down at the top namespace level approximately as follows:

- `defpackage.*`: **6,486** classes
- desugared `j$.*`: **519** classes
- `androidx.*`: **116** classes
- `com.spocky.projengmenu.*`: **90** classes
- `com.google.*`: **84** classes
- other retained namespaces make up the remainder

This materially refines the earlier wording. `defpackage.*` is **not synonymous with Projectivy implementation**: Group 2 proved that some short/obfuscated classes are minified Leanback/framework machinery while others are Projectivy-specific models/controllers/presenters. Future groups must classify an obfuscated class by its inheritance, xrefs, resources and API usage before assigning ownership.

There are also **three retained classes in a separate Projectivy plugin namespace**:

- `tv.projectivy.plugin.wallpaperprovider.api.Event`
- `tv.projectivy.plugin.wallpaperprovider.api.Wallpaper`
- `tv.projectivy.plugin.wallpaperprovider.api.a`

Therefore “90 Projectivy classes” should only ever mean “90 classes retaining the main `com.spocky.projengmenu` namespace”, not the complete Projectivy-owned/API surface.

### Wallpaper plugin API is a typed contract

The retained plugin namespace makes the wallpaper-provider boundary more concrete than the manifest action alone. `Wallpaper` is Parcelable/data-class-like and exposes these fields through getters:

- `uri`
- `type`
- `displayMode`
- `title`
- `author`
- `source`
- `actionUri`

`Event` is also parcelable and carries an integer `eventType`. The retained `tv.projectivy.plugin.wallpaperprovider.api.a` class exposes Binder-style `asBinder()` / `onTransact(...)`, showing that the plugin mechanism includes a typed IPC boundary in addition to discovery/update broadcasts. Exact transaction semantics remain Group 6 work, but the existence of the typed plugin API is now part of Group 1 architecture.

### Package visibility is deliberately bounded

The manifest does **not** request `QUERY_ALL_PACKAGES`. Instead it declares package visibility for HOME/LAUNCHER/LEANBACK launch targets plus specific intents/packages. That means later app-discovery analysis should not assume unrestricted package enumeration on modern Android; Group 3 must preserve the distinction between PackageManager queries made visible by manifest `<queries>` and any platform/vendor exceptions.

### MainActivity launcher metadata refinement

The MAIN intent filter also contains `android.intent.category.DEFAULT`, and `MainActivity` declares:

- `android.app.shortcuts` -> `@xml/shortcuts`

The shortcuts resource therefore belongs to the launcher entry-point architecture even though its exact contents are intentionally deferred to Group 10.

### Runtime/bootstrap components supplied by libraries

`androidx.startup.InitializationProvider` registers initializers for:

- EmojiCompat
- Mike Penz Google Material Outlined iconics
- WorkManager
- ProcessLifecycle
- ProfileInstaller
- OkHttp platform initialization

`androidx.appcompat.app.AppLocalesMetadataHolderService` is disabled as a runtime service but carries `autoStoreLocales=true`. The manifest also declares optional libraries for AdServices and AndroidX Window extensions/sidecar.

These are recorded so later reverse engineering does not accidentally attribute library bootstrap/background behavior to Projectivy itself.

### Build SDK identity

The shipped manifest reports both `compileSdkVersion=37` and targetSdk 37 (compile codename/platform build name `17`). This is artifact identity evidence and useful when matching platform behavior during reconstruction.

### PTApplication bootstrap boundary

`PTApplication.attachBaseContext(Context)` wraps the base context through an obfuscated helper before calling `super.attachBaseContext`, while `onCreate()` initializes application-scoped collaborators around the `Application.onCreate()` call. `getSharedPreferences()` returns cached wrapper instances rather than raw platform `SharedPreferences` objects. These facts strengthen the earlier conclusion that `PTApplication` is an application/bootstrap and persistence interception boundary; the semantics of those wrappers remain Group 10 work.

## Group 1 conclusions

1. Projectivy 4.72 is structurally a classic Android TV/Leanback launcher with `MainActivity` → `MainFragment`, not a single monolithic screen.
2. The manifest exposes clear subsystem boundaries for HOME, side panels, channels, notifications, wallpapers, inputs, device tools, accessibility, boot and import/export.
3. The 90 classes retaining `com.spocky.projengmenu` are only one visible part of the app-owned/API surface. Obfuscated `defpackage.*` classes must be traced **and ownership-classified** per subsystem because that namespace also contains minified library/framework code.
4. Resource XML gives strong, implementation-useful evidence for row/category presentation, hover/detail UI, status bar, settings and plugin contracts.
5. Wallpaper plugins, TV Provider access, notification listener, accessibility key filtering and vendor input/device adapters are all first-class architecture, not speculative features.
6. The next group should start at `MainFragment` and the home-specific resources, then close its xref graph before moving on to app actions or channels.

## Next group

**Group 02 — Home screen architecture**

Targets:
- `MainFragment`
- row/list presenters and adapters reached from it
- category/row model classes
- card presenter hierarchy
- focus and selection persistence
- row-header visibility/reveal behaviour
- hover/details panel
- title/status area
- activity/fragment lifecycle interactions
- concrete obfuscated collaborators reached through xrefs
