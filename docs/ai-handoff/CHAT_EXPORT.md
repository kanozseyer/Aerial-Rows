# Launcher Project — Reconstructed Chat Export / Continuation Record

**Purpose:** This is a durable reconstruction of the Launcher/Aerial-Rows project conversation up to the completion of C12 on 2026-10-23. It is not a verbatim transcript. It preserves the decisions, discoveries, corrections, user intent, workflow expectations, file/tool provenance, and implementation direction needed to continue with equivalent context.

For detailed subsystem evidence, use the complete Couchy and Projectivy archives stored next to this file.

---

# 1. Original project intent

The user wanted to create a better Android TV launcher by starting from an existing open-source launcher and adding the best Projectivy-style behavior plus Aerial video wallpapers.

The project evolved through several ideas:

1. Customize an existing Shield/Android TV launcher.
2. Consider cloning/forking Projectivy directly.
3. Recognize that Projectivy is proprietary and use reverse engineering only to understand behavior/architecture.
4. Consider Couchy Launcher as the clean open-source base.
5. Fork Couchy into `kanozseyer/Aerial-Rows`.
6. Add Projectivy-like features on top while retaining Couchy's modern Compose/performance qualities.
7. Reverse engineer Projectivy comprehensively enough to know what behavior to reimplement independently.
8. Compare Couchy subsystem-by-subsystem against the Projectivy audit.
9. Freeze a final architecture/implementation plan.

The user explicitly asked for a **streamlined settings UI**, clear switches, dropdown-like selectors, persistent focus/cursor position after changing a setting, and a left navigation rail that behaves like Projectivy: the rail should only appear/reveal when navigating left from the home rows.

Desired features accumulated over the project included long-press app actions/uninstall, Android TV Channels/Watch Next, Projectivy-style left rail, per-row customization/reordering, richer settings, Aerial video backgrounds, notifications/Now Playing, TV inputs/HDMI, startup/autostart/remotes, backup/theme/icon/shortcut systems and optional advanced device features. The user values **performance and polish**, not just feature parity.

# 2. Repository selection and early Aerial-Rows state

The user forked Couchy and created/named the repo **Aerial-Rows**. JVM 21 was required in the user's Android Studio environment. The project was built and run as the working base. The final research baseline was `aerialrows-development` commit `3c1cfd56234f1d386f4553dd6c69523670500d70`.

The branch already included a fourth Sliding Rows mode, `SlidingRows.kt`, Android TV Preview Channels/Watch Next, RowQuickEditor, Aerial/video wallpaper support, panel appearance settings and multi-source Aerial work. However several changes were not normal checked-in behavior: they were injected by `applyAerialRowsSourcePatches` in `app/build.gradle.kts`. This became the highest-priority cleanup item.

# 3. Projectivy reverse-engineering decision

The user had an REA fork at `https://github.com/kanozseyer/rea-fork` and asked whether it could help reverse engineer Projectivy. The Projectivy target APK was version 4.72 / code 97 and a JADX MCP all-in-one JAR was supplied. The project eventually established a working REA/JADX environment and performed a comprehensive grouped audit.

Clean-room boundary: study proprietary APK behavior/architecture; do not paste wholesale decompiled source into Aerial-Rows; do not bypass premium/licensing; independently reimplement desired behavior.

# 4. Reverse-engineering provenance

Projectivy target: package `com.spocky.projengmenu`, version 4.72/code 97, minSdk 23, targetSdk 37, 7395 classes, 1171 resource entries, 3 DEX files, `libptutils.so` on the four common ABIs, MainActivity `com.spocky.projengmenu.ui.home.MainActivity`.

APK SHA256 `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`; signing certificate SHA256 `f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`.

Toolchain: JADX headless MCP 0.7.1, REA 6.7.0, Java 21, Node 22.20.0, persistent JADX daemon. Historical paths: `/mnt/data/rea-portable`, `/mnt/data/rea`, `/mnt/data/launcherproject_work`.

# 5. Audit process evolution

The audit was split into small groups to reduce mistakes/timeouts. Final process principles: semantic-first search, canonical REA/JADX evidence for high-risk claims, persistent daemon, complete class/resource inventories, bounded resumable batches, write-ahead `in_progress` checkpoint before slow probes, interrupted-attempt recovery, atomic report/state writes, failed-probe quarantine, exact cache behavior, FTS dotted/literal fallback, named-class prevalidation, no empty-query REA as exhaustive search, no aggressive parallel JVMs, resume only unfinished work after timeout, closure matrix across 11 dimensions, stop/report after each group, and completion receipts to avoid repeating already-complete analysis.

Closure dimensions: entry points; base classes/inheritance; all DEX files; obfuscated collaborators; models/data flow; persistence; resources/XML; Android framework APIs; callbacks/observers; launch/actions/lifecycle; native/JNI.

Final process refs: contract v18 SHA `052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`; process bundle SHA `251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`.

# 6. Projectivy findings summary

