# Aerial-Rows — Current Implementation State

Updated: 2026-10-23

This file is the authoritative **current implementation checkpoint** for continuation work. It supersedes any older "Immediate next task" wording in `AI_PROJECT_CONTEXT.md`, which remains the historical C1–C12 research handoff/provenance record.

## Current branch and phase

Working branch: `aerialrows-development`.

The project is finishing **Phase 5 — wallpaper/Aerial engine**. Phases 0A/0B and the implemented Phase 1–4 slices are complete; do not restart them unless a regression specifically requires it.

Merged Phase 5 work includes:

- secure TLS for Aerial playback;
- typed Aerial metadata;
- fair shuffle-bag selection;
- audio-track decoding disabled for wallpaper video;
- session-local repeated-failure quarantine;
- shared `VideoWallpaper` renderer extraction;
- explicit typed wallpaper fallback plan;
- separate photo refresh vs Aerial rotation triggers;
- pure `WallpaperEngineState`/semantic events;
- typed `BuiltinAerialProvider` boundary;
- current-state Media3 callbacks plus renderer-local per-attempt error de-duplication;
- `LocalPhotoProvider` with IO-thread RGB_565 downsampling, `prepareToDraw()`, 1280px sharp and 96px pseudo-blur assets.

The active Phase 5 milestone completes the runtime ownership move:

- `WallpaperHost` is the single Compose/Android adapter around `WallpaperEngineState`;
- provider work is demand-gated by the current plan;
- Aerial, local-video, photo and preset fallback are selected through one typed state machine;
- local-video failure now falls through explicitly instead of pinning the cover;
- Aerial network/failure/quarantine recovery is coordinated through semantic source events;
- normal Aerial clip changes keep the previous frame until the next item is ready;
- dock-expanded pseudo-blur remains the existing cheap 96px texture path;
- `LauncherScreen` no longer owns photo/Aerial provider state or wallpaper rendering internals.

After this milestone passes CI and merges, the next action is one real-device Shield checkpoint for wallpaper/fallback/dock behavior, then begin **Phase 6 — settings architecture**.

## Development / CI strategy

Use a **milestone workflow**, not one expensive full CI run per tiny slice:

1. batch related work on one milestone branch with logical commits;
2. use pure JVM tests, targeted diff review and direct Git file/tree operations while developing;
3. temporary one-shot materializers are allowed only when they materially reduce risk/time and must be removed before review;
4. open one product PR when the milestone is ready;
5. run the full Android/R8/lint/source-purity gate once at the milestone boundary;
6. fix concrete CI failures on the same PR (superseded runs cancel automatically), then merge;
7. use Shield testing at meaningful phase checkpoints rather than every implementation slice.

Current CI rules:

- builds must not mutate tracked source;
- superseded runs for the same PR/ref are cancelled automatically;
- full Android build jobs are bounded to 30 minutes;
- AI-handoff validation jobs are bounded to 10 minutes;
- ordinary build/test jobs use read-only repository contents permission;
- tagged release publishing runs in a separate write-enabled job;
- replacing a release must preserve the existing Git tag, and release creation verifies that the tag already exists rather than silently creating/moving it.

## Development process rules

- Prefer one active product PR/milestone at a time; later ideas can be staged but should not become a long open PR stack.
- Merge only after the exact current PR head passes the full Android gate.
- Squash merges change commit ancestry. Replay later changes onto the new base rather than carrying superseded parent commits.
- If the base gained unrelated files (especially CI/workflow changes), replay changed file blobs/patches onto the latest base. Do **not** reuse an old whole-tree SHA.
- Preserve the Phase 0A source-purity invariant and Couchy performance invariants.
- Optional feature disabled must mean its expensive provider/background work is disabled.
- Prefer callbacks/events over polling and one shared renderer/provider path over parallel implementations.

## Known repository-admin gap

`aerialrows-development` is currently not protected and there is no repository ruleset. The connected GitHub capability can read this state but cannot change admin protection settings. Until protection is enabled in GitHub, "green before merge" is a procedural rule rather than server-enforced.

Recommended protection when repository admin settings are available: require pull requests and the Android `build` status check for `aerialrows-development`.

## Research/IP boundary

C1–C12 research is complete. Projectivy remains a behavioral/architectural reference only. Do not copy proprietary decompiled source, licensing/protection logic, or proprietary binaries into this repository.
