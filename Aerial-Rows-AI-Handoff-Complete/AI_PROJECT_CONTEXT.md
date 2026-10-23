# Aerial-Rows — AI Project Context / Handoff

**Read this file first before making changes.** It is a durable handoff reconstructed from the full Launcher project conversation and the completed Projectivy/Couchy research programme.

This document is intentionally redundant. It is designed so a fresh ChatGPT, Claude, Gemini, Codex-style agent, or human contributor can continue the project without the original conversation.

## 1. Project goal

The repository is **`kanozseyer/Aerial-Rows`**. It began as a Couchy Launcher fork and is being evolved into a high-quality Android TV launcher with:

- Couchy's lightweight Compose implementation and performance characteristics;
- Projectivy-level launcher behavior and subsystem completeness;
- Aerial video wallpapers / local wallpapers;
- Sliding horizontal rows with a Projectivy-style left rail;
- Android TV Preview Channels + Watch Next;
- future notifications + Now Playing;
- future physical TV inputs / HDMI support;
- future startup/remote/idle features;
- robust backup/theme/icon/shortcut support;
- optional advanced features such as parental controls and display profiles.

The desired result is **not** a wholesale clone of Projectivy source code. Projectivy was reverse engineered as a behavioral/architectural reference. Aerial-Rows should remain an independently implemented Compose launcher.

## 2. Clean-room / IP boundary

Research of the proprietary Projectivy APK was used to understand behavior, Android APIs, lifecycle, persistence, focus rules, JNI boundaries, device integration, and algorithms.

Do **not**:

- paste proprietary decompiled Projectivy source into this repo;
- copy premium/licensing/protection logic;
- bypass Projectivy licensing;
- upload the proprietary Projectivy APK or decompiled source into this repository;
- treat obfuscated/natively protected internals as required architecture.

Do:

- independently implement verified behavior using public Android APIs and clean interfaces;
- use Projectivy only as a behavioral reference;
- preserve Aerial-Rows/Couchy's own visual identity and implementation style.

## 3. Current authoritative branch and provenance

Implementation branch studied and to continue from:

