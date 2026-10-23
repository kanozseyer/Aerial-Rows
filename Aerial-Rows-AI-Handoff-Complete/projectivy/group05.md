# Projectivy 4.72 — Group 05: Notifications & Now Playing

Status: **COMPLETE + VERIFIED**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release(3).apk`  
SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

## Scope

This group closes two related but distinct Projectivy subsystems:

1. the Android notification-listener / notification side-panel pipeline; and
2. the MediaSession/Media3-based **Now Playing** home row and its controls.

It also records their intentional cross-links: notification dismissal can stop a linked Now Playing session, media state updates the home row and wallpaper-provider contract, and media playback participates in Projectivy's idle/power logic.

---

## Analysis workflow used

Group 5 uses the optimized RE workflow introduced after Group 4:

1. retain the already verified APK identity;
2. use a persistent decompiled source cache (`/mnt/data/projectivy_decompiled/sources`) for fast local search;
3. use a direct JADX Java-API index for class hierarchy/xrefs when the bulk cache is insufficient;
4. batch-export missing high-value classes in a **single APK load** rather than reopening the APK per class;
5. inspect only the Projectivy-specific cluster;
6. use smali/bytecode as the authority where JADX's Java reconstruction is ambiguous.

The source cache currently contains thousands of classes and allows `ripgrep`-style discovery without repeatedly invoking MCP over the complete 7,395-class APK. A selected-class dumper was added during this group so missing classes can be exported together after one ~single APK load.

This is both faster and more reliable: broad discovery happens locally, while exact control-flow claims are still checked against JADX xrefs or smali when needed.

---

# 1. Top-level architecture

The central chain for Now Playing is:

```text
NotificationListener
        ↓
       iw2                  media-session coordinator
        ↓
       ju2                  one active media-session/controller wrapper
        ↓
       c73 : t4             one Now Playing home card
        ↓
   i5 category ID 12
   source type 8
```

The ordinary notification-panel chain is separate:

```text
NotificationListenerService callbacks
        ↓
notification event/state flows
        ↓
       b73                  notifications Preference fragment
        ↓
       z63                  dynamic grouping/rendering
        ↓
NotificationPreference / l63
```

The two branches meet when Android emits a special `Notification.NowPlaying` notification: its `l63` model can resolve the corresponding `c73` card and operate on the linked media session.

---

# 2. `NotificationListener`

Class:

```text
com.spocky.projengmenu.services.notification.NotificationListener
    extends android.service.notification.NotificationListenerService
