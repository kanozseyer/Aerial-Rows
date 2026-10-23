# Couchy / Aerial-Rows Detailed Findings — C1 to C12

Research baseline: `aerialrows-development` @ `3c1cfd56234f1d386f4553dd6c69523670500d70`.

This file preserves the subsystem findings that drove the final implementation blueprint. It is intentionally detailed so a fresh contributor does not need to repeat the comparison programme.

## C1 — Build, manifest, lifecycle, outer architecture

Couchy/Aerial-Rows uses Kotlin + Compose, Java 17, minSdk 21, targetSdk 34 at the audited commit. It is a substantially smaller Android component graph than Projectivy.

The manifest baseline effectively centers on one exported `MainActivity` registered as HOME/DEFAULT/LEANBACK_LAUNCHER/LAUNCHER. It requests launcher-related permissions including network state, TV listings, package visibility/delete/background-process behavior. There is no Accessibility service, NotificationListener, boot receiver, wallpaper provider, theme provider or input service in the baseline.

`MainActivity` is deliberately small. It requests TV listings permission, registers package change events, applies persisted locale, consumes Back at HOME root and hosts Compose. Preserve this small Activity; future startup/input/accessibility/media work belongs in dedicated repositories/coordinators/services.

`ConfigStore` stores one serialized immutable `LauncherConfig` JSON value in Preferences DataStore. This is a cleaner persistence foundation than Projectivy's many preference descriptors. Keep typed config/DataStore and add schema/migrations/rollback rather than replacing it.

`AppRepository` is a major implementation strength: Leanback + regular launcher scans, package dedupe, bounded parallelism, memory cache, reuse of unchanged `AppEntry`, target-sized artwork cache keyed by package/update stamp/size, `prepareToDraw()`, stale cache sweep, precomputed focus/tile colors, same list instance if content is unchanged.

`Actions` is a useful platform boundary but direct callers should eventually use `LaunchDispatcher -> LaunchPolicy -> Actions/platform launcher`.

## C2 — Home screen, rows, focus, rail

Effective build architecture is split between checked-in source and Gradle source mutation. `SlidingRows.kt` exists, but raw `LauncherScreen.kt` does not fully express the built Sliding Rows branch; the Gradle patch inserts it. This is the Phase 0A blocker.

`LauncherApp()` currently coordinates config, app scan/loading, setup wizard, locale, network/time/date, new installs, menu/move state, category derivation, layout calculations, wallpaper/Aerial, status, settings and overlays. This is too broad for future feature growth. Keep Compose but gradually move runtime state into repositories/controllers.

Sliding Rows currently has private `HomeRowModel` variants:

- `WatchNextHomeRow`
- `ChannelHomeRow`
- `CategoryHomeRow`

Each has stable string ID/title/icon. `homeRowOrder` stores IDs and is normalized against currently available rows. This is a good base, but the model needs to become layout-independent and extensible for Now Playing, Inputs, Shortcuts, folders/actions.

Target:

```text
HomeRow(id, source, presentation, items)
RowSource: Apps / WatchNext / TvChannel / NowPlaying / Inputs / Shortcuts / Folder / InternalActions
HomeItem: App / TvProgram / NowPlaying / TvInput / Shortcut / Folder / InternalAction / EmptyRowAction
```

Current stable identity is good at row level, but card focus is not persisted per row. `activeRowId` + first-card FocusRequester means rail selection/closing goes to first item. Projectivy behavior demonstrates stable row+card restoration. Add:

```text
HomeFocusState
  activeRowId
  activeItemId
  lastItemByRow
```

Current rail is `railVisible: Boolean`. First Left shows rail while content retains focus; second Left requests rail focus. Target should model explicit pane state/focus ownership (Disabled, Hidden, Revealing, VisibleFocused, Hiding or equivalent) so reveal + transfer is one coordinated action.

Rail rows are currently emitted with `forEachIndexed` in a normal Column and `RailItem` has remembered focus state. Reorder therefore risks positional slot identity. Wrap rail entries in `key(row.id)` and explicitly restore active rail focus after reorder.

Couchy's older Carousel/Grid implementation already contains excellent custom `BringIntoViewSpec` behavior: vertical pivot beneath the status region, horizontal card pivot near the left edge, and `focusRestorer`. Reuse/extract those specs for Sliding Rows rather than reinventing focus geometry.

