# Projectivy 4.72 — Group 08: HDMI, TV inputs, vendor/device integration

Status: **complete + verified**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group covers:

- HDMI/AV source models and source cards
- TV-input discovery and naming
- source switching and fallback order
- Projectivy's embedded internal TV path
- source-popup shortcuts
- device/vendor settings adapters
- display profiles and input-bound SDR/HDR profiles
- Xiaomi/Amlogic picture-setting adapters
- input-lag/sysfs controls
- zoom and 4K window-manager tools
- root/Magisk property-override tools
- capability gating and launcher-component exposure

Accessibility key remapping and HOME/boot behavior remain Group 09, even where Group 08 components are invoked from the accessibility service.

---

## 1. Analysis workflow used for Group 08

Group 08 used the hardened cache-first process:

```text
persistent source cache
    ↓
SQLite semantic/full-text index
    ↓
local class-reference graph
    ↓
semantic + graph-distance candidate ranking
    ↓
targeted batch export of missing classes in one APK load
    ↓
JADX only for unresolved semantics
    ↓
smali/bytecode fallback only when critical Java reconstruction is ambiguous
```

The candidate ranker combines:

- literal/API hits;
- full-text source hits;
- graph distance from named Projectivy input/device activities;
- light ownership weighting.

This avoided broad JADX scans and quickly elevated the real subsystem core (`dd4`, `ed4`, `c3`, `InternalTvActivity`, `SourcePopupActivity`, vendor settings activities, display-profile classes and root/sysfs helpers).

Group 08 also uses checkpointed stages and SHA-256 evidence manifests. The semantic database passed `PRAGMA integrity_check = ok` after the group analysis.

---

# 2. Core input model — `dd4`

Projectivy has a concrete source enum/model, `dd4`:

| Source | value | default label | matching token | icon/action |
|---|---:|---|---|---|
| Android | 0 | `input_source_none` | `android` | none |
| HDMI 1 | 1 | `input_source_hdmi1` | `Hdmi1` | HDMI1 icon/activity |
| HDMI 2 | 2 | `input_source_hdmi2` | `Hdmi2` | HDMI2 icon/activity |
| HDMI 3 | 3 | `input_source_hdmi3` | `Hdmi3` | HDMI3 icon/activity |
| HDMI 4 | 4 | `input_source_hdmi4` | `Hdmi4` | HDMI4 icon/activity |
| AV | 5 | `input_source_av` | `AV1` | AV icon/activity |
| Unknown | `Integer.MIN_VALUE` | unknown | `unknown` | none |

Each hardware source points at an availability/action descriptor (`os`) whose activity is one of:

- `SourceHDMI1Activity`
- `SourceHDMI2Activity`
- `SourceHDMI3Activity`
- `SourceHDMI4Activity`
- `SourceAVActivity`

`dd4.d()` resolves the visible source title through the Projectivy card/action layer, so a custom title override can win over the stock resource string.

`dd4.e()` reads a dynamically scoped visibility preference. Group 07 established the key family:

```text
input_shortcut_visible_<suffix>
```

For source actions the suffix is derived from the corresponding internal activity identity.

This means source availability and source *visibility as a launcher shortcut* are distinct concepts.

---

# 3. TV input repository / capability layer — `ed4`

`ed4` is the central TV-input discovery/cache manager.

It uses:

```text
TvInputManager
TvInputInfo
PackageManager.hasSystemFeature("android.software.live_tv")
```

and keeps separate cached lists for:

- passthrough inputs;
- non-passthrough/tuner inputs;
- normalized Projectivy source enum entries;
- custom/default input labels.

## Discovery

`ed4.h()` calls:

```text
TvInputManager.getTvInputList()
```

and separates:

```text
TvInputInfo.isPassthroughInput() == true
    → passthrough/external inputs

otherwise
    → tuner/non-passthrough inputs
```

On Android 7.0+ it respects `TvInputInfo.isHidden(context)` for non-passthrough entries.

## Input labels

On API 24+ Projectivy can prefer:

```text
TvInputInfo.loadCustomLabel(context)
```

and falls back to:

```text
TvInputInfo.loadLabel(context)
```

