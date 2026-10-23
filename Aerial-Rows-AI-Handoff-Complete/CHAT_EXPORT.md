# Launcher Project — Reconstructed Chat Export / Continuation Record

**Purpose:** This is a durable reconstruction of the Launcher/Aerial-Rows project conversation up to the completion of C12 on 2026-10-23. It is not a verbatim transcript. It preserves the decisions, discoveries, corrections, user intent, workflow expectations, file/tool provenance, and implementation direction needed to continue with equivalent context.

For detailed subsystem evidence, use `COUCHY_C1_C12_FULL.md` and `PROJECTIVY_G1_G12_FULL.md` stored next to this file.

---

# 1. Original project intent

The user wanted to create a better Android TV launcher by starting from an existing open-source launcher and adding the best Projectivy-style behavior plus Aerial video wallpapers.

The project evolved through several ideas:

1. Customize an existing Shield/Android TV launcher.
2. Consider cloning/forking Projectivy directly.
3. Recognize that Projectivy is proprietary and use reverse engineering only to understand behavior/architecture.
4. Consider Couchy Launcher as the clean open-source base.
5. Fork Couchy into `kanozseyer/Aerial-Rows`.
6. Add Projectivy-like features on top while retaining Couchy's modern Compose/performance qualities.
7. Reverse engineer Projectivy comprehensively enough to know what behavior to reimplement independently.
8. Compare Couchy subsystem-by-subsystem against the Projectivy audit.
9. Freeze a final architecture/implementation plan.

The user explicitly asked for a **streamlined settings UI**, clear switches, dropdown-like selectors, persistent focus/cursor position after changing a setting, and a left navigation rail that behaves like Projectivy: the rail should only appear/reveal when navigating left from the home rows.

Desired features accumulated over the project included:

- long-press app options including uninstall;
- Android TV Channels / Watch Next;
- Projectivy-style left headers/rail;
- per-row customization and reordering;
- richer settings with dropdown/choice behavior;
- Aerial video backgrounds;
- notification/Now Playing behavior;
- TV inputs/HDMI;
- startup/autostart/remotes;
- backup/theme/icon/shortcut systems;
- optional advanced device features.

The user values **performance and polish**, not just feature parity.

---

# 2. Repository selection and early Aerial-Rows state

The user forked Couchy and created/named the repo **Aerial-Rows**.

During early setup:

- JVM 21 was required in the user's Android Studio/environment.
- The project was built and run as the working base.
- A development branch accumulated Aerial-Rows changes.
- The final research baseline was `aerialrows-development` commit `3c1cfd56234f1d386f4553dd6c69523670500d70`.

The Aerial-Rows branch already included:

- a fourth `LAYOUT_SLIDING_ROWS` mode;
- `SlidingRows.kt` with Projectivy-inspired horizontally sliding rows and a left rail;
- Android TV Preview Channels and Watch Next support via `TvContentRepository`;
- `RowQuickEditor` for row-level app/channel customization;
- Aerial sources and video wallpaper support;
- configurable row/dock panel appearance;
- multi-source Aerial selection concept;
- various Couchy UI/performance features.

However, several Aerial-Rows changes were not normal checked-in source behavior. They were injected at build time by `applyAerialRowsSourcePatches` in `app/build.gradle.kts`. This became the highest-priority cleanup item.

---

# 3. Projectivy reverse-engineering decision

The user had a fork of an REA repository intended to help reverse engineer Android applications:

`https://github.com/kanozseyer/rea-fork`

The user asked whether REA could be used to reverse engineer the Projectivy APK and wanted the full JADX pipeline working.

The Projectivy target APK supplied during the project was version 4.72 / code 97.

A JADX MCP all-in-one JAR was also supplied.

The project eventually established a working REA/JADX environment and performed a comprehensive grouped audit rather than relying on a single broad decompile pass.

Important safety/IP boundary agreed in practice:

- study proprietary APK behavior and architecture;
- do not paste wholesale proprietary decompiled source into Aerial-Rows;
- do not bypass premium/licensing;
- independently reimplement desired behavior.

---

# 4. Reverse-engineering toolchain and target provenance

Projectivy audit target:

