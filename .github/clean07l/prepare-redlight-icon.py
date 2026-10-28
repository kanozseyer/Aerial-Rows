#!/usr/bin/env python3
from pathlib import Path

# CLEAN 07L presentation-only patch: give every dynamic Red Light Android TV
# channel the real icon from the installed Kodi addon. Projectivy can then use
# the native channel logo in Edit Channels and for Home row-title icons.
engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

existing = '''                c.getContentResolver().update(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, id), v, null, null);\n                return id;'''
existing_repl = '''                c.getContentResolver().update(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, id), v, null, null);\n                applyRedLightChannelLogo(c, id);\n                return id;'''
if 'applyRedLightChannelLogo(c, id);' not in text:
    if text.count(existing) != 1:
        raise SystemExit(f'existing-channel logo anchor mismatch: {text.count(existing)}')
    text = text.replace(existing, existing_repl, 1)

created = '''            id = ContentUris.parseId(inserted);\n            try { TvContract.requestChannelBrowsable(c, id); } catch (Throwable ignored) {}\n            return id;'''
created_repl = '''            id = ContentUris.parseId(inserted);\n            applyRedLightChannelLogo(c, id);\n            try { TvContract.requestChannelBrowsable(c, id); } catch (Throwable ignored) {}\n            return id;'''
if text.count('applyRedLightChannelLogo(c, id);') < 2:
    if text.count(created) != 1:
        raise SystemExit(f'new-channel logo anchor mismatch: {text.count(created)}')
    text = text.replace(created, created_repl, 1)

helper_anchor = '''    private static long findChannel(Context c, String key, String title) {'''
helper = r'''    private static void applyRedLightChannelLogo(Context c, long channelId) {
        if (channelId <= 0) return;
        try {
            File icon = new File(c.getCacheDir(), "redlight_channel_icon.png");
            if (!icon.isFile() || icon.length() <= 0) {
                if (!copyKodiFile(c,
                        "special://home/addons/plugin.video.redlight/resources/media/addon_icons/icon.png",
                        icon)) {
                    Log.i(TAG, "CLEAN07L Red Light icon unavailable");
                    return;
                }
            }

            Uri logo = TvContract.buildChannelLogoUri(channelId);
            try (InputStream in = new java.io.FileInputStream(icon);
                 java.io.OutputStream out = c.getContentResolver().openOutputStream(logo)) {
                if (out == null) return;
                byte[] buf = new byte[32768];
                for (int n; (n = in.read(buf)) >= 0; ) out.write(buf, 0, n);
                out.flush();
                Log.i(TAG, "CLEAN07L channel logo applied id=" + channelId);
            }
        } catch (Throwable t) {
            Log.i(TAG, "CLEAN07L channel logo failed id=" + channelId);
        }
    }

'''
if 'private static void applyRedLightChannelLogo' not in text:
    if text.count(helper_anchor) != 1:
        raise SystemExit(f'channel-logo helper anchor mismatch: {text.count(helper_anchor)}')
    text = text.replace(helper_anchor, helper + helper_anchor, 1)

engine.write_text(text)
check = engine.read_text()
for token in [
    'special://home/addons/plugin.video.redlight/resources/media/addon_icons/icon.png',
    'TvContract.buildChannelLogoUri(channelId)',
    'CLEAN07L channel logo applied',
]:
    if token not in check:
        raise SystemExit('missing CLEAN07L icon token: ' + token)
if check.count('applyRedLightChannelLogo(c, id);') != 2:
    raise SystemExit('expected logo application on existing and new channels')
print('PASS: CLEAN 07L source patched with real Red Light native channel logo')