```

This is a genuine Android notification-listener service rather than a polling component.

Important static state includes:

- `H` — current connected service instance;
- `I` — connected flag;
- `C` — posted/raw event stream;
- `D` — removal stream;
- `E` — wrapper/derived removal flow;
- `F` — current filtered active-notification list;
- `G` — exposed/read-only-style wrapper around that list.

Important instance state includes:

- coroutine scope;
- reusable `NotificationListenerService.Ranking`;
- maps used for notification-group bookkeeping;
- key currently being explicitly cancelled;
- lazy `iw2` media-session coordinator;
- helper lifecycle state;
- package allowlist containing `com.android.systemui` and Projectivy itself.

## Lifecycle

`onListenerConnected()`:

1. records the singleton service instance;
2. marks the listener connected;
3. when Projectivy's global feature gate is enabled, triggers a complete notification refresh;
4. connects media-session monitoring via `e()`.

`onListenerDisconnected()`:

1. clears connected/global instance state;
2. resets active notification state/flows;
3. clears notification-group maps;
4. disconnects media-session monitoring.

`onNotificationPosted`, `onNotificationRemoved`, and `onNotificationRankingUpdate` are dispatched asynchronously into Projectivy's event pipeline.

Projectivy wraps `getActiveNotifications()` defensively: `SecurityException`/runtime failures produce an empty array rather than crashing the launcher.

---

# 3. Notification filtering

The filter method is decompiler-hostile, so its important conditions were checked against smali rather than inferred from JADX's duplicated/partially malformed Java output.

The intent is to keep the side panel focused on useful user-facing notifications while suppressing noisy service/system entries.

Verified filtering factors include:

- `NotificationListenerService.Ranking.canShowBadge()` on newer Android versions;
- special allowance for Projectivy/SystemUI where appropriate;
- suppression of certain ongoing `miscellaneous`-channel notifications;
- recognition of the tag `Notification.NowPlaying`;
- recognition of tags beginning with `com.android.server`;
- foreground-service / ongoing-notification rules;
- rejection of group-summary entries;
- requirement for usable notification title/text content.

The exact filter differs across Android-version branches, so an Aerial-Rows implementation should represent it as a policy function rather than scattering flag checks through the UI.

---

# 4. Notification event model

Projectivy converts framework callbacks into internal event objects rather than having UI screens consume the service directly.

Observed event classes include:

- `w63` — posted notification;
- `y63` — removed notification;
- `x63` — ranking update;
- `u63` — explicit cancel-by-key request;
- `v63` — full refresh request.

Supporting identity helpers reduce notifications to package/user and notification-key identities.

The event consumer updates both incremental event flows and the current complete filtered list. As a result, consumers can choose between:

```text
full current state
```

and

```text
incremental posted/removed events
```

without re-querying the platform themselves.

---

# 5. Notification grouping / summaries

`NotificationListener.g(StatusBarNotification)` maintains explicit grouping state.

For grouped notifications Projectivy tracks, among other things:

- summary key;
- child keys;
- key-to-group mapping.

When children disappear it can clean the corresponding summary state and, where appropriate, cancel a stale summary. This prevents the side panel from retaining orphan group-summary items.

This is a data-layer responsibility, not merely a visual grouping trick in the settings fragment.

---

# 6. Notifications side panel

Entry activity:

```text
NotificationsSidePanelActivity
```

Its actual screen is built by the Projectivy preference-fragment infrastructure. `b73` supplies:

```text
R.xml.settings_notifications
```

The XML root itself is intentionally almost empty. Notifications are generated dynamically.

`b73` starts collectors for:

- current full list (`NotificationListener.G`);
- removal events (`NotificationListener.E`).

`z63` handles the actual UI reconciliation.

## Full-list rendering

The panel:

1. groups notifications by provider package/application label;
2. stores groups in a case-insensitive `TreeMap`, giving alphabetically ordered provider groups;
3. sorts items inside each provider group by notification post time, newest first;
4. creates a `PTPreferenceCategory` for each provider;
5. creates one `NotificationPreference`/`l63` model per notification;
6. adds **Clear all** when at least two notifications are present.

The removal collector performs incremental cleanup rather than rebuilding everything unnecessarily:

- remove the dismissed preference;
- remove its category if the category becomes empty;
- remove **Clear all** when the list falls below two entries;
- handle/close an empty panel appropriately.

---

# 7. `l63` notification UI model

`l63` is the model/action wrapper behind a notification preference.

It extracts:

- notification key;
- title (`android.title`);
- text (`android.text`);
- content intent;
- delete intent;
- icon;
- auto-cancel state;
- dismissable/ongoing state;
- whether the tag is `Notification.NowPlaying`.

On Android TV it also understands `android.tv.EXTENSIONS`, including TV-specific content/delete intents.

## Icon selection

The model prefers an appropriate large icon when present, otherwise falls back to the notification's small icon. If no usable drawable exists it falls back to Projectivy's notification/Now Playing icons.

## Click

If a content `PendingIntent` exists, Projectivy sends it using a clip-reveal animation bundle. Auto-cancel notifications are then dismissed through the model.

If no content intent exists and the item is a linked `Notification.NowPlaying` notification, Projectivy can instead launch the matching `c73` Now Playing session/card.

## Dismiss

For dismissable notifications:

1. find and stop a linked Now Playing card if applicable;
2. send the notification delete intent if present;
3. ask the connected `NotificationListener` to cancel the notification by key.

This establishes a real bidirectional link between the notification subsystem and media-session subsystem.

---

# 8. Main-title notification indicator

`MainTitleView.k()` reads the size of the current filtered notification list (`NotificationListener.G`).

The notification button is controlled by Projectivy settings including:

- show notifications control;
- hide the control when the notification list is empty.

The badge renders:

```text
0..9 → numeric text
10+  → "+"
```

Pressing it opens `NotificationsSidePanelActivity`.

Therefore the notification button in the Projectivy title/status area is driven by the same observable state as the side panel; it is not separately counting system notifications.

---

# 9. Media-session access

When the notification listener connects, `NotificationListener.e()` establishes media monitoring.

It constructs a `ComponentName` referring to its own `NotificationListener`, obtains Android's `MediaSessionManager`, and requires either:

- `MEDIA_CONTENT_CONTROL`, or
- valid enabled notification-listener access.

It then:

1. calls `MediaSessionManager.getActiveSessions(componentName)`;
2. converts the returned sessions/tokens through the bundled media support layer;
3. passes them into `iw2`;
4. registers an active-sessions-changed listener.

Security failures are caught rather than taking down the launcher.

This explains why Projectivy can discover sessions from other applications without implementing a proprietary playback protocol.

---

# 10. `iw2` — media-session coordinator

`iw2` owns the current session list:

```text
CopyOnWriteArrayList<ju2>
```

It also contains a package exclusion list. One verified excluded provider is:

```text
com.humaxdigital.tv.freeview.tvapp
```

When the active-session list changes, Projectivy:

1. detaches/releases old `ju2` wrappers;
2. clears the active list;
3. filters excluded package tokens;
4. asynchronously builds/attaches controllers for the remaining tokens;
5. subscribes to their callbacks;
6. emits a media-state update.

## Primary session selection

`iw2.b()` selects the session used for the global Now Playing snapshot in this priority order:

1. first session whose `ju2.q()` reports playing;
2. otherwise first session whose `ju2.o()` reports active/non-terminal playback state;
3. otherwise the last session in the current list.

This is distinct from the Now Playing **row**, which can display multiple active sessions.

## Debouncing

The media-event pipeline applies approximately a **500 ms debounce/coalescing stage** before producing its global Now Playing update.

That prevents rapid metadata/playback callbacks from repeatedly rebuilding the home row or spamming the wallpaper-provider IPC contract.

---

# 11. `ju2` — one media session

`ju2` wraps one connected media controller and implements its player-listener callback interface.

Tracked state includes:

- playing state;
- player state;
- session token;
- several metadata text fields;
- artwork URI;
- artwork bytes.

The constructor snapshots the initial player/media metadata. Later metadata and playback-state callbacks update this state and signal `iw2`.

## Active-state test

`ju2.o()` excludes player state values `{1, 4}`.

In the consumed Media3 player state model these correspond to the terminal/non-active states (idle/ended), leaving buffering/ready-style sessions eligible for the Now Playing row.

`ju2.q()` exposes the stored playing boolean.

## Controller lifecycle

`ju2.I(boolean)` attaches or detaches itself as a controller listener and releases the controller on the detach path where requested. Main-thread dispatch is enforced when needed.

---

# 12. Recoverable metadata semantics

The obfuscated underlying Media3 metadata class has many fields, so Group 5 deliberately does **not** assign guessed names to every slot.

However Projectivy creates an `ew2` object whose `toString()` reveals the semantic contract:

```text
NowPlayingSnapshot(
    activeToken,
    title,
    artist,
    albumTitle,
    artworkUri,
    isPlaying
)
```

The snapshot is built from the selected `ju2` as:

```text
token       = ju2.h()
title       = ju2.l()
artist      = ju2.d()
albumTitle  = ju2.c()
artworkUri  = ju2.e()
isPlaying   = ju2.q()
```

This semantic snapshot is the safe implementation boundary for the wallpaper-provider event.

The `c73` card itself uses a slightly richer/different set of underlying metadata slots to choose its displayed primary/secondary text, so the report does not conflate the card renderer with the wallpaper snapshot.

---

# 13. Now Playing built-in category

`w40.k()` creates the built-in category explicitly as:

```text
category ID:   12
source type:   8
title:         menu_now_playing
icon:          ic_menu_now_playing
color:         ic_now_playing
```

This confirms Now Playing is a normal Projectivy `i5` category source integrated into the same generic home architecture established in Group 2.

When `w40` resolves source type `8`, it:

1. requires the notification listener/media subsystem to be available;
2. takes the active `iw2` session wrappers;
3. keeps sessions for which `ju2.o()` is true;
4. deduplicates them by session token;
5. maps each `ju2` into a `c73` card.

Thus the row can contain **multiple active media sessions**.

---

# 14. `c73` — Now Playing card

`c73` extends the common Projectivy `t4` card model:

```text
c73 : t4
item type = 8
```

It stores:

- the linked `ju2` session;
- display text assembled from media metadata;
- artwork URI;
- artwork bytes.

Its stable ID is derived from the string representation of the session token, giving session-level rather than package-only identity.

`B()` delegates to `ju2.o()` so the generic card architecture can tell whether the item remains active.

`k()` returns false: Now Playing cards are not manually reorderable like ordinary app cards.

## Text assembly

The primary card text comes from a media metadata/display-title slot and is prefixed with a playback-state glyph. The exact decompiled glyph assignment is retained as implementation evidence rather than semantically "corrected": Projectivy chooses between the play and pause symbols based on the stored playing boolean.

Secondary text is composed from metadata fields separated by:

```text
 · 
