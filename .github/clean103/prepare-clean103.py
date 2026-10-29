#!/usr/bin/env python3
from pathlib import Path

# CLEAN10.3 starts from runtime-proven CLEAN09.1. Movie/episode playback remains
# unchanged. TV-show cards are added, but browse routing uses only a CLEAN plugin
# URI: prefer Intent.getData(), which Projectivy preserves, and strip any Android
# #Intent serialization trailer as a defensive fallback.
engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

old = '''        if ((type == 0 || type == 2)\n                && pluginUri != null\n                && pluginUri.startsWith("plugin://plugin.video.redlight/")) {\n            Intent i = new Intent(Intent.ACTION_VIEW);\n            i.setComponent(new ComponentName(c.getPackageName(),\n                    RedLightLaunchActivity.class.getName()));\n            i.setData(Uri.parse(pluginUri));\n            i.putExtra(RedLightLaunchActivity.EXTRA_TARGET, pluginUri);\n            i.putExtra(RedLightLaunchActivity.EXTRA_KIND, type == 0 ? "movie" : "episode");\n            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n            return i;\n        }\n'''
new = '''        if ((type == 0 || type == 1 || type == 2)\n                && pluginUri != null\n                && pluginUri.startsWith("plugin://plugin.video.redlight/")) {\n            Intent i = new Intent(Intent.ACTION_VIEW);\n            i.setComponent(new ComponentName(c.getPackageName(),\n                    RedLightLaunchActivity.class.getName()));\n            i.setData(Uri.parse(pluginUri));\n            i.putExtra(RedLightLaunchActivity.EXTRA_TARGET, pluginUri);\n            i.putExtra(RedLightLaunchActivity.EXTRA_KIND,\n                    type == 0 ? "movie" : (type == 1 ? "show" : "episode"));\n            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n            return i;\n        }\n'''
if text.count(old) != 1:
    raise SystemExit(f'CLEAN10.3 program intent anchor mismatch: {text.count(old)}')
engine.write_text(text.replace(old, new, 1))

activity = Path('.github/clean09/RedLightLaunchActivity.java')
a = activity.read_text()

old_import = 'import android.util.Log;\n'
new_import = '''import android.util.Log;\n\nimport org.json.JSONArray;\nimport org.json.JSONObject;\n\nimport java.io.OutputStream;\nimport java.net.InetSocketAddress;\nimport java.net.Socket;\nimport java.nio.charset.StandardCharsets;\n'''
if a.count(old_import) != 1:
    raise SystemExit('CLEAN10.3 activity import anchor mismatch')
a = a.replace(old_import, new_import, 1)

old_forward = '''            Intent kodi = new Intent(Intent.ACTION_VIEW);\n            kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH));\n            kodi.setData(Uri.parse(target));\n            kodi.addCategory(Intent.CATEGORY_DEFAULT);\n            kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n            Log.i(TAG, "CLEAN09 forwarding kind=" + (kind == null ? "" : kind));\n            startActivity(kodi);\n'''
new_forward = '''            // For browse cards, Projectivy may preserve the clean data URI while an\n            // extra carries a serialized Intent URI. Never pass Android's #Intent\n            // trailer into Kodi/Red Light. Prefer the clean data URI first.\n            String browseTarget = null;\n            Uri sourceData = source == null ? null : source.getData();\n            if (sourceData != null\n                    && "plugin".equalsIgnoreCase(sourceData.getScheme())\n                    && "plugin.video.redlight".equalsIgnoreCase(sourceData.getHost())) {\n                browseTarget = sourceData.toString();\n            }\n            if (browseTarget == null || browseTarget.trim().isEmpty()) browseTarget = target;\n            int intentTrailer = browseTarget == null ? -1 : browseTarget.indexOf("#Intent;");\n            if (intentTrailer > 0) browseTarget = browseTarget.substring(0, intentTrailer);\n\n            Uri browseUri = Uri.parse(browseTarget);\n            String mode = browseUri.getQueryParameter("mode");\n            if ("build_season_list".equals(mode)) {\n                Log.i(TAG, "CLEAN10.3 browse clean uri=" + browseTarget);\n                browseShow(browseTarget);\n                return;\n            }\n\n            // Frozen CLEAN09.1 playback path. Intentionally continue using target\n            // exactly as before for movie/episode cards.\n            Intent kodi = new Intent(Intent.ACTION_VIEW);\n            kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH));\n            kodi.setData(Uri.parse(target));\n            kodi.addCategory(Intent.CATEGORY_DEFAULT);\n            kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n            Log.i(TAG, "CLEAN09 forwarding kind=" + (kind == null ? "" : kind));\n            startActivity(kodi);\n'''
if a.count(old_forward) != 1:
    raise SystemExit('CLEAN10.3 frozen playback anchor mismatch')
a = a.replace(old_forward, new_forward, 1)

old_tail = '''    }\n}\n'''
helper = '''    }\n\n    /** Open a Red Light TV-show folder through Kodi's own GUI navigation API. */\n    private void browseShow(final String target) {\n        Intent kodi = getPackageManager().getLeanbackLaunchIntentForPackage(KODI_PACKAGE);\n        if (kodi == null) {\n            kodi = new Intent(Intent.ACTION_MAIN);\n            kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH));\n            kodi.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);\n        }\n        kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n        startActivity(kodi);\n        Log.i(TAG, "CLEAN10 browse show bootstrap uri=" + target);\n\n        new Thread(new Runnable() {\n            @Override public void run() {\n                for (int attempt = 1; attempt <= 40; attempt++) {\n                    Socket socket = null;\n                    try {\n                        socket = new Socket();\n                        socket.connect(new InetSocketAddress("127.0.0.1", 9090), 250);\n\n                        JSONObject request = new JSONObject();\n                        request.put("jsonrpc", "2.0");\n                        request.put("method", "GUI.ActivateWindow");\n                        JSONObject params = new JSONObject();\n                        params.put("window", "videos");\n                        JSONArray parameters = new JSONArray();\n                        parameters.put(target);\n                        parameters.put("return");\n                        params.put("parameters", parameters);\n                        request.put("params", params);\n                        request.put("id", 10);\n\n                        OutputStream out = socket.getOutputStream();\n                        out.write((request.toString() + "\\n").getBytes(StandardCharsets.UTF_8));\n                        out.flush();\n                        Log.i(TAG, "CLEAN10 browse show ActivateWindow sent attempt=" + attempt);\n                        return;\n                    } catch (Throwable t) {\n                        if (attempt == 40) Log.e(TAG, "CLEAN10 browse show failed", t);\n                        try { Thread.sleep(250); } catch (InterruptedException ignored) { return; }\n                    } finally {\n                        if (socket != null) try { socket.close(); } catch (Throwable ignored) {}\n                    }\n                }\n            }\n        }, "ProjectivyRedLightBrowse").start();\n    }\n}\n'''
if not a.endswith(old_tail):
    raise SystemExit('CLEAN10.3 activity tail anchor mismatch')
a = a[:-len(old_tail)] + helper
activity.write_text(a)

combined = engine.read_text() + activity.read_text()
for token in [
    'type == 0 || type == 1 || type == 2',
    'source.getData()',
    'browseTarget.indexOf("#Intent;")',
    '"build_season_list".equals(mode)',
    'CLEAN10.3 browse clean uri=',
    'GUI.ActivateWindow',
    'parameters.put(target)',
    'CLEAN09 forwarding kind=',
]:
    if token not in combined:
        raise SystemExit('CLEAN10.3 source verification failed: ' + token)

print('PASS: CLEAN10.3 prepared — sanitized TV browse URI; CLEAN09.1 playback retained')
