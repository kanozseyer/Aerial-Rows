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
