# Aerial-Rows AI Handoff

This directory is the durable export of the Launcher project research conversation completed on 2026-10-23. It is intended for fresh AI sessions and human contributors who do not have the original chat history.

## Read order

1. [`../../AI_PROJECT_CONTEXT.md`](../../AI_PROJECT_CONTEXT.md) — current project state, provenance, invariants and next action.
2. [`C12_FINAL_BLUEPRINT.md`](C12_FINAL_BLUEPRINT.md) — final implementation architecture and phase gates.
3. [`CHAT_EXPORT.md`](CHAT_EXPORT.md) — chronological reconstructed project/chat record.
4. [`COUCHY_DETAILED_FINDINGS.md`](COUCHY_DETAILED_FINDINGS.md) — detailed C1–C12 source comparison findings and known bugs.
5. [`PROJECTIVY_DETAILED_FINDINGS.md`](PROJECTIVY_DETAILED_FINDINGS.md) — verified Projectivy behavioral findings/post-closure corrections used as reference.
6. [`RESEARCH_INDEX.md`](RESEARCH_INDEX.md) — completed report inventory, hashes and target provenance.
7. [`PROCESS_STATE.md`](PROCESS_STATE.md) — final REA/JADX audit/recovery process if APK research ever needs reopening.

## Current implementation next step

**Phase 0A — source-pure build.**

Materialize the code/resources injected by `applyAerialRowsSourcePatches`, remove the task, and verify debug/release/lint builds do not mutate tracked source.

## Important boundary

The handoff contains findings and provenance, not proprietary Projectivy binaries or decompiled source. Projectivy behavior must be independently implemented using clean Android/Compose interfaces. Licensing/premium bypass and opaque native/vendor maintenance features are not reconstruction goals.

## Fresh AI prompt

A useful starting prompt for a new conversation is:

> Continue the Aerial-Rows Android TV launcher project. Read `AGENTS.md`, `AI_PROJECT_CONTEXT.md`, and every file in `docs/ai-handoff/` before making changes. The Projectivy/Couchy research C1–C12 is complete. Verify the live `aerialrows-development` branch, then continue from Phase 0A while preserving the C11 performance/reliability invariants.
