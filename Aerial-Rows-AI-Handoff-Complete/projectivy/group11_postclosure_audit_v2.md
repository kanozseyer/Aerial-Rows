# Projectivy 4.72 — Group 11 Post-Closure Re-Audit v2

**Audit namespace:** `AUDIT_11_R2`  
**Process contract:** v18  
**Target APK:** Projectivy 4.72 / code 97  
**APK SHA-256:** `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
**Original Group 11 SHA-256:** `4f9c9bd36edba833c20b7964a12aae1182396ebde85e74aceca3da7396c4caca`

## Executive result

The original Group 11 closure remains **architecturally correct**.

Fresh verification found **no hidden second launcher subsystem**, no native implementation of the core home architecture, and no additional vendor/device architecture that invalidates Groups 1–10.

The fresh pass did, however, improve several details that were either intentionally left coarse in the original Group 11 report or only became known in later post-closure audits.

### Final assessment

- Core Group 11 architecture: **correct**
- Missing major subsystem: **none**
- Factual architectural reversals: **none**
- Canonical REA evidence: **22/22 success**
- Closure matrix: **11/11 covered**
- Stale canonical temp files: **0**

## 1. Autostart legacy sentinels can now be stated exactly

The original Group 11 correctly classified `-2` and `-101` as compatibility/internal values rather than current selectable startup targets.

Fresh canonical `dq3.b(Context)` evidence makes that more precise:

```text
-2
 └─ passes the literal persisted source string to the same yb3.n(...) app resolver
    used by ordinary application startup

-101
 └─ has no active launch branch
    └─ falls through to false
```

So the canonical interpretation is:

- `-2` = legacy/application-resolver compatibility behavior;
- `-101` = inactive/failure sentinel;
- neither should become a first-class Aerial-Rows autostart option.

## 2. `DisplayProfileManager.rps()` has one additional runtime reset

Fresh canonical evidence confirms the previous initialization behavior:

```text
load display_profiles_list
→ deserialize lz0 profiles
→ create default profile if empty
→ sort case-insensitively
→ ProjectivyAccessibilityService.s0 = 0
```

and adds another side effect:

```text
vo2.H0 = ""
```

The previous architectural recommendation therefore remains unchanged: Aerial-Rows should explicitly initialize its profile repository/state in normal Kotlin application startup rather than reproducing Projectivy's opaque bootstrap coupling.

Fresh managed xrefs still report **zero managed Java callers** for `DisplayProfileManager.rps()`.

That strengthens—but does not prove—the original conclusion that the exact invocation path crosses an opaque bootstrap/protection/native boundary.

## 3. Backup/runtime `s0` coupling remains bounded, not fully explained

Canonical `s71.b(byte[])` still extracts the salt prefix using:

```text
ProjectivyAccessibilityService.s0 + 16
```

and then treats the next 16 bytes as the AES-CBC IV.

Because `rps()` sets `s0 = 0`, normal initialized runtime strongly implies the effective salt prefix is 16 bytes.

Fresh evidence does **not** reveal a normal managed caller for `rps()`, so the exact bootstrap chain remains deliberately unproven.

This is still non-blocking for Aerial-Rows. We should own an explicit versioned backup format rather than duplicate this coupling.

## 4. JNI/native boundary remains narrow

Fresh direct inspection of the shipped ARM64 `libptutils.so` confirms exports:

```text
Java_com_spocky_projengmenu_PTApplication_vc
Java_com_spocky_projengmenu_PTApplication_vs
```

Fresh canonical managed evidence confirms:

```text
PTApplication.f()
    → ensure/load ptutils
    → PTApplication.vs(...)
