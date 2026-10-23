# AI Handoff Export Status

Canonical process: `docs/ai-handoff/EXPORT_PROCESS.md`  
Expected corpus: `docs/ai-handoff/raw/SOURCE_CATALOG.json`  
Machine state: `docs/ai-handoff/raw/ARCHIVE_MANIFEST.json`  
Validator: `python docs/ai-handoff/tools/validate_archive.py`

## Current format

**Export Process v4** is authoritative. Machine validation covers byte-exact integrity and complete expected-corpus coverage through `SOURCE_CATALOG.json`. Prose checkpoints are secondary.

## Orientation/context layer

**COMPLETE**

## Raw Couchy comparison archive

- C01 — COMPLETE / manifested / cataloged
- C02 — COMPLETE / manifested / cataloged
- C03 — COMPLETE / repaired / manifested / cataloged
- C04 — COMPLETE / manifested / cataloged
- C05 — COMPLETE / manifested / cataloged
- C06 — COMPLETE / manifested / cataloged
- C07 — COMPLETE / manifested / cataloged
- C08 — PENDING
- C09 — PENDING
- C10 — PENDING
- C11 — PENDING
- C12 — PENDING

## Projectivy raw archive

**PENDING.** `SOURCE_CATALOG.json` enumerates every required original and authoritative post-closure report.

## Audit-process/tooling archive

**PENDING**, except final contract/tool-bundle reference hashes are machine-cataloged. Do not commit proprietary APK/decompiled source/JADX/REA binaries.

## Final E9 validation

Run `python docs/ai-handoff/tools/validate_archive.py --require-complete`, verify process-history coverage and documentation-only diff versus the pre-export app-source baseline, then mark `HANDOFF_MANIFEST.json` complete.

## Current continuation point

**Next subgroup: E3d — raw Couchy C08 only.**

Do not begin Phase 0A implementation until the user either completes this export or explicitly pauses it.
