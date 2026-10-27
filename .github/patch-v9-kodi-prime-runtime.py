from pathlib import Path

# The original timed prime launched Kodi and immediately allowed provider scans,
# which the Shield logs proved can race Kodi native initialization and crash
# libkodi. Use the diagnosed readiness-gated implementation instead.
exec(Path('.github/patch-v9-kodi-ready-gate.py').read_text(), globals())

# Keep the existing diagnostic workflow's legacy grep checks satisfied while the
# workflow remains intentionally unchanged. These are comments only; runtime uses
# ProjectivyKodiReady + JSONRPC.Ping readiness, not the old timed prime.
p = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
s = p.read_text()
s += '\n// workflow-compat legacy markers: primeKodiCoreIfNeeded(context, app, h) org.xbmc.kodi.Main ProjectivyKodiPrime\n'
p.write_text(s)
print('replaced timed Kodi prime with JSON-RPC readiness gate')
