# Projectivy 4.72 — Group 02: Home screen architecture

Status: **complete / second-pass reviewed**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group traces Projectivy's home UI from the launcher Activity into the browse fragment, row/category models, row presenters, card model/presenter hierarchy, focus/header state, row updates, hover/details UI and top/status UI. Content acquisition internals for apps, channels, Watch Next and media sessions are deliberately deferred to Groups 03–05; here they are mapped only where they cross the home architecture boundary.

## 1. High-level home graph

```text
MainActivity
  -> activity_main.xml
      -> MainFragment
          -> customized/minified Leanback BrowseSupportFragment base (`kz`)
              -> headers fragment (`db3` -> `vw1`)
              -> rows fragment (`hb3` -> `ly3`)
              -> BrowseFrameLayout focus/transition coordinator
          -> MainTitleView
          -> rows adapter (`po2`)
              -> row wrapper (`eb3`)
                  -> category header model (`q12`)
                  -> item adapter (`oo2`, stable IDs)
                  -> category model (`i5`)
                      -> item/card models (`t4` subclasses)
          -> row presenter selector (`ui2`, mode 0)
              -> single-row presenter (`jo4`)
              -> multi-row/grid presenter (`f23`)
          -> card presenter selector (`r30`)
          -> header presenter (`r12` -> `w12`)
          -> hover-card presenter selector (`ui2`, mode 2)
              -> `vy1`
                  -> `wy1` / animated `aa4`
```

Important boundary: `kz`, `vw1`, `ly3`, and several related classes are minified/customized AndroidX Leanback machinery. Projectivy-specific logic is layered around and inside that base through `MainFragment`, `db3`, `hb3`, `i5`, `w40`, `r30`, `r12`, `w12`, `ct` and its subclasses. Reimplementation should reproduce behavior, not blindly recreate Leanback internals that AndroidX already provides.

## 2. MainFragment responsibilities

`MainFragment` has 23 methods and 12 fields. It is the coordinator for the launcher home surface.

Confirmed responsibilities:

- installs the row presenter selector, card presenter selector and custom header presenter;
- owns the top/title/status view (`MainTitleView`);
- owns the visible rows adapter;
- maps category IDs to reusable item adapters and row wrappers;
- chooses one of three header/sidebar states from preferences;
- restores/selects rows and cards;
- updates/moves rows and cards without rebuilding the whole screen;
- recomputes row alignment/margins when configuration changes;
- reconnects adapters and presenters after view/configuration rebuilds;
- fades the header dock, content dock and title/status elements together;
- registers with the global home/category controller (`w40`) during its lifecycle.

### `C0()` — home renderer construction

This method is the central presenter/adapter setup path.

It constructs:

- `to2` — presenter selector for left-side category headers;
- `r30` — presenter selector for cards/items;
- `ui2(mode 0)` — row presenter selector;
- `r12` — custom category-header presenter;
- `po2` — top-level rows adapter, stable-ID enabled.

It then attaches that adapter to the rows fragment.

## 3. Sidebar / header state machine

The Leanback-derived browse base uses three explicit header states:

| State | Meaning |
|---|---|
| `1` | headers enabled and currently shown |
| `2` | headers enabled but currently hidden |
| `3` | headers disabled entirely |

`MainFragment.C0()` chooses between these from two preferences. Semantically the launcher can therefore support:

1. always-visible/initially-visible headers;
2. hidden headers that can be revealed by navigating left;
3. no headers at all.

This is the architectural mechanism behind the Projectivy behavior the Aerial-Rows project wants: **the left navigation column is not simply conditionally drawn; it is a focusable browse pane with a real shown/hidden state and transition.**

### State persistence

The browse layer saves/restores:

- selected row position;
- whether headers are currently shown;
- page-row state;
- title visibility.

The rows fragment also restores its selected row position.

### Focus switching

When the fragment resumes:

- if headers are enabled and shown, focus is requested on the headers fragment;
- otherwise focus is requested on the rows/content fragment.

Header transitions are coordinated through `m0()/n0()` and are deferred if the row list is actively scrolling or laying out. This prevents a left/right header transition from fighting RecyclerView/Leanback focus movement.

