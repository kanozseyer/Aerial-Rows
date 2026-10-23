# Group 4 post-closure audit — Android TV Channels & Watch Next

Status: **POST-CLOSURE AUDITED after Groups 5–12**  
Original Group 4 SHA-256: `1ef505a55393f1952896cdd6c11e398df3e8e380593360662ad1617a71377de3`

## Executive conclusion

The original Group 4 report remains architecturally correct and unusually complete.

No later group revealed a second hidden TV-content subsystem. The core model still stands:

```text
Android TV Provider
        ↓
e70 repository/cache
        ↓
channels / Preview Programs / Watch Next
        ↓
m60 / bk3 / d15
        ↓
dynamic i5 rows + ck3/b15 cards
        ↓
w40 reconciliation
        ↓
ek3 presenter
```

The audit found:

- **no major factual reversal**;
- **2 genuine completeness omissions**;
- several important precision/design refinements;
- one Projectivy limitation that the Aerial-Rows plan should intentionally improve.

The two strongest additions are:

1. exact Preview/Watch Next stable-ID formulas;
2. program-card parental protection inherits both **provider-package protection** and **individual card protection**.

---

# 1. Genuine completeness miss — exact program stable-ID formulas

Group 4 correctly stated that ordinary `ck3` identity is based primarily on `intent_uri` and that `b15` uses a Watch Next-specific helper.

The exact formulas are now closed.

## Preview Program / `ck3`

`ck3.a()` is:

```text
intent_uri present
    -> Uri.parse(intent_uri).hashCode()

otherwise
    -> "id".hashCode()
```

Because the normal Group 4 repository filters out programs without `intent_uri`, normal Preview Program cards use the URI hash.

## Watch Next / `b15`

`b15.a()` calls:

```text
q82.b(xu)
```

which is exactly:

```text
(Uri.parse(intent_uri) + "WN").hashCode()
```

for the normal non-null URI path.

The `"WN"` suffix deliberately gives Watch Next a different stable ID from a Preview Program with the same deep-link URI.

### Architectural consequence

Projectivy consistently tries to keep:

```text
Preview version of deep link X
    !=
Watch Next version of deep link X
```

even if the provider reuses the same URI.

### Aerial-Rows improvement

Do not copy 32-bit Java hashes.

Use structured identities, for example:

```text
preview:<provider>:<channelStableKey>:<programIdentity>
watchnext:<provider>:<programIdentity>
```

This removes hash collisions and makes source semantics explicit.

---

# 2. Genuine completeness miss — provider-level parental protection applies to TV program cards

`ck3` overrides the generic parental/protection check:

```text
ck3.D()
```

The effective rule is:

```text
protected =
    parentalSet contains hash(provider package)
    OR
    generic t4 protection applies to this card stable ID
```

In code terms:

```text
package_name != null && w75.l(packageName.hashCode())
    OR
super.D()
```

`super.D()` checks the card's own stable identity.

Therefore a Preview Program or Watch Next card can become protected in two ways:

1. the **owning provider application** is protected;
2. the **individual program card** is protected.

The subsequent execution still passes through the common `t4` launch path and therefore through the same parental launch wrapper established in Groups 3 and 10.

### Why this matters

Projectivy deliberately avoids this loophole:

```text
protected Netflix app
        but
unprotected Netflix Watch Next deep link
```

The provider package protection propagates to the program card.

### Aerial-Rows requirement

Every launch path must converge through:

```text
LaunchGate
    ↓
LaunchDispatcher
```

including:

- apps;
- Preview Programs;
- Watch Next;
- shortcuts;
- Now Playing;
- inputs;
- boot actions;
- remote actions.

Provider/package policy should be able to protect derived cards automatically.

---

# 3. Important precision correction — dynamic channel identity is only stable while Android's channel ID remains stable

Group 4 says the `w40.G` channel-ID cache allows stable dynamic rows across refreshes.

That is true **within a runtime/provider lifecycle where the numeric Android channel ID remains unchanged**.

The actual Projectivy identity is:

```text
category ID = int(channelId + 2000)
```

and reconciliation is keyed by:

```text
channelId
```

This has two durability weaknesses:

1. a provider/OEM can delete and recreate a channel with a new `_id`;
2. the `long` channel ID is converted to an `int` after adding 2000.

Therefore the stronger canonical wording should be:

> Projectivy preserves dynamic-channel object identity across ordinary refreshes by numeric channel ID, but the identity is not guaranteed to survive provider/OEM channel recreation and is not collision-proof as a long-term persistent key.

This is especially relevant because Group 4 already discovered the better logical identity ingredients:

```text
package_name
internal_provider_id
display/channel metadata
```

### Aerial-Rows improvement

Persist a logical stable key such as:

```text
provider package
+ internal_provider_id
```

with sensible fallback identity when `internal_provider_id` is missing.

Keep numeric `channelId` only as the current TvProvider lookup handle.

This is already reflected in the Group 12 implementation plan and should now be considered a correction to Group 4's parity recommendation #7 ("preserve stable row identity by channel ID").

---

# 4. Precision upgrade — Projectivy discovers non-browsable Preview Channels

The channel projection used by `e70.b()` is:

```text
_id
package_name
type
display_name
description
app_link_intent_uri
internal_provider_id
```

It does **not** query/filter the channel's `browsable` column.

The channel filter is based on:

```text
type == TYPE_PREVIEW
provider package not ending in .tvrecommendations
dedup by package + internal_provider_id when available
```

Therefore Projectivy can discover a Preview Channel that is not currently browsable.

That makes the later call to:

```text
TvContract.requestChannelBrowsable(context, channelId)
```

semantically important rather than redundant.

### Canonical interpretation

```text
Discover Preview Channels
        ↓
show/manage Projectivy row
        ↓
user enables channel
        ↓
request Android browsable state
```

For Aerial-Rows, the **management view must not hide non-browsable channels** if we want users to be able to enable them.

---

# 5. Precision upgrade — Watch Next provider-management UI is channel-derived

Group 4 correctly notes that `CategoryAppsPreferencesFragment` builds the provider switches for Watch Next from apps represented by discovered TV channels.

This has a subtle limitation:

```text
app publishes Watch Next
but no discoverable Preview Channel
        ↓
its package may not naturally appear in the provider-switch UI
```

The filtering engine itself is package based, but the management choices are sourced from discovered channel providers.

### Aerial-Rows improvement

Build Watch Next provider management from the union of:

```text
packages present in Watch Next
+
packages owning discovered Preview Channels
```

rather than only channel providers.

This makes the UI reflect the actual Watch Next dataset.

---

# 6. Precision upgrade — Watch Next/provider exclusions use package hashes

The Watch Next category's exclusion list stores package-derived hashed identities.

The effective policy is:

```text
enabled = packageHash not in i5.v
```

This works in Projectivy's integer-ID architecture, but it inherits ordinary Java hash collision risk.

For Aerial-Rows, store the package name (or a structured provider identity) directly:

```text
excludedProviderPackages: Set<String>
```

rather than a 32-bit hash.

---

# 7. Precision upgrade — built-in Watch Next category identity

Group 2's post-closure audit closes the home-level identity around Group 4:

```text
Watch Next category ID = 10
source type = 5
```

Dynamic Preview Channel rows use:

```text
source type = 6
category ID = int(channelId + 2000)
```

So the canonical TV home model is:

```text
fixed Watch Next row
        +
dynamic Preview Channel rows
```

rather than all TV-provider content being dynamically derived.

---

# 8. Precision upgrade — exact program appearance settings

Group 7 closes the settings controlling `ek3` program-card behavior:

```text
appearance_cards_program_play_preview
appearance_cards_program_show_progressbar
appearance_cards_program_show_badge
appearance_cards_program_zoom_on_focus
```

mapped through:

```text
hj3.Z0
hj3.a1
hj3.b1
hj3.c1
```

This strengthens Group 4's presenter analysis:

- progress is configurable;
- provider badge is configurable;
- preview playback is configurable;
- focused-program artwork zoom is separately configurable.

These are independent controls, not one generic "program card enhancement" switch.

---

# 9. Additional presenter detail — focus zoom timing

The raw `ek3` presenter gives one extra exact UI behavior not captured in Group 4.

When:

```text
appearance_cards_program_zoom_on_focus
```

is enabled, the main image animates approximately:

```text
focus gained:
    start delay = 300 ms
    scale 1.0 -> 1.05
    duration = 3000 ms

focus lost:
    start delay = 0
    scale -> 1.0
    duration = 300 ms
```

This is separate from preview-video playback.

Important:

> The 300 ms value above is an artwork-zoom delay. It should **not** be interpreted as proof of a 300 ms preview-video dwell delay.

