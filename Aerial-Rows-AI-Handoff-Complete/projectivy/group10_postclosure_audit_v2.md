# Projectivy 4.72 — Group 10 Post-Closure Re-Audit v2

**Scope:** persistence, migrations, `.plbackup`, `.pltheme`, icon packs, Android shortcuts, parental controls, and their cross-group integration.  
**Audit namespace:** `AUDIT_10_R2`  
**Target APK SHA-256:** `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
**Original Group 10 report SHA-256:** `bbdd9ea9811e69e96e1e78b725cac9b7cdc75fee1793c02a2829a291d68c8a3c`  
**Process contract:** v16

## 1. Executive assessment

The original Group 10 architecture remains correct. This re-audit found **no second hidden persistence, backup, theme, icon-pack, shortcut, or parental-control subsystem**, and no factual reversal requiring the original Group 10 model to be discarded.

The fresh pass did, however, close several implementation details that materially improve the reconstruction specification:

1. parental authorization has its own active key-filtering path and transfers runtime state to/from `ProjectivyAccessibilityService`;
2. Projectivy can internally pin **and unpin** hosted Android shortcuts, not merely query/start them or accept external pin requests;
3. the two previously observed `ShortcutQuery` numeric flags have distinct verified roles (`2` for the global hosted cache, `11` for package-scoped queries);
4. theme export has an exact built-in-category cutoff (`i5.j < 23`) and strips two per-user category collections from exported copies;
5. the parsed ADW icon-pack cache is explicitly keyed by **icon-pack package + package version** and validates a magic/version header;
6. the full-backup replacement/import/migration/refresh flow is now split and canonically verified at method level;
7. the crypto framing is more tightly bounded: initialized runtime strongly indicates a 16-byte salt prefix, while the exact managed bootstrap call remains opaque and belongs to the Group 11 boundary.

Canonical verification: **23/23 successful evidence files, 0 errors, 0 invalid outputs**.  
Subsystem closure: **11/11 dimensions PASS**.

---

## 2. Original architecture that still holds

### 2.1 Typed configuration and structured state

`yh3` remains the typed setting/descriptor abstraction and `hj3` remains Projectivy's central configuration facade. Group 10's original conclusion that Projectivy is not merely scattering raw preference strings across Activities remains correct.

`hj3` also remains responsible for structured state beyond scalar settings, including serialized launcher/category/customization objects reached by the settings, backup and theme paths.

### 2.2 Sequential schema migration

The `key_app_version` migration model remains correct:

```text
stored app schema/version
        ↓
older than current version 97?
        ↓ yes
request safety backup
        ↓
run migration steps sequentially
        ↓
write current version
```

The earlier finding that migration 86 contains a substantial old-key rename/conversion layer remains valid.

### 2.3 `nexp` export policy

`nexp` remains a real **not-export policy**, not just a naming convention. Full backup export excludes such values and replacement-style restore deliberately preserves them when requested.

### 2.4 Full `.plbackup`

The original model remains correct:

```text
SharedPreferences / structured state
        ↓
filter non-exportable values
        ↓
typed JSON backup representation
        ↓
encrypt
        ↓
.plbackup
```

Restore replaces the exportable state, imports typed values, runs schema migration and refreshes runtime state.

### 2.5 `.pltheme`

`.pltheme` remains a ZIP package with the important structure:

```text
manifest
appearance.plbackup
wallpapers/
```

Theme scope is intentionally narrower/different from a complete user backup.

### 2.6 ADW-compatible icon packs

Projectivy continues to discover icon packs through the ADW action:

`org.adw.launcher.icons.ACTION_PICK_ICON`

and accepts package resources through `appfilter` and/or the `config_iconpack` capability marker.

### 2.7 Android launcher shortcuts

Projectivy remains a genuine Android launcher shortcut host using `LauncherApps`, `ShortcutInfo`, `ShortcutQuery`, `LauncherApps.Callback`, pin-item requests and shortcut-backed home cards.

### 2.8 Parental-control policy

The original stable-ID/launch-gate interpretation still holds. The system has persisted primary/recovery values, authentication TTL/session behavior, scheduled enforcement and centralized wrapping of protected actions.

---

## 3. New finding — parental UI actively filters key events

The original report concentrated on launch gating and authentication policy. The re-audit confirms that `ParentalControlCheckActivity` also actively filters key input.

`ParentalControlCheckActivity.dispatchKeyEvent(KeyEvent)` delegates the key code to `pq0.I(...)`. If the predicate says the key is blocked/consumed, the Activity consumes it instead of allowing normal dispatch.

The canonical `pq0.I()` evidence also shows that the key interpretation is adjusted using `ProjectivyAccessibilityService.s0` before being compared against the relevant parental-control key collection/state.

This means the parental subsystem is more tightly coupled to the Accessibility runtime than Group 10 originally stated.

### Reconstruction consequence

If Aerial-Rows ever implements parental controls, the better architecture is:

```text
Remote/Input layer
        ↓
