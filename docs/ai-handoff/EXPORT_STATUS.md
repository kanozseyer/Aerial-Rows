# AI Handoff Export Status

This file makes the chat/research export resumable. The original attempt tried to stage very large Git blobs before creating the final tree/commit, which made progress look stuck and made failure recovery poor. The safer process is now: **small logical groups, commit each group immediately, verify the branch, then continue**.

## Recovery result

The earlier export did **not** fail completely. It successfully created commit:

`b34d295407845ff1cb08e17d93875e888d3bbe24` — `Add complete AI project handoff and research context`

Parent research baseline:

`3c1cfd56234f1d386f4553dd6c69523670500d70`

That commit added the orientation/context layer without changing Kotlin, Gradle, manifest, or application resources.

## Already committed — E0/E1

- `AGENTS.md`
- `AI_PROJECT_CONTEXT.md`
- `docs/ai-handoff/README.md`
- `docs/ai-handoff/C12_FINAL_BLUEPRINT.md`
- `docs/ai-handoff/CHAT_EXPORT.md`
- `docs/ai-handoff/COUCHY_DETAILED_FINDINGS.md`
- `docs/ai-handoff/PROJECTIVY_DETAILED_FINDINGS.md`
- `docs/ai-handoff/PROCESS_STATE.md`
- `docs/ai-handoff/RESEARCH_INDEX.md`

These are enough for a new AI to understand the project and continue implementation, but they are **consolidations**, not yet the complete raw report corpus requested by the user.

## Export Process v2 — reliability/speed improvements

The C3–C5 export exposed three avoidable sources of fragility: too many tiny writes, trailing-newline normalization at chunk boundaries, and directory listings growing too large to use as a universal verification mechanism.

From C6 onward use this process:

1. Work on **one report only** per resumable subgroup.
2. Split large reports into the **fewest safe UTF-8 chunks**, targeting roughly 12–16 KB per part rather than ~4 KB.
3. Every **intermediate** chunk must end on a non-newline byte. Move the complete newline run at the split boundary to the **start of the next chunk**. Concatenating the ordered chunks must reproduce the source bytes exactly.
4. Before upload, record for every part:
   - byte size;
   - SHA-256;
   - expected Git blob SHA-1 (`git hash-object` semantics);
   - whether it begins/ends with a newline.
5. Commit a `Cxx.manifest.json` beside the parts containing the original whole-report SHA-256 and all per-part verification data.
6. Verify remote content using Git blob SHA:
   - for a small archive directory, one directory listing is acceptable;
   - once the directory listing becomes too large/truncated, fetch **only the newly written parts** and compare their remote `sha` values with the manifest.
7. Because the local ordered parts are verified before upload and every remote blob must match its corresponding local Git blob SHA, exact remote whole-report reconstruction follows transitively from those checks.
8. Update this checkpoint only after verification passes.
9. Stop/report before beginning the next subgroup.

This reduces GitHub commits/tool calls substantially while preserving byte-exact recovery.

## E2 — Couchy raw reports C1–C4

Target directory:

`docs/ai-handoff/raw/couchy/`

### E2a — C1–C2

Status: **COMPLETE**

Committed:

- `C01.md` — original SHA-256 `c85c2188906a849854865fc549e1f62c697899d7a648d05fb3f1fd6954e88cea`
- `C02.md` — original SHA-256 `2c9732f438cdfc075239ba4c06754821d09846e4d706fbf7dc181dc480e0e381`

### E2b — C3

Status: **COMPLETE**

Committed as ordered parts:

- `C03.part01.md`
- `C03.part02.md`
- `C03.part03.md`
- `C03.part04.md`
- `C03.part05.md`
- `C03.part06.md`

Reassembly check:

`cat C03.part*.md | sha256sum` -> `c756b965caa99ac016eb312a9828be05cfcd6618d666c5b41b1ca333c6761f03`

This exactly matches the original `couchy_group03.md` SHA-256 in `RESEARCH_INDEX.md`.

### E2c — C4

Status: **COMPLETE**

Committed as ordered parts:

- `C04.part01.md`
- `C04.part02.md`
- `C04.part03.md`
- `C04.part04.md`
- `C04.part05.md`
- `C04.part06.md`

Local source split/reassembly check:

`cat C04.part*.md | sha256sum` -> `935945ec232fe56e6e64e2112eb19179a3297e6194594234d239b9cde5ebd297`

Every GitHub C04 part blob was compared with its local Git blob hash. Part 2 initially differed by one trailing newline byte; it was corrected before E2c was marked complete. The six remote parts are byte-for-byte identical to the six local source chunks, so their concatenation reproduces the original `couchy_group04.md` SHA-256 exactly.

