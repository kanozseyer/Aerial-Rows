# Projectivy Detailed Findings — Behavioral Reference for Aerial-Rows

Target audited: Projectivy Launcher 4.72 / code 97, package `com.spocky.projengmenu`.

This document preserves the high-value Projectivy findings and post-closure corrections that matter to Aerial-Rows. Projectivy is a behavioral/architectural reference only; do not copy proprietary decompiled code or licensing/protection internals.

## Provenance

- APK SHA256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
- signing cert SHA256: `f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`
- package `com.spocky.projengmenu`
- version 4.72 / code 97
- minSdk 23 / targetSdk 37
- 7395 classes observed
- 1171 resource entries observed
- 3 DEX files
- `libptutils.so` on four common Android ABIs
- MainActivity `com.spocky.projengmenu.ui.home.MainActivity`

Tooling used: JADX headless MCP 0.7.1 all-in-one, REA 6.7.0, Java 21, Node 22.20.0, persistent JADX daemon, full class/resource inventory.

## Group 1 — outer architecture / components

Projectivy has a much larger Android component graph than Couchy. It uses a customized Leanback/BrowseSupportFragment-era home architecture and integrates Accessibility, notification listener, startup/boot-related components, wallpaper/theme integration and multiple Activities.

Aerial-Rows should not copy the component count or Leanback implementation. The useful lesson is to separate feature ownership into dedicated managers/repositories and keep MainActivity thin.

## Group 2 — home architecture

Reconstructed conceptual chain:

```text
MainActivity
 -> MainFragment
 -> customized BrowseSupportFragment
 -> rows adapter
 -> row wrapper
 -> category model + item adapter
 -> row presenter selector
 -> card presenter selector
```

Important behaviors:

- left headers/sidebar have meaningful states beyond Boolean: shown, enabled-but-hidden/revealable, disabled;
- browse layer saves/restores selected row, header state and page/title state;
- selected row is synchronized centrally between headers/content;
- startup/default row resolved by stable category ID rather than list index;
- refocus uses stable category ID + stable card ID after adapters settle;
- category row model supports rich presentation: visibility, multiline/sliding/title/hover behavior, scale, margins, items-per-line/max rows, backgrounds etc.;
- stable IDs are used for rows/cards and wrappers/adapters are reused by category ID;
- configured visible-but-empty category gets a concrete placeholder card which opens population/settings, avoiding disappearing configuration/focus trap;
- presenter selection/cache separates row/card layout characteristics;
- selected/non-selected rows can fade differently;
- header row can expose icon/text/settings/reorder controls;
- row/card movement uses stable adapter moves plus refocus, so focus does not jump;
- selected item can drive a dedicated hover/details surface.

Aerial-Rows should reproduce these semantics in Compose, not the Leanback classes.

## Group 3 — apps/categories/actions

Projectivy reinforced stable category context in app actions. Visibility is not simply one global hidden package set; ordinary Hide behavior can be row/category-local, while broader visibility options can exist separately. Add-to-row/move/reorder needs category identity, not just package.

Projectivy has richer card action routing and launch abstraction. The useful Aerial-Rows lesson is one `HomeItemActionProvider`/LaunchDispatcher boundary rather than hardcoding expanding app-only context menus.

## Group 4 — Android TV Provider content

Projectivy treats Preview Channels/Watch Next as provider content and has richer handling than Couchy's baseline.

Important behavior:

- discovers Preview Channels even when not yet browsable so user can enable/request them;
- can invoke `TvContract.requestChannelBrowsable()`;
- provider content ordering respects meaningful provider fields rather than relying on cursor natural order;
- Preview Programs behavior uses weight-style ordering;
- Watch Next favors engagement/recency semantics;
- provider and content identities are treated more semantically than bare visible labels;
- TV Provider content remains conceptually distinct from physical TV inputs.

Aerial-Rows should use public TvContract APIs and independent repository models.

## Group 5 — notifications and media / Now Playing

Projectivy demonstrates a crucial separation:

```text
NotificationListenerService
  -> notification state/repository
  -> also supplies authorization component for MediaSessionManager

MediaSessionManager / MediaController
  -> media-session state/repository
```

Now Playing comes from active MediaSessions, not by parsing notification text.

The system can represent multiple active sessions, not just one global media item. Session-level wrappers observe metadata and playback-state changes. A primary active/playing session can be selected for secondary behavior.

Media card behavior uses owning application/session Activity PendingIntent when available, with package/app fallback. Transport controls derive from session PlaybackState capabilities.

Post-comparison improvement for Aerial-Rows: route Now Playing launch through the same LaunchPolicy as apps. Projectivy has a path where Now Playing can bypass the general parental wrapper; do better.

Projectivy also has special coupling where dismissing a Now Playing-linked notification may stop/pause playback. Aerial-Rows should not copy this surprising default; notification dismissal and media controls remain separate unless explicitly configured.

