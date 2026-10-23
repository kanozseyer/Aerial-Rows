# Projectivy 4.72 — Group 09: HOME replacement, boot/autostart, accessibility and remote/key handling

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group closes four tightly related Projectivy subsystems:

1. becoming / detecting the Android HOME launcher;
2. boot and resume-from-standby autostart behavior;
3. `ProjectivyAccessibilityService` and its fallback HOME override;
4. remote/key gestures, direct input hotkeys, idle/power actions and Activity-level key behavior.

The TV-input implementation itself was mapped in Group 08; this group only covers how HOME/accessibility/key handling invokes it.

---

## Analysis workflow and reliability improvements

Group 09 used the persistent cache/semantic graph rather than rescanning the whole APK. Named entry points were used as graph seeds:

- `MainActivity`
- `ProjectivyAccessibilityService`
- `StartUpBootReceiver`

The relevant obfuscated cluster was then ranked by semantic matches + graph distance. Missing central classes were batch-exported in one JADX load.

Two process improvements were added during this group:

### Multi-field single-load preference tracer

The old field-use helper reopened the APK for every `hj3` field. Group 09 added `JadxFieldUsesBatch`, which loads the APK once and traces many preference fields in a single run.

### Machine-verifiable critical assertions

A verification artifact now checks critical constants/control paths directly against extracted Java/smali. Group 09 currently passes **11/11** assertions, including:

- 300 ms long-press timing;
- 400 ms double-press timing;
- 15-second autostart debounce;
- 90-second boot autostart window;
- wake/standby autostart path;
- AccessibilityService event/flag configuration;
- Android HOME RoleManager API usage;
- HDMI1–4 hotkey mapping;
- double-BACK ambient behavior;
- Somnambulator launch;
- accessibility-generated HOME intent.

Evidence files are hashed in `re_cache/group09/evidence_manifest.json`, and discovery, verification and report are independently checkpointed.

---

# 1. HOME launcher boundary

`MainActivity` is the Projectivy HOME activity. Its manifest filter contains the Android TV/launcher categories established in Group 01, including:

- `HOME`
- `LAUNCHER`
- `LEANBACK_LAUNCHER`
- `LAUNCHER_APP`
- Fire OS `MAIN_HOME`

The Activity is configured as a launcher shell rather than a normal task-oriented application Activity: single-task, removed/excluded from Recents and with HOME-oriented task behavior.

## Detecting whether Projectivy is the current HOME

`MainActivity` has a lazy boolean that resolves the current `MAIN + HOME + DEFAULT` activity through `PackageManager` and compares its package to:

```text
com.spocky.projengmenu
```

This provides a direct answer to "is Projectivy currently the device HOME?" rather than assuming that being launched means it owns HOME.

---

# 2. Android HOME Role API

Projectivy uses Android's `RoleManager` when available.

The `yb3 / wb3` path explicitly calls:

```text
RoleManager.isRoleAvailable("android.app.role.HOME")
RoleManager.createRequestRoleIntent("android.app.role.HOME")
RoleManager.isRoleHeld("android.app.role.HOME")
```

The General settings screen exposes a **Change default launcher** action that goes through this role-request pathway.

This should be reproduced rather than relying on vendor-specific Settings intents when RoleManager is available.

## Restarting into the HOME task

`yb3.v(context)` resolves Projectivy's launch intent and uses:

```text
Intent.makeRestartActivityTask(component)
    .addCategory(Intent.CATEGORY_HOME)
```

It then starts that task and finishes the previous Activity when appropriate.

This is used after settings/language changes that require Projectivy to re-enter as the HOME task.

---

# 3. `general_override_home` and Accessibility fallback

Projectivy has a second mechanism in addition to being the official HOME app:

```text
general_override_home
```

The corresponding runtime field is copied into `ProjectivyAccessibilityService.p0`.

The service also enumerates:

1. the current default HOME Activity;
2. all Activities responding to `MAIN + HOME`.