### Header transitions

Transitions use Leanback browse header-in/header-out transitions. Before transition start, rows are frozen/focus search is disabled; the transition swaps focusable panes and later restores normal layout/focus behavior.

The implementation also supports headers-back-stack behavior inherited from Leanback.

## 4. Selected-row coordinator

`jz` is a small but important deferred selection coordinator.

It stores a requested row index and priority, posts itself to the browse frame, and when executed:

- updates `kz.n1` (current selected row);
- synchronizes selected position in the headers list;
- synchronizes selected position in the rows/content list;
- performs smooth or immediate selection;
- swaps the content fragment if required;
- updates title visibility/state;
- reevaluates whether the sidebar/title can be shown.

This means row selection is centrally synchronized across both panes instead of letting the header list and content list drift apart.

### Startup/default-row and refocus behavior

`MainFragment.s0()` resolves a preferred/default category ID from the home preferences, converts that category ID into the **current visible-row index**, and falls back to row `0` if the category is absent/hidden. It therefore does not persist a brittle adapter index.

`MainFragment.w0(category, card)` refocuses by **stable category ID + stable card ID** after the adapters settle. If no explicit category/card is supplied it refocuses the resolved default category and its first item. This is performed asynchronously on the fragment lifecycle scope rather than synchronously during adapter mutation.

`MainFragment.y0()` is the “return focus to home content” path: it requests focus on the content dock and, when the relevant navigation preference requires it and the current row lies below the resolved default-row position, explicitly selects **row 0** before calling `w0(null, null)` to restore a valid card focus. This is another reason Projectivy avoids the common launcher bug where returning from settings or an app leaves focus at an invalid/stale position.

## 5. Category model (`i5`)

`i5` is the configurable home-category/row model. It has 31 fields and 19 methods.

### Confirmed fields / behavior

The following semantics are confirmed from construction and use sites:

- stable category ID;
- category/source type;
- default title resource / custom title;
- default icon / custom icon URI;
- category accent/icon colour;
- list of current `t4` items;
- per-category item ordering list;
- card aspect/style (`df`);
- category scale factor;
- visibility;
- multi-row/grid mode;
- items-per-line override;
- sorting mode;
- max-row override;
- start margin percentage;
- end margin percentage;
- top margin percentage;
- optional row background colour;
- external Android-TV channel metadata for channel-backed categories.

### Verified field-to-setting mapping

A second-pass trace through `CategoryPreferencesFragment` and its advanced subclass (`d40`) removes the remaining ambiguity around the main presentation fields:

| `i5` field | Verified meaning | Evidence at the home/settings boundary |
|---|---|---|
| `n` | category visible | `category_visible` reads/writes `i5.n`; `i5.l()` uses it as the first visibility gate |
| `o` | multiline / multi-row grid | `category_multiline` reads/writes `i5.o`; `ui2` selects `f23` when true |
| `q` | **inverse** of “sliding single row” | `category_sliding_single_row` displays `!i5.q` and writes `i5.q = !switch`; `ui2` passes `!q` into the single-row presenter’s custom sliding/window-alignment behavior |
| `r` | **inverse** of “show card title” / hover details | `category_show_app_title` displays `!i5.r` and writes `i5.r = !switch`; hover presenter selection returns null when `r` is true |
| `m` | per-category card scale | `category_scale` maps discrete UI values to `0.6/0.8/1.0/1.2/1.4`; multiplied by the global card scale |
| `p` | items per line | `category_items_per_line`; `0` means automatic |
| `w` | maximum rows | `category_max_rows`; `0` means automatic |
| `y` | start margin percentage | `category_start_margin`; falls back to the global start-margin preference when `0` |
| `z` | row right-edge percentage / end-margin control | `category_end_margin`; `100` means use the global value, and the renderer computes the physical end margin as `(1 - z/100) * screenWidth` |
| `A` | top margin percentage | `category_top_margin` |
| `B` | multi-row background colour | used by the rounded background decoration only for eligible multi-row layouts |
| `s` | ordering/sort mode | bound to `category_order` |
| `t` | custom title | used before the default/resource/channel title |
| `x` | custom icon URI | used before the default icon when allowed |

