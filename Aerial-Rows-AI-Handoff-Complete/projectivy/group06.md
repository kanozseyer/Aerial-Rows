# Projectivy 4.72 — Group 06: Wallpaper, Themes & Aerial Integration

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## 1. Scope

This group closes the wallpaper/background/theme subsystem sufficiently to reproduce its architecture independently in Aerial-Rows. It covers:

- wallpaper-provider abstraction
- built-in wallpaper providers
- external wallpaper-provider plugin protocol
- wallpaper event model
- wallpaper item model
- provider caching/invalidation boundaries
- renderer selection
- image/video/colour/drawable/Lottie rendering
- video playback lifecycle
- transitions and failure fallback
- user image/video backgrounds
- folder-backed backgrounds
- TV-program/Now Playing-driven backgrounds
- theme `.pltheme` packaging/import/export
- `ThemeContentProvider`
- Aerial integration design

The goal is not to copy Projectivy source; it is to understand the functional contract and data flow well enough to implement equivalent behavior cleanly.

---

## 2. Faster analysis workflow used for this group

Group 6 is the first group to use the improved persistent semantic-index workflow end-to-end.

### Persistent evidence surfaces

- Decompiled source cache: `/mnt/data/projectivy_decompiled/sources`
- Decompiled resources: `/mnt/data/projectivy_decompiled/resources`
- SQLite semantic index: `/mnt/data/re_cache/projectivy_semantic_index.sqlite`
- Indexed source files: **6,224**
- Semantic tags include wallpaper/media/TV provider/apps/settings/inputs/accessibility/boot/storage

The group workflow became:

```text
persistent source cache
        ↓
SQLite FTS / semantic role search
        ↓
small class cluster
        ↓
selected-class batch decompile in one APK load
        ↓
JADX xrefs for uncertain edges
        ↓
smali only where Java decompilation is unreliable
```

This avoids repeatedly loading and rescanning the entire APK for every class.

---

# 3. High-level architecture

Projectivy does **not** model wallpaper as a single URI preference.

The real structure is:

```text
Wallpaper source/provider
        ↓
List<Wallpaper>
        ↓
provider candidate/cache logic
        ↓
selected Wallpaper
        ↓
Wallpaper type
        ↓
renderer registry
        ↓
Activity background surface
```

The core coordinator is:

```text
defpackage.bs4
```

It owns or coordinates:

- provider registry
- selected provider
- provider connection/disconnection
- provider events
- selected/current wallpaper
- renderer registry
- Activity lifecycle
- retry/failure handling
- external-provider update broadcasts
- background transitions
- interaction with Leanback `BackgroundManager`

This architecture is a very good match for Aerial-Rows because Aerial videos can be treated as a **wallpaper provider/source**, while the existing video renderer remains generic.

---

# 4. Provider abstraction

The primary provider interface is:

```text
defpackage.m12
```

Its observed contract is effectively:

```text
WallpaperProvider
 ├─ getId()
 ├─ getName()
 ├─ getIcon()
 ├─ getWallpapers(event/context)
 ├─ getUpdateModeBitmask()
 ├─ acceptsEvent(event)
 ├─ cacheDurationMillis()
 ├─ hasSettings/configuration()
 ├─ isEventDriven()
 └─ disconnect()
```

Relevant obfuscated methods include:

```text
I(...)       provider/service connection or data retrieval boundary
J()          event/update bitmask
e()          cache duration in milliseconds
b(Event)     accepts event
d()          provider behavior characteristic / event-driven distinction
s()          provider supports configuration/settings
```

Built-in providers share a local Binder-compatible base:

```text
z05 extends tv.projectivy.plugin.wallpaperprovider.api.a implements m12
```

The built-in base defaults to:

```text
update/event mask = TimeElapsed (1)
cache duration     = 3,600,000 ms (1 hour)
```

Provider-specific persisted configuration is keyed by provider UUID via Projectivy's preference layer (`hj3.q0`).

---

# 5. Selected provider

The selected provider ID is persisted through:

```text
hj3.o0
```

The default/fallback provider UUID is:

```text
326618d8-3242-4322-a1b1-9e60e5d3dfc3
```

Projectivy can force this fallback under its own entitlement/premium rules. That entitlement logic is not required to reproduce the technical wallpaper architecture.

Provider lookup always has a fallback path rather than assuming the configured provider still exists.

This matters for external plugins that may be uninstalled.

---

# 6. Wallpaper-provider event model

Projectivy exposes a retained API package:

```text
tv.projectivy.plugin.wallpaperprovider.api
```

The primary event class is:

```text
tv.projectivy.plugin.wallpaperprovider.api.Event
```

Verified event types/bit values:

| Event | Value |
|---|---:|
| `LoadingError` | -1 |
| `TimeElapsed` | 1 |
| `NowPlayingChanged` | 2 |
| `CardFocused` | 4 |
| `ProgramCardFocused` | 8 |
| `LauncherIdleModeChanged` | 16 |

Providers declare which event families they respond to using the same bitmask concept.

### `CardFocused`

Carries focused-card information including the semantic equivalents of:

- light/palette colour
- dark/palette colour
- title
- package identity

### `ProgramCardFocused`

Carries program metadata including:

- title
- program type
- icon/artwork URI
- icon aspect ratio

### `NowPlayingChanged`

The Group 5 trace established the semantic snapshot fields used by the wallpaper layer:

- title
- artist
- album title
- artwork URI
- playing state

### `LauncherIdleModeChanged`

Contains launcher idle state.

This event is particularly useful for an Aerial integration because Aerial playback can become more active when the launcher enters idle mode without requiring a separate duplicate playback engine.

---

# 7. Wallpaper data object

The provider API exposes:

```text
tv.projectivy.plugin.wallpaperprovider.api.Wallpaper
```

Verified fields:

```text
uri
type
displayMode
title
source
author
actionUri
```

This is effectively a transport-neutral wallpaper descriptor rather than a decoded bitmap/video object.

---

# 8. Wallpaper types

The enum represented by `a15` defines:

| Type | Value |
|---|---:|
| IMAGE | 0 |
| DRAWABLE | 1 |
| ANIMATED_DRAWABLE | 2 |
| LOTTIE | 3 |
| VIDEO | 4 |
| COLOR | 5 |

The existence of `VIDEO` as a first-class wallpaper type is one of the most important Group 6 findings for Aerial-Rows.

Aerial videos do **not** need their own special background subsystem. They can be normal wallpaper items of type `VIDEO`.

---

# 9. Wallpaper display modes

The `t05` enum provides display behavior equivalent to:

```text
DEFAULT / automatic
CROP
STRETCH
FIT_CENTER
FIT_CENTER_BLURRED_BG
BLUR
```

This should be modeled independently of the wallpaper type.

For example, an IMAGE and a VIDEO can share presentation semantics without the provider having to know the renderer implementation.

---

# 10. Built-in provider registry

`bs4.n()` constructs Projectivy's built-in providers and then merges external provider services into the same provider map.

The built-in list is explicitly instantiated as ten providers.

## 10.1 None

Class:

```text
v53
```

UUID:

```text
773b5948-b4d1-49c5-b994-999210bc8877
```

Behavior:

- static
- supplies a black `COLOR` wallpaper
- effectively disables visible wallpaper imagery

---

## 10.2 Default

Class:

```text
xv0
```

UUID:

```text
326618d8-3242-4322-a1b1-9e60e5d3dfc3
```

Behavior:

- built-in default Projectivy image
- also used as fallback when configured providers disappear/fail or policy redirects to default

---

## 10.3 Gradient

Class:

```text
xt1
```

UUID:

```text
e77610c3-d495-4090-b387-fae3faafa718
```

Update mode:

```text
CardFocused = 4
```

Behavior:

- reacts to selected card changes
- derives gradient/background colors from focused-card palette information

This confirms that providers are not limited to static URI playlists.

---

## 10.4 User Image / Video

Class:

```text
jf1
```

UUID:

```text
5ca2f519-b7e3-4314-b878-40c4cc54e64b
```

