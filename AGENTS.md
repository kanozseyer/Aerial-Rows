# AGENTS.md — Aerial-Rows

This repository contains a durable AI handoff for the Launcher project. Before making architectural or feature changes, read in this order:

1. `AI_PROJECT_CONTEXT.md`
2. `docs/ai-handoff/HANDOFF_MANIFEST.json`
3. `docs/ai-handoff/C12_FINAL_BLUEPRINT.md`
4. `docs/ai-handoff/CHAT_EXPORT.md`
5. `docs/ai-handoff/EXPORT_STATUS.md`
6. `docs/ai-handoff/raw/ARCHIVE_MANIFEST.json`
7. `docs/ai-handoff/COUCHY_DETAILED_FINDINGS.md`
8. `docs/ai-handoff/PROJECTIVY_DETAILED_FINDINGS.md`
9. `docs/ai-handoff/RESEARCH_INDEX.md`
10. `docs/ai-handoff/PROCESS_STATE.md` if reopening APK/reverse-engineering work.

The project is a Couchy-derived Compose Android TV launcher. Preserve Couchy's lightweight hot paths and independently reimplement useful verified Projectivy behavior behind clean interfaces.

Immediate implementation phase at the end of research: **Phase 0A — materialize the Gradle-injected source changes and remove `applyAerialRowsSourcePatches` without changing behavior.** Research C1–C12 is complete; do not restart it unless a genuinely new unknown blocks implementation.

However, the documentation export is still being completed. `EXPORT_STATUS.md` is authoritative for whether to continue exporting or start implementation. If the user has not explicitly paused the export, continue from its first pending subgroup.

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
- For raw handoff exports, never trust prose completion alone: run `python docs/ai-handoff/tools/validate_archive.py`.
