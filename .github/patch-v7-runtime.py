from pathlib import Path
import json


def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 anchor, got {count} in {path}')
    path.write_text(text.replace(old, new, 1))
    print('patched', label)


# 1) Remove noisy personal/helper Trakt list entries from the bundled exported profile.
#    Keep the useful Red Light Movies/TV/episode shelves and the user's visibility/order.
profile = Path('embedded-runtime/src/main/assets/launcher_default_profile.json')
root = json.loads(profile.read_text())
raw_categories = root.get('action_categories_list', '[]')
categories = json.loads(raw_categories)


def noisy_channel_unique_id(value: str) -> bool:
    s = (value or '').lower()
    return (
        'red light · couchy-' in s
        or 'red light · ishows-' in s
        or 'red light · following [' in s
    )

cleaned = []
removed = []
for item in categories:
    uid = str(item.get('channelUniqueId', ''))
    if noisy_channel_unique_id(uid):
        removed.append(item.get('title', uid))
        continue
    cleaned.append(item)
root['action_categories_list'] = json.dumps(cleaned, separators=(',', ':'), ensure_ascii=False)
profile.write_text(json.dumps(root, separators=(',', ':'), ensure_ascii=False))
print('sanitized bundled channel profile; removed:', removed)


# 2) Red Light runtime: never publish personal Trakt lists as Android-TV channels,
#    prune stale ones from older builds, and restore the exported Projectivy channel
#    layout only after the visible Red Light channels actually exist.
engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()

personal_anchor = '''        if ("trakt.list.build_trakt_list".equals(mode)) {\n            String id=q(u,"list_id"), user=q(u,"user"), slug=q(u,"slug");\n            String identity=!id.isEmpty()?"id:"+id:(!user.isEmpty()||!slug.isEmpty())?"slug:"+norm(user)+":"+norm(slug):"menu:"+shortHash(join(trail));\n            return sec("trakt:list:"+identity,e.name,e);\n        }'''
personal_replacement = '''        // Personal Trakt list entries (couchy-*, ishows-*, Following, etc.) are\n        // useful inside Red Light but create noisy launcher channels. Keep them in\n        // Kodi only and publish the stable Movies/TV/episode shelves instead.\n        if ("trakt.list.build_trakt_list".equals(mode)) return null;'''
if personal_anchor not in text:
    raise SystemExit('personal Trakt-list classify anchor missing')
text = text.replace(personal_anchor, personal_replacement, 1)

states_anchor = '''        Map<String, ChannelState> states = loadStates(context);\n        Set<String> seen = new HashSet<>();'''
states_replacement = '''        Map<String, ChannelState> states = loadStates(context);\n        pruneObsoletePersonalListChannels(context, states);\n        cleanObsoletePersonalListCategories(context);\n        Set<String> seen = new HashSet<>();'''
if states_anchor not in text:
    raise SystemExit('channel-state prune anchor missing')
text = text.replace(states_anchor, states_replacement, 1)

end_anchor = '''        saveStates(context, states);\n        notifyProjectivyChannelsChanged(context);'''
end_replacement = '''        saveStates(context, states);\n        restoreBundledChannelLayoutOnce(context, states);\n        notifyProjectivyChannelsChanged(context);'''
if end_anchor not in text:
    raise SystemExit('post-sync layout restore anchor missing')
text = text.replace(end_anchor, end_replacement, 1)

