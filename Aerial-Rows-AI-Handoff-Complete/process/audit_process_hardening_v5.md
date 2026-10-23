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
