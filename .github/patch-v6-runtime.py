from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 anchor, got {count} in {path}')
    path.write_text(text.replace(old, new, 1))
    print('patched', label)


# Start Red Light discovery during Application bootstrap, before the Home/settings UI
# gets its first chance to cache the Android TV channel list.
embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
replace_once(
    embedded,
    'if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); }',
    'if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); syncRedLightAsync(c); }',
    'bootstrap Red Light sync',
)

# Notify Projectivy immediately when TV-provider rows change and invalidate its lazily
# cached channel query.  This avoids needing to leave/re-enter Edit Channels before
# newly discovered Red Light shelves become visible.
engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()
end_anchor = '''        saveStates(context, states);\n    }\n\n    private static boolean isKodiInstalled(Context c) {'''
end_replacement = '''        saveStates(context, states);\n        notifyProjectivyChannelsChanged(context);\n    }\n\n    private static void notifyProjectivyChannelsChanged(Context c) {\n        try { c.getContentResolver().notifyChange(TvContract.Channels.CONTENT_URI, null); } catch (Throwable ignored) {}\n        try { c.getContentResolver().notifyChange(TvContract.PreviewPrograms.CONTENT_URI, null); } catch (Throwable ignored) {}\n\n        // Projectivy 4.72 keeps the TvContract channel query in e70.b (a lazy List).\n        // Clear it reflectively so its next UI read sees the channels just published.\n        try {\n            Class<?> cls = Class.forName("e70");\n            java.lang.reflect.Field field = cls.getDeclaredField("b");\n            field.setAccessible(true);\n            Object lazy = field.get(null);\n            if (lazy != null) {\n                java.lang.reflect.Method getValue = lazy.getClass().getMethod("getValue");\n                Object value = getValue.invoke(lazy);\n                if (value instanceof java.util.List) ((java.util.List<?>) value).clear();\n            }\n        } catch (Throwable ignored) {}\n    }\n\n    private static boolean isKodiInstalled(Context c) {'''
if 'notifyProjectivyChannelsChanged(context)' not in text:
    if text.count(end_anchor) != 1:
        raise SystemExit(f'channel refresh anchor mismatch: {text.count(end_anchor)}')
    text = text.replace(end_anchor, end_replacement, 1)
    engine.write_text(text)
    print('patched channel refresh notification/cache invalidation')

# Route playable Red Light items through Kodi's ACTION_VIEW media path, but route
# folder/list items through Kodi GUI navigation instead.  Kodi's Android ACTION_VIEW
# handler treats every plugin:// target as media playback; that is why a TV-show card
# with mode=build_season_list reached Red Light and then failed.  GUI.ActivateWindow
# is the native equivalent of Red Light's own external-widget click behavior.
launch = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightLaunchActivity.java')
launch.write_text(r'''package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Invisible Projectivy-owned trampoline for Red Light Android-TV preview cards. */
public final class RedLightLaunchActivity extends Activity {
    static final String EXTRA_TARGET = "com.spocky.projengmenu.embedded.REDLIGHT_TARGET";
    private static final String KODI_PACKAGE = "org.xbmc.kodi";
    private static final String KODI_SPLASH = "org.xbmc.kodi.Splash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forward();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        forward();
    }

    private void forward() {
        try {
            Intent source = getIntent();
            String target = source == null ? null : source.getStringExtra(EXTRA_TARGET);
            if (target == null || target.trim().isEmpty()) {
                Uri data = source == null ? null : source.getData();
                if (data != null && "plugin".equalsIgnoreCase(data.getScheme())) target = data.toString();
            }

            if (target != null && target.startsWith("plugin://plugin.video.redlight/")) {
                if (isPlayableRoute(target)) {
                    openPlayableInKodi(target);
                } else {
                    // Red Light itself uses ActivateWindow(Videos,plugin://...,return)
                    // for external/widget folder clicks. Open Kodi, then issue that exact
                    // GUI operation through Kodi's localhost JSON-RPC endpoint.
                    launchKodiHome();
                    final String folder = target;
                    Thread t = new Thread(() -> navigateFolderWhenKodiReady(folder), "RedLightKodiNavigation");
                    t.setDaemon(true);
                    t.start();
                }
            } else {
                launchKodiHome();
            }
        } catch (Throwable ignored) {
            try { launchKodiHome(); } catch (Throwable ignoredAgain) {}
        } finally {
            finish();
        }
    }

    private static boolean isPlayableRoute(String target) {
        try {
            String mode = Uri.parse(target).getQueryParameter("mode");
            if (mode == null) return false;
            return mode.startsWith("playback.")
                    || mode.endsWith(".resolve_rd")
                    || mode.endsWith(".resolve_ad")
                    || mode.endsWith(".resolve_oc")
                    || mode.endsWith(".resolve_tb");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void openPlayableInKodi(String target) {
        Intent kodi = new Intent(Intent.ACTION_VIEW);
        kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH));
        kodi.setData(Uri.parse(target));
        kodi.addCategory(Intent.CATEGORY_DEFAULT);
        kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(kodi);
    }

    private void launchKodiHome() {
        Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(KODI_PACKAGE);
        if (launch == null) launch = getPackageManager().getLaunchIntentForPackage(KODI_PACKAGE);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launch);
        }
    }

    private static void navigateFolderWhenKodiReady(String pluginUri) {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"GUI.ActivateWindow\","
                + "\"params\":{\"window\":\"videos\",\"parameters\":[\""
                + jsonEscape(pluginUri) + "\",\"return\"]}}";

        // Kodi starts the local JSON-RPC TCP server on 9090 when its local remote-
        // control service is available. Retry while Kodi is coming to the foreground.
        for (int attempt = 0; attempt < 28; attempt++) {
            if (sendTcpJsonRpc(request) || sendHttpJsonRpc(request)) return;
            try { Thread.sleep(attempt < 8 ? 250L : 400L); } catch (InterruptedException ignored) { return; }
        }
        // Deliberately do not ACTION_VIEW a folder as a fallback: Kodi would send it
        // to the media player again and reproduce the Red Light error seen on Shield.
    }

    private static boolean sendTcpJsonRpc(String request) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 9090), 220);
            socket.setSoTimeout(650);
            OutputStream out = socket.getOutputStream();
            out.write((request + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            return line != null && line.contains("\"result\"");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean sendHttpJsonRpc(String request) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://127.0.0.1:8080/jsonrpc").openConnection();
            connection.setConnectTimeout(220);
            connection.setReadTimeout(650);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(request.getBytes(StandardCharsets.UTF_8));
            }
            if (connection.getResponseCode() != 200) return false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) if (line.contains("\"result\"")) return true;
            }
        } catch (Throwable ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }
        return false;
    }

    private static String jsonEscape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default: out.append(ch);
            }
        }
        return out.toString();
    }
}
''')
print('replaced Red Light launch trampoline with route-aware Kodi navigation')
