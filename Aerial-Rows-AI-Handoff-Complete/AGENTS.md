# AGENTS.md — Aerial-Rows

Before making architectural or feature changes, read:

1. `AI_PROJECT_CONTEXT.md`
2. `docs/ai-handoff/C12_FINAL_BLUEPRINT.md`
3. the relevant C1–C11 section in `docs/ai-handoff/COUCHY_C1_C12_FULL.md`
4. the matching Projectivy behavioral audit in `docs/ai-handoff/PROJECTIVY_G1_G12_FULL.md` when needed.

The project is a Couchy-derived Compose Android TV launcher. Preserve Couchy's lightweight hot paths and independently reimplement useful Projectivy behavior behind clean interfaces.

Immediate implementation phase at the end of the research handoff: **Phase 0A — materialize the Gradle-injected source changes and remove `applyAerialRowsSourcePatches` without changing behavior.**

Important rules:

- Do not copy proprietary Projectivy decompiled source or licensing/protection code.
- Do not upload proprietary Projectivy APK/JADX/REA binaries to the repository.
- Build must not mutate tracked source after Phase 0A.
- Core launcher functionality must not require Accessibility, vendor hacks, or root.
- All launcher-originated launches should converge on `LaunchDispatcher -> LaunchPolicy` once Phase 1 lands.
- Use stable semantic IDs, not list indices or labels.
- Preserve bounded app scanning, artwork caching/object reuse, SurfaceView wallpaper, small Media3 buffers, decoder release on stop, and other performance invariants documented in C11.
- Optional feature disabled should mean its expensive background work is disabled.
- Work in bounded commits and keep the handoff updated when major decisions change.
