package com.spocky.projengmenu.embedded;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * CLEAN 07: dynamic Red Light Movies/TV shelf discovery.
 *
 * The live Red Light menu is scanned on every sync. Current menu labels are used
 * only for presentation; channel identity is derived from mode/action semantics.
 * Known semantic routes keep stable keys and unknown future direct Movies/TV
 * shelves fall back to a normalized action key, so simple Red Light menu renames
 * do not break the integration and newly added direct shelves can appear without
 * rebuilding Projectivy. Playback remains deliberately disabled in CLEAN 07.
 */
public final class RedLightDiscoveryEngine {
    private static final String TAG = "ProjectivyRedLight";
    private static final String KODI_PACKAGE = "org.xbmc.kodi";
    private static final String AUTH = "org.xbmc.kodi.file";
    private static final String VIDEO_ADDONS = "addons://sources/video/";
    private static final int MAX_DEPTH = 7;
    private static final int MAX_SCANS = 96;
    private static final int MAX_CHILDREN = 100;
    private static final int MAX_PROGRAMS = 60;

    private RedLightDiscoveryEngine() {}

    static final class Entry {
        String name, category, uri, mediaType;
        boolean dir() {
            return "directory".equalsIgnoreCase(category)
                    || "folder".equalsIgnoreCase(category)
                    || "directory".equalsIgnoreCase(mediaType);
        }
    }

    static final class Node {
        Entry e;
        List<String> trail;
        int depth;
        Node(Entry e, List<String> trail, int depth) {
            this.e = e;
            this.trail = trail;
            this.depth = depth;
        }
    }

    static final class Section {
        String key, title, uri, media;
        Section(String key, String title, String uri, String media) {
            this.key = key;
            this.title = title;
            this.uri = uri;
            this.media = media;
        }
    }

    static final class ProgramStats {
        boolean available;
        int inserted;
        int artwork;
    }

    public static boolean syncOnce(Context c) {
        try {
            c.getPackageManager().getApplicationInfo(KODI_PACKAGE, 0);
            Entry root = findRedLight(c);
            if (root == null) {
                Log.i(TAG, "CLEAN07 Red Light root unavailable");
                return false;
            }

            LinkedHashMap<String, Section> sections = discoverSections(c, root);
            int movieSections = 0, showSections = 0;
            for (Section s : sections.values()) {
                if ("movies".equals(s.media)) movieSections++;
                if ("shows".equals(s.media) || "episodes".equals(s.media)) showSections++;
            }
            Log.i(TAG, "CLEAN07 DISCOVERED sections=" + sections.size()
                    + " movies=" + movieSections + " showsEpisodes=" + showSections);

            // A partially warmed Kodi provider can return a tiny fragment of the menu.
            // Do not freeze that as a successful scan; retry on the next Home resume.
            if (sections.size() < 12 || movieSections < 5 || showSections < 5) {
                Log.i(TAG, "CLEAN07 partial discovery — will retry on next MainActivity resume");
                return false;
            }

            ArtworkCache art = ArtworkCache.open(c);
            int channels = 0, populated = 0, totalPrograms = 0, totalArtwork = 0;
            for (Section s : sections.values()) {
                long channel = ensureChannel(c, s);
                if (channel <= 0) {
                    Log.i(TAG, "CLEAN07 CHANNEL FAILED key=" + s.key + " title=" + s.title);
                    continue;
                }
                channels++;
                ProgramStats ps = syncPrograms(c, channel, s, art);
                if (ps.available) {
                    if (ps.inserted > 0) populated++;
                    totalPrograms += ps.inserted;
                    totalArtwork += ps.artwork;
                }
                Log.i(TAG, "CLEAN07 CHANNEL key=" + s.key + " id=" + channel
                        + " programs=" + ps.inserted + " artwork=" + ps.artwork
                        + " title=" + s.title);
            }
            if (art != null) art.close();

            Log.i(TAG, "CLEAN07 SUCCESS channels=" + channels
                    + " populated=" + populated
                    + " programs=" + totalPrograms
                    + " artwork=" + totalArtwork);
            return channels >= 12;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07 FAILED", t);
            return false;
        }
    }

