from pathlib import Path

embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')

et = embedded.read_text()
rt = engine.read_text()

# Keep the normal immediate v9 sync, but replace the two unconditional short retries
# with bounded recovery checks. Each delayed attempt is skipped as soon as at least
# one real Red Light TvProvider channel exists.
old = '''        syncRedLightAsync(app);\n        h.postDelayed(() -> syncRedLightAsync(app), 2500L);\n        h.postDelayed(() -> syncRedLightAsync(app), 6500L);'''
new = '''        syncRedLightAsync(app);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 3000L);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 10000L);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 30000L);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 60000L);'''
if et.count(old) != 1:
    raise SystemExit(f'cold-Kodi retry anchor mismatch: {et.count(old)}')
et = et.replace(old, new, 1)

method_anchor = '''    static void syncRedLightAsync(Context c) {'''
method = '''    private static void retryRedLightIfMissing(Context c) {\n        if (c == null) return;\n        Context app = c.getApplicationContext();\n        if (RedLightEngine.hasPublishedChannels(app)) return;\n        syncRedLightAsync(app);\n    }\n\n'''
if et.count(method_anchor) != 1:
    raise SystemExit('syncRedLightAsync anchor missing')
et = et.replace(method_anchor, method + method_anchor, 1)
embedded.write_text(et)

# A failed/early Kodi provider query already returns before stale-channel cleanup in
# v9. This helper only answers whether startup recovery is still needed; it never
# creates, deletes or mutates channels.
engine_anchor = '''    private static boolean isKodiInstalled(Context c) {'''
helper = '''    static boolean hasPublishedChannels(Context c) {\n        try {\n            Map<String, ChannelState> states = loadStates(c);\n            for (ChannelState st : states.values()) {\n                if (st == null || st.channelId <= 0) continue;\n                if (st.key == null || !st.key.startsWith("redlight:")) continue;\n                if (channelExists(c, st.channelId)) return true;\n            }\n        } catch (Throwable ignored) {}\n        return false;\n    }\n\n'''
if rt.count(engine_anchor) != 1:
    raise SystemExit('RedLightEngine helper anchor missing')
rt = rt.replace(engine_anchor, helper + engine_anchor, 1)
engine.write_text(rt)

# Strict build-time checks so this patch cannot silently degrade back to the old
# 2.5/6.5-second-only behaviour.
final = embedded.read_text()
for ms in ('3000L', '10000L', '30000L', '60000L'):
    if ms not in final:
        raise SystemExit(f'missing retry {ms}')
if '2500L' in final or '6500L' in final:
    raise SystemExit('legacy retry timings still present')
if 'hasPublishedChannels' not in engine.read_text():
    raise SystemExit('published-channel guard missing')
print('patched bounded cold-Kodi Red Light recovery: 3s/10s/30s/60s')