Current `LauncherApp` filters empty categories before Sliding Rows. This avoids dead focus traps but makes empty user rows impossible to quick-edit. Distinguish ephemeral empty provider rows (hide) from configured user rows (keep with `EmptyRowAction` such as Add apps).

Per-row presentation is mostly global today. Add `RowPresentation` overrides keyed by stable RowId for visibility/title/card scale/margins/layout/items-per-line/max rows/background where needed; keep globals as defaults.

Keep `AppCard`: TV Surface focus scaling, sampled artwork glow, precomputed colors, own graphics layer, long-press release suppression, moving border, hidden overlay. Later add one shared selected-item/hover details surface driven by active item ID.

`SlidingRowsArea` rebuilds natural rows/IDs/rank/sorted rows on recomposition. Small today, but can move to remembered/domain-prepared `HomeRowsSnapshot`.

## C3 — Apps, categories, context actions

Keep Couchy's scanner/cache/render pipeline. It is one of the reasons Couchy is the implementation base.

Concrete correctness problems found:

1. `PACKAGE_CHANGED` can leave a stale app. The dynamic package receiver can decide the package is no longer launchable and skip the scan path that would remove it. Prefer `LauncherApps.Callback` and precise invalidation plus full reconciliation at safe points.

2. Move mode stores only `movePkg`. Because an app can belong to several sections, `findMoving()` can resolve the first occurrence rather than the one long-pressed. Duplicate cards with the same package can all see themselves as moving and share the same FocusRequester. Replace with `MovingItem(rowId,itemId)`.

3. Zero categories is reachable. Delete logic only guards total category count; All Apps can be last and then be disabled, leaving an empty structural category list. New-install code can later call `categories.first()`. Enforce at least one editable user category; treat All Apps as generated/system row rather than structural fallback.

4. App identity is package-only after discovery. The original ResolveInfo/activity is discarded, and launch resolves package launch intent again. A package with several launchable Activities can display label/art from one and launch another. Use component-aware AppId.

5. Current `hidden: Set<String>` is global. Keep global Hide Everywhere as an explicit action but add row-local exclusion/membership semantics.

6. New-install behavior is implicit. Existing apps may auto-classify while later installs can be forced into first section. Add explicit NewInstallPolicy/AppPlacementPolicy.

Context menu currently provides Open, Move, Hide/Unhide, App Info, Close, Uninstall. The long-press dialog correctly swallows the initiating select-key repeats/release so the app is not launched behind the menu. Preserve that UI behavior. Generalize action generation into `HomeItemActionProvider` so future TV/media/input/shortcut actions don't inflate an app-only function signature.

`Close` is best-effort `killBackgroundProcesses`, not force-stop. Keep it optional and describe semantics honestly.

Sort modes can evolve from current manual/install ordering to Manual/Install, Alphabetical, Last Used, Most Used, Random.

## C4 — Android TV Preview Channels and Watch Next

Current `TvContentRepository` is a useful minimal reader but not a complete provider subsystem.

Findings:

- Channel query filters `COLUMN_BROWSABLE == 0` before data reaches settings. The user therefore cannot see exactly the channels that need enabling. Projectivy behavior supports discovering them and calling `TvContract.requestChannelBrowsable()`.
- Discovery drops a channel if accepted programs are empty. Discovery/management identity should not depend on current content count.
- Preview Programs have no explicit sort. Respect provider weight (`weight DESC` behavior).
- Watch Next has no explicit engagement/recent ordering.
- Watch Next total limit has no per-provider cap/exclusion, so one app can dominate.
- Channel key uses `packageName:channelId`. Query `internal_provider_id` and prefer provider package + logical provider ID for durable identity; numeric DB ID is a current lookup handle.
- `SlidingRowsArea` owns a `produceState` snapshot keyed only by TV config fields. There is no ContentObserver or resume refresh, so provider updates can stay stale.
- `Actions.launchTvContent()` parses and starts provider intent directly; it does not constrain ownership/package or pass through future LaunchPolicy.

Keep Compose TV cards/progress/per-channel limits/master switches. Add `TvChannelRepository`, `PreviewProgramRepository`, `WatchNextRepository`, observer/debounce/resume refresh, provider policy and `TvArtworkRepository`.

