# Projectivy 4.72 + Liquid Glass dock

This directory contains a **personal patcher**, not a redistributed Projectivy APK.

The patcher accepts only the official Projectivy Launcher **4.72 / versionCode 97** APK with SHA-256:

`a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`

It modifies only Projectivy's existing rounded category-dock `RecyclerView.ItemDecoration` (`ox3`). It deliberately leaves Projectivy's dock eligibility, layout, focus, scrolling, category data, wallpaper provider system, Premium checks and licensing code untouched.

## What the patch changes

Projectivy 4.72 already draws eligible category docks with two rounded-rectangle `Paint`s:

- a fill paint using the category background colour;
- a 1 dp stroke paint whose colour is blended 20% toward white.

The patch keeps the same geometry but changes the paint treatment to:

- **37.6% fill alpha** by default;
- **69.0% rim alpha** by default;
- rim colour blended **45% toward white**.

That creates a lightweight glass-like dock over animated/video wallpaper without introducing a realtime blur pipeline on NVIDIA Shield.

Optional build overrides:

```bash
PROJECTIVY_GLASS_FILL_ALPHA=88 \
PROJECTIVY_GLASS_RIM_ALPHA=184 \
PROJECTIVY_GLASS_RIM_LIGHTEN=0.50 \
./projectivy-mod/build_projectivy_glass.sh
```

## Build on the Mac

From the repository root:

```bash
chmod +x projectivy-mod/build_projectivy_glass.sh
./projectivy-mod/build_projectivy_glass.sh
```

If no APK path is supplied, the script downloads the official 4.72 GitHub release and verifies its SHA-256 before touching it.

Requirements already normally present on the Android-development Mac:

- Java / `keytool`;
- Python 3;
- `curl` and `openssl`;
- Android SDK Build Tools (`zipalign` and `apksigner`), with `ANDROID_SDK_ROOT` or `ANDROID_HOME` set.

Output:

`projectivy-mod/dist/ProjectivyLauncher-4.72-liquid-glass.apk`

The first build creates a persistent personal signing identity in:

`~/.projectivy-liquid-glass/`

Keep that directory. Future mod builds must use the same signing key to update the mod in-place.

## Installation warning

The modified APK cannot retain Projectivy's official signing certificate. Android therefore **cannot install it over the official Projectivy APK**.

Before testing:

1. Make a Projectivy backup/export and verify you can restore it.
2. Keep the official APK available in case you want to revert.
3. Uninstall the official build only when you are ready to test the mod.
4. Install the locally signed liquid-glass APK.
5. Restore the Projectivy configuration.

The patcher does **not** modify or bypass Projectivy Premium licensing. Projectivy 4.72's GitHub release added external-license support, but entitlement behaviour on a re-signed APK must be treated as unverified until tested normally.

## Aerial wallpapers

Aerials remain separate from this APK patch. Use Spocky's official **Overflight** Projectivy wallpaper-provider plugin (`spocky/projectivy-plugin-wallpaper-overflight`) and select **Overflight** from:

`Projectivy Settings → Appearance → Wallpaper`

That preserves Projectivy's own video-wallpaper renderer, transitions, dynamic colour, caching and idle integration.

## Scope / non-goals

This project does not patch Projectivy feature gates, purchases, Premium detection, signature checks, channels, inputs, app discovery, notification/media handling or launcher behaviour. If a future Projectivy version changes the APK hash, the builder must refuse it until the visual patch is re-audited for that release.