Both label forms are cached by TV-input ID.

## Mapping generic HDMI1/2/3/4/AV to platform TV inputs

For a `dd4` source, `ed4.d(...)` searches discovered passthrough `TvInputInfo` records and compares:

- `TvInputInfo.getId()`;
- normalized default input label;
- the generic source token (`Hdmi1`, `Hdmi2`, etc.);
- `HW`-style IDs.

This lets Projectivy bridge its stable generic source model to vendor-specific TV Input Framework IDs.

## Compatibility fallback

The input-capability check is broader than only `android.software.live_tv`; Projectivy also has internal compatibility fallbacks for devices whose firmware exposes TV input behavior without reporting the feature conventionally.

Sony/TCL also receive defensive retry handling when the firmware reports an abnormal empty passthrough state; Projectivy retries a limited number of times rather than assuming permanently absent inputs.

---

# 4. HDMI/AV launcher activities — `c3`

`SourceHDMI1Activity` through `SourceHDMI4Activity` are tiny subclasses of the shared `c3` source launcher.

Their Droidlogic fallback service IDs are:

```text
HDMI1 → Hdmi1InputService/HW5
HDMI2 → Hdmi2InputService/HW6
HDMI3 → Hdmi3InputService/HW7
HDMI4 → Hdmi4InputService/HW8
AV    → AV1InputService/HW1
```

When `ed4` cannot resolve a runtime `TvInputInfo`, `c3.H()` can construct a Droidlogic-style fallback ID:

```text
com.droidlogic.tvinput/.services.<service>
```

---

# 5. Source switching strategy

`c3.x()` is the main external-source launch pipeline.

It first resolves whether the source should be shown through Projectivy's internal `TvView` or delegated to the platform/vendor TV application.

The choice can be forced by an intent extra:

```text
tvView = "internal"
tvView = "external"
```

otherwise a stored Projectivy preference supplies the default.

The requested source is persisted as the current input selection.

## Internal path

If the internal path is chosen:

```text
SourceHDMIxActivity / SourceAVActivity
    ↓
InternalTvActivity
    ↓
input enum value passed in extras
```

## External/vendor path

Projectivy first tries Xiaomi's external-source contract:

```text
com.xiaomi.mitv.tvplayer.EXTSRC_PLAY
```

with Xiaomi-specific source values mapped through `uz2`.

If that fails, Projectivy falls back through several vendor/platform targets using the resolved TV Input Framework URI:

1. Fengmi TV input
   - package `com.fengmi.tvinput`
   - `com.fengmi.tvinput.MainActivity`
2. MediaTek TV center
   - `com.mediatek.wwtv.tvcenter.nav.TurnkeyUiMainActivity`
3. Mi TV live TV variant
   - `com.mitv.livetv.nav.TurnkeyUiMainActivity`
4. AOSP Live Channels / TV
   - `com.android.tv.MainActivity`
5. Google TV variant
   - `com.google.android.tv.MainActivity`
6. generic intent resolution
7. final fallback back to Projectivy internal TV rendering

When launching vendor TV apps, Projectivy additionally supplies fields such as:

```text
deviceName
inputName
bootcomplete = true
```

and for MediaTek it explicitly places the channel/input URI in the intent data.

This is therefore a layered compatibility strategy, not a single hard-coded HDMI intent.

---

# 6. Embedded TV path — `InternalTvActivity`

`InternalTvActivity` is a real TV Input Framework host.

Its layout contains a platform:

```text
android.media.tv.TvView
```

plus waiting/title/logo UI.

On receiving the source enum it:

- resolves the source title;
- shows the source-specific animated icon;
- installs a `TvView` callback;
- installs an unhandled-input listener;
- requests focus on the TV view.

## Tuning

`InternalTvActivity.J(...)` resolves the input ID through `ed4` and then chooses:

```text
TvContract.buildChannelUriForPassthroughInput(inputId)
```

for passthrough sources whose ID contains the hardware-style `HW` form, or:

```text
TvContract.buildChannelsUriForInput(inputId)
```

for tuner/non-passthrough input paths.

It tunes with:

```text
TvView.tune(inputId, uri)
```

and resets/shows the waiting state when no usable input can be resolved.