TV artwork currently decodes source bitmap then scales to max card size with a small entry-count LRU. Future artwork repository should pre-sample decode and use a byte-sized memory budget.

Provider content is not physical TV input; keep C4 and C8 separate.

## C5 — Wallpaper and Aerial

Couchy's current `VideoWallpaper` is one of the strongest implementation pieces and should survive extraction almost intact.

Keep:

- Media3 ExoPlayer
- `SurfaceView`, not TextureView, so SurfaceFlinger handles composition independently of UI-thread focus/scroll work
- player release on `ON_STOP`
- rebuild/reprepare on return
- custom small buffer approximately 10s min / 15s max / 2.5s start / 5s rebuffer
- `onRenderedFirstFrame()` gate before revealing video
- fade current clip out, prepare behind cover, fade new first frame in
- gradient cover rather than black frame
- local video streaming via content URI
- static wallpaper downsampling
- RGB_565 where acceptable
- `prepareToDraw()`
- tiny ~96px pseudo-blur image upscaled for Dock instead of expensive real-time full-screen blur

Critical security issue: current shared OkHttp client uses a no-op X509TrustManager and always-true hostname verifier for Aerial streams. Remove this. Decorative media is still untrusted input into container/codec stacks. Use normal HTTPS trust; fail/disable bad legacy endpoint instead of globally trusting all certs/hosts.

Effective build has two Aerial source models: `LauncherConfig.builtinSources: Set<String>` exists, but checked-in Settings/Launcher paths still use legacy `builtinSource` and Gradle patch rewrites them. Materialize `builtinSources`; retain legacy field only for migration/backup decode.

Subtle patch dependency: the patched load call uses `builtinSources`, but `produceState` may still be keyed by legacy `builtinSource`. Collection changes therefore don't necessarily restart selection until clip end/10-minute timer/error. If finish-current-first is desired, make it an explicit playlist policy.

`BuiltinAerials` asset contains title/location, URL and source code (Apple/Amazon/Community collections), but loader returns URL strings and discards metadata. Use typed `AerialItem(id,title,sourceId,uri,metadata)`.

Current muted player sets `volume=0` but still allows audio decode. Disable audio track type when volume zero. Make decoder fallback and tunneling explicit renderer options. Built-in curated Aerials should prefer hardware/predictable path; local video can offer compatibility fallback.

Current 2.5s first-frame stall watchdog is useful but conflates network/HTTP/decoder/surface failures. Add structured `FailureRegistry`, bounded backoff and per-item quarantine.

Replace random selection with shuffle bag so every enabled candidate plays before reshuffle without immediate cross-bag repeat.

Replace clip-end/10-minute hard-coded rotation with `WallpaperRotationScheduler` and explicit policy (`ON_MEDIA_END`, `FIXED_INTERVAL`, `MEDIA_END_OR_MAX_INTERVAL`, `MANUAL_ONLY`).

Local photo import/copy and downsample is good; add EXIF/orientation and image compatibility policy (min width, fit/crop/blur/stretch/skip). Local video `takePersistableUriPermission()` failure is currently ignored; detect it and copy privately or report unsupported provider instead of storing an unreliable URI.

Target engine separates source/provider policy from renderer/playback:

```text
WallpaperEngine
  ProviderRegistry
    PresetGradientProvider
    LocalPhotoProvider
    LocalVideoProvider
    BuiltinAerialProvider
  EventBus
  RotationScheduler
  CandidateCache
  FailureRegistry
  RendererRegistry
    ColorRenderer
    ImageRenderer
    VideoRenderer
```

One shared VideoRenderer must serve Aerial/local video/future immersive idle.

## C6 — Settings architecture

Keep Couchy's typed `LauncherConfig`, DataStore and TV side-panel Compose UX. Keep D-pad-first rows, no focus-scale on text rows, immediate visual previews, dedicated text-entry dialogs, full-height right-side dialog and density handling.

Current architecture is too implicit: almost every row calls `store.update { it.copy(...) }`, so UI owns storage, validation, dependent values and runtime effects. Add a descriptor/controller layer:

```text
SettingDescriptor<T>
  id/title/description/control
  read/write typed config
  validate
  visibleWhen/enabledWhen
  PersistencePolicy
  SettingEffect
  ExportPolicy
```

