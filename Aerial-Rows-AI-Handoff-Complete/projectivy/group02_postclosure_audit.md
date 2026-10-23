# Group 2 post-closure audit — Home architecture

Status: **POST-CLOSURE AUDITED after Groups 3–12**
Original Group 2 SHA-256: `82ff747a7ddd4d9e14293cd8459685e1bc41e16f1c3a24ca07234580a3e47b55`

## Executive conclusion

The original Group 2 report is architecturally sound. Nothing found in Groups 3–12 invalidates its core model:

```text
MainActivity
  -> MainFragment
  -> customized Leanback browse state
  -> generic category/row registry
  -> stable row/card adapters
  -> type-specific card presenters
  -> focus/header/sidebar state
```

No hidden replacement home-screen subsystem was discovered later.

However, the later groups let us improve Group 2 in several material ways:

1. add the **exact built-in category IDs**, not only source-type IDs;
2. add the **dynamic TV-channel category-ID formula**;
3. map the exact global settings controlling sidebar/header/focus/row behavior;
4. distinguish the row hover metadata view from the separate Program Details screen;
5. refine "stable IDs" as **model-specific identities**, not one uniform global identity scheme;
6. clarify the two separate input row families;
7. strengthen the Now Playing and TV-channel row semantics;
8. slightly soften the claim that item type 0 is "fully mapped".

These are completeness/precision upgrades, not architectural corrections.

---

## 1. Genuine completeness miss — exact built-in category IDs

Group 2 mapped source types 0–10 correctly, but `w40.k()` gives the exact fixed IDs as well.

The built-in rows in Projectivy 4.72 are:

| Category ID | Source type | Built-in row |
|---:|---:|---|
| `1` | `1` | Inputs |
| `2` | `0` | Favorites |
| `3` | `2` | TV Apps |
| `4` | `3` | Mobile Apps |
| `5` | `4` | Settings |
| `6` | `4` | Display |
| `7` | `4` | Apps |
| `8` | `4` | System |
| `9` | `4` | About |
| `10` | `5` | Watch Next |
| `11` | `7` | Shortcuts |
| `12` | `8` | Now Playing |
| `13` | `9` | Category Folders |
| `14` | `10` | Live TV |
| `20` | `0` | Video |
| `21` | `0` | Music |
| `22` | `0` | Games |

This matters because later groups show fixed category IDs are used across:
- default-category selection;
- targeted home refresh;
- notification/Now Playing correlation;
- focus restoration;
- persistence.

For example, Group 5 independently verified:

```text
Now Playing category ID = 12
source type = 8
```

So the exact-ID table should now be considered part of the canonical Group 2 home model.

### Aerial-Rows implication

Do not reproduce these opaque integer IDs literally as the new architecture.

Use namespaced stable IDs such as:

```text
builtin:favorites
builtin:watch_next
builtin:now_playing
builtin:inputs
builtin:live_tv
```

and migrate any legacy numeric identity only if needed.

---

## 2. Genuine completeness miss — dynamic TV-channel category IDs

Group 4 closed the dynamic-channel constructor that Group 2 deliberately left at the boundary.

For a Preview Channel:

```text
i5(m60 channel)
```

uses:

```text
category source type = 6
category ID = (channelId + 2000) cast to int
```

and Projectivy caches/reuses dynamic channel-row objects keyed by the platform channel ID.

Therefore the complete Projectivy row-ID picture is:

```text
fixed built-in category IDs
        +
dynamic channel-derived category IDs
```

### Important reconstruction improvement

Projectivy's numeric channel ID is usable for its own runtime reconciliation, but Aerial-Rows should use a stronger stable logical channel identity because OEM/provider firmware can recreate TV-provider numeric IDs.

Recommended identity:

```text
provider package
+ internal_provider_id where present
+ stable provider/display identity fallback
```

with numeric `channelId` treated as the current lookup handle.

---

## 3. Precision upgrade — the exact sidebar state preferences are now known

Group 2 correctly reconstructed three left-header/sidebar states:

