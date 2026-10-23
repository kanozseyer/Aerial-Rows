# Group 6 post-closure audit — Wallpaper, Themes & Aerial Integration

Status: **POST-CLOSURE AUDITED after Groups 7–12**  
Original Group 6 SHA-256: `a227c5e9a123244f2383b45b30337692457562891deba97194bb5dc2edacc5fd`

## Executive conclusion

The original Group 6 architecture remains correct.

No later group revealed a second hidden wallpaper engine or a competing Aerial/video subsystem. The central result still holds:

```text
provider/source policy
        ≠
renderer/playback
```

with:

```text
WallpaperProvider
    ↓ Wallpaper descriptor
bs4 engine/coordinator
    ↓
fixed renderer registry
    ├─ IMAGE
    ├─ VIDEO
    ├─ COLOR
    ├─ DRAWABLE / ANIMATED_DRAWABLE
    └─ LOTTIE
```

The post-closure audit found no architectural reversal, but it did add several useful runtime details that were not fully captured in the original report.

Most important additions:

1. exact timed `TimeElapsed` scheduling behavior;
2. exact minimum-width / wrong-format fallback policy;
3. exact video audio, tunneling and decoder-fallback wiring;
4. dedicated video dynamic-color sampling cadence;
5. exact suppression behavior for timed updates while video is playing;
6. the remote Play/Pause wallpaper-advance shortcut;
7. a small additional external-provider validation rule;
8. stronger theme-package semantics from Group 10.

---

# 1. Genuine completeness upgrade — exact wallpaper update-delay semantics

Group 6 recorded the existence of timed provider updates but did not fully close the global scheduling policy.

The setting is:

```text
appearance_wallpaper_update_delay
```

with:

```text
default = 60 seconds
min     = 0
max     = 3600 seconds
step    = 15 seconds
```

`bs4.h()` converts the stored number of seconds into milliseconds.

The recurring scheduler uses:

```text
elapsed = now - lastWallpaperEventTime
remaining = updateDelay - elapsed
```

and, when scheduling another tick, enforces a minimum remaining delay of:

```text
5,000 ms
```

unless the configured update delay is effectively disabled.

The smali-verified scheduler behavior is conceptually:

```text
if updateDelay == 0 and prior event time exists:
    do not schedule another periodic timer

else:
    nextDelay = max(updateDelay - elapsedSinceLastEvent, 5000 ms)
    schedule TimeElapsed
```

After the timer fires:

```text
Event.TimeElapsed
    ↓
provider event path
    ↓
scheduler interval resets from current update-delay setting
```

The timing holder also observes relevant state changes so the interval can be updated without rebuilding the entire wallpaper engine.

### Aerial-Rows implication

Use one explicit scheduler owned by the wallpaper engine:

```text
WallpaperRotationScheduler
```

with:
- configured interval;
- monotonic next-run calculation;
- immediate cancellation when interval becomes zero;
- no duplicate timers;
- a modest minimum reschedule floor to avoid update storms.

---

# 2. Genuine completeness upgrade — “ignore updates while video is playing” is narrowly scoped

The setting is:

```text
appearance_wallpaper_ignore_update_on_video_playing
```

default:

```text
true
```

The exact suppression occurs inside `bs4.r(Event)`.

Projectivy drops an event only when all of these are true:

```text
a previous wallpaper event timestamp exists
AND
the video renderer reports a video is actively playing
AND
event type == TimeElapsed
AND
ignore-update-on-video-playing == true
```

So this option does **not** suppress all wallpaper events while video is playing.

It suppresses the periodic:

```text
TimeElapsed
```

rotation/update event.

Events such as:
- focused-card changes;
- Now Playing changes;
- idle-state changes;
- loading errors;

are not covered by this particular suppression branch.

### Aerial-Rows implication

Model this as:

```text
suppressTimedRotationWhileVideoPlaying
```

rather than a vague global `ignoreWallpaperUpdates` flag.

