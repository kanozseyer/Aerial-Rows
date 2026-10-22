# Projectivy 4.72 clean-room reimplementation blueprint

Status: research baseline for Aerial-Rows
Target: Android TV / Google TV launcher behavior compatible with the useful parts of Projectivy, implemented independently in Kotlin/Compose
Reference release: Projectivy 4.72 / versionCode 97

## 1. Scope and clean-room boundary

Projectivy Launcher is not open-source. Its public `spocky/miproja1` repository contains device configuration, release APKs and the issue tracker, not the application source.

This project therefore should **not** be treated as a literal Projectivy source fork. The safe and maintainable goal is a clean-room reimplementation:

- Observe public behavior, documented Android APIs, public release metadata, UI behavior and independently reported diagnostics.
- Use Projectivy class/component names only where Android exposes them (manifest/component names, stack traces, logs) and only as evidence of behavior.
- Do not copy decompiled method bodies, proprietary resources, art, strings or code.
- Do not bypass, patch, emulate or remove Projectivy Premium/licensing checks. Aerial-Rows implements its own feature set under its own licensing model.
- Prefer Android platform contracts over reproducing Projectivy-specific workarounds.

The result should feel familiar to a Projectivy user while retaining the existing Aerial-Rows codebase and its Compose architecture.

## 2. Evidence used

### High-confidence public evidence

- Projectivy 4.72 release notes (GitHub release, 2026-10-03).
- Projectivy public README and feature list.
- Projectivy 4.72 Android manifest/permission metadata published by APKMirror.
- Android TV / Google TV platform documentation for `TvProvider`, `WatchNextProgram`, preview programs and TV input APIs.
- Public Projectivy issues containing stack traces, `dumpsys`, `atrace`, component names and user-visible behavior.

### Independently reported reverse-engineering evidence

Public issues #521 and #608 contain third-party JADX/bytecode findings. They are useful to infer architecture, but this implementation must reproduce behavior independently rather than copy those decompiled bodies.

### Current Aerial-Rows source

Aerial-Rows already supplies the base launcher shell, Compose TV rendering, app discovery, app categorization, ordering, grid/carousel/dock layouts, wallpapers, video wallpaper playback, built-in aerials, status information, DataStore-backed settings and app actions.

## 3. Projectivy behavior map

### 3.1 Home screen model

Projectivy's home page is fundamentally a vertical collection of configurable rows. Rows fall into several families:

1. App/category rows
2. Android TV provider/channel rows
3. Watch Next / Continue Watching
4. Now Playing
5. Input-source shortcuts
6. User/internal shortcuts

Rows expose common presentation controls such as visibility, title, icon, card size, margins, sliding-row vs grid behavior and focus styling. Some media/program rows expose title/description metadata, progress and preview video.

A useful clean-room abstraction is therefore **not** `Category = list<AppEntry>`, but a common `HomeRow` containing common `HomeCard` models with row-specific capabilities.

### 3.2 App rows and long-press actions

Observed Projectivy app quick menu options include:

- App settings
- Open
- Uninstall
- Launch on boot
- Add/remove parental control

A public request asks for Force stop as an additional action. Aerial-Rows already has app launch, App Info, uninstall and best-effort background-process close actions, so these should become pluggable card actions rather than launcher-screen special cases.

### 3.3 Android TV channels

Projectivy reads Android TV channels/programs and exposes an `Edit channels` area distinct from `Edit categories`.

The 4.72 bundle declares TV-provider-related permissions including:

- `android.permission.READ_TV_LISTINGS`
- `com.android.providers.tv.permission.READ_EPG_DATA`
- `com.android.providers.tv.permission.WRITE_EPG_DATA`

For Aerial-Rows, prefer **read-only** access unless a concrete feature requires writes. Query channels and programs through `TvContractCompat` / `TvProvider` APIs and map them to launcher-owned immutable models.