```text
package: com.spocky.projengmenu
versionName: 4.72
versionCode: 97
minSdk: 23
targetSdk: 37
classes: 7395
resources: 1171
DEX files: 3
JNI library: libptutils.so (all four common Android ABIs)
MainActivity: com.spocky.projengmenu.ui.home.MainActivity
```

APK SHA256:

`a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

Signing certificate SHA256:

`f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`

Signer subject included `CN=Despesse Mickael`.

Toolchain used:

- JADX headless MCP 0.7.1 all-in-one JAR;
- REA 6.7.0;
- Java 21;
- Node 22.20.0;
- persistent JADX MCP daemon.

Historical analysis paths:

```text
/mnt/data/rea-portable
/mnt/data/rea
/mnt/data/launcherproject_work
```

These are preserved only as provenance. A future environment does not need identical paths unless re-running research.

---

# 5. Audit process evolution

The Projectivy audit was intentionally split into small groups to reduce mistakes, timeouts, and context loss.

The user repeatedly emphasized that analysis should be exhaustive and process fixes should survive interruptions.

The audit process was hardened through several iterations. Final principles included:

- semantic-first search instead of assuming class names;
- use REA/JADX canonical evidence for high-risk claims;
- persistent daemon for speed;
- independently complete class inventory and resource inventory;
- do not treat `resources=lite` as resource closure;
- bounded batches (default 6, max 12);
- write-ahead `in_progress` checkpoint before slow JVM probes;
- track interrupted/recovered attempts;
- atomic report/state writes;
- quarantine failures;
- exact cache behavior;
- FTS dotted/literal fallback;
- named-class prevalidation;
- no empty-query REA as exhaustive search;
- no aggressive parallel JVMs;
- resume only unfinished work after timeout;
- closure matrix across 11 dimensions;
- stop/report after each group;
- completion receipt mechanism to avoid repeating already-complete analysis.

Closure dimensions:

1. entry points;
2. base classes/inheritance;
3. all DEX files;
4. obfuscated collaborators;
5. models/data flow;
6. persistence;
7. resources/XML;
8. Android framework APIs;
9. callbacks/observers;
10. launch/actions/lifecycle;
11. native/JNI.

Final process contract references:

- v18 contract SHA `052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`
- process bundle SHA `251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

One important lesson: documentation once referred to a `--core` self-test option, but the actual installed CLI did not expose it. The project established a rule to inspect the actual CLI before trusting old process docs.

---

# 6. Projectivy Group 1–12 research programme

Full original and post-closure reports are concatenated in `PROJECTIVY_G1_G12_FULL.md`.

The original groups and post-closure audits established enough behavior to reconstruct a clean implementation plan.

The later post-closure corrections are especially important for Groups 8–12.

## Projectivy home architecture

The reconstructed chain was approximately:

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

Important verified behavioral ideas:

- left headers/rail have three meaningful states: shown, hidden-but-revealable, disabled;
- selected row/header/title/page state is saved/restored;
- startup default row resolves by stable category ID rather than index;
- focus restoration uses stable category ID + card ID;
- row models carry rich presentation configuration;
- configured empty rows get a concrete placeholder card rather than disappearing;
- presenter cache by layout characteristics;
- deliberate vertical alignment beneath status/title region;
- selected/nonselected row fade behavior;
- rail supports settings/reorder controls;
- row/card reorder preserves focus via stable IDs;
- selected card can drive a hover/details surface.

## Projectivy apps/actions

Projectivy behavior reinforced the need for:

- stable app/category context in actions;
- per-row and global visibility semantics;
- richer context action architecture;
- launch abstraction rather than direct UI `startActivity` calls;
- stable IDs throughout movement/reorder.

## Projectivy Android TV provider content

Key ideas:

- discover Preview Channels including non-browsable ones;
- allow requesting channel browsability;
- use provider ordering semantics;
- more deliberate Watch Next ordering/provider behavior;
- separate provider content domain from physical TV input domain.

## Projectivy wallpaper engine

Projectivy was stronger around the player than Couchy:

- provider abstraction;
- semantic wallpaper events;
- explicit rotation scheduler;
- provider cache invalidation;
- image format policy;
- retry/fallback behavior;
- renderer configuration including tunneling/decoder fallback;
- disable audio track for muted video;
- optional dynamic color sampling;
- external providers;
- idle/theme integration.