---

# 3. Precision upgrade — the video renderer changes repeat policy based on timed-update behavior

When the Media3 player is created, Projectivy sets its repeat mode according to:

```text
ignore-update-on-video-playing
+
wallpaper update delay
```

The effective logic is:

```text
if ignoreTimedUpdatesWhileVideoPlaying == false
   OR updateDelay == 0:
       repeat mode = ONE
else:
       repeat mode = OFF
```

This is consistent with two modes:

```text
loop the current video indefinitely
```

versus:

```text
allow the current video lifecycle/end state to participate in wallpaper rotation
```

The exact end-state callback still belongs to the generic renderer/engine retry/next-wallpaper logic.

### Aerial-Rows improvement

Make looping policy explicit in the provider/renderer contract rather than deriving it indirectly from two unrelated-looking preferences.

For example:

```text
VideoPlaybackPolicy(
    loop = true/false,
    rotateOnTimedEvent = true/false
)
```

---

# 4. Genuine completeness upgrade — exact minimum-width / bad-format image policy

Two settings control this behavior:

```text
appearance_wallpaper_min_accepted_width
appearance_wallpaper_wrong_format_display_mode
```

The minimum-width setting is:

```text
default = 800 px
min     = 0
max     = 4000 px
step    = 100 px
```

The normal global display mode is:

```text
appearance_wallpaper_display_mode
```

and defaults to:

```text
Automatic
```

For the transformation path where format validation is enabled, Projectivy considers the image acceptable for the normal display mode when:

```text
display mode == FIT_CENTER
```

or:

```text
image aspect ratio > 1.0
AND
bitmap width >= minimum accepted width
```

Otherwise it applies the separate:

```text
wrong-format display mode
```

The wrong-format mode defaults to:

```text
BLUR
```

The enum's first/default slot is relabelled as:

```text
Skip
```

in this particular settings context.

At transform time that slot produces no replacement bitmap, effectively causing the unwanted bitmap to be skipped.

### Canonical policy

```text
normal landscape / wide-enough image
    → normal wallpaper display mode

portrait, square, or too-narrow image
    → wrong-format fallback mode
        ├─ crop/stretch/etc.
        ├─ blurred presentation
        └─ skip
```

### Aerial-Rows implication

This belongs in the **renderer/image-policy layer**, not in each provider.

An Aerial provider should never need to know about portrait-image fallback.

---

# 5. Genuine completeness upgrade — video volume 0 means disable audio track selection

The setting is:

```text
appearance_wallpaper_video_volume
```

with:

```text
default = 0.0
range   = 0.0 .. 1.0
step    = 0.05
```

Projectivy does more than simply call:

```text
player.setVolume(0)
```

When the configured volume is exactly zero, it configures the track selector to disable track type:

```text
1
```

which is the Media3 audio track type.

So zero-volume video wallpaper effectively avoids selecting audio tracks.

When volume is greater than zero:

```text
audio track selection is allowed
```

and Projectivy calls the player's volume setter when the player reports that command as available.

### Aerial-Rows improvement

Keep this optimization.

For silent Aerial wallpaper:

```text
do not select/decode audio
```

is better than:

```text
decode audio then multiply it by zero
```

for resource usage on weaker Android TV hardware.

---

# 6. Genuine completeness upgrade — tunneling and decoder fallback are real Media3 construction settings

Two settings are directly wired into player construction:

```text
appearance_wallpaper_video_tunneling
appearance_wallpaper_video_decoders_fallback
```

Defaults:

```text
video tunneling       = true
decoder fallback      = false
```

The tunneling preference is passed into the Media3 track-selector configuration.

The decoder-fallback preference is passed into the renderer-factory configuration used by the ExoPlayer builder.

This is not merely UI metadata; both settings affect the actual player instance created by `jy4`.

### Aerial-Rows implication

These can remain advanced video options, but they belong under:

```text
VideoRendererConfig
```