Do not persist user visibility purely by provider row ID. Public issue #598 reports OEM firmware that recreates channels with new IDs after reboot. Persist a stable key such as package/component + internal provider ID/display name, with numeric channel ID treated as an ephemeral lookup key.

### 3.4 Watch Next / Continue Watching

Projectivy reads Android's Watch Next provider (`content://android.media.tv/watch_next_program`). Public issue #521 reports that 4.71 cached the result in a lazy singleton and failed to observe provider changes.

Aerial-Rows should implement this correctly from day one:

- Query `TvContractCompat.WatchNextPrograms.CONTENT_URI` off the main thread.
- Register a `ContentObserver` for the Watch Next URI while the launcher is alive.
- Debounce bursts of updates.
- Refresh on observer notification and on launcher resume as a safety net.
- Map provider data to immutable `ProgramCard` objects.
- Preserve provider intent/deep link, package, title, description, poster/thumbnail, progress, last-engagement time and Watch Next type.
- Apply parental-control checks to **all** launch paths, including Watch Next deep links. Public issue #132 shows why this must be centralized rather than implemented only on app-card clicks.

The current Android TV documentation still describes the Watch Next provider. Google is also moving partner recommendation publishing toward Engage SDK. A third-party launcher cannot assume all Google TV recommendation data is queryable through Engage; therefore our launcher should treat TvProvider/Watch Next as the supported local source and isolate it behind a repository so another source can be added later.

### 3.5 Program cards and preview video

Projectivy supports channel program cards and has a `Play preview video` setting. 4.72 added a side panel for selected-program details.

Implement one `ProgramCard` renderer shared by channel rows and Watch Next:

- poster/thumbnail image
- optional app/provider badge
- title
- description/subtitle
- progress bar when available
- live/new badges when derivable
- optional preview URI
- deep-link intent
- details panel data

Preview playback should be an explicit lifecycle state machine. Start only after a focus dwell delay, stop on focus loss, stop/pause when the launcher leaves foreground, and never keep an infinite animator running when nothing visible changes.

### 3.6 Focus and row rendering

Public diagnostics for 4.72 show its home UI still uses Android Views/RecyclerViews (`RV OnLayout`) with hover/detail child views. Another issue reports a persistent per-vsync animation after moving to a second row.

Do **not** reproduce this architecture. Keep Compose TV and make focus state explicit:

- one selected row index
- one selected card key per row
- stable keys for every card
- a row-level focus-restoration policy
- only finite focus animations (`animate*AsState`) and no unconstrained `rememberInfiniteTransition` for focus effects
- no invisible 1 px detail/hover containers kept animating in the tree

The left-side row/category header requested for Aerial-Rows should be a stateful overlay revealed only when focus reaches the left boundary, rather than a permanently rendered navigation rail.

### 3.7 Now Playing

Projectivy's Now Playing row depends on Android notification access. Public reports indicate it consumes active media sessions and filters persistent/spurious sessions (4.72 specifically filters a Humax TV persistent media session).

Recommended implementation:

- `NotificationListenerService` obtains/maintains permission state and allows `MediaSessionManager` access.
- Observe active `MediaController`s.
- Convert `MediaMetadata` + `PlaybackState` to `NowPlayingCard`.
- Deduplicate by package/session identity.
- Filter sessions without meaningful metadata/playback and provide a device-specific denylist hook rather than hardcoding UI logic.
- Clicking a card launches the owning package or session activity if available.
- Long press may expose stop/media controls only when supported.
- Keep idle-detection/media-playback logic separate from the Now Playing UI even if both consume the same media-session repository.

### 3.8 Wallpaper engine

Projectivy has a provider-oriented wallpaper subsystem. Publicly documented/observed sources and features include:

- no wallpaper
- static media file
- folder
- video/GIF
- random-program artwork
- external wallpaper-provider plugins
- online/provider sources
- display transforms and visual adjustments
- transitions including `NONE`
- wallpaper preview in settings
- image caching (4.72 release notes: capped at 50 MB)

