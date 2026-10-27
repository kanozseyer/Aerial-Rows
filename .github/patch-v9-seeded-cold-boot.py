from pathlib import Path

embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')

et = embedded.read_text()
rt = engine.read_text()

# v9.6: cold boot must never touch Kodi's provider or launch Kodi.  Instead the
# launcher may publish an optional bundled snapshot through the exact same
# RedLightEngine channel/program model used by live discovery.  The private seed
# asset is injected after CI; it is intentionally not stored in this repository.
resume_old = '''        h.postDelayed(() -> notifyAerial(app), 350L);\n        h.postDelayed(() -> notifyAerial(app), 1400L);\n        scheduleRedLight(app);\n        syncRedLightAsync(app);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 3000L);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 10000L);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 30000L);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 60000L);'''
resume_new = '''        h.postDelayed(() -> notifyAerial(app), 350L);\n        h.postDelayed(() -> notifyAerial(app), 1400L);\n        RedLightEngine.seedBundledChannelsOnce(app);\n        android.util.Log.i("ProjectivyRedLightSeed", "Cold-boot seed checked; Kodi/provider untouched");'''
if et.count(resume_old) != 1:
    raise SystemExit(f'seeded resume anchor mismatch: {et.count(resume_old)}')
et = et.replace(resume_old, resume_new, 1)

bootstrap_old = 'if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); syncRedLightAsync(c); }'
bootstrap_new = 'if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); }'
if et.count(bootstrap_old) != 1:
    raise SystemExit(f'seeded BootstrapProvider anchor mismatch: {et.count(bootstrap_old)}')
et = et.replace(bootstrap_old, bootstrap_new, 1)

boot_old = '''        @Override public void onReceive(Context c, Intent i) {\n            scheduleRedLight(c);\n            if (Intent.ACTION_BOOT_COMPLETED.equals(i == null ? null : i.getAction())) notifyAerial(c);\n        }'''
boot_new = '''        @Override public void onReceive(Context c, Intent i) {\n            if (Intent.ACTION_BOOT_COMPLETED.equals(i == null ? null : i.getAction())) notifyAerial(c);\n        }'''
if et.count(boot_old) != 1:
    raise SystemExit(f'seeded BootReceiver anchor mismatch: {et.count(boot_old)}')
et = et.replace(boot_old, boot_new, 1)

job_old = '''        @Override public boolean onStartJob(JobParameters p) {\n            EXECUTOR.execute(() -> { try { RedLightEngine.sync(getApplicationContext()); } catch (Throwable ignored) {} finally { jobFinished(p, false); } });\n            return true;\n        }'''
job_new = '''        @Override public boolean onStartJob(JobParameters p) {\n            android.util.Log.i("ProjectivyRedLightSeed", "Scheduled provider refresh suppressed in seeded cold-boot diagnostic");\n            jobFinished(p, false);\n            return false;\n        }'''
if et.count(job_old) != 1:
    raise SystemExit(f'seeded SyncJob anchor mismatch: {et.count(job_old)}')
et = et.replace(job_old, job_new, 1)
embedded.write_text(et)

# Add OutputStream for writing the optional seed channel logo into TvProvider.
if 'import java.io.OutputStream;' not in rt:
    rt = rt.replace('import java.io.InputStream;\n', 'import java.io.InputStream;\nimport java.io.OutputStream;\n', 1)

field_anchor = '    private static final String PREF_FILE = "embedded_redlight_channels";\n'
field_new = field_anchor + '    private static final String SEED_ASSET = "redlight_seed.json";\n    private static final String SEED_ICON_ASSET = "redlight_seed_icon.png";\n'
if rt.count(field_anchor) != 1:
    raise SystemExit('seed field anchor mismatch')
rt = rt.replace(field_anchor, field_new, 1)

method_anchor = '    static void sync(Context context) {\n'
if rt.count(method_anchor) != 1:
    raise SystemExit('RedLightEngine sync anchor mismatch')
