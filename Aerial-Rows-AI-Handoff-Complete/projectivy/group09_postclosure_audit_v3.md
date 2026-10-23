# Projectivy 4.72 — Group 09 post-closure re-audit v3

Status: **COMPLETE — POST-CLOSURE RE-AUDITED v3**  
Reference APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Verdict

The Group 9 architecture remains correct. Projectivy still uses a layered system rather than a hidden second HOME/accessibility/remote subsystem:

```text
platform HOME role / HOME task
        +
optional Accessibility HOME override
        +
MainActivity startup trigger
        +
Accessibility startup trigger
        +
shared dq3 startup/autostart dispatcher
        +
wake/standby foreground-state refresh
        +
remote gesture engine
        +
launcher idle/power coordination
```

The v2 re-audit remains valid. The fresh v3 audit found **two genuine completeness misses** and **one smaller Xiaomi-specific foreground workaround**.

## Process hardening before/during v3

This audit was run under the machine-enforced closure process and also improved that process further:

1. persistent MCP session reuse + in-process JADX batch discovery fast path;
2. representative two-query discovery reduced from about 1.54 s to 0.81 s while retaining isolated subprocess fallback;
3. closure-version invariant fixed so later process versions do not invalidate the v7 closure-introduction rule;
4. FTS5 searches for dotted package/property identifiers now retry safely as literal phrases instead of crashing;
5. named-package REA targets are prevalidated against the pinned complete 7,395-class inventory before JVM startup, while default-package synthetic helpers remain pass-through because JADX may resolve them even when absent from `list_classes`;
6. failed probe quarantine remained effective: one pre-fix receiver-package typo was isolated and the corrected canonical probe succeeded;
7. Group 9 v3 was not grandfathered: all 11 closure dimensions were required and passed.

Canonical v3 verification: **19/19 evidence artifacts semantically valid, 0 canonical errors**.

## 1. New completeness miss — `power_simulate_user_activity` is a dual-path feature

The original Group 9 only listed the setting. It is substantially more specific.

`hj3.T` is:

```text
power_simulate_user_activity
```

with a default of `false`. `ProjectivyAccessibilityService.d()` loads it into the runtime flag `g0`.

### 1A. Xiaomi startup preconditioning

The Accessibility startup coroutine (`mm3.o()`, case 0) does:

```text
if power_simulate_user_activity
  and com.xiaomi.mitv.settings is installed:
      AccessibilityService.performGlobalAction(2)   // HOME
      wait 1 second

then:
      dq3.z(service, true)
      if no startup action occurred and HOME override is active:
          post Projectivy HOME runnable
```

So one purpose is Xiaomi-specific startup preconditioning immediately before the normal boot-autostart dispatcher.

### 1B. Recurring 60-second Accessibility-node action

The same setting also controls a completely separate runtime path.

When `g0` is enabled, `ProjectivyAccessibilityService.d()` schedules its `Q` runnable. `Q` is `km3(service,1)`.

That runnable:

```text
get Accessibility windows
      ↓
select a root node
      ↓
root.performAction(16384)
      ↓
post same runnable again after 60,000 ms
```

Therefore `power_simulate_user_activity` is not just a startup option. It also creates a recurring one-minute Accessibility action loop.

### Aerial-Rows implication

Do **not** reproduce this as generic Accessibility behavior. Model the intent explicitly:

```text
Idle/PowerCoordinator
      +
optional VendorStartupPreconditioner
```

If a device really needs a keepalive/workaround, it should be capability-gated and diagnosable rather than implemented through an opaque global Accessibility action.

## 2. New completeness miss — `general_auto_fix_gplay` toggles a privileged system property

The original Group 9 listed:

```text
general_auto_fix_gplay
```

but did not close its behavior.

`hj3.C` defines this setting. Its default is capability-dependent through Projectivy's GApps/device helper rather than being a universal constant.

`ProjectivyAccessibilityService.d()` loads it into static runtime flag `e0`.

The foreground observer (`nm3.i()`) then watches the Google Play Store package:

```text
com.android.vending
```

### Entering Play Store

When the fix is enabled and its state flag is inactive:

```text
sx(byte=4).b(TRUE)
S = true
```

The matching `sx` helper executes through Projectivy's privileged/root command infrastructure:

```text
setprop sys.cts.running 1
```

### Leaving Play Store

When Play Store is no longer foreground while `S` is active:

```text
post K after 2,000 ms
```

`K` is `cm3(service,1)`. When it runs, it rechecks the current foreground package. If Play Store is still no longer foreground and the workaround is still active, it calls the same helper with `FALSE`:

```text
setprop sys.cts.running 0
S = false
```

So this is a privileged **foreground-scoped CTS-property workaround**, not ordinary Play Store integration.

### Aerial-Rows implication