1. shown;
2. hidden but revealable;
3. disabled.

Group 7 later provided the exact settings behind the initial state.

```text
hj3.D1 = appearance_left_headers_enabled
hj3.E1 = appearance_left_headers_show_on_launch
```

`MainFragment.C0()` effectively maps them as:

```text
left headers disabled
    -> state 3

left headers enabled + show on launch
    -> state 1

left headers enabled + do not show on launch
    -> state 2
```

The individual sidebar affordances are:

```text
hj3.F1 -> appearance_left_headers_show_icon
hj3.G1 -> appearance_left_headers_show_title
hj3.H1 -> appearance_left_headers_show_reorder
hj3.I1 -> appearance_left_headers_show_settings
```

This confirms Group 2's state-machine interpretation exactly.

---

## 4. Precision upgrade — content-row headers and left headers have separate preference families

Group 2 correctly distinguished:

```text
hv
  = header attached to the content row

w12/r12
  = left navigation/sidebar header
```

Group 7 lets us make the separation explicit in persisted settings.

### Content-row/category presentation

```text
hj3.m1 -> appearance_categories_show_title
hj3.n1 -> appearance_categories_show_icon
hj3.w1 -> appearance_categories_tint_custom_icons
```

### Left navigation pane

```text
hj3.F1 -> appearance_left_headers_show_icon
hj3.G1 -> appearance_left_headers_show_title
hj3.H1 -> appearance_left_headers_show_reorder
hj3.I1 -> appearance_left_headers_show_settings
```

This is useful because "hide category titles" and "hide left-nav titles" are two different settings and should remain different in Aerial-Rows.

---

## 5. Precision upgrade — exact global row/focus controls

Group 2 described these behaviors correctly but did not yet know their final preference names.

Later mapping gives:

```text
hj3.o1 -> appearance_categories_rows_spacing
hj3.p1 -> appearance_categories_focused_category_position_fixed
hj3.q1 -> appearance_categories_hide_rows_above_selection
hj3.r1 -> appearance_categories_remember_focused_card_position
hj3.s1 -> appearance_categories_unselected_row_opacity
hj3.t1 -> appearance_categories_listing_top_margin
hj3.u1 -> appearance_categories_start_margin
hj3.v1 -> appearance_categories_end_margin
hj3.x1 -> appearance_categories_default_category_id
```

### Remember-focused-card behavior

Group 2 said a global preference determines whether a row resets to item 0.

The exact setting is now known:

```text
appearance_categories_remember_focused_card_position
```

When remembering is disabled, Projectivy resets the horizontal row selection to position 0 when the row is selected through the relevant presenter path.

### Vertical focus geometry

Group 2's `hb3` analysis should now explicitly reference the combination of:

```text
appearance_categories_focused_category_position_fixed
appearance_categories_hide_rows_above_selection
```

because both participate in the vertical focus/window-alignment strategy.

---

## 6. Precision upgrade — focused-card visual settings

Group 2 accurately described the selected-card visual system, but Group 7 closed the preference keys.

```text
hj3.c1 -> appearance_cards_program_zoom_on_focus
hj3.d1 -> appearance_cards_focused_scale
hj3.e1 -> appearance_cards_focused_effect
hj3.f1 -> appearance_cards_focused_animation
hj3.g1 -> appearance_cards_focused_glow_size
hj3.h1 -> appearance_cards_focused_glow_tint
hj3.i1 -> appearance_cards_focused_color_override_mode
hj3.j1 -> appearance_cards_focused_bg_tint
hj3.k1 -> appearance_cards_focused_tint
```

So Group 2's "focus effects" are not one effect toggle; they are a configurable family covering scale, animation, glow and color treatment.

---

## 7. Factual refinement — "Hover/details card" should be renamed

Group 2 section 16 called the `vy1 -> wy1/aa4` path a:

> Hover/details card

The implementation itself was described correctly, but later Group 4 proves there is also a separate **Program Details** screen.

The Group 2 feature should therefore be named more precisely:

```text
row hover metadata / description area
```

