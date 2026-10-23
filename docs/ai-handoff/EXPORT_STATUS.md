# AI Handoff Export Status

Canonical process: `docs/ai-handoff/EXPORT_PROCESS.md`  
Expected corpus: `docs/ai-handoff/raw/SOURCE_CATALOG.json`  
Machine state: `docs/ai-handoff/raw/ARCHIVE_MANIFEST.json`  
Validator: `python docs/ai-handoff/tools/validate_archive.py`

**Export Process v5 is frozen.** Do not redesign the export method unless the existing validator fails.

## Orientation/context layer

**COMPLETE**

## Raw Couchy archive

**C01-C12 — COMPLETE / validated source identities / manifested**

## Projectivy raw archive

**NEXT: Stage 2 — Projectivy G1-G4 originals + post-closure audits.**

Remaining after that: G5-G8, G9-G12, then final process/chat closure.

## Audit-process/tooling archive

Final closure remains pending. Do not commit proprietary APK/decompiled source/JADX/REA binaries.

## Final E9

Run `validate_archive.py --require-complete`, verify process/chat coverage, and compare against the research baseline to prove export changes are documentation/workflow only.