rather than the Aerial provider itself.

---

# 7. Genuine completeness upgrade — dedicated dynamic-color update frequency for video

Group 6 correctly found that video colors are sampled with `PixelCopy`, but did not record the dedicated cadence control.

The setting is:

```text
appearance_wallpaper_video_dynamic_color_update_frequency_nexp
```

The renderer captures:

```text
frequencySeconds * 1000
```

at construction.

Its default is:

```text
5 seconds when Projectivy's internal capability predicate is true
0 otherwise
```

and its configured range extends up to:

```text
60 seconds
```

When:
- dynamic wallpaper colors are enabled;
- video playback is active;
- frequency is non-zero;

Projectivy starts a coroutine that repeatedly triggers the PixelCopy/palette path.

If frequency is zero, the renderer avoids that repeated sampling path and falls back to its non-periodic color behavior.

The `_nexp` suffix also matters after Group 10:

```text
nexp settings are excluded from exported backup/theme preference payloads
```

So this video palette cadence is intentionally treated as local/non-exported configuration.

### Aerial-Rows improvement

Keep palette sampling independently configurable from:
- video playback;
- wallpaper rotation;
- provider update frequency.

They solve different problems.

---

# 8. Precision upgrade — exact meaning of the global video-playing flag

Group 6 identified:

```text
bs4.M
```

as the global “video wallpaper playing” state.

The post-closure audit closes its lifecycle more explicitly.

`jy4` sets:

```text
bs4.M = true
```

when the renderer transitions into active playback while the owning Activity is in the appropriate lifecycle state.

It sets:

```text
bs4.M = false
```

when playback is stopped/released through the renderer shutdown path.

The flag is then consumed by `bs4.r()` for the timed-update suppression described above.

So `bs4.M` is not a generic:
```text
selected wallpaper type == VIDEO
```

flag.

It represents active video playback state.

---

# 9. Precision upgrade — manual `TimeElapsed` trigger from the remote

Group 9 later exposed a useful input bridge that belongs in the canonical Group 6 event model.

Inside `MainActivity.onKeyDown()`:

```text
KEYCODE_MEDIA_PLAY_PAUSE (85)
```

on:

```text
repeatCount == 1
```

checks whether the active wallpaper provider accepts:

```text
Event.TimeElapsed
```

If it does, Projectivy dispatches that event through the wallpaper coordinator and consumes the key.

Conceptually:

```text
hold / first repeat of Play-Pause
    ↓
provider supports TimeElapsed?
    ↓ yes
manual wallpaper TimeElapsed event
```

This effectively gives compatible wallpaper providers a remote-driven “advance/update” gesture.

### Aerial-Rows option

This is worth keeping as an optional configurable remote shortcut, but it should live in the generic remote-action mapping system rather than being hard-coded into `MainActivity`.

---

# 10. Precision upgrade — loading error remains exempt from provider event-subscription filtering

`bs4.r(Event)` normally checks:

```text
selectedProvider.b(event)
```

to determine whether the provider is interested in an event.

However:

```text
Event.LoadingError
```

is allowed through even when the provider's ordinary event mask does not include it.

This matches Group 6's retry model:

```text
renderer/load failure
    ↓
retry up to five times
    ↓
LoadingError
    ↓
fallback / next-wallpaper path
```

So loading errors are an engine-level reliability signal rather than merely one optional provider subscription.

---

# 11. Precision upgrade — external providers also pass an in-memory blacklist check

Group 6 already verified:

```text
apiVersion == 1
UUID length == 36
```

for external wallpaper-provider plugins.

The provider discovery path also checks the UUID against:

```text
MainActivity.s0
```

and rejects the provider when the UUID appears in that in-memory blacklist.

The audit does not establish how every blacklist entry is populated, so the safe canonical claim is only:

> An additional runtime UUID blacklist check exists during external-provider discovery.

### Aerial-Rows implication

