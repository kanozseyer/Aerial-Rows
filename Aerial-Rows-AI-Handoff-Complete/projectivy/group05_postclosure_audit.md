# Group 5 post-closure audit — Notifications & Now Playing

Status: **POST-CLOSURE AUDITED after Groups 6–12**  
Original Group 5 SHA-256: `20e6be9fd02df485749cac62ae4b9826177cf5997b370b02206cc6cab6e5113f`

## Executive conclusion

The original Group 5 report remains architecturally correct.

No later group revealed a second hidden notification/media subsystem. The central split is still valid:

```text
NotificationListenerService
    ├─ notification state/panel
    └─ MediaSessionManager access
            ↓
           iw2
            ↓
           ju2
            ↓
           c73
            ↓
      Now Playing row ID 12
```

The audit found:

- **1 factual overstatement** in the original report;
- **1 genuine completeness omission with important security/policy implications**;
- several precision upgrades useful for the Aerial-Rows implementation;
- no need to redo Group 5 from scratch.

The most important findings are:

1. `NowPlayingChanged` does **not** publish the active session/token identity through the wallpaper IPC event;
2. `c73` Now Playing launch is an exception to Projectivy's normal `t4` parental launch wrapper.

---

# 1. Factual correction — `NowPlayingChanged` IPC does not carry session identity

Group 5 section 17 says the wallpaper event payload includes:

> “selected/active session identity through the snapshot pipeline”

That is too strong.

Projectivy's internal snapshot `ew2` does contain:

```text
activeToken
title
artist
albumTitle
artworkUri
isPlaying
```

and `activeToken` participates in `ew2.equals()` / `hashCode()`.

That means changing the selected session can cause the internal snapshot to be considered different even if other metadata happens to match.

However, when the snapshot is converted to:

```text
Event.NowPlayingChanged
```

Projectivy sends only:

```text
eventType
isPlaying
title
artist
album
iconUri
```

The active token is **not serialized into the wallpaper-provider event**.

Exact event contract:

```text
NowPlayingChanged(
    eventType,
    isPlaying,
    title,
    artist,
    album,
    iconUri
)
```

### Corrected canonical statement

```text
ew2 internal snapshot
    includes active session token
        ↓
token participates in internal change detection
        ↓
Event.NowPlayingChanged
    omits token/session identity
```

### Aerial-Rows improvement

Our internal wallpaper event can keep a structured session identity if it is useful:

```text
NowPlayingChanged(
    sessionId,
    packageName,
    isPlaying,
    title,
    artist,
    album,
    artwork
)
```

while an external plugin API could expose only the minimum stable fields if desired.

---

# 2. Genuine completeness miss — Now Playing bypasses the generic `t4` parental launch wrapper

This is the most important post-closure Group 5 finding.

Generic `t4.f(...)` does:

```text
build destination Intent
        ↓
D() protected?
        ↓ yes
wrap in ParentalControlCheckActivity
        ↓
startActivity
```

`c73` overrides `f(...)`.

Its implementation directly performs:

```text
MediaController/session PendingIntent
        ↓
PendingIntent.send()
        ↓ fallback
owning package launcher PendingIntent
        ↓
PendingIntent.send()
```

It does **not** call:

```text
super.f(...)
```

and it does not reproduce `t4`'s:

```text
D()
→ pq0.M(...)
→ ParentalControlCheckActivity
```

wrapper.

Therefore the normal generic parental launch wrapping does **not** apply automatically to a Now Playing card.

This is independently reinforced by `t4.j()`:

```text
return !(this instanceof c73)
```

and `j6` uses `j()` to decide whether **Add parental control / Remove parental control** actions are available.

For a `c73`:

```text
parental toggle action = disabled
```

### Canonical interpretation

Projectivy 4.72 deliberately/special-cases Now Playing enough that:

```text
ordinary protected t4 card
    -> generic parental launch wrapper

c73 Now Playing card
    -> direct media PendingIntent
    -> no generic parental wrapper
```

The special notification fallback path also calls:

```text
c73.f(...)
```

directly, so it shares this behavior.

### Aerial-Rows improvement

Do **not** clone this exception.

All launcher-originated media launches should still route through:

```text
LaunchGate
    ↓
LaunchDispatcher
```

A sensible policy could inherit protection from the media session's owning package:

```text
protected app package
    -> derived Now Playing card protected
```

This matches the stronger protection model already found for Group 4 TV program cards.

---

# 3. Precision upgrade — Now Playing stable identity is session-level but not durable/collision-proof

Group 5 correctly says `c73.a()` is derived from the session token.

Exact form:

```text
String.valueOf(ju2.l).hashCode()
```

So it is:

- session-level rather than package-level;
- only a Java 32-bit hash;
- based on a string representation of the session token;
- not intended as a durable cross-reboot persistence identity.

This is appropriate for a transient dynamic row but should not be mistaken for a persistent media-card identity system.

### Aerial-Rows improvement

Use a structured runtime identity, for example:

```text
media:<package>:<session-token-derived-runtime-key>
```

and treat it as ephemeral.

Do not persist user customizations against a media-session runtime ID unless there is a separate stable package-level policy.

---

# 4. Precision upgrade — the row deduplicates on the underlying session token object

`w40` source type `8`:

1. requires a connected `NotificationListener`;
2. reads `notificationListener.b().b`, the active `ju2` wrappers;
3. keeps wrappers where `ju2.o()` is active;
4. deduplicates using:

```text
ju2.h()
```

which is the `z54` session token wrapper;
5. maps the unique sessions to Now Playing cards.

Therefore the row's deduplication identity is stronger than the final integer `c73.a()` card ID.

Canonical distinction:

```text
repository/list reconciliation:
    session token wrapper

card integer stable ID:
    hash(String.valueOf(token))
```

Aerial-Rows should simply retain one structured session identity throughout the repository/model layer.

---

# 5. Precision upgrade — embedded artwork support is richer on the card than in the wallpaper event

A `ju2` tracks:

```text
artwork Uri
artwork byte[]
```

`c73` keeps both.

`d73` renders in this order:

```text
artwork URI
    ↓
embedded artwork byte[]
    ↓
default Now Playing icon
```

and byte-array content participates in presenter equality/rebind checks.

However:

```text
Event.NowPlayingChanged
```

contains only:

```text
iconUri: String?
```

It has no artwork byte-array field.

Therefore Projectivy can display embedded metadata artwork on the **Now Playing card** even when the corresponding external wallpaper event cannot carry that embedded image.

### Aerial-Rows improvement

Our internal wallpaper engine does not need this limitation.

Use a common artwork model:

```text
MediaArtwork
 ├─ Uri
 ├─ Bytes/BitmapRef
 └─ None
```

for both the home card and wallpaper provider.

Only down-convert to URI-only representation when crossing an external IPC/plugin boundary that requires it.

---

# 6. Precision upgrade — exact status-bar notification settings are now known

Group 7 closed the settings behind Group 5's title/status notification behavior:

```text
appearance_statusbar_manage_notifications
appearance_statusbar_maincontainer_show_notifications
appearance_statusbar_maincontainer_autohide_notifications
```

mapped as:

```text
hj3.J1
hj3.S1
hj3.T1
```

This strengthens Group 5's statement that the title notification control is driven by shared observable notification state.

The UI decisions are independently configurable:

- expose notification-management action;
- show notification control;
- auto-hide notification control when appropriate.

---

# 7. Precision upgrade — media monitoring is not conceptually tied only to rendering the Now Playing row

The service connection establishes MediaSession monitoring independently from merely drawing category ID `12`.

Later Group 9 proves the same media state is consumed by:

```text
power_internal_idle_excludes_media_playback
```

The idle coordinator can suppress an idle/power transition when meaningful media playback is active.

Therefore:

```text
MediaSessionRepository
```

should be an application/system state service.

The home Now Playing row is only **one consumer**.

Canonical consumers include:

```text
Now Playing row
Now Playing wallpaper provider
idle/power policy
notification ↔ media correlation
```

### Aerial-Rows implication

Do not put MediaSession ownership inside the Now Playing Composable or row source.

---

# 8. Precision upgrade — idle exclusion is only one part of the full idle policy

Group 5 correctly identified:

```text
power_internal_idle_excludes_media_playback
```

Group 9 later closes the larger policy.

Idle can also consider:

- user/key/window activity;
- internal TV Activity;
- configured foreground-app conditions;
- accessibility-observed activity;
- other power settings.

So the canonical relation is:

```text
MediaSessionRepository.isPlaying
        ↓
one input into
LauncherIdleCoordinator
```

not:

```text
media repository itself owns idle behavior
```

This supports Group 12's separate `MediaSessionRepository` and `LauncherIdleCoordinator`.

---

# 9. Precision upgrade — `ProgramCardFocused` on a Now Playing card is a formal wallpaper event

Group 5 found that `d73.n(...)` publishes:

```text
Event.ProgramCardFocused
```

Group 6 gives the formal provider event model:

```text
ProgramCardFocused = 8
NowPlayingChanged  = 2
```

This means there are two distinct media-related wallpaper signals:

## Global session state

```text
MediaSessionRepository
    ↓ ~500 ms coalescing
NowPlayingChanged
```

## User focus state

```text
focused c73
    ↓
ProgramCardFocused
```

A wallpaper provider can react differently to "playback changed" versus "the user selected this particular session card."

For Aerial-Rows these should be semantic events, not direct UI-to-player calls.

---

# 10. Precision upgrade — Now Playing wallpaper provider identity

Group 6 identifies Projectivy's built-in Now Playing wallpaper provider:

```text
class: e73
UUID: b7ac18ab-d18d-4587-8ed7-50af45541033
update mode: NowPlayingChanged = 2
```

Its conceptual input is the Group 5 global media snapshot/event.

This confirms the home row and wallpaper provider are parallel consumers of one media-session state pipeline.

No duplicate media-session discovery subsystem exists inside the wallpaper engine.

---

# 11. Notification ↔ session matching is even weaker than a package correlation

Group 5 correctly calls `l63.b()` pragmatic.

The actual correlation is:

```text
notification tag == "Notification.NowPlaying"
        ↓
get category ID 12
        ↓
scan c73 cards
        ↓
compare c73.A() display title with notification title
```

It does **not** verify the notification package against the session package in this matching method.

Therefore two active sessions with identical display titles could theoretically be mis-correlated.

### Aerial-Rows improvement

Correlation preference should be:

```text
session/package identity
    ↓
notification package
    ↓
known media-session relation
    ↓
title fallback only as last resort
```

This is especially important because dismissing the linked notification can issue a real session Stop.

---

# 12. Precision upgrade — dismissing a linked notification is a playback-affecting action

The original report already captured this, but its consequence deserves stronger wording.

For a dismissable special `Notification.NowPlaying` item:

```text
dismiss
    ↓
resolve matching c73
    ↓
c73.I()
    ↓
controller Stop when supported
    ↓ fallback Pause on stop failure
    ↓
delete PendingIntent
    ↓
cancel notification
```

Thus dismissing the notification is not necessarily a purely cosmetic notification operation.

Aerial-Rows should **not assume this behavior is desirable**.

Recommended separation:

```text
Dismiss notification
```

and:

```text
Stop playback
```

should be independent actions unless a specific provider integration explicitly links them.

---

# 13. Precision upgrade — primary-session identity affects internal coalescing but not the external event payload

`iw2.b()` primary selection remains:

```text
1. first playing session
2. else first active/non-terminal session
3. else last session
```

The selected token is included in `ew2`.

Since `ew2.equals()` includes the token:

```text
session A -> session B
```

can produce a new internal snapshot even when title/artist/album/art URI happen to be identical.

But after conversion to `NowPlayingChanged`, external consumers cannot distinguish those sessions by token.

This is another reason to separate:

```text
internal state identity
```

from:

```text
external wallpaper event schema
```

in Aerial-Rows.

---

# 14. Media-command availability remains correctly capability-gated

No later group changes Group 5's finding.

Projectivy exposes:

```text
Play/Pause
Stop
Previous
Next
```

only where the underlying Media3/controller command is supported.

This remains worth preserving.

Aerial-Rows should not render a control merely because the card type is Now Playing; action availability should derive from current controller capabilities.

---

# 15. Negative finding — notification access and MediaSession access remain separate concerns

Group 5 correctly established that Projectivy obtains MediaSession access through:

```text
MEDIA_CONTENT_CONTROL
OR
enabled NotificationListener component
```

The NotificationListener service is therefore the practical permission bridge on normal installs.

But this does not mean Now Playing is reconstructed from notification text.

Later groups reinforce the original core rule:

> **Do not build Now Playing from notifications alone.**