`i5.i` is not a user-facing toggle. It is derived from category/item content and determines whether the hover area reserves the taller description-capable height. This is why rows with program/media metadata can reserve more vertical hover space than simple app rows.

These names are obfuscated, but their behavioral effect is clear from the presenter logic.

### Visibility rule

`i5.l()` determines whether the category should appear. A category must be enabled and normally non-empty; special generic/custom category types are allowed to remain visible under specific rules.

### Sorting

The category implements multiple sort modes, including:

- stored/manual order;
- case-insensitive alphabetic;
- other comparators tied to card metadata;
- shuffled/random order with persistence of the resulting order.

Detailed app/program sorting semantics belong in Groups 03/04.

## 6. Built-in category/source types

`w40.k()` constructs Projectivy's built-in row definitions.

Confirmed category source types:

| Category type | Built-in usage |
|---:|---|
| `0` | general/custom/favourites-style categories (also Video/Music/Games/custom user rows) |
| `1` | Inputs |
| `2` | TV Apps |
| `3` | Mobile Apps |
| `4` | Projectivy internal action rows (Settings, Display, Apps, System, About) |
| `5` | Watch Next |
| `6` | Android TV channel-backed category (constructed from `TvContract` channel metadata) |
| `7` | Android launcher shortcuts |
| `8` | Now Playing |
| `9` | Category folders |
| `10` | Live TV inputs |

Built-in category IDs also include separate default rows for favourites, video, music, games, TV apps, mobile apps, Watch Next, shortcuts, folders, Live TV, Now Playing, inputs, settings/display/apps/system/about.

This is important for reconstruction: **Projectivy's home is a generic category registry, not a set of unrelated hard-coded screens.**

## 7. Row wrapper and stable adapters

Each visible row is an `eb3` object. It combines:

- `q12` — lightweight category header object (ID/title/category reference);
- `oo2` — item adapter;
- `i5` — category configuration/data model.

`oo2` returns a card's own stable integer ID as the adapter stable ID. Top-level rows are also stable-ID backed.

`MainFragment` maintains two maps keyed by category ID:

- category ID -> reusable `oo2` card adapter;
- category ID -> reusable `eb3` row wrapper.

When the home model refreshes, existing adapters/wrappers are reused instead of recreating every row. Hidden/removed categories are evicted from those maps.

## 8. Global home/category controller (`w40`)

`w40` is the home model/controller boundary. It is both a lifecycle observer and a broadcaster to active `MainFragment` instances.

Confirmed responsibilities at the Group-02 boundary:

- owns the global ordered category list;
- owns category lookup by ID;
- constructs built-in category definitions;
- merges persisted category configuration into new/default definitions;
- broadcasts model refreshes to active home fragments;
- broadcasts row movement;
- broadcasts item movement;
- tracks temporary row-reorder state;
- tracks temporary item-reorder state;
- asks each active home fragment to refocus a category/card;
- triggers persistence/update work after reorder operations.

### Refresh algorithm

On a full category refresh, each active `MainFragment`:

1. iterates the global ordered category list;
2. ignores categories that should not currently be visible;
3. obtains/reuses an `oo2` item adapter for the category;
4. obtains/reuses an `eb3` row wrapper;
5. replaces the item adapter contents with the category's current items;
6. if an otherwise-visible category has no normal items, inserts a concrete `dp2` placeholder card rather than leaving a focusable empty row; `dp2` opens that category’s **Apps** configuration (`settings_category_apps` / `AllAppsSidePanelActivity`) so the user can populate it;
7. replaces/reorders the top-level rows adapter using the resulting list;
8. reports first draw when appropriate.

This gives Aerial-Rows a useful target: use stable category IDs and stable card IDs so updates/reorders do not destroy focus state.

## 9. Row presenter selection

`ui2(mode 0)` selects one of two Projectivy row presenters per category.

### `jo4`

Normal single-row presenter.

It supports standard horizontal browsing plus custom behavior for touch/scroll settling and window alignment.

### `f23`

Multi-row/grid presenter.

It extends `jo4` and configures the horizontal grid's internal layout manager for multiple rows. It keeps per-category state used while arranging grid rows.

The choice is driven by the category's multi-row flag.

