# Projectivy 4.72 — Group 11: Cross-cutting obfuscated-class closure

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
Verification: **33 / 33 closure assertions passed**

## 1. Purpose

Groups 1–10 reconstructed the major Projectivy subsystems independently: package architecture, home UI, apps/actions, Android TV Provider content, notifications/media, wallpaper/themes, settings, TV inputs/vendor integration, HOME/boot/accessibility, and persistence/backups/icons/shortcuts/parental controls.

Group 11 is deliberately different. It is a closure pass whose job is to answer:

> Is there still a Projectivy-specific implementation subsystem hidden behind obfuscated classes or overlooked named components that would materially block a from-scratch reconstruction?

The criterion is **behavioral and architectural closure**, not source-line closure. Generic AndroidX/Kotlin/Media3/Glide/coroutine machinery does not need to be reverse engineered merely because it was minified into `defpackage.*`. Conversely, an unclassified class that changes launcher behavior does need to be traced.

## 2. Process improvement used for this group

Before closure analysis, the existing persistent evidence cache was extended with an automated **closure auditor**.

It compares:

- every Groups 1–10 report;
- manifest-declared Projectivy components;
- the local semantic source index;
- class-to-class reference graph;
- resource references;
- string/literal index;
- unresolved/deferred wording in previous reports;
- app-affinity of obfuscated classes.

This converts closure from a broad APK rescan into:

```text
Groups 1–10 reports
        +
manifest component inventory
        +
semantic/class graph
        ↓
automated closure audit
        ↓
ranked unresolved items
        ↓
small targeted decompile/export batches
        ↓
smali/native verification where needed
        ↓
fail-closed assertions
```

The discovery stage recovered **26 previously untraced named Activity classes in one APK load** and ranked the remaining unmentioned obfuscated collaborators rather than searching all classes again.

All Group 11 stages are independently checkpointed and hashed:

```text
GROUP_11/discovery      COMPLETE
GROUP_11/verification   COMPLETE
GROUP_11/synthesis      COMPLETE
GROUP_11/report         COMPLETE
```

## 3. Closure result

The closure conclusion is:

> **No remaining cross-cutting Projectivy-specific subsystem appears necessary to reproduce the launcher behavior mapped in Groups 1–10.**

The remaining opaque implementation falls into one of these categories:

1. standard/minified AndroidX, Kotlin, coroutine, Media3 or other library machinery;
2. narrow Projectivy utility/helper classes whose externally relevant behavior is already covered;
3. optional root/vendor maintenance tools rather than launcher-core features;
4. synthetic/compiler-generated dispatch classes;
5. premium/entitlement UI, which is outside the clean-room reconstruction target;
6. native/protection/bootstrap details that do not implement the normal launcher feature set;
7. exact decompiler-hostile control flow for behaviors whose observable architecture is already closed.

This means Group 12 can move from reverse engineering to the concrete Aerial-Rows implementation map without assuming that a large unidentified Projectivy subsystem remains hidden.

---

# 4. Previously untraced named Projectivy components

The manifest/component comparison exposed a set of named classes that earlier thematic groups had described indirectly but had not explicitly traced.

A single batch export recovered 26 such Activities.

## 4.1 Category/settings shortcut wrappers

### `CategoryContentSidePanelActivity`

This is a thin settings wrapper around Projectivy's category-content preference screen.

It does not introduce another category engine. It enters the same category/settings system reconstructed in Groups 2 and 7.

### `ManageCategoriesActivity`

Thin wrapper for the categories-management preference screen.

No separate persistence or row implementation was found.

### `ManageChannelsActivity`

Thin wrapper for the TV-channel management preference screen.

It routes into the channel/settings functionality already reconstructed in Groups 4 and 7.

**Closure:** these three Activities are UI entry points, not missing subsystems.

---

# 5. Vendor settings and engineering wrappers

The closure pass explicitly classified the named vendor Activities from Group 1.

## 5.1 Xiaomi / Mi TV

`SettingsMiTvActivity` targets Xiaomi's settings package/component, including:

```text
com.xiaomi.mitv.settings
```

## 5.2 Formovie / FengOS

`SettingsFengOsActivity` targets:

```text
com.formovie.gsettings
```

## 5.3 Amlogic / Droidlogic

`SettingsAmlogicActivity` targets Droidlogic TV settings components and has a fallback path to an alternate settings Activity.

## 5.4 MediaTek

`SettingsMediatekActivity` uses the vendor action:

```text
mediatek.tvsetting.factory.intent.action.MainmenuActivity
```

## 5.5 Standard Android