- branch: `aerialrows-development`
- commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`
- tree: `457f4ab1d08a8d545bdf5545777cd31f907ae24d`
- commit message: `Wire row editor state into Sliding Rows host`

This branch was rechecked at the end of C12 and was still exactly on that commit. The C1–C12 comparison therefore matches the live branch.

Main branch at the time of the audit:

- `d3dcd70d3dd3b261108d25d72569671aee23b2d6`
- development was 36 commits ahead and 0 behind at the original Group 12 audit.

Important pinned file blob SHAs at the research baseline:

- `app/build.gradle.kts` — `1668e8c60f6cf21ff2363f216028fa431f3d6754`
- manifest — `5fb2138676d6e929b3e4fa61a865b32837274421`
- `MainActivity.kt` — `e5b83f7aa597ad245b607f30a65c81d878bdb303`
- `AppRepository.kt` — `6467d2787ce9fc920c025e8cb994c2cf520005cb`
- `Config.kt` — `4710f41198308e555ac9e3b12d515f1fefa1db6e`
- `LauncherScreen.kt` — `a3172678c0bde47a28fa89247b4c5662cc659b1b`
- `SlidingRows.kt` — `a43e8a4fec51549ea7d942490b9c127ee758bd69`
- `AppCard.kt` — `4f045e410d80fb13c7da49d732b3ad247edda95c`
- `RowQuickEditor.kt` — `515c796d7aa2f5b755e9858f2be435a0f72e19a8`
- `SettingsSheet.kt` — `a20140273352bd63c990b2c3e9037c1eb717836c`
- `Menus.kt` — `b847347b94b1be51cd03c180593500d8d6055de3`
- `PanelStyle.kt` — `05927aefe4f8132be356280fd7a5369cd46fbe57`
- `BuiltinAerials.kt` — `bdecdd784dd4b04c2c4c0877acd3faff46760a0f`
- `TvContentRepository.kt` — `714b6fd92637d76f6d12b4702db1475859fccac3`
- `Status.kt` — `fc3f01990123df6b402f7d8927e286e723d0bbda`
- `SetupWizard.kt` — `092f17cc098cbb4e72539020f437c5c796a4db39`
- `aerials.json` — `3ad397817eb236c858a5bbfe59ae40a80d661d99`

Before editing code, verify the current branch/commit because this handoff intentionally preserves the 2026-10-23 research baseline.

## 4. Immediate next action — do not restart research

All planned comparison research C1–C12 is complete.

The next task is **Phase 0A — make the build source-pure**:

1. Materialize every change injected by `applyAerialRowsSourcePatches` into normal checked-in Kotlin/resources.
2. Remove `applyAerialRowsSourcePatches` from `app/build.gradle.kts`.
3. Verify a clean build does not mutate tracked source.
4. Build debug + release + lint from a clean checkout.
5. Only then begin structural refactoring.

Do not start another reverse-engineering group unless a genuinely new unknown blocks implementation.

## 5. Critical current build flaw

`app/build.gradle.kts` contains an `applyAerialRowsSourcePatches` task wired to `preBuild`.

It rewrites source files before compilation, including:

- `SettingsSheet.kt`
- `LauncherScreen.kt`
- `strings.xml`

The patch currently injects / wires:

- `LAYOUT_SLIDING_ROWS` into Display settings;
- Channels settings page;
- Sliding Rows renderer branch;
- row/dock panel appearance settings;
- multi-select Aerial source collections via `builtinSources`;
- Dock `AerialPanel` background integration;
- related strings.

This is the number-one maintenance and reproducibility problem.

It is also a build-performance problem because it rewrites very large Kotlin files on every `preBuild`, potentially invalidating incremental compilation.

**Phase 0A is mandatory before feature work.**

## 6. Definitive high-level architecture decision

Couchy wins the implementation/hot-path layer. Projectivy wins the behavior/domain-boundary layer.

Keep Couchy/Aerial-Rows for:

- Kotlin + Compose;
- single small HOME Activity;
- D-pad-first TV UI;
- app scanning and pre-rasterized artwork cache;
- immutable object reuse;
- stable Compose rendering;
- SurfaceView + Media3 wallpaper rendering;
- trimmed video buffers;
- decoder release while launcher is hidden;
- low component/service count;
- typed `LauncherConfig` + DataStore;
- current visual identity.

Adopt/reimplement Projectivy concepts for:

- generic HomeRow/HomeItem model;
- stable row + card focus restoration;
- explicit rail state;
- launch dispatcher/policy;
- richer Android TV provider behavior;
- wallpaper providers/events/scheduler;
- typed setting descriptors and effects;
- notification + media repositories;
- physical TV input architecture;
- startup / remote gesture / idle coordinators;
- migration/backup/theme/icon/shortcut systems;
- optional parental/device features.

Do not copy Projectivy's:

- Leanback fragment implementation;
- Accessibility as a general-purpose ownership hub;
- preference-heavy internals;
- root/package maintenance internals;
- opaque native bootstrap;
- licensing/protection;
- vendor CTS / Play Store hacks by default;
- Accessibility-owned display polling;
- Projectivy Web Remote implementation.

## 7. Current effective Couchy/Aerial-Rows architecture

At the research baseline:

```text
MainActivity
  -> LauncherApp (very broad Compose root)
     -> ConfigStore / LauncherConfig
     -> AppRepository
     -> status/network/time
     -> wallpaper/Aerial player
     -> setup wizard
     -> layout selection
        -> Carousel / Grid / Dock
        -> SlidingRowsArea (injected by Gradle patch)
     -> SettingsSheet
```

Sliding Rows currently uses private row models:

```text
HomeRowModel
  - WatchNextHomeRow
  - ChannelHomeRow
  - CategoryHomeRow
```

The eventual target is a generic domain model outside the renderer.

## 8. Final target architecture

```text
Android platform callbacks/providers
        |
        +--> AppDiscoveryRepository / ShortcutRepository
        +--> TvChannelRepository / WatchNextRepository
        +--> InputRepository
        +--> NotificationRepository
        +--> MediaSessionRepository
        +--> ClockRepository / NetworkStatusRepository

                HomeRowsRepository
                       |
                       v
                 List<HomeRow>
                       |
                       v
                   Compose UI
                       |
                       v
                LaunchDispatcher
                       |
                       v
                   LaunchPolicy
                       |
        +--------------+------------------+
        |              |                  |
     App launch     Input launch      TV/media/etc.