Separate persistence timing (`IMMEDIATE`, `DEBOUNCED`, `ON_CONFIRM`, `SESSION_ONLY`) from runtime effect (`NONE`, `RECOMPOSE`, `RELOAD_APPS`, `RELOAD_TV_CONTENT`, `RELOAD_WALLPAPER`, `RECREATE_ACTIVITY`, `RESTART_HOME_TASK`, etc.).

Current focus systems have drifted:

- main Settings retries focus over several frames;
- Aerial extras use a separate one-shot helper;
- RowQuickEditor has another focus behavior.

Unify controls/focus. Add stable `SettingId` and `SettingsNavigator(routeStack,lastFocusByRoute)`. Returning from Display should focus Display, not the first root item. If a conditional row disappears, focus nearest next/previous valid ID deterministically.

Current selector behavior Left=previous, Right=next, OK=next works for small ranges. For named/many choices (layout/language/provider/input/remote action), OK should open a choice/dropdown-style dialog focused on current value and return focus to originating SettingId.

Rapid settings changes currently serialize full LauncherConfig through DataStore on every D-pad repeat. Use `SettingsSessionState` for immediate preview and 150–300ms-ish coalesced persistence, flushing on close/background/confirm. Critical one-shot settings remain immediate.

Save Config bug: direct `Downloads/CouchyBackup.json` File write is inside runCatching but success toast is unconditional. Use SAF `CreateDocument`, OutputStream, truthful success/failure.

Validation currently happens partly in normalize and partly with consumer-side `coerceIn`. Validate at descriptor + whole-config normalizer + migration levels so stored model is sane.

## C7 — Status, notifications, media

Current status domain is only Wi-Fi/Ethernet/VPN plus root time/date loops. No NotificationListenerService, notification repository or MediaSession code exists in baseline.

Current `networkStatusFlow()` enumerates `ConnectivityManager.allNetworks` on every callback and defines connected as Wi-Fi-or-Ethernet. Split transport presence from actual/validated Internet and maintain Network->Capabilities incrementally, emitting only changed aggregate state. Aerial retry policy should use validated Internet rather than transport icon state.

Merge separate clock/date minute loops into `ClockRepository/MinuteTicker` reacting to minute boundary, ON_RESUME, time changed, timezone changed and date changed broadcasts.

Keep StatusBar visual structure and hidden ~4dp focus/reveal strip. Add stateful accessibility content descriptions.

Add thin `LauncherNotificationListenerService` whose only role is platform callbacks/bridge. Domain `NotificationRepository` owns typed notification list/count/dismiss/clear state. Filtering belongs in testable `NotificationPolicy`, grouping in domain/presentation state, not service/UI tree surgery. Notification open goes through LaunchDispatcher/Policy using owner package as policy identity. Dismissing a notification does not stop playback by default.

Now Playing must come from platform MediaSessions, not notification text. Use `MediaSessionManager.getActiveSessions(notificationListenerComponent)` and platform `MediaController/MediaMetadata/PlaybackState` rather than adding Media3 Session just to observe.

`MediaSessionRepository` supports several active sessions and a separate primary session (first PLAYING, else first active). Reconcile controllers by token and retain wrappers/callbacks where possible. Coalesce metadata/state callback bursts. Expose transport actions only if `PlaybackState.actions` supports them.

Session identity is runtime/ephemeral (package + token). Do not persist customization against session tokens. Artwork may come from URI or embedded bitmap and should be bounded/downsampled.

Now Playing is a normal HomeRow, not overlay. Card open: LaunchPolicy owning package -> sessionActivity PendingIntent -> package launcher fallback. Derived media protection inherits owner app/package if parental policy exists later.

Media repository may feed WallpaperEvent.NowPlayingChanged and `isAnyPlaying` to idle coordinator without owning those subsystems.

## C8 — Physical TV inputs and device integration

No physical input subsystem exists in Couchy baseline. This was a clean-sheet design.

Use public `TvInputManager` snapshot plus `TvInputManager.TvInputCallback` for add/remove/update/state, plus resume safety refresh and bounded recovery after abnormal empty discovery.

Domain `TvInputSource` includes stable InputId/platform ID/type/label/package/state/passthrough/parent/capabilities. Preserve unknown/vendor input types rather than dropping them.

Keep three distinct collections:

- DiscoveredInputs = Android/vendor discovery
- AvailableInputs = launchable sources including explicit compatibility source when justified
- VisibleInputShortcuts = user's home projection

Hiding shortcut must not remove input from startup/remote/display-profile settings.

Do not synthesize individual HDMI holes when partial authoritative list exists (e.g. HDMI1+HDMI3). Compatibility synthesis only when passthrough discovery is completely unavailable and a tested adapter/user override supports assumptions.

Standard launch path: dedicated fullscreen `TvInputActivity -> TvView -> tune(...)`; passthrough URI via `TvContract.buildChannelUriForPassthroughInput(inputId)`. `TvInputManager.TvInputCallback` handles discovery; `TvView.TvInputCallback` handles active tuned session. Keep them separate.

Every input launch goes `LaunchDispatcher -> LaunchPolicy -> InputLaunchCoordinator`, which tries StandardTvInputStrategy then optional known vendor/external strategies and returns typed Success/Unsupported/Failure.

Model `CurrentInputState(requestedInputId,launchedInputId,strategy,status,confirmedInputId?,timestamp)`. External intent success means requested/launched, not hardware-confirmed. Persist last requested and optionally last confirmed separately.

Previous/next remote cycling should use actual visible/available snapshot, not enum ordinal. Direct HDMI remote action resolves through InputRepository.

Vendor behavior behind `InputLaunchStrategy` and `DeviceCapabilities`; no manufacturer switches in UI/repository. Optional explicit compatibility setting Automatic/Force enabled/Disabled is preferable to hidden activation ritual.

Display profiles are optional expert functionality. Separate `DisplayProfileRepository` stable UUIDs, `(inputId,dynamicRange)` binding, PictureBackend semantic capabilities, DisplayStateRepository and coordinator. Do not make hidden shortcut remove profile choice. Repair dangling profile UUIDs. Never couple display polling to Accessibility; transient null sample must not stop observer.

## C9 — HOME, startup, remote, Accessibility, idle

Current manifest HOME Activity + setup wizard is a good minimal foundation. `MainActivity` is singleTask and Back root is a no-op. Setup wizard resolves HOME and uses RoleManager.ROLE_HOME on modern Android or ACTION_HOME_SETTINGS fallback. Keep.

Google TV wizard displays manual ADB commands that can set HOME/disable stock launchers. Keep only as expert guidance; add rollback/enable commands and explain side effects. Do not silently automate disabling another launcher.

Add `HomeRoleCoordinator` for currentHome/isDefault/requestRole/openHomeSettings/restartAsHome. Add deliberate HOME re-entry cleanup policy (close transient menu/move mode etc while preserving row/item focus).

No boot receiver exists. Do not add one just to start HOME. Optional startup target behavior uses one `StartupCoordinator` receiving typed ColdHomeStart/BootHomeStart/Wake/ManualTest triggers, with pending cancellable job, duplicate suppression, boot-window validation and target resolver. Targets: None/App/Input/LastRequestedInput/LastConfirmedInput. Route through LaunchDispatcher/Policy.

Accessibility is absent and normal operation should remain zero-accessibility. Optional `AccessibilityBridge` may expose HOME fallback, foreground/window observation and global key filtering on devices that need it. It must not own startup, input, display profiles, wallpaper, idle or vendor workarounds.

Keep UI-local key behavior local: MENU Settings safety path, AppCard select-key long press/release suppression, move-mode D-pad/select/back capture. Add a separate `RemoteGestureEngine` with per-key Single/Double/Long/repeat state and RemoteAction mapping (Settings/Notifications/App/Input/Prev/Next input/Idle etc.). Activity + optional Accessibility key sources must dedupe physical event fingerprints.

Protected modal gate belongs between remote/input layer and parental auth, not Accessibility-owned state.

Idle is currently absent. Add `LauncherIdleCoordinator(lastInteraction,visibility,mediaPlaying,modal/settings state,mode)`. Preferred first mode is immersive wallpaper: reuse C5 WallpaperEngine/video renderer, hide rows/status, wake on user input and consume wake key when needed so it doesn't launch current card. External/system screensaver strategy is optional later. Startup and idle remain separate state machines.

## C10 — Persistence, backup, themes, icons, shortcuts, parental

