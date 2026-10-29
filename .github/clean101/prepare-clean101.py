#!/usr/bin/env python3
from pathlib import Path

# CLEAN10.1 is based on runtime-proven CLEAN09.1. Preserve movie/episode playback
# and add a distinct TV-show folder-navigation path only.
engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

old = '''        if ((type == 0 || type == 2)\n                && pluginUri != null\n                && pluginUri.startsWith("plugin://plugin.video.redlight/")) {\n            Intent i = new Intent(Intent.ACTION_VIEW);\n            i.setComponent(new ComponentName(c.getPackageName(),\n                    RedLightLaunchActivity.class.getName()));\n            i.setData(Uri.parse(pluginUri));\n            i.putExtra(RedLightLaunchActivity.EXTRA_TARGET, pluginUri);\n            i.putExtra(RedLightLaunchActivity.EXTRA_KIND, type == 0 ? "movie" : "episode");\n            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n            return i;\n        }\n'''
new = '''        if ((type == 0 || type == 1 || type == 2)\n                && pluginUri != null\n                && pluginUri.startsWith("plugin://plugin.video.redlight/")) {\n            Intent i = new Intent(Intent.ACTION_VIEW);\n            i.setComponent(new ComponentName(c.getPackageName(),\n                    RedLightLaunchActivity.class.getName()));\n            i.setData(Uri.parse(pluginUri));\n            i.putExtra(RedLightLaunchActivity.EXTRA_TARGET, pluginUri);\n            i.putExtra(RedLightLaunchActivity.EXTRA_KIND,\n                    type == 0 ? "movie" : (type == 1 ? "show" : "episode"));\n            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n            return i;\n        }\n'''
if text.count(old) != 1:
    raise SystemExit(f'CLEAN10.1 program intent anchor mismatch: {text.count(old)}')
engine.write_text(text.replace(old, new, 1))

activity = Path('.github/clean09/RedLightLaunchActivity.java')
a = activity.read_text()
old_import = 'import android.util.Log;\n'
new_import = '''import android.util.Log;\n\nimport org.json.JSONArray;\nimport org.json.JSONObject;\n\nimport java.io.OutputStream;\nimport java.net.InetSocketAddress;\nimport java.net.Socket;\nimport java.nio.charset.StandardCharsets;\n'''
if a.count(old_import) != 1:
    raise SystemExit('CLEAN10.1 activity import anchor mismatch')
a = a.replace(old_import, new_import, 1)

old_forward = '''            Intent kodi = new Intent(Intent.ACTION_VIEW);\n            kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH));\n            kodi.setData(Uri.parse(target));\n            kodi.addCategory(Intent.CATEGORY_DEFAULT);\n            kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n            Log.i(TAG, "CLEAN09 forwarding kind=" + (kind == null ? "" : kind));\n            startActivity(kodi);\n'''
new_forward = '''            if ("show".equals(kind)) {\n                browseShow(target);\n                return;\n            }\n\n            // Frozen CLEAN09.1 playback path: movie/episode behavior remains unchanged.\n            Intent kodi = new Intent(Intent.ACTION_VIEW);\n            kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH));\n            kodi.setData(Uri.parse(target));\n            kodi.addCategory(Intent.CATEGORY_DEFAULT);\n            kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n            Log.i(TAG, "CLEAN09 forwarding kind=" + (kind == null ? "" : kind));\n            startActivity(kodi);\n'''
if a.count(old_forward) != 1:
    raise SystemExit('CLEAN10.1 frozen playback anchor mismatch')
a = a.replace(old_forward, new_forward, 1)

old_tail = '''    }\n}\n'''
helper = '''    }\n\n    /**\n     * TV-show cards point to Red Light folder URLs. Android ACTION_VIEW is the\n     * correct frozen path for playable movie/episode URLs, but folder URLs must\n     * be opened as Kodi GUI navigation. Red Light itself uses\n     * ActivateWindow(Videos, plugin://..., return), so mirror that exactly.\n     */\n    private void browseShow(final String target) {\n        Intent kodi = getPackageManager().getLeanbackLaunchIntentForPackage(KODI_PACKAGE);\n        if (kodi == null) {\n            kodi = new Intent(Intent.ACTION_MAIN);\n            kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH));\n            kodi.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);\n        }\n        kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n        startActivity(kodi);\n        Log.i(TAG, "CLEAN10 browse show bootstrap");\n\n        new Thread(new Runnable() {\n            @Override public void run() {\n                for (int attempt = 1; attempt <= 40; attempt++) {\n                    Socket socket = null;\n                    try {\n                        socket = new Socket();\n                        socket.connect(new InetSocketAddress("127.0.0.1", 9090), 250);\n\n                        JSONObject request = new JSONObject();\n                        request.put("jsonrpc", "2.0");\n                        request.put("method", "GUI.ActivateWindow");\n                        JSONObject params = new JSONObject();\n                        params.put("window", "videos");\n                        JSONArray parameters = new JSONArray();\n                        parameters.put(target);\n                        parameters.put("return");\n                        params.put("parameters", parameters);\n                        request.put("params", params);\n                        request.put("id", 10);\n\n                        OutputStream out = socket.getOutputStream();\n                        out.write(request.toString().getBytes(StandardCharsets.UTF_8));\n                        out.flush();\n                        Log.i(TAG, "CLEAN10 browse show ActivateWindow sent attempt=" + attempt);\n                        return;\n                    } catch (Throwable t) {\n                        if (attempt == 40) Log.e(TAG, "CLEAN10 browse show failed", t);\n                        try { Thread.sleep(250); } catch (InterruptedException ignored) { return; }\n                    } finally {\n                        if (socket != null) try { socket.close(); } catch (Throwable ignored) {}\n                    }\n                }\n            }\n        }, "ProjectivyRedLightBrowse").start();\n    }\n}\n'''
if not a.endswith(old_tail):
    raise SystemExit('CLEAN10.1 activity tail anchor mismatch')
a = a[:-len(old_tail)] + helper
activity.write_text(a)

check_engine = engine.read_text()
check_activity = activity.read_text()
for token in [
    'type == 0 || type == 1 || type == 2',
    'type == 1 ? "show" : "episode"',
    'CLEAN09 forwarding kind=',
    'CLEAN10 browse show bootstrap',
    'GUI.ActivateWindow',
    '127.0.0.1',
    '9090',
    'parameters.put(target)',
    'parameters.put("return")',
]:
    if token not in check_engine + check_activity:
        raise SystemExit('CLEAN10.1 source verification failed: ' + token)

print('PASS: CLEAN10.1 prepared — CLEAN09.1 playback unchanged; TV shows browse with Kodi GUI.ActivateWindow')