```

Parallel runtime coordinators:

```text
WallpaperEngine
SettingsController
HomeFocusCoordinator
HomeRailController
StartupCoordinator
RemoteGestureEngine
LauncherIdleCoordinator
```

Optional integration boundaries:

```text
AccessibilityBridge
VendorCompatibilityRegistry
PictureBackendRegistry
ParentalPolicy
ExpertTools
```

## 9. Core dependency rule

Use this direction:

```text
UI
 -> feature/domain
 -> core interfaces/state
 -> Android/platform adapters
```

Vendor/root/accessibility implementations must remain optional adapters. Core repositories must not depend on them.

## 10. Final identity model

Do not use visible labels, list positions, row indices, or package name alone when stronger identity exists.

Target identities:

```text
AppId
  package + component + user/profile

RowId
  stable logical row key

HomeItemId
  typed semantic item identity

TvChannelId
  provider package + internal provider identity / fallback fingerprint

ProgramId
  provider + semantic program key

InputId
  platform TIF ID + optional compatibility metadata

ShortcutId
  package + shortcutId + user/profile
```

## 11. Launch architecture invariant

Every launcher-originated launch must eventually converge on:

```text
AppCard
TV Program
Now Playing
Input
Shortcut
Notification
RemoteAction
StartupCoordinator
        -> LaunchDispatcher
        -> LaunchPolicy
        -> platform-specific launch implementation
```

Initially use an `AllowAllLaunchPolicy`.

Later optional parental controls plug into `LaunchPolicy`; they must not require rewriting every surface.

## 12. Home architecture decisions

Keep Compose and `LazyColumn`/`LazyRow`.

Promote row/item models to domain types:

```text
HomeRow
  id: RowId
  title
  presentation: RowPresentation
  items: List<HomeItem>
```

Possible row sources:

- apps/category;
- Watch Next;
- TV Preview Channel;
- Now Playing;
- physical Inputs;
- shortcuts;
- folder/custom;
- empty/configure action.

Possible item types:

- App;
- TvProgram;
- NowPlaying;
- Input;
- Shortcut;
- EmptyRowAction;
- optional folder/custom.

Focus state must become:

```text
HomeFocusState
  activeRowId
  activeItemId
  lastItemByRow
```

Current first-card-only restoration is insufficient.

Rail state should not remain a single `railVisible: Boolean`; model reveal/focus ownership explicitly.

Reuse Couchy's existing custom BringIntoView pivot logic from Carousel/Grid inside Sliding Rows.

User-configurable empty rows should stay visible with an `EmptyRowAction`, not disappear and become impossible to edit.

## 13. C3 app/category findings that must not be forgotten

Couchy's app scan is one of its strongest parts:

- scans LEANBACK + normal launcher activities;
- deduplicates package entries with Leanback preference;
- bounded concurrency (`availableProcessors - 1`, minimum 2);
- memory cache;
- object reuse by package update stamp;
- target-size artwork cache;
- `prepareToDraw()`;
- stale cache sweep;
- precomputed glow/focus colors;
- same list instance returned when unchanged.

Keep this behavior.

Known current correctness issues:

1. Package-change stale-app hole: package update can remove its launcher activity, but current rescan logic can miss removal. Use `LauncherApps.Callback`.
2. `movePkg` loses row context. Same app in several rows means moving one occurrence can act on the wrong occurrence; duplicate cards can all think they are moving. Replace with `MovingItem(rowId,itemId)`.
3. Zero configured categories is reachable. Enforce at least one editable category in normalization.
4. All Apps should be a generated/system row, not the last structural editable category.
5. `AppEntry` discards exact component identity; launch can resolve to a different activity from the one whose label/art was selected. Use component-aware `AppId`.
6. Current hide semantics are global. Add row-local exclusions plus optional explicit Hide Everywhere.
7. New-install behavior needs an explicit policy rather than implicit first-category behavior.

Recommended row sort modes:

- manual/install;
- alphabetical;
- last used;
- most used;
- random.

Context UI currently has Open, Move, Hide/Unhide, App Info, Close, Uninstall. Keep the TV interaction/release suppression. Generalize into `HomeItemActionProvider` rather than growing an app-only menu signature forever.

`Close` is best-effort `killBackgroundProcesses`; keep it optional and label semantics honestly.

## 14. Android TV Channels / Watch Next decisions

Current `TvContentRepository` is a minimal snapshot reader.

Known gaps:

- filters non-browsable channels before settings/home can see them;
- therefore cannot expose channels that need `requestChannelBrowsable()`;
- drops channels with zero currently readable programs even in discovery/settings;
- no explicit Preview Program sort order;
- no explicit Watch Next recency/engagement sort;
- no Watch Next per-provider cap/exclusion;
- channel preference key is package + numeric database channel ID;
- no `internal_provider_id` logical identity;
- no ContentObserver;
- no resume safety refresh;
- program launch starts provider `intent_uri` directly rather than passing through LaunchPolicy / constraining ownership.

Target repositories:

```text
TvChannelRepository
PreviewProgramRepository
WatchNextRepository
TvArtworkRepository
```

Recommended channel identity:

```text
providerPackage + internalProviderId + fallbackFingerprint
```

Recommended program IDs prefer `content_id` or canonical intent semantics over raw provider row IDs.

Preview ordering should respect provider weight. Watch Next should prioritize recent engagement. Keep per-channel card limits; add per-provider Watch Next policy.

## 15. Wallpaper/Aerial decisions

Couchy's actual player is excellent and should be preserved:

- Media3/ExoPlayer;
- SurfaceView, not TextureView;
- small 10–15s buffer;
- first-rendered-frame gate;
- fade out old/fade in new;
- gradient loading/error cover;
- release on ON_STOP;
- rebuild on return;
- local video URI streaming;
- downsampled photo;
- tiny ~96px pseudo-blur for Dock.

Critical issue: current Aerial streaming client disables TLS certificate validation and hostname verification. Remove this. Use normal HTTPS validation; bad legacy endpoints should fail/disable rather than globally trust everything.

Current effective source model is split between legacy `builtinSource` in checked-in code and newer `builtinSources` injected by Gradle. Materialize `builtinSources`; keep old field only for migration decoding.

Current `BuiltinAerials` asset contains title/location + URL + source code, but code returns only URL strings. Preserve metadata with an `AerialItem`.

Target:

```text
WallpaperEngine
  ProviderRegistry
    PresetGradientProvider
    LocalPhotoProvider
    LocalVideoProvider
    BuiltinAerialProvider
  RotationScheduler
  FailureRegistry
  RendererRegistry
    ImageRenderer
    VideoRenderer