But Couchy's actual player implementation was better for Aerial-Rows and should be retained.

## Projectivy settings

Projectivy's strongest reusable idea was not its SharedPreferences store; it was metadata describing settings:

- stable IDs;
- validation;
- visibility/capability;
- update behavior;
- migration aliases;
- export policy;
- focus identity.

Aerial-Rows should preserve typed config/DataStore and add a descriptor layer instead.

## Projectivy media/notifications

Important split:

```text
NotificationListenerService
  -> NotificationRepository
  -> permission bridge for MediaSessionManager

MediaSessionManager
  -> MediaSessionRepository
```

Now Playing comes from active MediaSessions, not parsed notification text.

Multiple sessions are possible. A primary session can be selected for wallpaper/idle state.

## Projectivy TV inputs

Projectivy had mature source mapping/launch behavior, but later audit exposed weaknesses:

- no repository-level `TvInputManager.TvInputCallback`;
- discovery more snapshot/cache-driven than ideal;
- visible shortcut filtering leaked into display-profile choices;
- current input state could diverge across launch strategies;
- compatibility recovery could be demand-triggered;
- display profile polling was coupled to Accessibility and could stop after a transient null sample;
- dangling profile UUID bindings were tolerated.

Aerial-Rows should preserve the behavior but improve those weaknesses.

## Projectivy startup/remote/idle

Verified concepts included:

- shared startup dispatcher from multiple triggers;
- duplicate suppression (~15s observed);
- boot validity window (~90s observed);
- persisted last-used input;
- HOME task restart using Android HOME/task semantics;
- global key state machine with long/double press (~300/400ms observed);
- temporary hotkey mode (~3s observed);
- direct HDMI vendor key codes observed (243–246);
- screensaver strategies;
- short idle restore delay (~300ms observed).

Exact timings are references, not requirements.

Later audit also found Xiaomi/Play Store compatibility workarounds and an Accessibility user-activity hack. These are not generic core architecture.

## Projectivy persistence/backups/themes/icons/shortcuts/parental

Projectivy confirmed the value of:

- schema migrations;
- pre-migration safety state;
- backup separate from theme;
- explicit icon-pack parsing/cache;
- launcher shortcut hosting;
- optional parental policy.

But Aerial-Rows should not copy Projectivy's embedded-secret/weak backup crypto or magic export-name conventions.

## Projectivy native/obfuscated closure

Group 11 concluded there was no hidden architectural engine behind native/obfuscated code.

`libptutils.so` was a narrow optional route, not something Aerial-Rows needs to reproduce.

Other verified optional privileged behaviors included root package disable/enable commands, Google Play immutable APK blocker, GApps uninstall script, optional app installer, and external Web Remote URL. None are core reconstruction targets.

## Projectivy Group 12 plan

The original Projectivy-only reconstruction phase plan was:

```text
0 source-pure reproducible build
1 config+migrations+rollback+identities
2 home rows+stable focus+rail
3 apps+actions+LaunchDispatcher/LaunchPolicy
4 complete TV Provider
5 wallpaper/Aerial provider + shared renderer
6 settings
7 notifications+Now Playing
8 TV inputs
9 HOME/startup/remotes/accessibility/idle
10 backup/themes/icons/shortcuts
11 optional expert/device tools
```

Couchy C1–C11 later refined this plan, primarily by inserting performance/testing immediately after Phase 0 and adjusting implementation choices to preserve Couchy's hot paths.

---

# 7. Couchy comparison programme C1–C12

The user explicitly asked to study Couchy source and compare it to Projectivy in small groups to avoid mistakes.

The planned groups became:

1. C1 Build/manifest/lifecycle/architectural skeleton.
2. C2 Home screen architecture.
3. C3 Installed apps/categories/card actions.
4. C4 Android TV Channels & Watch Next.
5. C5 Wallpaper & Aerial architecture.
6. C6 Settings architecture.
7. C7 Status/media/notifications.
8. C8 TV inputs/device integration.
9. C9 HOME/startup/remotes/Accessibility/idle.
10. C10 Persistence/backups/icons/shortcuts/themes/parental.
11. C11 Performance/reliability.
12. C12 Final synthesis.

All are complete and reviewed.

---

# 8. C1 conclusions

Couchy's manifest/component graph is dramatically smaller/cleaner than Projectivy.

