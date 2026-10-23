# Reverse-Engineering Audit Process — Full Archive
This file concatenates the complete source reports from the research archive. Each section preserves the original report text.


---

# SOURCE FILE: `PROCESS_STATE.md`

SHA-256: `b8663336133821108d668b0d3859834114a66521b30d62b6ab815e623d3f6182`

---

# Reverse-Engineering Process State / Provenance

This file preserves the final operational state of the Projectivy analysis in case a future contributor must reopen APK research.

## Target

- Projectivy Launcher APK 4.72 / code 97
- package `com.spocky.projengmenu`
- APK SHA256 `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
- signing certificate SHA256 `f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`
- minSdk 23 / targetSdk 37
- 7395 classes
- 1171 resource entries
- 3 DEX files
- `libptutils.so` in the four common Android ABIs

## Toolchain used

- JADX headless MCP 0.7.1 all-in-one JAR
- REA 6.7.0
- Java 21
- Node 22.20.0
- persistent JADX MCP daemon

Historical environment paths:

```text
/mnt/data/rea-portable
/mnt/data/rea
/mnt/data/launcherproject_work
```

These paths are historical only.

## Final process contract behavior

The audit process was hardened through contract v18.

Important final rules:

- semantic-first discovery;
- persistent daemon;
- no empty-query REA as exhaustive evidence;
- no aggressive parallel JVMs;
- complete class inventory;
- complete separately pinned APK resource inventory;
- bounded/resumable batches (default 6, max 12);
- write-ahead batch checkpoint before slow JVM query;
- interrupted attempt recovery;
- atomic report/state/evidence writes;
- failed probes quarantined;
- exact REA and persistent JADX caches;
- FTS dotted/literal fallback;
- named-class prevalidation, preserving `defpackage.*` pass-through;
- source claims closed against 11 dimensions;
- after a report is ready, use completion receipt rather than repeatedly rerunning deep diagnostics.

Closure dimensions:

1. entry points
2. base classes / inheritance
3. all DEX files
4. obfuscated collaborators
5. models / data flow
6. persistence
7. resources / XML
8. Android framework APIs
9. callbacks / observers
10. launch / actions / lifecycle
11. native / JNI

Final contract references from the original environment:

- contract v18 SHA: `052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`
- process bundle SHA: `251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

## Operational lessons from interruptions

- If an outer timeout occurs, inspect durable batch state and resume only unfinished work.
- Never discard completed evidence because the chat/tool call was interrupted.
- A completion interruption can be delivery/finalization-only; verify the durable report before rerunning analysis.
- Inspect the actual installed CLI before relying on documentation; one historical process note mentioned `--core` self-test, but the installed CLI at the time did not expose that option.
- Do not chain historical re-attestation or deep diagnostics after the current result is already marked ready.

## Preserved process notes

The older hardening reports are stored next to this file. They document the evolution toward the final process and are useful if a future agent needs to understand why these rules exist.


---

# SOURCE FILE: `audit_process_hardening_v3.md`

SHA-256: `2b8904130aa745aea6d643c36b82e333f05cf5d5264d5fcd3380184483585536`

---

# Launcher Project Audit Process Hardening v3

## Result
The apparent interruption during the Group 9 re-audit did not lose or corrupt Group 9 analysis. `AUDIT_09_R2` had already completed discovery, verification, synthesis, and report generation. The failure was orchestration residue around the completed run, not a failed reverse-engineering stage.

## Defects found
1. Atomic `.tmp` files left by an externally interrupted parent process remained in the evidence directory.
2. The old evidence snapshot code could include those `.tmp` files because it only excluded `.err` files.
3. The first shared Android class inventory used one `list_classes` page. JADX reported 7,395 classes but the page contained only the first 4,096.
4. New REA probes still paid a JVM/APK load cost on every uncached query.
5. Concurrent probes targeting the same output had no per-output lock.
6. Timeout cleanup did not explicitly terminate the complete child process group.
7. The FTS rebuild updated documents but did not prune deleted stale paths.
8. There was no single fail-closed preflight gate combining source hashes, DB integrity, evidence semantics, temp-artifact state, and stage order.