Aerial-Rows already has preset/custom/video wallpapers and built-in aerials. Refactor these behind a provider contract rather than adding more booleans to `LauncherConfig`.

Suggested contract:

```kotlin
interface WallpaperProvider {
    val id: String
    val supportsPreview: Boolean
    suspend fun next(request: WallpaperRequest): WallpaperAsset?
}

sealed interface WallpaperAsset {
    data class Image(val uri: Uri) : WallpaperAsset
    data class Video(val uri: Uri) : WallpaperAsset
    data class Color(val argb: Long) : WallpaperAsset
}
```

Keep provider selection separate from rendering/transition settings:

```kotlin
data class WallpaperRenderConfig(
    val scaleMode: WallpaperScaleMode,
    val transition: WallpaperTransition,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val hue: Float,
    val blur: Float,
)
```

**Aerials should become one `WallpaperProvider`**. That gives us the desired result: Projectivy-class wallpaper flexibility with Couchy/Aerial-Rows' aerial catalogue as a native source.

Persist SAF folder/file URI permissions. Do not rely on fixed paths under `Android/data`; multiple public Projectivy issues show how fragile that becomes on recent Android TV versions.

### 3.9 Icons and shortcuts

Projectivy supports custom names/icons, image-based icons, icon packs and launcher/internal shortcuts. 4.72 added a built-in icon-pack browser.

Aerial-Rows should use a single `CardIdentity` independent of package name:

```kotlin
data class CardIdentity(
    val stableKey: String,
    val launchTarget: LaunchTarget,
    val sourcePackage: String?,
)
```

Then apply user overrides through a separate customization table:

```kotlin
data class CardOverride(
    val customTitle: String? = null,
    val iconSource: IconSource? = null,
)
```

Icon sources should support:

- original app banner/icon
- custom persisted image URI or launcher-copied file
- icon-pack drawable resolved through standard icon-pack metadata (`appfilter.xml` / drawable resources)
- launcher built-in icon

The backup format should either copy custom images into the backup package or store a launcher-owned content-addressed copy. Do not export only external absolute paths; public Projectivy backup reports show that this breaks cross-device restoration.

### 3.10 Inputs and vendor adapters

Projectivy exposes HDMI/AV/input-source shortcuts. Public issue #608 identifies multiple input Activities and reports that Projectivy combines standard `TvInputManager` discovery with vendor-specific fallbacks.

Aerial-Rows should model this explicitly:

```kotlin
interface InputSourceProvider {
    suspend fun sources(): List<InputSource>
    fun observe(): Flow<List<InputSource>>
    suspend fun launch(source: InputSource): Result<Unit>
}
```

Implementation layers:

1. `StandardTvInputProvider` using `TvInputManager.getTvInputList()` and `registerCallback()`.
2. Optional `DeviceInputAdapter`s keyed by manufacturer/model/known components.
3. User-configurable manual intent/component shortcut fallback.

Do not copy Projectivy's early-finish input Activity pattern. Issue #608 demonstrates an API-27 lifecycle crash caused by finishing a partially initialized `ComponentActivity` during creation. In Aerial-Rows, input launching should be a normal action or a minimal fully initialized trampoline Activity whose lifecycle is valid before it finishes.

### 3.11 Launcher replacement, boot and accessibility

Projectivy supports being a normal HOME launcher and an optional Accessibility-based launcher override/button-remapping mode.

Aerial-Rows should use this priority:

1. Proper Android HOME role / HOME intent declaration.
2. Optional compatibility mode using accessibility only when the device cannot keep the selected HOME launcher.
3. Boot receiver for restoring launcher-owned boot actions, not for fighting the OS continuously.

The optional Accessibility service should be narrowly scoped and disabled by default. Intercept only configured keys/events. Do not consume gamepad input globally; Projectivy 4.72 has public reports where remapping interferes with game controllers.