Behavior:

- stores a serialized wallpaper descriptor
- accepts a user URI
- supports image or video type
- can represent colors as `COLOR` wallpaper items
- settings include URI/picker and video mode

This provider is the closest existing Projectivy analogue to manually choosing a single Aerial/video asset.

---

## 10.5 Folder

Class:

```text
ek1
```

UUID:

```text
0b52e4f1-78c9-4d65-a732-d5f95f4665b7
```

Behavior:

- scans a selected file/tree URI
- supports recursive content discovery
- accepts at least:
  - `image/*`
  - `video/*`
  - `application/json` for Lottie

This proves mixed media wallpaper playlists are part of the provider concept.

---

## 10.6 Selected Program

Class:

```text
o34
```

UUID:

```text
895c68b3-6522-43b8-85bc-9a1624c41850
```

Update mode:

```text
ProgramCardFocused = 8
```

Behavior:

- reacts to focused Android TV program cards
- uses the selected program's artwork as background

This links directly to Group 4's `Event.ProgramCardFocused` trace.

---

## 10.7 Now Playing

Class:

```text
e73
```

UUID:

```text
b7ac18ab-d18d-4587-8ed7-50af45541033
```

Update mode:

```text
NowPlayingChanged = 2
```

Behavior:

- reacts to media-session changes
- derives wallpaper from currently playing media artwork
- carries title/album/artist context

This links directly to Group 5's media-session architecture.

---

## 10.8 Random TV Program Artwork

Class:

```text
rp3
```

UUID:

```text
eeac320d-bf54-449d-b852-b9048acd7c4b
```

Behavior:

- reuses the Group 4 TV provider repository
- requests Preview Programs globally (`e70.c(0L)`)
- filters unsuitable content/aspect information
- converts program artwork into image wallpaper descriptors
- preserves program/action URI so the wallpaper can remain actionable

Cache duration:

```text
7,200,000 ms = 2 hours
```

This confirms that TV content is reusable as a wallpaper source rather than being coupled to home rows.

---

## 10.9 Reddit

Class:

```text
is3
```

UUID:

```text
eea7a685-d68f-4fb5-b46e-b4897af5413f
```

Behavior:

- RSS/network wallpaper provider
- supports predefined content sources plus custom subreddit/source configuration

Observed source labels include concepts such as:

- Earth
- Projectivy Wallpapers
- Projectivy TMDB
- TMDB Wallpapers
- ArtWp
- TvDark Wallpapers
- Wallpaper AndroidTV
- Astrophotography
- Wallpapers
- Big Wallpapers
- Botanical
- Sea
- Sky
- Car
- Animal
- DALL·E & Sora
- Midjourney
- Custom

The exact remote feed URLs are implementation details rather than architecture requirements for Aerial-Rows.

---

## 10.10 Theme Wallpaper

Class:

```text
em4
```

UUID:

```text
78edbc3e-7e0e-49b6-8bf0-3787ee67b1c8
```

Behavior:

- obtains wallpaper files packaged in the currently selected `.pltheme`
- exposes them through `ThemeContentProvider`
- maps file type to wallpaper type

Observed mappings include:

```text
.json               → LOTTIE
.mp4/.mkv/.webm     → VIDEO
other image media   → IMAGE
```

---

# 11. External wallpaper-provider plugin protocol

This is a real plugin boundary rather than a convention internal to Projectivy.

Projectivy discovers external services using:

```text
Intent action:
tv.projectivy.plugin.WALLPAPER_PROVIDER
```

The lookup is performed with `PackageManager.queryIntentServices(...)`.

External providers are wrapped by:

```text
y05 implements m12
```

### Required/recognized service metadata

Observed metadata keys:

```text
uuid
name
updateMode
settingsActivity
itemsCacheDurationMillis
apiVersion
```

Requirements include:

```text
uuid length = 36
apiVersion = 1
```

Defaults include:

```text
updateMode = 1
itemsCacheDurationMillis = 3,600,000
```

Providers can expose their own settings Activity.

---

# 12. Binder API