```

with fallback fields used when the preferred pair is unavailable.

## Artwork

`d73`, the Now Playing presenter, selects:

```text
artwork URI
    ↓
artwork byte[]
    ↓
ic_default_now_playing
```

It includes the byte-array hash in equality/rebind decisions, allowing embedded artwork changes to invalidate a card even if its URI is unchanged/null.

---

# 15. Launch behavior for a Now Playing card

Clicking a `c73` does not merely open a generic media app blindly.

Launch resolution is:

```text
media session/controller primary PendingIntent
        ↓ if absent/fails
normal launcher intent for the session package
        ↓ if still unavailable
Projectivy "can't start activity" feedback
```

This is the correct behavior for Aerial-Rows to reproduce: prefer the exact activity/content target supplied by the active media session, then fall back to the owning application.

---

# 16. Media controls

`c73` exposes controller operations used by the generic `j6` action screen:

- pause;
- play;
- stop;
- previous media item;
- next media item.

The action screen only enables controls when the connected media controller reports the corresponding command as available.

Observed action entries:

```text
action_play_pause
action_stop
action_previous
action_next
```

Play/pause dynamically chooses pause when the wrapper is currently playing and play otherwise.

Stop attempts the real controller stop command; if that operation throws/fails, Projectivy falls back to pause.

Previous/next call the controller's previous/next-media-item operations after checking command availability.

Because minified Media3 command constants are not human-readable in the APK, this report intentionally describes their semantic operations rather than treating their obfuscated numeric checks as a public API contract.

---

# 17. Focus → wallpaper-provider integration

`d73` publishes a wallpaper-provider:

```text
Event.ProgramCardFocused
```

when a Now Playing card gains focus.

The event includes the card's visible title/type/aspect-style metadata and artwork URI where available.

Separately, the global media-session pipeline publishes:

```text
Event.NowPlayingChanged
```

from the debounced `ew2` snapshot.

Its recoverable payload is:

- isPlaying;
- title;
- artist;
- album title;
- artwork URI;
- selected/active session identity through the snapshot pipeline.

After sending `NowPlayingChanged`, Projectivy looks up home category ID `12` and refreshes it when that row is enabled/visible.

Therefore the wallpaper system has two complementary media signals:

```text
NowPlayingChanged
    → global playback/metadata change