`SettingsAndroidActivity` opens:

```text
android.settings.SETTINGS
```

## 5.6 Fengmi/FengOS engineering menus

`EngMenuFengmiActivity` and `EngMenuFengOsActivity` launch the appropriate vendor test/engineering Activities, including the FengOS autofocus-test package where available.

**Closure:** these are exactly the vendor-adapter entry points anticipated by Group 8. They do not introduce a second device-control architecture.

---

# 6. Previously overlooked app/device maintenance tools

Group 11 found a set of real Projectivy utilities that were not central to Groups 1–10. They are useful to understand but are **optional privileged maintenance tools**, not dependencies of the launcher core.

## 6.1 `InstallAppsActivity`

Projectivy includes a guided APK installer utility.

Verified behavior includes:

- accepts an `extra_install_url` target;
- supports HTTP(S) and local/storage-oriented paths;
- checks whether Projectivy is allowed to request package installs;
- if necessary opens:

```text
android.settings.MANAGE_UNKNOWN_APP_SOURCES
```

for its own package;

- launches APK content with:

```text
application/vnd.android.package-archive
```

- contains helper paths for downloaded/compressed package files;
- reports failures through Projectivy's common toast/status helper.

This is independent of the app-card uninstall/App Info actions mapped in Group 3.

### Aerial-Rows implication

If desired later, this belongs in an optional `DeviceMaintenanceTools`/installer module, not `AppRepository`.

---

## 6.2 App freeze/unfreeze utility

`CleanupActivity` is effectively an **application freeze** tool rather than a generic cache cleaner.

Its delegate offers:

- freeze suggested packages;
- unfreeze suggested packages;
- manually freeze a selected package;
- manually unfreeze a selected package.

The worker path calls Projectivy's privileged/root shell manager (`kx3`) for freeze and unfreeze operations.

Projectivy's root manager itself tests root capability and executes shell operations asynchronously.

### Aerial-Rows implication

This must remain capability-gated and optional. Normal app hiding/favourites/uninstall must never depend on root.

---

## 6.3 Stock launcher / Leanback launcher maintenance

`RemoveLauncherActivity` contains identities for stock launcher packages including:

```text
com.google.android.leanbacklauncher
```

as well as Xiaomi/Formovie launcher targets and associated system paths.

Its guided UI exposes **freeze** and **unfreeze** operations for stock or Leanback launchers.

The underlying work is privileged/root-oriented.

This is separate from Projectivy's legitimate Android HOME-role handling reconstructed in Group 9.

### Aerial-Rows implication

Do not make root-based stock-launcher modification part of the normal HOME setup path. Use Android HOME role/default-launcher APIs first. Privileged launcher maintenance, if ever implemented, should be clearly isolated as an expert tool.

---

## 6.4 Google Play self-update blocking

`BlockGooglePlaySelfUpdateActivity` exposes two actions:

```text
Block
Unblock
```

and delegates to a privileged helper.

The exact low-level mutation is not required for launcher reconstruction and was intentionally not expanded beyond the user-visible and privilege boundary.

---

## 6.5 GApps uninstall/debloat

`GappsUninstallActivity` presents a confirmation path and invokes a privileged helper for the operation.

Again, this is device maintenance rather than launcher architecture.

---

# 7. Web Remote utility

`WebRemoteActivity` does not appear to host its own local HTTP server.

Its delegate asynchronously determines the local-device IP and constructs an external URL of the form:

```text
http://mireco.hopto.org/#<local-ip>
```

It presents/opens that remote-web page.

The evidence therefore supports classifying Web Remote as an external web utility keyed to the local IP, not as a hidden Projectivy web-server subsystem.

If Aerial-Rows ever wants remote administration, it should be designed independently rather than depending on this external service.

---

# 8. Other shortcut/wrapper Activities

## 8.1 `MediaExplorerActivity`

Launches an available Xiaomi/FengOS media-explorer target. This is another vendor shortcut.

## 8.2 `GoogleSmartHomeActivity`

Launches Projectivy's lazily resolved Google TV Smart Home intent when the capability exists.

## 8.3 `ProfilePickerActivity`

Launches a lazily resolved supported profile-picker target.

## 8.4 `AccessibilityServiceActivity`

Provides a guided configuration/status surface around the already-mapped Projectivy Accessibility Service.

No second accessibility subsystem exists.

## 8.5 `ColorPickerActivity`

Reusable Material-style colour picker with luminance/alpha-related controls and a dynamic-colour preference.

This is a UI helper for settings/theme features rather than a hidden configuration store.

---

