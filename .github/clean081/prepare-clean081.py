#!/usr/bin/env python3
from pathlib import Path

# CLEAN 08.1 changes only two runtime behaviours on top of CLEAN 08:
# 1) use the exact opaque card background value from the user's 2026-10-28 backup
# 2) traverse Red Light's Trakt Lists intermediate menu so Watchlists are discovered

engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

old_traverse = '''        if (mode.equals("navigator.my_lists") || mode.equals("navigator.trakt_watchlists")) return true;'''
new_traverse = '''        if (mode.equals("navigator.my_lists") || mode.equals("navigator.trakt_lists_personal")\n                || mode.equals("navigator.trakt_watchlists")) return true;'''
if text.count(old_traverse) != 1:
    raise SystemExit(f'CLEAN081 watchlist traversal anchor mismatch: {text.count(old_traverse)}')
text = text.replace(old_traverse, new_traverse, 1)

# Force one CLEAN 08.1 canonical reconciliation even when CLEAN 08 already ran.
# This is intentionally a new marker only; the catalog/identity map formats stay compatible.
old_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v1";'
new_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v2_cardbg_watchlists";'
if text.count(old_marker) != 1:
    raise SystemExit(f'CLEAN081 applied-marker anchor mismatch: {text.count(old_marker)}')
text = text.replace(old_marker, new_marker, 1)

# The old canonical export stored 3355443 == 0x00333333 (alpha 00, transparent).
# The user's manually corrected backup stores -12566464 == 0xFF404040 (opaque).
old_bg = '''            if (canonical.has("appearance_cards_bg_tint"))\n                appearance.putInt("appearance_cards_bg_tint", canonical.getInt("appearance_cards_bg_tint"));'''
new_bg = '''            // CLEAN 08.1: exact card background from projectivy-settings-20261028_214702.plbackup.\n            appearance.putInt("appearance_cards_bg_tint", -12566464);'''
if text.count(old_bg) != 1:
    raise SystemExit(f'CLEAN081 card background anchor mismatch: {text.count(old_bg)}')
text = text.replace(old_bg, new_bg, 1)
engine.write_text(text)

# Fresh installs should get the corrected card background during CLEAN 05's native
# typed preference import as well, before any channel/layout work starts.
profile = Path('.github/clean05/ProfileRestoreProvider.java')
p = profile.read_text()
anchor = '''            root.remove("launcher_embedded_channel_layout_attempts_v2");\n'''
insert = '''            root.remove("launcher_embedded_channel_layout_attempts_v2");\n            // CLEAN 08.1: exact opaque card background from the user's corrected backup.\n            root.put("appearance_cards_bg_tint", -12566464);\n'''
if p.count(anchor) != 1:
    raise SystemExit(f'CLEAN081 profile background anchor mismatch: {p.count(anchor)}')
p = p.replace(anchor, insert, 1)
profile.write_text(p)

check = engine.read_text()
for token in [
    'navigator.trakt_lists_personal',
    'canonical_002828_applied_v2_cardbg_watchlists',
    'appearance.putInt("appearance_cards_bg_tint", -12566464)',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
]:
    if token not in check:
        raise SystemExit('CLEAN081 engine verification failed: ' + token)
if 'root.put("appearance_cards_bg_tint", -12566464)' not in profile.read_text():
    raise SystemExit('CLEAN081 profile background override missing')

print('PASS: CLEAN 08.1 prepared — opaque #404040 cards + Trakt Watchlist traversal')
