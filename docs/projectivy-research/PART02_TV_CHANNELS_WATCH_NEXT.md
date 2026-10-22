# Projectivy 4.72 Reconstruction Dossier — Part 02: Android TV Channels + Watch Next

Target: Projectivy 4.72 (97)  
Evidence: REA/JADX static analysis of the exact APK.  
Purpose: implementation specification for Aerial-Rows.

## 1. Subsystem boundaries

Projectivy's Android TV provider integration is centered around:

- `e70`: channel/program provider access and caches,
- `m60`: channel value object,
- `yu` / `xu`: common Android TV program value objects,
- `bk3`: preview-program value object,
- `d15`: Watch Next program value object,
- `ck3`: common program card/action,
- `b15`: Watch Next specialization of the program card,
- `w40`: integration into the unified home row repository,
- `g70`: channel-management settings UI.

The provider subsystem feeds the same `i5` row / `t4` card architecture described in Part 01.

## 2. Platform URIs

Observed platform provider URIs:

- channels: `content://android.media.tv/channel`
- preview programs: `content://android.media.tv/preview_program`
- Watch Next: `content://android.media.tv/watch_next_program`

For a specific channel Projectivy forms the preview-program URI by appending query parameter:

`content://android.media.tv/preview_program?channel=<channelId>`

For a zero/all-channels query it uses the base preview-program URI and separately filters results to discovered preview channels.

## 3. Capability / permission gate

Provider reads are gated by a capability check plus `android.permission.READ_TV_LISTINGS`.

Projectivy only uses channels/programs when its internal platform/device checks say TV listings are supported and the permission is granted.

Aerial-Rows should expose provider availability as a repository state rather than treating provider-query exceptions as normal empty data.

## 4. Channel discovery query

Channel query projection:

1. `_id`
2. `package_name`
3. `type`
4. `display_name`
5. `description`
6. `app_link_intent_uri`
7. `internal_provider_id`

Projectivy keeps channels where:

- `type == "TYPE_PREVIEW"`, and
- the package is not a `.tvrecommendations` package.

It deduplicates channels using package identity combined with `internal_provider_id` when those values are available.

The resulting channel object retains its ContentValues and exposes:

- raw channel ID,
- provider package name,
- display metadata.

## 5. Dynamic Projectivy row construction for a TV channel

A discovered Android TV preview channel becomes an `i5` row with:

- type discriminator: **6**,
- stable Projectivy row ID: raw channel ID converted to int then offset by **2000**,
- default scale: **1.2**,
- default icon: channel logo (`TvContract.buildChannelLogoUri(channelId)`),
- title: channel `display_name`,
- external identity derived from provider package + channel display identity,
- backing `m60` channel object.

`w40` maintains a separate cache keyed by raw long channel ID so the same in-memory row model can be reused across refreshes. Newly discovered provider rows are then merged into the user's persisted row configuration rather than blindly replacing customized row state.

### Aerial-Rows improvement

Do **not** make the provider's numeric row ID our only persistent identity. Use a stable key such as:

`providerPackage + internalProviderId + displayName/type`

and store the raw provider channel ID as a transient query handle. OEM providers can recycle row IDs after reboot.

## 6. Program projection shared by Preview Program and Watch Next

Projectivy queries this program projection:

1. `package_name`
2. `title`
3. `short_description`
4. `poster_art_uri`
5. `thumbnail_uri`
6. `channel_id`
7. `weight`
8. `browsable`
9. `intent_uri`
10. `preview_video_uri`
11. `logo_uri`
12. `poster_art_aspect_ratio`
13. `poster_thumbnail_aspect_ratio`
14. `season_display_number`
15. `episode_display_number`
16. `duration_millis`
17. `last_playback_position_millis`
18. `type`
19. `author`
20. `genre`
21. `release_date`
22. `_id`

On older Android versions season/episode fall back to legacy numeric columns.

## 7. Preview-program query

Preview-program sort order:

`weight DESC`

For a specific provider channel, Projectivy queries the preview-program URI with `?channel=<id>`.

For the global/all-channel form, it queries the base preview-program URI and accepts programs only when their `channel_id` belongs to the currently discovered preview-channel set.

A program is accepted only when:

- `browsable == 1`,
- `intent_uri` is non-null.

Projectivy deduplicates preview programs using an identity derived from `intent_uri` and channel ID.

`action_channel_general_programs_per_channel` controls the maximum number retained per channel. A non-positive value disables that cap.

When querying all channels, Projectivy simultaneously builds a per-channel program cache so later individual channel queries can be served without another provider query.

## 8. Watch Next query

Watch Next uses:

`content://android.media.tv/watch_next_program`

Sort order:

`last_engagement_time_utc_millis DESC`

Acceptance conditions are the same essential card requirements:

- `browsable == 1`,
- `intent_uri` present.

`action_watch_next_programs_per_app` limits the number of Watch Next entries per provider package. A non-positive value disables that cap.

Projectivy materializes Watch Next rows into `d15` program objects and caches the list.

## 9. Cache / refresh behavior

The provider layer maintains three important caches:

- Watch Next program list,
- discovered preview-channel list,
- preview programs keyed by channel ID.

The home refresh coroutine clears these provider caches when either a force-refresh path is used or the elapsed refresh age exceeds the central home refresh interval.

The observed interval is derived from `MainFragment.R1` (60,000 ms) divided by two, giving roughly **30,000 ms**.

During that refresh Projectivy also clears application-discovery caches, re-queries channels, merges channel rows into the row registry, and then repopulates each row.

### Important implementation decision for Aerial-Rows