Current baseline has essentially one MainActivity as HOME/DEFAULT/LEANBACK_LAUNCHER/LAUNCHER, with no Accessibility service, NotificationListener, boot receiver, wallpaper-provider receiver, or theme provider.

MainActivity handles:

- READ_TV_LISTINGS prompt;
- package receiver;
- locale bootstrap;
- no-back HOME root behavior;
- Compose entry.

Decision: keep MainActivity small. Future startup/input/accessibility should be separate coordinators/services.

`LauncherConfig` JSON in Preferences DataStore is a better persistence foundation than Projectivy's many individual preferences. Add schema/migrations/rollback rather than replacing it.

`AppRepository` was immediately identified as a major strength and should be preserved.

`Actions` is a useful platform boundary but future calls should converge on `LaunchDispatcher + LaunchPolicy`.

Guiding principle established in C1:

**Couchy lightweight foundation + Projectivy behavioral/subsystem reference.**

---

# 9. C2 conclusions — home

Effective current architecture:

```text
MainActivity -> LauncherApp -> config/app scan -> categorized rows -> shell/status/wallpaper -> layout
```

Raw checked-in `LauncherScreen` handles Carousel/Grid/Dock. Sliding Rows is connected by build-time source patch.

`LauncherApp` owns too much and should later be split into home/runtime repositories/controllers.

Current Sliding Rows row model is private and only covers Watch Next, channel, and category.

Target generic row/item model was established.

Stable identity findings:

- outer rows are keyed by row ID;
- app keys include category + package;
- TV program keys use provider item ID;
- `homeRowOrder` stores IDs rather than indices;
- but card focus identity is not persisted per row.

Need `HomeFocusState(activeRowId, activeItemId, lastItemByRow)`.

Rail behavior today is effectively Left #1 show, Left #2 focus. Target needs explicit reveal/focus ownership.

Rail reorder has positional identity risk because `rows.forEachIndexed` is used in a normal Column and `RailItem` remembers focus state. Key by row ID and explicitly refocus after reorder.

Couchy's older Carousel/Grid code already contains strong custom `BringIntoViewSpec` pivot geometry. Reuse this in Sliding Rows rather than inventing a Leanback-style implementation.

Empty rows: hide ephemeral empty rows but preserve user-configurable empty row via placeholder action card.

Per-row presentation should be introduced as overrides rather than dumping dozens of fields directly into LauncherConfig.

Couchy's AppCard should be retained. Add selected-details later as a shared surface driven by stable active item ID.

---

# 10. C3 conclusions — apps/categories/actions

Couchy's app scan/render pipeline is better than Projectivy's for the implementation base.

Concrete bugs found:

### Package removal through PACKAGE_CHANGED can leave stale cards

Current logic can skip a rescan/removal path if a package changed such that it no longer has a launcher activity. Use LauncherApps callback-driven invalidation/reconciliation.

### Move state bug

Current move state is only `movePkg`. Because an app can belong to several sections, `findMoving()` can pick the first occurrence and multiple cards can believe they are moving/use the same FocusRequester.

Replace with row-scoped `MovingItem(rowId,itemId)`.

### Zero-category bug

It is possible to leave only generated All Apps then disable it, resulting in zero configured categories. Later code can call `categories.first()` and fail.

Enforce structural invariant in normalization and treat All Apps as generated/system, not a structural user category.

### Package-only identity

App discovery initially has an exact ResolveInfo/activity, but `AppEntry` discards component identity. Later launching resolves package launch intent again, so label/art/activity identity can diverge for multi-activity packages.

Use component-aware AppId.

### Hide semantics

Current hidden set is global. Projectivy behavior demonstrates the value of row-local hide/exclusion plus explicit Hide Everywhere.

### Context actions

Keep current UI/release-suppression behavior. Generalize to a HomeItem action provider for future actions.

### Sort/new install

Keep install-time sorting as one mode and add alternatives. New-install behavior should be explicit policy.

---

# 11. C4 conclusions — TV Provider

Couchy's implementation is a good minimal reader but not a complete TV-provider subsystem.

Concrete gaps:

- non-browsable channels filtered before settings;
- channels with zero programs disappear from discovery/settings;
- no explicit Preview Program sort;
- no explicit Watch Next sort;
- no Watch Next per-provider cap/exclusion;
- durable channel identity uses package:numeric ID only;
- no observer/resume refresh;
- provider deep links are started directly and do not go through common policy or ownership constraints.

Target architecture separates channel discovery, Preview Programs, Watch Next, artwork, and observer lifecycle.

---

# 12. C5 conclusions — wallpaper/Aerial

Couchy's renderer is one of the strongest parts of the whole codebase.

Keep:

- SurfaceView;
- Media3;
- decoder release on stop;
- small buffer;
- first-frame gating;
- transitions;
- gradient cover;
- local media handling philosophy;
- downsampled images;
- pseudo-blur.

Serious issue found: trust-all TLS + hostname verifier for Aerial streaming. Remove.

Build-patch issue especially matters here because `builtinSources` is only partially materialized in checked-in source.

Another subtle bug: patched source selection uses new `builtinSources`, but old `builtinSource` remains in `produceState` keys, so changed collection selection may not apply until current clip ends/error/timer. If finish-current-first is desired, make it explicit policy.

Projectivy-inspired engine/provider separation was adopted as the target, while retaining Couchy's player.

Audio track should be disabled when muted.

Shuffle bag, failure quarantine, rotation scheduler, typed Aerial metadata, persistent URI error handling were all recommended.

---

# 13. C6 conclusions — settings

Couchy's UI is good but model is too implicit.

Every setting directly writes config and therefore UI owns persistence/effect semantics.

Add descriptors/controller/effect policy while retaining typed config and Compose.

Concrete issues:

- three separate focus/control families;
- focus restoration does not remember originating setting after child route;
- direct DataStore full-config write on rapid adjustments;
- backup save API/reporting bug;
- hardcoded English strings in extras;
- validation scattered.

Target `SettingsNavigator` stores last focused SettingId per route.

Small ranges keep Left/Right; larger named choices get an OK-opened choice panel/dropdown.

---

# 14. C7 conclusions — status/media/notifications

Couchy's baseline has no notifications/media implementation.

Keep current status bar and hidden focus strip.

Add separate repositories for notification and media.

Important rule: Now Playing derives from MediaSessions, not notification text.

Use platform MediaSession APIs rather than adding a Media3 Session dependency solely for observation.

Media launches must go through central LaunchPolicy, improving on Projectivy's policy gap.

Network state should distinguish transport vs validated Internet. Clock/date should share a repository/ticker.

---

# 15. C8 conclusions — physical TV inputs

Couchy has no physical TV input subsystem.

This became a clean-sheet design.

Use public `TvInputManager` + repository callback + resume refresh.

Keep three concepts distinct: discovered, available, visible shortcuts.

Use dedicated fullscreen TvInputActivity/TvView for standard TIF source display.

All input launching goes through InputLaunchCoordinator and LaunchPolicy.

Current input state should distinguish requested, launched, confirmed.

Vendor adapters and display profiles are optional later.

Do not tie display-profile polling to Accessibility.

---

# 16. C9 conclusions — HOME/startup/remote/idle

Couchy's small lifecycle is a strength.

Keep normal HOME role, singleTask MainActivity, no root Back exit, RoleManager setup, UI-local key handling.

Add HomeRoleCoordinator and StartupCoordinator separately.

Do not add boot receiver merely to launch HOME.

RemoteGestureEngine is for programmable global gestures; ordinary D-pad/AppCard long-press/move mode stay UI-local.

Accessibility remains optional and narrow.

Idle should use shared wallpaper renderer and hide launcher chrome rather than create another player.

Manual Google TV ADB setup remains expert guidance; add rollback guidance rather than silently disabling stock launchers.

---

# 17. C10 conclusions — persistence/backups/themes/icons/shortcuts/parental

Couchy has a strong simple storage primitive, but very little surrounding persistence infrastructure.

Critical ConfigStore bugs were identified:

- localized defaults vs English first update;
- malformed JSON silently becomes defaults and can later overwrite original state.

Current Save Config is not a full backup.

Custom photo is external file not in backup. Local video content URI is device/install-local.

Need separate runtime config and public backup DTO.

Need explicit full backup vs theme separation.

Existing artwork cache should remain cache-only.

Icon packs and launcher shortcuts are absent and can be designed cleanly from scratch.

