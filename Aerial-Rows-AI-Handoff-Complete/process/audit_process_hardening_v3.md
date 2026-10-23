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