It contains:
- title;
- short description;
- optional icon;
- animated collapse behavior.

It is **not** the same as the later Program Details side panel/screen.

The separate program-details path can perform an additional lazy TV-provider query for:

```text
long_description
```

using the program `_id`.

That distinction prevents an Aerial-Rows implementation from trying to put the full details experience into the row hover element.

---

## 8. Factual refinement — stable IDs are model-specific, not one universal identity formula

Group 2 correctly emphasizes stable category/card IDs and adapter reuse.

Later groups show that the actual identity formula depends on card type.

Examples:

```text
App
  -> packageName hash in the Projectivy app-card path

Preview Program
  -> primarily intent_uri-based identity

Watch Next
  -> specialized Watch Next identity helper

Now Playing
  -> media-session token-derived identity

Shortcut
  -> local card ID based on shortcut ID hash

Dynamic TV channel row
  -> channel-derived category ID
```

Therefore the canonical statement should be:

> Projectivy consistently *uses stable identity as an architectural principle*, but the actual identifier is type-specific.

It should **not** be interpreted as a single globally collision-proof ID system.

### Aerial-Rows improvement

Use namespaced structured identities:

```text
app:<package>/<component>
program:<provider>/<program identity>
watchnext:<provider>/<program identity>
session:<session identity>
shortcut:<package>/<user>/<shortcut id>
input:<stable input identity>
```

instead of Projectivy-style integer hashes where we control the design.

---

## 9. Precision upgrade — Inputs vs Live TV are two distinct row families

Group 2 already mapped:

```text
source type 1  = Inputs
source type 10 = Live TV inputs
```

Group 8 lets us state the semantic difference much more clearly.

### Category ID 1 — Inputs / source actions

Uses the internal-action/input-action family:

```text
e90
z52 extends e90
item type 1
q64 presenter
```

This is where generic HDMI/AV/source switching actions fit.

### Category ID 14 — Live TV

Uses actual Android TV Input Framework items:

```text
vj2
item type 10
wj2 presenter
```

This is backed by discovered/tunable TV inputs rather than merely an internal "switch to HDMI" action.

Aerial-Rows should preserve this conceptual split:

```text
InputAction
    vs
TvInputSource
```

even if both render as launcher cards.

---

## 10. Precision upgrade — Now Playing row semantics

Group 2 correctly identified the `c73` boundary.

Group 5 lets the home-level description be strengthened:

```text
category ID 12
source type 8
item type 8
model c73
presenter d73
```

Additional home semantics:

- the row can contain multiple active sessions;
- cards are deduplicated at session-token level;
- `c73` is not manually reorderable like ordinary app cards;
- one separate primary session is chosen for global Now Playing/wallpaper state;
- the row is refreshed through the normal `w40` category controller.

This further confirms that Now Playing is a normal dynamic `i5` row source rather than a special overlay.

---

## 11. Precision upgrade — TV channel rows are platform-manageable, not only launcher-local

Group 2 described source type 6 as Android TV channel-backed rows.

Group 4 adds an important behavior:

when a user enables a channel-backed category, Projectivy can call:

```text
TvContract.requestChannelBrowsable(context, channelId)
```

So row visibility has two layers:

```text
Projectivy row/category state
        +
Android TV-provider browsable state
```

The channel-management UI also groups dynamic rows by provider application.

This belongs in the canonical home model because "show channel" is not purely a local Compose/adapter operation.

---

## 12. Precision upgrade — program-card focus event semantics

Group 2 correctly found:

```text
CardFocused
ProgramCardFocused
```

events at the presenter boundary.

Group 6 later proved these are first-class events in the wallpaper-provider event system.

The relevant event families include:

```text
CardFocused
ProgramCardFocused
NowPlayingChanged
LauncherIdleModeChanged
TimeElapsed
```

So focus events should now be described as part of an explicit home-to-wallpaper event bus, not simply "plugin notifications".

For Aerial-Rows, card focus should similarly publish semantic state independent of the renderer.

---

## 13. Small wording correction — item type 0 was not semantically "fully mapped"