If we eventually support an external wallpaper-provider API, use an explicit compatibility/denylist layer with documented reasons rather than an opaque global list.

---

# 12. Precision upgrade — provider update broadcasts and timed rotation are separate mechanisms

This distinction was implicit in Group 6 but is useful to make explicit.

## Provider update broadcast

```text
tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED
```

means conceptually:

```text
the provider's candidate data changed
```

and Projectivy:
- debounces it for 5 seconds per provider;
- invalidates the matching cache;
- refreshes immediately when relevant.

## `TimeElapsed`

means conceptually:

```text
the wallpaper engine's rotation/update interval elapsed
```

and is controlled by:

```text
appearance_wallpaper_update_delay
```

These should remain different mechanisms in Aerial-Rows.

Do not implement provider catalogue refreshes by waiting for the wallpaper rotation timer.

---

# 13. Theme-package semantics strengthened by Group 10

Group 6 correctly identified:

```text
.pltheme ZIP
├─ manifest
├─ appearance.plbackup
└─ wallpapers/   optional
```

Group 10 closes the format further.

The theme manifest is ordinary JSON with typed fields including:

```text
projectivyLauncherVersion
themeVersion
date
author
name
description
URL
credits
```

`appearance.plbackup` is:

```text
an encrypted appearance-settings backup payload
```

using Projectivy's backup machinery, not a plain JSON settings document.

Theme scope is deliberately different from a full user backup.

A theme can include:
- appearance preferences;
- wallpaper-provider selection;
- selected category appearance/configuration;
- optional wallpaper assets;

while user content/membership/exclusions are treated separately so applying a theme does not simply replace the user's home content model.

### Canonical distinction

```text
full backup
    = user state

theme
    = presentation / appearance package
```

### Aerial-Rows implication

Preserve exactly that conceptual separation even though our own file format should use cleaner authenticated/versioned serialization.

---

# 14. Idle integration — what is directly verified vs architectural conclusion

Group 6 identified:

```text
LauncherIdleModeChanged
```

as a first-class wallpaper event.

Group 9 directly verifies the **exit** path:

```text
user key/touch interaction while launcher idle
    ↓
MainActivity.F()
    ↓
LauncherIdleModeChanged(false)
    ↓
rows/title/status UI fades back in
```

The combined Group 6 + Group 9 architecture supports using the same wallpaper/video renderer for an immersive idle presentation.

The safest canonical statement is:

```text
idle state is communicated semantically to the wallpaper provider/engine
```

and not:

```text
Projectivy launches a separate Aerial/video player
```

No later group found a second video renderer dedicated to idle mode.

### Aerial-Rows design remains valid

```text
one Media3 video renderer
    ↓
normal wallpaper presentation
    ↕ idle-state event
immersive Aerial presentation
```

---

# 15. Precision upgrade — dynamic-color sampling is renderer state, not provider state

The later raw verification reinforces an important design rule.

The following all live in `jy4` / rendering infrastructure:

```text
PixelCopy
palette extraction
sampling cadence
SurfaceView dimensions
temporary bitmap reuse
sampling thread
```

The provider simply produces:

```text
VIDEO Wallpaper
```

Therefore an Aerial provider should never contain:
- PixelCopy logic;
- palette extraction;
- SurfaceView ownership;
- ExoPlayer construction.

This separation is one of the strongest parts of Projectivy's architecture and should be preserved.

---

# 16. Negative verification — no hidden second Aerial/video subsystem appeared later

Groups 7–12 did not uncover:
- a second video player;
- a dedicated screensaver-only Aerial engine;
- a separate idle video Activity;
- provider-specific ExoPlayer ownership.

The five-renderer registry from Group 6 remains the authoritative rendering architecture.

This means the Group 12 recommendation is still correct:

```text
BuiltinAerials
    ↓
AerialProvider
    ↓
Wallpaper(type = VIDEO)
    ↓
shared Media3 VideoRenderer
```

---