```

Use one shared video renderer for normal wallpaper, built-in Aerials, local video, and future immersive idle.

When volume is zero, disable audio track selection, not only `volume=0`.

Make decoder fallback/tunneling configuration explicit.

Replace random-with-immediate-repeat-guard with a shuffle bag.

Replace global `wallpaperVersion` / `aerialError` control with semantic `WallpaperEvent` + structured failures.

Local video persistent URI permission failures must not be silently ignored.

## 16. Settings architecture decisions

Keep Couchy's side-panel Compose TV UX and typed config.

Do not copy Projectivy's 148-key preference registry.

Add a descriptor layer:

```text
SettingId
SettingDescriptor<T>
  read/write typed config
  validation
  visibility/capability
  persistence policy
  runtime effect
  export policy
```

Separate:

```text
PersistencePolicy
  IMMEDIATE
  DEBOUNCED
  ON_CONFIRM
  SESSION_ONLY

SettingEffect
  NONE
  RECOMPOSE
  RELOAD_APPS
  RELOAD_TV_CONTENT
  RELOAD_WALLPAPER
  RECREATE_ACTIVITY
  RESTART_HOME_TASK
  REFRESH_ACCESSIBILITY
  ...
```

Known current issues:

- three different settings control/focus families already drift;
- main Settings focus retries, extras only request once, RowQuickEditor is less reliable;
- no stable SettingId focus restoration after returning from child page;
- rapid selectors write complete JSON config on every D-pad step;
- Save Config writes direct public Downloads File, swallows write failure, then always shows success;
- extra strings are partly hardcoded English;
- validation is scattered between UI, normalizer, and consumers.

Add `SettingsNavigator` with route stack + last focused SettingId per route.

For small ranges, Left/Right is fine. For named enumerations, OK should open a choice/dropdown-style panel focused on the current value.

Use session state for immediate preview and coalesced persistence. Process every semantic move/command; only coalesce disk snapshots.

## 17. Status, notifications, media decisions

Current status subsystem only tracks Wi-Fi/Ethernet/VPN and time/date. There is no notification or media subsystem in the baseline.

Improve status first:

- split transport from validated Internet state;
- maintain network capabilities incrementally rather than enumerating all networks on every callback;
- merge date/time minute loops into a `ClockRepository` and react to time/timezone/date broadcasts.

Add a thin `NotificationListenerService` bridge.

Keep separate domain repositories:

```text
NotificationRepository
MediaSessionRepository
```

Now Playing must come from active Android MediaSessions, not notification text.

Use platform `MediaSessionManager` / `MediaController` APIs; Media3 Session dependency is not necessary just for observation.

Now Playing becomes a normal `HomeRow`.

Multiple sessions may appear. Define a primary session separately for wallpaper/idle state.

Now Playing card opens should go through LaunchPolicy and inherit owning app/package protection if parental controls exist later.

Do not copy Projectivy behavior where dismissing a linked notification automatically stops/pauses playback; keep notification dismissal and transport controls separate by default.

## 18. Physical TV inputs decisions

At the research baseline there is no physical TV input subsystem in Couchy.

Do not confuse C4 TvProvider content with C8 physical inputs.

Add:

```text
InputRepository
TvInputManager.TvInputCallback
TvInputSource
InputId
InputHomeRow
TvInputActivity
TvView
InputLaunchCoordinator
CurrentInputState
```

Maintain separate concepts:

```text
DiscoveredInputs
AvailableInputs
VisibleInputShortcuts
```

Hiding an input shortcut must not make the input disappear from startup, remote mapping, or display-profile choices.

Do not synthesize individual missing HDMI holes when the platform provides a partial authoritative list. Compatibility synthesis is only appropriate when normal passthrough discovery is completely unavailable and a tested fallback exists.

Use standard TvInputManager/TvView path first. Vendor strategies are optional adapters.

Distinguish requested/launched/confirmed input state. An external vendor intent succeeding does not prove the hardware actually switched.

Optional display profiles are a later advanced feature. They must be independent of Accessibility and of visible shortcut filtering.

## 19. HOME/startup/remote/Accessibility/idle decisions

Keep normal Android HOME role as the primary launcher mechanism.

Current `MainActivity` is small and `singleTask`; keep it that way.

Extract a `HomeRoleCoordinator` for resolving/requesting HOME role and restart-as-home behavior.

Do not add a BOOT_COMPLETED receiver merely to start the launcher; Android starts the selected HOME activity.

Optional autostart should use one `StartupCoordinator` with typed triggers and targets:

- none;
- app;
- input;
- last requested input;
- last confirmed input.

Many lifecycle triggers must converge on one dispatcher rather than each launching independently.

Keep UI-local key handling in Compose:

- D-pad navigation;
- app-card long press;
- move mode;
- dialog navigation.

Add a separate `RemoteGestureEngine` for user-programmable single/double/long global actions.

Accessibility is optional. Core launcher use must not require it. If added, it is only a bridge for capabilities Android otherwise cannot provide reliably, such as global key filtering/HOME fallback on specific devices.

Do not put vendor hacks, input polling, display profile logic, or startup policy inside Accessibility.

Idle should use a `LauncherIdleCoordinator`. Preferred first idle mode is immersive wallpaper using the existing shared video renderer, not a second player.

## 20. Persistence / backup / themes / icons / shortcuts / parental decisions

Current persistence is one `LauncherConfig` serialized to JSON inside Preferences DataStore.

Keep the typed config concept.

Known critical bugs:

1. First-run localization mismatch: read path uses localized default category names; first update falls back to plain `LauncherConfig()` with English names and can persist English on a non-English device.
2. Malformed inner JSON silently becomes defaults; next update can overwrite the only damaged copy.
3. Current Save Config is not a real backup; it serializes runtime config only.
4. Custom photo wallpaper file is not inside the backup.
5. Local video `content://` URI is not portable across install/device.
6. Backup serializes fields such as `knownApps` and `setupDone` that restore deliberately replaces.