# 9. About/device diagnostic Activities

`AboutAppActivity`, `AboutBackgroundActivity`, and `AboutDeviceActivity` are informational/diagnostic GuidedStep screens.

The device screen can surface information such as:

- Android/device/build properties;
- display and HDR capabilities;
- IP/network information;
- root status;
- display-profile information;
- some projector/device temperatures where available.

A repeated-tap developer-style interaction exists in this area and can invalidate/reload TV-input related information.

These screens expose already-known capabilities; they do not introduce another hardware abstraction layer.

---

# 10. Premium/entitlement surface

`PremiumActivity` and `PremiumCardActivity` were identified and classified as entitlement/purchase UI surfaces.

They were **not** reverse engineered for entitlement bypass or premium unlocking.

For the reconstruction project, the relevant principle is simply:

```text
feature implementation
    ≠
Projectivy entitlement implementation
```

Aerial-Rows can implement its own feature set and licensing model independently.

---

# 11. Autostart sentinel closure

Group 9 left two obscure numeric branches in the startup coordinator for closure.

The autostart handler contains special values including:

```text
-1    last-used TV input
-100  ambient/screensaver
-2    legacy/sentinel-looking branch
-101  legacy/sentinel-looking branch
```

The current 4.72 settings UI explicitly offers:

- the available `dd4` input values;
- `-1` for last-used input when appropriate;
- `-100` for ambient/screensaver;
- application/package targets separately.

The current settings source does **not** expose `-2` or `-101` as selectable options.

Therefore the best-supported classification is:

> `-2` and `-101` are legacy/internal/sentinel compatibility branches, not missing user-facing autostart choices in Projectivy 4.72.

No additional startup target needs to be reproduced merely because these compatibility branches remain in bytecode.

---

# 12. `SOURCE_UNKNOWN` closure

`dd4.SOURCE_UNKNOWN` is explicitly represented by:

```text
Integer.MIN_VALUE
```

and an `unknown` identity.

It is a sentinel/fallback state, not another user-selectable TV source.

This closes the Group 8 ambiguity cleanly.

---

# 13. Backup decrypt framing and runtime initialization

Group 10 established the portable export-side backup framing:

```text
16-byte random salt
16-byte random IV
AES/CBC/PKCS5Padding ciphertext
```

with the key derivation seeded by `walt_sent_me` and iterated through SHA-256 10,000 times.

The decryption method contains the unusual expression:

```text
ProjectivyAccessibilityService.s0 + 16
```

when extracting the salt prefix.

The closure pass found a highly relevant runtime fact:

`DisplayProfileManager.rps()` assigns:

```text
ProjectivyAccessibilityService.s0 = 0
```

and no managed Java caller of `DisplayProfileManager.rps()` appears in JADX's uses graph.

This creates the following strongly supported interpretation:

```text
process starts
   ↓
runtime/bootstrap path not visible as ordinary managed caller
   ↓
DisplayProfileManager.rps()
   ↓
s0 becomes 0
   ↓
decrypt prefix length becomes 16 bytes
```

However, the exact mechanism by which `rps()` is invoked is **not proven** by managed xrefs alone. It may involve native/bootstrap/protection behavior.

The report therefore does **not** assert an exact native call chain that has not been demonstrated.

For Aerial-Rows this ambiguity is non-blocking. We should not clone this framing trick. Use an explicit versioned backup format with modern authenticated encryption such as AES-GCM.

---

# 14. Native `libptutils.so` closure

Projectivy ships `libptutils.so` for its Android ABIs.

The arm64 library exports at least:

```text
Java_com_spocky_projengmenu_PTApplication_vc
Java_com_spocky_projengmenu_PTApplication_vs
```

The managed code declares/calls `PTApplication.vs(boolean)` through a narrow path:

```text
PTApplication.vs(...)
       ↑
PTApplication.f()
       ↑
  v.o(...)
  ya3.o(...)
```

JADX uses show `vs()` has only `PTApplication.f()` as its managed caller, and `f()` has two managed caller sites.

Arm64 inspection indicates `vc` is a very small global-byte accessor while `vs` is the larger JNI/JNIEnv-heavy routine.

The library also contains common archive/zlib/minizip machinery and helper code, but no evidence indicates that Projectivy's home-row engine, app discovery, TV Provider repository, media-session layer, wallpaper engine, settings hierarchy, or TV-input architecture is implemented primarily inside the native library.

### Closure conclusion

`libptutils.so` is best treated as a narrow **initialization / validation / protection / utility boundary**, not as an undiscovered native launcher implementation.

