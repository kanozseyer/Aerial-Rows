from pathlib import Path
import json

# Applied after the complete v9 patch chain. Keep all v9 Red Light/Aerial behaviour,
# but replace the broken first-run profile restore hooks with StableProfileBridge.

# Rebase the bundled v9 profile onto the user's latest 2026-10-27 00:28:28 export.
# We mutate only the settings that changed from the v9 export, leaving the known-good
# category inventory/identities intact. Internal embedded markers are never bundled.
profile = Path('embedded-runtime/src/main/assets/launcher_default_profile.json')
root = json.loads(profile.read_text())
for marker in (
    'launcher_embedded_profile_v2',
    'launcher_embedded_channel_layout_v1',
    'launcher_embedded_channel_layout_v2',
    'launcher_embedded_channel_layout_attempts_v2',
):
    root.pop(marker, None)
root['appearance_cards_bg_tint'] = 3355443
root['appearance_statusbar_maincontainer_icon_background_tint'] = 682273451
root['appearance_statusbar_maincontainer_icon_size'] = 0.5
root['appearance_cards_size'] = 0.8
root['appearance_cards_rounded_corners'] = 0.5
root['appearance_general_tint'] = -2981819
root['appearance_wallpaper_provider'] = '8c00feaf-c7be-43c2-80f9-d49853aaa99a'
root['appearance_wallpaper_provider_packagename'] = 'com.spocky.projengmenu'
root['appearance_wallpaper_provider_params_8c00feaf-c7be-43c2-80f9-d49853aaa99a'] = '{"sources":["A","C1","C2"],"prefer4k":true,"prefer_hdr":false,"fallback":true}'

cats = json.loads(root.get('action_categories_list', '[]'))
visible_red = {
    'Red Light · Trending Movies',
    'Red Light · Trending TV Shows',
    'Red Light · In Progress Episodes',
    'Red Light · Continue Watching',
    'Red Light · Movies Watchlist',
    'Red Light · TV Shows Watchlist',
}
for item in cats:
    if item.get('type') != 6:
        continue
    uid = str(item.get('channelUniqueId', ''))
    title = str(item.get('title', ''))
    if uid == 'org.smarttube.stableRecommended':
        item.pop('isVisible', None)
        item['scale'] = 1.2
    elif title in visible_red:
        item.pop('isVisible', None)
        item['scale'] = 1.4
    elif uid.startswith('com.spocky.projengmenuRed Light · '):
        item['isVisible'] = False
root['action_categories_list'] = json.dumps(cats, separators=(',', ':'), ensure_ascii=False)
profile.write_text(json.dumps(root, separators=(',', ':'), ensure_ascii=False))
print('updated bundled profile to latest exported card/icon colours and Home visibility')

embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
text = embedded.read_text()

old = '        applyDefaultProfile(app);\n        repairEmbeddedAerialPackage(app);'
new = '        StableProfileBridge.onLauncherResume(context);\n        repairEmbeddedAerialPackage(app);'
if text.count(old) != 1:
    raise SystemExit(f'v9 stable resume anchor mismatch: {text.count(old)}')
text = text.replace(old, new, 1)

old = '            if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); }'
new = '            if (c != null) { StableProfileBridge.bootstrap(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); }'
if text.count(old) != 1:
    raise SystemExit(f'v9 stable bootstrap anchor mismatch: {text.count(old)}')
text = text.replace(old, new, 1)
embedded.write_text(text)
print('replaced early raw default-profile writes with stable profile bridge')

engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()
old = '        restoreBundledChannelLayoutOnce(context, states);\n        notifyProjectivyChannelsChanged(context);'
new = '        StableProfileBridge.tryApplyBundledLayout(context);\n        notifyProjectivyChannelsChanged(context);'
if text.count(old) != 1:
    raise SystemExit(f'v9 stable Red Light layout hook mismatch: {text.count(old)}')
text = text.replace(old, new, 1)
engine.write_text(text)
print('replaced v8 layout restore call with stable real-channel layout merge')
