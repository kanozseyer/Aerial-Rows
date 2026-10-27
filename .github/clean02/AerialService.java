package com.spocky.projengmenu.embedded;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * CLEAN 02: minimal embedded Projectivy wallpaper provider.
 *
 * Deliberately contains no Configure Activity, boot receiver, background job,
 * profile restore, Red Light logic, or launcher hooks. It only implements the
 * Projectivy wallpaper-provider Binder contract and serves Aerial videos.
 */
public final class AerialService extends Service {
    public static final String AERIAL_ID = "8c00feaf-c7be-43c2-80f9-d49853aaa99a";
    private static final String DESCRIPTOR = "tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService";
    private static final String DEFAULT_PARAMS = "{\"sources\":[\"A\",\"C1\",\"C2\"],\"prefer4k\":true,\"prefer_hdr\":false,\"fallback\":true}";
    private static final String PREFS = "clean02_aerial_provider";
    private static final String PARAMS = "params";

    private final IBinder binder = new AerialBinder();

    @Override public IBinder onBind(Intent intent) {
        return binder;
    }

    private final class AerialBinder extends Binder {
        AerialBinder() {
            attachInterface(null, DESCRIPTOR);
        }

        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            try {
                if (code == IBinder.INTERFACE_TRANSACTION) {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                data.enforceInterface(DESCRIPTOR);
                if (code == 1) {
                    if (data.dataAvail() > 0) data.readInt();
                    ArrayList<Parcelable> items = loadAerials(AerialService.this);
                    reply.writeNoException();
                    reply.writeTypedList(items);
                    return true;
                }
                if (code == 2) {
                    reply.writeNoException();
                    reply.writeString(getParams(AerialService.this));
                    return true;
                }
                if (code == 3) {
                    String params = data.readString();
                    if (params != null && !params.trim().isEmpty()) setParams(AerialService.this, params);
                    reply.writeNoException();
                    return true;
                }
            } catch (Throwable t) {
                try { reply.writeException(new RuntimeException(t)); } catch (Throwable ignored) {}
                return true;
            }
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String getParams(Context c) {
        return prefs(c).getString(PARAMS, DEFAULT_PARAMS);
    }

    private static void setParams(Context c, String params) {
        prefs(c).edit().putString(PARAMS, params).apply();
    }

    private static ArrayList<Parcelable> loadAerials(Context c) {
        ArrayList<Parcelable> bundled = new ArrayList<>();
        try {
            JSONObject settings = new JSONObject(getParams(c));
            boolean prefer4k = settings.optBoolean("prefer4k", true);
            boolean preferHdr = settings.optBoolean("prefer_hdr", false);
            boolean fallback = settings.optBoolean("fallback", true);
            Set<String> sources = new HashSet<>();
            JSONArray configured = settings.optJSONArray("sources");
            if (configured != null) {
                for (int i = 0; i < configured.length(); i++) {
                    String value = configured.optString(i, "").trim();
                    if (!value.isEmpty()) sources.add(value);
                }
            }
            if (sources.isEmpty()) sources.add("A");

            try (InputStream in = c.getAssets().open("aerials.json")) {
                JSONArray a = new JSONArray(readAll(in, 8 * 1024 * 1024));
                for (int i = 0; i < a.length() && bundled.size() < 500; i++) {
                    JSONObject o = a.optJSONObject(i);
                    if (o == null) continue;
                    String sourceId = o.optString("s", "A");
                    if (!sources.contains(sourceId)) continue;
                    String uri = o.optString("u", "").trim();
                    if (uri.isEmpty()) continue;
                    Parcelable item = wallpaper(uri, "Aerial", sourceName(sourceId), null);
                    if (item != null) bundled.add(item);
                }
            } catch (Throwable ignored) {}

            // Prefer the live Apple catalogue when available, but retain the
            // pinned bundled catalogue as an offline/failure fallback.
            if (sources.contains("A")) {
                String json = httpGet(
                        "https://raw.githubusercontent.com/spocky/projectivy-plugin-wallpaper-overflight/main/videos.json",
                        8 * 1024 * 1024);
                if (json != null) {
                    ArrayList<Parcelable> live = new ArrayList<>();
                    JSONArray a = new JSONArray(json);
                    for (int i = 0; i < a.length() && live.size() < 500; i++) {
                        JSONObject o = a.optJSONObject(i);
                        if (o == null) continue;
                        String uri = choose(o, prefer4k, preferHdr, fallback);
                        if (uri == null) continue;
                        Parcelable item = wallpaper(
                                uri,
                                o.optString("title", "Aerial"),
                                o.optString("location", "Apple TV"),
                                o.optString("author", null));
                        if (item != null) live.add(item);
                    }
                    if (!live.isEmpty()) {
                        live.addAll(bundled);
                        bundled = live;
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (bundled.isEmpty()) {
            Parcelable item = wallpaper(
                    "http://sylvan.apple.com/Aerials/2x/Videos/DB_D008_C010_4K_SDR_HEVC.mov",
                    "Dubai from Above", "Apple TV", null);
            if (item != null) bundled.add(item);
        }
        return bundled;
    }

    private static String choose(JSONObject o, boolean fourK, boolean hdr, boolean fallback) {
        String exact = hdr
                ? (fourK ? o.optString("url_4k_hdr", "") : o.optString("url_1080p_hdr", ""))
                : (fourK ? o.optString("url_4k", "") : o.optString("url_1080p", ""));
        if (!exact.trim().isEmpty()) return exact.trim();
        if (!fallback) return null;
        String[] keys = hdr
                ? new String[]{"url_4k_hdr", "url_4k", "url_1080p_hdr", "url_1080p", "url_img"}
                : new String[]{"url_4k", "url_1080p", "url_img"};
        for (String key : keys) {
            String value = o.optString(key, "").trim();
            if (!value.isEmpty()) return value;
        }
        return null;
    }

    private static Parcelable wallpaper(String uri, String title, String source, String author) {
        try {
            Class<?> c = Class.forName("tv.projectivy.plugin.wallpaperprovider.api.Wallpaper");
            return (Parcelable) c.getConstructor(
                    String.class, int.class, int.class,
                    String.class, String.class, String.class, String.class)
                    .newInstance(uri, 4, 0, title, source, author, null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String sourceName(String id) {
        if ("A".equals(id)) return "Apple TV";
        if ("AMZ".equals(id)) return "Amazon Fire TV";
        if ("C1".equals(id)) return "Community 1";
        if ("C2".equals(id)) return "Community 2";
        return "Aerial";
    }

    private static String httpGet(String url, int maxBytes) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Projectivy-CLEAN02-Aerial/1.0");
            int code = connection.getResponseCode();
            if (code < 200 || code > 299) return null;
            try (InputStream in = connection.getInputStream()) {
                return readAll(in, maxBytes);
            }
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readAll(InputStream in, int maxBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        int total = 0;
        for (;;) {
            int n = in.read(buffer);
            if (n < 0) break;
            total += n;
            if (total > maxBytes) throw new IllegalStateException("input too large");
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
