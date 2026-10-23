# Aerial-Rows — Current Implementation State

Updated: 2026-10-23

This file is the authoritative **current implementation checkpoint** for continuation work. It supersedes any older "Immediate next task" wording in `AI_PROJECT_CONTEXT.md`, which remains the historical C1–C12 research handoff/provenance record.

## Current branch and phase

Working branch: `aerialrows-development`.

The project is in **Phase 5 — wallpaper/Aerial engine**. Phases 0A/0B and the implemented Phase 1–4 slices are already complete; do not restart them unless a regression specifically requires it.

Merged Phase 5 work through the current product checkpoint includes:

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
- LauncherScreen routed through that Aerial provider;
- current-state Media3 callbacks plus renderer-local per-attempt error de-duplication so one player error/stall cannot be counted twice or attributed using stale Compose state.

The product checkpoint after Phase 5N is commit `c3a04bed1e6deab161a3312a19d9a290dacc0370` before this documentation/process update.

## Next Phase 5 product slices

Continue with small, behavior-preserving slices. Already drafted as branches but intentionally not opened as stacked PRs:

1. `phase5l-photo-provider` — move the existing 1280px sharp / 96px blurred RGB_565 photo decode + `prepareToDraw()` behind `LocalPhotoProvider`.
2. `phase5m-wire-photo-provider` — route LauncherScreen custom-photo loading through that provider and remove its private bitmap decoder.
3. Then continue the blueprint toward runtime `WallpaperEngine` wiring, provider/descriptor separation, shared image rendering, rotation scheduling and semantic wallpaper events while preserving SurfaceView/Media3 hot-path behavior.

Those 5L/5M branches were drafted before later CI/process changes. Before opening them, replay **only their changed files** onto the latest development base. Never transplant an old full tree across unrelated base changes.

## CI / release process state

Current CI rules:

- builds must not mutate tracked source;
- superseded runs for the same PR/ref are cancelled automatically;
- full Android build jobs are bounded to 30 minutes;
- AI-handoff validation jobs are bounded to 10 minutes;
- ordinary build/test jobs use read-only repository contents permission;
- tagged release publishing runs in a separate write-enabled job;
- replacing a release must preserve the existing Git tag, and release creation verifies that the tag already exists rather than silently creating/moving it.

## Development process rules

- Keep **one active product PR at a time**. Later product slices may be staged as unopened branches, but do not build a long stack of open PRs on unmerged parents.
- Merge only after the exact current PR head passes the full Android gate. An older run is acceptable only when its Git tree is proven byte-for-byte identical; prefer a fresh exact-head run after ancestry changes.
- Squash merges change commit ancestry. After a parent squash-merge, replay the child change onto the new base rather than carrying superseded parent commits.
- If the base gained unrelated files (especially CI/workflow changes), replay changed file blobs/patches onto the latest base. Do **not** reuse an old whole-tree SHA, which would silently revert those unrelated changes.
- Temporary one-shot helper workflows must never remain in the reviewed tree. Prefer Git blobs/tree/commit operations or direct file updates for durable changes.
- Preserve the Phase 0A source-purity invariant: builds must not mutate tracked source.
- Preserve Couchy performance invariants and keep optional expensive work demand-gated.

## Known repository-admin gap

`aerialrows-development` is currently not protected and there is no repository ruleset. The connected GitHub capability can read this state but cannot change admin protection settings. Until protection is enabled in GitHub, "green before merge" is a procedural rule rather than server-enforced.

Recommended protection when repository admin settings are available: require pull requests and the Android `build` status check for `aerialrows-development`.

## Research/IP boundary

C1–C12 research is complete. Projectivy remains a behavioral/architectural reference only. Do not copy proprietary decompiled source, licensing/protection logic, or proprietary binaries into this repository.
