# Projectivy 4.72 Reconstruction Dossier — Part 01: Home / Row / Card Architecture

Target APK: `ProjectivyLauncher-4.72-c97-xda-release(2).apk`  
Package: `com.spocky.projengmenu`  
Version: 4.72 (97)  
Analysis stack: REA 6.7.0 + JADX MCP 0.7.1  
Purpose: implementation-grade behavioral/architectural specification for an independent Aerial-Rows implementation.

## 1. Scope and confidence

This part covers the main launcher activity, home fragment, row/category model, card/action model, row population registry, presenters, selection/focus state, reordering, left-header behavior, and the settings keys that directly control the home surface.

Confidence is high for the structural relationships below because they are directly evidenced by class bodies and cross references. Obfuscated field names are not treated as semantic names unless their behavior is independently established.

## 2. Top-level runtime composition

`MainActivity` is the launcher host. It inflates `R.layout.activity_main`, binds `R.id.main_overlay`, resolves the fragment at `R.id.main_browse_fragment`, and stores it as `MainFragment`.

At activity startup it also attaches the wallpaper/display subsystem (`bs4`) to the Activity lifecycle and creates a display scope used by image/video/drawable wallpaper renderers. This means wallpaper rendering is a peer subsystem to the home UI rather than a child of individual rows.

Activity lifecycle behavior relevant to a reimplementation:

- `onCreate`: configures edge/window behavior before normal activity creation.
- `onStart`: marks the launcher foreground state, performs launcher/default-home related work when required, and starts an activity-scoped asynchronous controller.
- `onResume`: refreshes network state, resets the cached TV-input source state, clears accessibility foreground-target state, and performs a general interaction/idle reset.
- `onPause`: records pause timestamp, unregisters connectivity callback, and stops an overlay/color animator.
- `onStop`: marks launcher foreground state false and stops the activity-scoped controller.
- `onDestroy`: cancels animation state and releases the fragment reference.
- `onUserInteraction`: records a high-resolution interaction timestamp used by idle logic.

The Activity intercepts selected remote keys. DPAD directions are fed into the launcher state layer; MENU key long/short behavior is differentiated; HOME causes the home fragment to reset/focus its default state.

## 3. Home UI host

`MainFragment` is the principal Leanback browse fragment. It uses:

- a vertical row list (`VerticalGridView`),
- a row adapter (`po2`),
- a row presenter selector (`ui2`),
- a card presenter selector (`r30`),
- a custom row-header presenter (`r12`),
- a custom title/status container (`MainTitleView`),
- separate dock/header container views.

The fragment inflates `R.layout.custom_browse_title` for its title surface. The underlying row list width and item spacing are dynamically recomputed rather than fixed.

### MainFragment setup flow

The central setup method constructs:

1. row presenter selector,
2. card presenter selector,
3. custom header presenter,
4. rows adapter with stable IDs,
5. an additional presenter for nonstandard row content,
6. Leanback adapter/presenter bindings.

Left-header mode is chosen from two preferences:

- `appearance_left_headers_enabled`
- `appearance_left_headers_show_on_launch`

Observed mode selection:

- enabled + show-on-launch => initial header mode 1,
- enabled + not show-on-launch => initial header mode 2,
- disabled => mode 3.

This is the architectural basis for Projectivy's "headers appear when navigating left" behavior. Aerial-Rows should model this explicitly as a UI state machine rather than always rendering a sidebar.

## 4. Unified row/category model (`i5`)

Projectivy normalizes built-in sections, custom categories, TV-provider channels, Watch Next, inputs, and utility rows into one row model.

Important established properties:

- `j`: stable category/row ID used throughout home selection and lookup.
- `k`: category/row type discriminator.
- `l`: stable external identity/string used particularly for provider-backed rows.
- `f`: current ordered list of cards/actions (`t4`).
- `u`: stored ordering / included-card IDs for ordinary/custom rows.
- `v`: complementary/excluded ordering list used by some row types.
- `m`: per-row scale factor.
- `n`: row enabled/visible switch; a row is renderable only when enabled and it has content (with a special allowance for custom rows).
- `s`: row sorting mode. Observed modes include manual, title-like sorts, another comparator sort, and shuffle/random.
- `t`: custom title override.
- `x`: custom icon URI/string override.
- `y`, `z`, `A`, `B`: additional per-row presentation overrides. Some fall back to global category appearance settings.
- `g`: TV-provider channel backing object when the row represents an Android TV channel.

Row title resolution follows this priority:

1. custom title override,
2. built-in resource title,
3. Android TV channel `display_name`,
4. fallback "new category" string.

