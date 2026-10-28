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
 * CLEAN 07: discover the real Red Light 2.9.2 Movies/TV shelves and publish
 * them as independent Android-TV preview channels. Playback remains disabled.
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
    private static final int MIN_COMPLETE_SECTIONS = 30;

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
        String key, title, uri;
        Section(String key, String title, String uri) {
            this.key = key;
            this.title = title;
            this.uri = uri;
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
            Log.i(TAG, "CLEAN07 DISCOVERED sections=" + sections.size());
            if (sections.size() < MIN_COMPLETE_SECTIONS) {
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
            return channels >= MIN_COMPLETE_SECTIONS;
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
        String label = norm(e.name);
        String path = norm(join(trail));
        boolean anime = "true".equalsIgnoreCase(q(u, "is_anime_list")) || path.contains("anime");
        if (anime) return null;

        if ("build_recently_watched_episode".equals(mode) || label.equals("recently watched episodes"))
            return section("redlight:episodes:recently_watched", "Recently Watched Episodes", e);
        if ("build_in_progress_episode".equals(mode) || label.equals("in progress episodes"))
            return section("redlight:episodes:in_progress", "In Progress Episodes", e);
        if ("build_next_episode".equals(mode) || label.equals("next episodes"))
            return section("redlight:episodes:next", "Next Episodes", e);
        if ("build_continue_watching".equals(mode) || label.equals("continue watching"))
            return section("redlight:episodes:continue_watching", "Continue Watching", e);

        String media = inferMedia(e, trail, u, mode);
        if ("trakt_watchlist".equals(action)) {
            if ("movies".equals(media)) return section("redlight:trakt:watchlist:movies", "Movies Watchlist", e);
            if ("shows".equals(media)) return section("redlight:trakt:watchlist:shows", "TV Shows Watchlist", e);
        }

        if (!"movies".equals(media) && !"shows".equals(media)) return null;
        String semantic = semanticFromAction(media, action);
        if (semantic == null) semantic = semanticFromLabel(media, label);
        if (semantic == null) return null;

        return section(keyFor(media, semantic), titleFor(media, semantic), e);
    }

    private static String semanticFromAction(String media, String action) {
        if ("movies".equals(media)) {
            if ("trakt_movies_trending".equals(action)) return "trending";
            if ("trakt_movies_trending_recent".equals(action)) return "trending_recent";
            if ("tmdb_movies_popular".equals(action)) return "popular";
            if ("tmdb_movies_popular_today".equals(action)) return "popular_today";
            if ("tmdb_movies_premieres".equals(action)) return "premieres";
            if ("tmdb_movies_latest_releases".equals(action)) return "latest_releases";
            if ("movies_most_watched".equals(action)) return "most_watched";
            if ("trakt_movies_most_favorited".equals(action)) return "most_favorited";
            if ("trakt_movies_top10_boxoffice".equals(action)) return "top10_boxoffice";
            if ("tmdb_movies_blockbusters".equals(action)) return "blockbusters";
            if ("tmdb_movies_in_theaters".equals(action)) return "in_theaters";
            if ("tmdb_movies_upcoming".equals(action)) return "upcoming";
            if ("tmdb_movies_oscar_winners".equals(action)) return "oscar_winners";
            if ("watched_movies".equals(action)) return "watched";
            if ("recent_watched_movies".equals(action)) return "recently_watched";
            if ("in_progress_movies".equals(action)) return "in_progress";
        } else if ("shows".equals(media)) {
            if ("trakt_tv_trending".equals(action)) return "trending";
            if ("trakt_tv_trending_recent".equals(action)) return "trending_recent";
            if ("tmdb_tv_popular".equals(action)) return "popular";
            if ("tmdb_tv_popular_today".equals(action)) return "popular_today";
            if ("tmdb_tv_premieres".equals(action)) return "premieres";
            if ("tv_most_watched".equals(action)) return "most_watched";
            if ("trakt_tv_most_favorited".equals(action)) return "most_favorited";
            if ("tmdb_tv_airing_today".equals(action)) return "airing_today";
            if ("tmdb_tv_on_the_air".equals(action)) return "on_the_air";
            if ("tmdb_tv_upcoming".equals(action)) return "upcoming";
            if ("watched_tvshows".equals(action)) return "watched";
            if ("recent_watched_tvshows".equals(action)) return "recently_watched";
            if ("in_progress_tvshows".equals(action)) return "in_progress";
        }
        return null;
    }

    private static String semanticFromLabel(String media, String label) {
        if ("trending".equals(label)) return "trending";
        if ("trending recent".equals(label)) return "trending_recent";
        if ("popular".equals(label)) return "popular";
        if ("popular today".equals(label)) return "popular_today";
        if ("premieres".equals(label) || "premiere".equals(label)) return "premieres";
        if ("most watched".equals(label)) return "most_watched";
        if ("most favorited".equals(label)) return "most_favorited";
        if ("up coming".equals(label) || "upcoming".equals(label)) return "upcoming";
        if ("watched".equals(label)) return "watched";
        if ("recently watched".equals(label)) return "recently_watched";
        if ("in progress".equals(label)) return "in_progress";

        if ("movies".equals(media)) {
            if ("latest releases".equals(label)) return "latest_releases";
            if ("top 10 box office".equals(label) || "top10 box office".equals(label)) return "top10_boxoffice";
            if ("blockbusters".equals(label)) return "blockbusters";
            if ("in theaters".equals(label) || "in theatres".equals(label)) return "in_theaters";
            if ("oscar winners".equals(label)) return "oscar_winners";
        } else if ("shows".equals(media)) {
            if ("airing today".equals(label)) return "airing_today";
            if ("on the air".equals(label)) return "on_the_air";
        }
        return null;
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

    private static String keyFor(String media, String semantic) {
        return "redlight:shelf:" + semantic + ":" + media;
    }

    private static String titleFor(String media, String semantic) {
        String base;
        if ("trending".equals(semantic)) base = "Trending";
        else if ("trending_recent".equals(semantic)) base = "Trending Recent";
        else if ("popular".equals(semantic)) base = "Popular";
        else if ("popular_today".equals(semantic)) base = "Popular Today";
        else if ("premieres".equals(semantic)) base = "Premieres";
        else if ("latest_releases".equals(semantic)) base = "Latest Releases";
        else if ("most_watched".equals(semantic)) base = "Most Watched";
        else if ("most_favorited".equals(semantic)) base = "Most Favorited";
        else if ("top10_boxoffice".equals(semantic)) base = "Top 10 Box Office";
        else if ("blockbusters".equals(semantic)) base = "Blockbusters";
        else if ("in_theaters".equals(semantic)) base = "In Theaters";
        else if ("upcoming".equals(semantic)) base = "Up Coming";
        else if ("oscar_winners".equals(semantic)) base = "Oscar Winners";
        else if ("watched".equals(semantic)) base = "Watched";
        else if ("recently_watched".equals(semantic)) base = "Recently Watched";
        else if ("in_progress".equals(semantic)) {
            return "movies".equals(media) ? "Movies In Progress" : "TV Shows In Progress";
        }
        else if ("airing_today".equals(semantic)) base = "Airing Today";
        else if ("on_the_air".equals(semantic)) base = "On the Air";
        else base = semantic;
        return base + ("movies".equals(media) ? " Movies" : " TV Shows");
    }

    private static Section section(String key, String title, Entry e) {
        return new Section(key, "Red Light · " + title, raw(e.uri));
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
        if (entries == null) return stats; // Preserve existing cards on a transient Kodi/provider failure.
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

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT)
                .replaceAll("\\[[^]]*]", "")
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