The Binder descriptor is:

```text
tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
```

The proxy class `n12` exposes three observed transactions:

```text
1 → List<Wallpaper> A(Event)
2 → String v()
3 → void k(String)
```

Semantically these represent:

```text
get wallpapers for event
read provider configuration/state
write provider configuration/state
```

Projectivy synchronizes persisted provider configuration with external providers when connecting/disconnecting.

This is sufficiently understood to recreate an equivalent plugin SPI without copying implementation source.

---

# 13. Provider-update broadcast

External providers can signal that their data has changed through:

```text
tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED
```

Observed extras:

```text
tv.projectivy.plugin.extra.PROVIDER_ID

tv.projectivy.plugin.extra.UPDATE_REASON
```

Receiver:

```text
WallpaperProviderUpdateReceiver
```

`bs4.s()` debounces repeated update signals from the same provider for:

```text
5,000 ms
```

If the updated provider is currently selected, Projectivy invalidates its current source state and requests a new wallpaper.

This is a useful pattern for Aerial catalog/download updates.

---

# 14. Provider candidate/cache behavior

Projectivy maintains a small provider candidate-cache state object (`s05`) containing:

```text
cached wallpaper list
cache timestamp
provider ID
```

Each provider supplies its own cache duration through `e()`.

Verified examples:

```text
built-in default duration ≈ 1 hour
Random TV Program provider = 2 hours
```

The exact coroutine implementation around candidate refresh contains decompiler-ambiguous control flow, so this report intentionally does **not** claim an exact line-by-line cache algorithm.

The reliable architectural facts are:

1. candidates are cached by provider identity;
2. the cache records time;
3. provider-specific TTL exists;
4. provider update broadcasts clear the matching cache;
5. switching/invalidation clears current wallpaper state and candidate state as needed.

That is enough to reproduce behavior robustly.

---

# 15. Renderer architecture

Wallpaper sources and wallpaper renderers are explicitly separate.

Base renderer:

```text
mu
```

The base responsibility includes:

- declare supported wallpaper types
- accept/apply a wallpaper
- lifecycle start/resume/stop/release
- reject unsupported/empty input
- avoid redundant reloads
- provide palette/dynamic-color information to the launcher

### Exact renderer registry

A final verification of `MainActivity.w(Bundle)` confirms the registry is explicitly instantiated as **five renderer instances**:

```text
v32                 image
djy4 / jy4           video
dc0(..., 0)          color
o21                  drawable / animated drawable
dc0(..., 1)          Lottie
```

and assigned to:

```text
bs4.O
```

This is important:

```text
Providers are extensible/dynamic
Renderers are a fixed type registry
```

Aerial should therefore add a **provider**, not a separate renderer, because VIDEO rendering already exists generically.

---

# 16. IMAGE renderer

Class:

```text
v32 extends o21
```

Observed behavior:

- loads images through Glide
- applies display/crop/blur-related transformations
- constrains very large loads to roughly 3840×2160 scale bounds
- extracts palette information
- supports HDR/gainmap-related handling on capable Android versions/devices
- can alter window color-mode behavior according to HDR content

The exact Glide transformation stack is not necessary to reproduce Projectivy's architecture, but the image renderer clearly owns decoding/rendering rather than the provider.

---

# 17. DRAWABLE / ANIMATED_DRAWABLE renderer

Class:

```text
o21
```

Behavior includes:

- static drawable display
- `Animatable` lifecycle start/stop where applicable
- wallpaper color/filter adjustments
- palette extraction

This is also the shared image/drawable presentation base used by the image path.

---

# 18. COLOR renderer

One variant of:

```text
dc0
```

handles `COLOR` wallpaper items.

This lets providers return a color descriptor through the same wallpaper pipeline rather than requiring special home-screen logic.

---

# 19. Lottie renderer

A second `dc0` variant handles:

```text
LOTTIE
```

This is why folder/theme providers can expose JSON animation assets as ordinary wallpaper descriptors.

---

# 20. VIDEO renderer

Class:

```text
jy4
```

This is the critical Aerial-relevant renderer.