## Remote/input handling

The activity forwards a set of TV-related key codes into `TvView.dispatchKeyEvent(...)` while preserving Projectivy's own navigation/back semantics.

Notably:

- long-press BACK stops the internal-TV session and exits;
- ordinary BACK is handled separately;
- MENU while internal TV is active opens vendor display/settings UI where available;
- Xiaomi pre-Android-8 firmware uses its display-settings popup service;
- otherwise the MediaTek/factory-settings launcher is attempted.

The activity also owns/release a media-session/controller helper around the internal-TV lifetime.

---

# 7. Source chooser popup — `SourcePopupActivity`

Projectivy has a separate generic "show input chooser" action.

The fallback order is explicitly vendor-aware:

1. Xiaomi settings broadcast
   - `com.xiaomi.mitv.settings.INPUTSOURCE_POPUP`
2. Mi TV Android-TV home input popup
   - `com.mitv.tvhome.atv.app.tv.INPUTSOURCE_POPUP`
3. Formovie media-select app
   - `com.formovie.fmmediaselect.MainActivity`
4. MediaTek TV center input panel
   - `com.mediatek.wwtv.tvcenter.nav.inputpanel.InputsPanelActivity`
5. Google TV launcher input panel
   - `com.google.android.tvlauncher.inputs.InputsPanelActivity`

The Xiaomi settings form is sent as a broadcast when the package is installed; the others are started as activities.

---

# 8. Input shortcuts as real launcher components

The manifest declares HDMI1–4, AV and Source Popup activities as:

```text
exported = true
enabled = false
launchMode = singleInstance
```

with:

```text
ACTION_MAIN
CATEGORY_LAUNCHER
CATEGORY_LEANBACK_LAUNCHER
```

So Projectivy can expose these as genuine launcher-visible source shortcuts when appropriate.

The shared internal-action card layer (`e90`) exposes component enable/disable through `wo2.d(...)`, which calls:

```text
PackageManager.setComponentEnabledSetting(...)
```

for `com.spocky.projengmenu` internal activities.

This gives Projectivy two representations of an input:

- a card inside its own configurable Inputs category;
- optionally a real Android/Leanback launcher shortcut component.

For Aerial-Rows, source visibility should therefore remain a model/policy decision rather than being tied directly to whether the activity exists in the manifest.

---

# 9. Capability gating

Projectivy wraps many device actions in capability descriptors (`os`, `p`, `cd4`).

Examples verified in this group:

- HDMI1–4 action available only when TV-input support exists and that input's visibility preference is enabled;
- AV uses the same pattern;
- Source Popup is considered supported when known Google TV/Xiaomi/Formovie/MediaTek input UI packages exist;
- Amlogic settings require `com.droidlogic.tv.settings`;
- FengOS settings require `com.formovie.gsettings`;
- MediaTek settings require `mediatek.factorymenu.ui`;
- Mi TV settings require `com.xiaomi.mitv.settings`;
- Zoom is offered if `/sys/class/video/zoom` exists or the Xiaomi screen-scale service is available;
- Input Lag requires Projectivy's privileged/root shell to be available and at least one relevant deinterlace sysfs node to exist;
- 4K window-manager tooling is exposed only when the privileged/root shell is available.

This keeps unsupported tools out of the UI rather than letting users repeatedly launch actions that can never work.

---

# 10. Vendor settings adapters

Projectivy contains explicit lightweight vendor launchers.

## Android settings

```text
android.settings.SETTINGS
```

## Amlogic / Droidlogic

Primary:

```text
com.droidlogic.tv.settings/.MainSettings
```

fallback:

```text
com.droidlogic.tv.settings/.more.MorePrefFragmentActivity
```

## Xiaomi Mi TV

```text
com.xiaomi.mitv.settings/.entry.MainActivity
```

## Formovie / FengOS

```text
com.formovie.gsettings/.ui.activity.MainActivity
```

## MediaTek

```text
mediatek.tvsetting.factory.intent.action.MainmenuActivity
```

## FengOS/Fengmi engineering tools

Projectivy additionally exposes:

```text
com.fengmi.autofocustest/.MainActivity
```

