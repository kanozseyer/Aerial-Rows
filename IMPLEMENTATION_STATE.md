# Aerial-Rows — Current Implementation State

Updated: 2026-10-23

This file is the authoritative **current implementation checkpoint** for continuation work. It supersedes older "Immediate next task" wording in `AI_PROJECT_CONTEXT.md`, which remains the historical C1–C12 research/provenance record.

## Current branch and phase

Working branch: `aerialrows-development`.

**Phase 5 — wallpaper/Aerial engine is merged** at `63e3590f859c2421fde8e28ed3329e6e192069d3`.

The active implementation phase is **Phase 6 — settings architecture**. Phases 0A/0B and implemented Phase 1–5 work are complete; do not restart them unless a regression specifically requires it.

## Phase 5 completion checkpoint

The merged wallpaper runtime now has:

- secure TLS, typed Aerial metadata, fair shuffle, failure quarantine and muted/no-audio decoding;
- shared SurfaceView/Media3 `VideoWallpaper` renderer with current callbacks and per-attempt error de-duplication;
- typed `WallpaperPlan`, pure `WallpaperEngineState` and semantic source events;
- `BuiltinAerialProvider` and `LocalPhotoProvider` boundaries;
- unified `WallpaperHost` runtime ownership for Aerial/local-video/photo/preset fallback;
- demand-gated provider work and explicit failure/recovery paths;
- existing 1280px RGB_565 photo + 96px pseudo-blur hot path preserved;
- LauncherScreen no longer owns wallpaper provider internals.

A meaningful Shield checkpoint for wallpaper/fallback/dock behavior is still useful, but it does not block Phase 6 coding.

## Active Phase 6 milestone

Current work batches the first settings-architecture foundation before one full CI gate:

- stable semantic `SettingId` and `SettingsRoute` values;
- `SettingsNavigationState` route stack and per-route semantic focus memory;
- settings descriptors with validation, persistence policy, effect and export policy;
- pure change planning for IMMEDIATE / DEBOUNCED / ON_CONFIRM / SESSION_ONLY persistence;
- effect batching without losing independent reload/recreate work;
- `SettingsSheet` migrated from a private screen enum to semantic routes;
- root settings menu restores the exact semantic row after returning from a sub-screen;
- selector rows can open a D-pad choice dialog focused on the current value while retaining Left/Right fast stepping;
- named dropdown choices enabled for layout, UI scale, roundness, icon size, spacing, wallpaper speed and gradient preset;
- existing toggle/checkmark behavior remains direct and focus-stable.

After this milestone merges, continue Phase 6 with:

1. semantic focus memory for important sub-screens rather than only the root menu;
2. session preview + coalesced DataStore writes for rapid/debounced settings;
3. progressively attach descriptors/effects to existing settings instead of rewriting all screens at once;
4. migrate remaining named multi-choice selectors to the shared choice-dialog primitive;
5. keep dialogs returning focus to their source row and current option.

## Development / CI strategy

Use a **milestone workflow**, not one expensive full CI run per tiny slice:

1. batch related work on one milestone branch with logical commits;
2. use pure JVM tests, targeted diff review and direct Git file/tree operations during development;
3. temporary one-shot materializers are allowed only when they materially reduce risk/time and must be removed before review;
4. open one product PR when the milestone is ready;
5. run the Android/R8/lint/source-purity gate once at the milestone boundary;
6. fix concrete CI failures on the same PR (superseded runs cancel automatically), then merge;
7. use Shield testing at meaningful checkpoints rather than every implementation slice.

The active Phase 6 milestone also validates a CI speed improvement: release/R8, unit tests, debug, device-test compilation, lint and source-purity remain mandatory on every PR, while benchmark APK variants are rebuilt only for benchmark/build-configuration changes (and always for tags). This avoids recompiling benchmark variants for unrelated UI/domain milestones.

## Development process rules

- Prefer one active product PR/milestone at a time; later ideas can be staged but should not become a long open PR stack.
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