Media metadata can feed wallpaper provider events and idle policy. Keep those as consumers of MediaSessionRepository rather than making notification/media service own wallpaper/idle.

## Group 6 — wallpaper / providers

Projectivy's strongest wallpaper lesson is provider/source policy separate from renderer/playback.

Verified conceptual capabilities include:

- provider abstraction and provider cache;
- semantic wallpaper events (e.g. time elapsed, item/card focus, Now Playing changes, idle state, loading error depending on provider);
- explicit rotation scheduling rather than one hard-coded clip timer;
- provider catalogue invalidation separate from ordinary timed rotation;
- image policy for incompatible dimensions/formats, including blur/fit/crop/skip-style behavior;
- loading failures feed engine-level fallback/retry;
- video renderer configuration supports decoder fallback and tunneling;
- when wallpaper volume is zero, audio track can be disabled rather than decoded silently;
- optional dynamic color sampling from renderer/video;
- external wallpaper provider integration;
- backup/theme/idle integration.

Do not copy Projectivy renderer implementation wholesale; Couchy's SurfaceView/Media3 lifecycle is better for Aerial-Rows. Combine Projectivy provider/event architecture with Couchy renderer.

## Group 7 — settings

Projectivy has many preference descriptors and migration/update semantics. Aerial-Rows should not copy the SharedPreferences-heavy storage architecture, but should reuse the descriptor ideas:

- stable setting IDs;
- typed/scoped identity;
- validation;
- visibility and enabled/capability predicates;
- explicit update/runtime effect policy;
- migration aliases/history;
- export policy;
- focus identity;
- dedicated editors (choice/color/etc.) rather than treating every value as one scalar selector;
- some settings legitimately require Activity recreate or HOME-task restart while appearance values should update live.

Bulk restore should not fire every setting side effect individually. Import/migrate/normalize/commit once, then one runtime rehydrate/reload event.

Projectivy can protect Settings behind parental policy; Aerial-Rows should do this through central LaunchPolicy if parental controls are later implemented.

## Group 8 post-closure v2 — physical TV inputs and display profiles

Projectivy has a mature input/source abstraction. Useful concepts:

- stable generic source identity distinct from visible shortcut and current hardware source;
- TV Input Framework discovery/mapping;
- launch coordinator that can use internal TvView or external/vendor strategy;
- persisted last-used source;
- previous/next remote source operations;
- optional display profile manager with vendor picture backends;
- SDR/HDR profile binding by stable profile UUID.

Important corrections/weaknesses found in post-closure audit:

1. There is no general repository-level `TvInputManager.TvInputCallback` closing discovery state; Projectivy relies more on snapshot/cache/recovery, while active TvView uses `TvView.TvInputCallback`. Aerial-Rows should add proper manager callback.

2. Input shortcut visibility can leak into display-profile input picker. A hidden home shortcut should not make physical input unavailable for profile/startup/remote settings. Keep `AvailableInputs` and `VisibleInputShortcuts` separate.

3. Some vendor fallback launch paths update persisted last-used state without converging on exactly the same in-memory current-input state as the internal path. Aerial-Rows should centralize current/requested/confirmed state in InputLaunchCoordinator/repository.

4. Recovery for empty input discovery on some Sony/TCL-like paths is bounded but can be demand-triggered. Aerial-Rows should use explicit callback/resume/bounded retry lifecycle.

5. Compatibility synthesis only occurs when the whole passthrough discovery is empty; Projectivy does not fill individual holes in a partially authoritative HDMI list. This is the correct conservative principle.

6. Display-profile observation/polling is Accessibility-owned in Projectivy. Aerial-Rows must keep Accessibility independent.

7. Projectivy's roughly 4-second display profile poll can stop rescheduling after one transient invalid/null sample. Aerial-Rows observer must survive transient null/error.

8. Dangling profile UUID bindings are tolerated/resolved null rather than repaired. Aerial-Rows should validate/repair or visibly flag invalid binding.

Vendor picture backend families verified include Amlogic `SystemControlManager`, Amlogic `TvControlManager`, Xiaomi picture/source managers. These are optional future adapter research references, not core dependencies.

## Group 9 post-closure v3 — startup, HOME, remotes, Accessibility, idle

Projectivy architecture conceptually combines:

- Android HOME role/task;
- optional Accessibility HOME override/fallback;
- MainActivity startup trigger;
- Accessibility startup trigger;
- shared autostart dispatcher;
- wake/standby trigger;
- remote gesture engine;
- idle/power coordination.

Important verified behavior/observed constants:

- startup duplicate debounce around 15 seconds;
- boot-time validity window around 90 seconds;
- `input_last_used` persisted;
- HOME task restart can use `Intent.makeRestartActivityTask(...)+CATEGORY_HOME`;
- remote long press roughly 300ms and double press roughly 400ms in observed implementation;
- temporary hotkey mode/lease roughly 3 seconds;
- HOME double can map to Recents;
- MENU long/double mappings exist;
- direct HDMI vendor key codes observed around 243–246;
- screensaver strategy can resolve configured system components and external fallback;
- launcher idle restore includes short delay around 300ms.