Add:

```text
ConfigDefaults
ConfigEnvelope(schemaVersion,...)
MigrationRegistry
ConfigNormalizer
LastKnownGoodStore
BackupEngine
ThemeEngine
IconRepository
ShortcutRepository
SecureStateRepository (only if needed)
```

Distinguish:

```text
Android Auto Backup / D2D
manual .aerialbackup
appearance .aerialtheme
internal last-known-good rollback
```

Do not export caches.

Full backup should use a portable DTO, eventually ZIP + manifest + assets, with transactional restore:

```text
verify -> decode -> migrate -> normalize -> stage assets -> commit -> one runtime rehydrate
```

Theme contains appearance only and optional appearance assets, not app membership/device bindings/security state.

App artwork cache remains regenerable.

Icon packs are currently absent. Add an `IconRepository`; ADW-compatible icon pack discovery/parsing is a reasonable compatibility target. Prefer good Leanback banners by default because TV banners are 16:9 while icon packs are usually square. Add per-item ArtworkPolicy.

Android shortcuts are currently absent. Add `ShortcutRepository` using stable package + shortcutId + user identity, callback observation, launch, pin/unpin, and optional external pin request intake. Restored missing shortcuts should remain unresolved references and resolve later if republished.

Parental controls are optional later. If implemented, integrate through LaunchPolicy, keep credentials in a secure separate store, never plaintext, never theme-exported, and never owned by Accessibility.