ParentalInputPolicy
        ↓
Authentication / LaunchGate
```

rather than making an Accessibility service own parental key semantics.

For the current Aerial-Rows scope, parental controls remain optional/excluded; this behavior should therefore be documented rather than copied incidentally into the Accessibility implementation.

---

## 4. New finding — successful parental authorization transfers Accessibility state

`ParentalControlCheckActivity.F()` verifies the entered value against the primary and emergency/recovery values and updates the authentication timestamp.

On successful authorization it also performs:

```text
ProjectivyAccessibilityService.u0 = ProjectivyAccessibilityService.t0
ProjectivyAccessibilityService.t0 = null
```

before continuing the requested action/result flow.

That is a genuine coupling between the parental authorization Activity and the global Accessibility runtime state that was absent from the original Group 10 report.

### Reconstruction consequence

Do **not** reproduce this coupling. Authentication/session state should be owned by a launch/auth coordinator and passed explicitly to whatever action needs authorization.

---

## 5. New finding — Projectivy can pin and unpin hosted shortcuts itself

The original report documented:

- incoming external pinned-shortcut requests;
- `LauncherApps` shortcut queries;
- shortcut cards;
- shortcut launching.

The re-audit closes an additional mutation path in `j6.y0()`.

For shortcut cards on supported Android versions, Projectivy:

1. extracts the `ShortcutInfo`;
2. gets `LauncherApps`;
3. queries the package's current shortcut IDs with query flag `2`;
4. removes the current ID if it is already pinned, otherwise adds it;
5. calls `LauncherApps.pinShortcuts(package, ids, userHandle)`;
6. refreshes the affected launcher/category state.

So the correct independent model is:

```text
ShortcutRepository
 ├─ query
 ├─ callback refresh
 ├─ startShortcut
 ├─ accept external pin request
 └─ mutate pinned ID set
```

rather than a read/launch-only repository.

---

## 6. Refined shortcut query semantics — flags `2` vs `11`

The original report correctly recorded both numeric flag values but intentionally did not invent semantic names. The fresh evidence now closes where each is used.

### `yb3.d()` — global hosted shortcut cache

Projectivy first verifies shortcut-host capability and then builds/updates the launcher-wide shortcut cache using:

```text
ShortcutQuery.setQueryFlags(2)
LauncherApps.getShortcuts(..., current user)
```

### `xb3.o()` — package-scoped query

The package-specific path uses:

```text
ShortcutQuery.setPackage(package)
ShortcutQuery.setQueryFlags(11)
```

### Callback refresh

`nb3.onShortcutsChanged(...)` forwards the package/list update to the shared shortcut cache updater.

The correct reconstruction therefore preserves the **two query contexts** even if our code replaces opaque numeric constants with named internal query modes.

---

## 7. Refined icon-pack cache — package and package-version identity

The correct cache-building/parsing method is `zl4.b()`.

It constructs the parsed icon-pack cache filename from both:

```text
icon-pack package
+
icon-pack package version
```

in the form equivalent to:

```text
iconpack-<package>-<version>.cache
```

The parser also validates:

- magic: `1346983491`;
- cache format version: `2`.

If the cache is invalid/missing, Projectivy reparses the icon-pack resources.

This directly verifies that cache invalidation is not merely package-name based.

`pq0.z()` independently reconfirms ADW-compatible discovery: `appfilter` XML or the `config_iconpack` boolean capability is sufficient for recognition.

### Aerial-Rows consequence

Use a cache key based on at least:

```text
packageName + versionCode/versionName + our cache schema version
```

and regenerate when any component changes.

---

## 8. New theme-export precision — built-in category cutoff and per-user stripping

`dj3.d()` confirms the full-backup branch still starts from all SharedPreferences values and excludes `nexp` values.

The **theme** branch has additional policy that the original report did not fully capture.

When default/category settings are included in a theme, Projectivy:

1. obtains the category collection;
2. retains only categories satisfying `i5.j < 23`;
3. copies each category object;
4. clears two copied per-user collections (`u` and `v`);
5. serializes the reduced category state under `action_default_categories_settings`.

This means Projectivy has an implicit numeric boundary between built-in/default categories and user-specific category data.

### Aerial-Rows consequence

Do **not** clone the magic number `23`.

Model this explicitly:

```text
CategoryOrigin
 ├─ BUILT_IN
 ├─ PROVIDER
 └─ USER