### Presenter cache key and transitional visibility fallback

The row presenter selector does not construct a new presenter for every bind. `ui2(mode 0)` caches presenters using a key derived from:

- effective card scale (`globalScale × categoryScale`);
- single-row vs multi-row mode;
- sliding-single-row mode (`i5.q`).

Categories with equivalent presentation parameters therefore reuse presenter instances. A setting that changes one of those parameters naturally resolves to a different cached presenter without requiring a global UI rewrite.

`MainFragment.C0()` then wraps the row selector with `ui2(mode 1)` plus an empty `c92` presenter. If an `eb3` row becomes non-visible (`!i5.l()`) while an adapter/layout transition is still in flight, the wrapper returns the empty presenter instead of attempting to render stale content. This is a small but important transition-safety layer in addition to the normal full-refresh removal of hidden categories.

## 10. Row layout algorithm

`lu`, the Projectivy row-presenter base, calculates row layout from the `i5` category model at bind time.

Confirmed inputs:

- global card-scale preference;
- category-specific scale;
- card aspect/style;
- item spacing;
- category start/end margins (percentage of display width);
- category top margin (percentage of display height);
- explicit items-per-line override;
- automatic items-per-line calculated from available width/card width;
- multi-row flag;
- maximum-row override;
- available vertical screen space;
- hover/title area height;
- focus zoom factor;
- optional rounded row-background colour.

For multi-row categories it computes how many rows are needed, caps the value, then also caps it by the vertical space available and the category's max-row setting.

The HorizontalGridView's height is then explicitly resized to fit the chosen number of card rows.

This confirms that Projectivy category settings such as items-per-line/max rows/margins are part of the actual layout algorithm, not superficial decoration.

### Reselection behavior

A global preference controls whether selecting a row resets its internal horizontal selection back to item zero. Otherwise the row can preserve its prior card position.

### Non-selected row fading

Rows can be rendered with reduced alpha based on a global preference. Selected-row interpolation drives the alpha of both the row content and row header.

### Vertical rows alignment (`hb3`)

The rows fragment also customizes the **vertical** focus geometry. Its `d0()` path selects the `VerticalGridView` focus-scroll strategy from navigation preferences and, in the custom-alignment mode, computes a top/window-alignment offset from:

- title/status-bar height;
- whether in-row headers are enabled;
- global card scale;
- current Leanback focus-zoom factor.

It then applies matching window/item alignment and a compensating top margin. In practical terms, Projectivy deliberately anchors the focused row underneath the top/status region instead of relying on RecyclerView’s default “scroll just enough to reveal” behavior. This should be reproduced explicitly in a Compose implementation with controlled scroll positioning rather than left to default `LazyColumn` focus scrolling.

## 11. Card model hierarchy (`t4`)

`t4` is the abstract/common launchable-card model. It exposes title, description, icon/artwork, category reference, launch Intent construction, stable ID, enabled/available state, visual aspect information and additional capability flags.

### Presenter type IDs

`r30` maps card item type IDs to presenter implementations:

| Item type | Model | Semantic role | Presenter |
|---:|---|---|---|
| `0` | `e90` (where used) | generic/internal action card | `q64` |
| `1` | `z52` extends `e90` (and configured `e90` input actions) | detected/configured input-source action | `q64` |
| `2` | `qb` | TV app | `rb` |
| `3` | `qb` | mobile app | `rb` |
| `4` | `e90` | Projectivy internal action card | `q64` |
| `5` | `b15` extends `ck3` | Watch Next program | `ek3` |
| `6` | `ck3` | Android TV channel program | `ek3` |
| `7` | `p94` | `LauncherApps` shortcut | `q94` |
| `8` | `c73` | Now Playing / MediaController item | `d73` |
| `9` | `ck1` | category-folder card | `dk1` |
| `10` | `vj2` | Android TV input | `wj2` |

This 0–10 **item type** must not be confused with the `i5.k` **category/source type** table above.

The inheritance distinction matters: `b15` is the Watch Next-specific `ck3` subclass and constructs the base with item type `5`; direct `ck3` instances use item type `6` for ordinary channel programs. Likewise, `z52` extends `e90` and is used for detected HDMI/AV/source actions at item type `1`, while configured input-source popup actions can also be represented by `e90` with that type. This keeps source discovery semantics in the model class while reusing the same `q64` presentation family.