```

and fresh managed xrefs still show `PTApplication.f()` has only two managed call sites:

```text
v.o(...)
ya3.o(...)
```

No new evidence moves any of these systems into native code:

- home row/category rendering;
- installed-app discovery;
- TV Provider/Watch Next;
- media/Now Playing;
- wallpaper renderer/provider architecture;
- settings hierarchy;
- TV input repository/launch architecture.

`libptutils.so` should therefore still be treated as a narrow initialization/validation/protection/utility boundary, not a native launcher implementation.

## 5. Guided APK installer remains a separate optional maintenance tool

Fresh `InstallAppsActivity.G(String)` evidence confirms:

- `MANAGE_UNKNOWN_APP_SOURCES` handling for Projectivy itself;
- URL normalization;
- HTTP/FTP/storage/USB-oriented source handling;
- `.gz` handling;
- APK MIME type `application/vnd.android.package-archive`;
- asynchronous install/download helpers.

This remains independent of normal app launch/uninstall/App Info behavior.

Recommended Aerial-Rows placement remains an optional package-maintenance module, not `AppRepository`.

## 6. Root package maintenance is now mapped more precisely

The original report correctly identified freeze/unfreeze and stock-launcher maintenance as root/privileged utilities.

Fresh canonical `kx3` evidence closes the exact command pairs.

### Disable/remove

`kx3.j(...)` chooses between:

```text
pm disable-user --user 0 [PACKAGE]
```

and:

```text
pm uninstall --user 0 [PACKAGE]
```

### Reverse/reinstall

`kx3.g(...)` chooses between:

```text
pm enable [PACKAGE]
```

and:

```text
cmd package install-existing [PACKAGE]
```

The `CleanupActivity → q90 → p90` path uses these helpers for freeze/unfreeze operations.

`RemoveLauncherActivity` remains a separate expert maintenance flow and must not be confused with legitimate Android HOME-role/default-launcher setup.

## 7. Google Play self-update blocking is now fully distinguished from the automatic Play workaround

The original Group 11 intentionally stopped at the privilege boundary for `BlockGooglePlaySelfUpdateActivity`.

Fresh `tx.i0()` + canonical `sx.h()` evidence now closes the low-level behavior.

### Manual block

The privileged helper runs:

```text
mkdir /data/app/com.android.vending-1 ;
touch /data/app/com.android.vending-1/base.apk &&
chattr +i /data/app/com.android.vending-1/base.apk
```

### Manual unblock

It runs:

```text
chattr -i /data/app/com.android.vending-1/base.apk
```

### Important distinction

This is **not** the same as the later Group 9 v3 foreground compatibility workaround.

The automatic Play Store foreground path uses:

```text
setprop sys.cts.running 1
```

and later resets it to `0` through the same privileged helper family.

So Projectivy contains two distinct privileged Play-related behaviors:

```text
Manual self-update blocker
    → immutable dummy Play base.apk path

Automatic foreground compatibility workaround
    → sys.cts.running property toggle
```

Neither belongs in ordinary Aerial-Rows launcher core.

## 8. Xiaomi updater foreground compatibility belongs in the same optional vendor layer

Fresh `nm3.i(...)` evidence confirms the foreground observer also tracks:

```text
com.xiaomi.mitv.upgrade
```

Entering that foreground package sets internal state; leaving schedules a delayed compatibility helper.

This behavior was discovered after the original Group 11 closure in Group 9 v3.

It does not create another subsystem. It strengthens the recommended architecture:

```text
VendorCompatibilityAdapters
├── XiaomiStartupPreconditioner
├── PlayStoreCompatibilityWorkaround
├── XiaomiUpdaterCompatibilityWorkaround
└── other device-specific expert helpers
```

with all of them optional and separated from generic Accessibility/HOME behavior.

## 9. GApps removal now has an exact helper boundary

The original Group 11 correctly classified `GappsUninstallActivity` as an optional privileged maintenance utility.

Fresh `kq1.i0()` + `sx.h()` evidence closes its execution target:

```text
/system/bin/gapps-uninstall.sh
```

executed through `kx3`'s privileged shell path.

This is not launcher architecture and should not be recreated in Aerial-Rows core.

## 10. Web Remote classification remains correct

Fresh wrapper evidence plus discovery of the `gt` helper continues to support:

```text
local IP
   ↓
