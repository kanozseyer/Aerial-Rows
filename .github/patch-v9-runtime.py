from pathlib import Path

# v9 gives every embedded Red Light Android-TV channel the real Red Light addon icon.
# Projectivy uses the native channel logo for channel/source artwork and for the
# optional icon shown in front of a Home category title, so one logo assignment
# covers both Edit Channels and Home row-title icons.

engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()

existing = '''                c.getContentResolver().update(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, st.channelId),up,null,null);\n                return st;'''
existing_repl = '''                c.getContentResolver().update(ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, st.channelId),up,null,null);\n                applyRedLightChannelLogo(c, st.channelId);\n                return st;'''
if text.count(existing) != 1:
    raise SystemExit(f'existing-channel logo anchor mismatch: {text.count(existing)}')
text = text.replace(existing, existing_repl, 1)

created = '''            st.channelId=ContentUris.parseId(inserted);\n            try { TvContract.requestChannelBrowsable(c,st.channelId); } catch(Throwable ignored) {}\n            return st;'''
created_repl = '''            st.channelId=ContentUris.parseId(inserted);\n            applyRedLightChannelLogo(c, st.channelId);\n            try { TvContract.requestChannelBrowsable(c,st.channelId); } catch(Throwable ignored) {}\n            return st;'''
if text.count(created) != 1:
    raise SystemExit(f'new-channel logo anchor mismatch: {text.count(created)}')
text = text.replace(created, created_repl, 1)

helper_anchor = '''    private static boolean channelExists(Context c,long id) {'''
helper = r'''    private static void applyRedLightChannelLogo(Context c, long channelId) {
        if (channelId <= 0) return;
        try {
            // Pull the exact icon from the Red Light addon installed in Kodi. This
            // keeps the launcher tied to the user's installed addon artwork rather
            // than a generic/recreated icon.
            File icon = new File(c.getCacheDir(), "redlight_channel_icon.png");
            if (!icon.isFile() || icon.length() <= 0) {
                if (!copyKodiFile(c,
                        "special://home/addons/plugin.video.redlight/resources/media/addon_icons/icon.png",
                        icon)) return;
            }

            Uri logo = TvContract.buildChannelLogoUri(channelId);
            try (InputStream in = new java.io.FileInputStream(icon);
                 java.io.OutputStream out = c.getContentResolver().openOutputStream(logo)) {
                if (out == null) return;
                byte[] buf = new byte[32768];
                for (int n; (n = in.read(buf)) >= 0; ) out.write(buf, 0, n);
                out.flush();
            }
        } catch (Throwable ignored) {}
    }

'''
if text.count(helper_anchor) != 1:
    raise SystemExit(f'channel-logo helper anchor mismatch: {text.count(helper_anchor)}')
text = text.replace(helper_anchor, helper + helper_anchor, 1)

engine.write_text(text)
print('patched native Red Light channel logos for settings and Home titles')