## Implemented fixes
### Clean canonical evidence manifests
- Temporary/partial/lock files are never canonical evidence.
- Snapshots include only semantically successful REA evidence.
- Snapshot verification checks SHA-256, size, semantic envelope, source-report hash, and target-APK hash.
- Group 9 snapshot regenerated: 53/53 valid files, zero temp entries.

### Deterministic cleanup and preflight
`auditctl.py` now supports:
- `cleanup`
- `health`
- `preflight`
- source-report and APK hash drift checking
- stage-order enforcement
- stale FTS document pruning
- WAL checkpointing after durable checkpoints

### Complete Android inventory
A persistent JADX MCP session now pages `list_classes` directly until `len(items) == total` and unique count matches total.

Verified inventory:
- expected: 7,395
- returned: 7,395
- unique: 7,395

The previous 4,096-entry file is explicitly retained only as a known-incomplete diagnostic artifact and is not used as a complete inventory.

### Persistent fast discovery layer
A local persistent JADX MCP service loads the APK once and stays resident.

Architecture:

```
APK
 ├─ Persistent JADX MCP (fast discovery / inventory / candidate inspection)
 └─ REA -> JADX (canonical verification evidence for high-risk claims)
```

This preserves REA provenance for conclusions while avoiding repeated 7–9 second JVM/APK startup costs during discovery.

A full 7,395-class paged inventory from the already-loaded service completed in ~0.084 s after startup.

### Safer REA probe execution
`rea_probe.py` now adds:
- per-output filesystem locking
- atomic final-file replacement
- semantic envelope validation
- exact-evidence cache keyed by operation + parameters + target APK SHA-256
- process-group termination on timeout
- bounded retries only for retryable failures
- stale temp cleanup

### FTS/index reliability
Index rebuild now:
- excludes temp/partial/lock files
- updates changed documents by SHA-256
- prunes records whose files no longer exist
- verifies SQLite integrity

## Current Group 9 state

```
AUDIT_09_R2
  discovery      complete
  verification   complete
  synthesis      complete
  report         complete

resume_from = none
canonical evidence = 53/53 valid
stale temp files = 0
DB integrity = ok
source report hash = ok
target APK hash = ok
```

The finished Group 9 v2 report remains authoritative; no Group 9 analysis needs to be repeated because of this interruption.


---

# SOURCE FILE: `audit_process_hardening_v4.md`

SHA-256: `0f721bb7432c753a087dbac9bb86fefd44dd0a1b68cabac27245787514db8197`

---

# Launcher / Projectivy Audit Process Hardening v4

## Purpose
This record consolidates the reliability and performance rules from the earlier Launcher/Projectivy reverse-engineering sessions and the latest interruption investigation. The process is intended to survive connection/session interruptions without losing verified work while keeping Projectivy analysis fast and evidence-backed.

## Current canonical toolchain
- Projectivy APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
- JADX MCP JAR SHA-256: `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`
- REA: `6.7.0`
- REA launcher SHA-256: `2f7ea4ae14f9d27fe47ac7595077bd3e8820665b8bdd87687b582a3b00ff4c40`
- Bundled Node: `v22.20.0`
- Bundled Node SHA-256: `b1cbec894e45a5814b6ab756e1e14f8a76516273197e67e0412b57c1e10d0d9f`
- JADX heap cap: `2048 MiB`
- Expected Projectivy classes: `7,395`
- Process contract: v6