Keep typed immutable LauncherConfig/DataStore and app artwork cache, but add mature lifecycle around durable state.

Concrete ConfigStore bugs:

1. Read path for missing config uses localized `defaultCategories(context)`. Update path for missing config falls back to plain `LauncherConfig()` with hardcoded English category names. First setting change on fresh non-English install can persist English. Use one `ConfigDefaults.create(context)` factory everywhere.

2. JSON decode failure uses `getOrNull() ?: defaults`. Launcher can display defaults and next setting write permanently overwrite damaged config. Decoder should return Success/DecodeFailure(raw,reason), attempt last-known-good and avoid destructive overwrite.

Add `schemaVersion` and ordered `MigrationRegistry`; migrations sequential, preferably idempotent, preserve source until new payload validates, snapshot before destructive migration, tests for historical fixtures. Add LastKnownGoodStore independent of exported backup.

Current normalize is partial. Future subsystem normalizers should repair categories, home rows, TV channels, wallpaper, shortcuts, inputs and display profile references.

Current manual Save Config is only LauncherConfig JSON. It is not full backup. Custom photo lives at `filesDir/wallpaper.jpg` and isn't included. Local video backup stores only `content://` URI with device/install-specific permission. Backup also contains fields like knownApps/setupDone that restore deliberately preserves/replaces, proving public backup schema should differ from runtime config.

Define full backup DTO/archive with manifest/version/config/layout/rows/shortcuts/inputs/display profiles/asset manifest/integrity metadata. Restore transactionally: read temp -> ZIP/path safety -> verify manifest/version/hashes -> decode all -> migrate -> normalize -> resolve local references -> stage assets -> atomic commit -> one RuntimeRehydrate. Failure before commit leaves current launcher untouched. Full backup defaults replace portable state while preserving secure/local-only state; theme is selective merge.

Separate:

- Android Auto Backup/D2D
- user `.aerialbackup`
- appearance `.aerialtheme`
- internal last-known-good

Once parental secrets exist, use separate secure store with explicit Android backup exclusion/rules.

Theme contains appearance settings + optional wallpaper/custom artwork assets, not app membership/hidden packages/device inputs/parental state/caches. Use explicit ExportPolicy, not magic key naming.

Do not copy Projectivy embedded fixed-secret AES-CBC backup crypto. Plain transparent archive is fine for non-sensitive backup; optional encrypted portable backup should use user password + modern KDF + authenticated encryption (AES-GCM). Device-only secrets can use Keystore.

Current app artwork cache is strong and regenerable; never export it. `Icons.kt` is just small internal UI vectors, unrelated to icon packs.

Add `IconRepository`: per-item custom -> per-item icon-pack -> global icon pack -> Leanback banner -> app icon -> generated tile. Because TV banners are 16:9 and icon packs usually square, use ArtworkPolicy (`AUTO`, `BANNER`, `ICON_PACK`, `APP_ICON`, `CUSTOM`) rather than blindly replacing banners. ADW-compatible pack discovery/parsing is useful; parsed cache keyed pack package+version+parser schema; cache regenerable. Focus glow should derive from final resolved artwork.

No Android shortcut subsystem exists. Add `ShortcutRepository` with LauncherApps query/callback, stable ShortcutId(package,shortcutId,user), HomeItem.Shortcut, start/pin/unpin/external PinItemRequest delegation. Restore placement even if provider missing; keep UnresolvedShortcut until app republishes instead of deleting immediately.

No parental subsystem exists. If added later, every target passes LaunchPolicy/ParentalPolicy using stable ProtectedTarget identities (App/Shortcut/Input/Package/Settings). Auth session can use elapsed realtime and expire on process restart. Never plaintext PIN; use salt+KDF/verifier and separate secure store. Recovery must be explicit; no hidden universal secret. Scheduled enforcement separate from auth UI. Accessibility supplies events only and does not own parental state.

## C11 — Performance and reliability

Couchy is already performance-conscious. Protect these as invariants rather than replacing them during feature growth.

Release already enables minification and resource shrinking; keep narrow keep rules for future reflection only.

`applyAerialRowsSourcePatches` attached to every `preBuild` unconditionally writes the two largest Kotlin files + strings even after patches are present. This damages reproducibility and can defeat incremental compilation. Phase 0A is both architecture and build-speed work.

