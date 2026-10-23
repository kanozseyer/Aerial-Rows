# AGENTS.md — Aerial-Rows

This repository contains a durable AI handoff for the Launcher project. Before making architectural or feature changes, read in this order:

1. `AI_PROJECT_CONTEXT.md`
2. `docs/ai-handoff/C12_FINAL_BLUEPRINT.md`
3. `docs/ai-handoff/CHAT_EXPORT.md`
4. `docs/ai-handoff/CHAT_EXPORT_APPENDIX_EXPORT_PROCESS.md`
5. `docs/ai-handoff/EXPORT_STATUS.md`
6. `docs/ai-handoff/raw/SOURCE_CATALOG.json`
7. `docs/ai-handoff/raw/ARCHIVE_MANIFEST.json`
8. `docs/ai-handoff/COUCHY_DETAILED_FINDINGS.md`
9. `docs/ai-handoff/PROJECTIVY_DETAILED_FINDINGS.md`
10. `docs/ai-handoff/RESEARCH_INDEX.md`
11. `docs/ai-handoff/PROCESS_STATE.md` if reopening APK/reverse-engineering work.

The project is a Couchy-derived Compose Android TV launcher. Preserve Couchy's lightweight hot paths and independently reimplement useful verified Projectivy behavior behind clean interfaces.

The C1–C12 research programme is complete. The raw handoff export is still in progress; machine state in `HANDOFF_MANIFEST.json`, `SOURCE_CATALOG.json` and `ARCHIVE_MANIFEST.json` is authoritative over prose. Run `python docs/ai-handoff/tools/validate_archive.py` before resuming export work.

Immediate implementation phase after the export is completed/paused explicitly: **Phase 0A — materialize the Gradle-injected source changes and remove `applyAerialRowsSourcePatches` without changing behavior.** Do not restart C1–C12 unless a genuinely new unknown blocks implementation.

Rules:

- Do not copy proprietary Projectivy decompiled source or licensing/protection code.
- Do not upload the proprietary Projectivy APK/JADX/REA binaries to this repository.
- Build must not mutate tracked source after Phase 0A.
- Core launcher functionality must not require Accessibility, vendor hacks, or root.
- Once Phase 1 lands, all launcher-originated launches converge on `LaunchDispatcher -> LaunchPolicy`.
- Use stable semantic IDs, not list indices or labels.
- Preserve bounded app scanning, artwork cache/object reuse, `prepareToDraw()`, SurfaceView wallpaper, small Media3 buffers, decoder release on stop, pseudo-blur and release shrinking/minification.
- Optional feature disabled should mean its expensive background work is disabled.
- Platform observation belongs in repositories/callbacks, not long-lived Composable-owned queries.
- Work in bounded commits; verify source/build behavior after each architecture change; update the handoff when its current phase/provenance changes.

Raw-export reliability rules:

- Expected corpus comes from `SOURCE_CATALOG.json`, not memory/prose.
- Package a raw report only if its source SHA matches the catalog.
- Publish a subgroup atomically using Git blobs/tree/commit + compare-and-swap branch-ref lease.
- Never force over unknown concurrent branch work.
- A report is complete only when repository validation succeeds.