## Earlier requirements retained
1. Work in small self-contained groups and stop/report after each group.
2. A group is not complete until its important dependency graph is closed.
3. Trace named Projectivy entry points into obfuscated R8/ProGuard collaborators.
4. Trace across all DEX files, resources, Android APIs, callbacks, persistence and JNI where relevant.
5. Preserve original `GROUP_xx` work separately from post-closure `AUDIT_xx` work.
6. A completed legacy report remains authoritative even if it predates the newer four-stage checkpoint schema.
7. Use deterministic stages: discovery -> verification -> synthesis -> report.
8. Checkpoint after every stage and preserve already-completed evidence after interruption.
9. Use later-report FTS delta search instead of rereading every later report.
10. Use raw REA/JADX verification only for uncertain/high-risk claims.

## Reliability fixes now enforced
- Exact source-report SHA drift detection.
- Exact target APK SHA drift detection.
- Exact JADX JAR SHA verification.
- REA launcher binary and bundled Node binary/version pinning.
- Process-tool bundle SHA pinning in completed evidence manifests.
- Atomic JSON/state/evidence/final-report writes.
- SQLite WAL + `PRAGMA integrity_check`.
- Ordered-stage enforcement and deterministic resume planning.
- Semantic REA success-envelope validation; shell exit code alone is insufficient.
- Bounded retry only for retryable failures.
- Adaptive bounded timeouts with process-group SIGTERM/SIGKILL cleanup.
- Per-output locking to prevent duplicate/racing probes.
- Complete paginated 7,395-class inventory; incomplete inventories fail closed.
- Smali fallback for decompilation failures.
- Persistent JADX discovery daemon with ownership/config/provenance checks.
- No aggressive parallel JADX JVMs.
- Canonical manifests contain SHA-256 + byte size + semantic-success status for every evidence file.
- Completed-manifest verification checks the current canonical evidence set exactly.
- Safe contract re-attestation: unchanged evidence is cryptographically revalidated, old manifests are preserved in provenance history, and only then is a new process-contract/tool-bundle attestation written.
- Failed/invalid exploratory probes are quarantined under `failed_probes/` with stderr and SHA provenance rather than left in canonical `evidence/` or deleted.
- Future bounded batches automatically quarantine failed probe output.
- Stale audit/shared transient workspaces are cleaned without touching canonical evidence.
- Persistent daemon log growth is bounded by rotation.
- Fail-closed `preflight` and full machine-readable `selftest`.

## Performance improvements now enforced
- Persistent JADX service loads Projectivy once instead of starting a JVM for every discovery query.
- Static semantic index contains 7,395 classes, 67,202 dependency edges, literals and resource references for cheap candidate discovery.
- Default-package names are normalized (`defpackage.yb3` -> `yb3`) for semantic dependency-graph queries.
- Exact JADX discovery cache keyed by target/tool hashes and arguments.
- Exact REA evidence cache keyed by target SHA, JADX SHA/version, REA version, heap and operation parameters.
- Existing verified evidence was backfilled into the current REA cache fingerprint format, including older canonical evidence that predated the cache database.
- Bounded resumable probe batches default to 6 and hard-cap at 12.
- FTS and semantic-index discovery are preferred before expensive canonical REA verification.

## Latest interruption diagnosis
The Group 9 analysis itself was not lost. `AUDIT_09_R2` had already completed discovery, verification, synthesis and report. The orchestration layer had several issues:

1. The process contract had advanced while completed manifests still carried the older contract hash/version.
2. Group 8 still had nine known failed/invalid exploratory outputs inside `evidence/` even though the canonical manifest correctly excluded them.
3. Semantic graph queries could silently miss default-package classes because REA used `defpackage.X` while the static index stores raw `X` names.
4. The hardened REA cache fingerprint changed, but older verified evidence had not all been backfilled to the new cache key.
5. Old semantic-build temporary directories remained in the shared workspace.

All five are now corrected.

## Current health
- Full process self-test: PASS
- SQLite integrity: PASS
- JADX daemon health/ownership/config: PASS
- Complete class inventory: 7,395 / 7,395 unique
- Shared stale transient count: 0
- `AUDIT_08_R2` preflight: PASS
  - canonical evidence: 80 / 80 semantic success
  - error/invalid canonical evidence: 0
  - snapshot verification: PASS