Verified behavior:

- uses AndroidX Media3 / ExoPlayer
- attaches a full-screen `SurfaceView` behind launcher content
- supports ordinary Media3 media sources
- contains special RTSP handling
- maintains playback lifecycle independently from provider logic
- releases/stops appropriately on Activity lifecycle transitions
- can resume playback position where sensible
- maintains a global “video wallpaper playing” state (`bs4.M`)
- can use `PixelCopy` frames to extract palette/dynamic colors from video output
- reports loading/playback errors back to `bs4`

This conclusively shows that Projectivy treats video as a real background medium rather than as a screensaver-only feature.

---

# 21. Video lifecycle and failure behavior

`bs4` selects a renderer according to `Wallpaper.getEnumType()`.

When wallpaper type changes:

```text
old renderer
    ↓ stop/release as appropriate
new renderer
    ↓ load wallpaper
```

Projectivy tracks load failures.

`bs4.q()` retries up to:

```text
5 attempts
```

After repeated failure it emits:

```text
Event.LoadingError
```

and enters fallback/next-wallpaper behavior.

For an Aerial provider this means broken/deleted/network-unavailable videos can naturally fall through the same engine rather than crashing or leaving a black background.

---

# 22. Background transitions

User-level transition modes are represented by `ye`:

```text
RANDOM
NONE
FADE
ZOOM
SLIDE_H
SLIDE_V
```

The lower rendering/background layer uses `fs` values conceptually equivalent to:

```text
NONE
FADE_CROSS
FADE_ZOOM
FADE_SLIDE_H
FADE_SLIDE_V
```

Random mode chooses among non-NONE transitions.

For drawable/image-type backgrounds, Projectivy integrates with AndroidX Leanback `BackgroundManager`.

VIDEO is managed by its own video surface rather than being forced through `BackgroundManager`.

---

# 23. Dynamic palette / launcher colors

Wallpaper renderers can feed palette/color information back into the launcher when the relevant appearance preference is enabled (`hj3.l1`).

For image backgrounds this is obtained from decoded bitmap content.

For video, Projectivy samples output frames using `PixelCopy`.

This is useful for Aerial-Rows because Aerial videos can provide dynamic title/header colors without provider-specific logic.

The color extraction should live in the renderer/engine layer, not the Aerial provider.

---

# 24. Wallpaper settings screen

The background/settings controller is represented by:

```text
defpackage.we
```

It adapts its controls to the selected provider.

Verified provider-specific controls include:

### Theme

- choose theme
- theme URI

### User image/video

- pick image/file
- choose color
- URI
- video toggle

### Folder

- choose directory/tree
- URI

### Reddit

- predefined source
- custom subreddit/source string

### Generic static providers

Providers with ordinary timed updates can asynchronously fetch candidates and expose those candidates in a wallpaper picker (`ImagePickerActivity`).

This is an important design clue: provider settings and wallpaper-item picking are related but not the same abstraction.

---

# 25. External ACTION_VIEW / ACTION_SEND background import

Named Activity:

```text
com.spocky.projengmenu.ui.launcherActivities.SetBackgroundActivity
```

It accepts image/video content from Android intents.

The URI can arrive from:

```text
Intent.EXTRA_STREAM
```

or the Activity's data URI.

The import helper (`j02`) asks `ContentResolver.getType(uri)`.

Verified classification:

```text
MIME starts with video/ → VIDEO
otherwise               → IMAGE
```

The deeper import/copy routine is partly affected by JADX control-flow reconstruction errors, so this report does not claim exact file-copy semantics where the Java output is uncertain.

The reliable behavior is that Projectivy can accept externally shared/opened image or video URIs and turn them into local wallpaper configuration.

---

# 26. Theme file format

Built-in `.pltheme` resources in the APK include:

```text
res/56.pltheme
res/8d.pltheme
res/9Z.pltheme
res/rd.pltheme
```

They are ZIP archives.

Each includes at least:

```text
manifest
appearance.plbackup
```

and may include:

```text
wallpapers/
```