Home: three rail/header states (shown, revealable hidden, disabled); stable category/card focus restoration; configured empty rows use placeholder action cards; rich per-row presentation; row/card reorder refocuses by stable IDs; selected-card details surface.

Apps/actions: stable app/category context, per-row/global visibility semantics, richer action architecture, launch abstraction rather than direct UI startActivity.

TV Provider: discover non-browsable Preview Channels, request browsability, respect provider ordering, more deliberate Watch Next behavior, separate provider content from physical inputs.

Wallpaper: provider abstraction, semantic events, rotation scheduler, cache invalidation, image format policy, retry/fallback, tunneling/decoder config, disable audio track when muted, optional dynamic color, external providers. Couchy's actual player remained preferable.

Settings: strongest reusable idea was metadata describing settings—stable IDs, validation, capability/visibility, update policy, migration aliases, export policy, focus identity—not Projectivy's preference storage.

Media/notifications: NotificationListener is a platform bridge; notification state and MediaSession state are separate. Now Playing comes from MediaSessions.

TV inputs: useful source mapping/launch architecture but weaknesses included no repository-level TvInputManager callback, visibility/profile coupling, current-state divergence across strategies, Accessibility-owned display polling and dangling profile bindings.

Startup/remotes: shared startup dispatcher, duplicate suppression (~15s observed), boot validity (~90s observed), last-used input, global long/double key state machine (~300/400ms observed), temporary hotkey mode (~3s), direct HDMI key codes observed (243–246), short idle restore delay (~300ms). These timings are references, not requirements.

Persistence/themes/icons/shortcuts: migrations, safety state, backup vs theme, icon pack cache and shortcut hosting were valuable references; do not copy weak crypto/magic export naming.

Native/obfuscated closure: no hidden architectural engine behind `libptutils.so`; no need for Aerial-Rows native bootstrap.

# 7. Couchy comparison programme C1–C12

C1 Build/manifest/lifecycle skeleton. C2 home. C3 apps/categories/actions. C4 Android TV Channels/Watch Next. C5 wallpaper/Aerial. C6 settings. C7 status/media/notifications. C8 physical TV inputs/device integration. C9 HOME/startup/remotes/Accessibility/idle. C10 persistence/backups/themes/icons/shortcuts/parental. C11 performance/reliability. C12 final synthesis. All are complete and reviewed.

# 8. C1 conclusions

Couchy's manifest/component graph is much smaller/cleaner than Projectivy. MainActivity is intentionally small and should stay small. `LauncherConfig` JSON in Preferences DataStore is a better persistence base than Projectivy's many preferences. `AppRepository` is a major strength. `Actions` is useful but future launches should converge on `LaunchDispatcher + LaunchPolicy`.

# 9. C2 conclusions — home

Effective architecture: `MainActivity -> LauncherApp -> config/app scan -> categorized rows -> shell/status/wallpaper -> layout`. Raw checked-in LauncherScreen handles Carousel/Grid/Dock; Sliding Rows is integrated partly by build-time patch.

`LauncherApp` owns too much and should later be decomposed. Current Sliding Rows row model is private and covers Watch Next, channel, category only. Target generic row/item model is needed. Stable row IDs already exist, but per-row card focus identity does not. Need `HomeFocusState(activeRowId, activeItemId, lastItemByRow)`. Rail behavior needs explicit reveal/focus ownership. Rail reorder should key by row ID. Reuse Couchy's existing BringIntoView pivot geometry. User-configurable empty rows should remain visible with placeholder action. Add per-row presentation overrides. Keep AppCard; add shared selected-details later.

# 10. C3 conclusions — apps/categories/actions

Keep Couchy's app scan/render pipeline. Bugs found: package-change can leave stale app when launcher activity disappears; `movePkg` loses row context and duplicates can share move focus; zero categories is reachable; package-only identity can launch a different activity than the one represented; hide semantics are global only. Use LauncherApps callback, `MovingItem(rowId,itemId)`, category invariant, component-aware AppId, row-local exclusions + explicit Hide Everywhere, explicit sort/new-install policies and generic HomeItem action provider.

# 11. C4 conclusions — TV Provider

Current implementation is a good minimal reader but incomplete: non-browsable channels filtered before settings; zero-program channels disappear; no explicit Preview/Watch Next sort; no per-provider Watch Next cap/exclusion; channel key uses package+numeric ID only; no observer/resume refresh; provider intents launch directly. Target separate channel/preview/watch-next/artwork repositories with logical IDs, observer lifecycle and policy-constrained launching.

# 12. C5 conclusions — wallpaper/Aerial

Couchy's renderer is one of the strongest components: SurfaceView, Media3, decoder release on stop, small buffer, first-frame gating, transitions, gradient cover, local-media philosophy, image downsampling, pseudo-blur. Remove trust-all TLS/hostname verifier. Materialize `builtinSources`; old `builtinSource` becomes migration-only. Preserve Aerial metadata as typed items. Add engine/provider separation, disable audio track when muted, shuffle bag, failure quarantine, rotation scheduler and robust persistent-URI handling.

