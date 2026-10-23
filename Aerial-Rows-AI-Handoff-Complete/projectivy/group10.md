# Projectivy 4.72 — Group 10: Persistence, Backups, Icons, Shortcuts & Parental Controls

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
Verification: **20/20 critical assertions passed**

## 1. Scope

This group closes five related Projectivy subsystems:

1. typed preference/config persistence and version migrations;
2. `.plbackup` full backups and `.pltheme` theme packages;
3. global icon packs and per-card artwork overrides;
4. Android launcher shortcuts and pinned-shortcut hosting;
5. parental-control policy, gating, authentication and recovery behavior.

The goal is behavioral reconstruction, not verbatim transplantation of Projectivy implementation code.

---

## 2. Process / reliability improvements used for this group

Before Group 10 discovery, the reverse-engineering pipeline was hardened again.

### 2.1 Fail-closed preflight

Every expensive stage now checks:

- exact APK SHA-256;
- semantic SQLite `PRAGMA integrity_check`;
- source-cache presence;
- hashes of every previously completed checkpoint artifact.

The current semantic index remains healthy:

- 5,952 indexed class records;
- 37,328 class-to-class reference edges;
- 1,994 resource references;
- 17,422 indexed literals;
- database integrity: `ok`.

### 2.2 Read-only/WAL discovery

Discovery/ranking queries use read-only SQLite access while checkpoint writes use the normal writable connection. This reduces contention and avoids unnecessary serialization between searching and checkpointing.

### 2.3 Skip-existing targeted class exporter

A content-aware class dumper now checks whether a requested selected-class export already exists. JADX is loaded only when at least one requested class is genuinely missing.

This preserves the reliability of targeted decompilation without repeatedly paying the APK-load/decompile cost for already-cached evidence.

### 2.4 Stage checkpoints

Group 10 was checkpointed separately as:

- discovery;
- verification;
- synthesis;
- report.

The discovery checkpoint contains a hashed 36-file evidence manifest. Verification used source + smali and passes 20/20 assertions.

---

# 3. Persistence architecture

## 3.1 `yh3`: typed preference descriptor

Projectivy does not scatter raw SharedPreferences calls throughout the launcher. The central preference abstraction is the obfuscated class `yh3`.

A descriptor carries information including:

- preference key;
- default value;
- title/description resource IDs;
- icon metadata;
- flags;
- optional numeric min/max/step;
- optional custom getter/setter hooks;
- optional dynamic/scoped suffix;
- legacy/migration alias information.

Important verified methods include behavior equivalent to:

```text
a()   → read current/effective value
b()   → calculate effective key
e(x)  → create/use a scoped key variant
g(old)→ migrate value from a legacy key
h(v)  → write value using hook/default path
i(v)  → direct typed write with numeric bounds handling
j(scope, value) → scoped write
```

The important architectural point is that settings are **descriptors**, not just strings.

That is why Projectivy can attach behavior such as:

- dynamic category-specific keys;
- migration aliases;
- validation/clamping;
- accessibility-service refresh requirements;
- custom serialization.

For Aerial-Rows, a comparable `TypedPreference<T>` abstraction is preferable to exposing raw DataStore keys everywhere.

---

## 3.2 `hj3`: central configuration store

`hj3` is the central application preference/configuration store.

It performs typed reads and writes for:

- Boolean;
- Int;
- Long;
- Float;
- String;
- StringSet;
- enum-like values represented by integer values.

Null/empty-string handling can remove preferences where appropriate.

`hj3` also owns serialization for structured launcher state rather than forcing all data into primitive keys.

Examples include:

```text
action_categories_list
    → serialized category models (`i5`)

action_customizers_list
    → serialized card customization/usage models (`l5`)

display_profiles_list
    → serialized display-profile objects

cached wallpaper state
    → serialized Wallpaper model
```

The implementation uses Kotlin serialization-style JSON infrastructure for these structured models.