```

and define separately which fields are theme-exportable vs user-state-only.

---

## 9. Full restore flow — now canonically split across methods

The re-audit strengthens the previous restore model.

### `dj3.n(true)` — replacement/reset policy

When preserving never-export values, Projectivy enumerates existing preference keys and removes every key that is not protected by the `nexp` policy. The alternate branch clears the store.

It then reapplies configuration/migration state and runs the runtime reset/rehydration path.

### `dj3.i(...)` — import path

For a full backup, the importer invokes the replacement/reset behavior, performs typed JSON import, calls `hj3.a(false)` to re-run/normalize migration state, and triggers the runtime/background refresh path.

### `dj3.j(...)` — theme ZIP import

The Smali fallback verifies the ZIP-based theme path including `appearance.plbackup` and the `wallpapers/` entries.

There is therefore no hidden second restore engine; full backup and theme import converge on the existing config/migration/runtime-refresh architecture.

---

## 10. Backup crypto — stronger bound, but bootstrap caveat remains

Canonical `s71.d()` proves the key derivation routine starts with the fixed UTF-8 secret:

`walt_sent_me`

and performs 10,000 rounds of SHA-256 over the evolving key material plus salt, then creates an AES key.

Canonical `s71.b()` proves decrypt uses:

- salt prefix length `ProjectivyAccessibilityService.s0 + 16`;
- next 16 bytes as IV;
- `AES/CBC/PKCS5Padding`;
- remaining bytes as ciphertext.

Cross-group canonical evidence for `DisplayProfileManager.rps()` sets:

```text
ProjectivyAccessibilityService.s0 = 0
```

as part of normal initialized runtime state. This strongly indicates an effective 16-byte salt prefix during initialized operation.

However, Group 11 found no normal managed Java caller for `DisplayProfileManager.rps()`. The exact bootstrap linkage therefore remains opaque across the managed/bootstrap/native boundary.

### Correct conclusion

We can say with high confidence that the normal initialized format is consistent with:

```text
16-byte salt
16-byte IV
AES-CBC ciphertext
```

but should **not** overclaim the exact hidden initialization mechanism that establishes `s0`.

### Aerial-Rows consequence

Do not reproduce this crypto design. Prefer authenticated encryption such as AES-GCM and appropriate key management/KDF semantics rather than a fixed embedded secret plus AES-CBC.

---

## 11. Recovery-code transform reconfirmed

`pq0.p()` reconfirms the original Group 10 recovery-code interpretation:

- a 12-character directional sequence is built from Up/Down/Left/Right;
- directions are mapped to digits `1–4`;
- the raw and transformed forms are persisted separately;
- the deterministic transform includes `(value * 2012789) % 1014333443013`.

No reversal was found here.

---

## 12. External pin-request intake reconfirmed

`PinShortcutActivity.onCreate()` is loaded from `classes2.dex` and verifies the incoming `LauncherApps.PinItemRequest`:

- request exists;
- request type is `1`;
- request is valid;
- shortcut is not already pinned;
- `accept()` succeeds.

It then finishes the Activity.

This remains a thin launcher-host entry surface rather than a second shortcut subsystem.

---

## 13. No hidden second subsystem

Fresh semantic-index discovery, narrowed JADX inspection, canonical REA verification and all-DEX closure found no second implementation for:

- configuration storage;
- schema migration;
- backup/export/import;
- theme packaging/import;
- icon-pack discovery/parsing/cache;
- Android shortcut hosting;
- parental authorization.

The major collaborators continue to converge on the previously mapped `yh3/hj3`, `dj3/s71`, `pq0/zl4`, `yb3/xb3/nb3/j6/p94` and parental Activity/helper architecture.

---

## 14. Updated Aerial-Rows reconstruction boundaries

### ConfigStore

```text
ConfigStore
 ├─ typed SettingKey<T>
 ├─ structured launcher state repositories
 ├─ schema version
 └─ MigrationRegistry
```

### BackupEngine

```text
BackupEngine
 ├─ explicit export policy
 ├─ typed serialization
 ├─ authenticated encryption
 ├─ replacement / merge policy
 └─ post-restore migration + rehydrate
```

### ThemeEngine

```text
ThemeEngine
 ├─ theme manifest
 ├─ appearance payload
 ├─ optional wallpaper assets
 ├─ explicit category-origin policy
 └─ theme-exportable vs user-state-only fields
```

### IconRepository

```text
IconRepository
 ├─ per-card override
 ├─ ADW icon-pack parser
 ├─ package+version keyed parsed cache
 └─ TV banner/icon fallback
```

### ShortcutRepository

```text
ShortcutRepository
 ├─ global hosted cache
 ├─ package-scoped queries
 ├─ LauncherApps.Callback
 ├─ startShortcut
 ├─ external PinItemRequest intake
 └─ pin/unpin mutation
```

### LaunchGate / parental state

If implemented in the future:

```text
LaunchGate
 ├─ protected stable IDs
 ├─ schedule policy
 ├─ auth TTL/session
 ├─ secure verifier
 └─ input filtering policy