- `AUDIT_09_R2` preflight: PASS
  - canonical evidence: 53 / 53 semantic success
  - error/invalid canonical evidence: 0
  - snapshot verification: PASS

## Measured speed improvement
A previously uncached canonical REA class probe took about 8.4 seconds because REA/JADX had to initialize. After cache backfill, the same canonical evidence retrieval is approximately 0.7-0.8 seconds. Persistent JADX discovery calls are typically tens of milliseconds internally after service startup; command-wrapper overhead is roughly sub-second.

## Repository mutation rule
Before changing Aerial-Rows through the remote connector, capture a repository snapshot pinned to a full commit SHA and per-file blob SHAs. Compare against the previous snapshot and analyze only changed files when possible. Never assume an earlier checkout/tree still matches the live repository.

## Canonical next-step workflow
1. Run global self-test.
2. Initialize/resume one bounded `AUDIT_xx` namespace.
3. Fail-closed preflight.
4. Source hash check + FTS/semantic delta discovery.
5. Checkpoint discovery.
6. Run small resumable verification batches; REA is canonical for high-risk conclusions.
7. Quarantine failed probes, preserve failure ledger.
8. Checkpoint verification.
9. Synthesize against original + later-group findings.
10. Checkpoint synthesis.
11. Write final report atomically.
12. Create hashed canonical evidence snapshot.
13. Checkpoint report complete.
14. Run preflight and global self-test again.
15. Stop and report before starting the next group.


---

# SOURCE FILE: `audit_process_hardening_v5.md`

SHA-256: `814366522ea28f989a3db50fc8cae3c0621df887acd368424695656c1852c728`

---

# Launcher / Projectivy Audit Process Hardening v5

## Purpose
This revision reconciles the active audit process with every earlier reliability/performance rule recovered from the previous Projectivy/Launcher handoff and the v3/v4 hardening records. It preserves all earlier protections and adds machine-enforced subsystem closure plus stronger APK provenance.

## Canonical toolchain and target provenance
- Projectivy APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
- APK signing certificate SHA-256: `f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`
- DEX inventory: `classes.dex`, `classes2.dex`, `classes3.dex`
- ART baseline-profile inventory: `assets/dexopt/baseline.prof`, `assets/dexopt/baseline.profm`, `assets/dexopt/baseline.bin`
- JADX MCP JAR SHA-256: `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`
- REA: `6.7.0`
- Bundled Node: `v22.20.0`
- JADX heap cap: `2048 MiB`
- Expected Projectivy classes: `7,395`
- Process contract: v7

## Earlier requirements retained and enforced
1. Work in small self-contained groups and stop/report after each group.
2. Use small bounded JADX/REA batches rather than one huge invocation.
3. Preserve verified partial work and resume deterministically after interruption instead of restarting.
4. Keep original `GROUP_xx` analysis separate from post-closure `AUDIT_xx` analysis.
5. A completed legacy report remains authoritative even if it predates newer checkpoint schemas.
6. Use deterministic stages: `discovery -> verification -> synthesis -> report`, checkpointing every stage.
7. Trace named Projectivy entry points through obfuscated R8/ProGuard collaborators rather than stopping at readable package classes.
8. Trace all DEX files and include resources/XML, models/data flow, persistence, Android framework APIs, callbacks/observers, launch/lifecycle paths and native/JNI when relevant.
9. A group is not complete until its important dependency graph and subsystem closure dimensions are closed.
10. Use FTS/static semantic discovery first; reserve canonical REA/JADX verification for uncertain or high-risk claims.
11. Preserve exact APK/tool/runtime provenance and fail closed on drift.
12. Before mutating Aerial-Rows remotely, snapshot the full commit SHA plus per-file blob SHAs and analyze only changed files when possible.

## New v7 enforcement
New audits created under process contract v7 receive a closure matrix with these required dimensions:
- entry points
- base classes / inheritance
- all DEX files
- obfuscated collaborators
- models / data flow
- persistence
- resources / XML
- Android framework APIs
- callbacks / observers
- launch actions / lifecycle
- native / JNI

