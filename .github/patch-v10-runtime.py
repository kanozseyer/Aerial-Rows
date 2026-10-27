from pathlib import Path
import json

# v10 is deliberately a low-risk efficiency pass. It does not alter Red Light route
# semantics or Kodi navigation. It removes redundant database/network/cache work.

profile = Path('embedded-runtime/src/main/assets/launcher_default_profile.json')
root = json.loads(profile.read_text())
for key in list(root.keys()):
    if key.startswith('launcher_embedded_'):
        root.pop(key, None)
profile.write_text(json.dumps(root, separators=(',', ':'), ensure_ascii=False))
print('sanitized internal embedded markers from bundled profile')

# ---------------------------------------------------------------------------
# Red Light: retain a fingerprint per channel and do not delete/reinsert up to
# 60 PreviewPrograms when the shelf content is unchanged.
# ---------------------------------------------------------------------------
engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()

old_state = '        String key, title, uri; long channelId; int missing;\n'
new_state = '        String key, title, uri, programHash; long channelId; int missing;\n'
if old_state not in text:
    raise SystemExit('ChannelState anchor missing')
text = text.replace(old_state, new_state, 1)

start = text.find('    private static void syncPrograms(Context c, ChannelState st, ArtworkCache art) {')
end = text.find('    private static int programType(Entry e) {', start)
if start < 0 or end < 0:
    raise SystemExit('syncPrograms region missing')
new_sync = r'''    private static void syncPrograms(Context c, ChannelState st, ArtworkCache art) {
        List<Entry> entries=list(c,st.uri); if(entries==null) return;
        ArrayList<Entry> selected = new ArrayList<>();
        ArrayList<String> posters = new ArrayList<>();
        StringBuilder signature = new StringBuilder();
        int n=0;
        for(Entry e:entries) {
            if(n>=MAX_PROGRAMS) break;
            String raw = rawUri(e.uri);
            if(e.dir() && !raw.contains("tmdb_id=")) continue;
            String poster=art==null?null:art.posterFor(e);
            selected.add(e);
            posters.add(poster==null?"":poster);
            signature.append(e.name==null?"":e.name).append('\u001f')
                    .append(raw).append('\u001f')
                    .append(e.mediaType==null?"":e.mediaType).append('\u001f')
                    .append(poster==null?"":poster).append('\u001e');
            n++;
        }

        String fingerprint = shortHash(signature.toString()) + ":" + selected.size();
        if (fingerprint.equals(st.programHash)) return;

        Uri programs=TvContract.PreviewPrograms.CONTENT_URI;
        try { c.getContentResolver().delete(programs,"channel_id=?",new String[]{String.valueOf(st.channelId)}); } catch(Throwable ignored) {}
        int inserted=0;
        for(int i=0;i<selected.size();i++) {
            Entry e=selected.get(i);
            ContentValues v=new ContentValues();
            v.put("channel_id",st.channelId);
            v.put("title",e.name);
            v.put("description",e.mediaType!=null?e.mediaType:"Red Light");
            v.put("type",programType(e));
            v.put("intent_uri",projectivyForwardIntent(c, rawUri(e.uri)).toUri(0));
            v.put("internal_provider_id",rawUri(e.uri));
            v.put("weight",MAX_PROGRAMS-i);
            v.put("poster_art_aspect_ratio",5);
            String poster=posters.get(i);
            if(!poster.isEmpty()) v.put("poster_art_uri",poster);
            try {
                Uri result=c.getContentResolver().insert(programs,v);
                if(result!=null) inserted++;
            } catch(Throwable ignored) {}
        }
        st.programHash = inserted == selected.size() ? fingerprint : "";
    }

'''
text = text[:start] + new_sync + text[end:]

old_load = 'st.key=o.getString("key");st.title=o.optString("title");st.uri=o.optString("uri");st.channelId=o.optLong("channelId");st.missing=o.optInt("missing");out.put(st.key,st);'
new_load = 'st.key=o.getString("key");st.title=o.optString("title");st.uri=o.optString("uri");st.channelId=o.optLong("channelId");st.missing=o.optInt("missing");st.programHash=o.optString("programHash","");out.put(st.key,st);'
if old_load not in text:
    raise SystemExit('loadStates anchor missing')
