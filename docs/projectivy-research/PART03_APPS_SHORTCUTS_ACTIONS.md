# Projectivy 4.72 Reconstruction Dossier — Part 03: Apps, Shortcuts, Card Customization, Context Actions and Launch Coordination

Target: Projectivy 4.72 (97)  
Evidence: REA/JADX static analysis of the exact APK.  
Purpose: implementation-grade specification for recreating Projectivy's app/shortcut behavior in Aerial-Rows without transplanting proprietary method bodies.

## 1. Scope

This part covers:

- TV vs mobile application discovery,
- package/activity de-duplication and sorting,
- application card identity/artwork/launching,
- live package change handling,
- launcher shortcut discovery, pinning and execution,
- the shared card customization record,
- persistence of card customizations,
- the long-press/context action surface,
- add/remove/hide/move semantics,
- the common launch coordinator and parental-gate hook.

Primary evidence classes:

- `yb3`: application/shortcut discovery and launcher utilities,
- `tb3`: app discovery mode enum,
- `qb`: application card,
- `p94`: shortcut card,
- `nb3`: `LauncherApps.Callback`,
- `ob3`: package-change receiver/debouncer,
- `t4`: common action/card base,
- `l5` + serializer `j5`: per-card customization record,
- `j6`: contextual action/settings fragment,
- `w40`: home row/card repository and persistence trigger,
- `t40`: async persistence of customizers/rows.

## 2. Application discovery modes

Projectivy defines three app-query modes in `tb3`:

- `ALL`
- `ONLY_MOBILE`
- `ONLY_TV`

The raw query path in `yb3.c(...)` constructs `ACTION_MAIN` intents as follows:

### TV applications

`MAIN + android.intent.category.LEANBACK_LAUNCHER`

### Mobile applications

`MAIN + android.intent.category.LAUNCHER`

### All applications

Concatenates the TV and mobile raw query results.

The PackageManager query uses flag value `64`, i.e. it requests resolved filters so the returned `ResolveInfo.filter` can be inspected for launcher categories.

Raw query results are cached per discovery mode.

## 3. Filtering and de-duplication

`yb3.b(mode, excludeSelf)` converts the raw `ResolveInfo` list into one selected activity per package.

Observed rules:

1. optionally exclude Projectivy's own package,
2. when building the mobile-only list, suppress a package's mobile launcher entry if that package also exposes a Leanback launcher entry,
3. de-duplicate by package name,
4. if multiple ResolveInfo entries exist for the same package, favor the default resolver entry,
5. sort the resulting package entries by the resolved activity label using `String.CASE_INSENSITIVE_ORDER`.

This means Projectivy intentionally presents an app primarily as a **package-level card**, not as a list of every launcher activity in that package.

### Recommended Aerial-Rows model

Keep a stable package-level identity for normal app cards while retaining the actual selected `ComponentName` as the launch target.

```kotlin
data class InstalledAppCard(
    val packageName: String,
    val component: ComponentName,
    val surface: AppSurface, // TV or MOBILE
    val label: String,
    val artwork: Artwork,
)
```

## 4. Application label and launch caches

`yb3` maintains multiple process caches:

- resolved app/activity labels keyed by package + activity,
- package availability checks,
- raw app query results by discovery mode,
- package launch intents,
- shortcut list state.

Activity labels are lazily loaded through PackageManager and cached.

Package-level app labels are likewise cached independently.

Package launch resolution favors:

1. `getLeanbackLaunchIntentForPackage`, then
2. `getLaunchIntentForPackage`.

The resolved package launch intent is cached.

## 5. Application card (`qb`)

An app card wraps an `ActivityInfo` and inherits the common `t4` card/action behavior.

### Stable identity

The inherited stable action ID is the Java hash of `ActivityInfo.packageName`.

Because discovery intentionally reduces to one card per package, this provides a package-level identity and also causes customization/statistics to follow the app even if the selected launcher activity changes.

Aerial-Rows should not persist raw Java `hashCode()` as its canonical identity. Use the package string as the canonical key and derive a stable numeric key only at the UI boundary if required.

### Title

The default title is the resolved activity label from `yb3.g(ActivityInfo)`.

### Artwork

Artwork resolution is layered:

1. explicit user custom icon URI,
2. selected icon-pack override when available,
3. application/activity banner where allowed,
4. activity icon,
5. application icon fallback.

TV and mobile app cards can use different configured aspect-ratio policies.