## 21. Performance/reliability invariants

These are mandatory review principles:

1. Build never mutates tracked source.
2. Release keeps R8 minification + resource shrinking.
3. D-pad focus must not trigger disk/network/package-provider IO.
4. Heavy artwork decode stays off main thread.
5. Unchanged semantic items preserve stable identity/object identity where practical.
6. Lazy items have stable keys.
7. One wallpaper/idle video renderer only.
8. Player/decoder is released when launcher no longer needs it.
9. Muted video should disable audio track.
10. Callback/observer-driven state is preferred over polling.
11. Polling is capability-gated and lifecycle-bounded.
12. Disabled optional feature means expensive background work is also disabled.
13. Rapid input must not enqueue unbounded DataStore work.
14. Cache is never treated as durable user data.
15. Corrupt state is not silently overwritten by defaults.
16. No trust-all TLS.
17. Failures are contained but diagnosable.
18. Vendor/privileged adapters are isolated and optional.

C11 found one concrete app-artwork cache correctness bug: disk cache key includes rendered size, but in-memory `AppEntry` reuse checks only package `lastUpdateTime`. Add `ArtworkVariant(bannerWidth, iconSize, cacheSchemaVersion)` and require stamp + variant match before reusing artwork.

C11 also found that fresh process startup waits for the complete app scan + artwork work before the real HOME becomes interactive. This is a tradeoff, not an automatic bug. Benchmark first. If needed later, add a persisted metadata catalogue and staged reconciliation. Do not complicate startup before measurements justify it.

Likewise, full app-artwork memory residency is a speed-vs-RAM tradeoff. Measure on low-memory/app-heavy TVs before switching to a bounded RAM LRU.

## 22. Testing / benchmark plan

At the baseline there is no `test` or `androidTest` source set and no Macrobenchmark harness. CI builds debug/release and runs lint.

Phase 0B should add:

- unit-test source set;
- Android/instrumented tests where needed;
- Macrobenchmark/baseline profile generation infrastructure.

Record the current baseline before big refactors:

- cold process HOME;
- warm HOME;
- time to first focusable card;
- horizontal row navigation frame timing;
- vertical row navigation frame timing;
- settings open/close;
- rapid settings interaction;
- Aerial playback + navigation;
- memory with representative app counts (20/50/100+ if possible).

Re-run performance gates after major phases, especially 0, 2, 4, 5, 7, 9, 10.

## 23. Final implementation phase order

### Phase 0A — source-pure baseline

- materialize all Gradle-injected code/resources;
- remove `applyAerialRowsSourcePatches`;
- clean checkout build must leave Git tree unchanged;
- build debug + release + lint.

### Phase 0B — measurement/test baseline

- unit tests;
- instrumentation/macrobenchmark infrastructure;
- generated baseline-profile path;
- record current performance/memory baseline.

### Phase 1 — config safety + IDs + launch core

- `ConfigDefaults`;
- corruption recovery;
- zero-category invariant;
- schemaVersion;
- migrations;
- last-known-good;
- ArtworkVariant cache fix;
- AppId / RowId / HomeItemId;
- LaunchDispatcher + AllowAllLaunchPolicy.

### Phase 2 — generic home model/focus

- HomeRow / HomeItem;
- RowPresentation;
- HomeRowsRepository;
- HomeFocusCoordinator;
- HomeRailController;
- exact row/card restore;
- empty configurable row card;
- reuse Couchy pivot scrolling.

### Phase 3 — apps/categories/actions

- component-aware AppId;
- LauncherApps callback;
- move bug fix;
- row-local hide/exclusions;
- category referential integrity;
- sorting/new-install policies;
- HomeItemActionProvider;
- all app launches through dispatcher.

### Phase 4 — complete Android TV provider support