```

It should **not** depend on Accessibility global static fields.

---

## 15. Process improvements made before/during this re-audit

The process was deliberately reviewed and improved before Group 10 conclusions were accepted.

### 15.1 Full authoritative APK resource inventory

Persistent JADX remains in `resources=lite` mode because that makes class/method discovery much faster. That lightweight daemon is now explicitly **non-authoritative for resource closure**.

A separate cached full APK inventory pins:

```text
APK entries          1,171
res/ entries         1,066
res XML entries        831
```

including entry CRC/size metadata and an inventory digest. Audit preflight/self-test fails closed if this no longer matches the pinned APK.

### 15.2 Safe additive provenance upgrades

Process evolution can add new derived provenance fields without invalidating sound old evidence, but only if:

1. every field present in the old manifest still matches exactly;
2. the newly introduced field is independently verified against the pinned target/toolchain;
3. the old manifest is preserved in provenance history before re-attestation.

### 15.3 In-process persistent JADX code-search batching

The existing single-session MCP batch path was extended to `jadx_search`.

A broad whole-APK code search still proved too expensive for the execution envelope. The process therefore now explicitly prefers:

```text
reports/evidence
   ↓
FTS + static semantic index
   ↓
narrowed persistent JADX
   ↓
canonical REA for high-risk claims
```

### 15.4 Dotted-literal semantic-index fallback

The semantic code index had the same FTS5 punctuation weakness previously fixed in `auditctl search`: identifiers such as `appearance.plbackup` could be parsed as FTS syntax.

The query layer now safely falls back to a quoted literal search. An initial implementation mistake in this change was caught, restored from backup, corrected, and a real runtime dotted-literal invocation was added to self-test so compilation alone is no longer considered sufficient.

### 15.5 Lock-aware immediate orphan cleanup

An outer execution timeout interrupted a canonical batch after two committed probes and left an uncommitted temporary output for the next probe.

The evidence remained safe, but cleanup was previously age-based. It now:

1. maps the temp output to its canonical per-output advisory lock;
2. attempts a nonblocking exclusive lock;
3. removes the temp/err/empty lock immediately only if that lock is free;
4. leaves active writers untouched.

This makes external interruption cleanup immediate without racing a live probe.

### 15.6 Content validation after probe success

A successful REA command is **not automatically accepted as proof of the intended claim**.

During this audit an initial `zl4.a()` probe succeeded but proved only runtime icon lookup, not icon-cache construction. Content validation caught the mismatch and the correct `zl4.b()` cache parser/builder was canonically verified before synthesis.

This is now an explicit operational rule: **probe success != claim success**.

### 15.7 Smaller canonical verification batches

Fresh REA verification generally took about 7–13 seconds per uncached high-impact probe in this cycle. Canonical probes were therefore run in small batches (typically three) so completed results land atomically before the outer command window, while state files make interruption/resume exact.

A cross-group cached `DisplayProfileManager.rps()` verification returned in roughly **0.79 seconds**, preserving the earlier cache gains.

---

## 16. Closure matrix

The fresh Group 10 audit is not grandfathered. It passed the current machine-enforced 11-dimension closure matrix:

| Dimension | Result |
|---|---|
| Entry points | PASS |
| Base classes / inheritance | PASS |
| All DEX files | PASS |
| Obfuscated collaborators | PASS |
| Models / data flow | PASS |
| Persistence | PASS |
| Resources / XML | PASS |
| Android framework APIs | PASS |
| Callbacks / observers | PASS |
| Launch actions / lifecycle | PASS |
| Native / JNI | N/A — no Group 10 core path depends on JNI; opaque display-profile bootstrap remains Group 11 boundary |

Canonical evidence health:

```text
success   23
error      0
invalid    0
```

---

## 17. Final assessment

```text
Core Group 10 architecture       correct
Missing major subsystem          none
Factual reversals                none
Canonical evidence               23 / 23 successful
Closure                          11 / 11 PASS
```

### Genuine completeness misses closed by v2

1. parental Activity key filtering through `pq0.I()`;
2. parental authorization's Accessibility runtime-state transfer;
3. internal shortcut pin/unpin mutation with `LauncherApps.pinShortcuts()`;
4. exact theme built-in-category cutoff and per-user collection stripping.

### Important refinements

- exact global flag-`2` vs package-scoped flag-`11` shortcut-query split;
- package+version keyed ADW icon-pack cache with verified magic/version;
- exact full-restore replacement/import/migration/rehydration split;
- stronger initialized-runtime interpretation of the decrypt salt prefix without overclaiming the opaque bootstrap caller;
- reconfirmed recovery-code transform and external pinned-shortcut intake.

**Group 10 status: COMPLETE — POST-CLOSURE RE-AUDITED v2.**