## 12. Card presenter base (`ct`)

All main card presenters derive from `ct`.

Confirmed responsibilities:

- creates Projectivy's `MainCardView`-derived view (`bt`);
- dynamically computes card width/height from aspect + category/global scale;
- loads card artwork asynchronously;
- uses stable item/category information as image-cache/signature input;
- updates card content description;
- applies enabled/disabled alpha;
- controls selected/unselected background/icon tint behavior;
- tracks the currently selected card;
- emits wallpaper-provider/plugin `CardFocused` events including dynamic colours, title and app package where relevant;
- delegates specialized binding/focus behavior to subclasses.

Card image size is therefore not fixed in XML. It is recomputed per card/category.

## 13. Card view / focus effects

`bt` is the concrete home-card view wrapper around Projectivy's common card view hierarchy.

On selection change it:

- informs the presenter (`ct.r`);
- updates selected colours/effects;
- can trigger Projectivy's focused-card visual effect depending on preferences;
- notifies the wallpaper/plugin event system.

The base card view supports:

- selected Z/elevation;
- glow/shadow effects;
- image alpha transition;
- dynamic primary and secondary colours;
- progress bar;
- badge artwork;
- card/image aspect handling.

The exact app long-press/context-action content is intentionally deferred to Group 03, but the card view already wires click, long-click and context-click listeners when hosted by `MainActivity`.

## 14. Program-card special behavior

The program presenter (`ek3`, item types 5/6) additionally:

- displays playback progress from TV-provider metadata when enabled;
- optionally displays a channel/program logo badge;
- emits a more detailed `ProgramCardFocused` plugin event;
- recognizes `preview_video_uri`;
- starts/stops preview playback on focus according to settings/lifecycle.

The underlying TV-provider query/data-building code is Group 04, but the home renderer boundary is now closed.

## 15. Now Playing card boundary

Item type `8` (`c73`) represents a live MediaController/media-session card. At the home layer it provides title/subtitle/artwork and launch/control behavior through the common card system.

The actual session discovery/notification/media logic is Group 05.

## 16. Hover/details card

Projectivy uses Leanback's row-hover area with a custom hover-card presenter.

`ui2(mode 2)` is the hover presenter selector. When hover-card display is enabled globally and not suppressed for the category, it returns `vy1`.

`vy1` binds:

- card title (`t4.A()`);
- description (`t4.o()`);
- optional app/program icon where appropriate;
- a unique shared-element transition name for the title.

The concrete hover view is `wy1`, which inflates `hover_card_view.xml` and contains:

- title;
- up-to-three-line description;
- optional 80x45dp icon.

Depending on preference, `vy1` creates either:

- normal `wy1`, or
- animated `aa4`.

`aa4` animates the hover area out by collapsing its height instead of abruptly removing it.

This closes the earlier Group-02 uncertainty around `hover_card_view.xml`: it is actively used by the home row presenter system.

## 17. Content-row header (`hv`)

Rows can also have a row header attached to their content presentation.

`hv` binds `row_header.xml` and uses the category model to apply:

- category title;
- optional category icon;
- category start/end margins;
- category top margin;
- icon tint/loading behavior.

Its layout margins deliberately mirror the card-row margins, keeping the row label aligned with its content.

This content-row header is separate from the **left navigation header pane** described next.

## 18. Left navigation header (`r12` / `w12`)

`r12` creates custom `w12` header views for the sidebar.

A `w12` contains:

- category icon;
- category text;
- reorder affordance;
- settings affordance.

Global preferences can independently hide:

- reorder affordance;
- settings affordance;
- header text;
- header icon.

If both icon and text are hidden, the base header focus target is removed from the internal focus sequence.

### Sidebar focus behavior

`w12.focusSearch()` implements custom horizontal focus between the visible internal elements. When moving out of the row-header controls toward content, focus falls back to the browse hierarchy.

### Selected-header animation

When a category header becomes selected, its secondary controls animate into view with staggered translation/alpha. When deselected they collapse/fade again.