- logical channel/program IDs;
- non-browsable discovery;
- request channel browsable;
- ordering;
- Watch Next provider caps/exclusions;
- observer/debounce/resume refresh;
- constrained launch;
- TV artwork repo.

### Phase 5 — wallpaper/Aerial engine

- remove trust-all transport first;
- typed WallpaperSelection;
- provider/renderer separation;
- shared VideoRenderer;
- audio track disable;
- optional tunneling/fallback controls;
- scheduler/failure registry/shuffle bag;
- robust local media handling.

### Phase 6 — settings architecture

- stable SettingId;
- route stack/focus restoration;
- descriptors/effects;
- unified controls;
- choice/dropdown UI;
- session state/coalesced persistence;
- truthful SAF backup export.

### Phase 7 — notifications/media/status

- ClockRepository;
- NetworkStatusRepository;
- NotificationListener bridge;
- NotificationRepository/Panel;
- MediaSessionRepository;
- Now Playing row/card/actions;
- wallpaper + idle signals.

### Phase 8 — physical TV inputs

- InputRepository + TvInputManager callbacks;
- TvInputActivity/TvView;
- InputLaunchCoordinator;
- current requested/confirmed state;
- Inputs home row/settings;
- standard platform path first.

### Phase 9 — HOME/startup/remotes/idle

- HomeRoleCoordinator;
- StartupCoordinator;
- RemoteGestureEngine;
- LauncherIdleCoordinator;
- optional AccessibilityBridge only where needed;
- vendor adapters later and isolated.

### Phase 10 — backup/themes/icons/shortcuts

- asset-aware transactional backup;
- themes;
- icon packs/custom artwork;
- shortcuts query/pin/launch/external request;
- unresolved-reference restore behavior.

### Phase 11 — optional advanced parity

- parental controls;
- display profiles;
- vendor input adapters;
- optional Accessibility global mappings;
- external wallpaper providers;
- diagnostics/expert tools.

## 24. Recommended first ten implementation commits / PRs

1. Materialize Gradle-injected launcher/settings/resources.
2. Remove source-rewriting task and add source-tree-cleanliness CI.
3. Add test skeleton + benchmark/profile harness.
4. Fix ConfigDefaults/corruption/category invariants.
5. Add schemaVersion + LastKnownGoodStore + MigrationRegistry.
6. Add ArtworkVariant cache-identity fix.
7. Add AppId / RowId / HomeItemId.
8. Add LaunchDispatcher + AllowAllLaunchPolicy.
9. Promote private Sliding Rows models to generic HomeRow/HomeItem domain models.
10. Add HomeFocusCoordinator + exact stable row/card restoration.

## 25. Projectivy APK research provenance

Projectivy target audited:

- package: `com.spocky.projengmenu`
- version: 4.72
- version code: 97
- minSdk: 23
- targetSdk: 37
- class count observed: 7395
- resource entries observed: 1171
- DEX files: 3
- native library: `libptutils.so` for all four common Android ABIs
- main Activity: `com.spocky.projengmenu.ui.home.MainActivity`

APK SHA-256:

`a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

Signing certificate SHA-256:

`f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`

Signer subject included `CN=Despesse Mickael`.

Do not add the APK to the repo. These hashes are here only to preserve research provenance.

## 26. Reverse-engineering tooling provenance

The Projectivy analysis used:

- `jadx-headless-mcp-0.7.1-all.jar`
- a locally prepared REA 6.7.0 environment;
- Java 21;
- Node 22.20.0;
- persistent JADX MCP daemon;
- complete 7395-class inventory;
- separately pinned 1171-entry resource inventory.

Historical working paths in the original analysis environment included:

- `/mnt/data/rea-portable`
- `/mnt/data/rea` symlink
- `/mnt/data/launcherproject_work/`

Those paths are not expected to exist on a future machine. The durable outputs are the reports stored under `docs/ai-handoff/`.

## 27. Audit process / evidence standard

The Projectivy audit was repeatedly hardened to avoid false closure and timeouts.

Final process principles included:

- small self-contained groups;
- semantic-first discovery;
- REA/JADX canonical evidence for high-risk claims;
- persistent daemon, not repeated JVM startup;
- full class inventory + separately pinned resource inventory;
- resumable bounded probe batches;
- write-ahead `in_progress` checkpoint before slow query;
- interrupted attempt recovery;
- atomic state/evidence/report writes;
- failed-probe quarantine;
- exact query/cache handling;
- FTS dotted/literal fallback;
- named-class prevalidation;
- no empty-query REA as an exhaustive search;
- no aggressive parallel JVMs;
- no claiming closure from lightweight resource mode alone;
- stop/report after each group.

Closure dimensions used:

1. entry points
2. base classes/inheritance
3. all DEX files
4. obfuscated collaborators
5. models/data flow
6. persistence
7. resources/XML
8. Android framework APIs
9. callbacks/observers
10. launch/actions/lifecycle
11. native/JNI

Final process contract reference from the original research environment:

- contract v18 SHA: `052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`
- process bundle SHA: `251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

