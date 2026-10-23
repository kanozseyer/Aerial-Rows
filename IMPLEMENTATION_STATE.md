# Aerial-Rows — Current Implementation State

Updated: 2026-10-23

This file is the authoritative **current implementation checkpoint** for continuation work. It supersedes older "Immediate next task" wording in `AI_PROJECT_CONTEXT.md`, which remains the historical C1–C12 research/provenance record.

## Current branch and phase

Working branch: `aerialrows-development`.

**Phase 5 — wallpaper/Aerial engine is merged** at `63e3590f859c2421fde8e28ed3329e6e192069d3`.

The active implementation phase is **Phase 6 — settings architecture**. Phases 0A/0B and implemented Phase 1–5 work are complete; do not restart them unless a regression specifically requires it.

Merged Phase 6 milestones:

- semantic settings navigation and shared choice controls: `891bcdb257110ddcad74312e54a07f7c8d704a51`;
- debounced settings runtime, Display/Wallpaper semantic focus and CI/cache speed work: `d458d2ed92a8c820c8634bb7aca73984d6a0016b`;
- panel semantic focus, debounced panel controls and live optimistic preview: `177900956ac46c9e48526df1f019b7f55083bdcf`;
- Status/Launcher semantic focus plus deterministic settings-effect policy: `ae8114d7e66bdbc924b595b062b4ce83e37ebe25`.

## Phase 5 completion checkpoint

The merged wallpaper runtime has secure/typed Aerial playback, fair shuffle and quarantine, shared SurfaceView/Media3 video rendering, `WallpaperPlan`/`WallpaperEngineState`, provider boundaries, unified `WallpaperHost`, explicit failure/recovery and the existing low-memory photo/pseudo-blur hot paths. LauncherScreen no longer owns wallpaper-provider internals.

A meaningful Shield checkpoint for wallpaper/fallback/dock behavior is still useful, but it does not block Phase 6 coding.

## Active Phase 6 shared-choice milestone

The current staged milestone is based directly on `ae8114d7e66bdbc924b595b062b4ce83e37ebe25` and its app code has passed the cheap development gate (`:app:testDebugUnitTest :app:compileDebugKotlin`).

Current work:

- extracts the named-choice dialog into one reusable `SettingsChoiceDialog` primitive;
- Display/Wallpaper/Status selectors keep the same current-value-focused choice behavior through the shared primitive;
- panel `ExtraSelector` keeps Left/Right instant stepping but OK now opens the full named choice list when options are available;
- panel dialogs open focused on the optimistic/current value and return to the selector after selection/dismissal;
- shared row/check primitives are internal to the settings UI package rather than duplicated;
- introduces one permanent marker-triggered `Fast Check` workflow so later milestones no longer create/delete temporary YAML just to run unit tests + Kotlin compile;
- the reusable fast check has concurrency cancellation, a 15-minute bound and a source-mutation check; deleting the marker becomes a checkout-only no-op.

After this milestone, continue Phase 6 with:

1. wire true external effects (`RELOAD_APPS`, `RELOAD_TV_CONTENT`, `RELOAD_WALLPAPER`, recreate/restart) to existing launcher/runtime invalidation seams only where config observation is insufficient;
2. add ON_CONFIRM/session-preview adapters only where a setting genuinely needs them;
3. add semantic restore to remaining dynamic settings surfaces where it materially improves TV navigation (Apps/Channels/Categories), using stable app/channel/category identities rather than list positions;
4. run the Phase 6 Shield D-pad/settings checkpoint;
5. then move to Phase 7 notifications/media/status cleanup.

## Development / CI strategy

Use a **milestone workflow**, not one expensive full CI run per tiny slice:

1. batch related work on one milestone branch;
2. stage later work as unopened branches while the active milestone validates;
3. use pure JVM tests, targeted review and the reusable marker-triggered fast check during development;
4. temporary one-shot materializers are allowed only when they materially reduce time/risk and must be removed before review;
5. open one product PR when the milestone is ready;
6. run the Android/R8/lint/source-purity gate once at the milestone boundary;
7. fix concrete failures on the same PR (superseded runs cancel automatically), then merge;
8. use Shield testing at meaningful phase checkpoints rather than every slice.

Current CI speed path:

- Gradle parallel execution and local build cache are enabled;
- pushes to `aerialrows-development` run a trusted compile/unit-test cache warmer;
- post-merge cache warming is verified working;
- PRs targeting `aerialrows-development` can restore that base-branch cache;
- benchmark APKs are compiled only when benchmark/build configuration changes (and for tags);
- reusable `.github/workflows/fast-check.yml` runs only when `.github/fast-check-trigger` is deliberately present/changed;
- release/R8, unit tests, debug build, device-test compilation, lint and source-purity stay mandatory for product PRs.

## Development process rules

- Prefer one active product PR/milestone at a time; later ideas may be staged but should not become a long open PR stack.
- Merge only after the exact current PR head passes its required Android gate.
- Squash merges change commit ancestry. Replay later changes onto the new base rather than carrying superseded parent commits.
- If the base gained unrelated files, replay changed file blobs/patches onto the latest base; never reuse an old whole-tree SHA across unrelated base changes.
- Preserve the Phase 0A source-purity invariant and Couchy performance invariants.
- Optional feature disabled must mean its expensive provider/background work is disabled.
- Prefer callbacks/events over polling and shared provider/renderer paths over parallel implementations.

## Known repository-admin gap

`aerialrows-development` is currently not protected and there is no repository ruleset. The connected GitHub capability can read this state but cannot change admin protection settings. Until protection is enabled in GitHub, "green before merge" is a procedural rule rather than server-enforced.

Recommended protection when repository admin settings are available: require pull requests and the Android `build` status check for `aerialrows-development`.

## Research/IP boundary

C1–C12 research is complete. Projectivy remains a behavioral/architectural reference only. Do not copy proprietary decompiled source, licensing/protection logic or proprietary binaries into this repository.