Row icon resolution similarly permits a custom URI and falls back to built-in/provider icons.

## 5. Built-in row registry

Projectivy constructs the following built-in rows. The stable ID and type discriminator are both important and should remain separate concepts in our implementation.

| Stable row ID | Row | Type (`k`) | Data source |
|---:|---|---:|---|
| 12 | Now Playing | 8 | Notification/media-session-derived items |
| 1 | Inputs | 1 | TV/input discovery + source helpers |
| 2 | Favorites | 0 | User-selected action IDs |
| 20 | Video | 0 | User/custom category membership |
| 21 | Music | 0 | User/custom category membership |
| 22 | Games | 0 | User/custom category membership |
| 3 | TV Apps | 2 | Leanback/TV application discovery |
| 4 | Mobile Apps | 3 | standard/mobile application discovery |
| 10 | Watch Next | 5 | Android TV Watch Next provider |
| 11 | Shortcuts | 7 | Launcher shortcuts |
| 13 | Folders | 9 | custom category/folder actions |
| 14 | Live TV | 10 | TV/input/live-TV source registry |
| 5 | Settings | 4 | Projectivy static settings actions |
| 6 | Display | 4 | Projectivy display utilities |
| 7 | Apps | 4 | Projectivy app-management utilities |
| 8 | System | 4 | Projectivy system utilities |
| 9 | About | 4 | about/premium/device/background actions |

