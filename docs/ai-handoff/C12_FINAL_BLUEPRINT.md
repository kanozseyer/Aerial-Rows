# C12 Final Blueprint — Couchy ↔ Projectivy Synthesis

Status: **COMPLETE + REVIEWED**  
Research baseline: `aerialrows-development` @ `3c1cfd56234f1d386f4553dd6c69523670500d70`  
Tree: `457f4ab1d08a8d545bdf5545777cd31f907ae24d`

## Final verdict

Aerial-Rows remains a **Couchy-derived Compose launcher**. Projectivy is the behavioral/subsystem reference, not the source implementation base.

Winning combination:

- Couchy: Compose rendering, small MainActivity, typed immutable config, efficient app scan/artwork cache, TV D-pad UX, SurfaceView/Media3 wallpaper, low background overhead.
- Projectivy: mature row/focus semantics, launch/policy boundary, richer Android TV Provider behavior, wallpaper provider/renderer separation, settings metadata/effects, media/notifications, TV inputs, startup/remotes, backup/theme/icon/shortcut systems, optional parental/device integration.
- Remove Couchy weaknesses: build-time source rewrite, package-only IDs, `movePkg`, UI-owned persistence/effects, snapshot-only TV content, insecure Aerial transport, raw-config backup, missing lifecycle repositories.
- Avoid Projectivy weaknesses: Leanback-specific implementation, Accessibility ownership sprawl, vendor/root helpers in core, polling weaknesses, preference-heavy state, opaque native/bootstrap/licensing internals, inconsistent policy coverage.

## Target dependency model

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

Parallel coordinators:

- WallpaperEngine
- SettingsController
- HomeFocusCoordinator
- HomeRailController
- StartupCoordinator
- RemoteGestureEngine
- LauncherIdleCoordinator

Optional adapters:

- AccessibilityBridge
- VendorCompatibilityRegistry
- PictureBackendRegistry
- ParentalPolicy
- ExpertTools

Core dependency direction is `UI -> feature/domain -> core interfaces/state -> platform adapters`. Vendor/root/accessibility implementations must not become core dependencies.

## Identity model

Use semantic stable IDs:

- AppId = package + component + user/profile.
- RowId = stable logical row identity.
- HomeItemId = typed semantic item identity.
- TV channel = provider package + internal provider ID/fallback fingerprint.
- TV program = provider + semantic program key.
- InputId = platform TIF input ID plus compatibility metadata where required.
- ShortcutId = package + shortcut ID + user/profile.

Never persist visible labels, list indices, row positions or array ordinals when stronger identity exists.

## Generic home model

```text
HomeRow
  id: RowId
  title
  presentation: RowPresentation
  items: List<HomeItem>

HomeItem
  App
  TvProgram
  NowPlaying
  Input
  Shortcut
  EmptyRowAction
  optional Folder/Custom
```

Focus state:

```text
HomeFocusState
  activeRowId
  activeItemId
  lastItemByRow
```

The left rail needs explicit state/focus ownership rather than one `railVisible` Boolean. Reuse Couchy's existing custom BringIntoView pivot geometry inside Sliding Rows. Configured user rows that are empty remain visible through an `EmptyRowAction`; ephemeral empty provider rows can disappear.

## Launch model

All launcher-originated opens converge on:

```text
AppCard / TV Program / NowPlaying / Input / Shortcut /
Notification / RemoteAction / StartupCoordinator
        -> LaunchDispatcher
        -> LaunchPolicy
        -> platform-specific launcher
```

Start with `AllowAllLaunchPolicy`. Optional parental protection later plugs into this boundary instead of being spread across cards/Activities.

## Persistence/config model

Keep a typed root config envelope rather than hundreds of preference keys:

```text
ConfigEnvelope(schemaVersion, LauncherConfig)
```

As it grows, organize LauncherConfig into domain sub-configs such as appearance, home, apps, wallpaper, TV content, status, media, inputs, remote and system.

