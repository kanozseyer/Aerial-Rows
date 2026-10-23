# Reverse-Engineering Process State / Recovery Notes

This file preserves the final operational state of the Projectivy research in case APK analysis ever has to be reopened. Normal Aerial-Rows implementation does **not** require repeating this work.

## Target

- Projectivy Launcher 4.72 / code 97
- package `com.spocky.projengmenu`
- APK SHA256 `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
- certificate SHA256 `f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`
- minSdk 23 / targetSdk 37
- 7395 classes
- 1171 resource entries
- 3 DEX files
- `libptutils.so` in four common Android ABIs

## Toolchain used

- JADX headless MCP 0.7.1 all-in-one JAR
- REA 6.7.0
- Java 21
- Node 22.20.0
- persistent JADX MCP daemon

Historical paths:

```text
/mnt/data/rea-portable
/mnt/data/rea
/mnt/data/launcherproject_work
```

These are provenance only.

## Why the process was hardened

Early analysis attempts showed predictable failure modes: long JVM startup, resource-lite queries being mistaken for complete inventory, outer tool timeouts after useful work had already completed, repeated expensive verification after a result was already durable, and FTS/package-name syntax edge cases. The audit harness was hardened so connection/chat interruptions could not destroy progress or force full reruns.

## Final operational rules

1. **Semantic-first discovery.** Search reports/evidence/index first; do not blindly probe named classes from assumptions.
2. **Use persistent daemon.** Avoid repeated full JADX JVM startup.
3. **Full class inventory is separately pinned.** 7395 classes/7395 unique at final health check.
4. **Full resource inventory is separately pinned.** Do not claim resource closure from lightweight resources mode.
5. **REA/JADX canonical evidence for high-risk claims.** Discovery can use cheaper semantic index; final claims needing decompilation are verified canonically.
6. **No empty REA query as exhaustive search.** Empty query behavior is not proof of absence.
7. **No aggressive parallel JVMs.** They increase contention/timeouts without improving evidence quality.
8. **Bounded/resumable batches.** Default batch 6, maximum 12 in final process.
9. **Write-ahead probe checkpoint.** Before entering slow JVM query, atomically record exact item as `in_progress` with output path/start time/attempt count.
10. **Interrupted attempt recovery.** On resume, recover only the interrupted/unfinished item; don't throw away successful evidence.
11. **Atomic state/evidence/report writes.** Partial output cannot masquerade as canonical success.
12. **Failed probes quarantined.** Errors/invalid records never enter success-only canonical evidence set.
13. **Exact caches.** REA cache fingerprints depend on operation/params/APK/JADX/REA/heap, not on orchestration bundle hash, so process-only upgrades preserve valid decompilation cache.
14. **FTS dotted/literal fallback.** Package/property names such as `com.android.vending` and `sys.cts.running` can break raw FTS syntax; retry input as safely quoted literal phrase after syntax error while preserving normal AND/OR expression behavior when valid.
15. **Named class prevalidation.** Non-`defpackage` exact class names are checked against pinned class inventory before JVM startup. Default-package helpers pass through because JADX may expose synthetic/folded helpers absent from list_classes.
16. **Actual CLI beats process docs.** A historical note mentioned a `--core` self-test option before the installed CLI actually exposed it; inspect installed tool behavior before trusting documentation.
17. **Closure matrix required.** Final audit dimensions: entry points; inheritance; all DEX; obfuscated collaborators; models/data flow; persistence; resources/XML; framework APIs; callbacks; launch/lifecycle; native/JNI.
18. **Stop/report each group.** Small durable groups are intentional.
19. **Completion receipt is handoff boundary.** Once report/evidence/state hashes verify, create durable receipt immediately.
20. **Fast interruption recovery.** First action after suspected transport interruption is receipt/hash readiness check; do not rerun deep diagnostics if result is already durable.
21. **Deep historical re-attestation is maintenance, not handoff.** Do not chain all historical audit checks behind the current result before replying.

## Final contract references

Process contract v18 SHA256:

`052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`

Process/tool bundle SHA256:

`251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

## Closure dimensions

```text
1  entry_points
2  base_classes_inheritance
3  all_dex_files
4  obfuscated_collaborators
5  models_data_flow
6  persistence
7  resources_xml
8  android_framework_apis
9  callbacks_observers
10 launch_actions_lifecycle
11 native_jni
```

## Last-mile interruption lesson

One Group 10 run looked interrupted to the user, but durable state later showed the analysis/report had already completed. The interruption was delivery/finalization-only, not evidence loss. The old process was doing redundant work after report durability: snapshot verification, preflight that reverified snapshot, global self-test, historical completed-audit checks and verbose resource inventory output. Final v18 moved durable completion earlier and added fast readiness check.

A simulated killed probe left an `in_progress` batch item; next run recovered it correctly with attempts incremented and recovered-interruption counter recorded, using persistent JADX cache. This is the desired recovery model.

## Operational recovery sequence if research is ever reopened

1. Verify target APK hash matches the audited target.
2. Verify current research/report state before launching any new JVM work.
3. If current requested claim is already in reports/evidence, use it.
4. If a previous run appears interrupted, inspect durable state/receipt and resume only unfinished item/stage.
5. Use semantic index/FTS/persistent JADX for discovery.
6. Use REA only for claims requiring canonical verification.
7. Update report and evidence atomically.
8. Stop and checkpoint rather than chaining unrelated groups.

The implementation project should not depend on the old RE environment. The durable architectural/behavioral conclusions are in `PROJECTIVY_DETAILED_FINDINGS.md`, `COUCHY_DETAILED_FINDINGS.md`, and `C12_FINAL_BLUEPRINT.md`.