Android TV provider channels are created dynamically as type 6 rows with stable IDs derived from the TV channel row ID (offset into Projectivy's row namespace), while also retaining provider/package/display identity.

## 6. Row population registry (`w40`)

`w40` is the central home-content repository/coordinator.

Its responsibilities include:

- owning the current ordered row list,
- row lookup by stable ID,
- constructing default rows,
- merging stored row configuration into newly discovered rows,
- populating a row according to its type,
- publishing row/card updates to active `MainFragment` subscribers,
- card reordering,
- row reordering,
- tracking the card/row currently being moved,
- persisting category/card ordering asynchronously.

The row type switch is especially important:

### Type 0 — manual/custom category

Uses the row's stored action-ID ordering/membership list, resolves IDs through the global action registry, and clones/resolves matching actions.

### Type 1 — Inputs

Uses the TV-input/source subsystem. It combines discovered inputs with Projectivy's input/source shortcut actions when available.

### Type 2 — TV Apps

Uses application discovery filtered to TV/Leanback-capable applications.

### Type 3 — Mobile Apps

Uses application discovery filtered to ordinary/mobile launcher activities.

### Type 4 — static Projectivy tools

Uses one of the preconstructed action lists (Settings / Display / Apps / System / About), selected by the row's stable ID.

### Type 5 — Watch Next

Queries the Android TV Watch Next content provider, converts rows to program card objects, optionally limits programs per app using `action_watch_next_programs_per_app`, then filters/reorders them.

### Type 6 — Android TV channel

Retrieves programs belonging to a specific Android TV provider channel and applies row membership/filter/order state.

### Type 7 — launcher shortcuts

Uses the application's dynamic/pinned shortcut registry.

### Type 8 — Now Playing

Uses the notification/media listener's active media-like items, filters for playable/current entries, and de-duplicates by an identity key.

### Type 9 — folders/categories

Builds cards that represent custom category/folder rows.

### Type 10 — Live TV

Uses a live-TV/input-derived collection maintained by the input subsystem.

This registry is the most important home-architecture idea to preserve: the UI should not know where a row's data comes from. A row data source resolves `HomeCard` objects for a `HomeRow` type.

## 7. Unified card/action model (`t4`)

All home cards derive from the common action abstraction `t4`.

Established behavior:

- each action has a stable integer ID (`c`),
- each action has a type/presenter discriminator (`a`),
- it has a default/fallback title,
- it can lazily attach a user customization record (`l5`),
- customization can override title, icon/artwork and selected behavioral flags,
- actions generate their launch `Intent` via an overridable method,
- common launch code adds activity flags/extras and starts the target,
- launch paths that require protection are wrapped through the parental-control gate before `startActivity`,
- failed activity launch may allow the action subtype to refresh/re-resolve its target once and retry,
- action artwork resolves from a customization override first, then the row/source artwork,
- visibility/enabled behavior can be overridden by customization state.

Known action subclasses include:

- application activity cards (`qb`, backed by `ActivityInfo`),
- TV input cards (`vj2`, backed by `TvInputInfo`),
- launcher shortcut cards (`p94`, backed by `ShortcutInfo`),
- notification/media cards (`c73`, backed by Projectivy notification/media object),
- static Projectivy action cards (`e90`),
- provider/program/folder related card classes represented by additional `t4` subclasses.

For Aerial-Rows, use a sealed `LaunchTarget` + `HomeCard` interface while preserving a common launch coordinator/gate. Do not let individual UI composables directly call PackageManager/TV provider/input APIs.

## 8. Presenter routing

`r30` selects the card presenter from the action's type discriminator. The mapping observed in Projectivy is:

- types 0, 1, 4 -> presenter family `q64`
- types 2, 3 -> presenter family `rb`
- types 5, 6 -> presenter family `ek3`
- type 7 -> presenter family `q94`
- type 8 -> presenter family `d73`
- type 9 -> presenter family `dk1`
- type 10 -> presenter family `wj2`

The selector caches one presenter instance per discriminator.

This implies card visual differences are determined primarily by action/card type, not row type. Aerial-Rows should therefore model a `CardPresentation`/`CardKind` independent of `RowKind`.

## 9. Row wrappers and stable IDs

Rows are wrapped in `eb3`. The wrapper holds the `i5` model and Leanback header/row-adapter state.

The top-level rows adapter `po2` returns a stable long ID from the row wrapper, falling back to adapter position only when absent. This stable-ID design is important for focus retention and incremental updates.

Aerial-Rows should use stable Compose keys for both rows and cards and must not key TV-provider rows only by Android provider row ID because those IDs can be recycled by OEM providers after restart.

## 10. Header presenter

`r12` renders a category header using the row's resolved title and icon. It supports custom/provider icons and applies the global category icon-tint behavior.

Relevant global settings:

- `appearance_categories_show_title`
- `appearance_categories_show_icon`
- `appearance_categories_tint_custom_icons`
- `appearance_left_headers_show_icon`
- `appearance_left_headers_show_title`
- `appearance_left_headers_show_reorder`
- `appearance_left_headers_show_settings`

## 11. Selection / focus model

Projectivy tracks focus at two levels:

- selected row/category,
- selected card within the row.

Important behavior:

- the default row comes from `appearance_categories_default_category_id`.
- row selection searches by stable row ID, not merely adapter position.
- card focus can be restored to a specific action within a row.
- `appearance_categories_remember_focused_card_position` controls whether row-specific card focus is retained.
- `appearance_categories_focused_category_position_fixed` affects row repositioning when returning home/focusing the default row.
- `appearance_categories_hide_rows_above_selection` changes which surrounding rows remain visible.

On HOME/reset behavior, the fragment requests focus on the home dock, conditionally scrolls back toward the default row, and restores default row/card focus.

## 12. Reorder model

The central repository tracks an active card move and active row move separately.

Card reordering:

- resolves current card index by stable action ID,
- updates both stored ID ordering and the current visible card list together,
- supports adjacent swap and arbitrary move,
- publishes an update event containing old/new indices.

Row reordering:

- skips rows that are not currently renderable,
- converts absolute list positions to visible-row positions for UI notifications,
- moves the underlying row in the complete row list,
- publishes old/new visible indices.

The fragment consumes repository update events and updates the relevant Leanback adapter ranges/moves rather than blindly rebuilding the whole home screen.

Aerial-Rows should do the same with immutable row/card state + stable keys + targeted list changes.

## 13. Home-related persisted settings recovered

### General / home

- `action_categories_list`
- `action_customizers_list`
- `appearance_general_load_cards_while_scrolling`
- `appearance_general_tint`

### Cards

- `appearance_cards_show_title`
- `appearance_cards_rounded_corners`
- `appearance_cards_size`
- `appearance_cards_spacing`
- `appearance_cards_loop_limit`
- `appearance_cards_ripple_effect`
- `appearance_cards_bg_tint`
- `appearance_cards_tint`
- `appearance_cards_tv_app_aspect_ratio`
- `appearance_cards_mobile_app_aspect_ratio`
- `appearance_cards_icon_pack`
- `appearance_cards_program_play_preview`
- `appearance_cards_program_show_progressbar`
- `appearance_cards_program_show_badge`
- `appearance_cards_program_zoom_on_focus`
- `appearance_cards_focused_scale`
- `appearance_cards_focused_effect`
- `appearance_cards_focused_animation`
- `appearance_cards_focused_glow_size`
- `appearance_cards_focused_glow_tint`
- `appearance_cards_focused_color_override_mode`
- `appearance_cards_focused_bg_tint`
- `appearance_cards_focused_tint`

### Categories / rows

- `appearance_categories_show_title`
- `appearance_categories_show_icon`
- `appearance_categories_rows_spacing`
- `appearance_categories_focused_category_position_fixed`
- `appearance_categories_hide_rows_above_selection`
- `appearance_categories_remember_focused_card_position`
- `appearance_categories_unselected_row_opacity`
- `appearance_categories_listing_top_margin`
- `appearance_categories_end_margin`
- `appearance_categories_tint_custom_icons`
- `appearance_categories_default_category_id`
- `appearance_categories_dock_padding`

### Left headers

- `appearance_left_headers_enabled`
- `appearance_left_headers_show_on_launch`
- `appearance_left_headers_show_icon`
- `appearance_left_headers_show_title`
- `appearance_left_headers_show_reorder`
- `appearance_left_headers_show_settings`

### Idle / status surface

- `appearance_idle_timeout_duration`
- `appearance_idle_opacity`
- `appearance_idle_no_clock_fade`
- `appearance_idle_prevent_screensaver`
- `appearance_statusbar_manage_notifications`
- `appearance_statusbar_autohide`
- `appearance_statusbar_show_system_statusbar`
- `appearance_statusbar_maincontainer_icon_size`
- `appearance_statusbar_maincontainer_icon_tint`
- `appearance_statusbar_maincontainer_icon_background_tint`
- `appearance_statusbar_maincontainer_show_settings`
- `appearance_statusbar_maincontainer_show_battery`
- `appearance_statusbar_maincontainer_show_connectivity`
- `appearance_statusbar_maincontainer_show_notifications`
- `appearance_statusbar_maincontainer_autohide_notifications`
- `appearance_statusbar_clock_show`
- `appearance_statusbar_clock_size`
- `appearance_statusbar_clock_format_l1`
- `appearance_statusbar_clock_format_l2`
- `appearance_statusbar_clock_tint`
- `appearance_statusbar_clock_drop_shadow_tint`

### Provider row limits

- `action_watch_next_programs_per_app`
- `action_channel_general_programs_per_channel`

## 14. Recommended clean implementation in Aerial-Rows

Use the following architecture rather than encoding Projectivy's obfuscated classes:

```text
HomeRepository
  |- RowRegistry
  |    |- ManualCategorySource
  |    |- TvAppsSource
  |    |- MobileAppsSource
  |    |- InputsSource
  |    |- WatchNextSource
  |    |- TvChannelSource
  |    |- ShortcutSource
  |    |- NowPlayingSource
  |    |- FolderSource
  |    `- StaticToolsSource
  |
  |- CustomizationRepository
  |- OrderingRepository
  `- HomeEventBus / StateFlow

HomeViewModel
  `- List<HomeRowState>
       `- List<HomeCardState>

LaunchCoordinator
  |- ParentalGate
  |- PackageLauncher
  |- IntentLauncher
  |- TvInputLauncher
  `- SystemActionLauncher
```

Suggested public models:

```kotlin
sealed interface RowKind {
    data object Manual : RowKind
    data object Inputs : RowKind
    data object TvApps : RowKind
    data object MobileApps : RowKind
    data object StaticTools : RowKind
    data object WatchNext : RowKind
    data class TvChannel(val stableProviderKey: String) : RowKind
    data object Shortcuts : RowKind
    data object NowPlaying : RowKind
    data object Folders : RowKind
    data object LiveTv : RowKind
}

data class HomeRow(
    val id: Long,
    val kind: RowKind,
    val title: String,
    val icon: Artwork?,
    val enabled: Boolean,
    val order: Int,
    val scale: Float,
    val sort: RowSort,
    val cards: List<HomeCard>,
)

data class HomeCard(
    val id: Long,
    val kind: CardKind,
    val title: String,
    val artwork: Artwork?,
    val launchTarget: LaunchTarget?,
    val customization: CardCustomization?,
)
```

## 15. Part 01 completion criteria

Completed:

- launcher activity -> home fragment relationship
- central row repository identified
- row model identified
- card/action base model identified
- built-in row registry recovered
- every row-type population route recovered at architectural level
- presenter routing recovered
- focus/default row behavior recovered
- card and row reorder state recovered
- left-header state recovered
- relevant persisted preference keys recovered

Not intentionally folded into this part because they require their own trace:

- exact TV Provider / Watch Next projections, observers and deep links (Part 02)
- application discovery and long-press actions (Part 03)
- full settings schema/defaults/migrations (Part 04)
- wallpaper engine/provider contract (Part 05)
- notification/media session/Now Playing lifecycle (Part 06)
- TV input/vendor source launch mechanics (Part 07)
- accessibility/remote/boot/power state machines (Part 08)
- parental-control implementation (Part 09)
- backup/theme/icon/customization serialization (Part 10)
- display/device/engineering/support utilities (Part 11)
