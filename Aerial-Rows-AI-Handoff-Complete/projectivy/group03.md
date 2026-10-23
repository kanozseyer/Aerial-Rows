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