This confirms the design we should use in Aerial-Rows:

```text
simple scalar setting
    → typed preference / DataStore

structured model collection
    → explicit versioned JSON serializer
```

rather than trying to encode every object into independent preference keys.

---

# 4. Versioned migrations

Projectivy has a genuine sequential migration engine.

The stored schema/app version is:

```text
key_app_version
```

Projectivy 4.72 migrates persisted state to:

```text
97
```

If the previous value is greater than zero and below 97, it iterates through intervening migration versions.

When migrations are invoked in the normal startup path, Projectivy first requests an automatic backup.

Conceptually:

```text
read stored version
      ↓
old but supported?
      ↓
automatic backup
      ↓
run migrations sequentially
      ↓
write version 97
```

### Confirmed migration examples

Migration 86 performs a large preference-key rename from older `key_*` names to the current descriptor keys. This includes launcher, background, card, row, status-bar, accessibility, parental-control and power settings.

Other verified migration families include changes around:

- category field representation;
- wallpaper delay units;
- category/grid configuration;
- wallpaper display-mode representation.

This is important for our reconstruction: **config migrations need to be part of the architecture from the beginning**, not something bolted on after we ship the first version.

Recommended Aerial-Rows pattern:

```text
MigrationRegistry
 ├─ v1 → v2
 ├─ v2 → v3
 ├─ ...
 └─ currentVersion
```

with an automatic safety backup before destructive migration.

---

# 5. Export policy and `nexp`

Projectivy uses a naming convention containing:

```text
nexp
```

for preferences that must not be included in ordinary exported backup/theme state.

The backup code explicitly skips any key whose name contains `nexp`.

Examples elsewhere in the settings map include transient/private-ish values such as:

- last-start date;
- unique installation/runtime key;
- theme author/name runtime metadata;
- cached wallpaper data.

So `nexp` can safely be interpreted at the behavior level as a **not-export marker**.

On full-restore/reset paths, Projectivy can remove all exportable settings while preserving these `nexp` settings.

For Aerial-Rows, this should become an explicit property rather than a string-name convention:

```text
TypedPreference(
    ...,
    exportPolicy = NEVER
)
```

That is safer and easier to audit.

---

# 6. Full `.plbackup` backups

Projectivy supports two high-level backup types:

```text
FULL_BACKUP
THEME
```

A `BackupParams`-style model controls theme/export options such as:

- export appearance settings;
- export wallpaper;
- export individual default-category settings.

Typical full-backup filename:

```text
projectivy-settings-yyyyMMdd_HHmmss.plbackup
```

## 6.1 Full backup payload

For a full backup, Projectivy reads all SharedPreferences and excludes `nexp` keys.

Supported values are converted into a JSON object while preserving type information through JSON representation:

- Boolean;
- Int;
- Long;
- Float;
- String;
- enum integer value;
- StringSet as an array.

The resulting JSON string is then encrypted before being written to the `.plbackup` URI.

---

## 6.2 Full backup restore

The import path was checked against smali because parts of the coroutine-heavy Java reconstruction are unreliable.

Verified behavior is:

```text
read backup bytes
      ↓
decrypt
      ↓
UTF-8 JSON
      ↓
clear existing exportable prefs
(preserve nexp keys)
      ↓
restore typed values
      ↓
run migrations
      ↓
refresh wallpaper / launcher caches / runtime state
```

Projectivy therefore treats restore as a **replacement of exported state**, not as a blind merge with arbitrary existing values.

That distinction matters when implementing our own import semantics.

---

# 7. `.plbackup` encryption

The export side is unambiguous.

Projectivy generates:

- random 16-byte salt;
- key derived from the salt;
- random 16-byte IV;
- AES/CBC/PKCS5Padding ciphertext.

The output is concatenated as:

```text
salt + IV + ciphertext
```

The key-derivation function starts from the UTF-8 bytes of:

```text
walt_sent_me
```

