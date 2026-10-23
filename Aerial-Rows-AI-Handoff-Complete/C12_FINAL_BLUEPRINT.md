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
