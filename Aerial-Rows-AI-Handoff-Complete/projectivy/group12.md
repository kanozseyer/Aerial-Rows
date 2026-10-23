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