If the resulting set is empty, or the only entry is Android's system `FallbackHome`, Projectivy forces the runtime HOME override on:

```text
p0 = true
```

even if the stored preference is off.

This means Projectivy's AccessibilityService can act as a resilience/fallback layer when Android does not have a usable normal launcher selected.

---

# 4. Boot receiver — important correction

The manifest declares `StartUpBootReceiver` for:

```text
android.intent.action.BOOT_COMPLETED
```

However, in the exact Projectivy 4.72 / versionCode 97 APK, the receiver itself is effectively a **no-op**.

Its complete `onReceive()` does only:

```text
intent?.action == BOOT_COMPLETED
```

and then returns. It does not launch Projectivy, an app, an input, or a service.

Therefore Projectivy's actual boot/autostart behavior must not be reconstructed as:

```text
BOOT_COMPLETED
    -> receiver starts selected app/input
```

That would be incorrect for this build.

---

# 5. Actual boot/autostart architecture

The real dispatcher is:

```text
dq3.z(context, bootWindow)
    -> dq3.b(context)
```

## Startup debounce

`dq3.z()` rejects another startup action if fewer than:

```text
15,000 ms
```

have elapsed since the previous attempt.

The timestamp is stored on `StartUpBootReceiver.a`, even though the receiver does not itself perform the launch.

This protects against MainActivity and AccessibilityService independently triggering the same startup behavior.

## Boot-only time window

When called with:

```text
bootWindow = true
```

Projectivy refuses to perform the startup action if:

```text
SystemClock.elapsedRealtime() > 90,000 ms
```

So the boot-autostart route is intentionally confined to approximately the first **90 seconds after Android boot**.

## Configurable startup delay

Preference:

```text
power_autostart_delay
```

If zero:

```text
dq3.b(context)
```

runs immediately.

Otherwise Projectivy posts it to the main looper after the configured delay in seconds.

---

# 6. What can be autostarted

Preference:

```text
power_autostart_source
```

is interpreted by `dq3.b()`.

Verified useful modes are:

### Application package

A package-name value resolves through Projectivy's normal app-launch resolver:

```text
Leanback launch intent
    -> normal launcher intent fallback
```

and can pass through the parental-control gate.

### Last-used TV input

Special value:

```text
-1
```

launches the `input_last_used` value through the Group 08 `ed4` TV-input subsystem.

### Screensaver / ambient mode

Special value:

```text
-100
```

calls `yb3.B(context)`, which launches:

```text
com.android.systemui/com.android.systemui.Somnambulator
```

with `FLAG_ACTIVITY_NEW_TASK`.

### Explicit TV input

Numeric values matching a `dd4` source resolve to the corresponding input and are launched by `ed4`.

The implementation therefore supports app, last input, explicit HDMI/AV input and ambient/screensaver startup from one setting.

Some additional numeric branches exist in the obfuscated dispatcher but are not needed to reproduce the verified user-visible modes and are intentionally not assigned speculative meanings here.

---

# 7. MainActivity boot trigger

`MainActivity.x()` is one real boot/startup trigger.

If:

```text
general_onboarding_completed == true
```

it invokes:

```text
dq3.z(this, true)
```

Otherwise it launches `OnboardingActivity`.

Thus when Projectivy is started as HOME shortly after Android boot, it can dispatch the configured autostart action.

---

# 8. AccessibilityService boot trigger

`ProjectivyAccessibilityService.onServiceConnected()` also checks:

```text
SystemClock.elapsedRealtime() < 90,000 ms
```

and launches an early-start coroutine.

That coroutine eventually invokes:

```text
dq3.z(service, true)
```

The 15-second debounce in `dq3.z()` prevents MainActivity and AccessibilityService from performing duplicate startup actions.

If startup does not occur and the Accessibility HOME-override is active, the service can post its internal HOME-launch runnable instead.

This is a much more robust design than placing all responsibility on `BOOT_COMPLETED`.

