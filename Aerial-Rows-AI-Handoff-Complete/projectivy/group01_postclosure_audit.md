# Projectivy 4.72 — Group 01 post-closure audit

Status: **AUDITED AFTER GROUPS 2–12**
Original report: `group01.md`
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## 1. Audit conclusion

The original Group 01 report remains structurally correct. Its key counts, package identity, component boundaries, resource architecture, minification/ownership warning, HOME entry point, TV/provider/accessibility/notification/plugin boundaries and native-library inventory all survive the later implementation-level work.

However, later groups allow several early statements to be corrected or strengthened. The most important changes are:

1. `StartUpBootReceiver` is manifest-declared for `BOOT_COMPLETED`, but its 4.72 `onReceive()` is effectively a no-op. Actual autostart is coordinated elsewhere.
2. `ThemeContentProvider` should be described as a read-only theme-wallpaper content bridge for ordinary consumers, not merely as a provider that happens to override CRUD methods.
3. Declaring `WRITE_EPG_DATA` does not prove Projectivy writes TV-provider rows. Group 04 verified read/query behavior plus `requestChannelBrowsable`; no row-insertion architecture needs to be inferred from the manifest permission alone.
4. The initially-disabled HDMI/AV/source launcher components are now verified to be dynamically enabled/disabled with `PackageManager.setComponentEnabledSetting(...)`; this is no longer only an inference from the manifest.
5. The native `libptutils.so` boundary is now sufficiently closed: it is narrow initialization/validation/protection/utility code, not a hidden native implementation of the launcher core.

No Group 01 assumption was found that invalidates the Groups 2–12 architecture.

---

## 2. Corrections to apply to the original Group 01 report

### 2.1 Boot receiver wording

Original static fact:

```text
StartUpBootReceiver — BOOT_COMPLETED
```

Correct post-Group-09 interpretation:

```text
BOOT_COMPLETED manifest entry
        ↓
StartUpBootReceiver.onReceive()
        ↓
no launch/service dispatch in Projectivy 4.72
```

Actual startup/autostart dispatch is handled through the `dq3` coordinator and can be triggered from the HOME/accessibility lifecycle with duplicate and boot-window guards.

Therefore Group 01 should explicitly distinguish **manifest registration** from **runtime responsibility**.

### 2.2 ThemeContentProvider semantics

Group 01 correctly identified:

```text
com.spocky.projengmenu.themeprovider
```

and noted that the class overrides the standard ContentProvider methods.

Group 06 later established the operational role more precisely:

```text
.pltheme/current theme state
        ↓
ThemeContentProvider
        ↓
content://com.spocky.projengmenu.themeprovider/current/wallpaper/<name>
        ↓
normal wallpaper renderer
```

The provider is effectively **read-only to ordinary consumers** and exists to expose packaged theme-wallpaper media as normal Android content URIs. The mere presence of `insert/update/delete` overrides must not be read as evidence of a mutable public theme database.

### 2.3 TV-provider permission wording

The manifest declares:

```text
READ_TV_LISTINGS
READ_EPG_DATA
WRITE_EPG_DATA
```

Group 04 verified Projectivy's actual launcher path as:

```text
query channels
query preview programs
query Watch Next
requestChannelBrowsable(...)
```

No hidden requirement to create/update/delete TV-provider program rows was discovered.

The improved Group 01 statement should therefore be:

> Projectivy declares both read and write EPG capabilities, but the reconstructed home/channel architecture is read-oriented. Do not infer active EPG mutation solely from the manifest permission declaration.

### 2.4 Dynamic source shortcut components

Group 01 said the initially-disabled HDMI/AV/source activities “strongly support” dynamic enablement.

Group 08 proved it:

```text
internal source/action model
        ↓
PackageManager.setComponentEnabledSetting(...)
        ↓
HDMI1–4 / AV / Source Popup launcher components
```

This should now be marked **verified behavior**.

### 2.5 Native-library interpretation

Group 01 correctly avoided assigning unsupported semantics to `libptutils.so`.

Group 11 later closed the boundary further:

```text
PTApplication.vs(boolean)
      ↑
PTApplication.f()
      ↑
small managed caller set
```

with native exports including:

```text
Java_com_spocky_projengmenu_PTApplication_vs
Java_com_spocky_projengmenu_PTApplication_vc
```

