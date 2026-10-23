# Projectivy 4.72 — Group 08 Post-Closure Re-Audit v2

Status: **COMPLETE — re-audited with restored REA 6.7.0 / JADX MCP 0.7.1**  
Reference APK: `ProjectivyLauncher-4.72-c97-xda-release.apk`  
APK SHA-256: `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`  
Original Group 8 report SHA-256: `668b12eba61d08be0d7e74904b3e60c6ae035f37c775db152e03747bc983f98e`

## Executive verdict

The original Group 8 architecture remains fundamentally correct. No hidden second HDMI/TV-input/device subsystem was found. The central model remains:

```text
dd4 stable source identity
        ↓
ed4 TIF discovery/cache/mapping
        ↓
c3 source launch coordinator
   ├─ internal TvView
   └─ vendor/platform fallback chain

DisplayProfileManager
        ↓
Amlogic / Xiaomi picture backends
```

The previous post-closure audit also remains valuable: its automatic SDR/HDR profile application, exact capability formula, seven-activation compatibility override, `input_last_used`, autostart convergence, remote HDMI keys, backend scoring, bootstrap and no-`TvInputManager.TvInputCallback` findings all survive this pass.

This deeper source-level re-audit does, however, make **two claim corrections** and adds **nine implementation-relevant precision/reliability refinements**.

---

## 1. Correction — internal `TvView` should not be described as proven tuner launching

`ed4.h()` genuinely discovers and caches both passthrough and non-passthrough `TvInputInfo` records.

`InternalTvActivity.J(TvView, dd4)` also contains both URI branches:

```text
ID contains HW
    → TvContract.buildChannelUriForPassthroughInput(id)

otherwise
    → TvContract.buildChannelsUriForInput(id)
```

But the verified generic source mapper `ed4.d(dd4)`:

- rejects Android/non-hardware `dd4` values;
- searches only the passthrough list `ed4.g`;
- requires the TIF ID to contain `HW`;
- then matches the stable source token/label.

Therefore, in the verified `dd4 -> ed4.d() -> InternalTvActivity.J()` route, the non-HW branch is not currently reachable.

### Canonical wording

Projectivy **discovers/cache non-passthrough/tuner inputs and contains defensive non-HW `TvView` code**, but the generic Group 8 launcher path is concretely verified for HDMI/AV passthrough. Do not infer first-class tuner launching from the defensive branch alone.

### Aerial-Rows

Model tuner/live-TV sources explicitly if we implement them. Do not let a defensive branch imply a supported `InputSource` type without a verified discovery-to-launch path.

---

## 2. Correction — hotkey cycling is visible-source cycling, not a direct available-input traversal

The Group 9 table previously summarized DPAD Left/Right as previous/next “available TV input.” Raw `dm3.a()` is more precise.

While temporary hotkey mode is active it:

1. starts from `ed4.l.u`;
2. increments/decrements the ordered `dd4` source value;
3. wraps through Android ↔ AV boundaries;
4. skips hardware source values whose `dd4.e()` shortcut-visibility preference is false;
5. invokes the selected generic source action.

It does **not** iterate `ed4.b()` or a resolved list of TIF IDs.

`dd4.c(source)` delegates to the generic `z52` action object. The associated action builds/launches the source activity; capability/fallback resolution happens later in the source-launch path.

### Canonical wording

```text
DPAD Left/Right in hotkey mode
    → previous/next visible generic source
    → downstream launch attempt
```

### Aerial-Rows

For predictable behavior, cycle an actual `AvailableInputs` snapshot rather than enum ordinals plus shortcut visibility.

---

## 3. `ed4.h()` fallback synthesis only applies when the whole passthrough list is empty

Fresh smali removes a Java-decompiler ambiguity.

For each HDMI/AV generic source:

```text
ed4.d(source) resolves
    → include

not resolved + passthrough list non-empty
    → do NOT synthesize that missing port

not resolved + passthrough list empty
    → Xiaomi compatibility OR manual compatibility override can include it
```

This is important: compatibility mode is **not** used to fill holes in a partially discovered HDMI list.

### Aerial-Rows

Keep three concepts separate:

```text
physical/platform discovery
compatibility assumption
user-visible shortcut configuration
```

Do not infer individual missing ports when the platform already supplied a partial authoritative input list unless a vendor adapter explicitly establishes them.

---

## 4. Sony/TCL recovery is demand-triggered rather than a timed retry loop

The previous audit correctly said Projectivy allows up to five recoveries from an abnormal empty passthrough state, but the mechanism can now be stated exactly.

When initialized and passthrough inputs are empty on Sony/TCL:

```text
retry counter < 5
    → ed4.e = false
    → retry counter++
    → return
```

There is no delayed re-query inside `ed4.h()` itself. A later caller that causes lazy initialization triggers the next discovery attempt.

### Aerial-Rows

Use explicit repository refresh plus bounded retry/backoff. Recovery should not depend on an unrelated future getter happening to touch the lazy cache.

---

## 5. Profile-binding choices are coupled to source-shortcut visibility in Projectivy

`ed4.b()` does:

```text
normalized source list ed4.i
    ↓
filter dd4.e() == true
    ↓
visible source list
```

The display-profile input-binding UI then uses:

```text
ed4.b()
+ Android inserted at index 0
```

So hiding an HDMI/source shortcut also removes that source from the profile-binding picker, even though the physical input concept and stored binding are separate.

### Aerial-Rows improvement

Do not copy this coupling.

```text
InputRepository.availableInputs
        ├─ home shortcut visibility
        └─ display-profile bindings
```

Both consumers should derive independently from the discovered source identity.

---

## 6. `input_last_used` is robust, but `ed4.l` current-source state is strategy-dependent

`c3.x()` persists the generic requested source before choosing its launch strategy:

```text
hj3.E.h(I())
```

This is the previously identified `input_last_used` state and remains reliable across internal/vendor launch strategies.

But the in-memory `ed4.l` value is explicitly updated only after:

- successful internal `F(...)` launch; or
- successful Xiaomi `EXTSRC_PLAY` launch.

If Xiaomi throws and one of the subsequent Fengmi / MediaTek / MiTV / AOSP / Google / generic external fallbacks succeeds, `c3.x()` returns without the same `ed4.l` assignment.

This matters because hotkey cycling uses `ed4.l` as its current-source starting point.

### Aerial-Rows improvement

All strategies should converge on one coordinator:

```text
launch(source)
    ↓
try strategy A/B/C/...
    ↓ success
CurrentInputState.set(source, strategy, platformId)
```

Do not allow individual adapters to own canonical current-source state.

---

## 7. Automatic display-profile polling has stricter activation rules

The previous audit correctly established the 4-second polling feature. Fresh verification adds the start condition.

`ProjectivyAccessibilityService.d()` only schedules the poller when:

```text
selected picture backend != null
AND
backend.g() != Integer.MIN_VALUE
```

So Accessibility being active is not sufficient by itself. The selected picture backend must expose usable source-detection capability.

### Aerial-Rows

Represent source/HDR detection as explicit backend capabilities rather than a sentinel integer.

---

## 8. Label changes are observed but do not trigger profile application

Each poll samples:

```text
current generic input
HDR/SDR state
input label
```

A change to any of those values updates the observed/diagnostic state, but profile application is gated specifically by:

```text
input changed
OR
HDR/SDR changed
```

A label-only change does not reapply the bound display profile.

### Aerial-Rows

Use separate state concepts:

```kotlin
InputPresentationState(input, label)
ProfileSelectionKey(input, hdrState)
```

---

## 9. The Projectivy 4-second poller can silently stop after a transient null sample

The poll runnable schedules its next 4-second execution only inside the path where the sampled label/state is non-null.

A transient invalid/null sample therefore skips `postDelayed(...)` and ends that polling chain until service configuration invokes its scheduler again.

This is a genuine reliability weakness, not a feature to reproduce.

### Aerial-Rows

`DisplayProfileCoordinator` should own a lifecycle-scoped continuous observation/retry loop. A temporary “unknown” sample must update state to `Unknown` or be ignored, but it must not terminate observation.

---

## 10. Dangling display-profile UUID bindings are tolerated, not repaired

Per-input SDR/HDR binding values are stable `lz0` profile UUIDs.

`DisplayProfileManager.d(uuid)` linearly resolves a UUID to a current profile index. If it cannot find it, it returns `-1`; `DisplayProfileManager.c(input,hdr)` then resolves the binding to `null`.

The picker stores:

```text
""         → None
profile.x  → selected profile UUID
```

No lookup-time repair/prune was found for a UUID that points to a deleted/malformed profile.

Result:

```text
dangling UUID
    → null selected profile
    → automatic apply becomes no-op
```

### Aerial-Rows

On profile load/delete/migration, perform referential-integrity cleanup or expose invalid bindings visibly rather than silently retaining them.

---

## 11. Exact picture-backend candidate set is now closed

The lazy DisplayProfile backend factory creates/probes exactly these candidates on the verified path:

1. Amlogic `SystemControlManager`
2. Amlogic `TvControlManager`
3. Xiaomi `PictureSettingsManager` / source-manager-backed adapter

