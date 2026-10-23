# Aerial-Rows — AI Project Context / Handoff

**Read this file first.** This is the durable continuation state for the Launcher/Aerial-Rows project exported on 2026-10-23 after the full Projectivy audit and Couchy comparison programme completed.

## Project goal

Repository: `kanozseyer/Aerial-Rows`.

Aerial-Rows is a Couchy Launcher fork being evolved into a high-quality Android TV launcher which keeps Couchy's lightweight Compose implementation and performance characteristics while independently reproducing useful, verified Projectivy behavior.

Target capabilities include Projectivy-style Sliding Rows/left rail/focus behavior, Android TV Preview Channels + Watch Next, Aerial/local wallpaper, streamlined TV settings, notifications + Now Playing, physical TV inputs/HDMI, startup/remotes/idle, robust backup/themes/icons/shortcuts, and optional parental/display/vendor features.

It is **not** intended to become a decompiled Projectivy clone.

## Clean-room / IP boundary

Projectivy was reverse engineered to understand behavior, Android APIs, persistence, lifecycle, focus, JNI boundaries and device integration. Do not paste proprietary decompiled source, copy licensing/premium protection, bypass licensing, or add the proprietary APK/decompiled source/JADX/REA binaries to the repository. Reimplement behavior independently using public Android APIs and clean interfaces.

## Authoritative research baseline

Branch studied and to continue from:

`aerialrows-development`

Commit reverified at C12 completion:

`3c1cfd56234f1d386f4553dd6c69523670500d70`

Tree:

`457f4ab1d08a8d545bdf5545777cd31f907ae24d`

Commit message: `Wire row editor state into Sliding Rows host`.

Main branch at the original Group 12 audit was `d3dcd70d3dd3b261108d25d72569671aee23b2d6` and development was then 36 commits ahead / 0 behind.

Pinned baseline blob SHAs:

- `app/build.gradle.kts` — `1668e8c60f6cf21ff2363f216028fa431f3d6754`
- manifest — `5fb2138676d6e929b3e4fa61a865b32837274421`
- `MainActivity.kt` — `e5b83f7aa597ad245b607f30a65c81d878bdb303`
- `AppRepository.kt` — `6467d2787ce9fc920c025e8cb994c2cf520005cb`
- `Config.kt` — `4710f41198308e555ac9e3b12d515f1fefa1db6e`
- `LauncherScreen.kt` — `a3172678c0bde47a28fa89247b4c5662cc659b1b`
- `SlidingRows.kt` — `a43e8a4fec51549ea7d942490b9c127ee758bd69`
- `AppCard.kt` — `4f045e410d80fb13c7da49d732b3ad247edda95c`
- `RowQuickEditor.kt` — `515c796d7aa2f5b755e9858f2be435a0f72e19a8`
- `SettingsSheet.kt` — `a20140273352bd63c990b2c3e9037c1eb717836c`
- `Menus.kt` — `b847347b94b1be51cd03c180593500d8d6055de3`
- `PanelStyle.kt` — `05927aefe4f8132be356280fd7a5369cd46fbe57`
- `BuiltinAerials.kt` — `bdecdd784dd4b04c2c4c0877acd3faff46760a0f`
- `TvContentRepository.kt` — `714b6fd92637d76f6d12b4702db1475859fccac3`
- `Status.kt` — `fc3f01990123df6b402f7d8927e286e723d0bbda`
- `SetupWizard.kt` — `092f17cc098cbb4e72539020f437c5c796a4db39`
- `aerials.json` — `3ad397817eb236c858a5bbfe59ae40a80d661d99`

Always verify the current branch before editing; this handoff records the end-of-research baseline, not later implementation changes.

## Research status

Projectivy Groups 1–12: COMPLETE.

Projectivy post-closure audits: COMPLETE, including corrected Groups 8/9/10/11/12.

Couchy/Aerial-Rows comparisons C1–C12: COMPLETE + REVIEWED.

Final architecture is sufficiently defined to implement. **Do not start another comparison/reverse-engineering group unless a new blocker genuinely requires it.**

## Immediate next task

**Phase 0A — source-pure build.**

`app/build.gradle.kts` currently contains `applyAerialRowsSourcePatches`, wired to `preBuild`, which rewrites `SettingsSheet.kt`, `LauncherScreen.kt`, and `strings.xml`. It injects/wires Sliding Rows layout selection, Channels settings, row/dock panel controls, multi-source Aerial selection, the `SlidingRowsArea` renderer branch, Dock `AerialPanel`, and related strings.

This is the number-one correctness/reproducibility/build-performance problem.

Next implementation steps:

1. Materialize all injected Kotlin/resource changes as checked-in source.
2. Remove `applyAerialRowsSourcePatches`.
3. Build from a clean checkout.
4. Verify the build leaves the Git working tree unchanged.
5. Run debug + release + lint.
6. Then establish tests/benchmarks before structural refactoring.