text = text.replace(old_load, new_load, 1)

old_save = 'o.put("key",st.key);o.put("title",st.title);o.put("uri",st.uri);o.put("channelId",st.channelId);o.put("missing",st.missing);a.put(o);'
new_save = 'o.put("key",st.key);o.put("title",st.title);o.put("uri",st.uri);o.put("channelId",st.channelId);o.put("missing",st.missing);o.put("programHash",st.programHash==null?"":st.programHash);a.put(o);'
if old_save not in text:
    raise SystemExit('saveStates anchor missing')
text = text.replace(old_save, new_save, 1)

# v9 writes the same PNG to every existing channel on every sync. Keep the write on
# channel creation, but do not repeat it for unchanged channels.
old_existing_logo = '                c.getContentResolver().update(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, st.channelId),up,null,null);\n                applyRedLightChannelLogo(c, st.channelId);\n                return st;'
new_existing_logo = '                c.getContentResolver().update(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, st.channelId),up,null,null);\n                return st;'
if old_existing_logo not in text:
    raise SystemExit('v9 existing channel logo anchor missing')
text = text.replace(old_existing_logo, new_existing_logo, 1)

# Cache the copied Kodi metadata database for ten minutes. The cache still refreshes
# frequently enough for new artwork while avoiding three file copies per sync.
old_db = '''                File dir=new File(c.getCacheDir(),"redlight_meta"); dir.mkdirs();\n                copyKodiFile(c,"special://profile/addon_data/plugin.video.redlight/databases/metacache.db",new File(dir,"metacache.db"));\n                copyKodiFile(c,"special://profile/addon_data/plugin.video.redlight/databases/metacache.db-wal",new File(dir,"metacache.db-wal"));\n                copyKodiFile(c,"special://profile/addon_data/plugin.video.redlight/databases/metacache.db-shm",new File(dir,"metacache.db-shm"));\n                File main=new File(dir,"metacache.db"); if(!main.isFile()||main.length()==0)return null;'''
new_db = '''                File dir=new File(c.getCacheDir(),"redlight_meta"); dir.mkdirs();\n                File main=new File(dir,"metacache.db");\n                boolean stale=!main.isFile() || main.length()==0 || System.currentTimeMillis()-main.lastModified()>10L*60L*1000L;\n                if(stale) {\n                    copyKodiFile(c,"special://profile/addon_data/plugin.video.redlight/databases/metacache.db",main);\n                    copyKodiFile(c,"special://profile/addon_data/plugin.video.redlight/databases/metacache.db-wal",new File(dir,"metacache.db-wal"));\n                    copyKodiFile(c,"special://profile/addon_data/plugin.video.redlight/databases/metacache.db-shm",new File(dir,"metacache.db-shm"));\n                }\n                if(!main.isFile()||main.length()==0)return null;'''
if old_db not in text:
    raise SystemExit('ArtworkCache database copy anchor missing')
text = text.replace(old_db, new_db, 1)
engine.write_text(text)
print('optimized Red Light program/database/logo sync work')

# ---------------------------------------------------------------------------
# Runtime: one throttled normal resume refresh. The 2.5s/6.5s retries are retained
# only while the initial bundled channel layout is still being materialized.
# ---------------------------------------------------------------------------
embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
text = embedded.read_text()
old_resume = '''        scheduleRedLight(app);\n        syncRedLightAsync(app);\n        h.postDelayed(() -> syncRedLightAsync(app), 2500L);\n        h.postDelayed(() -> syncRedLightAsync(app), 6500L);'''
new_resume = '''        scheduleRedLight(app);\n        boolean initialChannelRestore = !projectivyPrefs(app).getBoolean("launcher_embedded_channel_layout_v2", false);\n        syncRedLightAsyncThrottled(app, 10000L);\n        if (initialChannelRestore) {\n            h.postDelayed(() -> syncRedLightAsync(app), 2500L);\n            h.postDelayed(() -> syncRedLightAsync(app), 6500L);\n        }'''
if old_resume not in text:
    raise SystemExit('v7 resume retry anchor missing')