Baseline profile exists but is hand-authored and broad (`HSP` across launcher/Compose/foundation/TV Material/lifecycle/DataStore/coroutines/serialization) with no Macrobenchmark generator. Keep temporarily, then replace/validate with measured startup/interaction profile.

CI currently builds debug/release and lint, which catches R8 issues, but no test/androidTest/macrobenchmark source sets exist. Add unit tests for pure reducers/policies/migrations, targeted instrumentation for Android boundaries and device/nightly macrobenchmarks for startup/navigation/settings/Aerial/memory.

Cold startup correctly starts DataStore config read and app scan concurrently. But full launcher waits for complete app scan including metadata, artwork cache read/rasterization, focus color extraction and all app coroutines. This clean gate may delay true cold HOME. Do not add more repositories to that gate. If benchmark shows problem, use staged persisted app catalogue -> interactive HOME -> PackageManager reconciliation -> artwork refresh -> optional repos.

App scanner bounded parallelism is excellent. Share a bounded artwork work limiter with future icon packs/custom art rather than unbounded second pool.

Unchanged AppEntry and same-list instance reuse are excellent. Future repos should preserve semantic object identity where practical.

Concrete cache bug: disk artwork filename includes package stamp + banner width; memory reuse checks only stamp. Runtime density change can reuse wrong-sized image and mark new variant valid. Add `ArtworkVariant(bannerWidth,iconSize,cacheSchemaVersion)` and require stamp+variant before reuse.

All decoded app banners/icons remain inside static memory cache. This guarantees smooth scrolling but may be memory-heavy at 100+ apps/low-memory devices. Measure PSS first. If needed, evolve to ArtworkRef + byte-bounded memory LRU, focused/adjacent row pin/prefetch, existing WebP disk cache, trim-memory handling. Do not blindly replace with a generic image library.

`prepareToDraw()` and target-size rasterization are worth preserving.

As LauncherConfig grows, avoid passing whole config into every UI surface; use domain/presentation substate. Do not split persistence into hundreds of keys solely for recomposition.

SlidingRows derived naturalRows/IDs/rank/sort can move into HomeRowsRepository. Rail currently does a full `config.copy()` just to strengthen panel style; introduce small PanelVisualConfig. Panel brushes/shapes can later be remembered/cached if profiling justifies it. SmoothCornerShape path math is robust (degenerate fallback); remember shape by radius first, benchmark before deeper optimization.

Rapid settings/move `scope.launch { store.update }` can queue DataStore edits. Process every semantic command in in-memory reducer, coalesce only latest persistence snapshot. Do not debounce commands that must all execute.

TV content currently runs IO off main thread (good) but is UI-owned snapshot. Move to cached repository + ContentObserver. If query parallelism ever needed, bound it (2–4) rather than fan out every channel/provider query.

VideoWallpaper hot path is excellent; keep SurfaceView, 10–15s buffer, ON_STOP release/rebuild, first-frame gate. C5 security/audio/config improvements layer on top.

Optional future subsystems must be demand-driven: no media observation if nothing consumes it; no display poll when off; no Preview query when disabled; no Aerial work when off; no Accessibility requirement; parse only selected icon pack; shortcut callbacks/query targeted.

Do not copy Projectivy's Accessibility-owned 4s display poll.

`runCatching` is useful for HOME crash containment but should not become failure semantics. Return structured Load/Launch/Import results so UI and diagnostics can distinguish permission/provider/malformed/unsupported while HOME remains stable.

Add bounded diagnostics ring buffer for app scan duration/cache hit/miss/TV provider duration/wallpaper failures/input discovery/backup result; do not log private notification/media content.

MainActivity remains small. Large `LauncherScreen.kt`/`SettingsSheet.kt` are ownership warning signals, not runtime issue by themselves; split by state/lifecycle boundaries, not line count.

## C12 — Final conclusion

Couchy hot-path mechanics + Projectivy subsystem maturity − Projectivy coupling/polling debt.

Do not turn Aerial-Rows into Projectivy decompiled into Compose, and do not bolt every new feature into `LauncherScreen.kt`. Preserve Couchy's speed/modern implementation while adding mature domain/repository boundaries.

Implementation begins with source-pure Phase 0A followed by measurement Phase 0B; detailed phase order is in `C12_FINAL_BLUEPRINT.md`.
