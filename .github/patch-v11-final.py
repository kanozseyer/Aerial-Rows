from pathlib import Path

# A channel preseeded before Kodi is fully ready may miss its logo. The first real
# Red Light semantic adoption is a one-time opportunity to retry the installed icon
# without returning to v9's every-sync logo rewrite.
engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()
old = '''                c.getContentResolver().update(\n                        ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, st.channelId),\n                        up, null, null);\n                return st;'''
new = '''                c.getContentResolver().update(\n                        ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, st.channelId),\n                        up, null, null);\n                applyRedLightChannelLogo(c, st.channelId);\n                return st;'''
if text.count(old) != 1:
    raise SystemExit(f'v11 preseed adoption artwork anchor mismatch: {text.count(old)}')
engine.write_text(text.replace(old, new, 1))
print('added one-time Red Light logo retry when preseed channel is adopted')