and tries two Fengmi test-suite activities:

```text
com.fengmi.testsuite/.LunchActivity
com.fengmi.testsuite/.EngActivity
```

These adapters are simple launch/fallback wrappers; Projectivy does not duplicate those vendor settings screens itself.

---

# 11. Other vendor shortcuts

## Media Explorer

Projectivy prefers:

```text
com.xiaomi.mitv.mediaexplorer/.NewScraperMainEntryActivity
```

and falls back to:

```text
com.fengos.fmexplorer/.MainActivity
```

## Google Smart Home

A separate shortcut exists and is exposed only when the resolved intent is available on the applicable Google TV environment.

---

# 12. Display Profiles — Projectivy-owned subsystem

This is substantially deeper than the simple vendor settings launchers.

Named class:

```text
com.spocky.projengmenu.displayProfiles.DisplayProfileManager
```

Projectivy maintains a serialized list of `lz0` display-profile models.

If no profile exists it creates a default profile.

Profiles are sorted and stored independently of the runtime vendor adapter.

## Profile identity

Each `lz0` has:

- user-visible name;
- stable UUID;
- multiple numeric picture parameters;
- several boolean picture-processing flags;
- optional live adapter/delegate.

## Verified picture parameters

Through the Amlogic reflection adapter, the generic `ru` interface maps profile values to functions including:

```text
Brightness
Contrast
Saturation
Sharpness
Hue
Backlight
White balance gains/offsets
CM status
DNLP status
```

The profile also includes extra vendor-specific processing fields used by the supported adapters.

`lz0.a()` captures the current device picture settings into a profile.

`lz0.b()` applies a profile back to the current adapter.

This is a proper generic profile model on top of vendor-specific backends.

---

# 13. Amlogic display adapter

`j31` uses reflection so Projectivy can run on devices where Amlogic classes do not exist.

It supports two Droidlogic API families:

```text
com.droidlogic.app.tv.TvControlManager
com.droidlogic.app.SystemControlManager
```

and reflectively resolves methods/enums such as:

```text
GetBrightness / SetBrightness
GetContrast / SetContrast
GetSaturation / SetSaturation
GetSharpness / SetSharpness
GetHue / SetHue
GetBacklight / SetBacklight
getCMStatus / setCMStatus
getDNLPStatus / setDNLPStatus
WhiteBalanceParams
SOURCE_TYPE_HDMI
```

The use of reflection is deliberate compatibility isolation: absent vendor classes simply disable that backend instead of breaking application class loading.

Amlogic generic source names are separately represented through `f31` for HDMI1–4, AV and Android.

---

# 14. Xiaomi picture-settings adapter

Projectivy also has a Xiaomi backend (`wz2`) which is constructed reflectively from:

```text
mitv.display.PictureSettingsManager
```

and tries to obtain Xiaomi's source manager through `TvContext`.

Xiaomi source enums (`uz2`) map HDMI1–4, AV and Android into the generic Projectivy `dd4` source model.

The DisplayProfileManager chooses among available vendor adapters at runtime rather than compiling the launcher around one OEM API.

---

# 15. Per-input SDR/HDR display-profile bindings

Display Profiles has a dedicated binding UI (`i62`).

The user chooses an input from:

```text
Android
HDMI1
HDMI2
HDMI3
HDMI4
AV
```

filtered according to the currently available Projectivy source list.

For each input, the user independently selects:

- an SDR profile;
- an HDR profile.

Group 07 established the backing dynamic key families:

```text
display_profiles_input_sdr_<input>
display_profiles_input_hdr_<input>
```

Internally these are the `hj3.v` and `hj3.u` scoped settings respectively.

The stored value is the stable display-profile UUID, not just a list index.

This is an important reconstruction detail: profile bindings should survive profile reordering.

---

# 16. Display-profile list UI

`DisplayProfilesActivity` exposes two branches:

```text
Profile list
Input bindings
```

The input-binding branch appears only when Projectivy detects usable external TV inputs.

Profiles can be added and edited individually; the manager serializes/deserializes them using the app's persistence/serialization infrastructure.