The arm64 `vc` path is a very small global-byte-style accessor while `vs` is the larger JNI path. No evidence places home rows, app discovery, TV Provider, media sessions, wallpapers, settings or TV-input logic primarily inside the native library.

The revised architectural classification is:

> **narrow initialization / validation / protection / utility boundary; not required for Aerial-Rows feature reconstruction.**

---

## 3. Named component details that Group 01 did not spell out

The original report deliberately grouped many activities into families. That was efficient, but a static architecture baseline is stronger if the named manifest components are explicitly accounted for.

### 3.1 Category/settings wrappers

Later closure work identified:

- `CategoryContentSidePanelActivity`
- `ManageCategoriesActivity`
- `ManageChannelsActivity`

These are thin entry-point wrappers into the already-mapped category/channel settings system, not separate engines.

### 3.2 Internal launch/policy activities

Group 01 did not explicitly call out:

- `SettingsIntentActivity`
- `ToggleParentalControlActivity`
- `ParentalControlCheckActivity`

Later work shows these belong to the generic launcher-action / parental-policy boundary and are useful manifest anchors for the eventual centralized `LaunchGate` design.

### 3.3 Onboarding and reusable picker activities

The manifest also declares:

- `OnboardingActivity`
- `ColorPickerActivity`
- `ImagePickerActivity`

These are worth recording in Group 01 because they are distinct UI infrastructure surfaces rather than incidental resources.

### 3.4 Vendor wrappers now explicitly classified

The original “device/engineering activity families” section can now be made concrete:

- `SettingsMiTvActivity` → Xiaomi settings
- `SettingsFengOsActivity` → Formovie/FengOS settings
- `SettingsAmlogicActivity` → Droidlogic/Amlogic settings
- `SettingsMediatekActivity` → MediaTek factory TV settings
- `SettingsAndroidActivity` → standard Android Settings
- `EngMenuFengmiActivity` / `EngMenuFengOsActivity` → vendor engineering/test screens
- `MediaExplorerActivity` → Xiaomi/FengOS media explorer
- `GoogleSmartHomeActivity` → supported Google TV Smart Home target
- `ProfilePickerActivity` → supported profile-picker target
- `AccessibilityServiceActivity` → configuration/status UI for the existing accessibility service

None constitutes a second hidden device-integration architecture.

### 3.5 Optional maintenance utilities now classified

Group 11 resolved several activities that Group 01 only grouped broadly:

- `InstallAppsActivity` → guided APK installer
- `CleanupActivity` → privileged app freeze/unfreeze utility
- `RemoveLauncherActivity` → privileged stock-launcher freeze/unfreeze tool
- `BlockGooglePlaySelfUpdateActivity` → privileged Play self-update block/unblock utility
- `GappsUninstallActivity` → privileged GApps/debloat utility
- `WebRemoteActivity` → external web-remote page using the device's local IP, not an embedded HTTP server

These belong in an optional maintenance/tools layer and must not be treated as launcher-core dependencies.

### 3.6 About and entitlement surfaces

The following should be explicitly classified in the static map:

- `AboutAppActivity`
- `AboutBackgroundActivity`
- `AboutDeviceActivity`
- `PremiumActivity`
- `PremiumCardActivity`

The About activities are diagnostic/information surfaces. Premium activities are entitlement/purchase UI and are outside the feature-reimplementation target; they do not represent missing launcher architecture.

---

## 4. Plugin boundary can be stated more precisely

Group 01 correctly identified both:

```text
tv.projectivy.plugin.WALLPAPER_PROVIDER
```

and:

```text
tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED
```

Group 06 later closed the protocol enough to strengthen Group 01:

External services are discovered by `queryIntentServices(...)` and expose metadata conceptually including:

```text
uuid
name
updateMode
settingsActivity
itemsCacheDurationMillis
apiVersion
```

with observed requirements:

```text
UUID length = 36
apiVersion = 1
```

The Binder descriptor is:

```text
tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
```

and the observed transaction roles cover:

```text
get wallpapers for Event
read provider state/config
write provider state/config
```

So the better Group 01 architecture statement is:

> The wallpaper plugin system is a **discovered service + typed Binder contract + update broadcast**, not simply an intent-based plugin convention.

---

## 5. Exported-surface/security appendix worth adding

The original Group 01 identified the important exported components, but a reconstruction-oriented baseline benefits from explicitly classifying why they are exported.

### Protected exported services