Icon-pack details are deferred to Part 10.

### Launch intent

The app card tries a Leanback launcher intent first and falls back to ordinary launcher intent. The selected package/activity is used to create an explicit class launch target.

The common helper adds `FLAG_ACTIVITY_NEW_TASK` when the caller is not already an Activity and applies supplied extras.

### Launch statistics

Before delegating to the common launch path, `qb.f(...)` updates its customization record:

- `lastLaunchDate = System.currentTimeMillis()`
- increment `launchCount`

The update happens before the actual start call, so the counters represent **launch attempts**, not guaranteed successful launches.

The card exposes those values back to the sorting layer, enabling recent/frequent-style sorting.

## 6. Common launch coordinator (`t4`)

`t4.f(context, extras, transitionSource)` is the common Intent-based launch path for most cards.

Flow:

1. create optional animation/options bundle,
2. ask subtype for its target Intent,
3. if the card is parental-protected, wrap the target with the parental-check Activity,
4. start the target,
5. if `ActivityNotFoundException` occurs, allow the subtype to refresh/re-resolve once,
6. retry once,
7. on other exceptions, show the standard "can't start activity" feedback.

This is an important architecture rule: launch policy is centralized above individual card renderers.

### Recommended Aerial-Rows equivalent

```text
Card click
  -> LaunchCoordinator
       -> resolve target
       -> ParentalGate
       -> target-specific launcher
       -> optional retry/re-resolution
       -> result / feedback
```

Sub-launchers should include Package, ExplicitComponent, Shortcut, ProviderDeepLink, TvInput, and SystemAction.

## 7. Live package-change handling

Projectivy uses `LauncherApps.Callback` (`nb3`) for:

- package added,
- package changed,
- package removed,
- packages available,
- packages unavailable,
- shortcuts changed.

Package callbacks are normalized into ordinary package-change Intent actions and forwarded to `ob3`.

`ob3`:

- accumulates package/action pairs in a set,
- debounces processing by roughly **500 ms**,
- clears cached labels for affected packages,
- updates a package-installed cache to true/false for added/removed packages,
- conditionally triggers a central `w40` home refresh,
- then emits the broader application-change signal used by other consumers.

This avoids rebuilding the launcher once for every callback in a burst.

### Recommended Aerial-Rows behavior

Use `LauncherApps.Callback` as the primary live package source and debounce package events into one repository invalidation batch. Also keep a broadcast fallback for OEM behavior where appropriate.

## 8. Shortcut discovery

Projectivy has two related shortcut queries.

### Home Shortcuts row

The global shortcut cache uses `LauncherApps.ShortcutQuery` with query flag value `2`, then calls `LauncherApps.getShortcuts(...)` for the current user.

The resulting list backs the built-in Shortcuts row.

### Per-app shortcut submenu

When the context menu is opened for an app card, Projectivy queries that package with flag value `11`, giving it a broader app-specific shortcut list (dynamic/pinned/manifest classes of shortcuts on supported Android versions).

Each shortcut is displayed with:

- short label,
- long label,
- shortcut icon loaded through `LauncherApps.getShortcutIconDrawable`.

Selecting one invokes the shortcut directly.

## 9. Shortcut card (`p94`)

Shortcut cards:

- use card/presenter type 7,
- title = short label,
- secondary label = long label when different,
- load their icon through `LauncherApps`,
- launch via `LauncherApps.startShortcut(...)` rather than an ordinary Intent,
- expose enabled state from `ShortcutInfo.isEnabled()`.

### Existing Projectivy stable-ID weakness

The card ID is `ShortcutInfo.getId().hashCode()`.

Android shortcut IDs are package-scoped, so two unrelated packages can legally use the same shortcut ID. Aerial-Rows should fix this by using:

`user + packageName + shortcutId`

as canonical identity.

## 10. Shortcut pin / hide behavior

For shortcut cards on Android O+ Projectivy treats the contextual hide/toggle path differently from normal generated cards.

It:

1. queries the package's currently pinned shortcuts,
2. copies their IDs,
3. removes the selected shortcut ID when already pinned or adds it otherwise,
4. calls `LauncherApps.pinShortcuts(package, ids, user)`.

Thus the launcher can control which shortcuts are retained/pinned rather than merely keeping its own hidden-ID flag.

## 11. Shared card customization model (`l5`)

The serialized type name is:

`com.spocky.projengmenu.actions.ActionCustomizer`

Exact serialized fields recovered from serializer `j5`:

1. `actionId` — required integer stable card ID
2. `customIconUri` — optional string
3. `customIconCrop` — optional boolean
4. `customTitle` — optional string
5. `launchCount` — optional integer, default 0
6. `lastLaunchDate` — optional long, default 0

Runtime meanings:

- custom icon URI overrides source artwork,
- custom icon crop overrides default crop behavior,
- custom title overrides source title,
- launch count/date are used for usage-aware sorting/metadata.

A customization record is considered meaningful only if at least one override/stat field is set/non-zero.

Equality/hash identity is action ID only.

## 12. Customizer persistence

Customizers are stored in preference key:

`action_customizers_list`

At home initialization Projectivy:

1. reads the serialized string,
2. deserializes a list using the `ActionCustomizer` serializer,
3. discards records with no meaningful fields,
4. builds the runtime lookup map keyed by `actionId`.

Whenever card customization changes, `w40.t()` schedules asynchronous persistence. The persistence coroutine enumerates the runtime customizer map and writes the list back through the preferences layer.

Aerial-Rows should use a versioned structured store instead of a string blob while retaining a migration importer for our own earlier versions.

## 13. Long-press / contextual action surface

The contextual UI is backed by `settings_action.xml` and fragment/controller `j6`.

Potential actions include:

- Open
- Open input internally
- Program details
- Play/Pause
- Stop
- Previous
- Next
- App settings
- Uninstall
- Start at boot
- Add parental control
- Remove parental control
- package shortcuts
- custom title
- change icon
- crop icon
- move card
- remove from current manual category
- hide generated card
- add to category
- move to category

The menu is **capability filtered** rather than showing every action for every card.

## 14. Context-action visibility rules

Observed rules include:

### App Info

Visible only for `qb` application cards.

### Uninstall

Visible only for `qb` application cards.

### Start at boot

Available for ordinary app cards and the supported input/source action family.

### Media controls

Visible only for Now Playing/media cards and further gated by whether the media controller advertises support for the corresponding command.

### Program details

Visible only when the card subtype reports program-detail capability.

### Parental add/remove

Shown only for card types that opt into parental control. The current protected-ID collection determines whether Add or Remove is displayed.

Shortcut cards explicitly opt out of this capability in Projectivy.

### Appearance section

Not shown for media-session cards.

### Move inside row

Requires a current row, a non-media card, and more than one card in the row.

### Remove from category

Only for manual/custom row type 0 and non-media cards.

### Hide

Only for non-manual/generated/provider rows and non-media cards.

### Add to / Move to

Targets manual/custom categories. The Add list excludes categories that already contain the card.

For an application card, the visibility area also links relevant provider-channel rows originating from the same package.

## 15. App Info and Uninstall actions

### App Info

Projectivy constructs:

`android.settings.APPLICATION_DETAILS_SETTINGS` with `package:<packageName>`

and launches it through its SettingsIntent wrapper Activity.

### Uninstall

Projectivy constructs:

`android.intent.action.UNINSTALL_PACKAGE` with `package:<packageName>`

and launches the system uninstall UI.

There is no silent uninstall behavior in this path.

This directly matches the long-press behavior requested for Aerial-Rows.

## 16. Open action

`action_exec` delegates back to the selected card's normal launch path. Input/source cards may supply an external-TV-view mode; ordinary cards call the default launch route.

The internal-input variant supplies `tvView = "internal"` to the source action and uses the same card launch interface.

Input details are deferred to Part 07.

## 17. Start-at-boot action

The contextual action writes the selected card/source launch identity into preference field `hj3.P`, whose key is:

`power_autostart_source`

The actual boot/standby startup state machine is deferred to Part 08, but Part 03 establishes that the long-press action configures that shared startup source rather than storing a separate per-card boolean.

## 18. Parental-control action hook

Add/Remove parental control launches `ToggleParentalControlActivity` with a unique card identity based on the card's stable ID and passes that intent through Projectivy's parental-check wrapper.

The full PIN/schedule/protection implementation is Part 09.

## 19. Card title/icon customization flow

The context UI allows a card to change:

- title,
- icon source,
- icon crop.

Title changes update `customTitle` and then:

- persist customizers,
- publish a targeted row/card refresh.

Icon crop changes update `customIconCrop` and do the same.