The exact full editor UI is intentionally treated as part of the display-profile subsystem rather than re-described here field-by-field where the decompiler generated unrelated noise, but capture/apply semantics and the supported adapter methods are verified.

---

# 17. Input-lag / deinterlace controls

`InputLagActivity` is backed by `f62`.

It reads:

```text
/sys/module/di/parameters/nr2_en
/sys/module/di/parameters/bypass_all
```

The UI exposes at least:

- noise reduction;
- deinterlacing.

Current state is interpreted as:

```text
nr2_en == "1"      → noise reduction enabled
bypass_all == "0"  → deinterlacing enabled
```

Writes ultimately go through Projectivy's privileged shell (`kx3`) using root-required commands of the form:

```text
echo <value> > <sysfs path>
```

The root/shell capability is explicitly probed and cached as:

```text
UNKNOWN
AVAILABLE
UNAVAILABLE
```

These tools are therefore capability-gated and are not expected to function on an ordinary unprivileged Android TV installation.

---

# 18. Zoom controls

`ZoomActivity` has two possible paths.

## Xiaomi stock path

When `com.xiaomi.mitv.settings` exists and resolves the service, Projectivy launches:

```text
com.xiaomi.mitv.settings.SCREENSCALE
```

against:

```text
com.xiaomi.mitv.settings.TvSettingService
```

## Sysfs path

When supported, Projectivy reads/writes:

```text
/sys/class/video/zoom
```

using its privileged shell helper.

So the same Projectivy setting abstracts either a vendor-native UI or direct low-level control.

---

# 19. 4K window-manager/device-resolution tool

`FourKWMActivity` displays several different resolution concepts rather than conflating them:

- `/sys/class/video/device_resolution`;
- `/sys/class/display/mode`;
- Android window-manager pixel size;
- Android window-manager density.

The UI provides actions for 2160p and 1080p-style window-manager configuration.

The action path is privileged/root-gated.

This tool is separate from actual codec/display-mode capability; Projectivy explicitly warns that changing the UI rendering size does not magically add missing 4K hardware decode support.

---

# 20. Root/Magisk property override tools

Projectivy has a common property-override activity base (`vm3`) and UI (`an0`).

The UI explicitly states that Magisk/root is required.

It offers install/uninstall actions and feeds a named shell script plus property/value pairs into the privileged command layer.

## Custom properties

`CustomPropsOverrideActivity` uses:

```text
atvtools.props.sh
```

and receives user/configured property pairs maintained elsewhere in Projectivy.

## 4K property override

`FourKPropsOverrideActivity` requests:

```text
sys.display-size = 3840x2160
ro.sf.lcd_density = 640
sys.fb.size = 3840x2160
```

through:

```text
atvtools-4k.props.sh
```

## Auto-off override

`AutoOffPropsOverrideActivity` requests:

```text
persist.sys.power.autoff = 0
```

using the shared auto-off script path.

JADX does not reconstruct the long coroutine responsible for installing/removing these overrides reliably. The surrounding behavior, required privilege state, property payloads, script names, success/restart/error handling and root-shell boundary are all directly verified. We should not invent the missing coroutine internals.

---

# 21. Ownership boundary

Group 08 has three distinct layers which should remain separate in Aerial-Rows.

## A. Generic Projectivy input model

```text
InputSource
TvInputRepository
InputAction
```

## B. Vendor adapters

```text
Xiaomi
Amlogic/Droidlogic
MediaTek
Formovie/FengOS
Google TV/AOSP TV
```

These should be optional capability plugins/adapters.

## C. Privileged device tools

```text
sysfs writers
root shell
property overrides
display engineering controls
```

These must never be prerequisites for ordinary launcher functionality.

That separation is important for Nvidia Shield and normal certified Android TV devices, where many OEM-specific/root-only tools should simply remain unavailable.

---

# 22. Recommended Aerial-Rows architecture

A clean reconstruction should use approximately:

```text
InputRepository
 ├─ discoverTvInputs()
 ├─ passthroughInputs
 ├─ tunerInputs
 ├─ resolveGenericSource(HDMI1..AV)
 └─ observe/refresh()

InputSource
 ├─ Android
 ├─ HDMI1
 ├─ HDMI2
 ├─ HDMI3
 ├─ HDMI4
 ├─ AV
 └─ Unknown

InputLaunchCoordinator
 ├─ InternalTvStrategy
 ├─ XiaomiStrategy
 ├─ FengmiStrategy
 ├─ MediaTekStrategy
 ├─ AospTvStrategy
 └─ GenericTvInputStrategy

InputPopupCoordinator
 ├─ Xiaomi popup
 ├─ MiTV popup
 ├─ Formovie selector
 ├─ MediaTek panel
 └─ Google TV panel
```

Vendor/device tools should be separately modeled:

```text
DeviceCapabilityRegistry
 ├─ AmlogicSettingsAdapter
 ├─ XiaomiSettingsAdapter
 ├─ FengOsSettingsAdapter
 ├─ MediaTekSettingsAdapter
 ├─ DisplayProfileBackend
 ├─ ZoomBackend
 ├─ InputLagBackend
 └─ RootShellBackend
```

Display profiles:

```text
DisplayProfileRepository
 ├─ profiles by UUID
 ├─ capture current settings
 ├─ apply profile
 ├─ SDR binding by InputSource
 └─ HDR binding by InputSource

PictureBackend
 ├─ AmlogicTvControlBackend
 ├─ AmlogicSystemControlBackend
 └─ XiaomiPictureSettingsBackend
```

This keeps the generic launcher portable while still allowing Projectivy-like OEM enhancements where the hardware supports them.

---

# 23. Nvidia Shield implications

For our primary target, Nvidia Shield, the architecture should degrade gracefully:

- ordinary app launcher functionality remains independent;
- `TvInputManager` can still be queried when meaningful;
- Xiaomi/Formovie/MediaTek/Amlogic actions remain absent unless their packages/APIs exist;
- direct sysfs/property tools remain hidden unless capability/root probes pass;
- source popup can use Google TV/platform paths where available;
- no OEM-specific class should ever be loaded eagerly in a way that can crash a non-OEM device.

Projectivy's use of reflection and capability descriptors demonstrates this same principle.

---

# 24. Reliability notes

High-confidence evidence in this group comes from:

- manifest declarations;
- direct Android API calls;
- decompiled small named activities;
- `dd4`, `ed4`, `c3`, `InternalTvActivity` and source popup code;
- targeted missing-class export;
- dynamic settings key mapping from Group 07;
- reflection method names in the Amlogic adapter;
- concrete sysfs/property strings;
- root-shell state code.

Where JADX produced structurally unreliable coroutine Java, Group 08 deliberately reports only behavior corroborated by callers, constants, UI wiring and command boundaries.

No claim in the reconstruction plan depends on a failed JADX region reconstruction.

---

# 25. Group 08 conclusions

1. Projectivy has a real generic input layer (`dd4` + `ed4`) rather than one-off HDMI buttons.
2. HDMI/AV switching uses a compatibility ladder: internal `TvView`, Xiaomi vendor intents, Fengmi/MediaTek/AOSP/Google TV apps, and generic TV Input Framework URIs.
3. Projectivy can itself host passthrough/tuner input using `TvView`.
4. Input shortcuts can exist both as home cards and dynamically enabled Android/Leanback launcher components.
5. Vendor settings are isolated behind capability checks and lightweight adapters.
6. Display Profiles is a Projectivy-owned generic profile system with runtime Amlogic/Xiaomi backends.
7. SDR and HDR profiles can be bound independently per input using stable profile UUIDs.
8. Zoom, input-lag, 4K-WM and property override tools are optional privileged/device-specific capabilities, not core launcher dependencies.
9. Aerial-Rows should preserve the same separation: generic portable input model, optional vendor adapters, optional privileged tooling.

## Next group

**Group 09 — HOME replacement, boot, accessibility & remote/key handling**

Targets include:

- `ProjectivyAccessibilityService`
- default HOME/launcher replacement mechanisms
- HOME key and remote interception
- mapped key actions and custom key bindings
- boot receiver / launch-on-boot source or app
- returning to Projectivy from vendor/input apps
- input/source detection hooks that live specifically in accessibility
- lifecycle and OEM compatibility behavior around launcher replacement