These timings are behavioral references, not required constants for Aerial-Rows.

Critical post-closure separation: Projectivy Accessibility also participates in device-specific compatibility behavior, including Xiaomi startup preconditioning, optional periodic Accessibility root action for simulated user activity, Play Store foreground `sys.cts.running` workaround and Xiaomi updater workaround. These must **not** become generic Accessibility responsibilities in Aerial-Rows. If any are ever required, isolate under tested `VendorCompatibilityAdapters`.

Normal Aerial-Rows HOME must work without Accessibility. Startup policy is one coordinator receiving several triggers; do not let every lifecycle callback independently launch targets.

## Group 10 post-closure v2 — persistence, backup, theme, icons, shortcuts, parental

Projectivy demonstrates the value of mature schema/migration handling, full backup separate from theme, icon-pack parsing/cache, hosted shortcuts and optional parental policy.

Important reusable principles:

- versioned migrations rather than relying only on defaults/unknown-key tolerance;
- safety/backup before destructive migration;
- backup and theme are different scopes;
- cache data is regenerable and not portable user state;
- icon-pack parse cache identity includes pack package/version;
- shortcut identity is stable provider shortcut identity, not label/list index;
- shortcut placement can survive provider temporarily missing and resolve later;
- parental restrictions should be enforced at launch/policy boundary;
- settings itself can be protected.

Do not copy Projectivy backup crypto design that relies on an app-embedded fixed secret/weak portability model. Aerial-Rows should use transparent non-sensitive archive or user-derived authenticated encryption when optional encryption is desired.

Do not copy magic naming such as non-export prefixes or numeric category thresholds; use explicit ExportPolicy and CategoryOrigin metadata.

## Group 11 post-closure v2 — obfuscated/native closure and privileged tools

Final conclusion: no hidden Projectivy architectural engine exists in obfuscated/native code that Aerial-Rows needs to reproduce.

Refinements observed:

- special autostart target `-2` reuses app resolution; `-101` effectively no-action/failure path;
- a display-profile reset path resets some Java state but had no clear managed callers worth reconstructing;
- ARM64 `libptutils.so` exports narrow PTApplication routes; native path is not a general home engine;
- root package disable/uninstall and enable/install-existing commands exist;
- manual Play self-update blocker can create immutable `/data/app/com.android.vending-1/base.apk` using `chattr +i`, with inverse unblock;
- this is distinct from automatic Play Store `sys.cts.running` compatibility behavior;
- GApps removal can invoke `/system/bin/gapps-uninstall.sh`;
- optional InstallAppsActivity/installer exists;
- Web Remote is an external URL rather than a hidden local web server.

These are optional expert/vendor/privileged tools and explicitly outside Aerial-Rows core target.

## Group 12 post-closure v2 — reconstruction plan

Projectivy-only reconstruction plan established the first phase order:

0 source-pure reproducible build
1 config+migrations+rollback+identities
2 home rows+stable focus+rail
3 apps+actions+LaunchDispatcher/LaunchPolicy
4 complete TV Provider
5 wallpaper/Aerial provider + shared renderer
6 settings
7 notifications+Now Playing
8 TV inputs
9 HOME/startup/remotes/Accessibility/idle
10 backup/themes/icons/shortcuts
11 optional expert/device tools

Couchy C1–C11 later refined this into the final C12 plan by moving performance/test baseline immediately after Phase 0 and choosing Couchy's implementation for hot paths.

Important Group 12 architectural decisions retained:

- migration rollback safety belongs early;
- `LaunchDispatcher + LaunchPolicy` should exist by app/action phase and begin allow-all;
- core repositories must not depend on vendor/privileged code;
- Accessibility is optional; normal launcher does not require it;
- one shared video playback engine;
- priority zero is removal of Gradle source rewrite.

## Projectivy behaviors to borrow vs not borrow

Borrow/reimplement independently:

- stable row/item state and focus;
- rail semantics;
- richer card/action model;
- TV Provider semantics;
- wallpaper provider/event architecture;
- setting update/focus/export semantics;
- notification/media repositories;
- input launch abstraction;
- startup dispatcher and remote gesture state machine;
- backup/theme separation;
- icon packs/shortcuts;
- optional parental/display/vendor capabilities.

Do not make reconstruction goals:

- Leanback fragment implementation;
- obfuscated class topology;
- native PTApplication bootstrap;
- licensing/premium implementation;
- root package maintenance;
- GApps removal;
- Play immutable APK blocker;
- vendor Play Store/Xiaomi updater hacks by default;
- Accessibility-owned display polling;
- opaque privileged shell architecture;
- Projectivy Web Remote clone.

The guiding rule is to reproduce useful behavior behind clean Compose-era/public-API interfaces while correcting known Projectivy coupling/reliability weaknesses.