helper_anchor = '''    private static void notifyProjectivyChannelsChanged(Context c) {'''
helpers = r'''    private static final String CHANNEL_LAYOUT_MARKER = "launcher_embedded_channel_layout_v1";

    private static void pruneObsoletePersonalListChannels(Context c, Map<String, ChannelState> states) {
        ArrayList<String> remove = new ArrayList<>();
        for (ChannelState st : states.values()) {
            if (st.key != null && st.key.startsWith("redlight:trakt:list:")) {
                deleteChannel(c, st.channelId);
                remove.add(st.key);
            }
        }
        for (String key : remove) states.remove(key);
    }

    private static boolean isNoisyPersonalCategory(String uid) {
        String n = uid == null ? "" : uid.toLowerCase(Locale.ROOT);
        return n.contains("red light · couchy-")
                || n.contains("red light · ishows-")
                || n.contains("red light · following [");
    }

    private static void cleanObsoletePersonalListCategories(Context c) {
        try {
            android.content.SharedPreferences p = EmbeddedRuntime.projectivyPrefs(c);
            String raw = p.getString("action_categories_list", "[]");
            JSONArray in = new JSONArray(raw);
            JSONArray out = new JSONArray();
            boolean changed = false;
            for (int i = 0; i < in.length(); i++) {
                JSONObject o = in.optJSONObject(i);
                if (o != null && isNoisyPersonalCategory(o.optString("channelUniqueId", ""))) {
                    changed = true;
                    continue;
                }
                out.put(in.get(i));
            }
            if (changed) p.edit().putString("action_categories_list", out.toString()).apply();
        } catch (Throwable ignored) {}
    }

    private static void restoreBundledChannelLayoutOnce(Context c, Map<String, ChannelState> states) {
        try {
            android.content.SharedPreferences p = EmbeddedRuntime.projectivyPrefs(c);
            if (p.getBoolean(CHANNEL_LAYOUT_MARKER, false)) return;
            if (states == null || states.isEmpty()) return;

            HashSet<String> available = new HashSet<>();
            for (ChannelState st : states.values()) {
                if (st == null || st.channelId <= 0 || st.title == null) continue;
                if (channelExists(c, st.channelId)) available.add(c.getPackageName() + st.title);
            }

            JSONObject profile;
            try (InputStream in = c.getAssets().open("launcher_default_profile.json")) {
                profile = new JSONObject(EmbeddedRuntime.readAll(in, 1024 * 1024));
            }
            String wantedRaw = profile.optString("action_categories_list", "");
            if (wantedRaw.isEmpty()) return;
            JSONArray wanted = new JSONArray(wantedRaw);

            // Do not restore early. Projectivy removes categories whose channels do not
            // yet exist. Wait until every Red Light row that was visible in the export
            // is present in TvProvider, then put the complete exported order/visibility
            // back in one operation.
            boolean hasVisibleRedLight = false;
            for (int i = 0; i < wanted.length(); i++) {
                JSONObject o = wanted.optJSONObject(i);
                if (o == null || o.optInt("type", -1) != 6) continue;
                String uid = o.optString("channelUniqueId", "");
                if (!uid.startsWith(c.getPackageName() + "Red Light · ")) continue;
                boolean visible = !o.has("isVisible") || o.optBoolean("isVisible", true);
                if (!visible) continue;
                hasVisibleRedLight = true;
                if (!available.contains(uid)) return;
            }
            if (!hasVisibleRedLight) return;

            p.edit()
                    .putString("action_categories_list", wanted.toString())
                    .putBoolean(CHANNEL_LAYOUT_MARKER, true)
                    .apply();
        } catch (Throwable ignored) {}
    }

'''
if helper_anchor not in text:
    raise SystemExit('v6 channel-refresh helper anchor missing')
text = text.replace(helper_anchor, helpers + helper_anchor, 1)
engine.write_text(text)
print('patched Red Light personal-list pruning and delayed channel-layout restore')


# 3) Make initial discovery more deterministic. The bootstrap scan can overlap the
#    first Home resume, so add two delayed rescan opportunities after Kodi/provider
#    startup. Atomic guarding still prevents concurrent scans.
embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
resume_anchor = '''        h.postDelayed(() -> notifyAerial(app), 1400L);\n        scheduleRedLight(app);\n        syncRedLightAsync(app);'''
resume_replacement = '''        h.postDelayed(() -> notifyAerial(app), 1400L);\n        scheduleRedLight(app);\n        syncRedLightAsync(app);\n        h.postDelayed(() -> syncRedLightAsync(app), 2500L);\n        h.postDelayed(() -> syncRedLightAsync(app), 6500L);'''
replace_once(embedded, resume_anchor, resume_replacement, 'delayed Red Light rescans')
