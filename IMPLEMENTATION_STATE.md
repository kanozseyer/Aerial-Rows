# Aerial-Rows — Current Implementation State

Updated: 2026-10-23

This file is the authoritative **current implementation checkpoint** for continuation work. It supersedes older "Immediate next task" wording in `AI_PROJECT_CONTEXT.md`, which remains the historical C1–C12 research/provenance record.

## Current branch and phase

Working branch: `aerialrows-development`.

**Phase 5 — wallpaper/Aerial engine is merged** at `63e3590f859c2421fde8e28ed3329e6e192069d3`.

The active implementation phase is **Phase 6 — settings architecture**. Phases 0A/0B and implemented Phase 1–5 work are complete; do not restart them unless a regression specifically requires it.

The first Phase 6 milestone — semantic settings navigation and shared choice controls — is merged at `891bcdb257110ddcad74312e54a07f7c8d704a51`.

## Phase 5 completion checkpoint

The merged wallpaper runtime has secure/typed Aerial playback, fair shuffle and quarantine, shared SurfaceView/Media3 video rendering, `WallpaperPlan`/`WallpaperEngineState`, provider boundaries, unified `WallpaperHost`, explicit failure/recovery and the existing low-memory photo/pseudo-blur hot paths. LauncherScreen no longer owns wallpaper-provider internals.

A meaningful Shield checkpoint for wallpaper/fallback/dock behavior is still useful, but it does not block Phase 6 coding.

## Active Phase 6 runtime milestone

The current milestone batches runtime behavior and CI-speed work before one full merge gate:

- semantic IDs expanded to Display and Wallpaper controls;
- reusable semantic focus bindings for settings routes;
- Display and Wallpaper restore the exact visible semantic setting rather than always jumping to row 1;
- hidden conditional rows are excluded from the active focus-ID set so focus restoration cannot target an uncomposed option;
- pure `SettingMutationBuffer` coalesces pending transforms by `SettingId` without overwriting unrelated config changes;
- pure `SettingDebounceScheduler` owns debounce deadlines and explicit flush behavior;
- `DebouncedSettingsWriter` applies captured mutations over the latest ConfigStore value;
- rapid Display selectors and wallpaper dim/speed use optimistic local UI with coalesced persistence;
- pending writes flush when Settings closes or HOME stops;
- typed Display/Wallpaper descriptors define validation, persistence policy and effects;
- stable setting/route identity uniqueness is regression-tested;
- Gradle parallel execution and local build cache are enabled;
- CI keeps a trusted `aerialrows-development` Gradle cache warm so later PRs can restore a base-branch cache instead of repeatedly starting cold;
- benchmark APK compilation remains conditional while release/R8, unit tests, debug, device-test compile, lint and source-purity remain mandatory at milestone PR boundaries.

Development validation for this milestone uses a cheap unit-test + Kotlin-compile check before opening the single full-CI PR.

After this milestone, continue Phase 6 with:

1. attach semantic focus/descriptors to remaining high-value settings screens (Status, Launcher, Apps/Channels where useful);
2. move panel appearance sliders onto the same debounced writer;
3. add explicit ON_CONFIRM/session preview adapters only where a setting genuinely needs them;
4. progressively route setting effects (`RELOAD_APPS`, `RELOAD_TV_CONTENT`, `RELOAD_WALLPAPER`, recreate/restart) through one effect dispatcher;
5. migrate remaining named multi-choice controls to the shared choice-dialog primitive;
6. finish Phase 6 with a Shield D-pad/settings checkpoint before Phase 7 notifications/media work.

## Development / CI strategy

Use a **milestone workflow**, not one expensive full CI run per tiny slice:

1. batch related work on one milestone branch;
2. stage later work as unopened branches while the active milestone validates;
3. use pure JVM tests, targeted review and a cheap compile/unit check during development;
4. temporary one-shot materializers/check workflows are allowed only when they materially reduce time/risk and must be removed before review;
5. open one product PR when the milestone is ready;
6. run the Android/R8/lint/source-purity gate once at the milestone boundary;
7. fix concrete failures on the same PR (superseded runs cancel automatically), then merge;
8. use Shield testing at meaningful phase checkpoints rather than every slice.

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