seed_methods = r'''    static void seedBundledChannelsOnce(Context context) {
        if (context == null) return;
        try {
            JSONObject root;
            try (InputStream in = context.getAssets().open(SEED_ASSET)) {
                root = new JSONObject(EmbeddedRuntime.readAll(in, 2 * 1024 * 1024));
            } catch (Throwable missing) {
                android.util.Log.i("ProjectivyRedLightSeed", "No private Red Light seed asset present");
                return;
            }

            JSONArray channels = root.optJSONArray("channels");
            if (channels == null || channels.length() == 0) {
                android.util.Log.i("ProjectivyRedLightSeed", "Red Light seed asset is empty");
                return;
            }

            Map<String, ChannelState> states = loadStates(context);
            boolean complete = true;
            for (int i = 0; i < channels.length(); i++) {
                JSONObject o = channels.optJSONObject(i);
                if (o == null) continue;
                String key = o.optString("key", "");
                if (key.isEmpty()) continue;
                ChannelState st = states.get(key);
                if (st == null || st.channelId <= 0 || !channelExists(context, st.channelId)) {
                    complete = false;
                    break;
                }
            }
            if (complete) {
                restoreBundledChannelLayoutOnce(context, states);
                return;
            }

            int channelCount = 0;
            int programCount = 0;
            for (int i = 0; i < channels.length(); i++) {
                JSONObject o = channels.optJSONObject(i);
                if (o == null) continue;
                String key = o.optString("key", "");
                String title = o.optString("title", "");
                String uri = o.optString("uri", "");
                if (key.isEmpty() || title.isEmpty()) continue;

                ChannelState st = states.get(key);
                if (st == null) {
                    st = new ChannelState();
                    st.key = key;
                    st.channelId = 0;
                }
                st.title = title;
                st.uri = uri;
                st.missing = 0;
                st = ensureChannel(context, st);
                if (st == null) continue;

                writeSeedChannelLogo(context, st.channelId);
                programCount += syncSeedPrograms(context, st, o.optJSONArray("programs"));
                states.put(st.key, st);
                channelCount++;
            }

            if (channelCount > 0) {
                saveStates(context, states);
                restoreBundledChannelLayoutOnce(context, states);
                notifyProjectivyChannelsChanged(context);
                android.util.Log.i("ProjectivyRedLightSeed", "Seeded channels=" + channelCount + " programs=" + programCount);
            }
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyRedLightSeed", "Seed restore failed", t);
        }
    }

    private static int syncSeedPrograms(Context c, ChannelState st, JSONArray programsJson) {
        if (st == null || st.channelId <= 0 || programsJson == null) return 0;
        Uri programs = TvContract.PreviewPrograms.CONTENT_URI;
        try { c.getContentResolver().delete(programs, "channel_id=?", new String[]{String.valueOf(st.channelId)}); }
        catch (Throwable ignored) {}

        int inserted = 0;
        for (int i = 0; i < programsJson.length() && inserted < MAX_PROGRAMS; i++) {
            JSONObject o = programsJson.optJSONObject(i);
            if (o == null) continue;
            String title = o.optString("title", "");
            String mediaType = o.optString("mediaType", "");
            String uri = o.optString("uri", "");
            if (title.isEmpty() || uri.isEmpty()) continue;

            Entry e = new Entry();
            e.name = title;
            e.mediaType = mediaType;
            e.uri = uri;
            e.category = ("tvshow".equalsIgnoreCase(mediaType)) ? "directory" : "file";

            ContentValues v = new ContentValues();
            v.put("channel_id", st.channelId);
            v.put("title", title);
            v.put("description", mediaType.isEmpty() ? "Red Light" : mediaType);
            v.put("type", programType(e));
            v.put("intent_uri", projectivyForwardIntent(c, uri).toUri(0));
            v.put("internal_provider_id", uri);
            v.put("weight", MAX_PROGRAMS - inserted);
            v.put("poster_art_aspect_ratio", 5);
            String poster = o.optString("poster", "").trim();
            if (poster.startsWith("http://") || poster.startsWith("https://")) v.put("poster_art_uri", poster);
            try {
                if (c.getContentResolver().insert(programs, v) != null) inserted++;
            } catch (Throwable ignored) {}
        }
        return inserted;
    }

    private static void writeSeedChannelLogo(Context c, long channelId) {
        if (channelId <= 0) return;
        try (InputStream in = c.getAssets().open(SEED_ICON_ASSET);
             OutputStream out = c.getContentResolver().openOutputStream(TvContract.buildChannelLogoUri(channelId))) {
            if (out == null) return;
            byte[] b = new byte[16384];
            for (int n; (n = in.read(b)) >= 0;) out.write(b, 0, n);
        } catch (Throwable ignored) {}
    }

'''
rt = rt.replace(method_anchor, seed_methods + method_anchor, 1)
engine.write_text(rt)

# Hard safety assertions for the generated v9.6 runtime.
final_et = embedded.read_text()
final_rt = engine.read_text()
for forbidden in [
    'scheduleRedLight(app);',
    'scheduleRedLight(c);',
    'syncRedLightAsync(app);',
    'syncRedLightAsync(c);',
    'getLeanbackLaunchIntentForPackage("org.xbmc.kodi")',
    'setLaunchDisplayId(',
    'VIRTUAL_DISPLAY_FLAG_PUBLIC',
]:
    if forbidden in final_et:
        raise SystemExit('cold-boot Kodi/provider path still present in EmbeddedRuntime: ' + forbidden)
for required in [
    'RedLightEngine.seedBundledChannelsOnce(app);',
    'Cold-boot seed checked; Kodi/provider untouched',
    'Scheduled provider refresh suppressed in seeded cold-boot diagnostic',
]:
    if required not in final_et:
        raise SystemExit('seeded EmbeddedRuntime marker missing: ' + required)
for required in [
    'seedBundledChannelsOnce',
    'redlight_seed.json',
    'redlight_seed_icon.png',
    'restoreBundledChannelLayoutOnce(context, states)',
    'notifyProjectivyChannelsChanged(context)',
]:
    if required not in final_rt:
        raise SystemExit('seeded RedLightEngine marker missing: ' + required)

print('patched v9.6 seeded cold boot: same TvProvider model, zero cold Kodi/provider access')
