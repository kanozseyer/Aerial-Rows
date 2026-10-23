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