    private static Entry findRedLight(Context c) {
        List<Entry> addons = list(c, VIDEO_ADDONS);
        if (addons == null) return null;
        for (Entry e : addons) if ("red light".equals(norm(e.name))) return e;
        for (Entry e : addons) if (norm(e.name).contains("red light")) return e;
        return null;
    }

    private static LinkedHashMap<String, Section> discoverSections(Context c, Entry root) {
        LinkedHashMap<String, Section> found = new LinkedHashMap<>();
        List<Entry> first = list(c, raw(root.uri));
        if (first == null) return found;

        ArrayDeque<Node> queue = new ArrayDeque<>();
        for (Entry e : first) if (e.dir()) queue.add(new Node(e, single(e.name), 1));
        Set<String> visited = new HashSet<>();
        int scans = 0;

        while (!queue.isEmpty() && scans < MAX_SCANS) {
            Node n = queue.removeFirst();
            String nativeUri = raw(n.e.uri);
            if (!visited.add(nativeUri)) continue;

            Section s = classify(n.e, n.trail);
            if (s != null) {
                if (!found.containsKey(s.key)) {
                    found.put(s.key, s);
                    Log.i(TAG, "CLEAN07 CLASSIFY key=" + s.key + " title=" + s.title + " uri=" + s.uri);
                }
                continue;
            }

            if (n.depth >= MAX_DEPTH || !shouldTraverse(n.e, n.trail)) continue;
            scans++;
            List<Entry> children = list(c, nativeUri);
            if (children == null) continue;
            int count = 0;
            for (Entry child : children) {
                if (!child.dir()) continue;
                if (++count > MAX_CHILDREN) break;
                ArrayList<String> trail = new ArrayList<>(n.trail);
                trail.add(child.name);
                queue.add(new Node(child, trail, n.depth + 1));
            }
        }
        return found;
    }

    private static Section classify(Entry e, List<String> trail) {
        Uri u;
        try { u = Uri.parse(raw(e.uri)); }
        catch (Throwable t) { return null; }

        String mode = q(u, "mode");
        String action = q(u, "action");
        String path = norm(join(trail));
        boolean anime = "true".equalsIgnoreCase(q(u, "is_anime_list")) || path.contains("anime");
        if (anime) return null;

        // Episode/status shelves are identified by route mode, not by the current label.
        if ("build_recently_watched_episode".equals(mode))
            return section("redlight:recently_watched_episodes", liveTitle(e, u, "Recently Watched Episodes", ""), e, "episodes");
        if ("build_in_progress_episode".equals(mode))
            return section("redlight:in_progress_episodes", liveTitle(e, u, "In Progress Episodes", ""), e, "episodes");
        if ("build_next_episode".equals(mode))
            return section("redlight:next_episodes:default", liveTitle(e, u, "Next Episodes", ""), e, "episodes");
        if ("build_continue_watching".equals(mode))
            return section("redlight:continue_watching", liveTitle(e, u, "Continue Watching", ""), e, "episodes");

        String media = inferMedia(e, trail, u, mode);
        if (!"movies".equals(media) && !"shows".equals(media)) return null;

        // Keep the proven stable watchlist identities even if Red Light renames them.
        if ("trakt_watchlist".equals(action)) {
            String key = "redlight:trakt:watchlist:" + media;
            String fallback = "movies".equals(media) ? "Movies Watchlist" : "TV Shows Watchlist";
            return section(key, liveTitle(e, u, fallback, ""), e, media);
        }

        // Helper/list-generator routes are intentionally not promoted as launcher rows.
        if (action.isEmpty() || "trakt_watchlist_lists".equals(action)
                || action.contains("genre") || action.contains("provider")
                || action.contains("language") || action.contains("certification")
                || action.contains("decade") || action.contains("year")) return null;

        // Future-proof direct shelf rule: only promote content-list routes that live
        // under Red Light's actual Movies/TV Shows menus. New direct shelf actions
        // therefore appear automatically, while unrelated nested list helpers do not.
        if (!("build_movie_list".equals(mode) || "build_tvshow_list".equals(mode))) return null;
        if (!underMainMediaMenu(trail, media)) return null;

        String semantic = semanticFromAction(media, action);
        if (semantic.isEmpty()) return null;
        String key = stableKey(media, semantic, action);
        String suffix = "movies".equals(media) ? "Movies" : "TV Shows";
        String title = liveTitle(e, u, humanize(semantic), suffix);
        return section(key, title, e, media);
    }