and performs 10,000 iterations of:

```text
SHA-256(previousBytes + salt)
```

The resulting digest becomes the AES key.

## Reliability caveat

The decrypt method's reconstructed salt-prefix length includes a runtime/static `ProjectivyAccessibilityService.s0` term in addition to 16. Both Java reconstruction and smali show that reference, while the exporter visibly writes a 16-byte salt.

That expression is therefore **runtime/obfuscation-sensitive** and should not be promoted into a clean portable file-format claim without executing the original runtime path.

This does not affect the behavioral reconstruction:

- backups are encrypted;
- the exporter format and algorithm are known;
- Projectivy itself can read its backups.

For Aerial-Rows there is no reason to copy this legacy cryptographic design. We should use a versioned authenticated-encryption envelope, e.g. AES-GCM, with explicit header/version/KDF parameters.

---

# 8. Theme packages (`.pltheme`)

As established in Group 6 and completed here, `.pltheme` is a ZIP-based package.

Verified entries include:

```text
manifest
appearance.plbackup
wallpapers/        (optional)
```

## 8.1 Theme manifest

The typed manifest model carries fields including:

- `projectivyLauncherVersion`;
- `themeVersion`;
- date;
- author;
- name;
- description;
- URL;
- credits.

Built-in Projectivy sample themes confirm that `manifest` is ordinary human-readable JSON.

`appearance.plbackup` is an encrypted appearance-settings payload using the backup mechanism rather than plain JSON in the ZIP.

## 8.2 Appearance/theme scope

Theme export can include:

- general appearance preferences;
- wallpaper-provider selection;
- optionally default-category appearance/settings;
- optionally wallpaper media in `wallpapers/`.

When category styling is exported as theme appearance, membership/exclusion content is intentionally treated differently from category styling so a theme does not simply overwrite a user's application membership as though it were a full backup.

That distinction is exactly what we should preserve:

```text
full backup = user state

theme = appearance/presentation package
```

---

# 9. Runtime refresh after restore/reset

Backup/reset logic does more than update SharedPreferences.

Projectivy resets/invalidate runtime structures including areas such as:

- home/category state;
- cached model lists;
- icon-pack state;
- wallpaper-related state;
- AccessibilityService config refresh.

This is an important implementation lesson.

For Aerial-Rows, import should finish with a central:

```text
ConfigChanged / RestoreCompleted
```

operation that tells repositories and UI state holders to reload coherently.

We should not rely on individual Composables noticing random DataStore mutations independently.

---

# 10. Icon-pack discovery

Projectivy supports conventional Android launcher icon packs.

It discovers candidates with:

```text
org.adw.launcher.icons.ACTION_PICK_ICON
```

For each candidate package, Projectivy accepts it as an icon pack when it exposes either:

```text
appfilter.xml
```

or:

```text
config_iconpack = true
```

The discovered icon-pack metadata includes:

- package name;
- version name;
- version code;
- icon;
- label;
- whether the package has a Leanback launch activity.

The global configured icon pack is stored in:

```text
appearance_cards_icon_pack
```

---

# 11. Icon-pack parsing and cache

Projectivy parses `appfilter.xml` entries containing component and drawable mappings.

Conceptually:

```xml
<item
    component="ComponentInfo{package/activity}"
    drawable="some_drawable" />
```

The component identity is normalized and mapped to the icon-pack drawable resource.

Projectivy persists a compact parsed cache. The binary cache has a verified:

```text
magic   = 1346983491
version = 2
```

This allows icon resolution without repeatedly reparsing potentially large icon-pack XML files.

For Aerial-Rows we do not need to clone this binary format. A cache keyed by:

```text
iconPackPackage + versionCode
```

is enough, as long as package update events invalidate it.

---

# 12. Artwork priority: per-card override vs icon pack

For an application card, Projectivy effectively resolves art in this order:

```text
per-card custom artwork
        ↓
global icon-pack mapping
        ↓
normal app banner/icon artwork
```

The per-card customizer model (`l5`) also stores information already encountered in Group 3:

- stable card/action ID;
- custom artwork URI;
- crop setting;
- custom title;
- launch count;
- last-launch timestamp.

The serialized collection is stored under:

```text
action_customizers_list
```

This gives us a clean separation for Aerial-Rows:

```text
App metadata
    ≠
User presentation override
```

so package rescans do not destroy user customizations.

---

# 13. Icon picker / external picker integration

Projectivy has an internal image/icon picker mode specifically for icon packs.

It can receive:

- icon-pack package;
- target package;
- target label.

The picker can:

- search/filter icons;
- display icon-pack contents;
- adapt card shape/aspect based on whether the pack is TV/Leanback-oriented;
- return the selected URI;
- delegate to an external icon-pack picker where appropriate.

Projectivy also exposes external apply-icon-pack actions in the manifest, so compatible icon packs/tools can request application through the launcher.

Any Projectivy premium-entitlement checks around that feature are product policy, not architecture that needs to be copied into Aerial-Rows.

---

# 14. Android launcher shortcuts

Projectivy is a proper Android launcher shortcut host.

## 14.1 Pinned shortcut requests

`PinShortcutActivity` handles Android O+ `LauncherApps.PinItemRequest`.

It:

1. retrieves the pin request from the incoming Intent;
2. requires request type `1` (shortcut);
3. verifies the request is valid;
4. avoids accepting an already-pinned shortcut;
5. calls `accept()`;
6. finishes with user feedback.

This is why Projectivy declares the relevant `CONFIRM_PIN_SHORTCUT` launcher-host intent in its manifest.

---

## 14.2 Shortcut repository

Projectivy's shortcut repository uses real `LauncherApps` APIs.

It first verifies:

```text
LauncherApps.hasShortcutHostPermission()
```

Initial pinned-shortcut query uses:

```text
ShortcutQuery.setQueryFlags(2)
```

A per-package query uses flags `11`.

The exact Android flag composition for numeric value 11 is platform API-defined; the important verified behavior is that Projectivy asks LauncherApps for a richer per-package shortcut set rather than storing its own static shortcut definitions.

The `LauncherApps.Callback` path from Group 3 also receives:

```text
onShortcutsChanged
```

and updates the shortcut cache.

---

## 14.3 Shortcut home cards

Shortcut card model `p94` is home item type 7.

It uses:

- shortcut short label as title;
- long label as secondary description when useful;
- `ShortcutInfo.isEnabled`;
- `LauncherApps.getShortcutIconDrawable`;
- `LauncherApps.startShortcut` for activation.

The local shortcut card stable ID is based on:

```text
shortcutInfo.getId().hashCode()
```

Package context is tracked by the surrounding ShortcutInfo/repository structures, although this particular local ID expression does not itself include the package name.

For Aerial-Rows we should use a stronger global identity such as:

```text
hash(packageName + shortcutId + user/profile)
```

while preserving Android LauncherApps as the source of truth.

---

# 15. Parental-control persistence

The primary relevant settings are:

```text
parental_control_apps
parental_control_code_long_duration
parental_control_protect_settings
parental_control_partial
parental_control_partial_start
parental_control_partial_end
parental_control_code
parental_control_emr_code
parental_control_emr_enc_code
```

Protected items are stored as stable unique identifiers represented as strings in a comma-separated list.

`ToggleParentalControlActivity` receives:

```text
uniquename
```

and adds/removes that stable identity from the list.

This means parental protection attaches to Projectivy's **stable card/action identity**, not to screen position.

That is the correct model for Aerial-Rows as well.

---

# 16. Parental policy / launch gate

The central check behaves approximately as:

```text
no primary code?
    → no parental gate

partial/scheduled mode enabled?
    ↓
current time inside configured window?
    ├─ no → no gate
    └─ yes → continue

settings protection enabled
AND target == settings stable ID?
    → gate

otherwise
    → gate only if target stable ID is protected
```