Use both:

1. `ContentObserver`s for channel / preview-program / Watch Next URIs where allowed, and
2. an `onResume` / bounded periodic invalidation fallback.

That gives faster correctness than relying only on a time-based cache and remains robust on OEM provider implementations that fail to notify reliably.

## 10. Program value model

The common `yu` / `xu` value-object layer stores program data as ContentValues and provides common accessors such as:

- package name,
- poster art URI,
- duration.

`bk3` represents a Preview Program.

`d15` represents a Watch Next Program.

Both carry essentially the same visual/program metadata, allowing them to share a common card implementation.

## 11. Program card/action (`ck3`)

Projectivy converts provider program objects into the common `t4` action hierarchy.

Preview-program cards use action/presenter type **6**.

Watch Next cards use action/presenter type **5** through subclass `b15`.

### Stable IDs

Preview program card identity is based primarily on the parsed `intent_uri`.

Watch Next deliberately derives a different stable ID from the program identity (effectively namespaced as Watch Next) so an equivalent intent cannot collide with a preview-program card.

### Launch behavior

Program activation:

1. parse `intent_uri` using Android intent URI syntax,
2. set the intent package to the program's `package_name`,
3. add common launch flags/extras as required,
4. pass through the common parental-control launch gate when protected,
5. call `startActivity`.

Aerial-Rows should preserve the provider's deep link exactly while validating that the package/intended activity can still resolve.

## 12. Program artwork priority

For a program card, Projectivy resolves artwork approximately as:

1. user customization override,
2. `thumbnail_uri`,
3. `poster_art_uri`,
4. `logo_uri`,
5. generic/default card artwork.

Aspect ratio comes from the matching provider aspect-ratio column:

- thumbnail uses `poster_thumbnail_aspect_ratio`,
- poster uses `poster_art_aspect_ratio`.

The card's reported aspect ratio is mapped into Projectivy's standard card-aspect enum.

## 13. Program text/details

The card title is the provider `title`.

Secondary text is composed from provider metadata according to program type. Depending on type Projectivy combines fields such as:

- release date,
- genre,
- season,
- episode,
- author,
- duration,
- `short_description`.

The metadata summary uses compact separators and then places the short description on a separate line when present.

This should be recreated from the provider columns semantically rather than copying Projectivy's formatting implementation.

## 14. Progress bar

When `appearance_cards_program_show_progressbar` is enabled, Projectivy computes:

`progress = last_playback_position_millis / duration_millis * 100`

with invalid/non-positive values producing zero progress.

The progress value is rendered directly on the program card.

## 15. Provider logo badge

`logo_uri` can be rendered as a card badge.

Controlled by:

`appearance_cards_program_show_badge`

The badge is shown on focused program cards when a valid logo exists and the setting is enabled. The image loader sizes it relative to the card dimensions.

## 16. Focus zoom and preview video

Program cards support a slow focus zoom controlled by:

`appearance_cards_program_zoom_on_focus`

When focused, a program containing `preview_video_uri` can start an asynchronous preview-video flow; losing focus cancels the preview and resets the card image/video state.

The global behavior is governed by the program-preview appearance setting recovered in Part 01:

`appearance_cards_program_play_preview`

The exact player lifecycle/render integration will be documented together with the wallpaper/video rendering layer because they share media/display infrastructure.

## 17. Wallpaper/provider focus event

Focusing a program card emits a wallpaper-provider event containing semantic program data including:

- title,
- program type,
- selected program artwork URI,
- artwork aspect ratio.

This allows a wallpaper provider to react to the currently focused program independently of the card renderer.

This is a clean interface worth preserving in Aerial-Rows as a generalized `HomeFocusEvent`.

## 18. Manage Channels UI

The channel-management settings surface groups discovered TV-provider channel rows by the human-readable name of the provider package.

The Watch Next row is handled separately from provider channel groups.

This UI operates on the same persisted row visibility/customization model as the main row registry rather than maintaining a separate channel-enable database.

## 19. Recommended Aerial-Rows repositories

```text
TvProviderRepository
  |- observeChannels()
  |- observePreviewPrograms(channelKey)
  |- observeWatchNext()
  |- refresh(force)
  `- invalidate(uri)

TvChannelIdentity
  |- providerPackage
  |- internalProviderId
  |- displayName
  `- transientProviderRowId

Program
  |- providerId
  |- packageName
  |- title / description
  |- poster / thumbnail / logo
  |- intentUri
  |- previewVideoUri
  |- duration / playbackPosition
  |- type / season / episode / genre / author
  `- aspectRatio
```

Recommended observation strategy:

```text
ContentObserver(channel URI) ---------\
ContentObserver(preview programs) -----+--> invalidation flow --> repository refresh
ContentObserver(Watch Next) ----------/
onResume fallback --------------------/
30-60s safety TTL --------------------/
```

## 20. Part 02 completion criteria

Completed:

- channel URI/projection/filtering
- dynamic channel-row construction
- preview-program URI/projection/sort/filtering
- Watch Next URI/projection/sort/filtering
- per-channel and per-app limits
- provider caches and refresh interval
- program data models
- card conversion and stable IDs
- deep-link launch behavior
- artwork/aspect resolution
- program metadata/details
- progress/badge/focus behavior
- channel-management grouping

Deferred to dedicated parts:

- app/shortcut discovery and long-press card actions -> Part 03
- exact media-player implementation for program previews -> media/wallpaper parts
- full provider-change observation improvements -> implementation design, because Projectivy mainly exposes bounded cache refresh rather than a clean observer repository
