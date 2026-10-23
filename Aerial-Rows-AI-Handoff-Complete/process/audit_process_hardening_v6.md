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