The special stable ID used for settings protection is:

```text
-1922314973
```

The schedule logic handles a window crossing midnight by moving the end boundary into the following day.

So a policy such as:

```text
22:00 → 07:00
```

is treated correctly as an overnight interval.

---

# 17. Parental launch wrapping

Rather than duplicating PIN checks inside each application launcher path, Projectivy can wrap the real destination Intent inside `ParentalControlCheckActivity`.

The wrapper carries data such as:

```text
launchIntent
launchClassName
```

After successful authentication, the requested underlying operation is performed.

Settings can be similarly routed through a start-for-result protection path.

For Aerial-Rows this should become a generic:

```text
LaunchGate.authorize(targetStableId) {
    performAction()
}
```

usable for:

- application launch;
- settings;
- sensitive internal actions;
- potentially protected TV inputs or shortcuts.

---

# 18. Authentication session duration

Projectivy caches a successful parental authentication timestamp in process memory.

Two verified unlock-duration policies exist:

```text
short duration = 15,000 ms
long duration  = 900,000 ms (15 minutes)
```

If a prior successful authentication is still inside the chosen duration, the user does not need to re-enter the code.

This should be modeled explicitly in Aerial-Rows as a short-lived authentication session rather than by permanently mutating protection state.

---

# 19. Primary and emergency/recovery codes

The parental screen accepts either:

- the primary parental code;
- an emergency/recovery sequence.

If no emergency code exists, Projectivy generates one.

The generator creates a 12-character sequence from:

```text
U / D / L / R
```

It maps those directions to digits 1–4, converts the resulting number and computes a transformed display/recovery value with the constants:

```text
2012789
1014333443013
```

The raw directional sequence and transformed display/recovery value are stored in separate preferences.

The transformed value is displayed by the parental-control UI as recovery/emergency information.

The exact product recovery workflow should be treated separately from the core launch-gate architecture; the verified fact is that Projectivy has both a primary secret and a generated emergency path.

---

# 20. Security assessment for our implementation

Projectivy 4.72 stores the primary/emergency parental values as ordinary SharedPreferences strings.

They are not `nexp` keys, which means they are part of the exportable preference set; the full `.plbackup` envelope is encrypted, but the live local preferences themselves are not a modern secret store.

For Aerial-Rows we should preserve behavior while improving security:

```text
ParentalPolicyStore
    → ordinary DataStore/config

ParentalSecretStore
    → Android Keystore-backed verifier / encrypted store
```

Ordinary backups should export:

- which targets are protected;
- schedule settings;
- policy settings;

but should **not** export raw PIN/recovery secrets unless the user explicitly chooses a protected secret-transfer mechanism.

Likewise, new backups should use authenticated encryption instead of copying the legacy AES-CBC format.

---

# 21. Reconstructed architecture for Aerial-Rows

Group 10 suggests the following implementation boundaries.

## `ConfigStore`

```text
ConfigStore
 ├─ TypedPreference<T>
 ├─ scoped/dynamic keys
 ├─ JsonBlobStore
 ├─ ExportPolicy
 └─ MigrationRegistry
```

Properties should know:

- default;
- serializer/type;
- validation;
- export policy;
- whether a runtime subsystem must refresh.

## `MigrationRegistry`

```text
read schemaVersion
      ↓
optional safety backup
      ↓
apply ordered migrations
      ↓
write current schema
```

## `BackupEngine`

```text
BackupEngine
 ├─ FullBackupExporter
 ├─ FullBackupImporter
 ├─ ThemeExporter
 ├─ ThemeImporter
 └─ BackupEnvelopeCodec
```

Use a versioned envelope with authenticated encryption and explicit metadata.

## `IconRepository`

