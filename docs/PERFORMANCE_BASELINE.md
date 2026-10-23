# Aerial Rows Performance Baseline

This document defines the repeatable performance checks that protect Couchy's lightweight hot paths while Aerial Rows gains Projectivy-level behavior.

## Principles

- Measure a release-like `benchmark` build, not the debug build.
- Use a real Android TV device for performance numbers. Emulator runs are useful only for functional test validation.
- Keep device, launcher configuration, installed-app set, wallpaper mode, and measurement procedure stable when comparing revisions.
- Record regressions before optimizing them; do not replace proven hot paths based on assumptions.

## Automated startup benchmark

The `:macrobenchmark` module measures Aerial Rows externally using `StartupTimingMetric`.

Run all startup benchmarks on a connected device:

```bash
./gradlew :macrobenchmark:connectedCheck
```

Current scenarios:

- `coldStartup` — process is recreated between measured launches.
- `warmStartup` — process remains resident while the Activity is relaunched.

The benchmark deliberately uses `CompilationMode.Ignore()` so it can run on older Android TV devices without Macrobenchmark resetting compilation state by reinstalling the launcher. This also avoids wiping launcher state during repeated development measurements.

Record the median startup value and keep the generated Perfetto traces when investigating a regression.

## RAM / PSS snapshot

Use the same launcher configuration and allow the home screen to settle before every sample.

```bash
adb shell dumpsys meminfo com.conreo.couchytv
```

Record at minimum:

- TOTAL PSS
- Java Heap
- Native Heap
- Graphics
- Code

Take snapshots for:

1. fresh HOME after startup and settling;
2. after navigating all representative app rows;
3. after opening/closing Settings;
4. with Aerial video actively playing;
5. after leaving Aerial Rows for another app and returning HOME.

The installed app count should be recorded with each baseline because app artwork residency is intentionally one of the things being measured.

## Interaction baselines to add

As stable semantic IDs and the generic home model land, extend Macrobenchmark coverage for:

- horizontal movement through a populated row;
- vertical movement through several rows;
- rail reveal/focus/return;
- Settings open/close;
- Aerial playback while navigating;
- row reorder and exact focus restoration.

Use frame timing/trace metrics for navigation scenarios rather than wall-clock sleeps.

## Regression rule

A feature phase is not accepted merely because it builds. If a repeatable performance regression appears, identify the owning change before proceeding. Optional features must remain demand-driven: disabling a feature must disable its expensive background work.
