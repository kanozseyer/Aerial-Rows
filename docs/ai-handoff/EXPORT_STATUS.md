# AI Handoff Export Status

Canonical process: `docs/ai-handoff/EXPORT_PROCESS.md`  
History: `docs/ai-handoff/EXPORT_HISTORY.md`  
Machine index: `docs/ai-handoff/raw/ARCHIVE_MANIFEST.json`  
Validator: `python docs/ai-handoff/tools/validate_archive.py`

## Current format

**Export Process v3** is authoritative. Raw report bytes are validated through per-report manifests and an automated validator. Prose checkpoints are secondary to manifest validation.

## Orientation/context layer — COMPLETE

The repository contains the durable handoff entrypoints (`AGENTS.md`, `AI_PROJECT_CONTEXT.md`, C12 blueprint, chat reconstruction, detailed findings, research index and process state).

## Raw Couchy comparison archive

- C01 — COMPLETE / manifested
- C02 — COMPLETE / manifested
- C03 — COMPLETE / manifested; legacy one-byte part-3 defect repaired during v3 retrofit
- C04 — COMPLETE / manifested
- C05 — COMPLETE / manifested
- C06 — COMPLETE / manifested
- C07 — PENDING
- C08 — PENDING
- C09 — PENDING
- C10 — PENDING
- C11 — PENDING
- C12 — PENDING

## Projectivy raw archive

PENDING: original Groups 1–12 plus authoritative post-closure/corrected audits.

Authoritative corrected audits to preserve include:

- Group 8 post-closure v2
- Group 9 post-closure v3
- Group 10 post-closure v2
- Group 11 post-closure v2
- Group 12 post-closure v2

## Audit-process/tooling archive

PENDING: closure matrix, resumable batch method, cache/recovery rules, completion receipts, REA/JADX provenance, final contract v18/tool-bundle hashes, and other durable process hardening notes.

Do not commit proprietary APK/decompiled source/JADX/REA binaries.

## Final validation group E9

PENDING after all raw reports are present:

- validate every manifest;
- verify all expected C1–C12 reports are represented;
- verify all Projectivy original/final audits are represented;
- verify process history is represented;
- update README/AGENTS/research index paths;
- compare documentation-only export against the pre-export application source baseline;
- mark the archive complete.

## Current continuation point

**Next subgroup: E3c — raw Couchy C7 only.**

Do not begin Phase 0A implementation until the user either completes/pauses the export explicitly.
