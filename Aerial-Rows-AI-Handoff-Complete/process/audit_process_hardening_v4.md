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
