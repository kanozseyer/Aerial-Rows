# Projectivy 4.72 — Group 07: Settings System

Status: **COMPLETE + VERIFIED**  
Reference APK: Projectivy Launcher 4.72 / versionCode 97  
Package: `com.spocky.projengmenu`  
APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## 1. Analysis method and efficiency improvement

Group 7 uses the persistent reverse-engineering cache created during Groups 4–6, plus a settings-specific **role graph**:

```text
setting key
   ↓
yh3 descriptor
   ↓
settings screen / control
   ↓
SharedPreferences storage
   ↓
change handler
   ↓
runtime consumer / home preview
```

To avoid opening/decompiling each settings screen separately, this group added:

- a one-load exporter for every XML resource containing a `PreferenceScreen`;
- a batch dump of all 48 subclasses of Projectivy's GuidedStep settings base (`cb3`);
- a complete field→preference-key map for the central `hj3` registry;
- screen→preference mappings derived from the source cache;
- targeted smali verification only where JADX could not reconstruct coroutine control flow.

The central registry contains **151 `yh3` descriptor constructions** in this release. All **148 static `yh3` fields** were mapped to their exact preference keys; the remaining constructions are temporary/dynamic/migration descriptors rather than unmapped static fields.

This is considerably faster than treating every settings screen as an independent subsystem while retaining end-to-end verification.

---

## 2. Projectivy has two settings UI systems

This is the most important architectural finding in Group 7.

### A. Leanback Preference side panels

Used primarily for content-oriented/configuration screens such as:

- launcher-side dynamic settings/actions;
- categories;
- category configuration;
- category apps/content;
- channels;
- notifications;
- card/context actions;
- program details.

The boundary is:

```text
SettingsActivity
   ↓
lv
   ↓
x64 (Leanback settings host)
   ↓
Preference fragment selected from preferenceResource
```

### B. Metadata-driven GuidedStep settings

Used for most global launcher configuration:

```text
pf (root guided settings)
   ↓
General / Appearance / Inputs / Power / Remote / Accessibility /
Parental Controls / Display Profiles / Backup & Restore / Premium
   ↓
cb3 guided-screen subclasses
   ↓
yh3 preference descriptors
```

These screens are **not manually coded control-by-control**. A central descriptor object defines type/default/title/description/icon/flags/min/max/step and `cb3.q0()` generates the appropriate TV control.

For Aerial-Rows, this metadata-driven approach is the part worth reproducing conceptually even if the new UI is Compose TV rather than Leanback GuidedStep.

---

## 3. Leanback settings host

### `SettingsActivity`

`com.spocky.projengmenu.ui.sidePanels.settings.SettingsActivity` extends `lv`.

It intercepts the remote **MENU key (KEYCODE_MENU / 82)**:

```text
MENU down → startTracking()
MENU up   → finish SettingsActivity
```

So the same remote menu key that opens the side-panel settings can close it cleanly.

### Side-panel window geometry

`lv.onAttachedToWindow()` makes the settings Activity a real right-side panel rather than a full-screen Activity:

- width = Leanback settings-pane width resource;
- height = display height minus vertical margins;
- gravity = right/end + vertically centred;
- horizontal margin derived from Leanback settings-pane margin;
- adds a translucent/window-layout flag;
- chooses blurred vs normal Leanback preference theme depending on blur support.

This is an implementation-relevant reason Projectivy settings visually float over the launcher.

### `x64` settings host

`x64` extends Projectivy's customized Leanback settings host.

It:

- inflates `leanback_settings_fragment`;
- applies a rounded/clipped background to the preference pane;
- pushes nested preference fragments when a `Preference.fragment` is selected;
- loads the initial fragment from an integer `preferenceResource` extra.

The resource routing is explicitly:

```text
settings_channels         → g70
settings_categories       → b40
settings_category         → CategoryPreferencesFragment
settings_action           → j6
settings_notifications    → b73
settings_category_apps    → CategoryAppsPreferencesFragment
settings_category_content → k40
other/default             → vo2
```

This generic routing lets one side-panel Activity host many independent settings contexts.

---

## 4. XML Preference resources

The APK contains ten PreferenceScreen XML resources:

```text
settings_main.xml
settings_categories.xml
settings_category.xml
settings_category_advanced.xml
settings_category_apps.xml
settings_category_content.xml
settings_channels.xml
settings_notifications.xml
settings_action.xml
settings_program_details.xml
```

`settings_main.xml` is intentionally essentially empty: its real content is generated dynamically.

### Dynamic `settings_main`

`vo2` asynchronously builds the side-panel menu from Projectivy's internal home/action model.

The flow is:

```text
w40.L home/category definitions
   ↓
filter categories with i5.k == 4
   ↓
collect eligible e90 internal actions
   ↓
t40 → uo2 groups
   ↓
PTPreferenceCategory + CustomPreference rows
   ↓
e90.t() supplies Intent
```

Therefore Projectivy's main side-panel settings/actions are driven by the **same internal action/category model as the launcher**, rather than duplicated in static XML.

---

## 5. Category Preference screens

The category screens confirmed and reinforce the exact model discovered in Group 2.

`settings_category.xml` exposes:

- title;
- change icon;
- visibility;
- multi-row/grid mode;
- category background colour;
- sliding single row;
- card-title visibility;
- scale;
- advanced settings;
- sorting/order;
- reset/default;
- apps/content;
- delete;
- channel-related error state.

`settings_category_advanced.xml` exposes:

- items per line: 0..20;
- maximum rows: 0..20;
- start margin: 0..100;
- end margin: 0..100;
- top margin: 0..100.

`settings_category_apps.xml` explicitly separates:

```text
TV applications
Mobile applications
Channels
```

This is not merely presentation metadata: Group 2 already proved these values feed the actual row layout algorithm.

---

## 6. Focus behavior in Leanback Preference screens

Projectivy has its own base preference fragment `vu`.

It adds TV-specific focus/navigation handling on top of Leanback Preference:

- identifies the first visible/selectable preference;
- handles nested `PreferenceGroup` contents;
- intercepts DPAD navigation through the vertical list;
- pressing DOWN at the final selectable row wraps to the first;
- pressing UP at the first selectable row wraps to the last;
- BACK/escape routes through Projectivy's own close/back handler;
- title/icon/tint are handled by the same base.

Standard switch/edit/list preferences mutate their existing preference row rather than reconstructing the whole list, so normal value changes do not inherently reset list selection.

---

## 7. Custom Leanback Preference widgets

Projectivy includes reusable customized controls rather than hand-building each settings row.

### `CustomSwitchPreference`

- based on AndroidX `SwitchPreference`;
- renders using a Material switch;
- thumb/track are recoloured from Projectivy's current accent setting (`hj3.i0`).

### `CustomListPreference`

- directly extends AndroidX `ListPreference`;
- adds Projectivy styling;
- choice/dialog semantics remain standard Leanback/AndroidX preference behavior.

It is **not an inline dropdown implementation**.

For Aerial-Rows, we can retain the same semantic model but use an inline Compose TV choice/dropdown/popover if desired.

### `CustomEditTextPreference`

- extends AndroidX `EditTextPreference`;
- styled by Projectivy;
- shows its current text as summary.

### `AppCustomSwitchPreference`

Switch preference variant with app-sized/icon-oriented presentation.

### `AppPreference`

Supports long-click behavior for app rows.

### `NotificationPreference`

Adds custom key handling appropriate to notification rows.

### `PTPreferenceCategory`

Uses launcher accent colouring for preference-category headers.

### `MaterialSliderPreference`

A real Material slider embedded into a preference row, supporting:

- minimum;
- maximum;
- step;
- DPAD/keyboard interaction;
- optional value label;
- centred/non-centred values;
- optional tick behavior;
- transient state restoration when the preference itself is nonpersistent.

---

## 8. Guided settings root

`pf` is the main GuidedStep settings root.

Its top-level destinations are:

```text
General
Appearance
Input               (when supported on device)
Power
Parental Control
Remote Control
Accessibility
Display Profiles
Manage Settings / Backup-Restore
Premium             (conditional)
```

It additionally renders an explanatory legend for:

- premium settings;
- accessibility-required settings;
- accessibility-recommended settings;
- experimental settings.

These statuses are also embedded directly into generated setting titles/icons.

---

## 9. Central preference descriptor — `yh3`

`yh3` is the central metadata model for global settings.

Semantic field map:

```text
a = base preference key
b = default value
c = title resource ID
d = description resource ID
e = icon descriptor (ta3)
f = flag bitmask
g = minimum value
h = maximum value
i = step
j = optional computed-value getter
k = optional custom setter
l = dynamic key suffix
m = migration/legacy key
```

### Stable GuidedAction identity

`yh3.c()` returns:

```text
baseKey.hashCode()
```

and that value is used as the stable action ID in the GuidedStep interface.

### Dynamic/scoped preferences

`yh3` supports reusable key templates such as:

```text
input_shortcut_visible_
display_profiles_input_hdr_
display_profiles_input_sdr_
appearance_wallpaper_provider_params_
```

A suffix can be applied at runtime while preserving one descriptor definition.

### Migration

A descriptor can carry a legacy/migration key. When the old value exists and the new value is still default, the old value can be transferred and then removed.

---

## 10. Preference flags — `rh3`

Verified flags:

| Flag | Value | Meaning |
|---|---:|---|
| PREMIUM | 1 | Premium-labelled setting |
| ACCESSIBILITY_REQUIRED | 2 | Accessibility service required |
| ACCESSIBILITY_RECOMMENDED | 4 | Accessibility improves behavior |
| TIME_PREFERENCE | 8 | Time-style input |
| PERCENT_PREFERENCE | 16 | Format slider as percentage |
| ENUM_AS_SLIDER_PREFERENCE | 32 | Render enum as slider |
| CENTERED_SLIDER_PREFERENCE | 64 | Slider has meaningful center |
| EDITABLE_TEXT_PREFERENCE | 128 | Editable text |
| COLOR | 256 | Color/palette chooser |
| EXPERIMENTAL | 512 | Experimental badge |
| SECONDS_PREFERENCE | 1024 | Format value as seconds |

This flag model is a major reason Projectivy can generate most settings controls generically.

---

## 11. Generic control generation — `cb3.q0(yh3)`

`cb3.q0()` selects a TV control from descriptor metadata rather than screen-specific code.

Verified behavior:

```text
Boolean
   → check/toggle GuidedAction

Integer / Float
   → slider

TIME_PREFERENCE
   → time-oriented control

p72 enum
   → child choice actions

p72 enum + ENUM_AS_SLIDER_PREFERENCE
   → enum slider

EDITABLE_TEXT_PREFERENCE
   → editable text action

COLOR
   → color/palette action
```

Min/max/step come directly from the `yh3` descriptor.

Additional flags control formatting and slider geometry.

If no icon is explicitly provided, Projectivy can derive a suitable icon from semantic cues such as time, tint/color, size and opacity.

Premium/accessibility/experimental markers are inserted into the generated title presentation.

---

## 12. Storage layer

Most global descriptors ultimately store values in one SharedPreferences store exposed by `hj3.g()`.

`hj3.b(yh3)` reads according to the descriptor's default-value type:

- Boolean;
- Int;
- Long;
- Float;
- String;
- Set<String>;
- `p72` enum, stored as the enum's integer backing value.

`hj3.i(yh3, value)` writes the corresponding type using `SharedPreferences.Editor.apply()`.

