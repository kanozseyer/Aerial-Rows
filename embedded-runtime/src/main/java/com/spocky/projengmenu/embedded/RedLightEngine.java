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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RedLightEngine {
    private static final String KODI_PACKAGE = "org.xbmc.kodi";
    private static final String KODI_FILE_AUTHORITY = "org.xbmc.kodi.file";
    private static final String VIDEO_ADDONS = "addons://sources/video/";
    private static final String PREF_FILE = "embedded_redlight_channels";
    private static final int MAX_DEPTH = 7;
    private static final int MAX_SCANS = 72;
    private static final int MAX_CHILDREN = 100;
    private static final int MAX_PROGRAMS = 60;

    static final class Entry {
        String name, category, uri, mediaType;
        boolean dir() {
            return "directory".equalsIgnoreCase(category) || "folder".equalsIgnoreCase(category)
                    || "directory".equalsIgnoreCase(mediaType);
        }
    }
    static final class Node {
        Entry entry; List<String> trail; int depth;
        Node(Entry e, List<String> t, int d) { entry=e; trail=t; depth=d; }
    }
    static final class Section {
        String key, title, uri;
        Section(String k, String t, String u) { key=k; title=t; uri=u; }
    }
    static final class ChannelState {
        String key, title, uri; long channelId; int missing;
    }

    static void sync(Context context) {
        if (!isKodiInstalled(context)) return;
        Entry root = findRedLight(context);
        if (root == null) return;
        List<Entry> first = list(context, rawUri(root.uri));
        if (first == null) return;

        LinkedHashMap<String, Section> found = new LinkedHashMap<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        for (Entry e : first) if (e.dir()) queue.add(new Node(e, singleton(e.name), 1));
        Set<String> visited = new HashSet<>();
        int scans = 0;
        boolean complete = true;

        while (!queue.isEmpty() && scans < MAX_SCANS) {
            Node n = queue.removeFirst();
            String nativeUri = rawUri(n.entry.uri);
            if (!visited.add(nativeUri)) continue;
            Section s = classify(n.entry, n.trail);
            if (s != null) {
                if (!found.containsKey(s.key)) found.put(s.key, s);
                continue;
            }
            if (n.depth >= MAX_DEPTH || !shouldTraverse(n.entry, n.trail)) continue;
            scans++;
            List<Entry> children = list(context, nativeUri);
            if (children == null) { complete = false; continue; }
            int count = 0;
            for (Entry child : children) {
                if (!child.dir()) continue;
                if (++count > MAX_CHILDREN) break;
                ArrayList<String> trail = new ArrayList<>(n.trail); trail.add(child.name);
                queue.add(new Node(child, trail, n.depth + 1));
            }
        }
        if (!queue.isEmpty()) complete = false;

        Map<String, ChannelState> states = loadStates(context);
        Set<String> seen = new HashSet<>();
        ArtworkCache art = ArtworkCache.open(context);
        for (Section s : found.values()) {
            seen.add(s.key);
            ChannelState st = states.get(s.key);
            if (st == null) { st = new ChannelState(); st.key = s.key; st.channelId = 0; }
            st.title = s.title; st.uri = s.uri; st.missing = 0;
            st = ensureChannel(context, st);
            if (st != null) {
                syncPrograms(context, st, art);
                states.put(st.key, st);
            }
        }
        if (art != null) art.close();

        if (complete) {
            ArrayList<String> remove = new ArrayList<>();
            for (ChannelState st : states.values()) {
                if (seen.contains(st.key)) continue;
                st.missing++;
                if (st.missing >= 2) {
                    deleteChannel(context, st.channelId);
                    remove.add(st.key);
                }
            }
            for (String key : remove) states.remove(key);
        }
        saveStates(context, states);
    }

    private static boolean isKodiInstalled(Context c) {
        try { c.getPackageManager().getApplicationInfo(KODI_PACKAGE, 0); return true; }
        catch (Throwable ignored) { return false; }
    }

    private static Entry findRedLight(Context c) {
        List<Entry> addons = list(c, VIDEO_ADDONS);
        if (addons == null) return null;
        for (Entry e : addons) if ("red light".equals(norm(e.name))) return e;
        for (Entry e : addons) if (norm(e.name).contains("red light")) return e;
        return null;
    }

    private static List<Entry> list(Context c, String kodiUri) {
        Uri q = new Uri.Builder().scheme("content").authority(KODI_FILE_AUTHORITY).fragment(kodiUri).build();
        try (Cursor cursor = c.getContentResolver().query(q, null, null, null, null)) {
            if (cursor == null) return null;
            int name = cursor.getColumnIndex("name"), cat = cursor.getColumnIndex("category"), uri = cursor.getColumnIndex("uri"), mt = cursor.getColumnIndex("mediatype");
            ArrayList<Entry> out = new ArrayList<>();
            while (cursor.moveToNext()) {
                String u = uri >= 0 && !cursor.isNull(uri) ? cursor.getString(uri) : null;
                if (u == null || u.trim().isEmpty()) continue;
                Entry e = new Entry();
                e.uri = rawUri(u);
                e.name = name >= 0 && !cursor.isNull(name) ? cursor.getString(name) : e.uri;
                e.category = cat >= 0 && !cursor.isNull(cat) ? cursor.getString(cat) : null;
                e.mediaType = mt >= 0 && !cursor.isNull(mt) ? cursor.getString(mt) : null;
                out.add(e);
            }
            return out;
        } catch (Throwable ignored) { return null; }
    }

    private static String rawUri(String value) {
        if (value == null) return "";
        try {
            Uri u = Uri.parse(value);
            if ("content".equalsIgnoreCase(u.getScheme()) && KODI_FILE_AUTHORITY.equals(u.getAuthority())) {
                String f = u.getFragment(); if (f != null) return f;
            }
        } catch (Throwable ignored) {}
        return value;
    }

    private static Section classify(Entry e, List<String> trail) {
        Uri u;
        try { u = Uri.parse(rawUri(e.uri)); } catch (Throwable t) { return null; }
        String mode = q(u,"mode"), action=q(u,"action"), sort=q(u,"nextep_sort");
        boolean anime = "true".equalsIgnoreCase(q(u,"is_anime_list"));
        String label = norm(e.name);
        String media = inferMedia(e, trail, u, mode, anime);
        String route = norm(mode + " " + action + " " + q(u,"type") + " " + q(u,"list_type"));

        if ("build_continue_watching".equals(mode) || "continue watching".equals(label))
            return sec("continue_watching","Continue Watching",e);
        if ("build_next_episode".equals(mode) || label.startsWith("next episodes") || label.startsWith("anime next episodes")) {
            String suffix = anime ? "anime" : "last_played".equals(sort) ? "recently_watched" : "first_aired".equals(sort) ? "airdate" : "default";
            return sec("next_episodes:"+suffix, e.name, e);
        }
        if ("build_in_progress_episode".equals(mode) || label.contains("in progress episodes"))
            return sec(anime?"in_progress_episodes:anime":"in_progress_episodes", e.name,e);
        if ("build_recently_watched_episode".equals(mode) || label.contains("recently watched episodes"))
            return sec(anime?"recently_watched_episodes:anime":"recently_watched_episodes",e.name,e);
        if ("recent_watched_movies".equals(action)) return sec("recently_watched:movies","Recently Watched Movies",e);
        if ("recent_watched_tvshows".equals(action) && !anime) return sec("recently_watched:shows","Recently Watched TV Shows",e);
        if ("in_progress_movies".equals(action)) return sec("in_progress:movies","Movies In Progress",e);
        if ("in_progress_tvshows".equals(action) && !anime) return sec("in_progress:shows","TV Shows In Progress",e);

        if (!media.isEmpty()) {
            if ("trakt_watchlist".equals(action)) return sec("trakt:watchlist:"+media,e.name,e);
            if ("trakt_recommendations".equals(action)) return sec("trakt:recommended:"+media,e.name,e);
            if ("trakt_collection".equals(action)) return sec("trakt:library:"+media,e.name,e);
            if ("trakt_favorites".equals(action)) return sec("trakt:favorites:"+media,e.name,e);

            if (isShelf(label, route, "trending"))
                return sec("shelf:trending:"+media, shelfTitle("Trending", media, e.name), e);
            if (isShelf(label, route, "popular"))
                return sec("shelf:popular:"+media, shelfTitle("Popular", media, e.name), e);
            if (isShelf(label, route, "anticipated"))
                return sec("shelf:anticipated:"+media, shelfTitle("Most Anticipated", media, e.name), e);
            if (isShelf(label, route, "top rated") || isShelf(label, route, "top_rated"))
                return sec("shelf:top_rated:"+media, shelfTitle("Top Rated", media, e.name), e);
            if (isShelf(label, route, "premiere") || isShelf(label, route, "premieres"))
                return sec("shelf:premieres:"+media, shelfTitle("Premieres", media, e.name), e);
            if (isShelf(label, route, "recently added") || isShelf(label, route, "recent_added") || isShelf(label, route, "recently_added"))
                return sec("shelf:recently_added:"+media, shelfTitle("Recently Added", media, e.name), e);
            if (isShelf(label, route, "new releases") || isShelf(label, route, "new_releases"))
                return sec("shelf:new_releases:"+media, shelfTitle("New Releases", media, e.name), e);
            if (isShelf(label, route, "most watched") || isShelf(label, route, "most_watched"))
                return sec("shelf:most_watched:"+media, shelfTitle("Most Watched", media, e.name), e);
            if (isShelf(label, route, "box office") || isShelf(label, route, "box_office"))
                return sec("shelf:box_office:"+media, shelfTitle("Box Office", media, e.name), e);
        }

        if ("build_my_calendar".equals(mode) || "calendar".equals(label)) return sec("trakt:calendar","Trakt Calendar",e);
        if ("trakt.list.build_trakt_list".equals(mode)) {
            String id=q(u,"list_id"), user=q(u,"user"), slug=q(u,"slug");
            String identity=!id.isEmpty()?"id:"+id:(!user.isEmpty()||!slug.isEmpty())?"slug:"+norm(user)+":"+norm(slug):"menu:"+shortHash(join(trail));
            return sec("trakt:list:"+identity,e.name,e);
        }
        if ("movies watchlist".equals(label)) return sec("trakt:watchlist:movies","Movies Watchlist",e);
        if ("tv shows watchlist".equals(label) || "tv show watchlist".equals(label)) return sec("trakt:watchlist:shows","TV Shows Watchlist",e);
        if (label.contains("recommended movies")) return sec("trakt:recommended:movies","Recommended Movies",e);
        if (label.contains("recommended tv")) return sec("trakt:recommended:shows","Recommended TV Shows",e);
        return null;
    }

    private static String inferMedia(Entry e, List<String> trail, Uri u, String mode, boolean anime) {
        if ("build_movie_list".equals(mode)) return "movies";
        if ("build_tvshow_list".equals(mode)) return anime ? "anime" : "shows";
        String param = norm(q(u,"media_type") + " " + q(u,"media") + " " + q(u,"content_type"));
        if (param.contains("movie")) return "movies";
        if (param.contains("anime")) return "anime";
        if (param.contains("tvshow") || param.contains("tv show") || param.contains("series")) return "shows";
        String path = norm(join(trail));
        if (path.contains("anime")) return "anime";
        if (path.contains("movie")) return "movies";
        if (path.contains("tv show") || path.contains("series")) return "shows";
        String mt = norm(e.mediaType);
        if (mt.equals("movie")) return "movies";
        if (mt.equals("tvshow") || mt.equals("show")) return "shows";
        return "";
    }

    private static boolean isShelf(String label, String route, String token) {
        String n = norm(token);
        return label.equals(n) || label.startsWith(n + " ") || label.endsWith(" " + n)
                || label.contains(" " + n + " ") || route.contains(n);
    }

    private static String shelfTitle(String base, String media, String original) {
        String clean = original == null ? "" : original.replaceAll("\\[[^]]*]", "").trim();
        String n = norm(clean);
        if (!n.equals(norm(base)) && !clean.isEmpty()) return clean;
        if ("movies".equals(media)) return base + " Movies";
        if ("shows".equals(media)) return base + " TV Shows";
        if ("anime".equals(media)) return base + " Anime";
        return base;
    }

    private static Section sec(String key,String title,Entry e) { return new Section("redlight:"+key,"Red Light · "+title.trim(),rawUri(e.uri)); }
    private static boolean shouldTraverse(Entry e,List<String> trail) {
        Uri u; try {u=Uri.parse(rawUri(e.uri));}catch(Throwable t){return false;}
        String mode=q(u,"mode"), action=q(u,"action"), label=norm(e.name);
        if (label.equals("tools")||label.equals("search")||label.equals("people")||label.equals("downloads")||label.equals("my services")||label.equals("random lists")) return false;
        if (label.equals("discover")) return trail.size() <= 4;
        if ("navigator.main".equals(mode)) return action.equals("RootList")||action.equals("MovieList")||action.equals("TVShowList")||action.equals("AnimeList")||trail.size()<=2;
        if (mode.equals("navigator.discover") || mode.equals("navigator.movies") || mode.equals("navigator.tvshows")) return true;
        if (mode.equals("navigator.my_lists")||mode.equals("navigator.trakt_lists_personal")||mode.equals("navigator.trakt_watchlists")||mode.equals("navigator.trakt_collections")||mode.equals("navigator.trakt_favorites")||mode.equals("navigator.trakt_recommendations")||mode.equals("navigator.favorites")) return true;
        return mode.equals("trakt.list.get_trakt_lists") && "my_lists".equals(q(u,"list_type"));
    }

    private static ChannelState ensureChannel(Context c, ChannelState st) {
        try {
            if (st.channelId > 0 && channelExists(c, st.channelId)) {
                ContentValues up = new ContentValues();
                up.put("display_name",st.title);
                up.put("app_link_intent_uri",projectivyForwardIntent(c, st.uri).toUri(0));
                c.getContentResolver().update(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, st.channelId),up,null,null);
                return st;
            }
            ContentValues v = new ContentValues();
            v.put("type","TYPE_PREVIEW");
            v.put("display_name",st.title);
            v.put("internal_provider_id",st.key);
            v.put("app_link_intent_uri",projectivyForwardIntent(c, st.uri).toUri(0));
            Uri inserted = c.getContentResolver().insert(TvContract.Channels.CONTENT_URI,v);
            if (inserted == null) return null;
            st.channelId=ContentUris.parseId(inserted);
            try { TvContract.requestChannelBrowsable(c,st.channelId); } catch(Throwable ignored) {}
            return st;
        } catch(Throwable ignored) { return null; }
    }

    private static boolean channelExists(Context c,long id) {
        try(Cursor cur=c.getContentResolver().query(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI,id),new String[]{"_id"},null,null,null)) { return cur!=null&&cur.moveToFirst(); }
        catch(Throwable ignored){return false;}
    }

    private static void syncPrograms(Context c, ChannelState st, ArtworkCache art) {
        List<Entry> entries=list(c,st.uri); if(entries==null) return;
        Uri programs=TvContract.PreviewPrograms.CONTENT_URI;
        try { c.getContentResolver().delete(programs,"channel_id=?",new String[]{String.valueOf(st.channelId)}); } catch(Throwable ignored) {}
        int n=0;
        for(Entry e:entries) {
            if(n>=MAX_PROGRAMS) break;
            if(e.dir() && !rawUri(e.uri).contains("tmdb_id=")) continue;
            ContentValues v=new ContentValues();
            v.put("channel_id",st.channelId);
            v.put("title",e.name);
            v.put("description",e.mediaType!=null?e.mediaType:"Red Light");
            v.put("type",programType(e));
            v.put("intent_uri",projectivyForwardIntent(c, rawUri(e.uri)).toUri(0));
            v.put("internal_provider_id",rawUri(e.uri));
            v.put("weight",MAX_PROGRAMS-n);
            v.put("poster_art_aspect_ratio",5);
            String poster=art==null?null:art.posterFor(e);
            if(poster!=null&&!poster.isEmpty()) v.put("poster_art_uri",poster);
            try { c.getContentResolver().insert(programs,v); } catch(Throwable ignored) {}
            n++;
        }
    }

    private static int programType(Entry e) {
        String m=e.mediaType==null?"":e.mediaType.toLowerCase(Locale.ROOT);
        if("movie".equals(m)) return 0;
        if("tvshow".equals(m)) return 1;
        if("episode".equals(m)) return 2;
        return 5;
    }

    private static Intent projectivyForwardIntent(Context c, String uri) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(new ComponentName(c.getPackageName(), RedLightLaunchActivity.class.getName()));
        if(uri!=null&&!uri.isEmpty()) {
            i.setData(Uri.parse(uri));
            i.putExtra(RedLightLaunchActivity.EXTRA_TARGET, uri);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }

    private static void deleteChannel(Context c,long id) {
        if(id<=0)return; try {c.getContentResolver().delete(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI,id),null,null);}catch(Throwable ignored){}
    }

    private static Map<String,ChannelState> loadStates(Context c) {
        LinkedHashMap<String,ChannelState> out=new LinkedHashMap<>();
        try {
            String s=c.getSharedPreferences(PREF_FILE,0).getString("channels","[]"); JSONArray a=new JSONArray(s);
            for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);ChannelState st=new ChannelState();st.key=o.getString("key");st.title=o.optString("title");st.uri=o.optString("uri");st.channelId=o.optLong("channelId");st.missing=o.optInt("missing");out.put(st.key,st);}
        }catch(Throwable ignored){}
        return out;
    }
    private static void saveStates(Context c,Map<String,ChannelState> states) {
        JSONArray a=new JSONArray();try{for(ChannelState st:states.values()){JSONObject o=new JSONObject();o.put("key",st.key);o.put("title",st.title);o.put("uri",st.uri);o.put("channelId",st.channelId);o.put("missing",st.missing);a.put(o);}}catch(Throwable ignored){}
        c.getSharedPreferences(PREF_FILE,0).edit().putString("channels",a.toString()).apply();
    }

    static final class ArtworkCache {
        SQLiteDatabase db; File dir;
        static ArtworkCache open(Context c) {
            try {
                File dir=new File(c.getCacheDir(),"redlight_meta"); dir.mkdirs();
                copyKodiFile(c,"special://profile/addon_data/plugin.video.redlight/databases/metacache.db",new File(dir,"metacache.db"));
                copyKodiFile(c,"special://profile/addon_data/plugin.video.redlight/databases/metacache.db-wal",new File(dir,"metacache.db-wal"));
                copyKodiFile(c,"special://profile/addon_data/plugin.video.redlight/databases/metacache.db-shm",new File(dir,"metacache.db-shm"));
                File main=new File(dir,"metacache.db"); if(!main.isFile()||main.length()==0)return null;
                ArtworkCache a=new ArtworkCache();a.dir=dir;a.db=SQLiteDatabase.openDatabase(main.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);return a;
            }catch(Throwable ignored){return null;}
        }
        String posterFor(Entry e) {
            try {
                Uri u=Uri.parse(rawUri(e.uri));String tmdb=q(u,"tmdb_id"); if(tmdb.isEmpty()) tmdb=q(u,"meta"); if(tmdb.isEmpty())return null;
                String media=q(u,"media_type");String dbType=("movie".equals(media)||"movie".equalsIgnoreCase(e.mediaType))?"movie":"tvshow";
                try(Cursor c=db.query("metadata",new String[]{"meta"},"db_type=? AND tmdb_id=?",new String[]{dbType,tmdb},null,null,null,"1")) {
                    if(c==null||!c.moveToFirst())return null;JSONObject meta=new JSONObject(c.getString(0));
                    String[] keys={"poster","landscape","fanart","rpdb_poster"};
                    for(String key:keys){
                        String v=validArtwork(meta.optString(key,""));
                        if(!v.isEmpty())return v;
                    }
                }
            }catch(Throwable ignored){}
            return null;
        }
        private static String validArtwork(String value) {
            if(value==null)return "";
            String v=value.trim();
            if(v.isEmpty()||v.contains("%s"))return "";
            return (v.startsWith("http://")||v.startsWith("https://"))?v:"";
        }
        void close(){try{if(db!=null)db.close();}catch(Throwable ignored){}}
    }

    private static boolean copyKodiFile(Context c,String path,File out) {
        Uri u=new Uri.Builder().scheme("content").authority(KODI_FILE_AUTHORITY).fragment(path).build();
        try(InputStream in=c.getContentResolver().openInputStream(u)) {
            if(in==null)return false;File tmp=new File(out.getAbsolutePath()+".tmp");try(FileOutputStream fo=new FileOutputStream(tmp)){byte[] b=new byte[32768];for(int n;(n=in.read(b))>=0;)fo.write(b,0,n);}if(out.exists())out.delete();return tmp.renameTo(out);
        }catch(Throwable ignored){return false;}
    }

    private static String q(Uri u,String key){String v=u==null?null:u.getQueryParameter(key);return v==null?"":v;}
    private static String norm(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replaceAll("\\[[^]]*]","").replace('&',' ').replaceAll("[^a-z0-9]+"," ").trim();}
    private static List<String> singleton(String s){ArrayList<String>a=new ArrayList<>();a.add(s==null?"":s);return a;}
    private static String join(List<String> list){StringBuilder b=new StringBuilder();for(String s:list){if(b.length()>0)b.append('/');b.append(s);}return b.toString();}
    private static String shortHash(String value){try{byte[]d=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(int i=0;i<8;i++)b.append(String.format(Locale.US,"%02x",d[i]&0xff));return b.toString();}catch(Throwable t){return Integer.toHexString(value.hashCode());}}
}