It should **not** be transplanted into Aerial-Rows.

Any legitimate initialization behavior we need—such as loading display profiles—should be made explicit in Kotlin.

Opaque validation/protection logic is outside the clean-room reconstruction target.

---

# 15. Group 6 residual decompiler ambiguities

The closure auditor correctly surfaced two deliberately cautious statements from Group 6:

1. the exact coroutine control flow used to refresh/cache wallpaper-provider candidates;
2. the exact internal copy/persistence control flow for some imported background files.

These do **not** represent missing externally meaningful architecture.

Already verified behavior includes:

- provider discovery;
- provider IDs and update modes;
- event masks;
- per-provider candidate lists;
- cache timestamps/TTLs;
- invalidation/update broadcasts;
- IMAGE/VIDEO/COLOR/DRAWABLE/LOTTIE renderer selection;
- selected/folder/theme/program/Now Playing provider behavior;
- user-selected image/video handling;
- render/lifecycle/error fallback behavior.

For a from-scratch implementation, reproducing a compiler-generated coroutine state machine or Projectivy's private file-copy sequence line-for-line adds no behavioral value.

These are therefore classified as **source-level ambiguity, behavioral architecture closed**.

---

# 16. High-affinity obfuscated-class closure

The closure auditor ranked obfuscated classes by direct references to known Projectivy code, resource use, role tags, and graph proximity.

The highest-scoring previously unmentioned classes included examples such as:

```text
ez2
r82
ss
jj4
xo1
h03
em3
b5
b4
yn1
bi3
ko0
iw4
fs4
...
```

Inspection shows why raw reference-count ranking is insufficient.

## 16.1 Projectivy-specific narrow helpers

Examples include:

- `ez2` — Projectivy toast/process feedback utility;
- `h03` — drawable/colour/helper functionality used by Projectivy UI;
- `iw4` — broad platform/file/sysfs/accessibility/provider utility collection;
- `fs4` — package/platform utility;
- `q32` — application/global context/state helper.

These are important support utilities, but their Projectivy-facing behaviors have already been exercised by the functional subsystems in Groups 1–10.

## 16.2 Synthetic/compiler dispatchers

Classes such as `em3` and `b5` include generated/synthetic dispatch behavior tying together already-known settings, device and wallpaper paths.

They do not represent independently designed feature subsystems.

## 16.3 Minified library/framework machinery

Several high-affinity classes have many incoming references precisely because they are common Kotlin/AndroidX/base utilities. Examples from the ranked list include generic lazy/function/coroutine/base/activity machinery.

The closure rule is therefore:

```text
high reference count
    ≠
Projectivy-owned feature subsystem
```

A class only blocks closure when its behavior changes a Projectivy feature in a way that is not already accounted for.

No such new core subsystem remained after this pass.

---

# 17. Complete behavioral subsystem graph after Group 11

The Projectivy architecture now closes into the following major areas:

```text
Projectivy 4.72
│
├── Application/bootstrap
│   ├── PTApplication
│   ├── typed preferences/config
│   ├── migrations
│   └── narrow native validation/bootstrap boundary
│
├── HOME shell
│   ├── MainActivity
│   ├── MainFragment
│   ├── Leanback browse/header state
│   └── focus/selection restoration
│
├── Generic home categories/items
│   ├── configurable i5 categories
│   ├── stable item/card models
│   ├── app cards
│   ├── input cards
│   ├── Watch Next/program cards
│   ├── shortcuts
│   ├── Now Playing
│   └── category folders
│
├── Apps
│   ├── LauncherApps/PackageManager discovery
│   ├── TV/mobile de-duplication
│   ├── favourites/hidden/order
│   ├── launch/App Info/uninstall
│   ├── usage sorting
│   └── shortcut host
│
├── Android TV Provider
│   ├── channels
│   ├── preview programs
│   ├── Watch Next
│   └── program details/previews
│
├── Media/notifications
│   ├── NotificationListener
│   ├── notifications panel
│   ├── MediaSessionManager
│   ├── Now Playing row
│   └── playback actions
│
├── Wallpaper/theme engine
│   ├── provider registry
│   ├── external provider Binder API
│   ├── wallpaper events
│   ├── candidate caching
│   ├── image/video/color/drawable/Lottie renderers
│   └── .pltheme packages
│
├── Settings
│   ├── typed setting descriptors
│   ├── Preference/GuidedStep surfaces
│   ├── dynamic settings
│   └── live/reload/deferred update policies
│
├── TV inputs/device adapters
│   ├── TvInputManager/TvView
│   ├── HDMI/AV/Android source model
│   ├── Xiaomi/Amlogic/MediaTek/Formovie/Google adapters
│   ├── display profiles
│   └── optional root/sysfs engineering tools
│
├── HOME/boot/accessibility
│   ├── Android HOME role
│   ├── autostart coordinator
│   ├── AccessibilityService compatibility layer
│   ├── remote gesture engine
│   └── idle coordinator
│
├── Persistence/customization
│   ├── typed SharedPreferences/config
│   ├── migrations
│   ├── .plbackup
│   ├── icon packs/custom artwork
│   ├── shortcuts
│   └── parental LaunchGate
│
└── Optional maintenance utilities
    ├── APK installer
    ├── package freeze/unfreeze
    ├── stock launcher maintenance
    ├── Play self-update block/unblock
    ├── GApps removal
    ├── Web Remote link
    └── diagnostics/about tools
```