Important details:

- `null` removes the key;
- empty String removes the key;
- unsupported types are rejected rather than serialized arbitrarily;
- enum values are persisted as integers;
- numeric writes are clamped by `yh3.i()` against descriptor min/max before storage.

Projectivy therefore has a central typed settings schema even though the underlying persistence is SharedPreferences.

---

## 13. Computed values and custom setters

A descriptor can override ordinary storage behavior:

```text
j → computed getter
k → custom writer
```

This is used where the effective value depends on device capabilities, fallback defaults or another Projectivy subsystem.

It avoids hard-wiring these exceptional cases into every settings screen.

---

## 14. Accessibility-dependent settings

When `yh3.h(value)` changes a descriptor flagged either:

```text
ACCESSIBILITY_REQUIRED
or
ACCESSIBILITY_RECOMMENDED
```

and the live `ProjectivyAccessibilityService` exists, it increments/updates the service's observable configuration state.

So relevant settings can be consumed by the already-running service without requiring the user to restart Projectivy.

---

## 15. Appearance settings registry / theme boundary

`yh3.n` is populated automatically when descriptors are created.

A descriptor is included when its key:

- begins with `appearance`;
- is not an internal `nexp` setting;
- is not a dynamic key template ending in `_`.

This list is reused by theme/appearance export logic.

That gives Projectivy a clean boundary between:

```text
full application configuration
```

and:

```text
appearance settings safe to include in a theme
```

This is the relationship between Group 7 and the `.pltheme` architecture found in Group 6.

---

## 16. Complete central field→key map

The release contains 148 static `yh3` fields. All 148 were mapped.

Important examples:

```text
hj3.D1 → appearance_left_headers_enabled
hj3.E1 → appearance_left_headers_show_on_launch
hj3.F1 → appearance_left_headers_show_icon
hj3.G1 → appearance_left_headers_show_title
hj3.H1 → appearance_left_headers_show_reorder
hj3.I1 → appearance_left_headers_show_settings

hj3.m1 → appearance_categories_show_title
hj3.n1 → appearance_categories_show_icon
hj3.o1 → appearance_categories_rows_spacing
hj3.p1 → appearance_categories_focused_category_position_fixed
hj3.q1 → appearance_categories_hide_rows_above_selection
hj3.r1 → appearance_categories_remember_focused_card_position
hj3.s1 → appearance_categories_unselected_row_opacity
hj3.t1 → appearance_categories_listing_top_margin
hj3.u1 → appearance_categories_start_margin
hj3.v1 → appearance_categories_end_margin
hj3.w1 → appearance_categories_tint_custom_icons
hj3.x1 → appearance_categories_default_category_id
hj3.y1 → appearance_categories_dock_padding

hj3.O0 → appearance_cards_show_title
hj3.P0 → appearance_cards_rounded_corners
hj3.Q0 → appearance_cards_size
hj3.R0 → appearance_cards_spacing
hj3.S0 → appearance_cards_loop_limit
hj3.T0 → appearance_cards_ripple_effect
hj3.U0 → appearance_cards_bg_tint
hj3.V0 → appearance_cards_tint
hj3.W0 → appearance_cards_tv_app_aspect_ratio
hj3.X0 → appearance_cards_mobile_app_aspect_ratio
hj3.Y0 → appearance_cards_icon_pack

hj3.Z0 → appearance_cards_program_play_preview
hj3.a1 → appearance_cards_program_show_progressbar
hj3.b1 → appearance_cards_program_show_badge
hj3.c1 → appearance_cards_program_zoom_on_focus

hj3.d1 → appearance_cards_focused_scale
hj3.e1 → appearance_cards_focused_effect
hj3.f1 → appearance_cards_focused_animation
hj3.g1 → appearance_cards_focused_glow_size
hj3.h1 → appearance_cards_focused_glow_tint
hj3.i1 → appearance_cards_focused_color_override_mode
hj3.j1 → appearance_cards_focused_bg_tint
hj3.k1 → appearance_cards_focused_tint

hj3.o0 → appearance_wallpaper_provider
hj3.t0 → appearance_wallpaper_update_delay
hj3.u0 → appearance_wallpaper_display_mode
hj3.v0 → appearance_wallpaper_transition
hj3.w0 → appearance_wallpaper_wrong_format_display_mode
hj3.x0 → appearance_wallpaper_min_accepted_width
hj3.y0 → appearance_wallpaper_ignore_update_on_video_playing
hj3.A0 → appearance_wallpaper_video_volume
hj3.B0 → appearance_wallpaper_video_tunneling
hj3.C0 → appearance_wallpaper_video_decoders_fallback

hj3.P → power_autostart_source
hj3.Q → power_autostart_leaving_standby
hj3.R → power_autostart_delay

hj3.a2 → action_watch_next_programs_per_app
hj3.b2 → action_channel_general_programs_per_channel
```