    /**
     * Derive meaning algorithmically from Red Light's route action. This avoids a
     * table of current menu names and keeps Trending/Trending Recent, Popular/Popular
     * Today, etc. distinct. Known local-state spellings are canonicalized.
     */
    private static String semanticFromAction(String media, String action) {
        String a = normalizeToken(action);
        if (a.isEmpty()) return "";

        if ("movies".equals(media)) {
            if (a.startsWith("trakt_movies_")) a = a.substring("trakt_movies_".length());
            else if (a.startsWith("tmdb_movies_")) a = a.substring("tmdb_movies_".length());
            else if (a.startsWith("movies_")) a = a.substring("movies_".length());
            if (a.endsWith("_movies")) a = a.substring(0, a.length() - "_movies".length());
        } else {
            if (a.startsWith("trakt_tv_")) a = a.substring("trakt_tv_".length());
            else if (a.startsWith("tmdb_tv_")) a = a.substring("tmdb_tv_".length());
            else if (a.startsWith("tv_")) a = a.substring("tv_".length());
            if (a.endsWith("_tvshows")) a = a.substring(0, a.length() - "_tvshows".length());
            if (a.endsWith("_shows")) a = a.substring(0, a.length() - "_shows".length());
        }

        // Red Light's local-state actions put the media token at the end.
        if ("recent_watched".equals(a)) a = "recently_watched";
        if (a.startsWith("recent_watched_")) a = "recently_watched";
        if (a.startsWith("in_progress_")) a = "in_progress";
        if (a.startsWith("watched_")) a = "watched";

        return normalizeToken(a);
    }

    private static String stableKey(String media, String semantic, String action) {
        // Reuse CLEAN 06D's Trending Movies key and the stable v9 semantic keys for
        // common shelves. Unknown future shelves still get a deterministic action key.
        if ("trending".equals(semantic) || "trending_recent".equals(semantic)
                || "popular".equals(semantic) || "popular_today".equals(semantic)
                || "premieres".equals(semantic) || "latest_releases".equals(semantic)
                || "most_watched".equals(semantic) || "most_favorited".equals(semantic)
                || "top10_boxoffice".equals(semantic) || "blockbusters".equals(semantic)
                || "in_theaters".equals(semantic) || "upcoming".equals(semantic)
                || "oscar_winners".equals(semantic)) {
            return "redlight:shelf:" + semantic + ":" + media;
        }
        if ("watched".equals(semantic)) return "redlight:watched:" + media;
        if ("recently_watched".equals(semantic)) return "redlight:recently_watched:" + media;
        if ("in_progress".equals(semantic)) return "redlight:in_progress:" + media;
        return "redlight:menu:" + media + ":" + normalizeToken(action);
    }

    private static boolean underMainMediaMenu(List<String> trail, String media) {
        if (trail == null || trail.isEmpty()) return false;
        String first = norm(trail.get(0));
        if ("movies".equals(media)) return first.equals("movies") || first.equals("movie");
        return first.equals("tv shows") || first.equals("tv show") || first.equals("shows") || first.equals("series");
    }

    private static String inferMedia(Entry e, List<String> trail, Uri u, String mode) {
        if ("build_movie_list".equals(mode)) return "movies";
        if ("build_tvshow_list".equals(mode)) return "shows";
        String param = norm(q(u, "media_type") + " " + q(u, "media") + " " + q(u, "content_type"));
        if (param.contains("movie")) return "movies";
        if (param.contains("tvshow") || param.contains("tv show") || param.contains("series")) return "shows";
        String path = norm(join(trail));
        if (path.contains("movie")) return "movies";
        if (path.contains("tv show") || path.contains("series")) return "shows";
        String mt = norm(e.mediaType);
        if ("movie".equals(mt)) return "movies";
        if ("tvshow".equals(mt) || "show".equals(mt)) return "shows";
        return "";
    }