---

# 9. Resume-from-standby autostart

This is separate from boot startup.

Preference:

```text
power_autostart_leaving_standby
```

is observed by `nm3`.

When its wake/standby signal becomes true:

```text
if power_autostart_leaving_standby:
    dq3.z(service, false)
```

Passing `false` means the 90-second boot window does **not** apply.

So the architecture explicitly distinguishes:

```text
cold boot startup
    vs
resume-from-standby startup
```

while sharing the same final startup dispatcher.

---

# 10. AccessibilityService manifest/configuration

`ProjectivyAccessibilityService` is a real Android `AccessibilityService` protected by:

```text
android.permission.BIND_ACCESSIBILITY_SERVICE
```

Its XML declares:

```text
canRetrieveWindowContent = true
canRequestFilterKeyEvents = true
```

and points its settings activity to Projectivy's settings UI.

## Runtime service configuration

`d()` reads these preferences into runtime state:

```text
remotecontrol_show_menu
remotecontrol_decrease_input_lag
remotecontrol_bind_mic_btn_to_assistant

general_auto_fix_gplay
general_override_home

power_simulate_user_activity
power_internal_idle_detection
power_internal_idle_timeout
power_internal_idle_action
power_hdmi_input_exit_detection
power_internal_idle_excludes_media_playback
power_poweroff_mode

accessibility_alternate_config
accessibility_button_remapping
accessibility_volume_fix
```

The service then sets:

```text
eventTypes = 40
notificationTimeout = 0
```

`40` is the combination of the two event types it subsequently handles:

- focused-view changes;
- window-state changes.

Normal service flags:

```text
66
```

When accessibility button remapping is enabled:

```text
98
```

which adds the key-filtering capability.

Feedback type is selected between two modes according to the alternate-config preference.

---

# 11. Accessibility event handling

## Window-state changes

Projectivy tracks the foreground package/class as an `im3` value.

That state is then used for several behaviors:

- HOME override detection;
- idle/power logic;
- package-specific fixes;
- TV-input exit detection;
- Play Store workaround state;
- parental-control handling.

User-activity time is updated whenever a relevant window change occurs.

## Focus events

For specific configured package paths, focused `AccessibilityNodeInfo` trees can be inspected asynchronously.

The service includes recursive node-text extraction and Projectivy string/resource translation helpers, demonstrating that its accessibility integration goes beyond simple HOME-key interception.

---

# 12. Projectivy key-event engine

The remote remapper is a distinct subsystem:

```text
x00
 ├─ v00 ButtonEvent
 └─ w00 PressType
```

`w00` defines exactly:

```text
DOWN
PRESS
LONGPRESS
DBLPRESS
```

A binding key is stored as:

```text
pressType.ordinal * 10000 + keyCode
```

## Timing

Verified from `x00.e(KeyEvent)`:

```text
long-press dispatch: 300 ms

double-press window: 400 ms
```

If a key has a double-press binding, ordinary PRESS can be delayed until the 400 ms ambiguity window expires.

This timing should be reproduced explicitly rather than relying on Android's default long-click timing if Projectivy-like remote behavior is desired.

---

# 13. Verified key bindings

The AccessibilityService registers these bindings conditionally according to user preferences/capabilities.

| Key | Gesture | Behavior |
|---|---|---|
| Volume Up (24) | PRESS | volume-filter fix path when enabled |
| Volume Down (25) | PRESS | volume-filter fix path when enabled |
| Voice Assist (231) | PRESS | assistant broadcast path when enabled |
| synthetic/fallback key 0 | PRESS | assistant broadcast path |
| Menu (82) | LONGPRESS | toggle Projectivy low/high input-lag mode |
| Menu (82) | DBLPRESS | show temporary Projectivy remote/hotkey help mode |
| DPAD Up (19) | PRESS | previous display profile while hotkey mode is active |
| DPAD Down (20) | PRESS | next display profile while hotkey mode is active |
| DPAD Left (21) | PRESS | previous available TV input while hotkey mode is active |
| DPAD Right (22) | PRESS | next available TV input while hotkey mode is active |
| DPAD Center (23) | PRESS | exit/confirm hotkey mode |
| HOME (3) | PRESS | bring Projectivy HOME forward when HOME override is active |
| HOME (3) | DBLPRESS | Android global Recents action when HOME override is active |
| 243 | PRESS | HDMI1 |
| 244 | PRESS | HDMI2 |
| 245 | PRESS | HDMI3 |
| 246 | PRESS | HDMI4 |

