#!/usr/bin/env python3
from pathlib import Path

# CLEAN 08.5 starts from the successful CLEAN 08.2 tree. It deliberately keeps
# the raw int appearance write because the broken-state export proved the partial
# native importer/migration strips the alpha byte from 0xFF393938 -> 0x00393938.
# The only new behavior is a one-time full process restart after the persisted
# layout/appearance state and CLEAN08 marker have been committed.

engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

old_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v3_final_cardbg";'
new_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v6_raw_alpha_process_reload";'
if text.count(old_marker) != 1:
    raise SystemExit(f'CLEAN085 marker anchor mismatch: {text.count(old_marker)}')
text = text.replace(old_marker, new_marker, 1)

# CLEAN 08.2 must still be using the exact opaque signed ARGB value directly.
raw_bg = 'appearance.putInt("appearance_cards_bg_tint", -13027016);'
if text.count(raw_bg) != 1:
    raise SystemExit(f'CLEAN085 raw opaque card background missing: {text.count(raw_bg)}')

old_success = '''                    Log.i(CLEAN08_TAG, "CLEAN08 APPLIED categories=" + out.length()\n                            + " liveRed=" + redByKey.size()\n                            + " matched=" + matchedFinal\n                            + " removed=" + removedFinal\n                            + " addedHidden=" + addedFinal\n                            + " mode=" + (applied ? "reconcile" : "canonical"));\n                    ok.set(true);\n'''
new_success = '''                    Log.i(CLEAN08_TAG, "CLEAN08 APPLIED categories=" + out.length()\n                            + " liveRed=" + redByKey.size()\n                            + " matched=" + matchedFinal\n                            + " removed=" + removedFinal\n                            + " addedHidden=" + addedFinal\n                            + " mode=" + (applied ? "reconcile" : "canonical"));\n                    ok.set(true);\n\n                    // CLEAN 08.5: Activity recreation is insufficient because Projectivy\n                    // keeps live appearance objects in process-level caches. The opaque\n                    // ARGB value and v6 marker are already committed above, so terminate\n                    // this process once and let Android relaunch the active HOME launcher\n                    // from its persisted preferences. The v6 marker prevents a restart loop.\n                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {\n                        Log.i(CLEAN08_TAG, "CLEAN085 killing process after raw opaque appearance commit");\n                        android.os.Process.killProcess(android.os.Process.myPid());\n                    }, 1200L);\n'''
if text.count(old_success) != 1:
    raise SystemExit(f'CLEAN085 success anchor mismatch: {text.count(old_success)}')
text = text.replace(old_success, new_success, 1)
engine.write_text(text)

check = engine.read_text()
for token in [
    'canonical_002828_applied_v6_raw_alpha_process_reload',
    'appearance.putInt("appearance_cards_bg_tint", -13027016)',
    'CLEAN085 killing process after raw opaque appearance commit',
    'android.os.Process.killProcess(android.os.Process.myPid())',
    'navigator.trakt_lists_personal',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
]:
    if token not in check:
        raise SystemExit('CLEAN085 verification failed: ' + token)

# Explicitly reject the two failed appearance-update architectures.
for bad in [
    'appearancePatchJson',
    'CLEAN083 native appearance applied cardsBg=',
    'CLEAN084 Android HOME intent dispatched after native appearance apply',
]:
    if bad in check:
        raise SystemExit('CLEAN085 unexpectedly inherited rejected appearance path: ' + bad)

print('PASS: CLEAN 08.5 prepared — raw opaque #393938 + one full process reload')
