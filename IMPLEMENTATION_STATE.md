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
- Status/Launcher semantic focus plus deterministic settings-effect policy: `ae8114d7e66bdbc924b595b062b4ce83e37ebe25`;
- shared TV choice UI and reusable fast check: `07acd83bfbea8cd2f43580850488e6bf5b1b83a7`.

## Phase 5 completion checkpoint

The merged wallpaper runtime has secure/typed Aerial playback, fair shuffle and quarantine, shared SurfaceView/Media3 video rendering, `WallpaperPlan`/`WallpaperEngineState`, provider boundaries, unified `WallpaperHost`, explicit failure/recovery and the existing low-memory photo/pseudo-blur hot paths. LauncherScreen no longer owns wallpaper-provider internals.

A meaningful Shield checkpoint for wallpaper/fallback/dock behavior is still useful and can be combined with the Phase 6 settings checkpoint.

## Active Phase 6 final milestone

The staged final Phase 6 milestone is based on `07acd83bfbea8cd2f43580850488e6bf5b1b83a7`.

It adds:

- stable semantic focus IDs for dynamic Apps, Channels and Categories settings rows;
- exact route-level focus restoration for those three dynamic settings surfaces;
- app focus identity that includes package + exact launcher component;
- provider/channel focus identity based on stable package/channel keys rather than labels or list positions;
- category focus identity based on stable category IDs;
- lightweight configuration transfer through the schema envelope instead of a raw `LauncherConfig` dump;
- import through `ConfigCodec` migrations plus `ConfigNormalizer`, while preserving the live installed-app baseline;
- Storage Access Framework `CreateDocument` export instead of direct public Downloads writes;
- truthful save failure reporting and explicit newer-schema rejection;
- legacy raw-config import compatibility through the existing schema migration path.

Settings-effect audit result:

- current wallpaper controls already drive `WallpaperHost` through observed `LauncherConfig`, so explicit `RELOAD_WALLPAPER` callbacks would duplicate work; their descriptors now use `RECOMPOSE`;
- current TV content state already observes relevant config through `TvContentRepository`;
- current language handling already recreates the Activity through the existing locale observer;
- no current setting requires `RELOAD_APPS`, `RELOAD_TV_CONTENT`, `RELOAD_WALLPAPER`, `RECREATE_ACTIVITY` or `RESTART_HOME_TASK` beyond those existing observation seams;
- no current setting has a genuine `ON_CONFIRM` or `SESSION_ONLY` requirement, so no unused runtime adapter is being added merely because the policy enum exists.

The milestone has passed the targeted development gate (`:app:testDebugUnitTest :app:compileDebugKotlin`). The permanent marker-triggered Fast Check is the final cheap gate before the product PR; the normal Android/R8/lint/source-purity workflow remains the merge gate.

After this milestone merges:

1. run the Phase 6 Shield D-pad/settings checkpoint, including exact dynamic-row focus restoration and config Save/Load through the TV document picker;
2. fix any real Shield regression on a bounded Phase 6 follow-up only if needed;
3. otherwise mark Phase 6 complete and begin **Phase 7 — status, notifications and media**.

## Phase 7 next implementation target

Phase 7 should extend existing platform-aware code rather than replace working paths:

- promote the existing callback-based network flow behind a `NetworkStatusRepository` boundary;
- extract clock/date lifecycle state into a `ClockRepository`;
- add a thin `NotificationListenerService` bridge and immutable `NotificationRepository`/policy/panel;
- add a callback-driven `MediaSessionRepository`, keep multiple sessions distinct and select a primary session separately;
- add a generic Now Playing home row/card/actions;
- route notification/media opens through the existing `LaunchDispatcher -> LaunchPolicy` boundary;
- demand-gate notification/media observation so disabled/unconsumed features do not create unnecessary background work.

## Development / CI strategy

Use a **milestone workflow**, not one expensive full CI run per tiny slice:

1. batch related work on one milestone branch;
2. use pure JVM tests, targeted review and the reusable marker-triggered fast check during development;
3. temporary one-shot materializers are allowed only when they materially reduce time/risk and must be removed before review;
4. open one product PR when the milestone is ready;
5. run the Android/R8/lint/source-purity gate once at the milestone boundary;
6. fix concrete failures on the same PR (superseded runs cancel automatically), then merge;
7. use Shield testing at meaningful phase checkpoints rather than every slice.

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

`aerialrows-development` is currently not protected and there is no repository ruleset. Until protection is enabled in GitHub, "green before merge" is a procedural rule rather than server-enforced.

Recommended protection when repository admin settings are available: require pull requests and the Android `build` status check for `aerialrows-development`.

## Research/IP boundary

C1–C12 research is complete. Projectivy remains a behavioral/architectural reference only. Do not copy proprietary decompiled source, licensing/protection logic or proprietary binaries into this repository.