A machine-readable CSV containing the complete mapping accompanies this report.

---

## 17. Major GuidedStep screen map

### `pf` — Settings root

```text
General             → qf
Appearance          → se
Input               → sf
Power               → ng
Parental Control    → jg (with parental gate when PIN exists)
Remote Control      → og
Accessibility       → i4
Display Profiles    → pz0
Manage Settings     → ag
Premium             → premium fragment when available
```

### `se` — Appearance

Subscreens:

```text
Categories     → kf
Cards          → hf
Background     → ze
Statusbar      → qg
Left Headers   → vf
Idle mode      → rf
Font           → of
Themes         → theme flow
```

Direct appearance settings include:

- `appearance_general_load_cards_while_scrolling`;
- global appearance tint/accent (`hj3.i0`).

### `vf` — Left Headers

Exact keys:

```text
appearance_left_headers_enabled
appearance_left_headers_show_on_launch
appearance_left_headers_show_icon
appearance_left_headers_show_title
appearance_left_headers_show_reorder
appearance_left_headers_show_settings
```

### `kf` — Categories

Includes:

```text
appearance_categories_show_title
appearance_categories_show_icon
appearance_categories_rows_spacing
appearance_categories_focused_category_position_fixed
appearance_categories_hide_rows_above_selection
appearance_categories_remember_focused_card_position
appearance_categories_unselected_row_opacity
appearance_categories_listing_top_margin
appearance_categories_start_margin
appearance_categories_end_margin
appearance_categories_tint_custom_icons
appearance_categories_dock_padding
```

### `hf` — Cards

Includes title, corners, size, spacing, looping, ripple, background tint, tint, TV/mobile aspect ratios and icon pack.

Related subareas include:

- `jf`: program-card preview/progress/badge/focus zoom;
- `cf`: focused-card scale/effect/animation/glow/tints.

### `ze` — Background

Includes provider selection, update delay, display mode, transition, bad-format fallback mode and minimum accepted width.

Related:

- `af`: video volume/tunneling/decoder fallback/update behavior;
- `te`: brightness/contrast/saturation/hue/blur/tint/shadow adjustment.

### `qg` / `rg` / `pg` — Status bar

Covers:

- notification management;
- auto-hide;
- Android system status bar visibility;
- Projectivy status-icon size/tints;
- settings/battery/connectivity/notifications icons;
- notification auto-hide;
- clock visibility/size/formats/tints.

### `rf` — Idle mode

Includes:

```text
appearance_idle_timeout_duration
appearance_idle_opacity
appearance_idle_no_clock_fade
appearance_idle_prevent_screensaver
```

### `ng` — Power

Includes:

```text
power_autostart_source
power_autostart_leaving_standby
power_autostart_delay
power_hdmi_input_exit_detection
power_simulate_user_activity
power_internal_idle_detection
power_internal_idle_timeout
power_internal_idle_action
power_internal_idle_excludes_media_playback
power_poweroff_mode
```

