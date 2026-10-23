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