For Aerial-Rows we can deliberately use a shorter/explicit preview-video dwell policy if desired.

---

# 10. Precision upgrade — program-card focus background event is a formal wallpaper event

Group 4 correctly found:

```text
Event.ProgramCardFocused
```

Group 6 later proved that this belongs to a formal wallpaper-provider event contract.

Its event bit is:

```text
ProgramCardFocused = 8
```

and it participates alongside:

```text
TimeElapsed
NowPlayingChanged
CardFocused
LauncherIdleModeChanged
```

So the canonical relationship is:

```text
program focus
    ↓
semantic home event
    ↓
wallpaper provider coordinator
    ↓
provider may choose a new background
```

not merely "the presenter directly changes the background."

That strongly supports the Aerial-Rows event-bus/provider architecture.

---

# 11. Precision upgrade — Random TV Program wallpaper remains actionable

Group 4 notes that `rp3` reuses `e70.c(0)` to turn Preview Program artwork into wallpapers.

Group 6 closes another useful detail:

- it preserves the program/action URI in the resulting wallpaper descriptor;
- the Random TV Program wallpaper provider has a **2-hour provider cache duration**.

Thus a TV-program-derived wallpaper can retain an action/deep-link relationship rather than being reduced to a dead bitmap.

This is a cross-subsystem enhancement, not a change to the home query path.

---

# 12. Precision upgrade — Program Details is intentionally a two-tier data model

Group 4 already described the lazy `long_description` lookup correctly.

The post-closure interpretation should emphasize the architectural split:

```text
home row query
    -> bounded lightweight projection

details screen
    -> single-row lookup by _id
    -> long_description only
```

The generic Android `xu/yu` model supports many more Android TV columns, but the actual Group 4 home query **does not request all of them**.

The home projection remains exactly the bounded 22-column set described in the original report.

Therefore fields supported by generic `xu`, such as:

```text
availability
starting_price
offer_price
live
interaction_type
interaction_count
content_id
start/end time
preview_audio_uri
...
```

should **not** be retroactively claimed as queried Group 4 home data.

This is a useful negative verification: the original projection list was correct.

---

# 13. Precision upgrade — Preview vs Watch Next identity separation is deliberate, but still hash-based

Because:

```text
Preview ID = hash(intent Uri)
Watch Next ID = hash(intent Uri + "WN")
```

Projectivy deliberately avoids treating the same provider deep link in Preview and Watch Next as the same card identity.

However, two caveats remain:

1. both are only 32-bit hashes;
2. Preview card identity does not include channel ID.

The global Preview query deduplicates by:

```text
intent_uri + channel_id
```

but card identity itself is primarily only the `intent_uri` hash.

This is normally safe because cards live in separate row adapters, but it is not a globally structured identity scheme.

### Aerial-Rows improvement

Make source/channel explicit in `CardIdentity` rather than relying on row locality to disambiguate identical IDs.

---

# 14. Refresh finding remains valid — no dedicated TvProvider ContentObserver found

The later groups did not invalidate Group 4's negative finding.

Projectivy 4.72 still appears to use:

```text
separate caches
        +
forced invalidation
        +
home refresh
        +
~30 s minimum timed-refresh gate
```

rather than a dedicated observer on:

```text
channel
preview_program
watch_next_program
```

The `ContentObserver` hits found in the APK belong to other subsystems.

### Aerial-Rows improvement

Group 12 intentionally improves this:

```text
ContentObserver
    + debounce
    + repository cache
    + resume refresh safety net
```

This is an intentional improvement over Projectivy, not a correction to the reverse engineering.

---

# 15. Deep-link launching should now be described as policy-gated

Group 4 accurately traces card intent construction:

```text
Intent.parseUri(intent_uri, URI_INTENT_SCHEME)
    ↓
setPackage(program.package_name)
    ↓
generic t4 launch preparation
```

Groups 3 and 10 close what happens afterward:

```text
generic t4 launch path
    ↓
parental/protection policy
    ↓
launch
```

Therefore the canonical Group 4 launch description should be:

```text
provider-declared deep link
    ↓
constrained to provider package
    ↓
generic LaunchGate/policy
    ↓
activity dispatch
```

For Aerial-Rows, never call `startActivity()` directly from a Watch Next/Program Composable.

---

# 16. Clarification — declared EPG write permissions do not imply this subsystem writes program rows