## Definitive architecture verdict

**Couchy is the better implementation base. Projectivy is the better behavioral/domain reference.**

Keep Couchy/Aerial-Rows for:

- Kotlin + Compose;
- single small HOME Activity;
- TV-focused D-pad UI;
- bounded app scan concurrency;
- immutable `AppEntry` reuse and same-list reuse;
- pre-rasterized target-size artwork cache;
- `prepareToDraw()` before visible use;
- stable Compose rendering;
- typed `LauncherConfig` + DataStore;
- SurfaceView + Media3 wallpaper;
- deliberately small video buffers;
- decoder/player release while HOME is hidden;
- cheap pseudo-blur;
- low service/component count;
- R8/resource shrinking;
- Aerial-Rows visual identity.

Reimplement Projectivy concepts for:

- generic HomeRow/HomeItem domain model;
- stable row + item focus restoration;
- explicit rail state/focus ownership;
- launch dispatcher/policy;
- richer Android TV provider behavior;
- wallpaper provider/event/scheduler architecture;
- typed setting descriptors/effects/focus identity;
- notification + MediaSession repositories;
- physical TV inputs;
- startup/remotes/idle;
- schema migrations/backups/themes/icon packs/shortcuts;
- optional parental/display/vendor capabilities.

Do not reproduce Projectivy's Leanback internals, Accessibility as a general coordinator, preference-heavy architecture, root/package maintenance, opaque native bootstrap, licensing/protection, Play Store/Xiaomi hacks by default, Accessibility-owned display polling, or Projectivy Web Remote implementation.

## Core dependency direction

`UI -> feature/domain -> core interfaces/state -> Android/platform adapters`.

Vendor/root/Accessibility implementations remain optional integration adapters. Core repositories must not depend on them.

## Identity rules

Never persist a list index, row position or visible label when a semantic identity exists.

Target identities:

- `AppId`: package + component + user/profile.
- `RowId`: stable logical row key.
- `HomeItemId`: typed stable item identity.
- TV channel: provider package + internal provider identity/fallback fingerprint.
- Program: provider + semantic program key.
- `InputId`: platform TIF ID plus compatibility metadata when required.
- `ShortcutId`: package + shortcut ID + user/profile.

## Launch invariant

All launcher-originated opens/actions which launch another target eventually converge on:

`LaunchDispatcher -> LaunchPolicy -> platform-specific launcher`.

This covers apps, TV programs, Now Playing, inputs, shortcuts, notification opens, remote actions and startup targets. Initial policy is allow-all; optional parental controls plug into the same policy later.

## Major known current issues

- Build-time Kotlin/resource rewriting.
- Package-only app identity.
- `movePkg` loses row context when an app occurs in multiple sections.
- Zero configured categories is reachable; All Apps should be generated/system rather than the last structural category.
- Hide is global-only; target should support row-local exclusion plus explicit Hide Everywhere.
- PACKAGE_CHANGED path can leave a stale app when its launcher Activity disappears; prefer `LauncherApps.Callback` plus reconciliation.
- TV provider discovery filters non-browsable channels, uses weak package:numeric-channel identity, lacks explicit ordering/provider policy and has no ContentObserver/resume refresh.
- Provider TV-program launches bypass the future common launch policy.
- Aerial streaming currently disables TLS certificate and hostname verification; remove it.
- Checked-in legacy `builtinSource` and injected `builtinSources` behavior are inconsistent.
- Settings directly own persistence/effect semantics and have several duplicated focus/control implementations.
- Settings do not restore focus to the originating SettingId after returning from a child page.
- Rapid settings and move interactions can enqueue repeated full-config DataStore transactions.
- Save Config is a raw runtime-config dump, uses a problematic public Downloads write path, catches write failure and can still show success.
- ConfigStore has a first-run localization bug: read defaults use localized category names but first update can fall back to hardcoded-English `LauncherConfig()` and persist English.
- Malformed inner JSON silently becomes defaults and a later update can overwrite the only damaged configuration.
- Custom photo wallpaper asset is not part of the current manual backup; local video `content://` URI is not portable.
- AppRepository disk cache includes artwork render size but in-memory reuse only checks package `lastUpdateTime`; add `ArtworkVariant(bannerWidth, iconSize, cacheSchemaVersion)`.
- No notification/media/physical-input/startup/Accessibility/shortcut/icon-pack/parental subsystem exists in the baseline.
- No unit-test, androidTest or Macrobenchmark suite exists in the baseline.

## Performance/reliability invariants