A v7 audit cannot checkpoint `report=complete` until every dimension is either:
- `covered` with at least one evidence reference, or
- `not_applicable` with a written reason.

Older completed audits are grandfathered under the prior `legacy completed report is authoritative` rule; they are not retroactively rewritten just to satisfy a newer schema.

## Reliability protections retained
- Source-report SHA drift detection.
- Target APK SHA drift detection.
- APK signer, multidex inventory and baseline-profile provenance checks.
- Exact JADX JAR, REA launcher and bundled Node binary/version pinning.
- Process-tool bundle hash pinning.
- Atomic state/evidence/final-report writes.
- SQLite WAL + integrity checks.
- Stage-order enforcement and deterministic resume.
- Semantic REA success validation rather than shell-exit-only checks.
- Adaptive bounded timeout backoff.
- SIGTERM/SIGKILL process-group cleanup on timeout.
- Bounded retry only for retryable failures.
- Per-output locks.
- Complete paginated 7,395-class inventory; incomplete inventory fails closed.
- Automatic/explicit Smali fallback.
- Persistent owned/config-checked JADX daemon.
- No aggressive parallel JADX JVMs.
- Hashed canonical evidence manifests with byte size and semantic-success validation.
- Exact canonical evidence-set verification.
- Safe contract re-attestation preserving previous manifests under provenance history.
- Failed/invalid probe quarantine with stderr and hashes.
- Automatic quarantine in bounded probe batches.
- Stale audit/shared transient cleanup.
- Bounded daemon-log rotation.
- Fail-closed per-audit preflight and global machine-readable self-test.

## Performance protections retained
- Persistent JADX loads the APK once for discovery.
- Static semantic index contains class edges, literals and resource references.
- Default-package normalization (`defpackage.X -> X`) for dependency graph queries.
- Exact JADX discovery cache keyed by target/tool hashes and arguments.
- Exact REA evidence cache keyed by target/tool/runtime/heap/operation parameters.
- Verified historical evidence backfilled into current cache-key format.
- Resumable sequential probe batches default to 6 and hard-cap at 12.
- FTS + semantic index are used before expensive raw verification.
- Repository changed-file analysis avoids rereading unchanged Aerial-Rows files.

## Current verification
- Process v7 self-test: PASS.
- APK signer/provenance: PASS.
- DEX inventory: 3 / 3 expected.
- Baseline profiles: all expected files present.
- Projectivy class inventory: 7,395 / 7,395 unique.
- SQLite integrity: PASS.
- Persistent JADX daemon ownership/config: PASS.
- AUDIT_08_R2 preflight: PASS; 80/80 canonical evidence files valid.
- AUDIT_09_R2 preflight: PASS; 53/53 canonical evidence files valid.
- Both old manifests were safely preserved before v7 re-attestation.
- Closure-matrix regression: a new v7 state with pending dimensions fails; all-covered state passes; legacy completed state remains grandfathered.
- Representative REA cache hit: ~0.76 s, versus ~8 s for an uncached JVM/APK load.

## Canonical workflow from now on
1. Global self-test.
2. Initialize/resume exactly one small `GROUP_xx` or `AUDIT_xx` unit.
3. Fail-closed preflight.
4. Verify source/APK/tool provenance.
5. FTS + semantic delta discovery.
6. Update closure matrix as dimensions are actually evidenced.
7. Checkpoint discovery.
8. Run small resumable verification batches; use REA canonically for high-risk conclusions and Smali fallback when decompilation is inadequate.
9. Quarantine failures and preserve failure ledger.
10. Checkpoint verification.
11. Synthesize against prior/later group findings.
12. Checkpoint synthesis.
13. Confirm closure matrix complete.
14. Write final report atomically.
15. Create and verify hashed canonical evidence snapshot.
16. Checkpoint report complete.
17. Run audit preflight and global self-test again.
18. Stop and report before starting the next group.


