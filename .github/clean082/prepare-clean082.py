#!/usr/bin/env python3
from pathlib import Path

# CLEAN 08.2 changes exactly one user-facing value on top of CLEAN 08.1:
# card background from #404040 to the user's final exported #393938.

engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()
old_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v2_cardbg_watchlists";'
new_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v3_final_cardbg";'
if text.count(old_marker) != 1:
    raise SystemExit(f'CLEAN082 applied-marker anchor mismatch: {text.count(old_marker)}')
text = text.replace(old_marker, new_marker, 1)
old_bg = 'appearance.putInt("appearance_cards_bg_tint", -12566464);'
new_bg = 'appearance.putInt("appearance_cards_bg_tint", -13027016);'
if text.count(old_bg) != 1:
    raise SystemExit(f'CLEAN082 engine card background anchor mismatch: {text.count(old_bg)}')
text = text.replace(old_bg, new_bg, 1)
engine.write_text(text)

profile = Path('.github/clean05/ProfileRestoreProvider.java')
p = profile.read_text()
old_profile_bg = 'root.put("appearance_cards_bg_tint", -12566464);'
new_profile_bg = 'root.put("appearance_cards_bg_tint", -13027016);'
if p.count(old_profile_bg) != 1:
    raise SystemExit(f'CLEAN082 profile card background anchor mismatch: {p.count(old_profile_bg)}')
p = p.replace(old_profile_bg, new_profile_bg, 1)
profile.write_text(p)

check = engine.read_text()
for token in [
    'navigator.trakt_lists_personal',
    'navigator.trakt_watchlists',
    'canonical_002828_applied_v3_final_cardbg',
    'appearance.putInt("appearance_cards_bg_tint", -13027016)',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
]:
    if token not in check:
        raise SystemExit('CLEAN082 verification failed: ' + token)
if 'root.put("appearance_cards_bg_tint", -13027016)' not in profile.read_text():
    raise SystemExit('CLEAN082 profile background override missing')

print('PASS: CLEAN 08.2 prepared — final card background #393938 only')