This is now sufficiently closed to build an implementation plan without expecting a missing architectural layer to invalidate it later.

---

# 18. What should *not* be carried into Aerial-Rows core

The closure pass is as useful for identifying what **not** to transplant as what to implement.

The Aerial-Rows core should not depend on:

- Projectivy's native `libptutils.so`;
- Projectivy entitlement/purchase code;
- root access;
- stock-launcher freezing;
- GApps removal;
- Play Store self-update modification;
- OEM engineering menus;
- Projectivy's exact encrypted backup framing;
- Projectivy's compiler-generated coroutine/obfuscation structure.

Those are either optional device tools or implementation-specific internals.

---

# 19. Recommended optional maintenance module for Aerial-Rows

If we eventually want feature parity with Projectivy's expert/device utilities, isolate them behind capability interfaces:

```text
DeviceMaintenanceTools
├── PackageInstallerTool
├── PackageFreezeTool          [root optional]
├── StockLauncherTool          [root optional]
├── SystemAppMaintenanceTool   [root optional]
├── VendorEngineeringLauncher
├── DeviceDiagnostics
└── RemoteManagementLink
```

Runtime capability detection should determine whether each feature is shown.

None should be required for ordinary Shield/Android TV operation.

---

# 20. Verification summary

The Group 11 fail-closed verifier passed **33/33** checks covering:

- exact APK SHA;
- 26 recovered named Activity sources;
- category/channel/settings wrapper recovery;
- Xiaomi/Formovie/Amlogic/MediaTek/Android/Fengmi wrapper targets;
- unknown-source/APK installer behavior;
- Web Remote URL behavior;
- package freeze/unfreeze UI + privileged worker link;
- Leanback launcher maintenance;
- Play Store block/unblock helper path;
- GApps uninstall helper path;
- autostart `-2`/`-101` compatibility branches;
- current `-1`/`-100` visible choices;
- absence of current UI choices for `-2`/`-101`;
- `SOURCE_UNKNOWN` sentinel;
- backup 16-byte salt/IV framing;
- runtime-sensitive `s0 + 16` decrypt expression;
- `DisplayProfileManager.rps()` setting `s0 = 0`;
- lack of managed Java caller for `rps()`;
- native `vc`/`vs` exports;
- narrow managed callers of `vs()` / `PTApplication.f()`;
- explicit non-guessing treatment of Group 6 decompiler ambiguities.

---

# 21. Final Group 11 conclusion

Groups 1–10 did not merely produce a feature checklist. Together with this closure pass they now provide an implementation-level map of the launcher architecture, data sources, persistence, settings, UI/focus behavior, Android integrations, vendor adapters, wallpaper/media pipeline, and supporting actions.

The remaining unknowns are not architectural blockers.

The strongest remaining native relationship—between `libptutils`, `DisplayProfileManager.rps()` and the runtime `s0` value—is deliberately recorded as an inference rather than falsely promoted to a proven JNI call chain. It is also unnecessary for Aerial-Rows because we can initialize legitimate repositories explicitly and own our backup format.

Therefore:

> **Projectivy 4.72 is sufficiently closed at the behavioral/architectural level to proceed to the Aerial-Rows implementation architecture and staged build plan.**

## Next group

**Group 12 — Aerial-Rows implementation map and build order**

That group should map the verified Projectivy subsystems onto the current `Aerial-Rows` codebase and decide, file by file and component by component:

- what stays;
- what gets refactored;
- what gets replaced;
- what new interfaces/models/repositories are needed;
- dependency/build order;
- migration path from current Couchy-derived category/app structure;
- where Aerial provider/video background integration belongs;
- which optional Projectivy expert tools should be deferred;
- the first implementation commits to make on the development branch.