# 13. C6 conclusions — settings

Couchy's UI is good but model is implicit: every row directly writes config/effects. Add SettingId/Descriptor/Controller/Effect policy while retaining typed config and Compose. Issues: three control/focus families, no stable child-page return focus, full-config writes on rapid adjustment, backup save API/reporting bug, hardcoded English extras, scattered validation. Use route stack + last focused SettingId; small ranges keep Left/Right, named choices open choice panel.

# 14. C7 conclusions — status/media/notifications

No notification/media subsystem exists at baseline. Keep current status bar/hidden focus strip. Add separate NotificationRepository and MediaSessionRepository. Now Playing derives from MediaSessions, not notification text. Use platform MediaSession APIs. Media launches go through LaunchPolicy. Network state should separate transport from validated Internet; clock/date should share repository/ticker.

# 15. C8 conclusions — physical TV inputs

No physical TV input subsystem exists. Use TvInputManager + repository callback + resume refresh. Keep discovered/available/visible shortcuts distinct. Use fullscreen TvInputActivity/TvView for standard TIF. Input launch goes through InputLaunchCoordinator + LaunchPolicy. Distinguish requested/launched/confirmed input state. Vendor adapters/display profiles are optional and not Accessibility-owned.

# 16. C9 conclusions — HOME/startup/remote/idle

Couchy's small lifecycle is a strength. Keep normal HOME role, singleTask MainActivity, root Back behavior, RoleManager setup and UI-local keys. Add HomeRoleCoordinator and StartupCoordinator separately. No boot receiver merely to launch HOME. RemoteGestureEngine handles programmable global gestures; D-pad/AppCard long-press/move remain local. Accessibility stays optional/narrow. Idle reuses shared wallpaper renderer. Manual Google TV ADB takeover remains expert guidance with rollback.

# 17. C10 conclusions — persistence/backups/themes/icons/shortcuts/parental

Critical ConfigStore bugs: localized defaults can become English on first update; malformed JSON silently becomes defaults and can overwrite original state. Current Save Config is not full backup. Photo asset missing; local video URI is device-local. Need runtime config vs public backup DTO, full backup vs theme separation, schema/migrations/last-known-good. Existing artwork cache stays cache-only. Icon packs/shortcuts are absent and can be designed cleanly. Parental controls are optional through LaunchPolicy, not Accessibility.

# 18. C11 conclusions — performance/reliability

Preserve bounded scan, immutable reuse, same-list reuse, pre-rasterized artwork, prepareToDraw, stable keys, SurfaceView, small Media3 buffer, decoder release, pseudo-blur, shrink/minify and low service count. Build patch rewrites huge files every preBuild. AppRepository memory reuse ignores rendered artwork size/density variant—fix with ArtworkVariant. Cold startup waits for full app scan/artwork; measure before redesign. All decoded app art remains resident; measure memory before bounded LRU. No tests/benchmarks exist; add them before feature transplant. Disabled optional features must do no expensive background work.

# 19. C12 synthesis

Final conclusion: **Couchy is the better base implementation; Projectivy is the better behavioral reference.** Do not restart research. Begin source-purity and performance baseline.

# 20. Current branch/file state

At the final research baseline `app/build.gradle.kts` still contains `applyAerialRowsSourcePatches`; checked-in source does not fully equal compiled source; SlidingRows exists normally but integration/settings are partly injected; no notification/media/input/startup/accessibility/shortcut/icon-pack/parental subsystem exists yet. First implementation task is to remove that ambiguity without behavior change.

# 21. User expectations

The user prefers exact practical steps, small durable changes/checkpoints, exhaustive understanding, no repeated questions when context already answers them, source verification before edits, preservation of process fixes, actual behavior testing, explicit KEEP/EXTEND/REPLACE decisions, and strong performance/focus behavior.

# 22. Preserved archive layout

The repository handoff includes the C12 blueprint, this reconstructed chat/project record, complete Couchy C1–C12 reports, complete Projectivy Groups 1–12 plus post-closure corrections, and process hardening/provenance. Checksums are recorded in the handoff manifest.

# 23. Suggested fresh-conversation prompt

> We are continuing the Aerial-Rows Android TV launcher project. Read `AGENTS.md`, `AI_PROJECT_CONTEXT.md`, and `docs/ai-handoff/C12_FINAL_BLUEPRINT.md` before doing anything. The Projectivy/Couchy research is complete. Continue from Phase 0A exactly as documented, verify the current `aerialrows-development` branch first, and preserve the C11 performance/reliability invariants.

# 24. Final handoff state

Projectivy Groups 1–12 COMPLETE; Projectivy post-closure audits COMPLETE; Couchy C1–C12 COMPLETE + REVIEWED; final architecture frozen enough to implement; next phase PHASE 0A SOURCE-PURE BUILD. No additional discovery group is required before implementation.