---

# SOURCE FILE: `audit_process_hardening_v6.md`

SHA-256: `8cc97d337676040ffe9008abfe1db588ac6fbccbdb76499d5788bf72298e822d`

---

# Launcher / Projectivy audit process hardening v6

Status: **ACTIVE PROCESS SPECIFICATION**  
Process contract: **v10**

## Purpose

This document consolidates the previous interruption/reliability fixes and the additional process improvements implemented before and during the Group 9 v3 re-audit.

The goal is unchanged: maximize reverse-engineering completeness while making every stage resumable, provenance-pinned, fail-closed, and efficient enough that repeated evidence is not recomputed unnecessarily.

## Core workflow retained from previous versions

```text
source/APK/toolchain provenance
        ↓
later-report FTS + semantic-index delta discovery
        ↓
small discovery snapshot
        ↓ CHECKPOINT
persistent JADX exploratory analysis
        ↓
small bounded canonical REA batches
        ↓
semantic success validation
   ├─ success → canonical evidence + cache
   └─ failure → quarantine + failure ledger
        ↓ CHECKPOINT
11-dimension closure matrix
        ↓
synthesis
        ↓ CHECKPOINT
atomic final report
        ↓
hashed evidence snapshot
        ↓
preflight + global selftest
```

## Reliability controls

- separate `GROUP_xx` and `AUDIT_xx` checkpoint namespaces;
- deterministic `discovery → verification → synthesis → report` resume;
- completed legacy reports remain authoritative;
- atomic state, evidence, manifest and final-report writes;
- SQLite WAL + FULL synchronous mode + integrity checks;
- exact original report SHA and target APK SHA drift detection;
- APK signer, multidex inventory and baseline-profile provenance;
- pinned REA launcher, bundled Node, JADX JAR, heap policy and process-tool bundle hashes;
- semantic REA-envelope validation, not exit-code/stderr-only validation;
- adaptive bounded timeout, retryable-only retry, process-group cleanup;
- success-only canonical evidence directory;
- failed/invalid probe quarantine with forensic preservation;
- stale temp/shared-workspace cleanup;
- exact evidence cache keyed by operation/parameters/APK/JADX/REA/heap, not by harmless orchestration changes;
- persistent JADX daemon with ownership/config checks and bounded logs;
- complete paginated 7,395-class inventory;
- static semantic index and dependency graph;
- Smali fallback when Java decompilation is unavailable/unreliable;
- remote repository snapshots pinned to full commit SHA + per-file blob SHAs before mutation;
- no aggressive parallel JADX JVMs.

## Machine-enforced subsystem closure

New audits cannot complete unless all dimensions are either `covered` with evidence or explicitly `not_applicable` with a reason:

1. entry points;
2. base classes / inheritance;
3. all DEX files;
4. obfuscated collaborators;
5. models / data flow;
6. persistence;
7. resources / XML;
8. Android framework APIs;
9. callbacks / observers;
10. launch actions / lifecycle;
11. native / JNI.

Closure enforcement was introduced in contract v7 and remains active for all later contracts. The invariant now correctly requires the introduction version to be <= the current contract version rather than equal to it.

## Improvements added in this Group 9 cycle

### 1. Persistent MCP session reuse inside discovery batches

The persistent JADX daemon already avoided APK reloads, but each discovery CLI process still performed its own MCP initialization.

`jadx_fast.py` now reuses one MCP session inside a process and `audit_batch.py` can run JADX discovery in-process for a bounded batch.

Representative forced two-query benchmark:

```text
old separate-process path: ~1.54 s
new in-process/session path: ~0.81 s
```

The old isolated subprocess path remains an automatic fallback if the fast path fails.

### 2. Process-contract versioning bug fixed

The closure self-test previously assumed the current contract version had to equal the version that introduced closure enforcement. That made any later process upgrade appear invalid.

The invariant is now:

```text
1 <= closure_enforcement_from_contract_version <= current_contract_version
```