Observed manifest metadata includes fields conceptually equivalent to:

```text
Projectivy Launcher version
date
author
name
description
URL
credits
```

Built-in theme names observed include:

- One Line to Rule Them All
- Projectivy: Legacy
- Journey
- Forbidden Fruit

Important: `appearance.plbackup` should be treated as a Projectivy backup payload. Direct inspection does **not** justify calling it JSON, so this report deliberately does not assume a textual schema.

---

# 27. Theme export

The export/import coordinator includes:

```text
dj3
```

Theme export creates names resembling:

```text
projectivy-theme-[...].pltheme
```

The output is a ZIP archive using `ZipOutputStream`.

Observed contents:

```text
manifest
appearance.plbackup
optional wallpapers/
```

Wallpaper assets can be embedded when their URIs are exportable through normal file/content access.

Unsupported/nonportable URI types can trigger warnings rather than being silently assumed portable.

---

# 28. Theme import

Named Activity:

```text
ImportThemeActivity extends ImportSettingsActivity
```

It uses the common import pipeline (`dj3`) with a theme-specific mode.

The current theme can subsequently expose its packaged wallpaper files through `ThemeContentProvider`.

---

# 29. ThemeContentProvider

Provider authority:

```text
com.spocky.projengmenu.themeprovider
```

It is read-only to ordinary consumers and serves current-theme wallpaper content.

Observed conceptual URI pattern:

```text
content://com.spocky.projengmenu.themeprovider/current/wallpaper/<encoded-name>
```

Known MIME mappings cover media such as:

```text
jpg/jpeg
png
webp
gif
mp4
mkv
webm
```

The theme provider can read packaged entries from the active theme archive/extracted state and return them as normal Android content URIs.

This avoids making renderers understand ZIP archives.

That is an excellent separation of concerns to preserve.

---

# 30. Important cross-group integrations

Group 6 closes several links discovered in earlier groups.

## From Group 2 — focused cards

```text
home card focus
    ↓
Event.CardFocused
    ↓
Gradient or external provider
```

## From Group 4 — Android TV programs

```text
program focus
    ↓
Event.ProgramCardFocused
    ↓
Selected Program provider
```

The Random TV Program provider also reuses the TV Provider repository directly.

## From Group 5 — media sessions

```text
primary media-session change
    ↓
Event.NowPlayingChanged
    ↓
Now Playing wallpaper provider
```

## Launcher idle state

```text
idle mode changes
    ↓
Event.LauncherIdleModeChanged
    ↓
providers interested in idle state
```

This event bus is the architectural location where an Aerial provider can react to idle state cleanly.

---

# 31. Aerial integration conclusion

The correct integration is **not**:

```text
LauncherScreen
  if aerialEnabled
     create special video player
```

That would duplicate background rendering, lifecycle and error handling.

The Projectivy-like architecture should be:

```text
AerialProvider
       ↓
WallpaperSpec(type = VIDEO)
       ↓
WallpaperEngine
       ↓
VideoRenderer
       ↓
Media3 player / Surface
```

The Aerial provider is responsible for **selection/catalog policy**.

The generic video renderer is responsible for **playback**.

---

# 32. Recommended Aerial-Rows design

## Core model

```text
WallpaperSpec
 ├─ uri
 ├─ type
 ├─ displayMode
 ├─ title
 ├─ source
 ├─ author
 └─ actionUri
```

## Provider contract

```text
WallpaperProvider
 ├─ id
 ├─ name
 ├─ icon
 ├─ supportedEvents
 ├─ cacheDuration
 ├─ settingsDestination?
 ├─ getWallpapers(event)
 └─ invalidate()
```

## Engine

```text
WallpaperEngine
 ├─ selected provider
 ├─ provider registry
 ├─ candidate cache
 ├─ current item
 ├─ event dispatch
 ├─ timed rotation
 ├─ retry/fallback
 └─ Activity lifecycle
```

## Renderer registry

```text
WallpaperRendererRegistry
 ├─ ImageRenderer
 ├─ VideoRenderer
 ├─ ColorRenderer
 ├─ DrawableRenderer
 └─ LottieRenderer
```

