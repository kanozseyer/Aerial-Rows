# Aerial-Rows — Current Implementation State

Updated: 2026-10-23

This file is the authoritative **current implementation checkpoint** for continuation work. It supersedes older "Immediate next task" wording in `AI_PROJECT_CONTEXT.md`, which remains the historical C1–C12 research/provenance record.

## Current branch and phase

Working branch: `aerialrows-development`.

**Phase 5 — wallpaper/Aerial engine is merged** at `63e3590f859c2421fde8e28ed3329e6e192069d3`.

The active implementation phase is **Phase 6 — settings architecture**. Phases 0A/0B and implemented Phase 1–5 work are complete; do not restart them unless a regression specifically requires it.

Merged Phase 6 milestones:

- semantic settings navigation and shared choice controls: `891bcdb257110ddcad74312e54a07f7c8d704a51`;
- debounced settings runtime, Display/Wallpaper semantic focus and CI/cache speed work: `d458d2ed92a8c820c8634bb7aca73984d6a0016b`.

## Phase 5 completion checkpoint

The merged wallpaper runtime has secure/typed Aerial playback, fair shuffle and quarantine, shared SurfaceView/Media3 video rendering, `WallpaperPlan`/`WallpaperEngineState`, provider boundaries, unified `WallpaperHost`, explicit failure/recovery and the existing low-memory photo/pseudo-blur hot paths. LauncherScreen no longer owns wallpaper-provider internals.

A meaningful Shield checkpoint for wallpaper/fallback/dock behavior is still useful, but it does not block Phase 6 coding.

## Active Phase 6 panel-settings milestone

The current unopened milestone is rebased directly onto `d458d2ed92a8c820c8634bb7aca73984d6a0016b` and contains only the intended settings/runtime changes. Temporary materializer workflows have been removed.

Current work:

- stable `SettingId` for Sliding Rows top margin and existing panel appearance controls;
- panel style stays immediate because it changes which controls are composed;
- top margin, opacity, tint hue, tint saturation and glass-effect strength use validated debounced persistence;
- `ExtraSelector` uses optimistic local selection so D-pad stepping remains visually instant while DataStore writes are coalesced;
- Display focus restoration includes only panel controls currently visible for Clear/Solid/Liquid Glass state;
- pending panel mutations feed the live panel preview immediately through Compose-observable preview state;
- captured debounced mutations still persist non-cancellably once drained, preserving the runtime reliability invariant;
- preview invalidation waits for persisted ConfigStore state after a write, avoiding a brief rollback/flicker to the old base value;
- descriptor tests cover immediate panel style, debounced panel controls and the discrete top-margin value set.

After this milestone, continue Phase 6 with:

1. semantic focus/descriptors for Status Bar and Launcher settings;
2. explicit setting-effect dispatch (`RELOAD_APPS`, `RELOAD_TV_CONTENT`, `RELOAD_WALLPAPER`, recreate/restart) instead of ad-hoc reload callbacks;
3. ON_CONFIRM/session-preview adapters only where a setting genuinely needs them;
4. remaining named multi-choice controls migrated to the shared choice-dialog primitive where useful;
5. a Shield D-pad/settings checkpoint before Phase 7 notifications/media work.

## Development / CI strategy

Use a **milestone workflow**, not one expensive full CI run per tiny slice:

1. batch related work on one milestone branch;
2. stage later work as unopened branches while the active milestone validates;
3. use pure JVM tests, targeted review and a cheap compile/unit check during development where it saves a full PR cycle;
4. temporary one-shot materializers/check workflows are allowed only when they materially reduce time/risk and must be removed before review;
5. open one product PR when the milestone is ready;
6. run the Android/R8/lint/source-purity gate once at the milestone boundary;
7. fix concrete failures on the same PR (superseded runs cancel automatically), then merge;
8. use Shield testing at meaningful phase checkpoints rather than every slice.

Current CI speed path:

- Gradle parallel execution and local build cache are enabled;
- pushes to `aerialrows-development` run a trusted compile/unit-test cache warmer;
- the first post-merge cache warmer completed successfully after `d458d2e`;
- PRs targeting `aerialrows-development` can restore that base-branch cache;
- benchmark APKs are compiled only when benchmark/build configuration changes (and for tags);
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