The HDMI mapping is verified against `dd4`:

```text
dd4.B = HDMI1
dd4.C = HDMI2
dd4.D = HDMI3
dd4.E = HDMI4
```

---

# 14. Temporary remote "hotkey mode"

The Menu double-press helper calls `ProjectivyAccessibilityService.l()`.

It shows a helper message describing:

```text
◄/►
▲/▼
⦿
Projectivy
```

sets the temporary hotkey-mode flag, and schedules it to clear after:

```text
3 seconds
```

While this mode is active:

- UP/DOWN cycle display profiles;
- LEFT/RIGHT cycle enabled/available TV inputs;
- CENTER exits/confirms the mode.

The consume predicates for these DPAD bindings are conditional on the temporary mode, so Projectivy does not permanently steal navigation keys from other applications.

---

# 15. HOME override behavior

When Accessibility HOME override is active, HOME PRESS:

1. clears/resets Projectivy's current home-card selection state;
2. posts a runnable that launches Projectivy's HOME intent;
3. adds `FROM_HOME_KEY=true`;
4. consumes the key event.

The HOME Intent is lazily generated as:

```text
Projectivy package launch intent
```

with a fallback explicit `MainActivity` Intent carrying:

```text
CATEGORY_HOME
```

HOME DBLPRESS performs Android accessibility global action `3`, i.e. the Recents action.

This is the core of Projectivy's optional Accessibility-based HOME-button override.

---

# 16. Assistant-button remapping

When `remotecontrol_bind_mic_btn_to_assistant` is enabled, Projectivy binds the voice-assistant key and a fallback key path.

Those actions post an internal runnable that broadcasts Projectivy's stored:

```text
android.intent.action.ASSIST
```

Intent.

This keeps assistant invocation separate from the HOME/input/profile hotkey engine.

---

# 17. Input-lag hotkey

When `remotecontrol_decrease_input_lag` is enabled, LONGPRESS MENU invokes the input-lag toggle path.

The action flips Projectivy's runtime state and calls the display/input-lag tool mapped in Group 08.

The visible result uses the Projectivy strings corresponding to:

```text
input lag low
input lag high
```

This is a remote shortcut around the Group 08 privileged/device-specific tuning layer.

---

# 18. Accessibility volume fix

When:

```text
accessibility_volume_fix
```

is enabled, pressing the configured volume path calls `ProjectivyAccessibilityService.i()`.

That method temporarily clears AccessibilityService flag bit `32` (request-filter-key-events), applies the new service info, waits:

```text
5 seconds
```

and then re-adds bit `32`.

In practical architectural terms, Projectivy temporarily stops Accessibility key filtering so volume handling can pass through normally, then automatically re-enables its key interception.

---

# 19. MainActivity's own key handling

Not all remote behavior belongs to AccessibilityService.

`MainActivity` handles several keys directly while Projectivy itself is foreground.

## HOME

`KEYCODE_HOME` calls:

```text
MainFragment.y0()
```

which belongs to the Group 02 focus-restoration path.

## MENU

MENU DOWN calls `startTracking()` and consumes the event.

A normal non-long-press MENU UP opens Projectivy's settings/menu side panel through the existing side-panel dispatcher.

## DPAD

UP/DOWN/LEFT/RIGHT are also fed into Projectivy's local directional-input tracker.