The actual media state/control source remains active MediaSessions.

---

# 16. Exact status of Now Playing parental behavior

After Groups 3, 4 and 10, the card-policy picture is now:

```text
ordinary t4 cards
    -> generic D() protected-ID check
    -> ParentalControlCheckActivity wrapper

TV program cards / ck3
    -> provider-package protection OR card protection
    -> generic t4 wrapper

Now Playing / c73
    -> j() == false, so parental-toggle UI disabled
    -> f() overrides generic t4 launch
    -> direct media PendingIntent
```

This is a notable inconsistency in Projectivy 4.72's launch-policy coverage.

### Aerial-Rows requirement

Improve it deliberately:

```text
all actionable HomeCards
    ↓
LaunchGate
```

with source-specific inherited policy where appropriate:

```text
NowPlaying(package=X)
    inherits package X parental policy
```

---

# 17. Things Group 5 correctly deferred — not misses

These were correctly left to later groups:

- full wallpaper provider/renderer architecture -> Group 6;
- exact settings key schema -> Group 7;
- idle/power decision engine -> Group 9;
- parental policy persistence/authentication -> Group 10;
- final implementation mapping -> Group 12.

Those are clean subsystem boundaries rather than Group 5 omissions.

---

# 18. Revised canonical media architecture

With later groups folded back in:

```text
NotificationListenerService
│
├─ filtered notification repository
│   ├─ full current state
│   ├─ posted/removed/ranking events
│   ├─ group-summary bookkeeping
│   └─ notification side panel/status badge
│
└─ MediaSession bridge
    ↓
MediaSessionRepository / iw2
│
├─ active token reconciliation
├─ package denylist
├─ ju2 controller wrappers
├─ active-session filtering
├─ token-level dedup
├─ primary-session selection
└─ ~500 ms coalescing
    │
    ├─ Now Playing row
    │   └─ c73
    │      ├─ transient session identity
    │      ├─ URI/embedded-byte artwork
    │      ├─ media controls
    │      └─ direct session PendingIntent launch
    │
    ├─ internal ew2 snapshot
    │   ├─ token
    │   ├─ metadata
    │   └─ playback state
    │
    ├─ Event.NowPlayingChanged
    │   ├─ isPlaying
    │   ├─ title
    │   ├─ artist
    │   ├─ album
    │   └─ iconUri
    │       (NO token; NO artwork bytes)
    │
    └─ idle-policy media-playing signal

Focused c73
    ↓
Event.ProgramCardFocused
    ↓
wallpaper provider coordinator
```

---

# 19. Effect on Aerial-Rows implementation plan

The Group 12 implementation sequence remains correct.

The Group 5 audit strengthens Phase 7 with these requirements:

1. Keep notifications and media sessions as separate repositories.
2. Keep the MediaSession repository application-scoped.
3. Use structured runtime session IDs rather than integer hashes.
4. Keep richer internal session identity than any external wallpaper-event schema.
5. Support URI **and embedded artwork** through one media-artwork abstraction.
6. Correlate notifications to sessions by package/session identity before title fallback.
7. Do not automatically stop playback merely because a notification was dismissed unless explicitly desired.
8. Route Now Playing launches through Aerial-Rows' central `LaunchGate` even though Projectivy does not.
9. Inherit app/package parental policy onto derived Now Playing cards.
10. Expose controller commands according to capability.
11. Feed playback state into the separate idle coordinator.
12. Preserve separate global `NowPlayingChanged` and focused-card events.

---

# Final assessment

```text
Core Group 5 architecture:           correct
Missing major subsystem:             none

Factual correction:                  1
  - NowPlayingChanged IPC event does not include session/token identity

Genuine completeness omission:       1
  - c73 Now Playing launch bypasses generic parental t4 wrapper

Important precision upgrades:
  - session ID is transient 32-bit token-string hash
  - token-level dedup is stronger than card integer ID
  - card supports artwork bytes; wallpaper event does not
  - exact status-bar notification settings now known
  - media state is app/system state, not row-owned
  - notification/session title matching has no package check
  - notification dismissal can stop/pause media
  - exact Now Playing wallpaper-provider linkage
  - internal snapshot token affects change detection but is omitted from IPC

Need to redo Group 5:                 no
Effect on Group 12 plan:              strengthens Phase 7, no reorder required
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 6–12`.**