    private static String liveTitle(Entry e, Uri u, String fallback, String mediaSuffix) {
        String value = stripMarkup(e == null ? "" : e.name);
        String category = stripMarkup(q(u, "category_name"));
        if (!category.isEmpty()) value = category;
        if (value.isEmpty()) value = fallback;
        if (value.isEmpty()) value = "Red Light";

        String n = norm(value);
        if (!mediaSuffix.isEmpty()) {
            if ("Movies".equals(mediaSuffix) && !n.contains("movie")) value += " Movies";
            if ("TV Shows".equals(mediaSuffix) && !n.contains("tv show") && !n.contains("show") && !n.contains("series")) value += " TV Shows";
        }
        return value;
    }

    private static String humanize(String token) {
        if (token == null || token.isEmpty()) return "";
        String[] parts = token.replace('_', ' ').trim().split(" +");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            if ("top10".equals(p)) out.append("Top 10");
            else out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return out.toString();
    }

    private static Section section(String key, String title, Entry e, String media) {
        return new Section(key, "Red Light · " + title.trim(), raw(e.uri), media);
    }

    private static boolean shouldTraverse(Entry e, List<String> trail) {
        Uri u;
        try { u = Uri.parse(raw(e.uri)); }
        catch (Throwable t) { return false; }

        String mode = q(u, "mode"), action = q(u, "action"), label = norm(e.name), path = norm(join(trail));
        if (path.contains("anime")) return false;
        if (label.equals("tools") || label.equals("search") || label.equals("people")
                || label.equals("downloads") || label.equals("my services")
                || label.equals("random lists")) return false;
        if (label.equals("discover")) return trail.size() <= 4;
        if ("navigator.main".equals(mode)) {
            return action.equals("RootList") || action.equals("MovieList")
                    || action.equals("TVShowList") || trail.size() <= 2;
        }
        if (mode.equals("navigator.discover") || mode.equals("navigator.movies")
                || mode.equals("navigator.tvshows")) return true;
        if (mode.equals("navigator.my_lists") || mode.equals("navigator.trakt_watchlists")) return true;
        return false;
    }