Parental controls are optional and must sit in LaunchPolicy, not Accessibility.

---

# 18. C11 conclusions — performance/reliability

Couchy contains unusually thoughtful TV-specific performance decisions. Do not erase them during refactor.

High-value invariants:

- bounded app scan;
- immutable object reuse;
- same list instance reuse;
- pre-rasterized artwork;
- `prepareToDraw()`;
- stable keys;
- SurfaceView;
- small Media3 buffer;
- release player/decoder on stop;
- pseudo-blur;
- release shrink/minify;
- low service count.

Concrete build issue: source patch task writes huge Kotlin files every preBuild and hurts incremental builds/reproducibility.

Concrete AppRepository bug: in-memory reuse key ignores rendered artwork size/density variant even though disk filename contains it.

Cold process startup waits for the entire app scan + artwork load before rendering real HOME. Measure first before redesigning.

All decoded app artwork is retained process-wide. This may be a reasonable smoothness tradeoff. Measure PSS/RAM before replacing with bounded LRU.

No tests/benchmarks currently exist. Add benchmark infrastructure before feature transplant.

Projectivy feature breadth should be demand-driven so disabled features do no expensive background work.

---

# 19. C12 final synthesis

C12 froze the final conclusion:

**Couchy is the better base implementation. Projectivy is the better behavioral reference.**

Do not restart research. Begin implementation with source purity and performance baseline.

Final architecture and exact phase order are in `couchy/C12.md` and `AI_PROJECT_CONTEXT.md`.

---

# 20. Current branch/file state that a future agent should inspect first

At the final research baseline:

- development branch commit `3c1cfd56234f1d386f4553dd6c69523670500d70`;
- `app/build.gradle.kts` still contains `applyAerialRowsSourcePatches`;
- checked-in source therefore does **not** fully equal compiled source;
- `SlidingRows.kt` exists as normal source but is wired/integrated partly through the patch;
- settings Channels/SlidingRows/panel/Aerial multi-source wiring is partly patch-injected;
- no notification/media/input/startup/accessibility/shortcut/icon-pack/parental subsystems exist yet in the baseline.

The first implementation task is to remove that ambiguity without behavior change.

---

# 21. User expectations for future work

The user prefers:

- exact practical steps;
- small durable changes;
- visible checkpoints;
- exhaustive understanding;
- no repeated questions when context already answers them;
- source verification before edits;
- preservation of every discovered process fix;
- testing actual behavior rather than relying on assumptions;
- explicit KEEP/EXTEND/REPLACE decisions;
- strong performance and focus behavior.

When implementing, work in bounded commits and report what changed, what was verified, and what remains.

---

# 22. Files preserved with this export

## Couchy comparison

`COUCHY_C1_C12_FULL.md` (complete C1–C12 archive)

These are the final subsystem comparison reports. C12 is the authoritative blueprint; C1–C11 contain details C12 intentionally compresses.

## Projectivy research

`PROJECTIVY_G1_G12_FULL.md` (complete Groups 1–12 + post-closure archive).

Where a post-closure audit exists, use it to refine/correct the original report. In particular, Groups 8–12 have important corrected versions.

## Process

`PROCESS_FULL.md` containing current process state plus historical hardening reports.

## Manifest

`MANIFEST.md` records hashes of the handoff files at export time.

---

# 23. Suggested prompt for a new AI conversation

A user can start a fresh conversation with something like:

> We are continuing the Aerial-Rows Android TV launcher project. Read `AGENTS.md`, `AI_PROJECT_CONTEXT.md`, and `docs/ai-handoff/C12_FINAL_BLUEPRINT.md` from the repository before doing anything. The Projectivy/Couchy research is complete. Continue from Phase 0A exactly as documented, verify the current `aerialrows-development` branch first, and preserve the performance/reliability invariants in C11.

That should restore most of the project context immediately.

---

# 24. Final handoff state

Research status at export:

```text
Projectivy Groups 1–12: COMPLETE
Projectivy post-closure audits: COMPLETE
Couchy comparisons C1–C12: COMPLETE + REVIEWED
Final architecture: FROZEN ENOUGH TO IMPLEMENT
Next phase: PHASE 0A SOURCE-PURE BUILD
```

No additional discovery group is required before implementation.