# 17. Things Group 6 correctly deferred — not misses

The following remained appropriate subsystem boundaries:

- exact preference schema and settings UI → Group 7;
- idle/remote gesture state machine → Group 9;
- backup/theme encryption and export scope → Group 10;
- final implementation mapping → Group 12.

Later findings enrich Group 6 but do not invalidate its scope.

---

# 18. Revised canonical wallpaper architecture

With Groups 7–12 folded back in:

```text
WallpaperEngine / bs4
│
├─ ProviderRegistry
│   ├─ built-ins
│   └─ external Binder providers
│       ├─ API version check
│       ├─ UUID validation
│       └─ runtime UUID blacklist
│
├─ EventBus
│   ├─ TimeElapsed
│   ├─ NowPlayingChanged
│   ├─ CardFocused
│   ├─ ProgramCardFocused
│   ├─ LauncherIdleModeChanged
│   └─ LoadingError
│
├─ RotationScheduler
│   ├─ update delay, default 60 s
│   ├─ 0 disables periodic scheduling
│   ├─ 5 s minimum reschedule floor
│   └─ optional suppression while video plays
│
├─ CandidateCache
│   ├─ provider identity
│   ├─ provider-specific TTL
│   └─ provider-update invalidation
│
├─ RendererRegistry
│   ├─ Image
│   ├─ Video
│   ├─ Color
│   ├─ Drawable / AnimatedDrawable
│   └─ Lottie
│
└─ Presentation policy
    ├─ normal display mode
    ├─ minimum accepted image width
    ├─ wrong-format fallback
    ├─ transition
    └─ dynamic color
```

Video renderer:

```text
jy4
├─ SurfaceView
├─ Media3 / ExoPlayer
├─ RTSP path
├─ decoder fallback config
├─ tunneling config
├─ true audio-track disable at volume 0
├─ configurable positive volume
├─ repeat/rotation policy
├─ PixelCopy palette sampling
├─ configurable palette cadence
├─ active-playing state
└─ lifecycle/retry/fallback integration
```

---

# 19. Effect on Aerial-Rows implementation plan

The Group 12 implementation order remains correct.

The Group 6 audit strengthens Phase 5 with these requirements:

1. Keep Aerial as a **provider**, not a renderer.
2. Keep one shared Media3 video renderer for every VIDEO provider.
3. Give timed wallpaper rotation its own scheduler.
4. Distinguish timed rotation from provider-catalogue invalidation.
5. Make “ignore updates while video plays” specifically suppress timed rotation.
6. Explicitly model video loop/rotation policy.
7. Disable audio track selection entirely when video volume is zero.
8. Keep tunneling and decoder fallback renderer-level advanced options.
9. Keep palette sampling cadence independent from wallpaper rotation cadence.
10. Apply portrait/too-small image fallback in the image renderer.
11. Support a real `Skip` fallback for unacceptable images.
12. Keep remote-driven wallpaper advance as an optional generic remote action.
13. Preserve theme vs full-backup separation.
14. Keep idle state as a semantic wallpaper event and reuse the same renderer.
15. Keep external plugin validation explicit and versioned.

---

# Final assessment

```text
Core Group 6 architecture:             correct
Missing major subsystem:               none
Factual reversals:                      none

Genuine completeness upgrades:
  - exact update-delay scheduler policy
  - exact video-playing TimeElapsed suppression
  - exact bad-format/min-width image policy
  - true audio-track disable at volume 0
  - tunneling + decoder-fallback runtime wiring
  - dynamic video palette sampling cadence
  - remote Play/Pause TimeElapsed trigger
  - external-provider runtime blacklist check

Later-group refinements:
  - full theme manifest/schema
  - encrypted appearance.plbackup semantics
  - theme vs full-backup scope
  - verified idle-exit event relationship

Need to redo Group 6:                   no
Effect on Group 12 plan:                strengthens Phase 5, no reorder required
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 7–12`.**
