#!/usr/bin/env python3
from pathlib import Path

# CLEAN10 starts from the already-prepared CLEAN09.1 source and changes only
# preview-program click routing for TV-show cards. Red Light itself owns the
# show -> season -> episode UI: tvshow.py emits build_season_list (or, when the
# user's all-episodes preference requests it, build_episode_list season=all).
engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

old_scope = '(type == 0 || type == 2)'
new_scope = '(type == 0 || type == 1 || type == 2)'
if text.count(old_scope) != 1:
    raise SystemExit(f'CLEAN10 playback scope anchor mismatch: {text.count(old_scope)}')
text = text.replace(old_scope, new_scope, 1)

old_kind = 'type == 0 ? "movie" : "episode"'
new_kind = 'type == 0 ? "movie" : (type == 1 ? "show" : "episode")'
if text.count(old_kind) != 1:
    raise SystemExit(f'CLEAN10 kind anchor mismatch: {text.count(old_kind)}')
text = text.replace(old_kind, new_kind, 1)

text = text.replace(
    '/** CLEAN 09: movies and episodes are playable; TV-show/folder cards remain inert until CLEAN 10. */',
    '/** CLEAN 10: movies, TV shows and episodes use the proven Red Light trampoline; other navigation cards remain inert. */',
    1)
text = text.replace(
    '/** Unique inert identity retained for TV-show/folder/navigation cards until CLEAN 10. */',
    '/** Unique inert identity retained for non-media folder/navigation cards. */',
    1)

engine.write_text(text)

check = engine.read_text()
for token in [
    'type == 0 || type == 1 || type == 2',
    'type == 0 ? "movie" : (type == 1 ? "show" : "episode")',
    'RedLightLaunchActivity.class.getName()',
    'RedLightLaunchActivity.EXTRA_TARGET',
    'RedLightLaunchActivity.EXTRA_KIND',
    'return programIdentityIntent(c, pluginUri)',
    'projectivy-clean07',
]:
    if token not in check:
        raise SystemExit('CLEAN10 engine verification failed: ' + token)

if '(type == 0 || type == 2)' in check:
    raise SystemExit('CLEAN10 stale movie/episode-only scope remains')

print('PASS: CLEAN10 source prepared — TV-show cards now hand off to Red Light season/episode navigation')