ProgramCardFocused
    → user focus moved onto a particular Now Playing card
```

This becomes important in Group 6 when reconstructing wallpaper/Aerial behavior.

---

# 18. Notification ↔ Now Playing matching

`l63.b()` handles the special tag:

```text
Notification.NowPlaying
```

It:

1. obtains category ID `12`;
2. scans that category's cards;
3. compares the card display identity/title against the notification title;
4. returns the matching `c73` where possible.

The linked card is then used when clicking/dismissing the notification.

This is an intentionally pragmatic correlation mechanism rather than a formal session-token relation in the notification model.

For an independent implementation we can reproduce the behavior but should prefer a stronger package/session correlation whenever the framework exposes it, with title matching as a fallback.

---

# 19. Media playback and idle/power detection

Group 5 also found a cross-subsystem setting:

```text
power_internal_idle_excludes_media_playback
```

with user-facing wording equivalent to excluding active media playback from idle detection.

Projectivy conditionally keeps media-session monitoring active for this purpose. Therefore media awareness is not used only for the Now Playing row: it can prevent idle/power behavior from triggering while media is playing.

The complete idle/boot/accessibility behavior remains in Group 9, but this dependency is now part of the baseline.

---

# 20. Framework/library boundary

A substantial portion of the session/controller implementation is bundled/minified Media3 support code. Group 5 deliberately stops at the Projectivy-facing abstractions instead of reverse-engineering Media3 itself.

Projectivy-specific boundary:

```text
NotificationListener
       ↓
      iw2
       ↓
      ju2
       ↓
      c73 / d73
       ↓
      w40 / j6 / wallpaper events
```

Bundled framework/library boundary includes the underlying media controller/token/player classes (`nt2`, `mt2`, `z54`, `nv2`, etc.). Their public behavior is used where needed, but their internal implementation is not treated as proprietary Projectivy logic.

This ownership distinction significantly reduces unnecessary RE work while preserving reconstruction accuracy.

---

# 21. Recommended Aerial-Rows architecture

A clean equivalent can be implemented with explicit domain types rather than retaining the obfuscated layering:

```text
NotificationRepository
 ├─ currentNotifications: StateFlow<List<LauncherNotification>>
 ├─ postedEvents
 ├─ removedEvents
 ├─ dismiss(key)
 └─ clearAll()