```text
IconRepository
 ├─ AppOverrideStore
 ├─ IconPackDiscovery
 ├─ AppFilterParser
 ├─ IconPackCache
 └─ ArtworkResolver
```

Resolution order:

```text
user override
  → icon pack
  → TV banner/icon fallback
```

## `ShortcutRepository`

```text
ShortcutRepository
 ├─ LauncherApps permission check
 ├─ pinned/dynamic shortcut query
 ├─ LauncherApps.Callback refresh
 ├─ startShortcut
 └─ icon loading
```

plus a `PinShortcutActivity`-equivalent launcher-host entry point.

## `LaunchGate`

```text
LaunchGate
 ├─ protected stable IDs
 ├─ settings protection
 ├─ scheduled enforcement
 ├─ authentication session
 └─ secure secret verifier
```

It should wrap actions rather than make each individual card know PIN logic.

---

# 22. Cross-group integration

Group 10 connects directly to previous findings.

### Group 2 / home

Category and card stable IDs are the anchor for:

- custom artwork;
- custom titles;
- parental protection;
- persisted row membership/order.

### Group 3 / apps

App-card launch counts and last-launch timestamps live in the same customization model that stores card presentation overrides.

### Group 6 / themes

`.pltheme` combines appearance backup with optional wallpaper assets and uses the existing generic wallpaper/provider engine after import.

### Group 7 / settings

The `yh3/hj3` persistence layer is the backend for the 148 mapped settings descriptors.

### Group 9 / launching

Parental control is a generic launch wrapper and therefore composes with HOME, autostart and internal actions rather than needing separate protected launch code for each card type.

---

# 23. Reliability notes / unresolved implementation details

The subsystem is sufficiently closed for independent reconstruction, but a few details should intentionally **not** be overclaimed:

1. The decrypt-side salt prefix expression references `ProjectivyAccessibilityService.s0`. This was confirmed in both JADX Java and smali, but is inconsistent with a naive reading of the exporter. Treat it as runtime/protection-sensitive rather than a clean file-format constant.
2. Numeric `ShortcutQuery` flags are verified exactly as used (`2`, `11`), but this report does not invent semantic flag names that were not needed to reconstruct behavior.
3. Projectivy-specific premium gating encountered around icon/parental features is product entitlement logic, not required architecture for Aerial-Rows and is not being reproduced as a license bypass.
4. The exact internal compact binary layout of Projectivy's icon-pack cache is unnecessary for compatibility; the behavioral need is a versioned parsed mapping cache that invalidates when the icon-pack package changes.

---

# 24. Group 10 conclusions

1. Projectivy has a real typed configuration layer (`yh3`/`hj3`) rather than raw preference strings spread throughout the UI.
2. Persistence is versioned and migration-aware, with a safety backup before normal migrations.
3. Full backup is export-policy-aware: `nexp` values are intentionally preserved/excluded.
4. Full backup restores replace exportable state, then run migrations and refresh runtime caches.
5. `.pltheme` is a ZIP package containing a JSON manifest, encrypted appearance backup and optional wallpapers.
6. Global icon packs are ADW-compatible and parsed from `appfilter`; per-card custom artwork takes priority over the pack.
7. Projectivy is a genuine Android shortcut host, including pin requests and LauncherApps-backed shortcut cards.
8. Parental control is stable-ID based, centrally launch-gated, schedule-aware and session-authenticated.
9. Aerial-Rows should preserve these behaviors while deliberately improving secret storage and backup cryptography.
10. Persistence, backup, icon, shortcut and parental boundaries are now sufficiently understood to implement independently.

## Next group

**Group 11 — cross-cutting obfuscated-class closure / unresolved dependency graph**

The purpose of Group 11 is not another feature inventory. It is a systematic final pass over the classes and edges discovered in Groups 1–10 to identify any Projectivy-specific collaborator that remains unclassified or any behavior still supported only by inference, then close those gaps before writing the final Aerial-Rows implementation map in Group 12.