### `og` — Remote control

Includes:

```text
remotecontrol_show_menu
remotecontrol_double_back_ambient_mode
remotecontrol_decrease_input_lag
remotecontrol_bind_mic_btn_to_assistant
```

### `i4` — Accessibility

Includes:

```text
accessibility_service_enable_reminder
accessibility_alternate_config
accessibility_button_remapping
accessibility_volume_fix
```

### `qf` — General

Includes, conditionally according to device/build capabilities:

- application language;
- change/default launcher flow;
- `general_override_home`;
- TV-provider/Watch Next options;
- `action_channel_general_programs_per_channel`;
- analytics/crash reporting settings;
- update checking;
- Google Play repair/fix action on applicable devices.

The Watch Next per-app value is a lazily-created descriptor in `hj3.y`; it should not be confused with a normal static `yh3` field.

---

## 18. Focus retention after changing a setting

This directly answers one of the desired Aerial-Rows behaviors.

A typical GuidedStep screen (`vf`, Left Headers) does:

```text
user activates existing GuidedAction
   ↓
w54.h(action, descriptor)
   ↓
descriptor writes new value
   ↓
K0()
   ↓
approx. 500 ms delayed live update
   ↓
L0() applies preview/runtime reaction
```

Crucially, it **does not rebuild or navigate away from the action list**.

The same `GuidedAction` remains in place, which means selection/focus stays on the setting that was changed.

The 500 ms delay was verified in smali (`0x1f4` ms) because JADX did not cleanly reconstruct that coroutine method.

This explains the Projectivy behavior we want to retain:

```text
toggle setting
   ↓
switch visibly changes
   ↓
launcher preview updates
   ↓
remote cursor stays on same row
```

### Intentional recreation exception

Some changes really do require a style rebuild.

Example: changing the global appearance tint/accent (`hj3.i0`) calls Activity `recreate()` after the value changes.

That is an explicit exception rather than the normal settings update mechanism.

---

## 19. Live home preview behind Guided settings

`cb3` supports settings screens that expose/preview the launcher behind the GuidedStep panel.

Individual screens opt into this (`C0()`), and their `L0()` overrides update the active `MainFragment` after settings changes.

Example: `vf` calls the live home fragment's header-enable/update method after the preference changes.

This design is why many appearance changes can be previewed without leaving settings.

Recommended Aerial-Rows equivalent:

```text
Settings state changes
       ↓
SettingsRepository writes value
       ↓
StateFlow updates launcher composables immediately
       ↓
focus remains owned by the current settings row
```

No artificial delayed refresh should be necessary in Compose unless it is specifically useful for expensive operations.

---

## 20. Multi-choice behavior

There are two distinct choice mechanisms.

### Leanback Preference side

`CustomListPreference` is simply a styled `ListPreference`; the Leanback settings host opens the appropriate preference dialog/fragment.

### Guided settings side

Enum descriptors implementing Projectivy's `p72` interface are generated as:

- child choice actions; or
- a numeric/enum slider when `ENUM_AS_SLIDER_PREFERENCE` is set.

Each enum choice receives an action ID derived from:

```text
preferenceHash + 1 + enumBackingValue
```

For Aerial-Rows, we can improve the presentation while preserving the model:

```text
single current row
   ↓
Compose TV dropdown / choice popup
   ↓
select new value
   ↓
close popup
   ↓
return focus to same settings row
```

This satisfies the user's desired visible dropdown behavior better than Projectivy's older Leanback dialogs while retaining equivalent semantics.

---

## 21. Runtime-update taxonomy

Projectivy settings are not all treated identically. The useful conceptual categories are:

### Immediate in-place state

Examples: ordinary boolean, slider, enum or text descriptors. Value is persisted without screen rebuild.

### Delayed live preview