- Build never mutates tracked source.
- D-pad focus does not perform disk/network/package-provider IO.
- Heavy artwork decode remains off main thread.
- Stable semantic items preserve identity/object identity where practical.
- Lazy list items use stable keys.
- One shared wallpaper/idle video renderer.
- Video decoder/player releases when HOME no longer needs it.
- Muted wallpaper eventually disables audio track rather than only setting volume 0.
- Prefer callback/observer-driven state over polling.
- Polling is capability-gated and lifecycle-bounded.
- Disabled optional features do no expensive background work.
- Rapid user input must not create an unbounded persistence queue.
- Cache is never durable user state.
- Corrupt state is never silently overwritten by defaults.
- No trust-all TLS.
- Failures are contained but diagnosable.
- Vendor/privileged code remains optional/isolated.

Measure before redesigning two intentional Couchy tradeoffs: full cold-start gating on the complete app/artwork scan, and keeping all decoded app artwork resident for smooth scrolling. Benchmark on low-memory/app-heavy hardware first.

## Final implementation phases

0A — source-pure build.

0B — unit/instrumented/performance baseline + generated baseline-profile infrastructure.

1 — config safety, schemaVersion/migrations/last-known-good, ArtworkVariant fix, stable IDs, LaunchDispatcher + AllowAll policy.

2 — generic HomeRow/HomeItem/RowPresentation, HomeRowsRepository, exact row+card focus restoration, rail state/controller, pivot scrolling, empty-row action.

3 — component-aware apps, LauncherApps callbacks, move/hide/category/new-install/sort policy, generic HomeItem actions.

4 — complete Android TV Provider repositories, logical IDs, non-browsable channels, ordering, Watch Next provider policy, observers/resume refresh, constrained launch.

5 — WallpaperEngine/provider/renderer separation while preserving Couchy's SurfaceView/Media3 hot path; remove insecure transport first.

6 — SettingId/descriptors/navigation/effects, unified controls, focus restoration, choice/dropdown UI, session state/coalesced persistence.

7 — Clock/Network repositories, NotificationListener bridge/repository/panel, MediaSessionRepository, Now Playing row/card/actions.

8 — physical TV InputRepository/TvInputManager callback, TvInputActivity/TvView, InputLaunchCoordinator, requested/launched/confirmed state, Inputs row/settings; standard platform path first.

9 — HomeRoleCoordinator, StartupCoordinator, RemoteGestureEngine, LauncherIdleCoordinator; optional narrow Accessibility later.

10 — transactional full backup, themes, icon packs/custom artwork, Android shortcuts and unresolved-reference restore behavior.

11 — optional parental controls, display profiles, vendor input adapters, external wallpaper providers, diagnostics/expert tools.

## Recommended first ten implementation commits

1. Materialize injected launcher/settings/resources.
2. Remove source rewriting + add source-tree-cleanliness CI.
3. Add test skeleton + benchmark/profile harness.
4. Fix ConfigDefaults/corruption/category invariants.
5. Add schemaVersion + LastKnownGoodStore + MigrationRegistry.
6. Fix ArtworkVariant cache identity.
7. Add AppId / RowId / HomeItemId.
8. Add LaunchDispatcher + AllowAllLaunchPolicy.
9. Promote Sliding Rows private types to generic HomeRow/HomeItem.
10. Add HomeFocusCoordinator + exact row/card restore.

## Projectivy provenance

Audited target: package `com.spocky.projengmenu`, version 4.72/code 97, minSdk 23, targetSdk 37, 7395 classes, 1171 resources, 3 DEX files, `libptutils.so` on four common ABIs, MainActivity `com.spocky.projengmenu.ui.home.MainActivity`.

APK SHA256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`.

Signing cert SHA256: `f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`.

Toolchain used: JADX headless MCP 0.7.1, REA 6.7.0, Java 21, Node 22.20.0, persistent JADX daemon, complete class/resource inventory.

Final audit process contract SHA: `052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`; process-tool bundle SHA: `251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`.

## Handoff document map

- `docs/ai-handoff/C12_FINAL_BLUEPRINT.md` — final concise architecture/phase plan.
- `docs/ai-handoff/CHAT_EXPORT.md` — chronological reconstructed project conversation/decisions.
- `docs/ai-handoff/COUCHY_DETAILED_FINDINGS.md` — C1–C12 subsystem findings and bugs.
- `docs/ai-handoff/PROJECTIVY_DETAILED_FINDINGS.md` — Projectivy behavioral/audit findings that drive implementation.
- `docs/ai-handoff/RESEARCH_INDEX.md` — report inventory/hashes/provenance.
- `docs/ai-handoff/PROCESS_STATE.md` — reverse-engineering process state if research ever needs reopening.

## User/project working style

The user expects practical exact continuation, small durable chunks/checkpoints, exhaustive understanding, no repeated questions when context already answers them, verification of actual live source/tool behavior, preservation of process fixes after interruptions, explicit KEEP/EXTEND/REPLACE/ADD reasoning, and reproducible/performance-conscious implementation.

This file is the implementation starting specification as of 2026-10-23. Update current phase/provenance when later code changes make it stale.
