from pathlib import Path

p = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
s = p.read_text()

s = s.replace('public final class RedLightEngine {', '''public final class RedLightEngine {
    private static final String DIAG = "ProjectivyRedLight";
    private static void d(String m) { android.util.Log.i(DIAG, m); }
    private static void e(String m, Throwable t) { android.util.Log.e(DIAG, m, t); }''')

s = s.replace('''    static void sync(Context context) {
        if (!isKodiInstalled(context)) return;
        Entry root = findRedLight(context);
        if (root == null) return;
        List<Entry> first = list(context, rawUri(root.uri));
        if (first == null) return;''', '''    static void sync(Context context) {
        long syncStart = android.os.SystemClock.elapsedRealtime();
        d("SYNC start");
        if (!isKodiInstalled(context)) { d("SYNC stop: Kodi not installed"); return; }
        Entry root = findRedLight(context);
        if (root == null) { d("SYNC stop: Red Light root not found"); return; }
        d("ROOT name=" + root.name + " uri=" + rawUri(root.uri));
        List<Entry> first = list(context, rawUri(root.uri));
        if (first == null) { d("SYNC stop: root listing failed"); return; }
        d("ROOT children=" + first.size());''')

s = s.replace('''            Section s = classify(n.entry, n.trail);
            if (s != null) {
                if (!found.containsKey(s.key)) found.put(s.key, s);
                continue;
            }''', '''            Section s = classify(n.entry, n.trail);
            if (s != null) {
                if (!found.containsKey(s.key)) {
                    found.put(s.key, s);
                    d("CLASSIFY key=" + s.key + " title=" + s.title + " uri=" + s.uri);
                }
                continue;
            }''')

s = s.replace('''        if (!queue.isEmpty()) complete = false;

        Map<String, ChannelState> states = loadStates(context);''', '''        if (!queue.isEmpty()) complete = false;
        d("SCAN done scans=" + scans + " found=" + found.size() + " complete=" + complete + " elapsedMs=" + (android.os.SystemClock.elapsedRealtime()-syncStart));

        Map<String, ChannelState> states = loadStates(context);''')

s = s.replace('''            st = ensureChannel(context, st);
            if (st != null) {
                syncPrograms(context, st, art);
                states.put(st.key, st);
            }''', '''            st = ensureChannel(context, st);
            if (st != null) {
                d("CHANNEL ok key=" + st.key + " id=" + st.channelId + " title=" + st.title);
                syncPrograms(context, st, art);
                states.put(st.key, st);
            } else {
                d("CHANNEL failed key=" + s.key + " title=" + s.title);
            }''')

s = s.replace('''        saveStates(context, states);
    }''', '''        saveStates(context, states);
        d("SYNC finish states=" + states.size() + " elapsedMs=" + (android.os.SystemClock.elapsedRealtime()-syncStart));
    }''', 1)

old_list = '''    private static List<Entry> list(Context c, String kodiUri) {
        Uri q = new Uri.Builder().scheme("content").authority(KODI_FILE_AUTHORITY).fragment(kodiUri).build();
        try (Cursor cursor = c.getContentResolver().query(q, null, null, null, null)) {
            if (cursor == null) return null;'''
new_list = '''    private static List<Entry> list(Context c, String kodiUri) {
        Uri q = new Uri.Builder().scheme("content").authority(KODI_FILE_AUTHORITY).fragment(kodiUri).build();
        long started = android.os.SystemClock.elapsedRealtime();
        d("LIST start uri=" + kodiUri);
        try (Cursor cursor = c.getContentResolver().query(q, null, null, null, null)) {
            if (cursor == null) { d("LIST null uri=" + kodiUri + " elapsedMs=" + (android.os.SystemClock.elapsedRealtime()-started)); return null; }'''
s = s.replace(old_list, new_list)
s = s.replace('''            return out;
        } catch (Throwable ignored) { return null; }
    }

    private static String rawUri''', '''            d("LIST done rows=" + out.size() + " elapsedMs=" + (android.os.SystemClock.elapsedRealtime()-started) + " uri=" + kodiUri);
            return out;
        } catch (Throwable t) { e("LIST error uri=" + kodiUri + " elapsedMs=" + (android.os.SystemClock.elapsedRealtime()-started), t); return null; }
    }

    private static String rawUri''')

s = s.replace('''        } catch(Throwable ignored) { return null; }
    }

    private static boolean channelExists''', '''        } catch(Throwable t) { e("CHANNEL exception key=" + st.key + " title=" + st.title, t); return null; }
    }

    private static boolean channelExists''')

s = s.replace('''    private static void syncPrograms(Context c, ChannelState st, ArtworkCache art) {
        List<Entry> entries=list(c,st.uri); if(entries==null) return;''', '''    private static void syncPrograms(Context c, ChannelState st, ArtworkCache art) {
        long started = android.os.SystemClock.elapsedRealtime();
        List<Entry> entries=list(c,st.uri); if(entries==null) { d("PROGRAMS list failed key=" + st.key); return; }
        d("PROGRAMS source key=" + st.key + " entries=" + entries.size());''')

s = s.replace('''            try { c.getContentResolver().insert(programs,v); } catch(Throwable ignored) {}
            n++;
        }
    }''', '''            try { c.getContentResolver().insert(programs,v); } catch(Throwable t) { e("PROGRAM insert error key=" + st.key + " title=" + e.name, t); }
            n++;
        }
        d("PROGRAMS done key=" + st.key + " insertedAttempted=" + n + " elapsedMs=" + (android.os.SystemClock.elapsedRealtime()-started));
    }''')

required = ['SYNC start','LIST start uri=','SCAN done scans=','CHANNEL ok key=','PROGRAMS done key=']
for marker in required:
    if marker not in s:
        raise SystemExit('diagnostic patch marker missing: ' + marker)

p.write_text(s)
print('patched RedLightEngine diagnostic logging')