## Aerial provider

```text
AerialProvider : WallpaperProvider
 ├─ reads Aerial-Rows built-in aerial catalog
 ├─ maps available videos to VIDEO WallpaperSpec
 ├─ filters by selected locations/categories
 ├─ supports local/cache/network URI policy
 ├─ reacts to TimeElapsed
 ├─ optionally reacts to LauncherIdleModeChanged
 └─ exposes Aerial-specific settings
```

The existing Aerial-Rows `BuiltinAerials` / `aerials.json` data can feed this provider rather than being tied directly to a Composable player.

---

# 33. Screensaver / idle strategy

Aerial-Rows can improve on a literal Projectivy clone while preserving the same architecture.

Instead of maintaining two video systems:

```text
wallpaper video player
+
screensaver video player
```

use one playback engine with policy/state:

```text
launcher active
    → background-mode Aerial behavior

launcher idle
    → immersive Aerial behavior

user interaction
    → restore normal launcher presentation
```

`LauncherIdleModeChanged` is exactly the kind of event needed for this.

The provider can decide whether to rotate/change content; the video renderer does not need to know why it is playing.

---

# 34. What should be reproduced vs improved

## Reproduce

- provider abstraction
- typed wallpaper descriptors
- fixed renderer registry
- image/video/color/Lottie support
- event-driven providers
- timed providers
- provider-specific TTL
- provider update/invalidation path
- retry/fallback
- palette extraction
- theme-contained wallpapers
- user/folder video backgrounds
- TV-program and Now Playing providers

## Improve in Aerial-Rows

- Kotlin sealed models rather than obfuscated integer-enum handling
- explicit `StateFlow`/`SharedFlow` event stream
- a real typed provider-cache object
- optional file/download cache for Aerial media
- clearer renderer lifecycle ownership
- one video engine for wallpaper + immersive idle/screensaver presentation
- optional external plugin compatibility layer without coupling internal providers to Binder

---

# 35. Reliability / ambiguity notes

### High confidence / directly verified

- provider registry architecture
- ten built-in providers
- provider UUIDs listed above
- external provider service intent
- external provider metadata names/defaults
- Binder descriptor and three transaction roles
- provider-update broadcast and 5-second debounce
- Wallpaper fields
- event types and bit values
- wallpaper type enum
- display-mode enum
- exact five-renderer registry initialized by `MainActivity`
- Media3 video renderer
- SurfaceView background rendering
- PixelCopy palette extraction
- retry count of five
- theme ZIP structure
- ThemeContentProvider role
- user image/video MIME classification

### Architecture verified but exact control flow partly decompiler-ambiguous

- coroutine implementation of provider candidate cache refresh
- exact file-copy/persist sequence inside the generic imported-background helper
- portions of external-provider state synchronization coroutine

For those paths, this report intentionally records only behavior confirmed across fields, callers, xrefs and/or smali rather than inventing a source-level reconstruction.

---

# 36. Group 6 conclusion

Projectivy's wallpaper system is already architected in the way Aerial-Rows needs:

```text
content/provider policy
        ≠
rendering/playback
```

The decisive result is:

```text
Aerials belong in a WallpaperProvider,
not in a special LauncherScreen video path.
```

Aerial video items can be ordinary `VIDEO` wallpaper descriptors consumed by the same generic Media3 renderer that handles any other video source.

That gives Aerial-Rows:

- Projectivy-style backgrounds
- Couchy/Aerial content
- event-driven dynamic wallpaper
- media/program-aware providers
- theme support
- future external provider plugins
- one reusable video playback lifecycle

without duplicating player logic or coupling wallpaper selection to home-screen rendering.

---

## Next group

**Group 07 — Settings system**

Targets include:

- complete settings screen hierarchy
- preference/config key inventory
- persistence wrapper architecture
- toggle/choice/list/dropdown widgets
- focus retention after preference changes
- settings navigation/routes
- dependency/visibility rules
- category settings integration
- user-facing value mappings
- preference change propagation to the home screen

