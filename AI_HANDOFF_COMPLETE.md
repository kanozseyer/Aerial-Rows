# Aerial-Rows — Complete AI Handoff Export

> Generated 2026-10-23. This single file is designed to let a fresh AI model continue the Launcher/Aerial-Rows project without the original conversation.

---

# SECTION A — Agent Instructions
# AGENTS.md — Aerial-Rows

Before making architectural or feature changes, read:

1. `AI_PROJECT_CONTEXT.md`
2. `docs/ai-handoff/C12_FINAL_BLUEPRINT.md`
3. the relevant C1–C11 section in `docs/ai-handoff/COUCHY_C1_C12_FULL.md`
4. the matching Projectivy behavioral audit in `docs/ai-handoff/PROJECTIVY_G1_G12_FULL.md` when needed.

The project is a Couchy-derived Compose Android TV launcher. Preserve Couchy's lightweight hot paths and independently reimplement useful Projectivy behavior behind clean interfaces.

Immediate implementation phase at the end of the research handoff: **Phase 0A — materialize the Gradle-injected source changes and remove `applyAerialRowsSourcePatches` without changing behavior.**

Important rules:

- Do not copy proprietary Projectivy decompiled source or licensing/protection code.
- Do not upload proprietary Projectivy APK/JADX/REA binaries to the repository.
- Build must not mutate tracked source after Phase 0A.
- Core launcher functionality must not require Accessibility, vendor hacks, or root.
- All launcher-originated launches should converge on `LaunchDispatcher -> LaunchPolicy` once Phase 1 lands.
- Use stable semantic IDs, not list indices or labels.
- Preserve bounded app scanning, artwork caching/object reuse, SurfaceView wallpaper, small Media3 buffers, decoder release on stop, and other performance invariants documented in C11.
- Optional feature disabled should mean its expensive background work is disabled.
- Work in bounded commits and keep the handoff updated when major decisions change.

---

# SECTION B — Current Project Context
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

---

# SECTION C — Final Architecture Blueprint
# Couchy ↔ Projectivy — C12 Final Synthesis & Aerial-Rows Implementation Blueprint

Status: **COMPLETE + REVIEWED**  
Repository: `kanozseyer/Aerial-Rows`  
Implementation branch: `aerialrows-development`  
Pinned/live commit verified for C12: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Pinned tree: `457f4ab1d08a8d545bdf5545777cd31f907ae24d`  
Projectivy comparison basis: completed Projectivy Groups 1–12 including post-closure corrections through Group 12 v2.

---

# 1. Executive verdict

After studying both implementations subsystem-by-subsystem, the direction is now clear.

**Aerial-Rows should remain a Couchy-derived Compose launcher.**

Projectivy should be treated as the **behavioral and subsystem-architecture reference**, not as the implementation base.

The winning combination is:

```text
Couchy
    ├─ Compose rendering
    ├─ small MainActivity
    ├─ typed immutable config
    ├─ efficient app scan/artwork cache
    ├─ TV-friendly D-pad UI
    ├─ SurfaceView/Media3 wallpaper implementation
    └─ generally low background overhead

        +

Projectivy
    ├─ mature row/focus semantics
    ├─ launch/policy boundary
    ├─ Android TV provider behavior
    ├─ wallpaper provider/renderer separation
    ├─ settings metadata/update policies
    ├─ media + notification architecture
    ├─ input/startup/remote architecture
    ├─ migrations/backups/themes/icons/shortcuts
    └─ optional device/parental capabilities

        -

Couchy weaknesses
    ├─ build-time source rewriting
    ├─ package-only identities
    ├─ UI-owned persistence/effects
    ├─ snapshot-only TV content
    ├─ trust-all Aerial TLS
    ├─ raw-config backup
    └─ missing lifecycle/domain services

        -

Projectivy weaknesses
    ├─ Leanback-specific implementation
    ├─ Accessibility becoming an ownership hub
    ├─ vendor/root helpers near core runtime
    ├─ polling/cached input weaknesses
    ├─ preference-heavy internals
    ├─ opaque native/bootstrap paths
    └─ inconsistent policy coverage
```

This should **not** become a literal Projectivy clone internally.

It should become:

> A modern Compose implementation of the useful Projectivy behaviors, retaining Couchy's lighter and faster execution model.

---

# 2. Source baseline is still valid

The C1–C11 Couchy comparison was performed against:

```text
aerialrows-development
3c1cfd56234f1d386f4553dd6c69523670500d70
```

C12 reverified that the live branch still points to the same commit.

Therefore:

- none of C1–C11 needs to be repeated because of source drift;
- the source-specific recommendations remain current;
- implementation can begin directly from this branch;
- there is no reason to restart again from Couchy `main`;
- there is no reason to attempt a Projectivy source transplant.

---

# 3. C1–C11 evidence set

C12 synthesizes these completed comparison reports:

```text
C1  Outer architecture / build / lifecycle root
C2  Home rows / focus / rail
C3  Apps / categories / actions
C4  Android TV Channels + Watch Next
C5  Wallpaper + Aerial
C6  Settings
C7  Status / notifications / media
C8  TV inputs / device integration
C9  HOME / startup / remote / Accessibility / idle
C10 Persistence / backups / themes / icons / shortcuts / parental
C11 Performance / reliability
```

Report SHA-256 values:

```text
C1  c85c2188906a849854865fc549e1f62c697899d7a648d05fb3f1fd6954e88cea
C2  2c9732f438cdfc075239ba4c06754821d09846e4d706fbf7dc181dc480e0e381
C3  c756b965caa99ac016eb312a9828be05cfcd6618d666c5b41b1ca333c6761f03
C4  935945ec232fe56e6e64e2112eb19179a3297e6194594234d239b9cde5ebd297
C5  e7603080accb1bdb90cf2672048f647276671649d3cfc61c4712130950bd4b78
C6  0bf49b7fed0fa4fee3abe2c86f133c3f62fb1eba708821c4b2cebf12cba37b18
C7  3947a7627f2fed66747704557066a7c81894f94399f12116370342663c94be3c
C8  1b5d4ac78301cc6a635d9554cdde44ae61e607fc8857044c391779ed18f5019b
C9  1234bd850995ccecefa08d8c78316de1da403f439e2f0e898f3b70540141be2c
C10 e08ee8ae66118f595cba576834cb3b95f70827508729395a4d2a05b31a32be54
C11 20c18dd880f8ea15ce0ffa92aa08c9c5fcc38bb686897e9eb6b46342e14dc5f1
```

---

# 4. Final subsystem decision matrix

## C1 — outer architecture

### KEEP COUCHY

- Kotlin + Compose;
- single small HOME Activity;
- typed immutable `LauncherConfig`;
- DataStore;
- performance-conscious app scanner;
- central low-level `Actions` helper as a platform adapter.

### EXTEND

- explicit repositories/coordinators;
- typed capability layer;
- `LaunchDispatcher`;
- `LaunchPolicy`.

### DO NOT COPY FROM PROJECTIVY

- a large fragment/Leanback shell;
- large Activity/component ownership;
- native bootstrap as a launcher prerequisite.

---

## C2 — home screen

### KEEP COUCHY

- Compose;
- `LazyColumn` / `LazyRow`;
- stable row keys;
- AppCard rendering;
- focus glow/scale;
- D-pad-first navigation;
- hidden-status-bar focus strip;
- existing pivot scrolling technique from Couchy's older layout code.

### EXTEND WITH PROJECTIVY SEMANTICS

- generic `HomeRow`;
- generic `HomeItem`;
- stable `RowId`;
- stable `HomeItemId`;
- per-row last-focused item;
- rail shown/hidden/focused state;
- explicit rail/content focus ownership;
- configurable empty-row action card;
- per-row presentation overrides.

### REPLACE

```text
activeRowId only
first-card restoration
railVisible Boolean
```

with:

```text
HomeFocusState(
    activeRowId,
    activeItemId,
    lastItemByRow
)

HomeRailState
```

---

## C3 — apps/categories/actions

### KEEP COUCHY

- dual Leanback/launcher discovery;
- bounded scan parallelism;
- immutable `AppEntry` reuse;
- artwork WebP cache;
- pre-rasterization;
- focus color extraction;
- multi-section membership;
- category UI;
- long-press release suppression;
- optional best-effort Close action.

### FIX / REPLACE

- package-only app identity;
- `movePkg`;
- global-only Hide semantics;
- Activity-owned package observation;
- zero-category state;
- generated All Apps treated like a normal structural category;
- implicit new-install behavior.

### ADD

```text
AppId(component-aware)
MovingItem(rowId,itemId)
LauncherApps.Callback
AppPlacementPolicy
RowSortMode
NewInstallPolicy
HomeItemActionProvider
LaunchDispatcher
LaunchPolicy
```

---

## C4 — Android TV Channels / Watch Next

### KEEP COUCHY

- Compose presentation;
- immutable TV-content models;
- progress bar;
- per-channel limits;
- Watch Next/Preview visibility;
- row editor integration.

### REPLACE / EXTEND

- `packageName:numericChannelId` identity;
- browsable-only discovery;
- empty-channel omission;
- unspecified program ordering;
- direct provider-intent launch;
- UI-owned snapshot refresh.

### ADD

```text
TvChannelIdentity
ProgramId
TvChannelRepository
PreviewProgramRepository
WatchNextRepository
ContentObserver
resume refresh
provider cap/exclusion
requestChannelBrowsable()
provider-constrained LaunchDispatcher
TvArtworkRepository
```

Important:

```text
TV provider content
    ≠
physical TV input
```

---

## C5 — wallpaper/Aerial

### KEEP COUCHY

- Media3 / ExoPlayer;
- `SurfaceView`;
- player release on `ON_STOP`;
- rebuild on return;
- small buffer policy;
- first-rendered-frame gate;
- fade transition;
- gradient fallback;
- static wallpaper downsampling;
- tiny pseudo-blur;
- one-player philosophy;
- bundled Aerial catalogue;
- multi-source collection concept.

### REMOVE

- trust-all X509 manager;
- always-true hostname verifier;
- legacy runtime `builtinSource`;
- `wallpaperVersion++` as the final event architecture;
- global `aerialError`;
- anonymous URL-only playlist;
- always-enabled decoder fallback;
- ignored persistent-URI failures.

### ADD / EXTEND

```text
WallpaperEngine
WallpaperProvider
WallpaperDescriptor
BuiltinAerialProvider
VideoRenderer
ImageRenderer
WallpaperEvent
WallpaperRotationScheduler
FailureRegistry
shuffle bag
audio-track disable at volume 0
optional tunneling
decoder-fallback policy
optional dynamic palette sampling
```

Core invariant:

```text
one wallpaper/idle video renderer
```

---

## C6 — settings

### KEEP COUCHY

- side-panel Compose UX;
- D-pad interaction;
- typed config;
- DataStore;
- immediate visual previews;
- text rows without focus scaling;
- separate text-entry dialogs;
- full-height right-side panel.

### REPLACE

- every row directly calling `store.update { copy(...) }`;
- first-item-only page focus;
- multiple duplicate setting-row implementations;
- long-lived `SettingsScreen` enum as the final navigation model;
- direct Downloads File backup;
- hard-coded English extras.

### ADD

```text
SettingId
SettingDescriptor<T>
SettingsNavigator
SettingsController
SettingsSessionState
PersistencePolicy
SettingEffect
ExportPolicy
shared setting controls
choice/dropdown dialog
stable route focus restoration
```

---

## C7 — status/media/notifications

### KEEP COUCHY

- current status-bar Compose surface;
- VPN/network/settings interaction;
- hidden-status-bar focus/reveal strip.

### IMPROVE

```text
NetworkStatusRepository
ClockRepository
```

rather than:

```text
allNetworks enumeration on every callback
two separate minute loops
```

### ADD

```text
LauncherNotificationListenerService
NotificationRepository
NotificationPolicy
NotificationPanel
MediaSessionRepository
NowPlayingHomeRow
NowPlayingCard
```

Important:

```text
notification state
    ≠
media-session state
```

and:

```text
Now Playing launches
    → LaunchDispatcher
    → LaunchPolicy
```

---

## C8 — TV inputs/device integration

Couchy currently has effectively no subsystem here.

### ADD

```text
InputRepository
TvInputManager.TvInputCallback
TvInputSource
InputId
AvailableInputs
VisibleInputShortcuts
InputConnectionState
InputHomeRow
TvInputActivity
TvView
InputLaunchCoordinator
CurrentInputState
```

### OPTIONAL

```text
VendorInputStrategy
DisplayProfileRepository
PictureBackendRegistry
DisplayStateRepository
DisplayProfileCoordinator
```

Important invariants:

```text
available input
    ≠
visible shortcut

requested input
    ≠
confirmed physical input

input discovery
    ≠
Accessibility

input launch
    ≠
display-profile automation
```

---

## C9 — HOME/startup/remote/Accessibility/idle

### KEEP COUCHY

- normal Android HOME Activity;
- `singleTask`;
- root Back behavior;
- RoleManager setup;
- small `MainActivity`;
- UI-local AppCard/move/D-pad behavior;
- MENU Settings safety path.

### ADD

```text
HomeRoleCoordinator
StartupCoordinator
RemoteGestureEngine
LauncherIdleCoordinator
PowerStateMonitor
```

### OPTIONAL

```text
AccessibilityBridge
VendorCompatibilityAdapters
ExternalScreensaverStrategy
```

Important:

```text
HOME role
    ≠
Accessibility

startup policy
    ≠
boot receiver

UI keys
    ≠
global remote mappings

idle mode
    ≠
autostart
```

---

## C10 — persistence/backups/themes/icons/shortcuts/parental

### KEEP COUCHY

- typed immutable config;
- DataStore;
- app artwork cache.

### FIX FIRST

- localized default factory mismatch;
- silent JSON corruption fallback;
- zero-category invariant;
- backup success reporting;
- raw runtime config exported as backup;
- missing wallpaper asset packaging.

### ADD

```text
schemaVersion
MigrationRegistry
ConfigNormalizer
LastKnownGoodStore
BackupEngine
ThemeEngine
IconRepository
ShortcutRepository
SecureStateRepository
```

### OPTIONAL

```text
ParentalPolicy
ParentalAuthSession
```

Important:

```text
runtime config
    ≠
public backup schema

backup
    ≠
theme

cache
    ≠
user data

parental state
    ≠
Accessibility state
```

---

## C11 — performance/reliability

### KEEP AS INVARIANTS

- bounded app scan;
- unchanged-object reuse;
- same-list reuse;
- target-size artwork;
- `prepareToDraw()`;
- stable Compose IDs;
- SurfaceView;
- trimmed Media3 buffer;
- decoder release on stop;
- pseudo-blur;
- release R8/shrink;
- low service count.

### FIX

- Gradle source rewriting;
- artwork memory-reuse variant key;
- rapid DataStore command queueing;
- UI-owned platform observation;
- broad hand-authored baseline profile;
- missing tests/benchmarks;
- silent `runCatching` as primary error semantics.

### MEASURE BEFORE CHANGING

- full app-artwork RAM residency;
- cold-start full-scan gate;
- squircle/path cost;
- panel brush allocation.

---

# 5. Definitive high-level architecture

```text
                        Android platform
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
  Package/LauncherApps     TvProvider          TvInputManager
         │                    │                    │
         ▼                    ▼                    ▼
 AppDiscoveryRepo      TvContentRepos        InputRepository
         │                    │                    │
         └─────────────┬──────┴───────┬────────────┘
                       │              │
                       ▼              ▼
                HomeRowsRepository   Runtime state repos
                       │
                       ▼
                    Home UI
                       │
        ┌──────────────┼────────────────┐
        │              │                │
        ▼              ▼                ▼
      Apps         Programs          Inputs
        │              │                │
        └──────────────┴────────────────┘
                       │
                       ▼
                 LaunchDispatcher
                       │
                       ▼
                  LaunchPolicy
                       │
        ┌──────────────┼──────────────────────┐
        ▼              ▼                      ▼
   AppLauncher    InputCoordinator      Other Launchers
```

Parallel runtime services:

```text
WallpaperEngine
NotificationRepository
MediaSessionRepository
ClockRepository
NetworkStatusRepository
StartupCoordinator
RemoteGestureEngine
LauncherIdleCoordinator
```

Optional boundaries:

```text
AccessibilityBridge
VendorCompatibilityRegistry
PictureBackendRegistry
ParentalPolicy
OptionalExpertTools
```

---

# 6. Core dependency rule

The architecture should enforce this dependency direction:

```text
UI
 ↓
feature/domain
 ↓
core
 ↓
platform adapters
```

Optional/vendor code must not point back upward into core ownership.

Preferred conceptual dependency:

```text
core
    does not know Xiaomi/Amlogic/root

feature
    depends on interfaces

integration/vendor
    implements those interfaces
```

This is the most important structural improvement over Projectivy.

---

# 7. Final package structure

Keep one Gradle application module initially.

Do **not** prematurely create many Gradle modules.

Use package boundaries first:

```text
com.conreo.couchytv/

  core/
    model/
      AppId.kt
      HomeItemId.kt
      RowId.kt
      InputId.kt
      ShortcutId.kt

    config/
      LauncherConfig.kt
      ConfigRepository.kt
      ConfigNormalizer.kt
      ConfigDefaults.kt
      ConfigEnvelope.kt

    migration/
      Migration.kt
      MigrationRegistry.kt
      LastKnownGoodStore.kt

    launch/
      LaunchTarget.kt
      LaunchRequest.kt
      LaunchDispatcher.kt
      LaunchPolicy.kt
      AllowAllLaunchPolicy.kt

    capability/
      DeviceCapabilities.kt

    events/
      LauncherEvent.kt

    diagnostics/
      DiagnosticsRepository.kt


  feature/

    home/
      HomeRow.kt
      HomeItem.kt
      HomeRowsRepository.kt
      HomeFocusCoordinator.kt
      HomeRailController.kt
      RowPresentation.kt

    apps/
      AppDiscoveryRepository.kt
      AppEntry.kt
      PackageObserver.kt
      AppClassifier.kt
      AppPlacementPolicy.kt
      AppArtworkCache.kt

    actions/
      HomeItemActionProvider.kt

    tvcontent/
      TvChannelRepository.kt
      PreviewProgramRepository.kt
      WatchNextRepository.kt
      TvArtworkRepository.kt

    wallpaper/
      WallpaperEngine.kt
      WallpaperProvider.kt
      WallpaperDescriptor.kt
      WallpaperRotationScheduler.kt
      FailureRegistry.kt
      renderer/
        ImageRenderer.kt
        VideoRenderer.kt

    aerials/
      BuiltinAerialRepository.kt
      BuiltinAerialProvider.kt
      AerialItem.kt

    settings/
      SettingId.kt
      SettingDescriptor.kt
      SettingsNavigator.kt
      SettingsController.kt
      SettingEffectExecutor.kt

    status/
      ClockRepository.kt
      NetworkStatusRepository.kt

    notifications/
      NotificationRepository.kt
      NotificationPolicy.kt

    media/
      MediaSessionRepository.kt

    inputs/
      InputRepository.kt
      InputLaunchCoordinator.kt
      CurrentInputState.kt

    startup/
      HomeRoleCoordinator.kt
      StartupCoordinator.kt

    remote/
      RemoteGestureEngine.kt

    idle/
      LauncherIdleCoordinator.kt

    backup/
      BackupEngine.kt
      BackupManifest.kt

    themes/
      ThemeEngine.kt

    icons/
      IconRepository.kt
      IconPackParser.kt

    shortcuts/
      ShortcutRepository.kt


  integration/

    android/
      AppLauncher.kt
      TvInputActivity.kt
      NotificationListenerBridge.kt

    vendor/
      VendorCompatibilityRegistry.kt
      xiaomi/
      amlogic/
      mediatek/

    accessibility/
      AccessibilityBridge.kt


  optional/

    parental/
      ParentalPolicy.kt
      SecureStateRepository.kt

    displayprofiles/
      DisplayProfileRepository.kt
      DisplayStateRepository.kt
      DisplayProfileCoordinator.kt

    experttools/
      DiagnosticsScreen.kt
      PackageMaintenance.kt


  ui/
    home/
    settings/
    cards/
    components/
    theme/
```

This is a target structure, not a requirement to create empty files/packages up front.

Create classes only as their phase lands.

---

# 8. Final identity model

Identity mistakes were one of the recurring Couchy weaknesses.

Use explicit semantic IDs.

```text
AppId
    package + component + user/profile

RowId
    stable logical row key

HomeItemId
    typed stable item identity

TvChannelId
    provider package + internal provider identity/fallback

ProgramId
    provider + semantic program key

InputId
    platform TIF ID + optional compatibility metadata

ShortcutId
    package + shortcut ID + user/profile
```

Do not persist:

```text
list index
visible label
row position
array ordinal
random generated ID when platform semantic identity exists
```

---

# 9. Final HomeRow model

```kotlin
sealed interface HomeRow {
    val id: RowId
    val title: String
    val presentation: RowPresentation
    val items: List<HomeItem>
}
```

Possible sources:

```text
Apps
WatchNext
TvChannel
NowPlaying
Inputs
Shortcuts
Folder
ConfigureRow
```

`Inputs` and TV-provider/Live-TV-style content remain conceptually separate.

---

# 10. Final HomeItem model

```text
HomeItem
├─ App
├─ TvProgram
├─ NowPlaying
├─ Input
├─ Shortcut
├─ EmptyRowAction
└─ optional Folder/Custom
```

Every card renderer should work from a semantic domain item, not from a source-specific Android platform object.

---

# 11. Final launch architecture

Every launcher-originated launch converges here:

```text
AppCard
TvProgramCard
NowPlayingCard
InputCard
ShortcutCard
RemoteAction
StartupCoordinator
Notification open
        ↓
LaunchDispatcher
        ↓
LaunchPolicy
        ↓
specific platform launcher
```

Initial policy:

```text
AllowAllLaunchPolicy
```

Later optional:

```text
ParentalPolicy
```

This means parental support does **not** require rewriting every launch surface.

---

# 12. Final configuration architecture

Keep one typed root envelope:

```text
ConfigEnvelope(
    schemaVersion,
    launcherConfig
)
```

As it grows, make fields domain-oriented:

```text
LauncherConfig
├─ appearance
├─ home
├─ apps
├─ wallpaper
├─ tvContent
├─ status
├─ media
├─ inputs
├─ remote
└─ system
```

This can remain serialized as one JSON DataStore value initially.

Do not fragment into 148 preference keys.

---

# 13. Settings and persistence are separate concerns

A setting descriptor does not mean a preference key.

```text
SettingDescriptor
    reads/writes typed Config
```

Separate:

```text
PersistencePolicy
    IMMEDIATE
    DEBOUNCED
    ON_CONFIRM

SettingEffect
    NONE
    RECOMPOSE
    RELOAD_APPS
    RELOAD_TV_CONTENT
    RELOAD_WALLPAPER
    RECREATE_ACTIVITY
    RESTART_HOME_TASK
    REFRESH_ACCESSIBILITY
```

This gives Projectivy-level update behavior while keeping Couchy's typed storage.

---

# 14. Runtime observation architecture

Prefer:

```text
callback
    ↓
repository
    ↓
StateFlow snapshot
    ↓
UI
```

over:

```text
Composable
    ↓
query Android provider/platform
```

Examples:

```text
LauncherApps.Callback
    → AppRepository / ShortcutRepository

ContentObserver
    → TvContent repositories

TvInputManager.TvInputCallback
    → InputRepository

NotificationListenerService
    → NotificationRepository

MediaController callbacks
    → MediaSessionRepository

Connectivity callbacks
    → NetworkStatusRepository
```

Polling is allowed only where no callback exists, and only while the feature actually needs it.

---

# 15. Demand-driven rule

Aerial-Rows should remain lighter than Projectivy by making optional work truly optional.

```text
feature disabled
    → background work disabled
```

Examples:

```text
Now Playing disabled and no idle/wallpaper consumer
    → no active MediaController observation

Display profile automation off
    → no display polling

Preview Channels off
    → no program load

Aerial off
    → no Aerial network/player activity

Accessibility enhancements off
    → Accessibility not required

No icon pack selected
    → no icon-pack parser work
```

This should become a code-review invariant.

---

# 16. Final performance philosophy

Do not redesign proven Couchy hot paths before measurement.

## Preserve immediately

```text
bounded app scanning
immutable object reuse
same-list reuse
target-size artwork
prepareToDraw()
SurfaceView
small video buffer
release decoder on stop
pseudo-blur
R8/shrink
```

## Fix immediately

```text
source rewriting
artwork variant cache identity
trust-all networking
malformed-config destructive fallback
rapid persistence queueing
```

## Measure before changing

```text
full artwork RAM residency
cold-start full-scan gate
SmoothCornerShape cost
panel gradient allocation
```

---

# 17. Final implementation phases

The original Projectivy Group 12 phase order was good.

C1–C11 refine it into the following final sequence.

---

## PHASE 0A — source-pure baseline

**Goal:** make the checked-in repository the actual program.

1. Pin current commit as recovery point.
2. Materialize all Gradle-injected `LauncherScreen.kt` changes.
3. Materialize all injected `SettingsSheet.kt` changes.
4. Materialize all injected strings/resources.
5. Remove `applyAerialRowsSourcePatches`.
6. Build from a clean checkout.
7. Assert build does not change Git working tree.
8. Verify:
   - debug;
   - release;
   - lint.

**Gate:**

```text
git status before build
==
git status after build
```

No feature work before this gate.

---

## PHASE 0B — performance/reliability baseline

**Goal:** know what we are protecting before refactoring.

Add:

```text
unit-test source set
androidTest / benchmark infrastructure
Macrobenchmark / baseline-profile generator
```

Record:

```text
cold process HOME
warm HOME
first focusable card
horizontal row navigation
vertical row navigation
settings open/close
Aerial + navigation
memory with representative app counts
```

Keep current hand-authored baseline profile until generated profile is validated.

**Gate:**

current branch has a recorded performance baseline.

---

## PHASE 1 — config safety + identities + launch core

This phase intentionally comes before home feature expansion.

### Fix current correctness first

- canonical localized `ConfigDefaults`;
- zero-category invariant;
- malformed JSON recovery;
- last-known-good storage;
- `ArtworkVariant` memory-cache identity.

### Add persistence primitives

```text
schemaVersion
MigrationRegistry
ConfigNormalizer
LastKnownGoodStore
```

### Add identities

```text
AppId
RowId
HomeItemId
```

### Add launch core

```text
LaunchTarget
LaunchRequest
LaunchDispatcher
LaunchPolicy
AllowAllLaunchPolicy
```

Route existing app launches through it.

**Gate:**

- existing config migrates safely;
- failed migration cannot destroy previous config;
- normal app launching uses dispatcher/policy;
- visual UI should remain essentially unchanged.

---

## PHASE 2 — generic home model + focus

Promote the current Sliding Rows private types into domain models.

Add:

```text
HomeRow
HomeItem
RowPresentation
HomeRowsRepository
HomeFocusCoordinator
HomeRailController
```

Implement:

```text
activeRowId
activeItemId
lastItemByRow
```

Reuse Couchy's pivot scrolling logic.

Replace:

```text
railVisible Boolean
```

with an explicit rail/focus state.

Fix rail row keys.

Add `EmptyRowAction` for empty user rows.

**Gate:**

- rail reveal/close restores exact card;
- row reorder preserves stable focus;
- empty user row remains configurable;
- no Projectivy feature rows yet.

---

## PHASE 3 — apps/categories/actions

Refactor without destroying Couchy's artwork path.

Add:

```text
component-aware AppId
PackageObserver / LauncherApps.Callback
AppClassifier
AppPlacementPolicy
MovingItem(rowId,itemId)
RowSortMode
NewInstallPolicy
HomeItemActionProvider
```

Fix:

- stale `PACKAGE_CHANGED` case;
- multi-row move bug;
- global-only Hide;
- category referential integrity.

Try removing `QUERY_ALL_PACKAGES` after launcher visibility behavior is proven.

**Gate:**

- no app scan/render regression;
- package changes are precise;
- same app can safely exist in multiple rows;
- every app launch uses LaunchDispatcher.

---

## PHASE 4 — Android TV provider completion

Replace UI-owned snapshot loading with repositories.

Add:

```text
TvChannelIdentity
TvChannelRepository
PreviewProgramRepository
WatchNextRepository
TvArtworkRepository
```

Implement:

- non-browsable channel discovery;
- `requestChannelBrowsable`;
- `internal_provider_id`;
- explicit Preview `weight DESC`;
- Watch Next engagement ordering;
- per-provider cap/exclusion;
- ContentObserver + debounce;
- resume refresh;
- provider-constrained launch policy.

**Gate:**

provider changes appear without launcher restart and logical channel identity survives numeric ID recreation where possible.

---

## PHASE 5 — wallpaper/Aerial engine

First remove insecure networking.

Then extract the current strong player into the engine model.

Add:

```text
WallpaperSelection
WallpaperEngine
WallpaperProvider
BuiltinAerialProvider
AerialItem
WallpaperDescriptor
VideoRenderer
ImageRenderer
WallpaperRotationScheduler
FailureRegistry
WallpaperEvent
```

Preserve:

- SurfaceView;
- small buffer;
- first-frame gate;
- ON_STOP release;
- fades;
- pseudo-blur.

Add:

- audio track disable at volume 0;
- optional tunneling;
- decoder fallback policy;
- shuffle bag;
- failed-item quarantine;
- robust persistent local-video URI handling.

**Gate:**

- one video renderer;
- normal TLS verification;
- Aerial/local video use same renderer;
- provider switch does not create a second player architecture.

---

## PHASE 6 — settings architecture

Materialize the existing UI on:

```text
SettingsNavigator
SettingId
SettingDescriptor
SettingsController
SettingEffectExecutor
```

Unify control components.

Implement:

- stable return focus;
- choice/dropdown dialog;
- debounced session persistence;
- truthful save failures;
- stable conditional-row focus.

Keep immediate Compose previews.

**Gate:**

change a setting → focus remains;
enter child page → back returns to originating setting.

---

## PHASE 7 — notifications + media + status cleanup

Extract:

```text
ClockRepository
NetworkStatusRepository
```

Add:

```text
LauncherNotificationListenerService
NotificationRepository
NotificationPolicy
NotificationPanel
MediaSessionRepository
NowPlayingHomeRow
NowPlayingCard
```

Use platform media-session APIs.

Route Now Playing opens through LaunchPolicy.

Do not couple notification dismissal to playback stop.

**Gate:**

notification access loss gracefully removes notification/media state without destabilizing HOME.

---

## PHASE 8 — physical TV inputs

Add:

```text
InputRepository
TvInputManager.TvInputCallback
TvInputSource
InputId
InputHomeRow
TvInputActivity
InputLaunchCoordinator
CurrentInputState
```

Standard TIF path first.

Separate:

```text
AvailableInputs
VisibleInputShortcuts
```

Vendor strategies only after standard behavior is tested.

Display profiles remain optional.

**Gate:**

standard Android TV / Shield-like devices work without vendor adapters.

---

## PHASE 9 — HOME/startup/remotes/idle

Extract current role handling into:

```text
HomeRoleCoordinator
```

Then add:

```text
StartupCoordinator
RemoteGestureEngine
LauncherIdleCoordinator
PowerStateMonitor
```

Reuse the C5 renderer for immersive idle.

Optional later:

```text
AccessibilityBridge
VendorCompatibilityRegistry
external screensaver strategy
```

Do not add a boot receiver merely to make HOME start.

**Gate:**

ordinary launcher use, input discovery, startup policy and idle operation do not require Accessibility.

---

## PHASE 10 — backup/themes/icons/shortcuts

Add portable user-state architecture.

```text
BackupEngine
ThemeEngine
IconRepository
ShortcutRepository
```

Full backup:

```text
.aerialbackup
```

Theme:

```text
.aerialtheme
```

Keep caches out.

Handle custom assets.

Use transactional restore:

```text
verify
→ decode
→ migrate
→ normalize
→ stage assets
→ commit
→ one runtime rehydrate
```

Add icon packs/per-item artwork.

Add shortcut query/start/pin/unpin/external pin requests.

**Gate:**

backup round-trip restores user state/assets and missing external references become unresolved rather than silently corrupted.

---

## PHASE 11 — optional parity / expert features

Only after the core is stable.

Possible features:

```text
ParentalPolicy
Display profiles
Vendor input adapters
Accessibility global keys/HOME fallback
External wallpaper providers
Diagnostics
Engineering/vendor settings
Package maintenance
```

Explicitly outside core:

```text
Projectivy licensing/protection behavior
opaque libptutils/bootstrap behavior
root/GApps removal
Play Store immutable-APK hacks
vendor CTS-property workarounds by default
Projectivy Web Remote clone
```

**Gate:**

removing the entire optional layer must leave the ordinary launcher functional.

---

# 18. Recommended first implementation wave

The safest first coding wave should be deliberately boring.

Do **not** start with notifications, inputs or Accessibility.

Recommended first sequence:

```text
PR/commit 1
Materialize Gradle-injected launcher/settings source

PR/commit 2
Remove source-rewriting Gradle task + add source-tree cleanliness CI

PR/commit 3
Add unit-test skeleton + benchmark/profile harness

PR/commit 4
ConfigDefaults + malformed JSON recovery + category invariant

PR/commit 5
schemaVersion + LastKnownGoodStore + MigrationRegistry

PR/commit 6
ArtworkVariant cache-identity fix

PR/commit 7
AppId / RowId / HomeItemId

PR/commit 8
LaunchDispatcher + AllowAllLaunchPolicy

PR/commit 9
HomeRow/HomeItem domain promotion

PR/commit 10
HomeFocusCoordinator + exact row/card restore
```

After those ten steps, the project has the safe foundation required for the feature phases.

---

# 19. Architecture invariants

These should become review rules and, where possible, automated tests.

## Build

```text
build never mutates source
```

## Launch

```text
all launcher-originated launches
    → LaunchDispatcher
    → LaunchPolicy
```

## Focus

```text
focus restoration
    → stable IDs
    not list indices
```

## Platform observation

```text
callbacks/observers
    → repository state
    → UI

not
UI → repeated platform queries
```

## Vendor

```text
core feature repositories
    must not depend on vendor/root implementations
```

## Accessibility

Normal:

```text
HOME
apps
TV content
inputs
wallpaper
settings
startup
idle
```

must function without Accessibility.

## Media

```text
notification state
    ≠
MediaSession state
```

## Inputs

```text
available input
    ≠
visible shortcut
```

## Wallpaper

```text
one background video renderer
```

## Persistence

```text
no destructive migration
before verified rollback state exists
```

## Security

```text
no trust-all TLS
```

## Performance

```text
feature disabled
    → its expensive background work disabled
```

---

# 20. Performance gates throughout implementation

Do not wait until Phase 11 to measure.

After these phases rerun key benchmarks:

```text
Phase 0
Phase 2
Phase 4
Phase 5
Phase 7
Phase 9
Phase 10
```

Track at minimum:

```text
cold startup
warm startup
first focusable card
horizontal navigation frame timing
vertical navigation frame timing
PSS/RSS
Aerial playback + navigation
settings latency
```

If a phase creates a meaningful regression, fix it before continuing.

That prevents Projectivy-level feature breadth from slowly turning Aerial-Rows into a heavy launcher.

---

# 21. What we should deliberately preserve from Couchy

These are not temporary implementation details.

They are desirable product characteristics.

```text
small HOME Activity
Compose-first UI
low component count
no Accessibility requirement
bounded background work
very fast app card rendering
minimal dependency graph
one video renderer
responsive D-pad UX
clean TV-focused styling
```

The Projectivy feature set should be added **without sacrificing these**.

---

# 22. What we should deliberately borrow from Projectivy

The valuable Projectivy content is mostly behavioral architecture:

```text
stable row/item state
reliable focus restoration
rich card/action model
TV Provider semantics
wallpaper providers/events
settings update semantics
notification/media repositories
input launch abstraction
startup dispatch
remote gesture state machine
backup/theme separation
icon packs
shortcuts
optional parental/display/vendor capabilities
```

These are the parts worth reproducing independently.

---

# 23. What we should not reproduce from Projectivy

Do not make these reconstruction goals:

```text
Leanback fragment implementation
obfuscated class topology
native PTApplication bootstrap
licensing/premium implementation
root package maintenance
GApps removal
Google Play immutable-APK blocker
vendor Play Store CTS-property workaround
Xiaomi updater workaround by default
Accessibility-owned display polling
opaque privileged shell architecture
Projectivy Web Remote
```

Those are not required to achieve the launcher UX/functionality target.

---

# 24. What we should not retain from Couchy long term

```text
build-time Kotlin source rewriting
package-only app identity
movePkg
global-only app hide
browsable-only TV channel discovery
UI-owned TV content lifecycle
trust-all Aerial networking
legacy builtinSource runtime coupling
wallpaperVersion/aerialError orchestration
direct settings-to-DataStore wiring everywhere
multiple settings control families
raw LauncherConfig backup
silent malformed-config reset
no schema migrations
no tests/benchmarks
```

---

# 25. Final product architecture statement

The resulting Aerial-Rows should feel like:

```text
Projectivy-level launcher capabilities
        +
Couchy/Aerial visual identity
        +
modern Compose implementation
        +
better platform callbacks
        +
cleaner optional/vendor boundaries
        +
stronger performance discipline
```

not:

```text
Projectivy decompiled into Compose
```

and not:

```text
Couchy with dozens of features bolted into LauncherScreen.kt
```

---

# 26. Final scope tiers

## Tier A — core target

These should form the main Aerial-Rows product:

- source-pure build;
- stable config/migrations;
- generic home row/item model;
- reliable focus/rail;
- installed apps/categories/actions;
- Android TV Channels + Watch Next;
- Aerial/wallpaper engine;
- settings;
- status/network/clock;
- notifications + Now Playing;
- standard TV inputs;
- HOME/startup/remotes/idle;
- full backup;
- themes;
- icon/custom artwork support;
- shortcuts.

## Tier B — optional advanced parity

- parental controls;
- display profiles;
- vendor input adapters;
- optional Accessibility global mappings;
- external wallpaper providers;
- diagnostics.

## Tier C — not core

- root/package maintenance;
- privileged Google/Xiaomi hacks;
- GApps uninstall;
- licensing/protection cloning;
- opaque native bootstrap;
- Projectivy Web Remote clone.

---

# 27. Final answer to the Couchy-vs-Projectivy question

The study now gives a definitive answer:

### Couchy is the better **base implementation**.

It is:

- smaller;
- more modern;
- more Compose-native;
- more performance-conscious in app artwork and video;
- easier to maintain;
- less coupled to services/vendor behavior.

### Projectivy is the better **feature and behavioral reference**.

It has already solved:

- complex home state;
- launcher actions;
- TV-provider behavior;
- media/notifications;
- physical inputs;
- startup/remotes;
- backups/themes/icons/shortcuts;
- optional expert capabilities.

### Aerial-Rows should combine them selectively.

The guiding rule should be:

> **When Couchy already performs the job well, keep its implementation. When Couchy is missing a capability, reproduce the verified Projectivy behavior behind a clean Compose-era interface. When Projectivy has known coupling/reliability weaknesses, implement the behavior better rather than copying the weakness.**

---

# 28. C12 closure

All eleven Couchy comparison groups have now been integrated.

The source baseline is still current.

The architecture, subsystem ownership, identities, performance rules, optional boundaries and implementation order are sufficiently defined to begin actual repository changes without another discovery phase.

The next step should no longer be another research group.

It should be:

```text
PHASE 0A
materialize the source patches
and remove applyAerialRowsSourcePatches
```

followed immediately by:

```text
PHASE 0B
benchmark/test baseline
```

before structural refactoring begins.

**C12 status: COMPLETE + REVIEWED.**

---

# SECTION D — Chat / Project Reconstruction
# Launcher Project — Reconstructed Chat Export / Continuation Record

**Purpose:** This is a durable reconstruction of the Launcher/Aerial-Rows project conversation up to the completion of C12 on 2026-10-23. It is not a verbatim transcript. It preserves the decisions, discoveries, corrections, user intent, workflow expectations, file/tool provenance, and implementation direction needed to continue with equivalent context.

For detailed subsystem evidence, use `COUCHY_C1_C12_FULL.md` and `PROJECTIVY_G1_G12_FULL.md` stored next to this file.

---

# 1. Original project intent

The user wanted to create a better Android TV launcher by starting from an existing open-source launcher and adding the best Projectivy-style behavior plus Aerial video wallpapers.

The project evolved through several ideas:

1. Customize an existing Shield/Android TV launcher.
2. Consider cloning/forking Projectivy directly.
3. Recognize that Projectivy is proprietary and use reverse engineering only to understand behavior/architecture.
4. Consider Couchy Launcher as the clean open-source base.
5. Fork Couchy into `kanozseyer/Aerial-Rows`.
6. Add Projectivy-like features on top while retaining Couchy's modern Compose/performance qualities.
7. Reverse engineer Projectivy comprehensively enough to know what behavior to reimplement independently.
8. Compare Couchy subsystem-by-subsystem against the Projectivy audit.
9. Freeze a final architecture/implementation plan.

The user explicitly asked for a **streamlined settings UI**, clear switches, dropdown-like selectors, persistent focus/cursor position after changing a setting, and a left navigation rail that behaves like Projectivy: the rail should only appear/reveal when navigating left from the home rows.

Desired features accumulated over the project included:

- long-press app options including uninstall;
- Android TV Channels / Watch Next;
- Projectivy-style left headers/rail;
- per-row customization and reordering;
- richer settings with dropdown/choice behavior;
- Aerial video backgrounds;
- notification/Now Playing behavior;
- TV inputs/HDMI;
- startup/autostart/remotes;
- backup/theme/icon/shortcut systems;
- optional advanced device features.

The user values **performance and polish**, not just feature parity.

---

# 2. Repository selection and early Aerial-Rows state

The user forked Couchy and created/named the repo **Aerial-Rows**.

During early setup:

- JVM 21 was required in the user's Android Studio/environment.
- The project was built and run as the working base.
- A development branch accumulated Aerial-Rows changes.
- The final research baseline was `aerialrows-development` commit `3c1cfd56234f1d386f4553dd6c69523670500d70`.

The Aerial-Rows branch already included:

- a fourth `LAYOUT_SLIDING_ROWS` mode;
- `SlidingRows.kt` with Projectivy-inspired horizontally sliding rows and a left rail;
- Android TV Preview Channels and Watch Next support via `TvContentRepository`;
- `RowQuickEditor` for row-level app/channel customization;
- Aerial sources and video wallpaper support;
- configurable row/dock panel appearance;
- multi-source Aerial selection concept;
- various Couchy UI/performance features.

However, several Aerial-Rows changes were not normal checked-in source behavior. They were injected at build time by `applyAerialRowsSourcePatches` in `app/build.gradle.kts`. This became the highest-priority cleanup item.

---

# 3. Projectivy reverse-engineering decision

The user had a fork of an REA repository intended to help reverse engineer Android applications:

`https://github.com/kanozseyer/rea-fork`

The user asked whether REA could be used to reverse engineer the Projectivy APK and wanted the full JADX pipeline working.

The Projectivy target APK supplied during the project was version 4.72 / code 97.

A JADX MCP all-in-one JAR was also supplied.

The project eventually established a working REA/JADX environment and performed a comprehensive grouped audit rather than relying on a single broad decompile pass.

Important safety/IP boundary agreed in practice:

- study proprietary APK behavior and architecture;
- do not paste wholesale proprietary decompiled source into Aerial-Rows;
- do not bypass premium/licensing;
- independently reimplement desired behavior.

---

# 4. Reverse-engineering toolchain and target provenance

Projectivy audit target:

```text
package: com.spocky.projengmenu
versionName: 4.72
versionCode: 97
minSdk: 23
targetSdk: 37
classes: 7395
resources: 1171
DEX files: 3
JNI library: libptutils.so (all four common Android ABIs)
MainActivity: com.spocky.projengmenu.ui.home.MainActivity
```

APK SHA256:

`a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

Signing certificate SHA256:

`f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`

Signer subject included `CN=Despesse Mickael`.

Toolchain used:

- JADX headless MCP 0.7.1 all-in-one JAR;
- REA 6.7.0;
- Java 21;
- Node 22.20.0;
- persistent JADX MCP daemon.

Historical analysis paths:

```text
/mnt/data/rea-portable
/mnt/data/rea
/mnt/data/launcherproject_work
```

These are preserved only as provenance. A future environment does not need identical paths unless re-running research.

---

# 5. Audit process evolution

The Projectivy audit was intentionally split into small groups to reduce mistakes, timeouts, and context loss.

The user repeatedly emphasized that analysis should be exhaustive and process fixes should survive interruptions.

The audit process was hardened through several iterations. Final principles included:

- semantic-first search instead of assuming class names;
- use REA/JADX canonical evidence for high-risk claims;
- persistent daemon for speed;
- independently complete class inventory and resource inventory;
- do not treat `resources=lite` as resource closure;
- bounded batches (default 6, max 12);
- write-ahead `in_progress` checkpoint before slow JVM probes;
- track interrupted/recovered attempts;
- atomic report/state writes;
- quarantine failures;
- exact cache behavior;
- FTS dotted/literal fallback;
- named-class prevalidation;
- no empty-query REA as exhaustive search;
- no aggressive parallel JVMs;
- resume only unfinished work after timeout;
- closure matrix across 11 dimensions;
- stop/report after each group;
- completion receipt mechanism to avoid repeating already-complete analysis.

Closure dimensions:

1. entry points;
2. base classes/inheritance;
3. all DEX files;
4. obfuscated collaborators;
5. models/data flow;
6. persistence;
7. resources/XML;
8. Android framework APIs;
9. callbacks/observers;
10. launch/actions/lifecycle;
11. native/JNI.

Final process contract references:

- v18 contract SHA `052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`
- process bundle SHA `251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

One important lesson: documentation once referred to a `--core` self-test option, but the actual installed CLI did not expose it. The project established a rule to inspect the actual CLI before trusting old process docs.

---

# 6. Projectivy Group 1–12 research programme

Full original and post-closure reports are concatenated in `PROJECTIVY_G1_G12_FULL.md`.

The original groups and post-closure audits established enough behavior to reconstruct a clean implementation plan.

The later post-closure corrections are especially important for Groups 8–12.

## Projectivy home architecture

The reconstructed chain was approximately:

```text
MainActivity
 -> MainFragment
 -> customized BrowseSupportFragment
 -> rows adapter
 -> row wrapper
 -> category model + item adapter
 -> row presenter selector
 -> card presenter selector
```

Important verified behavioral ideas:

- left headers/rail have three meaningful states: shown, hidden-but-revealable, disabled;
- selected row/header/title/page state is saved/restored;
- startup default row resolves by stable category ID rather than index;
- focus restoration uses stable category ID + card ID;
- row models carry rich presentation configuration;
- configured empty rows get a concrete placeholder card rather than disappearing;
- presenter cache by layout characteristics;
- deliberate vertical alignment beneath status/title region;
- selected/nonselected row fade behavior;
- rail supports settings/reorder controls;
- row/card reorder preserves focus via stable IDs;
- selected card can drive a hover/details surface.

## Projectivy apps/actions

Projectivy behavior reinforced the need for:

- stable app/category context in actions;
- per-row and global visibility semantics;
- richer context action architecture;
- launch abstraction rather than direct UI `startActivity` calls;
- stable IDs throughout movement/reorder.

## Projectivy Android TV provider content

Key ideas:

- discover Preview Channels including non-browsable ones;
- allow requesting channel browsability;
- use provider ordering semantics;
- more deliberate Watch Next ordering/provider behavior;
- separate provider content domain from physical TV input domain.

## Projectivy wallpaper engine

Projectivy was stronger around the player than Couchy:

- provider abstraction;
- semantic wallpaper events;
- explicit rotation scheduler;
- provider cache invalidation;
- image format policy;
- retry/fallback behavior;
- renderer configuration including tunneling/decoder fallback;
- disable audio track for muted video;
- optional dynamic color sampling;
- external providers;
- idle/theme integration.

But Couchy's actual player implementation was better for Aerial-Rows and should be retained.

## Projectivy settings

Projectivy's strongest reusable idea was not its SharedPreferences store; it was metadata describing settings:

- stable IDs;
- validation;
- visibility/capability;
- update behavior;
- migration aliases;
- export policy;
- focus identity.

Aerial-Rows should preserve typed config/DataStore and add a descriptor layer instead.

## Projectivy media/notifications

Important split:

```text
NotificationListenerService
  -> NotificationRepository
  -> permission bridge for MediaSessionManager

MediaSessionManager
  -> MediaSessionRepository
```

Now Playing comes from active MediaSessions, not parsed notification text.

Multiple sessions are possible. A primary session can be selected for wallpaper/idle state.

## Projectivy TV inputs

Projectivy had mature source mapping/launch behavior, but later audit exposed weaknesses:

- no repository-level `TvInputManager.TvInputCallback`;
- discovery more snapshot/cache-driven than ideal;
- visible shortcut filtering leaked into display-profile choices;
- current input state could diverge across launch strategies;
- compatibility recovery could be demand-triggered;
- display profile polling was coupled to Accessibility and could stop after a transient null sample;
- dangling profile UUID bindings were tolerated.

Aerial-Rows should preserve the behavior but improve those weaknesses.

## Projectivy startup/remote/idle

Verified concepts included:

- shared startup dispatcher from multiple triggers;
- duplicate suppression (~15s observed);
- boot validity window (~90s observed);
- persisted last-used input;
- HOME task restart using Android HOME/task semantics;
- global key state machine with long/double press (~300/400ms observed);
- temporary hotkey mode (~3s observed);
- direct HDMI vendor key codes observed (243–246);
- screensaver strategies;
- short idle restore delay (~300ms observed).

Exact timings are references, not requirements.

Later audit also found Xiaomi/Play Store compatibility workarounds and an Accessibility user-activity hack. These are not generic core architecture.

## Projectivy persistence/backups/themes/icons/shortcuts/parental

Projectivy confirmed the value of:

- schema migrations;
- pre-migration safety state;
- backup separate from theme;
- explicit icon-pack parsing/cache;
- launcher shortcut hosting;
- optional parental policy.

But Aerial-Rows should not copy Projectivy's embedded-secret/weak backup crypto or magic export-name conventions.

## Projectivy native/obfuscated closure

Group 11 concluded there was no hidden architectural engine behind native/obfuscated code.

`libptutils.so` was a narrow optional route, not something Aerial-Rows needs to reproduce.

Other verified optional privileged behaviors included root package disable/enable commands, Google Play immutable APK blocker, GApps uninstall script, optional app installer, and external Web Remote URL. None are core reconstruction targets.

## Projectivy Group 12 plan

The original Projectivy-only reconstruction phase plan was:

```text
0 source-pure reproducible build
1 config+migrations+rollback+identities
2 home rows+stable focus+rail
3 apps+actions+LaunchDispatcher/LaunchPolicy
4 complete TV Provider
5 wallpaper/Aerial provider + shared renderer
6 settings
7 notifications+Now Playing
8 TV inputs
9 HOME/startup/remotes/accessibility/idle
10 backup/themes/icons/shortcuts
11 optional expert/device tools
```

Couchy C1–C11 later refined this plan, primarily by inserting performance/testing immediately after Phase 0 and adjusting implementation choices to preserve Couchy's hot paths.

---

# 7. Couchy comparison programme C1–C12

The user explicitly asked to study Couchy source and compare it to Projectivy in small groups to avoid mistakes.

The planned groups became:

1. C1 Build/manifest/lifecycle/architectural skeleton.
2. C2 Home screen architecture.
3. C3 Installed apps/categories/card actions.
4. C4 Android TV Channels & Watch Next.
5. C5 Wallpaper & Aerial architecture.
6. C6 Settings architecture.
7. C7 Status/media/notifications.
8. C8 TV inputs/device integration.
9. C9 HOME/startup/remotes/Accessibility/idle.
10. C10 Persistence/backups/icons/shortcuts/themes/parental.
11. C11 Performance/reliability.
12. C12 Final synthesis.

All are complete and reviewed.

---

# 8. C1 conclusions

Couchy's manifest/component graph is dramatically smaller/cleaner than Projectivy.

Current baseline has essentially one MainActivity as HOME/DEFAULT/LEANBACK_LAUNCHER/LAUNCHER, with no Accessibility service, NotificationListener, boot receiver, wallpaper-provider receiver, or theme provider.

MainActivity handles:

- READ_TV_LISTINGS prompt;
- package receiver;
- locale bootstrap;
- no-back HOME root behavior;
- Compose entry.

Decision: keep MainActivity small. Future startup/input/accessibility should be separate coordinators/services.

`LauncherConfig` JSON in Preferences DataStore is a better persistence foundation than Projectivy's many individual preferences. Add schema/migrations/rollback rather than replacing it.

`AppRepository` was immediately identified as a major strength and should be preserved.

`Actions` is a useful platform boundary but future calls should converge on `LaunchDispatcher + LaunchPolicy`.

Guiding principle established in C1:

**Couchy lightweight foundation + Projectivy behavioral/subsystem reference.**

---

# 9. C2 conclusions — home

Effective current architecture:

```text
MainActivity -> LauncherApp -> config/app scan -> categorized rows -> shell/status/wallpaper -> layout
```

Raw checked-in `LauncherScreen` handles Carousel/Grid/Dock. Sliding Rows is connected by build-time source patch.

`LauncherApp` owns too much and should later be split into home/runtime repositories/controllers.

Current Sliding Rows row model is private and only covers Watch Next, channel, and category.

Target generic row/item model was established.

Stable identity findings:

- outer rows are keyed by row ID;
- app keys include category + package;
- TV program keys use provider item ID;
- `homeRowOrder` stores IDs rather than indices;
- but card focus identity is not persisted per row.

Need `HomeFocusState(activeRowId, activeItemId, lastItemByRow)`.

Rail behavior today is effectively Left #1 show, Left #2 focus. Target needs explicit reveal/focus ownership.

Rail reorder has positional identity risk because `rows.forEachIndexed` is used in a normal Column and `RailItem` remembers focus state. Key by row ID and explicitly refocus after reorder.

Couchy's older Carousel/Grid code already contains strong custom `BringIntoViewSpec` pivot geometry. Reuse this in Sliding Rows rather than inventing a Leanback-style implementation.

Empty rows: hide ephemeral empty rows but preserve user-configurable empty row via placeholder action card.

Per-row presentation should be introduced as overrides rather than dumping dozens of fields directly into LauncherConfig.

Couchy's AppCard should be retained. Add selected-details later as a shared surface driven by stable active item ID.

---

# 10. C3 conclusions — apps/categories/actions

Couchy's app scan/render pipeline is better than Projectivy's for the implementation base.

Concrete bugs found:

### Package removal through PACKAGE_CHANGED can leave stale cards

Current logic can skip a rescan/removal path if a package changed such that it no longer has a launcher activity. Use LauncherApps callback-driven invalidation/reconciliation.

### Move state bug

Current move state is only `movePkg`. Because an app can belong to several sections, `findMoving()` can pick the first occurrence and multiple cards can believe they are moving/use the same FocusRequester.

Replace with row-scoped `MovingItem(rowId,itemId)`.

### Zero-category bug

It is possible to leave only generated All Apps then disable it, resulting in zero configured categories. Later code can call `categories.first()` and fail.

Enforce structural invariant in normalization and treat All Apps as generated/system, not a structural user category.

### Package-only identity

App discovery initially has an exact ResolveInfo/activity, but `AppEntry` discards component identity. Later launching resolves package launch intent again, so label/art/activity identity can diverge for multi-activity packages.

Use component-aware AppId.

### Hide semantics

Current hidden set is global. Projectivy behavior demonstrates the value of row-local hide/exclusion plus explicit Hide Everywhere.

### Context actions

Keep current UI/release-suppression behavior. Generalize to a HomeItem action provider for future actions.

### Sort/new install

Keep install-time sorting as one mode and add alternatives. New-install behavior should be explicit policy.

---

# 11. C4 conclusions — TV Provider

Couchy's implementation is a good minimal reader but not a complete TV-provider subsystem.

Concrete gaps:

- non-browsable channels filtered before settings;
- channels with zero programs disappear from discovery/settings;
- no explicit Preview Program sort;
- no explicit Watch Next sort;
- no Watch Next per-provider cap/exclusion;
- durable channel identity uses package:numeric ID only;
- no observer/resume refresh;
- provider deep links are started directly and do not go through common policy or ownership constraints.

Target architecture separates channel discovery, Preview Programs, Watch Next, artwork, and observer lifecycle.

---

# 12. C5 conclusions — wallpaper/Aerial

Couchy's renderer is one of the strongest parts of the whole codebase.

Keep:

- SurfaceView;
- Media3;
- decoder release on stop;
- small buffer;
- first-frame gating;
- transitions;
- gradient cover;
- local media handling philosophy;
- downsampled images;
- pseudo-blur.

Serious issue found: trust-all TLS + hostname verifier for Aerial streaming. Remove.

Build-patch issue especially matters here because `builtinSources` is only partially materialized in checked-in source.

Another subtle bug: patched source selection uses new `builtinSources`, but old `builtinSource` remains in `produceState` keys, so changed collection selection may not apply until current clip ends/error/timer. If finish-current-first is desired, make it explicit policy.

Projectivy-inspired engine/provider separation was adopted as the target, while retaining Couchy's player.

Audio track should be disabled when muted.

Shuffle bag, failure quarantine, rotation scheduler, typed Aerial metadata, persistent URI error handling were all recommended.

---

# 13. C6 conclusions — settings

Couchy's UI is good but model is too implicit.

Every setting directly writes config and therefore UI owns persistence/effect semantics.

Add descriptors/controller/effect policy while retaining typed config and Compose.

Concrete issues:

- three separate focus/control families;
- focus restoration does not remember originating setting after child route;
- direct DataStore full-config write on rapid adjustments;
- backup save API/reporting bug;
- hardcoded English strings in extras;
- validation scattered.

Target `SettingsNavigator` stores last focused SettingId per route.

Small ranges keep Left/Right; larger named choices get an OK-opened choice panel/dropdown.

---

# 14. C7 conclusions — status/media/notifications

Couchy's baseline has no notifications/media implementation.

Keep current status bar and hidden focus strip.

Add separate repositories for notification and media.

Important rule: Now Playing derives from MediaSessions, not notification text.

Use platform MediaSession APIs rather than adding a Media3 Session dependency solely for observation.

Media launches must go through central LaunchPolicy, improving on Projectivy's policy gap.

Network state should distinguish transport vs validated Internet. Clock/date should share a repository/ticker.

---

# 15. C8 conclusions — physical TV inputs

Couchy has no physical TV input subsystem.

This became a clean-sheet design.

Use public `TvInputManager` + repository callback + resume refresh.

Keep three concepts distinct: discovered, available, visible shortcuts.

Use dedicated fullscreen TvInputActivity/TvView for standard TIF source display.

All input launching goes through InputLaunchCoordinator and LaunchPolicy.

Current input state should distinguish requested, launched, confirmed.

Vendor adapters and display profiles are optional later.

Do not tie display-profile polling to Accessibility.

---

# 16. C9 conclusions — HOME/startup/remote/idle

Couchy's small lifecycle is a strength.

Keep normal HOME role, singleTask MainActivity, no root Back exit, RoleManager setup, UI-local key handling.

Add HomeRoleCoordinator and StartupCoordinator separately.

Do not add boot receiver merely to launch HOME.

RemoteGestureEngine is for programmable global gestures; ordinary D-pad/AppCard long-press/move mode stay UI-local.

Accessibility remains optional and narrow.

Idle should use shared wallpaper renderer and hide launcher chrome rather than create another player.

Manual Google TV ADB setup remains expert guidance; add rollback guidance rather than silently disabling stock launchers.

---

# 17. C10 conclusions — persistence/backups/themes/icons/shortcuts/parental

Couchy has a strong simple storage primitive, but very little surrounding persistence infrastructure.

Critical ConfigStore bugs were identified:

- localized defaults vs English first update;
- malformed JSON silently becomes defaults and can later overwrite original state.

Current Save Config is not a full backup.

Custom photo is external file not in backup. Local video content URI is device/install-local.

Need separate runtime config and public backup DTO.

Need explicit full backup vs theme separation.

Existing artwork cache should remain cache-only.

Icon packs and launcher shortcuts are absent and can be designed cleanly from scratch.

Parental controls are optional and must sit in LaunchPolicy, not Accessibility.

---

# 18. C11 conclusions — performance/reliability

Couchy contains unusually thoughtful TV-specific performance decisions. Do not erase them during refactor.

High-value invariants:

- bounded app scan;
- immutable object reuse;
- same list instance reuse;
- pre-rasterized artwork;
- `prepareToDraw()`;
- stable keys;
- SurfaceView;
- small Media3 buffer;
- release player/decoder on stop;
- pseudo-blur;
- release shrink/minify;
- low service count.

Concrete build issue: source patch task writes huge Kotlin files every preBuild and hurts incremental builds/reproducibility.

Concrete AppRepository bug: in-memory reuse key ignores rendered artwork size/density variant even though disk filename contains it.

Cold process startup waits for the entire app scan + artwork load before rendering real HOME. Measure first before redesigning.

All decoded app artwork is retained process-wide. This may be a reasonable smoothness tradeoff. Measure PSS/RAM before replacing with bounded LRU.

No tests/benchmarks currently exist. Add benchmark infrastructure before feature transplant.

Projectivy feature breadth should be demand-driven so disabled features do no expensive background work.

---

# 19. C12 final synthesis

C12 froze the final conclusion:

**Couchy is the better base implementation. Projectivy is the better behavioral reference.**

Do not restart research. Begin implementation with source purity and performance baseline.

Final architecture and exact phase order are in `couchy/C12.md` and `AI_PROJECT_CONTEXT.md`.

---

# 20. Current branch/file state that a future agent should inspect first

At the final research baseline:

- development branch commit `3c1cfd56234f1d386f4553dd6c69523670500d70`;
- `app/build.gradle.kts` still contains `applyAerialRowsSourcePatches`;
- checked-in source therefore does **not** fully equal compiled source;
- `SlidingRows.kt` exists as normal source but is wired/integrated partly through the patch;
- settings Channels/SlidingRows/panel/Aerial multi-source wiring is partly patch-injected;
- no notification/media/input/startup/accessibility/shortcut/icon-pack/parental subsystems exist yet in the baseline.

The first implementation task is to remove that ambiguity without behavior change.

---

# 21. User expectations for future work

The user prefers:

- exact practical steps;
- small durable changes;
- visible checkpoints;
- exhaustive understanding;
- no repeated questions when context already answers them;
- source verification before edits;
- preservation of every discovered process fix;
- testing actual behavior rather than relying on assumptions;
- explicit KEEP/EXTEND/REPLACE decisions;
- strong performance and focus behavior.

When implementing, work in bounded commits and report what changed, what was verified, and what remains.

---

# 22. Files preserved with this export

## Couchy comparison

`COUCHY_C1_C12_FULL.md` (complete C1–C12 archive)

These are the final subsystem comparison reports. C12 is the authoritative blueprint; C1–C11 contain details C12 intentionally compresses.

## Projectivy research

`PROJECTIVY_G1_G12_FULL.md` (complete Groups 1–12 + post-closure archive).

Where a post-closure audit exists, use it to refine/correct the original report. In particular, Groups 8–12 have important corrected versions.

## Process

`PROCESS_FULL.md` containing current process state plus historical hardening reports.

## Manifest

`MANIFEST.md` records hashes of the handoff files at export time.

---

# 23. Suggested prompt for a new AI conversation

A user can start a fresh conversation with something like:

> We are continuing the Aerial-Rows Android TV launcher project. Read `AGENTS.md`, `AI_PROJECT_CONTEXT.md`, and `docs/ai-handoff/C12_FINAL_BLUEPRINT.md` from the repository before doing anything. The Projectivy/Couchy research is complete. Continue from Phase 0A exactly as documented, verify the current `aerialrows-development` branch first, and preserve the performance/reliability invariants in C11.

That should restore most of the project context immediately.

---

# 24. Final handoff state

Research status at export:

```text
Projectivy Groups 1–12: COMPLETE
Projectivy post-closure audits: COMPLETE
Couchy comparisons C1–C12: COMPLETE + REVIEWED
Final architecture: FROZEN ENOUGH TO IMPLEMENT
Next phase: PHASE 0A SOURCE-PURE BUILD
```

No additional discovery group is required before implementation.


# Chat Export Continuation — Complete Handoff Export Work

This file continues `CHAT_EXPORT.md` with the work performed after C12, when the user asked for a durable export of the entire project context into Aerial-Rows so a fresh ChatGPT conversation or another AI model could continue as effectively as the current one.

## User's export requirement

The user explicitly requested:

> make an export of this chat inside the project for future. Include every little detail so that a new conversation with ChatGPT or any other AI model can use it to continue this project as well as the current conversation, and put the necessary files in the GitHub sources of Aerial-Rows.

The user also repeatedly required the export process to be reliable, efficient, resumable, and divided into small stages if necessary. After several process iterations, the user explicitly said to stop over-engineering and simply achieve the handoff quickly and without faults.

## Initial GitHub handoff layer

The first export attempt appeared to stall, but later inspection showed it had successfully created:

`b34d295407845ff1cb08e17d93875e888d3bbe24` — `Add complete AI project handoff and research context`

That commit added the orientation/context layer including:

- `AGENTS.md`
- `AI_PROJECT_CONTEXT.md`
- `docs/ai-handoff/README.md`
- `docs/ai-handoff/C12_FINAL_BLUEPRINT.md`
- `docs/ai-handoff/CHAT_EXPORT.md`
- `docs/ai-handoff/COUCHY_DETAILED_FINDINGS.md`
- `docs/ai-handoff/PROJECTIVY_DETAILED_FINDINGS.md`
- `docs/ai-handoff/PROCESS_STATE.md`
- `docs/ai-handoff/RESEARCH_INDEX.md`

It did not yet contain every raw research report.

## Raw export recovery and staged export

The export was then divided into resumable groups. Couchy C1–C6 were exported first. Several important reliability lessons were discovered:

- large one-shot Git/tool payloads could truncate or look stuck;
- many small Contents API commits were durable but slow;
- chunk-boundary newlines could be normalized unexpectedly;
- C4 part 2 required a one-byte newline repair;
- C5 required moving a boundary newline between parts while preserving the original full-report SHA;
- a later retroactive audit discovered C3 part 3 was one byte short even though an earlier prose checkpoint had claimed exact reassembly.

That C3 defect proved that prose status must never be considered more authoritative than byte-level validation.

## Export Process v3

The process was upgraded to:

- exact Git blobs;
- per-report manifests;
- master raw archive manifest;
- SHA-256 and Git blob SHA checks;
- automated repository validation;
- one atomic Git tree/commit per raw report batch;
- compare-and-swap branch-ref updates so concurrent work could not be overwritten.

The v3 retrofit repaired C3 and standardized C1–C6.

Important v3 checkpoint:

`f4997de730d869bc951c7ba25d8e61249ad7c38d` — `Standardize and validate AI handoff archive v3`

The GitHub handoff validator passed against that state.

## Export Process v4

The user asked for another reliability/efficiency/speed review. v4 added an authoritative `SOURCE_CATALOG.json` defining the expected source SHA-256 for every Couchy C1–C12 report and every required Projectivy original/post-closure audit.

This changed validation from only "are the files we exported correct?" to also "did we export every report that is supposed to exist?"

C7 became the first fully catalog-aware raw report.

C7 commit:

`5b5edc95c1c73bda919ef5f322388a8250ab7b13` — `Export raw Couchy C7 with handoff process v4`

The GitHub handoff validator completed successfully. A comparison against the original research baseline showed the export had only added handoff documentation and its validation workflow; no launcher implementation source had been modified.

## Export Process v5

C8 testing showed that the remaining weak point was not source validation or atomic publication; it was manually transporting larger report payloads through the connector.

v5 therefore introduced deterministic lossless `gzip + base64` transport for new reports:

1. original Markdown SHA remains authoritative;
2. source is gzip-compressed with deterministic metadata (`mtime=0`);
3. compressed bytes are base64 encoded;
4. ASCII transport is chunked into small files;
5. validator/materializer concatenates, base64-decodes, gzip-decompresses, and requires exact original Markdown bytes.

A critical transport rule was discovered: `.md.gz.b64` files are already base64 text and must be sent to GitHub as UTF-8 content. Asking GitHub's blob API to interpret them as base64 would decode the text and store different bytes.

C8 commit:

`8851b63765b89aea616abe4c442e3d70b9baaffb` — `Upgrade AI handoff transport to v5 and export Couchy C8`

The repository handoff validator passed successfully for that commit.

At that GitHub checkpoint the raw archive state was:

- C01–C08 complete;
- C09–C12 pending in the GitHub raw-per-report archive;
- Projectivy raw archive still pending there;
- process archive pending there.

However, the local workspace already contained a complete assembled export directory (`/mnt/data/ai_handoff_export/`) with all Couchy C1–C12, all Projectivy original/post-closure reports, process hardening notes, project context, chat export, C12 blueprint, and combined searchable archives. The user suggested using one compressed ZIP and manually uploading it if connector limits were the problem. This is the correct simplification.

## Final simplification decision

The user explicitly instructed:

> stop over engineering, you're taking too long. You just need to achieve this without any faults. Design an efficient, effective, fast and reliable way to do it in stages.

The final export approach therefore stops trying to push dozens of individual raw report files through ChatGPT's GitHub connector.

The complete local handoff is packaged as:

- one ZIP archive for manual upload/backup;
- one standalone `AI_HANDOFF_COMPLETE.md` for easy direct GitHub upload and AI reading;
- individual raw report files inside the ZIP;
- checksum manifests.

This ZIP/manual-upload approach supersedes the incomplete per-report GitHub raw export. Do not spend more project time completing the connector-based C9–Projectivy raw push unless the user explicitly asks for it. The research itself is already complete.

## Current project implementation state

The underlying implementation baseline remains the research pin:

`3c1cfd56234f1d386f4553dd6c69523670500d70`

The handoff/documentation commits after that baseline do not intentionally change launcher implementation behavior.

The next actual implementation work remains:

**Phase 0A — source-pure build**

- materialize all behavior currently injected by `applyAerialRowsSourcePatches` into normal checked-in Kotlin/XML/resources;
- remove the build-time source-rewriting task;
- verify clean debug/release/lint builds;
- verify the Git working tree remains unchanged after build;
- then establish the C11 benchmark/test baseline before architecture refactors.

## Future-AI instruction

When this archive is supplied to another AI:

1. read `AGENTS.md`;
2. read `AI_PROJECT_CONTEXT.md`;
3. read `C12_FINAL_BLUEPRINT.md`;
4. read `CHAT_EXPORT.md` and this continuation;
5. use `COUCHY_C1_C12_FULL.md` and `PROJECTIVY_G1_G12_FULL.md` as the complete research evidence;
6. use `PROCESS_FULL.md` only if reverse-engineering/audit work must be reopened;
7. verify the current live GitHub branch before editing;
8. do not restart C1–C12 research unless genuinely new source changes invalidate a conclusion;
9. begin Phase 0A when implementation resumes.

---

# SECTION E — Couchy C1–C12 Complete Research
# Couchy / Aerial-Rows Comparison Reports C1–C12 — Full Archive
This file concatenates the complete source reports from the research archive. Each section preserves the original report text.


---

# SOURCE FILE: `C01.md`

SHA-256: `c85c2188906a849854865fc549e1f62c697899d7a648d05fb3f1fd6954e88cea`

---

# Couchy vs Projectivy — Comparison Group C1

## Scope
Outer architecture, build/runtime model, manifest/component skeleton, MainActivity responsibilities, persistence root, app-discovery performance and launch-action boundary.

## Source baseline
Aerial-Rows / Couchy development commit:
`3c1cfd56234f1d386f4553dd6c69523670500d70`

Key files reviewed:
- app/build.gradle.kts
- app/src/main/AndroidManifest.xml
- MainActivity.kt
- data/Config.kt
- data/AppRepository.kt
- Actions.kt

## Architecture comparison

### Build/runtime
Couchy/Aerial-Rows uses Kotlin + Compose, Java 17, minSdk 21, targetSdk 34. The development branch still has a Gradle source-rewriting task (`applyAerialRowsSourcePatches`) that patches SettingsSheet.kt and LauncherScreen.kt before compilation. This is not suitable as the long-term architecture and remains Phase 0 cleanup.

Projectivy 4.72 is minSdk 23 / targetSdk 37 and uses a customized Leanback/BrowseSupportFragment home architecture with substantially more Android component integration.

### Manifest/component model
Couchy currently declares a single exported MainActivity as HOME, DEFAULT, LEANBACK_LAUNCHER and LAUNCHER. It requests QUERY_ALL_PACKAGES, ACCESS_NETWORK_STATE, READ_TV_LISTINGS, INTERNET, KILL_BACKGROUND_PROCESSES and REQUEST_DELETE_PACKAGES. There are no declared Couchy services, providers, boot receivers, accessibility services, notification listeners or wallpaper/theme plugin components in the current manifest.

Projectivy has a much larger component graph including many activities, AccessibilityService, NotificationListenerService, boot/locale/storage/wallpaper receivers and ThemeContentProvider. This is a feature-architecture difference rather than evidence that Couchy is poorly structured.

### MainActivity
Couchy MainActivity is intentionally small. It:
- requests READ_TV_LISTINGS once;
- registers a dynamic package add/remove/change receiver;
- increments a Compose rescan tick;
- applies a persisted locale;
- consumes Back because HOME should not exit;
- enters Compose through LauncherApp.

Projectivy MainActivity is a shell/lifecycle/input layer around MainFragment and owns substantially more key, overlay, startup and interaction behavior.

Recommendation: preserve Couchy's small Activity. Do not turn it into a Projectivy-style god coordinator. New capabilities should be introduced through dedicated repositories/coordinators/services.

### Persistence
Couchy stores one serialized `LauncherConfig` JSON value inside Preferences DataStore. This is simple, typed and easy to reason about. `normalize()` bounds invalid values and deduplicates row order. `ignoreUnknownKeys` helps forward/backward backup compatibility, and the legacy `builtinSource` field has already been retained for older Couchy backup decoding.

Weakness: there is no explicit configuration schema version or ordered migration registry comparable to Projectivy's version-by-version migration system. Parsing failure falls back to a default config, which is too destructive once the launcher becomes substantially more configurable.

Recommendation: keep DataStore + typed LauncherConfig, but add `schemaVersion`, ordered idempotent migrations, last-known-good rollback and explicit import/export migration.

### App discovery/performance
Couchy's AppRepository contains several strong implementation decisions worth retaining:
- scans both LEANBACK_LAUNCHER and regular LAUNCHER activities;
- deduplicates by package;
- excludes itself;
- uses `Dispatchers.IO.limitedParallelism(cores-1, minimum 2)` rather than unconstrained IO fan-out;
- reuses immutable AppEntry objects when package `lastUpdateTime` is unchanged;
- keeps an in-memory successful scan cache;
- uses disk artwork caching keyed by package, update timestamp and raster dimensions;
- pre-rasterizes banners/icons at useful display dimensions;
- calls `prepareToDraw()` before first composition;
- cleans stale artwork cache entries;
- precomputes tile/focus colours during scan instead of composition;
- returns the same list instance if scan content is unchanged, allowing Compose to skip downstream work.

These are examples where Couchy's implementation is preferable to copying Projectivy's architecture literally. We should retain these performance properties when the richer Projectivy-style app model is introduced.

### Launch/actions boundary
Couchy's `Actions` object already centralizes common operations: app launch, TV-program launch, app info, uninstall, background-process kill, system/network/VPN settings and store opening.

However, callers invoke these operations directly. There is no Projectivy-style launch coordinator/policy boundary for parental gating, generic source handling, startup requests, TV content and future shortcuts.

Recommendation: evolve rather than discard `Actions`:
`UI -> LaunchDispatcher -> LaunchPolicy -> platform-specific Actions/launchers`.
`LaunchPolicy` begins allow-all and later absorbs parental/device restrictions without coupling them to every UI caller.

## C1 conclusion
Couchy's outer architecture should remain the base: Compose, small MainActivity, typed config and performance-conscious app scanning. Projectivy should contribute missing subsystem boundaries and Android integrations, not replace Couchy's lightweight core wholesale.


---

# SOURCE FILE: `C02.md`

SHA-256: `2c9732f438cdfc075239ba4c06754821d09846e4d706fbf7dc181dc480e0e381`

---

# Couchy ↔ Projectivy comparison — C2 Home screen architecture

**Status:** COMPLETE / REVIEWED  
**Couchy/Aerial-Rows source pin:** `aerialrows-development` @ `3c1cfd56234f1d386f4553dd6c69523670500d70`  
**Projectivy comparison source:** completed `group02.md` (Projectivy 4.72/code 97 analysis)

## Scope

This group covers only the home-rendering architecture:

- `LauncherScreen.kt`
- `SlidingRows.kt`
- `AppCard.kt`
- the home-row portion of `RowQuickEditor.kt`
- category grouping from `AppRepository.kt`
- focus/navigation/rail/row ordering boundaries
- Compose state and recomposition characteristics

App acquisition internals, TV-provider query internals, settings architecture and media/wallpaper internals remain C3–C7.

---

## 1. Effective Couchy home architecture

The effective development build is currently split across **committed source plus Gradle source mutation**:

```text
MainActivity
  -> LauncherApp()                         LauncherScreen.kt
       -> config + app scan gates
       -> categorized app rows
       -> global launcher/menu/move state
       -> status bar + wallpaper shell
       -> layout selection
            -> Carousel/Grid/Dock          LauncherScreen.kt
            -> Sliding Rows                SlidingRows.kt
                 ^ wired by applyAerialRowsSourcePatches in build.gradle.kts
```

`SlidingRows.kt` is committed, but raw `LauncherScreen.kt` at the pinned commit does not itself contain the normal source-level `LAYOUT_SLIDING_ROWS -> SlidingRowsArea(...)` branch. `build.gradle.kts` injects that branch before compilation.

**Consequence:** the current repository does not have one authoritative Kotlin representation of the home architecture. Phase 0 must materialize the generated changes into normal source and delete the textual source patcher before deeper implementation work.

---

## 2. LauncherApp responsibilities

`LauncherApp()` is currently a very broad root coordinator. It owns or directly coordinates:

- typed config collection;
- app scanning and loading gate;
- setup-wizard gate;
- locale recreation;
- network/time/date state;
- new-install section assignment;
- menu state;
- app move/reorder state;
- row/category derivation;
- grid/dock layout calculations;
- wallpaper decoding/video/Aerial state;
- status bar;
- layout selection;
- dialogs/settings.

This is much smaller than Projectivy overall, but it concentrates far more responsibilities in one Compose function than Projectivy's `MainActivity -> MainFragment -> w40/i5/eb3/oo2/presenters` split.

### Recommendation

Keep Compose, but progressively split the root into ordinary Aerial-Rows boundaries:

```text
LauncherApp
  -> HomeScreenState / HomeController
  -> HomeContent
       -> HomeRail
       -> HomeRows
       -> StatusArea
  -> WallpaperHost
  -> LauncherOverlays
```

Do **not** recreate Leanback fragments/adapters simply because Projectivy uses them.

---

## 3. Couchy's current row model

`SlidingRows.kt` introduces a useful private sealed `HomeRowModel`:

```text
HomeRowModel
  |- WatchNextHomeRow
  |- ChannelHomeRow
  `- CategoryHomeRow
```

Every row has a stable string `id`, title and icon. Natural rows are formed from Watch Next, Android-TV channels and app categories. `homeRowOrder` is normalized against currently available row IDs so disappeared provider rows do not corrupt ordering and newly discovered rows append safely.

This is a good start and already closer to Projectivy's real architecture than `Category = List<App>`.

### Limitation

The model is private to the UI file and only covers three row sources. Future Projectivy-parity features would add Now Playing, TV inputs, shortcuts, folders and internal action rows, forcing more UI-specific branching.

### Recommended model

Promote a layout-independent model out of the composable file:

```text
HomeRow
  id: RowId
  source: RowSource
  presentation: RowPresentation
  items: List<HomeItem>

RowSource
  Apps(categoryId)
  WatchNext
  TvChannel(channelKey)
  NowPlaying
  Inputs
  Shortcuts
  Folder(...)
  InternalActions(...)

HomeItem
  App
  TvProgram
  NowPlaying
  TvInput
  Shortcut
  Folder
  InternalAction
  EmptyRowAction
```

Keep **row/source type**, **item type**, and **renderer type** separate, matching the important Projectivy Group-2 distinction without copying its class structure.

---

## 4. Stable identity — Couchy already gets much of this right

Good Couchy behavior:

- outer Sliding Rows `LazyColumn` uses `row.id` as the item key;
- app cards use category+package stable keys;
- TV programs use stable program IDs;
- `homeRowOrder` stores row IDs rather than brittle visible indices;
- `AppRepository` reuses unchanged `AppEntry` objects and may return the same list instance when content is unchanged.

Projectivy achieves the same goal through stable-ID adapters and reusable row wrappers. In Compose, Couchy's stable keys are the right primitive and should be retained.

### Missing piece

Couchy does **not** persist/coordinate the selected card identity per row. It mainly tracks `activeRowId` and a `FocusRequester` for each row's **first card**.

Projectivy restores by stable category ID + stable card ID.

### Required addition

Introduce stable focus state:

```text
HomeFocusState
  activeRowId: RowId?
  activeItemId: ItemId?
  lastItemByRow: Map<RowId, ItemId>
```

Focus restoration should always resolve IDs against the current visible model and fall back safely if a row/item disappeared.

---

## 5. Left rail state machine

Current Couchy state:

```text
railVisible: Boolean
activeRowId: String?
railFocus: FocusRequester
```

The first card of every row intercepts Left and calls `revealRail()`.

Important detail: when the rail is hidden, `revealRail()` only sets `railVisible = true`. It requests rail focus only if the rail was already visible. Therefore the current flow is effectively:

```text
Left #1 -> show rail, content card keeps focus
Left #2 -> request rail focus
```

Projectivy's Browse/headers system is a real pane transition with shown / hidden-but-revealable / disabled states, synchronized selection, and explicit focus ownership.

### Recommended Compose replacement

```text
RailState
  Disabled
  Hidden
  Revealing
  VisibleFocused
  Hiding
```

A Left action from the first item should initiate **reveal + focus transfer as one coordinated navigation action**. During transition, prevent the content pane and rail from simultaneously acting as independent focus owners.

Retain Couchy's Compose animation; reproduce Projectivy's state semantics rather than Leanback internals.

---

## 6. Rail row synchronization

Good behavior:

- `activeRowId` updates when a card/program gains focus;
- rail selection is based on row ID;
- choosing a rail item animates the vertical list to that row;
- row ordering persists by stable IDs;
- current-row tools operate on `activeRowId`.

### Focus-loss weakness

Choosing a rail row always focuses that row's **first card**, not the card last used in that row.

Closing the rail with Right/Back likewise sends focus to the row's first card.

Use `HomeFocusState.lastItemByRow` instead.

### Compose key weakness in the rail

The main home `LazyColumn` is keyed, but rail entries are emitted by `rows.forEachIndexed` inside a normal `Column` without `key(row.id)`. Each `RailItem` contains remembered focus state. When rows reorder, slot identity is therefore positional rather than row-ID-based.

Add:

```kotlin
key(row.id) { RailItem(...) }
```

and explicitly restore the active rail item after row reorder.

---

## 7. Vertical and horizontal focus geometry

This is one of the most useful findings in C2.

### Couchy's older Carousel/Grid path

`LauncherScreen.kt` already implements Projectivy-like controlled focus geometry:

- a custom vertical `BringIntoViewSpec` pins the focused row to a fixed pivot beneath the status area;
- `CarouselSection` supplies a horizontal `BringIntoViewSpec` so the focused card is moved directly to the 44dp start pivot in a single scroll;
- the vertical list has `focusRestorer()`.

The comments explicitly explain that this avoids two competing scroll animations and default "scroll only enough to reveal" behavior.

### SlidingRows path

`SlidingRows.kt` currently uses plain `LazyColumn` and plain `LazyRow` scrolling. It does not reuse those pivot specs.

### Recommendation

Do not invent a new focus-scroll algorithm. Move Couchy's existing pivot-scroll code into reusable components and use it in Sliding Rows:

```text
HomeVerticalBringIntoViewSpec
HomeHorizontalBringIntoViewSpec
```

Then layer stable row+item focus restoration on top.

This gives us Projectivy's predictable focus geometry using code patterns Couchy already uses successfully.

---

## 8. Empty rows

Before Sliding Rows receives app categories, `LauncherApp` filters out empty categories.

That avoids dead focus traps, which is good, but creates a configuration problem: once an app category becomes empty it disappears from the home/rail, so its quick editor is no longer reachable from the home screen.

Projectivy handles this more elegantly: a configured visible-but-empty category gets a concrete placeholder card which opens its population/settings UI.

### Recommendation

Distinguish between:

- **ephemeral source row empty** (Watch Next/provider channel) -> hide it;
- **user/configurable row empty** -> keep the row and insert an `EmptyRowAction` card such as "Add apps".

This preserves navigability without creating an empty focus container.

---

## 9. Row configuration model

Couchy currently has mostly global home-presentation settings:

- card size;
- spacing;
- corner radius;
- show category names;
- show app labels;
- panel style;
- global top margin;
- layout mode.

`RowQuickEditor` currently edits row contents/order for app categories and visibility/card count for TV-provider rows.

Projectivy's `i5` model is much richer per row: visibility, multiline mode, sliding behavior, title behavior, scale, margins, item count, max rows, background, etc.

### Recommendation

Do not add 30 Projectivy-like fields directly into `LauncherConfig`. Introduce:

```text
RowPresentation
  visible
  showTitle
  cardScaleOverride?
  startMargin?
  endMargin?
  topMargin?
  layoutMode?      // single row / grid where applicable
  itemsPerLine?
  maxRows?
  backgroundStyle?
```

Store overrides keyed by stable `RowId`; global values remain defaults.

---

## 10. Row reorder

Couchy persists reordered IDs through `homeRowOrder`; this is conceptually sound and simpler than RecyclerView adapter moves.

Projectivy uses adapter move notifications because Leanback needs them to preserve focus and animation.

Compose does not need that exact mechanism. With stable keys, Couchy can keep its immutable persisted list update.

What it **does** need is explicit stable focus restoration after order changes and keyed rail entries.

So this is **KEEP Couchy's data update model + ADD Projectivy's focus guarantees**, not a reason to emulate Projectivy's adapter operations.

---

## 11. Card presentation and long press

`AppCard.kt` contains several strong implementation choices worth preserving:

- TV Surface focus scaling;
- per-app sampled artwork glow;
- precomputed tile/glow data from the app scan rather than expensive composition-time image analysis;
- own render layer for artwork during focus scaling;
- a carefully engineered D-pad long-press path that prevents the release click from launching the app behind the menu;
- explicit moving-card border state;
- hidden-app dim overlay.

These are appropriate Compose equivalents to Projectivy presenter/card-focus behavior and should **not** be replaced by a generic Projectivy clone.

### Missing Projectivy feature

There is no general selected-card hover/details model equivalent to Projectivy's title/description/image hover presenter.

For app banners, `showLabel` does not produce a separate selected-app title/details area; it is used inside banner-less fallback tiles.

### Recommendation

Later add one shared `SelectedItemDetails`/`HoverDetails` composable driven by `HomeFocusState.activeItemId`, rather than putting more metadata state inside every card.

---

## 12. Row source and item renderer scaling

Current `SlidingRowsArea` chooses rendering with a `when(row)`:

- app category -> `AppCategoryRow` / `AppCard`;
- Watch Next/channel -> `TvContentRow` / `TvProgramCard`.

This is fine at today's feature count. It will become unwieldy when Now Playing, inputs, shortcuts, folders and internal actions arrive.

### Recommended boundary

Keep sealed models and Compose dispatch, but separate model from rendering:

```text
HomeRowFactory / HomeRepository
      -> List<HomeRow>

HomeRows composable
      -> HomeRowRenderer
            -> item renderer by HomeItem subtype
```

Avoid a Projectivy-style presenter framework unless Compose actually needs it.

---

## 13. Status-area navigation

Couchy's status bar is architecturally simple and has a clever navigation detail: when hidden, its container collapses to a 4dp focusable strip, so Up from the top row can reveal it again.

Keep that idea.

What should change is ownership: row/rail/status focus transitions should eventually be coordinated by one `HomeFocusCoordinator`, rather than each area relying only on default Compose focus search.

---

## 14. Recomposition/performance observations

### Keep

Couchy's existing performance choices are good:

- stable lazy keys;
- immutable `AppEntry`;
- reusable app entries/cache;
- `remember(categorized)` / `remember(dockApps)`;
- hoisted Brushes instead of frame-time allocation;
- GPU-friendly artwork handling and `prepareToDraw()`;
- controlled focus scaling through render layers.

### Improve

`SlidingRowsArea` rebuilds `naturalRows`, ID lists, rank maps and sorted `rows` on recomposition. They are small, but the launcher root also observes time/date and other frequently changing state.

Use `remember(tvContent, categorized, config.homeRowOrder)` for the stable row model and derived rank/order structures.

This is a modest optimization, not a rewrite priority.

---

## 15. Direct Couchy vs Projectivy decision table

| Area | Couchy today | Projectivy | Aerial-Rows decision |
|---|---|---|---|
| UI technology | Compose | customized Leanback | **KEEP Couchy** |
| Row identity | stable string keys | stable row adapter IDs | **KEEP Couchy** |
| Card identity | package/program IDs | stable card IDs | **KEEP + formalize ItemId** |
| Row model | 3 private row source types | rich configurable category model | **EXTEND Couchy** |
| Item model | renderer-specific lists | common `t4` hierarchy | **ADD sealed HomeItem** |
| Left navigation | animated Boolean overlay | shown/hidden/disabled browse pane | **ADD Projectivy state semantics** |
| Rail focus | first card only | category+card refocus | **ADD stable item restoration** |
| Scroll geometry | default in Sliding Rows | deliberate vertical/horizontal alignment | **REUSE Couchy's existing pivot code** |
| Empty user rows | dropped | placeholder action card | **ADOPT placeholder behavior** |
| Reorder persistence | immutable ID list | adapter moves + stable IDs | **KEEP Couchy + restore focus explicitly** |
| Per-row layout | mostly global | rich per-category layout | **ADD RowPresentation overrides** |
| Card visuals | efficient Compose glow/scale | presenter-driven effects | **KEEP Couchy** |
| Hover/details | mostly absent | dedicated hover presenter | **ADD one shared details surface later** |
| Status-bar reveal | 4dp hidden focus strip | title/header state system | **KEEP Couchy idea** |
| Transition safety | implicit/default Compose | freezes pane focus during transition | **ADD explicit rail/content focus ownership** |

---

## 16. Proposed C2 target architecture

```text
LauncherApp
  |
  +-- HomeController
  |     +-- HomeRowsRepository
  |     +-- HomeFocusCoordinator
  |     +-- HomeRailController
  |
  +-- HomeScreen
        +-- StatusArea
        +-- HomeRows
        |     +-- HomeRowRenderer
        |            +-- AppCard
        |            +-- TvProgramCard
        |            +-- future item renderers
        +-- HomeRail
        +-- SelectedItemDetails

HomeRow
  +-- RowId
  +-- RowSource
  +-- RowPresentation
  `-- List<HomeItem>

HomeFocusState
  +-- activeRowId
  +-- activeItemId
  `-- lastItemByRow
```

This preserves Couchy's lightweight Compose implementation while introducing only the Projectivy architectural ideas that solve real navigation/configuration problems.

---

## 17. Priority implementation consequences

When implementation begins, the home-screen order should be:

1. **Phase 0:** remove Gradle source mutation and wire Sliding Rows in committed Kotlin.
2. Extract `HomeRow`/`HomeItem` stable domain models.
3. Add `HomeFocusState` + `HomeFocusCoordinator` using stable row/item IDs.
4. Reuse Couchy's existing horizontal/vertical `BringIntoViewSpec` pivot logic inside Sliding Rows.
5. Replace `railVisible: Boolean` with a real rail state/focus transition.
6. Key rail entries by `row.id` and restore focus after row reorder.
7. Add `EmptyRowAction` for user-configurable empty categories.
8. Introduce per-row `RowPresentation` overrides only as settings/features require them.
9. Add optional shared hover/details surface after the core navigation is stable.

Do not add Now Playing, inputs, shortcuts or other future row types before the generic row/item/focus model exists.

---

## C2 closure

C2 is considered complete because the home dependency graph has been traced through:

```text
LauncherApp
  -> categorized app model
  -> effective layout selection
  -> SlidingRowsArea
       -> HomeRowModel
       -> row ordering
       -> active-row state
       -> rail state/actions
       -> AppCategoryRow / TvContentRow
       -> stable lazy keys
       -> AppCard / TV-program boundary
       -> RowQuickEditor boundary
  -> alternate Carousel/Grid focus geometry
  -> status-area focus boundary
```

Deferred intentionally:

- app discovery/action semantics -> C3;
- TvProvider acquisition/refresh -> C4;
- wallpaper/Aerial engine -> C5;
- settings system -> C6;
- media/notifications -> C7;
- inputs/device integration -> C8;
- HOME/Accessibility/remote lifecycle -> C9.

**C2 status: COMPLETE + REVIEWED.**


---

# SOURCE FILE: `C03.md`

SHA-256: `c756b965caa99ac016eb312a9828be05cfcd6618d666c5b41b1ca333c6761f03`

---

# Couchy ↔ Projectivy comparison — C3: Installed apps, categories & card actions

Status: **COMPLETE + REVIEWED**  
Couchy/Aerial-Rows source pin: `kanozseyer/Aerial-Rows` branch `aerialrows-development` @ `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Projectivy reference: Group 03 original + post-closure audit after Groups 4–12  
Projectivy APK: 4.72 / code 97

## Source provenance used

Pinned Couchy files:

- `AppRepository.kt` — blob `6467d2787ce9fc920c025e8cb994c2cf520005cb`
- `Config.kt` — blob `4710f41198308e555ac9e3b12d515f1fefa1db6e`
- `MainActivity.kt` — blob `e5b83f7aa597ad245b607f30a65c81d878bdb303`
- `Actions.kt` — blob `78fb2710b0fec7c603ce5641d96ae7c672f642d0`
- `Menus.kt` — blob `b847347b94b1be51cd03c180593500d8d6055de3`
- `LauncherScreen.kt` — blob `a3172678c0bde47a28fa89247b4c5662cc659b1b`
- `SettingsSheet.kt` — blob `a20140273352bd63c990b2c3e9037c1eb717836c`
- `RowQuickEditor.kt` — blob `515c796d7aa2f5b755e9858f2be435a0f72e19a8`
- `AndroidManifest.xml` — blob `5fb2138676d6e929b3e4fa61a865b32837274421`

Projectivy reports:

- `group03.md` SHA-256 `d3beda827648c7612494e1e97c5898255af840efdab91b71fbc352ddfc50afc9`
- `group03_postclosure_audit.md` SHA-256 `b7bbc6d739a90bc64f3c52f8d56551d670ea9a126d8769726fe02734923339f3`

---

## Executive conclusion

Couchy should remain the implementation base for installed-app rendering and artwork performance.

Projectivy has the stronger **app-domain architecture**:

- better package/shortcut invalidation;
- exact component-aware backing;
- category-local visibility semantics;
- richer sorting and usage metadata;
- generic launch gating;
- generic card actions;
- shortcuts/startup/parental/customization integration.

Couchy has the stronger **render/cache implementation**:

- immutable app entries;
- reuse by `lastUpdateTime`;
- bounded parallel artwork decode;
- display-sized banner rasterization;
- lossless disk cache;
- stale-file cleanup;
- `prepareToDraw()` before first frame;
- precomputed artwork focus colours;
- stable list/object reuse to reduce Compose recomposition.

The target therefore is:

```text
Projectivy app-domain boundaries
        +
Couchy scan/artwork/cache implementation
        +
Compose-native stable identity/focus semantics
```

There is no reason to replace `AppRepository` wholesale. It should be split and strengthened.

---

# 1. App discovery

## Couchy

`AppRepository.scan()` queries:

```text
MAIN + LEANBACK_LAUNCHER
MAIN + LAUNCHER
```

into a `LinkedHashMap<String, ResolveInfo>` keyed by package.

Because Leanback is queried first and later results do not replace an existing package, Couchy already implements:

```text
TV / Leanback result wins over ordinary launcher result
        +
package-level deduplication
```

That is conceptually similar to Projectivy's TV-over-mobile precedence.

### Strong Couchy behavior to KEEP

Couchy then performs expensive label/artwork work concurrently on a bounded dispatcher:

```text
Dispatchers.IO.limitedParallelism(cores - 1, minimum 2)
```

and reuses a previous `AppEntry` when the package `lastUpdateTime` is unchanged.

This is excellent for weak TV hardware and should survive the refactor.

### Couchy limitation

The selected `ResolveInfo` is used for label/artwork/category discovery, but `AppEntry` stores only:

```text
pkg
label
banner/icon
classification fields
```

It drops the exact activity/component identity.

`Actions.launchApp()` later asks PackageManager again for the package-level launch intent.

Therefore Couchy can theoretically display metadata from one launcher activity while launching another package default activity.

## Projectivy

Projectivy keeps the exact `ActivityInfo` as the backing object of its app card, while still deduplicating ordinary app rows primarily at package level.

### Target improvement

Introduce an identity with component context:

```text
AppId(
    packageName,
    componentName,
    user/profile identity optional
)
```

Default UI policy may still collapse to one card per package, but the underlying launch target should not discard the component.

Recommended stable ID string:

```text
app:<package>/<activity>
```

rather than package hash or package string alone.

---

# 2. Package visibility / QUERY_ALL_PACKAGES

Couchy's manifest currently declares:

```text
android.permission.QUERY_ALL_PACKAGES
```

although its actual scanner only needs launcher-visible activities.

Projectivy performs launcher intent queries without relying on broad `QUERY_ALL_PACKAGES` access.

## Recommended target

Test removal of `QUERY_ALL_PACKAGES` after introducing explicit manifest `<queries>` for:

```text
MAIN + LEANBACK_LAUNCHER
MAIN + LAUNCHER
```

and use `LauncherApps` where appropriate.

Keep broad visibility only if a later concrete feature proves it necessary.

---

# 3. Package-change observation

## Couchy

`MainActivity` owns a dynamic broadcast receiver for:

- `PACKAGE_ADDED`
- `PACKAGE_REMOVED`
- `PACKAGE_CHANGED`

It increments `rescanTick`, causing `AppRepository.scan()` to run again.

### Good property

A full scan is cheaper than it looks because unchanged `AppEntry` objects and artwork are reused.

### Important correctness hole

The receiver only rescans a changed package when it is *currently launchable*:

```text
if (PACKAGE_REMOVED || launchable) rescanTick++
```

Therefore if `PACKAGE_CHANGED` makes a previously visible app **non-launchable**, the launcher can retain a stale card because `launchable == false` and no rescan occurs.

This is a real bug.

### Other missing package-event coverage

Compared with Projectivy, Couchy does not explicitly cover:

- `LauncherApps.Callback.onPackagesAvailable`
- `onPackagesUnavailable`
- shortcut-change notifications
- the same ~500 ms debounce/aggregation
- targeted package/shortcut cache invalidation

## Projectivy

Projectivy registers a `LauncherApps.Callback`, translates events into one internal package-change pipeline, batches changes and debounces roughly 500 ms before refreshing affected content.

## Recommended target

Move package observation out of `MainActivity` into a repository/coordinator:

```text
PackageObserver
    ↓
LauncherApps.Callback
    ↓
MutableSharedFlow<PackageChange>
    ↓
~300–500 ms debounce
    ↓
AppRepository.invalidate(packages)
ShortcutRepository.invalidate(packages)
IconResolver.invalidate(packages)
```

Keep Couchy's cache reuse during the subsequent scan/materialization.

This gives us Projectivy's event correctness without sacrificing Couchy's rendering performance.

---

# 4. Artwork pipeline

Couchy's artwork path is one of the strongest parts of the current codebase.

## Couchy strengths to KEEP

- size-aware banner rasterization;
- density-aware cache key;
- banner/icon lossless WebP disk cache;
- stale cache cleanup;
- pre-upload via `prepareToDraw()`;
- one immutable `AppEntry` reused when package stamp is unchanged;
- artwork glow sampled once during scanning, never during composition;
- same list instance returned when nothing changed.

These are all worth retaining.

## Projectivy additions missing from Couchy

Projectivy adds two launcher-level artwork layers above normal package art:

```text
per-card custom artwork
        ↓
global icon-pack mapping
        ↓
original app banner/icon
```

It also has tri-state crop overrides and custom card title metadata.

## Target design

Split discovered data from user customization:

```text
DiscoveredApp
    original label
    original banner/icon
    component/package metadata

CardCustomization
    custom title?
    custom artwork URI?
    crop override?

IconResolver
    per-card override
    icon-pack mapping
    discovered original art
```

Do not bake user customizations into `AppEntry` scanning.

---

# 5. Category model

## Couchy

Couchy already supports a useful model:

```text
categories: List<CategoryCfg>
sections: package -> Set<categoryId>
order: categoryId -> List<package>
```

An app can belong to multiple categories.

This is simpler and in several ways cleaner than Projectivy's separate inclusion/exclusion integer lists.

### Good behavior to KEEP

`sectionsOf()` distinguishes:

```text
no stored entry
    -> auto-category fallback

stored empty set
    -> intentionally appears nowhere
```

That is a useful semantic distinction.

The category UI can:

- add a section;
- rename it;
- reorder sections;
- delete sections;
- toggle app membership;
- expose an auto-filled All Apps section.

`RowQuickEditor` also supports per-row membership and order edits directly from the rail.

## Projectivy additions

Projectivy distinguishes:

- editable/custom rows;
- generated rows;
- explicit inclusion/order;
- category-local exclusions for generated rows.

That becomes important for Hide semantics.

---

# 6. Important Couchy category invariant bug

Couchy currently does not guarantee at least one editable category.

The delete button is shown when:

```text
config.categories.size > 1
```

but the special All Apps row lives inside that same list.

A reachable sequence is:

```text
normal category + All Apps
        ↓
delete normal category
        ↓
only All Apps remains
        ↓
toggle All Apps off
        ↓
categories == empty
```

Consequences:

- no app rows are materialized;
- later new-install handling calls `cfg.categories.first()` and can throw;
- category fallback assumptions no longer hold.

## Target fix

`ConfigStore.normalize()` must enforce a structural invariant:

```text
at least one editable app category exists
```

Better still, model generated **All Apps** as a system row separate from the editable-category list.

Suggested split:

```text
editableCategories: List<UserCategory>
systemRows: generated configuration
```

---

# 7. Stale category references

Deleting a Couchy category removes it from `categories` but does not actively remove:

- that category ID from every package `sections` entry;
- that category's `order` map entry.

`sectionsOf()` filters invalid IDs at read time, so stale references usually do not render, but config data can accumulate stale relationships.

There is also an ambiguity:

```text
stored empty set because user intentionally removed app everywhere
```

versus

```text
stored set became empty only because its category was deleted
```

## Target fix

Category deletion should be one transactional domain operation:

```text
DeleteCategory(categoryId)
    remove category
    remove membership references
    remove order entry
    decide explicit re-home policy for orphaned apps
```

and `normalize()` should repair impossible references on load.

---

# 8. New-install behavior

## Couchy

After initial baseline discovery, newly seen packages are persisted into the **first configured category**.

This differs from the initial population:

- existing apps with no saved membership use `autoCategory`;
- later installs are explicitly assigned to the first category.

That produces an implicit two-policy system.

It can also cause a new app to appear briefly under its auto-category and then move after the `LaunchedEffect` writes first-category membership.

### Reinstall behavior

Removed app membership/order data can remain in config, but when the package is later seen as newly added, Couchy overwrites its membership with the first category.

That can destroy the user's former placement after reinstall.

## Target design

Make this explicit:

```text
NewInstallPolicy
    AUTO_CLASSIFY
    FIRST_EDITABLE_ROW
    ALL_APPS_ONLY
```

and only assign a default when no remembered placement exists.

If a previously known package returns, restore its old placement unless the user explicitly chose otherwise.

---

# 9. Classification policy

Couchy combines:

- hard-coded known package mappings;
- `ApplicationInfo.category`;
- game flag fallback;

into `autoCategory`.

This is useful, but it couples **package discovery** to **home-organization policy**.

Projectivy's discovery layer is more neutral; its built-in TV/mobile rows are source classifications rather than content heuristics.

## Target improvement

Split:

```text
AppDiscoveryRepository
```

from:

```text
AppClassifier / NewInstallPlacementPolicy
```

Then Couchy's heuristics can remain as one classification strategy without contaminating the package scanner.

---

# 10. Sorting

## Couchy

Normal categories currently sort by:

```text
explicit saved order first
then firstInstallTime
```

All Apps sorts alphabetically.

This is a nice default because newly installed apps naturally fall after older unpinned entries.

## Projectivy

Projectivy exposes:

- manual/custom;
- alphabetical;
- last used;
- most used;
- randomized/shuffle.

It persists its own launch counts/timestamps rather than depending on Android UsageStats.

## Target

Use an explicit row sort mode:

```text
RowSortMode
    MANUAL_THEN_INSTALL
    ALPHABETICAL
    LAST_USED
    MOST_USED
    RANDOM
```

Couchy's current behavior becomes a supported mode, not an implicit hard-coded algorithm.

Usage accounting should be stored separately from discovered app metadata.

Per the Projectivy post-closure finding, prefer counting a use **after successful dispatch**, rather than Projectivy's quirk of incrementing before launch is confirmed.

---

# 11. Hide semantics — major behavioral mismatch

## Couchy

Couchy's:

```text
hidden: Set<String>
```

is global.

Long-press **Hide** toggles that package in the global hidden set, so the app disappears from every normal category and All Apps unless `showHidden` is enabled.

## Projectivy

Projectivy's ordinary Hide behavior is principally **category-local**:

- generated row -> exclusion from that row;
- custom/favorites row -> remove membership / Remove favorite.

## Recommended target

Support both semantics explicitly:

```text
GlobalSuppression
    hiddenEverywhere

RowMembership/Visibility
    included item IDs
    excluded item IDs
    explicit order
```

Context-menu wording should be unambiguous:

```text
Hide from this row
Remove from this row
Hide everywhere   (optional advanced action)
```

Do not map Projectivy's `Hide` onto Couchy's existing global hidden flag.

---

# 12. Move mode — confirmed correctness bug

Couchy uses:

```text
movePkg: String?
```

as the entire move identity.

But Couchy also permits an app to appear in several sections.

`findMoving()` scans categories from the beginning and returns the **first visible occurrence** of `movePkg`.

Therefore if a user long-presses the same app in a later row and chooses Move:

```text
requested row != necessarily row that findMoving() edits
```

There is a second problem: every visible card with that package sees:

```text
movePkg == app.pkg
```

and can receive the same `moveFocus` requester / moving styling.

This is a real row-context bug.

## Projectivy

Projectivy's generic card actions retain current category context when hiding, moving or reordering.

## Target fix

Replace package-only move state with:

```text
MovingItem(
    rowId,
    itemId
)
```

For an app:

```text
itemId = app:<package>/<component>
```

All move/reorder functions must take the originating row explicitly.

This also aligns with the C2 `HomeFocusState` design.

---

# 13. Couchy move behavior worth keeping

The move implementation itself has several good ideas:

- horizontal reorder in a row;
- vertical movement between rows;
- grid movement by calculated column count;
- dock movement over a flattened sequence;
- persistent order updates rather than fragile visual-only swaps;
- key-up handling when leaving move mode to avoid accidental app launch.

These should be retained once the move identity becomes row-scoped.

---

# 14. Context actions

## Couchy current app menu

`AppContextMenu` exposes:

- Open
- Move
- Hide / Unhide
- App Info
- Close
- Uninstall

The dialog contains careful held-OK suppression so the release from the long press does not immediately activate the first menu item.

That is good TV UX and should be preserved.

## Projectivy app/card actions

Projectivy's generic card-action architecture additionally supports, depending on card type/context:

- select as the one global startup target;
- parental protection;
- hide/remove from current row;
- add to another category;
- move to another category;
- reorder;
- custom title;
- custom icon;
- crop override;
- app-published shortcuts;
- shortcut pin/unpin behavior for shortcut cards.

## Target architecture

Do not keep expanding one app-only `AppContextMenu` signature.

Move toward:

```text
HomeItemActionProvider
    actionsFor(item, rowContext)
        -> List<HomeAction>
```

with render/UI separated from action availability.

This allows apps, shortcuts, TV programs, inputs and Now Playing cards to share one action-surface architecture without a giant conditional composable.

---

# 15. App Info / uninstall / Close

## Couchy

App Info launches:

```text
Settings.ACTION_APPLICATION_DETAILS_SETTINGS
package:<pkg>
```

Uninstall launches:

```text
ACTION_DELETE
package:<pkg>
```

Close uses:

```text
ActivityManager.killBackgroundProcesses(pkg)
```

and correctly comments that this is best-effort, not a real force stop.

## Projectivy

Projectivy likewise provides normal App Info and uninstall actions; later privileged package-freeze utilities are separate maintenance tools, not normal app-card Force Stop.

## Decision

Keep Couchy's **Close** only as an optional Couchy/Aerial-Rows enhancement and label/document it accurately.

Do not confuse it with a Projectivy parity requirement or true Force Stop.

---

# 16. Launch architecture

## Couchy

`Actions.launchApp(context, pkg)` resolves:

```text
getLeanbackLaunchIntentForPackage(pkg)
    ↓ fallback
getLaunchIntentForPackage(pkg)
```

and starts it directly.

## Projectivy

Projectivy's ordinary app card retains the exact activity backing and launches through a generic launch path which can enforce parental policy.

The wider audit established that all launch sources should eventually converge through one policy/dispatch boundary.

## Target

```text
UI / remote / startup / TV content / shortcut / input
        ↓
LaunchDispatcher
        ↓
LaunchPolicy
        ↓
AppLauncher / ShortcutLauncher / InputLauncher / TvContentLauncher
```

For ordinary apps, the dispatcher should receive the retained component-aware `AppId`.

Initially `LaunchPolicy` can allow everything.

---

# 17. Usage metadata and customizers

Couchy does not currently maintain Projectivy-style per-card:

- custom title;
- custom artwork;
- crop override;
- launch count;
- last-launch timestamp.

Projectivy stores those separately from discovered app metadata.

## Recommended target

```text
CardCustomizationRepository
    custom title
    artwork override
    crop override

LaunchUsageRepository
    successful launch count
    last successful launch timestamp
```

Do not put mutable user state into `AppEntry`.

This keeps Couchy's immutable app-scan objects maximally reusable.

---

# 18. App shortcuts

Couchy currently has no equivalent of Projectivy's complete LauncherApps shortcut architecture in the app-card subsystem.

Projectivy has two related concepts:

1. app-published shortcuts exposed from an app's action menu;
2. pinned shortcuts as first-class home cards.

The latter belongs mainly to C10, but C3 needs the app-action boundary to be ready for it.

Recommended stable identity:

```text
shortcut:<package>:<profile>:<shortcutId>
```

---

# 19. Startup target / parental control

Couchy's current app menu does not implement either.

Projectivy's **Start at boot** action is not a per-app Boolean. It selects one global startup target.

Target domain:

```text
StartupTarget?
    App(AppId)
    Input(InputId)
    LastInput
    Ambient
```

Parental protection similarly belongs in `LaunchPolicy`, not inside `AppCard`.

Both can be introduced later without changing the C3 action surface architecture if actions are data-driven.

---

# 20. All Apps / app-management screen

Couchy's Apps screen is already useful and simple:

- stable package-keyed rows;
- app artwork;
- displayed section memberships;
- global hidden status;
- click to launch;
- hide/unhide button;
- App Info;
- uninstall.

Projectivy's All Apps screen additionally reuses the generic card-action system on long press.

## Target improvement

Keep Couchy's compact management screen, but route row long-press / action button through the same `HomeItemActionProvider` used by home cards.

That avoids duplicate action logic.

---

# 21. Final keep / extend / replace decisions

| Area | Couchy decision | Projectivy contribution |
|---|---|---|
| artwork decode/cache | **KEEP** | add override/icon-pack layers |
| immutable `AppEntry` reuse | **KEEP** | none needed |
| launcher intent discovery | **EXTEND** | source/event rigor |
| package dedup / Leanback precedence | **KEEP** | retain component internally |
| app identity | **REPLACE package-only identity** | exact activity backing / stable IDs |
| package receiver | **REPLACE architecture** | `LauncherApps.Callback` + debounce |
| auto classification | **MOVE OUT of repository** | neutral discovery separation |
| multi-section membership | **KEEP** | add generated-row exclusion semantics |
| global hidden | **KEEP only as explicit global action** | add row-local hide/remove |
| category create/rename/reorder | **KEEP** | richer row configuration later |
| category deletion | **FIX** | enforce referential integrity |
| All Apps category | **REMODEL as generated row** | aligns with generated-row concept |
| sorting | **EXTEND** | alpha / usage / random modes |
| new-install placement | **MAKE EXPLICIT POLICY** | generated/discovered behavior inspiration |
| move mode mechanics | **KEEP** | retain row context |
| move identity | **FIX** | `MovingItem(rowId,itemId)` |
| long-press held-key protection | **KEEP** | none needed |
| context menu | **GENERALIZE** | generic card action architecture |
| App Info / uninstall | **KEEP** | equivalent behavior |
| Close | **OPTIONAL KEEP** | not Projectivy parity |
| direct launch helper | **REPLACE boundary** | LaunchDispatcher + LaunchPolicy |
| usage stats | **ADD** | Projectivy model, but count successful dispatch |
| custom title/icon/crop | **ADD later** | Projectivy customizer model |
| app shortcuts | **ADD later** | LauncherApps shortcut architecture |
| startup target | **ADD later** | one typed global target |
| parental control | **ADD later** | generic LaunchPolicy |
| QUERY_ALL_PACKAGES | **TRY TO REMOVE** | Projectivy proves narrower path viable |

---

# 22. Recommended target classes after C3

```text
apps/
├─ AppDiscoveryRepository
├─ AppEntry / DiscoveredApp
├─ AppId
├─ AppClassifier
├─ PackageObserver
├─ AppArtworkCache
└─ AppPlacementPolicy

home/
├─ HomeItemId
├─ RowId
├─ RowMembershipRepository
├─ MovingItem
└─ HomeItemActionProvider

launch/
├─ LaunchDispatcher
├─ LaunchPolicy
└─ AppLauncher

customization/
├─ CardCustomizationRepository
└─ LaunchUsageRepository
```

This is not a requirement to create every file immediately; it is the domain separation implied by the comparison.

---

# 23. Implementation order implied by C3

When implementation begins:

1. materialize the current Gradle-injected source first (Phase 0);
2. introduce typed `AppId`, `RowId`, `HomeItemId`;
3. fix config invariants (at least one editable category; All Apps generated separately);
4. replace `movePkg` with `MovingItem(rowId,itemId)`;
5. separate app discovery from auto-classification;
6. move package observation out of `MainActivity` and introduce debounced `LauncherApps.Callback` invalidation;
7. preserve Couchy's artwork/cache pipeline unchanged where possible;
8. introduce `LaunchDispatcher + allow-all LaunchPolicy`;
9. generalize item actions without yet implementing every Projectivy action;
10. add row-local visibility semantics while retaining optional global hide;
11. add explicit `RowSortMode` and `NewInstallPolicy`;
12. later phases can plug in shortcuts, parental policy, startup target and card customizers.

---

# Final assessment

```text
Couchy C3 subsystem quality:       strong implementation, under-modelled domain
Projectivy C3 architecture:         richer and more extensible
Best base for scanning/artwork:     Couchy
Best base for actions/policy:       Projectivy-inspired domain boundaries

Confirmed Couchy correctness bugs:
  1. PACKAGE_CHANGED can miss removal of launchability
  2. package-only move state loses source-row context
  3. multiple occurrences can share moving state/focus requester
  4. zero-category state is reachable

Confirmed structural weaknesses:
  - package-only app identity
  - global-only Hide semantics
  - Activity-owned coarse package observation
  - no explicit sort/new-install policy model
  - stale category/order references can persist

Major missing Projectivy capabilities:
  - generic launch gate/policy
  - usage sorting
  - card customizers
  - shortcuts
  - startup target
  - parental policy
  - richer row-local actions

Recommended direction:
  KEEP Couchy's cache/render path
  EXTEND its category model
  REPLACE package-only identity and move state
  ADD Projectivy-style observation/action/policy boundaries
```

**C3 status: COMPLETE + REVIEWED.**


---

# SOURCE FILE: `C04.md`

SHA-256: `935945ec232fe56e6e64e2112eb19179a3297e6194594234d239b9cde5ebd297`

---

# Couchy vs Projectivy — C4: Android TV Channels & Watch Next

Status: **COMPLETE + REVIEWED**

Couchy/Aerial-Rows source pin:
- repository: `kanozseyer/Aerial-Rows`
- branch under study: `aerialrows-development`
- commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`

Projectivy comparison baseline:
- Group 04: Android TV Channels & Watch Next
- post-closure audit after Groups 5–12

## Scope

This comparison closes the Couchy side of:

- `READ_TV_LISTINGS` permission handling;
- TV-provider capability gating;
- Preview Channel discovery;
- channel identity and persistence;
- per-channel visibility and limits;
- Preview Program queries;
- Watch Next queries;
- ordering, filtering, deduplication and limits;
- provider policy;
- home-row construction;
- program-card identity, artwork, progress and launch;
- settings integration;
- cache/refresh behavior;
- comparison with Projectivy Group 4.

Files inspected:

- `MainActivity.kt`
- `Config.kt`
- `TvContentRepository.kt`
- `SlidingRows.kt`
- `TvProgramCard.kt`
- `AerialSettingsExtras.kt`
- `RowQuickEditor.kt`
- `Actions.kt`
- `AndroidManifest.xml`

---

# 1. Executive conclusion

Couchy already has a useful minimal Android-TV-content implementation, but it is best described as:

> a simple snapshot reader for currently browsable TV-provider data

rather than a complete provider subsystem.

Its strengths are simplicity, typed immutable models, direct integration into Compose rows, explicit per-channel display limits and a lightweight TV-program card.

Its main weaknesses are:

1. channel discovery incorrectly excludes non-browsable channels;
2. settings discovery also drops channels that currently have no programs;
3. durable channel identity is tied to the numeric Android channel ID;
4. Preview/Watch Next query ordering is unspecified;
5. Watch Next has no per-provider cap or provider exclusion;
6. there is no provider-content observer or resume refresh;
7. program launch is not constrained back to the provider package and bypasses the future `LaunchDispatcher/LaunchPolicy` boundary;
8. program identity is raw provider row ID rather than structured source identity;
9. Preview Programs and Watch Next are queried with a very small metadata projection and have no details tier;
10. TV artwork loading decodes the full source bitmap before scaling and uses only an 18-entry count-based memory cache.

The best target is therefore:

```text
Couchy Compose rows/cards
        +
Couchy typed immutable state
        +
Projectivy TV-provider behavior/policies
        +
our own stronger identity + observer architecture
```

---

# 2. Current Couchy architecture

The current source path is:

```text
MainActivity
    ↓ READ_TV_LISTINGS request once
SlidingRowsArea
    ↓ produceState
TvContentRepository.load(config)
    ├─ Watch Next query
    └─ Preview Channel discovery
           ↓
       per-channel Preview Program query
           ↓
TvContentRows
    ├─ WatchNextHomeRow
    └─ ChannelHomeRow
           ↓
       TvContentRow
           ↓
       TvProgramCard
           ↓
       Actions.launchTvContent()
```

The structure is small and understandable, which is valuable and should be preserved.

---

# 3. Capability and permission handling

Couchy currently uses two effective gates:

```text
SDK >= 26
+
provider query succeeds
```

`MainActivity` asks for `android.permission.READ_TV_LISTINGS` once and then recreates the Activity if permission is granted.

A SharedPreferences flag prevents another automatic prompt after the first attempt.

## Strength

This keeps the Activity logic compact and avoids repeatedly prompting the user.

## Weakness

The repository has no explicit capability state such as:

```text
UNSUPPORTED
PERMISSION_REQUIRED
AVAILABLE
ERROR
```

Security failures are swallowed through `runCatching` and become empty rows.

This creates an ambiguous UI:

```text
permission denied
        ↓
empty TV content
```

which looks identical to:

```text
permission granted
but no TV content exists
```

The Channels settings screen also remains enabled even when permission is unavailable.

## Recommended target

```text
TvProviderAvailability
    ├─ platformSupported
    ├─ permissionGranted
    ├─ providerReadable
    └─ userEnabled
```

Expose this state to settings and provide a deliberate permission/open-settings recovery action.

Do not hide permission failures as empty data.

---

# 4. Channel discovery — major correctness gap

Couchy queries:

```text
TvContract.Channels.CONTENT_URI
```

with:

```text
_id
display_name
browsable
type
package_name
```

It keeps only:

```text
type == TYPE_PREVIEW
browsable != 0
```

This differs materially from Projectivy.

Projectivy discovers Preview Channels whether or not Android currently marks them browsable, then can call:

```text
TvContract.requestChannelBrowsable(context, channelId)
```

when the user enables one.

## Consequence in Couchy

The exact channels that most need a management UI—non-browsable Preview Channels—never reach `ChannelsSettingsScreen`.

Therefore the current settings description "show every readable channel" is too strong. It actually means:

> every currently browsable Preview Channel with at least one currently accepted program.

## Required change

Split:

```text
channel discovery
```

from:

```text
home eligibility
```

Discovery should include non-browsable Preview Channels.

When a user enables one, call `TvContract.requestChannelBrowsable()` where supported.

---

# 5. Empty channels are incorrectly hidden from management

`queryChannels()` immediately queries programs for every discovered channel and then does:

```text
if (programs.isEmpty()) continue
```

`discoverChannels()` calls the same method.

Therefore a valid Preview Channel disappears from settings whenever it has no currently accepted programs.

This couples two concepts that should be separate:

```text
channel exists
!=
channel currently has displayable programs
```

## Recommended split

```text
TvChannelRepository.discoverChannels()
    -> metadata only

TvProgramRepository.loadPreviewPrograms(channelHandle)
    -> content only
```

Settings should use metadata discovery only.

Home can suppress a row when its current program list is empty without losing the persisted channel preference.

---

# 6. Channel identity is not durable

Couchy currently persists channel settings under:

```text
channelKey = "$packageName:$channelId"
```

This is better than numeric ID alone because it includes the provider package, but the channel ID remains an Android database row ID.

If the provider deletes/recreates the logical channel, Android can assign another `_id`.

Then Couchy loses:

- hidden state;
- per-channel item limit;
- home-row order identity.

Projectivy's post-closure audit identified the same weakness in its own numeric-ID model and exposed the better identity ingredients:

```text
package_name
+
internal_provider_id
```

Couchy does not currently query `internal_provider_id`.

## Recommended target

```text
TvChannelIdentity(
    providerPackage,
    internalProviderId,
    fallbackFingerprint
)
```

where numeric `channelId` is only the current runtime lookup handle.

Suggested fallback order:

```text
package + internal_provider_id
    ↓ if unavailable
package + app_link_intent_uri / display metadata fingerprint
    ↓ last resort
package + current numeric channelId
```

---

# 7. No duplicate-channel reconciliation

Projectivy suppresses duplicate logical channels using:

```text
package_name + internal_provider_id
```

when possible.

Couchy cannot do this because it does not request `internal_provider_id`.

If an OEM/provider exposes duplicate logical rows, Couchy can therefore surface duplicate home/settings rows.

This should be fixed as part of structured channel identity rather than with a display-name-only heuristic.

---

# 8. Preview Program query ordering is unspecified

Couchy calls:

```text
contentResolver.query(uri, projection, null, null, null)
```

for Preview Programs.

There is no explicit sort order.

Projectivy requests:

```text
weight DESC
```

This is important because provider-defined weight is how Preview Channels express preferred program order.

Couchy's own settings text says provider order is preserved, but with a null sort order the implementation is relying on provider/default database ordering rather than explicitly requesting the Android-TV ordering field.

## Required change

Query Preview Programs with:

```text
weight DESC
```

and include `weight` in the bounded home projection if useful for debugging/reconciliation.

---

# 9. Watch Next ordering is also unspecified

Couchy's Watch Next query also passes no sort order.

Projectivy explicitly requests:

```text
last_engagement_time_utc_millis DESC
```

which makes the row newest/recently-engaged first.

## Required change

Use the engagement timestamp ordering for Watch Next.

Do not rely on provider table default order.

---

# 10. Program filtering differs from Projectivy

Couchy currently accepts a program if:

```text
browsable != 0
and
title is not blank
```

It does **not** require `intent_uri`.

Its launch layer then falls back to launching the provider package when `intent_uri` is blank.

Projectivy takes the stricter behavior:

```text
browsable == 1
intent_uri required
```

and if the deep link is invalid it does not silently transform the card into an app-launch card.

## Recommended target

Make launchability explicit:

```text
TvProgramAction.DeepLink(intentUri, providerPackage)
TvProgramAction.OpenProvider(providerPackage)
TvProgramAction.None
```

Do not infer fallback behavior inside the UI click handler.

For strict Preview/Watch Next parity, filter cards without valid deep links unless we intentionally expose provider-app fallback as a distinct Aerial-Rows policy.

---

# 11. No program deduplication

Projectivy globally deduplicates Preview Programs by a semantic combination derived from:

```text
intent_uri + channel_id
```

Couchy has no explicit deduplication.

Because Couchy queries each Preview Channel separately, cross-channel duplication is less problematic, but duplicate rows within a provider channel or Watch Next can still surface as duplicate cards.

Recommended structured identity will naturally provide a dedup key.

---

# 12. Watch Next can be dominated by one provider

Couchy treats:

```text
channelItemLimit
```

as a total Watch Next row limit.

It has no:

- per-provider item cap;
- provider inclusion/exclusion set.

Therefore one app can consume the whole Watch Next row.

Projectivy has both:

```text
programs per app limit
+
provider exclusion controls
```

## Recommended config

```text
watchNextMaxItems: Int
watchNextPerProviderLimit: Int
excludedWatchNextProviders: Set<String>
```

Provider controls should be built from the union of:

```text
packages actually present in Watch Next
+
packages owning discovered Preview Channels
```

This improves on Projectivy's channel-provider-biased management UI.

---

# 13. Channel/provider settings UI is too flat

Couchy's `ChannelsSettingsScreen` currently shows individual channel rows with package name subtitles.

Projectivy groups channel rows by owning provider app.

For a small number of channels Couchy's approach is simpler; for many streaming apps it becomes noisy.

Recommended hierarchy:

```text
Watch Next
    ├─ global enabled
    ├─ total limit
    ├─ per-provider limit
    └─ provider exclusions

Preview Channels
    ├─ Provider A
    │   ├─ Channel 1
    │   └─ Channel 2
    └─ Provider B
        └─ Channel 3
```

Provider labels/icons should come from PackageManager/AppRepository rather than raw package strings.

---

# 14. Refresh lifecycle is currently too weak

`SlidingRowsArea` uses `produceState` keyed by:

- `showWatchNext`;
- `showPreviewChannels`;
- default channel limit;
- hidden channel set;
- per-channel limits.

No key represents:

- provider data changes;
- package changes;
- Activity resume/wake;
- time-based refresh.

There is no TV-provider `ContentObserver`.

This means content can remain stale indefinitely if the TV provider changes while those config values do not.

Projectivy itself also lacks a dedicated TV-provider observer, relying on caches/forced refresh and a timed gate.

Aerial-Rows should intentionally improve both implementations.

## Target refresh model

```text
TvProviderContentObserver
    ├─ Channels.CONTENT_URI
    ├─ PreviewPrograms.CONTENT_URI
    └─ WatchNextPrograms.CONTENT_URI
        ↓
    debounce
        ↓
TvContentRepository.invalidate(scope)
        ↓
StateFlow generation/version
        ↓
Compose rows refresh

ON_RESUME
    ↓
refresh safety net
```

A small debounce prevents provider write bursts from producing many queries.

---

# 15. Repository caching is too coarse / absent

Couchy's repository returns a fresh snapshot and keeps no provider-level cache.

Every relevant config change can re-query:

```text
Watch Next
+
channel table
+
programs for every visible channel
```

Projectivy maintains separate caches for channels, per-channel Preview Programs and Watch Next.

The better Aerial-Rows version is:

```text
ChannelMetadataCache
PreviewProgramsCache[channelStableId]
WatchNextCache
```

with targeted invalidation from the observer.

This avoids requerying every provider row when one channel changes.

---

# 16. Program-card identity needs to become structured

Couchy currently keys program cards by raw provider `_id` inside each `LazyRow`.

That is adequate for Compose list stability inside a single snapshot, but it is not a durable domain identity.

Preview and Watch Next are separate provider tables and provider rows may be recreated.

Projectivy deliberately distinguishes Preview from Watch Next identities, but uses 32-bit hashes.

Aerial-Rows should improve on both:

```text
ProgramId.Preview(
    providerPackage,
    channelStableId,
    semanticProgramKey
)

ProgramId.WatchNext(
    providerPackage,
    semanticProgramKey
)
```

Preferred semantic key:

```text
content_id if available
else canonical intent_uri
else provider row _id as ephemeral fallback
```

This also supports the C2 `activeItemId` focus-restoration model.

---

# 17. Deep-link launch path is currently unsafe/too permissive

Couchy's `Actions.launchTvContent()` does:

```text
Intent.parseUri(intentUri, URI_INTENT_SCHEME)
    ↓
FLAG_ACTIVITY_NEW_TASK
    ↓
startActivity(intent)
```

It does **not** constrain the parsed intent to `packageName`.

Projectivy explicitly does:

```text
parse provider intent
    ↓
setPackage(program.package_name)
    ↓
common policy-gated launch path
```

This matters because provider data is external input.

## Required target

```text
TvProgramAction
    ↓
LaunchDispatcher
    ↓
LaunchPolicy
    ↓
parse + validate intent
    ↓
constrain to provider package
    ↓
start activity
```

Never call `startActivity()` directly from a TV-program Composable.

This also lets future parental/provider policy cover derived program cards automatically.

---

# 18. Artwork fallback is weaker than Projectivy

Couchy chooses:

```text
poster_art_uri
    ↓ fallback
thumbnail_uri
```

It does not query/use:

- `logo_uri`;
- poster/thumbnail aspect-ratio metadata.

Projectivy resolves approximately:

```text
custom artwork
→ thumbnail
→ poster
→ logo
```

For Couchy's 16:9 card, preferring a suitable thumbnail can be better than blindly preferring a portrait poster.

## Recommended target

Keep a bounded home projection, but add:

```text
poster_art_uri
thumbnail_uri
logo_uri
poster_art_aspect_ratio
poster_thumbnail_aspect_ratio
```

Then let `ProgramArtworkResolver` choose the best candidate for the renderer shape.

---

# 19. TV artwork loading has a memory-efficiency weakness

`TvProgramCard` currently:

1. opens the content/network stream;
2. decodes the full bitmap;
3. scales it down to max 480×270;
4. stores the result in an `LruCache` limited by **18 entries**, not byte size.

Large provider artwork can therefore cause a large temporary allocation before downscaling.

By contrast, Couchy's app-artwork scanner already has much stronger sizing/cache discipline.

## Recommended improvement

Create a shared `TvArtworkRepository` that:

- reads bounds first;
- calculates `inSampleSize` before decode;
- uses a byte-sized memory LRU;
- optionally uses a small disk cache;
- deduplicates concurrent requests for the same URI;
- cancels work when no card needs it;
- calls `prepareToDraw()` before presentation where useful.

This is a place where we should reuse Couchy's own app-artwork performance philosophy.

---

# 20. Current program card is intentionally minimal

Couchy's `TvProgramCard` currently implements:

- artwork;
- title;
- focus scale/glow;
- playback progress.

This is a clean baseline.

Projectivy additionally supports:

- provider/logo badge;
- richer metadata summary;
- preview video on focus;
- optional focus artwork zoom;
- Program Details;
- semantic wallpaper focus event.

These should be introduced as independent capabilities, not one giant card rewrite.

Suggested progression:

```text
CORE
  artwork + title + progress

NEXT
  provider badge
  details action

OPTIONAL EYE-CANDY
  preview video on focus
  focus artwork zoom
  program-focused wallpaper event
```

Progress, badge, preview and zoom should remain separately configurable, matching the Projectivy lesson.

---

# 21. Details should use a two-tier data model

Couchy loads only lightweight home fields, which is good.

Do not expand every home query to every Android-TV column.

Instead add:

```text
TvProgramSummary
    -> home row bounded projection

TvProgramDetailsRepository
    -> query one provider row by _id
    -> long_description / extra metadata only when requested
```

This preserves the performance advantage of a small home model.

---

# 22. Provider-package policy inheritance

Projectivy's post-closure audit proved that protecting an app/provider also protects its derived Preview/Watch Next cards.

Aerial-Rows does not yet implement parental control, but C3 already established the neutral launch-policy boundary.

Therefore TV cards should carry:

```text
providerPackage
```

into `LaunchPolicy` so a future provider-level restriction automatically applies to:

- the provider app;
- Preview Program deep links;
- Watch Next deep links.

Do not build TV-specific parental checks in `TvProgramCard`.

---

# 23. System-provider filtering

Projectivy excludes a provider package suffix ending in:

```text
.tvrecommendations
```

Couchy currently has no equivalent provider filter.

Rather than blindly hard-code Projectivy's exact OEM workaround, introduce:

```text
TvProviderFilter
```

with:

- known problematic/system provider exclusions;
- explicit allow/deny hooks;
- diagnostics showing why a provider/channel was filtered.

This keeps vendor workarounds out of the core query code.

---

# 24. What Couchy already does better / should keep

## Keep

- immutable `TvProgramItem`, `TvChannelRow`, `TvContentRows` concept;
- direct Compose integration;
- separate Watch Next and channel row types in `SlidingRows`;
- stable home row IDs concept;
- per-channel item limits;
- global Preview Channels on/off switch;
- global Watch Next on/off switch;
- row quick editor integration;
- lightweight home card;
- progress bar implementation;
- graceful artwork fallback UI when image loading fails;
- bounded SDK gate (`>= 26`).

## Extend

- model identity;
- discovery metadata;
- refresh/invalidation;
- settings hierarchy;
- artwork repository;
- program metadata/action model.

## Replace

- `channelKey(package:id)` as durable identity;
- channel discovery that filters `browsable == 0`;
- `discoverChannels()` that also requires programs;
- null sort order for Preview/Watch Next;
- direct `startActivity()` path for program intents.

---

# 25. Target architecture

```text
TvProviderAvailability
    │
    ├─ permission / platform state
    │
    ▼
TvChannelRepository
    ├─ discover ALL Preview Channels
    ├─ logical stable identities
    ├─ runtime numeric handles
    ├─ non-browsable management
    └─ provider filtering/dedup
    │
    ├──────────────────────────────┐
    ▼                              ▼
PreviewProgramRepository       WatchNextRepository
    ├─ weight DESC                ├─ engagement DESC
    ├─ per-channel cache          ├─ provider exclusions
    ├─ structured identity        ├─ per-provider cap
    └─ bounded projection         └─ structured identity
    │                              │
    └──────────────┬───────────────┘
                   ▼
             TvContentState
                   │
       ContentObserver + debounce
       ON_RESUME safety refresh
                   │
                   ▼
              HomeRowsRepository
                   │
                   ▼
              TvContentRow
                   │
                   ▼
              TvProgramCard
                   │
                   ▼
              LaunchDispatcher
                   │
              LaunchPolicy
```

Supporting services:

```text
TvArtworkRepository
TvProgramDetailsRepository
TvProviderFilter
ProgramArtworkResolver
```

---

# 26. Recommended implementation order

## C4.1 — Correct identity/discovery first

1. Create `TvChannelIdentity`.
2. Query `internal_provider_id` and app-link metadata.
3. Separate metadata discovery from program loading.
4. Include non-browsable Preview Channels in discovery.
5. Add `requestChannelBrowsable()` on enable.
6. Add dedup by logical channel identity.

## C4.2 — Correct program ordering/policy

7. Explicit `weight DESC` for Preview.
8. Explicit engagement-descending Watch Next query.
9. Add structured `ProgramId`.
10. Add Watch Next per-provider cap and exclusions.
11. Add semantic launch action and provider-constrained dispatcher.

## C4.3 — Refresh architecture

12. Add provider `ContentObserver` registrations.
13. Add debounce/generation invalidation.
14. Add separate channel/Preview/WatchNext caches.
15. Refresh on Activity resume as a safety net.

## C4.4 — Presentation improvements

16. Replace card-local bitmap loader with `TvArtworkRepository`.
17. Add artwork candidate resolver including logo/aspect metadata.
18. Add optional provider badge.
19. Add lazy Program Details.
20. Later add preview-on-focus / semantic wallpaper event if desired.

---

# 27. Direct Couchy vs Projectivy decision table

| Area | Couchy | Projectivy | Aerial-Rows decision |
|---|---|---|---|
| Channel discovery | browsable only | includes non-browsable | **Use improved Projectivy behavior** |
| Channel identity | package + numeric ID | numeric ID runtime cache | **Use structured logical ID better than both** |
| `internal_provider_id` | not queried | used for dedup | **Add** |
| Empty channels in settings | omitted | discoverable channel independent of program load | **Separate metadata/content** |
| Preview order | unspecified | `weight DESC` | **Adopt explicit order** |
| Watch Next order | unspecified | engagement DESC | **Adopt explicit order** |
| Preview limit | per channel | per channel | **Keep Couchy config model** |
| Watch Next provider cap | none | yes | **Add** |
| Watch Next provider exclusion | none | yes | **Add, store package strings** |
| TV content cache | none | separate caches | **Add targeted caches** |
| ContentObserver | none | none | **Improve both: add observer + debounce** |
| Resume refresh | none | timed/forced refresh | **Add safety refresh** |
| Program ID | raw `_id` | URI hash / WN hash | **Use structured ID** |
| Program launch | parsed intent direct | provider-constrained + policy | **Adopt constrained policy path** |
| Artwork | poster → thumbnail | custom → thumbnail → poster → logo | **Use renderer-aware candidates** |
| Progress | yes | yes/configurable | **Keep; add setting later** |
| Provider badge | no | yes | **Optional add** |
| Preview video | no | yes | **Optional add** |
| Program Details | no | yes | **Add lazy details after core** |
| Compose renderer | yes | Leanback presenter | **Keep Couchy Compose** |

---

# Final assessment

```text
Couchy C4 subsystem status:        CLOSED FOR COMPARISON
Core implementation usable:        yes
Needs architectural replacement:   partially
Keep Compose row/card layer:        yes
Keep current repository unchanged: no

Highest-priority fixes:
  1. non-browsable channel discovery
  2. logical channel identity
  3. explicit Preview/WatchNext sorting
  4. provider-constrained LaunchDispatcher
  5. ContentObserver + resume refresh
  6. Watch Next provider policy

Projectivy behavior worth adopting:
  - non-browsable management
  - explicit sort orders
  - per-provider Watch Next control
  - provider-constrained deep links
  - richer program metadata/details

Improve beyond Projectivy:
  - structured non-hash IDs
  - logical persistent channel ID
  - ContentObserver + debounce
  - actual Watch Next packages included in provider-management UI
  - Compose-first renderer and typed state
```

**Recommended status: `C4 COMPLETE + REVIEWED`.**


---

# SOURCE FILE: `C05.md`

SHA-256: `e7603080accb1bdb90cf2672048f647276671649d3cfc61c4712130950bd4b78`

---

# Couchy vs Projectivy — C5 Wallpaper & Aerial Architecture

Status: **COMPLETE + REVIEWED**  
Repository: `kanozseyer/Aerial-Rows`  
Pinned commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Projectivy comparison basis: canonical `group06_postclosure_audit.md`

## Executive conclusion

Couchy/Aerial-Rows already has a strong video rendering implementation for a lightweight launcher:

- Media3 / ExoPlayer
- SurfaceView rather than TextureView
- decoder release on `ON_STOP`
- decoder rebuild on return
- small custom network buffer
- first-frame gating
- symmetric fade-out/fade-in transitions
- gradient fallback instead of black frames
- stall detection
- local video URI streaming
- downsampled static wallpaper
- cheap pre-decoded pseudo-blur for Dock mode

Those are valuable and should largely survive.

Projectivy is stronger in the layer around the renderer:

- provider abstraction
- event-driven wallpaper engine
- explicit rotation scheduler
- provider cache invalidation
- retry/fallback policy
- image format policy
- audio-track disabling
- configurable tunneling / decoder fallback
- dynamic-color sampling
- external wallpaper providers
- theme/backup integration
- idle integration

The correct target is therefore:

```text
Couchy rendering/performance
        +
Projectivy provider/engine separation
        +
Aerial-Rows source catalogue
```

not a direct copy of either application.

## 1. Current Couchy wallpaper architecture

The effective development build currently has four mutually exclusive presentation modes:

```text
preset gradient
photo copied to app-private storage
local video via persisted content URI
built-in Aerial stream
```

Selection currently lives directly in `LauncherConfig`:

```text
wallpaper
useCustomWallpaper
videoUri
useVideoWallpaper
useBuiltinAerials
builtinSource       // legacy
builtinSources      // current multi-select source set
videoSpeed
scrimMode
```

The rendering decision remains inside the large `LauncherApp` composition.

Conceptually:

```text
LauncherApp
    ↓ reads config
    ↓
if built-in Aerial
    → choose URL
    → VideoWallpaper
else if local video
    → VideoWallpaper
else if photo
    → Image
else
    → preset gradient
```

This is simple and fast, but provider policy, playlist state, rendering, retry policy and launcher UI ownership are currently mixed together.

## 2. BuiltinAerials — good lightweight source catalogue

`BuiltinAerials.kt` parses a bundled `assets/aerials.json` once and caches it in memory.

The source model currently contains:

```text
title
URL
source code
```

with source codes:

```text
A   Apple
Z   Amazon
C1  Community 1
C2  Community 2
```

The current development model supports selecting several collections simultaneously.

Good properties:

- catalogue ships with the launcher;
- no network request is required just to discover candidates;
- parsing happens once;
- unknown source values are filtered;
- source selection is normalized;
- older single-source backups remain decodable.

Important missed opportunity: `Entry.t` contains the Aerial title/location, but `BuiltinAerials.load()` discards it and returns only `List<String>`. Use a typed `AerialItem(id, title, sourceId, uri, metadata)` instead.

## 3. Build-time source patching currently changes shipped wallpaper behaviour

Committed source still contains the legacy `builtinSource: Int` paths in both `SettingsSheet.kt` and `LauncherScreen.kt`.

`applyAerialRowsSourcePatches` rewrites those files before every build so that:

```text
SettingsSheet
    → edits builtinSources: Set<String>

LauncherScreen
    → BuiltinAerials.load(context, config.builtinSources)
```

Phase 0 must therefore remain mandatory:

1. materialize the multi-source changes as normal Kotlin;
2. remove the Gradle source rewriting task;
3. delete legacy runtime dependencies on `builtinSource`;
4. retain migration-only decoding for old backups.

## 4. Multi-source changes currently apply only after the current clip advances

The committed Aerial state is keyed with:

```text
produceState(
    config.useBuiltinAerials,
    config.builtinSource,
    wallpaperVersion
)
```

The patch replaces the load call with `BuiltinAerials.load(context, config.builtinSources)` but does not replace the state key. The patched settings page updates only `builtinSources`.

Therefore changing selected collections does not restart playlist selection immediately. The current clip continues until it ends, the 10-minute timer fires, or an error advances `wallpaperVersion`.

If this behaviour is wanted, model it explicitly with an `AerialPlaylistController` and an `APPLY_NOW` / `APPLY_AFTER_CURRENT` policy instead of relying on a stale legacy key.

## 5. Couchy's VideoWallpaper implementation is one of its strongest components

### SurfaceView

Keep `SurfaceView`. It keeps video composition out of normal Compose/UI-thread rendering.

### Lifecycle resource ownership

```text
ON_START → build player
ON_STOP  → release player / decoder / buffers
return   → rebuild and reprepare current URI
```

This is excellent launcher behaviour. Keep it.

### Small buffer

Couchy uses roughly 10–15 seconds of buffering rather than Media3's much larger defaults. Keep the philosophy and benchmark exact values later.

### First-frame gating

It waits for `onRenderedFirstFrame()` before exposing the clip. Keep.

### Symmetric transitions

Current video fades out, new media prepares behind a cover, and the new first frame fades in. Keep.

## 6. Serious issue — TLS verification is explicitly disabled

The current network player uses a no-op `X509TrustManager` and an always-true hostname verifier.

**REMOVE THIS.**

Even decorative media is untrusted input to the network/container/codec stack. Use normal certificate and hostname verification. If a legacy Aerial endpoint cannot establish trust on an old device, use a valid alternative endpoint or disable that source on that device rather than trusting every certificate.

## 7. Muted wallpaper still decodes audio

Couchy sets `player.volume = 0f` but leaves audio track selection active.

Projectivy does better:

```text
wallpaper volume == 0
    → disable Media3 audio track type
```

Adopt that optimization.

## 8. Decoder fallback should be configurable

Couchy hard-codes decoder fallback ON. Projectivy exposes it and defaults it OFF.

Use:

```text
VideoRendererConfig(
    decoderFallback,
    tunneling,
    audioPolicy,
    bufferingPolicy
)
```

Recommended for curated built-in Aerials: hardware-first, fallback off by default. User-selected local videos can optionally enable compatibility fallback.

## 9. Add Media3 tunneling as an advanced renderer option

Projectivy verifies tunneling is a real playback setting and defaults it enabled. Add a capability-aware `VideoRendererConfig.tunnelingEnabled`.

## 10. Stall watchdog is useful but too coarse

Current behaviour waits 2.5 seconds for a first frame and then treats absence as an error if resumed.

Missing distinctions:

- network timeout
- HTTP failure
- decoder unsupported
- decoder failure
- surface unavailable
- transient slow startup

There is also no per-item failure count or temporary failed-URI quarantine.

Target:

```text
FailureRegistry
    key = wallpaper item ID
    retry count
    last failure
    temporary quarantine
```

with bounded backoff.

## 11. Aerial selection should become a shuffle bag

Current random selection prevents immediate repeat only.

Use a shuffle bag so every enabled candidate is consumed once before reshuffling, while preventing the next bag from starting with the previous final item.

## 12. Rotation policy is currently hard-coded

Current Aerial rotation is:

```text
clip ends
OR
10-minute safety timer
```

Projectivy has a real rotation scheduler with configurable interval and separate provider-update invalidation.

Introduce:

```text
RotationPolicy
    ON_MEDIA_END
    FIXED_INTERVAL
    MEDIA_END_OR_MAX_INTERVAL
    MANUAL_ONLY
```

For Aerials, `MEDIA_END_OR_MAX_INTERVAL` is a good default.

## 13. Local photo path — efficient but needs a real image policy

The current photo path is good:

- file copied to app-private storage;
- decode off main thread;
- sharp image downsampled to about 1280px;
- 96px blur proxy;
- RGB_565;
- `prepareToDraw()`.

Keep the 96px pseudo-blur optimization.

Add an image policy:

```text
ImageWallpaperPolicy(
    minWidth,
    normalDisplayMode,
    incompatibleDisplayMode
)
```

with options such as Blur, Fit, Crop, Stretch and Skip. Also handle EXIF orientation.

## 14. Local video URI persistence has an unchecked failure path

Couchy attempts `takePersistableUriPermission()` but ignores failure and stores the URI anyway.

Target:

```text
persist grant succeeds
    → store URI

persist grant fails
    → private-copy fallback or explicit error
```

Do not silently store a URI whose long-term readability is unknown.

## 15. No real wallpaper-provider abstraction exists yet

Current:

```text
BuiltinAerials
LauncherConfig
LauncherApp
VideoWallpaper
```

Target:

```text
WallpaperEngine
│
├─ ProviderRegistry
│   ├─ PresetGradientProvider
│   ├─ LocalPhotoProvider
│   ├─ LocalVideoProvider
│   └─ BuiltinAerialProvider
│
├─ EventBus
├─ RotationScheduler
├─ CandidateCache
├─ FailureRegistry
└─ RendererRegistry
    ├─ ColorRenderer
    ├─ ImageRenderer
    └─ VideoRenderer
```

Aerial should be `BuiltinAerialProvider → WallpaperDescriptor.Video → shared VideoRenderer`.

## 16. One shared video renderer

Use the same renderer implementation for:

- built-in Aerials;
- local video;
- future network wallpaper provider;
- future idle/immersive Aerial mode.

The player instance may still be lifecycle-recreated exactly as Couchy does today.

## 17. Dynamic wallpaper color is absent

Projectivy supports renderer-owned video palette sampling with PixelCopy and an independent cadence.

Make this optional later:

```text
WallpaperPaletteSampler
    image → one-shot sample
    video → periodic PixelCopy sample
```

Do not tie palette sampling frequency to wallpaper rotation.

## 18. External wallpaper providers are absent

Do not implement the external plugin protocol immediately, but define an internal `WallpaperProvider` seam now so future providers do not require renderer changes.

## 19. Wallpaper events should be semantic

Replace `wallpaperVersion++` style orchestration over time with:

```text
WallpaperEvent
    SourceConfigurationChanged
    RotationDue
    MediaEnded
    LoadingError
    LauncherVisibilityChanged
    IdleModeChanged
```

Later, CardFocused and NowPlayingChanged can be added cleanly.

## 20. Wallpaper mode should become one typed selection

Replace mutually-exclusive booleans with:

```text
WallpaperSelection
    Preset(id)
    Photo(assetId)
    LocalVideo(uri)
    BuiltinAerial
```

plus independent `WallpaperOptions`.

## 21. Network security / cleartext policy

Audit every built-in URL scheme. Replace avoidable HTTP endpoints, disable global cleartext traffic if possible, and use a narrow Network Security Config if a specific legacy source truly requires HTTP.

This is separate from the trust-all TLS bug; both should disappear from the final architecture.

## 22. KEEP / EXTEND / REPLACE

### KEEP

- SurfaceView
- Media3 / ExoPlayer
- ON_STOP decoder release
- player rebuild on return
- small buffer policy
- first-frame gating
- fade transition
- gradient fallback
- downsampled photo decode
- tiny pseudo-blur
- local-video streaming
- bundled Aerial catalogue
- multi-source selection
- one video renderer concept

### EXTEND

- typed Aerial metadata
- renderer config
- image format policy
- structured failures
- playlist state
- rotation scheduler
- semantic events
- optional dynamic colors
- provider seam

### REPLACE / REMOVE

- trust-all TLS
- hostname verification bypass
- anonymous URL-only Aerial model
- `wallpaperVersion` as engine orchestration
- global `aerialError`
- random-with-retry playlist
- ignored persistable-URI failure
- hard-coded decoder fallback
- legacy runtime `builtinSource`
- build-time source rewriting
- mutually-exclusive wallpaper booleans as long-term state

## 23. Recommended C5 implementation order

### C5.0
Materialize current Aerial-Rows source patches and remove Gradle source rewriting.

### C5.1
Remove trust-all TLS / permissive hostname verification.

### C5.2
Introduce typed `WallpaperSelection`.

### C5.3
Extract built-in/local providers from `LauncherScreen`.

### C5.4
Extract the shared `VideoRenderer`, preserving SurfaceView, lifecycle release, first-frame gate, small buffer and fades. Add audio track disable, tunneling and configurable fallback.

### C5.5
Add `WallpaperRotationScheduler`, `FailureRegistry` and semantic `WallpaperEvent`.

### C5.6
Replace URL-only random selection with typed Aerial items + shuffle bag + failed-item quarantine.

### C5.7
Improve local media import: EXIF/image policy and persistent-URI validation/fallback.

### C5.8
Optional parity later: dynamic colors, external providers, remote wallpaper advance and idle-mode integration.

## Final conclusion

The Couchy wallpaper/Aerial subsystem is sufficiently understood for reconstruction planning.

The current effective architecture is:

```text
Config + LauncherApp + BuiltinAerials + VideoWallpaper
```

The desired architecture is:

```text
provider/source policy
        ≠
renderer/playback
```

while preserving Couchy's unusually good TV-performance choices.

**C5 status: COMPLETE + REVIEWED.**


---

# SOURCE FILE: `C06.md`

SHA-256: `0bf49b7fed0fa4fee3abe2c86f133c3f62fb1eba708821c4b2cebf12cba37b18`

---

# Couchy vs Projectivy — C6 Settings Architecture

Status: **COMPLETE + REVIEWED**  
Repository: `kanozseyer/Aerial-Rows`  
Pinned commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Projectivy comparison basis: canonical Group 7 post-closure audit.

## Executive conclusion

Couchy's current settings experience is a good **Compose TV UI**, but the settings *architecture* is too implicit for the Projectivy-level feature set we are building.

The current model is broadly:

```text
Composable setting row
    ↓
store.update { config.copy(...) }
    ↓
serialize entire LauncherConfig JSON
    ↓
Preferences DataStore
    ↓
LauncherApp collects config
    ↓
Compose recomposition / ad-hoc side effect
```

This is simple and has worked while the feature set was small.

Projectivy's useful lesson is not its SharedPreferences implementation. Couchy's typed `LauncherConfig` + DataStore is the better persistence foundation.

The useful Projectivy lesson is the **descriptor/effect layer above persistence**:

```text
SettingId
SettingDescriptor<T>
validation
visibility/capability
scope
export policy
runtime effect
migration metadata
focus identity
```

The target should therefore be:

```text
Couchy typed DataStore/config
        +
Couchy Compose TV settings UI
        +
Projectivy-style typed descriptors/effect policies
        +
stable focus/navigation
```

---

# 1. Current Couchy settings surfaces

Couchy/Aerial-Rows currently has several related configuration surfaces.

## 1.1 Main SettingsSheet

`SettingsSheet.kt` contains:

- root settings page;
- Display;
- Wallpaper;
- Status bar;
- Apps;
- Categories;
- Launcher settings;
- Language;
- About;
- backup save/load;
- VPN app selector;
- several modal editors.

It uses a local enum:

```text
SettingsScreen
```

and switches the rendered child with Compose `AnimatedContent`.

## 1.2 AerialSettingsExtras

`AerialSettingsExtras.kt` contains the newer Aerial-Rows additions:

- Android TV Channels settings;
- Sliding Rows top-margin controls;
- row/dock panel appearance;
- panel opacity;
- tint;
- saturation;
- liquid-glass strength.

These are not integrated as native settings definitions. They are separate Compose functions wired into `SettingsSheet.kt` by the Gradle source-rewriting task.

## 1.3 RowQuickEditor

`RowQuickEditor.kt` is a second Projectivy-inspired settings surface opened from the left home rail.

It edits:

- category membership;
- category item order;
- TV channel visibility;
- per-channel item limit;
- Watch Next visibility/limit.

## 1.4 SetupWizard

`SetupWizard.kt` is onboarding rather than a third normal settings framework.

It configures:

- HOME/default launcher role;
- Google TV setup guidance;
- VPN shortcut choice;
- setup completion.

This can remain separate from the settings descriptor registry, though it should ultimately call the same repositories/commands.

---

# 2. What Couchy does well

## 2.1 Typed configuration

The entire launcher state is represented by a serializable Kotlin model:

```text
LauncherConfig
```

This is much easier to reason about than Projectivy's 148 individual static preference descriptors.

**KEEP this foundation.**

## 2.2 Transactional DataStore writes

`ConfigStore.update()` uses DataStore's edit transaction and applies a transform against the current configuration.

This is a good persistence primitive.

**KEEP DataStore.**

## 2.3 D-pad-first interaction

The settings UI is intentionally TV-oriented:

- focusable rows;
- Left/Right selectors;
- OK activation;
- no touch assumptions;
- dedicated text-entry dialog rather than accidental text fields in navigation lists.

**KEEP.**

## 2.4 No focus scaling on settings text

`SettingsItem` deliberately avoids tv-material's default focused scaling because scaling already-rasterized text can appear soft/blurry on TV panels.

Selection is signalled through background/content colours instead.

This is a good TV-specific visual decision.

**KEEP.**

## 2.5 Full-height right-side panel

The main Settings dialog expands its window to the full display and places a fixed-width settings surface against the right side.

It also re-applies the launcher's custom UI density inside the Dialog because Compose dialogs create a separate window/density context.

This is thoughtful and should survive the refactor.

## 2.6 Immediate Compose previews

Many appearance changes naturally redraw immediately because `LauncherConfig` is observed by the root launcher composition.

Examples:

- card size;
- spacing;
- roundness;
- panel style;
- panel tint;
- opacity;
- status settings.

This is simpler than Projectivy's fragment/adaptor refresh machinery.

**Do not introduce launcher restarts for ordinary visual settings.**

## 2.7 Language already demonstrates a real explicit side effect

Changing `config.language` is persisted, mirrored into the synchronous locale preference and then causes `Activity.recreate()`.

This is a legitimate:

```text
RECREATE_ACTIVITY
```

update effect.

The behavior is correct; only its ownership should move out of ad-hoc root-composition code into a formal effect system.

---

# 3. Major architecture issue — the UI owns persistence and effects

Almost every setting currently looks like:

```kotlin
onClick = {
    scope.launch {
        store.update {
            it.copy(someValue = ...)
        }
    }
}
```

This causes the UI to know:

1. where the value is stored;
2. how to validate it;
3. what other values must change;
4. what runtime reaction is required;
5. whether the setting is exportable;
6. whether it should be shown on the current device;
7. how it should migrate in future.

That will not scale cleanly to:

- inputs;
- autostart;
- Accessibility;
- remote mappings;
- display profiles;
- notifications;
- parental controls;
- shortcuts;
- wallpaper providers;
- themes;
- advanced vendor options.

## Target

The UI should render descriptors and send typed change requests.

```text
Settings UI
    ↓
SettingsController
    ↓
SettingDescriptor<T>
    ↓
SettingsRepository
    ↓
ConfigStore
    ↓
SettingEffectExecutor
```

---

# 4. Keep LauncherConfig, add SettingDescriptor on top

Do **not** reproduce Projectivy's preference-per-setting architecture.

Recommended descriptor:

```kotlin
data class SettingDescriptor<T>(
    val id: SettingId,
    val title: UiText,
    val description: UiText?,
    val control: SettingControl<T>,
    val read: (LauncherConfig) -> T,
    val write: (LauncherConfig, T) -> LauncherConfig,
    val validate: (T) -> T,
    val visibleWhen: (SettingsContext) -> Boolean,
    val enabledWhen: (SettingsContext) -> Boolean,
    val persistence: PersistencePolicy,
    val effect: SettingEffect,
    val exportPolicy: ExportPolicy,
)
```

For dynamic/scoped settings:

```text
SettingId("input.visible", scope = inputId)
SettingId("channel.limit", scope = channelId)
SettingId("display.profile.sdr", scope = inputId)
```

Unlike Projectivy's mutable scoped singleton descriptor, the scope should be immutable.

---

# 5. Separate persistence policy from runtime effect

Projectivy's update taxonomy is useful, but Aerial-Rows can make it clearer by separating two questions:

```text
When should the value be written?
```

from:

```text
What should happen after it changes?
```

## PersistencePolicy

```text
IMMEDIATE
DEBOUNCED
ON_CONFIRM
SESSION_ONLY
```

## SettingEffect

```text
NONE
RECOMPOSE
RELOAD_APPS
RELOAD_TV_CONTENT
RELOAD_WALLPAPER
REFRESH_ACCESSIBILITY
RECREATE_ACTIVITY
RESTART_HOME_TASK
RELOAD_INPUTS
RELOAD_DISPLAY_PROFILE
```

This is more composable than one large enum.

Examples:

```text
panel opacity
    persistence = DEBOUNCED
    effect = RECOMPOSE

language
    persistence = IMMEDIATE
    effect = RECREATE_ACTIVITY

future HOME-role option
    persistence = IMMEDIATE
    effect = RESTART_HOME_TASK
```

---

# 6. Performance issue — rapid selectors rewrite the complete JSON config

`ConfigStore.update()`:

1. decodes the current complete JSON `LauncherConfig`;
2. applies the transform;
3. normalizes it;
4. encodes the complete configuration back to JSON;
5. commits through DataStore.

Every D-pad Left/Right press on:

- opacity;
- tint;
- UI scale;
- spacing;
- card size;
- date format;
- channel limits;

can therefore become a complete DataStore transaction.

This is reliable but unnecessary for rapid preview controls.

## Target

Use optimistic session state:

```text
key repeat / selector changes
    ↓
SettingsSessionState immediately updates UI
    ↓
debounce / collectLatest
    ↓
one ConfigStore write
```

Recommended debounce for continuous adjustments:

```text
150–300 ms
```

Then always flush pending changes when:

- settings dialog closes;
- app goes to background;
- user confirms an editor.

This keeps the UI instant without sacrificing durability.

Do not debounce critical one-shot changes such as:

- parental PIN;
- launcher role;
- permission-related state;
- backup restore.

---

# 7. Focus behavior does not meet the desired UX yet

The user requirement for Aerial-Rows is:

> after changing a setting or returning from a child page, keep the cursor at the relevant selection rather than jumping to the top.

Couchy's normal in-place setting mutation often retains Compose focus naturally.

But route changes do not have stable focus identity.

The root settings page calls:

```text
initialFocus()
```

and always assigns that requester to its first setting row.

When a child screen returns to `SettingsScreen.Main`, the Main screen is re-entered and its focus policy is first-item oriented.

There is no stored:

```text
last focused SettingId per route
```

Projectivy's stable-action-ID lesson is directly useful here.

## Target

```text
SettingsNavigator
    currentRoute
    routeStack
    lastFocusByRoute: Map<SettingsRoute, SettingId>
```

Flow:

```text
Display row focused on Main
    ↓
open Display
    ↓
Back
    ↓
Main
    ↓
restore focus to "Display"
```

not:

```text
Main → first item
```

---

# 8. Three independent focus implementations already drift

There are currently at least three settings-control/focus families.

## Main SettingsSheet

`initialFocus()`:

- creates a `FocusRequester`;
- retries once per frame;
- up to 30 frames;
- stops as soon as focus succeeds.

This is the strongest implementation.

## AerialSettingsExtras

`extraInitialFocus()` makes a single `requestFocus()` attempt.

If the node is not attached at that exact moment, focus recovery is less reliable.

## RowQuickEditor

`CategoryContentsEditor` creates a `FocusRequester` and attaches it to the first item but does not explicitly request focus in the shown code.

This relies on platform/default dialog focus behavior.

## Decision

**REPLACE all three with one shared focus coordinator/helper.**

Never add a fourth copy.

---

# 9. Stable SettingId is needed for conditional rows

Several settings appear/disappear or become disabled dynamically.

Examples:

- section-name setting hidden for Dock;
- panel controls shown only when panel style is not Clear;
- liquid-glass strength shown only for Liquid Glass;
- VPN target disabled when VPN button is hidden;
- channel-specific controls conditionally shown.

If the currently focused row disappears because a setting changes, Compose's default focus fallback is not a complete settings policy.

Use stable IDs plus a deterministic fallback:

```text
focused setting removed
    ↓
nearest next visible enabled SettingId
    ↓
else previous
    ↓
else section header's first control
```

This prevents unexpected focus jumps.

---

# 10. Selector interaction should be upgraded

Couchy's `SelectorRow`, `ExtraSelector` and `QuickSelectorRow` all use essentially:

```text
Left  → previous value
Right → next value
OK    → next value
```

and they wrap around.

This is fast for a short range, but poor for long enumerations.

The requested Aerial-Rows UX includes dropdown/choice menus for multi-choice settings.

## Recommended split

### Small numeric/ordinal controls

Examples:

- opacity;
- spacing;
- size;
- roundness;
- playback speed.

Keep:

```text
Left / Right direct adjustment
```

OK may optionally open the choice dialog.

### Enumerations / larger choice sets

Examples:

- layout;
- language;
- wallpaper source;
- date format;
- VPN target;
- future autostart target;
- input assignment.

Use:

```text
OK
  ↓
ChoiceDialog / drop-down style panel
  ↓
stable focus on current selection
  ↓
commit
  ↓
return to originating SettingId
```

Left/Right can remain as an optional quick shortcut where sensible.

---

# 11. Current SettingsScreen navigation will become too small

Today:

```text
enum SettingsScreen
```

is effectively one root + one level of sub-screens.

That is enough for Couchy's original feature set.

It will become awkward for:

- Inputs → HDMI1 → profile binding;
- Remote → key → action picker;
- Wallpaper → provider → provider options;
- Parental → protected items;
- Apps → app → customisation;
- Display Profiles → profile → detailed values.

## Target

Use a typed route stack:

```text
SettingsRoute.Main
SettingsRoute.Display
SettingsRoute.Wallpaper
SettingsRoute.Channels
SettingsRoute.Apps
SettingsRoute.Category(id)
SettingsRoute.Input(id)
SettingsRoute.RemoteKey(keyCode)
...
```

and:

```text
SettingsNavigator.push(route)
SettingsNavigator.pop()
```

with stable focus history.

---

# 12. Build-time settings patching remains a Phase-0 blocker

The committed `SettingsScreen` enum does not contain the Aerial-Rows Channels page.

The Gradle task rewrites `SettingsSheet.kt` before compilation to add:

- `SettingsScreen.Channels`;
- Channels root entry;
- Channels screen routing;
- Sliding Rows layout choice;
- PanelAppearance settings;
- multi-select Aerial source settings.

This means the source visible in the repository is not the source that is actually built.

For settings work this is especially dangerous because future refactors can silently break string-replacement anchors.

## Decision

Before extending settings:

1. materialize every injected setting into normal Kotlin;
2. remove `applyAerialRowsSourcePatches`;
3. make the build source-pure;
4. only then introduce the typed settings registry.

---

# 13. Duplicate control families should be unified

Current overlapping components include:

```text
SettingsItem
SelectorRow

ExtraSettingRow
ExtraSelector
ExtraListItem

QuickToggleRow
QuickSelectorRow
```

This duplication already produces differences in:

- focus acquisition;
- background styling;
- selector behavior;
- localization;
- disabled-state treatment;
- spacing;
- future accessibility semantics.

## Target reusable controls

```text
SettingRow
ToggleSettingRow
ChoiceSettingRow
RangeSettingRow
ActionSettingRow
ColorSettingRow
TextSettingRow
AppPickerSettingRow
```

All rendered from the same `SettingDescriptor` semantics.

`RowQuickEditor` can still use a compact visual density while sharing the same primitives and controller.

---

# 14. Localization is already drifting

Core SettingsSheet uses resource strings extensively.

Several Aerial-Rows extras currently contain hardcoded English labels such as:

- Channels;
- Watch Next;
- App preview channels;
- Sliding Rows position;
- Row & Dock Background;
- Live panel preview;
- various RowQuickEditor instructions.

As more settings arrive this will become increasingly expensive to clean up.

## Decision

Descriptor titles/descriptions should use:

```text
UiText.Resource(...)
```

or equivalent, not arbitrary hardcoded strings.

Dynamic provider/app names can remain runtime text.

---

# 15. Validation is scattered between UI and consumers

`ConfigStore.normalize()` currently validates only a subset of the model, such as:

- enabled Aerial source set;
- top-row margin;
- channel item limits;
- duplicate home-row IDs.

Other fields are usually clamped where they are consumed:

```text
layout.coerceIn(...)
cornerRadius.coerceIn(...)
uiScale.coerceIn(...)
accent.coerceIn(...)
videoSpeed.coerceIn(...)
```

This prevents many crashes, but it makes the persisted state capable of remaining invalid.

## Target

Validation should happen at three layers:

```text
SettingDescriptor.validate(value)
        ↓
ConfigNormalizer.validateWholeConfig()
        ↓
MigrationRegistry
```

The stored model should be normalized, not merely tolerated.

---

# 16. Schema migration remains missing

Couchy's JSON decoder has:

```text
ignoreUnknownKeys = true
```

and default values make additive changes convenient.

That is not a substitute for versioned migration.

Projectivy's verified architecture has:

```text
schema/app version
ordered migrations
pre-migration backup
descriptor aliases
```

Aerial-Rows should add:

```text
schemaVersion
MigrationRegistry
lastKnownGoodConfig
```

while keeping typed JSON/Proto-like structured storage.

This was already identified in C1 and is confirmed by C6.

---

# 17. Backup Save contains a real failure-reporting bug

Current Save config:

```text
Environment.getExternalStoragePublicDirectory(DOWNLOADS)
    ↓
File(..., "CouchyBackup.json").writeText(...)
```

is wrapped in `runCatching`.

The result of `runCatching` is ignored.

After the IO block completes, the UI unconditionally shows:

```text
Saved to Downloads/CouchyBackup.json
```

Therefore any write failure is reported as success.

On modern scoped-storage Android builds, direct arbitrary File writes to public Downloads are also not the correct durable API.

## Fix

Use:

```text
ActivityResultContracts.CreateDocument("application/json")
```

or MediaStore.

Then:

```text
openOutputStream(uri)
write
verify/close
show success only if successful
```

The existing load side already correctly uses the Storage Access Framework.

---

# 18. Backup restore is simple but needs a future bulk-rehydrate contract

Current load:

```text
OpenDocument
    ↓
decode LauncherConfig
    ↓
store.update { loaded.copy(...) }
```

For today's Compose-only state, this works reasonably well.

But future subsystems will include:

- Accessibility service;
- input repository;
- notification listener;
- wallpaper engine;
- display-profile backend;
- shortcut publisher.

A restored configuration must not call every individual setting callback one by one.

Use:

```text
ConfigRepository.replaceImportedConfig(...)
    ↓
decode
    ↓
schema migration
    ↓
validation
    ↓
transactional commit
    ↓
one RuntimeRehydrate event
```

This matches the useful Projectivy bulk-restore lesson while remaining cleaner.

---

# 19. Export policy should be explicit

Projectivy uses a `nexp` naming convention to exclude certain preferences from export.

Do not copy that string convention.

Aerial-Rows descriptors should contain:

```text
ExportPolicy
    BACKUP
    THEME
    BOTH
    NEVER
```

Examples:

```text
UI scale                 → BOTH or THEME
row memberships          → BACKUP
setupDone                → NEVER or BACKUP depending restore policy
runtime cache            → NEVER
wallpaper provider prefs → explicit policy
parental secrets         → encrypted/special handling
```

Full backup and theme export should have separate serializers.

---

# 20. Settings access can later use the central LaunchPolicy

Projectivy later proved that its Settings surface can itself be parental-protected.

Aerial-Rows should not sprinkle PIN checks throughout the settings code.

Use:

```text
LaunchDispatcher
    ↓
LaunchPolicy.authorize(SettingsTarget)
    ↓
open Settings
```

This aligns with the C3 architecture.

---

# 21. Color editing should use a dedicated editor

Projectivy uses a dedicated color-picker flow rather than pretending color is an ordinary scalar selector.

Aerial-Rows currently has predefined hue/saturation selectors for panel appearance, which are fine.

When arbitrary user colors are introduced, use a dedicated Compose color-picker dialog:

```text
Setting row
    ↓
ColorPickerDialog
    ↓
preview
    ↓
confirm/cancel
    ↓
return focus to same SettingId
```

---

# 22. Settings-preview behavior should remain Compose-native

Projectivy uses a globally shared 500 ms preview job because its GuidedStep architecture needs to push visual changes into the home UI.

Aerial-Rows does not need to imitate that literally.

For ordinary Compose appearance:

```text
SettingsSessionState changes
    ↓
home preview recomposes immediately
```

Persistence can be debounced separately.

For expensive subsystems:

```text
collectLatest
debounce
reload subsystem
```

This is cleaner and preserves Couchy's immediacy.

---

# 23. Settings should not restart HOME unnecessarily

Projectivy has a verified `RESTART_HOME_TASK` policy for changes which require re-entering the launcher HOME task.

Aerial-Rows should support that policy because future:

- HOME role;
- launcher identity;
- task behavior;
- certain vendor startup modes

may genuinely require it.

But it should **not** be used for:

- panel tint;
- card size;
- row spacing;
- wallpaper scrim;
- ordinary content settings.

Compose can update those live.

---

# 24. Current architecture causes broad config invalidation

`LauncherApp` observes the entire `LauncherConfig`.

Changing any field produces a new immutable config object.

That is simple and Compose can often skip stable child work, but as the model grows, passing the entire config into many large composables increases the number of invalidation candidates.

## Future optimization

Split the root model into immutable sub-configs:

```text
LauncherConfig
├─ AppearanceConfig
├─ WallpaperConfig
├─ ContentConfig
├─ StatusConfig
├─ InputConfig
├─ RemoteConfig
└─ SystemConfig
```

Then expose derived flows/selectors where useful.

Do this only when profiling shows value; do not prematurely fragment persistence.

---

# 25. What to keep from Couchy

## KEEP

- typed `LauncherConfig`;
- DataStore persistence;
- immutable configuration;
- Compose side-panel;
- `AnimatedContent` page transitions;
- D-pad-first UI;
- no focused text scaling;
- immediate visual previews;
- dedicated rename/text-entry dialogs;
- full-height settings window;
- dialog density rescaling;
- language Activity recreation behavior;
- app thumbnails inside pickers;
- current simple backup JSON as a human-readable format concept.

---

# 26. What to adopt from Projectivy

## ADD / EXTEND

- stable `SettingId`;
- immutable scoped setting identity;
- `SettingDescriptor<T>`;
- validation metadata;
- capability/visibility predicates;
- update/effect policy;
- export policy;
- schema version;
- ordered migration registry;
- pre-migration last-known-good backup;
- focus restoration by SettingId;
- dedicated choice/color editors;
- explicit HOME-task restart effect;
- bulk restore + single runtime rehydrate;
- parental policy for Settings access.

---

# 27. What to remove/replace in Couchy

## REPLACE

- direct `store.update { copy(...) }` ownership in every UI row;
- `SettingsScreen` as the long-term navigation model;
- first-item-only route focus policy;
- duplicated Settings/Extra/Quick controls;
- one-shot `extraInitialFocus`;
- RowQuickEditor implicit focus;
- hardcoded English settings labels;
- direct public-Downloads File backup write;
- unconditional save-success toast;
- validation only at consumers;
- build-time settings source rewriting.

---

# 28. Recommended target architecture

```text
SettingsHost
│
├─ SettingsNavigator
│   ├─ route stack
│   └─ lastFocusByRoute
│
├─ SettingsRegistry
│   └─ SettingDescriptor<T>
│
├─ SettingsController
│   ├─ session state
│   ├─ validation
│   └─ persistence scheduling
│
├─ SettingsRepository
│   └─ ConfigStore / DataStore
│
└─ SettingEffectExecutor
    ├─ recomposition
    ├─ subsystem reload
    ├─ Activity recreation
    ├─ HOME-task restart
    └─ future service refresh
```

Persistence:

```text
LauncherConfig
    ↓
schemaVersion
    ↓
MigrationRegistry
    ↓
ConfigNormalizer
    ↓
Preferences DataStore / serialized config
```

Focus:

```text
SettingsRoute
    ↓
SettingId
    ↓
FocusRequester registry
    ↓
restore exact prior setting
```

---

# 29. Recommended control model

```text
SettingControl
├─ Toggle
├─ Range
├─ Choice
├─ Action
├─ Text
├─ Color
├─ AppPicker
└─ Custom
```

Recommended UX:

```text
small range
    Left/Right = adjust

choice/enum
    OK = open choice/drop-down panel
    optional Left/Right = quick adjust

text/color/app
    OK = dedicated editor
```

This matches the requested streamlined/drop-down behavior while retaining fast remote control.

---

# 30. Recommended C6 implementation order

## C6.0 — source-pure settings

- materialize Channels;
- materialize Sliding Rows layout option;
- materialize panel settings;
- materialize multi-source Aerial controls;
- remove Gradle source mutation.

## C6.1 — shared controls

Merge:

```text
SettingsItem
ExtraListItem
Quick...
```

into one reusable control family.

## C6.2 — stable IDs + navigator

Implement:

```text
SettingId
SettingsRoute
SettingsNavigator
lastFocusByRoute
```

Fix return-from-child focus before adding more pages.

## C6.3 — descriptors

Create the registry for existing settings first.

Do not change persistence yet; descriptors simply read/write `LauncherConfig`.

## C6.4 — effect executor

Move:

- language recreation;
- wallpaper reload requests;
- TV content refresh;
- future service reactions

behind explicit effects.

## C6.5 — persistence efficiency

Add SettingsSessionState and debounce continuous changes while keeping critical writes immediate.

## C6.6 — schema migration / validation

Add:

```text
schemaVersion
MigrationRegistry
lastKnownGoodConfig
whole-config normalization
```

## C6.7 — choice dialogs/dropdowns

Replace long wraparound selector lists with a reusable `ChoiceDialog`.

## C6.8 — backup action fix

Use Storage Access Framework `CreateDocument` and truthful success/failure reporting.

## C6.9 — future policy hooks

Add:

- ExportPolicy;
- parental Settings gate;
- RESTART_HOME_TASK support;
- bulk-rehydrate event.

---

# 31. Final C6 decision table

| Area | Decision |
|---|---|
| LauncherConfig | KEEP |
| DataStore | KEEP |
| Compose settings panel | KEEP |
| D-pad interaction | KEEP |
| No focus scaling on text rows | KEEP |
| Immediate visual preview | KEEP |
| Dedicated text dialogs | KEEP |
| SettingsScreen enum | EXTEND then REPLACE with route stack |
| initialFocus first-item behavior | REPLACE with SettingId restoration |
| Extra/Quick control copies | MERGE |
| direct store.update in every row | REPLACE with controller/descriptors |
| full JSON write per rapid step | OPTIMIZE with debounced persistence |
| language recreate | KEEP behavior, move to effect policy |
| selector wraparound | KEEP for small ranges only |
| dropdown/choice panel | ADD |
| validation | CENTRALIZE |
| schemaVersion/migrations | ADD |
| ExportPolicy | ADD |
| HOME-task restart policy | ADD |
| parental Settings gate | ADD |
| backup save File API | REPLACE |
| backup success reporting | FIX |
| bulk restore | EXTEND with migration/rehydrate |
| Gradle source patching | REMOVE |

---

# 32. C6 closure

No hidden fourth settings subsystem was found.

The effective current Couchy/Aerial-Rows settings architecture is:

```text
SettingsSheet
+ AerialSettingsExtras
+ RowQuickEditor
+ SetupWizard
+ ConfigStore
+ build-time source patching
```

The desired architecture is:

```text
Compose UI
    ≠
setting metadata
    ≠
persistence
    ≠
runtime effects
```

while retaining Couchy's typed config and TV-specific visual strengths.

**C6 status: COMPLETE + REVIEWED.**


---

# SOURCE FILE: `C07.md`

SHA-256: `3947a7627f2fed66747704557066a7c81894f94399f12116370342663c94be3c`

---

# Couchy vs Projectivy — C7 Status, Media & Notifications

Status: **COMPLETE + REVIEWED**  
Repository: `kanozseyer/Aerial-Rows`  
Pinned commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Projectivy comparison basis: canonical Group 5 + post-closure Group 5 findings.

## Executive conclusion

Couchy's current "status" subsystem is small and clean:

```text
ConnectivityManager
      ↓
networkStatusFlow()
      ↓
NetStatus
      ↓
LauncherApp
      ↓
StatusBar
```

plus two root-composition time/date loops.

There is currently **no notification subsystem** and **no media-session/Now Playing subsystem** in the pinned Aerial-Rows source:

- no `NotificationListenerService`;
- no notification listener service declaration;
- no notification state repository;
- no notification panel;
- no notification count/badge;
- no `MediaSessionManager`;
- no `MediaController`;
- no Now Playing row;
- no media actions;
- no media → wallpaper event;
- no media → idle-policy signal.

This makes C7 primarily an **addition**, not a rewrite.

The correct target is:

```text
existing lightweight status UI
        +
thin NotificationListener platform bridge
        +
separate NotificationRepository
        +
separate MediaSessionRepository
        +
Now Playing HomeRow source
        +
central LaunchDispatcher / LaunchPolicy
```

Do not make the NotificationListener itself the application's media/notification architecture.

---

# 1. Current Couchy status model

Current domain model:

```kotlin
data class NetStatus(
    val wifi: Boolean = false,
    val ethernet: Boolean = false,
    val vpn: Boolean = false,
) {
    val connected get() = wifi || ethernet
}
```

`networkStatusFlow()` registers a `ConnectivityManager.NetworkCallback`.

On:

```text
onAvailable
onLost
onCapabilitiesChanged
```

it calls `compute()`.

`compute()` enumerates:

```text
ConnectivityManager.allNetworks
```

and tests each network for:

```text
TRANSPORT_WIFI
TRANSPORT_ETHERNET
TRANSPORT_VPN
```

The flow correctly unregisters its callback in `awaitClose`.

This is a reasonable minimal implementation.

---

# 2. Current StatusBar

The current status bar renders:

```text
VPN
network
settings
optional date
clock
```

The network icon is:

```text
Ethernet
Wi-Fi
Wi-Fi off
```

depending on `NetStatus`.

VPN and network icons are clickable.

The Settings icon opens launcher settings.

The status-bar container is focus-aware and participates in D-pad navigation.

An especially good Couchy behavior is that when the status bar is hidden, the launcher leaves an invisible approximately 4 dp focusable strip. Pressing Up from the top content row can therefore reveal/focus the status bar again without requiring a mouse or another settings entry point.

**KEEP this behavior.**

---

# 3. Network state currently conflates transport presence with Internet availability

Current:

```text
connected = wifi || ethernet
```

This does not mean the device actually has usable Internet.

Examples:

```text
Wi-Fi associated but captive/no Internet
Ethernet connected but gateway offline
network validated through another transport
VPN active over a transport
```

This matters beyond the icon because the Aerial wallpaper retry logic currently uses `net.connected`.

## Target state

Use:

```text
NetworkStatus(
    wifiConnected,
    ethernetConnected,
    vpnActive,
    internetAvailable,
    internetValidated
)
```

Transport identity and Internet reachability should not be the same boolean.

For Aerial/network policy:

```text
internetValidated
```

is the useful state.

For the visual icon:

```text
wifiConnected / ethernetConnected
```

remains useful.

---

# 4. Network callback implementation can be more efficient

Current callback behavior:

```text
network callback
    ↓
enumerate allNetworks
    ↓
getNetworkCapabilities() for each
    ↓
emit aggregate state
```

This can happen several times during one connectivity transition.

## Target

Maintain an internal map:

```text
Network → NetworkCapabilities
```

Initialize once from `allNetworks`, then update it incrementally from:

```text
onAvailable
onCapabilitiesChanged
onLost
```

Derive `NetworkStatus` from the current map.

Only emit when:

```text
newStatus != lastStatus
```

This removes repeated binder enumeration and redundant work.

---

# 5. Current clock/date implementation should be unified

`LauncherApp` currently runs two separate `produceState` loops:

```text
time
date
```

Each wakes at the next minute boundary.

It also increments `resumeTick` on `ON_RESUME` so both refresh when returning from sleep/background.

This works, but it duplicates scheduling.

## Target

Use one:

```text
ClockRepository / MinuteTicker
```

emitting a single:

```text
ClockState(
    instant,
    formattedTime,
    formattedDate
)
```

It should refresh on:

```text
minute boundary
ON_RESUME
ACTION_TIME_CHANGED
ACTION_TIMEZONE_CHANGED
ACTION_DATE_CHANGED
```

A timezone change should not have to wait for an Activity recreation.

---

# 6. Current StatusBar accessibility semantics can improve

The icon content descriptions are generic:

```text
VPN
Network
Settings
```

but visual tint represents active/inactive state.

For accessibility, announce state:

```text
VPN active
VPN disconnected

Ethernet connected
Wi-Fi connected
Network disconnected
```

The visual implementation can remain unchanged.

---

# 7. Notifications are genuinely absent

The current manifest contains only `MainActivity`.

There is no service using:

```text
android.permission.BIND_NOTIFICATION_LISTENER_SERVICE
```

and no:

```text
android.service.notification.NotificationListenerService
```

intent filter.

Therefore notification access cannot currently be granted to Aerial-Rows, and the launcher cannot read or dismiss system notifications.

This is not a hidden or partially wired feature.

It needs to be added deliberately.

---

# 8. MediaSession monitoring is genuinely absent

Current source contains no:

```text
MediaSessionManager
MediaController
getActiveSessions
```

pipeline.

The existing Media3 dependency is used for **wallpaper video playback**, not for observing other applications' media sessions.

There is no reason to couple these two concerns.

---

# 9. Important Projectivy lesson — notifications and media are separate repositories

Projectivy's architecture uses one Android `NotificationListenerService` as a practical platform bridge, but notification state and media-session state are different systems.

Conceptually:

```text
NotificationListenerService
│
├─ notification callbacks
│      ↓
│   NotificationRepository
│
└─ notification-listener authorization
       ↓
   MediaSessionManager
       ↓
   MediaSessionRepository
```

Aerial-Rows should preserve this separation more explicitly than Projectivy.

---

# 10. Thin notification-listener service

Recommended Android component:

```text
LauncherNotificationListenerService
    extends NotificationListenerService
```

Its responsibilities should be intentionally narrow:

```text
onListenerConnected
onListenerDisconnected
onNotificationPosted
onNotificationRemoved
onNotificationRankingUpdate
```

and bridge those callbacks into repositories.

Do not put:

- Compose UI;
- grouping UI logic;
- card creation;
- wallpaper logic;
- parental logic;
- idle policy;

inside the service.

---

# 11. Manifest shape

The new service will require a normal notification-listener declaration equivalent to:

```xml
<service
    android:name=".services.LauncherNotificationListenerService"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

The user grants notification-listener access in Android Settings.

This is **not** the Android 13 `POST_NOTIFICATIONS` runtime permission; Aerial-Rows does not need that permission merely to read notifications through a user-enabled listener.

---

# 12. Notification access belongs in Settings

Using the C6 settings architecture:

```text
Notifications
    ├─ Access status
    ├─ Open notification-access settings
    ├─ Show notification button
    ├─ Auto-hide button when empty
    └─ Notification panel options
```

The UI should detect whether the listener is enabled and clearly show:

```text
Granted
Not granted
```

Do not repeatedly nag the user.

The feature should remain optional.

---

# 13. NotificationRepository

Target:

```text
NotificationRepository
├─ notifications: StateFlow<List<LauncherNotification>>
├─ count: StateFlow<Int>
├─ posted events
├─ removed events
├─ dismiss(key)
├─ clearAll()
└─ listenerConnected
```

Domain item:

```text
LauncherNotification(
    key,
    packageName,
    user,
    appLabel,
    title,
    text,
    postTime,
    icon,
    contentIntent,
    deleteIntent,
    dismissible,
    ongoing,
    groupKey,
    isGroupSummary
)
```

Do not expose raw `StatusBarNotification` to Compose.

---

# 14. Notification filtering should be a testable policy

Projectivy contains a substantial notification filter involving:

- ranking/badge eligibility;
- group summaries;
- ongoing/foreground-service notifications;
- useful title/text;
- system/service exceptions;
- special tags.

Do not copy individual obfuscated branches literally.

Implement:

```text
NotificationPolicy.shouldShow(
    notification,
    ranking,
    context
)
```

with unit tests.

Recommended baseline policy:

```text
reject unusable group summaries
reject notifications with no useful visible content
handle ongoing/foreground-service notifications deliberately
respect package allow/deny policy
retain dismissibility/action information
```

Keep the policy separate from UI.

---

# 15. Notification grouping belongs in the repository/domain layer

Projectivy groups notifications by provider/application and keeps group-summary state outside the UI.

Aerial-Rows should expose ready-to-render group models:

```text
NotificationGroup(
    packageName,
    appLabel,
    notifications
)
```

Recommended ordering:

```text
provider groups → app label alphabetically
inside group    → newest first
```

The Compose panel then only renders state.

---

# 16. StatusBar notification indicator

Projectivy uses the current filtered notification state for its notification indicator and panel.

Aerial-Rows can integrate this naturally into its existing status bar.

Recommended order:

```text
VPN
network
notifications
settings
date
clock
```

with configuration:

```text
showNotificationButton
autoHideNotificationButton
```

Badge:

```text
0     → no badge
1..9  → number
10+   → "9+" or "+"
```

The exact visual representation can remain Aerial-Rows styled.

When the status bar is hidden, the existing focus-to-reveal mechanism still works.

---

# 17. Notification panel

Use a Compose side panel/dialog rather than Projectivy's PreferenceFragment infrastructure.

Recommended:

```text
NotificationPanel
    ├─ app group heading
    ├─ notification cards
    ├─ open
    ├─ dismiss
    └─ Clear all
```

Show Clear all only when there are enough dismissible notifications to make it useful.

Incremental repository state means Compose will reconcile removals naturally; no manual preference-tree surgery is needed.

---

# 18. Notification launch must pass through LaunchPolicy

A notification can supply a `PendingIntent`.

Do not directly call:

```text
pendingIntent.send()
```

from the UI.

Use:

```text
LaunchDispatcher.launch(
    LaunchTarget.PendingIntent(
        ownerPackage,
        pendingIntent
    )
)
    ↓
LaunchPolicy
```

This lets future parental/security policy apply consistently.

The owning package is a useful policy identity even if the `PendingIntent` destination itself is opaque.

---

# 19. Notification dismissal must NOT stop playback automatically

Projectivy has a special `Notification.NowPlaying` path where dismissing the linked notification can issue:

```text
Stop
or fallback Pause
```

before cancelling the notification.

That is surprising coupling.

Aerial-Rows should improve this.

Default semantics:

```text
Dismiss notification
    ≠
Stop playback
```

If we later add a provider-specific option that deliberately links them, it should be explicit.

---

# 20. MediaSession access

On normal third-party launcher installs, the practical route to other apps' active sessions is:

```text
user grants Notification Listener access
        ↓
MediaSessionManager.getActiveSessions(
    notificationListenerComponent
)
```

Projectivy uses this same platform relationship.

Aerial-Rows should use the enabled notification-listener component as the authorization bridge.

Catch `SecurityException` and degrade to:

```text
no active sessions
```

rather than destabilizing HOME.

---

# 21. Use Android platform media-session APIs

Aerial-Rows already depends on Media3 ExoPlayer for wallpaper playback, but it does not need to add Media3 Session solely for Now Playing.

API 21+ provides:

```text
android.media.session.MediaSessionManager
android.media.session.MediaController
android.media.MediaMetadata
android.media.session.PlaybackState
```

These provide:

- active sessions;
- package name;
- session token;
- metadata;
- playback state;
- playback actions;
- session activity PendingIntent;
- transport controls;
- callbacks.

Using the platform API keeps this subsystem lightweight and matches Aerial-Rows' current minSdk 21.

---

# 22. MediaSessionRepository

Target:

```text
MediaSessionRepository
├─ sessions: StateFlow<List<MediaSessionItem>>
├─ primarySession: StateFlow<MediaSessionItem?>
├─ isAnyPlaying: StateFlow<Boolean>
├─ connect()
├─ disconnect()
└─ reconcileActiveSessions()
```

The repository is application-scoped.

The home row is one consumer, not its owner.

---

# 23. MediaSessionItem

Recommended domain object:

```text
MediaSessionItem(
    runtimeId,
    packageName,
    appLabel,
    title,
    artist,
    album,
    artwork,
    isPlaying,
    playbackState,
    availableActions,
    sessionActivity,
    token
)
```

Possible richer fields:

```text
duration
position
lastPositionUpdateTime
```

but do not create a 1 Hz global recomposition loop merely to animate progress.

If progress is displayed, calculate/interpolate it only for visible/focused cards.

---

# 24. Session identity must be runtime-stable, not falsely durable

Projectivy ultimately exposes a 32-bit hash derived from its session token string as the card integer ID.

Its repository-level deduplication uses the stronger session-token object.

Aerial-Rows should keep the stronger identity throughout.

Recommended runtime identity:

```text
MediaSessionRuntimeId(
    packageName,
    sessionToken
)
```

For Compose/presentation a derived string/hash may be used, but treat it as **ephemeral**.

Do not persist user customization against an active-session token.

---

# 25. Session reconciliation

When active sessions change:

```text
new platform controller list
        ↓
match existing wrappers by token
        ↓
retain unchanged wrappers/callbacks
        ↓
create wrappers only for new sessions
        ↓
unregister removed sessions
```

This is more efficient than tearing down/recreating every controller on every callback.

Projectivy rebuilds wrappers aggressively; Aerial-Rows can improve that.

---

# 26. Active-session filtering

Now Playing should not show terminal/dead sessions.

Use semantic platform playback states.

Eligible examples:

```text
PLAYING
PAUSED
BUFFERING
CONNECTING
FAST_FORWARDING
REWINDING
SKIPPING...
```

Exclude:

```text
NONE
STOPPED
ERROR
```

The exact policy should be unit-tested.

---

# 27. Multiple active sessions

Projectivy can show more than one active media session.

Aerial-Rows should preserve this.

Example:

```text
Spotify paused
YouTube playing
Kodi paused
```

can produce several Now Playing cards if all remain active according to policy.

Do not collapse everything to one package/global card.

---

# 28. Primary-session selection

A separate primary session is useful for:

- wallpaper metadata;
- global playback indicator;
- idle policy.

Recommended priority matching Projectivy's semantics:

```text
1. first PLAYING session
2. otherwise first active/non-terminal session
3. otherwise none
```

A stable secondary ordering may use:

```text
last playback-state update time
```

to avoid arbitrary controller ordering.

---

# 29. Coalescing media callbacks

Media applications can produce bursts of:

```text
metadata changed
state changed
queue changed
session event
```

Do not rebuild the home row/wallpaper on every callback synchronously.

Use bounded coalescing.

Suggested:

```text
repository session-state reconciliation:
    ~100–200 ms debounce/conflate

wallpaper NowPlayingChanged:
    up to ~500 ms coalescing
```

This keeps play/pause UI responsive while preventing wallpaper churn.

---

# 30. Media controls must be capability-aware

Use the current `PlaybackState.actions` bitmask.

Expose only supported actions:

```text
Play
Pause
Play/Pause
Stop
Previous
Next
```

Examples:

```text
ACTION_PLAY
ACTION_PAUSE
ACTION_PLAY_PAUSE
ACTION_STOP
ACTION_SKIP_TO_PREVIOUS
ACTION_SKIP_TO_NEXT
```

Do not render disabled-looking controls for operations the session cannot perform.

---

# 31. Launch behavior for Now Playing

Projectivy uses:

```text
session PendingIntent first
        ↓
owning app launcher fallback
```

This is the correct behavior.

Aerial-Rows should preserve it but improve policy coverage:

```text
NowPlaying card clicked
        ↓
LaunchDispatcher
        ↓
LaunchPolicy(packageName)
        ↓ allowed
sessionActivity.send()
        ↓ failure/absent
launch owning package
```

Unlike Projectivy, do not bypass the central policy layer.

---

# 32. Parental policy inheritance

Derived Now Playing cards should inherit policy from their owning app.

Example:

```text
package Netflix protected
        ↓
Netflix MediaSession
        ↓
Now Playing card is protected
```

Do not create a separate manually toggled protection identity for every transient session.

---

# 33. Artwork model

Projectivy's Now Playing card can display:

```text
artwork URI
embedded artwork bytes
fallback icon
```

while its external wallpaper event loses the embedded byte form.

Aerial-Rows can improve this internally.

Use:

```text
MediaArtwork
├─ Uri
├─ BitmapRef / CachedBitmap
└─ None
```

Extract from platform metadata in sensible priority:

```text
DISPLAY_ICON_URI
ALBUM_ART_URI
ART_URI
DISPLAY_ICON bitmap
ALBUM_ART bitmap
ART bitmap
```

Downsample/cache artwork rather than retaining unlimited full-resolution bitmaps.

---

# 34. Now Playing home-row integration

`SlidingRows.kt` currently knows only:

```text
WatchNextHomeRow
ChannelHomeRow
CategoryHomeRow
```

Add:

```text
NowPlayingHomeRow(
    sessions: List<MediaSessionItem>
)
```

with stable row ID:

```text
now_playing
```

Then it naturally participates in:

```text
home row ordering
left rail
row focus
row visibility
```

instead of creating a separate special overlay.

This matches Projectivy's strongest architectural decision: Now Playing is a normal home-row source.

---

# 35. Do not make SlidingRows own MediaSessionRepository

The current TV-content implementation loads inside `SlidingRowsArea`.

C4 already identified that as too UI-owned.

For media the repository boundary should be correct from day one:

```text
application/domain layer
    MediaSessionRepository
        ↓
HomeRowsRepository
        ↓
NowPlayingHomeRow
        ↓
SlidingRows renderer
```

This also allows future Carousel/Grid/Dock layouts to consume the same domain content if desired.

---

# 36. NowPlayingCard

Do not reuse `TvProgramCard` blindly.

Create a dedicated card or generic richer HomeCard model because media needs:

- session-level identity;
- playback glyph/state;
- richer subtitle;
- media actions;
- artwork URI/bitmap fallback;
- possibly progress;
- launch package identity.

Recommended visible data:

```text
artwork
playing/paused indicator
title
artist · album
```

Long press can open media controls.

---

# 37. Media action menu

Recommended actions:

```text
Open
Play / Pause
Stop
Previous
Next
App info
```

only where applicable.

`Open` passes through LaunchPolicy.

Media transport actions do not launch another Activity, but parental policy may still optionally gate them if the owning app is protected.

That choice belongs in `LaunchPolicy` / parental policy, not the card renderer.

---

# 38. Notification ↔ MediaSession correlation

Do not reconstruct Now Playing from notification title/text.

If a notification needs to be correlated with a media session, prefer:

```text
notification package
        ↓
media session package
        ↓
known session relation
        ↓
title fallback only if necessary
```

Projectivy's title-only special matching is weaker than we need.

---

# 39. Wallpaper integration

C5 established the target semantic wallpaper engine.

MediaSessionRepository should publish a rich internal event:

```text
WallpaperEvent.NowPlayingChanged(
    sessionId,
    packageName,
    isPlaying,
    title,
    artist,
    album,
    artwork
)
```

This is richer than Projectivy's external IPC event and is safe because it remains internal.

Focused media card:

```text
WallpaperEvent.HomeItemFocused(
    NowPlaying(...)
)
```

or a specialized semantic equivalent.

The WallpaperEngine consumes the event; MediaSessionRepository does not call ExoPlayer directly.

---

# 40. Idle/power integration

Projectivy also uses active media playback as one input into its idle/power policy.

Keep the same separation:

```text
MediaSessionRepository.isAnyPlaying
        ↓
LauncherIdleCoordinator
```

The media repository itself must not own screensaver/idle behavior.

C9 will close the complete idle policy.

---

# 41. Observation demand / background efficiency

The media repository can be application-scoped without always maintaining expensive controller callbacks.

Define demand:

```text
MediaObservationDemand
├─ Now Playing row enabled
├─ wallpaper needs Now Playing state
├─ idle policy excludes media playback
└─ future status media indicator
```

If none of those consumers is enabled:

```text
do not maintain active MediaController callbacks
```

Notification filtering can remain active while the user has enabled notification display.

This is an optional optimization; correctness comes first.

---

# 42. Service disconnect and permission revocation

When notification-listener access is lost or the service disconnects:

```text
NotificationRepository
    → clear current state
    → listenerConnected = false

MediaSessionRepository
    → unregister controller callbacks
    → clear sessions
    → primarySession = null
```

The UI should degrade cleanly:

```text
notification button hidden/disabled
Now Playing row disappears
settings shows access not granted
```

Never leave stale cards indefinitely.

---

# 43. Security / privacy boundary

Notification contents can contain private information.

Aerial-Rows should:

- keep notification state in memory;
- not persist notification text in launcher backup/config;
- not send notification/media metadata over the network;
- not write notification contents to verbose production logs;
- expose clear settings for enabling the feature;
- treat PendingIntents as opaque capabilities;
- route user-initiated launches through LaunchPolicy.

Media-session metadata should likewise remain transient unless the user explicitly opts into a feature that needs storage.

---

# 44. StatusBar performance after adding notifications

Keep the StatusBar dumb:

```text
StatusBarUiState(
    network,
    vpn,
    notificationCount,
    time,
    date
)
```

Do not let it collect four repositories independently.

Create a presentation/view-model layer which combines flows and emits structurally equal immutable UI state.

Compose then recomposes only when visible state changes.

---

# 45. StatusBar notification settings

Recommended settings:

```text
showStatusBar
showVpnButton
showNotificationButton
autoHideNotificationButton
statusBarGlass
dateFormat
clockFormat
```

Optional future:

```text
showMediaIndicator
```

but the Now Playing row is the better primary media UI.

---

# 46. Notification panel interaction

Recommended remote behavior:

```text
OK
    → open notification through LaunchDispatcher

Long press / Menu
    → actions:
       Dismiss
       App info
       Notification settings

Clear all
    → cancel only dismissible notifications
```

Do not attempt to clear ongoing/non-clearable notifications.

---

# 47. Testing requirements

## Network

- Wi-Fi only
- Ethernet only
- VPN over Wi-Fi
- Wi-Fi connected but Internet unvalidated
- network handover
- callback bursts
- callback unregister

## Clock

- minute rollover
- resume after sleep
- manual time change
- timezone change
- date rollover
- language change

## Notification repository

- service connect/disconnect
- initial full refresh
- post
- remove
- ranking update
- group summary
- dismissible vs ongoing
- clear all
- permission revoked

## Media sessions

- zero sessions
- one playing
- several sessions
- paused session
- session removed
- metadata change
- artwork change
- duplicate token reconciliation
- capability change
- sessionActivity absent/failing
- package fallback
- SecurityException

## Policy

- protected package Now Playing launch
- denied PendingIntent launch
- notification open through policy
- notification dismiss does not stop playback

---

# 48. Recommended C7 implementation order

## C7.0 — complete Phase 0 first

The overall source-pure build requirement from C2/C5/C6 still applies before significant implementation work.

C7 itself currently has no injected media/notification code to materialize.

## C7.1 — improve current status repositories

Extract:

```text
NetworkStatusRepository
ClockRepository
```

Keep current StatusBar UI.

## C7.2 — notification-access plumbing

Add:

```text
LauncherNotificationListenerService
NotificationAccessRepository
settings action/status
```

No panel yet.

## C7.3 — NotificationRepository

Add:

- full current state;
- filtering;
- grouping;
- dismiss;
- clear all;
- reconnect cleanup.

## C7.4 — StatusBar notification button + panel

Add badge and Compose notification side panel.

## C7.5 — MediaSessionRepository

Use platform:

```text
MediaSessionManager
MediaController
PlaybackState
MediaMetadata
```

Reconcile by session token.

## C7.6 — Now Playing row

Add:

```text
NowPlayingHomeRow
NowPlayingHomeItem
NowPlayingCard
```

to HomeRowsRepository / Sliding Rows.

## C7.7 — actions and launch policy

Add capability-aware controls and:

```text
sessionActivity → LaunchDispatcher → LaunchPolicy
```

with owning-package protection inheritance.

## C7.8 — wallpaper + idle signals

Feed:

```text
NowPlayingChanged
```

to WallpaperEngine and:

```text
isAnyPlaying
```

to the future LauncherIdleCoordinator.

## C7.9 — settings/polish

Add:

- show Now Playing;
- show notification button;
- auto-hide;
- access status;
- optional media/notification filters.

---

# 49. Final decision table

| Area | Decision |
|---|---|
| StatusBar visual structure | KEEP |
| Hidden 4 dp focus/reveal strip | KEEP |
| Network callback lifecycle | KEEP concept |
| `allNetworks` on every callback | OPTIMIZE |
| `connected = wifi || ethernet` | REPLACE for Internet policy |
| two separate clock/date loops | MERGE |
| dynamic state content descriptions | ADD |
| NotificationListenerService | ADD |
| NotificationRepository | ADD |
| notification panel | ADD |
| notification status badge | ADD |
| notification persistence | DO NOT ADD |
| MediaSessionRepository | ADD |
| Media3 Session dependency | NOT REQUIRED |
| platform MediaSession APIs | USE |
| multiple active media sessions | ADD |
| session-level runtime identity | ADD |
| Now Playing row | ADD |
| capability-aware media controls | ADD |
| session PendingIntent first | ADD |
| app launch fallback | ADD |
| central LaunchPolicy | REQUIRED |
| Now Playing parental inheritance | ADD |
| title-only notification/session matching | AVOID |
| dismiss notification stops playback | DO NOT COPY |
| media → wallpaper event | ADD |
| media → idle signal | ADD |
| service/repository cleanup on revoke | REQUIRED |

---

# 50. C7 closure

The Couchy side is fully understood for this scope.

Current architecture:

```text
Status.kt
    ↓
NetStatus
    ↓
LauncherApp clock/date state
    ↓
StatusBar.kt
```

There is no hidden media/notification implementation.

Target architecture:

```text
NetworkStatusRepository ─┐
ClockRepository ─────────┼──→ StatusBarUiState → StatusBar
NotificationRepository ──┘          │
                                    └→ NotificationPanel

NotificationListenerService
          │
          ├→ NotificationRepository
          │
          └→ MediaSession access bridge
                    ↓
             MediaSessionRepository
                ├→ Now Playing row
                ├→ media controls
                ├→ WallpaperEngine
                └→ LauncherIdleCoordinator
```

The crucial rule is:

```text
Notification state
        ≠
Media-session state
```

and:

```text
Now Playing
    comes from MediaSessions,
    not notification text.
```

**C7 status: COMPLETE + REVIEWED.**


---

# SOURCE FILE: `C08.md`

SHA-256: `1b5d4ac78301cc6a635d9554cdde44ae61e607fc8857044c391779ed18f5019b`

---

# Couchy vs Projectivy — C8 TV Inputs & Device Integration

Status: **COMPLETE + REVIEWED**  
Repository: `kanozseyer/Aerial-Rows`  
Pinned commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Projectivy comparison basis: canonical Group 8 post-closure re-audit v2.

## Executive conclusion

Couchy/Aerial-Rows currently has **no TV Input Framework subsystem**.

The pinned source tree contains:

```text
Actions.kt
MainActivity.kt
AppRepository.kt
BuiltinAerials.kt
Config.kt
Status.kt
TvContentRepository.kt
UI files
```

but no:

```text
TvInputManager repository
TvInputManager.TvInputCallback
TvView input Activity
HDMI/source model
input launch coordinator
current-input repository
display-profile repository
picture backend
vendor input adapter
```

The current manifest likewise declares no dedicated TV-input Activity/service, and `Actions.kt` has no HDMI/source launch operation.

This is therefore a clean-sheet addition.

The Projectivy input subsystem is mature enough to provide the behavioral reference, but Aerial-Rows should **not copy several of its reliability/coupling weaknesses**.

The target is:

```text
Android TvInputManager
        ↓
InputRepository
        ├─ AvailableInputs
        ├─ VisibleInputShortcuts
        └─ InputAvailabilityState
        ↓
InputLaunchCoordinator
        ├─ StandardTvInputStrategy
        └─ optional VendorInputAdapters
        ↓
CurrentInputState

optional:
DisplayStateRepository
        ↓
DisplayProfileCoordinator
        ↓
PictureBackendRegistry
```

---

# 1. TV Provider content is not TV Input Framework input

C4 covered:

```text
TvContract Preview Channels
Watch Next
TvContentRepository
```

Those are launcher/content recommendation APIs.

C8 is a different Android subsystem:

```text
android.media.tv.TvInputManager
TvInputInfo
TvView
TvInputService
HDMI / AV / tuner inputs
```

Do not combine the two repositories merely because both contain "TV".

Recommended package separation:

```text
data/tvcontent/
data/tvinput/
```

or equivalent.

---

# 2. Current Couchy input support: none

The pinned source tree has no TV-input-specific Kotlin file or component.

`MainActivity` currently owns:

- TV Listings permission;
- package-change receiver;
- locale bootstrap;
- HOME Back suppression;
- Compose root.

It has no `TvInputManager` state.

`Actions` currently owns:

- app launch;
- Android TV Preview/Watch Next content launch;
- app info;
- uninstall;
- best-effort close;
- Android/network/VPN settings;
- app-store launch.

It has no physical-source launch method.

`LauncherConfig` currently contains no:

- input visibility;
- input ordering;
- input aliases;
- last-used input;
- display-profile bindings;
- compatibility mode.

No hidden implementation exists elsewhere in the pinned Kotlin tree.

---

# 3. What Projectivy gets right

Projectivy's verified architecture is broadly:

```text
stable generic source identity
        ↓
TvInputManager/TIF discovery & mapping
        ↓
source launch coordinator
        ├─ internal TvView
        └─ platform/vendor fallback strategies

DisplayProfileManager
        ↓
vendor picture backends
```

Important useful concepts:

1. separate generic/stable source identity from platform TIF identity;
2. cache discovered inputs;
3. support standard Android TIF before vendor-specific strategies;
4. store the user's last requested source;
5. allow remote/startup code to target the same source-launch layer;
6. bind SDR/HDR picture profiles by stable profile UUID;
7. select picture backends based on capabilities;
8. keep vendor/device behavior optional.

These are worth adopting conceptually.

---

# 4. What Projectivy should NOT be copied literally

The Group 8 v2 audit found several weaknesses.

## 4.1 No repository-level TvInputManager callback

Projectivy uses `TvView.TvInputCallback` for an actively tuned TvView but does not register a repository-level:

```text
TvInputManager.TvInputCallback
```

for input add/remove/update discovery.

Aerial-Rows should use it.

## 4.2 Shortcut visibility leaks into display-profile binding

Projectivy derives the display-profile input picker from the list of visible input shortcuts.

Therefore:

```text
hide HDMI shortcut
    ↓
HDMI disappears from profile binding UI
```

even though the physical input still exists.

Do not reproduce this.

## 4.3 Current-input state is strategy-dependent

Some Projectivy launch paths update both:

```text
input_last_used
current in-memory source
```

while some external fallbacks only reliably update persisted last-used state.

Do not let individual adapters own canonical current state.

## 4.4 Recovery can depend on later demand

Sony/TCL empty-input recovery is bounded but demand-triggered rather than driven by an explicit retry scheduler.

Use a real repository refresh/backoff policy instead.

## 4.5 Missing-port synthesis is easy to misread

Projectivy only synthesizes generic HDMI/AV fallbacks when the **entire passthrough list is empty**.

It does not fill individual holes in a partially authoritative list.

Keep this principle.

## 4.6 Display profile polling is Accessibility-owned

Projectivy's automatic display-profile switching runs from Accessibility.

That creates unnecessary coupling.

Aerial-Rows should keep:

```text
Accessibility
    ≠
input discovery
    ≠
display profile observation
```

## 4.7 Poller can terminate on transient null

Projectivy's 4-second display poll can stop rescheduling if a transient sample is invalid/null.

Aerial-Rows must use a resilient lifecycle-owned observation loop.

## 4.8 Dangling profile bindings are tolerated

Deleted/malformed profile UUID bindings simply resolve to null.

Aerial-Rows should clean or visibly flag invalid references.

---

# 5. Public Android API should be the primary discovery layer

Use:

```text
TvInputManager
```

from `Context.TV_INPUT_SERVICE`.

The repository should obtain:

```text
getTvInputList()
```

and map `TvInputInfo` records into domain models.

Register:

```text
TvInputManager.TvInputCallback
```

for:

```text
onInputAdded
onInputRemoved
onInputUpdated
onInputStateChanged
```

and refresh the relevant repository state.

Also perform a safety refresh on:

```text
launcher/app ON_RESUME
```

because vendor implementations are not always perfectly callback-consistent.

---

# 6. Input domain model

Do not expose `TvInputInfo` directly to Compose.

Recommended:

```kotlin
data class TvInputSource(
    val id: InputId,
    val platformInputId: String,
    val type: InputType,
    val label: String,
    val packageName: String?,
    val state: InputConnectionState,
    val passthrough: Boolean,
    val parentPlatformInputId: String?,
    val capabilities: Set<InputCapability>,
)
```

`InputType`:

```text
HDMI
AV
COMPONENT
DISPLAY_PORT
TUNER
OTHER
```

The exact mapping should preserve unknown/vendor types rather than dropping them.

---

# 7. Stable identity

Do not use labels such as:

```text
HDMI 1
HDMI 2
```

as durable identities.

Preferred:

```text
InputId(
    platformInputId
)
```

with an optional normalized generic kind:

```text
GenericSource.HDMI1
GenericSource.HDMI2
GenericSource.AV1
...
```

where reliable mapping is available.

Persist platform IDs where possible and use generic source only as compatibility/fallback metadata.

If a vendor recreates IDs across firmware updates, a migration/fingerprint layer can reconcile:

```text
package
type
parent
logical port hint
label fallback
```

Do not silently guess when several candidates match.

---

# 8. Three separate collections

Aerial-Rows should explicitly maintain three concepts.

## 8.1 DiscoveredInputs

What Android/vendor discovery returned.

## 8.2 AvailableInputs

Inputs the launcher believes can currently be launched.

This can include:

```text
discovered TIF input
+
explicit vendor compatibility source
```

## 8.3 VisibleInputShortcuts

The user's home UI projection.

Therefore:

```text
input exists
    ≠
input is launchable
    ≠
input shortcut is visible
```

This is one of the most important improvements over Projectivy.

---

# 9. Do not synthesize holes in partial discovery

Suppose Android reports:

```text
HDMI1
HDMI3
```

Do not automatically create HDMI2/HDMI4 because the model is known to have four ports.

The authoritative platform list may reflect:

- disabled ports;
- vendor implementation;
- parent/child TIF structure;
- inaccessible source;
- firmware behavior.

Compatibility synthesis should run only when:

```text
authoritative passthrough discovery is completely unavailable
```

and an explicit vendor adapter or user compatibility mode supports it.

---

# 10. InputRepository

Recommended:

```text
InputRepository
├─ discoveredInputs: StateFlow<List<TvInputSource>>
├─ availableInputs: StateFlow<List<TvInputSource>>
├─ inputStates: StateFlow<Map<InputId, InputConnectionState>>
├─ refresh()
├─ start()
└─ stop()
```

`start()`:

```text
register TvInputManager callback
initial refresh
```

`stop()`:

```text
unregister callback
```

The repository should be application-scoped or lifecycle-scoped to launcher visibility, not created inside a composable row.

---

# 11. Refresh and retry policy

Use:

```text
callback-driven refresh
+
ON_RESUME refresh
+
bounded recovery after abnormal empty state
```

Example:

```text
expected TV-input-capable device
AND
previously had inputs
AND
current discovery unexpectedly empty
    ↓
retry 0.5 s
retry 1 s
retry 2 s
retry 4 s
then settle as empty/unknown
```

Do not retry forever.

Do not depend on a future getter call to trigger recovery.

---

# 12. Capability detection

Do not show an Inputs section just because Android TV exists.

Recommended capability result:

```text
InputFeatureState
├─ Unsupported
├─ SupportedNoInputs
├─ Supported(inputs)
└─ CompatibilityMode(adapter)
```

Evidence can include:

```text
TvInputManager service available
TvInputInfo list
passthrough inputs present
vendor adapter capability
explicit user compatibility override
```

This is more reliable than a single system-feature flag.

---

# 13. Home-row integration

Physical inputs should become a normal home source.

Recommended row:

```text
InputHomeRow
    id = "inputs"
```

Cards:

```text
InputHomeItem(
    inputId,
    label,
    state,
    icon
)
```

This allows:

- row ordering;
- left-rail integration;
- focus restoration;
- row visibility;
- per-item actions.

Do not hardwire HDMI buttons into `StatusBar` or `MainActivity`.

---

# 14. Input shortcuts should be user-configurable

Settings/row editor should support:

```text
Show/hide individual input shortcut
Rename display label
Reorder
Choose icon
```

Optional:

```text
Pin input to another category/row
```

But the underlying input remains in `InputRepository`.

Hiding a shortcut must not remove the input from:

- autostart target picker;
- remote mapping picker;
- display-profile binding picker.

---

# 15. Standard launch strategy — isolated TvInputActivity

For standard Android TIF sources, use a dedicated fullscreen Activity rather than embedding TV input playback inside the launcher home composition.

Example:

```text
TvInputActivity
    ↓
TvView
    ↓
tune(platformInputId, uri)
```

For passthrough inputs:

```text
TvContract.buildChannelUriForPassthroughInput(inputId)
```

For a real tuner/channel source, model it explicitly and tune an actual channel URI rather than treating `buildChannelsUriForInput()` as proof of first-class tuner support.

Benefits of a dedicated Activity:

- isolates Surface/decoder/resource lifecycle;
- clean Back → launcher behavior;
- clean remote-key dispatch;
- avoids Compose home recomposition affecting TvView;
- clear active-session callback ownership.

---

# 16. Active TvView callback

Inside `TvInputActivity`, use:

```text
TvView.TvInputCallback
```

for active playback/tune state.

This is different from:

```text
TvInputManager.TvInputCallback
```

in `InputRepository`.

Responsibilities:

```text
TvInputManager callback
    → discovery/input-state changes

TvView callback
    → active tune/video/session events
```

Do not conflate them.

---

# 17. InputLaunchCoordinator

Every input launch should go through one coordinator:

```text
InputLaunchCoordinator.launch(input)
```

which tries strategies in a deterministic order.

Example:

```text
1. StandardTvInputStrategy
2. vendor strategy known to be required/preferred
3. generic external TV/source app fallback
```

Each strategy returns a typed result:

```text
LaunchResult.Success(strategy, platformId)
LaunchResult.Unsupported
LaunchResult.Failure(reason)
```

Do not hide failures behind booleans.

---

# 18. Central launch policy

Input cards should also go through:

```text
LaunchDispatcher
    ↓
LaunchPolicy
    ↓
InputLaunchCoordinator
```

This keeps:

- parental restrictions;
- future input restrictions;
- launch analytics/history;
- startup invocation;

consistent with apps, TV content and Now Playing.

---

# 19. Current input state — improve on Projectivy

Projectivy has a reliable persisted `input_last_used`, but some fallback paths do not update the same in-memory current source.

Aerial-Rows should model state centrally.

Recommended:

```text
CurrentInputState(
    requestedInputId,
    launchedInputId,
    strategy,
    status,
    confirmedInputId?,
    timestamp
)
```

Possible `status`:

```text
REQUESTED
LAUNCHED
CONFIRMED
FAILED
UNKNOWN
```

Why distinguish requested vs confirmed?

For an internal `TvView` session we can often know exactly what was tuned.

For an external vendor source application we may only know:

```text
we successfully requested HDMI2
```

not that the physical TV actually switched.

Do not falsely promote a requested source to hardware-confirmed state.

---

# 20. Persist last used separately

Persist:

```text
lastRequestedInputId
```

after an accepted launch request.

Optionally persist:

```text
lastConfirmedInputId
```

where a backend can verify it.

This makes future autostart semantics explicit:

```text
LAST_REQUESTED_INPUT
LAST_CONFIRMED_INPUT
```

rather than one ambiguous field.

---

# 21. Previous/next remote cycling

Projectivy's remote hotkey behavior is:

```text
previous/next visible generic source
    ↓
downstream launch attempt
```

It does not iterate the actual live TIF input list.

Aerial-Rows should improve this.

Use:

```text
VisibleInputShortcuts snapshot
    ↓
find current/requested input
    ↓
previous/next
    ↓
LaunchDispatcher
```

or, if the user selects such a mode:

```text
AvailableInputs snapshot
```

The semantics should be explicit in settings.

Never cycle enum ordinals that may not correspond to real discovered inputs.

---

# 22. Direct HDMI remote mappings

Future remote settings can support:

```text
HDMI1
HDMI2
HDMI3
HDMI4
```

but resolve those names through `InputRepository`.

Example:

```text
RemoteAction.Hdmi(2)
    ↓
repository.resolve(GenericSource.HDMI2)
    ↓
LaunchDispatcher
```

If HDMI2 does not exist:

```text
no-op + optional user feedback
```

not a blind vendor intent.

---

# 23. Vendor adapters

Keep vendor behavior optional.

Interface:

```kotlin
interface InputLaunchStrategy {
    val id: String
    fun isSupported(device: DeviceCapabilities): Boolean
    suspend fun launch(input: TvInputSource): LaunchResult
}
```

Possible adapters later:

```text
Xiaomi
MediaTek
Amlogic/FengOS
Google/AOSP external TV app
```

Only implement an adapter when a target device requires it and we can test it.

Do not put vendor reflection/intent strings into `InputRepository`.

---

# 24. DeviceCapabilities

Create a small central device capability service rather than rediscovering vendor/package/features in every subsystem.

Example:

```text
DeviceCapabilities
├─ manufacturer
├─ model
├─ systemFeatures
├─ installedKnownPackages
├─ tvInputAvailable
├─ vendorFamily
└─ privilegedCapabilities
```

This can later be shared by:

- input adapters;
- display backends;
- startup compatibility;
- Accessibility workarounds.

Avoid giant manufacturer `when` statements in UI.

---

# 25. Manual compatibility mode

Projectivy has a hidden seven-activation compatibility override.

Aerial-Rows can be more explicit.

Under:

```text
Settings → Advanced → TV input compatibility
```

offer:

```text
Automatic
Force enabled
Disabled
```

Only show the advanced override when useful.

If forced mode synthesizes generic HDMI entries, label them as compatibility assumptions rather than discovered physical inputs.

---

# 26. Display profiles are optional expert functionality

Standard Android does not provide one universal public API for television picture-mode control across vendors.

Therefore display-profile support should be:

```text
optional
capability-driven
vendor-adapter based
```

not a dependency of the core input row.

Core input support must work even when:

```text
PictureBackendRegistry = empty
```

---

# 27. DisplayProfileRepository

If implemented:

```text
DisplayProfile(
    id: UUID,
    name,
    values
)
```

Repository:

```text
DisplayProfileRepository
├─ profiles
├─ create
├─ update
├─ delete
├─ duplicate
└─ repairBindings
```

Profile IDs must be stable UUIDs.

Do not use list index as persisted identity.

---

# 28. SDR/HDR binding model

Bindings should be separate from shortcut visibility.

Recommended:

```text
InputDisplayBinding(
    inputId,
    sdrProfileId?,
    hdrProfileId?
)
```

Selection key:

```text
(inputId, DynamicRange)
```

where:

```text
DynamicRange.SDR
DynamicRange.HDR
DynamicRange.UNKNOWN
```

The input label is presentation metadata, not part of the profile selection key.

This preserves the Projectivy v2 correction:

```text
label-only changes
    ≠
profile reapplication
```

---

# 29. Referential integrity

On:

```text
profile deletion
profile import
migration
backup restore
```

check every binding.

If a binding refers to a missing UUID:

```text
clear binding
or
mark it visibly invalid and require repair
```

Do not silently leave a dangling value forever.

---

# 30. PictureBackend interface

Recommended:

```kotlin
interface PictureBackend {
    val id: String
    val capabilities: PictureBackendCapabilities
    suspend fun readCurrentDisplayState(): DisplayState?
    suspend fun apply(profile: DisplayProfile): ApplyResult
}
```

Capabilities should be semantic:

```text
CURRENT_INPUT
HDR_STATE
PICTURE_MODE
BRIGHTNESS
CONTRAST
COLOR
SHARPNESS
...
```

Do not choose a backend based on reflection-method count as Projectivy does.

Choose by explicit suitability score/capabilities.

---

# 31. Known Projectivy backend families

Projectivy's verified candidates are:

```text
Amlogic SystemControlManager
Amlogic TvControlManager
Xiaomi PictureSettingsManager/source manager
```

These are useful research references for future adapters.

They should not enter Aerial-Rows core until:

- target hardware needs them;
- behavior can be tested;
- failures are isolated.

---

# 32. DisplayStateRepository

Do not make Accessibility responsible for display sampling.

Use:

```text
DisplayStateRepository
    ↓
PictureBackend
    ↓
StateFlow<DisplayState>
```

`DisplayState`:

```text
currentInputId?
dynamicRange
label?
backendState
```

This repository owns retry/observation lifecycle.

---

# 33. Polling policy

If no event-based vendor callback exists, polling is acceptable.

Projectivy uses 4 seconds.

Aerial-Rows can start with:

```text
foreground/launcher-active: 2–4 s
background: stop or much slower
```

depending on whether automatic profile switching must continue while another source Activity is foreground.

The important rule:

```text
invalid/null sample
    ≠
stop observation
```

A transient failure should:

```text
emit Unknown or retain last-known state
schedule next observation
```

---

# 34. Profile application coordinator

Recommended:

```text
DisplayProfileCoordinator
    observes DisplayState
        ↓
key = (inputId, dynamicRange)
        ↓
resolve binding
        ↓
if key changed and profile exists
        ↓
apply profile
```

Use `distinctUntilChanged()` on the selection key.

Do not reapply merely because a display label changed.

---

# 35. Input launch and display profile state must remain independent

Launching an input does not prove the TV switched successfully.

Therefore:

```text
InputLaunchCoordinator
    → Requested/Launched state

DisplayStateRepository
    → Confirmed physical/backend state

DisplayProfileCoordinator
    → apply when confirmed state supports it
```

This avoids applying an HDMI2 profile just because an external vendor intent to HDMI2 returned without throwing.

---

# 36. UI structure

Recommended home:

```text
Inputs row
    HDMI 1
    HDMI 2
    AV
    ...
```

Recommended settings:

```text
Inputs
├─ Show input row
├─ Input shortcuts
│   ├─ HDMI 1
│   │   ├─ Visible
│   │   ├─ Rename
│   │   └─ Reorder
│   └─ ...
├─ Compatibility
└─ Display profiles (only if backend available)
```

Display profiles:

```text
Display Profiles
├─ Profiles
└─ Input bindings
    └─ HDMI 1
        ├─ SDR profile
        └─ HDR profile
```

This is clearer than coupling profile availability to visible home shortcuts.

---

# 37. Config model

Future typed configuration can contain:

```text
InputConfig(
    showInputRow = true,
    visibleInputIds = ...,
    inputOrder = ...,
    aliases = ...,
    compatibilityMode = AUTOMATIC,
    lastRequestedInputId = ...
)
```

Optional display:

```text
DisplayProfileConfig(
    profiles = ...,
    bindings = ...
)
```

C6's future `SettingDescriptor` system can expose these cleanly.

---

# 38. Backup/migration behavior

Input data that should be backed up:

```text
visibility
order
aliases
compatibility choice
display profiles
bindings
```

Data that should not be treated as portable truth:

```text
current runtime input
temporary connection state
vendor runtime handles
TIF callback state
```

On restore to a different TV/device:

```text
persisted InputId unavailable
    ↓
keep as unresolved or migrate by fingerprint
    ↓
do not invent a match silently
```

This will matter in C10.

---

# 39. No input-specific permission prompt should be invented unnecessarily

TV-input discovery uses the Android TV Input Framework service.

Do not conflate this with:

```text
READ_TV_LISTINGS
```

which Couchy currently requests for Preview Channels/Watch Next.

C4 and C8 should remain independently capability-gated.

---

# 40. Failure behavior

A launcher must never become unusable because input support fails.

Rules:

```text
TvInputManager absent
    → Inputs feature hidden/unsupported

discovery exception
    → empty/unknown state + retry

input launch fails
    → stay/return launcher + visible error

vendor adapter fails
    → fall through only to safe applicable strategy

display backend fails
    → disable profile automation, not input launching
```

Vendor code must never be required for HOME rendering.

---

# 41. Performance

Input discovery is cheap compared with app artwork or TV-provider program queries.

Do not poll `getTvInputList()` continuously.

Use:

```text
initial snapshot
TvInputManager callbacks
resume safety refresh
bounded recovery
```

For picture state, poll only if a backend actually exists and profile automation is enabled.

This is more efficient than unconditional Accessibility-owned polling.

---

# 42. Testing requirements

## Discovery

- no TvInputManager
- no inputs
- one HDMI
- several HDMI
- partial HDMI numbering
- input added
- input removed
- input updated
- input state changed
- callback missed + resume refresh
- abnormal empty recovery

## Identity

- label rename
- duplicate labels
- platform ID persistence
- firmware/device restore mismatch

## Launch

- standard passthrough TvView
- tune failure
- Back exits to launcher
- TvView callback error
- vendor strategy success
- vendor strategy exception
- fallback chain
- current-state convergence

## Visibility

- hide shortcut without losing available input
- reorder visible shortcuts
- profile picker still sees hidden input

## Remote/startup

- direct HDMI mapping
- previous/next visible input
- missing requested input
- last-requested launch

## Display profiles

- no backend
- backend becomes unavailable
- SDR → HDR
- HDR → SDR
- label-only change
- transient null sample
- deleted profile binding
- failed apply
- repeated same key does not reapply

---

# 43. Recommended C8 implementation order

## C8.0 — core domain only

Add:

```text
InputId
TvInputSource
InputConnectionState
InputFeatureState
```

No UI yet.

## C8.1 — InputRepository

Add:

```text
TvInputManager snapshot
TvInputManager.TvInputCallback
resume refresh
bounded retry
```

## C8.2 — standard input Activity

Add dedicated:

```text
TvInputActivity
TvView
TvView.TvInputCallback
```

with passthrough URI support.

## C8.3 — launch coordinator

Add:

```text
InputLaunchCoordinator
StandardTvInputStrategy
typed LaunchResult
CurrentInputState
```

Route through `LaunchDispatcher → LaunchPolicy`.

## C8.4 — Inputs home row

Add:

```text
InputHomeRow
InputHomeItem
```

to the same home-row model as apps/channels/Now Playing.

## C8.5 — settings

Add:

- visible shortcuts;
- order;
- aliases;
- compatibility mode.

Use C6 stable setting IDs.

## C8.6 — remote/startup integration

C9 can consume:

```text
InputId
InputLaunchCoordinator
lastRequestedInputId
```

rather than implementing input logic itself.

## C8.7 — vendor adapters only as required

Implement target-device-specific strategies after the standard path is tested.

## C8.8 — optional display profiles

Only then add:

```text
DisplayProfileRepository
PictureBackendRegistry
DisplayStateRepository
DisplayProfileCoordinator
```

This prevents advanced vendor functionality from delaying basic HDMI support.

---

# 44. KEEP / ADD / IMPROVE decision table

| Area | Couchy status | Decision |
|---|---|---|
| TV Provider rows | Exists | KEEP separate from inputs |
| TvInputManager discovery | Missing | ADD |
| TvInputManager callback | Missing | ADD |
| resume refresh | Missing | ADD |
| stable input identity | Missing | ADD |
| AvailableInputs | Missing | ADD |
| VisibleInputShortcuts | Missing | ADD separately |
| Input row | Missing | ADD |
| internal TvView Activity | Missing | ADD |
| TvView callback | Missing | ADD for active tune |
| InputLaunchCoordinator | Missing | ADD |
| CurrentInputState | Missing | ADD |
| last requested input | Missing | ADD |
| previous/next input cycling | Missing | ADD using real snapshot |
| HDMI remote mappings | Missing | ADD later |
| vendor adapters | Missing | ADD only where tested |
| compatibility override | Missing | ADD explicit advanced setting |
| display profiles | Missing | OPTIONAL ADD |
| picture backends | Missing | OPTIONAL ADD |
| Accessibility-owned input polling | Missing | DO NOT ADD |
| shortcut/profile coupling | Missing | DO NOT INTRODUCE |
| enum-ordinal source cycling | Missing | DO NOT INTRODUCE |
| transient-null terminating poll | Missing | DO NOT INTRODUCE |

---

# 45. Final target architecture

```text
Platform TvInputManager
        │
        ├─ initial snapshot
        ├─ TvInputCallback
        ├─ resume safety refresh
        └─ bounded recovery
        ↓
InputRepository
        │
        ├─ DiscoveredInputs
        ├─ AvailableInputs
        ├─ VisibleInputShortcuts
        └─ InputConnectionState
        ↓
HomeRowsRepository
        ↓
InputHomeRow
        ↓
LaunchDispatcher
        ↓
LaunchPolicy
        ↓
InputLaunchCoordinator
        ├─ StandardTvInputStrategy
        │      ↓
        │   TvInputActivity / TvView
        │
        └─ optional VendorInputStrategies
        ↓
CurrentInputState
```

Optional display layer:

```text
PictureBackendRegistry
        ↓
DisplayStateRepository
        ↓
DisplayProfileCoordinator
        ↕
DisplayProfileRepository
        ↓
(inputId, SDR/HDR) bindings
```

The core rules are:

```text
TV provider content
    ≠
physical TV input

available input
    ≠
visible shortcut

requested input
    ≠
confirmed physical input

input discovery
    ≠
Accessibility

input launch
    ≠
display-profile automation
```

**C8 status: COMPLETE + REVIEWED.**


---

# SOURCE FILE: `C09.md`

SHA-256: `1234bd850995ccecefa08d8c78316de1da403f439e2f0e898f3b70540141be2c`

---

# Couchy vs Projectivy — C9 HOME, Startup, Remote, Accessibility & Idle Lifecycle

Status: **COMPLETE + REVIEWED**  
Repository: `kanozseyer/Aerial-Rows`  
Pinned commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Projectivy comparison basis: canonical Group 9 post-closure audit v3.

## Executive conclusion

Couchy/Aerial-Rows currently has a deliberately small lifecycle/control architecture:

```text
Android HOME intent
        ↓
MainActivity(singleTask)
        ↓
LauncherApp / Compose
```

with:

- normal HOME activity declaration;
- setup-time default-launcher role guidance;
- Activity Back suppression;
- package-change receiver;
- UI-local MENU handling;
- UI-local AppCard long press;
- move-mode D-pad interception;
- ordinary lifecycle reactions such as ON_RESUME refresh and video-player ON_STOP release.

There is currently **no**:

- BOOT_COMPLETED receiver;
- startup/autostart dispatcher;
- delayed startup target scheduler;
- wake/standby autostart policy;
- AccessibilityService;
- HOME override through Accessibility;
- global programmable remote-gesture engine;
- double-press/long-press global key mapping;
- HDMI hotkey subsystem;
- launcher-owned idle coordinator;
- automatic system-screensaver launch policy;
- vendor startup compatibility adapter.

This simplicity is worth preserving.

Projectivy proves that those capabilities can coexist, but Aerial-Rows should add them as **small independent coordinators** rather than moving their ownership into `MainActivity`, `LauncherScreen`, or Accessibility.

The target is:

```text
HomeRoleCoordinator
StartupCoordinator
RemoteGestureEngine
LauncherIdleCoordinator
AccessibilityBridge (optional)
VendorCompatibilityAdapters (optional)
```

with the existing Compose UI continuing to own only **UI-local navigation**.

---

# 1. Current HOME declaration

The pinned manifest declares `MainActivity` as:

```text
ACTION_MAIN
CATEGORY_HOME
CATEGORY_DEFAULT
```

and also exposes it through:

```text
CATEGORY_LEANBACK_LAUNCHER
CATEGORY_LAUNCHER
```

`MainActivity` uses:

```text
launchMode = singleTask
```

There are no other launcher Activities/services/receivers in the manifest.

This is an excellent minimal HOME foundation.

**KEEP.**

---

# 2. Current Back behavior

`MainActivity.onCreate()` installs an always-enabled:

```text
OnBackPressedCallback
```

whose handler is intentionally a no-op.

The rationale is correct for a HOME activity:

```text
Back at launcher root
    ≠
exit launcher
```

Dialogs/sub-UIs can still consume Back before the Activity-level fallback.

**KEEP the root behavior.**

Long term, route/dialog ownership should remain explicit so Activity-level Back is only the final launcher-root fallback.

---

# 3. Default HOME setup

`SetupWizard` checks the currently resolved HOME package using:

```text
ACTION_MAIN + CATEGORY_HOME
resolveActivity(MATCH_DEFAULT_ONLY)
```

On Android 10+ it prefers:

```text
RoleManager.ROLE_HOME
```

and launches the system role request when available.

Otherwise it falls back to:

```text
Settings.ACTION_HOME_SETTINGS
```

This is a good public-API-first strategy.

**KEEP.**

---

# 4. Google TV setup path

The wizard detects:

```text
com.google.android.feature.AMATI_EXPERIENCE
```

and shows manual ADB guidance including:

```text
cmd package set-home-activity
pm disable-user --user 0 com.google.android.apps.tv.launcherx
pm disable-user --user 0 com.google.android.tvlauncher
pm disable-user --user 0 com.google.android.tungsten.setupwraith
```

The application displays these commands; it does not execute them automatically.

That distinction is important.

## Recommendation

Keep this in an **Expert compatibility** section, not core launcher startup.

Improve it by showing:

- what each command changes;
- which command is optional;
- equivalent rollback/enable commands;
- a "check current HOME" command;
- warning that vendor firmware behavior varies.

Do not silently automate disabling another launcher.

---

# 5. No HomeRoleCoordinator yet

Current HOME logic is split between:

```text
AndroidManifest
SetupWizard
MainActivity
```

There is no reusable domain component for:

```text
isDefaultHome
requestHomeRole
openHomeSettings
restartAsHome
resolveCurrentHome
```

Projectivy's post-closure work showed the usefulness of a dedicated role/task boundary.

## Target

```text
HomeRoleCoordinator
├─ currentHomePackage()
├─ isDefaultHome()
├─ requestRole()
├─ openHomeSettings()
└─ restartAsHome()
```

`SetupWizard` becomes a UI consumer of this coordinator instead of owning role mechanics.

---

# 6. No explicit HOME re-entry policy

`MainActivity` is `singleTask`, but the pinned source does not override:

```text
onNewIntent()
```

and has no explicit "HOME invoked again" event.

Therefore Aerial-Rows currently has no centralized policy for what should happen when the launcher task is re-entered through HOME.

Potential future behavior should be deliberate:

```text
HOME re-entry
    ├─ close transient context menu
    ├─ close settings? configurable
    ├─ exit move mode
    ├─ leave row/card focus intact
    └─ restore normal launcher state
```

Do not reset focus/scroll unnecessarily.

The exact Android delivery path varies with task state, so this should be implemented around observed HOME task entry rather than assumptions.

---

# 7. Current startup model

There is no `BOOT_COMPLETED` permission/receiver in the manifest.

There is no boot receiver in the pinned Kotlin source tree.

Startup relies on Android starting/resuming the configured HOME Activity.

That is normally the correct foundation for a launcher.

**Do not add a boot receiver merely to launch the launcher itself.**

---

# 8. Current startup/autostart behavior: none

There is no persisted startup target such as:

```text
launch app after boot
launch last input after boot
launch selected input after boot
```

and no delayed dispatcher.

Couchy therefore avoids:

- duplicate boot launches;
- timing races;
- arbitrary boot receivers;
- startup target conflicts.

But it also lacks Projectivy's useful autostart functionality.

---

# 9. What Projectivy proved about startup

The corrected Projectivy Group 9 architecture is:

```text
MainActivity early boot trigger
        +
Accessibility early boot trigger
        ↓
shared startup/autostart dispatcher
```

Its manifest boot receiver itself turned out not to be the real coordinator.

Important verified behavior:

- startup requests are deduplicated;
- a boot-time window is enforced;
- the same dispatcher can launch apps or inputs;
- wake/standby re-entry can invoke the same dispatcher with different semantics;
- last-used input is a real startup target;
- special target `-2` reuses app resolution;
- `-101` is an inactive/no-action path.

The architectural lesson is:

```text
many triggers
    ↓
one dispatcher
```

not:

```text
every lifecycle callback launches something independently
```

---

# 10. Recommended StartupCoordinator

Aerial-Rows should implement:

```text
StartupCoordinator
├─ trigger(event)
├─ pendingJob
├─ duplicate suppression
├─ boot-window validation
├─ StartupTarget resolver
└─ LaunchDispatcher
```

Events:

```text
ColdHomeStart
BootHomeStart
Wake
ManualTest
```

Targets:

```text
StartupTarget.None
StartupTarget.App(AppId)
StartupTarget.Input(InputId)
StartupTarget.LastRequestedInput
StartupTarget.LastConfirmedInput
```

Use C3/C8 launch architecture:

```text
StartupCoordinator
    ↓
LaunchDispatcher
    ↓
LaunchPolicy
    ↓
App/Input launch
```

Startup should never bypass parental/policy checks unless there is an explicit documented startup exception.

---

# 11. Startup delay and duplicate policy

Do not blindly copy Projectivy's exact timing constants as magic values.

Expose a clear policy such as:

```text
startupDelay
duplicateWindow
bootValidityWindow
```

with safe defaults.

Important behavior:

1. only one pending startup job;
2. a new equivalent trigger does not schedule another;
3. cancellation is explicit;
4. boot validity is checked **at execution time**, not only scheduling time;
5. a launch failure does not cause an unbounded retry loop.

This preserves the best Projectivy behavior.

---

# 12. Wake/standby behavior

Current Couchy lifecycle reactions are limited:

- root state increments a resume tick on `ON_RESUME`;
- C5's video renderer releases resources on `ON_STOP` and rebuilds on return.

There is no wake/standby launch coordinator.

## Target

Create a small:

```text
PowerStateMonitor
```

which emits typed signals such as:

```text
LauncherResumed
LauncherStopped
ScreenOn
ScreenOff
```

where supported/needed.

Feed those signals into:

```text
StartupCoordinator
LauncherIdleCoordinator
WallpaperEngine
```

Do not make `PowerStateMonitor` launch apps directly.

---

# 13. Accessibility is currently absent

The manifest declares no:

```text
AccessibilityService
BIND_ACCESSIBILITY_SERVICE
```

service.

There is no accessibility-service XML metadata.

There is no AccessibilityService subclass in the pinned source tree.

This means Aerial-Rows currently requires **zero Accessibility access** for normal operation.

That is a strong property.

**Preserve normal operation without Accessibility.**

---

# 14. What Accessibility is useful for

Projectivy proved that optional Accessibility can provide:

- HOME fallback/override behavior on some devices;
- foreground-window/package observation;
- optional key filtering;
- early lifecycle signals.

Those are legitimate optional capabilities.

## Target

```text
AccessibilityBridge
├─ HOME fallback
├─ foreground/window observation
├─ optional key filtering
└─ typed event Flow
```

The rest of the application subscribes to typed events.

Accessibility must **not own**:

- startup policy;
- input state;
- display profiles;
- wallpaper playback;
- idle policy;
- vendor hacks.

---

# 15. Do not copy Projectivy's vendor hacks into Accessibility

The Group 9 v3 re-audit found Projectivy's Accessibility service also participates in device-specific/privileged compatibility behavior, including:

- Xiaomi startup preconditioning;
- periodic Accessibility-node action when `power_simulate_user_activity` is enabled;
- Play Store foreground workaround using `sys.cts.running`;
- Xiaomi updater workaround.

Those are **not generic Accessibility responsibilities**.

Aerial-Rows should isolate anything similar as:

```text
VendorCompatibilityAdapters
```

and only implement a given adapter when target hardware actually needs it.

Core Accessibility should remain clean.

---

# 16. Current global remote behavior

At the `LauncherScreen` root, Couchy handles:

```text
MENU key down
    → open launcher Settings
```

This is a nice fallback when the status bar is hidden.

**KEEP.**

There is no general-purpose programmable remote mapping engine.

---

# 17. Move mode key capture

When app move mode is active, the root Compose key handler captures essentially every key event.

Behavior:

```text
Left/Right/Up/Down
    → move app

select/back key-up
    → exit/drop move mode

other keys
    → swallowed while moving
```

Ending on key-up is deliberate so the release cannot leak into the card underneath and launch it.

This is good UI-local input engineering.

**KEEP this logic local to move mode.**

Do not move it into a global RemoteGestureEngine.

---

# 18. Current AppCard long press

`AppCard` implements its own TV-select long press.

It distinguishes repeated key-down events from a new press.

Important current timing:

```text
repeat gap > 300 ms
    → treat as new press

hold >= 450 ms
    → long press

within 1000 ms after long press
    → suppress normal click
```

This exists to prevent:

```text
hold OK → open menu
release OK → launch app behind menu
```

It is a strong local solution.

**KEEP its semantics.**

Do not force local AppCard context-menu timing to equal global remote-action timing.

---

# 19. Projectivy's remote gesture engine

Projectivy has a broader programmable engine with verified concepts such as:

```text
single press
double press
long press
temporary hotkey mode
direct HDMI/source actions
HOME/MENU special behavior
```

Its observed timing included approximately:

```text
long press ~300 ms
double press ~400 ms
hotkey-mode lease ~3 s
```

These timings describe Projectivy's global mapping semantics, not a requirement for Aerial-Rows.

The useful lesson is the **per-key gesture state machine**.

---

# 20. Recommended RemoteGestureEngine

Use:

```text
RemoteGestureEngine
├─ KeyGestureState per key
├─ Single
├─ Double
├─ Long
├─ consume policy
└─ mapped RemoteAction
```

Example action model:

```text
RemoteAction
├─ OpenSettings
├─ OpenNotifications
├─ LaunchApp(AppId)
├─ LaunchInput(InputId)
├─ PreviousInput
├─ NextInput
├─ ToggleRail
├─ StartIdleMode
└─ None
```

Input launch actions reuse C8.

App launches reuse C3.

Never duplicate the launch implementation inside the remote layer.

---

# 21. Global vs UI-local key handling

This distinction is crucial.

## UI-local

Handled by Compose:

```text
D-pad navigation
card context-menu long press
row rail behavior
move mode
dialog navigation
```

## Global programmable actions

Handled by `RemoteGestureEngine`:

```text
mapped HOME/MENU/vendor key
double press
long press
direct input keys
user-defined shortcuts
```

Do not route ordinary D-pad navigation through a global engine.

That would make focus behavior brittle.

---

# 22. Key-event source

When Aerial-Rows is foreground, `MainActivity`/Compose can observe many key events directly.

For keys that must work outside the launcher or require a broader system context, use the optional `AccessibilityBridge` only when the user enables that capability.

Recommended flow:

```text
Activity key event ───────┐
                          ├→ RemoteGestureEngine
Accessibility key event ──┘
```

The engine performs deduplication/source arbitration so one physical key is never handled twice.

---

# 23. Protected modal gate

Projectivy's Group 9 audit found parental-control key filtering in the Accessibility path.

Aerial-Rows should generalize this cleanly.

When a protected PIN/modal flow is active:

```text
RemoteGestureEngine
    ↓
ProtectedModalGate
    ↓
block global actions which could escape/bypass policy
```

UI navigation inside the modal remains available.

---

# 24. HOME behavior

Current Couchy has no global HOME-key mapping.

When it is the default launcher, Android's HOME role is the primary mechanism.

This should remain the default.

Optional user-facing actions may include:

```text
HOME single
    → return/show Aerial-Rows

HOME double
    → Recents
```

only where the platform/device permits reliable handling and the user explicitly enables the Accessibility/global-key capability.

Do not make global HOME interception a requirement for being a launcher.

---

# 25. MENU behavior

Current:

```text
MENU down
    → Settings
```

This is simple and useful.

If programmable gestures are added later:

```text
MENU single
MENU double
MENU long
```

should be represented through the same `RemoteGestureEngine`.

But keep a safety path so the user can always reach settings, especially if the status bar is hidden.

A misconfigured remote mapping must not lock the user out of settings.

---

# 26. Direct HDMI keys

Projectivy recognizes vendor key codes for direct HDMI inputs.

Aerial-Rows should not hardcode raw HDMI actions directly into `MainActivity`.

Use:

```text
raw key code
    ↓
RemoteKeyMapper
    ↓
RemoteAction.LaunchInput(GenericSource.HDMIx)
    ↓
InputRepository resolve
    ↓
LaunchDispatcher
```

If the input does not exist:

```text
no launch + optional toast
```

This lets C8 remain canonical.

---

# 27. No idle coordinator currently exists

The pinned source has no inactivity timer or launcher-idle state machine.

Couchy's Aerial playback is a wallpaper, not an idle/screensaver coordinator.

There is no code in the traced lifecycle path which says:

```text
no input for N minutes
    → hide UI
    → start screensaver
```

---

# 28. AerialViews helper is not an idle subsystem

`Actions.kt` contains:

```text
AERIAL_PKG = "com.neilturner.aerialviews"
isInstalled()
openAppStore()
```

but no screensaver-start coordinator is present there.

This is best treated as an external-app/install integration helper, not evidence of built-in screensaver control.

---

# 29. Recommended LauncherIdleCoordinator

Aerial-Rows has an opportunity to improve on Projectivy because C5 is already moving toward one shared WallpaperEngine.

Use:

```text
LauncherIdleCoordinator
├─ inactivity timer
├─ last user interaction
├─ launcher visibility
├─ media playing state
├─ modal/settings state
└─ idle mode
```

Possible modes:

```text
IdleMode.None
IdleMode.ImmersiveWallpaper
IdleMode.ExternalScreensaver
```

Default recommendation:

```text
ImmersiveWallpaper
```

which can:

```text
keep the existing WallpaperEngine/video renderer
hide launcher chrome
hide rows/status
wait for user input
restore UI
```

No second ExoPlayer is needed.

---

# 30. Idle wake behavior

Projectivy's launcher idle path restores active launcher UI with a short approximately 300 ms delay.

Aerial-Rows can use the same general principle:

```text
first key / pointer input
    ↓
leave idle mode
    ↓
restore UI/focus
    ↓
consume wake gesture when appropriate
```

Do not allow the wake key to accidentally launch the currently focused app.

The exact delay should be tuned with Compose animation/focus behavior rather than copied as a magic constant.

---

# 31. Media-aware idle policy

C7 provides:

```text
MediaSessionRepository.isAnyPlaying
```

Feed this into `LauncherIdleCoordinator`.

Policy example:

```text
if media playing
    optionally suppress launcher idle
or
    allow visual idle without stopping media
```

This belongs in the idle coordinator, not MediaSessionRepository.

---

# 32. External/system screensaver strategy

Projectivy has a more complex "launch system screensaver" strategy with an external Somnambulator fallback and secure configured-screensaver resolution.

Aerial-Rows does not need that complexity for the first version.

Recommended abstraction:

```text
ScreensaverLauncher
├─ ImmersiveWallpaperStrategy
└─ ExternalAppStrategy
```

A future platform/vendor system-dream strategy can be added if it can be implemented and tested reliably.

Do not couple basic idle mode to privileged secure-settings access.

---

# 33. Startup and idle must remain separate

These are different state machines:

```text
StartupCoordinator
    → what to launch after cold HOME/wake

LauncherIdleCoordinator
    → what to do after user inactivity
```

Do not let "wake from idle" accidentally run the boot autostart target.

Typed trigger events make this distinction explicit.

---

# 34. Wake startup policy

If the user enables "launch startup target after wake", define it separately from cold boot.

Example:

```text
StartupPolicy(
    onColdHomeStart,
    onWake,
    delay,
    target
)
```

Default:

```text
onWake = false
```

because repeatedly launching an app/input every time the TV wakes can be surprising.

---

# 35. Startup target persistence

Use stable identities from prior groups.

Apps:

```text
AppId / ComponentName
```

Inputs:

```text
InputId
```

Do not persist:

```text
list index
row index
label
package-only when component identity is available
```

C3 and C8 already established these identity rules.

---

# 36. Launcher state reset on startup actions

Before launching an autostart target:

```text
close transient app menu
exit move mode
close settings if appropriate
finish idle mode
```

through a centralized launcher UI command/state layer.

Do not let startup launch from behind an open modal and leave stale overlay state waiting when the user returns.

---

# 37. MainActivity should remain small

Recommended future responsibilities:

```text
MainActivity
├─ Android Activity lifecycle
├─ HOME entry signal
├─ Compose host
├─ foreground key event source
└─ system ActivityResult plumbing
```

It should **not** become:

```text
boot dispatcher
input manager
remote mapper
display poller
wallpaper engine
vendor workaround container
```

This is one of Couchy's current strengths.

---

# 38. LauncherScreen should remain UI-focused

Remove long-term platform policy from `LauncherScreen`.

Today it already contains significant non-UI state for:

- package/home config;
- wallpaper orchestration;
- time/date;
- move operations;
- menu/settings state.

C2–C9 increasingly justify extracting coordinators.

For C9 specifically, do not add:

```text
startup timers
global remote maps
Accessibility state
idle timers
```

to this file.

---

# 39. Vendor compatibility boundary

Projectivy v3 verified device-specific behavior such as:

```text
Xiaomi startup preconditioner
Play Store CTS-property workaround
Xiaomi updater workaround
```

Those should be represented, if ever needed, as:

```text
VendorCompatibilityAdapter
```

with:

```text
isSupported(device)
enable()
disable()
```

and clear user-visible advanced settings where appropriate.

They must not silently activate based only on manufacturer strings unless the behavior is proven safe.

---

# 40. Power/user-activity workaround

Projectivy's `power_simulate_user_activity` setting has two distinct effects, including periodic Accessibility-node action behavior.

Do **not** copy that opaque numeric Accessibility action as a generic idle/power solution.

Aerial-Rows should implement normal launcher idle around actual observed user interaction and lifecycle.

If a specific TV later requires a keepalive workaround, isolate it in a tested vendor adapter with a clear name and capability check.

---

# 41. Accessibility enable UX

If/when global-key/HOME fallback features require Accessibility:

Settings should explain:

```text
what feature needs it
what the service observes
what it does not do
how to disable it
```

Recommended setting state:

```text
Accessibility feature
    Off
    Enable…
    Enabled
```

Core launcher functionality, normal HOME replacement, apps, rows, wallpaper, TV provider content, inputs and settings must continue working when it is Off.

---

# 42. Remote gesture settings

Using C6 descriptors:

```text
Remote controls
├─ Enable advanced mappings
├─ MENU
│   ├─ Single
│   ├─ Double
│   └─ Long
├─ HOME
│   ├─ Single
│   ├─ Double
│   └─ Long
└─ Vendor keys
```

For each mapping use a choice dialog:

```text
None
Settings
Notifications
App…
Input…
Previous input
Next input
Idle mode
```

Never expose raw integer action codes.

---

# 43. Gesture timing

Use explicit typed configuration:

```text
RemoteGestureTiming(
    longPressMs,
    doublePressWindowMs,
    repeatSuppressionMs
)
```

but keep defaults internal/advanced unless users genuinely need to change them.

Do not reuse the AppCard's 450 ms local long-press state automatically.

Global key semantics and card context-menu semantics are different interaction surfaces.

---

# 44. Deduplication when Accessibility is enabled

A physical key might be observable through:

```text
Activity
AccessibilityService
```

depending on device/key.

Use an event fingerprint:

```text
deviceId
keyCode
action
eventTime
```

with a tiny dedupe window.

The global engine must never perform one mapped action twice.

---

# 45. Error containment

Every optional coordinator must fail independently.

Rules:

```text
Accessibility unavailable
    → global enhancements off, launcher still works

Startup target invalid
    → remain launcher, show/log bounded error

Remote mapping invalid
    → ignore mapping, Settings remains reachable

Vendor adapter throws
    → disable/fallback adapter

Idle strategy fails
    → restore normal launcher UI

input launch fails
    → remain/return HOME
```

HOME must be the safe state.

---

# 46. Testing matrix

## HOME

- first HOME selection;
- already default;
- role request accepted;
- role request rejected;
- HOME settings fallback;
- repeated HOME while launcher foreground;
- repeated HOME while settings/menu open;
- Back at root;
- Back from dialog.

## Startup

- cold launcher start;
- boot HOME entry;
- duplicate triggers;
- delayed target;
- target removed before execution;
- boot window expires;
- app startup target;
- input startup target;
- launch denied by policy;
- startup disabled.

## Wake

- resume after screen sleep;
- wake policy off;
- wake policy on;
- duplicate wake/resume signals;
- wallpaper renderer recovery.

## Remote

- app card short press;
- app card long press;
- move-mode swallow;
- MENU Settings fallback;
- single/double/long mapped key;
- key repeats;
- Activity + Accessibility duplicate event;
- protected modal active.

## Accessibility

- disabled;
- enabled;
- service killed/reconnected;
- HOME fallback supported/unsupported;
- foreground observation;
- optional key filtering;
- service permission revoked.

## Idle

- no interaction timeout;
- media playing;
- settings open;
- context menu open;
- enter immersive wallpaper;
- wake key consumed;
- focus restored;
- external screensaver failure;
- app goes background while idle.

---

# 47. Recommended C9 implementation order

## C9.0 — preserve normal HOME behavior

Extract:

```text
HomeRoleCoordinator
```

from SetupWizard/MainActivity without changing visible behavior.

Add explicit HOME-entry handling/state cleanup only after testing actual task behavior.

## C9.1 — StartupCoordinator

Add:

```text
StartupTarget
StartupPolicy
pending cancellable job
duplicate suppression
boot validity
```

No Accessibility dependency.

## C9.2 — integrate C8 input startup

Allow:

```text
App
Input
Last input
```

through the shared LaunchDispatcher.

## C9.3 — UI-local key cleanup

Keep:

- AppCard long press;
- move mode;
- rail navigation;
- MENU Settings safety path.

Do not change these while building global gestures.

## C9.4 — RemoteGestureEngine

Add programmable single/double/long actions for foreground-observable keys.

## C9.5 — optional AccessibilityBridge

Add only for features which need it:

- HOME fallback;
- foreground observation;
- global key filtering.

Keep it capability-gated.

## C9.6 — LauncherIdleCoordinator

Start with:

```text
ImmersiveWallpaper
```

using C5's single WallpaperEngine.

Feed C7 media state into idle policy.

## C9.7 — external screensaver strategy

Optionally integrate AerialViews/another external saver after internal idle works reliably.

## C9.8 — vendor compatibility

Only implement adapters proven necessary on target TVs.

---

# 48. Final decision table

| Area | Couchy now | Decision |
|---|---|---|
| HOME manifest Activity | Exists | KEEP |
| singleTask HOME Activity | Exists | KEEP |
| root Back no-op | Exists | KEEP |
| RoleManager HOME setup | Exists | KEEP / EXTRACT |
| HOME settings fallback | Exists | KEEP |
| Google TV ADB guidance | Exists | KEEP as manual expert mode + add rollback guidance |
| HomeRoleCoordinator | Missing | ADD |
| explicit HOME re-entry handling | Missing | ADD carefully |
| boot receiver | Missing | DO NOT ADD just to launch HOME |
| StartupCoordinator | Missing | ADD |
| app autostart | Missing | ADD optional |
| input autostart | Missing | ADD via C8 |
| wake autostart | Missing | ADD optional/off by default |
| AccessibilityService | Missing | OPTIONAL ADD |
| Accessibility required for core | No | KEEP NO |
| MENU opens Settings | Exists | KEEP |
| AppCard long press | Exists | KEEP |
| move-mode key swallowing | Exists | KEEP |
| programmable global remote engine | Missing | ADD |
| double-press mapping | Missing | ADD |
| global long-press mapping | Missing | ADD |
| HDMI remote mapping | Missing | ADD via C8 |
| protected modal key gate | Missing | ADD with parental system |
| launcher idle coordinator | Missing | ADD |
| immersive Aerial idle | Missing | ADD using shared C5 renderer |
| external screensaver launch | Missing | OPTIONAL ADD |
| vendor compatibility hacks | Missing | DO NOT PUT IN CORE |
| Accessibility-owned display polling | Missing | DO NOT INTRODUCE |

---

# 49. Final target architecture

```text
Android HOME role
       ↓
HomeRoleCoordinator
       ↓
MainActivity
       ├─ Compose host
       ├─ HOME-entry signal
       └─ foreground key source
             │
             └────────────┐
                          ↓
                 RemoteGestureEngine
                          ↑
                AccessibilityBridge
                    (optional only)

Power/Lifecycle signals
       ↓
StartupCoordinator
       ↓
LaunchDispatcher
       ↓
LaunchPolicy
       ├─ App launcher
       └─ C8 InputLaunchCoordinator

User interaction
       ↓
LauncherIdleCoordinator
       ├─ C7 media-playing signal
       ├─ C5 WallpaperEngine
       └─ optional ScreensaverLauncher

VendorCompatibilityAdapters
       └─ isolated from every core coordinator
```

The core principles are:

```text
HOME role
    ≠
Accessibility

startup policy
    ≠
boot receiver

UI navigation keys
    ≠
global remote mappings

idle mode
    ≠
startup/wake autostart

Accessibility bridge
    ≠
vendor compatibility

launcher wallpaper renderer
    =
idle immersive renderer
```

Couchy's lightweight lifecycle is therefore a **good base**. Projectivy contributes the missing behavior, but not the ownership/coupling.

**C9 status: COMPLETE + REVIEWED.**


---

# SOURCE FILE: `C10.md`

SHA-256: `e08ee8ae66118f595cba576834cb3b95f70827508729395a4d2a05b31a32be54`

---

# Couchy vs Projectivy — C10 Persistence, Backups, Icons, Shortcuts, Themes & Parental Controls

Status: **COMPLETE + REVIEWED**  
Repository: `kanozseyer/Aerial-Rows`  
Pinned commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Projectivy comparison basis: canonical Group 10 post-closure audit v2.

## Executive conclusion

Couchy/Aerial-Rows has a good **small-launcher persistence primitive**:

```text
LauncherConfig
    ↓
JSON
    ↓
one Preferences DataStore string
```

and a strong **app artwork cache**:

```text
package + lastUpdateTime + render size
    ↓
lossless WebP cache
```

But it currently has no mature subsystem for:

- schema-versioned migrations;
- last-known-good recovery;
- full portable backups;
- theme packages;
- icon packs;
- per-item artwork overrides;
- Android launcher shortcut hosting;
- external pin requests;
- parental controls;
- secure backup/export policy.

The current manual backup is only a JSON dump of `LauncherConfig`, not a complete launcher backup.

The correct target is:

```text
typed Couchy configuration
        +
versioned migrations
        +
explicit backup/theme DTOs
        +
asset packaging
        +
Projectivy-style icon/shortcut capabilities
        +
central LaunchPolicy / optional parental gate
```

without copying Projectivy's weak crypto, magic export flags, or Accessibility coupling.

---

# 1. Current Couchy persistence

Current persistent model:

```kotlin
@Serializable
@Immutable
data class LauncherConfig(...)
```

stored under one Preferences DataStore key:

```text
KEY_CONFIG = "config"
```

as JSON.

The JSON parser uses:

```text
ignoreUnknownKeys = true
```

which is useful for additive forward compatibility.

`ConfigStore.update()` performs an atomic DataStore edit.

This is a much cleaner basic foundation than reproducing Projectivy's very large SharedPreferences descriptor registry.

**KEEP the typed configuration concept.**

---

# 2. Current persistence is not actually typed at the DataStore boundary

Although the Kotlin object is typed, the DataStore itself contains:

```text
String → complete JSON document
```

rather than a typed Proto DataStore.

That is acceptable for the current project size.

Do not change storage format merely for architectural purity.

The bigger needs are:

- schema version;
- migration;
- validation;
- corruption recovery;
- export policy;
- referential integrity.

A later move to Proto can remain optional.

---

# 3. Real first-run localisation bug

There are two different default paths.

## Read path

When no stored config exists:

```text
flow
    ↓
LauncherConfig(categories = defaultCategories(context))
```

which correctly uses localized category strings.

## Update path

When no stored config exists:

```text
update()
    ↓
LauncherConfig()
```

whose defaults are hard-coded:

```text
Streaming
Games
Music
Apps
```

Therefore a fresh non-English installation can:

1. render localized default categories;
2. perform its first config write;
3. persist English category names.

This is a genuine correctness bug.

## Fix

Use one canonical factory:

```text
ConfigDefaults.create(context)
```

for every no-config path.

Never construct persistence defaults by calling `LauncherConfig()` directly when localized/device-aware defaults exist.

---

# 4. Malformed inner JSON can silently destroy configuration

Current read behavior:

```text
decode JSON
    ↓ failure
getOrNull()
    ↓
default config
```

Current update behavior does the same.

That means:

```text
stored JSON malformed
    ↓
launcher silently displays defaults
    ↓
user changes one setting
    ↓
update() starts from defaults
    ↓
damaged original state is permanently overwritten
```

Preferences DataStore's own file may be structurally valid, so this is not necessarily caught as DataStore file corruption.

## Target

```text
ConfigDecoder
    ↓
Success(config)
or
DecodeFailure(rawPayload, reason)
```

On failure:

```text
try last-known-good snapshot
    ↓
if valid
recover
    ↓
else
safe recovery UI / defaults without deleting original
```

Never silently overwrite the only damaged copy.

---

# 5. Add schemaVersion

Current `LauncherConfig` has no schema version.

`ignoreUnknownKeys` and default constructor values handle only the simplest additive evolution.

They cannot safely handle:

- renamed fields;
- split fields;
- merged fields;
- ID-format changes;
- semantic changes;
- wallaper model migration;
- package → component identity;
- channel-key migration;
- future input-ID migration.

Add:

```text
schemaVersion: Int
```

to the persisted envelope.

---

# 6. MigrationRegistry

Target:

```text
MigrationRegistry
    1 → 2
    2 → 3
    3 → 4
    ...
```

Rules:

1. migrations run sequentially;
2. each migration is idempotent where practical;
3. source schema is retained until migrated payload validates;
4. create a safety snapshot before destructive migration;
5. write the new schema only after success;
6. test every historical migration fixture.

This adopts Projectivy's strongest persistence lesson.

---

# 7. Last-known-good configuration

Before committing a validated configuration change which alters schema/structure, maintain:

```text
last-known-good.json
```

or an equivalent secondary DataStore/file.

Use it for:

- migration failure;
- malformed JSON recovery;
- failed bulk restore;
- unexpected normalization failure.

This is more useful than immediately resetting to defaults.

The rollback snapshot should be internal app state, not the same thing as the user's exported backup.

---

# 8. Config normalization is currently too narrow

Current `normalize()` covers only a subset including:

- Aerial source set;
- top-row margin;
- TV-channel limits;
- duplicate home-row IDs.

It does not fully repair:

- zero editable categories;
- category membership references;
- order entries for removed categories;
- dynamic row identities;
- missing app/package references;
- future shortcut references;
- future input/profile references.

## Target

Use:

```text
ConfigNormalizer
```

with subsystem-specific repair functions.

Examples:

```text
normalizeCategories()
normalizeHomeRows()
normalizeTvChannels()
normalizeWallpaper()
normalizeShortcuts()
normalizeInputs()
normalizeDisplayBindings()
```

Unknown references should be handled deliberately, not merely tolerated.

---

# 9. Current manual backup is only a config dump

"Save config" currently serializes:

```text
LauncherConfig
```

as pretty-printed JSON.

No other files are packaged.

This is not a full launcher backup.

It excludes actual external/internal assets and runtime repository data.

---

# 10. Current Save implementation has a reporting/storage bug

Already identified in C6:

```text
Downloads/CouchyBackup.json
```

is written through direct `File(...)` access.

The write is wrapped in `runCatching`, but its result is ignored.

The UI then always displays success.

Therefore:

```text
write fails
    ↓
success toast still shown
```

## Fix

Use Storage Access Framework:

```text
CreateDocument
    ↓
OutputStream
    ↓
verified write
    ↓
success/failure result
```

---

# 11. Custom photo wallpaper is not backed up

The photo picker copies the chosen image to:

```text
filesDir/wallpaper.jpg
```

The backup saves only `LauncherConfig`.

Therefore a backup can contain:

```text
useCustomWallpaper = true
```

without containing:

```text
wallpaper.jpg
```

Restoring that backup on another install/device can produce a configuration which references a photo that does not exist.

This is a concrete backup-integrity bug.

---

# 12. Local video wallpaper is not portable

For local video, config stores:

```text
videoUri = content://...
```

The app requests a persistable URI permission on the original device/provider.

The manual backup then serializes only that URI string.

On another device/install:

```text
same URI string
    ≠
same provider
    ≠
same persisted permission
    ≠
readable media
```

Therefore local-video wallpaper references must be classified as:

```text
DEVICE_LOCAL_REFERENCE
```

unless the media is explicitly bundled into a portable backup.

Given video size, the recommended default is **not** to bundle it automatically.

---

# 13. Backup needs an explicit portable DTO

Do not serialize `LauncherConfig` directly as the public backup format.

Use:

```text
LauncherBackup
├─ formatVersion
├─ appVersion
├─ createdAt
├─ config
├─ appLayoutState
├─ row state
├─ shortcut placements
├─ input settings
├─ optional display profiles
├─ asset manifest
└─ integrity metadata
```

This prevents implementation-only fields from leaking into the public format.

---

# 14. Current backup exports fields it then ignores on restore

Current backup includes every serialized config field, including:

```text
knownApps
setupDone
```

But restore explicitly replaces them with:

```text
knownApps = current knownApps
setupDone = true
```

So the backup file contains values that are deliberately not restored.

That is a sign that the serialized runtime config and backup schema need to become separate types.

## Target export policies

```text
PORTABLE_BACKUP
THEME
LOCAL_RUNTIME
SECURE_LOCAL
NEVER_EXPORT
```

A field/repository can participate in more than one controlled export surface where appropriate.

---

# 15. Full backup vs theme

Projectivy correctly separates:

```text
.plbackup
```

from:

```text
.pltheme
```

Aerial-Rows should preserve the concept but use its own cleaner format.

## Full backup

User state:

- category definitions;
- app memberships/order;
- hidden/excluded items;
- home-row order;
- launcher settings;
- wallpaper selection;
- TV content settings;
- future input aliases/order;
- future display profiles;
- future shortcut placements;
- per-item artwork overrides.

## Theme

Appearance only:

- accent;
- panel style;
- tint/saturation/opacity;
- corner radius;
- icon/card size;
- spacing;
- UI scale where desired;
- scrim;
- category-label visibility;
- wallpaper appearance/source settings;
- optional wallpaper/custom artwork assets.

Theme should normally exclude:

- installed-app membership;
- hidden packages;
- current VPN app;
- Watch Next provider state;
- device input bindings;
- parental/security state;
- runtime caches.

---

# 16. Theme format

Recommended:

```text
.aerialtheme (ZIP)
├─ manifest.json
├─ appearance.json
└─ assets/
    ├─ wallpaper.*
    └─ custom-artwork/
```

Manifest:

```text
formatVersion
name
author?
createdAt
minimumAppVersion?
asset hashes
```

Do not copy Projectivy's numeric built-in-category cutoff.

Use explicit metadata such as:

```text
CategoryOrigin.BUILT_IN
CategoryOrigin.PROVIDER
CategoryOrigin.USER
```

and export policies.

---

# 17. Full portable backup format

Recommended:

```text
.aerialbackup (ZIP)
├─ manifest.json
├─ config.json
├─ state/
│   ├─ rows.json
│   ├─ shortcuts.json
│   ├─ inputs.json
│   └─ display_profiles.json
└─ assets/
    ├─ wallpaper/
    └─ custom_artwork/
```

Do not put regenerable caches inside it.

---

# 18. Transactional restore

Target flow:

```text
select backup
    ↓
copy/read to temporary area
    ↓
verify ZIP/path safety
    ↓
verify manifest/version/hashes
    ↓
decode every payload
    ↓
migrate
    ↓
normalize
    ↓
resolve device-local references
    ↓
prepare assets
    ↓
atomic config/state commit
    ↓
single RuntimeRehydrate event
```

If any stage fails before commit:

```text
current launcher state remains untouched
```

This improves on ad-hoc field-by-field restore.

---

# 19. Replace vs merge

Support explicit policies.

## Full backup

Default:

```text
REPLACE_PORTABLE_STATE
```

with secure/local-only fields preserved.

Optional advanced:

```text
MERGE
```

for categories/layout where semantics are well-defined.

## Theme

Always behaves like a selective merge:

```text
apply theme-exportable fields only
```

It must not wipe unrelated user state.

---

# 20. Platform Android backup is a separate channel

The manifest currently enables:

```text
android:allowBackup="true"
```

There are no explicit backup-policy attributes in the shown application declaration.

Preferences DataStore files are normally part of Android Auto Backup/device transfer by default.

Therefore Aerial-Rows already has **platform backup behavior** in addition to its manual JSON "Save config".

These are not the same system.

Target documentation/architecture must distinguish:

```text
Android platform backup / D2D
        ≠
user-exported .aerialbackup
        ≠
appearance .aerialtheme
        ≠
internal last-known-good snapshot
```

Once sensitive parental/security state exists, use explicit Android backup extraction rules and separate sensitive data from general settings.

---

# 21. Secure state should use a separate store

Do not place future sensitive parental material into the same auto-backed DataStore as normal appearance settings.

Recommended:

```text
user_settings DataStore
    → portable/non-sensitive

secure_state store
    → parental verifier/recovery metadata
    → excluded from cloud/D2D as policy requires
```

Android's backup rules can then include/exclude them independently.

---

# 22. Backup encryption

Projectivy's backup crypto uses an embedded fixed secret, iterative SHA-256 and AES-CBC.

Do **not** copy it.

For Aerial-Rows:

## If backup contains no sensitive secrets

A plain ZIP + hashes is acceptable and transparent.

## If optional encrypted portable backup is added

Use:

```text
user-supplied backup password
    ↓
modern password KDF
    ↓
AES-GCM authenticated encryption
```

Do not use an app-embedded universal secret.

## Internal device-only secret protection

Use Android Keystore where appropriate.

Keystore-bound keys are not a portable-backup mechanism.

---

# 23. Existing app artwork cache is strong

Couchy's current `AppRepository` cache is one of the project's strengths.

Cache identity includes:

```text
package
lastUpdateTime
render variant/size
```

Examples:

```text
<package>-<stamp>-b<width>.webp
<package>-<stamp>-i128.webp
```

This correctly invalidates app artwork when the app updates or requested rendered size changes.

Stale files are removed after scans.

**KEEP this cache.**

---

# 24. Cache is not user data

Do not put:

```text
filesDir/iconcache/*
```

into backup or theme archives.

It is fully regenerable.

On restore/install:

```text
AppRepository.scan()
    ↓
regenerate
```

This keeps backups small and avoids stale cross-device raster assets.

---

# 25. Current launcher UI icons are unrelated to icon packs

`Icons.kt` is a deliberately small built-in vector set used for:

- menus;
- settings;
- status controls;
- launcher UI.

It is not an app icon-pack subsystem.

**KEEP these UI vectors internal.**

External icon packs should affect home items, not settings/status glyphs.

---

# 26. No icon-pack subsystem currently exists

There is no current equivalent of Projectivy's:

```text
ADW ACTION_PICK_ICON
appfilter.xml parser
config_iconpack marker
parsed icon-pack cache
```

and `LauncherConfig` contains no selected icon-pack package.

This can be added behind a new repository without disturbing the existing app scan.

---

# 27. IconRepository

Recommended:

```text
IconRepository
├─ base app artwork
├─ selected icon-pack mapping
├─ per-item override
├─ custom imported artwork
└─ resolved card artwork
```

Resolution policy:

```text
per-item custom image
    ↓
per-item selected icon-pack drawable
    ↓
global icon-pack drawable
    ↓
app Leanback banner
    ↓
app icon
    ↓
generated tile
```

Because TV banners are 16:9 while icon packs are usually square, make behavior configurable rather than automatically replacing every good Leanback banner.

---

# 28. Artwork policy per item

Recommended:

```text
ArtworkPolicy
├─ AUTO
├─ BANNER
├─ ICON_PACK
├─ APP_ICON
└─ CUSTOM
```

Store against stable `HomeItemId`, not list position.

For apps, use the component-aware identity established in C3.

For shortcuts, use the shortcut identity established below.

---

# 29. Icon-pack discovery

Projectivy's ADW-compatible support is a useful compatibility target.

Discover packages responding to:

```text
org.adw.launcher.icons.ACTION_PICK_ICON
```

and parse standard icon-pack mapping resources when available.

Do not make the icon-pack package a compile-time dependency.

If a selected pack is missing:

```text
retain selected package identity
show "not installed"
fall back to normal app artwork
```

so reinstalling the pack can restore the look.

---

# 30. Parsed icon-pack cache

Projectivy's post-closure audit verified cache identity by:

```text
icon-pack package + icon-pack version
```

Aerial-Rows should use:

```text
packageName
+ versionCode
+ our parser schema version
```

Do not reuse the app artwork cache directory/format.

Recommended:

```text
filesDir/iconpack-cache/
```

Parsed mapping caches are regenerable and must not be exported.

---

# 31. Focus colour should use final resolved artwork

Couchy currently derives focus glow from the app's banner/icon at scan time.

Once custom/icon-pack art is supported:

```text
IconRepository resolves artwork
    ↓
ArtworkColorExtractor
    ↓
focus glow/tile color
```

Cache the resulting color by resolved artwork identity.

Otherwise an icon-pack/custom image could visually change while retaining the old app-artwork glow.

---

# 32. No Android launcher shortcut subsystem currently exists

There is no current:

```text
ShortcutRepository
LauncherApps shortcut query
ShortcutInfo home item
shortcut pin/unpin action
external pin-request Activity
```

in the pinned source tree.

This is a clean addition.

---

# 33. ShortcutRepository

Recommended:

```text
ShortcutRepository
├─ hosted shortcuts StateFlow
├─ package-scoped query
├─ global pinned/hosted query
├─ LauncherApps.Callback integration
├─ startShortcut()
├─ pin()
├─ unpin()
└─ external PinItemRequest intake
```

Do not put shortcut logic into `AppRepository`.

C3's package/app callback layer can share platform observation infrastructure where appropriate.

---

# 34. Shortcut identity

Recommended:

```text
ShortcutId(
    packageName,
    shortcutId,
    userIdentity
)
```

For the primary single-user TV case this can render as a simpler stable string, but retain user/profile identity in the domain model.

Do not key shortcuts by:

- title;
- icon;
- list position.

Android specifically expects shortcut IDs to remain stable across backup/restore semantics.

---

# 35. Shortcut home items

Represent shortcut cards as normal home items:

```text
HomeItem.Shortcut
```

They should participate in:

- row membership;
- order;
- hide/exclude;
- custom artwork;
- parental policy;
- backup placement;
- context actions.

They should not be a special non-row overlay.

---

# 36. Starting shortcuts

Use platform `LauncherApps.startShortcut(...)` or the appropriate supported launch path.

Then route through:

```text
LaunchDispatcher
    ↓
LaunchPolicy
    ↓
ShortcutRepository.startShortcut
```

so parental/policy behavior remains consistent.

---

# 37. Pin/unpin semantics

Projectivy can modify the hosted pinned-shortcut ID set itself.

Aerial-Rows should support:

```text
Pin to Aerial-Rows
Unpin from Aerial-Rows
```

where the platform permits it.

Do not expose numeric ShortcutQuery flags in our architecture.

Use internal semantic query modes and platform constants.

---

# 38. External pin requests

If Aerial-Rows becomes a full shortcut-hosting launcher, add a thin Activity for external pin requests.

Responsibilities:

```text
validate PinItemRequest
confirm/accept as appropriate
map to ShortcutId
place in selected/default row
finish
```

Do not create a second shortcut subsystem in the Activity.

It should delegate to `ShortcutRepository`.

---

# 39. Shortcut callbacks

Use `LauncherApps.Callback` to refresh:

- package additions/changes;
- shortcut changes;
- package removal.

This also improves C3's package-invalidation architecture.

One platform observer can feed distinct:

```text
AppRepository
ShortcutRepository
```

without merging their domain models.

---

# 40. Shortcut restore behavior

Backup should store launcher placement/customization of semantic shortcut identities.

But shortcuts themselves are provider-owned and may not exist immediately after restore.

Restore behavior:

```text
shortcut reference restored
    ↓
provider app missing/not republished
    ↓
UnresolvedShortcut placeholder/state
    ↓
resolve automatically when callback publishes it
```

Do not delete the placement immediately.

---

# 41. No parental-control subsystem currently exists

Current source has no:

- PIN/recovery fields;
- protected item set;
- auth Activity;
- LaunchGate;
- parental schedule;
- parental key filter.

This is optional future parity, not required for the core launcher.

---

# 42. Parental policy belongs above every launch source

If added:

```text
LaunchDispatcher
        ↓
LaunchPolicy
        ↓
ParentalPolicy
        ↓
authorization
        ↓
target launcher
```

Protected identities can include:

```text
AppId
ShortcutId
InputId
TvProgram owner/package
Now Playing owner/package
SettingsTarget
```

Now Playing inherits its app/package protection.

---

# 43. Stable protected identity

Do not store protection by visible label or list index.

Recommended:

```text
ProtectedTarget
├─ App(component/package stable ID)
├─ Shortcut(ShortcutId)
├─ Input(InputId)
├─ Package(packageName)
└─ Settings
```

TV-program/Now Playing policies can resolve to owning package where that is more stable than transient content IDs.

---

# 44. Authentication session

Recommended:

```text
ParentalAuthSession
├─ authenticatedAtElapsedRealtime
├─ expiresAfter
└─ scope
```

Prefer process/session monotonic time for temporary authorization.

A process restart can safely expire authorization.

Do not persist an "authenticated until" wall-clock timestamp unless there is a strong UX reason.

---

# 45. Parental verifier storage

Do not store a plaintext PIN.

Do not reproduce Projectivy's deterministic recovery-code transform as a security primitive.

Recommended:

```text
random salt
    +
password/PIN KDF/verifier
```

Store secure verifier/recovery metadata separately from normal config.

For device-local encrypted secrets, use Android Keystore appropriately.

---

# 46. Recovery

Recovery design must be explicit before parental controls ship.

Options include:

- user-created recovery code;
- one-time recovery phrase;
- reset through device-local trusted flow.

Do not invent a hidden universal recovery secret.

Do not export recovery secrets through theme.

Full portable backup should exclude parental credentials by default unless a separately authenticated, encrypted export flow is deliberately designed.

---

# 47. Scheduled parental policy

Projectivy supports scheduled enforcement.

If Aerial-Rows later adds it:

```text
ParentalSchedule
    ↓
ParentalPolicy
```

Keep schedule evaluation separate from authentication UI.

Tests must cover:

- timezone changes;
- midnight crossover;
- DST changes;
- overnight ranges.

---

# 48. Input/key filtering

Projectivy's parental Activity and Accessibility runtime have coupled key-filtering state.

Do not copy that ownership.

Use the C9 structure:

```text
Remote/Input layer
    ↓
ProtectedModalGate / ParentalInputPolicy
    ↓
Auth UI
```

Accessibility, when enabled, supplies events only.

It does not own parental state.

---

# 49. Settings protection

Allow:

```text
ProtectedTarget.Settings
```

so a parental policy can protect launcher settings.

This prevents a protected user from simply disabling the parental system.

The emergency/recovery path must still remain accessible and documented.

---

# 50. Android platform backup and parental state

The current manifest has `allowBackup=true`.

Android's DataStore files are normally included in Auto Backup/D2D unless rules exclude them.

Therefore before adding parental state:

```text
normal settings store
        ≠
secure parental store
```

Add explicit backup extraction rules.

Do not rely on accidental platform behavior for secrets.

---

# 51. Referential integrity after restore

A mature restore must repair references such as:

```text
category → app ID
row order → row ID
shortcut placement → shortcut ID
input binding → InputId
display binding → profile UUID
icon override → asset ID
selected icon pack → package
```

Classify each restored reference as:

```text
Resolved
TemporarilyUnresolved
Invalid
```

Do not silently discard everything unavailable on the current device.

---

# 52. Cache vs portable assets

## Regenerate after restore

```text
app banner/icon cache
icon-pack parsed cache
focus-color cache
TV artwork cache
temporary wallpaper thumbnails
```

## Package in backup when selected by user

```text
custom photo wallpaper
custom app/shortcut artwork
theme-owned wallpaper image
```

## Normally reference only / non-portable

```text
local video content URI
vendor runtime handles
current media session
current TV input connection state
```

This classification should be encoded in repository export policy.

---

# 53. Recommended target architecture

```text
PersistenceRoot
│
├─ ConfigRepository
│   ├─ LauncherConfig
│   ├─ schemaVersion
│   ├─ ConfigNormalizer
│   └─ MigrationRegistry
│
├─ LastKnownGoodStore
│
├─ BackupEngine
│   ├─ BackupManifest
│   ├─ portable DTOs
│   ├─ asset packager
│   ├─ hash verification
│   ├─ replace/merge
│   └─ runtime rehydrate
│
├─ ThemeEngine
│   ├─ ThemeManifest
│   ├─ appearance export policy
│   └─ optional assets
│
├─ IconRepository
│   ├─ app artwork
│   ├─ icon-pack parser/cache
│   └─ per-item overrides
│
├─ ShortcutRepository
│   ├─ queries
│   ├─ callbacks
│   ├─ launch
│   ├─ pin/unpin
│   └─ external pin requests
│
└─ SecureStateRepository
    └─ optional parental credentials/policy
```

Launch:

```text
HomeItem
    ↓
LaunchDispatcher
    ↓
LaunchPolicy
        └─ ParentalPolicy (optional)
    ↓
App / Shortcut / Input / TV content / Media
```

---

# 54. Recommended C10 implementation order

## C10.0 — fix current ConfigStore correctness

Before new features:

1. one canonical localized default factory;
2. stop silent destructive JSON fallback;
3. add last-known-good state;
4. strengthen normalization;
5. fix zero-category invariant from C3.

## C10.1 — schema and migrations

Add:

```text
schemaVersion
MigrationRegistry
migration fixtures/tests
```

Migrate existing Aerial-Rows config into the first explicit schema.

## C10.2 — backup DTO + SAF export

Replace direct `LauncherConfig` dump with:

```text
LauncherBackup
```

and `CreateDocument`.

Keep readable JSON first.

## C10.3 — asset-aware full backup

Move to ZIP package containing:

```text
manifest
config/state
portable assets
```

Add hashes and transactional restore.

## C10.4 — ThemeEngine

Add selective:

```text
.aerialtheme
```

using C6 `ExportPolicy`.

## C10.5 — IconRepository

First extract current app artwork resolution into repository form.

Then add:

- per-item override;
- ADW icon pack;
- package+version parser cache.

## C10.6 — ShortcutRepository

Add:

- LauncherApps queries;
- callback;
- shortcut cards;
- launch;
- pin/unpin;
- external pin requests.

## C10.7 — restore referential integrity

Handle missing:

- apps;
- shortcuts;
- icon packs;
- channels;
- inputs;
- profiles.

## C10.8 — optional parental controls

Only after the central `LaunchPolicy` exists.

Add secure state and policy without Accessibility coupling.

---

# 55. Final decision table

| Area | Couchy now | Decision |
|---|---|---|
| typed LauncherConfig | Exists | KEEP |
| DataStore | Exists | KEEP |
| one JSON blob | Exists | KEEP initially |
| localized default factory consistency | Broken | FIX |
| malformed JSON recovery | Weak | FIX |
| schemaVersion | Missing | ADD |
| sequential migrations | Missing | ADD |
| last-known-good | Missing | ADD |
| normalization | Partial | EXTEND |
| manual JSON export | Exists | REPLACE as full-backup format |
| direct Downloads write | Exists | REMOVE |
| truthful export result | Broken | FIX |
| custom photo in backup | Missing | ADD asset packaging |
| local video portability | Missing | CLASSIFY non-portable/default reference only |
| Android platform backup | Enabled | DEFINE explicit rules |
| full backup DTO | Missing | ADD |
| transactional restore | Missing | ADD |
| theme package | Missing | ADD |
| theme/user-state separation | Missing | ADD |
| app artwork cache | Strong | KEEP |
| app cache invalidation | Strong | KEEP |
| cache in backup | N/A | NEVER |
| icon packs | Missing | ADD optional |
| icon-pack cache | Missing | ADD package+version+schema |
| per-item artwork | Missing | ADD |
| Android shortcuts | Missing | ADD |
| LauncherApps callback | Missing | ADD/shared observation |
| shortcut pin/unpin | Missing | ADD |
| external pin requests | Missing | ADD |
| parental controls | Missing | OPTIONAL ADD |
| parental secrets in main config | Missing | DO NOT INTRODUCE |
| parental Accessibility coupling | Missing | DO NOT INTRODUCE |

---

# 56. C10 closure

The current Couchy/Aerial-Rows subsystem is sufficiently understood.

Current persistence/customization architecture:

```text
LauncherConfig JSON
        ↓
Preferences DataStore

AppRepository
        ↓
app-owned banner/icon WebP cache

SettingsSheet
        ↓
raw JSON save/load
```

There is no hidden:

- ThemeEngine;
- IconPackRepository;
- ShortcutRepository;
- parental-control subsystem.

The desired architecture keeps Couchy's typed configuration and high-performance artwork cache while adding Projectivy's missing lifecycle around persistent state:

```text
typed config
    +
schema migrations
    +
recovery
    +
explicit export policies
    +
asset-aware backup/theme packages
    +
icon/shortcut repositories
    +
optional centralized parental policy
```

The key rules are:

```text
runtime config
    ≠
public backup schema

full backup
    ≠
theme

cache
    ≠
user data

portable asset
    ≠
device-local URI

launcher shortcut placement
    ≠
shortcut provider state

parental state
    ≠
Accessibility state
```

**C10 status: COMPLETE + REVIEWED.**


---

# SOURCE FILE: `C11.md`

SHA-256: `20c18dd880f8ea15ce0ffa92aa08c9c5fcc38bb686897e9eb6b46342e14dc5f1`

---

# Couchy vs Projectivy — C11 Performance & Reliability Architecture

Status: **COMPLETE + REVIEWED**  
Repository: `kanozseyer/Aerial-Rows`  
Pinned commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Comparison basis: Couchy/Aerial-Rows source plus completed Projectivy Groups 1–11/post-closure findings.

## Executive conclusion

Couchy/Aerial-Rows is already **performance-conscious in the places that matter most on weak Android TV hardware**.

Its best current decisions include:

- a very small Android component graph;
- release R8 minification + resource shrinking;
- bounded app-scan concurrency;
- immutable app models;
- reuse of unchanged `AppEntry` instances;
- same-list-instance reuse when a scan produces no changes;
- disk-cached pre-rasterized artwork;
- `prepareToDraw()` before first use;
- stable Lazy-list keys;
- SurfaceView for video wallpaper;
- small Media3 buffers;
- release/rebuild of the video decoder across launcher visibility;
- cheap downsampled wallpaper blur proxy;
- no always-on Accessibility service;
- no unconditional display/input polling;
- no notification/media service yet.

These properties should be treated as **performance invariants** during the Projectivy feature expansion.

However, the current architecture also has several concrete performance/reliability risks:

1. every Gradle `preBuild` rewrites the two largest Kotlin source files and `strings.xml`;
2. fresh process startup waits for the complete app scan + all artwork loading before HOME becomes interactive;
3. every decoded app banner/icon is retained in the process-wide `AppRepository.memoryCache`;
4. the in-memory artwork reuse key does not include the rendered size/density variant;
5. the settings/move architecture can queue full-JSON DataStore transactions during rapid D-pad input;
6. TV-content loading is UI-owned and snapshot-based;
7. the hand-authored baseline profile is very broad and is not backed by a Macrobenchmark generator;
8. no unit/instrumentation/performance test source sets exist;
9. `LauncherScreen.kt` and `SettingsSheet.kt` have become very large ownership surfaces;
10. several subsystems use `runCatching` to suppress failure without structured recovery/diagnostics.

The correct synthesis is:

```text
preserve Couchy's hot-path implementation
        +
adopt Projectivy's subsystem boundaries/callback model
        +
measure every major refactor
        +
activate optional services only on demand
```

The goal is **not** to optimize every allocation prematurely. The goal is to protect proven high-value decisions, fix clear correctness/queueing problems, and add a benchmark harness before feature growth makes regressions difficult to identify.

---

# 1. Release build is already appropriately optimized

Current release:

```text
isMinifyEnabled = true
isShrinkResources = true
proguard-android-optimize.txt
```

Dependencies are relatively focused.

The project also deliberately avoids the multi-megabyte Material Icons Extended dependency by defining only the UI vectors it needs.

**KEEP.**

Do not disable R8/shrinking to make future reflection/vendor integrations easier. Instead add targeted keep rules only for reflection boundaries that actually require them.

---

# 2. Build-time source rewriting is both a correctness and build-performance problem

Current Gradle registers:

```text
applyAerialRowsSourcePatches
```

and makes every:

```text
preBuild
```

depend on it.

The task reads and then unconditionally writes:

```text
SettingsSheet.kt
LauncherScreen.kt
strings.xml
```

even when the desired patches are already present.

The two Kotlin files are also among the largest source files in the project.

Consequences:

- tracked source is mutated by the build;
- file timestamps change on each build;
- Kotlin/Gradle incremental compilation can treat the huge files as modified;
- resource processing can be invalidated by `strings.xml` rewriting;
- IDE/build source can diverge;
- compilation inputs are harder to cache reproducibly;
- patch-anchor failure becomes a build failure;
- performance profiling can accidentally benchmark a patched source tree that differs from the checked-in tree.

## Decision

**C11 priority zero remains Phase 0: remove source rewriting.**

Materialize every Aerial-Rows change as normal source.

This should improve both:

```text
reproducibility
developer build speed
```

before any runtime optimization work.

---

# 3. Baseline profile exists — good, but it is hand-authored and extremely broad

Current `baseline-prof.txt` explicitly states:

```text
hand-authored (no macrobenchmark harness)
```

and marks broad package trees as:

```text
HSP
```

including:

```text
all Couchy code
Compose runtime
Compose UI
Compose foundation
Compose animation
TV Material
graphics-shapes
lifecycle
DataStore
coroutines
serialization
```

Having a baseline profile is directionally good.

But broad wildcard compilation is not equivalent to an observed startup/hot-path profile.

Potential costs:

- unnecessary AOT compilation;
- larger compiled-code footprint;
- profile rules disconnected from actual user paths;
- no automated regression check when architecture changes.

## Target

Keep the current profile temporarily.

Add a dedicated Baseline Profile / Macrobenchmark generator and replace the broad rules with **measured startup + interaction traces**.

Profiles should exercise at least:

```text
cold HOME launch
warm HOME launch
Sliding Rows navigation
row horizontal scroll
settings open/close
app context menu
Aerial/video wallpaper startup
```

The generated profile becomes source-controlled.

---

# 4. CI currently checks compile/shrink/lint but not behavior or performance

Current workflow runs:

```text
assembleDebug
assembleRelease
lintRelease
```

This is valuable because it catches R8/shrinker regressions.

But current `app/src` contains only:

```text
main/
```

There is no:

```text
test/
androidTest/
```

and no Macrobenchmark module.

## Target test pyramid

### Unit tests

Fast pure/domain tests for:

- ConfigNormalizer;
- migrations;
- row ordering;
- move reducers;
- notification policy;
- launch policy;
- remote gesture state machine;
- input mapping;
- backup import/export;
- parental policy;
- wallpaper shuffle/failure registry.

### Android/instrumented tests

For:

- DataStore migration;
- LauncherApps/shortcut adapter boundaries;
- TV provider parsing;
- Activity HOME/task behavior;
- backup file round-trip;
- TvInput Activity on supported test device/emulator where practical.

### Macrobenchmark

Track:

```text
cold startup
warm startup
time to interactive HOME
frame timing during D-pad row navigation
frame timing during vertical row movement
settings open/close
Aerial playing while rows scroll
memory footprint
```

CI does not need to run expensive TV macrobenchmarks on every tiny PR.

A reasonable split:

```text
every PR       → unit + lint + build
device/nightly → macrobenchmark + baseline generation check
release        → full smoke/perf suite
```

---

# 5. Cold-start architecture already does one important thing correctly

`LauncherApp` starts:

```text
DataStore config read
```

and:

```text
AppRepository.scan()
```

concurrently from first composition.

The app scan does not wait for config first.

That is good.

**KEEP concurrent startup work.**

Do not serialize future repositories behind config unless they genuinely require configuration before starting.

---

# 6. Cold process start is currently gated on complete app/artwork scan

After config is available, HOME still waits for:

```text
appsOrNull
```

before rendering the real launcher.

`AppRepository.scan()` completes only after:

- launcher activity enumeration;
- package metadata;
- label loading;
- banner/icon cache read or rasterization;
- WebP cache generation for misses;
- focus-color extraction;
- completion of every app coroutine.

Only then does the full launcher replace `LoadingScreen`.

This produces a visually clean first frame but makes:

```text
time-to-interactive HOME
```

equal to:

```text
slowest complete app-scan path
```

on a cold process.

Warm Activity recreation is much faster because the static memory cache is reused.

## This is a tradeoff, not automatically a bug

Advantages:

- no half-populated home;
- no card artwork popping in;
- no focus target disappearing during initial navigation.

Disadvantages:

- slower post-reboot/process-death HOME;
- one slow package/artwork decode delays everything;
- future icon-pack/shortcut work can make the gate worse if attached incorrectly.

## Rule for future features

**Do not add more repositories to the startup gate.**

Notifications, media sessions, TV content, inputs, icon packs and display profiles must load asynchronously after the minimum usable HOME state exists.

---

# 7. Recommended future cold-start stages

If benchmarks show current cold startup is too slow, move to staged startup.

```text
Stage 0
Activity + config + minimal cached app catalogue

Stage 1
interactive HOME from last-known app snapshot

Stage 2
PackageManager reconciliation

Stage 3
bounded artwork refresh/prefetch

Stage 4
optional dynamic repositories:
TV content / media / inputs / notifications
```

The key is a **persisted app-catalogue snapshot** separate from the bitmap cache.

Possible snapshot:

```text
AppCatalogEntry(
    AppId,
    package/component,
    label,
    packageStamp,
    firstInstall,
    artworkCacheKey,
    autoCategory
)
```

A cold launch can load this tiny metadata index quickly and render cached art/fallbacks while the platform scan verifies it.

Only adopt this if Macrobenchmark shows meaningful benefit. The current all-complete gate is simpler and reliable.

---

# 8. Bounded app scanning is excellent

`AppRepository` uses:

```text
Dispatchers.IO.limitedParallelism(
    max(2, availableProcessors - 1)
)
```

rather than launching heavy image work across unconstrained IO parallelism.

This is specifically designed to prevent icon compression/decode work from starving the TV render thread.

**KEEP bounded concurrency.**

When icon packs/custom artwork are added, do not create a second unbounded decoder pool.

Use a shared artwork work limiter.

---

# 9. Unchanged AppEntry object reuse is excellent

On a rescan, if:

```text
lastUpdateTime
```

is unchanged, the existing immutable `AppEntry` is reused.

Benefits:

- no disk read;
- no bitmap decode;
- no new ImageBitmap;
- same object identity;
- Compose can skip unchanged cards.

Additionally, if the final sorted list is equal to the old cache, `scan()` returns the exact same list instance.

This is one of Couchy's strongest performance mechanisms.

**KEEP the identity-preserving repository contract.**

Future repositories should follow the same principle:

```text
semantic data unchanged
    → preserve object identity where practical
```

---

# 10. Concrete bug — memory reuse key ignores rendered artwork variant

Disk artwork filenames include:

```text
package
package lastUpdateTime
banner width
```

For example conceptually:

```text
pkg-stamp-b720.webp
```

But the in-memory reuse test checks only:

```text
cached.stamp == package.lastUpdateTime
```

`AppEntry` does not record the artwork raster variant.

If Android display density changes while the process remains alive:

1. `bannerW` changes;
2. package stamp does not;
3. old `AppEntry` is reused;
4. old-sized artwork remains in memory;
5. new-size cache name is marked valid;
6. old disk variant may be swept.

This is both a correctness and cache-efficiency issue.

## Fix

Add a cache variant:

```text
ArtworkVariant(
    bannerWidth,
    iconSize,
    cacheSchemaVersion
)
```

and require:

```text
stamp matches
AND
variant matches
```

before reusing an `AppEntry`'s artwork.

This is a **high-confidence fix that does not require benchmarking first**.

---

# 11. Artwork cache trades memory for extremely smooth scrolling

Current `AppEntry` directly contains:

```text
ImageBitmap? banner
ImageBitmap? icon
```

and `AppRepository.memoryCache` holds the complete app list statically for the process lifetime.

Banner width can be as high as:

```text
720 px
```

with `ARGB_8888`.

This is excellent for:

- instant card rendering;
- no disk decode on scroll;
- warm Activity recreation;
- stable Compose identity.

But on an app-heavy device it can produce substantial CPU/GPU image residency.

## Decision

**Do not immediately replace it. Measure first.**

For a typical TV with a moderate app count, the current strategy may outperform a lazy image library.

For low-memory/app-heavy devices, the next evolution should be a custom continuation of Couchy's cache, not necessarily a third-party image stack.

---

# 12. Future memory-bounded artwork architecture

If memory benchmarks justify it:

```text
AppEntry
    → ArtworkRef
```

rather than permanent bitmap ownership.

Then:

```text
ArtworkRepository
├─ byte-bounded memory LRU
├─ current-row pinning
├─ adjacent-row prefetch
├─ existing lossless WebP disk cache
└─ onTrimMemory handling
```

Policy:

```text
focused row
    → keep resident

next/previous row
    → prefetch

far-off rows
    → evictable
```

This retains Couchy's smoothness while placing a hard ceiling on bitmap memory.

Do this after measuring:

```text
20 apps
50 apps
100+ apps
```

on low-memory TV hardware.

---

# 13. Add trim-memory awareness even if the full cache remains

Today there is no explicit repository response to:

```text
onTrimMemory
```

If the full-memory model is retained, add a central memory-pressure policy.

Possible levels:

```text
UI hidden
    → retain catalogue, optionally release expensive transient caches

running low
    → clear regenerable TV/media artwork caches

critical
    → clear non-visible app artwork if architecture supports it
```

Never discard persistent user state.

---

# 14. `prepareToDraw()` is worth preserving

App artwork is uploaded/prepared before first visible use.

That intentionally moves graphics preparation out of the focus/scroll frame.

**KEEP.**

If artwork becomes lazy later, prefetch should still call `prepareToDraw()` before the item enters the viewport.

---

# 15. Pre-rasterizing to the actual display size is good

Couchy avoids loading arbitrarily huge source banners into every card.

It rasterizes banners to a bounded target based on physical display density.

This reduces:

- bitmap memory;
- GPU upload cost;
- sampling cost.

**KEEP target-size decoding/rasterization.**

The variant-key fix above makes it reliable across density changes.

---

# 16. Compose models already use important stability hints

`AppEntry` and `LauncherConfig` are marked immutable.

Rows/cards use stable item keys in the newer Sliding Rows implementation.

The code also deliberately remembers derived collections such as the Dock flat app list to preserve identity across unrelated clock recompositions.

These are all good.

**KEEP stable IDs and immutable domain state as architecture rules.**

---

# 17. Avoid turning Projectivy adapter behavior into Compose mutation

Projectivy often updates RecyclerView adapters incrementally.

Aerial-Rows should not imitate adapter mutation merely for performance.

Preferred Compose pattern:

```text
immutable snapshot
stable item IDs
object reuse for unchanged items
small StateFlow domains
```

This gives predictable recomposition without a second mutable adapter model.

---

# 18. Root LauncherApp has become too broad

`LauncherScreen.kt` is now a very large source file and `LauncherApp` currently owns or coordinates:

- config collection;
- app scanning;
- locale recreation;
- window transparency;
- setup wizard;
- connectivity;
- clock/date;
- new-app assignment;
- menu state;
- move state;
- wallpaper selection;
- Aerial retry;
- layout choice;
- settings;
- intro/loading transitions.

This is still workable today, but adding C7–C10 features directly here would make performance ownership impossible to reason about.

## Target

Move toward:

```text
LauncherRuntime
├─ ConfigRepository
├─ AppRepository
├─ HomeRowsRepository
├─ WallpaperEngine
├─ NetworkStatusRepository
├─ ClockRepository
├─ NotificationRepository
├─ MediaSessionRepository
├─ InputRepository
└─ LauncherUiState
```

Then `LauncherApp` becomes a presentation composition over a few stable UI-state objects.

---

# 19. Do not combine every repository into one giant LauncherUiState

There is a balance.

A single enormous state object:

```text
LauncherUiState(everything)
```

would cause broad invalidation.

Prefer bounded state:

```text
HomeUiState
StatusBarUiState
WallpaperUiState
OverlayUiState
SettingsUiState
```

with each composable collecting only what it needs.

This is especially important once media callbacks, notification callbacks and input callbacks exist.

---

# 20. Whole LauncherConfig propagation will become more expensive as config grows

Today passing the entire `LauncherConfig` is convenient.

As C7–C10 add fields for:

- notifications;
- media;
- inputs;
- remotes;
- backup/theme;
- shortcuts;
- parental policy;

an unrelated setting change can create a new root config object visible to many composables.

Current Compose skipping may mitigate this, but architectural locality is still better.

Future split:

```text
AppearanceConfig
WallpaperConfig
HomeContentConfig
StatusConfig
InputConfig
RemoteConfig
SystemConfig
```

These can remain serialized inside one `LauncherConfig` envelope.

Do not split DataStore into dozens of preference keys just for recomposition.

---

# 21. SlidingRows currently rebuilds several derived collections in composition

Each `SlidingRowsArea` composition rebuilds:

```text
naturalRows
naturalIds
normalizedOrder
rank map
sorted rows
rowIds
```

The lists are usually small, so this is not currently a crisis.

But it is cheap to make the ownership clearer:

```text
remember(tvContent, categorized, config.homeRowOrder) {
    build HomeRowsSnapshot
}
```

Longer term this logic belongs in:

```text
HomeRowsRepository / HomeRowsReducer
```

outside the renderer.

The UI should ideally receive:

```text
List<HomeRow>
```

already ordered and ready to display.

---

# 22. Concrete avoidable allocation — `config.copy()` in rail rendering

The Sliding Rows rail calls `AerialPanel` using a derived:

```text
config.copy(...)
```

to strengthen panel opacity/effect.

That creates another complete `LauncherConfig` object for a purely visual variation.

As `LauncherConfig` grows, this becomes increasingly wasteful and also obscures the true dependency of `AerialPanel`.

## Fix

`AerialPanel` should take:

```text
PanelVisualConfig
```

or explicit stable appearance parameters.

Example:

```text
PanelVisualConfig(
    style,
    opacity,
    tintHue,
    tintSaturation,
    effectStrength
)
```

Then rail can derive a tiny remembered object.

This also improves C6 settings isolation.

---

# 23. Panel brushes are recreated from raw config

`AerialPanel` creates gradient brushes from panel state during composition.

This is acceptable when appearance rarely changes.

But the same style can be shared by many rows.

Potential optimization:

```text
remember(PanelVisualConfig) {
    PanelBrushSet(...)
}
```

and reuse it across row panels.

Do this only after the panel-config extraction.

---

# 24. SmoothCornerShape deserves a small cache/remember improvement

`SmoothCornerShape.createOutline()` constructs:

```text
RoundedPolygon
Path
Compose Path
```

for the measured size/radius.

The implementation correctly guards:

- zero/degenerate size;
- invalid path math.

That reliability guard is good.

However card code often constructs:

```text
SmoothCornerShape(radius)
```

as a fresh object during recomposition.

## Cheap first improvement

```text
val shape = remember(radius) {
    SmoothCornerShape(radius)
}
```

Then optionally let the shape cache its last:

```text
size + density + radius
→ Outline
```

because a card normally keeps the same measured size across many frames.

Do not replace the visual style solely on speculation.

Measure squircle cost with the macrobenchmark before more aggressive changes.

---

# 25. Settings writes can become an IO/serialization queue

C6 established that each `ConfigStore.update()`:

1. reads current JSON;
2. decodes the full config;
3. applies transform;
4. normalizes;
5. serializes the full config;
6. writes DataStore.

Rapid D-pad controls can call this repeatedly.

More importantly, app/row move functions do:

```text
scope.launch {
    store.update { ... }
}
```

for individual movement commands.

Key auto-repeat can enqueue multiple DataStore transactions.

DataStore serializes edits safely, but:

```text
safe serialization
    ≠
low-latency UI reducer
```

## Target

Use an in-memory session/reducer for rapid interactions.

```text
D-pad move
    ↓
local ordered state updates immediately
    ↓
coalesced persistence
```

For move operations, do not blindly debounce and lose commands.

Instead:

```text
apply every command in memory
persist latest resulting snapshot
```

with one writer/coalescer.

This can materially improve move/settings responsiveness.

---

# 26. One serialized config blob is not inherently the problem

Do not solve the above by returning to hundreds of independent SharedPreferences keys.

A full config JSON write is small compared with video/image work.

The problem is **write frequency**, not necessarily blob architecture.

Fix:

```text
session state + coalescing
```

first.

Profile before changing storage technology.

---

# 27. TV content is correctly off the main thread but UI-owned

`TvContentRepository.load()` runs on `Dispatchers.IO`.

Good.

But `SlidingRowsArea` owns the `produceState` that performs the load.

Problems already identified in C4:

- no observer;
- no resume refresh;
- full snapshot load;
- sequential channel/program queries;
- repository state disappears with the UI owner.

## Target

Application/domain-scoped:

```text
TvContentRepository
    ↓
cached StateFlow
    ↓
ContentObserver callbacks
    ↓
bounded reload/coalescing
```

Then home collects already-prepared rows.

This improves both performance and reliability.

---

# 28. Do not aggressively parallelize TvProvider queries

Preview channels can involve several `ContentResolver` queries.

Parallelizing every channel query can:

- overload a vendor provider;
- increase Binder contention;
- worsen low-end storage behavior.

If profiling shows query latency is material:

```text
bounded concurrency = 2–4
```

is safer than unbounded async.

A cached observer-driven repository will likely provide more value than aggressive parallelism.

---

# 29. VideoWallpaper is one of the best current hot paths

Current strong choices:

```text
SurfaceView
```

rather than TextureView;

```text
10s min / 15s max buffer
```

rather than Media3's larger defaults;

```text
ON_STOP
    → player.release()
```

so other foreground apps can use the hardware decoder;

```text
ON_START
    → rebuild
```

when HOME returns;

```text
onRenderedFirstFrame
```

before revealing the clip.

**KEEP all of these unless profiling proves a device-specific problem.**

---

# 30. Video wallpaper still has optimizations from C5 to apply

C5 identified:

- remove trust-all TLS;
- disable audio track at volume zero;
- configurable decoder fallback;
- optional tunneling;
- structured failure registry;
- better stall policy.

From a performance perspective the most obvious win is:

```text
muted wallpaper
    → disable audio track
```

instead of merely:

```text
volume = 0f
```

This avoids useless audio decoding/buffering.

---

# 31. One shared video renderer remains important

Do not add:

- one player for wallpaper;
- one player for Aerial idle mode;
- one player for dynamic background.

Use the shared C5 renderer.

This protects:

- decoder count;
- RAM;
- network connections;
- lifecycle complexity.

Projectivy's post-closure work reinforces this principle.

---

# 32. Network state should not re-enumerate everything on every callback

C7 already identified:

```text
networkStatusFlow
    → allNetworks
    → getNetworkCapabilities for each
```

for every connectivity callback.

Replace with an incrementally maintained capabilities map.

This is a small but clean Binder-work reduction.

---

# 33. Date and time should share one ticker

Current root composition has independent minute loops for:

```text
clock
date
```

Merge them into one `ClockRepository`.

This reduces duplicate coroutines/wakeups and improves timezone/date-change correctness.

The absolute performance win is small, but architecture becomes cleaner before C7 expands the status subsystem.

---

# 34. Optional future subsystems must remain demand-driven

This is a major place where Aerial-Rows can outperform Projectivy.

Do not automatically activate every new subsystem on startup.

## Notifications

Listener service exists only if user grants access; repository should perform only enabled work.

## Media sessions

Maintain active controller callbacks only when a consumer needs them:

- Now Playing row;
- wallpaper media events;
- idle media policy.

## TV inputs

Callback-driven discovery; no continuous input-list polling.

## Display profiles

Poll only if:

- supported backend exists;
- automatic profile switching is enabled.

## Accessibility

Off by default; never required for basic launcher operation.

## Icon packs

Parse selected pack only; don't scan/parse every installed pack on every launch.

## Shortcuts

Observe callbacks but do not fully query every package repeatedly.

This demand-driven rule is one of the most important ways to add Projectivy features without inheriting Projectivy-sized background overhead.

---

# 35. Do not copy Projectivy's Accessibility-owned 4-second display poll

Projectivy's display-profile poll is:

- tied to Accessibility;
- every ~4 seconds;
- capable of terminating after a transient invalid sample.

Aerial-Rows should not introduce this.

Use:

```text
DisplayStateRepository
```

with:

- explicit lifecycle;
- resilient retry;
- capability gating;
- zero work if profile automation is disabled.

---

# 36. Avoid silent `runCatching` as the reliability architecture

Couchy uses `runCatching` extensively.

This is useful at platform edges where HOME must never crash.

But several paths effectively turn failures into:

```text
empty state
null
no-op
```

without preserving why.

Examples across completed C-groups include:

- config JSON decoding;
- backup writing;
- TV content queries;
- Activity launching;
- URI permission persistence.

## Target

Keep failure containment but return structured results.

```text
Result<T>
```

or domain outcomes:

```text
LoadResult.Success
LoadResult.PermissionDenied
LoadResult.ProviderUnavailable
LoadResult.MalformedData
LoadResult.Unsupported
```

Then:

```text
HOME remains stable
+
diagnostics remain possible
+
UI can give truthful feedback
```

---

# 37. Add bounded diagnostics, not verbose production logging

For a launcher, diagnostics should be cheap.

Recommended:

```text
DiagnosticsRepository
    ring buffer of recent important events
```

Examples:

- app scan duration;
- package scan count;
- artwork cache hits/misses;
- TV provider load duration;
- wallpaper load failure;
- input discovery failure;
- backup restore result.

Bound it by count/bytes.

Do not log notification content or private media metadata.

An optional "Export diagnostics" action can help device-specific debugging.

---

# 38. MainActivity should stay small

Projectivy's mature feature set should not tempt us to turn `MainActivity` into a service locator/controller.

Keep it as:

```text
Activity lifecycle
HOME/task signal
Compose host
foreground key source
ActivityResult plumbing
```

Every new expensive subsystem gets its own owner.

This improves both performance and failure containment.

---

# 39. Large source files are now a maintainability/performance-warning signal

Pinned tree sizes show roughly:

```text
LauncherScreen.kt  ~75 KB
SettingsSheet.kt   ~65 KB
```

Large source files do not automatically make runtime slow.

But they correlate here with broad ownership:

- many independent states;
- many unrelated recomposition triggers;
- difficult benchmark attribution;
- expensive incremental compile when the files are touched;
- greater risk that a feature edit invalidates an unrelated hot path.

C2–C10 extraction work will naturally reduce these files.

Do not split files just for line count; split by **state ownership and lifecycle**.

---

# 40. Preserve Couchy's focused render optimizations

Examples worth retaining:

- app bitmaps are rendered at display-appropriate size;
- card image gets its own graphics layer during focus scale;
- expensive artwork colour extraction happens during scan, not composition;
- scrim brushes are top-level constants;
- Dock app list is remembered to preserve identity;
- loading overlay remains until real launcher frames exist;
- pseudo-blur uses a tiny predecoded bitmap rather than runtime blur.

These are more valuable than micro-optimizing trivial Kotlin list operations.

---

# 41. Avoid unnecessary full-screen RenderEffect blur

Couchy's current Dock pseudo-blur:

```text
decode ~96px image
    ↓
GPU upscale
    ↓
dim overlay
```

is extremely cheap and predictable.

Keep this as the default on low-end TVs.

A more accurate blur can be an optional capability/high-quality mode later.

Do not make a real-time full-screen blur mandatory merely to imitate another launcher.

---

# 42. App focus color is correctly precomputed

Artwork glow calculation samples a bounded grid during app scan.

It does not hash/sample the bitmap on every focus.

**KEEP.**

When C10 icon/custom artwork resolution is introduced, recompute/cache the glow only when the resolved artwork identity changes.

---

# 43. Package-change invalidation should become callback-driven and precise

C3 already identified the stale-card bug in the current BroadcastReceiver path.

Performance and correctness point in the same direction:

```text
LauncherApps.Callback
    ↓
affected package/shortcut
    ↓
invalidate only relevant entries
```

rather than broad rescans whenever possible.

Still retain a bounded full reconciliation on:

```text
cold start
resume safety check if required
```

---

# 44. Do not put network/provider IO in composition owners

C4/C7 already identified repository ownership issues.

General rule:

```text
Composable
    should collect state

Composable
    should not own long-lived platform observation/query lifecycle
```

Exceptions can remain for tiny UI-local async work.

For:

- TV provider;
- notifications;
- media sessions;
- inputs;
- icon packs;
- shortcuts;

use repository/controller lifecycles.

---

# 45. Performance state should use StateFlow / immutable snapshots

As new repositories arrive:

```text
StateFlow<Snapshot>
```

is preferable to:

```text
mutable global fields
callbacks directly mutating UI
```

Combine flows at the presentation boundary.

Use:

```text
distinctUntilChanged
conflate/debounce where semantics allow
```

to prevent callback bursts from propagating unnecessary recompositions.

Examples:

- media metadata/state callbacks;
- package/shortcut changes;
- TV provider content changes;
- network capabilities changes;
- display-state samples.

---

# 46. Do not debounce semantic commands which must all execute

Important distinction:

## Good debounce/coalesce

```text
opacity preview persistence
wallpaper palette sampling
TV provider invalidation burst
media metadata callback burst
```

## Do not simply debounce

```text
move left
move right
pin shortcut
unpin shortcut
launch target
```

For command streams:

```text
process every command in reducer
coalesce only persistence/render snapshots
```

This prevents lost user input.

---

# 47. Feature flags/capability gates should remove work, not just hide UI

A hidden feature that still runs in the background is not disabled.

Examples:

```text
Now Playing disabled
    → no media controller observation unless idle/wallpaper still needs it

Display profiles disabled
    → no display poll

Preview channels disabled
    → no channel program queries

Aerial off
    → no Aerial network/player work

Notification feature off/no permission
    → no notification state processing beyond service lifecycle minimum
```

This should become a design review rule.

---

# 48. Proposed performance budgets

Before choosing absolute thresholds, record the current pinned build on target hardware.

Establish baselines for:

```text
cold process HOME
warm HOME
first focusable card
50-card horizontal navigation
10-row vertical navigation
settings open
settings rapid adjustment
Aerial + row scrolling
idle enter/exit
```

Memory:

```text
20 apps
50 apps
100 apps
Aerial on/off
settings open/closed
```

Then CI/release review can use relative budgets such as:

```text
no statistically meaningful regression beyond agreed tolerance
```

instead of inventing one universal millisecond threshold across every TV chipset.

---

# 49. Suggested benchmark devices

At minimum test two classes:

```text
reference/high-end Android TV
low-memory/slow Android TV or representative emulator/device
```

The high-end device catches correctness and smoothness.

The low-end device determines whether an optimization actually matters.

Projectivy-level feature completeness should not be judged only on Shield-class hardware.

---

# 50. Reliability state machines should replace ad-hoc counters

Current patterns such as:

```text
wallpaperVersion++
resumeTick++
rescanTick++
```

are simple and currently understandable.

As features expand they become fragile because the integer says:

```text
something changed
```

rather than:

```text
what happened
```

Use typed events/repositories introduced in earlier C-groups:

```text
WallpaperEvent
PackageEvent
LifecycleEvent
StartupEvent
```

This improves both debugging and performance because subscribers can ignore events irrelevant to them.

---

# 51. Keep animations isolated from data work

Current visual animations mostly use Compose animation state while heavy work occurs off main thread.

Preserve this.

Rules:

```text
no package queries on focus
no image decode on focus
no provider query on row focus
no DataStore decode in card composition
no network request from draw/layout
```

Couchy already mostly follows these rules.

---

# 52. R8/reflection future-proofing

Future Projectivy-style vendor adapters may use reflection for optional system APIs.

Do not add:

```text
-keep class ** { *; }
```

or other broad ProGuard exemptions.

Instead:

- isolate vendor adapter packages;
- add exact keep rules only where needed;
- exercise release builds in CI;
- keep `assembleRelease + lintRelease`.

The current release CI is a strong base.

---

# 53. Projectivy vs Couchy — performance synthesis

## Couchy is stronger at

- modern immutable Compose model;
- artwork pre-rasterization;
- identity reuse;
- weak-TV scan concurrency control;
- SurfaceView wallpaper;
- decoder release when hidden;
- trimmed wallpaper buffer;
- cheap pseudo-blur;
- smaller default component/service footprint;
- no mandatory Accessibility;
- no unconditional vendor/display polling.

## Projectivy is stronger at

- subsystem boundaries;
- long-lived repositories/managers;
- callback/observer-driven invalidation in several areas;
- richer persistent caches;
- mature event routing;
- feature-specific lifecycle management.

## Projectivy weaknesses to avoid

- Accessibility becoming an ownership hub;
- periodic display polling tied to Accessibility;
- snapshot-only TV input discovery;
- device compatibility mixed into generic paths;
- broad privileged helpers in core runtime;
- state duplicated across strategies.

## Desired combination

```text
Couchy hot-path mechanics
        +
Projectivy subsystem maturity
        -
Projectivy coupling/polling debt
```

---

# 54. Performance/reliability invariants for Aerial-Rows

These should become review/CI principles.

### Build

1. Build never mutates tracked source.
2. Release always runs shrink/minify.
3. Baseline profile is generated from measured flows.
4. R8 keep rules stay narrow.

### UI

5. D-pad focus never performs disk/network/package IO.
6. Unchanged HomeItems preserve semantic identity.
7. List items always have stable keys.
8. Artwork decode never occurs on the UI thread.
9. Focus animation does not rerasterize artwork.

### Media

10. Only one wallpaper/idle video renderer exists.
11. Video decoder releases when HOME no longer needs it.
12. Muted wallpaper disables the audio track.
13. Optional dynamic sampling does zero work when disabled.

### Repositories

14. Platform callbacks are preferred over polling.
15. Polling is capability-gated and lifecycle-bounded.
16. Callback bursts are coalesced where semantics permit.
17. Optional feature disabled means background work disabled.
18. A subsystem failure never prevents HOME rendering.

### Persistence

19. Rapid UI input does not enqueue unbounded DataStore work.
20. Cache is never treated as durable user state.
21. Corrupt persisted state is never silently overwritten by defaults.

### Security/reliability

22. No trust-all network transport.
23. Failures are contained but diagnosable.
24. Privileged/vendor adapters are isolated and optional.

---

# 55. Highest-priority concrete fixes before major feature work

## Priority 0 — source-pure build

Remove `applyAerialRowsSourcePatches`.

This improves:

- correctness;
- compile incrementality;
- reproducibility;
- benchmark trustworthiness.

## Priority 1 — create performance baseline

Before large refactors, record current:

- cold/warm launch;
- scrolling/focus;
- memory;
- Aerial + scrolling.

Add Macrobenchmark/baseline-profile infrastructure.

Without this, we cannot prove that the larger architecture remains lightweight.

## Priority 2 — fix AppRepository variant reuse

Add:

```text
ArtworkVariant
```

to memory cache identity.

Low risk, clear correctness benefit.

## Priority 3 — fix persistence queueing

Implement C6 SettingsSession/reducer and move-mode coalesced persistence.

## Priority 4 — repository ownership

Extract:

- TV content;
- clock;
- network;
- wallpaper engine;
- future notifications/media/inputs

out of the root composable.

## Priority 5 — cold-start staging only if measured necessary

Do not complicate startup before measuring.

If required, add persisted app-catalogue snapshot and progressive reconciliation.

## Priority 6 — memory-bounded artwork only if measured necessary

Preserve current smoothness unless memory/PSS demonstrates a real problem.

## Priority 7 — micro-optimizations

After architecture/measurement:

- remember SmoothCornerShape;
- optional outline cache;
- shared PanelVisualConfig/brushes;
- remembered ordered HomeRows snapshot.

These are worthwhile but should not distract from startup, memory and persistence architecture.

---

# 56. Recommended C11 implementation sequence

```text
C11.0
Source-pure build

C11.1
Benchmark + baseline-profile infrastructure

C11.2
App cache variant correctness

C11.3
Settings/move command reducer + persistence coalescing

C11.4
Repository/lifecycle extraction

C11.5
Callback-driven package/TV/network state

C11.6
Demand-driven new Projectivy features

C11.7
Cold-start staging if benchmark requires it

C11.8
Artwork memory budgeting if benchmark requires it

C11.9
Measured shape/panel/render micro-optimizations
```

This order deliberately measures before making invasive performance refactors.

---

# 57. Final C11 decision table

| Area | Current Couchy | Decision |
|---|---|---|
| R8 minification | Strong | KEEP |
| resource shrinking | Strong | KEEP |
| small dependency set | Strong | KEEP |
| build-time source rewriting | Bad | REMOVE FIRST |
| hand-authored baseline profile | Useful but broad | REPLACE with generated measured profile |
| app scan concurrency | Strong | KEEP |
| app object reuse | Strong | KEEP |
| same-list reuse | Strong | KEEP |
| disk artwork cache | Strong | KEEP |
| artwork variant memory key | Incomplete | FIX |
| all artwork resident in RAM | Fast but potentially heavy | MEASURE, then budget if needed |
| cold launch full-scan gate | Clean but potentially slow | MEASURE before redesign |
| stable Lazy keys | Good | KEEP |
| immutable models | Good | KEEP |
| root config breadth | Growing risk | SPLIT presentation dependencies |
| UI-owned provider observation | Weak | MOVE to repositories |
| DataStore per repeat/move | Queue risk | COALESCE |
| SurfaceView wallpaper | Excellent | KEEP |
| small Media3 buffer | Excellent | KEEP |
| player release on stop | Excellent | KEEP |
| audio track while muted | Waste | DISABLE |
| pseudo-blur | Excellent low-end optimization | KEEP |
| connectivity full enumeration | Minor overhead | OPTIMIZE |
| separate clock/date loops | Minor duplication | MERGE |
| silent `runCatching` | Reliability debt | STRUCTURED outcomes |
| tests | Missing | ADD |
| macrobenchmark | Missing | ADD |
| generated profile harness | Missing | ADD |
| optional feature demand gating | Not yet needed | REQUIRE for new subsystems |
| Accessibility always-on | Absent | KEEP absent |
| display polling always-on | Absent | DO NOT INTRODUCE |

---

# 58. C11 closure

Couchy/Aerial-Rows is not a naive launcher that needs wholesale performance reconstruction.

Its current hot paths contain **several unusually thoughtful TV-specific optimizations**.

The main risk is that adding Projectivy's feature breadth directly into the existing root composables would erase those advantages through:

```text
more startup gates
more broad config invalidation
more permanent bitmap state
more polling/services
more DataStore writes
more mixed ownership
```

The target is therefore:

```text
Couchy's existing rendering/cache discipline
        +
small repository boundaries
        +
callback/demand-driven feature activation
        +
measured performance budgets
```

The most important rule coming out of C11 is:

> **Do not optimize Aerial-Rows by making it architecturally heavier before measuring it; make the architecture separable and measurable first, then preserve or improve the hot paths with evidence.**

**C11 status: COMPLETE + REVIEWED.**


---

# SOURCE FILE: `C12.md`

SHA-256: `c0b2ccf993f1333426aae1cd55507b0f31cf8955c1243c4dac13850c22a5a68a`

---

# Couchy ↔ Projectivy — C12 Final Synthesis & Aerial-Rows Implementation Blueprint

Status: **COMPLETE + REVIEWED**  
Repository: `kanozseyer/Aerial-Rows`  
Implementation branch: `aerialrows-development`  
Pinned/live commit verified for C12: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Pinned tree: `457f4ab1d08a8d545bdf5545777cd31f907ae24d`  
Projectivy comparison basis: completed Projectivy Groups 1–12 including post-closure corrections through Group 12 v2.

---

# 1. Executive verdict

After studying both implementations subsystem-by-subsystem, the direction is now clear.

**Aerial-Rows should remain a Couchy-derived Compose launcher.**

Projectivy should be treated as the **behavioral and subsystem-architecture reference**, not as the implementation base.

The winning combination is:

```text
Couchy
    ├─ Compose rendering
    ├─ small MainActivity
    ├─ typed immutable config
    ├─ efficient app scan/artwork cache
    ├─ TV-friendly D-pad UI
    ├─ SurfaceView/Media3 wallpaper implementation
    └─ generally low background overhead

        +

Projectivy
    ├─ mature row/focus semantics
    ├─ launch/policy boundary
    ├─ Android TV provider behavior
    ├─ wallpaper provider/renderer separation
    ├─ settings metadata/update policies
    ├─ media + notification architecture
    ├─ input/startup/remote architecture
    ├─ migrations/backups/themes/icons/shortcuts
    └─ optional device/parental capabilities

        -

Couchy weaknesses
    ├─ build-time source rewriting
    ├─ package-only identities
    ├─ UI-owned persistence/effects
    ├─ snapshot-only TV content
    ├─ trust-all Aerial TLS
    ├─ raw-config backup
    └─ missing lifecycle/domain services

        -

Projectivy weaknesses
    ├─ Leanback-specific implementation
    ├─ Accessibility becoming an ownership hub
    ├─ vendor/root helpers near core runtime
    ├─ polling/cached input weaknesses
    ├─ preference-heavy internals
    ├─ opaque native/bootstrap paths
    └─ inconsistent policy coverage
```

This should **not** become a literal Projectivy clone internally.

It should become:

> A modern Compose implementation of the useful Projectivy behaviors, retaining Couchy's lighter and faster execution model.

---

# 2. Source baseline is still valid

The C1–C11 Couchy comparison was performed against:

```text
aerialrows-development
3c1cfd56234f1d386f4553dd6c69523670500d70
```

C12 reverified that the live branch still points to the same commit.

Therefore:

- none of C1–C11 needs to be repeated because of source drift;
- the source-specific recommendations remain current;
- implementation can begin directly from this branch;
- there is no reason to restart again from Couchy `main`;
- there is no reason to attempt a Projectivy source transplant.

---

# 3. C1–C11 evidence set

C12 synthesizes these completed comparison reports:

```text
C1  Outer architecture / build / lifecycle root
C2  Home rows / focus / rail
C3  Apps / categories / actions
C4  Android TV Channels + Watch Next
C5  Wallpaper + Aerial
C6  Settings
C7  Status / notifications / media
C8  TV inputs / device integration
C9  HOME / startup / remote / Accessibility / idle
C10 Persistence / backups / themes / icons / shortcuts / parental
C11 Performance / reliability
```

Report SHA-256 values:

```text
C1  c85c2188906a849854865fc549e1f62c697899d7a648d05fb3f1fd6954e88cea
C2  2c9732f438cdfc075239ba4c06754821d09846e4d706fbf7dc181dc480e0e381
C3  c756b965caa99ac016eb312a9828be05cfcd6618d666c5b41b1ca333c6761f03
C4  935945ec232fe56e6e64e2112eb19179a3297e6194594234d239b9cde5ebd297
C5  e7603080accb1bdb90cf2672048f647276671649d3cfc61c4712130950bd4b78
C6  0bf49b7fed0fa4fee3abe2c86f133c3f62fb1eba708821c4b2cebf12cba37b18
C7  3947a7627f2fed66747704557066a7c81894f94399f12116370342663c94be3c
C8  1b5d4ac78301cc6a635d9554cdde44ae61e607fc8857044c391779ed18f5019b
C9  1234bd850995ccecefa08d8c78316de1da403f439e2f0e898f3b70540141be2c
C10 e08ee8ae66118f595cba576834cb3b95f70827508729395a4d2a05b31a32be54
C11 20c18dd880f8ea15ce0ffa92aa08c9c5fcc38bb686897e9eb6b46342e14dc5f1
```

---

# 4. Final subsystem decision matrix

## C1 — outer architecture

### KEEP COUCHY

- Kotlin + Compose;
- single small HOME Activity;
- typed immutable `LauncherConfig`;
- DataStore;
- performance-conscious app scanner;
- central low-level `Actions` helper as a platform adapter.

### EXTEND

- explicit repositories/coordinators;
- typed capability layer;
- `LaunchDispatcher`;
- `LaunchPolicy`.

### DO NOT COPY FROM PROJECTIVY

- a large fragment/Leanback shell;
- large Activity/component ownership;
- native bootstrap as a launcher prerequisite.

---

## C2 — home screen

### KEEP COUCHY

- Compose;
- `LazyColumn` / `LazyRow`;
- stable row keys;
- AppCard rendering;
- focus glow/scale;
- D-pad-first navigation;
- hidden-status-bar focus strip;
- existing pivot scrolling technique from Couchy's older layout code.

### EXTEND WITH PROJECTIVY SEMANTICS

- generic `HomeRow`;
- generic `HomeItem`;
- stable `RowId`;
- stable `HomeItemId`;
- per-row last-focused item;
- rail shown/hidden/focused state;
- explicit rail/content focus ownership;
- configurable empty-row action card;
- per-row presentation overrides.

### REPLACE

```text
activeRowId only
first-card restoration
railVisible Boolean
```

with:

```text
HomeFocusState(
    activeRowId,
    activeItemId,
    lastItemByRow
)

HomeRailState
```

---

## C3 — apps/categories/actions

### KEEP COUCHY

- dual Leanback/launcher discovery;
- bounded scan parallelism;
- immutable `AppEntry` reuse;
- artwork WebP cache;
- pre-rasterization;
- focus color extraction;
- multi-section membership;
- category UI;
- long-press release suppression;
- optional best-effort Close action.

### FIX / REPLACE

- package-only app identity;
- `movePkg`;
- global-only Hide semantics;
- Activity-owned package observation;
- zero-category state;
- generated All Apps treated like a normal structural category;
- implicit new-install behavior.

### ADD

```text
AppId(component-aware)
MovingItem(rowId,itemId)
LauncherApps.Callback
AppPlacementPolicy
RowSortMode
NewInstallPolicy
HomeItemActionProvider
LaunchDispatcher
LaunchPolicy
```

---

## C4 — Android TV Channels / Watch Next

### KEEP COUCHY

- Compose presentation;
- immutable TV-content models;
- progress bar;
- per-channel limits;
- Watch Next/Preview visibility;
- row editor integration.

### REPLACE / EXTEND

- `packageName:numericChannelId` identity;
- browsable-only discovery;
- empty-channel omission;
- unspecified program ordering;
- direct provider-intent launch;
- UI-owned snapshot refresh.

### ADD

```text
TvChannelIdentity
ProgramId
TvChannelRepository
PreviewProgramRepository
WatchNextRepository
ContentObserver
resume refresh
provider cap/exclusion
requestChannelBrowsable()
provider-constrained LaunchDispatcher
TvArtworkRepository
```

Important:

```text
TV provider content
    ≠
physical TV input
```

---

## C5 — wallpaper/Aerial

### KEEP COUCHY

- Media3 / ExoPlayer;
- `SurfaceView`;
- player release on `ON_STOP`;
- rebuild on return;
- small buffer policy;
- first-rendered-frame gate;
- fade transition;
- gradient fallback;
- static wallpaper downsampling;
- tiny pseudo-blur;
- one-player philosophy;
- bundled Aerial catalogue;
- multi-source collection concept.

### REMOVE

- trust-all X509 manager;
- always-true hostname verifier;
- legacy runtime `builtinSource`;
- `wallpaperVersion++` as the final event architecture;
- global `aerialError`;
- anonymous URL-only playlist;
- always-enabled decoder fallback;
- ignored persistent-URI failures.

### ADD / EXTEND

```text
WallpaperEngine
WallpaperProvider
WallpaperDescriptor
BuiltinAerialProvider
VideoRenderer
ImageRenderer
WallpaperEvent
WallpaperRotationScheduler
FailureRegistry
shuffle bag
audio-track disable at volume 0
optional tunneling
decoder-fallback policy
optional dynamic palette sampling
```

Core invariant:

```text
one wallpaper/idle video renderer
```

---

## C6 — settings

### KEEP COUCHY

- side-panel Compose UX;
- D-pad interaction;
- typed config;
- DataStore;
- immediate visual previews;
- text rows without focus scaling;
- separate text-entry dialogs;
- full-height right-side panel.

### REPLACE

- every row directly calling `store.update { copy(...) }`;
- first-item-only page focus;
- multiple duplicate setting-row implementations;
- long-lived `SettingsScreen` enum as the final navigation model;
- direct Downloads File backup;
- hard-coded English extras.

### ADD

```text
SettingId
SettingDescriptor<T>
SettingsNavigator
SettingsController
SettingsSessionState
PersistencePolicy
SettingEffect
ExportPolicy
shared setting controls
choice/dropdown dialog
stable route focus restoration
```

---

## C7 — status/media/notifications

### KEEP COUCHY

- current status-bar Compose surface;
- VPN/network/settings interaction;
- hidden-status-bar focus/reveal strip.

### IMPROVE

```text
NetworkStatusRepository
ClockRepository
```

rather than:

```text
allNetworks enumeration on every callback
two separate minute loops
```

### ADD

```text
LauncherNotificationListenerService
NotificationRepository
NotificationPolicy
NotificationPanel
MediaSessionRepository
NowPlayingHomeRow
NowPlayingCard
```

Important:

```text
notification state
    ≠
media-session state
```

and:

```text
Now Playing launches
    → LaunchDispatcher
    → LaunchPolicy
```

---

## C8 — TV inputs/device integration

Couchy currently has effectively no subsystem here.

### ADD

```text
InputRepository
TvInputManager.TvInputCallback
TvInputSource
InputId
AvailableInputs
VisibleInputShortcuts
InputConnectionState
InputHomeRow
TvInputActivity
TvView
InputLaunchCoordinator
CurrentInputState
```

### OPTIONAL

```text
VendorInputStrategy
DisplayProfileRepository
PictureBackendRegistry
DisplayStateRepository
DisplayProfileCoordinator
```

Important invariants:

```text
available input
    ≠
visible shortcut

requested input
    ≠
confirmed physical input

input discovery
    ≠
Accessibility

input launch
    ≠
display-profile automation
```

---

## C9 — HOME/startup/remote/Accessibility/idle

### KEEP COUCHY

- normal Android HOME Activity;
- `singleTask`;
- root Back behavior;
- RoleManager setup;
- small `MainActivity`;
- UI-local AppCard/move/D-pad behavior;
- MENU Settings safety path.

### ADD

```text
HomeRoleCoordinator
StartupCoordinator
RemoteGestureEngine
LauncherIdleCoordinator
PowerStateMonitor
```

### OPTIONAL

```text
AccessibilityBridge
VendorCompatibilityAdapters
ExternalScreensaverStrategy
```

Important:

```text
HOME role
    ≠
Accessibility

startup policy
    ≠
boot receiver

UI keys
    ≠
global remote mappings

idle mode
    ≠
autostart
```

---

## C10 — persistence/backups/themes/icons/shortcuts/parental

### KEEP COUCHY

- typed immutable config;
- DataStore;
- app artwork cache.

### FIX FIRST

- localized default factory mismatch;
- silent JSON corruption fallback;
- zero-category invariant;
- backup success reporting;
- raw runtime config exported as backup;
- missing wallpaper asset packaging.

### ADD

```text
schemaVersion
MigrationRegistry
ConfigNormalizer
LastKnownGoodStore
BackupEngine
ThemeEngine
IconRepository
ShortcutRepository
SecureStateRepository
```

### OPTIONAL

```text
ParentalPolicy
ParentalAuthSession
```

Important:

```text
runtime config
    ≠
public backup schema

backup
    ≠
theme

cache
    ≠
user data

parental state
    ≠
Accessibility state
```

---

## C11 — performance/reliability

### KEEP AS INVARIANTS

- bounded app scan;
- unchanged-object reuse;
- same-list reuse;
- target-size artwork;
- `prepareToDraw()`;
- stable Compose IDs;
- SurfaceView;
- trimmed Media3 buffer;
- decoder release on stop;
- pseudo-blur;
- release R8/shrink;
- low service count.

### FIX

- Gradle source rewriting;
- artwork memory-reuse variant key;
- rapid DataStore command queueing;
- UI-owned platform observation;
- broad hand-authored baseline profile;
- missing tests/benchmarks;
- silent `runCatching` as primary error semantics.

### MEASURE BEFORE CHANGING

- full app-artwork RAM residency;
- cold-start full-scan gate;
- squircle/path cost;
- panel brush allocation.

---

# 5. Definitive high-level architecture

```text
                        Android platform
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
  Package/LauncherApps     TvProvider          TvInputManager
         │                    │                    │
         ▼                    ▼                    ▼
 AppDiscoveryRepo      TvContentRepos        InputRepository
         │                    │                    │
         └─────────────┬──────┴───────┬────────────┘
                       │              │
                       ▼              ▼
                HomeRowsRepository   Runtime state repos
                       │
                       ▼
                    Home UI
                       │
        ┌──────────────┼────────────────┐
        │              │                │
        ▼              ▼                ▼
      Apps         Programs          Inputs
        │              │                │
        └──────────────┴────────────────┘
                       │
                       ▼
                 LaunchDispatcher
                       │
                       ▼
                  LaunchPolicy
                       │
        ┌──────────────┼──────────────────────┐
        ▼              ▼                      ▼
   AppLauncher    InputCoordinator      Other Launchers
```

Parallel runtime services:

```text
WallpaperEngine
NotificationRepository
MediaSessionRepository
ClockRepository
NetworkStatusRepository
StartupCoordinator
RemoteGestureEngine
LauncherIdleCoordinator
```

Optional boundaries:

```text
AccessibilityBridge
VendorCompatibilityRegistry
PictureBackendRegistry
ParentalPolicy
OptionalExpertTools
```

---

# 6. Core dependency rule

The architecture should enforce this dependency direction:

```text
UI
 ↓
feature/domain
 ↓
core
 ↓
platform adapters
```

Optional/vendor code must not point back upward into core ownership.

Preferred conceptual dependency:

```text
core
    does not know Xiaomi/Amlogic/root

feature
    depends on interfaces

integration/vendor
    implements those interfaces
```

This is the most important structural improvement over Projectivy.

---

# 7. Final package structure

Keep one Gradle application module initially.

Do **not** prematurely create many Gradle modules.

Use package boundaries first:

```text
com.conreo.couchytv/

  core/
    model/
      AppId.kt
      HomeItemId.kt
      RowId.kt
      InputId.kt
      ShortcutId.kt

    config/
      LauncherConfig.kt
      ConfigRepository.kt
      ConfigNormalizer.kt
      ConfigDefaults.kt
      ConfigEnvelope.kt

    migration/
      Migration.kt
      MigrationRegistry.kt
      LastKnownGoodStore.kt

    launch/
      LaunchTarget.kt
      LaunchRequest.kt
      LaunchDispatcher.kt
      LaunchPolicy.kt
      AllowAllLaunchPolicy.kt

    capability/
      DeviceCapabilities.kt

    events/
      LauncherEvent.kt

    diagnostics/
      DiagnosticsRepository.kt


  feature/

    home/
      HomeRow.kt
      HomeItem.kt
      HomeRowsRepository.kt
      HomeFocusCoordinator.kt
      HomeRailController.kt
      RowPresentation.kt

    apps/
      AppDiscoveryRepository.kt
      AppEntry.kt
      PackageObserver.kt
      AppClassifier.kt
      AppPlacementPolicy.kt
      AppArtworkCache.kt

    actions/
      HomeItemActionProvider.kt

    tvcontent/
      TvChannelRepository.kt
      PreviewProgramRepository.kt
      WatchNextRepository.kt
      TvArtworkRepository.kt

    wallpaper/
      WallpaperEngine.kt
      WallpaperProvider.kt
      WallpaperDescriptor.kt
      WallpaperRotationScheduler.kt
      FailureRegistry.kt
      renderer/
        ImageRenderer.kt
        VideoRenderer.kt

    aerials/
      BuiltinAerialRepository.kt
      BuiltinAerialProvider.kt
      AerialItem.kt

    settings/
      SettingId.kt
      SettingDescriptor.kt
      SettingsNavigator.kt
      SettingsController.kt
      SettingEffectExecutor.kt

    status/
      ClockRepository.kt
      NetworkStatusRepository.kt

    notifications/
      NotificationRepository.kt
      NotificationPolicy.kt

    media/
      MediaSessionRepository.kt

    inputs/
      InputRepository.kt
      InputLaunchCoordinator.kt
      CurrentInputState.kt

    startup/
      HomeRoleCoordinator.kt
      StartupCoordinator.kt

    remote/
      RemoteGestureEngine.kt

    idle/
      LauncherIdleCoordinator.kt

    backup/
      BackupEngine.kt
      BackupManifest.kt

    themes/
      ThemeEngine.kt

    icons/
      IconRepository.kt
      IconPackParser.kt

    shortcuts/
      ShortcutRepository.kt


  integration/

    android/
      AppLauncher.kt
      TvInputActivity.kt
      NotificationListenerBridge.kt

    vendor/
      VendorCompatibilityRegistry.kt
      xiaomi/
      amlogic/
      mediatek/

    accessibility/
      AccessibilityBridge.kt


  optional/

    parental/
      ParentalPolicy.kt
      SecureStateRepository.kt

    displayprofiles/
      DisplayProfileRepository.kt
      DisplayStateRepository.kt
      DisplayProfileCoordinator.kt

    experttools/
      DiagnosticsScreen.kt
      PackageMaintenance.kt


  ui/
    home/
    settings/
    cards/
    components/
    theme/
```

This is a target structure, not a requirement to create empty files/packages up front.

Create classes only as their phase lands.

---

# 8. Final identity model

Identity mistakes were one of the recurring Couchy weaknesses.

Use explicit semantic IDs.

```text
AppId
    package + component + user/profile

RowId
    stable logical row key

HomeItemId
    typed stable item identity

TvChannelId
    provider package + internal provider identity/fallback

ProgramId
    provider + semantic program key

InputId
    platform TIF ID + optional compatibility metadata

ShortcutId
    package + shortcut ID + user/profile
```

Do not persist:

```text
list index
visible label
row position
array ordinal
random generated ID when platform semantic identity exists
```

---

# 9. Final HomeRow model

```kotlin
sealed interface HomeRow {
    val id: RowId
    val title: String
    val presentation: RowPresentation
    val items: List<HomeItem>
}
```

Possible sources:

```text
Apps
WatchNext
TvChannel
NowPlaying
Inputs
Shortcuts
Folder
ConfigureRow
```

`Inputs` and TV-provider/Live-TV-style content remain conceptually separate.

---

# 10. Final HomeItem model

```text
HomeItem
├─ App
├─ TvProgram
├─ NowPlaying
├─ Input
├─ Shortcut
├─ EmptyRowAction
└─ optional Folder/Custom
```

Every card renderer should work from a semantic domain item, not from a source-specific Android platform object.

---

# 11. Final launch architecture

Every launcher-originated launch converges here:

```text
AppCard
TvProgramCard
NowPlayingCard
InputCard
ShortcutCard
RemoteAction
StartupCoordinator
Notification open
        ↓
LaunchDispatcher
        ↓
LaunchPolicy
        ↓
specific platform launcher
```

Initial policy:

```text
AllowAllLaunchPolicy
```

Later optional:

```text
ParentalPolicy
```

This means parental support does **not** require rewriting every launch surface.

---

# 12. Final configuration architecture

Keep one typed root envelope:

```text
ConfigEnvelope(
    schemaVersion,
    launcherConfig
)
```

As it grows, make fields domain-oriented:

```text
LauncherConfig
├─ appearance
├─ home
├─ apps
├─ wallpaper
├─ tvContent
├─ status
├─ media
├─ inputs
├─ remote
└─ system
```

This can remain serialized as one JSON DataStore value initially.

Do not fragment into 148 preference keys.

---

# 13. Settings and persistence are separate concerns

A setting descriptor does not mean a preference key.

```text
SettingDescriptor
    reads/writes typed Config
```

Separate:

```text
PersistencePolicy
    IMMEDIATE
    DEBOUNCED
    ON_CONFIRM

SettingEffect
    NONE
    RECOMPOSE
    RELOAD_APPS
    RELOAD_TV_CONTENT
    RELOAD_WALLPAPER
    RECREATE_ACTIVITY
    RESTART_HOME_TASK
    REFRESH_ACCESSIBILITY
```

This gives Projectivy-level update behavior while keeping Couchy's typed storage.

---

# 14. Runtime observation architecture

Prefer:

```text
callback
    ↓
repository
    ↓
StateFlow snapshot
    ↓
UI
```

over:

```text
Composable
    ↓
query Android provider/platform
```

Examples:

```text
LauncherApps.Callback
    → AppRepository / ShortcutRepository

ContentObserver
    → TvContent repositories

TvInputManager.TvInputCallback
    → InputRepository

NotificationListenerService
    → NotificationRepository

MediaController callbacks
    → MediaSessionRepository

Connectivity callbacks
    → NetworkStatusRepository
```

Polling is allowed only where no callback exists, and only while the feature actually needs it.

---

# 15. Demand-driven rule

Aerial-Rows should remain lighter than Projectivy by making optional work truly optional.

```text
feature disabled
    → background work disabled
```

Examples:

```text
Now Playing disabled and no idle/wallpaper consumer
    → no active MediaController observation

Display profile automation off
    → no display polling

Preview Channels off
    → no program load

Aerial off
    → no Aerial network/player activity

Accessibility enhancements off
    → Accessibility not required

No icon pack selected
    → no icon-pack parser work
```

This should become a code-review invariant.

---

# 16. Final performance philosophy

Do not redesign proven Couchy hot paths before measurement.

## Preserve immediately

```text
bounded app scanning
immutable object reuse
same-list reuse
target-size artwork
prepareToDraw()
SurfaceView
small video buffer
release decoder on stop
pseudo-blur
R8/shrink
```

## Fix immediately

```text
source rewriting
artwork variant cache identity
trust-all networking
malformed-config destructive fallback
rapid persistence queueing
```

## Measure before changing

```text
full artwork RAM residency
cold-start full-scan gate
SmoothCornerShape cost
panel gradient allocation
```

---

# 17. Final implementation phases

The original Projectivy Group 12 phase order was good.

C1–C11 refine it into the following final sequence.

---

## PHASE 0A — source-pure baseline

**Goal:** make the checked-in repository the actual program.

1. Pin current commit as recovery point.
2. Materialize all Gradle-injected `LauncherScreen.kt` changes.
3. Materialize all injected `SettingsSheet.kt` changes.
4. Materialize all injected strings/resources.
5. Remove `applyAerialRowsSourcePatches`.
6. Build from a clean checkout.
7. Assert build does not change Git working tree.
8. Verify:
   - debug;
   - release;
   - lint.

**Gate:**

```text
git status before build
==
git status after build
```

No feature work before this gate.

---

## PHASE 0B — performance/reliability baseline

**Goal:** know what we are protecting before refactoring.

Add:

```text
unit-test source set
androidTest / benchmark infrastructure
Macrobenchmark / baseline-profile generator
```

Record:

```text
cold process HOME
warm HOME
first focusable card
horizontal row navigation
vertical row navigation
settings open/close
Aerial + navigation
memory with representative app counts
```

Keep current hand-authored baseline profile until generated profile is validated.

**Gate:**

current branch has a recorded performance baseline.

---

## PHASE 1 — config safety + identities + launch core

This phase intentionally comes before home feature expansion.

### Fix current correctness first

- canonical localized `ConfigDefaults`;
- zero-category invariant;
- malformed JSON recovery;
- last-known-good storage;
- `ArtworkVariant` memory-cache identity.

### Add persistence primitives

```text
schemaVersion
MigrationRegistry
ConfigNormalizer
LastKnownGoodStore
```

### Add identities

```text
AppId
RowId
HomeItemId
```

### Add launch core

```text
LaunchTarget
LaunchRequest
LaunchDispatcher
LaunchPolicy
AllowAllLaunchPolicy
```

Route existing app launches through it.

**Gate:**

- existing config migrates safely;
- failed migration cannot destroy previous config;
- normal app launching uses dispatcher/policy;
- visual UI should remain essentially unchanged.

---

## PHASE 2 — generic home model + focus

Promote the current Sliding Rows private types into domain models.

Add:

```text
HomeRow
HomeItem
RowPresentation
HomeRowsRepository
HomeFocusCoordinator
HomeRailController
```

Implement:

```text
activeRowId
activeItemId
lastItemByRow
```

Reuse Couchy's pivot scrolling logic.

Replace:

```text
railVisible Boolean
```

with an explicit rail/focus state.

Fix rail row keys.

Add `EmptyRowAction` for empty user rows.

**Gate:**

- rail reveal/close restores exact card;
- row reorder preserves stable focus;
- empty user row remains configurable;
- no Projectivy feature rows yet.

---

## PHASE 3 — apps/categories/actions

Refactor without destroying Couchy's artwork path.

Add:

```text
component-aware AppId
PackageObserver / LauncherApps.Callback
AppClassifier
AppPlacementPolicy
MovingItem(rowId,itemId)
RowSortMode
NewInstallPolicy
HomeItemActionProvider
```

Fix:

- stale `PACKAGE_CHANGED` case;
- multi-row move bug;
- global-only Hide;
- category referential integrity.

Try removing `QUERY_ALL_PACKAGES` after launcher visibility behavior is proven.

**Gate:**

- no app scan/render regression;
- package changes are precise;
- same app can safely exist in multiple rows;
- every app launch uses LaunchDispatcher.

---

## PHASE 4 — Android TV provider completion

Replace UI-owned snapshot loading with repositories.

Add:

```text
TvChannelIdentity
TvChannelRepository
PreviewProgramRepository
WatchNextRepository
TvArtworkRepository
```

Implement:

- non-browsable channel discovery;
- `requestChannelBrowsable`;
- `internal_provider_id`;
- explicit Preview `weight DESC`;
- Watch Next engagement ordering;
- per-provider cap/exclusion;
- ContentObserver + debounce;
- resume refresh;
- provider-constrained launch policy.

**Gate:**

provider changes appear without launcher restart and logical channel identity survives numeric ID recreation where possible.

---

## PHASE 5 — wallpaper/Aerial engine

First remove insecure networking.

Then extract the current strong player into the engine model.

Add:

```text
WallpaperSelection
WallpaperEngine
WallpaperProvider
BuiltinAerialProvider
AerialItem
WallpaperDescriptor
VideoRenderer
ImageRenderer
WallpaperRotationScheduler
FailureRegistry
WallpaperEvent
```

Preserve:

- SurfaceView;
- small buffer;
- first-frame gate;
- ON_STOP release;
- fades;
- pseudo-blur.

Add:

- audio track disable at volume 0;
- optional tunneling;
- decoder fallback policy;
- shuffle bag;
- failed-item quarantine;
- robust persistent local-video URI handling.

**Gate:**

- one video renderer;
- normal TLS verification;
- Aerial/local video use same renderer;
- provider switch does not create a second player architecture.

---

## PHASE 6 — settings architecture

Materialize the existing UI on:

```text
SettingsNavigator
SettingId
SettingDescriptor
SettingsController
SettingEffectExecutor
```

Unify control components.

Implement:

- stable return focus;
- choice/dropdown dialog;
- debounced session persistence;
- truthful save failures;
- stable conditional-row focus.

Keep immediate Compose previews.

**Gate:**

change a setting → focus remains;
enter child page → back returns to originating setting.

---

## PHASE 7 — notifications + media + status cleanup

Extract:

```text
ClockRepository
NetworkStatusRepository
```

Add:

```text
LauncherNotificationListenerService
NotificationRepository
NotificationPolicy
NotificationPanel
MediaSessionRepository
NowPlayingHomeRow
NowPlayingCard
```

Use platform media-session APIs.

Route Now Playing opens through LaunchPolicy.

Do not couple notification dismissal to playback stop.

**Gate:**

notification access loss gracefully removes notification/media state without destabilizing HOME.

---

## PHASE 8 — physical TV inputs

Add:

```text
InputRepository
TvInputManager.TvInputCallback
TvInputSource
InputId
InputHomeRow
TvInputActivity
InputLaunchCoordinator
CurrentInputState
```

Standard TIF path first.

Separate:

```text
AvailableInputs
VisibleInputShortcuts
```

Vendor strategies only after standard behavior is tested.

Display profiles remain optional.

**Gate:**

standard Android TV / Shield-like devices work without vendor adapters.

---

## PHASE 9 — HOME/startup/remotes/idle

Extract current role handling into:

```text
HomeRoleCoordinator
```

Then add:

```text
StartupCoordinator
RemoteGestureEngine
LauncherIdleCoordinator
PowerStateMonitor
```

Reuse the C5 renderer for immersive idle.

Optional later:

```text
AccessibilityBridge
VendorCompatibilityRegistry
external screensaver strategy
```

Do not add a boot receiver merely to make HOME start.

**Gate:**

ordinary launcher use, input discovery, startup policy and idle operation do not require Accessibility.

---

## PHASE 10 — backup/themes/icons/shortcuts

Add portable user-state architecture.

```text
BackupEngine
ThemeEngine
IconRepository
ShortcutRepository
```

Full backup:

```text
.aerialbackup
```

Theme:

```text
.aerialtheme
```

Keep caches out.

Handle custom assets.

Use transactional restore:

```text
verify
→ decode
→ migrate
→ normalize
→ stage assets
→ commit
→ one runtime rehydrate
```

Add icon packs/per-item artwork.

Add shortcut query/start/pin/unpin/external pin requests.

**Gate:**

backup round-trip restores user state/assets and missing external references become unresolved rather than silently corrupted.

---

## PHASE 11 — optional parity / expert features

Only after the core is stable.

Possible features:

```text
ParentalPolicy
Display profiles
Vendor input adapters
Accessibility global keys/HOME fallback
External wallpaper providers
Diagnostics
Engineering/vendor settings
Package maintenance
```

Explicitly outside core:

```text
Projectivy licensing/protection behavior
opaque libptutils/bootstrap behavior
root/GApps removal
Play Store immutable-APK hacks
vendor CTS-property workarounds by default
Projectivy Web Remote clone
```

**Gate:**

removing the entire optional layer must leave the ordinary launcher functional.

---

# 18. Recommended first implementation wave

The safest first coding wave should be deliberately boring.

Do **not** start with notifications, inputs or Accessibility.

Recommended first sequence:

```text
PR/commit 1
Materialize Gradle-injected launcher/settings source

PR/commit 2
Remove source-rewriting Gradle task + add source-tree cleanliness CI

PR/commit 3
Add unit-test skeleton + benchmark/profile harness

PR/commit 4
ConfigDefaults + malformed JSON recovery + category invariant

PR/commit 5
schemaVersion + LastKnownGoodStore + MigrationRegistry

PR/commit 6
ArtworkVariant cache-identity fix

PR/commit 7
AppId / RowId / HomeItemId

PR/commit 8
LaunchDispatcher + AllowAllLaunchPolicy

PR/commit 9
HomeRow/HomeItem domain promotion

PR/commit 10
HomeFocusCoordinator + exact row/card restore
```

After those ten steps, the project has the safe foundation required for the feature phases.

---

# 19. Architecture invariants

These should become review rules and, where possible, automated tests.

## Build

```text
build never mutates source
```

## Launch

```text
all launcher-originated launches
    → LaunchDispatcher
    → LaunchPolicy
```

## Focus

```text
focus restoration
    → stable IDs
    not list indices
```

## Platform observation

```text
callbacks/observers
    → repository state
    → UI

not
UI → repeated platform queries
```

## Vendor

```text
core feature repositories
    must not depend on vendor/root implementations
```

## Accessibility

Normal:

```text
HOME
apps
TV content
inputs
wallpaper
settings
startup
idle
```

must function without Accessibility.

## Media

```text
notification state
    ≠
MediaSession state
```

## Inputs

```text
available input
    ≠
visible shortcut
```

## Wallpaper

```text
one background video renderer
```

## Persistence

```text
no destructive migration
before verified rollback state exists
```

## Security

```text
no trust-all TLS
```

## Performance

```text
feature disabled
    → its expensive background work disabled
```

---

# 20. Performance gates throughout implementation

Do not wait until Phase 11 to measure.

After these phases rerun key benchmarks:

```text
Phase 0
Phase 2
Phase 4
Phase 5
Phase 7
Phase 9
Phase 10
```

Track at minimum:

```text
cold startup
warm startup
first focusable card
horizontal navigation frame timing
vertical navigation frame timing
PSS/RSS
Aerial playback + navigation
settings latency
```

If a phase creates a meaningful regression, fix it before continuing.

That prevents Projectivy-level feature breadth from slowly turning Aerial-Rows into a heavy launcher.

---

# 21. What we should deliberately preserve from Couchy

These are not temporary implementation details.

They are desirable product characteristics.

```text
small HOME Activity
Compose-first UI
low component count
no Accessibility requirement
bounded background work
very fast app card rendering
minimal dependency graph
one video renderer
responsive D-pad UX
clean TV-focused styling
```

The Projectivy feature set should be added **without sacrificing these**.

---

# 22. What we should deliberately borrow from Projectivy

The valuable Projectivy content is mostly behavioral architecture:

```text
stable row/item state
reliable focus restoration
rich card/action model
TV Provider semantics
wallpaper providers/events
settings update semantics
notification/media repositories
input launch abstraction
startup dispatch
remote gesture state machine
backup/theme separation
icon packs
shortcuts
optional parental/display/vendor capabilities
```

These are the parts worth reproducing independently.

---

# 23. What we should not reproduce from Projectivy

Do not make these reconstruction goals:

```text
Leanback fragment implementation
obfuscated class topology
native PTApplication bootstrap
licensing/premium implementation
root package maintenance
GApps removal
Google Play immutable-APK blocker
vendor Play Store CTS-property workaround
Xiaomi updater workaround by default
Accessibility-owned display polling
opaque privileged shell architecture
Projectivy Web Remote
```

Those are not required to achieve the launcher UX/functionality target.

---

# 24. What we should not retain from Couchy long term

```text
build-time Kotlin source rewriting
package-only app identity
movePkg
global-only app hide
browsable-only TV channel discovery
UI-owned TV content lifecycle
trust-all Aerial networking
legacy builtinSource runtime coupling
wallpaperVersion/aerialError orchestration
direct settings-to-DataStore wiring everywhere
multiple settings control families
raw LauncherConfig backup
silent malformed-config reset
no schema migrations
no tests/benchmarks
```

---

# 25. Final product architecture statement

The resulting Aerial-Rows should feel like:

```text
Projectivy-level launcher capabilities
        +
Couchy/Aerial visual identity
        +
modern Compose implementation
        +
better platform callbacks
        +
cleaner optional/vendor boundaries
        +
stronger performance discipline
```

not:

```text
Projectivy decompiled into Compose
```

and not:

```text
Couchy with dozens of features bolted into LauncherScreen.kt
```

---

# 26. Final scope tiers

## Tier A — core target

These should form the main Aerial-Rows product:

- source-pure build;
- stable config/migrations;
- generic home row/item model;
- reliable focus/rail;
- installed apps/categories/actions;
- Android TV Channels + Watch Next;
- Aerial/wallpaper engine;
- settings;
- status/network/clock;
- notifications + Now Playing;
- standard TV inputs;
- HOME/startup/remotes/idle;
- full backup;
- themes;
- icon/custom artwork support;
- shortcuts.

## Tier B — optional advanced parity

- parental controls;
- display profiles;
- vendor input adapters;
- optional Accessibility global mappings;
- external wallpaper providers;
- diagnostics.

## Tier C — not core

- root/package maintenance;
- privileged Google/Xiaomi hacks;
- GApps uninstall;
- licensing/protection cloning;
- opaque native bootstrap;
- Projectivy Web Remote clone.

---

# 27. Final answer to the Couchy-vs-Projectivy question

The study now gives a definitive answer:

### Couchy is the better **base implementation**.

It is:

- smaller;
- more modern;
- more Compose-native;
- more performance-conscious in app artwork and video;
- easier to maintain;
- less coupled to services/vendor behavior.

### Projectivy is the better **feature and behavioral reference**.

It has already solved:

- complex home state;
- launcher actions;
- TV-provider behavior;
- media/notifications;
- physical inputs;
- startup/remotes;
- backups/themes/icons/shortcuts;
- optional expert capabilities.

### Aerial-Rows should combine them selectively.

The guiding rule should be:

> **When Couchy already performs the job well, keep its implementation. When Couchy is missing a capability, reproduce the verified Projectivy behavior behind a clean Compose-era interface. When Projectivy has known coupling/reliability weaknesses, implement the behavior better rather than copying the weakness.**

---

# 28. C12 closure

All eleven Couchy comparison groups have now been integrated.

The source baseline is still current.

The architecture, subsystem ownership, identities, performance rules, optional boundaries and implementation order are sufficiently defined to begin actual repository changes without another discovery phase.

The next step should no longer be another research group.

It should be:

```text
PHASE 0A
materialize the source patches
and remove applyAerialRowsSourcePatches
```

followed immediately by:

```text
PHASE 0B
benchmark/test baseline
```

before structural refactoring begins.

**C12 status: COMPLETE + REVIEWED.**

---

# SECTION F — Projectivy G1–G12 + Post-Closure Complete Research
# Projectivy Reverse-Engineering Reports Groups 1–12 — Full Archive
This file concatenates the complete source reports from the research archive. Each section preserves the original report text.


---

# SOURCE FILE: `group01.md`

SHA-256: `8776c66d67db00866b28c25c7b43b48daf4076c174709df88bd7f0a1c3df3f7d`

---

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


---

# SOURCE FILE: `group01_postclosure_audit.md`

SHA-256: `9fc4b5dea909163b704bb88c678059fdb88807062cc00fd64f32446e43e08770`

---

# Projectivy 4.72 — Group 01 post-closure audit

Status: **AUDITED AFTER GROUPS 2–12**
Original report: `group01.md`
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## 1. Audit conclusion

The original Group 01 report remains structurally correct. Its key counts, package identity, component boundaries, resource architecture, minification/ownership warning, HOME entry point, TV/provider/accessibility/notification/plugin boundaries and native-library inventory all survive the later implementation-level work.

However, later groups allow several early statements to be corrected or strengthened. The most important changes are:

1. `StartUpBootReceiver` is manifest-declared for `BOOT_COMPLETED`, but its 4.72 `onReceive()` is effectively a no-op. Actual autostart is coordinated elsewhere.
2. `ThemeContentProvider` should be described as a read-only theme-wallpaper content bridge for ordinary consumers, not merely as a provider that happens to override CRUD methods.
3. Declaring `WRITE_EPG_DATA` does not prove Projectivy writes TV-provider rows. Group 04 verified read/query behavior plus `requestChannelBrowsable`; no row-insertion architecture needs to be inferred from the manifest permission alone.
4. The initially-disabled HDMI/AV/source launcher components are now verified to be dynamically enabled/disabled with `PackageManager.setComponentEnabledSetting(...)`; this is no longer only an inference from the manifest.
5. The native `libptutils.so` boundary is now sufficiently closed: it is narrow initialization/validation/protection/utility code, not a hidden native implementation of the launcher core.

No Group 01 assumption was found that invalidates the Groups 2–12 architecture.

---

## 2. Corrections to apply to the original Group 01 report

### 2.1 Boot receiver wording

Original static fact:

```text
StartUpBootReceiver — BOOT_COMPLETED
```

Correct post-Group-09 interpretation:

```text
BOOT_COMPLETED manifest entry
        ↓
StartUpBootReceiver.onReceive()
        ↓
no launch/service dispatch in Projectivy 4.72
```

Actual startup/autostart dispatch is handled through the `dq3` coordinator and can be triggered from the HOME/accessibility lifecycle with duplicate and boot-window guards.

Therefore Group 01 should explicitly distinguish **manifest registration** from **runtime responsibility**.

### 2.2 ThemeContentProvider semantics

Group 01 correctly identified:

```text
com.spocky.projengmenu.themeprovider
```

and noted that the class overrides the standard ContentProvider methods.

Group 06 later established the operational role more precisely:

```text
.pltheme/current theme state
        ↓
ThemeContentProvider
        ↓
content://com.spocky.projengmenu.themeprovider/current/wallpaper/<name>
        ↓
normal wallpaper renderer
```

The provider is effectively **read-only to ordinary consumers** and exists to expose packaged theme-wallpaper media as normal Android content URIs. The mere presence of `insert/update/delete` overrides must not be read as evidence of a mutable public theme database.

### 2.3 TV-provider permission wording

The manifest declares:

```text
READ_TV_LISTINGS
READ_EPG_DATA
WRITE_EPG_DATA
```

Group 04 verified Projectivy's actual launcher path as:

```text
query channels
query preview programs
query Watch Next
requestChannelBrowsable(...)
```

No hidden requirement to create/update/delete TV-provider program rows was discovered.

The improved Group 01 statement should therefore be:

> Projectivy declares both read and write EPG capabilities, but the reconstructed home/channel architecture is read-oriented. Do not infer active EPG mutation solely from the manifest permission declaration.

### 2.4 Dynamic source shortcut components

Group 01 said the initially-disabled HDMI/AV/source activities “strongly support” dynamic enablement.

Group 08 proved it:

```text
internal source/action model
        ↓
PackageManager.setComponentEnabledSetting(...)
        ↓
HDMI1–4 / AV / Source Popup launcher components
```

This should now be marked **verified behavior**.

### 2.5 Native-library interpretation

Group 01 correctly avoided assigning unsupported semantics to `libptutils.so`.

Group 11 later closed the boundary further:

```text
PTApplication.vs(boolean)
      ↑
PTApplication.f()
      ↑
small managed caller set
```

with native exports including:

```text
Java_com_spocky_projengmenu_PTApplication_vs
Java_com_spocky_projengmenu_PTApplication_vc
```

The arm64 `vc` path is a very small global-byte-style accessor while `vs` is the larger JNI path. No evidence places home rows, app discovery, TV Provider, media sessions, wallpapers, settings or TV-input logic primarily inside the native library.

The revised architectural classification is:

> **narrow initialization / validation / protection / utility boundary; not required for Aerial-Rows feature reconstruction.**

---

## 3. Named component details that Group 01 did not spell out

The original report deliberately grouped many activities into families. That was efficient, but a static architecture baseline is stronger if the named manifest components are explicitly accounted for.

### 3.1 Category/settings wrappers

Later closure work identified:

- `CategoryContentSidePanelActivity`
- `ManageCategoriesActivity`
- `ManageChannelsActivity`

These are thin entry-point wrappers into the already-mapped category/channel settings system, not separate engines.

### 3.2 Internal launch/policy activities

Group 01 did not explicitly call out:

- `SettingsIntentActivity`
- `ToggleParentalControlActivity`
- `ParentalControlCheckActivity`

Later work shows these belong to the generic launcher-action / parental-policy boundary and are useful manifest anchors for the eventual centralized `LaunchGate` design.

### 3.3 Onboarding and reusable picker activities

The manifest also declares:

- `OnboardingActivity`
- `ColorPickerActivity`
- `ImagePickerActivity`

These are worth recording in Group 01 because they are distinct UI infrastructure surfaces rather than incidental resources.

### 3.4 Vendor wrappers now explicitly classified

The original “device/engineering activity families” section can now be made concrete:

- `SettingsMiTvActivity` → Xiaomi settings
- `SettingsFengOsActivity` → Formovie/FengOS settings
- `SettingsAmlogicActivity` → Droidlogic/Amlogic settings
- `SettingsMediatekActivity` → MediaTek factory TV settings
- `SettingsAndroidActivity` → standard Android Settings
- `EngMenuFengmiActivity` / `EngMenuFengOsActivity` → vendor engineering/test screens
- `MediaExplorerActivity` → Xiaomi/FengOS media explorer
- `GoogleSmartHomeActivity` → supported Google TV Smart Home target
- `ProfilePickerActivity` → supported profile-picker target
- `AccessibilityServiceActivity` → configuration/status UI for the existing accessibility service

None constitutes a second hidden device-integration architecture.

### 3.5 Optional maintenance utilities now classified

Group 11 resolved several activities that Group 01 only grouped broadly:

- `InstallAppsActivity` → guided APK installer
- `CleanupActivity` → privileged app freeze/unfreeze utility
- `RemoveLauncherActivity` → privileged stock-launcher freeze/unfreeze tool
- `BlockGooglePlaySelfUpdateActivity` → privileged Play self-update block/unblock utility
- `GappsUninstallActivity` → privileged GApps/debloat utility
- `WebRemoteActivity` → external web-remote page using the device's local IP, not an embedded HTTP server

These belong in an optional maintenance/tools layer and must not be treated as launcher-core dependencies.

### 3.6 About and entitlement surfaces

The following should be explicitly classified in the static map:

- `AboutAppActivity`
- `AboutBackgroundActivity`
- `AboutDeviceActivity`
- `PremiumActivity`
- `PremiumCardActivity`

The About activities are diagnostic/information surfaces. Premium activities are entitlement/purchase UI and are outside the feature-reimplementation target; they do not represent missing launcher architecture.

---

## 4. Plugin boundary can be stated more precisely

Group 01 correctly identified both:

```text
tv.projectivy.plugin.WALLPAPER_PROVIDER
```

and:

```text
tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED
```

Group 06 later closed the protocol enough to strengthen Group 01:

External services are discovered by `queryIntentServices(...)` and expose metadata conceptually including:

```text
uuid
name
updateMode
settingsActivity
itemsCacheDurationMillis
apiVersion
```

with observed requirements:

```text
UUID length = 36
apiVersion = 1
```

The Binder descriptor is:

```text
tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
```

and the observed transaction roles cover:

```text
get wallpapers for Event
read provider state/config
write provider state/config
```

So the better Group 01 architecture statement is:

> The wallpaper plugin system is a **discovered service + typed Binder contract + update broadcast**, not simply an intent-based plugin convention.

---

## 5. Exported-surface/security appendix worth adding

The original Group 01 identified the important exported components, but a reconstruction-oriented baseline benefits from explicitly classifying why they are exported.

### Protected exported services

- `ProjectivyAccessibilityService` → exported but protected by `BIND_ACCESSIBILITY_SERVICE`
- `NotificationListener` → exported but protected by `BIND_NOTIFICATION_LISTENER_SERVICE`

### User/document-facing exported activities

- `SetBackgroundActivity` → VIEW/SEND image/video
- `ImportSettingsActivity` → `.plbackup`
- `ImportThemeActivity` → `.pltheme`
- `ApplyIconPackActivity` → icon-pack apply actions
- `PinShortcutActivity` → `CONFIRM_PIN_SHORTCUT`
- `AppSettingsActivity` → `APPLICATION_PREFERENCES`

### Dynamically exposed launcher entries

- HDMI1–4
- AV
- Source Popup
- Media Explorer

These are exported but initially disabled and capability-gated.

### Exported receivers

- boot
- locale change
- storage/USB changes
- wallpaper-provider update

The wallpaper-update receiver is exported without a receiver-level permission in the manifest, so validation belongs to the runtime/plugin layer rather than being assumed from the custom permission declaration alone.

This security/capability distinction is useful when designing Aerial-Rows' manifest: only declare components as capabilities are implemented, and prefer system-protected bind services or explicit validation for exported surfaces.

---

## 6. MainActivity details worth adding

The application uses `@style/AppTheme`, but the HOME activity itself overrides that with:

```text
@style/SplashScreenTheme
```

This is a useful static detail because launcher startup presentation is intentionally separated from the application-wide theme.

MainActivity's previously recorded task posture remains valid:

```text
singleTask
empty taskAffinity
clearTaskOnLaunch
stateNotNeeded
excludeFromRecents
autoRemoveFromRecents
resumeWhilePausing
```

These should be understood as launcher-task behavior, not copied mechanically into Aerial-Rows without testing on Shield/Google TV.

---

## 7. Resource observations strengthened by later groups

### Themes

The built-in `.pltheme` files are now known to be ZIP packages containing at least:

```text
manifest               JSON metadata
appearance.plbackup    encrypted appearance backup
wallpapers/            optional media
```

### Calibration assets

The SDR/HDR/Dolby Vision raw assets correspond to the verified display/device tool family, not unused bundled media.

### Shortcuts

`@xml/shortcuts` is not just metadata noise. Group 10 confirmed Projectivy acts as a real Android shortcut host using `LauncherApps` / `ShortcutInfo` / `PinItemRequest`.

---

## 8. Things that were *not* missed

Several apparent gaps in Group 01 were intentional deferrals and were correctly closed later:

- home row/card presenters → Group 02
- app discovery/actions/package callbacks → Group 03
- channels/Watch Next → Group 04
- notification/media sessions → Group 05
- wallpaper provider/renderer internals → Group 06
- settings descriptors/storage effects → Group 07
- TV inputs/vendor adapters → Group 08
- HOME role/boot/accessibility/keys → Group 09
- backup/icons/shortcuts/parental control → Group 10
- residual named activities/native/obfuscated closure → Group 11

So Group 01 should not be criticized for lacking the implementation details that its scope explicitly deferred.

---

## 9. Revised Group 01 conclusions

1. The static package/component/resource map was correct and remains a reliable foundation.
2. `StartUpBootReceiver` must be relabelled as a registered-but-no-op receiver in 4.72; actual autostart lives elsewhere.
3. `ThemeContentProvider` is specifically a read-only theme-wallpaper URI bridge for ordinary consumers.
4. HDMI/AV/source launcher components are verified dynamically enabled components, not merely a manifest inference.
5. Declared EPG write permission should not be conflated with verified write behavior.
6. The named activity inventory should explicitly include category/channel wrappers, parental/internal launch activities, onboarding/pickers, vendor wrappers, maintenance utilities and About/entitlement surfaces.
7. The wallpaper plugin boundary is now known to combine service discovery, Binder IPC and update broadcasts.
8. `libptutils.so` is sufficiently closed as a narrow non-core boundary; no Aerial-Rows launcher subsystem depends on transplanting it.
9. No newly discovered component invalidates the Groups 2–12 implementation map.

## 10. Recommended status of Group 01

Change the status line from:

```text
complete — reviewed and strengthened
```

to:

```text
complete — post-closure audited after Groups 2–12
```

The original report can remain as historical evidence, with this document kept as its canonical audit/addendum.


---

# SOURCE FILE: `group02.md`

SHA-256: `82ff747a7ddd4d9e14293cd8459685e1bc41e16f1c3a24ca07234580a3e47b55`

---

# Projectivy 4.72 — Group 02: Home screen architecture

Status: **complete / second-pass reviewed**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group traces Projectivy's home UI from the launcher Activity into the browse fragment, row/category models, row presenters, card model/presenter hierarchy, focus/header state, row updates, hover/details UI and top/status UI. Content acquisition internals for apps, channels, Watch Next and media sessions are deliberately deferred to Groups 03–05; here they are mapped only where they cross the home architecture boundary.

## 1. High-level home graph

```text
MainActivity
  -> activity_main.xml
      -> MainFragment
          -> customized/minified Leanback BrowseSupportFragment base (`kz`)
              -> headers fragment (`db3` -> `vw1`)
              -> rows fragment (`hb3` -> `ly3`)
              -> BrowseFrameLayout focus/transition coordinator
          -> MainTitleView
          -> rows adapter (`po2`)
              -> row wrapper (`eb3`)
                  -> category header model (`q12`)
                  -> item adapter (`oo2`, stable IDs)
                  -> category model (`i5`)
                      -> item/card models (`t4` subclasses)
          -> row presenter selector (`ui2`, mode 0)
              -> single-row presenter (`jo4`)
              -> multi-row/grid presenter (`f23`)
          -> card presenter selector (`r30`)
          -> header presenter (`r12` -> `w12`)
          -> hover-card presenter selector (`ui2`, mode 2)
              -> `vy1`
                  -> `wy1` / animated `aa4`
```

Important boundary: `kz`, `vw1`, `ly3`, and several related classes are minified/customized AndroidX Leanback machinery. Projectivy-specific logic is layered around and inside that base through `MainFragment`, `db3`, `hb3`, `i5`, `w40`, `r30`, `r12`, `w12`, `ct` and its subclasses. Reimplementation should reproduce behavior, not blindly recreate Leanback internals that AndroidX already provides.

## 2. MainFragment responsibilities

`MainFragment` has 23 methods and 12 fields. It is the coordinator for the launcher home surface.

Confirmed responsibilities:

- installs the row presenter selector, card presenter selector and custom header presenter;
- owns the top/title/status view (`MainTitleView`);
- owns the visible rows adapter;
- maps category IDs to reusable item adapters and row wrappers;
- chooses one of three header/sidebar states from preferences;
- restores/selects rows and cards;
- updates/moves rows and cards without rebuilding the whole screen;
- recomputes row alignment/margins when configuration changes;
- reconnects adapters and presenters after view/configuration rebuilds;
- fades the header dock, content dock and title/status elements together;
- registers with the global home/category controller (`w40`) during its lifecycle.

### `C0()` — home renderer construction

This method is the central presenter/adapter setup path.

It constructs:

- `to2` — presenter selector for left-side category headers;
- `r30` — presenter selector for cards/items;
- `ui2(mode 0)` — row presenter selector;
- `r12` — custom category-header presenter;
- `po2` — top-level rows adapter, stable-ID enabled.

It then attaches that adapter to the rows fragment.

## 3. Sidebar / header state machine

The Leanback-derived browse base uses three explicit header states:

| State | Meaning |
|---|---|
| `1` | headers enabled and currently shown |
| `2` | headers enabled but currently hidden |
| `3` | headers disabled entirely |

`MainFragment.C0()` chooses between these from two preferences. Semantically the launcher can therefore support:

1. always-visible/initially-visible headers;
2. hidden headers that can be revealed by navigating left;
3. no headers at all.

This is the architectural mechanism behind the Projectivy behavior the Aerial-Rows project wants: **the left navigation column is not simply conditionally drawn; it is a focusable browse pane with a real shown/hidden state and transition.**

### State persistence

The browse layer saves/restores:

- selected row position;
- whether headers are currently shown;
- page-row state;
- title visibility.

The rows fragment also restores its selected row position.

### Focus switching

When the fragment resumes:

- if headers are enabled and shown, focus is requested on the headers fragment;
- otherwise focus is requested on the rows/content fragment.

Header transitions are coordinated through `m0()/n0()` and are deferred if the row list is actively scrolling or laying out. This prevents a left/right header transition from fighting RecyclerView/Leanback focus movement.

### Header transitions

Transitions use Leanback browse header-in/header-out transitions. Before transition start, rows are frozen/focus search is disabled; the transition swaps focusable panes and later restores normal layout/focus behavior.

The implementation also supports headers-back-stack behavior inherited from Leanback.

## 4. Selected-row coordinator

`jz` is a small but important deferred selection coordinator.

It stores a requested row index and priority, posts itself to the browse frame, and when executed:

- updates `kz.n1` (current selected row);
- synchronizes selected position in the headers list;
- synchronizes selected position in the rows/content list;
- performs smooth or immediate selection;
- swaps the content fragment if required;
- updates title visibility/state;
- reevaluates whether the sidebar/title can be shown.

This means row selection is centrally synchronized across both panes instead of letting the header list and content list drift apart.

### Startup/default-row and refocus behavior

`MainFragment.s0()` resolves a preferred/default category ID from the home preferences, converts that category ID into the **current visible-row index**, and falls back to row `0` if the category is absent/hidden. It therefore does not persist a brittle adapter index.

`MainFragment.w0(category, card)` refocuses by **stable category ID + stable card ID** after the adapters settle. If no explicit category/card is supplied it refocuses the resolved default category and its first item. This is performed asynchronously on the fragment lifecycle scope rather than synchronously during adapter mutation.

`MainFragment.y0()` is the “return focus to home content” path: it requests focus on the content dock and, when the relevant navigation preference requires it and the current row lies below the resolved default-row position, explicitly selects **row 0** before calling `w0(null, null)` to restore a valid card focus. This is another reason Projectivy avoids the common launcher bug where returning from settings or an app leaves focus at an invalid/stale position.

## 5. Category model (`i5`)

`i5` is the configurable home-category/row model. It has 31 fields and 19 methods.

### Confirmed fields / behavior

The following semantics are confirmed from construction and use sites:

- stable category ID;
- category/source type;
- default title resource / custom title;
- default icon / custom icon URI;
- category accent/icon colour;
- list of current `t4` items;
- per-category item ordering list;
- card aspect/style (`df`);
- category scale factor;
- visibility;
- multi-row/grid mode;
- items-per-line override;
- sorting mode;
- max-row override;
- start margin percentage;
- end margin percentage;
- top margin percentage;
- optional row background colour;
- external Android-TV channel metadata for channel-backed categories.

### Verified field-to-setting mapping

A second-pass trace through `CategoryPreferencesFragment` and its advanced subclass (`d40`) removes the remaining ambiguity around the main presentation fields:

| `i5` field | Verified meaning | Evidence at the home/settings boundary |
|---|---|---|
| `n` | category visible | `category_visible` reads/writes `i5.n`; `i5.l()` uses it as the first visibility gate |
| `o` | multiline / multi-row grid | `category_multiline` reads/writes `i5.o`; `ui2` selects `f23` when true |
| `q` | **inverse** of “sliding single row” | `category_sliding_single_row` displays `!i5.q` and writes `i5.q = !switch`; `ui2` passes `!q` into the single-row presenter’s custom sliding/window-alignment behavior |
| `r` | **inverse** of “show card title” / hover details | `category_show_app_title` displays `!i5.r` and writes `i5.r = !switch`; hover presenter selection returns null when `r` is true |
| `m` | per-category card scale | `category_scale` maps discrete UI values to `0.6/0.8/1.0/1.2/1.4`; multiplied by the global card scale |
| `p` | items per line | `category_items_per_line`; `0` means automatic |
| `w` | maximum rows | `category_max_rows`; `0` means automatic |
| `y` | start margin percentage | `category_start_margin`; falls back to the global start-margin preference when `0` |
| `z` | row right-edge percentage / end-margin control | `category_end_margin`; `100` means use the global value, and the renderer computes the physical end margin as `(1 - z/100) * screenWidth` |
| `A` | top margin percentage | `category_top_margin` |
| `B` | multi-row background colour | used by the rounded background decoration only for eligible multi-row layouts |
| `s` | ordering/sort mode | bound to `category_order` |
| `t` | custom title | used before the default/resource/channel title |
| `x` | custom icon URI | used before the default icon when allowed |

`i5.i` is not a user-facing toggle. It is derived from category/item content and determines whether the hover area reserves the taller description-capable height. This is why rows with program/media metadata can reserve more vertical hover space than simple app rows.

These names are obfuscated, but their behavioral effect is clear from the presenter logic.

### Visibility rule

`i5.l()` determines whether the category should appear. A category must be enabled and normally non-empty; special generic/custom category types are allowed to remain visible under specific rules.

### Sorting

The category implements multiple sort modes, including:

- stored/manual order;
- case-insensitive alphabetic;
- other comparators tied to card metadata;
- shuffled/random order with persistence of the resulting order.

Detailed app/program sorting semantics belong in Groups 03/04.

## 6. Built-in category/source types

`w40.k()` constructs Projectivy's built-in row definitions.

Confirmed category source types:

| Category type | Built-in usage |
|---:|---|
| `0` | general/custom/favourites-style categories (also Video/Music/Games/custom user rows) |
| `1` | Inputs |
| `2` | TV Apps |
| `3` | Mobile Apps |
| `4` | Projectivy internal action rows (Settings, Display, Apps, System, About) |
| `5` | Watch Next |
| `6` | Android TV channel-backed category (constructed from `TvContract` channel metadata) |
| `7` | Android launcher shortcuts |
| `8` | Now Playing |
| `9` | Category folders |
| `10` | Live TV inputs |

Built-in category IDs also include separate default rows for favourites, video, music, games, TV apps, mobile apps, Watch Next, shortcuts, folders, Live TV, Now Playing, inputs, settings/display/apps/system/about.

This is important for reconstruction: **Projectivy's home is a generic category registry, not a set of unrelated hard-coded screens.**

## 7. Row wrapper and stable adapters

Each visible row is an `eb3` object. It combines:

- `q12` — lightweight category header object (ID/title/category reference);
- `oo2` — item adapter;
- `i5` — category configuration/data model.

`oo2` returns a card's own stable integer ID as the adapter stable ID. Top-level rows are also stable-ID backed.

`MainFragment` maintains two maps keyed by category ID:

- category ID -> reusable `oo2` card adapter;
- category ID -> reusable `eb3` row wrapper.

When the home model refreshes, existing adapters/wrappers are reused instead of recreating every row. Hidden/removed categories are evicted from those maps.

## 8. Global home/category controller (`w40`)

`w40` is the home model/controller boundary. It is both a lifecycle observer and a broadcaster to active `MainFragment` instances.

Confirmed responsibilities at the Group-02 boundary:

- owns the global ordered category list;
- owns category lookup by ID;
- constructs built-in category definitions;
- merges persisted category configuration into new/default definitions;
- broadcasts model refreshes to active home fragments;
- broadcasts row movement;
- broadcasts item movement;
- tracks temporary row-reorder state;
- tracks temporary item-reorder state;
- asks each active home fragment to refocus a category/card;
- triggers persistence/update work after reorder operations.

### Refresh algorithm

On a full category refresh, each active `MainFragment`:

1. iterates the global ordered category list;
2. ignores categories that should not currently be visible;
3. obtains/reuses an `oo2` item adapter for the category;
4. obtains/reuses an `eb3` row wrapper;
5. replaces the item adapter contents with the category's current items;
6. if an otherwise-visible category has no normal items, inserts a concrete `dp2` placeholder card rather than leaving a focusable empty row; `dp2` opens that category’s **Apps** configuration (`settings_category_apps` / `AllAppsSidePanelActivity`) so the user can populate it;
7. replaces/reorders the top-level rows adapter using the resulting list;
8. reports first draw when appropriate.

This gives Aerial-Rows a useful target: use stable category IDs and stable card IDs so updates/reorders do not destroy focus state.

## 9. Row presenter selection

`ui2(mode 0)` selects one of two Projectivy row presenters per category.

### `jo4`

Normal single-row presenter.

It supports standard horizontal browsing plus custom behavior for touch/scroll settling and window alignment.

### `f23`

Multi-row/grid presenter.

It extends `jo4` and configures the horizontal grid's internal layout manager for multiple rows. It keeps per-category state used while arranging grid rows.

The choice is driven by the category's multi-row flag.

### Presenter cache key and transitional visibility fallback

The row presenter selector does not construct a new presenter for every bind. `ui2(mode 0)` caches presenters using a key derived from:

- effective card scale (`globalScale × categoryScale`);
- single-row vs multi-row mode;
- sliding-single-row mode (`i5.q`).

Categories with equivalent presentation parameters therefore reuse presenter instances. A setting that changes one of those parameters naturally resolves to a different cached presenter without requiring a global UI rewrite.

`MainFragment.C0()` then wraps the row selector with `ui2(mode 1)` plus an empty `c92` presenter. If an `eb3` row becomes non-visible (`!i5.l()`) while an adapter/layout transition is still in flight, the wrapper returns the empty presenter instead of attempting to render stale content. This is a small but important transition-safety layer in addition to the normal full-refresh removal of hidden categories.

## 10. Row layout algorithm

`lu`, the Projectivy row-presenter base, calculates row layout from the `i5` category model at bind time.

Confirmed inputs:

- global card-scale preference;
- category-specific scale;
- card aspect/style;
- item spacing;
- category start/end margins (percentage of display width);
- category top margin (percentage of display height);
- explicit items-per-line override;
- automatic items-per-line calculated from available width/card width;
- multi-row flag;
- maximum-row override;
- available vertical screen space;
- hover/title area height;
- focus zoom factor;
- optional rounded row-background colour.

For multi-row categories it computes how many rows are needed, caps the value, then also caps it by the vertical space available and the category's max-row setting.

The HorizontalGridView's height is then explicitly resized to fit the chosen number of card rows.

This confirms that Projectivy category settings such as items-per-line/max rows/margins are part of the actual layout algorithm, not superficial decoration.

### Reselection behavior

A global preference controls whether selecting a row resets its internal horizontal selection back to item zero. Otherwise the row can preserve its prior card position.

### Non-selected row fading

Rows can be rendered with reduced alpha based on a global preference. Selected-row interpolation drives the alpha of both the row content and row header.

### Vertical rows alignment (`hb3`)

The rows fragment also customizes the **vertical** focus geometry. Its `d0()` path selects the `VerticalGridView` focus-scroll strategy from navigation preferences and, in the custom-alignment mode, computes a top/window-alignment offset from:

- title/status-bar height;
- whether in-row headers are enabled;
- global card scale;
- current Leanback focus-zoom factor.

It then applies matching window/item alignment and a compensating top margin. In practical terms, Projectivy deliberately anchors the focused row underneath the top/status region instead of relying on RecyclerView’s default “scroll just enough to reveal” behavior. This should be reproduced explicitly in a Compose implementation with controlled scroll positioning rather than left to default `LazyColumn` focus scrolling.

## 11. Card model hierarchy (`t4`)

`t4` is the abstract/common launchable-card model. It exposes title, description, icon/artwork, category reference, launch Intent construction, stable ID, enabled/available state, visual aspect information and additional capability flags.

### Presenter type IDs

`r30` maps card item type IDs to presenter implementations:

| Item type | Model | Semantic role | Presenter |
|---:|---|---|---|
| `0` | `e90` (where used) | generic/internal action card | `q64` |
| `1` | `z52` extends `e90` (and configured `e90` input actions) | detected/configured input-source action | `q64` |
| `2` | `qb` | TV app | `rb` |
| `3` | `qb` | mobile app | `rb` |
| `4` | `e90` | Projectivy internal action card | `q64` |
| `5` | `b15` extends `ck3` | Watch Next program | `ek3` |
| `6` | `ck3` | Android TV channel program | `ek3` |
| `7` | `p94` | `LauncherApps` shortcut | `q94` |
| `8` | `c73` | Now Playing / MediaController item | `d73` |
| `9` | `ck1` | category-folder card | `dk1` |
| `10` | `vj2` | Android TV input | `wj2` |

This 0–10 **item type** must not be confused with the `i5.k` **category/source type** table above.

The inheritance distinction matters: `b15` is the Watch Next-specific `ck3` subclass and constructs the base with item type `5`; direct `ck3` instances use item type `6` for ordinary channel programs. Likewise, `z52` extends `e90` and is used for detected HDMI/AV/source actions at item type `1`, while configured input-source popup actions can also be represented by `e90` with that type. This keeps source discovery semantics in the model class while reusing the same `q64` presentation family.

## 12. Card presenter base (`ct`)

All main card presenters derive from `ct`.

Confirmed responsibilities:

- creates Projectivy's `MainCardView`-derived view (`bt`);
- dynamically computes card width/height from aspect + category/global scale;
- loads card artwork asynchronously;
- uses stable item/category information as image-cache/signature input;
- updates card content description;
- applies enabled/disabled alpha;
- controls selected/unselected background/icon tint behavior;
- tracks the currently selected card;
- emits wallpaper-provider/plugin `CardFocused` events including dynamic colours, title and app package where relevant;
- delegates specialized binding/focus behavior to subclasses.

Card image size is therefore not fixed in XML. It is recomputed per card/category.

## 13. Card view / focus effects

`bt` is the concrete home-card view wrapper around Projectivy's common card view hierarchy.

On selection change it:

- informs the presenter (`ct.r`);
- updates selected colours/effects;
- can trigger Projectivy's focused-card visual effect depending on preferences;
- notifies the wallpaper/plugin event system.

The base card view supports:

- selected Z/elevation;
- glow/shadow effects;
- image alpha transition;
- dynamic primary and secondary colours;
- progress bar;
- badge artwork;
- card/image aspect handling.

The exact app long-press/context-action content is intentionally deferred to Group 03, but the card view already wires click, long-click and context-click listeners when hosted by `MainActivity`.

## 14. Program-card special behavior

The program presenter (`ek3`, item types 5/6) additionally:

- displays playback progress from TV-provider metadata when enabled;
- optionally displays a channel/program logo badge;
- emits a more detailed `ProgramCardFocused` plugin event;
- recognizes `preview_video_uri`;
- starts/stops preview playback on focus according to settings/lifecycle.

The underlying TV-provider query/data-building code is Group 04, but the home renderer boundary is now closed.

## 15. Now Playing card boundary

Item type `8` (`c73`) represents a live MediaController/media-session card. At the home layer it provides title/subtitle/artwork and launch/control behavior through the common card system.

The actual session discovery/notification/media logic is Group 05.

## 16. Hover/details card

Projectivy uses Leanback's row-hover area with a custom hover-card presenter.

`ui2(mode 2)` is the hover presenter selector. When hover-card display is enabled globally and not suppressed for the category, it returns `vy1`.

`vy1` binds:

- card title (`t4.A()`);
- description (`t4.o()`);
- optional app/program icon where appropriate;
- a unique shared-element transition name for the title.

The concrete hover view is `wy1`, which inflates `hover_card_view.xml` and contains:

- title;
- up-to-three-line description;
- optional 80x45dp icon.

Depending on preference, `vy1` creates either:

- normal `wy1`, or
- animated `aa4`.

`aa4` animates the hover area out by collapsing its height instead of abruptly removing it.

This closes the earlier Group-02 uncertainty around `hover_card_view.xml`: it is actively used by the home row presenter system.

## 17. Content-row header (`hv`)

Rows can also have a row header attached to their content presentation.

`hv` binds `row_header.xml` and uses the category model to apply:

- category title;
- optional category icon;
- category start/end margins;
- category top margin;
- icon tint/loading behavior.

Its layout margins deliberately mirror the card-row margins, keeping the row label aligned with its content.

This content-row header is separate from the **left navigation header pane** described next.

## 18. Left navigation header (`r12` / `w12`)

`r12` creates custom `w12` header views for the sidebar.

A `w12` contains:

- category icon;
- category text;
- reorder affordance;
- settings affordance.

Global preferences can independently hide:

- reorder affordance;
- settings affordance;
- header text;
- header icon.

If both icon and text are hidden, the base header focus target is removed from the internal focus sequence.

### Sidebar focus behavior

`w12.focusSearch()` implements custom horizontal focus between the visible internal elements. When moving out of the row-header controls toward content, focus falls back to the browse hierarchy.

### Selected-header animation

When a category header becomes selected, its secondary controls animate into view with staggered translation/alpha. When deselected they collapse/fade again.

### Category actions

From the header:

- settings opens that category's settings screen;
- long-press settings has a secondary action path;
- reorder enters category reorder mode;
- certain Android-TV channel categories support launching their source/provider app via long press.

### Reorder mode

`w40.O` tracks the category being reordered. While this mode is active:

- DPAD up/down calls `w40.q(-1/+1)`;
- the category is moved only across other visible categories;
- top-level rows are updated with adapter move notifications;
- leaving reorder mode restores normal focus/action behavior and triggers persistence.

This is a strong implementation target for Aerial-Rows: category reorder can be an explicit UI state rather than drag-and-drop.

## 19. Row/card movement without focus loss

Projectivy does not rebuild the entire home screen when an item moves.

For card reorder, `MainFragment.u0()` obtains the existing category adapter, emits an item-change notification where needed, and uses adapter move operations when source/destination differ.

For row reorder, `w40` broadcasts the move and `MainFragment` moves the existing top-level row adapter entry, then refocuses the category.

`MainFragment.w0()` and `x0()` schedule refocusing of a specific category/card after the adapter settles.

This is directly relevant to the user's settings/focus requirement: preserving stable adapter objects and IDs is one of the reasons Projectivy can change state without sending focus back to the first item.

## 20. `MainTitleView` / top status area

`MainFragment` inflates `custom_browse_title` as the title/top area and keeps a `MainTitleView` reference.

The title/status view contains:

- clock;
- wallpaper/view-mode control;
- VPN control;
- network/connectivity control;
- notifications control + notification count;
- settings control;
- battery control;
- Material button group container.

Confirmed interactions include:

- Settings -> Projectivy settings side panel;
- Notifications -> Projectivy notification side panel;
- Network -> Android Wi-Fi settings;
- VPN -> Android VPN settings;
- wallpaper/view-mode button -> configured Intent/action path.

The status controls have custom left/right focus traversal that skips hidden buttons and prevents focus from disappearing at the edges.

Preferences control clock visibility/size, settings visibility, notification visibility/count behavior, icon scale/padding, colours/background tint and other status presentation.

`MainFragment.q0()` drives a coordinated 1-second alpha transition for the **headers dock and content dock**. When its status-bar flag is set, it also fades the `MainTitleView` status-icon button group to the requested alpha while keeping the `MainTitleView` root at a separate `0.8` alpha. The previous wording that all title elements simply share one alpha was too broad.

`MainFragment.Z(boolean)` also overrides the Leanback title-visibility decision with a Projectivy preference, so top/title visibility is not solely a consequence of the current selected row.

## 21. Dynamic configuration refresh

`MainFragment.v0()` is effectively a home renderer refresh path used after relevant preference/configuration changes.

It:

- validates the fragment/view is active;
- asks the rows fragment to refresh;
- recomputes top margins/alignment;
- refreshes the top title/status bar;
- recomputes header-row width/spacing;
- rebuilds/rebinds presenter selection;
- asks `w40` to refresh the model.

The architecture therefore separates:

```text
model refresh (w40)
from
renderer/presenter refresh (MainFragment / db3 / hb3)
```

That separation is worth preserving in Aerial-Rows.

## 22. Leanback machinery vs Projectivy-specific behavior

### Mostly Leanback/minified framework machinery

- `kz` browse-fragment state/transition base;
- `vw1` headers fragment base;
- `ly3` rows fragment base;
- RecyclerView/GridLayoutManager helper layers;
- standard browse header transition plumbing.

### Projectivy-specific or materially customized layers

- `MainFragment`;
- `db3` sizing/spacing for sidebar;
- `hb3` row alignment/spacing/performance behavior;
- `i5` category model;
- `w40` category/home controller;
- `eb3` / `q12` category-row wrappers;
- `r12` / `w12` sidebar presentation/actions;
- `ui2` Projectivy row + hover presenter selection;
- `r30` card presenter selection;
- `ct` card presenter base and its type-specific presenters;
- `MainTitleView` status/actions;
- wallpaper/provider focus event emission.

This distinction prevents an Aerial-Rows rewrite from unnecessarily cloning AndroidX internals.

## 23. Recommended Aerial-Rows home architecture derived from Group 02

Do not preserve Aerial-Rows' current assumption that a home category is merely a list of installed apps.

A more faithful reconstruction should use something like:

```text
HomeController
  categories: StateFlow<List<HomeCategory>>

HomeCategory
  id
  sourceType
  title/icon/colour
  visible
  presentationConfig
  orderingConfig
  items

PresentationConfig
  cardScale
  cardAspect
  multiRow
  itemsPerLine
  maxRows
  startMarginPercent
  endMarginPercent
  topMarginPercent
  rowBackground
  showHoverDetails
  singleRowScrollMode

HomeItem (sealed interface)
  App
  InternalAction
  TvProgram
  Shortcut
  NowPlaying
  CategoryFolder
  TvInput

HomeSelectionState
  selectedCategoryId
  perCategorySelectedItemId
  sidebarState
  reorderMode
```

For the Compose implementation, the behavioral equivalent of Projectivy's Leanback setup should be maintained even if the actual UI toolkit changes:

- stable keys for categories and cards;
- independent vertical category selection and per-row horizontal item selection;
- hidden/revealable/disabled sidebar states;
- explicit focus transfer when sidebar opens/closes;
- do not rebuild LazyRow/LazyGrid objects unnecessarily on settings changes;
- retain per-category card position and restore selection by category/card IDs rather than raw adapter indices;
- item and row moves should use stable IDs and state mutation, not full list reset;
- visible-but-empty custom categories should expose an intentional “configure/populate this row” card rather than a dead empty focus surface;
- hover/details should be a first-class presenter/state, not text hacked into individual card composables;
- category presentation settings must feed the layout calculation directly.

## 24. Group-02 closure / deferred work

The **home renderer and interaction architecture is now closed enough for independent implementation**.

The following are intentionally not considered missing Group-02 work because they are separate subsystem groups:

- where installed-app lists come from and exact app filtering -> Group 03;
- complete click/long-press app action menu -> Group 03;
- TvProvider/channel/Watch Next query/update observers -> Group 04;
- media-session/notification discovery -> Group 05;
- actual wallpaper-provider implementation and video wallpaper behavior -> Group 06;
- exact persistence keys/settings screens for every category field -> Group 07/10.

## 25. Corrections/improvements over the earlier Group-02 pass

The review added or corrected the following:

1. separated **category/source type IDs** from **card item/presenter type IDs**;
2. fully mapped built-in category types 0–10;
3. fully mapped item presenter types 0–10 and corrected the model inheritance split (`z52`/`e90` for type 1; `b15`/`ck3` for types 5/6);
4. confirmed the full hover-card path `ui2 -> vy1 -> wy1/aa4` and active use of `hover_card_view.xml`;
5. confirmed `hv` is a separate in-row header from the left navigation header `w12`;
6. traced the category refresh algorithm and stable row/item adapter reuse;
7. traced row and card reorder update paths and their focus-preserving behavior;
8. confirmed the row layout algorithm consumes the configurable margins/items-per-line/max-rows/background/scale settings directly;
9. confirmed explicit shown/hidden/disabled sidebar states plus saved/restored selection/header state;
10. confirmed header transition deferral while RecyclerViews are scrolling;
11. confirmed category header internal focus/actions/reorder mode;
12. traced top/status-bar focus and action behavior;
13. identified which classes are primarily Leanback internals versus actual Projectivy logic;
14. replaced the ambiguous `i5.q`/`i5.r` interpretations with exact settings mappings: `q = !slidingSingleRow`, `r = !showCardTitle`;
15. verified the advanced field mappings for items-per-line, max rows and start/end/top margins;
16. traced the `dp2` empty-row card and confirmed it opens that category’s Apps configuration rather than being a generic dummy placeholder;
17. documented the row-presenter cache key and the `c92` transitional visibility fallback;
18. documented ID-based startup/refocus behavior (`s0/w0/y0`) rather than treating selection as raw list-position state;
19. added the `hb3` vertical focus/window-alignment algorithm;
20. corrected the `q0()` fade description and documented the separate title-visibility override.

**Group 02 status: COMPLETE / SECOND-PASS REVIEWED.**


---

# SOURCE FILE: `group02_postclosure_audit.md`

SHA-256: `61cbbc67441bed68d7d837fc1f43d1f3ca22ae8eeb9b7c860ae9a7bf853cd373`

---

# Group 2 post-closure audit — Home architecture

Status: **POST-CLOSURE AUDITED after Groups 3–12**
Original Group 2 SHA-256: `82ff747a7ddd4d9e14293cd8459685e1bc41e16f1c3a24ca07234580a3e47b55`

## Executive conclusion

The original Group 2 report is architecturally sound. Nothing found in Groups 3–12 invalidates its core model:

```text
MainActivity
  -> MainFragment
  -> customized Leanback browse state
  -> generic category/row registry
  -> stable row/card adapters
  -> type-specific card presenters
  -> focus/header/sidebar state
```

No hidden replacement home-screen subsystem was discovered later.

However, the later groups let us improve Group 2 in several material ways:

1. add the **exact built-in category IDs**, not only source-type IDs;
2. add the **dynamic TV-channel category-ID formula**;
3. map the exact global settings controlling sidebar/header/focus/row behavior;
4. distinguish the row hover metadata view from the separate Program Details screen;
5. refine "stable IDs" as **model-specific identities**, not one uniform global identity scheme;
6. clarify the two separate input row families;
7. strengthen the Now Playing and TV-channel row semantics;
8. slightly soften the claim that item type 0 is "fully mapped".

These are completeness/precision upgrades, not architectural corrections.

---

## 1. Genuine completeness miss — exact built-in category IDs

Group 2 mapped source types 0–10 correctly, but `w40.k()` gives the exact fixed IDs as well.

The built-in rows in Projectivy 4.72 are:

| Category ID | Source type | Built-in row |
|---:|---:|---|
| `1` | `1` | Inputs |
| `2` | `0` | Favorites |
| `3` | `2` | TV Apps |
| `4` | `3` | Mobile Apps |
| `5` | `4` | Settings |
| `6` | `4` | Display |
| `7` | `4` | Apps |
| `8` | `4` | System |
| `9` | `4` | About |
| `10` | `5` | Watch Next |
| `11` | `7` | Shortcuts |
| `12` | `8` | Now Playing |
| `13` | `9` | Category Folders |
| `14` | `10` | Live TV |
| `20` | `0` | Video |
| `21` | `0` | Music |
| `22` | `0` | Games |

This matters because later groups show fixed category IDs are used across:
- default-category selection;
- targeted home refresh;
- notification/Now Playing correlation;
- focus restoration;
- persistence.

For example, Group 5 independently verified:

```text
Now Playing category ID = 12
source type = 8
```

So the exact-ID table should now be considered part of the canonical Group 2 home model.

### Aerial-Rows implication

Do not reproduce these opaque integer IDs literally as the new architecture.

Use namespaced stable IDs such as:

```text
builtin:favorites
builtin:watch_next
builtin:now_playing
builtin:inputs
builtin:live_tv
```

and migrate any legacy numeric identity only if needed.

---

## 2. Genuine completeness miss — dynamic TV-channel category IDs

Group 4 closed the dynamic-channel constructor that Group 2 deliberately left at the boundary.

For a Preview Channel:

```text
i5(m60 channel)
```

uses:

```text
category source type = 6
category ID = (channelId + 2000) cast to int
```

and Projectivy caches/reuses dynamic channel-row objects keyed by the platform channel ID.

Therefore the complete Projectivy row-ID picture is:

```text
fixed built-in category IDs
        +
dynamic channel-derived category IDs
```

### Important reconstruction improvement

Projectivy's numeric channel ID is usable for its own runtime reconciliation, but Aerial-Rows should use a stronger stable logical channel identity because OEM/provider firmware can recreate TV-provider numeric IDs.

Recommended identity:

```text
provider package
+ internal_provider_id where present
+ stable provider/display identity fallback
```

with numeric `channelId` treated as the current lookup handle.

---

## 3. Precision upgrade — the exact sidebar state preferences are now known

Group 2 correctly reconstructed three left-header/sidebar states:

1. shown;
2. hidden but revealable;
3. disabled.

Group 7 later provided the exact settings behind the initial state.

```text
hj3.D1 = appearance_left_headers_enabled
hj3.E1 = appearance_left_headers_show_on_launch
```

`MainFragment.C0()` effectively maps them as:

```text
left headers disabled
    -> state 3

left headers enabled + show on launch
    -> state 1

left headers enabled + do not show on launch
    -> state 2
```

The individual sidebar affordances are:

```text
hj3.F1 -> appearance_left_headers_show_icon
hj3.G1 -> appearance_left_headers_show_title
hj3.H1 -> appearance_left_headers_show_reorder
hj3.I1 -> appearance_left_headers_show_settings
```

This confirms Group 2's state-machine interpretation exactly.

---

## 4. Precision upgrade — content-row headers and left headers have separate preference families

Group 2 correctly distinguished:

```text
hv
  = header attached to the content row

w12/r12
  = left navigation/sidebar header
```

Group 7 lets us make the separation explicit in persisted settings.

### Content-row/category presentation

```text
hj3.m1 -> appearance_categories_show_title
hj3.n1 -> appearance_categories_show_icon
hj3.w1 -> appearance_categories_tint_custom_icons
```

### Left navigation pane

```text
hj3.F1 -> appearance_left_headers_show_icon
hj3.G1 -> appearance_left_headers_show_title
hj3.H1 -> appearance_left_headers_show_reorder
hj3.I1 -> appearance_left_headers_show_settings
```

This is useful because "hide category titles" and "hide left-nav titles" are two different settings and should remain different in Aerial-Rows.

---

## 5. Precision upgrade — exact global row/focus controls

Group 2 described these behaviors correctly but did not yet know their final preference names.

Later mapping gives:

```text
hj3.o1 -> appearance_categories_rows_spacing
hj3.p1 -> appearance_categories_focused_category_position_fixed
hj3.q1 -> appearance_categories_hide_rows_above_selection
hj3.r1 -> appearance_categories_remember_focused_card_position
hj3.s1 -> appearance_categories_unselected_row_opacity
hj3.t1 -> appearance_categories_listing_top_margin
hj3.u1 -> appearance_categories_start_margin
hj3.v1 -> appearance_categories_end_margin
hj3.x1 -> appearance_categories_default_category_id
```

### Remember-focused-card behavior

Group 2 said a global preference determines whether a row resets to item 0.

The exact setting is now known:

```text
appearance_categories_remember_focused_card_position
```

When remembering is disabled, Projectivy resets the horizontal row selection to position 0 when the row is selected through the relevant presenter path.

### Vertical focus geometry

Group 2's `hb3` analysis should now explicitly reference the combination of:

```text
appearance_categories_focused_category_position_fixed
appearance_categories_hide_rows_above_selection
```

because both participate in the vertical focus/window-alignment strategy.

---

## 6. Precision upgrade — focused-card visual settings

Group 2 accurately described the selected-card visual system, but Group 7 closed the preference keys.

```text
hj3.c1 -> appearance_cards_program_zoom_on_focus
hj3.d1 -> appearance_cards_focused_scale
hj3.e1 -> appearance_cards_focused_effect
hj3.f1 -> appearance_cards_focused_animation
hj3.g1 -> appearance_cards_focused_glow_size
hj3.h1 -> appearance_cards_focused_glow_tint
hj3.i1 -> appearance_cards_focused_color_override_mode
hj3.j1 -> appearance_cards_focused_bg_tint
hj3.k1 -> appearance_cards_focused_tint
```

So Group 2's "focus effects" are not one effect toggle; they are a configurable family covering scale, animation, glow and color treatment.

---

## 7. Factual refinement — "Hover/details card" should be renamed

Group 2 section 16 called the `vy1 -> wy1/aa4` path a:

> Hover/details card

The implementation itself was described correctly, but later Group 4 proves there is also a separate **Program Details** screen.

The Group 2 feature should therefore be named more precisely:

```text
row hover metadata / description area
```

It contains:
- title;
- short description;
- optional icon;
- animated collapse behavior.

It is **not** the same as the later Program Details side panel/screen.

The separate program-details path can perform an additional lazy TV-provider query for:

```text
long_description
```

using the program `_id`.

That distinction prevents an Aerial-Rows implementation from trying to put the full details experience into the row hover element.

---

## 8. Factual refinement — stable IDs are model-specific, not one universal identity formula

Group 2 correctly emphasizes stable category/card IDs and adapter reuse.

Later groups show that the actual identity formula depends on card type.

Examples:

```text
App
  -> packageName hash in the Projectivy app-card path

Preview Program
  -> primarily intent_uri-based identity

Watch Next
  -> specialized Watch Next identity helper

Now Playing
  -> media-session token-derived identity

Shortcut
  -> local card ID based on shortcut ID hash

Dynamic TV channel row
  -> channel-derived category ID
```

Therefore the canonical statement should be:

> Projectivy consistently *uses stable identity as an architectural principle*, but the actual identifier is type-specific.

It should **not** be interpreted as a single globally collision-proof ID system.

### Aerial-Rows improvement

Use namespaced structured identities:

```text
app:<package>/<component>
program:<provider>/<program identity>
watchnext:<provider>/<program identity>
session:<session identity>
shortcut:<package>/<user>/<shortcut id>
input:<stable input identity>
```

instead of Projectivy-style integer hashes where we control the design.

---

## 9. Precision upgrade — Inputs vs Live TV are two distinct row families

Group 2 already mapped:

```text
source type 1  = Inputs
source type 10 = Live TV inputs
```

Group 8 lets us state the semantic difference much more clearly.

### Category ID 1 — Inputs / source actions

Uses the internal-action/input-action family:

```text
e90
z52 extends e90
item type 1
q64 presenter
```

This is where generic HDMI/AV/source switching actions fit.

### Category ID 14 — Live TV

Uses actual Android TV Input Framework items:

```text
vj2
item type 10
wj2 presenter
```

This is backed by discovered/tunable TV inputs rather than merely an internal "switch to HDMI" action.

Aerial-Rows should preserve this conceptual split:

```text
InputAction
    vs
TvInputSource
```

even if both render as launcher cards.

---

## 10. Precision upgrade — Now Playing row semantics

Group 2 correctly identified the `c73` boundary.

Group 5 lets the home-level description be strengthened:

```text
category ID 12
source type 8
item type 8
model c73
presenter d73
```

Additional home semantics:

- the row can contain multiple active sessions;
- cards are deduplicated at session-token level;
- `c73` is not manually reorderable like ordinary app cards;
- one separate primary session is chosen for global Now Playing/wallpaper state;
- the row is refreshed through the normal `w40` category controller.

This further confirms that Now Playing is a normal dynamic `i5` row source rather than a special overlay.

---

## 11. Precision upgrade — TV channel rows are platform-manageable, not only launcher-local

Group 2 described source type 6 as Android TV channel-backed rows.

Group 4 adds an important behavior:

when a user enables a channel-backed category, Projectivy can call:

```text
TvContract.requestChannelBrowsable(context, channelId)
```

So row visibility has two layers:

```text
Projectivy row/category state
        +
Android TV-provider browsable state
```

The channel-management UI also groups dynamic rows by provider application.

This belongs in the canonical home model because "show channel" is not purely a local Compose/adapter operation.

---

## 12. Precision upgrade — program-card focus event semantics

Group 2 correctly found:

```text
CardFocused
ProgramCardFocused
```

events at the presenter boundary.

Group 6 later proved these are first-class events in the wallpaper-provider event system.

The relevant event families include:

```text
CardFocused
ProgramCardFocused
NowPlayingChanged
LauncherIdleModeChanged
TimeElapsed
```

So focus events should now be described as part of an explicit home-to-wallpaper event bus, not simply "plugin notifications".

For Aerial-Rows, card focus should similarly publish semantic state independent of the renderer.

---

## 13. Small wording correction — item type 0 was not semantically "fully mapped"

The Group 2 closure says:

> fully mapped item presenter types 0–10

The **dispatch table** was mapped, but item type 0 was still described as:

```text
e90 (where used)
generic/internal action card
```

while the active built-in internal rows seen in `w40` predominantly construct `e90` with item type 4, and input actions use type 1.

So a more precise statement is:

> Presenter dispatch slots 0–10 were mapped; active semantics are strongly established for types 1–10, while type 0 remains a generic/legacy-compatible internal-action slot in the inspected 4.72 paths.

This does not affect implementation because Aerial-Rows should use sealed card types rather than numerical presenter IDs.

---

## 14. Things Group 2 correctly deferred — not misses

The following should **not** be counted as failures in Group 2:

- exact installed-app discovery and package update semantics — Group 3;
- generic `j6` card-action menu — Group 3;
- TvProvider query/filter/cache policy — Group 4;
- media-session discovery and session reconciliation — Group 5;
- wallpaper-provider implementation — Group 6;
- preference schema/key names — Group 7;
- HDMI/TvInput vendor launch implementation — Group 8;
- HOME/Accessibility/remote keys — Group 9;
- persistence/icon packs/shortcuts/parental controls — Group 10.

Group 2 correctly stopped at those subsystem boundaries.

---

## 15. Revised canonical home model

With Groups 3–12 folded back into Group 2, the home model is best summarized as:

```text
HomeController / w40
│
├── fixed built-in category registry
│   ├── Inputs            ID 1
│   ├── Favorites         ID 2
│   ├── TV Apps           ID 3
│   ├── Mobile Apps       ID 4
│   ├── Settings          ID 5
│   ├── Display           ID 6
│   ├── Apps              ID 7
│   ├── System            ID 8
│   ├── About             ID 9
│   ├── Watch Next        ID 10
│   ├── Shortcuts         ID 11
│   ├── Now Playing       ID 12
│   ├── Category Folders  ID 13
│   ├── Live TV           ID 14
│   ├── Video             ID 20
│   ├── Music             ID 21
│   └── Games             ID 22
│
├── dynamic TV channel categories
│   └── source type 6 / channel-derived runtime ID
│
├── stable per-category adapters/wrappers
│
├── type-specific cards
│   ├── App
│   ├── InputAction
│   ├── Program
│   ├── WatchNext
│   ├── Shortcut
│   ├── NowPlaying
│   ├── CategoryFolder
│   └── TvInput
│
├── row presentation policy
│   ├── single/multi-row
│   ├── margins
│   ├── card scale/aspect
│   ├── items per line/max rows
│   ├── opacity/background
│   └── hover metadata
│
├── selection/focus policy
│   ├── selected category
│   ├── per-row selected card
│   ├── fixed focus position
│   ├── hide rows above selection
│   └── default category
│
└── left navigation state
    ├── shown
    ├── hidden/revealable
    └── disabled
```

---

## 16. Effect on the Aerial-Rows implementation plan

No Phase 0–11 ordering from Group 12 needs to change.

The audit does strengthen four implementation requirements:

1. **Use namespaced stable string IDs**, not integer/hash identities.
2. **Separate content-row headers from the left navigation rail** in config/state.
3. **Keep per-row selected-card state** and the fixed-row-position/hide-above behaviors as explicit focus policy.
4. **Separate generic input actions from true TvInput Framework sources.**

It also confirms that the current Aerial-Rows `SlidingRows` prototype is conceptually on the correct path, but the row/card identity and focus state need promotion into core models rather than remaining private UI state.

---

## Final assessment

```text
Core Group 2 architecture:        correct
Major missing subsystem:          none
Genuine completeness misses:      2
  - exact built-in category IDs
  - dynamic channel category-ID formula

Factual/wording refinements:      5+
  - hover metadata vs Program Details
  - model-specific stable identities
  - input-action vs Live-TV input distinction
  - exact sidebar/focus preference keys
  - item-type-0 wording

Implementation-plan impact:       refinement only
Redo Group 2 from scratch:        not needed
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 3–12`.**


---

# SOURCE FILE: `group03.md`

SHA-256: `d3beda827648c7612494e1e97c5898255af840efdab91b71fbc352ddfc50afc9`

---

# Projectivy 4.72 — Group 03: Installed apps & card actions

Status: **complete and second-pass verified for app discovery, runtime package updates, app-card model, sorting, category membership, app launch and contextual app actions**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group closes the app-facing path:

`PackageManager/LauncherApps -> ResolveInfo/ActivityInfo -> qb app card -> category membership/sorting -> launch -> long-press j6 actions`.

Boot execution itself is deferred to Group 09; Group 03 only records how an app is selected as the boot target.


## Analysis method used for this group

To make this pass faster and more reliable than the earlier one-by-one MCP workflow, the APK was loaded once for targeted JADX API indexing where possible. Class hierarchy/usage indexes were used to identify high-value obfuscated classes, and only those classes were decompiled. REA/JADX MCP evidence from Groups 1–2 was reused rather than re-querying the same classes.

This exposed app-specific framework subclasses directly; for example `defpackage.nb3` was identified from the hierarchy as a concrete `LauncherApps.Callback`, which would have been expensive to find through repeated full-code keyword scans.

## 1. App discovery

Projectivy does **not** depend on `QUERY_ALL_PACKAGES`.

The central package helper is `defpackage.yb3`.

`yb3.c(tb3)` resolves launcher activities using `ACTION_MAIN` and one of three source modes:

- combined TV + mobile
- `android.intent.category.LAUNCHER`
- `android.intent.category.LEANBACK_LAUNCHER`

The resolved lists are cached by source mode.

`CategoryAppsPreferencesFragment.n0(...)` proves the mapping used by the UI:

- `tb3.w` -> TV apps
- `tb3.v` -> mobile apps

The combined mode concatenates the TV and mobile source lists.

### Deduplication / classification

`yb3.b(tb3, boolean)` filters and deduplicates `ResolveInfo` records before sorting them case-insensitively.

`yb3.x(...)` implements the important mobile-vs-TV rule:

- Projectivy itself can be excluded from the list;
- when building the mobile-app set, an activity/package that also exposes a `LEANBACK_LAUNCHER` activity is excluded from mobile results;
- duplicate packages are collapsed, preferring a default resolve result.

This gives Projectivy a practical TV/mobile split without querying arbitrary installed packages.


## 1A. Runtime install/remove/change handling

Projectivy does not rely on polling the package manager or only refresh apps when the Activity resumes. The running HOME Activity owns a package-change bridge:

- `defpackage.nb3` extends `android.content.pm.LauncherApps.Callback`.
- `MainActivity.onStart()` asynchronously registers this callback with `LauncherApps.registerCallback(...)`, using `ob3.b` as its `Handler`.
- `MainActivity.onStop()` unregisters it with `LauncherApps.unregisterCallback(...)`.

`nb3` translates launcher callbacks into the same internal receiver path:

- `onPackageAdded` -> synthetic `PACKAGE_ADDED` intent
- `onPackageChanged` -> synthetic `PACKAGE_CHANGED` intent
- `onPackageRemoved` -> synthetic `PACKAGE_REMOVED` intent
- `onPackagesAvailable` -> one `PACKAGE_ADDED` event per package
- `onPackagesUnavailable` -> one `PACKAGE_REMOVED` event per package
- `onShortcutsChanged` -> updates the cached shortcut set through `yb3.D(package, shortcuts)`

The receiver/controller is `defpackage.ob3`. It also understands `PACKAGE_REPLACED` and `PACKAGE_FULLY_REMOVED`. Package events are accumulated in a `LinkedHashSet`, the previous refresh job is cancelled, and processing is **debounced by 500 ms** before the collected changes are applied.

For each changed package the worker:

- removes its cached label entry from `yb3.c`;
- updates the installed-state cache `yb3.d` for add/remove events;
- requests the central `w40.z(false)` refresh path when the launcher is in the appropriate active state.

The central refresh path (`u40 -> v40`) contains cache invalidation for the broader launcher-content caches, including:

- `yb3.h = false` (shortcut cache no longer considered initialized);
- `yb3.e.a()` (clear cached launcher/Leanback query lists);
- `yb3.d.clear()` (installed-state cache);
- `yb3.f.a()` (cached package launch intents);
- relevant TV/content caches before category materialization.

This is an important Aerial-Rows design requirement: app rows should be driven by a package-change event stream with debounce and targeted invalidation rather than rescanning all packages on every frame/resume.

## 2. App card model: `qb`

Every app activity is wrapped as `defpackage.qb`, which extends the generic home-card base `t4`.

`qb` stores the original `ActivityInfo` as its backing object.

Important behavior:

- constructor resolves and caches the user-visible activity label through `yb3.g(ActivityInfo)`;
- `z()` returns the package name;
- `n()` resolves the custom artwork URI, with a package/component fallback on supported builds;
- `s()` returns the custom URI when present, otherwise the activity banner/icon resource URI;
- stable card ID comes from the package hash through the inherited `t4` path;
- `y()` is false for an ordinary app, meaning default icon-crop behavior is overridden unless a custom per-card value exists.

### Artwork fallback order

`yb3.e(ActivityInfo, boolean)` shows the ordinary artwork fallback chain:

1. activity banner (unless icon-only mode requested)
2. application banner
3. activity icon
4. application icon

`yb3.f(ActivityInfo)` builds an `android.resource://` URI from the banner resource when allowed by the current display mode, otherwise the icon resource.

## 3. Launch intent resolution

`qb.t(Context, Bundle)` tries:

1. `MAIN + LEANBACK_LAUNCHER`
2. `MAIN + LAUNCHER`

using the exact `ActivityInfo` package/class.

The helper `yb3.n(Context, package)` uses the package-level equivalent:

1. `PackageManager.getLeanbackLaunchIntentForPackage()`
2. `PackageManager.getLaunchIntentForPackage()`

and caches the result.

`t4.f(...)` is the generic execution path. It:

- obtains the card's intent;
- routes protected cards through `ParentalControlCheckActivity` / `pq0.M(...)`;
- starts the activity;
- on `ActivityNotFoundException`, calls the card's optional fallback hook `e(true)` and only retries if that hook reports success;
- displays a user-facing failure message for other launch failures.

For ordinary `qb` app cards, `e(...)` is not overridden (the inherited implementation returns `false`), so a normal app card does **not** blindly retry the same failed launch. The retry mechanism is a generic capability for card types that provide a fallback.

## 4. Per-app usage statistics

`qb.f(...)` updates the card's persistent metadata immediately before calling the generic launch path:

- `w().i()` sets the last-launch timestamp to `System.currentTimeMillis()`;
- `w().h()` increments the launch counter.

The metadata object is `l5`.

Relevant fields/methods:

- `l5.f` / `e()` -> last-used timestamp
- `l5.e` / `f()` -> launch count
- `h()` -> increment count
- `i()` -> set current timestamp

Thus Projectivy's **Last used** and **Most used** sorting modes are backed by its own persisted launcher-use data; they are not Android `UsageStatsManager` estimates.

## 5. Category sorting

The category model `i5` owns sort mode `s` and ordered-card list `u`.

`i5.p(ArrayList)` maps sort values as follows:

- `0` -> manual/custom order, using the IDs stored in `u`
- `1` -> alphabetical, case-insensitive (`t4.A()`)
- `2` -> last used, descending (`t4.u()` -> `qb.u()` -> last-launch timestamp)
- `3` -> most used, descending (`t4.v()` -> `qb.v()` -> launch count)
- `4` -> shuffled/randomized order

For shuffle mode Projectivy can reshuffle the live list and then persist that resulting card order back into `u`.

## 6. Category membership and hidden state

Two integer-ID lists in `i5` are central:

- `u` -> explicit/manual card membership/order where applicable
- `v` -> exclusions/hidden IDs for generated categories where applicable

`CategoryAppsPreferencesFragment.n0()` exposes the switching behavior directly.

For normal editable/custom categories, the app switch tests explicit membership in `u`.

For generated app categories (the relevant built-in TV/mobile modes), the switch is inverted against `v`: cards are present by default and an ID in `v` means excluded/hidden.

App-category identity is package-based in this path: the code hashes `ActivityInfo.packageName` for membership/exclusion checks.

`w40.e(i5, includeConfig)` confirms the same data flow when materializing a category:

- custom/favorites category -> passes `u` as the inclusion/order list;
- generated categories -> passes `v` as the exclusion list.

`w40.i(tb3, include, exclude)` converts discovered `ResolveInfo` entries into `qb` cards through the `pb` model adapter.

## 7. Favorites/custom categories

The generic long-press action fragment treats category type `k == 0` as the favorites/custom-list behavior.

For a card in such a category it exposes **Remove favorite** rather than ordinary Hide.

For non-favorites categories it exposes **Hide** and toggles the selected card ID in the current category's `v` exclusion list.

The generic action screen can also:

- add the card to another editable category;
- move the card to another editable category;
- reorder it within the current category.

Destination lists are constructed dynamically from the current `w40.E` category registry and exclude categories already containing the card where appropriate.

## 8. All Apps / category-app configuration UI

`AllAppsSidePanelActivity` is a thin side-panel host that loads `R.xml.settings_category_apps`.

`CategoryAppsPreferencesFragment` reuses that same screen in two modes:

- `categoryId > 0` -> configure apps belonging to a specific category;
- no category -> All Apps management.

The fragment creates one preference per resolved `qb` card, loads its artwork asynchronously, and sorts the choices alphabetically.

For category configuration it uses `AppCustomSwitchPreference` toggles.

For the All Apps view it uses `AppPreference`; the row has both:

- normal click -> launch the app;
- long click -> open the card-action fragment `j6` with the card's `actionId`.

## 9. Long-press / contextual action architecture

The actual contextual screen is `defpackage.j6`, backed by `R.xml.settings_action`.

This is a **generic card-action fragment**, not an app-only popup. It adapts the menu based on the runtime subtype of `t4`.

For `qb` application cards it enables or constructs the following app-relevant actions.

### Launch

`action_exec` executes the selected card through `t4.f(...)`.

### App info

`action_app_settings` builds:

`android.settings.APPLICATION_DETAILS_SETTINGS` with `package:<packageName>`

and routes it through `SettingsIntentActivity`.

### Uninstall

`action_app_uninstall` builds:

`android.intent.action.UNINSTALL_PACKAGE` with `package:<packageName>`

and `FLAG_ACTIVITY_NEW_TASK`.

This is the exact implementation path needed for Aerial-Rows' requested long-press **Uninstall** action.

### Start at boot

`action_start_at_boot` is enabled for ordinary apps (`qb`) and supported source actions (`z52`).

Selecting it writes `x0().z()` through `hj3.P.h(...)`. `hj3.P` is the preference whose concrete key is **`power_autostart_source`**.

For `qb`, `z()` is the package name.

Therefore Group 03 proves the boot preference stores the chosen app/source identity. The boot receiver's later use of this value belongs to Group 09.

### Parental control

The menu conditionally exposes:

- Add parental control
- Remove parental control

based on whether the stable card ID already appears in the parental-control set.

The action launches `ToggleParentalControlActivity`, passing the stable unique ID and routing through the parental PIN gate.

### Hide / favorite / move

Depending on the current category type:

- `action_card_hide`
- `action_card_remove_favorite`
- `action_card_move`
- `action_card_add_to`
- `action_card_move_to`

are enabled or disabled dynamically.

Now Playing cards (`c73`) are deliberately excluded from ordinary hide/move/favorite operations.

### Custom title

`action_title` writes a per-card custom title into the card's `l5` metadata object.

The home/card title getter `t4.A()` returns that custom title when available, otherwise the original app/activity label.

After editing, Projectivy refreshes the card/category rather than rebuilding the full launcher.

### Custom icon and crop

The action screen supports:

- change card icon via the shared image/icon picker flow;
- per-card crop toggle.

The package name and current card title are passed into the icon/image-picker path so icon packs and custom assets can resolve the target application correctly.

### App shortcuts

When the platform supports LauncherApps shortcuts, the action screen adds a dynamic **Shortcuts** section for a `qb` card.

Projectivy uses `LauncherApps`, `ShortcutQuery` and `ShortcutInfo`; shortcut launch uses `LauncherApps.startShortcut(...)`.

Pinned shortcut state is handled independently from ordinary app-card visibility.

## 10. TV/mobile filtering details

A useful implementation detail from `yb3.x(...)` is that Projectivy's mobile category is not simply every `LAUNCHER` result.

When processing mobile results it checks the TV/Leanback list and rejects an ordinary launcher result if the same package exposes a Leanback launcher entry. This prevents the same application appearing in both built-in TV and mobile categories.

Results are also collapsed by package, with the default resolve entry preferred when duplicates occur.

## 11. Home repository/controller boundary

`w40` is the central category/content controller reached by both the home screen and contextual actions.

Group 03-relevant methods include:

- `i(tb3, include, exclude)` -> discover/materialize app cards
- `e(i5, boolean)` -> derive cards for a category from its type/config
- `r(i5, t4)` -> remove/move mutation entry point
- `s(t4, boolean)` -> card reordering/mutation entry point
- `y(cardId, categoryId)` -> membership mutation entry point
- `o(i5, Runnable)` -> refresh one category
- `t/u/w()` -> persist/refresh controller state (exact storage implementation remains Group 10)

This controller is why card actions can update one category and preserve focus instead of forcing a full home-screen reload.

## 12. Recommended Aerial-Rows equivalent

Do not model this as `List<AppInfo>` only.

A closer independent architecture is:

```text
AppDiscoveryRepository
  - query TV launchables
  - query mobile launchables
  - deduplicate / TV-over-mobile precedence

AppHomeItem : HomeItem
  - stableId
  - packageName
  - componentName
  - label
  - artwork
  - launchIntent resolver
  - usage metadata
  - custom appearance metadata

CategoryConfig
  - sourceType
  - explicitOrderIds
  - excludedIds
  - sortMode

CardActionResolver
  - launch
  - app info
  - uninstall
  - start at boot
  - parental gate
  - hide / favorite
  - add / move category
  - rename
  - icon override / crop
  - shortcuts
```

For Android package visibility, keep explicit launcher/Leanback queries rather than adding broad `QUERY_ALL_PACKAGES` unless a genuinely separate requirement appears.

## Group 03 conclusions

1. App discovery is launcher-intent based, cached, deduplicated, and explicitly split into TV/mobile sources.
2. TV apps take precedence over mobile duplicates from the same package.
3. `qb` is the concrete app-card model; `t4` is the common action-card base.
4. App launch prefers Leanback and falls back to ordinary Launcher.
5. Projectivy maintains its own launch timestamp and launch count, enabling deterministic Last Used / Most Used sorting.
6. Category app membership uses package/card IDs in explicit inclusion/order (`u`) and exclusion (`v`) lists depending on category type.
7. Sorting modes are manual, alphabetical, last-used, most-used and shuffle.
8. Long press opens a generic `j6` action screen with app-specific operations including **App Info, Uninstall, Start at boot, hide/favorite/move, parental controls, rename, icon override/crop and shortcuts**.
9. App-info and uninstall intent implementations are now known exactly.
10. Start-at-boot selection is stored under `power_autostart_source`; actual boot-time execution is intentionally deferred to Group 09.
11. Runtime app changes are handled with a `LauncherApps.Callback`, a 500 ms debounced internal receiver, cache invalidation and targeted `w40` refreshes.

## Deferred to later groups

- TvProvider/channel program discovery: Group 04
- Media-session actions: Group 05
- icon-pack internals and persisted asset storage: Groups 06/10
- exact SharedPreferences/DataStore/Room serialization for `i5`, `l5`, `w40`: Group 10
- boot receiver execution and HOME/accessibility interaction: Group 09

## Next group

**Group 04 — Android TV Channels & Watch Next**

Targets:

- TvProvider/TvContract queries
- channels and preview-program models
- Watch Next model (`b15`) vs channel-program model (`ck3`)
- content observers / refresh triggers
- progress, artwork, metadata and deep-link handling
- provider/category mapping
- details and preview-video flow


---

# SOURCE FILE: `group03_postclosure_audit.md`

SHA-256: `b7bbc6d739a90bc64f3c52f8d56551d670ea9a126d8769726fe02734923339f3`

---

# Group 3 post-closure audit — Installed apps & card actions

Status: **POST-CLOSURE AUDITED after Groups 4–12**  
Original Group 3 SHA-256: `d3beda827648c7612494e1e97c5898255af840efdab91b71fbc352ddfc50afc9`

## Executive conclusion

The original Group 3 report is strong and its main architecture remains correct.

No later group revealed a missing installed-app subsystem or invalidated the central flow:

```text
PackageManager / LauncherApps
        ↓
TV/mobile app discovery
        ↓
qb app card
        ↓
category membership/sorting
        ↓
generic t4 launch path
        ↓
generic j6 card-action UI
```

There is, however:

- **1 factual correction**;
- several important precision/completeness upgrades;
- two design implications that matter directly to Aerial-Rows.

The main factual correction is the boot path: `StartUpBootReceiver` does **not** consume `power_autostart_source` in Projectivy 4.72. The actual consumer is the `dq3` autostart coordinator described in Group 9.

---

# 1. Factual correction — boot target consumer

Group 3 states:

> “The boot receiver's later use of this value belongs to Group 09.”

That wording should be corrected.

Group 9 proved that `StartUpBootReceiver.onReceive()` is effectively a no-op for `BOOT_COMPLETED`.

The actual runtime path is:

```text
power_autostart_source
        ↓
dq3.z(context, bootWindow)
        ↓
dq3.b(context)
        ↓
app / TV input / last input / ambient target
```

Possible triggers include:

```text
MainActivity early after boot
AccessibilityService early after boot
resume-from-standby observer
```

The receiver only participates indirectly through shared startup state/timing; it is **not** the launcher of the selected app.

### Corrected canonical statement

Group 3 proves that app/source selection is persisted in:

```text
power_autostart_source
```

Group 9 proves that the **`dq3` startup coordinator**, not `StartUpBootReceiver`, consumes it.

---

# 2. Precision upgrade — “Start at boot” is one global target, not a per-app toggle

The Group 3 action name can sound like Projectivy stores a Boolean on every app.

It does not.

Selecting **Start at boot** writes that card/source identity into one global setting:

```text
power_autostart_source
```

So the model is:

```text
one selected startup target
```

not:

```text
many apps each independently marked launch-on-boot
```

For an ordinary `qb` application:

```text
qb.z() -> package name
```

and that package string becomes the selected target.

Group 9 later proved the same setting can instead contain:

- an explicit TV input;
- `-1` for last-used input;
- `-100` for ambient/screensaver;
- application package names.

### Aerial-Rows implication

Implement:

```text
StartupTarget?
```

as one typed global target.

Do **not** add a `launchAtBoot: Boolean` field to every app card.

---

# 3. Precision upgrade — app identity is package-level even though the backing card is activity-level

`qb` is backed by an exact `ActivityInfo`.

Its launch path uses the exact activity:

```text
ActivityInfo.packageName
ActivityInfo.name
```

and attempts:

```text
MAIN + LEANBACK_LAUNCHER
    ↓ fallback
MAIN + LAUNCHER
```

before setting that exact class.

However, the inherited stable card ID is:

```text
ActivityInfo.packageName.hashCode()
```

and discovery also collapses results by package.

Therefore Projectivy deliberately combines:

```text
activity-specific backing/launch target
        +
package-level identity
```

This means ordinary app rows effectively represent **one card per package**, even when a package exposes several launcher activities.

That interaction was present in the Group 3 evidence but was not stated explicitly.

### Aerial-Rows improvement

Use an identity that retains the component:

```text
app:<package>/<activity>
```

while still allowing optional package-level deduplication policy.

That avoids:

- Java hash collisions;
- losing multi-activity identity;
- tying customizations to a 32-bit hash.

---

# 4. Precision upgrade — launch statistics record attempts, not confirmed successful launches

`qb.f(...)` performs:

```text
w().i()   // last-launch timestamp
w().h()   // increment counter
```

**before** calling:

```text
super.f(...)
```

which actually resolves/starts the target.

Therefore:

```text
last-used timestamp
launch count
```

are updated on a **launch attempt**, even if the eventual launch fails.

The original report already noted the order, but the consequence should be explicit.

### Aerial-Rows design choice

We can intentionally improve this by deciding whether usage means:

```text
launch attempted
```

or:

```text
launch successfully dispatched
```

For deterministic UX, I recommend recording only after `LaunchDispatcher` has successfully handed off the Intent/shortcut/input action.

That is a deliberate behavioral improvement, not a requirement to clone Projectivy's exact accounting quirk.

---

# 5. Completeness upgrade — full app artwork priority

Group 3 correctly documented the ordinary Android artwork fallback:

```text
activity banner
application banner
activity icon
application icon
```

Group 10 later closed the layers above it.

The complete Projectivy app-art resolution is effectively:

```text
per-card custom artwork
        ↓
global icon-pack mapping
        ↓
ordinary Android app artwork:
    activity banner
    application banner
    activity icon
    application icon
```

So Group 3's chain should be understood as the **base Android fallback**, not the entire launcher-level resolution stack.

### Aerial-Rows implication

Keep three concerns separate:

```text
discovered original art
global icon-pack resolver
per-card override
```

with per-card override taking priority.

---

# 6. Completeness upgrade — `l5` is one unified customization + usage record

Group 3 encountered `l5` through:

- last launch;
- launch count;
- custom title;
- custom artwork;
- crop.

Group 10 proved these are not independent stores.

`l5` is one serialized per-card model containing:

```text
stable card/action ID
custom artwork URI
nullable crop override
custom title
launch count
last-launch timestamp
```

and the collection is persisted under:

```text
action_customizers_list
```

This means Projectivy deliberately separates:

```text
discovered package/app metadata
        ≠
persistent user card customization/usage metadata
```

Package rescans therefore do not inherently erase presentation overrides or usage state.

### Crop behavior refinement

The crop field is effectively tri-state:

```text
null  -> use card-type default
true  -> force crop
false -> force non-crop
```

For ordinary `qb` cards, the default fallback (`y()`) is false.

---

# 7. Important clarification — Hide is category-local, not a global “hide this app everywhere”

This is one of the most important implementation implications.

Group 3 correctly observed that generated categories use `i5.v` as an exclusion list.

The contextual **Hide** action mutates the current category's exclusion state.

So Projectivy's app visibility model is principally:

```text
card visible/hidden within category
```

rather than one global:

```text
hiddenPackages
```

For custom/favorites-style categories, the equivalent action is generally **Remove favorite** / remove membership instead of Hide.

### Why this matters for Aerial-Rows

The current Aerial-Rows development model has a global:

```text
hidden: Set<String>
```

which hides an app across rows.

That is **not equivalent** to Projectivy's row/category-specific exclusion behavior.

Recommended final model:

```text
AppConfig
  global suppression (optional)

RowConfig
  included card IDs
  excluded card IDs
  explicit order
```

Then long-press **Hide** can mean “hide from this row/category” while an explicit separate action can mean “hide globally” if we want that feature.

This should be reflected when Phase 1/3 of the Group 12 implementation plan is executed.

---

# 8. Precision upgrade — app shortcuts actually span two related but separate systems

Group 3 correctly found a dynamic **Shortcuts** section on an app's `j6` action screen.

Group 10 later made the whole shortcut architecture clear.

There are two distinct concepts:

## A. App-published shortcuts surfaced from an app card

For an app package, Projectivy queries Android `LauncherApps` shortcuts and presents them through the app action UI.

## B. Pinned shortcut cards as launcher content

Projectivy is also a genuine shortcut host:

```text
PinShortcutActivity
LauncherApps.PinItemRequest
LauncherApps.hasShortcutHostPermission()
ShortcutQuery
LauncherApps.startShortcut()
```

Pinned shortcuts can exist as their own home cards:

```text
p94
item type 7
built-in Shortcuts row/category ID 11
```

The shortcut card's local ID uses:

```text
shortcutInfo.getId().hashCode()
```

while package/user context lives around it.

### Additional action behavior

For a shortcut card, the generic action logic can pin/unpin it through:

```text
LauncherApps.pinShortcuts(...)
```

instead of merely toggling an ordinary category exclusion.

### Aerial-Rows improvement

Use:

```text
shortcut:<package>:<user/profile>:<shortcutId>
```

as the stable identity.

---

# 9. Precision upgrade — parental control is a generic LaunchGate, not merely an app action

Group 3 correctly found:

```text
Add parental control
Remove parental control
```

in the generic action UI and the wrapper around protected launches.

Group 10 established the broader policy.

Protected entities are stored by stable card/action identity, and the parental system also supports:

- protected Settings;
- time-window / partial protection;
- overnight schedules;
- authentication grace sessions;
- primary and emergency/recovery code paths.

Most importantly, protection is enforced around the generic launch path.

So the canonical model is:

```text
card action toggles protection policy
        ↓
all later launch paths
        ↓
generic LaunchGate
```

not:

```text
app card checks PIN itself
```

This reinforces the Group 12 architecture:

```text
HomeCard / shortcut / TV program / input / autostart
        ↓
LaunchGate
        ↓
LaunchDispatcher
```

---

# 10. Precision upgrade — package events also underpin shortcut/icon-related cache correctness

Group 3 correctly documented:

```text
LauncherApps.Callback
        ↓
nb3
        ↓
ob3
        ↓
500 ms debounce
        ↓
cache invalidation / w40 refresh
```

Later work shows why this boundary is even more valuable:

- shortcut state changes arrive through `onShortcutsChanged`;
- icon-pack package changes need cache invalidation;
- package replacements can change banners/icons/components;
- TV/mobile classification can change after app updates.

So the package-change bridge should be regarded as a **shared package/content invalidation subsystem**, not merely “refresh the app list.”

That strengthens the recommendation to replace Aerial-Rows' current coarse Activity receiver with a repository-owned `LauncherApps.Callback`.

---

# 11. Negative finding strengthened — normal app actions do not contain a real Force Stop

Group 3 mapped the normal app-card action set and did **not** find a privileged force-stop action.

Group 11 later found separate privileged utilities such as:

- package freeze/unfreeze;
- stock-launcher freeze/unfreeze;
- GApps removal;
- installer/device maintenance tools.

Those are separate maintenance tools and should not be confused with ordinary app-card context actions.

Therefore:

> The normal Projectivy app-card menu includes App Info and Uninstall, but the later privileged maintenance utilities are not evidence of a hidden normal “Force Stop” app-card action.

For Aerial-Rows, the existing `killBackgroundProcesses()` “Close” action is our own best-effort behavior, not something required by the Projectivy reconstruction.

---

# 12. Precision upgrade — package visibility conclusion is now independently reinforced

Group 3 concluded that Projectivy does not require:

```text
QUERY_ALL_PACKAGES
```

Groups 1 and 12 reinforce that conclusion.

Projectivy relies on launcher-intent visibility/query patterns and explicit manifest queries.

Aerial-Rows currently declares `QUERY_ALL_PACKAGES`, so the final implementation should attempt to remove it and rely on:

```text
MAIN + LEANBACK_LAUNCHER
MAIN + LAUNCHER
explicit <queries>
LauncherApps where appropriate
```

unless a later concrete feature proves broad visibility is genuinely necessary.

---

# 13. Precision upgrade — TV/mobile dedup is package-first by design

Group 3 already described Leanback precedence.

The deeper implication is:

```text
TV query results
        ↓
mobile query results checked against TV packages
        ↓
same package rejected from mobile set
```

and results within a source are also collapsed by package, preferring a default ResolveInfo where duplicates exist.

So the built-in TV and mobile categories are not two independent activity lists.

They are a **partition of package-level app identity**, with TV/Leanback taking precedence.

For Aerial-Rows we can preserve the default UX while retaining component identity internally.

---

# 14. Things Group 3 correctly deferred — not misses

The following were correctly left to later groups:

- TV Provider channel/program cards — Group 4;
- media sessions / Now Playing — Group 5;
- wallpaper/provider focus behavior — Group 6;
- exact preference names/UI — Group 7;
- input launch semantics — Group 8;
- actual autostart coordinator — Group 9;
- persistence, icon packs, pinned shortcuts and parental policy — Group 10;
- root/device maintenance activities — Group 11.

Those are subsystem boundaries, not holes in Group 3.

---

# 15. Revised canonical installed-app model

With later groups folded back in, Projectivy's app architecture is best summarized as:

```text
AppDiscoveryRepository
│
├─ query MAIN + LEANBACK_LAUNCHER
├─ query MAIN + LAUNCHER
├─ package-level dedup
├─ TV-over-mobile precedence
├─ cached ResolveInfo lists
└─ LauncherApps.Callback
      ├─ package add/change/remove
      ├─ package available/unavailable
      └─ shortcut changes
             ↓
          ~500 ms debounce
             ↓
        targeted cache invalidation

AppCard / qb
│
├─ exact ActivityInfo launch backing
├─ package-level stable ID/hash
├─ activity/app artwork fallback
├─ customizer link / l5
└─ generic t4 LaunchGate path

CardCustomizer / l5
│
├─ custom artwork
├─ nullable crop override
├─ custom title
├─ launch count
└─ last launch
     persisted in action_customizers_list

Full artwork resolution
│
├─ per-card override
├─ icon-pack mapping
└─ original banner/icon

Category relationship
│
├─ explicit inclusion/order (u)
└─ generated-row exclusion (v)

Context actions / j6
├─ launch
├─ App Info
├─ uninstall
├─ select global startup target
├─ parental protection
├─ hide from current generated row
├─ remove/add/move category membership
├─ reorder
├─ rename
├─ icon/crop
└─ app shortcuts

Shortcut subsystem
├─ app shortcut discovery
├─ pinned shortcut host
├─ shortcut home cards
└─ startShortcut/pinShortcuts
```

---

# 16. Aerial-Rows implementation implications

The Group 12 implementation order does not need restructuring, but Group 3's audit strengthens these requirements:

1. **App identity should retain component information.**
2. **Use namespaced stable string IDs instead of Java `hashCode()`.**
3. **Separate discovery metadata from persistent card overrides/usage.**
4. **Row/category visibility must not be represented only by one global hidden-package set.**
5. **Startup selection is one typed global target, not per-app booleans.**
6. **All launches should converge through LaunchGate + LaunchDispatcher.**
7. **Shortcuts are first-class cards/content, not merely app submenu commands.**
8. **Package/shortcut observation belongs in the repository, not MainActivity.**
9. **Icon-pack resolution sits between per-card override and original app artwork.**
10. **Decide explicitly whether usage tracks attempted or successful launches.**

---

# Final assessment

```text
Core Group 3 architecture:          correct
Missing major subsystem:            none
Factual corrections:                1
  - boot target is consumed by dq3, not StartUpBootReceiver

Important completeness upgrades:
  - full icon/artwork priority
  - l5 persistence/unified role
  - shortcut-host architecture
  - generic parental LaunchGate policy
  - single global startup target semantics

Important design refinement:
  - Projectivy Hide is category-local;
    Aerial-Rows currently has global hidden packages

Need to redo Group 3:                no
Effect on Group 12 plan:             refinement, especially Phase 1/3
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 4–12`.**


---

# SOURCE FILE: `group04.md`

SHA-256: `1ef505a55393f1952896cdd6c11e398df3e8e380593360662ad1617a71377de3`

---

# Projectivy 4.72 — Group 04: Android TV Channels & Watch Next

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group closes Projectivy's Android TV Provider content pipeline:

- channel discovery;
- dynamic channel rows;
- Preview Program queries;
- Watch Next queries;
- provider/app filtering;
- configurable per-provider/per-channel limits;
- caching and refresh;
- program-card models;
- progress/badges/artwork;
- preview video;
- deep-link launch;
- Program Details;
- channel visibility / `requestChannelBrowsable` behavior.

Live-TV/input tuning that is not part of Preview Channels is left for Group 08.

---

# 1. Faster analysis workflow used for this group

Instead of repeatedly asking JADX MCP to rescan all 7,395 classes, Group 04 used one raw JADX bytecode pass over Projectivy/obfuscated candidates.

The scanner inspected **6,579 candidate classes in one APK load** and reduced the TV-provider search to **21 classes** containing references to concepts such as:

- `TvContract`
- `WatchNextProgram`
- `PreviewProgram`
- `ContentObserver`
- `TvContentRating`
- TV provider content URIs

High-value classes were then decompiled in small batches, with smali used only where JADX control-flow reconstruction was ambiguous.

This workflow should be reused for later groups.

---

# 2. Core architecture

The TV content path is:

```text
Android TV Provider
   |
   +-- channel table
   |      -> e70.b()
   |      -> m60 Channel model
   |      -> i5 dynamic category/row
   |
   +-- preview_program table
   |      -> e70.c(channelId)
   |      -> bk3 PreviewProgram value object
   |      -> ck3 Program card
   |
   +-- watch_next_program table
          -> w40.d(type=5)
          -> d15 WatchNextProgram value object
          -> b15 Watch Next card (subclass of ck3)
```

The same generic home/category system from Group 02 therefore renders apps, channels and programs. Projectivy does not maintain a second independent home renderer for TV content.

---

# 3. TV-provider availability gate

`e70.e()` verifies the platform/device boundary before provider content is used.

The important checks are:

- Android/platform capability gates (`u9.h`, `ed4.d`);
- `android.permission.READ_TV_LISTINGS` permission.

`e70.d()` then reads the user-facing setting generated by `hj3.y`.

That setting is created as:

```text
general_enable_tv_channels
```

Its default value is based on `e70.e()` — i.e. availability/permission.

Both channel discovery and Preview/Watch Next content require the capability gate **and** this setting.

Equivalent Aerial-Rows design:

```text
TvProviderAvailability
    platform supported
    + permission available
    + user enabled
```

---

# 4. Channel discovery

Projectivy queries:

```text
content://android.media.tv/channel
```

via `ur4.a`.

The channel projection used by `e70.b()` is:

```text
_id
package_name
type
display_name
description
app_link_intent_uri
internal_provider_id
```

Each result is converted to `m60`, a light wrapper around `ContentValues`.

`m60` exposes at least:

- `_id` through `a()`;
- `package_name` through `b()`.

## Channel filtering

`e70.b()` keeps channels whose `type` is:

```text
TYPE_PREVIEW
```

and deliberately excludes a provider package ending in:

```text
.tvrecommendations
```

There is also duplicate suppression based on the combination of:

```text
package_name + internal_provider_id
```

when both values are available.

The resulting channel list is cached.

---

# 5. Dynamic home rows for channels

Each discovered `m60` is converted into an `i5` category through the special channel constructor:

```text
i5(m60 channel)
```

Important resulting properties:

- row/category type `k = 6`;
- category ID derived from the channel ID (`channelId + 2000`, cast to int);
- default card scale around `1.2`;
- channel-themed icon/colour;
- title from `display_name`;
- backing `m60` retained in `i5.g`;
- logo URI generated with `TvContract.buildChannelLogoUri(channelId)`;
- initially not treated like a fixed built-in row.

`v40` rebuilds the dynamic category list from `e70.b()` and retains/reuses channel-row objects in a channel-ID keyed cache (`w40.G`) when possible.

So channel rows have stable identities across refreshes rather than always being recreated as unrelated objects.

---

# 6. Manage Channels UI

The Manage Channels settings implementation is anchored by `g70`.

It:

1. reads the current home-category list (`w40.E`);
2. groups channel rows by provider package/app label;
3. sorts provider groups case-insensitively;
4. exposes visibility/preferences for those rows;
5. also handles the Watch Next category separately.

This confirms that Projectivy treats a TV provider's Preview Channels as user-manageable home rows, grouped under the app that owns them.

---

# 7. Making a channel browsable

When the user enables visibility for a channel-backed category, `CategoryPreferencesFragment` checks `i5.g` and calls:

```text
z62.h(context, channelId)
```

On Android 8.0+ that resolves to:

```text
TvContract.requestChannelBrowsable(context, channelId)
```

So Projectivy does not merely make its own row visible. It also asks Android to mark/request that Preview Channel as browsable at the platform TV-provider level.

This should be preserved in an independent implementation where supported.

---

# 8. Preview Program repository

`e70.c(long channelId)` is the central Preview Program query.

It returns empty content when:

- the supplied ID is negative;
- TV-provider capability/permission is unavailable;
- TV channels are disabled by the Projectivy setting.

Results are cached **per channel ID** in `e70.c`'s map-like cache.

## URI behavior

For a specific channel:

```text
content://android.media.tv/preview_program?channel=<channelId>
```

For `channelId == 0`, Projectivy performs a general Preview Program query against:

```text
content://android.media.tv/preview_program
```

and then applies its own channel filtering/grouping logic.

## Sort order

Preview Programs are requested using:

```text
weight DESC
```

---

# 9. Preview Program projection

The shared program projection contains:

```text
package_name
title
short_description
poster_art_uri
thumbnail_uri
channel_id
weight
browsable
intent_uri
preview_video_uri
logo_uri
poster_art_aspect_ratio
poster_thumbnail_aspect_ratio
season_display_number
episode_display_number
duration_millis
last_playback_position_millis
type
author
genre
release_date
_id
```

`e70.f(Cursor)` converts this into a `bk3` (`PreviewProgram`) value object backed by `ContentValues`.

---

# 10. Preview Program filtering

A Preview Program is only accepted when at least the following are true:

```text
browsable == 1
intent_uri != null
```

For the global query (`channelId == 0`), Projectivy additionally restricts programs to channel IDs discovered by `e70.b()`.

It also deduplicates repeated program entries using a key derived from:

```text
intent_uri + channel_id
```

rather than blindly displaying every provider row returned by the ContentResolver.

---

# 11. Configurable Preview Programs per channel

Projectivy has a dedicated setting:

```text
action_channel_general_programs_per_channel
```

represented by:

```text
hj3.b2
```

`e70.c()` maintains a count keyed by `channel_id` while constructing the global list.

When the setting is greater than zero, additional programs from a channel are skipped once that channel's configured limit is reached.

A value of zero disables the limit.

This is an important Projectivy behavior to reproduce because it stops one provider/channel dominating the home screen.

---

# 12. Watch Next query

The Watch Next content URI is explicitly defined as:

```text
content://android.media.tv/watch_next_program
```

in `wr4.a`.

The home controller `w40.d()` handles built-in category type `5` as Watch Next.

The query uses the same general program projection as Preview Programs.

## Sort order

Watch Next is requested with:

```text
last_engagement_time_utc_millis DESC
```

so the most recently engaged entries are returned first.

---

# 13. Watch Next filtering

As with Preview Programs, Projectivy requires:

```text
browsable == 1
intent_uri != null
```

before a Watch Next entry is accepted.

`e70.g(Cursor)` maps accepted rows into `d15`, Projectivy's `WatchNextProgram` value object.

The Watch Next list itself is cached separately in `e70.a`.

---

# 14. Configurable Watch Next entries per app

Projectivy exposes:

```text
action_watch_next_programs_per_app
```

through:

```text
hj3.a2
```

The Category Apps settings screen provides a slider for it, with a displayed "No limit" state when set to zero.

During Watch Next construction, `w40` keeps a count keyed by the program's `package_name`.

Smali verification confirms that once a package has reached the configured limit, further Watch Next entries from that app are skipped.

Therefore Projectivy deliberately prevents a single streaming app from filling the entire Watch Next row.

---

# 15. Watch Next provider selection / exclusions

For the Watch Next category (`i5.k == 5`), `CategoryAppsPreferencesFragment` enumerates provider apps represented by discovered TV channels.

Provider apps are shown as switches.

The category's exclusion list (`i5.v`) stores package-hash identities, and the switch state is effectively:

```text
enabled = packageHash not in exclusion list
```

This gives Watch Next both:

- a global per-app item limit;
- per-provider inclusion/exclusion.

---

# 16. Program value-object hierarchy

The provider-side data model is:

```text
yu / xu       generic TV-program ContentValues wrapper
   |
   +-- bk3    PreviewProgram
   +-- d15    WatchNextProgram
```

`xu` exposes shared program behavior such as `duration_millis`.

Preview and Watch Next therefore share almost all metadata handling.

---

# 17. Home card hierarchy

The corresponding home-card hierarchy is:

```text
t4
 |
 +-- ck3      generic TV program card
       |
       +-- b15 Watch Next specialization
```

A normal channel Preview Program is represented as a `ck3` card.

A Watch Next entry is represented as `b15`, which subclasses `ck3` but uses Watch Next-specific identity behavior.

This matches the Group 02 presenter map:

- item type `5` -> Watch Next (`b15`);
- item type `6` -> channel Preview Program (`ck3`);
- both render through the same program presenter (`ek3`).

---

# 18. Program stable identity

For ordinary `ck3`, stable identity is based primarily on the program's `intent_uri` when available.

`b15` overrides identity using a Watch Next-specific helper (`q82.b(xu)`).

The practical architectural point is that Watch Next identity is not simply inherited from generic Preview Programs; Projectivy deliberately specializes it.

---

# 19. Program card artwork fallback

`ck3.s()` resolves display artwork in this order:

```text
custom/overridden card artwork (through normal t4 path)
    -> thumbnail_uri
    -> program poster-art URI
    -> logo_uri
```

The presenter separately understands poster/thumbnail aspect-ratio metadata.

This prevents a program from becoming visually empty when a provider omits one artwork field but supplies another.

---

# 20. Provider logo badge

`ek3` reads `logo_uri` from the program model.

Depending on the relevant appearance setting, it loads that URI into the program-card badge image.

Badge visibility is also focus-aware.

So Projectivy can show the streaming/provider brand on program cards independently of the main poster artwork.

---

# 21. Playback progress

When progress display is enabled, `ek3` calculates:

```text
last_playback_position_millis / duration_millis * 100
```

and sets the program-card progress indicator.

Invalid/non-positive positions produce zero progress.

This applies to the shared program presenter and therefore supports both Preview/Watch Next program models where providers expose progress metadata.

---

# 22. Program descriptive metadata

`ck3.o()` builds the hover/details summary dynamically from the provider `type` and available fields.

Depending on program type, it composes combinations of:

- release date;
- genre;
- duration;
- season;
- episode;
- author;
- short description.

Empty values are removed and the short description is placed on a separate line when present.

So Projectivy is not merely displaying `short_description` verbatim.

---

# 23. Program deep-link launching

`ck3.t(Context, Bundle)` performs provider deep-link launch as follows:

```text
read intent_uri
 -> Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
 -> force Intent package to program package_name
 -> pass through generic t4 launch preparation
```

If the URI cannot be parsed or no valid intent exists, it returns no launch intent rather than guessing a package launcher activity.

This is a useful security/behavioral detail for Aerial-Rows: provider program cards should honor their declared deep link and constrain it back to the owning package.

---

# 24. Preview video on focus

The shared presenter `ek3` reads:

```text
preview_video_uri
```

When a card becomes focused:

- existing preview playback is cancelled/replaced;
- if the URI exists, a lifecycle-bound async playback task is started;
- the card can transition from static artwork into video preview.

When focus leaves:

- preview playback is stopped/cancelled;
- the card is restored to its non-preview state.

This is therefore tied directly to card focus, not started for all visible cards.

The exact Media3/player implementation remains part of the rendering/media plumbing already identified in Group 02 and can be further cross-referenced in Group 05 where necessary.

---

# 25. Wallpaper-provider focus event

On program focus, `ek3.n()` publishes a typed wallpaper-provider event:

```text
Event.ProgramCardFocused
```

including program information such as:

- title;
- type;
- artwork URI;
- artwork aspect ratio.

This creates an important cross-subsystem bridge:

```text
TV program focus
   -> wallpaper provider event
   -> dynamic background provider
```

Group 06 should reuse this finding when integrating Aerial/background behavior.

---

# 26. Program Details action

All generic cards expose actions through `j6`.

For a program card, `ck3.E()` returns true, enabling:

```text
action_program_details
```

That routes into:

```text
ProgramDetailsPreferencesFragment
```

The details screen displays available metadata such as:

- release date;
- genre;
- duration;
- season;
- episode;
- author;
- notes/description.

---

# 27. Lazy `long_description` lookup

The Program Details screen first displays the cached `long_description`, falling back to `short_description`.

It then asynchronously launches `d70` for an additional provider lookup using the program `_id`.

`d70` chooses the provider URI based on card type:

```text
Watch Next card
  -> content://android.media.tv/watch_next_program/<id>

Preview Program card
  -> content://android.media.tv/preview_program/<id>
```

and queries only:

```text
long_description
```

If a non-empty result is returned, the notes preference is updated.

This proves that Program Details can fetch richer metadata lazily instead of making every home-row query expensive.

Recommended Aerial-Rows pattern:

```text
Home card query = light/fast projection
Details screen = lazy extended metadata lookup
```

---

# 28. Caching

`e70` maintains separate caches for:

- Watch Next (`e70.a`);
- channel list (`e70.b`);
- Preview Programs grouped/cached by channel (`e70.c`).

This avoids running ContentResolver queries every time a row rebinds.

---

# 29. Refresh / invalidation behavior

The home refresh coroutine (`v40`) clears TV-provider caches when a refresh is required:

```text
channel cache.clear()
Watch Next cache.clear()
Preview Program per-channel cache.clear()
```

It then re-queries channels and rebuilds/reconciles dynamic channel categories.

`w40` records the last refresh timestamp and uses:

```text
C = MainFragment.R1 / 2
```

with:

```text
MainFragment.R1 = 60000 ms
```

so a timed refresh request is gated to roughly **30 seconds**.

There are also forced refresh paths (`w40.z(false)`) used when changes are known and a stale-cache delay is undesirable.

This is an important finding: Projectivy does **not** appear to register a dedicated `ContentObserver` directly on the three TV-provider content URIs. The bytecode-wide observer scan found ContentObservers, but the matching observer classes belong to other subsystems such as the clock/accessibility/media plumbing.

Instead, Projectivy relies on its controlled home refresh/invalidation pipeline for these provider caches.

For Aerial-Rows we can reproduce this behavior exactly, or improve it by combining the same cache strategy with optional provider observers where OEM behavior is reliable.

---

# 30. Extra use of Preview Programs as wallpaper content

The TV-provider repository is reused outside rows.

`rp3` asks `e70.c(0)` for global Preview Programs and converts suitable poster-art programs into wallpaper-provider `Wallpaper` objects.

It filters for appropriate artwork aspect classes and rejects short/ordinary playback durations that do not fit its background use case.

This confirms that Projectivy views Preview Program artwork as a reusable content source, not only a row/card source.

That is directly relevant to Group 06's wallpaper/provider architecture.

---

# 31. What is stock Android vs Projectivy-specific

## Android/platform concepts

- `TvContract`
- `ContentResolver`
- TV Provider channel/Preview Program/Watch Next tables
- `requestChannelBrowsable`
- standard program metadata fields

## Projectivy-specific behavior

- `e70` repository/caches;
- filtering of system recommendation channels;
- provider/channel deduplication;
- dynamic conversion of channels into `i5` home rows;
- per-channel Preview Program limit;
- per-app Watch Next limit;
- provider exclusion switches;
- Projectivy row/card stable identities;
- program details formatting;
- lazy long-description query;
- focus-triggered preview playback integration;
- wallpaper-provider focus events;
- timed/forced cache-refresh strategy.

This distinction matters when rebuilding: use Android's public TV Provider APIs directly, but reproduce Projectivy's policy/model layer independently.

---

# 32. Recommended Aerial-Rows architecture

```text
TvProviderRepository
 |
 +-- observeCapability()
 +-- getPreviewChannels()
 +-- getPreviewPrograms(channelId)
 +-- getWatchNextPrograms()
 +-- getLongDescription(programId, source)
 +-- invalidate()

TvChannel
 +-- id
 +-- providerPackage
 +-- internalProviderId
 +-- displayName
 +-- description
 +-- appLink
 +-- logoUri

TvProgram
 +-- source: PREVIEW | WATCH_NEXT
 +-- id
 +-- providerPackage
 +-- channelId
 +-- title
 +-- shortDescription
 +-- artwork / thumbnail / logo
 +-- aspect metadata
 +-- intentUri
 +-- previewVideoUri
 +-- duration
 +-- playbackPosition
 +-- type / author / genre / releaseDate

HomeCategorySource
 +-- WatchNext
 +-- PreviewChannel(channelId)

TvContentPolicy
 +-- enabled
 +-- excludedProviderPackages
 +-- watchNextPerAppLimit
 +-- previewProgramsPerChannelLimit

TvContentCache
 +-- channels
 +-- watchNext
 +-- programsByChannel
```

Refresh layer:

```text
known content/package/config event
      -> forced invalidate/refresh

periodic/home refresh request
      -> 30 s minimum refresh gate
      -> invalidate
      -> query
      -> reconcile stable categories/items
```

---

# 33. Implementation requirements for Projectivy parity

Aerial-Rows should eventually implement all of the following for parity:

1. Detect whether TV Provider access is supported and permitted.
2. Offer a master TV Channels setting.
3. Query Preview Channels.
4. Exclude inappropriate system recommendation channels.
5. Deduplicate channel providers.
6. Turn every Preview Channel into a configurable home row.
7. Preserve stable row identity by channel ID.
8. Load channel logos with `TvContract.buildChannelLogoUri`.
9. Call `requestChannelBrowsable` when enabling a channel where appropriate.
10. Query programs by channel.
11. Require browsable entries with valid deep links.
12. Sort Preview Programs by weight.
13. Support a configurable maximum number of programs per channel.
14. Query Watch Next independently.
15. Sort Watch Next by last engagement.
16. Support a configurable maximum number of Watch Next items per provider app.
17. Allow providers/apps to be excluded from Watch Next.
18. Display progress from playback-position/duration metadata.
19. Display optional provider/logo badges.
20. Respect poster/thumbnail aspect metadata.
21. Launch declared program deep links constrained to the provider package.
22. Play preview video only for the focused program.
23. Stop preview playback when focus leaves.
24. Expose Program Details.
25. Lazy-load long descriptions by provider row ID.
26. Cache channel/program results.
27. Invalidate/reconcile caches without destroying focus/stable row identities.
28. Publish focused-program information to the wallpaper/background layer.

---

# 34. Deliberately deferred

These are related but belong to later groups rather than unresolved Group 04 work:

- Media3/player internals and Now Playing sessions -> Group 05.
- Full wallpaper-provider/Aerial interaction -> Group 06.
- Complete preference persistence/schema -> Groups 07/10.
- HDMI/internal-TV tuning, `TvInputManager`, passthrough inputs and Live TV source activities -> Group 08.

---

# 35. Group 04 conclusion

The TV-content subsystem is sufficiently closed for an independent reconstruction.

The critical architecture is:

```text
ContentResolver / Android TV Provider
        |
        v
      e70 repository
   /        |         \
channels  preview    Watch Next
   |         |          |
  m60       bk3        d15
   |         \          /
 dynamic      ck3 / b15
 i5 rows          |
      \           /
        w40 home reconciliation
                |
              ek3
      artwork / progress / badge
      preview video / focus event
                |
       provider deep-link launch
```

Most importantly, Projectivy adds a real policy layer above Android TV Provider: per-provider and per-channel limits, provider exclusions, cache management, stable dynamic rows, lazy details, focus video and background events. Implementing only the raw `TvContract` queries would therefore not reproduce Projectivy's behavior.


---

# SOURCE FILE: `group04_postclosure_audit.md`

SHA-256: `25e6fcdb32775249e577abe8b55108ae0521647bde50225e2bb170b56f86aa9d`

---

# Group 4 post-closure audit — Android TV Channels & Watch Next

Status: **POST-CLOSURE AUDITED after Groups 5–12**  
Original Group 4 SHA-256: `1ef505a55393f1952896cdd6c11e398df3e8e380593360662ad1617a71377de3`

## Executive conclusion

The original Group 4 report remains architecturally correct and unusually complete.

No later group revealed a second hidden TV-content subsystem. The core model still stands:

```text
Android TV Provider
        ↓
e70 repository/cache
        ↓
channels / Preview Programs / Watch Next
        ↓
m60 / bk3 / d15
        ↓
dynamic i5 rows + ck3/b15 cards
        ↓
w40 reconciliation
        ↓
ek3 presenter
```

The audit found:

- **no major factual reversal**;
- **2 genuine completeness omissions**;
- several important precision/design refinements;
- one Projectivy limitation that the Aerial-Rows plan should intentionally improve.

The two strongest additions are:

1. exact Preview/Watch Next stable-ID formulas;
2. program-card parental protection inherits both **provider-package protection** and **individual card protection**.

---

# 1. Genuine completeness miss — exact program stable-ID formulas

Group 4 correctly stated that ordinary `ck3` identity is based primarily on `intent_uri` and that `b15` uses a Watch Next-specific helper.

The exact formulas are now closed.

## Preview Program / `ck3`

`ck3.a()` is:

```text
intent_uri present
    -> Uri.parse(intent_uri).hashCode()

otherwise
    -> "id".hashCode()
```

Because the normal Group 4 repository filters out programs without `intent_uri`, normal Preview Program cards use the URI hash.

## Watch Next / `b15`

`b15.a()` calls:

```text
q82.b(xu)
```

which is exactly:

```text
(Uri.parse(intent_uri) + "WN").hashCode()
```

for the normal non-null URI path.

The `"WN"` suffix deliberately gives Watch Next a different stable ID from a Preview Program with the same deep-link URI.

### Architectural consequence

Projectivy consistently tries to keep:

```text
Preview version of deep link X
    !=
Watch Next version of deep link X
```

even if the provider reuses the same URI.

### Aerial-Rows improvement

Do not copy 32-bit Java hashes.

Use structured identities, for example:

```text
preview:<provider>:<channelStableKey>:<programIdentity>
watchnext:<provider>:<programIdentity>
```

This removes hash collisions and makes source semantics explicit.

---

# 2. Genuine completeness miss — provider-level parental protection applies to TV program cards

`ck3` overrides the generic parental/protection check:

```text
ck3.D()
```

The effective rule is:

```text
protected =
    parentalSet contains hash(provider package)
    OR
    generic t4 protection applies to this card stable ID
```

In code terms:

```text
package_name != null && w75.l(packageName.hashCode())
    OR
super.D()
```

`super.D()` checks the card's own stable identity.

Therefore a Preview Program or Watch Next card can become protected in two ways:

1. the **owning provider application** is protected;
2. the **individual program card** is protected.

The subsequent execution still passes through the common `t4` launch path and therefore through the same parental launch wrapper established in Groups 3 and 10.

### Why this matters

Projectivy deliberately avoids this loophole:

```text
protected Netflix app
        but
unprotected Netflix Watch Next deep link
```

The provider package protection propagates to the program card.

### Aerial-Rows requirement

Every launch path must converge through:

```text
LaunchGate
    ↓
LaunchDispatcher
```

including:

- apps;
- Preview Programs;
- Watch Next;
- shortcuts;
- Now Playing;
- inputs;
- boot actions;
- remote actions.

Provider/package policy should be able to protect derived cards automatically.

---

# 3. Important precision correction — dynamic channel identity is only stable while Android's channel ID remains stable

Group 4 says the `w40.G` channel-ID cache allows stable dynamic rows across refreshes.

That is true **within a runtime/provider lifecycle where the numeric Android channel ID remains unchanged**.

The actual Projectivy identity is:

```text
category ID = int(channelId + 2000)
```

and reconciliation is keyed by:

```text
channelId
```

This has two durability weaknesses:

1. a provider/OEM can delete and recreate a channel with a new `_id`;
2. the `long` channel ID is converted to an `int` after adding 2000.

Therefore the stronger canonical wording should be:

> Projectivy preserves dynamic-channel object identity across ordinary refreshes by numeric channel ID, but the identity is not guaranteed to survive provider/OEM channel recreation and is not collision-proof as a long-term persistent key.

This is especially relevant because Group 4 already discovered the better logical identity ingredients:

```text
package_name
internal_provider_id
display/channel metadata
```

### Aerial-Rows improvement

Persist a logical stable key such as:

```text
provider package
+ internal_provider_id
```

with sensible fallback identity when `internal_provider_id` is missing.

Keep numeric `channelId` only as the current TvProvider lookup handle.

This is already reflected in the Group 12 implementation plan and should now be considered a correction to Group 4's parity recommendation #7 ("preserve stable row identity by channel ID").

---

# 4. Precision upgrade — Projectivy discovers non-browsable Preview Channels

The channel projection used by `e70.b()` is:

```text
_id
package_name
type
display_name
description
app_link_intent_uri
internal_provider_id
```

It does **not** query/filter the channel's `browsable` column.

The channel filter is based on:

```text
type == TYPE_PREVIEW
provider package not ending in .tvrecommendations
dedup by package + internal_provider_id when available
```

Therefore Projectivy can discover a Preview Channel that is not currently browsable.

That makes the later call to:

```text
TvContract.requestChannelBrowsable(context, channelId)
```

semantically important rather than redundant.

### Canonical interpretation

```text
Discover Preview Channels
        ↓
show/manage Projectivy row
        ↓
user enables channel
        ↓
request Android browsable state
```

For Aerial-Rows, the **management view must not hide non-browsable channels** if we want users to be able to enable them.

---

# 5. Precision upgrade — Watch Next provider-management UI is channel-derived

Group 4 correctly notes that `CategoryAppsPreferencesFragment` builds the provider switches for Watch Next from apps represented by discovered TV channels.

This has a subtle limitation:

```text
app publishes Watch Next
but no discoverable Preview Channel
        ↓
its package may not naturally appear in the provider-switch UI
```

The filtering engine itself is package based, but the management choices are sourced from discovered channel providers.

### Aerial-Rows improvement

Build Watch Next provider management from the union of:

```text
packages present in Watch Next
+
packages owning discovered Preview Channels
```

rather than only channel providers.

This makes the UI reflect the actual Watch Next dataset.

---

# 6. Precision upgrade — Watch Next/provider exclusions use package hashes

The Watch Next category's exclusion list stores package-derived hashed identities.

The effective policy is:

```text
enabled = packageHash not in i5.v
```

This works in Projectivy's integer-ID architecture, but it inherits ordinary Java hash collision risk.

For Aerial-Rows, store the package name (or a structured provider identity) directly:

```text
excludedProviderPackages: Set<String>
```

rather than a 32-bit hash.

---

# 7. Precision upgrade — built-in Watch Next category identity

Group 2's post-closure audit closes the home-level identity around Group 4:

```text
Watch Next category ID = 10
source type = 5
```

Dynamic Preview Channel rows use:

```text
source type = 6
category ID = int(channelId + 2000)
```

So the canonical TV home model is:

```text
fixed Watch Next row
        +
dynamic Preview Channel rows
```

rather than all TV-provider content being dynamically derived.

---

# 8. Precision upgrade — exact program appearance settings

Group 7 closes the settings controlling `ek3` program-card behavior:

```text
appearance_cards_program_play_preview
appearance_cards_program_show_progressbar
appearance_cards_program_show_badge
appearance_cards_program_zoom_on_focus
```

mapped through:

```text
hj3.Z0
hj3.a1
hj3.b1
hj3.c1
```

This strengthens Group 4's presenter analysis:

- progress is configurable;
- provider badge is configurable;
- preview playback is configurable;
- focused-program artwork zoom is separately configurable.

These are independent controls, not one generic "program card enhancement" switch.

---

# 9. Additional presenter detail — focus zoom timing

The raw `ek3` presenter gives one extra exact UI behavior not captured in Group 4.

When:

```text
appearance_cards_program_zoom_on_focus
```

is enabled, the main image animates approximately:

```text
focus gained:
    start delay = 300 ms
    scale 1.0 -> 1.05
    duration = 3000 ms

focus lost:
    start delay = 0
    scale -> 1.0
    duration = 300 ms
```

This is separate from preview-video playback.

Important:

> The 300 ms value above is an artwork-zoom delay. It should **not** be interpreted as proof of a 300 ms preview-video dwell delay.

For Aerial-Rows we can deliberately use a shorter/explicit preview-video dwell policy if desired.

---

# 10. Precision upgrade — program-card focus background event is a formal wallpaper event

Group 4 correctly found:

```text
Event.ProgramCardFocused
```

Group 6 later proved that this belongs to a formal wallpaper-provider event contract.

Its event bit is:

```text
ProgramCardFocused = 8
```

and it participates alongside:

```text
TimeElapsed
NowPlayingChanged
CardFocused
LauncherIdleModeChanged
```

So the canonical relationship is:

```text
program focus
    ↓
semantic home event
    ↓
wallpaper provider coordinator
    ↓
provider may choose a new background
```

not merely "the presenter directly changes the background."

That strongly supports the Aerial-Rows event-bus/provider architecture.

---

# 11. Precision upgrade — Random TV Program wallpaper remains actionable

Group 4 notes that `rp3` reuses `e70.c(0)` to turn Preview Program artwork into wallpapers.

Group 6 closes another useful detail:

- it preserves the program/action URI in the resulting wallpaper descriptor;
- the Random TV Program wallpaper provider has a **2-hour provider cache duration**.

Thus a TV-program-derived wallpaper can retain an action/deep-link relationship rather than being reduced to a dead bitmap.

This is a cross-subsystem enhancement, not a change to the home query path.

---

# 12. Precision upgrade — Program Details is intentionally a two-tier data model

Group 4 already described the lazy `long_description` lookup correctly.

The post-closure interpretation should emphasize the architectural split:

```text
home row query
    -> bounded lightweight projection

details screen
    -> single-row lookup by _id
    -> long_description only
```

The generic Android `xu/yu` model supports many more Android TV columns, but the actual Group 4 home query **does not request all of them**.

The home projection remains exactly the bounded 22-column set described in the original report.

Therefore fields supported by generic `xu`, such as:

```text
availability
starting_price
offer_price
live
interaction_type
interaction_count
content_id
start/end time
preview_audio_uri
...
```

should **not** be retroactively claimed as queried Group 4 home data.

This is a useful negative verification: the original projection list was correct.

---

# 13. Precision upgrade — Preview vs Watch Next identity separation is deliberate, but still hash-based

Because:

```text
Preview ID = hash(intent Uri)
Watch Next ID = hash(intent Uri + "WN")
```

Projectivy deliberately avoids treating the same provider deep link in Preview and Watch Next as the same card identity.

However, two caveats remain:

1. both are only 32-bit hashes;
2. Preview card identity does not include channel ID.

The global Preview query deduplicates by:

```text
intent_uri + channel_id
```

but card identity itself is primarily only the `intent_uri` hash.

This is normally safe because cards live in separate row adapters, but it is not a globally structured identity scheme.

### Aerial-Rows improvement

Make source/channel explicit in `CardIdentity` rather than relying on row locality to disambiguate identical IDs.

---

# 14. Refresh finding remains valid — no dedicated TvProvider ContentObserver found

The later groups did not invalidate Group 4's negative finding.

Projectivy 4.72 still appears to use:

```text
separate caches
        +
forced invalidation
        +
home refresh
        +
~30 s minimum timed-refresh gate
```

rather than a dedicated observer on:

```text
channel
preview_program
watch_next_program
```

The `ContentObserver` hits found in the APK belong to other subsystems.

### Aerial-Rows improvement

Group 12 intentionally improves this:

```text
ContentObserver
    + debounce
    + repository cache
    + resume refresh safety net
```

This is an intentional improvement over Projectivy, not a correction to the reverse engineering.

---

# 15. Deep-link launching should now be described as policy-gated

Group 4 accurately traces card intent construction:

```text
Intent.parseUri(intent_uri, URI_INTENT_SCHEME)
    ↓
setPackage(program.package_name)
    ↓
generic t4 launch preparation
```

Groups 3 and 10 close what happens afterward:

```text
generic t4 launch path
    ↓
parental/protection policy
    ↓
launch
```

Therefore the canonical Group 4 launch description should be:

```text
provider-declared deep link
    ↓
constrained to provider package
    ↓
generic LaunchGate/policy
    ↓
activity dispatch
```

For Aerial-Rows, never call `startActivity()` directly from a Watch Next/Program Composable.

---

# 16. Clarification — declared EPG write permissions do not imply this subsystem writes program rows

Group 1's manifest analysis found TV/EPG-related permissions including write permissions.

Group 4's verified provider architecture, however, is read-centric:

- query channels;
- query Preview Programs;
- query Watch Next;
- request a channel become browsable.

No Projectivy TV-content row creation/update pipeline was found here.

So:

> Do not infer a "publish our own Preview Programs" subsystem from manifest permissions alone.

Aerial-Rows should remain read-only unless we later introduce a concrete feature that requires writes.

---

# 17. Things Group 4 correctly deferred — not misses

These remain correctly separated:

- Android media sessions / Now Playing -> Group 5;
- wallpaper provider implementation -> Group 6;
- exact settings schema/UI -> Group 7;
- `TvInputManager`, HDMI and actual tuner/input launching -> Group 8;
- generic parental storage/policy -> Group 10.

The fact that later groups enrich these boundaries does not mean Group 4 was incomplete within its stated scope.

---

# 18. Revised canonical TV-content model

With Groups 5–12 folded back in:

```text
TvProviderRepository / e70
│
├─ capability
│   ├─ platform/device gate
│   ├─ READ_TV_LISTINGS
│   └─ general_enable_tv_channels
│
├─ channels
│   ├─ TYPE_PREVIEW
│   ├─ includes non-browsable channels
│   ├─ exclude *.tvrecommendations
│   ├─ dedup package + internal_provider_id
│   ├─ numeric channelId runtime handle
│   └─ cache
│
├─ Preview Programs
│   ├─ weight DESC
│   ├─ browsable == 1
│   ├─ intent_uri required
│   ├─ global dedup intent_uri + channel_id
│   ├─ configurable per-channel limit
│   └─ cache per channel
│
├─ Watch Next
│   ├─ last_engagement_time_utc_millis DESC
│   ├─ browsable == 1
│   ├─ intent_uri required
│   ├─ per-provider item limit
│   ├─ provider exclusions
│   └─ separate cache
│
└─ details
    └─ lazy long_description lookup by program _id

Home rows
├─ Watch Next
│   ├─ category ID 10
│   └─ source type 5
└─ Preview Channels
    ├─ source type 6
    └─ runtime category ID int(channelId + 2000)

ProgramCard
├─ Preview / ck3
│   └─ ID = hash(intent Uri)
├─ Watch Next / b15
│   └─ ID = hash(intent Uri + "WN")
├─ provider-package parental inheritance
├─ individual-card parental protection
├─ custom artwork -> thumbnail -> poster -> logo
├─ progress
├─ provider badge
├─ preview video
├─ details action
└─ ProgramCardFocused wallpaper event

Refresh
├─ forced invalidation
├─ separate cache clears
├─ timed home refresh
└─ ~30 s minimum gate
```

---

# 19. Effect on Aerial-Rows implementation plan

The Group 12 implementation order remains correct, but the Group 4 audit strengthens Phase 4 in these ways:

1. **Persist logical channel identity, not numeric channel ID.**
2. Keep numeric channel ID as an ephemeral current provider handle.
3. Manage non-browsable Preview Channels too.
4. Derive Watch Next provider controls from actual Watch Next packages as well as channel providers.
5. Use structured Preview/Watch Next card IDs rather than hashes.
6. Route all TV-content deep links through `LaunchGate`.
7. Let provider-level parental policy automatically cover derived program cards.
8. Keep the bounded home projection and lazy details query.
9. Add ContentObserver + debounce as an intentional improvement over Projectivy.
10. Keep program focus events semantic and renderer-independent.
11. Keep preview zoom animation and preview-video policy as separate concerns.

---

# Final assessment

```text
Core Group 4 architecture:          correct
Missing major subsystem:            none

Genuine completeness omissions:     2
  - exact Preview/Watch Next card-ID formulas
  - provider-level parental protection inheritance

Important precision refinements:
  - channel-ID stability is conditional, not durable
  - non-browsable channels are still discovered
  - Watch Next management source is channel-provider biased
  - program appearance setting keys now known
  - exact focus-zoom timing
  - deep links are generic-policy gated
  - Random TV wallpaper remains actionable

Negative finding retained:
  - no dedicated TV-provider ContentObserver found

Need to redo Group 4:                no
Effect on Group 12 plan:             strengthens Phase 4, no reorder required
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 5–12`.**


---

# SOURCE FILE: `group05.md`

SHA-256: `20e6be9fd02df485749cac62ae4b9826177cf5997b370b02206cc6cab6e5113f`

---

# Projectivy 4.72 — Group 05: Notifications & Now Playing

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group closes two related but distinct Projectivy subsystems:

1. the Android notification-listener / notification side-panel pipeline; and
2. the MediaSession/Media3-based **Now Playing** home row and its controls.

It also records their intentional cross-links: notification dismissal can stop a linked Now Playing session, media state updates the home row and wallpaper-provider contract, and media playback participates in Projectivy's idle/power logic.

---

## Analysis workflow used

Group 5 uses the optimized RE workflow introduced after Group 4:

1. retain the already verified APK identity;
2. use a persistent decompiled source cache (`/mnt/data/projectivy_decompiled/sources`) for fast local search;
3. use a direct JADX Java-API index for class hierarchy/xrefs when the bulk cache is insufficient;
4. batch-export missing high-value classes in a **single APK load** rather than reopening the APK per class;
5. inspect only the Projectivy-specific cluster;
6. use smali/bytecode as the authority where JADX's Java reconstruction is ambiguous.

The source cache currently contains thousands of classes and allows `ripgrep`-style discovery without repeatedly invoking MCP over the complete 7,395-class APK. A selected-class dumper was added during this group so missing classes can be exported together after one ~single APK load.

This is both faster and more reliable: broad discovery happens locally, while exact control-flow claims are still checked against JADX xrefs or smali when needed.

---

# 1. Top-level architecture

The central chain for Now Playing is:

```text
NotificationListener
        ↓
       iw2                  media-session coordinator
        ↓
       ju2                  one active media-session/controller wrapper
        ↓
       c73 : t4             one Now Playing home card
        ↓
   i5 category ID 12
   source type 8
```

The ordinary notification-panel chain is separate:

```text
NotificationListenerService callbacks
        ↓
notification event/state flows
        ↓
       b73                  notifications Preference fragment
        ↓
       z63                  dynamic grouping/rendering
        ↓
NotificationPreference / l63
```

The two branches meet when Android emits a special `Notification.NowPlaying` notification: its `l63` model can resolve the corresponding `c73` card and operate on the linked media session.

---

# 2. `NotificationListener`

Class:

```text
com.spocky.projengmenu.services.notification.NotificationListener
    extends android.service.notification.NotificationListenerService
```

This is a genuine Android notification-listener service rather than a polling component.

Important static state includes:

- `H` — current connected service instance;
- `I` — connected flag;
- `C` — posted/raw event stream;
- `D` — removal stream;
- `E` — wrapper/derived removal flow;
- `F` — current filtered active-notification list;
- `G` — exposed/read-only-style wrapper around that list.

Important instance state includes:

- coroutine scope;
- reusable `NotificationListenerService.Ranking`;
- maps used for notification-group bookkeeping;
- key currently being explicitly cancelled;
- lazy `iw2` media-session coordinator;
- helper lifecycle state;
- package allowlist containing `com.android.systemui` and Projectivy itself.

## Lifecycle

`onListenerConnected()`:

1. records the singleton service instance;
2. marks the listener connected;
3. when Projectivy's global feature gate is enabled, triggers a complete notification refresh;
4. connects media-session monitoring via `e()`.

`onListenerDisconnected()`:

1. clears connected/global instance state;
2. resets active notification state/flows;
3. clears notification-group maps;
4. disconnects media-session monitoring.

`onNotificationPosted`, `onNotificationRemoved`, and `onNotificationRankingUpdate` are dispatched asynchronously into Projectivy's event pipeline.

Projectivy wraps `getActiveNotifications()` defensively: `SecurityException`/runtime failures produce an empty array rather than crashing the launcher.

---

# 3. Notification filtering

The filter method is decompiler-hostile, so its important conditions were checked against smali rather than inferred from JADX's duplicated/partially malformed Java output.

The intent is to keep the side panel focused on useful user-facing notifications while suppressing noisy service/system entries.

Verified filtering factors include:

- `NotificationListenerService.Ranking.canShowBadge()` on newer Android versions;
- special allowance for Projectivy/SystemUI where appropriate;
- suppression of certain ongoing `miscellaneous`-channel notifications;
- recognition of the tag `Notification.NowPlaying`;
- recognition of tags beginning with `com.android.server`;
- foreground-service / ongoing-notification rules;
- rejection of group-summary entries;
- requirement for usable notification title/text content.

The exact filter differs across Android-version branches, so an Aerial-Rows implementation should represent it as a policy function rather than scattering flag checks through the UI.

---

# 4. Notification event model

Projectivy converts framework callbacks into internal event objects rather than having UI screens consume the service directly.

Observed event classes include:

- `w63` — posted notification;
- `y63` — removed notification;
- `x63` — ranking update;
- `u63` — explicit cancel-by-key request;
- `v63` — full refresh request.

Supporting identity helpers reduce notifications to package/user and notification-key identities.

The event consumer updates both incremental event flows and the current complete filtered list. As a result, consumers can choose between:

```text
full current state
```

and

```text
incremental posted/removed events
```

without re-querying the platform themselves.

---

# 5. Notification grouping / summaries

`NotificationListener.g(StatusBarNotification)` maintains explicit grouping state.

For grouped notifications Projectivy tracks, among other things:

- summary key;
- child keys;
- key-to-group mapping.

When children disappear it can clean the corresponding summary state and, where appropriate, cancel a stale summary. This prevents the side panel from retaining orphan group-summary items.

This is a data-layer responsibility, not merely a visual grouping trick in the settings fragment.

---

# 6. Notifications side panel

Entry activity:

```text
NotificationsSidePanelActivity
```

Its actual screen is built by the Projectivy preference-fragment infrastructure. `b73` supplies:

```text
R.xml.settings_notifications
```

The XML root itself is intentionally almost empty. Notifications are generated dynamically.

`b73` starts collectors for:

- current full list (`NotificationListener.G`);
- removal events (`NotificationListener.E`).

`z63` handles the actual UI reconciliation.

## Full-list rendering

The panel:

1. groups notifications by provider package/application label;
2. stores groups in a case-insensitive `TreeMap`, giving alphabetically ordered provider groups;
3. sorts items inside each provider group by notification post time, newest first;
4. creates a `PTPreferenceCategory` for each provider;
5. creates one `NotificationPreference`/`l63` model per notification;
6. adds **Clear all** when at least two notifications are present.

The removal collector performs incremental cleanup rather than rebuilding everything unnecessarily:

- remove the dismissed preference;
- remove its category if the category becomes empty;
- remove **Clear all** when the list falls below two entries;
- handle/close an empty panel appropriately.

---

# 7. `l63` notification UI model

`l63` is the model/action wrapper behind a notification preference.

It extracts:

- notification key;
- title (`android.title`);
- text (`android.text`);
- content intent;
- delete intent;
- icon;
- auto-cancel state;
- dismissable/ongoing state;
- whether the tag is `Notification.NowPlaying`.

On Android TV it also understands `android.tv.EXTENSIONS`, including TV-specific content/delete intents.

## Icon selection

The model prefers an appropriate large icon when present, otherwise falls back to the notification's small icon. If no usable drawable exists it falls back to Projectivy's notification/Now Playing icons.

## Click

If a content `PendingIntent` exists, Projectivy sends it using a clip-reveal animation bundle. Auto-cancel notifications are then dismissed through the model.

If no content intent exists and the item is a linked `Notification.NowPlaying` notification, Projectivy can instead launch the matching `c73` Now Playing session/card.

## Dismiss

For dismissable notifications:

1. find and stop a linked Now Playing card if applicable;
2. send the notification delete intent if present;
3. ask the connected `NotificationListener` to cancel the notification by key.

This establishes a real bidirectional link between the notification subsystem and media-session subsystem.

---

# 8. Main-title notification indicator

`MainTitleView.k()` reads the size of the current filtered notification list (`NotificationListener.G`).

The notification button is controlled by Projectivy settings including:

- show notifications control;
- hide the control when the notification list is empty.

The badge renders:

```text
0..9 → numeric text
10+  → "+"
```

Pressing it opens `NotificationsSidePanelActivity`.

Therefore the notification button in the Projectivy title/status area is driven by the same observable state as the side panel; it is not separately counting system notifications.

---

# 9. Media-session access

When the notification listener connects, `NotificationListener.e()` establishes media monitoring.

It constructs a `ComponentName` referring to its own `NotificationListener`, obtains Android's `MediaSessionManager`, and requires either:

- `MEDIA_CONTENT_CONTROL`, or
- valid enabled notification-listener access.

It then:

1. calls `MediaSessionManager.getActiveSessions(componentName)`;
2. converts the returned sessions/tokens through the bundled media support layer;
3. passes them into `iw2`;
4. registers an active-sessions-changed listener.

Security failures are caught rather than taking down the launcher.

This explains why Projectivy can discover sessions from other applications without implementing a proprietary playback protocol.

---

# 10. `iw2` — media-session coordinator

`iw2` owns the current session list:

```text
CopyOnWriteArrayList<ju2>
```

It also contains a package exclusion list. One verified excluded provider is:

```text
com.humaxdigital.tv.freeview.tvapp
```

When the active-session list changes, Projectivy:

1. detaches/releases old `ju2` wrappers;
2. clears the active list;
3. filters excluded package tokens;
4. asynchronously builds/attaches controllers for the remaining tokens;
5. subscribes to their callbacks;
6. emits a media-state update.

## Primary session selection

`iw2.b()` selects the session used for the global Now Playing snapshot in this priority order:

1. first session whose `ju2.q()` reports playing;
2. otherwise first session whose `ju2.o()` reports active/non-terminal playback state;
3. otherwise the last session in the current list.

This is distinct from the Now Playing **row**, which can display multiple active sessions.

## Debouncing

The media-event pipeline applies approximately a **500 ms debounce/coalescing stage** before producing its global Now Playing update.

That prevents rapid metadata/playback callbacks from repeatedly rebuilding the home row or spamming the wallpaper-provider IPC contract.

---

# 11. `ju2` — one media session

`ju2` wraps one connected media controller and implements its player-listener callback interface.

Tracked state includes:

- playing state;
- player state;
- session token;
- several metadata text fields;
- artwork URI;
- artwork bytes.

The constructor snapshots the initial player/media metadata. Later metadata and playback-state callbacks update this state and signal `iw2`.

## Active-state test

`ju2.o()` excludes player state values `{1, 4}`.

In the consumed Media3 player state model these correspond to the terminal/non-active states (idle/ended), leaving buffering/ready-style sessions eligible for the Now Playing row.

`ju2.q()` exposes the stored playing boolean.

## Controller lifecycle

`ju2.I(boolean)` attaches or detaches itself as a controller listener and releases the controller on the detach path where requested. Main-thread dispatch is enforced when needed.

---

# 12. Recoverable metadata semantics

The obfuscated underlying Media3 metadata class has many fields, so Group 5 deliberately does **not** assign guessed names to every slot.

However Projectivy creates an `ew2` object whose `toString()` reveals the semantic contract:

```text
NowPlayingSnapshot(
    activeToken,
    title,
    artist,
    albumTitle,
    artworkUri,
    isPlaying
)
```

The snapshot is built from the selected `ju2` as:

```text
token       = ju2.h()
title       = ju2.l()
artist      = ju2.d()
albumTitle  = ju2.c()
artworkUri  = ju2.e()
isPlaying   = ju2.q()
```

This semantic snapshot is the safe implementation boundary for the wallpaper-provider event.

The `c73` card itself uses a slightly richer/different set of underlying metadata slots to choose its displayed primary/secondary text, so the report does not conflate the card renderer with the wallpaper snapshot.

---

# 13. Now Playing built-in category

`w40.k()` creates the built-in category explicitly as:

```text
category ID:   12
source type:   8
title:         menu_now_playing
icon:          ic_menu_now_playing
color:         ic_now_playing
```

This confirms Now Playing is a normal Projectivy `i5` category source integrated into the same generic home architecture established in Group 2.

When `w40` resolves source type `8`, it:

1. requires the notification listener/media subsystem to be available;
2. takes the active `iw2` session wrappers;
3. keeps sessions for which `ju2.o()` is true;
4. deduplicates them by session token;
5. maps each `ju2` into a `c73` card.

Thus the row can contain **multiple active media sessions**.

---

# 14. `c73` — Now Playing card

`c73` extends the common Projectivy `t4` card model:

```text
c73 : t4
item type = 8
```

It stores:

- the linked `ju2` session;
- display text assembled from media metadata;
- artwork URI;
- artwork bytes.

Its stable ID is derived from the string representation of the session token, giving session-level rather than package-only identity.

`B()` delegates to `ju2.o()` so the generic card architecture can tell whether the item remains active.

`k()` returns false: Now Playing cards are not manually reorderable like ordinary app cards.

## Text assembly

The primary card text comes from a media metadata/display-title slot and is prefixed with a playback-state glyph. The exact decompiled glyph assignment is retained as implementation evidence rather than semantically "corrected": Projectivy chooses between the play and pause symbols based on the stored playing boolean.

Secondary text is composed from metadata fields separated by:

```text
 · 
```

with fallback fields used when the preferred pair is unavailable.

## Artwork

`d73`, the Now Playing presenter, selects:

```text
artwork URI
    ↓
artwork byte[]
    ↓
ic_default_now_playing
```

It includes the byte-array hash in equality/rebind decisions, allowing embedded artwork changes to invalidate a card even if its URI is unchanged/null.

---

# 15. Launch behavior for a Now Playing card

Clicking a `c73` does not merely open a generic media app blindly.

Launch resolution is:

```text
media session/controller primary PendingIntent
        ↓ if absent/fails
normal launcher intent for the session package
        ↓ if still unavailable
Projectivy "can't start activity" feedback
```

This is the correct behavior for Aerial-Rows to reproduce: prefer the exact activity/content target supplied by the active media session, then fall back to the owning application.

---

# 16. Media controls

`c73` exposes controller operations used by the generic `j6` action screen:

- pause;
- play;
- stop;
- previous media item;
- next media item.

The action screen only enables controls when the connected media controller reports the corresponding command as available.

Observed action entries:

```text
action_play_pause
action_stop
action_previous
action_next
```

Play/pause dynamically chooses pause when the wrapper is currently playing and play otherwise.

Stop attempts the real controller stop command; if that operation throws/fails, Projectivy falls back to pause.

Previous/next call the controller's previous/next-media-item operations after checking command availability.

Because minified Media3 command constants are not human-readable in the APK, this report intentionally describes their semantic operations rather than treating their obfuscated numeric checks as a public API contract.

---

# 17. Focus → wallpaper-provider integration

`d73` publishes a wallpaper-provider:

```text
Event.ProgramCardFocused
```

when a Now Playing card gains focus.

The event includes the card's visible title/type/aspect-style metadata and artwork URI where available.

Separately, the global media-session pipeline publishes:

```text
Event.NowPlayingChanged
```

from the debounced `ew2` snapshot.

Its recoverable payload is:

- isPlaying;
- title;
- artist;
- album title;
- artwork URI;
- selected/active session identity through the snapshot pipeline.

After sending `NowPlayingChanged`, Projectivy looks up home category ID `12` and refreshes it when that row is enabled/visible.

Therefore the wallpaper system has two complementary media signals:

```text
NowPlayingChanged
    → global playback/metadata change

ProgramCardFocused
    → user focus moved onto a particular Now Playing card
```

This becomes important in Group 6 when reconstructing wallpaper/Aerial behavior.

---

# 18. Notification ↔ Now Playing matching

`l63.b()` handles the special tag:

```text
Notification.NowPlaying
```

It:

1. obtains category ID `12`;
2. scans that category's cards;
3. compares the card display identity/title against the notification title;
4. returns the matching `c73` where possible.

The linked card is then used when clicking/dismissing the notification.

This is an intentionally pragmatic correlation mechanism rather than a formal session-token relation in the notification model.

For an independent implementation we can reproduce the behavior but should prefer a stronger package/session correlation whenever the framework exposes it, with title matching as a fallback.

---

# 19. Media playback and idle/power detection

Group 5 also found a cross-subsystem setting:

```text
power_internal_idle_excludes_media_playback
```

with user-facing wording equivalent to excluding active media playback from idle detection.

Projectivy conditionally keeps media-session monitoring active for this purpose. Therefore media awareness is not used only for the Now Playing row: it can prevent idle/power behavior from triggering while media is playing.

The complete idle/boot/accessibility behavior remains in Group 9, but this dependency is now part of the baseline.

---

# 20. Framework/library boundary

A substantial portion of the session/controller implementation is bundled/minified Media3 support code. Group 5 deliberately stops at the Projectivy-facing abstractions instead of reverse-engineering Media3 itself.

Projectivy-specific boundary:

```text
NotificationListener
       ↓
      iw2
       ↓
      ju2
       ↓
      c73 / d73
       ↓
      w40 / j6 / wallpaper events
```

Bundled framework/library boundary includes the underlying media controller/token/player classes (`nt2`, `mt2`, `z54`, `nv2`, etc.). Their public behavior is used where needed, but their internal implementation is not treated as proprietary Projectivy logic.

This ownership distinction significantly reduces unnecessary RE work while preserving reconstruction accuracy.

---

# 21. Recommended Aerial-Rows architecture

A clean equivalent can be implemented with explicit domain types rather than retaining the obfuscated layering:

```text
NotificationRepository
 ├─ currentNotifications: StateFlow<List<LauncherNotification>>
 ├─ postedEvents
 ├─ removedEvents
 ├─ dismiss(key)
 └─ clearAll()

NotificationPolicy
 └─ shouldShow(notification, ranking)

NotificationGroupCoordinator
 └─ provider/group summary bookkeeping
```

For media:

```text
MediaSessionRepository
 ├─ sessions: StateFlow<List<MediaSessionItem>>
 ├─ primarySession: StateFlow<MediaSessionItem?>
 ├─ connect()
 └─ disconnect()

MediaSessionItem
 ├─ stableId/token
 ├─ packageName
 ├─ title/displayTitle
 ├─ artist
 ├─ albumTitle
 ├─ artworkUri/bytes
 ├─ isPlaying
 ├─ playerState
 ├─ launchIntent
 └─ availableCommands
```

Home integration:

```text
NowPlayingSource : HomeCategorySource
      ↓
List<NowPlayingHomeItem>
      ↓
generic card/presenter system
```

Actions:

```text
CardActionResolver
 └─ NowPlaying
     ├─ Play/Pause
     ├─ Stop
     ├─ Previous
     └─ Next
```

Wallpaper integration:

```text
MediaSessionRepository
     ↓ debounce/coalesce (~500 ms)
NowPlayingSnapshot
     ↓
WallpaperEvent.NowPlayingChanged

focused NowPlayingHomeItem
     ↓
WallpaperEvent.ProgramCardFocused
```

---

# 22. Improvements over blindly cloning the implementation

For equivalent behavior in Aerial-Rows, several Projectivy patterns are worth retaining:

1. **single observable notification state** shared by title badge and panel;
2. **incremental notification removals** instead of full panel rebuilds;
3. **session-level stable IDs** rather than package IDs;
4. **multiple active sessions in the home row**;
5. **separate primary-session selection** for global wallpaper/media state;
6. **500 ms media-event coalescing** to avoid UI/wallpaper churn;
7. **controller PendingIntent first, app-launch fallback second**;
8. **capability-aware media controls**;
9. **artwork bytes as well as URI support**;
10. **media playback participates in idle detection**.

Potential implementation improvements that do not change user-visible compatibility:

- correlate Now Playing notification ↔ session primarily by package/session identity, falling back to title matching;
- express notification filtering as testable policy rules;
- use Android/Kotlin domain names rather than reproducing the opaque minified class graph;
- keep framework Media3 implementation behind a repository adapter.

---

# 23. Closed dependency graph

The subsystem is sufficiently closed for independent reconstruction:

```text
Android NotificationListenerService
        ↓
NotificationListener
 ├─ filtering
 ├─ grouping
 ├─ state/event flows
 └─ MediaSessionManager access
        ↓
       iw2
 ├─ active-session reconciliation
 ├─ primary-session selection
 └─ 500 ms event coalescing
        ↓
       ju2
 ├─ controller
 ├─ state
 ├─ metadata
 └─ artwork
        ↓
       c73
 ├─ launch
 ├─ play/pause/stop
 ├─ stable ID
 └─ card metadata
        ↓
       d73
 └─ rendering/focus wallpaper event
        ↓
       w40
 └─ category ID 12 / source type 8
```

Parallel notification UI path:

```text
NotificationListener
        ↓
 current list + removal stream
        ↓
       b73
        ↓
       z63
        ↓
provider categories
        ↓
NotificationPreference / l63
        ↓
launch / dismiss / linked media stop
```

---

# 24. Deliberately deferred

These are not Group 5 gaps:

- wallpaper-provider IPC implementation and Aerial/video provider design → **Group 6**;
- exact notification/title/settings preference storage schema → **Groups 7/10**;
- idle timeout, boot and accessibility behavior that consumes media state → **Group 9**;
- broader persistence/migration details → **Group 10**.

---

## Group 05 conclusion

Projectivy implements notifications and Now Playing as two coordinated observable subsystems rather than as static launcher widgets. Notifications are filtered, grouped and dynamically rendered from a `NotificationListenerService`; the same service's privileged access is used to enumerate active Android media sessions. Active sessions are wrapped, deduplicated, tracked through Media3 callbacks, coalesced through a ~500 ms update pipeline, exposed as a normal Projectivy category, and connected to both card actions and the wallpaper-provider event API.

The key reconstruction principle is therefore:

```text
Do not build "Now Playing" from notifications alone.
```

Use real active media sessions for playback state/control and treat the notification panel as a separate UI/data stream, linking the two only where useful.

**Group 05 status: COMPLETE + VERIFIED.**


---

# SOURCE FILE: `group05_postclosure_audit.md`

SHA-256: `eee0869a2988ab51be75410059dec13f1a7d7fa5bc0a2c4ba3e2b953be96734a`

---

# Group 5 post-closure audit — Notifications & Now Playing

Status: **POST-CLOSURE AUDITED after Groups 6–12**  
Original Group 5 SHA-256: `20e6be9fd02df485749cac62ae4b9826177cf5997b370b02206cc6cab6e5113f`

## Executive conclusion

The original Group 5 report remains architecturally correct.

No later group revealed a second hidden notification/media subsystem. The central split is still valid:

```text
NotificationListenerService
    ├─ notification state/panel
    └─ MediaSessionManager access
            ↓
           iw2
            ↓
           ju2
            ↓
           c73
            ↓
      Now Playing row ID 12
```

The audit found:

- **1 factual overstatement** in the original report;
- **1 genuine completeness omission with important security/policy implications**;
- several precision upgrades useful for the Aerial-Rows implementation;
- no need to redo Group 5 from scratch.

The most important findings are:

1. `NowPlayingChanged` does **not** publish the active session/token identity through the wallpaper IPC event;
2. `c73` Now Playing launch is an exception to Projectivy's normal `t4` parental launch wrapper.

---

# 1. Factual correction — `NowPlayingChanged` IPC does not carry session identity

Group 5 section 17 says the wallpaper event payload includes:

> “selected/active session identity through the snapshot pipeline”

That is too strong.

Projectivy's internal snapshot `ew2` does contain:

```text
activeToken
title
artist
albumTitle
artworkUri
isPlaying
```

and `activeToken` participates in `ew2.equals()` / `hashCode()`.

That means changing the selected session can cause the internal snapshot to be considered different even if other metadata happens to match.

However, when the snapshot is converted to:

```text
Event.NowPlayingChanged
```

Projectivy sends only:

```text
eventType
isPlaying
title
artist
album
iconUri
```

The active token is **not serialized into the wallpaper-provider event**.

Exact event contract:

```text
NowPlayingChanged(
    eventType,
    isPlaying,
    title,
    artist,
    album,
    iconUri
)
```

### Corrected canonical statement

```text
ew2 internal snapshot
    includes active session token
        ↓
token participates in internal change detection
        ↓
Event.NowPlayingChanged
    omits token/session identity
```

### Aerial-Rows improvement

Our internal wallpaper event can keep a structured session identity if it is useful:

```text
NowPlayingChanged(
    sessionId,
    packageName,
    isPlaying,
    title,
    artist,
    album,
    artwork
)
```

while an external plugin API could expose only the minimum stable fields if desired.

---

# 2. Genuine completeness miss — Now Playing bypasses the generic `t4` parental launch wrapper

This is the most important post-closure Group 5 finding.

Generic `t4.f(...)` does:

```text
build destination Intent
        ↓
D() protected?
        ↓ yes
wrap in ParentalControlCheckActivity
        ↓
startActivity
```

`c73` overrides `f(...)`.

Its implementation directly performs:

```text
MediaController/session PendingIntent
        ↓
PendingIntent.send()
        ↓ fallback
owning package launcher PendingIntent
        ↓
PendingIntent.send()
```

It does **not** call:

```text
super.f(...)
```

and it does not reproduce `t4`'s:

```text
D()
→ pq0.M(...)
→ ParentalControlCheckActivity
```

wrapper.

Therefore the normal generic parental launch wrapping does **not** apply automatically to a Now Playing card.

This is independently reinforced by `t4.j()`:

```text
return !(this instanceof c73)
```

and `j6` uses `j()` to decide whether **Add parental control / Remove parental control** actions are available.

For a `c73`:

```text
parental toggle action = disabled
```

### Canonical interpretation

Projectivy 4.72 deliberately/special-cases Now Playing enough that:

```text
ordinary protected t4 card
    -> generic parental launch wrapper

c73 Now Playing card
    -> direct media PendingIntent
    -> no generic parental wrapper
```

The special notification fallback path also calls:

```text
c73.f(...)
```

directly, so it shares this behavior.

### Aerial-Rows improvement

Do **not** clone this exception.

All launcher-originated media launches should still route through:

```text
LaunchGate
    ↓
LaunchDispatcher
```

A sensible policy could inherit protection from the media session's owning package:

```text
protected app package
    -> derived Now Playing card protected
```

This matches the stronger protection model already found for Group 4 TV program cards.

---

# 3. Precision upgrade — Now Playing stable identity is session-level but not durable/collision-proof

Group 5 correctly says `c73.a()` is derived from the session token.

Exact form:

```text
String.valueOf(ju2.l).hashCode()
```

So it is:

- session-level rather than package-level;
- only a Java 32-bit hash;
- based on a string representation of the session token;
- not intended as a durable cross-reboot persistence identity.

This is appropriate for a transient dynamic row but should not be mistaken for a persistent media-card identity system.

### Aerial-Rows improvement

Use a structured runtime identity, for example:

```text
media:<package>:<session-token-derived-runtime-key>
```

and treat it as ephemeral.

Do not persist user customizations against a media-session runtime ID unless there is a separate stable package-level policy.

---

# 4. Precision upgrade — the row deduplicates on the underlying session token object

`w40` source type `8`:

1. requires a connected `NotificationListener`;
2. reads `notificationListener.b().b`, the active `ju2` wrappers;
3. keeps wrappers where `ju2.o()` is active;
4. deduplicates using:

```text
ju2.h()
```

which is the `z54` session token wrapper;
5. maps the unique sessions to Now Playing cards.

Therefore the row's deduplication identity is stronger than the final integer `c73.a()` card ID.

Canonical distinction:

```text
repository/list reconciliation:
    session token wrapper

card integer stable ID:
    hash(String.valueOf(token))
```

Aerial-Rows should simply retain one structured session identity throughout the repository/model layer.

---

# 5. Precision upgrade — embedded artwork support is richer on the card than in the wallpaper event

A `ju2` tracks:

```text
artwork Uri
artwork byte[]
```

`c73` keeps both.

`d73` renders in this order:

```text
artwork URI
    ↓
embedded artwork byte[]
    ↓
default Now Playing icon
```

and byte-array content participates in presenter equality/rebind checks.

However:

```text
Event.NowPlayingChanged
```

contains only:

```text
iconUri: String?
```

It has no artwork byte-array field.

Therefore Projectivy can display embedded metadata artwork on the **Now Playing card** even when the corresponding external wallpaper event cannot carry that embedded image.

### Aerial-Rows improvement

Our internal wallpaper engine does not need this limitation.

Use a common artwork model:

```text
MediaArtwork
 ├─ Uri
 ├─ Bytes/BitmapRef
 └─ None
```

for both the home card and wallpaper provider.

Only down-convert to URI-only representation when crossing an external IPC/plugin boundary that requires it.

---

# 6. Precision upgrade — exact status-bar notification settings are now known

Group 7 closed the settings behind Group 5's title/status notification behavior:

```text
appearance_statusbar_manage_notifications
appearance_statusbar_maincontainer_show_notifications
appearance_statusbar_maincontainer_autohide_notifications
```

mapped as:

```text
hj3.J1
hj3.S1
hj3.T1
```

This strengthens Group 5's statement that the title notification control is driven by shared observable notification state.

The UI decisions are independently configurable:

- expose notification-management action;
- show notification control;
- auto-hide notification control when appropriate.

---

# 7. Precision upgrade — media monitoring is not conceptually tied only to rendering the Now Playing row

The service connection establishes MediaSession monitoring independently from merely drawing category ID `12`.

Later Group 9 proves the same media state is consumed by:

```text
power_internal_idle_excludes_media_playback
```

The idle coordinator can suppress an idle/power transition when meaningful media playback is active.

Therefore:

```text
MediaSessionRepository
```

should be an application/system state service.

The home Now Playing row is only **one consumer**.

Canonical consumers include:

```text
Now Playing row
Now Playing wallpaper provider
idle/power policy
notification ↔ media correlation
```

### Aerial-Rows implication

Do not put MediaSession ownership inside the Now Playing Composable or row source.

---

# 8. Precision upgrade — idle exclusion is only one part of the full idle policy

Group 5 correctly identified:

```text
power_internal_idle_excludes_media_playback
```

Group 9 later closes the larger policy.

Idle can also consider:

- user/key/window activity;
- internal TV Activity;
- configured foreground-app conditions;
- accessibility-observed activity;
- other power settings.

So the canonical relation is:

```text
MediaSessionRepository.isPlaying
        ↓
one input into
LauncherIdleCoordinator
```

not:

```text
media repository itself owns idle behavior
```

This supports Group 12's separate `MediaSessionRepository` and `LauncherIdleCoordinator`.

---

# 9. Precision upgrade — `ProgramCardFocused` on a Now Playing card is a formal wallpaper event

Group 5 found that `d73.n(...)` publishes:

```text
Event.ProgramCardFocused
```

Group 6 gives the formal provider event model:

```text
ProgramCardFocused = 8
NowPlayingChanged  = 2
```

This means there are two distinct media-related wallpaper signals:

## Global session state

```text
MediaSessionRepository
    ↓ ~500 ms coalescing
NowPlayingChanged
```

## User focus state

```text
focused c73
    ↓
ProgramCardFocused
```

A wallpaper provider can react differently to "playback changed" versus "the user selected this particular session card."

For Aerial-Rows these should be semantic events, not direct UI-to-player calls.

---

# 10. Precision upgrade — Now Playing wallpaper provider identity

Group 6 identifies Projectivy's built-in Now Playing wallpaper provider:

```text
class: e73
UUID: b7ac18ab-d18d-4587-8ed7-50af45541033
update mode: NowPlayingChanged = 2
```

Its conceptual input is the Group 5 global media snapshot/event.

This confirms the home row and wallpaper provider are parallel consumers of one media-session state pipeline.

No duplicate media-session discovery subsystem exists inside the wallpaper engine.

---

# 11. Notification ↔ session matching is even weaker than a package correlation

Group 5 correctly calls `l63.b()` pragmatic.

The actual correlation is:

```text
notification tag == "Notification.NowPlaying"
        ↓
get category ID 12
        ↓
scan c73 cards
        ↓
compare c73.A() display title with notification title
```

It does **not** verify the notification package against the session package in this matching method.

Therefore two active sessions with identical display titles could theoretically be mis-correlated.

### Aerial-Rows improvement

Correlation preference should be:

```text
session/package identity
    ↓
notification package
    ↓
known media-session relation
    ↓
title fallback only as last resort
```

This is especially important because dismissing the linked notification can issue a real session Stop.

---

# 12. Precision upgrade — dismissing a linked notification is a playback-affecting action

The original report already captured this, but its consequence deserves stronger wording.

For a dismissable special `Notification.NowPlaying` item:

```text
dismiss
    ↓
resolve matching c73
    ↓
c73.I()
    ↓
controller Stop when supported
    ↓ fallback Pause on stop failure
    ↓
delete PendingIntent
    ↓
cancel notification
```

Thus dismissing the notification is not necessarily a purely cosmetic notification operation.

Aerial-Rows should **not assume this behavior is desirable**.

Recommended separation:

```text
Dismiss notification
```

and:

```text
Stop playback
```

should be independent actions unless a specific provider integration explicitly links them.

---

# 13. Precision upgrade — primary-session identity affects internal coalescing but not the external event payload

`iw2.b()` primary selection remains:

```text
1. first playing session
2. else first active/non-terminal session
3. else last session
```

The selected token is included in `ew2`.

Since `ew2.equals()` includes the token:

```text
session A -> session B
```

can produce a new internal snapshot even when title/artist/album/art URI happen to be identical.

But after conversion to `NowPlayingChanged`, external consumers cannot distinguish those sessions by token.

This is another reason to separate:

```text
internal state identity
```

from:

```text
external wallpaper event schema
```

in Aerial-Rows.

---

# 14. Media-command availability remains correctly capability-gated

No later group changes Group 5's finding.

Projectivy exposes:

```text
Play/Pause
Stop
Previous
Next
```

only where the underlying Media3/controller command is supported.

This remains worth preserving.

Aerial-Rows should not render a control merely because the card type is Now Playing; action availability should derive from current controller capabilities.

---

# 15. Negative finding — notification access and MediaSession access remain separate concerns

Group 5 correctly established that Projectivy obtains MediaSession access through:

```text
MEDIA_CONTENT_CONTROL
OR
enabled NotificationListener component
```

The NotificationListener service is therefore the practical permission bridge on normal installs.

But this does not mean Now Playing is reconstructed from notification text.

Later groups reinforce the original core rule:

> **Do not build Now Playing from notifications alone.**

The actual media state/control source remains active MediaSessions.

---

# 16. Exact status of Now Playing parental behavior

After Groups 3, 4 and 10, the card-policy picture is now:

```text
ordinary t4 cards
    -> generic D() protected-ID check
    -> ParentalControlCheckActivity wrapper

TV program cards / ck3
    -> provider-package protection OR card protection
    -> generic t4 wrapper

Now Playing / c73
    -> j() == false, so parental-toggle UI disabled
    -> f() overrides generic t4 launch
    -> direct media PendingIntent
```

This is a notable inconsistency in Projectivy 4.72's launch-policy coverage.

### Aerial-Rows requirement

Improve it deliberately:

```text
all actionable HomeCards
    ↓
LaunchGate
```

with source-specific inherited policy where appropriate:

```text
NowPlaying(package=X)
    inherits package X parental policy
```

---

# 17. Things Group 5 correctly deferred — not misses

These were correctly left to later groups:

- full wallpaper provider/renderer architecture -> Group 6;
- exact settings key schema -> Group 7;
- idle/power decision engine -> Group 9;
- parental policy persistence/authentication -> Group 10;
- final implementation mapping -> Group 12.

Those are clean subsystem boundaries rather than Group 5 omissions.

---

# 18. Revised canonical media architecture

With later groups folded back in:

```text
NotificationListenerService
│
├─ filtered notification repository
│   ├─ full current state
│   ├─ posted/removed/ranking events
│   ├─ group-summary bookkeeping
│   └─ notification side panel/status badge
│
└─ MediaSession bridge
    ↓
MediaSessionRepository / iw2
│
├─ active token reconciliation
├─ package denylist
├─ ju2 controller wrappers
├─ active-session filtering
├─ token-level dedup
├─ primary-session selection
└─ ~500 ms coalescing
    │
    ├─ Now Playing row
    │   └─ c73
    │      ├─ transient session identity
    │      ├─ URI/embedded-byte artwork
    │      ├─ media controls
    │      └─ direct session PendingIntent launch
    │
    ├─ internal ew2 snapshot
    │   ├─ token
    │   ├─ metadata
    │   └─ playback state
    │
    ├─ Event.NowPlayingChanged
    │   ├─ isPlaying
    │   ├─ title
    │   ├─ artist
    │   ├─ album
    │   └─ iconUri
    │       (NO token; NO artwork bytes)
    │
    └─ idle-policy media-playing signal

Focused c73
    ↓
Event.ProgramCardFocused
    ↓
wallpaper provider coordinator
```

---

# 19. Effect on Aerial-Rows implementation plan

The Group 12 implementation sequence remains correct.

The Group 5 audit strengthens Phase 7 with these requirements:

1. Keep notifications and media sessions as separate repositories.
2. Keep the MediaSession repository application-scoped.
3. Use structured runtime session IDs rather than integer hashes.
4. Keep richer internal session identity than any external wallpaper-event schema.
5. Support URI **and embedded artwork** through one media-artwork abstraction.
6. Correlate notifications to sessions by package/session identity before title fallback.
7. Do not automatically stop playback merely because a notification was dismissed unless explicitly desired.
8. Route Now Playing launches through Aerial-Rows' central `LaunchGate` even though Projectivy does not.
9. Inherit app/package parental policy onto derived Now Playing cards.
10. Expose controller commands according to capability.
11. Feed playback state into the separate idle coordinator.
12. Preserve separate global `NowPlayingChanged` and focused-card events.

---

# Final assessment

```text
Core Group 5 architecture:           correct
Missing major subsystem:             none

Factual correction:                  1
  - NowPlayingChanged IPC event does not include session/token identity

Genuine completeness omission:       1
  - c73 Now Playing launch bypasses generic parental t4 wrapper

Important precision upgrades:
  - session ID is transient 32-bit token-string hash
  - token-level dedup is stronger than card integer ID
  - card supports artwork bytes; wallpaper event does not
  - exact status-bar notification settings now known
  - media state is app/system state, not row-owned
  - notification/session title matching has no package check
  - notification dismissal can stop/pause media
  - exact Now Playing wallpaper-provider linkage
  - internal snapshot token affects change detection but is omitted from IPC

Need to redo Group 5:                 no
Effect on Group 12 plan:              strengthens Phase 7, no reorder required
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 6–12`.**


---

# SOURCE FILE: `group06.md`

SHA-256: `a227c5e9a123244f2383b45b30337692457562891deba97194bb5dc2edacc5fd`

---

# Projectivy 4.72 — Group 06: Wallpaper, Themes & Aerial Integration

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## 1. Scope

This group closes the wallpaper/background/theme subsystem sufficiently to reproduce its architecture independently in Aerial-Rows. It covers:

- wallpaper-provider abstraction
- built-in wallpaper providers
- external wallpaper-provider plugin protocol
- wallpaper event model
- wallpaper item model
- provider caching/invalidation boundaries
- renderer selection
- image/video/colour/drawable/Lottie rendering
- video playback lifecycle
- transitions and failure fallback
- user image/video backgrounds
- folder-backed backgrounds
- TV-program/Now Playing-driven backgrounds
- theme `.pltheme` packaging/import/export
- `ThemeContentProvider`
- Aerial integration design

The goal is not to copy Projectivy source; it is to understand the functional contract and data flow well enough to implement equivalent behavior cleanly.

---

## 2. Faster analysis workflow used for this group

Group 6 is the first group to use the improved persistent semantic-index workflow end-to-end.

### Persistent evidence surfaces

- Decompiled source cache: `/mnt/data/projectivy_decompiled/sources`
- Decompiled resources: `/mnt/data/projectivy_decompiled/resources`
- SQLite semantic index: `/mnt/data/re_cache/projectivy_semantic_index.sqlite`
- Indexed source files: **6,224**
- Semantic tags include wallpaper/media/TV provider/apps/settings/inputs/accessibility/boot/storage

The group workflow became:

```text
persistent source cache
        ↓
SQLite FTS / semantic role search
        ↓
small class cluster
        ↓
selected-class batch decompile in one APK load
        ↓
JADX xrefs for uncertain edges
        ↓
smali only where Java decompilation is unreliable
```

This avoids repeatedly loading and rescanning the entire APK for every class.

---

# 3. High-level architecture

Projectivy does **not** model wallpaper as a single URI preference.

The real structure is:

```text
Wallpaper source/provider
        ↓
List<Wallpaper>
        ↓
provider candidate/cache logic
        ↓
selected Wallpaper
        ↓
Wallpaper type
        ↓
renderer registry
        ↓
Activity background surface
```

The core coordinator is:

```text
defpackage.bs4
```

It owns or coordinates:

- provider registry
- selected provider
- provider connection/disconnection
- provider events
- selected/current wallpaper
- renderer registry
- Activity lifecycle
- retry/failure handling
- external-provider update broadcasts
- background transitions
- interaction with Leanback `BackgroundManager`

This architecture is a very good match for Aerial-Rows because Aerial videos can be treated as a **wallpaper provider/source**, while the existing video renderer remains generic.

---

# 4. Provider abstraction

The primary provider interface is:

```text
defpackage.m12
```

Its observed contract is effectively:

```text
WallpaperProvider
 ├─ getId()
 ├─ getName()
 ├─ getIcon()
 ├─ getWallpapers(event/context)
 ├─ getUpdateModeBitmask()
 ├─ acceptsEvent(event)
 ├─ cacheDurationMillis()
 ├─ hasSettings/configuration()
 ├─ isEventDriven()
 └─ disconnect()
```

Relevant obfuscated methods include:

```text
I(...)       provider/service connection or data retrieval boundary
J()          event/update bitmask
e()          cache duration in milliseconds
b(Event)     accepts event
d()          provider behavior characteristic / event-driven distinction
s()          provider supports configuration/settings
```

Built-in providers share a local Binder-compatible base:

```text
z05 extends tv.projectivy.plugin.wallpaperprovider.api.a implements m12
```

The built-in base defaults to:

```text
update/event mask = TimeElapsed (1)
cache duration     = 3,600,000 ms (1 hour)
```

Provider-specific persisted configuration is keyed by provider UUID via Projectivy's preference layer (`hj3.q0`).

---

# 5. Selected provider

The selected provider ID is persisted through:

```text
hj3.o0
```

The default/fallback provider UUID is:

```text
326618d8-3242-4322-a1b1-9e60e5d3dfc3
```

Projectivy can force this fallback under its own entitlement/premium rules. That entitlement logic is not required to reproduce the technical wallpaper architecture.

Provider lookup always has a fallback path rather than assuming the configured provider still exists.

This matters for external plugins that may be uninstalled.

---

# 6. Wallpaper-provider event model

Projectivy exposes a retained API package:

```text
tv.projectivy.plugin.wallpaperprovider.api
```

The primary event class is:

```text
tv.projectivy.plugin.wallpaperprovider.api.Event
```

Verified event types/bit values:

| Event | Value |
|---|---:|
| `LoadingError` | -1 |
| `TimeElapsed` | 1 |
| `NowPlayingChanged` | 2 |
| `CardFocused` | 4 |
| `ProgramCardFocused` | 8 |
| `LauncherIdleModeChanged` | 16 |

Providers declare which event families they respond to using the same bitmask concept.

### `CardFocused`

Carries focused-card information including the semantic equivalents of:

- light/palette colour
- dark/palette colour
- title
- package identity

### `ProgramCardFocused`

Carries program metadata including:

- title
- program type
- icon/artwork URI
- icon aspect ratio

### `NowPlayingChanged`

The Group 5 trace established the semantic snapshot fields used by the wallpaper layer:

- title
- artist
- album title
- artwork URI
- playing state

### `LauncherIdleModeChanged`

Contains launcher idle state.

This event is particularly useful for an Aerial integration because Aerial playback can become more active when the launcher enters idle mode without requiring a separate duplicate playback engine.

---

# 7. Wallpaper data object

The provider API exposes:

```text
tv.projectivy.plugin.wallpaperprovider.api.Wallpaper
```

Verified fields:

```text
uri
type
displayMode
title
source
author
actionUri
```

This is effectively a transport-neutral wallpaper descriptor rather than a decoded bitmap/video object.

---

# 8. Wallpaper types

The enum represented by `a15` defines:

| Type | Value |
|---|---:|
| IMAGE | 0 |
| DRAWABLE | 1 |
| ANIMATED_DRAWABLE | 2 |
| LOTTIE | 3 |
| VIDEO | 4 |
| COLOR | 5 |

The existence of `VIDEO` as a first-class wallpaper type is one of the most important Group 6 findings for Aerial-Rows.

Aerial videos do **not** need their own special background subsystem. They can be normal wallpaper items of type `VIDEO`.

---

# 9. Wallpaper display modes

The `t05` enum provides display behavior equivalent to:

```text
DEFAULT / automatic
CROP
STRETCH
FIT_CENTER
FIT_CENTER_BLURRED_BG
BLUR
```

This should be modeled independently of the wallpaper type.

For example, an IMAGE and a VIDEO can share presentation semantics without the provider having to know the renderer implementation.

---

# 10. Built-in provider registry

`bs4.n()` constructs Projectivy's built-in providers and then merges external provider services into the same provider map.

The built-in list is explicitly instantiated as ten providers.

## 10.1 None

Class:

```text
v53
```

UUID:

```text
773b5948-b4d1-49c5-b994-999210bc8877
```

Behavior:

- static
- supplies a black `COLOR` wallpaper
- effectively disables visible wallpaper imagery

---

## 10.2 Default

Class:

```text
xv0
```

UUID:

```text
326618d8-3242-4322-a1b1-9e60e5d3dfc3
```

Behavior:

- built-in default Projectivy image
- also used as fallback when configured providers disappear/fail or policy redirects to default

---

## 10.3 Gradient

Class:

```text
xt1
```

UUID:

```text
e77610c3-d495-4090-b387-fae3faafa718
```

Update mode:

```text
CardFocused = 4
```

Behavior:

- reacts to selected card changes
- derives gradient/background colors from focused-card palette information

This confirms that providers are not limited to static URI playlists.

---

## 10.4 User Image / Video

Class:

```text
jf1
```

UUID:

```text
5ca2f519-b7e3-4314-b878-40c4cc54e64b
```

Behavior:

- stores a serialized wallpaper descriptor
- accepts a user URI
- supports image or video type
- can represent colors as `COLOR` wallpaper items
- settings include URI/picker and video mode

This provider is the closest existing Projectivy analogue to manually choosing a single Aerial/video asset.

---

## 10.5 Folder

Class:

```text
ek1
```

UUID:

```text
0b52e4f1-78c9-4d65-a732-d5f95f4665b7
```

Behavior:

- scans a selected file/tree URI
- supports recursive content discovery
- accepts at least:
  - `image/*`
  - `video/*`
  - `application/json` for Lottie

This proves mixed media wallpaper playlists are part of the provider concept.

---

## 10.6 Selected Program

Class:

```text
o34
```

UUID:

```text
895c68b3-6522-43b8-85bc-9a1624c41850
```

Update mode:

```text
ProgramCardFocused = 8
```

Behavior:

- reacts to focused Android TV program cards
- uses the selected program's artwork as background

This links directly to Group 4's `Event.ProgramCardFocused` trace.

---

## 10.7 Now Playing

Class:

```text
e73
```

UUID:

```text
b7ac18ab-d18d-4587-8ed7-50af45541033
```

Update mode:

```text
NowPlayingChanged = 2
```

Behavior:

- reacts to media-session changes
- derives wallpaper from currently playing media artwork
- carries title/album/artist context

This links directly to Group 5's media-session architecture.

---

## 10.8 Random TV Program Artwork

Class:

```text
rp3
```

UUID:

```text
eeac320d-bf54-449d-b852-b9048acd7c4b
```

Behavior:

- reuses the Group 4 TV provider repository
- requests Preview Programs globally (`e70.c(0L)`)
- filters unsuitable content/aspect information
- converts program artwork into image wallpaper descriptors
- preserves program/action URI so the wallpaper can remain actionable

Cache duration:

```text
7,200,000 ms = 2 hours
```

This confirms that TV content is reusable as a wallpaper source rather than being coupled to home rows.

---

## 10.9 Reddit

Class:

```text
is3
```

UUID:

```text
eea7a685-d68f-4fb5-b46e-b4897af5413f
```

Behavior:

- RSS/network wallpaper provider
- supports predefined content sources plus custom subreddit/source configuration

Observed source labels include concepts such as:

- Earth
- Projectivy Wallpapers
- Projectivy TMDB
- TMDB Wallpapers
- ArtWp
- TvDark Wallpapers
- Wallpaper AndroidTV
- Astrophotography
- Wallpapers
- Big Wallpapers
- Botanical
- Sea
- Sky
- Car
- Animal
- DALL·E & Sora
- Midjourney
- Custom

The exact remote feed URLs are implementation details rather than architecture requirements for Aerial-Rows.

---

## 10.10 Theme Wallpaper

Class:

```text
em4
```

UUID:

```text
78edbc3e-7e0e-49b6-8bf0-3787ee67b1c8
```

Behavior:

- obtains wallpaper files packaged in the currently selected `.pltheme`
- exposes them through `ThemeContentProvider`
- maps file type to wallpaper type

Observed mappings include:

```text
.json               → LOTTIE
.mp4/.mkv/.webm     → VIDEO
other image media   → IMAGE
```

---

# 11. External wallpaper-provider plugin protocol

This is a real plugin boundary rather than a convention internal to Projectivy.

Projectivy discovers external services using:

```text
Intent action:
tv.projectivy.plugin.WALLPAPER_PROVIDER
```

The lookup is performed with `PackageManager.queryIntentServices(...)`.

External providers are wrapped by:

```text
y05 implements m12
```

### Required/recognized service metadata

Observed metadata keys:

```text
uuid
name
updateMode
settingsActivity
itemsCacheDurationMillis
apiVersion
```

Requirements include:

```text
uuid length = 36
apiVersion = 1
```

Defaults include:

```text
updateMode = 1
itemsCacheDurationMillis = 3,600,000
```

Providers can expose their own settings Activity.

---

# 12. Binder API

The Binder descriptor is:

```text
tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
```

The proxy class `n12` exposes three observed transactions:

```text
1 → List<Wallpaper> A(Event)
2 → String v()
3 → void k(String)
```

Semantically these represent:

```text
get wallpapers for event
read provider configuration/state
write provider configuration/state
```

Projectivy synchronizes persisted provider configuration with external providers when connecting/disconnecting.

This is sufficiently understood to recreate an equivalent plugin SPI without copying implementation source.

---

# 13. Provider-update broadcast

External providers can signal that their data has changed through:

```text
tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED
```

Observed extras:

```text
tv.projectivy.plugin.extra.PROVIDER_ID

tv.projectivy.plugin.extra.UPDATE_REASON
```

Receiver:

```text
WallpaperProviderUpdateReceiver
```

`bs4.s()` debounces repeated update signals from the same provider for:

```text
5,000 ms
```

If the updated provider is currently selected, Projectivy invalidates its current source state and requests a new wallpaper.

This is a useful pattern for Aerial catalog/download updates.

---

# 14. Provider candidate/cache behavior

Projectivy maintains a small provider candidate-cache state object (`s05`) containing:

```text
cached wallpaper list
cache timestamp
provider ID
```

Each provider supplies its own cache duration through `e()`.

Verified examples:

```text
built-in default duration ≈ 1 hour
Random TV Program provider = 2 hours
```

The exact coroutine implementation around candidate refresh contains decompiler-ambiguous control flow, so this report intentionally does **not** claim an exact line-by-line cache algorithm.

The reliable architectural facts are:

1. candidates are cached by provider identity;
2. the cache records time;
3. provider-specific TTL exists;
4. provider update broadcasts clear the matching cache;
5. switching/invalidation clears current wallpaper state and candidate state as needed.

That is enough to reproduce behavior robustly.

---

# 15. Renderer architecture

Wallpaper sources and wallpaper renderers are explicitly separate.

Base renderer:

```text
mu
```

The base responsibility includes:

- declare supported wallpaper types
- accept/apply a wallpaper
- lifecycle start/resume/stop/release
- reject unsupported/empty input
- avoid redundant reloads
- provide palette/dynamic-color information to the launcher

### Exact renderer registry

A final verification of `MainActivity.w(Bundle)` confirms the registry is explicitly instantiated as **five renderer instances**:

```text
v32                 image
djy4 / jy4           video
dc0(..., 0)          color
o21                  drawable / animated drawable
dc0(..., 1)          Lottie
```

and assigned to:

```text
bs4.O
```

This is important:

```text
Providers are extensible/dynamic
Renderers are a fixed type registry
```

Aerial should therefore add a **provider**, not a separate renderer, because VIDEO rendering already exists generically.

---

# 16. IMAGE renderer

Class:

```text
v32 extends o21
```

Observed behavior:

- loads images through Glide
- applies display/crop/blur-related transformations
- constrains very large loads to roughly 3840×2160 scale bounds
- extracts palette information
- supports HDR/gainmap-related handling on capable Android versions/devices
- can alter window color-mode behavior according to HDR content

The exact Glide transformation stack is not necessary to reproduce Projectivy's architecture, but the image renderer clearly owns decoding/rendering rather than the provider.

---

# 17. DRAWABLE / ANIMATED_DRAWABLE renderer

Class:

```text
o21
```

Behavior includes:

- static drawable display
- `Animatable` lifecycle start/stop where applicable
- wallpaper color/filter adjustments
- palette extraction

This is also the shared image/drawable presentation base used by the image path.

---

# 18. COLOR renderer

One variant of:

```text
dc0
```

handles `COLOR` wallpaper items.

This lets providers return a color descriptor through the same wallpaper pipeline rather than requiring special home-screen logic.

---

# 19. Lottie renderer

A second `dc0` variant handles:

```text
LOTTIE
```

This is why folder/theme providers can expose JSON animation assets as ordinary wallpaper descriptors.

---

# 20. VIDEO renderer

Class:

```text
jy4
```

This is the critical Aerial-relevant renderer.

Verified behavior:

- uses AndroidX Media3 / ExoPlayer
- attaches a full-screen `SurfaceView` behind launcher content
- supports ordinary Media3 media sources
- contains special RTSP handling
- maintains playback lifecycle independently from provider logic
- releases/stops appropriately on Activity lifecycle transitions
- can resume playback position where sensible
- maintains a global “video wallpaper playing” state (`bs4.M`)
- can use `PixelCopy` frames to extract palette/dynamic colors from video output
- reports loading/playback errors back to `bs4`

This conclusively shows that Projectivy treats video as a real background medium rather than as a screensaver-only feature.

---

# 21. Video lifecycle and failure behavior

`bs4` selects a renderer according to `Wallpaper.getEnumType()`.

When wallpaper type changes:

```text
old renderer
    ↓ stop/release as appropriate
new renderer
    ↓ load wallpaper
```

Projectivy tracks load failures.

`bs4.q()` retries up to:

```text
5 attempts
```

After repeated failure it emits:

```text
Event.LoadingError
```

and enters fallback/next-wallpaper behavior.

For an Aerial provider this means broken/deleted/network-unavailable videos can naturally fall through the same engine rather than crashing or leaving a black background.

---

# 22. Background transitions

User-level transition modes are represented by `ye`:

```text
RANDOM
NONE
FADE
ZOOM
SLIDE_H
SLIDE_V
```

The lower rendering/background layer uses `fs` values conceptually equivalent to:

```text
NONE
FADE_CROSS
FADE_ZOOM
FADE_SLIDE_H
FADE_SLIDE_V
```

Random mode chooses among non-NONE transitions.

For drawable/image-type backgrounds, Projectivy integrates with AndroidX Leanback `BackgroundManager`.

VIDEO is managed by its own video surface rather than being forced through `BackgroundManager`.

---

# 23. Dynamic palette / launcher colors

Wallpaper renderers can feed palette/color information back into the launcher when the relevant appearance preference is enabled (`hj3.l1`).

For image backgrounds this is obtained from decoded bitmap content.

For video, Projectivy samples output frames using `PixelCopy`.

This is useful for Aerial-Rows because Aerial videos can provide dynamic title/header colors without provider-specific logic.

The color extraction should live in the renderer/engine layer, not the Aerial provider.

---

# 24. Wallpaper settings screen

The background/settings controller is represented by:

```text
defpackage.we
```

It adapts its controls to the selected provider.

Verified provider-specific controls include:

### Theme

- choose theme
- theme URI

### User image/video

- pick image/file
- choose color
- URI
- video toggle

### Folder

- choose directory/tree
- URI

### Reddit

- predefined source
- custom subreddit/source string

### Generic static providers

Providers with ordinary timed updates can asynchronously fetch candidates and expose those candidates in a wallpaper picker (`ImagePickerActivity`).

This is an important design clue: provider settings and wallpaper-item picking are related but not the same abstraction.

---

# 25. External ACTION_VIEW / ACTION_SEND background import

Named Activity:

```text
com.spocky.projengmenu.ui.launcherActivities.SetBackgroundActivity
```

It accepts image/video content from Android intents.

The URI can arrive from:

```text
Intent.EXTRA_STREAM
```

or the Activity's data URI.

The import helper (`j02`) asks `ContentResolver.getType(uri)`.

Verified classification:

```text
MIME starts with video/ → VIDEO
otherwise               → IMAGE
```

The deeper import/copy routine is partly affected by JADX control-flow reconstruction errors, so this report does not claim exact file-copy semantics where the Java output is uncertain.

The reliable behavior is that Projectivy can accept externally shared/opened image or video URIs and turn them into local wallpaper configuration.

---

# 26. Theme file format

Built-in `.pltheme` resources in the APK include:

```text
res/56.pltheme
res/8d.pltheme
res/9Z.pltheme
res/rd.pltheme
```

They are ZIP archives.

Each includes at least:

```text
manifest
appearance.plbackup
```

and may include:

```text
wallpapers/
```

Observed manifest metadata includes fields conceptually equivalent to:

```text
Projectivy Launcher version
date
author
name
description
URL
credits
```

Built-in theme names observed include:

- One Line to Rule Them All
- Projectivy: Legacy
- Journey
- Forbidden Fruit

Important: `appearance.plbackup` should be treated as a Projectivy backup payload. Direct inspection does **not** justify calling it JSON, so this report deliberately does not assume a textual schema.

---

# 27. Theme export

The export/import coordinator includes:

```text
dj3
```

Theme export creates names resembling:

```text
projectivy-theme-[...].pltheme
```

The output is a ZIP archive using `ZipOutputStream`.

Observed contents:

```text
manifest
appearance.plbackup
optional wallpapers/
```

Wallpaper assets can be embedded when their URIs are exportable through normal file/content access.

Unsupported/nonportable URI types can trigger warnings rather than being silently assumed portable.

---

# 28. Theme import

Named Activity:

```text
ImportThemeActivity extends ImportSettingsActivity
```

It uses the common import pipeline (`dj3`) with a theme-specific mode.

The current theme can subsequently expose its packaged wallpaper files through `ThemeContentProvider`.

---

# 29. ThemeContentProvider

Provider authority:

```text
com.spocky.projengmenu.themeprovider
```

It is read-only to ordinary consumers and serves current-theme wallpaper content.

Observed conceptual URI pattern:

```text
content://com.spocky.projengmenu.themeprovider/current/wallpaper/<encoded-name>
```

Known MIME mappings cover media such as:

```text
jpg/jpeg
png
webp
gif
mp4
mkv
webm
```

The theme provider can read packaged entries from the active theme archive/extracted state and return them as normal Android content URIs.

This avoids making renderers understand ZIP archives.

That is an excellent separation of concerns to preserve.

---

# 30. Important cross-group integrations

Group 6 closes several links discovered in earlier groups.

## From Group 2 — focused cards

```text
home card focus
    ↓
Event.CardFocused
    ↓
Gradient or external provider
```

## From Group 4 — Android TV programs

```text
program focus
    ↓
Event.ProgramCardFocused
    ↓
Selected Program provider
```

The Random TV Program provider also reuses the TV Provider repository directly.

## From Group 5 — media sessions

```text
primary media-session change
    ↓
Event.NowPlayingChanged
    ↓
Now Playing wallpaper provider
```

## Launcher idle state

```text
idle mode changes
    ↓
Event.LauncherIdleModeChanged
    ↓
providers interested in idle state
```

This event bus is the architectural location where an Aerial provider can react to idle state cleanly.

---

# 31. Aerial integration conclusion

The correct integration is **not**:

```text
LauncherScreen
  if aerialEnabled
     create special video player
```

That would duplicate background rendering, lifecycle and error handling.

The Projectivy-like architecture should be:

```text
AerialProvider
       ↓
WallpaperSpec(type = VIDEO)
       ↓
WallpaperEngine
       ↓
VideoRenderer
       ↓
Media3 player / Surface
```

The Aerial provider is responsible for **selection/catalog policy**.

The generic video renderer is responsible for **playback**.

---

# 32. Recommended Aerial-Rows design

## Core model

```text
WallpaperSpec
 ├─ uri
 ├─ type
 ├─ displayMode
 ├─ title
 ├─ source
 ├─ author
 └─ actionUri
```

## Provider contract

```text
WallpaperProvider
 ├─ id
 ├─ name
 ├─ icon
 ├─ supportedEvents
 ├─ cacheDuration
 ├─ settingsDestination?
 ├─ getWallpapers(event)
 └─ invalidate()
```

## Engine

```text
WallpaperEngine
 ├─ selected provider
 ├─ provider registry
 ├─ candidate cache
 ├─ current item
 ├─ event dispatch
 ├─ timed rotation
 ├─ retry/fallback
 └─ Activity lifecycle
```

## Renderer registry

```text
WallpaperRendererRegistry
 ├─ ImageRenderer
 ├─ VideoRenderer
 ├─ ColorRenderer
 ├─ DrawableRenderer
 └─ LottieRenderer
```

## Aerial provider

```text
AerialProvider : WallpaperProvider
 ├─ reads Aerial-Rows built-in aerial catalog
 ├─ maps available videos to VIDEO WallpaperSpec
 ├─ filters by selected locations/categories
 ├─ supports local/cache/network URI policy
 ├─ reacts to TimeElapsed
 ├─ optionally reacts to LauncherIdleModeChanged
 └─ exposes Aerial-specific settings
```

The existing Aerial-Rows `BuiltinAerials` / `aerials.json` data can feed this provider rather than being tied directly to a Composable player.

---

# 33. Screensaver / idle strategy

Aerial-Rows can improve on a literal Projectivy clone while preserving the same architecture.

Instead of maintaining two video systems:

```text
wallpaper video player
+
screensaver video player
```

use one playback engine with policy/state:

```text
launcher active
    → background-mode Aerial behavior

launcher idle
    → immersive Aerial behavior

user interaction
    → restore normal launcher presentation
```

`LauncherIdleModeChanged` is exactly the kind of event needed for this.

The provider can decide whether to rotate/change content; the video renderer does not need to know why it is playing.

---

# 34. What should be reproduced vs improved

## Reproduce

- provider abstraction
- typed wallpaper descriptors
- fixed renderer registry
- image/video/color/Lottie support
- event-driven providers
- timed providers
- provider-specific TTL
- provider update/invalidation path
- retry/fallback
- palette extraction
- theme-contained wallpapers
- user/folder video backgrounds
- TV-program and Now Playing providers

## Improve in Aerial-Rows

- Kotlin sealed models rather than obfuscated integer-enum handling
- explicit `StateFlow`/`SharedFlow` event stream
- a real typed provider-cache object
- optional file/download cache for Aerial media
- clearer renderer lifecycle ownership
- one video engine for wallpaper + immersive idle/screensaver presentation
- optional external plugin compatibility layer without coupling internal providers to Binder

---

# 35. Reliability / ambiguity notes

### High confidence / directly verified

- provider registry architecture
- ten built-in providers
- provider UUIDs listed above
- external provider service intent
- external provider metadata names/defaults
- Binder descriptor and three transaction roles
- provider-update broadcast and 5-second debounce
- Wallpaper fields
- event types and bit values
- wallpaper type enum
- display-mode enum
- exact five-renderer registry initialized by `MainActivity`
- Media3 video renderer
- SurfaceView background rendering
- PixelCopy palette extraction
- retry count of five
- theme ZIP structure
- ThemeContentProvider role
- user image/video MIME classification

### Architecture verified but exact control flow partly decompiler-ambiguous

- coroutine implementation of provider candidate cache refresh
- exact file-copy/persist sequence inside the generic imported-background helper
- portions of external-provider state synchronization coroutine

For those paths, this report intentionally records only behavior confirmed across fields, callers, xrefs and/or smali rather than inventing a source-level reconstruction.

---

# 36. Group 6 conclusion

Projectivy's wallpaper system is already architected in the way Aerial-Rows needs:

```text
content/provider policy
        ≠
rendering/playback
```

The decisive result is:

```text
Aerials belong in a WallpaperProvider,
not in a special LauncherScreen video path.
```

Aerial video items can be ordinary `VIDEO` wallpaper descriptors consumed by the same generic Media3 renderer that handles any other video source.

That gives Aerial-Rows:

- Projectivy-style backgrounds
- Couchy/Aerial content
- event-driven dynamic wallpaper
- media/program-aware providers
- theme support
- future external provider plugins
- one reusable video playback lifecycle

without duplicating player logic or coupling wallpaper selection to home-screen rendering.

---

## Next group

**Group 07 — Settings system**

Targets include:

- complete settings screen hierarchy
- preference/config key inventory
- persistence wrapper architecture
- toggle/choice/list/dropdown widgets
- focus retention after preference changes
- settings navigation/routes
- dependency/visibility rules
- category settings integration
- user-facing value mappings
- preference change propagation to the home screen



---

# SOURCE FILE: `group06_postclosure_audit.md`

SHA-256: `cc6046f4661d9ddeb468103cecb161433c427e55665d1b9c71efb1f6122afed2`

---

# Group 6 post-closure audit — Wallpaper, Themes & Aerial Integration

Status: **POST-CLOSURE AUDITED after Groups 7–12**  
Original Group 6 SHA-256: `a227c5e9a123244f2383b45b30337692457562891deba97194bb5dc2edacc5fd`

## Executive conclusion

The original Group 6 architecture remains correct.

No later group revealed a second hidden wallpaper engine or a competing Aerial/video subsystem. The central result still holds:

```text
provider/source policy
        ≠
renderer/playback
```

with:

```text
WallpaperProvider
    ↓ Wallpaper descriptor
bs4 engine/coordinator
    ↓
fixed renderer registry
    ├─ IMAGE
    ├─ VIDEO
    ├─ COLOR
    ├─ DRAWABLE / ANIMATED_DRAWABLE
    └─ LOTTIE
```

The post-closure audit found no architectural reversal, but it did add several useful runtime details that were not fully captured in the original report.

Most important additions:

1. exact timed `TimeElapsed` scheduling behavior;
2. exact minimum-width / wrong-format fallback policy;
3. exact video audio, tunneling and decoder-fallback wiring;
4. dedicated video dynamic-color sampling cadence;
5. exact suppression behavior for timed updates while video is playing;
6. the remote Play/Pause wallpaper-advance shortcut;
7. a small additional external-provider validation rule;
8. stronger theme-package semantics from Group 10.

---

# 1. Genuine completeness upgrade — exact wallpaper update-delay semantics

Group 6 recorded the existence of timed provider updates but did not fully close the global scheduling policy.

The setting is:

```text
appearance_wallpaper_update_delay
```

with:

```text
default = 60 seconds
min     = 0
max     = 3600 seconds
step    = 15 seconds
```

`bs4.h()` converts the stored number of seconds into milliseconds.

The recurring scheduler uses:

```text
elapsed = now - lastWallpaperEventTime
remaining = updateDelay - elapsed
```

and, when scheduling another tick, enforces a minimum remaining delay of:

```text
5,000 ms
```

unless the configured update delay is effectively disabled.

The smali-verified scheduler behavior is conceptually:

```text
if updateDelay == 0 and prior event time exists:
    do not schedule another periodic timer

else:
    nextDelay = max(updateDelay - elapsedSinceLastEvent, 5000 ms)
    schedule TimeElapsed
```

After the timer fires:

```text
Event.TimeElapsed
    ↓
provider event path
    ↓
scheduler interval resets from current update-delay setting
```

The timing holder also observes relevant state changes so the interval can be updated without rebuilding the entire wallpaper engine.

### Aerial-Rows implication

Use one explicit scheduler owned by the wallpaper engine:

```text
WallpaperRotationScheduler
```

with:
- configured interval;
- monotonic next-run calculation;
- immediate cancellation when interval becomes zero;
- no duplicate timers;
- a modest minimum reschedule floor to avoid update storms.

---

# 2. Genuine completeness upgrade — “ignore updates while video is playing” is narrowly scoped

The setting is:

```text
appearance_wallpaper_ignore_update_on_video_playing
```

default:

```text
true
```

The exact suppression occurs inside `bs4.r(Event)`.

Projectivy drops an event only when all of these are true:

```text
a previous wallpaper event timestamp exists
AND
the video renderer reports a video is actively playing
AND
event type == TimeElapsed
AND
ignore-update-on-video-playing == true
```

So this option does **not** suppress all wallpaper events while video is playing.

It suppresses the periodic:

```text
TimeElapsed
```

rotation/update event.

Events such as:
- focused-card changes;
- Now Playing changes;
- idle-state changes;
- loading errors;

are not covered by this particular suppression branch.

### Aerial-Rows implication

Model this as:

```text
suppressTimedRotationWhileVideoPlaying
```

rather than a vague global `ignoreWallpaperUpdates` flag.

---

# 3. Precision upgrade — the video renderer changes repeat policy based on timed-update behavior

When the Media3 player is created, Projectivy sets its repeat mode according to:

```text
ignore-update-on-video-playing
+
wallpaper update delay
```

The effective logic is:

```text
if ignoreTimedUpdatesWhileVideoPlaying == false
   OR updateDelay == 0:
       repeat mode = ONE
else:
       repeat mode = OFF
```

This is consistent with two modes:

```text
loop the current video indefinitely
```

versus:

```text
allow the current video lifecycle/end state to participate in wallpaper rotation
```

The exact end-state callback still belongs to the generic renderer/engine retry/next-wallpaper logic.

### Aerial-Rows improvement

Make looping policy explicit in the provider/renderer contract rather than deriving it indirectly from two unrelated-looking preferences.

For example:

```text
VideoPlaybackPolicy(
    loop = true/false,
    rotateOnTimedEvent = true/false
)
```

---

# 4. Genuine completeness upgrade — exact minimum-width / bad-format image policy

Two settings control this behavior:

```text
appearance_wallpaper_min_accepted_width
appearance_wallpaper_wrong_format_display_mode
```

The minimum-width setting is:

```text
default = 800 px
min     = 0
max     = 4000 px
step    = 100 px
```

The normal global display mode is:

```text
appearance_wallpaper_display_mode
```

and defaults to:

```text
Automatic
```

For the transformation path where format validation is enabled, Projectivy considers the image acceptable for the normal display mode when:

```text
display mode == FIT_CENTER
```

or:

```text
image aspect ratio > 1.0
AND
bitmap width >= minimum accepted width
```

Otherwise it applies the separate:

```text
wrong-format display mode
```

The wrong-format mode defaults to:

```text
BLUR
```

The enum's first/default slot is relabelled as:

```text
Skip
```

in this particular settings context.

At transform time that slot produces no replacement bitmap, effectively causing the unwanted bitmap to be skipped.

### Canonical policy

```text
normal landscape / wide-enough image
    → normal wallpaper display mode

portrait, square, or too-narrow image
    → wrong-format fallback mode
        ├─ crop/stretch/etc.
        ├─ blurred presentation
        └─ skip
```

### Aerial-Rows implication

This belongs in the **renderer/image-policy layer**, not in each provider.

An Aerial provider should never need to know about portrait-image fallback.

---

# 5. Genuine completeness upgrade — video volume 0 means disable audio track selection

The setting is:

```text
appearance_wallpaper_video_volume
```

with:

```text
default = 0.0
range   = 0.0 .. 1.0
step    = 0.05
```

Projectivy does more than simply call:

```text
player.setVolume(0)
```

When the configured volume is exactly zero, it configures the track selector to disable track type:

```text
1
```

which is the Media3 audio track type.

So zero-volume video wallpaper effectively avoids selecting audio tracks.

When volume is greater than zero:

```text
audio track selection is allowed
```

and Projectivy calls the player's volume setter when the player reports that command as available.

### Aerial-Rows improvement

Keep this optimization.

For silent Aerial wallpaper:

```text
do not select/decode audio
```

is better than:

```text
decode audio then multiply it by zero
```

for resource usage on weaker Android TV hardware.

---

# 6. Genuine completeness upgrade — tunneling and decoder fallback are real Media3 construction settings

Two settings are directly wired into player construction:

```text
appearance_wallpaper_video_tunneling
appearance_wallpaper_video_decoders_fallback
```

Defaults:

```text
video tunneling       = true
decoder fallback      = false
```

The tunneling preference is passed into the Media3 track-selector configuration.

The decoder-fallback preference is passed into the renderer-factory configuration used by the ExoPlayer builder.

This is not merely UI metadata; both settings affect the actual player instance created by `jy4`.

### Aerial-Rows implication

These can remain advanced video options, but they belong under:

```text
VideoRendererConfig
```

rather than the Aerial provider itself.

---

# 7. Genuine completeness upgrade — dedicated dynamic-color update frequency for video

Group 6 correctly found that video colors are sampled with `PixelCopy`, but did not record the dedicated cadence control.

The setting is:

```text
appearance_wallpaper_video_dynamic_color_update_frequency_nexp
```

The renderer captures:

```text
frequencySeconds * 1000
```

at construction.

Its default is:

```text
5 seconds when Projectivy's internal capability predicate is true
0 otherwise
```

and its configured range extends up to:

```text
60 seconds
```

When:
- dynamic wallpaper colors are enabled;
- video playback is active;
- frequency is non-zero;

Projectivy starts a coroutine that repeatedly triggers the PixelCopy/palette path.

If frequency is zero, the renderer avoids that repeated sampling path and falls back to its non-periodic color behavior.

The `_nexp` suffix also matters after Group 10:

```text
nexp settings are excluded from exported backup/theme preference payloads
```

So this video palette cadence is intentionally treated as local/non-exported configuration.

### Aerial-Rows improvement

Keep palette sampling independently configurable from:
- video playback;
- wallpaper rotation;
- provider update frequency.

They solve different problems.

---

# 8. Precision upgrade — exact meaning of the global video-playing flag

Group 6 identified:

```text
bs4.M
```

as the global “video wallpaper playing” state.

The post-closure audit closes its lifecycle more explicitly.

`jy4` sets:

```text
bs4.M = true
```

when the renderer transitions into active playback while the owning Activity is in the appropriate lifecycle state.

It sets:

```text
bs4.M = false
```

when playback is stopped/released through the renderer shutdown path.

The flag is then consumed by `bs4.r()` for the timed-update suppression described above.

So `bs4.M` is not a generic:
```text
selected wallpaper type == VIDEO
```

flag.

It represents active video playback state.

---

# 9. Precision upgrade — manual `TimeElapsed` trigger from the remote

Group 9 later exposed a useful input bridge that belongs in the canonical Group 6 event model.

Inside `MainActivity.onKeyDown()`:

```text
KEYCODE_MEDIA_PLAY_PAUSE (85)
```

on:

```text
repeatCount == 1
```

checks whether the active wallpaper provider accepts:

```text
Event.TimeElapsed
```

If it does, Projectivy dispatches that event through the wallpaper coordinator and consumes the key.

Conceptually:

```text
hold / first repeat of Play-Pause
    ↓
provider supports TimeElapsed?
    ↓ yes
manual wallpaper TimeElapsed event
```

This effectively gives compatible wallpaper providers a remote-driven “advance/update” gesture.

### Aerial-Rows option

This is worth keeping as an optional configurable remote shortcut, but it should live in the generic remote-action mapping system rather than being hard-coded into `MainActivity`.

---

# 10. Precision upgrade — loading error remains exempt from provider event-subscription filtering

`bs4.r(Event)` normally checks:

```text
selectedProvider.b(event)
```

to determine whether the provider is interested in an event.

However:

```text
Event.LoadingError
```

is allowed through even when the provider's ordinary event mask does not include it.

This matches Group 6's retry model:

```text
renderer/load failure
    ↓
retry up to five times
    ↓
LoadingError
    ↓
fallback / next-wallpaper path
```

So loading errors are an engine-level reliability signal rather than merely one optional provider subscription.

---

# 11. Precision upgrade — external providers also pass an in-memory blacklist check

Group 6 already verified:

```text
apiVersion == 1
UUID length == 36
```

for external wallpaper-provider plugins.

The provider discovery path also checks the UUID against:

```text
MainActivity.s0
```

and rejects the provider when the UUID appears in that in-memory blacklist.

The audit does not establish how every blacklist entry is populated, so the safe canonical claim is only:

> An additional runtime UUID blacklist check exists during external-provider discovery.

### Aerial-Rows implication

If we eventually support an external wallpaper-provider API, use an explicit compatibility/denylist layer with documented reasons rather than an opaque global list.

---

# 12. Precision upgrade — provider update broadcasts and timed rotation are separate mechanisms

This distinction was implicit in Group 6 but is useful to make explicit.

## Provider update broadcast

```text
tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED
```

means conceptually:

```text
the provider's candidate data changed
```

and Projectivy:
- debounces it for 5 seconds per provider;
- invalidates the matching cache;
- refreshes immediately when relevant.

## `TimeElapsed`

means conceptually:

```text
the wallpaper engine's rotation/update interval elapsed
```

and is controlled by:

```text
appearance_wallpaper_update_delay
```

These should remain different mechanisms in Aerial-Rows.

Do not implement provider catalogue refreshes by waiting for the wallpaper rotation timer.

---

# 13. Theme-package semantics strengthened by Group 10

Group 6 correctly identified:

```text
.pltheme ZIP
├─ manifest
├─ appearance.plbackup
└─ wallpapers/   optional
```

Group 10 closes the format further.

The theme manifest is ordinary JSON with typed fields including:

```text
projectivyLauncherVersion
themeVersion
date
author
name
description
URL
credits
```

`appearance.plbackup` is:

```text
an encrypted appearance-settings backup payload
```

using Projectivy's backup machinery, not a plain JSON settings document.

Theme scope is deliberately different from a full user backup.

A theme can include:
- appearance preferences;
- wallpaper-provider selection;
- selected category appearance/configuration;
- optional wallpaper assets;

while user content/membership/exclusions are treated separately so applying a theme does not simply replace the user's home content model.

### Canonical distinction

```text
full backup
    = user state

theme
    = presentation / appearance package
```

### Aerial-Rows implication

Preserve exactly that conceptual separation even though our own file format should use cleaner authenticated/versioned serialization.

---

# 14. Idle integration — what is directly verified vs architectural conclusion

Group 6 identified:

```text
LauncherIdleModeChanged
```

as a first-class wallpaper event.

Group 9 directly verifies the **exit** path:

```text
user key/touch interaction while launcher idle
    ↓
MainActivity.F()
    ↓
LauncherIdleModeChanged(false)
    ↓
rows/title/status UI fades back in
```

The combined Group 6 + Group 9 architecture supports using the same wallpaper/video renderer for an immersive idle presentation.

The safest canonical statement is:

```text
idle state is communicated semantically to the wallpaper provider/engine
```

and not:

```text
Projectivy launches a separate Aerial/video player
```

No later group found a second video renderer dedicated to idle mode.

### Aerial-Rows design remains valid

```text
one Media3 video renderer
    ↓
normal wallpaper presentation
    ↕ idle-state event
immersive Aerial presentation
```

---

# 15. Precision upgrade — dynamic-color sampling is renderer state, not provider state

The later raw verification reinforces an important design rule.

The following all live in `jy4` / rendering infrastructure:

```text
PixelCopy
palette extraction
sampling cadence
SurfaceView dimensions
temporary bitmap reuse
sampling thread
```

The provider simply produces:

```text
VIDEO Wallpaper
```

Therefore an Aerial provider should never contain:
- PixelCopy logic;
- palette extraction;
- SurfaceView ownership;
- ExoPlayer construction.

This separation is one of the strongest parts of Projectivy's architecture and should be preserved.

---

# 16. Negative verification — no hidden second Aerial/video subsystem appeared later

Groups 7–12 did not uncover:
- a second video player;
- a dedicated screensaver-only Aerial engine;
- a separate idle video Activity;
- provider-specific ExoPlayer ownership.

The five-renderer registry from Group 6 remains the authoritative rendering architecture.

This means the Group 12 recommendation is still correct:

```text
BuiltinAerials
    ↓
AerialProvider
    ↓
Wallpaper(type = VIDEO)
    ↓
shared Media3 VideoRenderer
```

---

# 17. Things Group 6 correctly deferred — not misses

The following remained appropriate subsystem boundaries:

- exact preference schema and settings UI → Group 7;
- idle/remote gesture state machine → Group 9;
- backup/theme encryption and export scope → Group 10;
- final implementation mapping → Group 12.

Later findings enrich Group 6 but do not invalidate its scope.

---

# 18. Revised canonical wallpaper architecture

With Groups 7–12 folded back in:

```text
WallpaperEngine / bs4
│
├─ ProviderRegistry
│   ├─ built-ins
│   └─ external Binder providers
│       ├─ API version check
│       ├─ UUID validation
│       └─ runtime UUID blacklist
│
├─ EventBus
│   ├─ TimeElapsed
│   ├─ NowPlayingChanged
│   ├─ CardFocused
│   ├─ ProgramCardFocused
│   ├─ LauncherIdleModeChanged
│   └─ LoadingError
│
├─ RotationScheduler
│   ├─ update delay, default 60 s
│   ├─ 0 disables periodic scheduling
│   ├─ 5 s minimum reschedule floor
│   └─ optional suppression while video plays
│
├─ CandidateCache
│   ├─ provider identity
│   ├─ provider-specific TTL
│   └─ provider-update invalidation
│
├─ RendererRegistry
│   ├─ Image
│   ├─ Video
│   ├─ Color
│   ├─ Drawable / AnimatedDrawable
│   └─ Lottie
│
└─ Presentation policy
    ├─ normal display mode
    ├─ minimum accepted image width
    ├─ wrong-format fallback
    ├─ transition
    └─ dynamic color
```

Video renderer:

```text
jy4
├─ SurfaceView
├─ Media3 / ExoPlayer
├─ RTSP path
├─ decoder fallback config
├─ tunneling config
├─ true audio-track disable at volume 0
├─ configurable positive volume
├─ repeat/rotation policy
├─ PixelCopy palette sampling
├─ configurable palette cadence
├─ active-playing state
└─ lifecycle/retry/fallback integration
```

---

# 19. Effect on Aerial-Rows implementation plan

The Group 12 implementation order remains correct.

The Group 6 audit strengthens Phase 5 with these requirements:

1. Keep Aerial as a **provider**, not a renderer.
2. Keep one shared Media3 video renderer for every VIDEO provider.
3. Give timed wallpaper rotation its own scheduler.
4. Distinguish timed rotation from provider-catalogue invalidation.
5. Make “ignore updates while video plays” specifically suppress timed rotation.
6. Explicitly model video loop/rotation policy.
7. Disable audio track selection entirely when video volume is zero.
8. Keep tunneling and decoder fallback renderer-level advanced options.
9. Keep palette sampling cadence independent from wallpaper rotation cadence.
10. Apply portrait/too-small image fallback in the image renderer.
11. Support a real `Skip` fallback for unacceptable images.
12. Keep remote-driven wallpaper advance as an optional generic remote action.
13. Preserve theme vs full-backup separation.
14. Keep idle state as a semantic wallpaper event and reuse the same renderer.
15. Keep external plugin validation explicit and versioned.

---

# Final assessment

```text
Core Group 6 architecture:             correct
Missing major subsystem:               none
Factual reversals:                      none

Genuine completeness upgrades:
  - exact update-delay scheduler policy
  - exact video-playing TimeElapsed suppression
  - exact bad-format/min-width image policy
  - true audio-track disable at volume 0
  - tunneling + decoder-fallback runtime wiring
  - dynamic video palette sampling cadence
  - remote Play/Pause TimeElapsed trigger
  - external-provider runtime blacklist check

Later-group refinements:
  - full theme manifest/schema
  - encrypted appearance.plbackup semantics
  - theme vs full-backup scope
  - verified idle-exit event relationship

Need to redo Group 6:                   no
Effect on Group 12 plan:                strengthens Phase 5, no reorder required
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 7–12`.**


---

# SOURCE FILE: `group07.md`

SHA-256: `2bbd244f3113dbd3854ce3ef1d555027d5d00eda1cd23080acbc4153c74eba5d`

---

# Projectivy 4.72 — Group 07: Settings System

Status: **COMPLETE + VERIFIED**  
Reference APK: Projectivy Launcher 4.72 / versionCode 97  
Package: `com.spocky.projengmenu`  
APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## 1. Analysis method and efficiency improvement

Group 7 uses the persistent reverse-engineering cache created during Groups 4–6, plus a settings-specific **role graph**:

```text
setting key
   ↓
yh3 descriptor
   ↓
settings screen / control
   ↓
SharedPreferences storage
   ↓
change handler
   ↓
runtime consumer / home preview
```

To avoid opening/decompiling each settings screen separately, this group added:

- a one-load exporter for every XML resource containing a `PreferenceScreen`;
- a batch dump of all 48 subclasses of Projectivy's GuidedStep settings base (`cb3`);
- a complete field→preference-key map for the central `hj3` registry;
- screen→preference mappings derived from the source cache;
- targeted smali verification only where JADX could not reconstruct coroutine control flow.

The central registry contains **151 `yh3` descriptor constructions** in this release. All **148 static `yh3` fields** were mapped to their exact preference keys; the remaining constructions are temporary/dynamic/migration descriptors rather than unmapped static fields.

This is considerably faster than treating every settings screen as an independent subsystem while retaining end-to-end verification.

---

## 2. Projectivy has two settings UI systems

This is the most important architectural finding in Group 7.

### A. Leanback Preference side panels

Used primarily for content-oriented/configuration screens such as:

- launcher-side dynamic settings/actions;
- categories;
- category configuration;
- category apps/content;
- channels;
- notifications;
- card/context actions;
- program details.

The boundary is:

```text
SettingsActivity
   ↓
lv
   ↓
x64 (Leanback settings host)
   ↓
Preference fragment selected from preferenceResource
```

### B. Metadata-driven GuidedStep settings

Used for most global launcher configuration:

```text
pf (root guided settings)
   ↓
General / Appearance / Inputs / Power / Remote / Accessibility /
Parental Controls / Display Profiles / Backup & Restore / Premium
   ↓
cb3 guided-screen subclasses
   ↓
yh3 preference descriptors
```

These screens are **not manually coded control-by-control**. A central descriptor object defines type/default/title/description/icon/flags/min/max/step and `cb3.q0()` generates the appropriate TV control.

For Aerial-Rows, this metadata-driven approach is the part worth reproducing conceptually even if the new UI is Compose TV rather than Leanback GuidedStep.

---

## 3. Leanback settings host

### `SettingsActivity`

`com.spocky.projengmenu.ui.sidePanels.settings.SettingsActivity` extends `lv`.

It intercepts the remote **MENU key (KEYCODE_MENU / 82)**:

```text
MENU down → startTracking()
MENU up   → finish SettingsActivity
```

So the same remote menu key that opens the side-panel settings can close it cleanly.

### Side-panel window geometry

`lv.onAttachedToWindow()` makes the settings Activity a real right-side panel rather than a full-screen Activity:

- width = Leanback settings-pane width resource;
- height = display height minus vertical margins;
- gravity = right/end + vertically centred;
- horizontal margin derived from Leanback settings-pane margin;
- adds a translucent/window-layout flag;
- chooses blurred vs normal Leanback preference theme depending on blur support.

This is an implementation-relevant reason Projectivy settings visually float over the launcher.

### `x64` settings host

`x64` extends Projectivy's customized Leanback settings host.

It:

- inflates `leanback_settings_fragment`;
- applies a rounded/clipped background to the preference pane;
- pushes nested preference fragments when a `Preference.fragment` is selected;
- loads the initial fragment from an integer `preferenceResource` extra.

The resource routing is explicitly:

```text
settings_channels         → g70
settings_categories       → b40
settings_category         → CategoryPreferencesFragment
settings_action           → j6
settings_notifications    → b73
settings_category_apps    → CategoryAppsPreferencesFragment
settings_category_content → k40
other/default             → vo2
```

This generic routing lets one side-panel Activity host many independent settings contexts.

---

## 4. XML Preference resources

The APK contains ten PreferenceScreen XML resources:

```text
settings_main.xml
settings_categories.xml
settings_category.xml
settings_category_advanced.xml
settings_category_apps.xml
settings_category_content.xml
settings_channels.xml
settings_notifications.xml
settings_action.xml
settings_program_details.xml
```

`settings_main.xml` is intentionally essentially empty: its real content is generated dynamically.

### Dynamic `settings_main`

`vo2` asynchronously builds the side-panel menu from Projectivy's internal home/action model.

The flow is:

```text
w40.L home/category definitions
   ↓
filter categories with i5.k == 4
   ↓
collect eligible e90 internal actions
   ↓
t40 → uo2 groups
   ↓
PTPreferenceCategory + CustomPreference rows
   ↓
e90.t() supplies Intent
```

Therefore Projectivy's main side-panel settings/actions are driven by the **same internal action/category model as the launcher**, rather than duplicated in static XML.

---

## 5. Category Preference screens

The category screens confirmed and reinforce the exact model discovered in Group 2.

`settings_category.xml` exposes:

- title;
- change icon;
- visibility;
- multi-row/grid mode;
- category background colour;
- sliding single row;
- card-title visibility;
- scale;
- advanced settings;
- sorting/order;
- reset/default;
- apps/content;
- delete;
- channel-related error state.

`settings_category_advanced.xml` exposes:

- items per line: 0..20;
- maximum rows: 0..20;
- start margin: 0..100;
- end margin: 0..100;
- top margin: 0..100.

`settings_category_apps.xml` explicitly separates:

```text
TV applications
Mobile applications
Channels
```

This is not merely presentation metadata: Group 2 already proved these values feed the actual row layout algorithm.

---

## 6. Focus behavior in Leanback Preference screens

Projectivy has its own base preference fragment `vu`.

It adds TV-specific focus/navigation handling on top of Leanback Preference:

- identifies the first visible/selectable preference;
- handles nested `PreferenceGroup` contents;
- intercepts DPAD navigation through the vertical list;
- pressing DOWN at the final selectable row wraps to the first;
- pressing UP at the first selectable row wraps to the last;
- BACK/escape routes through Projectivy's own close/back handler;
- title/icon/tint are handled by the same base.

Standard switch/edit/list preferences mutate their existing preference row rather than reconstructing the whole list, so normal value changes do not inherently reset list selection.

---

## 7. Custom Leanback Preference widgets

Projectivy includes reusable customized controls rather than hand-building each settings row.

### `CustomSwitchPreference`

- based on AndroidX `SwitchPreference`;
- renders using a Material switch;
- thumb/track are recoloured from Projectivy's current accent setting (`hj3.i0`).

### `CustomListPreference`

- directly extends AndroidX `ListPreference`;
- adds Projectivy styling;
- choice/dialog semantics remain standard Leanback/AndroidX preference behavior.

It is **not an inline dropdown implementation**.

For Aerial-Rows, we can retain the same semantic model but use an inline Compose TV choice/dropdown/popover if desired.

### `CustomEditTextPreference`

- extends AndroidX `EditTextPreference`;
- styled by Projectivy;
- shows its current text as summary.

### `AppCustomSwitchPreference`

Switch preference variant with app-sized/icon-oriented presentation.

### `AppPreference`

Supports long-click behavior for app rows.

### `NotificationPreference`

Adds custom key handling appropriate to notification rows.

### `PTPreferenceCategory`

Uses launcher accent colouring for preference-category headers.

### `MaterialSliderPreference`

A real Material slider embedded into a preference row, supporting:

- minimum;
- maximum;
- step;
- DPAD/keyboard interaction;
- optional value label;
- centred/non-centred values;
- optional tick behavior;
- transient state restoration when the preference itself is nonpersistent.

---

## 8. Guided settings root

`pf` is the main GuidedStep settings root.

Its top-level destinations are:

```text
General
Appearance
Input               (when supported on device)
Power
Parental Control
Remote Control
Accessibility
Display Profiles
Manage Settings / Backup-Restore
Premium             (conditional)
```

It additionally renders an explanatory legend for:

- premium settings;
- accessibility-required settings;
- accessibility-recommended settings;
- experimental settings.

These statuses are also embedded directly into generated setting titles/icons.

---

## 9. Central preference descriptor — `yh3`

`yh3` is the central metadata model for global settings.

Semantic field map:

```text
a = base preference key
b = default value
c = title resource ID
d = description resource ID
e = icon descriptor (ta3)
f = flag bitmask
g = minimum value
h = maximum value
i = step
j = optional computed-value getter
k = optional custom setter
l = dynamic key suffix
m = migration/legacy key
```

### Stable GuidedAction identity

`yh3.c()` returns:

```text
baseKey.hashCode()
```

and that value is used as the stable action ID in the GuidedStep interface.

### Dynamic/scoped preferences

`yh3` supports reusable key templates such as:

```text
input_shortcut_visible_
display_profiles_input_hdr_
display_profiles_input_sdr_
appearance_wallpaper_provider_params_
```

A suffix can be applied at runtime while preserving one descriptor definition.

### Migration

A descriptor can carry a legacy/migration key. When the old value exists and the new value is still default, the old value can be transferred and then removed.

---

## 10. Preference flags — `rh3`

Verified flags:

| Flag | Value | Meaning |
|---|---:|---|
| PREMIUM | 1 | Premium-labelled setting |
| ACCESSIBILITY_REQUIRED | 2 | Accessibility service required |
| ACCESSIBILITY_RECOMMENDED | 4 | Accessibility improves behavior |
| TIME_PREFERENCE | 8 | Time-style input |
| PERCENT_PREFERENCE | 16 | Format slider as percentage |
| ENUM_AS_SLIDER_PREFERENCE | 32 | Render enum as slider |
| CENTERED_SLIDER_PREFERENCE | 64 | Slider has meaningful center |
| EDITABLE_TEXT_PREFERENCE | 128 | Editable text |
| COLOR | 256 | Color/palette chooser |
| EXPERIMENTAL | 512 | Experimental badge |
| SECONDS_PREFERENCE | 1024 | Format value as seconds |

This flag model is a major reason Projectivy can generate most settings controls generically.

---

## 11. Generic control generation — `cb3.q0(yh3)`

`cb3.q0()` selects a TV control from descriptor metadata rather than screen-specific code.

Verified behavior:

```text
Boolean
   → check/toggle GuidedAction

Integer / Float
   → slider

TIME_PREFERENCE
   → time-oriented control

p72 enum
   → child choice actions

p72 enum + ENUM_AS_SLIDER_PREFERENCE
   → enum slider

EDITABLE_TEXT_PREFERENCE
   → editable text action

COLOR
   → color/palette action
```

Min/max/step come directly from the `yh3` descriptor.

Additional flags control formatting and slider geometry.

If no icon is explicitly provided, Projectivy can derive a suitable icon from semantic cues such as time, tint/color, size and opacity.

Premium/accessibility/experimental markers are inserted into the generated title presentation.

---

## 12. Storage layer

Most global descriptors ultimately store values in one SharedPreferences store exposed by `hj3.g()`.

`hj3.b(yh3)` reads according to the descriptor's default-value type:

- Boolean;
- Int;
- Long;
- Float;
- String;
- Set<String>;
- `p72` enum, stored as the enum's integer backing value.

`hj3.i(yh3, value)` writes the corresponding type using `SharedPreferences.Editor.apply()`.

Important details:

- `null` removes the key;
- empty String removes the key;
- unsupported types are rejected rather than serialized arbitrarily;
- enum values are persisted as integers;
- numeric writes are clamped by `yh3.i()` against descriptor min/max before storage.

Projectivy therefore has a central typed settings schema even though the underlying persistence is SharedPreferences.

---

## 13. Computed values and custom setters

A descriptor can override ordinary storage behavior:

```text
j → computed getter
k → custom writer
```

This is used where the effective value depends on device capabilities, fallback defaults or another Projectivy subsystem.

It avoids hard-wiring these exceptional cases into every settings screen.

---

## 14. Accessibility-dependent settings

When `yh3.h(value)` changes a descriptor flagged either:

```text
ACCESSIBILITY_REQUIRED
or
ACCESSIBILITY_RECOMMENDED
```

and the live `ProjectivyAccessibilityService` exists, it increments/updates the service's observable configuration state.

So relevant settings can be consumed by the already-running service without requiring the user to restart Projectivy.

---

## 15. Appearance settings registry / theme boundary

`yh3.n` is populated automatically when descriptors are created.

A descriptor is included when its key:

- begins with `appearance`;
- is not an internal `nexp` setting;
- is not a dynamic key template ending in `_`.

This list is reused by theme/appearance export logic.

That gives Projectivy a clean boundary between:

```text
full application configuration
```

and:

```text
appearance settings safe to include in a theme
```

This is the relationship between Group 7 and the `.pltheme` architecture found in Group 6.

---

## 16. Complete central field→key map

The release contains 148 static `yh3` fields. All 148 were mapped.

Important examples:

```text
hj3.D1 → appearance_left_headers_enabled
hj3.E1 → appearance_left_headers_show_on_launch
hj3.F1 → appearance_left_headers_show_icon
hj3.G1 → appearance_left_headers_show_title
hj3.H1 → appearance_left_headers_show_reorder
hj3.I1 → appearance_left_headers_show_settings

hj3.m1 → appearance_categories_show_title
hj3.n1 → appearance_categories_show_icon
hj3.o1 → appearance_categories_rows_spacing
hj3.p1 → appearance_categories_focused_category_position_fixed
hj3.q1 → appearance_categories_hide_rows_above_selection
hj3.r1 → appearance_categories_remember_focused_card_position
hj3.s1 → appearance_categories_unselected_row_opacity
hj3.t1 → appearance_categories_listing_top_margin
hj3.u1 → appearance_categories_start_margin
hj3.v1 → appearance_categories_end_margin
hj3.w1 → appearance_categories_tint_custom_icons
hj3.x1 → appearance_categories_default_category_id
hj3.y1 → appearance_categories_dock_padding

hj3.O0 → appearance_cards_show_title
hj3.P0 → appearance_cards_rounded_corners
hj3.Q0 → appearance_cards_size
hj3.R0 → appearance_cards_spacing
hj3.S0 → appearance_cards_loop_limit
hj3.T0 → appearance_cards_ripple_effect
hj3.U0 → appearance_cards_bg_tint
hj3.V0 → appearance_cards_tint
hj3.W0 → appearance_cards_tv_app_aspect_ratio
hj3.X0 → appearance_cards_mobile_app_aspect_ratio
hj3.Y0 → appearance_cards_icon_pack

hj3.Z0 → appearance_cards_program_play_preview
hj3.a1 → appearance_cards_program_show_progressbar
hj3.b1 → appearance_cards_program_show_badge
hj3.c1 → appearance_cards_program_zoom_on_focus

hj3.d1 → appearance_cards_focused_scale
hj3.e1 → appearance_cards_focused_effect
hj3.f1 → appearance_cards_focused_animation
hj3.g1 → appearance_cards_focused_glow_size
hj3.h1 → appearance_cards_focused_glow_tint
hj3.i1 → appearance_cards_focused_color_override_mode
hj3.j1 → appearance_cards_focused_bg_tint
hj3.k1 → appearance_cards_focused_tint

hj3.o0 → appearance_wallpaper_provider
hj3.t0 → appearance_wallpaper_update_delay
hj3.u0 → appearance_wallpaper_display_mode
hj3.v0 → appearance_wallpaper_transition
hj3.w0 → appearance_wallpaper_wrong_format_display_mode
hj3.x0 → appearance_wallpaper_min_accepted_width
hj3.y0 → appearance_wallpaper_ignore_update_on_video_playing
hj3.A0 → appearance_wallpaper_video_volume
hj3.B0 → appearance_wallpaper_video_tunneling
hj3.C0 → appearance_wallpaper_video_decoders_fallback

hj3.P → power_autostart_source
hj3.Q → power_autostart_leaving_standby
hj3.R → power_autostart_delay

hj3.a2 → action_watch_next_programs_per_app
hj3.b2 → action_channel_general_programs_per_channel
```

A machine-readable CSV containing the complete mapping accompanies this report.

---

## 17. Major GuidedStep screen map

### `pf` — Settings root

```text
General             → qf
Appearance          → se
Input               → sf
Power               → ng
Parental Control    → jg (with parental gate when PIN exists)
Remote Control      → og
Accessibility       → i4
Display Profiles    → pz0
Manage Settings     → ag
Premium             → premium fragment when available
```

### `se` — Appearance

Subscreens:

```text
Categories     → kf
Cards          → hf
Background     → ze
Statusbar      → qg
Left Headers   → vf
Idle mode      → rf
Font           → of
Themes         → theme flow
```

Direct appearance settings include:

- `appearance_general_load_cards_while_scrolling`;
- global appearance tint/accent (`hj3.i0`).

### `vf` — Left Headers

Exact keys:

```text
appearance_left_headers_enabled
appearance_left_headers_show_on_launch
appearance_left_headers_show_icon
appearance_left_headers_show_title
appearance_left_headers_show_reorder
appearance_left_headers_show_settings
```

### `kf` — Categories

Includes:

```text
appearance_categories_show_title
appearance_categories_show_icon
appearance_categories_rows_spacing
appearance_categories_focused_category_position_fixed
appearance_categories_hide_rows_above_selection
appearance_categories_remember_focused_card_position
appearance_categories_unselected_row_opacity
appearance_categories_listing_top_margin
appearance_categories_start_margin
appearance_categories_end_margin
appearance_categories_tint_custom_icons
appearance_categories_dock_padding
```

### `hf` — Cards

Includes title, corners, size, spacing, looping, ripple, background tint, tint, TV/mobile aspect ratios and icon pack.

Related subareas include:

- `jf`: program-card preview/progress/badge/focus zoom;
- `cf`: focused-card scale/effect/animation/glow/tints.

### `ze` — Background

Includes provider selection, update delay, display mode, transition, bad-format fallback mode and minimum accepted width.

Related:

- `af`: video volume/tunneling/decoder fallback/update behavior;
- `te`: brightness/contrast/saturation/hue/blur/tint/shadow adjustment.

### `qg` / `rg` / `pg` — Status bar

Covers:

- notification management;
- auto-hide;
- Android system status bar visibility;
- Projectivy status-icon size/tints;
- settings/battery/connectivity/notifications icons;
- notification auto-hide;
- clock visibility/size/formats/tints.

### `rf` — Idle mode

Includes:

```text
appearance_idle_timeout_duration
appearance_idle_opacity
appearance_idle_no_clock_fade
appearance_idle_prevent_screensaver
```

### `ng` — Power

Includes:

```text
power_autostart_source
power_autostart_leaving_standby
power_autostart_delay
power_hdmi_input_exit_detection
power_simulate_user_activity
power_internal_idle_detection
power_internal_idle_timeout
power_internal_idle_action
power_internal_idle_excludes_media_playback
power_poweroff_mode
```

### `og` — Remote control

Includes:

```text
remotecontrol_show_menu
remotecontrol_double_back_ambient_mode
remotecontrol_decrease_input_lag
remotecontrol_bind_mic_btn_to_assistant
```

### `i4` — Accessibility

Includes:

```text
accessibility_service_enable_reminder
accessibility_alternate_config
accessibility_button_remapping
accessibility_volume_fix
```

### `qf` — General

Includes, conditionally according to device/build capabilities:

- application language;
- change/default launcher flow;
- `general_override_home`;
- TV-provider/Watch Next options;
- `action_channel_general_programs_per_channel`;
- analytics/crash reporting settings;
- update checking;
- Google Play repair/fix action on applicable devices.

The Watch Next per-app value is a lazily-created descriptor in `hj3.y`; it should not be confused with a normal static `yh3` field.

---

## 18. Focus retention after changing a setting

This directly answers one of the desired Aerial-Rows behaviors.

A typical GuidedStep screen (`vf`, Left Headers) does:

```text
user activates existing GuidedAction
   ↓
w54.h(action, descriptor)
   ↓
descriptor writes new value
   ↓
K0()
   ↓
approx. 500 ms delayed live update
   ↓
L0() applies preview/runtime reaction
```

Crucially, it **does not rebuild or navigate away from the action list**.

The same `GuidedAction` remains in place, which means selection/focus stays on the setting that was changed.

The 500 ms delay was verified in smali (`0x1f4` ms) because JADX did not cleanly reconstruct that coroutine method.

This explains the Projectivy behavior we want to retain:

```text
toggle setting
   ↓
switch visibly changes
   ↓
launcher preview updates
   ↓
remote cursor stays on same row
```

### Intentional recreation exception

Some changes really do require a style rebuild.

Example: changing the global appearance tint/accent (`hj3.i0`) calls Activity `recreate()` after the value changes.

That is an explicit exception rather than the normal settings update mechanism.

---

## 19. Live home preview behind Guided settings

`cb3` supports settings screens that expose/preview the launcher behind the GuidedStep panel.

Individual screens opt into this (`C0()`), and their `L0()` overrides update the active `MainFragment` after settings changes.

Example: `vf` calls the live home fragment's header-enable/update method after the preference changes.

This design is why many appearance changes can be previewed without leaving settings.

Recommended Aerial-Rows equivalent:

```text
Settings state changes
       ↓
SettingsRepository writes value
       ↓
StateFlow updates launcher composables immediately
       ↓
focus remains owned by the current settings row
```

No artificial delayed refresh should be necessary in Compose unless it is specifically useful for expensive operations.

---

## 20. Multi-choice behavior

There are two distinct choice mechanisms.

### Leanback Preference side

`CustomListPreference` is simply a styled `ListPreference`; the Leanback settings host opens the appropriate preference dialog/fragment.

### Guided settings side

Enum descriptors implementing Projectivy's `p72` interface are generated as:

- child choice actions; or
- a numeric/enum slider when `ENUM_AS_SLIDER_PREFERENCE` is set.

Each enum choice receives an action ID derived from:

```text
preferenceHash + 1 + enumBackingValue
```

For Aerial-Rows, we can improve the presentation while preserving the model:

```text
single current row
   ↓
Compose TV dropdown / choice popup
   ↓
select new value
   ↓
close popup
   ↓
return focus to same settings row
```

This satisfies the user's desired visible dropdown behavior better than Projectivy's older Leanback dialogs while retaining equivalent semantics.

---

## 21. Runtime-update taxonomy

Projectivy settings are not all treated identically. The useful conceptual categories are:

### Immediate in-place state

Examples: ordinary boolean, slider, enum or text descriptors. Value is persisted without screen rebuild.

### Delayed live preview

Many appearance screens call `K0()`, which waits roughly 500 ms then executes `L0()` to update the active launcher preview.

### Explicit subsystem reaction

Some screens override `L0()`/`y0()` to invoke a specific home/wallpaper/service method after the value changes.

### Accessibility service notification

Descriptors flagged accessibility-required/recommended notify the running service configuration state.

### Activity recreation

Used selectively for changes such as global theme/accent where current widgets must be recreated with a new style.

### Deferred/consumer-driven

Some stored preferences are simply read the next time their subsystem executes; they do not need an immediate callback.

This taxonomy should become explicit metadata in the Aerial-Rows settings schema rather than scattered `if` statements.

---

## 22. Recommended Aerial-Rows settings architecture

A clean implementation target is:

```text
SettingDescriptor<T>
 ├─ key
 ├─ default
 ├─ title
 ├─ description
 ├─ icon
 ├─ controlType
 ├─ min/max/step
 ├─ flags
 ├─ optional scope
 ├─ optional migration key
 └─ update policy

SettingsRepository
 ├─ typed read/write
 ├─ migration
 ├─ export/import
 └─ StateFlow<T>

SettingsScreenRegistry
 ├─ General
 ├─ Appearance
 ├─ Cards
 ├─ Categories
 ├─ Left headers
 ├─ Status bar
 ├─ Background / Aerial
 ├─ Idle
 ├─ Power
 ├─ Input
 ├─ Remote
 ├─ Accessibility
 └─ Parental / backup etc.

TV settings controls
 ├─ Switch row
 ├─ Slider row
 ├─ Choice/dropdown row
 ├─ Text row
 ├─ Color row
 └─ Navigation row
```

For focus stability:

```text
LazyColumn items use stable descriptor keys
        ↓
value changes mutate state only
        ↓
no list replacement / no route replacement
        ↓
FocusRequester remains attached to same row
```

Only a true structural change should replace a settings route.

---

## 23. Reliability notes

- `e32.o()` was not trusted from broken JADX Java output. Smali verified the ~500 ms delay and subsequent `cb3.L0()` call.
- `CustomListPreference` was verified as AndroidX `ListPreference`; no unsupported claim of an inline Projectivy dropdown is made.
- The 148 static central registry fields were all mapped programmatically against the decompiled static initializer rather than manually guessing field names.
- `hj3.y` is a lazy container for a dynamic setting descriptor, not a static `yh3` field; automated screen-mapping output that labels it simply `?y` should not be interpreted as an unresolved key.
- Group 7 deliberately does not fully reverse the backup/import serialization format; that belongs to Group 10.
- Input/vendor-specific effects of settings belong to Group 8.
- Accessibility/remote-key consumers belong to Group 9.

---

## 24. Group 7 conclusion

Projectivy's settings system is considerably more systematic than its obfuscated class names initially suggest.

Its global configuration is effectively a **typed schema-driven settings framework** layered over SharedPreferences, with generic rendering and per-subsystem update hooks. Its content-management screens use Leanback Preferences where richer dynamic app/channel/category lists are needed.

The most important implementation lessons for Aerial-Rows are:

1. Keep one typed settings registry rather than hard-coding settings separately in every Compose screen.
2. Separate persistence from presentation.
3. Give each setting a stable key/row identity.
4. Update values in place so focus never jumps to the top.
5. Use dropdown/choice UI for enums while returning focus to the originating row.
6. Mark settings with explicit update policies: immediate, live-preview, subsystem-reload, Activity/retheme, or deferred.
7. Keep dynamic/scoped settings (input IDs, provider IDs, etc.) first-class rather than inventing arbitrary one-off keys.
8. Reuse the appearance-only registry for theme export.
9. Keep category/content management separate from ordinary global preferences.
10. Allow the launcher behind settings to update live without recreating the settings route.

**Group 07 is complete and sufficiently closed for independent reconstruction.**


---

# SOURCE FILE: `group07_postclosure_audit.md`

SHA-256: `1c10815c1f2cc0de9cc26395ff902d488a017ab1d5cb1da6d19f928e5b7cbdc1`

---

# Group 7 post-closure audit — Settings System

Status: **POST-CLOSURE AUDITED after Groups 8–12**  
Original Group 7 SHA-256: `2bbd244f3113dbd3854ce3ef1d555027d5d00eda1cd23080acbc4153c74eba5d`  
Verification: **24/24 assertions passed**

## Executive conclusion

The original Group 7 report remains technically strong.

The most important part of the original work — the settings registry itself — still holds:

- both settings UI families were correctly identified;
- all **148 static `yh3` fields** remain correctly mapped;
- the `yh3` descriptor model remains correct;
- the `rh3` flag map remains correct;
- `CustomListPreference` is still correctly classified as AndroidX `ListPreference`, not an inline dropdown;
- the normal GuidedStep focus-retention explanation remains valid.

No hidden third settings framework appeared in Groups 8–12.

The post-closure audit did, however, uncover several important improvements.

The strongest are:

1. Group 7 mapped descriptor-level legacy-key migration, but **missed the separate global sequential schema migration engine** and its pre-migration backup request.
2. Group 7's runtime-update taxonomy missed a distinct **restart/re-enter HOME task on return** policy used by many settings screens.
3. Scoped `yh3` descriptors are more stateful than the original report made explicit: the shared descriptor stores a mutable runtime suffix while its GuidedAction ID remains based only on the base key.
4. `nexp` should be understood as a **not-export policy marker**, not merely as an "internal" setting naming convention.
5. `hj3` is not only a scalar typed-preference store; it also owns structured serialized launcher state.
6. The static appearance registry `yh3.n` is the core of theme export, but it is **not the entire theme export surface**.
7. Settings themselves can be protected by the parental launch gate through a dedicated stable target ID.

These findings refine the Aerial-Rows settings architecture but do not require redoing Group 7.

---

# 1. Genuine completeness miss — Projectivy has two migration layers

Group 7 documented the migration field on `yh3` correctly:

```text
yh3.m
    ↓
legacy preference key
```

and `yh3.g(oldKey)` can migrate one descriptor from an old key when appropriate.

That is only the **local descriptor migration layer**.

Group 10 later proved that Projectivy also has a separate application-wide sequential migration engine in `hj3`.

The stored schema/application version is:

```text
key_app_version
```

and Projectivy 4.72 migrates persisted state to:

```text
97
```

The startup path is effectively:

```text
read key_app_version
        ↓
0 < oldVersion < 97 ?
        ↓ yes
request automatic backup
        ↓
for each intervening version
        ↓
run that migration
        ↓
write 97
```

The normal migration entry point calls:

```text
dj3.b()
```

before the migration loop, requesting the automatic backup.

## Migration 86

Version 86 performs a particularly large legacy conversion.

It uses the descriptor migration helper to rename many old:

```text
key_...
```

preferences into the current descriptor schema, including:

- launcher;
- wallpaper;
- card;
- category/row;
- status bar;
- accessibility;
- power;
- remote;
- parental-control settings.

It also converts older structured representations for:

- categories;
- card customizers;
- display profiles.

Other verified version migrations change things such as:

- wallpaper-delay units;
- category representations;
- grid/category configuration;
- wallpaper display mode.

## Canonical migration model

Projectivy therefore has:

```text
Layer A
descriptor alias migration
    yh3.m / yh3.g(oldKey)

Layer B
ordered application schema migrations
    key_app_version
    hj3.a(...)
```

Group 7 only fully documented Layer A.

### Aerial-Rows implication

The settings model should therefore contain both:

```text
SettingDescriptor<T>.legacyKeys
```

and:

```text
MigrationRegistry
 ├─ v1 -> v2
 ├─ v2 -> v3
 └─ ...
```

A descriptor alias is useful for a simple rename, but it is not sufficient for structural migrations.

---

# 2. Genuine completeness miss — there is a HOME-task restart update policy

Group 7's runtime-update taxonomy listed:

```text
immediate in-place
delayed live preview
explicit subsystem reaction
Accessibility service notification
Activity recreation
deferred/consumer-driven
```

That taxonomy is missing one important policy.

Many GuidedStep settings handlers set:

```text
MainActivity.x0 = true
```

The audited Group 7 source contains **31 such assignments across 16 GuidedStep subclasses**.

When `MainActivity` next enters `onStart()`:

```text
if (x0) {
    x0 = false
    yb3.v(this)
}
```

Group 9 later proved that `yb3.v(context)` uses:

```text
Intent.makeRestartActivityTask(component)
    .addCategory(Intent.CATEGORY_HOME)
```

to re-enter Projectivy as a HOME task.

Therefore there is a distinct settings update policy:

```text
setting changes
    ↓
mark launcher restart/re-entry required
    ↓
leave/return from settings
    ↓
restart Projectivy HOME task
```

This is different from:

```text
Activity.recreate()
```

and different from:

```text
500 ms live preview
```

## Revised runtime-update taxonomy

The canonical list should be:

```text
1. Immediate in-place state
2. Debounced live preview
3. Explicit subsystem callback/reload
4. Accessibility-service refresh
5. Activity recreation/retheme
6. HOME-task restart/re-entry on return
7. Deferred consumer-driven read
```

### Aerial-Rows implication

This should become explicit metadata rather than a global Boolean:

```text
UpdatePolicy.IMMEDIATE
UpdatePolicy.LIVE_PREVIEW
UpdatePolicy.RELOAD_SUBSYSTEM
UpdatePolicy.RECREATE_ACTIVITY
UpdatePolicy.RESTART_HOME_TASK
UpdatePolicy.DEFERRED
```

Use `RESTART_HOME_TASK` only when a real Android task/role change requires it. Compose settings should not restart the launcher merely to repaint ordinary UI state.

---

# 3. Structural refinement — scoped `yh3` descriptors are mutable

Group 7 correctly said that `yh3` supports dynamic/scoped preference families such as:

```text
input_shortcut_visible_
display_profiles_input_hdr_
display_profiles_input_sdr_
appearance_wallpaper_provider_params_
```

The raw `yh3` source reveals an important implementation detail.

The descriptor has:

```text
String l
```

for the current runtime scope/suffix.

Calling:

```text
yh3.e(scope)
```

actually mutates the shared descriptor:

```text
this.l = scope.toString()
```

and `yh3.b()` then computes the effective preference key from:

```text
base key
+
current suffix
```

Likewise:

```text
yh3.j(scope, value)
```

sets the descriptor's runtime suffix before writing.

So Projectivy's descriptor object is **not immutable** with respect to scope.

### Aerial-Rows improvement

Do not reproduce this mutable-singleton pattern.

Prefer:

```kotlin
data class SettingKey(
    val base: String,
    val scope: String? = null,
)
```

or:

```kotlin
descriptor.scoped(scope)
    -> immutable ScopedSetting<T>
```

This makes concurrent reads/writes much easier to reason about.

---

# 4. Structural refinement — GuidedAction IDs are base-key IDs, not scope-unique IDs

`yh3.c()` is exactly:

```text
baseKey.hashCode()
```

It does **not** use `yh3.b()`, the effective scoped key.

Therefore:

```text
input_shortcut_visible_HDMI1
input_shortcut_visible_HDMI2
```

can conceptually share the same descriptor-level GuidedAction ID if represented through the same `yh3` template.

Projectivy's screen design avoids relying on multiple conflicting scoped instances in the same Guided action list where this would become problematic.

But the important reconstruction rule is:

> `yh3` has stable descriptor identity, not necessarily globally unique scoped-setting identity.

### Aerial-Rows improvement

Our stable row ID should include scope:

```text
setting:<baseKey>
setting:<baseKey>:<scope>
```

Do not use only:

```text
baseKey.hashCode()
```

for dynamic settings.

---

# 5. Precision correction — `nexp` means “do not export”

Group 7 described the appearance registry as excluding an:

> internal `nexp` setting

That is directionally correct but too narrow.

Group 10 verified the actual behavioral rule:

```text
if preference key contains "nexp"
    -> exclude it from ordinary exported backup/theme state
```

The full-backup exporter literally filters the entire SharedPreferences map using this marker.

Examples include:

- last-start state;
- installation/runtime unique key;
- external-license/runtime metadata;
- theme author/name runtime metadata;
- cached wallpaper information;
- font cache;
- video dynamic-colour sampling cadence.

These are not all the same conceptual category.

The safe meaning is:

```text
nexp = NOT EXPORT
```

not:

```text
nexp = internal/transient
```

On reset/restore paths, Projectivy can remove all ordinary exportable preferences while preserving the `nexp` values.

### Aerial-Rows improvement

Do not encode this policy in a key substring.

Use:

```kotlin
enum class ExportPolicy {
    NORMAL,
    THEME,
    NEVER,
}
```

or equivalent descriptor metadata.

---

# 6. Completeness upgrade — `hj3` also owns structured launcher state

Group 7 accurately mapped scalar descriptor persistence:

```text
Boolean
Int
Long
Float
String
StringSet
enum integer value
```

through SharedPreferences.

Group 10 later established that `hj3` also owns serialized structured state.

Verified examples include:

```text
action_categories_list
    -> List<i5>

action_customizers_list
    -> List<l5>

display_profiles_list
    -> display profile objects

cached wallpaper state
    -> Wallpaper model
```

These use explicit serialization rather than one primitive preference per field.

So the broader backend is:

```text
hj3
├─ typed scalar preferences
└─ structured serialized launcher state
```

### Aerial-Rows implication

Split the responsibilities cleanly:

```text
TypedSettingStore
    -> scalar settings

StructuredConfigStore
    -> versioned structured models
```

They can share one repository API, but should not pretend complex row/card models are ordinary scalar settings.

---

# 7. Theme-boundary refinement — `yh3.n` is not the whole theme payload

Group 7 correctly proved that the static appearance registry `yh3.n` automatically receives descriptors whose keys:

```text
start with "appearance"
do not contain "nexp"
do not end with "_"
```

That registry is indeed used by theme export.

Group 10 showed that theme export also has explicit additions outside that list.

The theme export path can include:

1. the static appearance descriptors from `yh3.n`;
2. a dynamically scoped wallpaper/provider parameter;
3. optionally selected built-in category appearance/configuration;
4. optional wallpaper assets in the `.pltheme` ZIP.

When category appearance is exported for a theme, Projectivy deliberately strips user content/membership-like state instead of treating it as a full backup.

Therefore:

```text
yh3.n
```

is the **core static appearance registry**, not the complete definition of a theme.

### Revised recommendation

Aerial-Rows should use:

```text
ThemeExportPolicy
├─ appearance settings
├─ provider-specific appearance state
├─ selected row/category presentation
└─ optional assets
```

rather than assuming that:

```text
all theme data == keys starting with "appearance"
```

---

# 8. Precision upgrade — the 500 ms live-preview debounce is globally shared

Group 7 correctly smali-verified:

```text
500 ms
```

before `cb3.L0()` applies a live preview.

The raw `cb3` source adds a useful detail:

```text
cb3.S0
```

is a **static** coroutine/job reference.

When `K0()` runs:

```text
existing S0?
    -> cancel it

start new delayed preview job
    -> store it back in static S0
```

So the Guided settings family uses one shared debounce slot for this preview mechanism rather than one independent pending job per row.

### Aerial-Rows improvement

The useful behavior is:

```text
latest setting change wins during rapid interaction
```

not the static global implementation itself.

Use a lifecycle-aware mechanism such as:

```text
StateFlow / SharedFlow
    -> debounce
    -> collectLatest
```

and scope it explicitly to the settings session/subsystem.

---

# 9. Focus-retention conclusion remains correct, with one qualification

The original Group 7 focus finding remains valid for normal GuidedStep changes.

Example `vf`:

```text
existing GuidedAction
    ↓
write current value
    ↓
K0()
    ↓
debounced preview
```

does not rebuild the current action list.

Therefore:

```text
toggle/adjust setting
    -> focus remains on the same row
```

is still the correct normal behavior.

The qualification is that this should not be generalized to every settings operation.

Explicit structural transitions include:

- opening a child choice list;
- opening a dedicated color picker;
- navigating to a sub-screen;
- Activity recreation;
- leaving settings and later triggering HOME-task restart/re-entry.

Those are not ordinary in-place mutations.

### Aerial-Rows implication

Persist focus using:

```text
SettingId
```

rather than trusting compositional position.

Then even real route changes can restore the originating setting cleanly.

---

# 10. Precision upgrade — COLOR uses a dedicated color-picker Activity/result path

Group 7 correctly mapped:

```text
COLOR
    -> color/palette control
```

Group 11 and the raw `cb3` source let us make this more precise.

`cb3.U0(yh3)` stores the current color descriptor and launches the dedicated:

```text
ColorPickerActivity
```

through an Activity-result style launcher.

Group 11 established that the color picker supports reusable Material-style color editing including luminance/alpha-related behavior and dynamic-color preferences.

So Projectivy does not implement color as a simple inline numeric slider.

### Aerial-Rows option

A Compose modal/panel is preferable, but preserve the interaction model:

```text
setting row
    ↓
focused color editor
    ↓
commit/cancel
    ↓
return focus to originating row
```

---

# 11. Settings access itself participates in parental policy

Group 7 mapped:

```text
parental_control_protect_settings
```

as a setting, but Group 10 later closed its enforcement.

Projectivy has a special stable action/target ID for the settings surface:

```text
-1922314973
```

The parental policy can therefore say:

```text
settings protection enabled
AND
target == settings stable ID
    -> require parental authorization
```

Settings can be routed through the same parental-check architecture used for other protected targets.

### Aerial-Rows implication

Protecting Settings should be an access policy:

```text
LaunchGate.authorize(SettingsTarget)
```

not a conditional sprinkled throughout each settings screen.

---

# 12. Bulk import/restore has a separate invalidation path

Normal descriptor changes can trigger:

- descriptor custom setters;
- Accessibility configuration updates;
- Guided preview callbacks.

Backup restore cannot safely depend on firing every UI descriptor handler one by one.

Group 10 verifies that import/reset performs bulk SharedPreferences operations and then explicitly refreshes runtime state/caches and notifies the running Accessibility service.

Therefore the settings architecture has two mutation modes:

```text
interactive single-setting mutation
    -> per-setting update policy

bulk configuration replacement
    -> batch apply
    -> one global rehydrate/invalidate step
```

### Aerial-Rows implication

Do not restore a backup by calling every Compose setting callback sequentially.

Use:

```text
repository.transaction {
    replace imported state
}
    ↓
validate/migrate
    ↓
single runtime reload
```

---

# 13. Full backup and theme export read settings differently

Another useful persistence nuance emerges from Group 10.

## Full backup

Projectivy starts from:

```text
SharedPreferences.getAll()
```

and exports stored values except `nexp`.

## Theme export

For the static appearance registry it calls the descriptors themselves:

```text
descriptor.b()  -> effective key
descriptor.a()  -> effective value
```

and adds explicit theme extras.

This distinction matters for descriptors with:

- computed getters;
- fallback values;
- scoped state.

So "settings export" is not one universal reflection operation.

### Aerial-Rows improvement

Define explicit serializers for:

```text
FullBackup
Theme
```

rather than asking the UI descriptor registry to magically serve every export purpose.

---

# 14. Later Groups 8 and 9 validate, rather than contradict, the Group 7 key map

Group 8 verifies runtime consumption of dynamic/scoped families such as:

```text
input_shortcut_visible_<input>
display_profiles_input_sdr_<input>
display_profiles_input_hdr_<input>
```

Group 9 verifies runtime consumption of:

```text
general_override_home

power_autostart_source
power_autostart_leaving_standby
power_autostart_delay
power_internal_idle_detection
power_internal_idle_timeout
power_internal_idle_action
power_internal_idle_excludes_media_playback

remotecontrol_show_menu
remotecontrol_double_back_ambient_mode
remotecontrol_decrease_input_lag
remotecontrol_bind_mic_btn_to_assistant

accessibility_alternate_config
accessibility_button_remapping
accessibility_volume_fix
accessibility_service_enable_reminder
```

No later group exposed an incorrectly mapped static `yh3` key.

That is strong evidence that the **148-field Group 7 key map remains reliable**.

---

# 15. No hidden third settings UI architecture appeared

Group 11 closed several Activities that could have looked like another settings framework.

They turned out to be thin entry points/wrappers around systems Group 7 had already mapped, including:

```text
CategoryContentSidePanelActivity
ManageCategoriesActivity
ManageChannelsActivity
AccessibilityServiceActivity
```

`ColorPickerActivity` is a reusable editor helper, not another configuration store.

So the canonical split remains:

```text
A. Leanback Preference side-panel system
B. metadata-driven GuidedStep system
```

with helper Activities layered around them.

---

# 16. Revised canonical settings architecture

With Groups 8–12 folded back in:

```text
Settings UI
│
├─ Leanback Preference side panels
│   ├─ dynamic content/category/channel screens
│   ├─ ListPreference/dialog choices
│   └─ TV-specific focus/navigation
│
└─ GuidedStep settings
    ├─ yh3 descriptor registry
    ├─ generic cb3 renderer
    ├─ stable base descriptor action IDs
    ├─ switches/sliders/enums/text/color
    ├─ debounced live home preview
    └─ subsystem-specific reactions

yh3
├─ base key
├─ default
├─ title/description/icon
├─ flags
├─ min/max/step
├─ computed getter
├─ custom setter
├─ mutable runtime scope suffix
└─ legacy alias

hj3
├─ typed SharedPreferences scalars
├─ structured serialized state
├─ descriptor aliases
├─ app-version migration engine
└─ schema version key

Update policies
├─ immediate
├─ debounced live preview
├─ subsystem reload
├─ Accessibility refresh
├─ Activity recreation
├─ HOME-task restart/re-entry
└─ deferred

Persistence/export
├─ ordinary settings
├─ nexp / never-export state
├─ full backup
├─ theme export
└─ bulk import/reload

Policy
└─ Settings target can itself be parental-protected
```

---

# 17. Effect on Aerial-Rows implementation plan

The Group 12 phase ordering remains correct.

The Group 7 audit strengthens Phase 1, Phase 6 and Phase 10.

## Phase 1 — config

Add from the beginning:

```text
schemaVersion
MigrationRegistry
immutable scoped SettingKey
structured-model serializers
explicit ExportPolicy
```

## Phase 6 — settings

Use:

```text
SettingDescriptor<T>
├─ stable SettingId including scope
├─ value type
├─ control type
├─ validation
├─ visibility/capability predicate
├─ export policy
└─ update policy
```

Add an explicit update policy for:

```text
RESTART_HOME_TASK
```

but avoid using it for ordinary Compose appearance changes.

Focus should be stored by stable `SettingId`, not list index.

## Phase 10 — backups/parental

Use:

```text
bulk configuration transaction
    ↓
migration/validation
    ↓
single rehydrate/invalidation
```

and route access to protected Settings through the central `LaunchGate`.

---

# Final assessment

```text
Core Group 7 architecture:             correct
Static descriptor/key map:             still correct (148/148)
Missing major settings subsystem:       none

Genuine completeness misses:
  1. global versioned schema migration engine + backup request
  2. HOME-task restart/re-entry update policy

Important structural refinements:
  - scoped yh3 descriptors are mutable
  - GuidedAction ID is base-key, not scope-unique
  - shared static 500 ms preview debounce job
  - hj3 also stores structured serialized state
  - theme export extends beyond yh3.n
  - nexp means behavioral not-export policy
  - bulk restore uses global rehydration
  - Settings itself can be parental-gated
  - COLOR uses a dedicated picker flow

Factual reversals:                      none
Need to redo Group 7:                   no
Effect on Group 12 plan:                strengthens Phases 1, 6 and 10
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 8–12`.**


---

# SOURCE FILE: `group08.md`

SHA-256: `668b12eba61d08be0d7e74904b3e60c6ae035f37c775db152e03747bc983f98e`

---

# Projectivy 4.72 — Group 08: HDMI, TV inputs, vendor/device integration

Status: **complete + verified**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group covers:

- HDMI/AV source models and source cards
- TV-input discovery and naming
- source switching and fallback order
- Projectivy's embedded internal TV path
- source-popup shortcuts
- device/vendor settings adapters
- display profiles and input-bound SDR/HDR profiles
- Xiaomi/Amlogic picture-setting adapters
- input-lag/sysfs controls
- zoom and 4K window-manager tools
- root/Magisk property-override tools
- capability gating and launcher-component exposure

Accessibility key remapping and HOME/boot behavior remain Group 09, even where Group 08 components are invoked from the accessibility service.

---

## 1. Analysis workflow used for Group 08

Group 08 used the hardened cache-first process:

```text
persistent source cache
    ↓
SQLite semantic/full-text index
    ↓
local class-reference graph
    ↓
semantic + graph-distance candidate ranking
    ↓
targeted batch export of missing classes in one APK load
    ↓
JADX only for unresolved semantics
    ↓
smali/bytecode fallback only when critical Java reconstruction is ambiguous
```

The candidate ranker combines:

- literal/API hits;
- full-text source hits;
- graph distance from named Projectivy input/device activities;
- light ownership weighting.

This avoided broad JADX scans and quickly elevated the real subsystem core (`dd4`, `ed4`, `c3`, `InternalTvActivity`, `SourcePopupActivity`, vendor settings activities, display-profile classes and root/sysfs helpers).

Group 08 also uses checkpointed stages and SHA-256 evidence manifests. The semantic database passed `PRAGMA integrity_check = ok` after the group analysis.

---

# 2. Core input model — `dd4`

Projectivy has a concrete source enum/model, `dd4`:

| Source | value | default label | matching token | icon/action |
|---|---:|---|---|---|
| Android | 0 | `input_source_none` | `android` | none |
| HDMI 1 | 1 | `input_source_hdmi1` | `Hdmi1` | HDMI1 icon/activity |
| HDMI 2 | 2 | `input_source_hdmi2` | `Hdmi2` | HDMI2 icon/activity |
| HDMI 3 | 3 | `input_source_hdmi3` | `Hdmi3` | HDMI3 icon/activity |
| HDMI 4 | 4 | `input_source_hdmi4` | `Hdmi4` | HDMI4 icon/activity |
| AV | 5 | `input_source_av` | `AV1` | AV icon/activity |
| Unknown | `Integer.MIN_VALUE` | unknown | `unknown` | none |

Each hardware source points at an availability/action descriptor (`os`) whose activity is one of:

- `SourceHDMI1Activity`
- `SourceHDMI2Activity`
- `SourceHDMI3Activity`
- `SourceHDMI4Activity`
- `SourceAVActivity`

`dd4.d()` resolves the visible source title through the Projectivy card/action layer, so a custom title override can win over the stock resource string.

`dd4.e()` reads a dynamically scoped visibility preference. Group 07 established the key family:

```text
input_shortcut_visible_<suffix>
```

For source actions the suffix is derived from the corresponding internal activity identity.

This means source availability and source *visibility as a launcher shortcut* are distinct concepts.

---

# 3. TV input repository / capability layer — `ed4`

`ed4` is the central TV-input discovery/cache manager.

It uses:

```text
TvInputManager
TvInputInfo
PackageManager.hasSystemFeature("android.software.live_tv")
```

and keeps separate cached lists for:

- passthrough inputs;
- non-passthrough/tuner inputs;
- normalized Projectivy source enum entries;
- custom/default input labels.

## Discovery

`ed4.h()` calls:

```text
TvInputManager.getTvInputList()
```

and separates:

```text
TvInputInfo.isPassthroughInput() == true
    → passthrough/external inputs

otherwise
    → tuner/non-passthrough inputs
```

On Android 7.0+ it respects `TvInputInfo.isHidden(context)` for non-passthrough entries.

## Input labels

On API 24+ Projectivy can prefer:

```text
TvInputInfo.loadCustomLabel(context)
```

and falls back to:

```text
TvInputInfo.loadLabel(context)
```

Both label forms are cached by TV-input ID.

## Mapping generic HDMI1/2/3/4/AV to platform TV inputs

For a `dd4` source, `ed4.d(...)` searches discovered passthrough `TvInputInfo` records and compares:

- `TvInputInfo.getId()`;
- normalized default input label;
- the generic source token (`Hdmi1`, `Hdmi2`, etc.);
- `HW`-style IDs.

This lets Projectivy bridge its stable generic source model to vendor-specific TV Input Framework IDs.

## Compatibility fallback

The input-capability check is broader than only `android.software.live_tv`; Projectivy also has internal compatibility fallbacks for devices whose firmware exposes TV input behavior without reporting the feature conventionally.

Sony/TCL also receive defensive retry handling when the firmware reports an abnormal empty passthrough state; Projectivy retries a limited number of times rather than assuming permanently absent inputs.

---

# 4. HDMI/AV launcher activities — `c3`

`SourceHDMI1Activity` through `SourceHDMI4Activity` are tiny subclasses of the shared `c3` source launcher.

Their Droidlogic fallback service IDs are:

```text
HDMI1 → Hdmi1InputService/HW5
HDMI2 → Hdmi2InputService/HW6
HDMI3 → Hdmi3InputService/HW7
HDMI4 → Hdmi4InputService/HW8
AV    → AV1InputService/HW1
```

When `ed4` cannot resolve a runtime `TvInputInfo`, `c3.H()` can construct a Droidlogic-style fallback ID:

```text
com.droidlogic.tvinput/.services.<service>
```

---

# 5. Source switching strategy

`c3.x()` is the main external-source launch pipeline.

It first resolves whether the source should be shown through Projectivy's internal `TvView` or delegated to the platform/vendor TV application.

The choice can be forced by an intent extra:

```text
tvView = "internal"
tvView = "external"
```

otherwise a stored Projectivy preference supplies the default.

The requested source is persisted as the current input selection.

## Internal path

If the internal path is chosen:

```text
SourceHDMIxActivity / SourceAVActivity
    ↓
InternalTvActivity
    ↓
input enum value passed in extras
```

## External/vendor path

Projectivy first tries Xiaomi's external-source contract:

```text
com.xiaomi.mitv.tvplayer.EXTSRC_PLAY
```

with Xiaomi-specific source values mapped through `uz2`.

If that fails, Projectivy falls back through several vendor/platform targets using the resolved TV Input Framework URI:

1. Fengmi TV input
   - package `com.fengmi.tvinput`
   - `com.fengmi.tvinput.MainActivity`
2. MediaTek TV center
   - `com.mediatek.wwtv.tvcenter.nav.TurnkeyUiMainActivity`
3. Mi TV live TV variant
   - `com.mitv.livetv.nav.TurnkeyUiMainActivity`
4. AOSP Live Channels / TV
   - `com.android.tv.MainActivity`
5. Google TV variant
   - `com.google.android.tv.MainActivity`
6. generic intent resolution
7. final fallback back to Projectivy internal TV rendering

When launching vendor TV apps, Projectivy additionally supplies fields such as:

```text
deviceName
inputName
bootcomplete = true
```

and for MediaTek it explicitly places the channel/input URI in the intent data.

This is therefore a layered compatibility strategy, not a single hard-coded HDMI intent.

---

# 6. Embedded TV path — `InternalTvActivity`

`InternalTvActivity` is a real TV Input Framework host.

Its layout contains a platform:

```text
android.media.tv.TvView
```

plus waiting/title/logo UI.

On receiving the source enum it:

- resolves the source title;
- shows the source-specific animated icon;
- installs a `TvView` callback;
- installs an unhandled-input listener;
- requests focus on the TV view.

## Tuning

`InternalTvActivity.J(...)` resolves the input ID through `ed4` and then chooses:

```text
TvContract.buildChannelUriForPassthroughInput(inputId)
```

for passthrough sources whose ID contains the hardware-style `HW` form, or:

```text
TvContract.buildChannelsUriForInput(inputId)
```

for tuner/non-passthrough input paths.

It tunes with:

```text
TvView.tune(inputId, uri)
```

and resets/shows the waiting state when no usable input can be resolved.

## Remote/input handling

The activity forwards a set of TV-related key codes into `TvView.dispatchKeyEvent(...)` while preserving Projectivy's own navigation/back semantics.

Notably:

- long-press BACK stops the internal-TV session and exits;
- ordinary BACK is handled separately;
- MENU while internal TV is active opens vendor display/settings UI where available;
- Xiaomi pre-Android-8 firmware uses its display-settings popup service;
- otherwise the MediaTek/factory-settings launcher is attempted.

The activity also owns/release a media-session/controller helper around the internal-TV lifetime.

---

# 7. Source chooser popup — `SourcePopupActivity`

Projectivy has a separate generic "show input chooser" action.

The fallback order is explicitly vendor-aware:

1. Xiaomi settings broadcast
   - `com.xiaomi.mitv.settings.INPUTSOURCE_POPUP`
2. Mi TV Android-TV home input popup
   - `com.mitv.tvhome.atv.app.tv.INPUTSOURCE_POPUP`
3. Formovie media-select app
   - `com.formovie.fmmediaselect.MainActivity`
4. MediaTek TV center input panel
   - `com.mediatek.wwtv.tvcenter.nav.inputpanel.InputsPanelActivity`
5. Google TV launcher input panel
   - `com.google.android.tvlauncher.inputs.InputsPanelActivity`

The Xiaomi settings form is sent as a broadcast when the package is installed; the others are started as activities.

---

# 8. Input shortcuts as real launcher components

The manifest declares HDMI1–4, AV and Source Popup activities as:

```text
exported = true
enabled = false
launchMode = singleInstance
```

with:

```text
ACTION_MAIN
CATEGORY_LAUNCHER
CATEGORY_LEANBACK_LAUNCHER
```

So Projectivy can expose these as genuine launcher-visible source shortcuts when appropriate.

The shared internal-action card layer (`e90`) exposes component enable/disable through `wo2.d(...)`, which calls:

```text
PackageManager.setComponentEnabledSetting(...)
```

for `com.spocky.projengmenu` internal activities.

This gives Projectivy two representations of an input:

- a card inside its own configurable Inputs category;
- optionally a real Android/Leanback launcher shortcut component.

For Aerial-Rows, source visibility should therefore remain a model/policy decision rather than being tied directly to whether the activity exists in the manifest.

---

# 9. Capability gating

Projectivy wraps many device actions in capability descriptors (`os`, `p`, `cd4`).

Examples verified in this group:

- HDMI1–4 action available only when TV-input support exists and that input's visibility preference is enabled;
- AV uses the same pattern;
- Source Popup is considered supported when known Google TV/Xiaomi/Formovie/MediaTek input UI packages exist;
- Amlogic settings require `com.droidlogic.tv.settings`;
- FengOS settings require `com.formovie.gsettings`;
- MediaTek settings require `mediatek.factorymenu.ui`;
- Mi TV settings require `com.xiaomi.mitv.settings`;
- Zoom is offered if `/sys/class/video/zoom` exists or the Xiaomi screen-scale service is available;
- Input Lag requires Projectivy's privileged/root shell to be available and at least one relevant deinterlace sysfs node to exist;
- 4K window-manager tooling is exposed only when the privileged/root shell is available.

This keeps unsupported tools out of the UI rather than letting users repeatedly launch actions that can never work.

---

# 10. Vendor settings adapters

Projectivy contains explicit lightweight vendor launchers.

## Android settings

```text
android.settings.SETTINGS
```

## Amlogic / Droidlogic

Primary:

```text
com.droidlogic.tv.settings/.MainSettings
```

fallback:

```text
com.droidlogic.tv.settings/.more.MorePrefFragmentActivity
```

## Xiaomi Mi TV

```text
com.xiaomi.mitv.settings/.entry.MainActivity
```

## Formovie / FengOS

```text
com.formovie.gsettings/.ui.activity.MainActivity
```

## MediaTek

```text
mediatek.tvsetting.factory.intent.action.MainmenuActivity
```

## FengOS/Fengmi engineering tools

Projectivy additionally exposes:

```text
com.fengmi.autofocustest/.MainActivity
```

and tries two Fengmi test-suite activities:

```text
com.fengmi.testsuite/.LunchActivity
com.fengmi.testsuite/.EngActivity
```

These adapters are simple launch/fallback wrappers; Projectivy does not duplicate those vendor settings screens itself.

---

# 11. Other vendor shortcuts

## Media Explorer

Projectivy prefers:

```text
com.xiaomi.mitv.mediaexplorer/.NewScraperMainEntryActivity
```

and falls back to:

```text
com.fengos.fmexplorer/.MainActivity
```

## Google Smart Home

A separate shortcut exists and is exposed only when the resolved intent is available on the applicable Google TV environment.

---

# 12. Display Profiles — Projectivy-owned subsystem

This is substantially deeper than the simple vendor settings launchers.

Named class:

```text
com.spocky.projengmenu.displayProfiles.DisplayProfileManager
```

Projectivy maintains a serialized list of `lz0` display-profile models.

If no profile exists it creates a default profile.

Profiles are sorted and stored independently of the runtime vendor adapter.

## Profile identity

Each `lz0` has:

- user-visible name;
- stable UUID;
- multiple numeric picture parameters;
- several boolean picture-processing flags;
- optional live adapter/delegate.

## Verified picture parameters

Through the Amlogic reflection adapter, the generic `ru` interface maps profile values to functions including:

```text
Brightness
Contrast
Saturation
Sharpness
Hue
Backlight
White balance gains/offsets
CM status
DNLP status
```

The profile also includes extra vendor-specific processing fields used by the supported adapters.

`lz0.a()` captures the current device picture settings into a profile.

`lz0.b()` applies a profile back to the current adapter.

This is a proper generic profile model on top of vendor-specific backends.

---

# 13. Amlogic display adapter

`j31` uses reflection so Projectivy can run on devices where Amlogic classes do not exist.

It supports two Droidlogic API families:

```text
com.droidlogic.app.tv.TvControlManager
com.droidlogic.app.SystemControlManager
```

and reflectively resolves methods/enums such as:

```text
GetBrightness / SetBrightness
GetContrast / SetContrast
GetSaturation / SetSaturation
GetSharpness / SetSharpness
GetHue / SetHue
GetBacklight / SetBacklight
getCMStatus / setCMStatus
getDNLPStatus / setDNLPStatus
WhiteBalanceParams
SOURCE_TYPE_HDMI
```

The use of reflection is deliberate compatibility isolation: absent vendor classes simply disable that backend instead of breaking application class loading.

Amlogic generic source names are separately represented through `f31` for HDMI1–4, AV and Android.

---

# 14. Xiaomi picture-settings adapter

Projectivy also has a Xiaomi backend (`wz2`) which is constructed reflectively from:

```text
mitv.display.PictureSettingsManager
```

and tries to obtain Xiaomi's source manager through `TvContext`.

Xiaomi source enums (`uz2`) map HDMI1–4, AV and Android into the generic Projectivy `dd4` source model.

The DisplayProfileManager chooses among available vendor adapters at runtime rather than compiling the launcher around one OEM API.

---

# 15. Per-input SDR/HDR display-profile bindings

Display Profiles has a dedicated binding UI (`i62`).

The user chooses an input from:

```text
Android
HDMI1
HDMI2
HDMI3
HDMI4
AV
```

filtered according to the currently available Projectivy source list.

For each input, the user independently selects:

- an SDR profile;
- an HDR profile.

Group 07 established the backing dynamic key families:

```text
display_profiles_input_sdr_<input>
display_profiles_input_hdr_<input>
```

Internally these are the `hj3.v` and `hj3.u` scoped settings respectively.

The stored value is the stable display-profile UUID, not just a list index.

This is an important reconstruction detail: profile bindings should survive profile reordering.

---

# 16. Display-profile list UI

`DisplayProfilesActivity` exposes two branches:

```text
Profile list
Input bindings
```

The input-binding branch appears only when Projectivy detects usable external TV inputs.

Profiles can be added and edited individually; the manager serializes/deserializes them using the app's persistence/serialization infrastructure.

The exact full editor UI is intentionally treated as part of the display-profile subsystem rather than re-described here field-by-field where the decompiler generated unrelated noise, but capture/apply semantics and the supported adapter methods are verified.

---

# 17. Input-lag / deinterlace controls

`InputLagActivity` is backed by `f62`.

It reads:

```text
/sys/module/di/parameters/nr2_en
/sys/module/di/parameters/bypass_all
```

The UI exposes at least:

- noise reduction;
- deinterlacing.

Current state is interpreted as:

```text
nr2_en == "1"      → noise reduction enabled
bypass_all == "0"  → deinterlacing enabled
```

Writes ultimately go through Projectivy's privileged shell (`kx3`) using root-required commands of the form:

```text
echo <value> > <sysfs path>
```

The root/shell capability is explicitly probed and cached as:

```text
UNKNOWN
AVAILABLE
UNAVAILABLE
```

These tools are therefore capability-gated and are not expected to function on an ordinary unprivileged Android TV installation.

---

# 18. Zoom controls

`ZoomActivity` has two possible paths.

## Xiaomi stock path

When `com.xiaomi.mitv.settings` exists and resolves the service, Projectivy launches:

```text
com.xiaomi.mitv.settings.SCREENSCALE
```

against:

```text
com.xiaomi.mitv.settings.TvSettingService
```

## Sysfs path

When supported, Projectivy reads/writes:

```text
/sys/class/video/zoom
```

using its privileged shell helper.

So the same Projectivy setting abstracts either a vendor-native UI or direct low-level control.

---

# 19. 4K window-manager/device-resolution tool

`FourKWMActivity` displays several different resolution concepts rather than conflating them:

- `/sys/class/video/device_resolution`;
- `/sys/class/display/mode`;
- Android window-manager pixel size;
- Android window-manager density.

The UI provides actions for 2160p and 1080p-style window-manager configuration.

The action path is privileged/root-gated.

This tool is separate from actual codec/display-mode capability; Projectivy explicitly warns that changing the UI rendering size does not magically add missing 4K hardware decode support.

---

# 20. Root/Magisk property override tools

Projectivy has a common property-override activity base (`vm3`) and UI (`an0`).

The UI explicitly states that Magisk/root is required.

It offers install/uninstall actions and feeds a named shell script plus property/value pairs into the privileged command layer.

## Custom properties

`CustomPropsOverrideActivity` uses:

```text
atvtools.props.sh
```

and receives user/configured property pairs maintained elsewhere in Projectivy.

## 4K property override

`FourKPropsOverrideActivity` requests:

```text
sys.display-size = 3840x2160
ro.sf.lcd_density = 640
sys.fb.size = 3840x2160
```

through:

```text
atvtools-4k.props.sh
```

## Auto-off override

`AutoOffPropsOverrideActivity` requests:

```text
persist.sys.power.autoff = 0
```

using the shared auto-off script path.

JADX does not reconstruct the long coroutine responsible for installing/removing these overrides reliably. The surrounding behavior, required privilege state, property payloads, script names, success/restart/error handling and root-shell boundary are all directly verified. We should not invent the missing coroutine internals.

---

# 21. Ownership boundary

Group 08 has three distinct layers which should remain separate in Aerial-Rows.

## A. Generic Projectivy input model

```text
InputSource
TvInputRepository
InputAction
```

## B. Vendor adapters

```text
Xiaomi
Amlogic/Droidlogic
MediaTek
Formovie/FengOS
Google TV/AOSP TV
```

These should be optional capability plugins/adapters.

## C. Privileged device tools

```text
sysfs writers
root shell
property overrides
display engineering controls
```

These must never be prerequisites for ordinary launcher functionality.

That separation is important for Nvidia Shield and normal certified Android TV devices, where many OEM-specific/root-only tools should simply remain unavailable.

---

# 22. Recommended Aerial-Rows architecture

A clean reconstruction should use approximately:

```text
InputRepository
 ├─ discoverTvInputs()
 ├─ passthroughInputs
 ├─ tunerInputs
 ├─ resolveGenericSource(HDMI1..AV)
 └─ observe/refresh()

InputSource
 ├─ Android
 ├─ HDMI1
 ├─ HDMI2
 ├─ HDMI3
 ├─ HDMI4
 ├─ AV
 └─ Unknown

InputLaunchCoordinator
 ├─ InternalTvStrategy
 ├─ XiaomiStrategy
 ├─ FengmiStrategy
 ├─ MediaTekStrategy
 ├─ AospTvStrategy
 └─ GenericTvInputStrategy

InputPopupCoordinator
 ├─ Xiaomi popup
 ├─ MiTV popup
 ├─ Formovie selector
 ├─ MediaTek panel
 └─ Google TV panel
```

Vendor/device tools should be separately modeled:

```text
DeviceCapabilityRegistry
 ├─ AmlogicSettingsAdapter
 ├─ XiaomiSettingsAdapter
 ├─ FengOsSettingsAdapter
 ├─ MediaTekSettingsAdapter
 ├─ DisplayProfileBackend
 ├─ ZoomBackend
 ├─ InputLagBackend
 └─ RootShellBackend
```

Display profiles:

```text
DisplayProfileRepository
 ├─ profiles by UUID
 ├─ capture current settings
 ├─ apply profile
 ├─ SDR binding by InputSource
 └─ HDR binding by InputSource

PictureBackend
 ├─ AmlogicTvControlBackend
 ├─ AmlogicSystemControlBackend
 └─ XiaomiPictureSettingsBackend
```

This keeps the generic launcher portable while still allowing Projectivy-like OEM enhancements where the hardware supports them.

---

# 23. Nvidia Shield implications

For our primary target, Nvidia Shield, the architecture should degrade gracefully:

- ordinary app launcher functionality remains independent;
- `TvInputManager` can still be queried when meaningful;
- Xiaomi/Formovie/MediaTek/Amlogic actions remain absent unless their packages/APIs exist;
- direct sysfs/property tools remain hidden unless capability/root probes pass;
- source popup can use Google TV/platform paths where available;
- no OEM-specific class should ever be loaded eagerly in a way that can crash a non-OEM device.

Projectivy's use of reflection and capability descriptors demonstrates this same principle.

---

# 24. Reliability notes

High-confidence evidence in this group comes from:

- manifest declarations;
- direct Android API calls;
- decompiled small named activities;
- `dd4`, `ed4`, `c3`, `InternalTvActivity` and source popup code;
- targeted missing-class export;
- dynamic settings key mapping from Group 07;
- reflection method names in the Amlogic adapter;
- concrete sysfs/property strings;
- root-shell state code.

Where JADX produced structurally unreliable coroutine Java, Group 08 deliberately reports only behavior corroborated by callers, constants, UI wiring and command boundaries.

No claim in the reconstruction plan depends on a failed JADX region reconstruction.

---

# 25. Group 08 conclusions

1. Projectivy has a real generic input layer (`dd4` + `ed4`) rather than one-off HDMI buttons.
2. HDMI/AV switching uses a compatibility ladder: internal `TvView`, Xiaomi vendor intents, Fengmi/MediaTek/AOSP/Google TV apps, and generic TV Input Framework URIs.
3. Projectivy can itself host passthrough/tuner input using `TvView`.
4. Input shortcuts can exist both as home cards and dynamically enabled Android/Leanback launcher components.
5. Vendor settings are isolated behind capability checks and lightweight adapters.
6. Display Profiles is a Projectivy-owned generic profile system with runtime Amlogic/Xiaomi backends.
7. SDR and HDR profiles can be bound independently per input using stable profile UUIDs.
8. Zoom, input-lag, 4K-WM and property override tools are optional privileged/device-specific capabilities, not core launcher dependencies.
9. Aerial-Rows should preserve the same separation: generic portable input model, optional vendor adapters, optional privileged tooling.

## Next group

**Group 09 — HOME replacement, boot, accessibility & remote/key handling**

Targets include:

- `ProjectivyAccessibilityService`
- default HOME/launcher replacement mechanisms
- HOME key and remote interception
- mapped key actions and custom key bindings
- boot receiver / launch-on-boot source or app
- returning to Projectivy from vendor/input apps
- input/source detection hooks that live specifically in accessibility
- lifecycle and OEM compatibility behavior around launcher replacement


---

# SOURCE FILE: `group08_postclosure_audit_v2.md`

SHA-256: `0ba786c46fd5e6aa19f1266e7b434d2739bb73b4e8e74ff6953aa05007d8a3b3`

---

# Projectivy 4.72 — Group 08 Post-Closure Re-Audit v2

Status: **COMPLETE — re-audited with restored REA 6.7.0 / JADX MCP 0.7.1**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release.apk`  
APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
Original Group 8 report SHA-256: `668b12eba61d08be0d7e74904b3e60c6ae035f37c775db152e03747bc983f98e`

## Executive verdict

The original Group 8 architecture remains fundamentally correct. No hidden second HDMI/TV-input/device subsystem was found. The central model remains:

```text
dd4 stable source identity
        ↓
ed4 TIF discovery/cache/mapping
        ↓
c3 source launch coordinator
   ├─ internal TvView
   └─ vendor/platform fallback chain

DisplayProfileManager
        ↓
Amlogic / Xiaomi picture backends
```

The previous post-closure audit also remains valuable: its automatic SDR/HDR profile application, exact capability formula, seven-activation compatibility override, `input_last_used`, autostart convergence, remote HDMI keys, backend scoring, bootstrap and no-`TvInputManager.TvInputCallback` findings all survive this pass.

This deeper source-level re-audit does, however, make **two claim corrections** and adds **nine implementation-relevant precision/reliability refinements**.

---

## 1. Correction — internal `TvView` should not be described as proven tuner launching

`ed4.h()` genuinely discovers and caches both passthrough and non-passthrough `TvInputInfo` records.

`InternalTvActivity.J(TvView, dd4)` also contains both URI branches:

```text
ID contains HW
    → TvContract.buildChannelUriForPassthroughInput(id)

otherwise
    → TvContract.buildChannelsUriForInput(id)
```

But the verified generic source mapper `ed4.d(dd4)`:

- rejects Android/non-hardware `dd4` values;
- searches only the passthrough list `ed4.g`;
- requires the TIF ID to contain `HW`;
- then matches the stable source token/label.

Therefore, in the verified `dd4 -> ed4.d() -> InternalTvActivity.J()` route, the non-HW branch is not currently reachable.

### Canonical wording

Projectivy **discovers/cache non-passthrough/tuner inputs and contains defensive non-HW `TvView` code**, but the generic Group 8 launcher path is concretely verified for HDMI/AV passthrough. Do not infer first-class tuner launching from the defensive branch alone.

### Aerial-Rows

Model tuner/live-TV sources explicitly if we implement them. Do not let a defensive branch imply a supported `InputSource` type without a verified discovery-to-launch path.

---

## 2. Correction — hotkey cycling is visible-source cycling, not a direct available-input traversal

The Group 9 table previously summarized DPAD Left/Right as previous/next “available TV input.” Raw `dm3.a()` is more precise.

While temporary hotkey mode is active it:

1. starts from `ed4.l.u`;
2. increments/decrements the ordered `dd4` source value;
3. wraps through Android ↔ AV boundaries;
4. skips hardware source values whose `dd4.e()` shortcut-visibility preference is false;
5. invokes the selected generic source action.

It does **not** iterate `ed4.b()` or a resolved list of TIF IDs.

`dd4.c(source)` delegates to the generic `z52` action object. The associated action builds/launches the source activity; capability/fallback resolution happens later in the source-launch path.

### Canonical wording

```text
DPAD Left/Right in hotkey mode
    → previous/next visible generic source
    → downstream launch attempt
```

### Aerial-Rows

For predictable behavior, cycle an actual `AvailableInputs` snapshot rather than enum ordinals plus shortcut visibility.

---

## 3. `ed4.h()` fallback synthesis only applies when the whole passthrough list is empty

Fresh smali removes a Java-decompiler ambiguity.

For each HDMI/AV generic source:

```text
ed4.d(source) resolves
    → include

not resolved + passthrough list non-empty
    → do NOT synthesize that missing port

not resolved + passthrough list empty
    → Xiaomi compatibility OR manual compatibility override can include it
```

This is important: compatibility mode is **not** used to fill holes in a partially discovered HDMI list.

### Aerial-Rows

Keep three concepts separate:

```text
physical/platform discovery
compatibility assumption
user-visible shortcut configuration
```

Do not infer individual missing ports when the platform already supplied a partial authoritative input list unless a vendor adapter explicitly establishes them.

---

## 4. Sony/TCL recovery is demand-triggered rather than a timed retry loop

The previous audit correctly said Projectivy allows up to five recoveries from an abnormal empty passthrough state, but the mechanism can now be stated exactly.

When initialized and passthrough inputs are empty on Sony/TCL:

```text
retry counter < 5
    → ed4.e = false
    → retry counter++
    → return
```

There is no delayed re-query inside `ed4.h()` itself. A later caller that causes lazy initialization triggers the next discovery attempt.

### Aerial-Rows

Use explicit repository refresh plus bounded retry/backoff. Recovery should not depend on an unrelated future getter happening to touch the lazy cache.

---

## 5. Profile-binding choices are coupled to source-shortcut visibility in Projectivy

`ed4.b()` does:

```text
normalized source list ed4.i
    ↓
filter dd4.e() == true
    ↓
visible source list
```

The display-profile input-binding UI then uses:

```text
ed4.b()
+ Android inserted at index 0
```

So hiding an HDMI/source shortcut also removes that source from the profile-binding picker, even though the physical input concept and stored binding are separate.

### Aerial-Rows improvement

Do not copy this coupling.

```text
InputRepository.availableInputs
        ├─ home shortcut visibility
        └─ display-profile bindings
```

Both consumers should derive independently from the discovered source identity.

---

## 6. `input_last_used` is robust, but `ed4.l` current-source state is strategy-dependent

`c3.x()` persists the generic requested source before choosing its launch strategy:

```text
hj3.E.h(I())
```

This is the previously identified `input_last_used` state and remains reliable across internal/vendor launch strategies.

But the in-memory `ed4.l` value is explicitly updated only after:

- successful internal `F(...)` launch; or
- successful Xiaomi `EXTSRC_PLAY` launch.

If Xiaomi throws and one of the subsequent Fengmi / MediaTek / MiTV / AOSP / Google / generic external fallbacks succeeds, `c3.x()` returns without the same `ed4.l` assignment.

This matters because hotkey cycling uses `ed4.l` as its current-source starting point.

### Aerial-Rows improvement

All strategies should converge on one coordinator:

```text
launch(source)
    ↓
try strategy A/B/C/...
    ↓ success
CurrentInputState.set(source, strategy, platformId)
```

Do not allow individual adapters to own canonical current-source state.

---

## 7. Automatic display-profile polling has stricter activation rules

The previous audit correctly established the 4-second polling feature. Fresh verification adds the start condition.

`ProjectivyAccessibilityService.d()` only schedules the poller when:

```text
selected picture backend != null
AND
backend.g() != Integer.MIN_VALUE
```

So Accessibility being active is not sufficient by itself. The selected picture backend must expose usable source-detection capability.

### Aerial-Rows

Represent source/HDR detection as explicit backend capabilities rather than a sentinel integer.

---

## 8. Label changes are observed but do not trigger profile application

Each poll samples:

```text
current generic input
HDR/SDR state
input label
```

A change to any of those values updates the observed/diagnostic state, but profile application is gated specifically by:

```text
input changed
OR
HDR/SDR changed
```

A label-only change does not reapply the bound display profile.

### Aerial-Rows

Use separate state concepts:

```kotlin
InputPresentationState(input, label)
ProfileSelectionKey(input, hdrState)
```

---

## 9. The Projectivy 4-second poller can silently stop after a transient null sample

The poll runnable schedules its next 4-second execution only inside the path where the sampled label/state is non-null.

A transient invalid/null sample therefore skips `postDelayed(...)` and ends that polling chain until service configuration invokes its scheduler again.

This is a genuine reliability weakness, not a feature to reproduce.

### Aerial-Rows

`DisplayProfileCoordinator` should own a lifecycle-scoped continuous observation/retry loop. A temporary “unknown” sample must update state to `Unknown` or be ignored, but it must not terminate observation.

---

## 10. Dangling display-profile UUID bindings are tolerated, not repaired

Per-input SDR/HDR binding values are stable `lz0` profile UUIDs.

`DisplayProfileManager.d(uuid)` linearly resolves a UUID to a current profile index. If it cannot find it, it returns `-1`; `DisplayProfileManager.c(input,hdr)` then resolves the binding to `null`.

The picker stores:

```text
""         → None
profile.x  → selected profile UUID
```

No lookup-time repair/prune was found for a UUID that points to a deleted/malformed profile.

Result:

```text
dangling UUID
    → null selected profile
    → automatic apply becomes no-op
```

### Aerial-Rows

On profile load/delete/migration, perform referential-integrity cleanup or expose invalid bindings visibly rather than silently retaining them.

---

## 11. Exact picture-backend candidate set is now closed

The lazy DisplayProfile backend factory creates/probes exactly these candidates on the verified path:

1. Amlogic `SystemControlManager`
2. Amlogic `TvControlManager`
3. Xiaomi `PictureSettingsManager` / source-manager-backed adapter

It then chooses the candidate with the greatest resolved capability count (`ru.s()`).

This confirms the previous audit: backend choice is capability-scored, not fixed vendor precedence, and there is no hidden fourth backend in this factory path.

### Aerial-Rows

Use an explicit `PictureBackendCapabilities` model and deterministic suitability selection rather than reflection-method count.

---

## 12. `DisplayProfileManager.rps()` and bootstrap finding remain confirmed

Fresh source verifies that `rps()`:

- reads structured persisted profile records;
- deserializes valid `lz0` models;
- tolerates malformed records;
- creates a default profile if the resulting list is empty;
- sorts profiles case-insensitively;
- resets `ProjectivyAccessibilityService.s0` and another cached runtime string.

The earlier recommendation remains correct: Aerial-Rows should explicitly call a repository load during application bootstrap rather than copy Projectivy's opaque/native/bootstrap coupling.

---

## 13. Callback wording needs one important distinction

The prior audit's statement “no `TvInputManager.TvInputCallback`” remains correct.

Projectivy **does** use `TvView.TvInputCallback` for the actively tuned embedded `TvView`.

What is absent is a repository-level:

```text
TvInputManager.TvInputCallback
```

for live add/remove/update input discovery.

So the repository remains snapshot/cache/invalidation based.

### Aerial-Rows

Use platform input-manager callbacks where possible, plus explicit refresh on resume/re-entry as a safety net.

---

# Confirmed findings from the first post-closure audit

The deeper pass did not overturn these earlier improvements:

- automatic per-input SDR/HDR profile switching exists;
- normal poll interval is 4 seconds;
- compatibility formula is:
  `android.software.live_tv || Xiaomi tvplayer installed || manual compatibility override`;
- the hidden seven-activation About Device compatibility override invalidates/rebuilds input state;
- generic source identity is persisted via `input_last_used`;
- Group 9 autostart reuses the Group 8 source-launch layer;
- remote codes 243–246 map HDMI1–4;
- display backends are capability scored;
- `DisplayProfileManager.rps()` is an important initialization boundary;
- no repository-level `TvInputManager.TvInputCallback` is registered;
- root/sysfs functionality is optional shared expert infrastructure, not the normal HDMI path;
- standard Android TV / Nvidia Shield operation must not depend on vendor adapters.

---

# Updated Aerial-Rows Group 8 target architecture

```text
Platform TvInputManager
        │
        ├─ callback events
        ├─ resume safety refresh
        └─ bounded recovery/backoff
        ↓
InputRepository
        │
        ├─ DiscoveredInputs / AvailableInputs
        │       ├─ stable generic identity
        │       ├─ platform TIF identity
        │       └─ capabilities
        │
        ├─ VisibleInputShortcuts   (separate preference projection)
        └─ CurrentInputState
                 ↑
                 │ successful result from every strategy
InputLaunchCoordinator
        ├─ standard TIF/internal path
        ├─ Xiaomi adapter
        ├─ MediaTek adapter
        ├─ Amlogic/FengOS adapters
        └─ generic fallback

DisplayProfileRepository
        ├─ profiles by stable UUID
        ├─ integrity-checked SDR/HDR bindings
        └─ explicit startup load

DisplayStateRepository
        ↓
DisplayProfileCoordinator
        ├─ continuous resilient observation
        ├─ selection key = (input, HDR/SDR)
        └─ label = presentation metadata
```

This is cleaner and more reliable than duplicating Projectivy's visibility coupling, strategy-specific current-input writes, demand-triggered retry and Accessibility-owned poller.

---

# Updated final assessment

| Item | Result |
|---|---|
| Core Group 8 architecture | ✅ correct |
| Missing second input/device subsystem | none found |
| Subsystem-level factual reversal | none |
| Claim-level corrections | **2** |
| New precision/reliability refinements | **9** |
| Earlier post-closure findings | ✅ retained |
| REA/JADX verification | ✅ complete |
| Audit DB integrity | ✅ ok |

## Canonical corrections to carry forward

1. Do **not** claim verified first-class tuner/non-passthrough launching from the current `dd4` path.
2. Replace “previous/next available TV input” with “previous/next visible generic source, then downstream launch attempt.”
3. Separate discovered/available inputs from shortcut visibility in Aerial-Rows.
4. Centralize successful launch-state updates.
5. Make display-profile observation resilient and independent of Accessibility.
6. Clean dangling display-profile UUID bindings.

Group 8 can now be marked:

**COMPLETE — POST-CLOSURE RE-AUDITED v2**


---

# SOURCE FILE: `group09.md`

SHA-256: `a5b966da60c9a9d6940e0d4669176be500681c75764c4999d28e9bdf0eb74aa4`

---

# Projectivy 4.72 — Group 09: HOME replacement, boot/autostart, accessibility and remote/key handling

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group closes four tightly related Projectivy subsystems:

1. becoming / detecting the Android HOME launcher;
2. boot and resume-from-standby autostart behavior;
3. `ProjectivyAccessibilityService` and its fallback HOME override;
4. remote/key gestures, direct input hotkeys, idle/power actions and Activity-level key behavior.

The TV-input implementation itself was mapped in Group 08; this group only covers how HOME/accessibility/key handling invokes it.

---

## Analysis workflow and reliability improvements

Group 09 used the persistent cache/semantic graph rather than rescanning the whole APK. Named entry points were used as graph seeds:

- `MainActivity`
- `ProjectivyAccessibilityService`
- `StartUpBootReceiver`

The relevant obfuscated cluster was then ranked by semantic matches + graph distance. Missing central classes were batch-exported in one JADX load.

Two process improvements were added during this group:

### Multi-field single-load preference tracer

The old field-use helper reopened the APK for every `hj3` field. Group 09 added `JadxFieldUsesBatch`, which loads the APK once and traces many preference fields in a single run.

### Machine-verifiable critical assertions

A verification artifact now checks critical constants/control paths directly against extracted Java/smali. Group 09 currently passes **11/11** assertions, including:

- 300 ms long-press timing;
- 400 ms double-press timing;
- 15-second autostart debounce;
- 90-second boot autostart window;
- wake/standby autostart path;
- AccessibilityService event/flag configuration;
- Android HOME RoleManager API usage;
- HDMI1–4 hotkey mapping;
- double-BACK ambient behavior;
- Somnambulator launch;
- accessibility-generated HOME intent.

Evidence files are hashed in `re_cache/group09/evidence_manifest.json`, and discovery, verification and report are independently checkpointed.

---

# 1. HOME launcher boundary

`MainActivity` is the Projectivy HOME activity. Its manifest filter contains the Android TV/launcher categories established in Group 01, including:

- `HOME`
- `LAUNCHER`
- `LEANBACK_LAUNCHER`
- `LAUNCHER_APP`
- Fire OS `MAIN_HOME`

The Activity is configured as a launcher shell rather than a normal task-oriented application Activity: single-task, removed/excluded from Recents and with HOME-oriented task behavior.

## Detecting whether Projectivy is the current HOME

`MainActivity` has a lazy boolean that resolves the current `MAIN + HOME + DEFAULT` activity through `PackageManager` and compares its package to:

```text
com.spocky.projengmenu
```

This provides a direct answer to "is Projectivy currently the device HOME?" rather than assuming that being launched means it owns HOME.

---

# 2. Android HOME Role API

Projectivy uses Android's `RoleManager` when available.

The `yb3 / wb3` path explicitly calls:

```text
RoleManager.isRoleAvailable("android.app.role.HOME")
RoleManager.createRequestRoleIntent("android.app.role.HOME")
RoleManager.isRoleHeld("android.app.role.HOME")
```

The General settings screen exposes a **Change default launcher** action that goes through this role-request pathway.

This should be reproduced rather than relying on vendor-specific Settings intents when RoleManager is available.

## Restarting into the HOME task

`yb3.v(context)` resolves Projectivy's launch intent and uses:

```text
Intent.makeRestartActivityTask(component)
    .addCategory(Intent.CATEGORY_HOME)
```

It then starts that task and finishes the previous Activity when appropriate.

This is used after settings/language changes that require Projectivy to re-enter as the HOME task.

---

# 3. `general_override_home` and Accessibility fallback

Projectivy has a second mechanism in addition to being the official HOME app:

```text
general_override_home
```

The corresponding runtime field is copied into `ProjectivyAccessibilityService.p0`.

The service also enumerates:

1. the current default HOME Activity;
2. all Activities responding to `MAIN + HOME`.

If the resulting set is empty, or the only entry is Android's system `FallbackHome`, Projectivy forces the runtime HOME override on:

```text
p0 = true
```

even if the stored preference is off.

This means Projectivy's AccessibilityService can act as a resilience/fallback layer when Android does not have a usable normal launcher selected.

---

# 4. Boot receiver — important correction

The manifest declares `StartUpBootReceiver` for:

```text
android.intent.action.BOOT_COMPLETED
```

However, in the exact Projectivy 4.72 / versionCode 97 APK, the receiver itself is effectively a **no-op**.

Its complete `onReceive()` does only:

```text
intent?.action == BOOT_COMPLETED
```

and then returns. It does not launch Projectivy, an app, an input, or a service.

Therefore Projectivy's actual boot/autostart behavior must not be reconstructed as:

```text
BOOT_COMPLETED
    -> receiver starts selected app/input
```

That would be incorrect for this build.

---

# 5. Actual boot/autostart architecture

The real dispatcher is:

```text
dq3.z(context, bootWindow)
    -> dq3.b(context)
```

## Startup debounce

`dq3.z()` rejects another startup action if fewer than:

```text
15,000 ms
```

have elapsed since the previous attempt.

The timestamp is stored on `StartUpBootReceiver.a`, even though the receiver does not itself perform the launch.

This protects against MainActivity and AccessibilityService independently triggering the same startup behavior.

## Boot-only time window

When called with:

```text
bootWindow = true
```

Projectivy refuses to perform the startup action if:

```text
SystemClock.elapsedRealtime() > 90,000 ms
```

So the boot-autostart route is intentionally confined to approximately the first **90 seconds after Android boot**.

## Configurable startup delay

Preference:

```text
power_autostart_delay
```

If zero:

```text
dq3.b(context)
```

runs immediately.

Otherwise Projectivy posts it to the main looper after the configured delay in seconds.

---

# 6. What can be autostarted

Preference:

```text
power_autostart_source
```

is interpreted by `dq3.b()`.

Verified useful modes are:

### Application package

A package-name value resolves through Projectivy's normal app-launch resolver:

```text
Leanback launch intent
    -> normal launcher intent fallback
```

and can pass through the parental-control gate.

### Last-used TV input

Special value:

```text
-1
```

launches the `input_last_used` value through the Group 08 `ed4` TV-input subsystem.

### Screensaver / ambient mode

Special value:

```text
-100
```

calls `yb3.B(context)`, which launches:

```text
com.android.systemui/com.android.systemui.Somnambulator
```

with `FLAG_ACTIVITY_NEW_TASK`.

### Explicit TV input

Numeric values matching a `dd4` source resolve to the corresponding input and are launched by `ed4`.

The implementation therefore supports app, last input, explicit HDMI/AV input and ambient/screensaver startup from one setting.

Some additional numeric branches exist in the obfuscated dispatcher but are not needed to reproduce the verified user-visible modes and are intentionally not assigned speculative meanings here.

---

# 7. MainActivity boot trigger

`MainActivity.x()` is one real boot/startup trigger.

If:

```text
general_onboarding_completed == true
```

it invokes:

```text
dq3.z(this, true)
```

Otherwise it launches `OnboardingActivity`.

Thus when Projectivy is started as HOME shortly after Android boot, it can dispatch the configured autostart action.

---

# 8. AccessibilityService boot trigger

`ProjectivyAccessibilityService.onServiceConnected()` also checks:

```text
SystemClock.elapsedRealtime() < 90,000 ms
```

and launches an early-start coroutine.

That coroutine eventually invokes:

```text
dq3.z(service, true)
```

The 15-second debounce in `dq3.z()` prevents MainActivity and AccessibilityService from performing duplicate startup actions.

If startup does not occur and the Accessibility HOME-override is active, the service can post its internal HOME-launch runnable instead.

This is a much more robust design than placing all responsibility on `BOOT_COMPLETED`.

---

# 9. Resume-from-standby autostart

This is separate from boot startup.

Preference:

```text
power_autostart_leaving_standby
```

is observed by `nm3`.

When its wake/standby signal becomes true:

```text
if power_autostart_leaving_standby:
    dq3.z(service, false)
```

Passing `false` means the 90-second boot window does **not** apply.

So the architecture explicitly distinguishes:

```text
cold boot startup
    vs
resume-from-standby startup
```

while sharing the same final startup dispatcher.

---

# 10. AccessibilityService manifest/configuration

`ProjectivyAccessibilityService` is a real Android `AccessibilityService` protected by:

```text
android.permission.BIND_ACCESSIBILITY_SERVICE
```

Its XML declares:

```text
canRetrieveWindowContent = true
canRequestFilterKeyEvents = true
```

and points its settings activity to Projectivy's settings UI.

## Runtime service configuration

`d()` reads these preferences into runtime state:

```text
remotecontrol_show_menu
remotecontrol_decrease_input_lag
remotecontrol_bind_mic_btn_to_assistant

general_auto_fix_gplay
general_override_home

power_simulate_user_activity
power_internal_idle_detection
power_internal_idle_timeout
power_internal_idle_action
power_hdmi_input_exit_detection
power_internal_idle_excludes_media_playback
power_poweroff_mode

accessibility_alternate_config
accessibility_button_remapping
accessibility_volume_fix
```

The service then sets:

```text
eventTypes = 40
notificationTimeout = 0
```

`40` is the combination of the two event types it subsequently handles:

- focused-view changes;
- window-state changes.

Normal service flags:

```text
66
```

When accessibility button remapping is enabled:

```text
98
```

which adds the key-filtering capability.

Feedback type is selected between two modes according to the alternate-config preference.

---

# 11. Accessibility event handling

## Window-state changes

Projectivy tracks the foreground package/class as an `im3` value.

That state is then used for several behaviors:

- HOME override detection;
- idle/power logic;
- package-specific fixes;
- TV-input exit detection;
- Play Store workaround state;
- parental-control handling.

User-activity time is updated whenever a relevant window change occurs.

## Focus events

For specific configured package paths, focused `AccessibilityNodeInfo` trees can be inspected asynchronously.

The service includes recursive node-text extraction and Projectivy string/resource translation helpers, demonstrating that its accessibility integration goes beyond simple HOME-key interception.

---

# 12. Projectivy key-event engine

The remote remapper is a distinct subsystem:

```text
x00
 ├─ v00 ButtonEvent
 └─ w00 PressType
```

`w00` defines exactly:

```text
DOWN
PRESS
LONGPRESS
DBLPRESS
```

A binding key is stored as:

```text
pressType.ordinal * 10000 + keyCode
```

## Timing

Verified from `x00.e(KeyEvent)`:

```text
long-press dispatch: 300 ms

double-press window: 400 ms
```

If a key has a double-press binding, ordinary PRESS can be delayed until the 400 ms ambiguity window expires.

This timing should be reproduced explicitly rather than relying on Android's default long-click timing if Projectivy-like remote behavior is desired.

---

# 13. Verified key bindings

The AccessibilityService registers these bindings conditionally according to user preferences/capabilities.

| Key | Gesture | Behavior |
|---|---|---|
| Volume Up (24) | PRESS | volume-filter fix path when enabled |
| Volume Down (25) | PRESS | volume-filter fix path when enabled |
| Voice Assist (231) | PRESS | assistant broadcast path when enabled |
| synthetic/fallback key 0 | PRESS | assistant broadcast path |
| Menu (82) | LONGPRESS | toggle Projectivy low/high input-lag mode |
| Menu (82) | DBLPRESS | show temporary Projectivy remote/hotkey help mode |
| DPAD Up (19) | PRESS | previous display profile while hotkey mode is active |
| DPAD Down (20) | PRESS | next display profile while hotkey mode is active |
| DPAD Left (21) | PRESS | previous available TV input while hotkey mode is active |
| DPAD Right (22) | PRESS | next available TV input while hotkey mode is active |
| DPAD Center (23) | PRESS | exit/confirm hotkey mode |
| HOME (3) | PRESS | bring Projectivy HOME forward when HOME override is active |
| HOME (3) | DBLPRESS | Android global Recents action when HOME override is active |
| 243 | PRESS | HDMI1 |
| 244 | PRESS | HDMI2 |
| 245 | PRESS | HDMI3 |
| 246 | PRESS | HDMI4 |

The HDMI mapping is verified against `dd4`:

```text
dd4.B = HDMI1
dd4.C = HDMI2
dd4.D = HDMI3
dd4.E = HDMI4
```

---

# 14. Temporary remote "hotkey mode"

The Menu double-press helper calls `ProjectivyAccessibilityService.l()`.

It shows a helper message describing:

```text
◄/►
▲/▼
⦿
Projectivy
```

sets the temporary hotkey-mode flag, and schedules it to clear after:

```text
3 seconds
```

While this mode is active:

- UP/DOWN cycle display profiles;
- LEFT/RIGHT cycle enabled/available TV inputs;
- CENTER exits/confirms the mode.

The consume predicates for these DPAD bindings are conditional on the temporary mode, so Projectivy does not permanently steal navigation keys from other applications.

---

# 15. HOME override behavior

When Accessibility HOME override is active, HOME PRESS:

1. clears/resets Projectivy's current home-card selection state;
2. posts a runnable that launches Projectivy's HOME intent;
3. adds `FROM_HOME_KEY=true`;
4. consumes the key event.

The HOME Intent is lazily generated as:

```text
Projectivy package launch intent
```

with a fallback explicit `MainActivity` Intent carrying:

```text
CATEGORY_HOME
```

HOME DBLPRESS performs Android accessibility global action `3`, i.e. the Recents action.

This is the core of Projectivy's optional Accessibility-based HOME-button override.

---

# 16. Assistant-button remapping

When `remotecontrol_bind_mic_btn_to_assistant` is enabled, Projectivy binds the voice-assistant key and a fallback key path.

Those actions post an internal runnable that broadcasts Projectivy's stored:

```text
android.intent.action.ASSIST
```

Intent.

This keeps assistant invocation separate from the HOME/input/profile hotkey engine.

---

# 17. Input-lag hotkey

When `remotecontrol_decrease_input_lag` is enabled, LONGPRESS MENU invokes the input-lag toggle path.

The action flips Projectivy's runtime state and calls the display/input-lag tool mapped in Group 08.

The visible result uses the Projectivy strings corresponding to:

```text
input lag low
input lag high
```

This is a remote shortcut around the Group 08 privileged/device-specific tuning layer.

---

# 18. Accessibility volume fix

When:

```text
accessibility_volume_fix
```

is enabled, pressing the configured volume path calls `ProjectivyAccessibilityService.i()`.

That method temporarily clears AccessibilityService flag bit `32` (request-filter-key-events), applies the new service info, waits:

```text
5 seconds
```

and then re-adds bit `32`.

In practical architectural terms, Projectivy temporarily stops Accessibility key filtering so volume handling can pass through normally, then automatically re-enables its key interception.

---

# 19. MainActivity's own key handling

Not all remote behavior belongs to AccessibilityService.

`MainActivity` handles several keys directly while Projectivy itself is foreground.

## HOME

`KEYCODE_HOME` calls:

```text
MainFragment.y0()
```

which belongs to the Group 02 focus-restoration path.

## MENU

MENU DOWN calls `startTracking()` and consumes the event.

A normal non-long-press MENU UP opens Projectivy's settings/menu side panel through the existing side-panel dispatcher.

## DPAD

UP/DOWN/LEFT/RIGHT are also fed into Projectivy's local directional-input tracker.

## Media Play/Pause

A repeated `KEYCODE_MEDIA_PLAY_PAUSE` can emit wallpaper:

```text
Event.TimeElapsed
```

when the active wallpaper provider accepts that event.

This is a small but useful bridge between remote input and the Group 06 wallpaper-provider event model.

## User interaction

`onUserInteraction()` updates Projectivy's local last-interaction timestamp.

---

# 20. Double-BACK ambient/screensaver gesture

Preference:

```text
remotecontrol_double_back_ambient_mode
```

is implemented in the Activity back callback (`jm1`).

If disabled, or if the previous BACK was at least 400 ms ago, Projectivy performs its normal back behavior:

- if a row/card is not at its initial position, restore/refocus the relevant home position;
- otherwise use normal HOME/back behavior;
- finish Projectivy only when its HOME-override/default-HOME conditions permit it.

If a second BACK occurs within:

```text
< 400 ms
```

Projectivy calls:

```text
yb3.B(MainActivity)
```

which launches Android SystemUI's:

```text
com.android.systemui.Somnambulator
```

Therefore **double BACK enters ambient/screensaver mode**.

This is independent of HOME-button remapping.

---

# 21. Launcher idle mode and remote wake

`MainActivity` has a modal/idle-state gate.

While the launcher is in its idle presentation state, incoming key/touch DOWN events are consumed. On release, `MainActivity.F()`:

1. records user activity;
2. clears the idle-state flag;
3. sends wallpaper event:

```text
LauncherIdleModeChanged(false)
```

4. fades Projectivy overlays/title/status UI back in.

This closes an important loop with Group 06: Aerial/video wallpaper can enter an immersive idle presentation and ordinary remote interaction can return the same renderer to active-launcher presentation.

---

# 22. Internal idle detection

AccessibilityService also participates in Projectivy's internal idle/power system.

It maintains a global last-user-activity timestamp updated by key and window events.

Preferences involved include:

```text
power_internal_idle_detection
power_internal_idle_timeout
power_internal_idle_action
power_internal_idle_excludes_media_playback
power_poweroff_mode
```

The idle check can exclude situations where:

- a real media session is actively playing;
- notification/media state says playback is active;
- Projectivy's internal TV Activity is active;
- other configured foreground-app conditions apply.

This connects Group 05 media-session state to power/idle behavior.

---

# 23. Idle/power actions

`kg` defines the configured idle actions:

```text
POWER_OFF
SCREEN_OFF
SCREENSAVER
```

Availability is Android-version/capability gated where needed.

For power-off handling, `lg` defines:

```text
POWER_DIALOG
POWER_DIALOG_TWICE
POWER_DIALOG_OK
```

and `ProjectivyAccessibilityService.j()` uses Accessibility global actions to perform the selected behavior.

This is another reason Projectivy's AccessibilityService is a substantial system-integration component rather than merely a HOME-button hook.

---

# 24. Settings UI connections

Group 09 explicitly verified the settings surfaces corresponding to these runtime behaviors.

## Remote Control

The Remote Control settings expose:

```text
remotecontrol_show_menu
remotecontrol_double_back_ambient_mode
remotecontrol_decrease_input_lag
remotecontrol_bind_mic_btn_to_assistant
```

Device/capability checks hide unsupported options.

## Accessibility

The Accessibility settings expose status rows for both:

- service enabled;
- service actually started.

and options including:

```text
accessibility_alternate_config
accessibility_button_remapping
accessibility_volume_fix
accessibility_service_enable_reminder
```

## General

General settings expose:

- Change default launcher;
- `general_override_home`.

## Power

The Group 07 Power settings feed the autostart, standby, idle-detection and power-action paths described above.

---

# 25. Correct reconstruction architecture for Aerial-Rows

Projectivy should not be copied as one monolithic AccessibilityService. The behavior naturally separates into portable components.

```text
HomeRoleCoordinator
 ├─ isHomeRoleAvailable()
 ├─ isHomeRoleHeld()
 ├─ requestHomeRole()
 ├─ resolveCurrentHome()
 └─ restartAsHome()

StartupCoordinator
 ├─ startupTarget
 ├─ configurable delay
 ├─ 15s duplicate suppression
 ├─ boot-window policy
 ├─ wake-from-standby policy
 └─ dispatch app / input / ambient

RemoteGestureEngine
 ├─ DOWN
 ├─ PRESS
 ├─ LONGPRESS (300ms)
 ├─ DBLPRESS (400ms)
 └─ conditional consume predicates

ProjectivyAccessibilityBridge (optional)
 ├─ HOME override
 ├─ remote remapping
 ├─ foreground-window observation
 ├─ volume-filter workaround
 └─ privileged/global power actions

LauncherIdleCoordinator
 ├─ last interaction
 ├─ media-aware idle policy
 ├─ LauncherIdleModeChanged
 └─ Aerial immersive presentation
```

The Accessibility bridge should remain optional: normal HOME, app launching, TV Provider, rows, wallpaper/Aerial playback and ordinary input cards should not depend on it.

---

# 26. Specific Aerial-Rows recommendations

## Preserve Projectivy's separation of normal HOME and Accessibility override

Prefer:

```text
Android HOME role = primary mechanism
Accessibility HOME override = optional fallback / power-user feature
```

Do not make AccessibilityService mandatory just to use the launcher.

## Reuse one remote gesture engine

Implement the 300/400 ms behavior centrally so Activity and Accessibility bindings don't develop different timing semantics.

## Keep Aerial idle transition in the wallpaper engine

Group 06 + Group 09 together imply:

```text
normal launcher
    ↓ idle timeout
LauncherIdleModeChanged(true)
    ↓
Aerial provider/video remains on same renderer
    ↓ immersive presentation

remote interaction
    ↓
LauncherIdleModeChanged(false)
    ↓
rows/title/status fade back in
```

This is preferable to launching a second video Activity/screensaver player.

## Keep privileged power/display fixes modular

Nvidia Shield and normal Android TV operation should work without:

- Accessibility HOME override;
- root;
- sysfs access;
- Xiaomi/Amlogic engineering paths.

Those belong behind capability-driven adapters.

---

# 27. Confidence / unresolved edges

### High-confidence, independently verified

- RoleManager HOME support;
- default-HOME resolution;
- boot receiver no-op behavior;
- actual autostart dispatcher;
- 15-second startup debounce;
- 90-second boot window;
- app/input/last-input/ambient startup targets;
- standby-resume startup;
- accessibility config flags/events;
- HOME fallback activation when only FallbackHome exists;
- key gesture timings;
- HOME/MENU/DPAD/HDMI bindings;
- double-BACK Somnambulator behavior;
- volume-fix flag cycling;
- launcher idle wake path.

### Intentionally not assigned speculative semantics

A couple of obscure numeric autostart branches exist in `dq3.b()` but are not necessary to describe the verified visible startup choices. They should be revisited only if later persistence/backup evidence exposes a user-visible option that depends on them.

No evidence required treating `StartUpBootReceiver` as an active launcher in this exact build; smali proves the opposite.

---

# Group 09 conclusion

Projectivy's HOME/system integration is deliberately redundant:

```text
Android HOME role
      +
MainActivity HOME task behavior
      +
optional Accessibility HOME override
      +
early-boot Accessibility startup path
      +
MainActivity early-boot startup path
      +
shared 15s autostart debounce
```

This makes the launcher resilient across Android TV devices where OEM HOME behavior, boot timing and source switching differ.

The key implementation lesson for Aerial-Rows is to reproduce that **layering**, not to put everything inside a BootReceiver or AccessibilityService.

**Group 09 status: COMPLETE + VERIFIED.**

Next planned group: **Group 10 — persistence, backups, themes/config serialization, icons, shortcuts and parental controls.**


---

# SOURCE FILE: `group09_postclosure_audit_v3.md`

SHA-256: `81fee3e161b399cd24836408bfb508473402040e70deef4604ab983ac4aec07d`

---

# Projectivy 4.72 — Group 09 post-closure re-audit v3

Status: **COMPLETE — POST-CLOSURE RE-AUDITED v3**  
Reference APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Verdict

The Group 9 architecture remains correct. Projectivy still uses a layered system rather than a hidden second HOME/accessibility/remote subsystem:

```text
platform HOME role / HOME task
        +
optional Accessibility HOME override
        +
MainActivity startup trigger
        +
Accessibility startup trigger
        +
shared dq3 startup/autostart dispatcher
        +
wake/standby foreground-state refresh
        +
remote gesture engine
        +
launcher idle/power coordination
```

The v2 re-audit remains valid. The fresh v3 audit found **two genuine completeness misses** and **one smaller Xiaomi-specific foreground workaround**.

## Process hardening before/during v3

This audit was run under the machine-enforced closure process and also improved that process further:

1. persistent MCP session reuse + in-process JADX batch discovery fast path;
2. representative two-query discovery reduced from about 1.54 s to 0.81 s while retaining isolated subprocess fallback;
3. closure-version invariant fixed so later process versions do not invalidate the v7 closure-introduction rule;
4. FTS5 searches for dotted package/property identifiers now retry safely as literal phrases instead of crashing;
5. named-package REA targets are prevalidated against the pinned complete 7,395-class inventory before JVM startup, while default-package synthetic helpers remain pass-through because JADX may resolve them even when absent from `list_classes`;
6. failed probe quarantine remained effective: one pre-fix receiver-package typo was isolated and the corrected canonical probe succeeded;
7. Group 9 v3 was not grandfathered: all 11 closure dimensions were required and passed.

Canonical v3 verification: **19/19 evidence artifacts semantically valid, 0 canonical errors**.

## 1. New completeness miss — `power_simulate_user_activity` is a dual-path feature

The original Group 9 only listed the setting. It is substantially more specific.

`hj3.T` is:

```text
power_simulate_user_activity
```

with a default of `false`. `ProjectivyAccessibilityService.d()` loads it into the runtime flag `g0`.

### 1A. Xiaomi startup preconditioning

The Accessibility startup coroutine (`mm3.o()`, case 0) does:

```text
if power_simulate_user_activity
  and com.xiaomi.mitv.settings is installed:
      AccessibilityService.performGlobalAction(2)   // HOME
      wait 1 second

then:
      dq3.z(service, true)
      if no startup action occurred and HOME override is active:
          post Projectivy HOME runnable
```

So one purpose is Xiaomi-specific startup preconditioning immediately before the normal boot-autostart dispatcher.

### 1B. Recurring 60-second Accessibility-node action

The same setting also controls a completely separate runtime path.

When `g0` is enabled, `ProjectivyAccessibilityService.d()` schedules its `Q` runnable. `Q` is `km3(service,1)`.

That runnable:

```text
get Accessibility windows
      ↓
select a root node
      ↓
root.performAction(16384)
      ↓
post same runnable again after 60,000 ms
```

Therefore `power_simulate_user_activity` is not just a startup option. It also creates a recurring one-minute Accessibility action loop.

### Aerial-Rows implication

Do **not** reproduce this as generic Accessibility behavior. Model the intent explicitly:

```text
Idle/PowerCoordinator
      +
optional VendorStartupPreconditioner
```

If a device really needs a keepalive/workaround, it should be capability-gated and diagnosable rather than implemented through an opaque global Accessibility action.

## 2. New completeness miss — `general_auto_fix_gplay` toggles a privileged system property

The original Group 9 listed:

```text
general_auto_fix_gplay
```

but did not close its behavior.

`hj3.C` defines this setting. Its default is capability-dependent through Projectivy's GApps/device helper rather than being a universal constant.

`ProjectivyAccessibilityService.d()` loads it into static runtime flag `e0`.

The foreground observer (`nm3.i()`) then watches the Google Play Store package:

```text
com.android.vending
```

### Entering Play Store

When the fix is enabled and its state flag is inactive:

```text
sx(byte=4).b(TRUE)
S = true
```

The matching `sx` helper executes through Projectivy's privileged/root command infrastructure:

```text
setprop sys.cts.running 1
```

### Leaving Play Store

When Play Store is no longer foreground while `S` is active:

```text
post K after 2,000 ms
```

`K` is `cm3(service,1)`. When it runs, it rechecks the current foreground package. If Play Store is still no longer foreground and the workaround is still active, it calls the same helper with `FALSE`:

```text
setprop sys.cts.running 0
S = false
```

So this is a privileged **foreground-scoped CTS-property workaround**, not ordinary Play Store integration.

### Aerial-Rows implication

This should not live inside a generic launcher Accessibility bridge. If support is ever needed for a specific device, isolate it as an explicit optional privileged compatibility adapter, disabled on ordinary Android TV/Nvidia Shield installations.

## 3. Additional refinement — Xiaomi updater foreground workaround

The same Accessibility foreground observer also tracks:

```text
com.xiaomi.mitv.upgrade
```

Behavior:

```text
enter package:
    T = true

leave package while T active:
    T = false
    post L after 2,000 ms
```

The matching `e6` runnable invokes a device-specific helper using:

```text
Upgrade.UpgradeManager
http.*zip
```

This is another Xiaomi-specific compatibility action embedded in Accessibility foreground observation. It is not evidence of a separate input/device or Accessibility subsystem.

## 4. v2 architecture reverified

Fresh canonical evidence confirmed the important v2 conclusions still hold:

- `StartUpBootReceiver` is registered for boot but is not the actual launch coordinator;
- actual cold-start dispatch comes from MainActivity and/or Accessibility and converges on `dq3`;
- `dq3.z()` retains the 15-second duplicate policy and 90-second boot-window behavior documented in v2;
- `dq3.b()` remains the shared target dispatcher;
- wake/standby continues to use the same dispatcher without the 90-second boot restriction;
- `yb3.v()` remains the HOME-task restart/re-entry path;
- `x00.e()` remains the global remote gesture engine;
- `jm1.e()` remains part of the temporary profile/input hotkey path;
- `yb3.B()` / `yb3.y()` retain the system screensaver strategy/fallback chain;
- Accessibility `onAccessibilityEvent()` and `onKeyEvent()` retain the previously documented observer/remapping roles;
- Group 8 v2's correction still applies: LEFT/RIGHT hotkey cycling means previous/next **visible generic source**, not traversal of the physical TvInputManager list;
- no second hidden HOME-role, Accessibility or remote-control subsystem was found.

## 5. Closure matrix

Group 9 v3 passed every required process dimension:

| Dimension | Result |
|---|---|
| entry points | ✅ covered |
| base classes / inheritance | ✅ covered |
| all DEX files | ✅ covered |
| obfuscated collaborators | ✅ covered |
| models / data flow | ✅ covered |
| persistence | ✅ covered |
| resources / XML | ✅ covered |
| Android framework APIs | ✅ covered |
| callbacks / observers | ✅ covered |
| launch actions / lifecycle | ✅ covered |
| native / JNI | ✅ not applicable to Group 9 behavior; boundary already closed in Groups 1/11 |

The complete 7,395-class inventory spans the three DEX files. Relevant Group 9 classes resolved in `classes.dex` and `classes2.dex`; the complete inventory/semantic search found no Group 9-specific node in `classes3.dex` that changes this architecture.

## Updated Aerial-Rows target architecture

```text
HomeRoleCoordinator
 ├─ current/default HOME
 ├─ HOME role request/state
 └─ restartAsHome()

StartupCoordinator
 ├─ single cancellable pending startup job
 ├─ duplicate policy
 ├─ execution-time boot-window validation
 ├─ wake refresh + optional dispatch
 ├─ app/input targets
 └─ SystemScreensaverLauncher strategy chain

RemoteGestureEngine
 ├─ per-key gesture state
 ├─ explicit long/double timing
 ├─ explicit consume policy
 └─ protected-modal gate before global actions

AccessibilityBridge (optional)
 ├─ HOME fallback
 ├─ foreground/window observation
 ├─ optional key filtering
 └─ typed signals only

VendorCompatibilityAdapters
 ├─ XiaomiStartupPreconditioner
 ├─ PlayStoreCompatibilityWorkaround   // privileged, optional
 └─ XiaomiUpdaterCompatibilityWorkaround

InputLaunchCoordinator
 ├─ persisted canonical source state
 ├─ current runtime source state
 └─ visible-source traversal separate from physical availability

LauncherIdleCoordinator
 └─ shared wallpaper/Aerial renderer + active UI restore
```

The key design improvement is to keep vendor/root workarounds outside the generic Accessibility bridge.

## Final assessment

| Area | Result |
|---|---|
| Core Group 9 architecture | ✅ correct |
| Missing second subsystem | none |
| Major factual reversal | none |
| `power_simulate_user_activity` | **genuine miss — fully closed** |
| `general_auto_fix_gplay` | **genuine miss — fully closed** |
| Xiaomi updater observer workaround | newly documented refinement |
| v2 autostart edge semantics | reverified |
| HOME role/restart | reverified |
| remote/hotkey behavior | reverified |
| screensaver fallback | reverified |
| 11-dimension closure | ✅ 11/11 |

**Group 09: COMPLETE — POST-CLOSURE RE-AUDITED v3.**


---

# SOURCE FILE: `group10.md`

SHA-256: `bbdd9ea9811e69e96e1e78b725cac9b7cdc75fee1793c02a2829a291d68c8a3c`

---

# Projectivy 4.72 — Group 10: Persistence, Backups, Icons, Shortcuts & Parental Controls

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
Verification: **20/20 critical assertions passed**

## 1. Scope

This group closes five related Projectivy subsystems:

1. typed preference/config persistence and version migrations;
2. `.plbackup` full backups and `.pltheme` theme packages;
3. global icon packs and per-card artwork overrides;
4. Android launcher shortcuts and pinned-shortcut hosting;
5. parental-control policy, gating, authentication and recovery behavior.

The goal is behavioral reconstruction, not verbatim transplantation of Projectivy implementation code.

---

## 2. Process / reliability improvements used for this group

Before Group 10 discovery, the reverse-engineering pipeline was hardened again.

### 2.1 Fail-closed preflight

Every expensive stage now checks:

- exact APK SHA-256;
- semantic SQLite `PRAGMA integrity_check`;
- source-cache presence;
- hashes of every previously completed checkpoint artifact.

The current semantic index remains healthy:

- 5,952 indexed class records;
- 37,328 class-to-class reference edges;
- 1,994 resource references;
- 17,422 indexed literals;
- database integrity: `ok`.

### 2.2 Read-only/WAL discovery

Discovery/ranking queries use read-only SQLite access while checkpoint writes use the normal writable connection. This reduces contention and avoids unnecessary serialization between searching and checkpointing.

### 2.3 Skip-existing targeted class exporter

A content-aware class dumper now checks whether a requested selected-class export already exists. JADX is loaded only when at least one requested class is genuinely missing.

This preserves the reliability of targeted decompilation without repeatedly paying the APK-load/decompile cost for already-cached evidence.

### 2.4 Stage checkpoints

Group 10 was checkpointed separately as:

- discovery;
- verification;
- synthesis;
- report.

The discovery checkpoint contains a hashed 36-file evidence manifest. Verification used source + smali and passes 20/20 assertions.

---

# 3. Persistence architecture

## 3.1 `yh3`: typed preference descriptor

Projectivy does not scatter raw SharedPreferences calls throughout the launcher. The central preference abstraction is the obfuscated class `yh3`.

A descriptor carries information including:

- preference key;
- default value;
- title/description resource IDs;
- icon metadata;
- flags;
- optional numeric min/max/step;
- optional custom getter/setter hooks;
- optional dynamic/scoped suffix;
- legacy/migration alias information.

Important verified methods include behavior equivalent to:

```text
a()   → read current/effective value
b()   → calculate effective key
e(x)  → create/use a scoped key variant
g(old)→ migrate value from a legacy key
h(v)  → write value using hook/default path
i(v)  → direct typed write with numeric bounds handling
j(scope, value) → scoped write
```

The important architectural point is that settings are **descriptors**, not just strings.

That is why Projectivy can attach behavior such as:

- dynamic category-specific keys;
- migration aliases;
- validation/clamping;
- accessibility-service refresh requirements;
- custom serialization.

For Aerial-Rows, a comparable `TypedPreference<T>` abstraction is preferable to exposing raw DataStore keys everywhere.

---

## 3.2 `hj3`: central configuration store

`hj3` is the central application preference/configuration store.

It performs typed reads and writes for:

- Boolean;
- Int;
- Long;
- Float;
- String;
- StringSet;
- enum-like values represented by integer values.

Null/empty-string handling can remove preferences where appropriate.

`hj3` also owns serialization for structured launcher state rather than forcing all data into primitive keys.

Examples include:

```text
action_categories_list
    → serialized category models (`i5`)

action_customizers_list
    → serialized card customization/usage models (`l5`)

display_profiles_list
    → serialized display-profile objects

cached wallpaper state
    → serialized Wallpaper model
```

The implementation uses Kotlin serialization-style JSON infrastructure for these structured models.

This confirms the design we should use in Aerial-Rows:

```text
simple scalar setting
    → typed preference / DataStore

structured model collection
    → explicit versioned JSON serializer
```

rather than trying to encode every object into independent preference keys.

---

# 4. Versioned migrations

Projectivy has a genuine sequential migration engine.

The stored schema/app version is:

```text
key_app_version
```

Projectivy 4.72 migrates persisted state to:

```text
97
```

If the previous value is greater than zero and below 97, it iterates through intervening migration versions.

When migrations are invoked in the normal startup path, Projectivy first requests an automatic backup.

Conceptually:

```text
read stored version
      ↓
old but supported?
      ↓
automatic backup
      ↓
run migrations sequentially
      ↓
write version 97
```

### Confirmed migration examples

Migration 86 performs a large preference-key rename from older `key_*` names to the current descriptor keys. This includes launcher, background, card, row, status-bar, accessibility, parental-control and power settings.

Other verified migration families include changes around:

- category field representation;
- wallpaper delay units;
- category/grid configuration;
- wallpaper display-mode representation.

This is important for our reconstruction: **config migrations need to be part of the architecture from the beginning**, not something bolted on after we ship the first version.

Recommended Aerial-Rows pattern:

```text
MigrationRegistry
 ├─ v1 → v2
 ├─ v2 → v3
 ├─ ...
 └─ currentVersion
```

with an automatic safety backup before destructive migration.

---

# 5. Export policy and `nexp`

Projectivy uses a naming convention containing:

```text
nexp
```

for preferences that must not be included in ordinary exported backup/theme state.

The backup code explicitly skips any key whose name contains `nexp`.

Examples elsewhere in the settings map include transient/private-ish values such as:

- last-start date;
- unique installation/runtime key;
- theme author/name runtime metadata;
- cached wallpaper data.

So `nexp` can safely be interpreted at the behavior level as a **not-export marker**.

On full-restore/reset paths, Projectivy can remove all exportable settings while preserving these `nexp` settings.

For Aerial-Rows, this should become an explicit property rather than a string-name convention:

```text
TypedPreference(
    ...,
    exportPolicy = NEVER
)
```

That is safer and easier to audit.

---

# 6. Full `.plbackup` backups

Projectivy supports two high-level backup types:

```text
FULL_BACKUP
THEME
```

A `BackupParams`-style model controls theme/export options such as:

- export appearance settings;
- export wallpaper;
- export individual default-category settings.

Typical full-backup filename:

```text
projectivy-settings-yyyyMMdd_HHmmss.plbackup
```

## 6.1 Full backup payload

For a full backup, Projectivy reads all SharedPreferences and excludes `nexp` keys.

Supported values are converted into a JSON object while preserving type information through JSON representation:

- Boolean;
- Int;
- Long;
- Float;
- String;
- enum integer value;
- StringSet as an array.

The resulting JSON string is then encrypted before being written to the `.plbackup` URI.

---

## 6.2 Full backup restore

The import path was checked against smali because parts of the coroutine-heavy Java reconstruction are unreliable.

Verified behavior is:

```text
read backup bytes
      ↓
decrypt
      ↓
UTF-8 JSON
      ↓
clear existing exportable prefs
(preserve nexp keys)
      ↓
restore typed values
      ↓
run migrations
      ↓
refresh wallpaper / launcher caches / runtime state
```

Projectivy therefore treats restore as a **replacement of exported state**, not as a blind merge with arbitrary existing values.

That distinction matters when implementing our own import semantics.

---

# 7. `.plbackup` encryption

The export side is unambiguous.

Projectivy generates:

- random 16-byte salt;
- key derived from the salt;
- random 16-byte IV;
- AES/CBC/PKCS5Padding ciphertext.

The output is concatenated as:

```text
salt + IV + ciphertext
```

The key-derivation function starts from the UTF-8 bytes of:

```text
walt_sent_me
```

and performs 10,000 iterations of:

```text
SHA-256(previousBytes + salt)
```

The resulting digest becomes the AES key.

## Reliability caveat

The decrypt method's reconstructed salt-prefix length includes a runtime/static `ProjectivyAccessibilityService.s0` term in addition to 16. Both Java reconstruction and smali show that reference, while the exporter visibly writes a 16-byte salt.

That expression is therefore **runtime/obfuscation-sensitive** and should not be promoted into a clean portable file-format claim without executing the original runtime path.

This does not affect the behavioral reconstruction:

- backups are encrypted;
- the exporter format and algorithm are known;
- Projectivy itself can read its backups.

For Aerial-Rows there is no reason to copy this legacy cryptographic design. We should use a versioned authenticated-encryption envelope, e.g. AES-GCM, with explicit header/version/KDF parameters.

---

# 8. Theme packages (`.pltheme`)

As established in Group 6 and completed here, `.pltheme` is a ZIP-based package.

Verified entries include:

```text
manifest
appearance.plbackup
wallpapers/        (optional)
```

## 8.1 Theme manifest

The typed manifest model carries fields including:

- `projectivyLauncherVersion`;
- `themeVersion`;
- date;
- author;
- name;
- description;
- URL;
- credits.

Built-in Projectivy sample themes confirm that `manifest` is ordinary human-readable JSON.

`appearance.plbackup` is an encrypted appearance-settings payload using the backup mechanism rather than plain JSON in the ZIP.

## 8.2 Appearance/theme scope

Theme export can include:

- general appearance preferences;
- wallpaper-provider selection;
- optionally default-category appearance/settings;
- optionally wallpaper media in `wallpapers/`.

When category styling is exported as theme appearance, membership/exclusion content is intentionally treated differently from category styling so a theme does not simply overwrite a user's application membership as though it were a full backup.

That distinction is exactly what we should preserve:

```text
full backup = user state

theme = appearance/presentation package
```

---

# 9. Runtime refresh after restore/reset

Backup/reset logic does more than update SharedPreferences.

Projectivy resets/invalidate runtime structures including areas such as:

- home/category state;
- cached model lists;
- icon-pack state;
- wallpaper-related state;
- AccessibilityService config refresh.

This is an important implementation lesson.

For Aerial-Rows, import should finish with a central:

```text
ConfigChanged / RestoreCompleted
```

operation that tells repositories and UI state holders to reload coherently.

We should not rely on individual Composables noticing random DataStore mutations independently.

---

# 10. Icon-pack discovery

Projectivy supports conventional Android launcher icon packs.

It discovers candidates with:

```text
org.adw.launcher.icons.ACTION_PICK_ICON
```

For each candidate package, Projectivy accepts it as an icon pack when it exposes either:

```text
appfilter.xml
```

or:

```text
config_iconpack = true
```

The discovered icon-pack metadata includes:

- package name;
- version name;
- version code;
- icon;
- label;
- whether the package has a Leanback launch activity.

The global configured icon pack is stored in:

```text
appearance_cards_icon_pack
```

---

# 11. Icon-pack parsing and cache

Projectivy parses `appfilter.xml` entries containing component and drawable mappings.

Conceptually:

```xml
<item
    component="ComponentInfo{package/activity}"
    drawable="some_drawable" />
```

The component identity is normalized and mapped to the icon-pack drawable resource.

Projectivy persists a compact parsed cache. The binary cache has a verified:

```text
magic   = 1346983491
version = 2
```

This allows icon resolution without repeatedly reparsing potentially large icon-pack XML files.

For Aerial-Rows we do not need to clone this binary format. A cache keyed by:

```text
iconPackPackage + versionCode
```

is enough, as long as package update events invalidate it.

---

# 12. Artwork priority: per-card override vs icon pack

For an application card, Projectivy effectively resolves art in this order:

```text
per-card custom artwork
        ↓
global icon-pack mapping
        ↓
normal app banner/icon artwork
```

The per-card customizer model (`l5`) also stores information already encountered in Group 3:

- stable card/action ID;
- custom artwork URI;
- crop setting;
- custom title;
- launch count;
- last-launch timestamp.

The serialized collection is stored under:

```text
action_customizers_list
```

This gives us a clean separation for Aerial-Rows:

```text
App metadata
    ≠
User presentation override
```

so package rescans do not destroy user customizations.

---

# 13. Icon picker / external picker integration

Projectivy has an internal image/icon picker mode specifically for icon packs.

It can receive:

- icon-pack package;
- target package;
- target label.

The picker can:

- search/filter icons;
- display icon-pack contents;
- adapt card shape/aspect based on whether the pack is TV/Leanback-oriented;
- return the selected URI;
- delegate to an external icon-pack picker where appropriate.

Projectivy also exposes external apply-icon-pack actions in the manifest, so compatible icon packs/tools can request application through the launcher.

Any Projectivy premium-entitlement checks around that feature are product policy, not architecture that needs to be copied into Aerial-Rows.

---

# 14. Android launcher shortcuts

Projectivy is a proper Android launcher shortcut host.

## 14.1 Pinned shortcut requests

`PinShortcutActivity` handles Android O+ `LauncherApps.PinItemRequest`.

It:

1. retrieves the pin request from the incoming Intent;
2. requires request type `1` (shortcut);
3. verifies the request is valid;
4. avoids accepting an already-pinned shortcut;
5. calls `accept()`;
6. finishes with user feedback.

This is why Projectivy declares the relevant `CONFIRM_PIN_SHORTCUT` launcher-host intent in its manifest.

---

## 14.2 Shortcut repository

Projectivy's shortcut repository uses real `LauncherApps` APIs.

It first verifies:

```text
LauncherApps.hasShortcutHostPermission()
```

Initial pinned-shortcut query uses:

```text
ShortcutQuery.setQueryFlags(2)
```

A per-package query uses flags `11`.

The exact Android flag composition for numeric value 11 is platform API-defined; the important verified behavior is that Projectivy asks LauncherApps for a richer per-package shortcut set rather than storing its own static shortcut definitions.

The `LauncherApps.Callback` path from Group 3 also receives:

```text
onShortcutsChanged
```

and updates the shortcut cache.

---

## 14.3 Shortcut home cards

Shortcut card model `p94` is home item type 7.

It uses:

- shortcut short label as title;
- long label as secondary description when useful;
- `ShortcutInfo.isEnabled`;
- `LauncherApps.getShortcutIconDrawable`;
- `LauncherApps.startShortcut` for activation.

The local shortcut card stable ID is based on:

```text
shortcutInfo.getId().hashCode()
```

Package context is tracked by the surrounding ShortcutInfo/repository structures, although this particular local ID expression does not itself include the package name.

For Aerial-Rows we should use a stronger global identity such as:

```text
hash(packageName + shortcutId + user/profile)
```

while preserving Android LauncherApps as the source of truth.

---

# 15. Parental-control persistence

The primary relevant settings are:

```text
parental_control_apps
parental_control_code_long_duration
parental_control_protect_settings
parental_control_partial
parental_control_partial_start
parental_control_partial_end
parental_control_code
parental_control_emr_code
parental_control_emr_enc_code
```

Protected items are stored as stable unique identifiers represented as strings in a comma-separated list.

`ToggleParentalControlActivity` receives:

```text
uniquename
```

and adds/removes that stable identity from the list.

This means parental protection attaches to Projectivy's **stable card/action identity**, not to screen position.

That is the correct model for Aerial-Rows as well.

---

# 16. Parental policy / launch gate

The central check behaves approximately as:

```text
no primary code?
    → no parental gate

partial/scheduled mode enabled?
    ↓
current time inside configured window?
    ├─ no → no gate
    └─ yes → continue

settings protection enabled
AND target == settings stable ID?
    → gate

otherwise
    → gate only if target stable ID is protected
```

The special stable ID used for settings protection is:

```text
-1922314973
```

The schedule logic handles a window crossing midnight by moving the end boundary into the following day.

So a policy such as:

```text
22:00 → 07:00
```

is treated correctly as an overnight interval.

---

# 17. Parental launch wrapping

Rather than duplicating PIN checks inside each application launcher path, Projectivy can wrap the real destination Intent inside `ParentalControlCheckActivity`.

The wrapper carries data such as:

```text
launchIntent
launchClassName
```

After successful authentication, the requested underlying operation is performed.

Settings can be similarly routed through a start-for-result protection path.

For Aerial-Rows this should become a generic:

```text
LaunchGate.authorize(targetStableId) {
    performAction()
}
```

usable for:

- application launch;
- settings;
- sensitive internal actions;
- potentially protected TV inputs or shortcuts.

---

# 18. Authentication session duration

Projectivy caches a successful parental authentication timestamp in process memory.

Two verified unlock-duration policies exist:

```text
short duration = 15,000 ms
long duration  = 900,000 ms (15 minutes)
```

If a prior successful authentication is still inside the chosen duration, the user does not need to re-enter the code.

This should be modeled explicitly in Aerial-Rows as a short-lived authentication session rather than by permanently mutating protection state.

---

# 19. Primary and emergency/recovery codes

The parental screen accepts either:

- the primary parental code;
- an emergency/recovery sequence.

If no emergency code exists, Projectivy generates one.

The generator creates a 12-character sequence from:

```text
U / D / L / R
```

It maps those directions to digits 1–4, converts the resulting number and computes a transformed display/recovery value with the constants:

```text
2012789
1014333443013
```

The raw directional sequence and transformed display/recovery value are stored in separate preferences.

The transformed value is displayed by the parental-control UI as recovery/emergency information.

The exact product recovery workflow should be treated separately from the core launch-gate architecture; the verified fact is that Projectivy has both a primary secret and a generated emergency path.

---

# 20. Security assessment for our implementation

Projectivy 4.72 stores the primary/emergency parental values as ordinary SharedPreferences strings.

They are not `nexp` keys, which means they are part of the exportable preference set; the full `.plbackup` envelope is encrypted, but the live local preferences themselves are not a modern secret store.

For Aerial-Rows we should preserve behavior while improving security:

```text
ParentalPolicyStore
    → ordinary DataStore/config

ParentalSecretStore
    → Android Keystore-backed verifier / encrypted store
```

Ordinary backups should export:

- which targets are protected;
- schedule settings;
- policy settings;

but should **not** export raw PIN/recovery secrets unless the user explicitly chooses a protected secret-transfer mechanism.

Likewise, new backups should use authenticated encryption instead of copying the legacy AES-CBC format.

---

# 21. Reconstructed architecture for Aerial-Rows

Group 10 suggests the following implementation boundaries.

## `ConfigStore`

```text
ConfigStore
 ├─ TypedPreference<T>
 ├─ scoped/dynamic keys
 ├─ JsonBlobStore
 ├─ ExportPolicy
 └─ MigrationRegistry
```

Properties should know:

- default;
- serializer/type;
- validation;
- export policy;
- whether a runtime subsystem must refresh.

## `MigrationRegistry`

```text
read schemaVersion
      ↓
optional safety backup
      ↓
apply ordered migrations
      ↓
write current schema
```

## `BackupEngine`

```text
BackupEngine
 ├─ FullBackupExporter
 ├─ FullBackupImporter
 ├─ ThemeExporter
 ├─ ThemeImporter
 └─ BackupEnvelopeCodec
```

Use a versioned envelope with authenticated encryption and explicit metadata.

## `IconRepository`

```text
IconRepository
 ├─ AppOverrideStore
 ├─ IconPackDiscovery
 ├─ AppFilterParser
 ├─ IconPackCache
 └─ ArtworkResolver
```

Resolution order:

```text
user override
  → icon pack
  → TV banner/icon fallback
```

## `ShortcutRepository`

```text
ShortcutRepository
 ├─ LauncherApps permission check
 ├─ pinned/dynamic shortcut query
 ├─ LauncherApps.Callback refresh
 ├─ startShortcut
 └─ icon loading
```

plus a `PinShortcutActivity`-equivalent launcher-host entry point.

## `LaunchGate`

```text
LaunchGate
 ├─ protected stable IDs
 ├─ settings protection
 ├─ scheduled enforcement
 ├─ authentication session
 └─ secure secret verifier
```

It should wrap actions rather than make each individual card know PIN logic.

---

# 22. Cross-group integration

Group 10 connects directly to previous findings.

### Group 2 / home

Category and card stable IDs are the anchor for:

- custom artwork;
- custom titles;
- parental protection;
- persisted row membership/order.

### Group 3 / apps

App-card launch counts and last-launch timestamps live in the same customization model that stores card presentation overrides.

### Group 6 / themes

`.pltheme` combines appearance backup with optional wallpaper assets and uses the existing generic wallpaper/provider engine after import.

### Group 7 / settings

The `yh3/hj3` persistence layer is the backend for the 148 mapped settings descriptors.

### Group 9 / launching

Parental control is a generic launch wrapper and therefore composes with HOME, autostart and internal actions rather than needing separate protected launch code for each card type.

---

# 23. Reliability notes / unresolved implementation details

The subsystem is sufficiently closed for independent reconstruction, but a few details should intentionally **not** be overclaimed:

1. The decrypt-side salt prefix expression references `ProjectivyAccessibilityService.s0`. This was confirmed in both JADX Java and smali, but is inconsistent with a naive reading of the exporter. Treat it as runtime/protection-sensitive rather than a clean file-format constant.
2. Numeric `ShortcutQuery` flags are verified exactly as used (`2`, `11`), but this report does not invent semantic flag names that were not needed to reconstruct behavior.
3. Projectivy-specific premium gating encountered around icon/parental features is product entitlement logic, not required architecture for Aerial-Rows and is not being reproduced as a license bypass.
4. The exact internal compact binary layout of Projectivy's icon-pack cache is unnecessary for compatibility; the behavioral need is a versioned parsed mapping cache that invalidates when the icon-pack package changes.

---

# 24. Group 10 conclusions

1. Projectivy has a real typed configuration layer (`yh3`/`hj3`) rather than raw preference strings spread throughout the UI.
2. Persistence is versioned and migration-aware, with a safety backup before normal migrations.
3. Full backup is export-policy-aware: `nexp` values are intentionally preserved/excluded.
4. Full backup restores replace exportable state, then run migrations and refresh runtime caches.
5. `.pltheme` is a ZIP package containing a JSON manifest, encrypted appearance backup and optional wallpapers.
6. Global icon packs are ADW-compatible and parsed from `appfilter`; per-card custom artwork takes priority over the pack.
7. Projectivy is a genuine Android shortcut host, including pin requests and LauncherApps-backed shortcut cards.
8. Parental control is stable-ID based, centrally launch-gated, schedule-aware and session-authenticated.
9. Aerial-Rows should preserve these behaviors while deliberately improving secret storage and backup cryptography.
10. Persistence, backup, icon, shortcut and parental boundaries are now sufficiently understood to implement independently.

## Next group

**Group 11 — cross-cutting obfuscated-class closure / unresolved dependency graph**

The purpose of Group 11 is not another feature inventory. It is a systematic final pass over the classes and edges discovered in Groups 1–10 to identify any Projectivy-specific collaborator that remains unclassified or any behavior still supported only by inference, then close those gaps before writing the final Aerial-Rows implementation map in Group 12.


---

# SOURCE FILE: `group10_postclosure_audit_v2.md`

SHA-256: `d75640cc4a23d2a7d392ee3322a50d59c942fad9c09094b4e38a58c130d2c126`

---

# Projectivy 4.72 — Group 10 Post-Closure Re-Audit v2

**Scope:** persistence, migrations, `.plbackup`, `.pltheme`, icon packs, Android shortcuts, parental controls, and their cross-group integration.  
**Audit namespace:** `AUDIT_10_R2`  
**Target APK SHA-256:** `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
**Original Group 10 report SHA-256:** `bbdd9ea9811e69e96e1e78b725cac9b7cdc75fee1793c02a2829a291d68c8a3c`  
**Process contract:** v16

## 1. Executive assessment

The original Group 10 architecture remains correct. This re-audit found **no second hidden persistence, backup, theme, icon-pack, shortcut, or parental-control subsystem**, and no factual reversal requiring the original Group 10 model to be discarded.

The fresh pass did, however, close several implementation details that materially improve the reconstruction specification:

1. parental authorization has its own active key-filtering path and transfers runtime state to/from `ProjectivyAccessibilityService`;
2. Projectivy can internally pin **and unpin** hosted Android shortcuts, not merely query/start them or accept external pin requests;
3. the two previously observed `ShortcutQuery` numeric flags have distinct verified roles (`2` for the global hosted cache, `11` for package-scoped queries);
4. theme export has an exact built-in-category cutoff (`i5.j < 23`) and strips two per-user category collections from exported copies;
5. the parsed ADW icon-pack cache is explicitly keyed by **icon-pack package + package version** and validates a magic/version header;
6. the full-backup replacement/import/migration/refresh flow is now split and canonically verified at method level;
7. the crypto framing is more tightly bounded: initialized runtime strongly indicates a 16-byte salt prefix, while the exact managed bootstrap call remains opaque and belongs to the Group 11 boundary.

Canonical verification: **23/23 successful evidence files, 0 errors, 0 invalid outputs**.  
Subsystem closure: **11/11 dimensions PASS**.

---

## 2. Original architecture that still holds

### 2.1 Typed configuration and structured state

`yh3` remains the typed setting/descriptor abstraction and `hj3` remains Projectivy's central configuration facade. Group 10's original conclusion that Projectivy is not merely scattering raw preference strings across Activities remains correct.

`hj3` also remains responsible for structured state beyond scalar settings, including serialized launcher/category/customization objects reached by the settings, backup and theme paths.

### 2.2 Sequential schema migration

The `key_app_version` migration model remains correct:

```text
stored app schema/version
        ↓
older than current version 97?
        ↓ yes
request safety backup
        ↓
run migration steps sequentially
        ↓
write current version
```

The earlier finding that migration 86 contains a substantial old-key rename/conversion layer remains valid.

### 2.3 `nexp` export policy

`nexp` remains a real **not-export policy**, not just a naming convention. Full backup export excludes such values and replacement-style restore deliberately preserves them when requested.

### 2.4 Full `.plbackup`

The original model remains correct:

```text
SharedPreferences / structured state
        ↓
filter non-exportable values
        ↓
typed JSON backup representation
        ↓
encrypt
        ↓
.plbackup
```

Restore replaces the exportable state, imports typed values, runs schema migration and refreshes runtime state.

### 2.5 `.pltheme`

`.pltheme` remains a ZIP package with the important structure:

```text
manifest
appearance.plbackup
wallpapers/
```

Theme scope is intentionally narrower/different from a complete user backup.

### 2.6 ADW-compatible icon packs

Projectivy continues to discover icon packs through the ADW action:

`org.adw.launcher.icons.ACTION_PICK_ICON`

and accepts package resources through `appfilter` and/or the `config_iconpack` capability marker.

### 2.7 Android launcher shortcuts

Projectivy remains a genuine Android launcher shortcut host using `LauncherApps`, `ShortcutInfo`, `ShortcutQuery`, `LauncherApps.Callback`, pin-item requests and shortcut-backed home cards.

### 2.8 Parental-control policy

The original stable-ID/launch-gate interpretation still holds. The system has persisted primary/recovery values, authentication TTL/session behavior, scheduled enforcement and centralized wrapping of protected actions.

---

## 3. New finding — parental UI actively filters key events

The original report concentrated on launch gating and authentication policy. The re-audit confirms that `ParentalControlCheckActivity` also actively filters key input.

`ParentalControlCheckActivity.dispatchKeyEvent(KeyEvent)` delegates the key code to `pq0.I(...)`. If the predicate says the key is blocked/consumed, the Activity consumes it instead of allowing normal dispatch.

The canonical `pq0.I()` evidence also shows that the key interpretation is adjusted using `ProjectivyAccessibilityService.s0` before being compared against the relevant parental-control key collection/state.

This means the parental subsystem is more tightly coupled to the Accessibility runtime than Group 10 originally stated.

### Reconstruction consequence

If Aerial-Rows ever implements parental controls, the better architecture is:

```text
Remote/Input layer
        ↓
ParentalInputPolicy
        ↓
Authentication / LaunchGate
```

rather than making an Accessibility service own parental key semantics.

For the current Aerial-Rows scope, parental controls remain optional/excluded; this behavior should therefore be documented rather than copied incidentally into the Accessibility implementation.

---

## 4. New finding — successful parental authorization transfers Accessibility state

`ParentalControlCheckActivity.F()` verifies the entered value against the primary and emergency/recovery values and updates the authentication timestamp.

On successful authorization it also performs:

```text
ProjectivyAccessibilityService.u0 = ProjectivyAccessibilityService.t0
ProjectivyAccessibilityService.t0 = null
```

before continuing the requested action/result flow.

That is a genuine coupling between the parental authorization Activity and the global Accessibility runtime state that was absent from the original Group 10 report.

### Reconstruction consequence

Do **not** reproduce this coupling. Authentication/session state should be owned by a launch/auth coordinator and passed explicitly to whatever action needs authorization.

---

## 5. New finding — Projectivy can pin and unpin hosted shortcuts itself

The original report documented:

- incoming external pinned-shortcut requests;
- `LauncherApps` shortcut queries;
- shortcut cards;
- shortcut launching.

The re-audit closes an additional mutation path in `j6.y0()`.

For shortcut cards on supported Android versions, Projectivy:

1. extracts the `ShortcutInfo`;
2. gets `LauncherApps`;
3. queries the package's current shortcut IDs with query flag `2`;
4. removes the current ID if it is already pinned, otherwise adds it;
5. calls `LauncherApps.pinShortcuts(package, ids, userHandle)`;
6. refreshes the affected launcher/category state.

So the correct independent model is:

```text
ShortcutRepository
 ├─ query
 ├─ callback refresh
 ├─ startShortcut
 ├─ accept external pin request
 └─ mutate pinned ID set
```

rather than a read/launch-only repository.

---

## 6. Refined shortcut query semantics — flags `2` vs `11`

The original report correctly recorded both numeric flag values but intentionally did not invent semantic names. The fresh evidence now closes where each is used.

### `yb3.d()` — global hosted shortcut cache

Projectivy first verifies shortcut-host capability and then builds/updates the launcher-wide shortcut cache using:

```text
ShortcutQuery.setQueryFlags(2)
LauncherApps.getShortcuts(..., current user)
```

### `xb3.o()` — package-scoped query

The package-specific path uses:

```text
ShortcutQuery.setPackage(package)
ShortcutQuery.setQueryFlags(11)
```

### Callback refresh

`nb3.onShortcutsChanged(...)` forwards the package/list update to the shared shortcut cache updater.

The correct reconstruction therefore preserves the **two query contexts** even if our code replaces opaque numeric constants with named internal query modes.

---

## 7. Refined icon-pack cache — package and package-version identity

The correct cache-building/parsing method is `zl4.b()`.

It constructs the parsed icon-pack cache filename from both:

```text
icon-pack package
+
icon-pack package version
```

in the form equivalent to:

```text
iconpack-<package>-<version>.cache
```

The parser also validates:

- magic: `1346983491`;
- cache format version: `2`.

If the cache is invalid/missing, Projectivy reparses the icon-pack resources.

This directly verifies that cache invalidation is not merely package-name based.

`pq0.z()` independently reconfirms ADW-compatible discovery: `appfilter` XML or the `config_iconpack` boolean capability is sufficient for recognition.

### Aerial-Rows consequence

Use a cache key based on at least:

```text
packageName + versionCode/versionName + our cache schema version
```

and regenerate when any component changes.

---

## 8. New theme-export precision — built-in category cutoff and per-user stripping

`dj3.d()` confirms the full-backup branch still starts from all SharedPreferences values and excludes `nexp` values.

The **theme** branch has additional policy that the original report did not fully capture.

When default/category settings are included in a theme, Projectivy:

1. obtains the category collection;
2. retains only categories satisfying `i5.j < 23`;
3. copies each category object;
4. clears two copied per-user collections (`u` and `v`);
5. serializes the reduced category state under `action_default_categories_settings`.

This means Projectivy has an implicit numeric boundary between built-in/default categories and user-specific category data.

### Aerial-Rows consequence

Do **not** clone the magic number `23`.

Model this explicitly:

```text
CategoryOrigin
 ├─ BUILT_IN
 ├─ PROVIDER
 └─ USER
```

and define separately which fields are theme-exportable vs user-state-only.

---

## 9. Full restore flow — now canonically split across methods

The re-audit strengthens the previous restore model.

### `dj3.n(true)` — replacement/reset policy

When preserving never-export values, Projectivy enumerates existing preference keys and removes every key that is not protected by the `nexp` policy. The alternate branch clears the store.

It then reapplies configuration/migration state and runs the runtime reset/rehydration path.

### `dj3.i(...)` — import path

For a full backup, the importer invokes the replacement/reset behavior, performs typed JSON import, calls `hj3.a(false)` to re-run/normalize migration state, and triggers the runtime/background refresh path.

### `dj3.j(...)` — theme ZIP import

The Smali fallback verifies the ZIP-based theme path including `appearance.plbackup` and the `wallpapers/` entries.

There is therefore no hidden second restore engine; full backup and theme import converge on the existing config/migration/runtime-refresh architecture.

---

## 10. Backup crypto — stronger bound, but bootstrap caveat remains

Canonical `s71.d()` proves the key derivation routine starts with the fixed UTF-8 secret:

`walt_sent_me`

and performs 10,000 rounds of SHA-256 over the evolving key material plus salt, then creates an AES key.

Canonical `s71.b()` proves decrypt uses:

- salt prefix length `ProjectivyAccessibilityService.s0 + 16`;
- next 16 bytes as IV;
- `AES/CBC/PKCS5Padding`;
- remaining bytes as ciphertext.

Cross-group canonical evidence for `DisplayProfileManager.rps()` sets:

```text
ProjectivyAccessibilityService.s0 = 0
```

as part of normal initialized runtime state. This strongly indicates an effective 16-byte salt prefix during initialized operation.

However, Group 11 found no normal managed Java caller for `DisplayProfileManager.rps()`. The exact bootstrap linkage therefore remains opaque across the managed/bootstrap/native boundary.

### Correct conclusion

We can say with high confidence that the normal initialized format is consistent with:

```text
16-byte salt
16-byte IV
AES-CBC ciphertext
```

but should **not** overclaim the exact hidden initialization mechanism that establishes `s0`.

### Aerial-Rows consequence

Do not reproduce this crypto design. Prefer authenticated encryption such as AES-GCM and appropriate key management/KDF semantics rather than a fixed embedded secret plus AES-CBC.

---

## 11. Recovery-code transform reconfirmed

`pq0.p()` reconfirms the original Group 10 recovery-code interpretation:

- a 12-character directional sequence is built from Up/Down/Left/Right;
- directions are mapped to digits `1–4`;
- the raw and transformed forms are persisted separately;
- the deterministic transform includes `(value * 2012789) % 1014333443013`.

No reversal was found here.

---

## 12. External pin-request intake reconfirmed

`PinShortcutActivity.onCreate()` is loaded from `classes2.dex` and verifies the incoming `LauncherApps.PinItemRequest`:

- request exists;
- request type is `1`;
- request is valid;
- shortcut is not already pinned;
- `accept()` succeeds.

It then finishes the Activity.

This remains a thin launcher-host entry surface rather than a second shortcut subsystem.

---

## 13. No hidden second subsystem

Fresh semantic-index discovery, narrowed JADX inspection, canonical REA verification and all-DEX closure found no second implementation for:

- configuration storage;
- schema migration;
- backup/export/import;
- theme packaging/import;
- icon-pack discovery/parsing/cache;
- Android shortcut hosting;
- parental authorization.

The major collaborators continue to converge on the previously mapped `yh3/hj3`, `dj3/s71`, `pq0/zl4`, `yb3/xb3/nb3/j6/p94` and parental Activity/helper architecture.

---

## 14. Updated Aerial-Rows reconstruction boundaries

### ConfigStore

```text
ConfigStore
 ├─ typed SettingKey<T>
 ├─ structured launcher state repositories
 ├─ schema version
 └─ MigrationRegistry
```

### BackupEngine

```text
BackupEngine
 ├─ explicit export policy
 ├─ typed serialization
 ├─ authenticated encryption
 ├─ replacement / merge policy
 └─ post-restore migration + rehydrate
```

### ThemeEngine

```text
ThemeEngine
 ├─ theme manifest
 ├─ appearance payload
 ├─ optional wallpaper assets
 ├─ explicit category-origin policy
 └─ theme-exportable vs user-state-only fields
```

### IconRepository

```text
IconRepository
 ├─ per-card override
 ├─ ADW icon-pack parser
 ├─ package+version keyed parsed cache
 └─ TV banner/icon fallback
```

### ShortcutRepository

```text
ShortcutRepository
 ├─ global hosted cache
 ├─ package-scoped queries
 ├─ LauncherApps.Callback
 ├─ startShortcut
 ├─ external PinItemRequest intake
 └─ pin/unpin mutation
```

### LaunchGate / parental state

If implemented in the future:

```text
LaunchGate
 ├─ protected stable IDs
 ├─ schedule policy
 ├─ auth TTL/session
 ├─ secure verifier
 └─ input filtering policy
```

It should **not** depend on Accessibility global static fields.

---

## 15. Process improvements made before/during this re-audit

The process was deliberately reviewed and improved before Group 10 conclusions were accepted.

### 15.1 Full authoritative APK resource inventory

Persistent JADX remains in `resources=lite` mode because that makes class/method discovery much faster. That lightweight daemon is now explicitly **non-authoritative for resource closure**.

A separate cached full APK inventory pins:

```text
APK entries          1,171
res/ entries         1,066
res XML entries        831
```

including entry CRC/size metadata and an inventory digest. Audit preflight/self-test fails closed if this no longer matches the pinned APK.

### 15.2 Safe additive provenance upgrades

Process evolution can add new derived provenance fields without invalidating sound old evidence, but only if:

1. every field present in the old manifest still matches exactly;
2. the newly introduced field is independently verified against the pinned target/toolchain;
3. the old manifest is preserved in provenance history before re-attestation.

### 15.3 In-process persistent JADX code-search batching

The existing single-session MCP batch path was extended to `jadx_search`.

A broad whole-APK code search still proved too expensive for the execution envelope. The process therefore now explicitly prefers:

```text
reports/evidence
   ↓
FTS + static semantic index
   ↓
narrowed persistent JADX
   ↓
canonical REA for high-risk claims
```

### 15.4 Dotted-literal semantic-index fallback

The semantic code index had the same FTS5 punctuation weakness previously fixed in `auditctl search`: identifiers such as `appearance.plbackup` could be parsed as FTS syntax.

The query layer now safely falls back to a quoted literal search. An initial implementation mistake in this change was caught, restored from backup, corrected, and a real runtime dotted-literal invocation was added to self-test so compilation alone is no longer considered sufficient.

### 15.5 Lock-aware immediate orphan cleanup

An outer execution timeout interrupted a canonical batch after two committed probes and left an uncommitted temporary output for the next probe.

The evidence remained safe, but cleanup was previously age-based. It now:

1. maps the temp output to its canonical per-output advisory lock;
2. attempts a nonblocking exclusive lock;
3. removes the temp/err/empty lock immediately only if that lock is free;
4. leaves active writers untouched.

This makes external interruption cleanup immediate without racing a live probe.

### 15.6 Content validation after probe success

A successful REA command is **not automatically accepted as proof of the intended claim**.

During this audit an initial `zl4.a()` probe succeeded but proved only runtime icon lookup, not icon-cache construction. Content validation caught the mismatch and the correct `zl4.b()` cache parser/builder was canonically verified before synthesis.

This is now an explicit operational rule: **probe success != claim success**.

### 15.7 Smaller canonical verification batches

Fresh REA verification generally took about 7–13 seconds per uncached high-impact probe in this cycle. Canonical probes were therefore run in small batches (typically three) so completed results land atomically before the outer command window, while state files make interruption/resume exact.

A cross-group cached `DisplayProfileManager.rps()` verification returned in roughly **0.79 seconds**, preserving the earlier cache gains.

---

## 16. Closure matrix

The fresh Group 10 audit is not grandfathered. It passed the current machine-enforced 11-dimension closure matrix:

| Dimension | Result |
|---|---|
| Entry points | PASS |
| Base classes / inheritance | PASS |
| All DEX files | PASS |
| Obfuscated collaborators | PASS |
| Models / data flow | PASS |
| Persistence | PASS |
| Resources / XML | PASS |
| Android framework APIs | PASS |
| Callbacks / observers | PASS |
| Launch actions / lifecycle | PASS |
| Native / JNI | N/A — no Group 10 core path depends on JNI; opaque display-profile bootstrap remains Group 11 boundary |

Canonical evidence health:

```text
success   23
error      0
invalid    0
```

---

## 17. Final assessment

```text
Core Group 10 architecture       correct
Missing major subsystem          none
Factual reversals                none
Canonical evidence               23 / 23 successful
Closure                          11 / 11 PASS
```

### Genuine completeness misses closed by v2

1. parental Activity key filtering through `pq0.I()`;
2. parental authorization's Accessibility runtime-state transfer;
3. internal shortcut pin/unpin mutation with `LauncherApps.pinShortcuts()`;
4. exact theme built-in-category cutoff and per-user collection stripping.

### Important refinements

- exact global flag-`2` vs package-scoped flag-`11` shortcut-query split;
- package+version keyed ADW icon-pack cache with verified magic/version;
- exact full-restore replacement/import/migration/rehydration split;
- stronger initialized-runtime interpretation of the decrypt salt prefix without overclaiming the opaque bootstrap caller;
- reconfirmed recovery-code transform and external pinned-shortcut intake.

**Group 10 status: COMPLETE — POST-CLOSURE RE-AUDITED v2.**


---

# SOURCE FILE: `group11.md`

SHA-256: `4f9c9bd36edba833c20b7964a12aae1182396ebde85e74aceca3da7396c4caca`

---

# Projectivy 4.72 — Group 11: Cross-cutting obfuscated-class closure

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
Verification: **33 / 33 closure assertions passed**

## 1. Purpose

Groups 1–10 reconstructed the major Projectivy subsystems independently: package architecture, home UI, apps/actions, Android TV Provider content, notifications/media, wallpaper/themes, settings, TV inputs/vendor integration, HOME/boot/accessibility, and persistence/backups/icons/shortcuts/parental controls.

Group 11 is deliberately different. It is a closure pass whose job is to answer:

> Is there still a Projectivy-specific implementation subsystem hidden behind obfuscated classes or overlooked named components that would materially block a from-scratch reconstruction?

The criterion is **behavioral and architectural closure**, not source-line closure. Generic AndroidX/Kotlin/Media3/Glide/coroutine machinery does not need to be reverse engineered merely because it was minified into `defpackage.*`. Conversely, an unclassified class that changes launcher behavior does need to be traced.

## 2. Process improvement used for this group

Before closure analysis, the existing persistent evidence cache was extended with an automated **closure auditor**.

It compares:

- every Groups 1–10 report;
- manifest-declared Projectivy components;
- the local semantic source index;
- class-to-class reference graph;
- resource references;
- string/literal index;
- unresolved/deferred wording in previous reports;
- app-affinity of obfuscated classes.

This converts closure from a broad APK rescan into:

```text
Groups 1–10 reports
        +
manifest component inventory
        +
semantic/class graph
        ↓
automated closure audit
        ↓
ranked unresolved items
        ↓
small targeted decompile/export batches
        ↓
smali/native verification where needed
        ↓
fail-closed assertions
```

The discovery stage recovered **26 previously untraced named Activity classes in one APK load** and ranked the remaining unmentioned obfuscated collaborators rather than searching all classes again.

All Group 11 stages are independently checkpointed and hashed:

```text
GROUP_11/discovery      COMPLETE
GROUP_11/verification   COMPLETE
GROUP_11/synthesis      COMPLETE
GROUP_11/report         COMPLETE
```

## 3. Closure result

The closure conclusion is:

> **No remaining cross-cutting Projectivy-specific subsystem appears necessary to reproduce the launcher behavior mapped in Groups 1–10.**

The remaining opaque implementation falls into one of these categories:

1. standard/minified AndroidX, Kotlin, coroutine, Media3 or other library machinery;
2. narrow Projectivy utility/helper classes whose externally relevant behavior is already covered;
3. optional root/vendor maintenance tools rather than launcher-core features;
4. synthetic/compiler-generated dispatch classes;
5. premium/entitlement UI, which is outside the clean-room reconstruction target;
6. native/protection/bootstrap details that do not implement the normal launcher feature set;
7. exact decompiler-hostile control flow for behaviors whose observable architecture is already closed.

This means Group 12 can move from reverse engineering to the concrete Aerial-Rows implementation map without assuming that a large unidentified Projectivy subsystem remains hidden.

---

# 4. Previously untraced named Projectivy components

The manifest/component comparison exposed a set of named classes that earlier thematic groups had described indirectly but had not explicitly traced.

A single batch export recovered 26 such Activities.

## 4.1 Category/settings shortcut wrappers

### `CategoryContentSidePanelActivity`

This is a thin settings wrapper around Projectivy's category-content preference screen.

It does not introduce another category engine. It enters the same category/settings system reconstructed in Groups 2 and 7.

### `ManageCategoriesActivity`

Thin wrapper for the categories-management preference screen.

No separate persistence or row implementation was found.

### `ManageChannelsActivity`

Thin wrapper for the TV-channel management preference screen.

It routes into the channel/settings functionality already reconstructed in Groups 4 and 7.

**Closure:** these three Activities are UI entry points, not missing subsystems.

---

# 5. Vendor settings and engineering wrappers

The closure pass explicitly classified the named vendor Activities from Group 1.

## 5.1 Xiaomi / Mi TV

`SettingsMiTvActivity` targets Xiaomi's settings package/component, including:

```text
com.xiaomi.mitv.settings
```

## 5.2 Formovie / FengOS

`SettingsFengOsActivity` targets:

```text
com.formovie.gsettings
```

## 5.3 Amlogic / Droidlogic

`SettingsAmlogicActivity` targets Droidlogic TV settings components and has a fallback path to an alternate settings Activity.

## 5.4 MediaTek

`SettingsMediatekActivity` uses the vendor action:

```text
mediatek.tvsetting.factory.intent.action.MainmenuActivity
```

## 5.5 Standard Android

`SettingsAndroidActivity` opens:

```text
android.settings.SETTINGS
```

## 5.6 Fengmi/FengOS engineering menus

`EngMenuFengmiActivity` and `EngMenuFengOsActivity` launch the appropriate vendor test/engineering Activities, including the FengOS autofocus-test package where available.

**Closure:** these are exactly the vendor-adapter entry points anticipated by Group 8. They do not introduce a second device-control architecture.

---

# 6. Previously overlooked app/device maintenance tools

Group 11 found a set of real Projectivy utilities that were not central to Groups 1–10. They are useful to understand but are **optional privileged maintenance tools**, not dependencies of the launcher core.

## 6.1 `InstallAppsActivity`

Projectivy includes a guided APK installer utility.

Verified behavior includes:

- accepts an `extra_install_url` target;
- supports HTTP(S) and local/storage-oriented paths;
- checks whether Projectivy is allowed to request package installs;
- if necessary opens:

```text
android.settings.MANAGE_UNKNOWN_APP_SOURCES
```

for its own package;

- launches APK content with:

```text
application/vnd.android.package-archive
```

- contains helper paths for downloaded/compressed package files;
- reports failures through Projectivy's common toast/status helper.

This is independent of the app-card uninstall/App Info actions mapped in Group 3.

### Aerial-Rows implication

If desired later, this belongs in an optional `DeviceMaintenanceTools`/installer module, not `AppRepository`.

---

## 6.2 App freeze/unfreeze utility

`CleanupActivity` is effectively an **application freeze** tool rather than a generic cache cleaner.

Its delegate offers:

- freeze suggested packages;
- unfreeze suggested packages;
- manually freeze a selected package;
- manually unfreeze a selected package.

The worker path calls Projectivy's privileged/root shell manager (`kx3`) for freeze and unfreeze operations.

Projectivy's root manager itself tests root capability and executes shell operations asynchronously.

### Aerial-Rows implication

This must remain capability-gated and optional. Normal app hiding/favourites/uninstall must never depend on root.

---

## 6.3 Stock launcher / Leanback launcher maintenance

`RemoveLauncherActivity` contains identities for stock launcher packages including:

```text
com.google.android.leanbacklauncher
```

as well as Xiaomi/Formovie launcher targets and associated system paths.

Its guided UI exposes **freeze** and **unfreeze** operations for stock or Leanback launchers.

The underlying work is privileged/root-oriented.

This is separate from Projectivy's legitimate Android HOME-role handling reconstructed in Group 9.

### Aerial-Rows implication

Do not make root-based stock-launcher modification part of the normal HOME setup path. Use Android HOME role/default-launcher APIs first. Privileged launcher maintenance, if ever implemented, should be clearly isolated as an expert tool.

---

## 6.4 Google Play self-update blocking

`BlockGooglePlaySelfUpdateActivity` exposes two actions:

```text
Block
Unblock
```

and delegates to a privileged helper.

The exact low-level mutation is not required for launcher reconstruction and was intentionally not expanded beyond the user-visible and privilege boundary.

---

## 6.5 GApps uninstall/debloat

`GappsUninstallActivity` presents a confirmation path and invokes a privileged helper for the operation.

Again, this is device maintenance rather than launcher architecture.

---

# 7. Web Remote utility

`WebRemoteActivity` does not appear to host its own local HTTP server.

Its delegate asynchronously determines the local-device IP and constructs an external URL of the form:

```text
http://mireco.hopto.org/#<local-ip>
```

It presents/opens that remote-web page.

The evidence therefore supports classifying Web Remote as an external web utility keyed to the local IP, not as a hidden Projectivy web-server subsystem.

If Aerial-Rows ever wants remote administration, it should be designed independently rather than depending on this external service.

---

# 8. Other shortcut/wrapper Activities

## 8.1 `MediaExplorerActivity`

Launches an available Xiaomi/FengOS media-explorer target. This is another vendor shortcut.

## 8.2 `GoogleSmartHomeActivity`

Launches Projectivy's lazily resolved Google TV Smart Home intent when the capability exists.

## 8.3 `ProfilePickerActivity`

Launches a lazily resolved supported profile-picker target.

## 8.4 `AccessibilityServiceActivity`

Provides a guided configuration/status surface around the already-mapped Projectivy Accessibility Service.

No second accessibility subsystem exists.

## 8.5 `ColorPickerActivity`

Reusable Material-style colour picker with luminance/alpha-related controls and a dynamic-colour preference.

This is a UI helper for settings/theme features rather than a hidden configuration store.

---

# 9. About/device diagnostic Activities

`AboutAppActivity`, `AboutBackgroundActivity`, and `AboutDeviceActivity` are informational/diagnostic GuidedStep screens.

The device screen can surface information such as:

- Android/device/build properties;
- display and HDR capabilities;
- IP/network information;
- root status;
- display-profile information;
- some projector/device temperatures where available.

A repeated-tap developer-style interaction exists in this area and can invalidate/reload TV-input related information.

These screens expose already-known capabilities; they do not introduce another hardware abstraction layer.

---

# 10. Premium/entitlement surface

`PremiumActivity` and `PremiumCardActivity` were identified and classified as entitlement/purchase UI surfaces.

They were **not** reverse engineered for entitlement bypass or premium unlocking.

For the reconstruction project, the relevant principle is simply:

```text
feature implementation
    ≠
Projectivy entitlement implementation
```

Aerial-Rows can implement its own feature set and licensing model independently.

---

# 11. Autostart sentinel closure

Group 9 left two obscure numeric branches in the startup coordinator for closure.

The autostart handler contains special values including:

```text
-1    last-used TV input
-100  ambient/screensaver
-2    legacy/sentinel-looking branch
-101  legacy/sentinel-looking branch
```

The current 4.72 settings UI explicitly offers:

- the available `dd4` input values;
- `-1` for last-used input when appropriate;
- `-100` for ambient/screensaver;
- application/package targets separately.

The current settings source does **not** expose `-2` or `-101` as selectable options.

Therefore the best-supported classification is:

> `-2` and `-101` are legacy/internal/sentinel compatibility branches, not missing user-facing autostart choices in Projectivy 4.72.

No additional startup target needs to be reproduced merely because these compatibility branches remain in bytecode.

---

# 12. `SOURCE_UNKNOWN` closure

`dd4.SOURCE_UNKNOWN` is explicitly represented by:

```text
Integer.MIN_VALUE
```

and an `unknown` identity.

It is a sentinel/fallback state, not another user-selectable TV source.

This closes the Group 8 ambiguity cleanly.

---

# 13. Backup decrypt framing and runtime initialization

Group 10 established the portable export-side backup framing:

```text
16-byte random salt
16-byte random IV
AES/CBC/PKCS5Padding ciphertext
```

with the key derivation seeded by `walt_sent_me` and iterated through SHA-256 10,000 times.

The decryption method contains the unusual expression:

```text
ProjectivyAccessibilityService.s0 + 16
```

when extracting the salt prefix.

The closure pass found a highly relevant runtime fact:

`DisplayProfileManager.rps()` assigns:

```text
ProjectivyAccessibilityService.s0 = 0
```

and no managed Java caller of `DisplayProfileManager.rps()` appears in JADX's uses graph.

This creates the following strongly supported interpretation:

```text
process starts
   ↓
runtime/bootstrap path not visible as ordinary managed caller
   ↓
DisplayProfileManager.rps()
   ↓
s0 becomes 0
   ↓
decrypt prefix length becomes 16 bytes
```

However, the exact mechanism by which `rps()` is invoked is **not proven** by managed xrefs alone. It may involve native/bootstrap/protection behavior.

The report therefore does **not** assert an exact native call chain that has not been demonstrated.

For Aerial-Rows this ambiguity is non-blocking. We should not clone this framing trick. Use an explicit versioned backup format with modern authenticated encryption such as AES-GCM.

---

# 14. Native `libptutils.so` closure

Projectivy ships `libptutils.so` for its Android ABIs.

The arm64 library exports at least:

```text
Java_com_spocky_projengmenu_PTApplication_vc
Java_com_spocky_projengmenu_PTApplication_vs
```

The managed code declares/calls `PTApplication.vs(boolean)` through a narrow path:

```text
PTApplication.vs(...)
       ↑
PTApplication.f()
       ↑
  v.o(...)
  ya3.o(...)
```

JADX uses show `vs()` has only `PTApplication.f()` as its managed caller, and `f()` has two managed caller sites.

Arm64 inspection indicates `vc` is a very small global-byte accessor while `vs` is the larger JNI/JNIEnv-heavy routine.

The library also contains common archive/zlib/minizip machinery and helper code, but no evidence indicates that Projectivy's home-row engine, app discovery, TV Provider repository, media-session layer, wallpaper engine, settings hierarchy, or TV-input architecture is implemented primarily inside the native library.

### Closure conclusion

`libptutils.so` is best treated as a narrow **initialization / validation / protection / utility boundary**, not as an undiscovered native launcher implementation.

It should **not** be transplanted into Aerial-Rows.

Any legitimate initialization behavior we need—such as loading display profiles—should be made explicit in Kotlin.

Opaque validation/protection logic is outside the clean-room reconstruction target.

---

# 15. Group 6 residual decompiler ambiguities

The closure auditor correctly surfaced two deliberately cautious statements from Group 6:

1. the exact coroutine control flow used to refresh/cache wallpaper-provider candidates;
2. the exact internal copy/persistence control flow for some imported background files.

These do **not** represent missing externally meaningful architecture.

Already verified behavior includes:

- provider discovery;
- provider IDs and update modes;
- event masks;
- per-provider candidate lists;
- cache timestamps/TTLs;
- invalidation/update broadcasts;
- IMAGE/VIDEO/COLOR/DRAWABLE/LOTTIE renderer selection;
- selected/folder/theme/program/Now Playing provider behavior;
- user-selected image/video handling;
- render/lifecycle/error fallback behavior.

For a from-scratch implementation, reproducing a compiler-generated coroutine state machine or Projectivy's private file-copy sequence line-for-line adds no behavioral value.

These are therefore classified as **source-level ambiguity, behavioral architecture closed**.

---

# 16. High-affinity obfuscated-class closure

The closure auditor ranked obfuscated classes by direct references to known Projectivy code, resource use, role tags, and graph proximity.

The highest-scoring previously unmentioned classes included examples such as:

```text
ez2
r82
ss
jj4
xo1
h03
em3
b5
b4
yn1
bi3
ko0
iw4
fs4
...
```

Inspection shows why raw reference-count ranking is insufficient.

## 16.1 Projectivy-specific narrow helpers

Examples include:

- `ez2` — Projectivy toast/process feedback utility;
- `h03` — drawable/colour/helper functionality used by Projectivy UI;
- `iw4` — broad platform/file/sysfs/accessibility/provider utility collection;
- `fs4` — package/platform utility;
- `q32` — application/global context/state helper.

These are important support utilities, but their Projectivy-facing behaviors have already been exercised by the functional subsystems in Groups 1–10.

## 16.2 Synthetic/compiler dispatchers

Classes such as `em3` and `b5` include generated/synthetic dispatch behavior tying together already-known settings, device and wallpaper paths.

They do not represent independently designed feature subsystems.

## 16.3 Minified library/framework machinery

Several high-affinity classes have many incoming references precisely because they are common Kotlin/AndroidX/base utilities. Examples from the ranked list include generic lazy/function/coroutine/base/activity machinery.

The closure rule is therefore:

```text
high reference count
    ≠
Projectivy-owned feature subsystem
```

A class only blocks closure when its behavior changes a Projectivy feature in a way that is not already accounted for.

No such new core subsystem remained after this pass.

---

# 17. Complete behavioral subsystem graph after Group 11

The Projectivy architecture now closes into the following major areas:

```text
Projectivy 4.72
│
├── Application/bootstrap
│   ├── PTApplication
│   ├── typed preferences/config
│   ├── migrations
│   └── narrow native validation/bootstrap boundary
│
├── HOME shell
│   ├── MainActivity
│   ├── MainFragment
│   ├── Leanback browse/header state
│   └── focus/selection restoration
│
├── Generic home categories/items
│   ├── configurable i5 categories
│   ├── stable item/card models
│   ├── app cards
│   ├── input cards
│   ├── Watch Next/program cards
│   ├── shortcuts
│   ├── Now Playing
│   └── category folders
│
├── Apps
│   ├── LauncherApps/PackageManager discovery
│   ├── TV/mobile de-duplication
│   ├── favourites/hidden/order
│   ├── launch/App Info/uninstall
│   ├── usage sorting
│   └── shortcut host
│
├── Android TV Provider
│   ├── channels
│   ├── preview programs
│   ├── Watch Next
│   └── program details/previews
│
├── Media/notifications
│   ├── NotificationListener
│   ├── notifications panel
│   ├── MediaSessionManager
│   ├── Now Playing row
│   └── playback actions
│
├── Wallpaper/theme engine
│   ├── provider registry
│   ├── external provider Binder API
│   ├── wallpaper events
│   ├── candidate caching
│   ├── image/video/color/drawable/Lottie renderers
│   └── .pltheme packages
│
├── Settings
│   ├── typed setting descriptors
│   ├── Preference/GuidedStep surfaces
│   ├── dynamic settings
│   └── live/reload/deferred update policies
│
├── TV inputs/device adapters
│   ├── TvInputManager/TvView
│   ├── HDMI/AV/Android source model
│   ├── Xiaomi/Amlogic/MediaTek/Formovie/Google adapters
│   ├── display profiles
│   └── optional root/sysfs engineering tools
│
├── HOME/boot/accessibility
│   ├── Android HOME role
│   ├── autostart coordinator
│   ├── AccessibilityService compatibility layer
│   ├── remote gesture engine
│   └── idle coordinator
│
├── Persistence/customization
│   ├── typed SharedPreferences/config
│   ├── migrations
│   ├── .plbackup
│   ├── icon packs/custom artwork
│   ├── shortcuts
│   └── parental LaunchGate
│
└── Optional maintenance utilities
    ├── APK installer
    ├── package freeze/unfreeze
    ├── stock launcher maintenance
    ├── Play self-update block/unblock
    ├── GApps removal
    ├── Web Remote link
    └── diagnostics/about tools
```

This is now sufficiently closed to build an implementation plan without expecting a missing architectural layer to invalidate it later.

---

# 18. What should *not* be carried into Aerial-Rows core

The closure pass is as useful for identifying what **not** to transplant as what to implement.

The Aerial-Rows core should not depend on:

- Projectivy's native `libptutils.so`;
- Projectivy entitlement/purchase code;
- root access;
- stock-launcher freezing;
- GApps removal;
- Play Store self-update modification;
- OEM engineering menus;
- Projectivy's exact encrypted backup framing;
- Projectivy's compiler-generated coroutine/obfuscation structure.

Those are either optional device tools or implementation-specific internals.

---

# 19. Recommended optional maintenance module for Aerial-Rows

If we eventually want feature parity with Projectivy's expert/device utilities, isolate them behind capability interfaces:

```text
DeviceMaintenanceTools
├── PackageInstallerTool
├── PackageFreezeTool          [root optional]
├── StockLauncherTool          [root optional]
├── SystemAppMaintenanceTool   [root optional]
├── VendorEngineeringLauncher
├── DeviceDiagnostics
└── RemoteManagementLink
```

Runtime capability detection should determine whether each feature is shown.

None should be required for ordinary Shield/Android TV operation.

---

# 20. Verification summary

The Group 11 fail-closed verifier passed **33/33** checks covering:

- exact APK SHA;
- 26 recovered named Activity sources;
- category/channel/settings wrapper recovery;
- Xiaomi/Formovie/Amlogic/MediaTek/Android/Fengmi wrapper targets;
- unknown-source/APK installer behavior;
- Web Remote URL behavior;
- package freeze/unfreeze UI + privileged worker link;
- Leanback launcher maintenance;
- Play Store block/unblock helper path;
- GApps uninstall helper path;
- autostart `-2`/`-101` compatibility branches;
- current `-1`/`-100` visible choices;
- absence of current UI choices for `-2`/`-101`;
- `SOURCE_UNKNOWN` sentinel;
- backup 16-byte salt/IV framing;
- runtime-sensitive `s0 + 16` decrypt expression;
- `DisplayProfileManager.rps()` setting `s0 = 0`;
- lack of managed Java caller for `rps()`;
- native `vc`/`vs` exports;
- narrow managed callers of `vs()` / `PTApplication.f()`;
- explicit non-guessing treatment of Group 6 decompiler ambiguities.

---

# 21. Final Group 11 conclusion

Groups 1–10 did not merely produce a feature checklist. Together with this closure pass they now provide an implementation-level map of the launcher architecture, data sources, persistence, settings, UI/focus behavior, Android integrations, vendor adapters, wallpaper/media pipeline, and supporting actions.

The remaining unknowns are not architectural blockers.

The strongest remaining native relationship—between `libptutils`, `DisplayProfileManager.rps()` and the runtime `s0` value—is deliberately recorded as an inference rather than falsely promoted to a proven JNI call chain. It is also unnecessary for Aerial-Rows because we can initialize legitimate repositories explicitly and own our backup format.

Therefore:

> **Projectivy 4.72 is sufficiently closed at the behavioral/architectural level to proceed to the Aerial-Rows implementation architecture and staged build plan.**

## Next group

**Group 12 — Aerial-Rows implementation map and build order**

That group should map the verified Projectivy subsystems onto the current `Aerial-Rows` codebase and decide, file by file and component by component:

- what stays;
- what gets refactored;
- what gets replaced;
- what new interfaces/models/repositories are needed;
- dependency/build order;
- migration path from current Couchy-derived category/app structure;
- where Aerial provider/video background integration belongs;
- which optional Projectivy expert tools should be deferred;
- the first implementation commits to make on the development branch.


---

# SOURCE FILE: `group11_postclosure_audit_v2.md`

SHA-256: `a875f79d450182038e6a0763ac320a870acb016da456a757d1e73b72b79890e1`

---

# Projectivy 4.72 — Group 11 Post-Closure Re-Audit v2

**Audit namespace:** `AUDIT_11_R2`  
**Process contract:** v18  
**Target APK:** Projectivy 4.72 / code 97  
**APK SHA-256:** `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
**Original Group 11 SHA-256:** `4f9c9bd36edba833c20b7964a12aae1182396ebde85e74aceca3da7396c4caca`

## Executive result

The original Group 11 closure remains **architecturally correct**.

Fresh verification found **no hidden second launcher subsystem**, no native implementation of the core home architecture, and no additional vendor/device architecture that invalidates Groups 1–10.

The fresh pass did, however, improve several details that were either intentionally left coarse in the original Group 11 report or only became known in later post-closure audits.

### Final assessment

- Core Group 11 architecture: **correct**
- Missing major subsystem: **none**
- Factual architectural reversals: **none**
- Canonical REA evidence: **22/22 success**
- Closure matrix: **11/11 covered**
- Stale canonical temp files: **0**

## 1. Autostart legacy sentinels can now be stated exactly

The original Group 11 correctly classified `-2` and `-101` as compatibility/internal values rather than current selectable startup targets.

Fresh canonical `dq3.b(Context)` evidence makes that more precise:

```text
-2
 └─ passes the literal persisted source string to the same yb3.n(...) app resolver
    used by ordinary application startup

-101
 └─ has no active launch branch
    └─ falls through to false
```

So the canonical interpretation is:

- `-2` = legacy/application-resolver compatibility behavior;
- `-101` = inactive/failure sentinel;
- neither should become a first-class Aerial-Rows autostart option.

## 2. `DisplayProfileManager.rps()` has one additional runtime reset

Fresh canonical evidence confirms the previous initialization behavior:

```text
load display_profiles_list
→ deserialize lz0 profiles
→ create default profile if empty
→ sort case-insensitively
→ ProjectivyAccessibilityService.s0 = 0
```

and adds another side effect:

```text
vo2.H0 = ""
```

The previous architectural recommendation therefore remains unchanged: Aerial-Rows should explicitly initialize its profile repository/state in normal Kotlin application startup rather than reproducing Projectivy's opaque bootstrap coupling.

Fresh managed xrefs still report **zero managed Java callers** for `DisplayProfileManager.rps()`.

That strengthens—but does not prove—the original conclusion that the exact invocation path crosses an opaque bootstrap/protection/native boundary.

## 3. Backup/runtime `s0` coupling remains bounded, not fully explained

Canonical `s71.b(byte[])` still extracts the salt prefix using:

```text
ProjectivyAccessibilityService.s0 + 16
```

and then treats the next 16 bytes as the AES-CBC IV.

Because `rps()` sets `s0 = 0`, normal initialized runtime strongly implies the effective salt prefix is 16 bytes.

Fresh evidence does **not** reveal a normal managed caller for `rps()`, so the exact bootstrap chain remains deliberately unproven.

This is still non-blocking for Aerial-Rows. We should own an explicit versioned backup format rather than duplicate this coupling.

## 4. JNI/native boundary remains narrow

Fresh direct inspection of the shipped ARM64 `libptutils.so` confirms exports:

```text
Java_com_spocky_projengmenu_PTApplication_vc
Java_com_spocky_projengmenu_PTApplication_vs
```

Fresh canonical managed evidence confirms:

```text
PTApplication.f()
    → ensure/load ptutils
    → PTApplication.vs(...)
```

and fresh managed xrefs still show `PTApplication.f()` has only two managed call sites:

```text
v.o(...)
ya3.o(...)
```

No new evidence moves any of these systems into native code:

- home row/category rendering;
- installed-app discovery;
- TV Provider/Watch Next;
- media/Now Playing;
- wallpaper renderer/provider architecture;
- settings hierarchy;
- TV input repository/launch architecture.

`libptutils.so` should therefore still be treated as a narrow initialization/validation/protection/utility boundary, not a native launcher implementation.

## 5. Guided APK installer remains a separate optional maintenance tool

Fresh `InstallAppsActivity.G(String)` evidence confirms:

- `MANAGE_UNKNOWN_APP_SOURCES` handling for Projectivy itself;
- URL normalization;
- HTTP/FTP/storage/USB-oriented source handling;
- `.gz` handling;
- APK MIME type `application/vnd.android.package-archive`;
- asynchronous install/download helpers.

This remains independent of normal app launch/uninstall/App Info behavior.

Recommended Aerial-Rows placement remains an optional package-maintenance module, not `AppRepository`.

## 6. Root package maintenance is now mapped more precisely

The original report correctly identified freeze/unfreeze and stock-launcher maintenance as root/privileged utilities.

Fresh canonical `kx3` evidence closes the exact command pairs.

### Disable/remove

`kx3.j(...)` chooses between:

```text
pm disable-user --user 0 [PACKAGE]
```

and:

```text
pm uninstall --user 0 [PACKAGE]
```

### Reverse/reinstall

`kx3.g(...)` chooses between:

```text
pm enable [PACKAGE]
```

and:

```text
cmd package install-existing [PACKAGE]
```

The `CleanupActivity → q90 → p90` path uses these helpers for freeze/unfreeze operations.

`RemoveLauncherActivity` remains a separate expert maintenance flow and must not be confused with legitimate Android HOME-role/default-launcher setup.

## 7. Google Play self-update blocking is now fully distinguished from the automatic Play workaround

The original Group 11 intentionally stopped at the privilege boundary for `BlockGooglePlaySelfUpdateActivity`.

Fresh `tx.i0()` + canonical `sx.h()` evidence now closes the low-level behavior.

### Manual block

The privileged helper runs:

```text
mkdir /data/app/com.android.vending-1 ;
touch /data/app/com.android.vending-1/base.apk &&
chattr +i /data/app/com.android.vending-1/base.apk
```

### Manual unblock

It runs:

```text
chattr -i /data/app/com.android.vending-1/base.apk
```

### Important distinction

This is **not** the same as the later Group 9 v3 foreground compatibility workaround.

The automatic Play Store foreground path uses:

```text
setprop sys.cts.running 1
```

and later resets it to `0` through the same privileged helper family.

So Projectivy contains two distinct privileged Play-related behaviors:

```text
Manual self-update blocker
    → immutable dummy Play base.apk path

Automatic foreground compatibility workaround
    → sys.cts.running property toggle
```

Neither belongs in ordinary Aerial-Rows launcher core.

## 8. Xiaomi updater foreground compatibility belongs in the same optional vendor layer

Fresh `nm3.i(...)` evidence confirms the foreground observer also tracks:

```text
com.xiaomi.mitv.upgrade
```

Entering that foreground package sets internal state; leaving schedules a delayed compatibility helper.

This behavior was discovered after the original Group 11 closure in Group 9 v3.

It does not create another subsystem. It strengthens the recommended architecture:

```text
VendorCompatibilityAdapters
├── XiaomiStartupPreconditioner
├── PlayStoreCompatibilityWorkaround
├── XiaomiUpdaterCompatibilityWorkaround
└── other device-specific expert helpers
```

with all of them optional and separated from generic Accessibility/HOME behavior.

## 9. GApps removal now has an exact helper boundary

The original Group 11 correctly classified `GappsUninstallActivity` as an optional privileged maintenance utility.

Fresh `kq1.i0()` + `sx.h()` evidence closes its execution target:

```text
/system/bin/gapps-uninstall.sh
```

executed through `kx3`'s privileged shell path.

This is not launcher architecture and should not be recreated in Aerial-Rows core.

## 10. Web Remote classification remains correct

Fresh wrapper evidence plus discovery of the `gt` helper continues to support:

```text
local IP
   ↓
http://mireco.hopto.org/#<local-ip>
```

There is still no evidence of a local Projectivy HTTP server behind `WebRemoteActivity`.

If Aerial-Rows ever gets remote administration, it should be independently designed.

## 11. Vendor settings/display adapters remain wrappers and capability backends

Fresh evidence for the named MiTV/FengOS wrappers plus `b5.a()` confirms the earlier architecture.

The display backend factory probes a bounded candidate set including:

- Amlogic/Droidlogic `SystemControlManager`;
- Amlogic `TvControlManager`;
- Xiaomi picture backend.

The best available backend is selected by capability score.

No separate hidden vendor-control architecture appeared.

Normal Nvidia Shield / standard Android TV operation should remain independent of those classes.

## 12. High-affinity obfuscated helpers remain support machinery, not hidden feature layers

Fresh canonical class evidence for representative helpers such as:

```text
iw4
em3
b5
sx
nm3
kx3
q90
p90
```

supports the original Group 11 classification:

- platform/file/provider/accessibility helpers;
- synthetic/compiler dispatchers;
- vendor/privileged workers;
- guided-screen delegates;
- shared utility machinery.

High reference count still does not imply a missing Projectivy-owned subsystem.

## 13. Updated optional-maintenance architecture for Aerial-Rows

The earlier `DeviceMaintenanceTools` recommendation should now be split more explicitly:

```text
OptionalExpertTools
│
├── PackageMaintenance
│   ├── APK installer
│   ├── package disable/enable
│   ├── package uninstall/install-existing
│   ├── stock-launcher maintenance
│   └── GApps removal [do not include by default]
│
├── VendorCompatibility
│   ├── Xiaomi startup preconditioning
│   ├── Play Store foreground workaround
│   ├── Xiaomi updater workaround
│   └── vendor engineering/settings launchers
│
├── Diagnostics
│   ├── device/build/display information
│   ├── TV-input compatibility controls
│   └── optional root/sysfs diagnostics
│
└── RemoteManagement
    └── independently designed if ever needed
```

None of these should be required by:

```text
HomeRepository
AppRepository
TvProgramRepository
MediaRepository
WallpaperRepository
InputRepository
SettingsRepository
```

## 14. Updated closure assessment

The post-closure pass did **not** discover:

- a second home renderer;
- a second app repository;
- a second TV-program/channel engine;
- a hidden native wallpaper engine;
- a second input architecture;
- a second persistence/settings engine;
- a local Web Remote server;
- a native implementation of the core launcher.

The remaining opaque relationship is still the bootstrap path that invokes `DisplayProfileManager.rps()` / initializes `s0`.

That ambiguity remains explicitly bounded and is unnecessary to reproduce.

## 15. Verification and closure status

Canonical REA evidence:

```text
22 / 22 success
errors  0
invalid 0
```

The v18 closure matrix is fully satisfied:

```text
entry points                 PASS
base classes / inheritance   PASS
all DEX files                PASS
obfuscated collaborators     PASS
models / data flow           PASS
persistence                  PASS
resources / XML              PASS
Android framework APIs       PASS
callbacks / observers        PASS
launch actions / lifecycle   PASS
native / JNI                 PASS
```

One canonical probe was interrupted by the outer command limit. The v18 write-ahead batch state recorded the exact in-progress item, recovered it on the next run, and no successful probes were repeated. Its orphan atomic temp residue was removed before synthesis.

## Final conclusion

The original Group 11 conclusion remains valid:

> **Projectivy 4.72 is sufficiently closed at the behavioral and architectural level to proceed with the Aerial-Rows implementation architecture without expecting a hidden core subsystem to invalidate the plan.**

The post-closure pass improves precision around legacy startup values, profile bootstrap side effects, privileged package commands, Play/GApps maintenance and later Xiaomi/Play compatibility helpers, but does not change the core reconstruction architecture.

Group 12 can therefore remain the implementation-map/build-order layer, updated with these refined optional expert-tool boundaries.


---

# SOURCE FILE: `group12.md`

SHA-256: `6f066515287480a1666edd4a73b49be233d57592d267049a78dc60561f8bf8b6`

---

# Group 12 — Aerial-Rows implementation map

Status: **COMPLETE + VERIFIED**  
Projectivy reference: Groups 1–11 (Projectivy 4.72 / versionCode 97)  
Aerial-Rows implementation base: `aerialrows-development`  
Pinned implementation commit: `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Main commit at analysis time: `d3dcd70d3dd3b261108d25d72569671aee23b2d6`  
Development delta: **36 commits ahead of main, 0 behind**

## 1. Purpose

Groups 1–11 established the Projectivy architecture and behavior at implementation level. Group 12 maps that knowledge to the current Aerial-Rows codebase and defines the order in which Aerial-Rows should be turned from a Couchy-derived prototype with Projectivy-like additions into a coherent Projectivy-class launcher with Aerials as a first-class wallpaper/background source.

This group answers four questions:

1. What current Aerial-Rows code is good and should be retained?
2. What current code should be refactored rather than extended further?
3. What Projectivy subsystems are still absent?
4. In what order should the work be implemented so that every intermediate commit remains buildable and usable?

This is an implementation plan, not another reverse-engineering pass.

---

## 2. Reliability and performance changes made for Group 12

### 2.1 Pinned remote source snapshot

The repository mapping is pinned to one exact development commit rather than a moving branch name:

`aerialrows-development @ 3c1cfd56234f1d386f4553dd6c69523670500d70`

The current branch head was rechecked before verification and remained identical. Therefore no repository files needed to be reread after the interrupted attempt.

Important source files are also pinned by Git blob SHA. Future analysis can compare blob hashes and refetch only changed files.

### 2.2 Incremental remote verification

For connector-backed source analysis, the workflow is now:

```text
pinned commit + important blob SHAs
          ↓
resume after interruption
          ↓
check current branch head
          ↓
head unchanged? ── yes ─→ reuse snapshot
          │
          no
          ↓
compare blob SHAs
          ↓
refetch only changed files
```

This replaces repeated repository rereads.

### 2.3 Durable stages

Group 12 now follows the same durable stage model as the later reverse-engineering groups:

```text
discovery     → checkpoint
verification  → checkpoint
synthesis     → checkpoint
report        → checkpoint
```

The earlier interruption occurred after remote discovery but before a report was persisted. That gap has been removed.

### 2.4 Verification result

Group 12 verification passed **19/19 assertions**, including:

- pinned development commit;
- branch relationship to main;
- semantic database integrity;
- hashed completion of Groups 1–11;
- source-rewriting risk captured;
- development branch selected as implementation base;
- partial TV implementation captured;
- settings architecture risk captured;
- current toolchain captured.

---

## 3. Executive conclusion

The correct path is **not** to restart from main and not to discard Aerial-Rows.

The correct base is the existing `aerialrows-development` branch because it already contains useful work that corresponds to several Projectivy subsystems:

- Sliding Rows;
- a left navigation rail;
- row ordering/editing;
- Android TV Watch Next/preview-channel prototypes;
- TV program cards;
- expanded launcher settings;
- multi-source built-in Aerials;
- configurable row/dock panels;
- app artwork/cache optimizations.

However, the branch has reached the point where continuing to add features directly into the existing large Couchy files will create more architectural debt than progress.

The immediate goal must therefore change from:

```text
add another Projectivy feature
```

to:

```text
make the existing Aerial-Rows additions first-class source
        ↓
introduce common launcher models/services
        ↓
migrate existing features onto them
        ↓
add remaining Projectivy subsystems
```

The most urgent technical problem is **build-time source rewriting** in `app/build.gradle.kts`. It patches `SettingsSheet.kt` and `LauncherScreen.kt` before compilation. The checked-in Kotlin files therefore do not fully describe what is compiled.

That must be removed before the large reconstruction begins.

---

## 4. Current source baseline

At the pinned development commit the project uses approximately:

```text
AGP          8.5.2
Kotlin       2.0.20
Gradle       8.9
Java target  17
Kotlin JVM   17
compileSdk   34
targetSdk    34
minSdk       21
namespace    com.conreo.couchytv
application com.conreo.couchytv
```

The previously requested AGP 9.4.1 / JVM 21 upgrade should be performed, but only as an isolated infrastructure migration after the source-rewriting build hack is removed.

Do not combine:

- toolchain upgrade;
- package/application-ID rename;
- config migration;
- home architecture rewrite.

Each should remain independently reversible.

---

## 5. Source-by-source assessment

### 5.1 `MainActivity.kt`

**Keep, then slim down.**

Useful current responsibilities:

- HOME/LAUNCHER activity;
- Compose host;
- locale context wrapping;
- basic TV-listing permission request;
- package-change trigger;
- Back suppression.

Long-term target:

```text
MainActivity
 ├─ app lifecycle bridge
 ├─ HOME task shell
 └─ Compose LauncherRoute
```

Move out:

- package observation;
- boot/autostart;
- HOME-role setup;
- notification-listener status;
- accessibility compatibility logic;
- input/device logic.

These belong to repositories/coordinators rather than the Activity.

---

### 5.2 `LauncherScreen.kt`

**Keep useful Compose rendering; aggressively refactor orchestration.**

The file currently coordinates too many unrelated concerns:

- config loading;
- app scanning;
- setup wizard;
- language recreation;
- wallpaper/video handling;
- status/clock/date;
- new-app assignment;
- categories;
- carousel/grid/dock;
- overlays/context menu;
- settings;
- focus;
- launch animations.

Target decomposition:

```text
LauncherRoute
 ├─ LauncherState / LauncherViewModel
 ├─ WallpaperHost
 ├─ StatusHost
 ├─ HomeSurface
 │   ├─ SlidingRowsSurface
 │   ├─ CarouselSurface
 │   ├─ GridSurface
 │   └─ DockSurface
 └─ OverlayHost
     ├─ CardActions
     ├─ ProgramDetails
     ├─ Settings
     └─ LaunchGate UI
```

Do not rewrite all renderers at once. Extract state and orchestration while preserving the existing working Compose visuals.

---

### 5.3 `data/Config.kt`

**Keep DataStore; replace the monolithic config schema.**

Current state is one `LauncherConfig` JSON blob containing apps, rows, TV content, wallpaper, appearance, language, status and layout.

Introduce a schema version and migrate toward domains:

```text
LauncherConfigEnvelope
 ├─ schemaVersion
 ├─ HomeConfig
 ├─ AppConfig
 ├─ TvContentConfig
 ├─ WallpaperConfig
 ├─ AppearanceConfig
 ├─ StatusConfig
 ├─ SystemConfig
 ├─ AccessibilityConfig
 ├─ ParentalPolicyConfig
 └─ BackupConfig
```

The first migration must be able to decode the current `LauncherConfig` so existing development installs do not lose configuration.

Do not store parental secrets in the ordinary config object.

---

### 5.4 `data/AppRepository.kt`

**Keep the performance/artwork work; refactor identity, observation and state.**

Existing strengths:

- LEANBACK + normal LAUNCHER queries;
- TV launcher precedence;
- parallel artwork processing;
- density-aware banner rasterization;
- persistent bitmap cache;
- object reuse for unchanged packages;
- precomputed tile/glow colours;
- category grouping/order.

Needed changes:

1. retain actual component/activity identity, not only package;
2. split TV and mobile discovery explicitly;
3. replace coarse Activity BroadcastReceiver rescans with `LauncherApps.Callback` plus debounce;
4. add launch count / last-launch metadata;
5. separate app customization from discovery;
6. expose app cards through the generic `HomeCard` model;
7. stop making app-category membership the global home model.

Target:

```text
AppRepository
 ├─ discoverTvLaunchables()
 ├─ discoverMobileLaunchables()
 ├─ deduplicate()
 ├─ observePackages()
 └─ artworkResolver
```

---

### 5.5 `ui/SlidingRows.kt`

**Keep the UX prototype. Promote its private model into the core architecture.**

This is one of the most valuable pieces already implemented.

It already demonstrates:

- vertical heterogeneous rows;
- Watch Next;
- preview channels;
- app rows;
- saved row order;
- Left-revealed rail;
- row move actions;
- quick row editing.

The current problem is that `HomeRowModel` is private UI state.

Move to core models:

```text
sealed interface HomeRow
 ├─ Apps
 ├─ WatchNext
 ├─ TvChannel
 ├─ NowPlaying
 ├─ Inputs
 ├─ Shortcuts
 └─ Folder
```

Add explicit stable row/card focus state:

```text
selectedRowId
selectedCardKeyByRow
railState
```

Rail state should support:

- shown;
- hidden but revealable with Left;
- disabled.

Returning from the rail should restore the previously selected card, not automatically the first card.

---

### 5.6 `ui/RowQuickEditor.kt`

**Keep concept; generalize around row capabilities.**

Current implementation handles:

- app membership;
- app ordering;
- preview-channel visibility;
- per-channel card count;
- Watch Next enable/count.

Replace row-type conditionals over time with:

```text
RowActionResolver
 ├─ EditContents
 ├─ ReorderCards
 ├─ Visibility
 ├─ Rename
 ├─ ChangeIcon
 ├─ ShowCardTitles
 ├─ CardScale
 ├─ LayoutMode
 ├─ ItemsPerLine
 ├─ MaximumRows
 ├─ Margins
 └─ ProviderLimit
```

Only applicable capabilities are shown for each row source.

---

### 5.7 `data/TvContentRepository.kt`

**Useful partial implementation. Expand rather than replace.**

Keep:

- off-main-thread queries;
- Watch Next query;
- preview-channel query;
- immutable models;
- hidden channel settings;
- per-channel card count.

Required Group 4 parity work:

1. replace `packageName:channelId` persistence with a stable provider/channel identity where possible;
2. numeric channel ID remains an ephemeral runtime lookup ID;
3. management discovery must include channels that are not yet browsable;
4. support `TvContract.requestChannelBrowsable()`;
5. Preview Programs sort by `weight DESC`;
6. Watch Next sort by `last_engagement_time_utc_millis DESC`;
7. add per-provider Watch Next limits;
8. add excluded Watch Next providers;
9. retain richer metadata:
   - preview video URI;
   - provider logo/badge;
   - aspect ratios;
   - type;
   - genre;
   - author;
   - release date;
   - season/episode;
10. lazy long-description query for details;
11. ContentObserver refresh with debounce;
12. resume refresh as safety net;
13. cache + stable row reconciliation.

---

### 5.8 `ui/TvProgramCard.kt`

**Keep visual base; evolve into generic ProgramCard.**

Keep:

- 16:9 presentation;
- artwork;
- progress bar;
- focus scale/glow.

Add:

- provider badge;
- title/description hover data;
- preview-video URI and focus dwell controller;
- details action;
- common image loader/cache;
- stable `CardIdentity`;
- launch through `LaunchGate`/`LaunchDispatcher`.

Do not maintain feature-specific direct HTTP image loaders long-term. Centralize image/network/cache behavior.

---

### 5.9 `ui/AppCard.kt`, `ui/Menus.kt`, `Actions.kt`

**Keep current visuals and useful long-press handling; replace the action architecture.**

Existing app menu:

- Open;
- Move;
- Hide/show;
- App Info;
- Close/background kill;
- Uninstall.

Move toward:

```text
CardActionResolver(card)
          ↓
List<CardAction>
```

with actions such as:

- Launch;
- App Info;
- Uninstall;
- Hide;
- Favourite/category membership;
- Reorder;
- Move;
- Launch on boot;
- parental protection;
- rename;
- icon/artwork override;
- crop;
- Android shortcuts;
- media controls for Now Playing;
- row/program details where appropriate.

`Actions.kt` should be replaced gradually by typed dispatchers rather than becoming a global collection of unrelated Intents.

---

### 5.10 Aerials / wallpapers

`BuiltinAerials.kt` is **keep**.

Its catalogue and multi-source logic are useful.

Replace the current wallpaper booleans:

```text
useCustomWallpaper
useVideoWallpaper
useBuiltinAerials
videoUri
wallpaper
```

with provider selection:

```text
WallpaperProviderConfig
 ├─ providerId
 └─ provider-specific configuration
```

Target providers:

```text
Preset/Default
User image/video
Folder
Aerial
Selected TV program
Random TV program
Now Playing
Theme
External provider (later if desired)
```

Renderer registry:

```text
ImageRenderer
VideoRenderer (Media3)
ColorRenderer
DrawableRenderer
LottieRenderer (optional/later)
```

Aerials should feed VIDEO wallpaper candidates into the same video renderer.

There should not be one video player for wallpaper and another independent player for the Aerial idle/screensaver presentation.

Use one Media3 engine and change presentation state when the launcher enters/leaves idle mode.

---

### 5.11 `SettingsSheet.kt` and `AerialSettingsExtras.kt`

**Replace structure; retain useful individual controls and visual styling.**

Current risks:

- very large settings file;
- local route enum;
- settings features injected by Gradle source rewriting;
- hard-coded English text in the Aerial additions;
- values mostly cycled with Left/Right rather than real reusable choice/dropdown controls;
- screen-entry focus rather than stable per-setting focus retention.

Target:

```text
SettingsNavigator
SettingsRoute
SettingsFocusStore

SettingRow
 ├─ Action
 ├─ Switch
 ├─ Choice
 ├─ Dropdown
 ├─ Stepper
 └─ Navigation
```

Each setting gets a stable `SettingId`.

Required behavior:

```text
focus Prefer HDR
      ↓
toggle/change value
      ↓
state updates in place
      ↓
focus remains on Prefer HDR
```

Returning from a subpage should restore the item that opened it.

Settings should be migrated one page at a time rather than rewritten in one huge change.

---

### 5.12 `PanelStyle.kt`, status/network/theme helpers

**Keep.**

These are Aerial-Rows-specific presentation features and do not conflict with the reconstructed Projectivy architecture.

Move them into cleaner packages as part of structural refactors but do not rewrite them unnecessarily.

---

### 5.13 Setup wizard

**Keep and extend later.**

Eventually include optional/setup checks for:

- default HOME launcher;
- TV listings capability;
- notification-listener permission if Now Playing is enabled;
- Accessibility compatibility features if the user wants them;
- restore from backup.

Accessibility must remain optional.

---

## 6. Projectivy group → Aerial-Rows map

| Projectivy research group | Current Aerial-Rows state | Decision |
|---|---|---|
| 1. Components/resources | HOME shell exists; manifest minimal | Keep shell, expand capability-by-capability |
| 2. Home rows/focus | Sliding Rows prototype exists | Promote models, add stable focus/rail state |
| 3. Apps/actions | Strong app/artwork implementation | Keep cache/render work; refactor identity/actions/observation |
| 4. TV Provider | Partial implementation exists | Expand to full provider behavior |
| 5. Notifications/Now Playing | Missing | Add after common row/card/launch models |
| 6. Wallpaper/Aerial | Strong Aerial/video prototype | Convert to provider + renderer architecture |
| 7. Settings | Functional but monolithic/patch-driven | Replace structure incrementally |
| 8. Inputs/vendor | Missing | Add standard input layer, vendor adapters optional |
| 9. HOME/boot/accessibility/remote/idle | HOME only | Add coordinators; Accessibility optional |
| 10. Backup/icons/shortcuts/parental | Minimal backup only | Add versioned systems after common identity/launch gate |
| 11. Optional utilities/closure | Mostly absent | Defer until core launcher is stable |

---

## 7. Core architecture to introduce

### 7.1 Card identity

```kotlin
data class CardIdentity(
    val stableKey: String,
    val sourcePackage: String? = null,
)
```

Stable identity must not be tied to current screen index.

Examples:

```text
app:<package>/<activity>
program:preview:<provider identity>:<program id>
program:watchnext:<provider>:<program id>
input:<stable input identity>
shortcut:<package>:<shortcut id>
internal:<action id>
```

---

### 7.2 Launch target

```kotlin
sealed interface LaunchTarget {
    data class Component(/* component/user */) : LaunchTarget
    data class IntentUri(/* uri + package */) : LaunchTarget
    data class Shortcut(/* package/id/user */) : LaunchTarget
    data class TvInput(/* stable input */) : LaunchTarget
    data class InternalAction(/* action id */) : LaunchTarget
}
```

---

### 7.3 Home cards

```kotlin
sealed interface HomeCard {
    val identity: CardIdentity
    val title: String
    val launchTarget: LaunchTarget?

    data class App(/* ... */) : HomeCard
    data class Program(/* ... */) : HomeCard
    data class NowPlaying(/* ... */) : HomeCard
    data class Input(/* ... */) : HomeCard
    data class Shortcut(/* ... */) : HomeCard
    data class Folder(/* ... */) : HomeCard
    data class ConfigureRow(/* ... */) : HomeCard
}
```

---

### 7.4 Home rows

```kotlin
sealed interface HomeRow {
    val id: String
    val title: String
    val cards: List<HomeCard>
    val presentation: RowPresentation
}
```

`RowPresentation` should eventually cover the Group 2 configuration model:

- visibility;
- header/title/icon;
- card labels;
- scale/aspect;
- single/multi row;
- items per line;
- maximum rows;
- start/end/top margins;
- row background;
- sliding/reset behavior.

---

### 7.5 Generic card actions

```kotlin
sealed interface CardAction {
    data object Launch : CardAction
    data object AppInfo : CardAction
    data object Uninstall : CardAction
    data object Hide : CardAction
    data object Reorder : CardAction
    data object LaunchOnBoot : CardAction
    data object ParentalControl : CardAction
    // feature-specific actions continue here
}
```

---

### 7.6 Central launch gate

Every launch path must converge:

```text
App card
Program deep link
Shortcut
Now Playing
TV input
Remote mapping
Autostart
       ↓
LaunchGate
       ↓
policy / parental authorization
       ↓
LaunchDispatcher
```

This prevents parental-control or future policy logic from being scattered through UI handlers.

---

## 8. Proposed source layout

Keep one Gradle app module initially.

Use package boundaries before considering module extraction:

```text
com.conreo.couchytv/
  core/
    model/
    config/
    launch/
    focus/
    image/
    system/

  feature/
    home/
    apps/
    tvcontent/
    media/
    wallpaper/
    aerials/
    settings/
    inputs/
    shortcuts/
    icons/
    parental/
    backup/
    accessibility/
    boot/
    devicetools/

  ui/
    components/
    theme/
```

Do not introduce many Gradle modules before the architecture stabilizes; module churn would slow the reconstruction without improving behavior.

---

## 9. Manifest strategy

The current manifest declares `QUERY_ALL_PACKAGES`.

The reconstructed Projectivy architecture does not require blanket package visibility for the launcher behavior we need.

Long-term target:

- remove `QUERY_ALL_PACKAGES` where possible;
- declare `<queries>` for relevant launcher intents and optional integrations;
- add permissions/services/receivers only as each feature lands.

Capability-driven manifest additions:

```text
TV Provider          → READ_TV_LISTINGS
Now Playing          → NotificationListenerService
boot/autostart       → RECEIVE_BOOT_COMPLETED
Accessibility mode   → BIND_ACCESSIBILITY_SERVICE service
shortcuts            → LauncherApps host flows
TV inputs            → standard platform APIs/vendor queries
wallpaper plugins    → provider service query if implemented
```

Do not declare future privileged capabilities before their features are actually implemented.

---

## 10. Implementation order

The following ordering is designed to keep Aerial-Rows runnable after every phase.

### PHASE 0 — Reproducible baseline

#### 0.1 Pin current development baseline

Keep `3c1cfd56234f1d386f4553dd6c69523670500d70` as the known recovery point.

#### 0.2 Run CI on development/integration pushes

Current workflow covers PRs/tags but ordinary development pushes should also run:

```text
assembleDebug
assembleRelease
lintRelease
```

#### 0.3 Remove build-time Kotlin source rewriting

Permanently apply the intended Sliding Rows/Channels/panel/Aerial changes to source.

Delete the Gradle `applyAerialRowsSourcePatches` mutation step.

Add a CI cleanliness assertion so a build cannot modify tracked source files.

#### 0.4 Toolchain upgrade

After source is reproducible, make the requested AGP/JVM upgrade in its own commit.

Target direction:

- AGP 9.4.1 as requested;
- JVM/JDK 21 as requested;
- compatible Gradle/Kotlin versions required by that AGP;
- SDK level upgrade separately verified.

Do not change launcher architecture in that commit.

**Gate:** debug + release + lint pass; build leaves source unchanged; launcher opens as HOME.

---

### PHASE 1 — Versioned config and core models

1. add config schema version;
2. add migration registry;
3. decode/migrate current `LauncherConfig`;
4. add domain config classes;
5. add `CardIdentity`;
6. add `LaunchTarget`;
7. add `HomeCard`;
8. add `HomeRow`;
9. add `RowPresentation`.

Use adapters so the existing UI continues to render while the new models are introduced.

**Gate:** launcher appearance/launch behavior remains unchanged using the new model adapters.

---

### PHASE 2 — Home registry and focus

1. move `HomeRowModel` out of `SlidingRows.kt`;
2. create `HomeRowRegistry`/`HomeRepository`;
3. create stable row/card focus store;
4. model rail states;
5. preserve per-row selected card;
6. generic row capability metadata;
7. migrate row reordering to stable IDs.

**Gate:** Left reveals rail only at row boundary; returning restores the selected card; reordering does not lose focus/identity.

---

### PHASE 3 — App subsystem and generic actions

1. add component-level app identity;
2. split TV/mobile discovery;
3. add `LauncherApps.Callback`;
4. debounce package updates (~500 ms);
5. targeted cache invalidation;
6. add usage metadata;
7. add sorting modes;
8. add app override/customization layer;
9. introduce `CardActionResolver`;
10. introduce `LaunchDispatcher`;
11. route current app launches through the dispatcher.

**Gate:** no duplicate TV/mobile app cards; launch/App Info/uninstall/hide/reorder still work.

---

### PHASE 4 — Complete TV Provider implementation

Extend `TvContentRepository` rather than replacing it.

1. stable channel keys;
2. browsable management;
3. provider grouping;
4. preview weight sorting;
5. Watch Next engagement sorting;
6. per-channel limits;
7. per-provider Watch Next limits/exclusions;
8. complete metadata;
9. details/long description;
10. preview video;
11. provider badge;
12. ContentObserver refresh/debounce;
13. cache/reconciliation;
14. resume fallback refresh.

**Gate:** provider rows refresh without app restart and saved row order survives channel-ID recreation.

---

### PHASE 5 — Wallpaper engine and Aerial provider

1. define provider SPI;
2. define renderer SPI;
3. move preset/user media to providers;
4. convert BuiltinAerials into `AerialProvider`;
5. use one Media3 video renderer;
6. add provider cache/TTL;
7. add wallpaper events;
8. add launcher-idle presentation mode;
9. then add TV-program and Now Playing providers.

**Gate:** switching wallpaper source does not recreate the launcher; Aerial background and immersive idle state share one playback engine.

---

### PHASE 6 — Settings architecture

1. create route stack;
2. add stable `SettingId`;
3. add `SettingsFocusStore`;
4. reusable Switch row;
5. reusable Choice row;
6. actual dropdown/choice dialog;
7. reusable Stepper;
8. navigation/action rows;
9. migrate one existing settings page at a time;
10. eliminate hard-coded feature strings.

**Gate:** changing a setting keeps focus on the same setting; subpage return restores prior focus.

---

### PHASE 7 — Notifications and Now Playing

1. NotificationListenerService;
2. notification repository/panel state;
3. MediaSession repository;
4. session filtering/deduplication;
5. Now Playing cards/row;
6. supported transport controls;
7. primary-session global state;
8. expose media-playing state to idle coordinator.

**Gate:** notification permission absence degrades gracefully; multiple valid media sessions can be represented.

---

### PHASE 8 — TV inputs and vendor adapters

Implement standard Android TV behavior before OEM-specific code.

1. `TvInputManager` repository;
2. source models;
3. input row/cards;
4. standard input launch strategy;
5. optional internal `TvView` strategy where useful;
6. Xiaomi adapter;
7. Amlogic/Droidlogic adapter;
8. MediaTek adapter;
9. Formovie/FengOS adapter;
10. vendor settings/engineering Intent specs.

Privileged sysfs/root features remain separate.

**Gate:** Nvidia Shield/standard Android TV input path works without loading vendor code.

---

### PHASE 9 — HOME role, startup, accessibility, remotes and idle

1. `HomeRoleManager`;
2. current-HOME detection;
3. `StartupCoordinator`;
4. startup target/delay/duplicate guard;
5. standby resume trigger;
6. `RemoteGestureEngine`;
7. short/double/long press actions;
8. optional Accessibility service;
9. foreground-window tracking where required;
10. `LauncherIdleCoordinator`;
11. connect idle state to Aerial/wallpaper presentation.

**Gate:** normal launcher use never requires Accessibility; global interception is opt-in.

---

### PHASE 10 — Backups, icons, shortcuts and parental controls

#### Backups

Create a versioned format rather than exporting the raw DataStore object.

Suggested shape:

```text
backup.zip
 ├─ manifest.json
 ├─ config.json
 ├─ overrides.json
 └─ assets/<sha256>.*
```

Use authenticated encryption if encrypted backups are offered.

#### Icons

Add:

- custom title/artwork overrides;
- icon-pack discovery;
- `appfilter.xml` parsing;
- versioned parsed cache.

#### Shortcuts

Add:

- shortcut host permission;
- ShortcutQuery;
- PinItemRequest handling;
- `LauncherApps.startShortcut()`.

#### Parental controls

Add central `LaunchGate` and secure secret verifier.

Every launch path must pass through it.

**Gate:** backup round-trip tests pass; custom assets survive restore; all launcher-originated launches pass the same gate.

---

### PHASE 11 — Optional device tools and polish

Only after core launcher behavior is stable:

- calibration tools;
- display profiles;
- engineering-menu shortcuts;
- optional root/sysfs tools;
- APK installer/freeze utilities if desired;
- external wallpaper-provider compatibility if desired;
- performance profiling;
- release migration/package-name decision.

These must never become prerequisites for ordinary Nvidia Shield / Android TV operation.

---

## 11. Suggested commit boundaries

Do not implement each phase as one giant commit.

A good pattern is:

```text
infra: commit injected Aerial-Rows source changes
infra: remove preBuild source patcher
ci: build development pushes and assert clean source
build: upgrade AGP/JDK toolchain

config: add versioned config envelope and migration
core: add launch/card/row identities
home: migrate Sliding Rows to core row models
home: add stable focus store and rail states

apps: add component identity and LauncherApps observation
apps: add generic card action resolver
launch: centralize launch dispatcher

... then continue subsystem-by-subsystem
```

Each commit should compile independently where practical.

---

## 12. Branch strategy

Do not modify `main` directly.

Recommended structure:

```text
main
  ↑ release/merge only

aerialrows-development
  ↑ integration branch

feature/<small-slice>
  ├─ source-pure-build
  ├─ core-models
  ├─ home-registry
  ├─ app-actions
  ├─ tvprovider
  ├─ wallpaper-engine
  └─ ...
```

The current development branch should remain the integration base because it contains the existing 36 commits of Aerial-Rows work.

---

## 13. Testing and reliability gates

### Every PR/slice

- `assembleDebug`;
- `assembleRelease`;
- `lintRelease`;
- source tree remains unchanged after build;
- config migration test;
- unit tests for new pure model/repository logic.

### HOME smoke test

- launcher starts;
- HOME returns to the existing task;
- Back cannot accidentally close HOME;
- package install/remove refreshes apps;
- D-pad can reach every major UI surface;
- focus returns correctly after dialogs/settings.

### Focus regression tests

Highest priority TV-launcher regressions:

- toggle setting → focus remains on that row;
- submenu return → focus restored;
- long press → release does not accidentally launch card;
- row reorder → same row/card remains selected;
- channel disappearance → focus moves to a valid stable fallback;
- rail reveal/close → previous content focus restored.

### Performance policy

Preserve existing Aerial-Rows performance strengths:

- stable immutable models;
- cache decoded artwork;
- no bitmap work during composition;
- bounded concurrency;
- stable Lazy list keys;
- targeted provider/package refresh;
- no perpetual animations without visible purpose;
- one video playback engine for background/Aerial use;
- do not rebuild the entire home model for an unrelated preference change.

---

## 14. Migration and compatibility policy

### Existing Aerial-Rows/Couchy config

Never simply replace the current DataStore schema.

Migration should:

1. decode current `LauncherConfig`;
2. normalize it;
3. map it to the new domain configs;
4. preserve category IDs/order/membership;
5. preserve hidden apps;
6. preserve wallpaper/Aerial selection;
7. preserve TV channel settings where identity can be mapped;
8. write new schema only after validation;
9. retain a last-good automatic backup.

### Package/application ID

Keep `com.conreo.couchytv` during the architecture work unless there is a deliberate migration plan.

Renaming application ID affects:

- Android HOME selection;
- user data path;
- update compatibility;
- permissions;
- notification-listener authorization;
- accessibility authorization;
- pinned shortcuts;
- backup expectations.

Treat branding/application-ID migration as a release project, not a cleanup step.

---

## 15. Features already substantially started

Do **not** reimplement these from zero:

- Compose TV shell;
- app scanner/artwork cache;
- app cards;
- long-press handling;
- app info/uninstall;
- category ordering/membership;
- Sliding Rows prototype;
- left rail prototype;
- row order/editing;
- Watch Next prototype;
- preview-channel prototype;
- TV program cards;
- video wallpaper;
- built-in Aerial catalogue;
- multi-source Aerial selection;
- status/network infrastructure;
- panel appearance;
- UI scale/card size/spacing/corners;
- setup wizard.

The task is to **rebase those capabilities on the new common architecture**.

---

## 16. Major missing subsystems

These still need to be added after the foundations are ready:

- generic card/action architecture;
- generic row registry/model;
- focus persistence by stable identity;
- complete TV Provider semantics;
- program details/preview video;
- NotificationListener/notification panel;
- media sessions / Now Playing;
- wallpaper provider engine;
- selected-program/Now Playing wallpaper providers;
- icon packs/custom card overrides;
- Android shortcuts;
- standard TV input repository;
- vendor input adapters;
- HomeRole setup;
- boot/autostart;
- remote gesture engine;
- optional Accessibility compatibility service;
- launcher idle coordinator;
- parental LaunchGate;
- versioned backup engine;
- config migrations;
- optional device tools/calibration.

---

## 17. Deliberate non-goals for the first implementation waves

Do not prioritize these while the core architecture is being rebuilt:

- root package freezing;
- GApps removal;
- Google Play self-update blocking;
- stock-launcher root modification;
- web-remote utility;
- opaque Projectivy native/protection behavior;
- premium/licensing bypass behavior;
- exact reproduction of compiler-generated Leanback/coroutine internals.

They are not prerequisites for Projectivy-class launcher functionality.

---

## 18. Priority-zero implementation decision

Before another feature is added, do this:

```text
1. Use aerialrows-development as base.
2. Commit the source that Gradle currently injects at build time.
3. Delete the source-rewriting Gradle task.
4. Make CI run on integration/development pushes.
5. Assert builds leave tracked source unchanged.
6. Build/lint the source-pure baseline.
7. Upgrade the toolchain separately.
8. Then begin Phase 1 core architecture.
```

This is the most important result of Group 12.

If we skip it and continue implementing Projectivy features through the current patching approach, every later subsystem becomes harder to review, test, migrate and debug.

---

## 19. Final architecture target

```text
Aerial Rows
│
├── Core
│   ├── Config + migrations
│   ├── Card/row identity
│   ├── LaunchTarget
│   ├── LaunchGate
│   ├── Focus state
│   └── Image/cache infrastructure
│
├── Home
│   ├── Row registry
│   ├── Sliding Rows
│   ├── left rail
│   ├── Grid/Carousel/Dock
│   └── generic card actions
│
├── Apps
│   ├── LauncherApps discovery/observation
│   ├── usage data
│   └── overrides
│
├── TV content
│   ├── Watch Next
│   ├── Preview Channels
│   ├── details
│   └── preview playback
│
├── Media
│   ├── Notifications
│   └── Now Playing
│
├── Wallpaper
│   ├── Provider registry
│   ├── Renderer registry
│   ├── AerialProvider
│   ├── Program providers
│   └── Media3 video renderer
│
├── Settings
│   ├── routes
│   ├── persistent focus
│   └── switches/choices/dropdowns
│
├── Inputs
│   ├── standard TV inputs
│   └── optional vendor adapters
│
├── System integration
│   ├── HOME role
│   ├── startup/autostart
│   ├── remote gestures
│   ├── optional Accessibility
│   └── idle coordinator
│
├── Personalization
│   ├── icons/icon packs
│   ├── shortcuts
│   └── custom artwork
│
├── Safety/policy
│   └── parental LaunchGate
│
├── Backup
│   ├── versioned schema
│   ├── migration
│   └── bundled custom assets
│
└── Optional device tools
    ├── calibration
    ├── display profiles
    └── privileged/vendor utilities
```

---

## 20. Group 12 conclusion

Groups 1–11 show that the Projectivy architecture is sufficiently closed to implement. Group 12 shows that the existing Aerial-Rows branch is also sufficiently understood to begin that implementation without restarting the project.

The current Aerial-Rows code is not throwaway code. Several of its hardest user-facing pieces already exist and should be preserved. The necessary change is architectural: convert those prototypes into common row/card/provider/config/launch systems and then add the missing Projectivy-class subsystems on top.

**Implementation should begin with Phase 0, not with another launcher feature.**


---

# SOURCE FILE: `group12_postclosure_audit_v2.md`

SHA-256: `55b146b0bc63bbd0436039be1774c88438b88e97cd0c4d41f86f213c5ea0753c`

---

# Projectivy 4.72 / Aerial-Rows — Group 12 Post-Closure Audit v2

**Group:** 12 — final Aerial-Rows reconstruction / implementation map  
**Audit:** `AUDIT_12_R2`  
**Process contract:** v18  
**Projectivy target:** 4.72 / code 97  
**Projectivy APK SHA-256:** `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
**Aerial-Rows implementation branch:** `aerialrows-development`  
**Pinned implementation commit:** `3c1cfd56234f1d386f4553dd6c69523670500d70`  
**Pinned tree:** `457f4ab1d08a8d545bdf5545777cd31f907ae24d`  
**Main reference:** `d3dcd70d3dd3b261108d25d72569671aee23b2d6`  

## 1. Executive verdict

The original Group 12 implementation map remains structurally correct.

The correct implementation base is still the existing `aerialrows-development` branch. It has not moved since the original Group 12 analysis, so the source-by-source assessment remains current. The development branch is still 36 commits ahead of `main` and 0 behind.

There is no reason to restart from Couchy/main and no reason to attempt a literal Projectivy source transplant. The existing Aerial-Rows work already contains useful launcher, Aerial, TV-content, row-editing and presentation functionality that should be preserved.

The central Group 12 conclusion still stands:

```text
Aerial-Rows development branch
        ↓
make source reproducible
        ↓
introduce common launcher architecture
        ↓
migrate existing working features onto it
        ↓
add remaining Projectivy-class subsystems
```

Post-closure audits of Groups 1–11 do **not** require a wholesale phase reorder. They do, however, make two dependency changes important enough to alter the implementation sequence inside the existing phases:

1. **Migration safety must move into Phase 1.** A last-good rollback snapshot must exist before the first sequential schema migration can safely mutate persistent state. The full portable user backup engine can remain in Phase 10.
2. **The common launch policy must move earlier.** `LaunchDispatcher` plus a neutral allow-all `LaunchPolicy`/`LaunchGate` interface should exist before programs, inputs, remote mappings and autostart targets are added. Optional parental policy can plug into it later without rerouting launch code.

Those are the only true ordering changes. Everything else is a refinement or stronger boundary.

---

## 2. Live Aerial-Rows snapshot remains unchanged

The live GitHub branch still matches the original Group 12 pin exactly:

```text
aerialrows-development
3c1cfd56234f1d386f4553dd6c69523670500d70

tree
457f4ab1d08a8d545bdf5545777cd31f907ae24d

main
d3dcd70d3dd3b261108d25d72569671aee23b2d6

relationship
36 ahead / 0 behind
```

Important blob identities also remain pinned, including:

```text
app/build.gradle.kts                         1668e8c60f6cf21ff2363f216028fa431f3d6754
MainActivity.kt                              e5b83f7aa597ad245b607f30a65c81d878bdb303
AppRepository.kt                             6467d2787ce9fc920c025e8cb994c2cf520005cb
BuiltinAerials.kt                            bdecdd784dd4b04c2c4c0877acd3faff46760a0f
Config.kt                                    4710f41198308e555ac9e3b12d515f1fefa1db6e
TvContentRepository.kt                       714b6fd92637d76f6d12b4702db1475859fccac3
LauncherScreen.kt                            a3172678c0bde47a28fa89247b4c5662cc659b1b
SettingsSheet.kt                             a20140273352bd63c990b2c3e9037c1eb717836c
SlidingRows.kt                               a43e8a4fec51549ea7d942490b9c127ee758bd69
```

Therefore there is no need to redo the entire source map.

---

## 3. Priority zero is still mandatory

`app/build.gradle.kts` still contains the `applyAerialRowsSourcePatches` build task.

That task mutates source files such as:

- `SettingsSheet.kt`;
- `LauncherScreen.kt`;

before compilation.

This remains the single most urgent engineering problem because the checked-in source tree is not a complete description of the program being built.

Before another Projectivy-class feature is added:

```text
1. Materialize every intended generated/injected change into normal source.
2. Commit those source changes.
3. Remove applyAerialRowsSourcePatches.
4. Make CI fail if a build changes tracked source.
5. Verify debug + release + lint from a clean checkout.
6. Only then do the toolchain upgrade.
7. Only after that begin the architectural migration.
```

This recommendation is stronger after the post-closure audits, not weaker.

---

## 4. What Groups 1–5 change in Group 12

### 4.1 Manifest/capability policy

Carry forward the Group 1 correction:

- declared EPG write permission is not proof of a TV-program publishing subsystem;
- Aerial-Rows should remain read-oriented unless a future feature explicitly requires writes;
- exported/plugin surfaces should be added only with corresponding runtime validation;
- `libptutils.so` is not a launcher-core dependency.

### 4.2 Home identity/focus

Group 2 strengthens the home model:

```text
left navigation headers != content-row headers
row identity             != current list index
card identity            != presenter type
Inputs row               != Live TV row
```

Use logical stable identities and preserve per-row focus by identity.

Focused-card changes should publish semantic events that wallpaper/media features can consume without coupling directly to the renderer.

### 4.3 Apps and visibility

The most important Group 3 implication is that Projectivy's ordinary Hide behavior is category/row-local rather than simply a global hidden-package switch.

Aerial-Rows should therefore distinguish:

```text
AppIdentity
 ├─ package
 ├─ component/activity
 └─ user/profile where relevant

AppCustomization
 ├─ title/artwork override
 ├─ usage metadata
 └─ category/row visibility/membership
```

A separate global recovery/exclusion mechanism can still exist, but it should not be the only representation of Hide.

### 4.4 TV Provider

Group 4 strengthens Phase 4:

- runtime numeric channel IDs are not durable logical identity;
- management must include non-browsable channels;
- `requestChannelBrowsable()` should be supported where appropriate;
- Preview and Watch Next have distinct identities/sorts;
- richer program metadata belongs in the model;
- long description may remain lazy;
- use `ContentObserver` + debounce + resume refresh rather than copying Projectivy's stale refresh limitations;
- all deep links go through the common launch policy;
- remain read-oriented by default.

### 4.5 Notifications and Now Playing

Group 5 strengthens Phase 7:

- Notification state and MediaSession state are separate repositories;
- keep structured session identity rather than flattening it too early;
- capability-gate transport controls;
- do not make notification dismissal implicitly stop playback unless intentionally designed;
- Now Playing launches should go through our common launch policy even though Projectivy contains an inconsistent bypass;
- media/session state and user-focused-card state are different semantic signals.

---

## 5. Group 6 — revised Phase 5 requirements

Aerial integration should follow this boundary:

```text
WallpaperProvider
      ↓ candidates
WallpaperCoordinator
      ↓
RendererRegistry
 ├─ ImageRenderer
 ├─ VideoRenderer / Media3
 ├─ ColorRenderer
 ├─ DrawableRenderer
 └─ optional later renderers
```

`BuiltinAerials` becomes an `AerialProvider`.

It must not become its own player.

Phase 5 now explicitly requires:

1. one shared Media3 VIDEO renderer;
2. timed wallpaper rotation in its own scheduler;
3. provider catalogue invalidation separate from timed rotation;
4. “ignore while video plays” suppresses timed rotation specifically;
5. explicit video repeat/rotation policy;
6. video volume `0` disables audio-track selection entirely;
7. tunneling and decoder fallback live in renderer config;
8. dynamic palette sampling cadence is independent from rotation cadence;
9. portrait/too-small rejection and Blur/Skip behavior live in ImageRenderer;
10. remote wallpaper advance is a generic remote action, not Aerial-specific input code;
11. idle/immersive presentation reuses the same player;
12. themes and full backups remain separate scopes;
13. external provider validation is explicit/versioned if plugin compatibility is implemented.

---

## 6. Group 7 — revised config/settings foundation

Phase 1 must establish these primitives from the start:

```text
ConfigEnvelope
 ├─ schemaVersion
 └─ domain configs

MigrationRegistry
MigrationSafetySnapshotStore

SettingKey<T>
ExportPolicy
UpdatePolicy
SettingId
```

The important new ordering rule is:

```text
read old config
     ↓
write/verify last-good migration snapshot
     ↓
run sequential migrations
     ↓
validate normalized result
     ↓
commit new schema
     ↓
rehydrate repositories once
```

Do not wait until Phase 10's user-facing backup engine to gain migration rollback safety.

Phase 6 settings use:

```text
SettingDescriptor<T>
 ├─ stable SettingId including scope
 ├─ control type
 ├─ validation
 ├─ capability/visibility predicate
 ├─ export policy
 └─ update policy
```

Update policies should include at least:

```text
IN_PLACE
LIVE_PREVIEW
RELOAD_SUBSYSTEM
REFRESH_ACCESSIBILITY
RECREATE_ACTIVITY
RESTART_HOME_TASK
DEFERRED
```

Most Compose appearance changes should remain `IN_PLACE`, not restart HOME.

---

## 7. Group 8 v2 — revised input architecture

The original Phase 8 needs a stronger state model:

```text
TvInputManager
   ├─ TvInputCallback
   ├─ resume refresh
   └─ bounded recovery/backoff
          ↓
InputRepository
   ├─ AvailableInputs
   ├─ VisibleInputShortcuts
   └─ CurrentInputState
          ↑
InputLaunchCoordinator
   ├─ standard strategy
   ├─ internal TvView where justified
   ├─ Xiaomi adapter
   ├─ MediaTek adapter
   ├─ Amlogic/FengOS adapter
   └─ generic fallback
```

Important corrections:

- do not equate visible shortcut rows with physical input availability;
- do not claim the verified generic Projectivy path proves first-class tuner launching;
- do not synthesize individual missing HDMI ports when the platform already supplied a partial authoritative list;
- every successful launch strategy writes the same canonical `CurrentInputState`;
- Sony/TCL-style recovery should be explicit bounded recovery rather than accidental demand-triggered rediscovery;
- standard Android TV / Nvidia Shield operation must work with no vendor adapter loaded.

If display profiles are ever enabled, keep them optional:

```text
DisplayStateRepository
      ↓
DisplayProfileCoordinator
```

Do not make Accessibility own the profile poller.

---

## 8. Group 9 v3 — revised HOME/system architecture

Phase 9 should be split into these independent services:

```text
HomeRoleCoordinator
StartupCoordinator
RemoteGestureEngine
AccessibilityBridge       [optional]
LauncherIdleCoordinator
VendorCompatibilityRegistry [optional]
```

### HomeRoleCoordinator

Owns:

- current/default HOME detection;
- HOME-role setup/request guidance;
- restart/re-enter HOME task behavior.

### StartupCoordinator

Owns:

- one cancellable pending job;
- boot-window validation at execution time;
- duplicate suppression;
- app/input/screensaver target dispatch;
- wake-from-standby trigger.

### RemoteGestureEngine

Owns:

- key gesture state;
- short/double/long timing;
- consumption policy;
- typed actions.

### AccessibilityBridge

Optional only:

- HOME fallback where needed;
- foreground/window signals;
- optional global key filtering;
- typed events to coordinators.

It must **not** own startup, input state, display profiles or idle state.

### VendorCompatibilityRegistry

Optional adapters only:

- Xiaomi startup preconditioner;
- Play Store `sys.cts.running` workaround;
- Xiaomi updater workaround;
- future vendor-specific compatibility hacks.

These should be disabled by default and capability gated.

---

## 9. Group 10 v2 — revised backup/icons/shortcuts phase

### 9.1 Full backup remains Phase 10

The portable backup engine should still be introduced later:

```text
BackupEngine
 ├─ manifest/schema version
 ├─ explicit export policy
 ├─ typed configuration
 ├─ custom assets by digest
 ├─ merge/replace policy
 ├─ authenticated encryption
 └─ restore -> migrate -> validate -> one rehydrate
```

Do not reproduce Projectivy's opaque `s0`/bootstrap-dependent crypto boundary.

### 9.2 ThemeEngine

Keep theme semantics separate from full backup:

- theme manifest;
- appearance payload;
- optional wallpaper assets;
- explicit category-origin rules;
- explicit user-state stripping.

### 9.3 IconRepository

Use:

```text
IconRepository
 ├─ per-card overrides
 ├─ ADW appfilter parser
 ├─ package + packageVersion cache key
 └─ fallback banner/icon resolution
```

### 9.4 ShortcutRepository

Support both shortcut worlds:

```text
ShortcutRepository
 ├─ global hosted shortcut cache
 ├─ package-scoped queries
 ├─ LauncherApps.Callback refresh
 ├─ startShortcut()
 ├─ external PinItemRequest intake
 └─ launcher pin/unpin mutation
```

### 9.5 Parental control

Parental controls should be **optional**, not required for the first implementation waves.

But the neutral launch-policy boundary must already exist from Phase 3:

```text
LaunchDispatcher
      ↓
LaunchPolicy
      ↓ default implementation
AllowAllLaunchPolicy
```

If parental controls are later requested:

```text
ParentalLaunchPolicy
 ├─ protected stable IDs
 ├─ schedule
 ├─ auth/session TTL
 ├─ secure verifier
 └─ modal input filtering
```

This avoids a future launch-path rewrite.

---

## 10. Group 11 v2 — optional expert tools boundary

Do not place Projectivy's maintenance utilities into normal repositories.

Use:

```text
OptionalExpertTools
│
├── PackageMaintenance
│   ├─ APK installer [only if wanted]
│   ├─ disable/enable [only if wanted]
│   └─ uninstall/install-existing [only if wanted]
│
├── VendorCompatibility
│   ├─ Xiaomi startup preconditioner
│   ├─ Play Store workaround
│   ├─ Xiaomi updater workaround
│   └─ vendor settings/engineering intents
│
├── Diagnostics
│   ├─ device/build/display information
│   ├─ TV-input compatibility diagnostics
│   └─ optional root/sysfs diagnostics
│
└── RemoteManagement
    └─ independently designed if ever requested
```

Do **not** make these dependencies of:

```text
HomeRepository
AppRepository
TvContentRepository
MediaRepository
WallpaperRepository
InputRepository
SettingsRepository
```

Projectivy's `libptutils.so`, opaque `rps()` bootstrap and protection/licensing behavior are not required for Aerial-Rows.

---

## 11. Updated core model architecture

### Identities

```kotlin
data class AppIdentity(
    val packageName: String,
    val componentName: String?,
    val userSerial: Long? = null,
)

data class RowIdentity(val stableKey: String)
data class CardIdentity(val stableKey: String)
data class ProviderIdentity(val packageName: String, val providerKey: String? = null)
```

Do not use transient Android row/channel/list indexes as durable identity.

### Launch model

```kotlin
sealed interface LaunchTarget {
    data class App(/* component/user */) : LaunchTarget
    data class IntentUri(/* ... */) : LaunchTarget
    data class Shortcut(/* ... */) : LaunchTarget
    data class TvInput(/* ... */) : LaunchTarget
    data class InternalAction(/* ... */) : LaunchTarget
}

data class LaunchRequest(
    val target: LaunchTarget,
    val source: LaunchSource,
)
```

All launcher-originated launches converge:

```text
UI card
program
shortcut
input
remote
boot/autostart
        ↓
LaunchDispatcher
        ↓
LaunchPolicy
        ↓
platform/vendor strategy
```

### Home cards

```text
HomeCard
 ├─ App
 ├─ Program
 ├─ NowPlaying
 ├─ Input
 ├─ Shortcut
 ├─ Folder
 └─ ConfigureRow
```

### Home rows

```text
HomeRow
 ├─ Apps
 ├─ WatchNext
 ├─ TvChannel
 ├─ NowPlaying
 ├─ Inputs
 ├─ LiveTv
 ├─ Shortcuts
 └─ Folder
```

`Inputs` and `LiveTv` remain conceptually different row families.

---

## 12. Updated source/package structure

Keep one Gradle app module initially.

Prefer package architecture over premature Gradle-module extraction:

```text
com.conreo.couchytv/
  core/
    model/
    config/
    migration/
    launch/
    focus/
    events/
    image/
    capability/

  feature/
    home/
    apps/
    tvcontent/
    media/
    wallpaper/
    aerials/
    settings/
    inputs/
    shortcuts/
    icons/
    backup/
    boot/
    accessibility/
    idle/

  integration/
    androidtv/
    vendor/
    privileged/

  optional/
    parental/
    displayprofiles/
    devicetools/
    packagemaintenance/

  ui/
    components/
    theme/
```

This makes the key dependency rule visible in the package tree:

```text
feature/core code
      must not depend on
integration/vendor or integration/privileged
```

---

## 13. Updated implementation order

### PHASE 0 — Source-pure reproducible baseline

1. Pin `3c1cfd56234f1d386f4553dd6c69523670500d70` as recovery point.
2. Materialize every build-injected source change.
3. Remove `applyAerialRowsSourcePatches`.
4. CI on development/integration pushes.
5. CI asserts source tree unchanged after build.
6. `assembleDebug`, `assembleRelease`, `lintRelease` from clean checkout.
7. Toolchain upgrade in a separate commit.

**Gate:** build is reproducible from committed source only.

### PHASE 1 — Config, migrations, safety snapshots and core identity

1. versioned ConfigEnvelope;
2. domain configs;
3. immutable SettingKey + ExportPolicy + UpdatePolicy;
4. MigrationRegistry;
5. MigrationSafetySnapshotStore;
6. migrate existing LauncherConfig with validation/rollback;
7. CardIdentity / RowIdentity / AppIdentity / ProgramIdentity;
8. LaunchTarget / LaunchRequest / LaunchPolicy interface;
9. adapters so current UI still looks/works the same.

**Gate:** current config round-trips through new schema; failed migration restores last-good config.

### PHASE 2 — Home registry and focus

1. promote SlidingRows private models;
2. HomeRepository/RowRegistry;
3. stable row/card focus store;
4. separate rail and content-header states;
5. row capabilities;
6. semantic focused-card events;
7. stable reordering.

**Gate:** rail reveal/return and row reorder preserve focus by identity.

### PHASE 3 — Apps, actions and launch core

1. component-aware app identity;
2. TV/mobile discovery + package-first dedup;
3. LauncherApps.Callback + debounce;
4. targeted cache invalidation;
5. customization/usage repository;
6. category/row-local visibility;
7. CardActionResolver;
8. LaunchDispatcher;
9. default AllowAllLaunchPolicy;
10. route existing app launch through dispatcher.

**Gate:** all app-originated launches use the common path; no regression to app menu behavior.

### PHASE 4 — TV Provider completion

1. stable provider/channel/program keys;
2. management includes non-browsable channels;
3. request browsability;
4. Preview/Watch Next sort policies;
5. provider limits/exclusions;
6. rich metadata/details;
7. preview playback dwell controller;
8. ContentObserver + debounce;
9. resume refresh;
10. provider/cache reconciliation;
11. deep links through common launch path.

**Gate:** provider changes appear without app restart; channel-ID recreation does not destroy logical row identity.

### PHASE 5 — Wallpaper/Aerial engine

1. WallpaperProvider SPI;
2. Renderer SPI;
3. current preset/user media -> providers;
4. BuiltinAerials -> AerialProvider;
5. one Media3 video renderer;
6. timed-rotation coordinator;
7. provider invalidation events;
8. image fallback policy;
9. video renderer advanced policy;
10. focus/media/idle semantic events;
11. immersive idle presentation reuses same renderer.

**Gate:** switching provider does not recreate launcher/player; only one background video engine exists.

### PHASE 6 — Settings

1. SettingsNavigator;
2. SettingId + focus store;
3. reusable control rows;
4. real choice/dropdown dialog;
5. explicit UpdatePolicy;
6. lifecycle-scoped preview debounce;
7. migrate one page at a time;
8. restore/bulk-update rehydrate path.

**Gate:** value mutation preserves focus; subpage return restores opener.

### PHASE 7 — Notifications and media

1. NotificationListenerService;
2. NotificationRepository;
3. MediaSessionRepository;
4. structured session identity/dedup;
5. Now Playing row/cards;
6. capability-gated transport controls;
7. media/focus semantic events;
8. idle coordinator input;
9. policy-gated launch;
10. notification dismissal and playback control remain separate actions.

**Gate:** notification access failure degrades gracefully and media monitoring remains independently testable.

### PHASE 8 — Inputs

1. live TvInputManager repository;
2. TvInputCallback;
3. resume refresh;
4. AvailableInputs;
5. VisibleInputShortcuts;
6. CurrentInputState;
7. InputLaunchCoordinator;
8. standard Android strategy first;
9. conservative fallback/recovery;
10. optional vendor strategies;
11. optional display-state/profile subsystem only if wanted.

**Gate:** Nvidia Shield/standard Android TV path works with no vendor implementation loaded.

### PHASE 9 — HOME/startup/remotes/accessibility/idle

1. HomeRoleCoordinator;
2. StartupCoordinator;
3. duplicate/boot-window policy;
4. standby/wake trigger;
5. RemoteGestureEngine;
6. optional AccessibilityBridge;
7. LauncherIdleCoordinator;
8. screensaver strategy chain;
9. capability-gated vendor compatibility adapters.

**Gate:** ordinary launcher use never requires Accessibility or privileged workarounds.

### PHASE 10 — Backup/themes/icons/shortcuts

1. portable BackupEngine;
2. authenticated encryption;
3. merge/replace + migration/rehydrate;
4. ThemeEngine;
5. custom assets by digest;
6. IconRepository + package-version cache;
7. ShortcutRepository;
8. pin request intake;
9. launcher pin/unpin;
10. optional ParentalLaunchPolicy if deliberately enabled.

**Gate:** backup round trip passes; custom assets survive; all launch surfaces still traverse LaunchPolicy.

### PHASE 11 — Optional expert/device tools and release polish

Only after core is stable:

- display profiles/calibration if wanted;
- engineering/settings intents;
- diagnostics;
- optional APK/package maintenance;
- optional vendor compatibility;
- optional external provider compatibility;
- performance profiling;
- branding/application-ID release migration decision.

**Gate:** removing the whole optional package must not break ordinary launcher operation.

---

## 14. Scope tiers

### Tier A — core Projectivy-class launcher target

- home rows/focus/rail;
- installed apps/actions;
- TV Provider/Watch Next;
- notifications/media/Now Playing;
- wallpaper engine + Aerials;
- settings;
- standard TV inputs;
- HOME role/startup/remotes/idle;
- versioned config/migrations;
- backups/themes/icons/shortcuts.

### Tier B — optional parity

- parental controls;
- display profiles;
- calibration tools;
- vendor-specific input/settings adapters;
- external wallpaper-provider compatibility;
- selected diagnostics.

### Tier C — deliberately not part of core reconstruction

- premium/licensing bypass;
- opaque Projectivy native/protection/bootstrap behavior;
- root stock-launcher modification;
- GApps removal;
- Google Play immutable APK/self-update blocker;
- privileged Play/Xiaomi hacks by default;
- cloning Projectivy Web Remote.

These can only be independently designed later if explicitly wanted.

---

## 15. Manifest strategy after the post-closure audits

Add Android capabilities only when their implementation lands.

Examples:

```text
TV Provider reads       -> READ_TV_LISTINGS where required
media notification bridge -> NotificationListenerService
boot/autostart          -> RECEIVE_BOOT_COMPLETED
Accessibility fallback  -> optional BIND_ACCESSIBILITY_SERVICE service
shortcuts               -> LauncherApps/PinItemRequest flows
TV inputs               -> standard TvInputManager APIs
wallpaper plugins       -> explicit service discovery/validation if implemented
```

Do not add write-EPG behavior simply because Projectivy declared permissions.

Attempt to remove `QUERY_ALL_PACKAGES` once app/launcher visibility has been reworked around launcher intents, LauncherApps and explicit queries.

---

## 16. Architecture invariants

These are now stronger than ordinary phase gates and should be checked during review/CI where practical.

### Build invariant

```text
git status before build == git status after build
```

### Launch invariant

Every launcher-originated target uses:

```text
LaunchDispatcher -> LaunchPolicy
```

No feature Composable directly calls `startActivity()` for a launcher content target.

### Vendor boundary invariant

Core repositories do not depend on vendor/privileged implementations.

### Accessibility invariant

Normal home rendering, input enumeration, display state, startup scheduling and idle state do not depend on Accessibility.

### Input-state invariant

```text
AvailableInputs != VisibleInputShortcuts
```

and every successful launch strategy updates `CurrentInputState`.

### Wallpaper invariant

Only one background/Aerial VIDEO engine exists.

### Migration invariant

No destructive schema migration executes before a validated last-good snapshot exists.

### Focus invariant

Focus restoration uses stable identity, not list index.

### Refresh invariant

Package/provider/input callbacks update the affected repository rather than rebuilding unrelated home state.

---

## 17. Testing gates strengthened by the audits

### Config/migration

- every historical schema fixture migrates to current;
- invalid migration result does not overwrite last-good state;
- interrupted/power-loss-like write path remains recoverable;
- bulk import causes one final repository rehydrate.

### Home/focus

- rail reveal and close restore exact card;
- row reorder preserves row/card identity;
- disappearing program/input moves focus to deterministic fallback;
- content-row title visibility does not alter nav-rail title state.

### Apps

- same package TV/mobile activities do not duplicate visually unless explicitly intended;
- component identity is preserved internally;
- category-local Hide does not accidentally remove app everywhere;
- package callback invalidates only affected cache records.

### TV content

- non-browsable channels appear in management;
- channel recreation can reconcile to logical identity;
- Preview/Watch Next sorts are deterministic;
- ContentObserver refresh works without Activity recreation.

### Wallpaper

- provider invalidation does not masquerade as timed rotation;
- volume zero disables audio track selection;
- Aerial idle/background use same player;
- renderer palette cadence does not change rotation cadence.

### Settings

- mutate setting -> focus stays on SettingId;
- subpage return -> opening SettingId restored;
- RESTART_HOME_TASK used only where explicitly required.

### Media

- notifications and sessions can exist independently;
- dismiss notification does not stop playback unless user selected such an action;
- media controls reflect controller capabilities.

### Inputs

- callback add/remove/update changes AvailableInputs;
- hiding shortcut changes VisibleInputShortcuts only;
- partial platform list does not synthesize arbitrary missing ports;
- every launch strategy updates CurrentInputState.

### System integration

- boot/autostart duplicate guard;
- execution-time boot-window check;
- wake path independent from Accessibility;
- global remote gestures disabled/gated around protected modal flows;
- optional vendor adapters cannot affect unsupported devices.

---

## 18. Suggested first implementation slices

Do not begin with TV inputs, parental controls or device tools.

The first slices should be:

```text
1. infra: materialize injected source changes
2. infra: remove preBuild source patcher
3. ci: fail build if tracked source changes
4. build: isolated AGP/JDK migration
5. config: ConfigEnvelope + schemaVersion
6. config: MigrationSafetySnapshotStore + MigrationRegistry
7. core: stable identities + LaunchTarget/LaunchRequest
8. launch: LaunchDispatcher + AllowAllLaunchPolicy
9. home: promote SlidingRows models
10. home: stable focus/rail state
11. apps: component identity + LauncherApps observation
12. apps: generic CardActionResolver + row-local visibility
```

That gives later TV/media/input/remote work a stable foundation instead of adding more feature-specific state to the existing monolithic files.

---

## 19. Verification status

This audit used the unchanged pinned Aerial-Rows repository snapshot, all post-closure reports from Groups 1–11, and a fresh representative canonical REA anchor set.

Verification assertions:

```text
24 / 24 PASS
```

Fresh Group 12 canonical REA anchor evidence:

```text
12 / 12 success
errors  0
invalid 0
```

The v18 11-dimension closure matrix is fully satisfied:

```text
entry points                 PASS
base classes / inheritance   PASS
all DEX files                PASS
obfuscated collaborators     PASS
models / data flow           PASS
persistence                  PASS
resources / XML              PASS
Android framework APIs       PASS
callbacks / observers        PASS
launch actions / lifecycle   PASS
native / JNI                 PASS
```

No new hidden Projectivy subsystem was found that invalidates the implementation map.

---

## 20. Final conclusion

The Aerial-Rows project is ready to move from reverse-engineering/planning into implementation.

The correct decision remains:

```text
KEEP aerialrows-development
        ↓
PHASE 0 source purity
        ↓
PHASE 1 config + migration safety + identities
        ↓
PHASE 2 home/focus architecture
        ↓
PHASE 3 common launch/app architecture
        ↓
then add the remaining Projectivy-class subsystems
```

Do not add another major feature before Phase 0.

Do not restart from Couchy/main.

Do not copy Projectivy's opaque native/bootstrap or privileged maintenance quirks into launcher core.

Preserve the working Aerial-Rows visuals/Aerial catalogue/TV prototypes, but move them onto the common row/card/config/launch/provider architecture described above.

**Group 12 status: COMPLETE — POST-CLOSURE RE-AUDITED v2.**

---

# SECTION G — Reverse-Engineering / Audit Process History
# Reverse-Engineering Audit Process — Full Archive
This file concatenates the complete source reports from the research archive. Each section preserves the original report text.


---

# SOURCE FILE: `PROCESS_STATE.md`

SHA-256: `b8663336133821108d668b0d3859834114a66521b30d62b6ab815e623d3f6182`

---

# Reverse-Engineering Process State / Provenance

This file preserves the final operational state of the Projectivy analysis in case a future contributor must reopen APK research.

## Target

- Projectivy Launcher APK 4.72 / code 97
- package `com.spocky.projengmenu`
- APK SHA256 `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
- signing certificate SHA256 `f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`
- minSdk 23 / targetSdk 37
- 7395 classes
- 1171 resource entries
- 3 DEX files
- `libptutils.so` in the four common Android ABIs

## Toolchain used

- JADX headless MCP 0.7.1 all-in-one JAR
- REA 6.7.0
- Java 21
- Node 22.20.0
- persistent JADX MCP daemon

Historical environment paths:

```text
/mnt/data/rea-portable
/mnt/data/rea
/mnt/data/launcherproject_work
```

These paths are historical only.

## Final process contract behavior

The audit process was hardened through contract v18.

Important final rules:

- semantic-first discovery;
- persistent daemon;
- no empty-query REA as exhaustive evidence;
- no aggressive parallel JVMs;
- complete class inventory;
- complete separately pinned APK resource inventory;
- bounded/resumable batches (default 6, max 12);
- write-ahead batch checkpoint before slow JVM query;
- interrupted attempt recovery;
- atomic report/state/evidence writes;
- failed probes quarantined;
- exact REA and persistent JADX caches;
- FTS dotted/literal fallback;
- named-class prevalidation, preserving `defpackage.*` pass-through;
- source claims closed against 11 dimensions;
- after a report is ready, use completion receipt rather than repeatedly rerunning deep diagnostics.

Closure dimensions:

1. entry points
2. base classes / inheritance
3. all DEX files
4. obfuscated collaborators
5. models / data flow
6. persistence
7. resources / XML
8. Android framework APIs
9. callbacks / observers
10. launch / actions / lifecycle
11. native / JNI

Final contract references from the original environment:

- contract v18 SHA: `052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`
- process bundle SHA: `251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

## Operational lessons from interruptions

- If an outer timeout occurs, inspect durable batch state and resume only unfinished work.
- Never discard completed evidence because the chat/tool call was interrupted.
- A completion interruption can be delivery/finalization-only; verify the durable report before rerunning analysis.
- Inspect the actual installed CLI before relying on documentation; one historical process note mentioned `--core` self-test, but the installed CLI at the time did not expose that option.
- Do not chain historical re-attestation or deep diagnostics after the current result is already marked ready.

## Preserved process notes

The older hardening reports are stored next to this file. They document the evolution toward the final process and are useful if a future agent needs to understand why these rules exist.


---

# SOURCE FILE: `audit_process_hardening_v3.md`

SHA-256: `2b8904130aa745aea6d643c36b82e333f05cf5d5264d5fcd3380184483585536`

---

# Launcher Project Audit Process Hardening v3

## Result
The apparent interruption during the Group 9 re-audit did not lose or corrupt Group 9 analysis. `AUDIT_09_R2` had already completed discovery, verification, synthesis, and report generation. The failure was orchestration residue around the completed run, not a failed reverse-engineering stage.

## Defects found
1. Atomic `.tmp` files left by an externally interrupted parent process remained in the evidence directory.
2. The old evidence snapshot code could include those `.tmp` files because it only excluded `.err` files.
3. The first shared Android class inventory used one `list_classes` page. JADX reported 7,395 classes but the page contained only the first 4,096.
4. New REA probes still paid a JVM/APK load cost on every uncached query.
5. Concurrent probes targeting the same output had no per-output lock.
6. Timeout cleanup did not explicitly terminate the complete child process group.
7. The FTS rebuild updated documents but did not prune deleted stale paths.
8. There was no single fail-closed preflight gate combining source hashes, DB integrity, evidence semantics, temp-artifact state, and stage order.

## Implemented fixes
### Clean canonical evidence manifests
- Temporary/partial/lock files are never canonical evidence.
- Snapshots include only semantically successful REA evidence.
- Snapshot verification checks SHA-256, size, semantic envelope, source-report hash, and target-APK hash.
- Group 9 snapshot regenerated: 53/53 valid files, zero temp entries.

### Deterministic cleanup and preflight
`auditctl.py` now supports:
- `cleanup`
- `health`
- `preflight`
- source-report and APK hash drift checking
- stage-order enforcement
- stale FTS document pruning
- WAL checkpointing after durable checkpoints

### Complete Android inventory
A persistent JADX MCP session now pages `list_classes` directly until `len(items) == total` and unique count matches total.

Verified inventory:
- expected: 7,395
- returned: 7,395
- unique: 7,395

The previous 4,096-entry file is explicitly retained only as a known-incomplete diagnostic artifact and is not used as a complete inventory.

### Persistent fast discovery layer
A local persistent JADX MCP service loads the APK once and stays resident.

Architecture:

```
APK
 ├─ Persistent JADX MCP (fast discovery / inventory / candidate inspection)
 └─ REA -> JADX (canonical verification evidence for high-risk claims)
```

This preserves REA provenance for conclusions while avoiding repeated 7–9 second JVM/APK startup costs during discovery.

A full 7,395-class paged inventory from the already-loaded service completed in ~0.084 s after startup.

### Safer REA probe execution
`rea_probe.py` now adds:
- per-output filesystem locking
- atomic final-file replacement
- semantic envelope validation
- exact-evidence cache keyed by operation + parameters + target APK SHA-256
- process-group termination on timeout
- bounded retries only for retryable failures
- stale temp cleanup

### FTS/index reliability
Index rebuild now:
- excludes temp/partial/lock files
- updates changed documents by SHA-256
- prunes records whose files no longer exist
- verifies SQLite integrity

## Current Group 9 state

```
AUDIT_09_R2
  discovery      complete
  verification   complete
  synthesis      complete
  report         complete

resume_from = none
canonical evidence = 53/53 valid
stale temp files = 0
DB integrity = ok
source report hash = ok
target APK hash = ok
```

The finished Group 9 v2 report remains authoritative; no Group 9 analysis needs to be repeated because of this interruption.


---

# SOURCE FILE: `audit_process_hardening_v4.md`

SHA-256: `0f721bb7432c753a087dbac9bb86fefd44dd0a1b68cabac27245787514db8197`

---

# Launcher / Projectivy Audit Process Hardening v4

## Purpose
This record consolidates the reliability and performance rules from the earlier Launcher/Projectivy reverse-engineering sessions and the latest interruption investigation. The process is intended to survive connection/session interruptions without losing verified work while keeping Projectivy analysis fast and evidence-backed.

## Current canonical toolchain
- Projectivy APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
- JADX MCP JAR SHA-256: `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`
- REA: `6.7.0`
- REA launcher SHA-256: `2f7ea4ae14f9d27fe47ac7595077bd3e8820665b8bdd87687b582a3b00ff4c40`
- Bundled Node: `v22.20.0`
- Bundled Node SHA-256: `b1cbec894e45a5814b6ab756e1e14f8a76516273197e67e0412b57c1e10d0d9f`
- JADX heap cap: `2048 MiB`
- Expected Projectivy classes: `7,395`
- Process contract: v6

## Earlier requirements retained
1. Work in small self-contained groups and stop/report after each group.
2. A group is not complete until its important dependency graph is closed.
3. Trace named Projectivy entry points into obfuscated R8/ProGuard collaborators.
4. Trace across all DEX files, resources, Android APIs, callbacks, persistence and JNI where relevant.
5. Preserve original `GROUP_xx` work separately from post-closure `AUDIT_xx` work.
6. A completed legacy report remains authoritative even if it predates the newer four-stage checkpoint schema.
7. Use deterministic stages: discovery -> verification -> synthesis -> report.
8. Checkpoint after every stage and preserve already-completed evidence after interruption.
9. Use later-report FTS delta search instead of rereading every later report.
10. Use raw REA/JADX verification only for uncertain/high-risk claims.

## Reliability fixes now enforced
- Exact source-report SHA drift detection.
- Exact target APK SHA drift detection.
- Exact JADX JAR SHA verification.
- REA launcher binary and bundled Node binary/version pinning.
- Process-tool bundle SHA pinning in completed evidence manifests.
- Atomic JSON/state/evidence/final-report writes.
- SQLite WAL + `PRAGMA integrity_check`.
- Ordered-stage enforcement and deterministic resume planning.
- Semantic REA success-envelope validation; shell exit code alone is insufficient.
- Bounded retry only for retryable failures.
- Adaptive bounded timeouts with process-group SIGTERM/SIGKILL cleanup.
- Per-output locking to prevent duplicate/racing probes.
- Complete paginated 7,395-class inventory; incomplete inventories fail closed.
- Smali fallback for decompilation failures.
- Persistent JADX discovery daemon with ownership/config/provenance checks.
- No aggressive parallel JADX JVMs.
- Canonical manifests contain SHA-256 + byte size + semantic-success status for every evidence file.
- Completed-manifest verification checks the current canonical evidence set exactly.
- Safe contract re-attestation: unchanged evidence is cryptographically revalidated, old manifests are preserved in provenance history, and only then is a new process-contract/tool-bundle attestation written.
- Failed/invalid exploratory probes are quarantined under `failed_probes/` with stderr and SHA provenance rather than left in canonical `evidence/` or deleted.
- Future bounded batches automatically quarantine failed probe output.
- Stale audit/shared transient workspaces are cleaned without touching canonical evidence.
- Persistent daemon log growth is bounded by rotation.
- Fail-closed `preflight` and full machine-readable `selftest`.

## Performance improvements now enforced
- Persistent JADX service loads Projectivy once instead of starting a JVM for every discovery query.
- Static semantic index contains 7,395 classes, 67,202 dependency edges, literals and resource references for cheap candidate discovery.
- Default-package names are normalized (`defpackage.yb3` -> `yb3`) for semantic dependency-graph queries.
- Exact JADX discovery cache keyed by target/tool hashes and arguments.
- Exact REA evidence cache keyed by target SHA, JADX SHA/version, REA version, heap and operation parameters.
- Existing verified evidence was backfilled into the current REA cache fingerprint format, including older canonical evidence that predated the cache database.
- Bounded resumable probe batches default to 6 and hard-cap at 12.
- FTS and semantic-index discovery are preferred before expensive canonical REA verification.

## Latest interruption diagnosis
The Group 9 analysis itself was not lost. `AUDIT_09_R2` had already completed discovery, verification, synthesis and report. The orchestration layer had several issues:

1. The process contract had advanced while completed manifests still carried the older contract hash/version.
2. Group 8 still had nine known failed/invalid exploratory outputs inside `evidence/` even though the canonical manifest correctly excluded them.
3. Semantic graph queries could silently miss default-package classes because REA used `defpackage.X` while the static index stores raw `X` names.
4. The hardened REA cache fingerprint changed, but older verified evidence had not all been backfilled to the new cache key.
5. Old semantic-build temporary directories remained in the shared workspace.

All five are now corrected.

## Current health
- Full process self-test: PASS
- SQLite integrity: PASS
- JADX daemon health/ownership/config: PASS
- Complete class inventory: 7,395 / 7,395 unique
- Shared stale transient count: 0
- `AUDIT_08_R2` preflight: PASS
  - canonical evidence: 80 / 80 semantic success
  - error/invalid canonical evidence: 0
  - snapshot verification: PASS
- `AUDIT_09_R2` preflight: PASS
  - canonical evidence: 53 / 53 semantic success
  - error/invalid canonical evidence: 0
  - snapshot verification: PASS

## Measured speed improvement
A previously uncached canonical REA class probe took about 8.4 seconds because REA/JADX had to initialize. After cache backfill, the same canonical evidence retrieval is approximately 0.7-0.8 seconds. Persistent JADX discovery calls are typically tens of milliseconds internally after service startup; command-wrapper overhead is roughly sub-second.

## Repository mutation rule
Before changing Aerial-Rows through the remote connector, capture a repository snapshot pinned to a full commit SHA and per-file blob SHAs. Compare against the previous snapshot and analyze only changed files when possible. Never assume an earlier checkout/tree still matches the live repository.

## Canonical next-step workflow
1. Run global self-test.
2. Initialize/resume one bounded `AUDIT_xx` namespace.
3. Fail-closed preflight.
4. Source hash check + FTS/semantic delta discovery.
5. Checkpoint discovery.
6. Run small resumable verification batches; REA is canonical for high-risk conclusions.
7. Quarantine failed probes, preserve failure ledger.
8. Checkpoint verification.
9. Synthesize against original + later-group findings.
10. Checkpoint synthesis.
11. Write final report atomically.
12. Create hashed canonical evidence snapshot.
13. Checkpoint report complete.
14. Run preflight and global self-test again.
15. Stop and report before starting the next group.


---

# SOURCE FILE: `audit_process_hardening_v5.md`

SHA-256: `814366522ea28f989a3db50fc8cae3c0621df887acd368424695656c1852c728`

---

# Launcher / Projectivy Audit Process Hardening v5

## Purpose
This revision reconciles the active audit process with every earlier reliability/performance rule recovered from the previous Projectivy/Launcher handoff and the v3/v4 hardening records. It preserves all earlier protections and adds machine-enforced subsystem closure plus stronger APK provenance.

## Canonical toolchain and target provenance
- Projectivy APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
- APK signing certificate SHA-256: `f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`
- DEX inventory: `classes.dex`, `classes2.dex`, `classes3.dex`
- ART baseline-profile inventory: `assets/dexopt/baseline.prof`, `assets/dexopt/baseline.profm`, `assets/dexopt/baseline.bin`
- JADX MCP JAR SHA-256: `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`
- REA: `6.7.0`
- Bundled Node: `v22.20.0`
- JADX heap cap: `2048 MiB`
- Expected Projectivy classes: `7,395`
- Process contract: v7

## Earlier requirements retained and enforced
1. Work in small self-contained groups and stop/report after each group.
2. Use small bounded JADX/REA batches rather than one huge invocation.
3. Preserve verified partial work and resume deterministically after interruption instead of restarting.
4. Keep original `GROUP_xx` analysis separate from post-closure `AUDIT_xx` analysis.
5. A completed legacy report remains authoritative even if it predates newer checkpoint schemas.
6. Use deterministic stages: `discovery -> verification -> synthesis -> report`, checkpointing every stage.
7. Trace named Projectivy entry points through obfuscated R8/ProGuard collaborators rather than stopping at readable package classes.
8. Trace all DEX files and include resources/XML, models/data flow, persistence, Android framework APIs, callbacks/observers, launch/lifecycle paths and native/JNI when relevant.
9. A group is not complete until its important dependency graph and subsystem closure dimensions are closed.
10. Use FTS/static semantic discovery first; reserve canonical REA/JADX verification for uncertain or high-risk claims.
11. Preserve exact APK/tool/runtime provenance and fail closed on drift.
12. Before mutating Aerial-Rows remotely, snapshot the full commit SHA plus per-file blob SHAs and analyze only changed files when possible.

## New v7 enforcement
New audits created under process contract v7 receive a closure matrix with these required dimensions:
- entry points
- base classes / inheritance
- all DEX files
- obfuscated collaborators
- models / data flow
- persistence
- resources / XML
- Android framework APIs
- callbacks / observers
- launch actions / lifecycle
- native / JNI

A v7 audit cannot checkpoint `report=complete` until every dimension is either:
- `covered` with at least one evidence reference, or
- `not_applicable` with a written reason.

Older completed audits are grandfathered under the prior `legacy completed report is authoritative` rule; they are not retroactively rewritten just to satisfy a newer schema.

## Reliability protections retained
- Source-report SHA drift detection.
- Target APK SHA drift detection.
- APK signer, multidex inventory and baseline-profile provenance checks.
- Exact JADX JAR, REA launcher and bundled Node binary/version pinning.
- Process-tool bundle hash pinning.
- Atomic state/evidence/final-report writes.
- SQLite WAL + integrity checks.
- Stage-order enforcement and deterministic resume.
- Semantic REA success validation rather than shell-exit-only checks.
- Adaptive bounded timeout backoff.
- SIGTERM/SIGKILL process-group cleanup on timeout.
- Bounded retry only for retryable failures.
- Per-output locks.
- Complete paginated 7,395-class inventory; incomplete inventory fails closed.
- Automatic/explicit Smali fallback.
- Persistent owned/config-checked JADX daemon.
- No aggressive parallel JADX JVMs.
- Hashed canonical evidence manifests with byte size and semantic-success validation.
- Exact canonical evidence-set verification.
- Safe contract re-attestation preserving previous manifests under provenance history.
- Failed/invalid probe quarantine with stderr and hashes.
- Automatic quarantine in bounded probe batches.
- Stale audit/shared transient cleanup.
- Bounded daemon-log rotation.
- Fail-closed per-audit preflight and global machine-readable self-test.

## Performance protections retained
- Persistent JADX loads the APK once for discovery.
- Static semantic index contains class edges, literals and resource references.
- Default-package normalization (`defpackage.X -> X`) for dependency graph queries.
- Exact JADX discovery cache keyed by target/tool hashes and arguments.
- Exact REA evidence cache keyed by target/tool/runtime/heap/operation parameters.
- Verified historical evidence backfilled into current cache-key format.
- Resumable sequential probe batches default to 6 and hard-cap at 12.
- FTS + semantic index are used before expensive raw verification.
- Repository changed-file analysis avoids rereading unchanged Aerial-Rows files.

## Current verification
- Process v7 self-test: PASS.
- APK signer/provenance: PASS.
- DEX inventory: 3 / 3 expected.
- Baseline profiles: all expected files present.
- Projectivy class inventory: 7,395 / 7,395 unique.
- SQLite integrity: PASS.
- Persistent JADX daemon ownership/config: PASS.
- AUDIT_08_R2 preflight: PASS; 80/80 canonical evidence files valid.
- AUDIT_09_R2 preflight: PASS; 53/53 canonical evidence files valid.
- Both old manifests were safely preserved before v7 re-attestation.
- Closure-matrix regression: a new v7 state with pending dimensions fails; all-covered state passes; legacy completed state remains grandfathered.
- Representative REA cache hit: ~0.76 s, versus ~8 s for an uncached JVM/APK load.

## Canonical workflow from now on
1. Global self-test.
2. Initialize/resume exactly one small `GROUP_xx` or `AUDIT_xx` unit.
3. Fail-closed preflight.
4. Verify source/APK/tool provenance.
5. FTS + semantic delta discovery.
6. Update closure matrix as dimensions are actually evidenced.
7. Checkpoint discovery.
8. Run small resumable verification batches; use REA canonically for high-risk conclusions and Smali fallback when decompilation is inadequate.
9. Quarantine failures and preserve failure ledger.
10. Checkpoint verification.
11. Synthesize against prior/later group findings.
12. Checkpoint synthesis.
13. Confirm closure matrix complete.
14. Write final report atomically.
15. Create and verify hashed canonical evidence snapshot.
16. Checkpoint report complete.
17. Run audit preflight and global self-test again.
18. Stop and report before starting the next group.


---

# SOURCE FILE: `audit_process_hardening_v6.md`

SHA-256: `8cc97d337676040ffe9008abfe1db588ac6fbccbdb76499d5788bf72298e822d`

---

# Launcher / Projectivy audit process hardening v6

Status: **ACTIVE PROCESS SPECIFICATION**  
Process contract: **v10**

## Purpose

This document consolidates the previous interruption/reliability fixes and the additional process improvements implemented before and during the Group 9 v3 re-audit.

The goal is unchanged: maximize reverse-engineering completeness while making every stage resumable, provenance-pinned, fail-closed, and efficient enough that repeated evidence is not recomputed unnecessarily.

## Core workflow retained from previous versions

```text
source/APK/toolchain provenance
        ↓
later-report FTS + semantic-index delta discovery
        ↓
small discovery snapshot
        ↓ CHECKPOINT
persistent JADX exploratory analysis
        ↓
small bounded canonical REA batches
        ↓
semantic success validation
   ├─ success → canonical evidence + cache
   └─ failure → quarantine + failure ledger
        ↓ CHECKPOINT
11-dimension closure matrix
        ↓
synthesis
        ↓ CHECKPOINT
atomic final report
        ↓
hashed evidence snapshot
        ↓
preflight + global selftest
```

## Reliability controls

- separate `GROUP_xx` and `AUDIT_xx` checkpoint namespaces;
- deterministic `discovery → verification → synthesis → report` resume;
- completed legacy reports remain authoritative;
- atomic state, evidence, manifest and final-report writes;
- SQLite WAL + FULL synchronous mode + integrity checks;
- exact original report SHA and target APK SHA drift detection;
- APK signer, multidex inventory and baseline-profile provenance;
- pinned REA launcher, bundled Node, JADX JAR, heap policy and process-tool bundle hashes;
- semantic REA-envelope validation, not exit-code/stderr-only validation;
- adaptive bounded timeout, retryable-only retry, process-group cleanup;
- success-only canonical evidence directory;
- failed/invalid probe quarantine with forensic preservation;
- stale temp/shared-workspace cleanup;
- exact evidence cache keyed by operation/parameters/APK/JADX/REA/heap, not by harmless orchestration changes;
- persistent JADX daemon with ownership/config checks and bounded logs;
- complete paginated 7,395-class inventory;
- static semantic index and dependency graph;
- Smali fallback when Java decompilation is unavailable/unreliable;
- remote repository snapshots pinned to full commit SHA + per-file blob SHAs before mutation;
- no aggressive parallel JADX JVMs.

## Machine-enforced subsystem closure

New audits cannot complete unless all dimensions are either `covered` with evidence or explicitly `not_applicable` with a reason:

1. entry points;
2. base classes / inheritance;
3. all DEX files;
4. obfuscated collaborators;
5. models / data flow;
6. persistence;
7. resources / XML;
8. Android framework APIs;
9. callbacks / observers;
10. launch actions / lifecycle;
11. native / JNI.

Closure enforcement was introduced in contract v7 and remains active for all later contracts. The invariant now correctly requires the introduction version to be <= the current contract version rather than equal to it.

## Improvements added in this Group 9 cycle

### 1. Persistent MCP session reuse inside discovery batches

The persistent JADX daemon already avoided APK reloads, but each discovery CLI process still performed its own MCP initialization.

`jadx_fast.py` now reuses one MCP session inside a process and `audit_batch.py` can run JADX discovery in-process for a bounded batch.

Representative forced two-query benchmark:

```text
old separate-process path: ~1.54 s
new in-process/session path: ~0.81 s
```

The old isolated subprocess path remains an automatic fallback if the fast path fails.

### 2. Process-contract versioning bug fixed

The closure self-test previously assumed the current contract version had to equal the version that introduced closure enforcement. That made any later process upgrade appear invalid.

The invariant is now:

```text
1 <= closure_enforcement_from_contract_version <= current_contract_version
```

while audit creation version still determines whether the closure matrix applies.

### 3. Safe literal FTS fallback

FTS5 treats punctuation such as dots in package/property names as query syntax. Queries such as:

```text
com.android.vending
sys.cts.running
com.xiaomi.mitv.upgrade
```

could therefore throw a SQLite syntax error.

`auditctl search` now:

1. tries the supplied FTS expression unchanged so `AND`/`OR` and quoted expressions still work;
2. on FTS syntax failure, retries the entire input as a safely quoted literal phrase.

Search remains read-only.

### 4. REA named-class target prevalidation

A misspelled named class previously spent a full REA/JADX startup before returning `invalid_request`.

Canonical batches now prevalidate non-`defpackage` exact class names against the pinned complete 7,395-class inventory before JVM startup.

Default-package helpers deliberately pass through because JADX can expose synthetic/folded helpers to class/method lookup even when `list_classes` omits them.

This would now reject a typo such as:

```text
com.spocky.projengmenu.receivers.StartUpBootReceiver
```

immediately, while accepting the real:

```text
com.spocky.projengmenu.services.StartUpBootReceiver
```

without weakening support for helpers such as `defpackage.cm3`.

### 5. Cache behavior verified after orchestration upgrades

REA cache fingerprints depend only on analysis-affecting state:

```text
operation + params + APK SHA + JADX SHA/version + REA version + heap
```

They do not depend on the overall process-bundle hash, so changes to checkpoint/search/batch orchestration do not invalidate valid decompilation evidence.

Representative canonical cache hit after the v10 changes:

```text
~0.68 s
```

versus roughly 7–9 seconds for a fresh REA/JADX load.

## Current integrity state

At the time of this document:

```text
Process contract                  v10
Global self-test                  PASS
SQLite integrity                  PASS
Persistent JADX daemon            PASS
Complete class inventory          7,395 / 7,395 unique
Stale temp files                  0
Shared transient workspaces       0

AUDIT_08_R2 snapshot              PASS (80 evidence)
AUDIT_09_R2 snapshot              PASS (53 evidence)
AUDIT_09_R3 snapshot              PASS (19 evidence)
AUDIT_09_R3 closure               PASS (11/11)
```

Process bundle SHA-256:

`e949e7a3d252ad0688a925aaae97f709771245307afb40de0a9cf5c7dfb30ca1`

## Operational rule going forward

Discovery should use, in order:

```text
existing reports / evidence
    ↓
FTS + static semantic index
    ↓
persistent JADX batch discovery
    ↓
REA only for claims that need canonical verification
```

This minimizes repeated JVM/APK work without lowering the evidence standard.


---

# SOURCE FILE: `audit_process_hardening_v8.md`

SHA-256: `2f648cf10e0fec1d0e15abadba7b150bb9a03762b6d9a68144f4b86d79e4f0c1`

---

# Audit Process Hardening v8 — Connection-Interruption / Last-Mile Recovery

## Failure diagnosis

The interrupted Group 10 run did **not** lose or corrupt reverse-engineering work. Durable state proves that `AUDIT_10_R2` had already completed discovery, verification, synthesis and report before the visible conversation stopped:

- `resume_from = none`;
- 23/23 canonical REA evidence files validate successfully;
- all 11 closure dimensions are covered or explicitly not applicable;
- the final Group 10 v2 report exists with a matching SHA-256 sidecar;
- the evidence snapshot passes;
- no stale audit temp files or orphaned REA/JADX analysis workers remain.

The exact browser/platform transport cause is not observable from the analysis container. The durable timeline therefore supports a **last-mile chat/response interruption after analytical completion**, not an evidence or checkpoint failure.

## Secondary orchestration weakness found

The old last-mile path kept doing expensive work after the report was already durable:

1. snapshot verification;
2. preflight (which verified the same snapshot again);
3. a full global self-test;
4. verification of every historical completed audit;
5. serialization of the full APK resource inventory in diagnostic output.

That could add 15+ seconds of duplicated work after the result was already ready, which is exactly the wrong place to risk a chat interruption.

A separate diagnostic command also combined several historical re-attestations and a full self-test into one outer shell invocation. Individual operations were healthy, but the combined command exceeded the outer execution window.

**Rule:** critical handoff operations are now individually bounded and are never chained behind historical maintenance or verbose diagnostics.

## Process contract v18

The process now has an explicit durable completion boundary plus an O(1)-style recovery path.

New/strengthened policies include:

- completion receipt is the canonical user-handoff record;
- finalisation runs immediately after report completion;
- current audit snapshot is verified once, inside preflight;
- compact **core** self-test is used during handoff;
- full/deep self-test is separate maintenance and may verify all completed audits;
- self-test still writes full diagnostics to disk but can print a compact summary;
- batch state is written **before** entering an expensive REA/JADX call;
- an interrupted batch therefore records the exact `in_progress` item, attempt count and recovery count;
- a fast `ready` command validates an existing completion receipt and skips expensive rechecks when nothing changed;
- receipt validity is tied to report/state/manifest hashes, process contract SHA/version, process-tool bundle SHA and target APK SHA.

## Write-ahead batch recovery

Before every expensive probe the batch runner atomically records:

- item id;
- `status = in_progress`;
- output path;
- start time;
- attempt count;
- recovered-interruption count.

A regression test simulated a killed probe by leaving an item `in_progress`. The next run recovered it correctly:

    attempts                         2
    recovered_interrupted_attempts  1
    final status                     complete

The retry completed from the persistent JADX cache.

## Compact finalisation

Canonical command:

    python3 auditctl.py finalize AUDIT_NAME --report /path/to/report.md --process-doc /path/to/process.md

It refuses to create a handoff receipt unless:

1. report stage is complete and `resume_from=none`;
2. report SHA-256 sidecar matches the report bytes;
3. current audit preflight passes;
4. the completed evidence snapshot passes inside preflight;
5. compact core runtime/toolchain self-test passes;
6. optional process-document SHA sidecar matches.

It atomically writes:

- `audits/<AUDIT>/completion_receipt.json`;
- `audit_system/last_completion_receipt.json`.

Measured Group 10 v18 finalisation: approximately **8.07 seconds** total.

## Fast interruption recovery

First command after any suspected interruption:

    python3 auditctl.py ready AUDIT_NAME

It validates:

- completion receipt exists and is marked ready;
- report hash;
- state hash;
- evidence-manifest hash;
- current process contract version and SHA;
- current process-tool bundle SHA;
- target APK SHA.

If all match, the result is safe to hand to the user immediately without repeating preflight or self-tests.

Measured Group 10 recovery: approximately **0.79 seconds**.

If the receipt is stale or absent, normal deterministic recovery applies: inspect `state.json` / `resume_from` and continue only the unfinished stage/item.

## Deep maintenance remains available

A full self-test still verifies all completed audit snapshots and remains mandatory after process/tool changes before those snapshots are re-attested.

Current full/deep self-test time: approximately **10.49 seconds** for four completed audits.

Current core handoff self-test time: approximately **3.8 seconds**.

## Current stable health

    Process contract          v18
    Full/deep self-test       PASS
    SQLite integrity          PASS
    Persistent JADX           PASS
    Class inventory           7,395 / 7,395
    APK/resource provenance   PASS
    Stale audit temps         0
    Shared transients         0
    Completed snapshots       4 / 4 PASS

Current process contract SHA-256:

`052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`

Current process-tool bundle SHA-256:

`251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

## Group 10 state

`AUDIT_10_R2` remains complete and does not need to be rerun:

- 23/23 canonical evidence files successful;
- 0 canonical errors/invalid records;
- 11/11 closure dimensions closed;
- report snapshot verified;
- final report SHA-256: `d75640cc4a23d2a7d392ee3322a50d59c942fad9c09094b4e38a58c130d2c126`.

---

# SECTION H — Manual Upload Instructions
# Manual Upload Instructions

The simplest durable repository setup is:

## Recommended

Upload `AI_HANDOFF_COMPLETE.md` to the repository root (or `docs/AI_HANDOFF_COMPLETE.md`). This is a single searchable Markdown file containing the project context, final blueprint, chat reconstruction, all Couchy C1–C12 reports, all Projectivy G1–G12 reports/post-closure audits, process state, and export continuation.

Also upload `Aerial-Rows-AI-Handoff-Complete-2026-10-23.zip` somewhere under `docs/ai-handoff/` as the lossless backup bundle if desired.

## Future AI prompt

Tell the new AI:

> Read `AI_HANDOFF_COMPLETE.md` first. Then verify the current `aerialrows-development` branch. The research programme is complete; continue from Phase 0A unless the handoff says otherwise.

## Why both files

- Markdown is directly searchable/readable by GitHub-connected AI tools.
- ZIP preserves the full file hierarchy and exact individual reports as a compact backup.