## Media Play/Pause

A repeated `KEYCODE_MEDIA_PLAY_PAUSE` can emit wallpaper:

```text
Event.TimeElapsed
```

when the active wallpaper provider accepts that event.

This is a small but useful bridge between remote input and the Group 06 wallpaper-provider event model.

## User interaction

`onUserInteraction()` updates Projectivy's local last-interaction timestamp.

---

# 20. Double-BACK ambient/screensaver gesture

Preference:

```text
remotecontrol_double_back_ambient_mode
```

is implemented in the Activity back callback (`jm1`).

If disabled, or if the previous BACK was at least 400 ms ago, Projectivy performs its normal back behavior:

- if a row/card is not at its initial position, restore/refocus the relevant home position;
- otherwise use normal HOME/back behavior;
- finish Projectivy only when its HOME-override/default-HOME conditions permit it.

If a second BACK occurs within:

```text
< 400 ms
```

Projectivy calls:

```text
yb3.B(MainActivity)
```

which launches Android SystemUI's:

```text
com.android.systemui.Somnambulator
```

Therefore **double BACK enters ambient/screensaver mode**.

This is independent of HOME-button remapping.

---

# 21. Launcher idle mode and remote wake

`MainActivity` has a modal/idle-state gate.

While the launcher is in its idle presentation state, incoming key/touch DOWN events are consumed. On release, `MainActivity.F()`:

1. records user activity;
2. clears the idle-state flag;
3. sends wallpaper event:

```text
LauncherIdleModeChanged(false)
```

4. fades Projectivy overlays/title/status UI back in.

This closes an important loop with Group 06: Aerial/video wallpaper can enter an immersive idle presentation and ordinary remote interaction can return the same renderer to active-launcher presentation.

---

# 22. Internal idle detection

AccessibilityService also participates in Projectivy's internal idle/power system.

It maintains a global last-user-activity timestamp updated by key and window events.

Preferences involved include:

```text
power_internal_idle_detection
power_internal_idle_timeout
power_internal_idle_action
power_internal_idle_excludes_media_playback
power_poweroff_mode
```

The idle check can exclude situations where:

- a real media session is actively playing;
- notification/media state says playback is active;
- Projectivy's internal TV Activity is active;
- other configured foreground-app conditions apply.

This connects Group 05 media-session state to power/idle behavior.

---

# 23. Idle/power actions

`kg` defines the configured idle actions:

```text
POWER_OFF
SCREEN_OFF
SCREENSAVER
```

Availability is Android-version/capability gated where needed.

For power-off handling, `lg` defines:

```text
POWER_DIALOG
POWER_DIALOG_TWICE
POWER_DIALOG_OK
```

and `ProjectivyAccessibilityService.j()` uses Accessibility global actions to perform the selected behavior.

This is another reason Projectivy's AccessibilityService is a substantial system-integration component rather than merely a HOME-button hook.

---

# 24. Settings UI connections

Group 09 explicitly verified the settings surfaces corresponding to these runtime behaviors.

## Remote Control

The Remote Control settings expose:

```text
remotecontrol_show_menu
remotecontrol_double_back_ambient_mode
remotecontrol_decrease_input_lag
remotecontrol_bind_mic_btn_to_assistant
```

Device/capability checks hide unsupported options.

## Accessibility

The Accessibility settings expose status rows for both:

- service enabled;
- service actually started.

and options including:

```text
accessibility_alternate_config
accessibility_button_remapping
accessibility_volume_fix
accessibility_service_enable_reminder
```

## General

General settings expose:

- Change default launcher;
- `general_override_home`.

## Power

The Group 07 Power settings feed the autostart, standby, idle-detection and power-action paths described above.

---

# 25. Correct reconstruction architecture for Aerial-Rows

Projectivy should not be copied as one monolithic AccessibilityService. The behavior naturally separates into portable components.

