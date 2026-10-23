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