The old process hardening documents are preserved under `docs/ai-handoff/PROCESS_FULL.md`.

## 28. High-value Projectivy findings not obvious from the final phase list

### Home

Projectivy home chain reconstructed as:

```text
MainActivity
 -> MainFragment
 -> customized Leanback BrowseSupportFragment
 -> rows adapter
 -> row wrapper
 -> category model + item adapter
 -> row presenter selector
 -> card presenter selector
```

Relevant behavior, not implementation, should be retained:

- header rail has shown / hidden-revealable / disabled semantics;
- browse layer saves selected row/header/page/title state;
- stable category ID + card ID refocus;
- user-configured empty row uses a real placeholder action card;
- row/card movement refocuses by stable IDs;
- selected/nonselected row fading;
- hover/details surface for selected item.

### Startup / remote

Projectivy verified concepts:

- shared startup dispatcher;
- duplicate debounce around 15s;
- boot validity window around 90s;
- persisted last-used input;
- HOME restart can use `Intent.makeRestartActivityTask(...)+CATEGORY_HOME`;
- remote long/double recognition around 300/400ms;
- temporary hotkey mode around 3s;
- HOME double can map to Recents;
- direct HDMI vendor key codes observed (243–246);
- idle restore used a short delay (~300ms).

Do not copy timing blindly; use these as behavioral references.

### Vendor/privileged boundaries

Later audits found compatibility behavior such as Xiaomi startup preconditioning, Play Store CTS property toggling, Xiaomi updater workaround, root package commands, Play self-update immutable APK hack, GApps uninstall script. These are optional/expert/device-specific and should not be part of Aerial-Rows core architecture.

### Native Group 11 conclusion

No hidden architectural engine was found behind the Projectivy obfuscated/native layer. Native `libptutils.so` is a narrow optional path, not a reason to reproduce a native bootstrap in Aerial-Rows.

## 29. User/project workflow preferences

For this project, the user consistently prefers:

- practical exact continuation rather than vague advice;
- small durable chunks with a report/checkpoint after each chunk;
- exhaustive understanding before large implementation steps;
- no repeated questions when context already contains the answer;
- verify actual current source/tool behavior rather than trusting old notes;
- preserve process fixes after interruptions/timeouts;
- compare alternatives explicitly (KEEP / EXTEND / REPLACE / ADD);
- avoid large risky one-shot mutations;
- build reproducibly from checked-in source.

Future AI agents should therefore:

1. read this file + C12 before coding;
2. verify live branch before edits;
3. make bounded commits;
4. run the relevant build/tests after each architectural change;
5. update this handoff when architectural decisions change.

## 30. Where the detailed evidence lives

Read in this order:

1. `docs/ai-handoff/C12_FINAL_BLUEPRINT.md` — definitive synthesis and implementation blueprint.
2. `docs/ai-handoff/CHAT_EXPORT.md` — chronological research/project reconstruction.
3. Search the relevant C1–C11 section inside `docs/ai-handoff/COUCHY_C1_C12_FULL.md` for the subsystem being changed.
4. Search the matching Projectivy group and post-closure section inside `docs/ai-handoff/PROJECTIVY_G1_G12_FULL.md` for behavioral details.
5. `docs/ai-handoff/PROCESS_FULL.md` if reopening reverse-engineering/tooling work.

The full detailed reports are intentionally stored rather than summarized away.

## 31. Do not silently invalidate the handoff

When implementation changes the architecture materially:

- update the relevant C/implementation note or add a successor note;
- update `AI_PROJECT_CONTEXT.md` current branch/phase;
- record migrations/identity changes;
- record new performance baselines when they become authoritative.

This file describes the state at the end of research on **2026-10-23** and should be treated as the starting specification for implementation, not immutable truth after later code changes.
