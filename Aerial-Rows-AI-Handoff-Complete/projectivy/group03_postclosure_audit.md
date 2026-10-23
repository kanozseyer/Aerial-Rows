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