Add:

- ConfigDefaults
- ConfigNormalizer
- MigrationRegistry
- LastKnownGoodStore

A setting descriptor reads/writes typed config; it is not a preference key. Separate PersistencePolicy from SettingEffect.

## Runtime observation rule

Prefer:

`platform callback/observer -> repository -> StateFlow immutable snapshot -> UI`

Examples:

- LauncherApps.Callback -> apps/shortcuts
- ContentObserver -> TV provider data
- TvInputManager.TvInputCallback -> inputs
- NotificationListener -> notifications
- MediaController callbacks -> active media sessions
- Connectivity callbacks -> network status

Polling only when no event API exists, and only while the feature needs it.

## Demand-driven rule

Disabled optional feature means its expensive work is disabled. Examples: no active media-controller observation when no consumer needs it; no display-profile poll when automation off; no TV program load when Preview/Watch Next off; no Aerial network/player work when Aerial off; Accessibility never required for normal launcher use; icon packs parsed only when selected.

## Phase 0A — source-pure baseline

Mandatory first implementation phase.

1. Pin current baseline as recovery point.
2. Materialize all Gradle-injected `LauncherScreen.kt` changes.
3. Materialize all injected `SettingsSheet.kt` changes.
4. Materialize strings/resources.
5. Remove `applyAerialRowsSourcePatches`.
6. Build from clean checkout.
7. Assert build does not change Git working tree.
8. Verify debug, release and lint.

No feature work before this gate.

## Phase 0B — performance/reliability baseline

Before large refactors add test + benchmark infrastructure and record current behavior:

- cold process HOME
- warm HOME
- first focusable card
- horizontal row navigation
- vertical row navigation
- settings open/close
- Aerial + navigation
- memory at representative app counts

Keep current hand-authored baseline profile until a generated/validated profile exists.

## Phase 1 — config safety, identities, launch core

Fix current correctness first:

- one canonical localized ConfigDefaults factory;
- zero-category invariant;
- malformed JSON recovery without destructive overwrite;
- last-known-good state;
- ArtworkVariant memory-cache identity.

Then add schemaVersion, ordered migrations, AppId/RowId/HomeItemId and `LaunchDispatcher + LaunchPolicy + AllowAllLaunchPolicy`. Route existing app launch through it without changing visual behavior.

## Phase 2 — generic home model + focus

Promote Sliding Rows private types into `HomeRow`, `HomeItem`, `RowPresentation`, `HomeRowsRepository`, `HomeFocusCoordinator`, `HomeRailController`.

Implement active row/item and per-row last item. Reuse Couchy pivot scrolling. Key rail entries. Add EmptyRowAction. Gate: rail reveal/close restores exact item; reorder preserves stable focus.

## Phase 3 — apps/categories/actions

Keep Couchy's scan/artwork path. Add component-aware AppId, LauncherApps callbacks, AppClassifier/placement policy, `MovingItem(rowId,itemId)`, sort modes/new-install policy, row-local exclusions and generic HomeItemActionProvider. Fix stale package-change case, multi-row move bug, global-only hide and category referential integrity.

## Phase 4 — Android TV Provider completion

Move provider state out of UI-owned `produceState` into repositories. Add logical channel/program IDs, non-browsable channel discovery + `requestChannelBrowsable`, Preview program weight ordering, Watch Next engagement ordering, provider cap/exclusions, ContentObserver + debounce, resume refresh, provider-constrained launches and TV artwork repository.

## Phase 5 — wallpaper/Aerial engine

First remove trust-all TLS and permissive hostname verification.

Then introduce typed WallpaperSelection, WallpaperEngine, providers, descriptors, shared ImageRenderer/VideoRenderer, rotation scheduler, failure registry and semantic events while preserving Couchy's SurfaceView, small buffer, first-frame gate, fades, ON_STOP release and pseudo-blur.

