# AI Handoff Archive — Aerial-Rows

This directory is a durable export of the Launcher project research conversation completed on 2026-10-23.

It exists so a fresh AI model or contributor can continue the project without the original chat history.

## Read order

1. [`../../AI_PROJECT_CONTEXT.md`](../../AI_PROJECT_CONTEXT.md) — complete current-state handoff.
2. [`C12_FINAL_BLUEPRINT.md`](C12_FINAL_BLUEPRINT.md) — final Couchy ↔ Projectivy synthesis and implementation blueprint.
3. [`CHAT_EXPORT.md`](CHAT_EXPORT.md) — chronological project/research reconstruction.
4. Search [`COUCHY_C1_C12_FULL.md`](COUCHY_C1_C12_FULL.md) for the relevant C1–C11 subsystem report.
5. Search [`PROJECTIVY_G1_G12_FULL.md`](PROJECTIVY_G1_G12_FULL.md) for the matching Projectivy report and post-closure audit.
6. [`PROCESS_FULL.md`](PROCESS_FULL.md) if reopening reverse-engineering/tooling work.

## What is included

- Complete final Couchy/Aerial-Rows comparison reports C1–C12 in one searchable full archive.
- Original Projectivy Group 1–12 reports in one searchable full archive.
- Projectivy post-closure audits, including corrected Group 8/9/10/11/12 versions.
- Audit-process hardening notes.
- A comprehensive reconstructed chat/project export.
- Checksums in `MANIFEST.md`.

## What is deliberately not included

- The proprietary Projectivy APK.
- Decompiled Projectivy source.
- JADX/REA binaries.
- Any Projectivy licensing/premium bypass material.

The Projectivy artifact hashes and research provenance are preserved in `AI_PROJECT_CONTEXT.md` so the work can be identified without redistributing the proprietary binary.

## Current implementation next step

**Phase 0A: source-pure build.**

Materialize the code injected by `applyAerialRowsSourcePatches`, remove the task, and verify debug/release/lint builds leave the Git working tree unchanged.
