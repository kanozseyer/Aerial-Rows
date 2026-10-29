#!/usr/bin/env bash
set -euo pipefail

python3 - <<'PY'
from pathlib import Path

# Expand the proven CLEAN13.1D build in /tmp rather than rewriting the long base script.
b = Path('.github/clean13_1/build-clean13-1.sh').read_text()
b = b.replace(
    'python3 .github/clean13_1/fix-weather-controls.py\n',
    'python3 .github/clean13_1/fix-weather-controls.py\npython3 .github/clean13_1/fix-weather-polish.py\n', 1)
b = b.replace('unsigned-clean13-1d.apk', 'unsigned-clean13-1e.apk')
b = b.replace('Projectivy-CLEAN-13-1D-Weather-Controls-Fix.apk', 'Projectivy-CLEAN-13-1E-Weather-UI-Polish.apk')
b = b.replace('embedded131d-', 'embedded131e-')
b = b.replace('classes4-clean13-1d.dex', 'classes4-clean13-1e.dex')
b = b.replace('/tmp/pj131d', '/tmp/pj131e')
b = b.replace('/tmp/rebuilt131d.apk', '/tmp/rebuilt131e.apk')
b = b.replace('decode-clean13-1d.log', 'decode-clean13-1e.log')
b = b.replace('rebuild-clean13-1d.log', 'rebuild-clean13-1e.log')
b = b.replace('classes2-clean13-1d.dex', 'classes2-clean13-1e.dex')
b = b.replace('aligned-clean13-1d.apk', 'aligned-clean13-1e.apk')
b = b.replace('apksigner-clean13-1d.txt', 'apksigner-clean13-1e.txt')
b = b.replace('zipalign-clean13-1d.txt', 'zipalign-clean13-1e.txt')
b = b.replace('package-clean13-1d.txt', 'package-clean13-1e.txt')
b = b.replace('SHA256-clean13-1d.txt', 'SHA256-clean13-1e.txt')
b = b.replace("'CLEAN13.1B status order=settings/status icons > weather > date > clock' \\\n", "'CLEAN13.1E status order=settings/status icons > weather > date > clock' \\\n  'CLEAN13.1E status order=settings/status icons > date > clock (weather hidden)' \\\n")
b = b.replace("'CLEAN13.1D show weather=' \\\n", "'CLEAN13.1E show weather=' \\\n  'CLEAN13.1E Weather menu icon applied=' \\\n  'CLEAN13.1E Show weather menu row updated=' \\\n")
b = b.replace('CLEAN13.1D APKTOOL REBUILD FAILURE', 'CLEAN13.1E APKTOOL REBUILD FAILURE')
b = b.replace("'CLEAN13.1D: Weather controls/postcode correction'", "'CLEAN13.1E: Weather UI polish after runtime feedback'")
b = b.replace('CLEAN13-1D-NOTES.txt', 'CLEAN13-1E-NOTES.txt')
Path('/tmp/build-clean13-1e-expanded.sh').write_text(b)

# Tighten the existing verifier for the new runtime-facing markers.
vpath = Path('.github/clean13_1/verify-clean13-1.py')
v = vpath.read_text()
v = v.replace("b'CLEAN13.1B status order=settings/status icons > weather > date > clock',",
              "b'CLEAN13.1E status order=settings/status icons > weather > date > clock',\n    b'CLEAN13.1E status order=settings/status icons > date > clock (weather hidden)',")
v = v.replace("b'CLEAN13.1D show weather=',",
              "b'CLEAN13.1E show weather=',\n    b'CLEAN13.1E Weather menu icon applied=',\n    b'CLEAN13.1E Show weather menu row updated=',")
v = v.replace("print('PASS: CLEAN13.1D scope exact: classes2.dex + classes4.dex only')",
              "print('PASS: CLEAN13.1E scope exact: classes2.dex + classes4.dex only')")
vpath.write_text(v)
PY

bash /tmp/build-clean13-1e-expanded.sh