When a boot compatibility action must relaunch the home screen, explicitly construct `ACTION_MAIN + CATEGORY_HOME` so Android creates/returns to a home task rather than a normal standard task. Public Projectivy issue #605 demonstrates why the intent category matters on Google TV Streamer.

### 3.12 Parental controls

Parental control must be implemented in one launch gate, not scattered through UI handlers:

```kotlin
interface LaunchGate {
    suspend fun authorize(target: LaunchTarget): AuthorizationResult
}
```

Every path goes through it:

- app card
- shortcut
- Watch Next/program deep link
- Now Playing
- input source
- remote button mapping
- launch-on-boot

Store only a salted password/PIN hash, never plaintext. Support a grace window only after successful authorization. Prefer protecting launch requests initiated by our launcher; aggressive foreground-app policing can interfere with apps, as multiple Projectivy parental-control issue reports demonstrate.

### 3.13 Backups

Use an explicit versioned schema, e.g. ZIP:

```text
backup.zip
  manifest.json
  config.json
  overrides.json
  assets/<sha256>.*
```

Requirements:

- schema version and migration layer
- no raw DataStore file copy
- launcher-owned custom icon/wallpaper assets included by content hash
- SAF `ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT` for save/restore
- automatic internal backups stored in app-controlled storage
- user-visible browse/restore screen
- import validation before replacing current config
- rollback to last-good automatic backup on failed migration

### 3.14 Calibration and engineering/device tools

Projectivy advertises calibration patterns and vendor engineering menu detection. These are independent from the launcher core and should live in `feature-device-tools`.

Calibration patterns are easy to implement clean-room from generated graphics/video test assets. Engineering menu launchers should be data-driven `IntentSpec`s keyed by manufacturer/model with `resolveActivity()` checks. Never assume a hidden component exists.

## 4. Inferred Projectivy component map

The following public component names are useful only as behavioral evidence:

- `com.spocky.projengmenu.ui.home.MainActivity`
- `com.spocky.projengmenu.services.ProjectivyAccessibilityService`
- settings Activity family
- category/channel shortcut Activity family
- HDMI1/HDMI2/HDMI3/AV/source-popup Activity family

Public traces also show a shared base Activity and vendor input-resolution layer, but their obfuscated implementation should not be copied.

## 5. Aerial-Rows today: keep vs replace

### Keep

- `MainActivity` HOME launcher shell
- Compose TV architecture
- `AppRepository` package scan/cache and categorization concept
- `ConfigStore` / DataStore direction
- existing category order and app movement behavior
- carousel/grid/dock renderers
- current status bar and network state infrastructure
- app actions (`launch`, App Info, uninstall)
- Media3 wallpaper/video playback
- `BuiltinAerials` catalogue and aerial playback behavior
- current UI scaling, icon size, spacing and corner controls

### Refactor

- `LauncherConfig`: split into smaller config domains instead of accumulating feature booleans.
- category-only home model: replace with a row registry that can mix app rows, provider channels, Watch Next and Now Playing.
- wallpaper flags: replace with `WallpaperProviderConfig` + `WallpaperRenderConfig`.
- app-specific context menu state: replace with generic `CardAction` dispatch.
- settings sheet: migrate to nested settings routes with persistent focus and reusable switch/choice/dropdown rows.

### Add

- TV Provider repository + ContentObservers
- channels + Watch Next
- program-card model and details panel
- preview playback coordinator
- notification/media session repository + Now Playing
- icon-pack resolver/custom overrides
- shortcut model
- input-source repository + device adapters
- optional accessibility service/key mapping
- boot coordinator
- launch-on-boot
- parental launch gate
- versioned backup engine
- wallpaper-provider SPI
- device tools/calibration

## 6. Proposed source layout

Keep one app module initially, but organize by feature boundaries so extraction into Gradle modules remains possible later:

```text
com.conreo.couchytv/
  core/
    model/
    config/
    launch/
    images/
    focus/
  feature/
    apps/
    home/
    tvprovider/
    watchnext/
    channels/
    nowplaying/
    wallpaper/
    aerials/
    icons/
    shortcuts/
    inputs/
    accessibility/
    boot/
    parental/
    backup/
    settings/
    devicetools/
```

## 7. Core model proposal

```kotlin
sealed interface HomeRow {
    val id: String
    val title: String
    val presentation: RowPresentation

    data class Apps(/*...*/) : HomeRow
    data class Channel(/*...*/) : HomeRow
    data class WatchNext(/*...*/) : HomeRow
    data class NowPlaying(/*...*/) : HomeRow
    data class Inputs(/*...*/) : HomeRow
    data class Shortcuts(/*...*/) : HomeRow
}

sealed interface HomeCard {
    val key: String
    val title: String
    val artwork: Artwork?
    val launchTarget: LaunchTarget?
}

sealed interface LaunchTarget {
    data class Package(val packageName: String) : LaunchTarget
    data class IntentUri(val uri: Uri) : LaunchTarget
    data class Component(val component: ComponentName) : LaunchTarget
    data class Input(val id: String) : LaunchTarget
    data class SystemAction(val action: String) : LaunchTarget
}
```

This is the key architecture change: the renderer no longer needs to know whether a card came from an installed app, Watch Next, a TV provider channel or HDMI. It renders capability data and delegates launch/action behavior.

## 8. Data flow

```text
PackageManager -----> AppsRepository -----------\
TvProvider ---------> ChannelRepository ---------\
WatchNextProvider --> WatchNextRepository --------> HomeRowRegistry -> HomeViewModel -> Compose UI
MediaSession -------> NowPlayingRepository ------/
TvInputManager -----> InputRepository -----------/
User shortcuts -----> ShortcutRepository -------/

Card click -> LaunchCoordinator -> LaunchGate -> target-specific launcher
```

Every repository exposes `StateFlow` or `Flow` and does blocking/provider work off the main thread. Home composition receives immutable snapshots.

## 9. Settings UX target

The requested streamlined Projectivy-like settings should use a proper routed settings screen rather than one giant sheet.

Rules:

- Selecting/toggling an option must not reset list focus to the top.
- Booleans use a clearly visible TV switch.
- Enumerated settings use a dropdown/choice dialog.
- Back returns to the exact prior row/focus.
- A row can show current value on the right.
- Changes that are safe should apply immediately.
- Dangerous actions (reset, uninstall-like operations, destructive import) get confirmation.
- Home left headers are hidden until the user navigates left from the first card.

Suggested top-level sections:

```text
Home screen
Rows & channels
Apps & shortcuts
Appearance
Wallpaper & aerials
Cards & focus
Status bar
Remote & accessibility
Power & startup
Parental controls
Backup & restore
Device tools
About
```

## 10. Manifest/component changes to expect

Add only as features land:

- `android.permission.READ_TV_LISTINGS`
- TV-provider EPG read permission where available/required
- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.WAKE_LOCK` only if an implemented feature requires it
- Notification listener service with `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`
- optional Accessibility service with `android.permission.BIND_ACCESSIBILITY_SERVICE`
- boot receiver
- package queries/components needed for launch targets/icon packs

Avoid adding `WRITE_EPG_DATA` unless Aerial-Rows actually publishes/modifies TV provider data.

## 11. Implementation order

### Phase A — architecture foundation

1. Introduce `HomeRow`, `HomeCard`, `LaunchTarget`, `CardAction` and `LaunchCoordinator`.
2. Adapt current app categories into `HomeRow.Apps` without changing the visible launcher.
3. Split config into domain configs and add migrations from current `LauncherConfig`.
4. Replace app-only menu plumbing with generic card actions.

Acceptance: existing carousel/grid/dock behavior, ordering, hiding, uninstall and aerial wallpapers remain identical.

### Phase B — Projectivy-defining media rows

5. Add `TvProviderRepository`.
6. Add channel discovery/program queries and `Edit channels` settings.
7. Add Watch Next repository + `ContentObserver`.
8. Add 16:9/poster-aware `ProgramCard` renderer, progress and deep links.
9. Add selected-program detail side panel.
10. Add preview-video coordinator with Media3.

Acceptance: Shield/Google TV provider channels and Watch Next populate, update without app restart and deep-link correctly.

### Phase C — Now Playing and status integration

11. Add NotificationListener/MediaSession repository.
12. Add dynamic Now Playing row.
13. Add optional notifications/media status UI.

Acceptance: Spotify/YouTube-style sessions appear/disappear reliably and no idle animation remains after playback stops.

### Phase D — Projectivy-style customization

14. Replace settings sheet with routed settings UI preserving focus.
15. Add per-row presentation settings (grid/sliding/title/icon/size/margins).
16. Add focus effects and fixed/attached details positioning options.
17. Add hidden-until-left-navigation side headers.
18. Add custom card titles/icons and icon-pack browser.

### Phase E — wallpaper provider architecture

19. Introduce provider/render-config split.
20. Port static/custom/video wallpaper to providers.
21. Port built-in aerials to `AerialWallpaperProvider`.
22. Add folder provider using SAF persisted permissions.
23. Add random-program artwork provider.
24. Define a documented external wallpaper-provider plugin contract.
25. Add preview, transition and bounded image cache.

### Phase F — device/launcher integration

26. Add standard TV input discovery + callbacks.
27. Add manual/vendor input adapter layer.
28. Add optional Accessibility service and key mapping.
29. Add boot coordinator and launch-on-boot.
30. Add HOME-task compatibility handling.

### Phase G — robustness features

31. Add centralized parental launch gate.
32. Add versioned backup/restore with embedded assets.
33. Add automatic backups and restore browser.
34. Add calibration patterns/device tools.
35. Performance/accessibility/OEM test pass.

## 12. Regression traps learned from Projectivy reports

Treat these as explicit tests:

- Watch Next updates while launcher remains alive.
- Channel IDs changing after reboot do not lose user visibility preferences.
- Moving focus across rows does not leave an infinite animation/vsync callback.
- Leaving settings with HOME does not leave animated UI alive in the background.
- Input launch trampoline does not finish during an invalid lifecycle state on API 23-28.
- `TvInputManager` updates are observed rather than cached forever.
- Gamepads still work when remote remapping is enabled.
- Parental control protects program/deep-link/remote launch paths, not only app cards.
- Parental overlay does not repeatedly interrupt an already-authorized foreground app.
- Custom icon/wallpaper assets survive backup to another device.
- SAF folder/file access survives reboot and Android 11-14 storage restrictions.
- Video/aerial wallpaper stops decoding/playing while launcher is not visible unless explicitly needed.
- Portrait and odd-ratio wallpaper images cannot corrupt/overdraw launcher UI.
- Plex/private-LAN artwork failure is handled as an image-load error without weakening global TLS verification.

## 13. Performance rules

Aerial-Rows already contains several performance-conscious choices. Preserve them and add these constraints:

- No provider queries on main thread.
- Stable Compose keys for rows/cards.
- Immutable row/card snapshots.
- Conflate/debounce content-observer bursts.
- One image pipeline/cache shared across apps/programs/wallpapers, with independent wallpaper budget if needed.
- Pause preview/video wallpaper when home is not RESUMED.
- No infinite animations for hidden content.
- Avoid recomputing full app/channel categorization on clock/status updates.
- Measure cold launch, warm resume, D-pad focus latency, memory and idle CPU on real Shield/Google TV hardware.

## 14. REA/JADX bytecode-analysis runbook

The user's `rea-fork` supports the following static Android workflow with JDK 17+ and `jadx-headless-mcp 0.7.1`:

```sh
export REA_JADX_MCP_JAR=/absolute/path/jadx-headless-mcp-0.7.1-all.jar
export JAVA_HOME=/absolute/path/jdk-21