The Group 2 closure says:

> fully mapped item presenter types 0–10

The **dispatch table** was mapped, but item type 0 was still described as:

```text
e90 (where used)
generic/internal action card
```

while the active built-in internal rows seen in `w40` predominantly construct `e90` with item type 4, and input actions use type 1.

So a more precise statement is:

> Presenter dispatch slots 0–10 were mapped; active semantics are strongly established for types 1–10, while type 0 remains a generic/legacy-compatible internal-action slot in the inspected 4.72 paths.

This does not affect implementation because Aerial-Rows should use sealed card types rather than numerical presenter IDs.

---

## 14. Things Group 2 correctly deferred — not misses

The following should **not** be counted as failures in Group 2:

- exact installed-app discovery and package update semantics — Group 3;
- generic `j6` card-action menu — Group 3;
- TvProvider query/filter/cache policy — Group 4;
- media-session discovery and session reconciliation — Group 5;
- wallpaper-provider implementation — Group 6;
- preference schema/key names — Group 7;
- HDMI/TvInput vendor launch implementation — Group 8;
- HOME/Accessibility/remote keys — Group 9;
- persistence/icon packs/shortcuts/parental controls — Group 10.

Group 2 correctly stopped at those subsystem boundaries.

---

## 15. Revised canonical home model

With Groups 3–12 folded back into Group 2, the home model is best summarized as:

```text
HomeController / w40
│
├── fixed built-in category registry
│   ├── Inputs            ID 1
│   ├── Favorites         ID 2
│   ├── TV Apps           ID 3
│   ├── Mobile Apps       ID 4
│   ├── Settings          ID 5
│   ├── Display           ID 6
│   ├── Apps              ID 7
│   ├── System            ID 8
│   ├── About             ID 9
│   ├── Watch Next        ID 10
│   ├── Shortcuts         ID 11
│   ├── Now Playing       ID 12
│   ├── Category Folders  ID 13
│   ├── Live TV           ID 14
│   ├── Video             ID 20
│   ├── Music             ID 21
│   └── Games             ID 22
│
├── dynamic TV channel categories
│   └── source type 6 / channel-derived runtime ID
│
├── stable per-category adapters/wrappers
│
├── type-specific cards
│   ├── App
│   ├── InputAction
│   ├── Program
│   ├── WatchNext
│   ├── Shortcut
│   ├── NowPlaying
│   ├── CategoryFolder
│   └── TvInput
│
├── row presentation policy
│   ├── single/multi-row
│   ├── margins
│   ├── card scale/aspect
│   ├── items per line/max rows
│   ├── opacity/background
│   └── hover metadata
│
├── selection/focus policy
│   ├── selected category
│   ├── per-row selected card
│   ├── fixed focus position
│   ├── hide rows above selection
│   └── default category
│
└── left navigation state
    ├── shown
    ├── hidden/revealable
    └── disabled
```

---

## 16. Effect on the Aerial-Rows implementation plan

No Phase 0–11 ordering from Group 12 needs to change.

The audit does strengthen four implementation requirements:

1. **Use namespaced stable string IDs**, not integer/hash identities.
2. **Separate content-row headers from the left navigation rail** in config/state.
3. **Keep per-row selected-card state** and the fixed-row-position/hide-above behaviors as explicit focus policy.
4. **Separate generic input actions from true TvInput Framework sources.**

It also confirms that the current Aerial-Rows `SlidingRows` prototype is conceptually on the correct path, but the row/card identity and focus state need promotion into core models rather than remaining private UI state.

---

## Final assessment

```text
Core Group 2 architecture:        correct
Major missing subsystem:          none
Genuine completeness misses:      2
  - exact built-in category IDs
  - dynamic channel category-ID formula

Factual/wording refinements:      5+
  - hover metadata vs Program Details
  - model-specific stable identities
  - input-action vs Live-TV input distinction
  - exact sidebar/focus preference keys
  - item-type-0 wording

Implementation-plan impact:       refinement only
Redo Group 2 from scratch:        not needed
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 3–12`.**