text = text.replace(old_resume, new_resume, 1)

schedule_anchor = '    static void scheduleRedLight(Context c) {\n'
throttle = r'''    static void syncRedLightAsyncThrottled(Context c, long minimumIntervalMs) {
        try {
            SharedPreferences p = c.getSharedPreferences("embedded_redlight_runtime", Context.MODE_PRIVATE);
            long now = System.currentTimeMillis();
            long last = p.getLong("last_sync_request", 0L);
            if (last > 0L && now - last < minimumIntervalMs) return;
            p.edit().putLong("last_sync_request", now).apply();
        } catch (Throwable ignored) {}
        syncRedLightAsync(c);
    }

'''
if schedule_anchor not in text:
    raise SystemExit('scheduleRedLight anchor missing')
text = text.replace(schedule_anchor, throttle + schedule_anchor, 1)
text = text.replace('.setPeriodic(30L * 60L * 1000L).setPersisted(true).build();',
                    '.setPeriodic(60L * 60L * 1000L).setPersisted(true).build();', 1)
embedded.write_text(text)
print('throttled normal Red Light refresh and reduced periodic job frequency')

# ---------------------------------------------------------------------------
# Aerial: cache the final Parcelable catalogue in process for six hours. Projectivy
# can ask the provider repeatedly; this prevents repeated JSON parsing + GitHub HTTP.
# ---------------------------------------------------------------------------
aerial = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/AerialService.java')
text = aerial.read_text()
field_anchor = 'public final class AerialService extends Service {\n    private final IBinder binder = new AerialBinder();\n'
field_replacement = '''public final class AerialService extends Service {\n    private static final long CATALOG_CACHE_MS = 6L * 60L * 60L * 1000L;\n    private static volatile ArrayList<Parcelable> cachedAerials;\n    private static volatile String cachedAerialParams;\n    private static volatile long cachedAerialAt;\n    private final IBinder binder = new AerialBinder();\n'''
if field_anchor not in text:
    raise SystemExit('Aerial cache field anchor missing')
text = text.replace(field_anchor, field_replacement, 1)

old_set = '''    private static void setParams(Context c, String p) {\n        EmbeddedRuntime.projectivyPrefs(c).edit()\n                .putString("appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID, p).apply();\n    }\n\n    private static ArrayList<Parcelable> loadAerials(Context c) {'''
new_set = '''    private static void setParams(Context c, String p) {\n        EmbeddedRuntime.projectivyPrefs(c).edit()\n                .putString("appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID, p).apply();\n        synchronized (AerialService.class) {\n            cachedAerials = null;\n            cachedAerialParams = null;\n            cachedAerialAt = 0L;\n        }\n    }\n\n    private static ArrayList<Parcelable> loadAerials(Context c) {\n        String params = getParams(c);\n        long now = System.currentTimeMillis();\n        ArrayList<Parcelable> cached = cachedAerials;\n        if (cached != null && params.equals(cachedAerialParams) && now - cachedAerialAt < CATALOG_CACHE_MS) {\n            return new ArrayList<>(cached);\n        }\n        ArrayList<Parcelable> loaded = loadAerialsUncached(c);\n        synchronized (AerialService.class) {\n            cachedAerials = new ArrayList<>(loaded);\n            cachedAerialParams = params;\n            cachedAerialAt = now;\n        }\n        return loaded;\n    }\n\n    private static ArrayList<Parcelable> loadAerialsUncached(Context c) {'''
if old_set not in text:
    raise SystemExit('Aerial load method anchor missing')
text = text.replace(old_set, new_set, 1)
aerial.write_text(text)
print('cached Aerial catalogue for six hours')