Many appearance screens call `K0()`, which waits roughly 500 ms then executes `L0()` to update the active launcher preview.

### Explicit subsystem reaction

Some screens override `L0()`/`y0()` to invoke a specific home/wallpaper/service method after the value changes.

### Accessibility service notification

Descriptors flagged accessibility-required/recommended notify the running service configuration state.

### Activity recreation

Used selectively for changes such as global theme/accent where current widgets must be recreated with a new style.

### Deferred/consumer-driven

Some stored preferences are simply read the next time their subsystem executes; they do not need an immediate callback.

This taxonomy should become explicit metadata in the Aerial-Rows settings schema rather than scattered `if` statements.

---

## 22. Recommended Aerial-Rows settings architecture

A clean implementation target is:

```text
SettingDescriptor<T>
 ├─ key
 ├─ default
 ├─ title
 ├─ description
 ├─ icon
 ├─ controlType
 ├─ min/max/step
 ├─ flags
 ├─ optional scope
 ├─ optional migration key
 └─ update policy

SettingsRepository
 ├─ typed read/write
 ├─ migration
 ├─ export/import
 └─ StateFlow<T>

SettingsScreenRegistry
 ├─ General
 ├─ Appearance
 ├─ Cards
 ├─ Categories
 ├─ Left headers
 ├─ Status bar
 ├─ Background / Aerial
 ├─ Idle
 ├─ Power
 ├─ Input
 ├─ Remote
 ├─ Accessibility
 └─ Parental / backup etc.

TV settings controls
 ├─ Switch row
 ├─ Slider row
 ├─ Choice/dropdown row
 ├─ Text row
 ├─ Color row
 └─ Navigation row
```

For focus stability:

```text
LazyColumn items use stable descriptor keys
        ↓
value changes mutate state only
        ↓
no list replacement / no route replacement
        ↓
FocusRequester remains attached to same row
```

Only a true structural change should replace a settings route.

---

## 23. Reliability notes

- `e32.o()` was not trusted from broken JADX Java output. Smali verified the ~500 ms delay and subsequent `cb3.L0()` call.
- `CustomListPreference` was verified as AndroidX `ListPreference`; no unsupported claim of an inline Projectivy dropdown is made.
- The 148 static central registry fields were all mapped programmatically against the decompiled static initializer rather than manually guessing field names.
- `hj3.y` is a lazy container for a dynamic setting descriptor, not a static `yh3` field; automated screen-mapping output that labels it simply `?y` should not be interpreted as an unresolved key.
- Group 7 deliberately does not fully reverse the backup/import serialization format; that belongs to Group 10.
- Input/vendor-specific effects of settings belong to Group 8.
- Accessibility/remote-key consumers belong to Group 9.

---

## 24. Group 7 conclusion

Projectivy's settings system is considerably more systematic than its obfuscated class names initially suggest.

Its global configuration is effectively a **typed schema-driven settings framework** layered over SharedPreferences, with generic rendering and per-subsystem update hooks. Its content-management screens use Leanback Preferences where richer dynamic app/channel/category lists are needed.

The most important implementation lessons for Aerial-Rows are:

1. Keep one typed settings registry rather than hard-coding settings separately in every Compose screen.
2. Separate persistence from presentation.
3. Give each setting a stable key/row identity.
4. Update values in place so focus never jumps to the top.
5. Use dropdown/choice UI for enums while returning focus to the originating row.
6. Mark settings with explicit update policies: immediate, live-preview, subsystem-reload, Activity/retheme, or deferred.
7. Keep dynamic/scoped settings (input IDs, provider IDs, etc.) first-class rather than inventing arbitrary one-off keys.
8. Reuse the appearance-only registry for theme export.
9. Keep category/content management separate from ordinary global preferences.
10. Allow the launcher behind settings to update live without recreating the settings route.

**Group 07 is complete and sufficiently closed for independent reconstruction.**
