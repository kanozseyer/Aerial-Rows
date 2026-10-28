#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = p.read_text()

old = '''            Log.i(TAG, "CLEAN07 SUCCESS channels=" + channels
                    + " populated=" + populated
                    + " programs=" + totalPrograms
                    + " artwork=" + totalArtwork);
            return channels >= 12;'''
new = '''            Log.i(TAG, "CLEAN07 SUCCESS channels=" + channels
                    + " populated=" + populated
                    + " programs=" + totalPrograms
                    + " artwork=" + totalArtwork);
            if (channels < 12) return false;
            // CLEAN 08: discovery is not considered complete until the canonical
            // Home layout has been reconciled against the live Android-TV channels.
            return restoreCanonicalHomeLayout(c);'''
if text.count(old) != 1:
    raise SystemExit(f'CLEAN08 sync-return anchor mismatch: {text.count(old)}')
text = text.replace(old, new, 1)

anchor = '''    private static Entry findRedLight(Context c) {'''
if text.count(anchor) != 1:
    raise SystemExit(f'CLEAN08 helper anchor mismatch: {text.count(anchor)}')

helper = r'''    // ---------------------------------------------------------------------
    // CLEAN 08 — canonical 00:28:28 Home layout + dynamic Red Light identity
    // ---------------------------------------------------------------------
    private static final String CLEAN08_TAG = "ProjectivyHomeRestore";
    private static final String CLEAN08_STATE = "clean08_home_restore_state";
    private static final String CLEAN08_APPLIED = "canonical_002828_applied_v1";
    private static final String CLEAN08_SIGNATURE = "redlight_catalog_signature_v1";
    private static final String CLEAN08_IDENTITIES = "redlight_identity_map_v1";
    private static final String CLEAN08_PROFILE_STATE = "clean05_profile_restore_state";
    private static final String CLEAN08_PROFILE_DONE = "canonical_profile_applied_cda71dba_v1";
    private static final String CLEAN08_PROFILE_ASSET = "launcher_default_profile.plbackup";
    private static final String CLEAN08_PROFILE_SHA = "cda71dbae3d2eca7bd298a198aca4068187bde34f25727df9e9ddd18e8aac2b7";

    static final class HomeChannel {
        long id;
        String pkg;
        String key;
        String title;
        String uniqueId() { return (pkg == null ? "" : pkg) + (title == null ? "" : title); }
    }

    private static boolean restoreCanonicalHomeLayout(Context c) {
        try {
            // CLEAN 05 owns the canonical preference/appearance import. Never build
            // Home categories before that import has finished, otherwise Projectivy
            // can cache a layout against pre-restore appearance/settings state.
            android.content.SharedPreferences profileState = c.getSharedPreferences(CLEAN08_PROFILE_STATE, Context.MODE_PRIVATE);
            if (!profileState.getBoolean(CLEAN08_PROFILE_DONE, false)) {
                Log.i(CLEAN08_TAG, "CLEAN08 WAIT canonical profile/appearance not applied yet");
                return false;
            }

            java.util.ArrayList<HomeChannel> all = queryHomeChannels(c);
            java.util.LinkedHashMap<String, HomeChannel> redByKey = new java.util.LinkedHashMap<>();
            java.util.HashMap<String, HomeChannel> byUniqueId = new java.util.HashMap<>();
            java.util.HashMap<String, HomeChannel> redByTitle = new java.util.HashMap<>();
            for (HomeChannel ch : all) {
                byUniqueId.put(ch.uniqueId(), ch);
                if (ch.key != null && ch.key.startsWith("redlight:") && c.getPackageName().equals(ch.pkg)) {
                    redByKey.put(ch.key, ch);
                    redByTitle.put(cleanRedTitle(ch.title), ch);
                }
            }
            if (redByKey.isEmpty()) {
                Log.i(CLEAN08_TAG, "CLEAN08 WAIT no live redlight: channels");
                return false;
            }

            String signature = redCatalogSignature(redByKey);
            android.content.SharedPreferences state = c.getSharedPreferences(CLEAN08_STATE, Context.MODE_PRIVATE);
            boolean applied = state.getBoolean(CLEAN08_APPLIED, false);
            String oldSignature = state.getString(CLEAN08_SIGNATURE, "");
            if (applied && signature.equals(oldSignature)) {
                Log.i(CLEAN08_TAG, "CLEAN08 catalog unchanged liveRed=" + redByKey.size());
                return true;
            }

            JSONObject canonical = loadCanonicalProfile(c);
            org.json.JSONArray canonicalLayout = new org.json.JSONArray(canonical.getString("action_categories_list"));
            android.content.SharedPreferences prefs = c.getSharedPreferences(c.getPackageName() + "_preferences", Context.MODE_PRIVATE);

            // Explicitly carry the two requested appearance colours from the exact
            // canonical backup before rebuilding the category model. CLEAN 05 already
            // imports all non-layout settings; these writes make the dependency
            // intentional and verifiable in CLEAN 08 as well.
            android.content.SharedPreferences.Editor appearance = prefs.edit();
            if (canonical.has("appearance_cards_bg_tint"))
                appearance.putInt("appearance_cards_bg_tint", canonical.getInt("appearance_cards_bg_tint"));
            if (canonical.has("appearance_statusbar_maincontainer_icon_background_tint"))
                appearance.putInt("appearance_statusbar_maincontainer_icon_background_tint",
                        canonical.getInt("appearance_statusbar_maincontainer_icon_background_tint"));
            appearance.commit();
            Log.i(CLEAN08_TAG, "CLEAN08 appearance cardsBg="
                    + prefs.getInt("appearance_cards_bg_tint", 0)
                    + " iconBg=" + prefs.getInt("appearance_statusbar_maincontainer_icon_background_tint", 0));

            org.json.JSONArray baseLayout = canonicalLayout;
            java.util.HashMap<String, String> previousUidToKey = new java.util.HashMap<>();
            if (applied) {
                String currentRaw = prefs.getString("action_categories_list", "");
                if (currentRaw != null && !currentRaw.isEmpty()) {
                    try { baseLayout = new org.json.JSONArray(currentRaw); } catch (Throwable ignored) {}
                }
                try {
                    JSONObject oldIds = new JSONObject(state.getString(CLEAN08_IDENTITIES, "{}"));
                    java.util.Iterator<String> keys = oldIds.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String uid = oldIds.optString(key, "");
                        if (!uid.isEmpty()) previousUidToKey.put(uid, key);
                    }
                } catch (Throwable ignored) {}
            }

            java.util.HashMap<String, JSONObject> canonicalByKey = canonicalRedTemplates(canonicalLayout);
            org.json.JSONArray out = new org.json.JSONArray();
            java.util.HashSet<String> usedRed = new java.util.HashSet<>();
            int lastRed = lastRedIndex(baseLayout, c.getPackageName());
            int matched = 0, removed = 0, added = 0;

            for (int i = 0; i < baseLayout.length(); i++) {
                Object raw = baseLayout.get(i);
                if (!(raw instanceof JSONObject)) {
                    out.put(raw);
                    if (i == lastRed) added += appendNewRedRows(out, redByKey, usedRed, canonicalByKey, c);
                    continue;
                }
                JSONObject src = (JSONObject) raw;
                if (isRedCategory(src, c.getPackageName())) {
                    String uid = src.optString("channelUniqueId", "");
                    String key = previousUidToKey.get(uid);
                    if (key == null || key.isEmpty()) key = canonicalRedKey(src.optString("title", ""));
                    HomeChannel live = key == null || key.isEmpty() ? null : redByKey.get(key);
                    if (live == null) live = redByTitle.get(cleanRedTitle(src.optString("title", "")));
                    if (live != null) {
                        out.put(materializeChannelCategory(src, live, false));
                        usedRed.add(live.key);
                        matched++;
                    } else {
                        removed++;
                    }
                } else {
                    // Keep canonical/non-Red-Light structure exactly. Where the same
                    // real Android-TV channel exists now, refresh only its volatile id/title.
                    JSONObject copy = new JSONObject(src.toString());
                    if (copy.optInt("type", -1) == 6) {
                        HomeChannel live = byUniqueId.get(copy.optString("channelUniqueId", ""));
                        if (live != null) {
                            copy.put("id", live.id);
                            copy.put("title", live.title);
                            copy.put("channelUniqueId", live.uniqueId());
                        }
                    }
                    out.put(copy);
                }
                if (i == lastRed) added += appendNewRedRows(out, redByKey, usedRed, canonicalByKey, c);
            }
            if (lastRed < 0) added += appendNewRedRows(out, redByKey, usedRed, canonicalByKey, c);

            JSONObject identities = new JSONObject();
            for (java.util.Map.Entry<String, HomeChannel> e : redByKey.entrySet())
                identities.put(e.getKey(), e.getValue().uniqueId());

            final String layout = out.toString();
            final JSONObject identityFinal = identities;
            final int matchedFinal = matched, removedFinal = removed, addedFinal = added;
            final java.util.concurrent.atomic.AtomicBoolean ok = new java.util.concurrent.atomic.AtomicBoolean(false);
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                try {
                    if (!prefs.edit().putString("action_categories_list", layout).commit())
                        throw new IllegalStateException("could not persist action_categories_list");

                    Class<?> settings = Class.forName("dj3");
                    java.lang.reflect.Method reset = settings.getDeclaredMethod("m");
                    reset.setAccessible(true);
                    reset.invoke(null);

                    Class<?> categories = Class.forName("w40");
                    java.lang.reflect.Method reload = categories.getDeclaredMethod("z", boolean.class);
                    reload.setAccessible(true);
                    reload.invoke(null, false);

                    if (!state.edit()
                            .putBoolean(CLEAN08_APPLIED, true)
                            .putString(CLEAN08_SIGNATURE, signature)
                            .putString(CLEAN08_IDENTITIES, identityFinal.toString())
                            .commit()) throw new IllegalStateException("could not persist CLEAN08 state");

                    Log.i(CLEAN08_TAG, "CLEAN08 APPLIED categories=" + out.length()
                            + " liveRed=" + redByKey.size()
                            + " matched=" + matchedFinal
                            + " removed=" + removedFinal
                            + " addedHidden=" + addedFinal
                            + " mode=" + (applied ? "reconcile" : "canonical"));
                    ok.set(true);
                } catch (Throwable t) {
                    Log.e(CLEAN08_TAG, "CLEAN08 APPLY FAILED", t);
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(6, java.util.concurrent.TimeUnit.SECONDS)) {
                Log.i(CLEAN08_TAG, "CLEAN08 APPLY timeout");
                return false;
            }
            return ok.get();
        } catch (Throwable t) {
            Log.e(CLEAN08_TAG, "CLEAN08 FAILED", t);
            return false;
        }
    }

    private static java.util.ArrayList<HomeChannel> queryHomeChannels(Context c) {
        java.util.ArrayList<HomeChannel> out = new java.util.ArrayList<>();
        try (Cursor cur = c.getContentResolver().query(TvContract.Channels.CONTENT_URI,
                new String[]{"_id", "package_name", "internal_provider_id", "display_name"}, null, null, null)) {
            if (cur == null) return out;
            while (cur.moveToNext()) {
                HomeChannel ch = new HomeChannel();
                ch.id = cur.getLong(0);
                ch.pkg = cur.isNull(1) ? "" : cur.getString(1);
                ch.key = cur.isNull(2) ? "" : cur.getString(2);
                ch.title = cur.isNull(3) ? "" : cur.getString(3);
                if (!ch.title.isEmpty()) out.add(ch);
            }
        } catch (Throwable t) {
            Log.i(CLEAN08_TAG, "CLEAN08 channel query unavailable");
        }
        return out;
    }

    private static JSONObject loadCanonicalProfile(Context c) throws Exception {
        byte[] encrypted;
        try (InputStream in = c.getAssets().open(CLEAN08_PROFILE_ASSET);
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[16384];
            for (int n; (n = in.read(buf)) >= 0;) out.write(buf, 0, n);
            encrypted = out.toByteArray();
        }
        if (!CLEAN08_PROFILE_SHA.equals(clean08Sha256(encrypted)))
            throw new IllegalStateException("canonical backup SHA mismatch");
        Class<?> s71 = Class.forName("s71");
        java.lang.reflect.Method decrypt = s71.getDeclaredMethod("b", byte[].class);
        decrypt.setAccessible(true);
        byte[] plain = (byte[]) decrypt.invoke(null, (Object) encrypted);
        if (plain == null || plain.length == 0) throw new IllegalStateException("canonical backup decrypt empty");
        return new JSONObject(new String(plain, java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String clean08Sha256(byte[] data) throws Exception {
        byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder s = new StringBuilder(d.length * 2);
        for (byte b : d) s.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
        return s.toString();
    }

    private static String redCatalogSignature(java.util.LinkedHashMap<String, HomeChannel> redByKey) throws Exception {
        java.util.ArrayList<String> rows = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, HomeChannel> e : redByKey.entrySet()) {
            HomeChannel ch = e.getValue();
            rows.add(e.getKey() + "\t" + ch.id + "\t" + ch.title);
        }
        java.util.Collections.sort(rows);
        return clean08Sha256(joinStrings(rows, "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String joinStrings(java.util.List<String> rows, String sep) {
        StringBuilder b = new StringBuilder();
        for (String row : rows) {
            if (b.length() > 0) b.append(sep);
            b.append(row);
        }
        return b.toString();
    }

    private static java.util.HashMap<String, JSONObject> canonicalRedTemplates(org.json.JSONArray canonical) {
        java.util.HashMap<String, JSONObject> out = new java.util.HashMap<>();
        for (int i = 0; i < canonical.length(); i++) {
            JSONObject o = canonical.optJSONObject(i);
            if (o == null) continue;
            String key = canonicalRedKey(o.optString("title", ""));
            if (key != null && !key.isEmpty()) out.put(key, o);
        }
        return out;
    }

    private static int lastRedIndex(org.json.JSONArray a, String pkg) {
        int last = -1;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && isRedCategory(o, pkg)) last = i;
        }
        return last;
    }

    private static boolean isRedCategory(JSONObject o, String pkg) {
        if (o == null || o.optInt("type", -1) != 6) return false;
        String uid = o.optString("channelUniqueId", "");
        String title = o.optString("title", "");
        return uid.startsWith(pkg + "Red Light · ") || title.startsWith("Red Light · ");
    }

    private static JSONObject materializeChannelCategory(JSONObject src, HomeChannel live, boolean forceHidden) throws Exception {
        JSONObject o = new JSONObject(src.toString());
        o.put("id", live.id);
        o.put("type", 6);
        o.put("channelUniqueId", live.uniqueId());
        o.put("title", live.title);
        if (forceHidden) o.put("isVisible", false);
        return o;
    }

    private static int appendNewRedRows(org.json.JSONArray out,
            java.util.LinkedHashMap<String, HomeChannel> redByKey,
            java.util.HashSet<String> used,
            java.util.HashMap<String, JSONObject> canonicalByKey,
            Context c) throws Exception {
        int added = 0;
        java.util.ArrayList<String> keys = new java.util.ArrayList<>(redByKey.keySet());
        java.util.Collections.sort(keys);
        for (String key : keys) {
            if (used.contains(key)) continue;
            HomeChannel live = redByKey.get(key);
            JSONObject template = canonicalByKey.get(key);
            JSONObject o;
            if (template != null) {
                // A canonical row that temporarily vanished and later returned should
                // regain its canonical visibility/scale rather than being treated new.
                o = materializeChannelCategory(template, live, false);
            } else {
                o = new JSONObject();
                o.put("id", live.id);
                o.put("type", 6);
                o.put("channelUniqueId", live.uniqueId());
                o.put("scale", 1.2);
                o.put("isVisible", false);
                o.put("title", live.title);
            }
            out.put(o);
            used.add(key);
            added++;
        }
        return added;
    }

    private static String cleanRedTitle(String title) {
        String s = title == null ? "" : title.trim();
        if (s.startsWith("Red Light · ")) s = s.substring("Red Light · ".length());
        return normalizeToken(s);
    }

    private static String canonicalRedKey(String title) {
        String n = cleanRedTitle(title);
        if (n.isEmpty()) return "";
        if ("in_progress_episodes".equals(n)) return "redlight:in_progress_episodes";
        if ("recently_watched_episodes".equals(n)) return "redlight:recently_watched_episodes";
        if ("next_episodes".equals(n)) return "redlight:next_episodes:default";
        if ("continue_watching".equals(n)) return "redlight:continue_watching";
        if ("movies_watchlist".equals(n)) return "redlight:trakt:watchlist:movies";
        if ("tv_shows_watchlist".equals(n)) return "redlight:trakt:watchlist:shows";
        if ("movies_in_progress".equals(n)) return "redlight:in_progress:movies";
        if ("tv_shows_in_progress".equals(n)) return "redlight:in_progress:shows";

        String media = "";
        String semantic = n;
        if (semantic.endsWith("_movies")) {
            media = "movies";
            semantic = semantic.substring(0, semantic.length() - "_movies".length());
        } else if (semantic.endsWith("_tv_shows")) {
            media = "shows";
            semantic = semantic.substring(0, semantic.length() - "_tv_shows".length());
        }
        if (media.isEmpty()) return "";
        if ("watched".equals(semantic)) return "redlight:watched:" + media;
        if ("recently_watched".equals(semantic)) return "redlight:recently_watched:" + media;
        if ("trending".equals(semantic) || "trending_recent".equals(semantic)
                || "popular".equals(semantic) || "popular_today".equals(semantic)
                || "premieres".equals(semantic) || "latest_releases".equals(semantic)
                || "most_watched".equals(semantic) || "most_favorited".equals(semantic)
                || "top_10_box_office".equals(semantic) || "top10_box_office".equals(semantic)
                || "blockbusters".equals(semantic) || "in_theaters".equals(semantic)
                || "up_coming".equals(semantic) || "upcoming".equals(semantic)
                || "oscar_winners".equals(semantic)) {
            if ("top_10_box_office".equals(semantic)) semantic = "top10_boxoffice";
            if ("top10_box_office".equals(semantic)) semantic = "top10_boxoffice";
            if ("up_coming".equals(semantic)) semantic = "upcoming";
            return "redlight:shelf:" + semantic + ":" + media;
        }
        return "";
    }

'''
text = text.replace(anchor, helper + anchor, 1)
p.write_text(text)

check = p.read_text()
for token in [
    'CLEAN08 APPLIED categories=',
    'redlight_catalog_signature_v1',
    'canonical_profile_applied_cda71dba_v1',
    'appearance_cards_bg_tint',
    'appearance_statusbar_maincontainer_icon_background_tint',
    'redlight:shelf:trending:movies',
    'redlight:trakt:watchlist:shows',
    'return restoreCanonicalHomeLayout(c);',
]:
    if token not in check:
        raise SystemExit('CLEAN08 source verification failed: ' + token)
print('PASS: CLEAN 08 source prepared — canonical layout + dynamic Red Light semantic reconciliation')