It then chooses the candidate with the greatest resolved capability count (`ru.s()`).

This confirms the previous audit: backend choice is capability-scored, not fixed vendor precedence, and there is no hidden fourth backend in this factory path.

### Aerial-Rows

Use an explicit `PictureBackendCapabilities` model and deterministic suitability selection rather than reflection-method count.

---

## 12. `DisplayProfileManager.rps()` and bootstrap finding remain confirmed

Fresh source verifies that `rps()`:

- reads structured persisted profile records;
- deserializes valid `lz0` models;
- tolerates malformed records;
- creates a default profile if the resulting list is empty;
- sorts profiles case-insensitively;
- resets `ProjectivyAccessibilityService.s0` and another cached runtime string.

The earlier recommendation remains correct: Aerial-Rows should explicitly call a repository load during application bootstrap rather than copy Projectivy's opaque/native/bootstrap coupling.

---

## 13. Callback wording needs one important distinction

The prior audit's statement “no `TvInputManager.TvInputCallback`” remains correct.

Projectivy **does** use `TvView.TvInputCallback` for the actively tuned embedded `TvView`.

What is absent is a repository-level:

```text
TvInputManager.TvInputCallback
```

for live add/remove/update input discovery.

So the repository remains snapshot/cache/invalidation based.

### Aerial-Rows

Use platform input-manager callbacks where possible, plus explicit refresh on resume/re-entry as a safety net.

---

# Confirmed findings from the first post-closure audit

The deeper pass did not overturn these earlier improvements:

- automatic per-input SDR/HDR profile switching exists;
- normal poll interval is 4 seconds;
- compatibility formula is:
  `android.software.live_tv || Xiaomi tvplayer installed || manual compatibility override`;
- the hidden seven-activation About Device compatibility override invalidates/rebuilds input state;
- generic source identity is persisted via `input_last_used`;
- Group 9 autostart reuses the Group 8 source-launch layer;
- remote codes 243–246 map HDMI1–4;
- display backends are capability scored;
- `DisplayProfileManager.rps()` is an important initialization boundary;
- no repository-level `TvInputManager.TvInputCallback` is registered;
- root/sysfs functionality is optional shared expert infrastructure, not the normal HDMI path;
- standard Android TV / Nvidia Shield operation must not depend on vendor adapters.

---

# Updated Aerial-Rows Group 8 target architecture

```text
Platform TvInputManager
        │
        ├─ callback events
        ├─ resume safety refresh
        └─ bounded recovery/backoff
        ↓
InputRepository
        │
        ├─ DiscoveredInputs / AvailableInputs
        │       ├─ stable generic identity
        │       ├─ platform TIF identity
        │       └─ capabilities
        │
        ├─ VisibleInputShortcuts   (separate preference projection)
        └─ CurrentInputState
                 ↑
                 │ successful result from every strategy
InputLaunchCoordinator
        ├─ standard TIF/internal path
        ├─ Xiaomi adapter
        ├─ MediaTek adapter
        ├─ Amlogic/FengOS adapters
        └─ generic fallback

DisplayProfileRepository
        ├─ profiles by stable UUID
        ├─ integrity-checked SDR/HDR bindings
        └─ explicit startup load

DisplayStateRepository
        ↓
DisplayProfileCoordinator
        ├─ continuous resilient observation
        ├─ selection key = (input, HDR/SDR)
        └─ label = presentation metadata
```

This is cleaner and more reliable than duplicating Projectivy's visibility coupling, strategy-specific current-input writes, demand-triggered retry and Accessibility-owned poller.

---

# Updated final assessment

| Item | Result |
|---|---|
| Core Group 8 architecture | ✅ correct |
| Missing second input/device subsystem | none found |
| Subsystem-level factual reversal | none |
| Claim-level corrections | **2** |
| New precision/reliability refinements | **9** |
| Earlier post-closure findings | ✅ retained |
| REA/JADX verification | ✅ complete |
| Audit DB integrity | ✅ ok |

## Canonical corrections to carry forward

1. Do **not** claim verified first-class tuner/non-passthrough launching from the current `dd4` path.
2. Replace “previous/next available TV input” with “previous/next visible generic source, then downstream launch attempt.”
3. Separate discovered/available inputs from shortcut visibility in Aerial-Rows.
4. Centralize successful launch-state updates.
5. Make display-profile observation resilient and independent of Accessibility.
6. Clean dangling display-profile UUID bindings.

Group 8 can now be marked:

**COMPLETE — POST-CLOSURE RE-AUDITED v2**