Group 1's manifest analysis found TV/EPG-related permissions including write permissions.

Group 4's verified provider architecture, however, is read-centric:

- query channels;
- query Preview Programs;
- query Watch Next;
- request a channel become browsable.

No Projectivy TV-content row creation/update pipeline was found here.

So:

> Do not infer a "publish our own Preview Programs" subsystem from manifest permissions alone.

Aerial-Rows should remain read-only unless we later introduce a concrete feature that requires writes.

---

# 17. Things Group 4 correctly deferred — not misses

These remain correctly separated:

- Android media sessions / Now Playing -> Group 5;
- wallpaper provider implementation -> Group 6;
- exact settings schema/UI -> Group 7;
- `TvInputManager`, HDMI and actual tuner/input launching -> Group 8;
- generic parental storage/policy -> Group 10.

The fact that later groups enrich these boundaries does not mean Group 4 was incomplete within its stated scope.

---

# 18. Revised canonical TV-content model

With Groups 5–12 folded back in:

```text
TvProviderRepository / e70
│
├─ capability
│   ├─ platform/device gate
│   ├─ READ_TV_LISTINGS
│   └─ general_enable_tv_channels
│
├─ channels
│   ├─ TYPE_PREVIEW
│   ├─ includes non-browsable channels
│   ├─ exclude *.tvrecommendations
│   ├─ dedup package + internal_provider_id
│   ├─ numeric channelId runtime handle
│   └─ cache
│
├─ Preview Programs
│   ├─ weight DESC
│   ├─ browsable == 1
│   ├─ intent_uri required
│   ├─ global dedup intent_uri + channel_id
│   ├─ configurable per-channel limit
│   └─ cache per channel
│
├─ Watch Next
│   ├─ last_engagement_time_utc_millis DESC
│   ├─ browsable == 1
│   ├─ intent_uri required
│   ├─ per-provider item limit
│   ├─ provider exclusions
│   └─ separate cache
│
└─ details
    └─ lazy long_description lookup by program _id

Home rows
├─ Watch Next
│   ├─ category ID 10
│   └─ source type 5
└─ Preview Channels
    ├─ source type 6
    └─ runtime category ID int(channelId + 2000)

ProgramCard
├─ Preview / ck3
│   └─ ID = hash(intent Uri)
├─ Watch Next / b15
│   └─ ID = hash(intent Uri + "WN")
├─ provider-package parental inheritance
├─ individual-card parental protection
├─ custom artwork -> thumbnail -> poster -> logo
├─ progress
├─ provider badge
├─ preview video
├─ details action
└─ ProgramCardFocused wallpaper event

Refresh
├─ forced invalidation
├─ separate cache clears
├─ timed home refresh
└─ ~30 s minimum gate
```

---

# 19. Effect on Aerial-Rows implementation plan

The Group 12 implementation order remains correct, but the Group 4 audit strengthens Phase 4 in these ways:

1. **Persist logical channel identity, not numeric channel ID.**
2. Keep numeric channel ID as an ephemeral current provider handle.
3. Manage non-browsable Preview Channels too.
4. Derive Watch Next provider controls from actual Watch Next packages as well as channel providers.
5. Use structured Preview/Watch Next card IDs rather than hashes.
6. Route all TV-content deep links through `LaunchGate`.
7. Let provider-level parental policy automatically cover derived program cards.
8. Keep the bounded home projection and lazy details query.
9. Add ContentObserver + debounce as an intentional improvement over Projectivy.
10. Keep program focus events semantic and renderer-independent.
11. Keep preview zoom animation and preview-video policy as separate concerns.

---

# Final assessment

```text
Core Group 4 architecture:          correct
Missing major subsystem:            none

Genuine completeness omissions:     2
  - exact Preview/Watch Next card-ID formulas
  - provider-level parental protection inheritance

Important precision refinements:
  - channel-ID stability is conditional, not durable
  - non-browsable channels are still discovered
  - Watch Next management source is channel-provider biased
  - program appearance setting keys now known
  - exact focus-zoom timing
  - deep links are generic-policy gated
  - Random TV wallpaper remains actionable

Negative finding retained:
  - no dedicated TV-provider ContentObserver found

Need to redo Group 4:                no
Effect on Group 12 plan:             strengthens Phase 4, no reorder required
```

**Recommended canonical status: `COMPLETE — POST-CLOSURE AUDITED AFTER GROUPS 5–12`.**