### Category actions

From the header:

- settings opens that category's settings screen;
- long-press settings has a secondary action path;
- reorder enters category reorder mode;
- certain Android-TV channel categories support launching their source/provider app via long press.

### Reorder mode

`w40.O` tracks the category being reordered. While this mode is active:

- DPAD up/down calls `w40.q(-1/+1)`;
- the category is moved only across other visible categories;
- top-level rows are updated with adapter move notifications;
- leaving reorder mode restores normal focus/action behavior and triggers persistence.

This is a strong implementation target for Aerial-Rows: category reorder can be an explicit UI state rather than drag-and-drop.

## 19. Row/card movement without focus loss

Projectivy does not rebuild the entire home screen when an item moves.

For card reorder, `MainFragment.u0()` obtains the existing category adapter, emits an item-change notification where needed, and uses adapter move operations when source/destination differ.

For row reorder, `w40` broadcasts the move and `MainFragment` moves the existing top-level row adapter entry, then refocuses the category.

`MainFragment.w0()` and `x0()` schedule refocusing of a specific category/card after the adapter settles.

This is directly relevant to the user's settings/focus requirement: preserving stable adapter objects and IDs is one of the reasons Projectivy can change state without sending focus back to the first item.

## 20. `MainTitleView` / top status area

`MainFragment` inflates `custom_browse_title` as the title/top area and keeps a `MainTitleView` reference.

The title/status view contains:

- clock;
- wallpaper/view-mode control;
- VPN control;
- network/connectivity control;
- notifications control + notification count;
- settings control;
- battery control;
- Material button group container.

Confirmed interactions include:

- Settings -> Projectivy settings side panel;
- Notifications -> Projectivy notification side panel;
- Network -> Android Wi-Fi settings;
- VPN -> Android VPN settings;
- wallpaper/view-mode button -> configured Intent/action path.

The status controls have custom left/right focus traversal that skips hidden buttons and prevents focus from disappearing at the edges.

Preferences control clock visibility/size, settings visibility, notification visibility/count behavior, icon scale/padding, colours/background tint and other status presentation.

`MainFragment.q0()` drives a coordinated 1-second alpha transition for the **headers dock and content dock**. When its status-bar flag is set, it also fades the `MainTitleView` status-icon button group to the requested alpha while keeping the `MainTitleView` root at a separate `0.8` alpha. The previous wording that all title elements simply share one alpha was too broad.

`MainFragment.Z(boolean)` also overrides the Leanback title-visibility decision with a Projectivy preference, so top/title visibility is not solely a consequence of the current selected row.

## 21. Dynamic configuration refresh

`MainFragment.v0()` is effectively a home renderer refresh path used after relevant preference/configuration changes.

It:

- validates the fragment/view is active;
- asks the rows fragment to refresh;
- recomputes top margins/alignment;
- refreshes the top title/status bar;
- recomputes header-row width/spacing;
- rebuilds/rebinds presenter selection;
- asks `w40` to refresh the model.

The architecture therefore separates:

```text
model refresh (w40)
from
renderer/presenter refresh (MainFragment / db3 / hb3)
```

That separation is worth preserving in Aerial-Rows.

## 22. Leanback machinery vs Projectivy-specific behavior

### Mostly Leanback/minified framework machinery

- `kz` browse-fragment state/transition base;
- `vw1` headers fragment base;
- `ly3` rows fragment base;
- RecyclerView/GridLayoutManager helper layers;
- standard browse header transition plumbing.

### Projectivy-specific or materially customized layers

- `MainFragment`;
- `db3` sizing/spacing for sidebar;
- `hb3` row alignment/spacing/performance behavior;
- `i5` category model;
- `w40` category/home controller;
- `eb3` / `q12` category-row wrappers;
- `r12` / `w12` sidebar presentation/actions;
- `ui2` Projectivy row + hover presenter selection;
- `r30` card presenter selection;
- `ct` card presenter base and its type-specific presenters;
- `MainTitleView` status/actions;
- wallpaper/provider focus event emission.

This distinction prevents an Aerial-Rows rewrite from unnecessarily cloning AndroidX internals.

