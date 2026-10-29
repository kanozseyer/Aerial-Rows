#!/usr/bin/env bash
set -euo pipefail

python3 - <<'PY'
from pathlib import Path

b = Path('.github/clean13_1/build-clean13-1.sh').read_text()
b = b.replace(
    'python3 .github/clean13_1/fix-weather-controls.py\n',
    'python3 .github/clean13_1/fix-weather-controls.py\n'
    'python3 .github/clean13_1/fix-weather-polish.py\n'
    'python3 .github/clean13_1/fix-weather-final-polish.py\n', 1)

repl = {
    'unsigned-clean13-1d.apk': 'unsigned-clean13-1f.apk',
    'Projectivy-CLEAN-13-1D-Weather-Controls-Fix.apk': 'Projectivy-CLEAN-13-1F-Weather-Final-Polish.apk',
    'embedded131d-': 'embedded131f-',
    'classes4-clean13-1d.dex': 'classes4-clean13-1f.dex',
    '/tmp/pj131d': '/tmp/pj131f',
    '/tmp/rebuilt131d.apk': '/tmp/rebuilt131f.apk',
    'decode-clean13-1d.log': 'decode-clean13-1f.log',
    'rebuild-clean13-1d.log': 'rebuild-clean13-1f.log',
    'classes2-clean13-1d.dex': 'classes2-clean13-1f.dex',
    'aligned-clean13-1d.apk': 'aligned-clean13-1f.apk',
    'apksigner-clean13-1d.txt': 'apksigner-clean13-1f.txt',
    'zipalign-clean13-1d.txt': 'zipalign-clean13-1f.txt',
    'package-clean13-1d.txt': 'package-clean13-1f.txt',
    'SHA256-clean13-1d.txt': 'SHA256-clean13-1f.txt',
    'CLEAN13.1D APKTOOL REBUILD FAILURE': 'CLEAN13.1F APKTOOL REBUILD FAILURE',
    "'CLEAN13.1D: Weather controls/postcode correction'": "'CLEAN13.1F: Weather final UI polish after Shield feedback'",
    'CLEAN13-1D-NOTES.txt': 'CLEAN13-1F-NOTES.txt',
}
for old,new in repl.items():
    b = b.replace(old,new)

# Runtime marker expectations for the final-polish source.
b = b.replace("'CLEAN13.1B status order=settings/status icons > weather > date > clock' \\\n",
              "'CLEAN13.1F status order=settings/status icons > weather > date > clock' \\\n  'CLEAN13.1F status order=settings/status icons > date > clock (weather hidden)' \\\n")
b = b.replace("'CLEAN13.1D show weather=' \\\n",
              "'CLEAN13.1F show weather=' \\\n  'CLEAN13.1F native Show weather checkbox added' \\\n  'CLEAN13.1F Weather menu icon applied=' \\\n")
Path('/tmp/build-clean13-1f-expanded.sh').write_text(b)

vpath = Path('.github/clean13_1/verify-clean13-1.py')
v = vpath.read_text()
v = v.replace("b'CLEAN13.1B status order=settings/status icons > weather > date > clock',",
              "b'CLEAN13.1F status order=settings/status icons > weather > date > clock',\n    b'CLEAN13.1F status order=settings/status icons > date > clock (weather hidden)',")
v = v.replace("b'CLEAN13.1D show weather=',",
              "b'CLEAN13.1F show weather=',\n    b'CLEAN13.1F native Show weather checkbox added',\n    b'CLEAN13.1F Weather menu icon applied=',")
v = v.replace("print('PASS: CLEAN13.1D scope exact: classes2.dex + classes4.dex only')",
              "print('PASS: CLEAN13.1F scope exact: classes2.dex + classes4.dex only')")
vpath.write_text(v)
PY

bash /tmp/build-clean13-1f-expanded.sh
