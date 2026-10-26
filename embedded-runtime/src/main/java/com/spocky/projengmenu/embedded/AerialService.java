package com.spocky.projengmenu.embedded;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Projectivy wallpaper-provider service living inside Projectivy itself. */
public final class AerialService extends Service {
    private final IBinder binder = new AerialBinder();
    @Override public IBinder onBind(Intent intent) { return binder; }

    private final class AerialBinder extends Binder {
        private static final String DESCRIPTOR = "tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService";
        AerialBinder() { attachInterface(null, DESCRIPTOR); }

        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            try {
                if (code == IBinder.INTERFACE_TRANSACTION) { reply.writeString(DESCRIPTOR); return true; }
                data.enforceInterface(DESCRIPTOR);
                if (code == 1) {
                    if (data.dataAvail() > 0) data.readInt();
                    ArrayList<Parcelable> list = loadAerials(AerialService.this);
                    reply.writeNoException();
                    reply.writeTypedList(list);
                    return true;
                }
                if (code == 2) {
                    reply.writeNoException(); reply.writeString(getParams(AerialService.this)); return true;
                }
                if (code == 3) {
                    String p = data.readString();
                    if (p != null) setParams(AerialService.this, p);
                    reply.writeNoException();
                    EmbeddedRuntime.notifyAerial(AerialService.this);
                    return true;
                }
            } catch (Throwable t) {
                try { reply.writeException(new RuntimeException(t)); } catch (Throwable ignored) {}
                return true;
            }
            try { return super.onTransact(code, data, reply, flags); }
            catch (Throwable ignored) { return false; }
        }
    }

    private static String getParams(Context c) {
        return EmbeddedRuntime.projectivyPrefs(c).getString(
                "appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID,
                "{\"sources\":[\"A\",\"C1\",\"C2\"],\"prefer4k\":true,\"prefer_hdr\":false,\"fallback\":true}");
    }

    private static void setParams(Context c, String p) {
        EmbeddedRuntime.projectivyPrefs(c).edit()
                .putString("appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID, p).apply();
    }

    private static ArrayList<Parcelable> loadAerials(Context c) {
        ArrayList<Parcelable> bundled = new ArrayList<>();
        try {
            JSONObject prefs = new JSONObject(getParams(c));
            boolean prefer4k = prefs.optBoolean("prefer4k", true);
            boolean preferHdr = prefs.optBoolean("prefer_hdr", false);
            boolean fallback = prefs.optBoolean("fallback", true);
            Set<String> sources = new HashSet<>();
            JSONArray configured = prefs.optJSONArray("sources");
            if (configured != null) for (int i = 0; i < configured.length(); i++) sources.add(configured.optString(i));
            if (sources.isEmpty()) sources.add("A");

            try (InputStream in = c.getAssets().open("aerials.json")) {
                JSONArray a = new JSONArray(EmbeddedRuntime.readAll(in, 8 * 1024 * 1024));
                for (int i = 0; i < a.length() && bundled.size() < 500; i++) {
                    JSONObject o = a.optJSONObject(i);
                    if (o == null) continue;
                    String sid = o.optString("s", "A");
                    if (!sources.contains(sid)) continue;
                    String uri = o.optString("u", "").trim();
                    if (uri.isEmpty()) continue;
                    Parcelable w = wallpaper(uri, "Aerial", sourceName(sid), null);
                    if (w != null) bundled.add(w);
                }
            } catch (Throwable ignored) {}

            if (sources.contains("A")) {
                String json = httpGet("https://raw.githubusercontent.com/spocky/projectivy-plugin-wallpaper-overflight/main/videos.json", 8 * 1024 * 1024);
                if (json != null) {
                    ArrayList<Parcelable> apple = new ArrayList<>();
                    JSONArray a = new JSONArray(json);
                    for (int i = 0; i < a.length() && apple.size() < 500; i++) {
                        JSONObject o = a.optJSONObject(i);
                        if (o == null) continue;
                        String uri = choose(o, prefer4k, preferHdr, fallback);
                        if (uri == null) continue;
                        Parcelable w = wallpaper(uri, o.optString("title", "Aerial"),
                                o.optString("location", "Apple TV"), o.optString("author", null));
                        if (w != null) apple.add(w);
                    }
                    if (!apple.isEmpty()) {
                        apple.addAll(bundled);
                        bundled = apple;
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (bundled.isEmpty()) {
            Parcelable w = wallpaper("http://sylvan.apple.com/Aerials/2x/Videos/DB_D008_C010_4K_SDR_HEVC.mov",
                    "Dubai from Above", "Apple TV", null);
            if (w != null) bundled.add(w);
        }
        return bundled;
    }

    private static String choose(JSONObject o, boolean fourK, boolean hdr, boolean fallback) {
        String exact = hdr ? (fourK ? o.optString("url_4k_hdr") : o.optString("url_1080p_hdr"))
                : (fourK ? o.optString("url_4k") : o.optString("url_1080p"));
        if (!exact.trim().isEmpty()) return exact.trim();
        if (!fallback) return null;
        String[] keys = hdr ? new String[]{"url_4k_hdr", "url_4k", "url_1080p_hdr", "url_1080p"}
                : new String[]{"url_4k", "url_1080p", "url_img"};
        for (String key : keys) {
            String v = o.optString(key, "").trim();
            if (!v.isEmpty()) return v;
        }
        return null;
    }

    private static Parcelable wallpaper(String uri, String title, String source, String author) {
        try {
            Class<?> c = Class.forName("tv.projectivy.plugin.wallpaperprovider.api.Wallpaper");
            return (Parcelable) c.getConstructor(String.class, int.class, int.class,
                    String.class, String.class, String.class, String.class)
                    .newInstance(uri, 4, 0, title, source, author, null);
        } catch (Throwable ignored) { return null; }
    }

    private static String sourceName(String id) {
        if ("A".equals(id)) return "Apple TV";
        if ("AMZ".equals(id)) return "Amazon Fire TV";
        if ("C1".equals(id)) return "Community 1";
        if ("C2".equals(id)) return "Community 2";
        return "Aerial";
    }

    private static String httpGet(String url, int maxBytes) {
        HttpURLConnection h = null;
        try {
            h = (HttpURLConnection) new URL(url).openConnection();
            h.setConnectTimeout(6000); h.setReadTimeout(10000); h.setInstanceFollowRedirects(true);
            h.setRequestProperty("User-Agent", "Projectivy-Aerial-Embedded/1.0");
            int code = h.getResponseCode();
            if (code < 200 || code > 299) return null;
            try (InputStream in = h.getInputStream()) { return EmbeddedRuntime.readAll(in, maxBytes); }
        } catch (Throwable ignored) { return null; }
        finally { if (h != null) h.disconnect(); }
    }
}