```text
HomeRoleCoordinator
 ├─ isHomeRoleAvailable()
 ├─ isHomeRoleHeld()
 ├─ requestHomeRole()
 ├─ resolveCurrentHome()
 └─ restartAsHome()

StartupCoordinator
 ├─ startupTarget
 ├─ configurable delay
 ├─ 15s duplicate suppression
 ├─ boot-window policy
 ├─ wake-from-standby policy
 └─ dispatch app / input / ambient

RemoteGestureEngine
 ├─ DOWN
 ├─ PRESS
 ├─ LONGPRESS (300ms)
 ├─ DBLPRESS (400ms)
 └─ conditional consume predicates

ProjectivyAccessibilityBridge (optional)
 ├─ HOME override
 ├─ remote remapping
 ├─ foreground-window observation
 ├─ volume-filter workaround
 └─ privileged/global power actions

LauncherIdleCoordinator
 ├─ last interaction
 ├─ media-aware idle policy
 ├─ LauncherIdleModeChanged
 └─ Aerial immersive presentation
```

The Accessibility bridge should remain optional: normal HOME, app launching, TV Provider, rows, wallpaper/Aerial playback and ordinary input cards should not depend on it.

---

# 26. Specific Aerial-Rows recommendations

## Preserve Projectivy's separation of normal HOME and Accessibility override

Prefer:

```text
Android HOME role = primary mechanism
Accessibility HOME override = optional fallback / power-user feature
```

Do not make AccessibilityService mandatory just to use the launcher.

## Reuse one remote gesture engine

Implement the 300/400 ms behavior centrally so Activity and Accessibility bindings don't develop different timing semantics.

## Keep Aerial idle transition in the wallpaper engine

Group 06 + Group 09 together imply:

```text
normal launcher
    ↓ idle timeout
LauncherIdleModeChanged(true)
    ↓
Aerial provider/video remains on same renderer
    ↓ immersive presentation

remote interaction
    ↓
LauncherIdleModeChanged(false)
    ↓
rows/title/status fade back in
```

This is preferable to launching a second video Activity/screensaver player.

## Keep privileged power/display fixes modular

Nvidia Shield and normal Android TV operation should work without:

- Accessibility HOME override;
- root;
- sysfs access;
- Xiaomi/Amlogic engineering paths.

Those belong behind capability-driven adapters.

---

# 27. Confidence / unresolved edges

### High-confidence, independently verified

- RoleManager HOME support;
- default-HOME resolution;
- boot receiver no-op behavior;
- actual autostart dispatcher;
- 15-second startup debounce;
- 90-second boot window;
- app/input/last-input/ambient startup targets;
- standby-resume startup;
- accessibility config flags/events;
- HOME fallback activation when only FallbackHome exists;
- key gesture timings;
- HOME/MENU/DPAD/HDMI bindings;
- double-BACK Somnambulator behavior;
- volume-fix flag cycling;
- launcher idle wake path.

### Intentionally not assigned speculative semantics

A couple of obscure numeric autostart branches exist in `dq3.b()` but are not necessary to describe the verified visible startup choices. They should be revisited only if later persistence/backup evidence exposes a user-visible option that depends on them.

No evidence required treating `StartUpBootReceiver` as an active launcher in this exact build; smali proves the opposite.

---

# Group 09 conclusion

Projectivy's HOME/system integration is deliberately redundant:

```text
Android HOME role
      +
MainActivity HOME task behavior
      +
optional Accessibility HOME override
      +
early-boot Accessibility startup path
      +
MainActivity early-boot startup path
      +
shared 15s autostart debounce
```

This makes the launcher resilient across Android TV devices where OEM HOME behavior, boot timing and source switching differ.

The key implementation lesson for Aerial-Rows is to reproduce that **layering**, not to put everything inside a BootReceiver or AccessibilityService.

**Group 09 status: COMPLETE + VERIFIED.**

Next planned group: **Group 10 — persistence, backups, themes/config serialization, icons, shortcuts and parental controls.**
