# AI Handoff Export Process v4

This is the canonical procedure for exporting the Launcher/Aerial-Rows research and conversation context into GitHub without losing bytes, missing reports, or making partial progress difficult to recover.

## Goals

- Preserve every raw research report byte-for-byte.
- Make the archive readable by humans and mechanically verifiable by any future AI/model.
- Detect both integrity errors and coverage gaps.
- Avoid oversized/fragile tool payloads.
- Make every report an independently resumable unit.
- Publish each raw-report batch atomically.
- Never modify launcher application source while performing the documentation export.
- Never overwrite concurrent branch work.

## Authoritative machine files

- `raw/SOURCE_CATALOG.json` — expected corpus: every Couchy/Projectivy report ID, original filename and authoritative SHA-256.
- `raw/ARCHIVE_MANIFEST.json` — current export state: complete/pending/reference and manifest path.
- `raw/<collection>/<ID>.manifest.json` — byte-level manifest for one report.
- `HANDOFF_MANIFEST.json` — top-level handoff/read-order state.

Prose status is explanatory only. Machine manifests + validator are authoritative.

## v4 improvement over v3

v3 made completed reports byte-verifiable but expected-corpus coverage still depended partly on human/prose bookkeeping. v4 adds `SOURCE_CATALOG.json`, so the validator independently knows what must exist.

This catches forgotten post-closure reports, wrong source SHA values, unknown IDs, and silently omitted reports.

v4 also adds `package_report.py`, which rejects a source unless its SHA matches the catalog before generating any archive output.

## Canonical packaging

```bash
python docs/ai-handoff/tools/package_report.py /path/to/source.md \
  --collection couchy --report-id C07
```

The tool verifies the source against the catalog, chunks only when needed, writes exact bytes + manifest, proves local reassembly, and marks that report complete in the local archive manifest.

Default chunk target is about 24 KiB to reduce API/tool calls while remaining bounded. Raw Git-object publication removes the old Contents-API newline-normalization hazard.

## Atomic GitHub publication

For each raw report/subgroup:

1. read the live `aerialrows-development` head/tree;
2. prepare and validate files locally;
3. create exact Git blobs for raw report bytes (base64 is preferred through tools/APIs);
4. create one Git tree based on the current tree containing every report part, its manifest and state updates;
5. create one commit with the live head as parent;
6. move the branch using `expected_sha` compare-and-swap;
7. if the lease fails, never force—inspect concurrent work and rebuild on the new head;
8. require repository validation to pass before declaring the report closed.

Unattached blobs/trees are harmless if a tool call stops before the ref move; the branch remains unchanged.

## Validation

Progress:

```bash
python docs/ai-handoff/tools/validate_archive.py
```

Final E9 closure:

```bash
python docs/ai-handoff/tools/validate_archive.py --require-complete
```

Validation covers expected-corpus coverage, source SHA/catalog agreement, required context files, manifest/part existence, byte sizes, SHA-256, Git blob SHA-1 and exact full-report reassembly. Reference-only audit contract/process-bundle hashes are also checked without pretending their third-party/tool binaries are committed.

`.github/workflows/validate-ai-handoff.yml` runs the validator on repository changes.

## Failure/resume rule

If the chat/tool/environment stops:

1. inspect live branch head;
2. read `HANDOFF_MANIFEST.json`, `EXPORT_STATUS.md`, `SOURCE_CATALOG.json`, `ARCHIVE_MANIFEST.json`;
3. run the validator;
4. continue the first required catalog report whose state is `pending`;
5. never redo a complete report unless validation fails;
6. never trust prose over machine state;
7. use raw Git objects + atomic ref lease for raw report publication.

## Historical failures preserved

- Initial large staging attempt looked stuck and was difficult to resume.
- Early many-small-commit Contents API process was slow.
- C4/C5 exposed newline-boundary repair issues.
- v3 retroactive validation found C3 part 3 one byte short despite a previous prose success claim.
- v3 fixed byte integrity; v4 adds authoritative coverage.

## Repository hygiene / clean-room boundary

Do not commit proprietary Projectivy APK/decompiled source, JADX/REA binaries, signing material, licensing bypass logic, or unrelated private files. Commit only clean-room findings, provenance/process notes, conversation handoff material and open-source Aerial-Rows material.
