#!/usr/bin/env python3
from pathlib import Path

engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

old_intent = '            v.put("intent_uri", programIdentityIntent(c, raw(e.uri)).toUri(0));\n'
new_intent = '            v.put("intent_uri", programIntent(c, e).toUri(0));\n'
if text.count(old_intent) != 1:
    raise SystemExit(f'CLEAN09 program intent anchor mismatch: {text.count(old_intent)}')
text = text.replace(old_intent, new_intent, 1)

old_helper = '''    /** Unique but inert identity: CLEAN 07 still does not implement playback. */\n    private static Intent programIdentityIntent(Context c, String pluginUri) {\n        Intent i = homeIntent(c);\n        Uri data = new Uri.Builder().scheme("projectivy-clean07").authority("redlight")\n                .appendQueryParameter("target", pluginUri == null ? "" : pluginUri).build();\n        i.setData(data);\n        return i;\n    }\n'''
new_helper = '''    /** CLEAN 09: movies and episodes are playable; TV-show/folder cards remain inert until CLEAN 10. */\n    private static Intent programIntent(Context c, Entry e) {\n        String pluginUri = raw(e == null ? null : e.uri);\n        int type = programType(e);\n        if ((type == 0 || type == 2)\n                && pluginUri != null\n                && pluginUri.startsWith("plugin://plugin.video.redlight/")) {\n            Intent i = new Intent(Intent.ACTION_VIEW);\n            i.setComponent(new ComponentName(c.getPackageName(),\n                    RedLightLaunchActivity.class.getName()));\n            i.setData(Uri.parse(pluginUri));\n            i.putExtra(RedLightLaunchActivity.EXTRA_TARGET, pluginUri);\n            i.putExtra(RedLightLaunchActivity.EXTRA_KIND, type == 0 ? "movie" : "episode");\n            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n            return i;\n        }\n        return programIdentityIntent(c, pluginUri);\n    }\n\n    /** Unique inert identity retained for TV-show/folder/navigation cards until CLEAN 10. */\n    private static Intent programIdentityIntent(Context c, String pluginUri) {\n        Intent i = homeIntent(c);\n        Uri data = new Uri.Builder().scheme("projectivy-clean07").authority("redlight")\n                .appendQueryParameter("target", pluginUri == null ? "" : pluginUri).build();\n        i.setData(data);\n        return i;\n    }\n'''
if text.count(old_helper) != 1:
    raise SystemExit(f'CLEAN09 inert helper anchor mismatch: {text.count(old_helper)}')
text = text.replace(old_helper, new_helper, 1)
engine.write_text(text)

# CLEAN07 is itself a generator around CLEAN06. Extend the generator statements,
# rather than looking for commands that only exist later in /tmp generated scripts.
build = Path('.github/clean07/build-clean07.sh')
b = build.read_text()

manifest_generator = "src=src.replace(anchor, anchor+'python3 .github/clean07/patch-grouped-channel-ui.py /tmp/pj\\n',1)"
manifest_generator_new = "src=src.replace(anchor, anchor+'python3 .github/clean07/patch-grouped-channel-ui.py /tmp/pj\\n'+'python3 .github/clean09/patch-manifest.py /tmp/pj\\n',1)"
if b.count(manifest_generator) != 1:
    raise SystemExit(f'CLEAN09 manifest generator anchor mismatch: {b.count(manifest_generator)}')
b = b.replace(manifest_generator, manifest_generator_new, 1)

javac_generator = "src=src.replace(anchor,'.github/clean07/RedLightDiscoveryProvider.java .github/clean07/ChannelVisualBridge.java\\n',1)"
javac_generator_new = "src=src.replace(anchor,'.github/clean07/RedLightDiscoveryProvider.java .github/clean07/ChannelVisualBridge.java .github/clean09/RedLightLaunchActivity.java\\n',1)"
if b.count(javac_generator) != 1:
    raise SystemExit(f'CLEAN09 javac generator anchor mismatch: {b.count(javac_generator)}')
b = b.replace(javac_generator, javac_generator_new, 1)
build.write_text(b)

check = engine.read_text()
for token in [
    'programIntent(c, e)',
    'type == 0 || type == 2',
    'RedLightLaunchActivity.class.getName()',
    'RedLightLaunchActivity.EXTRA_TARGET',
    'RedLightLaunchActivity.EXTRA_KIND',
    'type == 0 ? "movie" : "episode"',
    'projectivy-clean07',
]:
    if token not in check:
        raise SystemExit('CLEAN09 engine verification failed: ' + token)

bcheck = build.read_text()
for token in [
    '.github/clean09/patch-manifest.py /tmp/pj',
    '.github/clean09/RedLightLaunchActivity.java',
]:
    if token not in bcheck:
        raise SystemExit('CLEAN09 build-generator verification failed: ' + token)

print('PASS: CLEAN09 prepared — movie/episode playback bridge only; TV-show navigation remains inert')