while audit creation version still determines whether the closure matrix applies.

### 3. Safe literal FTS fallback

FTS5 treats punctuation such as dots in package/property names as query syntax. Queries such as:

```text
com.android.vending
sys.cts.running
com.xiaomi.mitv.upgrade
```

could therefore throw a SQLite syntax error.

`auditctl search` now:

1. tries the supplied FTS expression unchanged so `AND`/`OR` and quoted expressions still work;
2. on FTS syntax failure, retries the entire input as a safely quoted literal phrase.

Search remains read-only.

### 4. REA named-class target prevalidation

A misspelled named class previously spent a full REA/JADX startup before returning `invalid_request`.

Canonical batches now prevalidate non-`defpackage` exact class names against the pinned complete 7,395-class inventory before JVM startup.

Default-package helpers deliberately pass through because JADX can expose synthetic/folded helpers to class/method lookup even when `list_classes` omits them.

This would now reject a typo such as:

```text
com.spocky.projengmenu.receivers.StartUpBootReceiver
```

immediately, while accepting the real:

```text
com.spocky.projengmenu.services.StartUpBootReceiver
```

without weakening support for helpers such as `defpackage.cm3`.

### 5. Cache behavior verified after orchestration upgrades

REA cache fingerprints depend only on analysis-affecting state:

```text
operation + params + APK SHA + JADX SHA/version + REA version + heap
```

They do not depend on the overall process-bundle hash, so changes to checkpoint/search/batch orchestration do not invalidate valid decompilation evidence.

Representative canonical cache hit after the v10 changes:

```text
~0.68 s
```

versus roughly 7–9 seconds for a fresh REA/JADX load.

## Current integrity state

At the time of this document:

```text
Process contract                  v10
Global self-test                  PASS
SQLite integrity                  PASS
Persistent JADX daemon            PASS
Complete class inventory          7,395 / 7,395 unique
Stale temp files                  0
Shared transient workspaces       0

AUDIT_08_R2 snapshot              PASS (80 evidence)
AUDIT_09_R2 snapshot              PASS (53 evidence)
AUDIT_09_R3 snapshot              PASS (19 evidence)
AUDIT_09_R3 closure               PASS (11/11)
```

Process bundle SHA-256:

`e949e7a3d252ad0688a925aaae97f709771245307afb40de0a9cf5c7dfb30ca1`

## Operational rule going forward

Discovery should use, in order:

```text
existing reports / evidence
    ↓
FTS + static semantic index
    ↓
persistent JADX batch discovery
    ↓
REA only for claims that need canonical verification
```

This minimizes repeated JVM/APK work without lowering the evidence standard.


---

# SOURCE FILE: `audit_process_hardening_v8.md`

SHA-256: `2f648cf10e0fec1d0e15abadba7b150bb9a03762b6d9a68144f4b86d79e4f0c1`

---

# Audit Process Hardening v8 — Connection-Interruption / Last-Mile Recovery

## Failure diagnosis

The interrupted Group 10 run did **not** lose or corrupt reverse-engineering work. Durable state proves that `AUDIT_10_R2` had already completed discovery, verification, synthesis and report before the visible conversation stopped:

- `resume_from = none`;
- 23/23 canonical REA evidence files validate successfully;
- all 11 closure dimensions are covered or explicitly not applicable;
- the final Group 10 v2 report exists with a matching SHA-256 sidecar;
- the evidence snapshot passes;
- no stale audit temp files or orphaned REA/JADX analysis workers remain.

The exact browser/platform transport cause is not observable from the analysis container. The durable timeline therefore supports a **last-mile chat/response interruption after analytical completion**, not an evidence or checkpoint failure.

## Secondary orchestration weakness found

The old last-mile path kept doing expensive work after the report was already durable:

1. snapshot verification;
2. preflight (which verified the same snapshot again);
3. a full global self-test;
4. verification of every historical completed audit;
5. serialization of the full APK resource inventory in diagnostic output.