The image/icon picker updates `customIconUri`; icon-pack browsing and imported icon formats are deferred to Part 10.

## 20. Manual-category add/remove/move semantics

Manual/custom rows use their stored included-action ID list.

### Remove from current manual category

Toggles/removes the action ID in that row's membership state, persists row state, and refreshes the affected row.

### Add to another category

Adds/toggles the action ID in the selected destination manual row and refreshes/persists it.

### Move to another category

If the source is a manual row, remove it from the current row first. If the source is generated/provider-backed, invoke that source's hide/toggle mechanism. Then add the card to the destination manual row.

### Reorder in current row

For Move, Projectivy switches the row to manual sort mode, marks the selected card as the globally active moving card, and lets the home fragment perform index moves using stable action IDs.

## 21. Generated-row hide state

For normal generated rows, Projectivy stores a complementary/excluded action-ID list on the row model and toggles the selected ID there.

The row is then repopulated through the central repository.

Shortcut rows are a special case on Android O+: hide/toggle is implemented through the system pinned-shortcut set instead of only the row's excluded-ID list.

## 22. Implementation rules for Aerial-Rows

### App repository

```text
AppRepository
  |- queryTvApps()      MAIN + LEANBACK_LAUNCHER
  |- queryMobileApps()  MAIN + LAUNCHER
  |- deDuplicateByPackage()
  |- observe LauncherApps.Callback
  |- label/artwork cache
  `- package launch resolution cache
```

### Canonical identities

Use strings as canonical persisted identities:

```text
app:<user>:<package>
shortcut:<user>:<package>:<shortcutId>
program:<provider identity>
input:<input identity>
static:<action name>
```

Do not make Java hash codes the database key. A stable 64-bit hash can be derived for Compose/Lazy layout keys if needed.

### Card customization

```kotlin
data class CardCustomization(
    val cardKey: String,
    val customIconUri: String? = null,
    val cropIcon: Boolean? = null,
    val customTitle: String? = null,
    val launchCount: Long = 0,
    val lastLaunchAttemptAt: Instant? = null,
)
```

### Context actions

Represent actions as capabilities generated by the domain layer, not hard-coded menu checks in Compose:

```kotlin
sealed interface CardAction {
    data object Open : CardAction
    data object AppInfo : CardAction
    data object Uninstall : CardAction
    data object StartAtBoot : CardAction
    data object EditTitle : CardAction
    data object ChangeIcon : CardAction
    data object ToggleIconCrop : CardAction
    data object Move : CardAction
    data object Hide : CardAction
    data class AddToRow(val rowKey: String) : CardAction
    data class MoveToRow(val rowKey: String) : CardAction
    // media/program/input actions supplied by their feature modules
}
```

The domain model should expose `availableActions(card, row)` so settings/menu UI is only a renderer.

## 23. Known Projectivy weaknesses worth not copying

1. App card persisted identity is a 32-bit Java package hash rather than the package string.
2. Shortcut identity hashes only `ShortcutInfo.id`, which is not globally unique.
3. Launch count/date are incremented before launch success is known, so the names imply successful launches while the data actually tracks attempts.
4. Several caches are mutable process-global maps; Aerial-Rows should use repository-owned state and explicit invalidation.
5. Shortcut parental protection is bypassed by design because shortcut cards opt out and invoke `LauncherApps.startShortcut` directly. A centralized Aerial-Rows launch gate can cover shortcuts consistently if desired.

## 24. Part 03 completion criteria

Completed:

- TV/mobile app query intents
- raw query caching
- package-level de-duplication
- TV-vs-mobile duplicate suppression
- alphabetical sorting behavior
- app labels/artwork/launch intent resolution
- app card stable ID and launch statistics
- package live-update callback/debounce path
- global and per-app shortcut queries
- shortcut card launch/artwork/enabled state
- shortcut pin/unpin behavior
- ActionCustomizer exact serialized fields
- customizer load/filter/runtime map/persist flow
- context menu inventory
- context action visibility rules
- App Info and Uninstall intents
- Start-at-boot configuration hook
- parental-control hook
- title/icon/crop customization
- manual category add/remove/move
- generated-row hide behavior
- common Intent launch coordinator/retry path

Deferred:

- media controller internals -> Part 06
- icon packs/image picker/theme import internals -> Part 10
- startup execution state machine -> Part 08
- parental PIN/schedule implementation -> Part 09
- TV input/source details -> Part 07