rea inspect-android-package ProjectivyLauncher-4.72-c97-xda-release.apk
rea search-android-classes ProjectivyLauncher-4.72-c97-xda-release.apk MainActivity
rea inspect-android-class ProjectivyLauncher-4.72-c97-xda-release.apk com.spocky.projengmenu.ui.home.MainActivity
rea trace-android-references ProjectivyLauncher-4.72-c97-xda-release.apk com.spocky.projengmenu.ui.home.MainActivity
```

Pinned JADX JAR SHA-256 expected by REA 6.7.0:

`6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`

Official Projectivy 4.72 GitHub/XDA APK SHA-256 from the release asset metadata:

`a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

### Analysis queries to run when byte access is available

Inventory first, then targeted inspection. Do not bulk-copy source.

```text
Manifest/components
  MainActivity
  AccessibilityService
  NotificationListenerService
  BootReceiver
  shortcut/input Activities
  providers/services/receivers

Home
  search classes containing home / row / category / card / program
  trace MainActivity references
  identify model/repository boundaries

TV provider
  search TvContract, TvProvider, PreviewProgram, WatchNextProgram
  inspect query projections and sort/filter logic
  identify refresh/observer code

Media
  search MediaSessionManager, MediaController, NotificationListenerService

Wallpaper
  search wallpaper/provider/plugin/folder/media/video
  identify external plugin intent/binder/content-provider contract from manifest/API usage

Inputs
  search TvInputManager, TvInputInfo, SourceHDMI, SourceAV
  map standard-vs-vendor branches only at a behavioral level

Backup/config
  search backup/export/import/preferences/datastore/sharedpreferences
  identify schema only; do not copy serialized proprietary data verbatim

Cards/focus
  search preview video, glow, title, description, RecyclerView adapters
  use this only to enumerate behavior/settings, not to port implementation
```

Record findings as capability/interface notes, not decompiled Java pasted into Aerial-Rows.

## 15. Current bytecode-analysis limitation in the ChatGPT workspace

The APK and JADX JAR were supplied in the Project/Library and their metadata is visible, but the current Project file bridge denies raw-byte materialization for those earlier uploads. REA therefore cannot be executed against those Library references inside this workspace yet.

Separately, `rea-fork` 6.7.0 declares Node `^22.19.0 || ^24.11.0 || >=26.0.0`; the current analysis container reports Node 22.16.0. JDK 21 is available. A real REA execution here therefore needs both an accessible raw APK/JAR path and a compatible Node runtime.

This does **not** block implementation: the high-value Android contracts and the public reverse-engineering findings above already establish the architecture needed for Channels, Watch Next, Now Playing, Inputs, launcher override, wallpapers and settings. The bytecode pass should be used to fill in edge-case behavior and component inventory, not as a source-code donor.

## 16. Recommendation

Use **Aerial-Rows as the product base** and implement Projectivy-compatible behavior on top of it. Do not start a second launcher from a decompiled Projectivy tree.

Why:

- Aerial-Rows is already under our control and builds on modern Compose TV.
- Its existing aerial/video pipeline is a differentiator and naturally becomes a wallpaper provider.
- Most missing Projectivy features are Android platform integrations, not UI code that needs Projectivy internals.
- We can avoid known Projectivy lifecycle/cache/animation problems instead of inheriting them.
- The resulting code remains maintainable, auditable and legally cleaner.

The first coding milestone should be **HomeRow abstraction + TV Provider/Watch Next**. Once that is in place, almost every remaining Projectivy-like feature plugs into the same row/card/action architecture instead of becoming a one-off launcher special case.