- `ProjectivyAccessibilityService` → exported but protected by `BIND_ACCESSIBILITY_SERVICE`
- `NotificationListener` → exported but protected by `BIND_NOTIFICATION_LISTENER_SERVICE`

### User/document-facing exported activities

- `SetBackgroundActivity` → VIEW/SEND image/video
- `ImportSettingsActivity` → `.plbackup`
- `ImportThemeActivity` → `.pltheme`
- `ApplyIconPackActivity` → icon-pack apply actions
- `PinShortcutActivity` → `CONFIRM_PIN_SHORTCUT`
- `AppSettingsActivity` → `APPLICATION_PREFERENCES`

### Dynamically exposed launcher entries

- HDMI1–4
- AV
- Source Popup
- Media Explorer

These are exported but initially disabled and capability-gated.

### Exported receivers

- boot
- locale change
- storage/USB changes
- wallpaper-provider update

The wallpaper-update receiver is exported without a receiver-level permission in the manifest, so validation belongs to the runtime/plugin layer rather than being assumed from the custom permission declaration alone.

This security/capability distinction is useful when designing Aerial-Rows' manifest: only declare components as capabilities are implemented, and prefer system-protected bind services or explicit validation for exported surfaces.

---

## 6. MainActivity details worth adding

The application uses `@style/AppTheme`, but the HOME activity itself overrides that with:

```text
@style/SplashScreenTheme
```

This is a useful static detail because launcher startup presentation is intentionally separated from the application-wide theme.

MainActivity's previously recorded task posture remains valid:

```text
singleTask
empty taskAffinity
clearTaskOnLaunch
stateNotNeeded
excludeFromRecents
autoRemoveFromRecents
resumeWhilePausing
```

These should be understood as launcher-task behavior, not copied mechanically into Aerial-Rows without testing on Shield/Google TV.

---

## 7. Resource observations strengthened by later groups

### Themes

The built-in `.pltheme` files are now known to be ZIP packages containing at least:

```text
manifest               JSON metadata
appearance.plbackup    encrypted appearance backup
wallpapers/            optional media
```

### Calibration assets

The SDR/HDR/Dolby Vision raw assets correspond to the verified display/device tool family, not unused bundled media.

### Shortcuts

`@xml/shortcuts` is not just metadata noise. Group 10 confirmed Projectivy acts as a real Android shortcut host using `LauncherApps` / `ShortcutInfo` / `PinItemRequest`.

---

## 8. Things that were *not* missed

Several apparent gaps in Group 01 were intentional deferrals and were correctly closed later:

- home row/card presenters → Group 02
- app discovery/actions/package callbacks → Group 03
- channels/Watch Next → Group 04
- notification/media sessions → Group 05
- wallpaper provider/renderer internals → Group 06
- settings descriptors/storage effects → Group 07
- TV inputs/vendor adapters → Group 08
- HOME role/boot/accessibility/keys → Group 09
- backup/icons/shortcuts/parental control → Group 10
- residual named activities/native/obfuscated closure → Group 11

So Group 01 should not be criticized for lacking the implementation details that its scope explicitly deferred.

---

## 9. Revised Group 01 conclusions

1. The static package/component/resource map was correct and remains a reliable foundation.
2. `StartUpBootReceiver` must be relabelled as a registered-but-no-op receiver in 4.72; actual autostart lives elsewhere.
3. `ThemeContentProvider` is specifically a read-only theme-wallpaper URI bridge for ordinary consumers.
4. HDMI/AV/source launcher components are verified dynamically enabled components, not merely a manifest inference.
5. Declared EPG write permission should not be conflated with verified write behavior.
6. The named activity inventory should explicitly include category/channel wrappers, parental/internal launch activities, onboarding/pickers, vendor wrappers, maintenance utilities and About/entitlement surfaces.
7. The wallpaper plugin boundary is now known to combine service discovery, Binder IPC and update broadcasts.
8. `libptutils.so` is sufficiently closed as a narrow non-core boundary; no Aerial-Rows launcher subsystem depends on transplanting it.
9. No newly discovered component invalidates the Groups 2–12 implementation map.

## 10. Recommended status of Group 01

Change the status line from:

```text
complete — reviewed and strengthened
```

to:

```text
complete — post-closure audited after Groups 2–12
```

The original report can remain as historical evidence, with this document kept as its canonical audit/addendum.