Add audio-track disable at zero volume, optional tunneling, configurable decoder fallback, typed Aerial metadata, shuffle bag and failed-item quarantine. Aerial/local video/immersive idle share one renderer.

## Phase 6 — settings architecture

Add SettingId, SettingDescriptor, SettingsNavigator, SettingsController and SettingEffectExecutor. Merge duplicated controls. Restore exact originating setting after child route. Add choice/dropdown panel for named choices. Use immediate session preview + coalesced persistence. Fix backup save through SAF and truthful failure reporting.

## Phase 7 — status, notifications, media

Extract ClockRepository and NetworkStatusRepository. Add thin NotificationListener bridge, NotificationRepository/Policy/Panel, MediaSessionRepository and NowPlayingHomeRow/Card. Use platform MediaSession APIs. Multiple sessions may exist; primary session is separate. Now Playing launch passes through LaunchPolicy. Notification dismissal does not stop playback by default.

## Phase 8 — physical TV inputs

Add InputRepository, TvInputManager callback, TvInputSource/InputId, InputHomeRow, TvInputActivity/TvView, InputLaunchCoordinator and CurrentInputState. Separate DiscoveredInputs/AvailableInputs/VisibleInputShortcuts. Standard TIF path first. Requested/launched/confirmed state remains distinct. Vendor strategies/display profiles later and optional.

## Phase 9 — HOME/startup/remotes/idle

Extract HomeRoleCoordinator. Add StartupCoordinator, RemoteGestureEngine, LauncherIdleCoordinator and PowerStateMonitor. Keep ordinary D-pad/AppCard/move behavior UI-local. No boot receiver merely to launch HOME. Accessibility only for optional capabilities that actually need it. Immersive idle reuses shared wallpaper renderer.

## Phase 10 — backup/themes/icons/shortcuts

Add BackupEngine, ThemeEngine, IconRepository and ShortcutRepository.

Full backup and theme are separate products. Caches are never exported. Full restore is transactional: verify -> decode -> migrate -> normalize -> stage assets -> commit -> one runtime rehydrate. Support custom assets, icon pack mapping/cache, per-item ArtworkPolicy, Android shortcuts and unresolved restore references.

## Phase 11 — optional advanced parity

Only after core stability: parental controls, display profiles, vendor input adapters, optional Accessibility global mappings, external wallpaper providers, diagnostics/expert tooling.

Explicitly not core: Projectivy licensing/protection, opaque libptutils bootstrap, root/GApps removal, Play immutable-APK hacks, vendor CTS-property workarounds by default, Projectivy Web Remote clone.

## Performance gates

Re-run performance checks after major phases (at least 0, 2, 4, 5, 7, 9, 10). Track cold/warm startup, first focusable card, horizontal/vertical frame timing, PSS/RSS, Aerial + navigation and settings latency. Fix meaningful regressions before continuing.

## Performance invariants to preserve

- bounded app scan concurrency;
- immutable `AppEntry` reuse;
- same-list reuse when unchanged;
- target-size artwork;
- `prepareToDraw()`;
- stable Compose keys;
- SurfaceView video;
- trimmed Media3 buffering;
- decoder release on stop;
- pseudo-blur;
- release shrinking/minification;
- optional/background work demand-gated.

Do not redesign full artwork residency or complete cold-scan startup gate before measurement demonstrates a problem.

## Recommended first implementation wave

1. Materialize Gradle-injected source/resources.
2. Remove source-rewriting task + source-tree cleanliness CI.
3. Add unit test skeleton + benchmark/profile harness.
4. ConfigDefaults/corruption/category invariant fixes.
5. schemaVersion + LastKnownGoodStore + MigrationRegistry.
6. ArtworkVariant cache fix.
7. AppId / RowId / HomeItemId.
8. LaunchDispatcher + AllowAllLaunchPolicy.
9. Promote Sliding Rows model to HomeRow/HomeItem.
10. HomeFocusCoordinator + exact stable row/card restoration.

This is the point at which implementation should begin. No further general research group is required.