NotificationPolicy
 └─ shouldShow(notification, ranking)

NotificationGroupCoordinator
 └─ provider/group summary bookkeeping
```

For media:

```text
MediaSessionRepository
 ├─ sessions: StateFlow<List<MediaSessionItem>>
 ├─ primarySession: StateFlow<MediaSessionItem?>
 ├─ connect()
 └─ disconnect()

MediaSessionItem
 ├─ stableId/token
 ├─ packageName
 ├─ title/displayTitle
 ├─ artist
 ├─ albumTitle
 ├─ artworkUri/bytes
 ├─ isPlaying
 ├─ playerState
 ├─ launchIntent
 └─ availableCommands
```

Home integration:

```text
NowPlayingSource : HomeCategorySource
      ↓
List<NowPlayingHomeItem>
      ↓
generic card/presenter system
```

Actions:

```text
CardActionResolver
 └─ NowPlaying
     ├─ Play/Pause
     ├─ Stop
     ├─ Previous
     └─ Next
```

Wallpaper integration:

```text
MediaSessionRepository
     ↓ debounce/coalesce (~500 ms)
NowPlayingSnapshot
     ↓
WallpaperEvent.NowPlayingChanged

focused NowPlayingHomeItem
     ↓
WallpaperEvent.ProgramCardFocused
```

---

# 22. Improvements over blindly cloning the implementation

For equivalent behavior in Aerial-Rows, several Projectivy patterns are worth retaining:

1. **single observable notification state** shared by title badge and panel;
2. **incremental notification removals** instead of full panel rebuilds;
3. **session-level stable IDs** rather than package IDs;
4. **multiple active sessions in the home row**;
5. **separate primary-session selection** for global wallpaper/media state;
6. **500 ms media-event coalescing** to avoid UI/wallpaper churn;
7. **controller PendingIntent first, app-launch fallback second**;
8. **capability-aware media controls**;
9. **artwork bytes as well as URI support**;
10. **media playback participates in idle detection**.

Potential implementation improvements that do not change user-visible compatibility:

- correlate Now Playing notification ↔ session primarily by package/session identity, falling back to title matching;
- express notification filtering as testable policy rules;
- use Android/Kotlin domain names rather than reproducing the opaque minified class graph;
- keep framework Media3 implementation behind a repository adapter.

---

# 23. Closed dependency graph

The subsystem is sufficiently closed for independent reconstruction:

```text
Android NotificationListenerService
        ↓
NotificationListener
 ├─ filtering
 ├─ grouping
 ├─ state/event flows
 └─ MediaSessionManager access
        ↓
       iw2
 ├─ active-session reconciliation
 ├─ primary-session selection
 └─ 500 ms event coalescing
        ↓
       ju2
 ├─ controller
 ├─ state
 ├─ metadata
 └─ artwork
        ↓
       c73
 ├─ launch
 ├─ play/pause/stop
 ├─ stable ID
 └─ card metadata
        ↓
       d73
 └─ rendering/focus wallpaper event
        ↓
       w40
 └─ category ID 12 / source type 8
```

Parallel notification UI path:

```text
NotificationListener
        ↓
 current list + removal stream
        ↓
       b73
        ↓
       z63
        ↓
provider categories
        ↓
NotificationPreference / l63
        ↓
launch / dismiss / linked media stop
```

---

# 24. Deliberately deferred

These are not Group 5 gaps:

- wallpaper-provider IPC implementation and Aerial/video provider design → **Group 6**;
- exact notification/title/settings preference storage schema → **Groups 7/10**;
- idle timeout, boot and accessibility behavior that consumes media state → **Group 9**;
- broader persistence/migration details → **Group 10**.

---

## Group 05 conclusion

Projectivy implements notifications and Now Playing as two coordinated observable subsystems rather than as static launcher widgets. Notifications are filtered, grouped and dynamically rendered from a `NotificationListenerService`; the same service's privileged access is used to enumerate active Android media sessions. Active sessions are wrapped, deduplicated, tracked through Media3 callbacks, coalesced through a ~500 ms update pipeline, exposed as a normal Projectivy category, and connected to both card actions and the wallpaper-provider event API.

The key reconstruction principle is therefore:

```text
Do not build "Now Playing" from notifications alone.
```

Use real active media sessions for playback state/control and treat the notification panel as a separate UI/data stream, linking the two only where useful.

**Group 05 status: COMPLETE + VERIFIED.**