That could add 15+ seconds of duplicated work after the result was already ready, which is exactly the wrong place to risk a chat interruption.

A separate diagnostic command also combined several historical re-attestations and a full self-test into one outer shell invocation. Individual operations were healthy, but the combined command exceeded the outer execution window.

**Rule:** critical handoff operations are now individually bounded and are never chained behind historical maintenance or verbose diagnostics.

## Process contract v18

The process now has an explicit durable completion boundary plus an O(1)-style recovery path.

New/strengthened policies include:

- completion receipt is the canonical user-handoff record;
- finalisation runs immediately after report completion;
- current audit snapshot is verified once, inside preflight;
- compact **core** self-test is used during handoff;
- full/deep self-test is separate maintenance and may verify all completed audits;
- self-test still writes full diagnostics to disk but can print a compact summary;
- batch state is written **before** entering an expensive REA/JADX call;
- an interrupted batch therefore records the exact `in_progress` item, attempt count and recovery count;
- a fast `ready` command validates an existing completion receipt and skips expensive rechecks when nothing changed;
- receipt validity is tied to report/state/manifest hashes, process contract SHA/version, process-tool bundle SHA and target APK SHA.

## Write-ahead batch recovery

Before every expensive probe the batch runner atomically records:

- item id;
- `status = in_progress`;
- output path;
- start time;
- attempt count;
- recovered-interruption count.

A regression test simulated a killed probe by leaving an item `in_progress`. The next run recovered it correctly:

    attempts                         2
    recovered_interrupted_attempts  1
    final status                     complete

The retry completed from the persistent JADX cache.

## Compact finalisation

Canonical command:

    python3 auditctl.py finalize AUDIT_NAME --report /path/to/report.md --process-doc /path/to/process.md

It refuses to create a handoff receipt unless:

1. report stage is complete and `resume_from=none`;
2. report SHA-256 sidecar matches the report bytes;
3. current audit preflight passes;
4. the completed evidence snapshot passes inside preflight;
5. compact core runtime/toolchain self-test passes;
6. optional process-document SHA sidecar matches.

It atomically writes:

- `audits/<AUDIT>/completion_receipt.json`;
- `audit_system/last_completion_receipt.json`.

Measured Group 10 v18 finalisation: approximately **8.07 seconds** total.

## Fast interruption recovery

First command after any suspected interruption:

    python3 auditctl.py ready AUDIT_NAME

It validates:

- completion receipt exists and is marked ready;
- report hash;
- state hash;
- evidence-manifest hash;
- current process contract version and SHA;
- current process-tool bundle SHA;
- target APK SHA.

If all match, the result is safe to hand to the user immediately without repeating preflight or self-tests.

Measured Group 10 recovery: approximately **0.79 seconds**.

If the receipt is stale or absent, normal deterministic recovery applies: inspect `state.json` / `resume_from` and continue only the unfinished stage/item.

## Deep maintenance remains available

A full self-test still verifies all completed audit snapshots and remains mandatory after process/tool changes before those snapshots are re-attested.

Current full/deep self-test time: approximately **10.49 seconds** for four completed audits.

Current core handoff self-test time: approximately **3.8 seconds**.

## Current stable health

    Process contract          v18
    Full/deep self-test       PASS
    SQLite integrity          PASS
    Persistent JADX           PASS
    Class inventory           7,395 / 7,395
    APK/resource provenance   PASS
    Stale audit temps         0
    Shared transients         0
    Completed snapshots       4 / 4 PASS

Current process contract SHA-256:

`052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`

Current process-tool bundle SHA-256:

`251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

## Group 10 state

`AUDIT_10_R2` remains complete and does not need to be rerun:

- 23/23 canonical evidence files successful;
- 0 canonical errors/invalid records;
- 11/11 closure dimensions closed;
- report snapshot verified;
- final report SHA-256: `d75640cc4a23d2a7d392ee3322a50d59c942fad9c09094b4e38a58c130d2c126`.