This should not live inside a generic launcher Accessibility bridge. If support is ever needed for a specific device, isolate it as an explicit optional privileged compatibility adapter, disabled on ordinary Android TV/Nvidia Shield installations.

## 3. Additional refinement — Xiaomi updater foreground workaround

The same Accessibility foreground observer also tracks:

```text
com.xiaomi.mitv.upgrade
```

Behavior:

```text
enter package:
    T = true

leave package while T active:
    T = false
    post L after 2,000 ms
```

The matching `e6` runnable invokes a device-specific helper using:

```text
Upgrade.UpgradeManager
http.*zip
```

This is another Xiaomi-specific compatibility action embedded in Accessibility foreground observation. It is not evidence of a separate input/device or Accessibility subsystem.

## 4. v2 architecture reverified

Fresh canonical evidence confirmed the important v2 conclusions still hold:

- `StartUpBootReceiver` is registered for boot but is not the actual launch coordinator;
- actual cold-start dispatch comes from MainActivity and/or Accessibility and converges on `dq3`;
- `dq3.z()` retains the 15-second duplicate policy and 90-second boot-window behavior documented in v2;
- `dq3.b()` remains the shared target dispatcher;
- wake/standby continues to use the same dispatcher without the 90-second boot restriction;
- `yb3.v()` remains the HOME-task restart/re-entry path;
- `x00.e()` remains the global remote gesture engine;
- `jm1.e()` remains part of the temporary profile/input hotkey path;
- `yb3.B()` / `yb3.y()` retain the system screensaver strategy/fallback chain;
- Accessibility `onAccessibilityEvent()` and `onKeyEvent()` retain the previously documented observer/remapping roles;
- Group 8 v2's correction still applies: LEFT/RIGHT hotkey cycling means previous/next **visible generic source**, not traversal of the physical TvInputManager list;
- no second hidden HOME-role, Accessibility or remote-control subsystem was found.

## 5. Closure matrix

Group 9 v3 passed every required process dimension:

| Dimension | Result |
|---|---|
| entry points | ✅ covered |
| base classes / inheritance | ✅ covered |
| all DEX files | ✅ covered |
| obfuscated collaborators | ✅ covered |
| models / data flow | ✅ covered |
| persistence | ✅ covered |
| resources / XML | ✅ covered |
| Android framework APIs | ✅ covered |
| callbacks / observers | ✅ covered |
| launch actions / lifecycle | ✅ covered |
| native / JNI | ✅ not applicable to Group 9 behavior; boundary already closed in Groups 1/11 |

The complete 7,395-class inventory spans the three DEX files. Relevant Group 9 classes resolved in `classes.dex` and `classes2.dex`; the complete inventory/semantic search found no Group 9-specific node in `classes3.dex` that changes this architecture.

## Updated Aerial-Rows target architecture

```text
HomeRoleCoordinator
 ├─ current/default HOME
 ├─ HOME role request/state
 └─ restartAsHome()

StartupCoordinator
 ├─ single cancellable pending startup job
 ├─ duplicate policy
 ├─ execution-time boot-window validation
 ├─ wake refresh + optional dispatch
 ├─ app/input targets
 └─ SystemScreensaverLauncher strategy chain

RemoteGestureEngine
 ├─ per-key gesture state
 ├─ explicit long/double timing
 ├─ explicit consume policy
 └─ protected-modal gate before global actions

AccessibilityBridge (optional)
 ├─ HOME fallback
 ├─ foreground/window observation
 ├─ optional key filtering
 └─ typed signals only

VendorCompatibilityAdapters
 ├─ XiaomiStartupPreconditioner
 ├─ PlayStoreCompatibilityWorkaround   // privileged, optional
 └─ XiaomiUpdaterCompatibilityWorkaround

InputLaunchCoordinator
 ├─ persisted canonical source state
 ├─ current runtime source state
 └─ visible-source traversal separate from physical availability

LauncherIdleCoordinator
 └─ shared wallpaper/Aerial renderer + active UI restore
```

The key design improvement is to keep vendor/root workarounds outside the generic Accessibility bridge.

## Final assessment

| Area | Result |
|---|---|
| Core Group 9 architecture | ✅ correct |
| Missing second subsystem | none |
| Major factual reversal | none |
| `power_simulate_user_activity` | **genuine miss — fully closed** |
| `general_auto_fix_gplay` | **genuine miss — fully closed** |
| Xiaomi updater observer workaround | newly documented refinement |
| v2 autostart edge semantics | reverified |
| HOME role/restart | reverified |
| remote/hotkey behavior | reverified |
| screensaver fallback | reverified |
| 11-dimension closure | ✅ 11/11 |

**Group 09: COMPLETE — POST-CLOSURE RE-AUDITED v3.**