http://mireco.hopto.org/#<local-ip>
```

There is still no evidence of a local Projectivy HTTP server behind `WebRemoteActivity`.

If Aerial-Rows ever gets remote administration, it should be independently designed.

## 11. Vendor settings/display adapters remain wrappers and capability backends

Fresh evidence for the named MiTV/FengOS wrappers plus `b5.a()` confirms the earlier architecture.

The display backend factory probes a bounded candidate set including:

- Amlogic/Droidlogic `SystemControlManager`;
- Amlogic `TvControlManager`;
- Xiaomi picture backend.

The best available backend is selected by capability score.

No separate hidden vendor-control architecture appeared.

Normal Nvidia Shield / standard Android TV operation should remain independent of those classes.

## 12. High-affinity obfuscated helpers remain support machinery, not hidden feature layers

Fresh canonical class evidence for representative helpers such as:

```text
iw4
em3
b5
sx
nm3
kx3
q90
p90
```

supports the original Group 11 classification:

- platform/file/provider/accessibility helpers;
- synthetic/compiler dispatchers;
- vendor/privileged workers;
- guided-screen delegates;
- shared utility machinery.

High reference count still does not imply a missing Projectivy-owned subsystem.

## 13. Updated optional-maintenance architecture for Aerial-Rows

The earlier `DeviceMaintenanceTools` recommendation should now be split more explicitly:

```text
OptionalExpertTools
│
├── PackageMaintenance
│   ├── APK installer
│   ├── package disable/enable
│   ├── package uninstall/install-existing
│   ├── stock-launcher maintenance
│   └── GApps removal [do not include by default]
│
├── VendorCompatibility
│   ├── Xiaomi startup preconditioning
│   ├── Play Store foreground workaround
│   ├── Xiaomi updater workaround
│   └── vendor engineering/settings launchers
│
├── Diagnostics
│   ├── device/build/display information
│   ├── TV-input compatibility controls
│   └── optional root/sysfs diagnostics
│
└── RemoteManagement
    └── independently designed if ever needed
```

None of these should be required by:

```text
HomeRepository
AppRepository
TvProgramRepository
MediaRepository
WallpaperRepository
InputRepository
SettingsRepository
```

## 14. Updated closure assessment

The post-closure pass did **not** discover:

- a second home renderer;
- a second app repository;
- a second TV-program/channel engine;
- a hidden native wallpaper engine;
- a second input architecture;
- a second persistence/settings engine;
- a local Web Remote server;
- a native implementation of the core launcher.

The remaining opaque relationship is still the bootstrap path that invokes `DisplayProfileManager.rps()` / initializes `s0`.

That ambiguity remains explicitly bounded and is unnecessary to reproduce.

## 15. Verification and closure status

Canonical REA evidence:

```text
22 / 22 success
errors  0
invalid 0
```

The v18 closure matrix is fully satisfied:

```text
entry points                 PASS
base classes / inheritance   PASS
all DEX files                PASS
obfuscated collaborators     PASS
models / data flow           PASS
persistence                  PASS
resources / XML              PASS
Android framework APIs       PASS
callbacks / observers        PASS
launch actions / lifecycle   PASS
native / JNI                 PASS
```

One canonical probe was interrupted by the outer command limit. The v18 write-ahead batch state recorded the exact in-progress item, recovered it on the next run, and no successful probes were repeated. Its orphan atomic temp residue was removed before synthesis.

## Final conclusion

The original Group 11 conclusion remains valid:

> **Projectivy 4.72 is sufficiently closed at the behavioral and architectural level to proceed with the Aerial-Rows implementation architecture without expecting a hidden core subsystem to invalidate the plan.**

The post-closure pass improves precision around legacy startup values, profile bootstrap side effects, privileged package commands, Play/GApps maintenance and later Xiaomi/Play compatibility helpers, but does not change the core reconstruction architecture.

Group 12 can therefore remain the implementation-map/build-order layer, updated with these refined optional expert-tool boundaries.
