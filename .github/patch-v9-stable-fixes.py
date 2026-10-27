from pathlib import Path

# Applied after the complete v9 patch chain. Keep all v9 Red Light/Aerial behaviour,
# but replace the broken first-run profile restore hooks with StableProfileBridge.
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