## 23. Recommended Aerial-Rows home architecture derived from Group 02

Do not preserve Aerial-Rows' current assumption that a home category is merely a list of installed apps.

A more faithful reconstruction should use something like:

```text
HomeController
  categories: StateFlow<List<HomeCategory>>

HomeCategory
  id
  sourceType
  title/icon/colour
  visible
  presentationConfig
  orderingConfig
  items

PresentationConfig
  cardScale
  cardAspect
  multiRow
  itemsPerLine
  maxRows
  startMarginPercent
  endMarginPercent
  topMarginPercent
  rowBackground
  showHoverDetails
  singleRowScrollMode

HomeItem (sealed interface)
  App
  InternalAction
  TvProgram
  Shortcut
  NowPlaying
  CategoryFolder
  TvInput

HomeSelectionState
  selectedCategoryId
  perCategorySelectedItemId
  sidebarState
  reorderMode
```

For the Compose implementation, the behavioral equivalent of Projectivy's Leanback setup should be maintained even if the actual UI toolkit changes:

- stable keys for categories and cards;
- independent vertical category selection and per-row horizontal item selection;
- hidden/revealable/disabled sidebar states;
- explicit focus transfer when sidebar opens/closes;
- do not rebuild LazyRow/LazyGrid objects unnecessarily on settings changes;
- retain per-category card position and restore selection by category/card IDs rather than raw adapter indices;
- item and row moves should use stable IDs and state mutation, not full list reset;
- visible-but-empty custom categories should expose an intentional “configure/populate this row” card rather than a dead empty focus surface;
- hover/details should be a first-class presenter/state, not text hacked into individual card composables;
- category presentation settings must feed the layout calculation directly.

## 24. Group-02 closure / deferred work

The **home renderer and interaction architecture is now closed enough for independent implementation**.

The following are intentionally not considered missing Group-02 work because they are separate subsystem groups:

- where installed-app lists come from and exact app filtering -> Group 03;
- complete click/long-press app action menu -> Group 03;
- TvProvider/channel/Watch Next query/update observers -> Group 04;
- media-session/notification discovery -> Group 05;
- actual wallpaper-provider implementation and video wallpaper behavior -> Group 06;
- exact persistence keys/settings screens for every category field -> Group 07/10.

## 25. Corrections/improvements over the earlier Group-02 pass

The review added or corrected the following:

1. separated **category/source type IDs** from **card item/presenter type IDs**;
2. fully mapped built-in category types 0–10;
3. fully mapped item presenter types 0–10 and corrected the model inheritance split (`z52`/`e90` for type 1; `b15`/`ck3` for types 5/6);
4. confirmed the full hover-card path `ui2 -> vy1 -> wy1/aa4` and active use of `hover_card_view.xml`;
5. confirmed `hv` is a separate in-row header from the left navigation header `w12`;
6. traced the category refresh algorithm and stable row/item adapter reuse;
7. traced row and card reorder update paths and their focus-preserving behavior;
8. confirmed the row layout algorithm consumes the configurable margins/items-per-line/max-rows/background/scale settings directly;
9. confirmed explicit shown/hidden/disabled sidebar states plus saved/restored selection/header state;
10. confirmed header transition deferral while RecyclerViews are scrolling;
11. confirmed category header internal focus/actions/reorder mode;
12. traced top/status-bar focus and action behavior;
13. identified which classes are primarily Leanback internals versus actual Projectivy logic;
14. replaced the ambiguous `i5.q`/`i5.r` interpretations with exact settings mappings: `q = !slidingSingleRow`, `r = !showCardTitle`;
15. verified the advanced field mappings for items-per-line, max rows and start/end/top margins;
16. traced the `dp2` empty-row card and confirmed it opens that category’s Apps configuration rather than being a generic dummy placeholder;
17. documented the row-presenter cache key and the `c92` transitional visibility fallback;
18. documented ID-based startup/refocus behavior (`s0/w0/y0`) rather than treating selection as raw list-position state;
19. added the `hb3` vertical focus/window-alignment algorithm;
20. corrected the `q0()` fade description and documented the separate title-visibility override.

**Group 02 status: COMPLETE / SECOND-PASS REVIEWED.**