    private static long ensureChannel(Context c, Section s) {
        long id = findChannel(c, s.key, s.title);
        ContentValues v = new ContentValues();
        v.put("type", "TYPE_PREVIEW");
        v.put("display_name", s.title);
        v.put("internal_provider_id", s.key);
        v.put("app_link_intent_uri", homeIntent(c).toUri(0));
        try {
            if (id > 0) {
                c.getContentResolver().update(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, id), v, null, null);
                return id;
            }
            Uri inserted = c.getContentResolver().insert(TvContract.Channels.CONTENT_URI, v);
            if (inserted == null) return -1;
            id = ContentUris.parseId(inserted);
            try { TvContract.requestChannelBrowsable(c, id); } catch (Throwable ignored) {}
            return id;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07 channel provider error key=" + s.key, t);
            return -1;
        }
    }

    private static long findChannel(Context c, String key, String title) {
        long titleMatch = -1;
        try (Cursor cur = c.getContentResolver().query(TvContract.Channels.CONTENT_URI,
                new String[]{"_id", "internal_provider_id", "display_name"}, null, null, null)) {
            if (cur == null) return -1;
            while (cur.moveToNext()) {
                long id = cur.getLong(0);
                String existingKey = cur.isNull(1) ? "" : cur.getString(1);
                String existingTitle = cur.isNull(2) ? "" : cur.getString(2);
                if (key.equals(existingKey)) return id;
                if (titleMatch <= 0 && title.equals(existingTitle)) titleMatch = id;
            }
        } catch (Throwable ignored) {}
        return titleMatch;
    }

    private static ProgramStats syncPrograms(Context c, long channel, Section section, ArtworkCache art) {
        ProgramStats stats = new ProgramStats();
        List<Entry> entries = list(c, section.uri);
        if (entries == null) return stats; // Keep cached cards during a transient Kodi/provider failure.
        stats.available = true;
        deleteProgramsForChannel(c, channel);

        int n = 0;
        for (Entry e : entries) {
            if (n >= MAX_PROGRAMS) break;
            if (e.dir() && !raw(e.uri).contains("tmdb_id=")) continue;

            ContentValues v = new ContentValues();
            v.put("channel_id", channel);
            v.put("title", e.name);
            v.put("description", e.mediaType != null ? e.mediaType : "Red Light");
            v.put("type", programType(e));
            v.put("intent_uri", programIdentityIntent(c, raw(e.uri)).toUri(0));
            v.put("internal_provider_id", raw(e.uri));
            v.put("weight", MAX_PROGRAMS - n);
            v.put("poster_art_aspect_ratio", 5);
            String poster = art == null ? null : art.posterFor(e);
            if (poster != null && !poster.isEmpty()) {
                v.put("poster_art_uri", poster);
                stats.artwork++;
            }
            try {
                if (c.getContentResolver().insert(TvContract.PreviewPrograms.CONTENT_URI, v) != null) {
                    n++;
                    stats.inserted++;
                }
            } catch (Throwable t) {
                Log.i(TAG, "CLEAN07 preview insert failed key=" + section.key + " title=" + e.name);
            }
        }
        return stats;
    }

    private static void deleteProgramsForChannel(Context c, long channel) {
        int removed = 0;
        try (Cursor cur = c.getContentResolver().query(TvContract.PreviewPrograms.CONTENT_URI,
                new String[]{"_id", "channel_id"}, null, null, null)) {
            if (cur != null) while (cur.moveToNext()) {
                if (cur.getLong(1) != channel) continue;
                long id = cur.getLong(0);
                try {
                    removed += c.getContentResolver().delete(
                            ContentUris.withAppendedId(TvContract.PreviewPrograms.CONTENT_URI, id), null, null);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            Log.i(TAG, "CLEAN07 preview cleanup unavailable channelId=" + channel);
        }
        Log.i(TAG, "CLEAN07 PROGRAM CLEANUP channelId=" + channel + " removed=" + removed);
    }

    private static Intent homeIntent(Context c) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(new ComponentName(c.getPackageName(), "com.spocky.projengmenu.ui.home.MainActivity"));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }

    /** Unique but inert identity: CLEAN 07 still does not implement playback. */
    private static Intent programIdentityIntent(Context c, String pluginUri) {
        Intent i = homeIntent(c);
        Uri data = new Uri.Builder().scheme("projectivy-clean07").authority("redlight")
                .appendQueryParameter("target", pluginUri == null ? "" : pluginUri).build();
        i.setData(data);
        return i;
    }

    private static int programType(Entry e) {
        String m = e.mediaType == null ? "" : e.mediaType.toLowerCase(Locale.ROOT);
        if ("movie".equals(m)) return 0;
        if ("tvshow".equals(m)) return 1;
        if ("episode".equals(m)) return 2;
        return 5;
    }

    private static List<Entry> list(Context c, String kodiUri) {
        Uri u = new Uri.Builder().scheme("content").authority(AUTH).fragment(kodiUri).build();
        try (Cursor cur = c.getContentResolver().query(u, null, null, null, null)) {
            if (cur == null) return null;
            int name = cur.getColumnIndex("name"), cat = cur.getColumnIndex("category"),
                    uri = cur.getColumnIndex("uri"), mt = cur.getColumnIndex("mediatype");
            ArrayList<Entry> out = new ArrayList<>();
            while (cur.moveToNext()) {
                String x = uri >= 0 && !cur.isNull(uri) ? cur.getString(uri) : null;
                if (x == null || x.trim().isEmpty()) continue;
                Entry e = new Entry();
                e.uri = raw(x);
                e.name = name >= 0 && !cur.isNull(name) ? cur.getString(name) : e.uri;
                e.category = cat >= 0 && !cur.isNull(cat) ? cur.getString(cat) : null;
                e.mediaType = mt >= 0 && !cur.isNull(mt) ? cur.getString(mt) : null;
                out.add(e);
            }
            return out;
        } catch (Throwable t) {
            Log.i(TAG, "CLEAN07 Kodi provider query unavailable: " + kodiUri);
            return null;
        }
    }

    static final class ArtworkCache {
        SQLiteDatabase db;

        static ArtworkCache open(Context c) {
            try {
                File dir = new File(c.getCacheDir(), "redlight_meta");
                dir.mkdirs();
                copyKodiFile(c, "special://profile/addon_data/plugin.video.redlight/databases/metacache.db", new File(dir, "metacache.db"));
                copyKodiFile(c, "special://profile/addon_data/plugin.video.redlight/databases/metacache.db-wal", new File(dir, "metacache.db-wal"));
                copyKodiFile(c, "special://profile/addon_data/plugin.video.redlight/databases/metacache.db-shm", new File(dir, "metacache.db-shm"));
                File main = new File(dir, "metacache.db");
                if (!main.isFile() || main.length() == 0) return null;
                ArtworkCache a = new ArtworkCache();
                a.db = SQLiteDatabase.openDatabase(main.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                return a;
            } catch (Throwable t) {
                Log.i(TAG, "CLEAN07 artwork cache unavailable");
                return null;
            }
        }

        String posterFor(Entry e) {
            try {
                Uri u = Uri.parse(raw(e.uri));
                String tmdb = q(u, "tmdb_id");
                if (tmdb.isEmpty()) tmdb = q(u, "meta");
                if (tmdb.isEmpty()) return null;
                String media = q(u, "media_type");
                String dbType = ("movie".equals(media) || "movie".equalsIgnoreCase(e.mediaType)) ? "movie" : "tvshow";
                try (Cursor c = db.query("metadata", new String[]{"meta"}, "db_type=? AND tmdb_id=?",
                        new String[]{dbType, tmdb}, null, null, null, "1")) {
                    if (c == null || !c.moveToFirst()) return null;
                    JSONObject meta = new JSONObject(c.getString(0));
                    String[] keys = {"poster", "landscape", "fanart", "rpdb_poster"};
                    for (String key : keys) {
                        String v = validArtwork(meta.optString(key, ""));
                        if (!v.isEmpty()) return v;
                    }
                }
            } catch (Throwable ignored) {}
            return null;
        }

        void close() {
            try { if (db != null) db.close(); } catch (Throwable ignored) {}
        }
    }

    private static String validArtwork(String value) {
        if (value == null) return "";
        String v = value.trim();
        if (v.isEmpty() || v.contains("%s")) return "";
        return (v.startsWith("http://") || v.startsWith("https://")) ? v : "";
    }

    private static boolean copyKodiFile(Context c, String path, File out) {
        Uri u = new Uri.Builder().scheme("content").authority(AUTH).fragment(path).build();
        try (InputStream in = c.getContentResolver().openInputStream(u)) {
            if (in == null) return false;
            File tmp = new File(out.getAbsolutePath() + ".tmp");
            try (FileOutputStream fo = new FileOutputStream(tmp)) {
                byte[] b = new byte[32768];
                for (int n; (n = in.read(b)) >= 0;) fo.write(b, 0, n);
            }
            if (out.exists()) out.delete();
            return tmp.renameTo(out);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String raw(String value) {
        if (value == null) return "";
        try {
            Uri u = Uri.parse(value);
            if ("content".equalsIgnoreCase(u.getScheme()) && AUTH.equals(u.getAuthority()) && u.getFragment() != null)
                return u.getFragment();
        } catch (Throwable ignored) {}
        return value;
    }

    private static String q(Uri u, String k) {
        String v = u == null ? null : u.getQueryParameter(k);
        return v == null ? "" : v;
    }

    private static String stripMarkup(String s) {
        if (s == null) return "";
        return s.replaceAll("\\[[^]]*]", "").trim();
    }

    private static String normalizeToken(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String norm(String s) {
        return stripMarkup(s).toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static List<String> single(String s) {
        ArrayList<String> a = new ArrayList<>();
        a.add(s == null ? "" : s);
        return a;
    }

    private static String join(List<String> x) {
        StringBuilder b = new StringBuilder();
        for (String s : x) {
            if (b.length() > 0) b.append('/');
            b.append(s);
        }
        return b.toString();
    }
}
