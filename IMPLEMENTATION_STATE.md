# Aerial-Rows — Current Implementation State

Updated: 2026-10-23

This file is the authoritative **current implementation checkpoint** for continuation work. It supersedes any older "Immediate next task" wording in `AI_PROJECT_CONTEXT.md`, which remains the historical C1–C12 research handoff/provenance record.

## Current branch and phase

Working branch: `aerialrows-development`.

The project is in **Phase 5 — wallpaper/Aerial engine**. Phases 0A/0B and the implemented Phase 1–4 slices are already complete; do not restart them unless a regression specifically requires it.

Merged Phase 5 work through the current baseline includes:

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
- CI source-cleanliness gate, per-PR/ref concurrency cancellation and job timeouts.

## Active correctness fix

**Phase 5N — current video callback ownership** is the active product slice.

Reason: Media3 listener/watchdog callbacks outlive individual Compose recompositions. They must read current callbacks/URI, and player-error + stall-watchdog reports must be de-duplicated per `(player, uri)` playback attempt so one failed Aerial cannot be counted twice or attributed using stale state.

Do not advance the product stack until this slice is green and merged.

## Staged next Phase 5 work

After 5N, continue with small, behavior-preserving slices. Already drafted locally as branches but intentionally not opened as stacked PRs:

1. `phase5l-photo-provider` — move existing 1280px sharp / 96px blurred RGB_565 photo decode + `prepareToDraw()` behind `LocalPhotoProvider`.
2. `phase5m-wire-photo-provider` — route LauncherScreen custom-photo loading through that provider and remove its private bitmap decoder.
3. Then continue the blueprint toward runtime `WallpaperEngine` wiring, provider/descriptor separation, shared image rendering, rotation scheduling and semantic wallpaper events while preserving SurfaceView/Media3 hot-path behavior.

Because CI/workflow files changed after those staged branches were first drafted, replay **only their changed files** onto the latest development base before opening them. Never transplant an old full tree across unrelated base changes.

## Development process rules

- Keep **one active product PR at a time**. Later product slices may be staged as unopened branches, but do not build a long stack of open PRs on unmerged parents.
- Merge only after the exact current PR head passes the full Android gate. An older run is acceptable only when its Git tree is proven byte-for-byte identical; prefer a fresh exact-head run after ancestry changes.
- Squash merges change commit ancestry. After a parent squash-merge, replay the child change onto the new base rather than carrying superseded parent commits.
- If the base gained unrelated files (especially CI/workflow changes), replay changed file blobs/patches onto the latest base. Do **not** reuse an old whole-tree SHA, which would silently revert those unrelated changes.
- Temporary one-shot helper workflows must never remain in the reviewed tree. Prefer Git blobs/tree/commit operations or direct file updates for durable changes.
- The main Build workflow cancels superseded runs for the same PR/ref and has a 30-minute job bound; AI-handoff validation has a 10-minute bound.
- Preserve the Phase 0A source-purity invariant: builds must not mutate tracked source.
- Preserve Couchy performance invariants and keep optional expensive work demand-gated.

## Known repository-admin gap

`aerialrows-development` is currently not protected and there is no repository ruleset. The connected GitHub capability can read this state but cannot change admin protection settings. Until protection is enabled in GitHub, "green before merge" is a procedural rule rather than server-enforced.

Recommended protection when repository admin settings are available: require pull requests and the Android `build` status check for `aerialrows-development`.

## Research/IP boundary

C1–C12 research is complete. Projectivy remains a behavioral/architectural reference only. Do not copy proprietary decompiled source, licensing/protection logic, or proprietary binaries into this repository.
