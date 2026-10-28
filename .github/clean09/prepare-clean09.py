#!/usr/bin/env python3
from pathlib import Path

# CLEAN09 changes only preview-program click routing in the already-prepared
# frozen CLEAN08.5 Red Light source. Build/manifest integration is handled after
# the frozen CLEAN08.5 APK has been produced, so no frozen generator is edited.
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

check = engine.read_text()
for token in [
    'programIntent(c, e)',
    'type == 0 || type == 2',
    'RedLightLaunchActivity.class.getName()',
    'RedLightLaunchActivity.EXTRA_TARGET',
    'RedLightLaunchActivity.EXTRA_KIND',
    'type == 0 ? "movie" : "episode"',
    'return programIdentityIntent(c, pluginUri)',
    'projectivy-clean07',
]:
    if token not in check:
        raise SystemExit('CLEAN09 engine verification failed: ' + token)

print('PASS: CLEAN09 source prepared — movie/episode playback bridge only; TV-show navigation remains inert')