**E2 status: COMPLETE.**

## E3 — Couchy raw reports C5–C8

Status: **IN PROGRESS**

For safe recovery, E3 is subdivided one report at a time.

### E3a — C5 only

Status: **COMPLETE**

Committed as ordered parts:

- `C05.part00.md`
- `C05.part01.md`
- `C05.part02.md`

Original report SHA-256:

`e7603080accb1bdb90cf2672048f647276671649d3cfc61c4712130950bd4b78`

The initial local split reconstructed exactly to that SHA. GitHub normalized one boundary newline at the end of part 0, so the exact newline was moved to the beginning of part 1. Final remote/local Git blob identities for the adjusted chunks are:

- `C05.part00.md` -> `b070d6ee9f4f750af992fa5d2365cd8fb8acc90b`
- `C05.part01.md` -> `cfcb9271732b13927eacf401f15fe8a489ed1ad5`
- `C05.part02.md` -> `71d0a7067a812110ba9285e4d94f7fc818ebf12f`

Concatenating those adjusted chunks still reproduces the original report byte-for-byte and yields the original SHA-256 `e7603080accb1bdb90cf2672048f647276671649d3cfc61c4712130950bd4b78`.

### E3b — C6 only

Status: **COMPLETE**

Export Process v2 was introduced here.

Committed:

- `C06.part00.md`
- `C06.part01.md`
- `C06.manifest.json`

Original report:

- size: `27847` bytes
- SHA-256: `0bf49b7fed0fa4fee3abe2c86f133c3f62fb1eba708821c4b2cebf12cba37b18`

Verified remote Git blob identities:

- `C06.part00.md` -> `37cd26bf580aaad5b027db76f0b8aab59d7f82f7`
- `C06.part01.md` -> `2faf6edb83e7d05cee8a135c0f901cff8c2b09c4`

These exactly match the locally precomputed Git blob hashes in `C06.manifest.json`. The ordered local chunks reconstruct exactly to the original C6 SHA-256, so the verified remote blobs reproduce the original report byte-for-byte.

### E3c — C7 only

Status: **PENDING**

Use Export Process v2.

### E3d — C8 only

Status: **PENDING**

Use Export Process v2.

## E4 — Couchy raw reports C9–C12

Status: **PENDING**

Use one report per resumable subgroup and Export Process v2.

## E5 — Projectivy raw G1–G4

Target directory:

`docs/ai-handoff/raw/projectivy/`

Include each original group report and its post-closure audit where available.

Status: **PENDING**

## E6 — Projectivy raw G5–G8

Include originals + post-closure/corrected reports, with Group 8 using the final v2 audit as the authoritative correction.

Status: **PENDING**

## E7 — Projectivy raw G9–G12

Include originals + final corrected reports:

- Group 9 post-closure v3
- Group 10 post-closure v2
- Group 11 post-closure v2
- Group 12 post-closure v2

Status: **PENDING**

## E8 — Raw audit-process/tooling history

Target directory:

`docs/ai-handoff/raw/process/`

Include the durable audit-process notes needed to reproduce or safely resume APK analysis if ever necessary: closure matrix, resumable batch behavior, caches, recovery rules, completion receipts, REA/JADX provenance, and final contract/tool-bundle hashes.

Do **not** commit proprietary APKs, decompiled Projectivy source, JADX binaries, REA binaries, or other third-party analysis binaries.

Status: **PENDING**

## E9 — Final validation and manifest

Tasks:

- verify all expected raw files/parts are present;
- compare/reassemble SHA-256 values against `RESEARCH_INDEX.md` where recorded;
- verify every C1–C12 report is represented;
- verify every Projectivy G1–G12 original/final audit is represented;
- update `README.md`, `AGENTS.md`, and `RESEARCH_INDEX.md` with raw archive paths;
- mark this export COMPLETE;
- verify application source files are unchanged from the pre-export implementation baseline except for documentation commits.

Status: **PENDING**

## Failure/recovery rule

If a tool call, conversation, or environment fails mid-subgroup:

- do not repeat completed groups/subgroups;
- inspect the branch head and this file first;
- check which individual part files from the current report exist;
- continue only the missing part files;
- use small GitHub Contents API writes rather than one giant archive/blob;
- use Export Process v2 for new split reports;
- verify exact report identity before marking a subgroup complete;
- never wait until the end of the whole export to commit progress.

## Current continuation point

**Next subgroup: E3c — raw Couchy C7 only.**

Do not begin Phase 0A implementation until the user either finishes E2–E9 or explicitly chooses to pause the export and start coding.
