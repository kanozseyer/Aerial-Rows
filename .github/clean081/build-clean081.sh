#!/usr/bin/env bash
set -euo pipefail

# CLEAN 08.1 is CLEAN 08 plus exactly two runtime changes:
# corrected opaque card background and Red Light Trakt Watchlist traversal.
python3 - <<'PY'
from pathlib import Path
src = Path('.github/clean08/build-clean08.sh').read_text()

old = 'python3 .github/clean08/prepare-canonical-layout-v2.py\n'
new = old + 'python3 .github/clean081/prepare-clean081.py\n'
if old not in src:
    raise SystemExit('CLEAN08 prepare anchor missing')
src = src.replace(old, new, 1)

repls = {
    '.github/clean08/verify-clean08.py': '.github/clean081/verify-clean081.py',
    '/tmp/verify-clean08': '/tmp/verify-clean081',
    '/tmp/unsigned-clean08.apk': '/tmp/unsigned-clean081.apk',
    '/tmp/Projectivy-CLEAN-08-Canonical-Dynamic-Home-Layout.apk': '/tmp/Projectivy-CLEAN-08-1-CardBG-Watchlists.apk',
    '/tmp/clean08-artifact': '/tmp/clean081-artifact',
    '/tmp/build-clean08-stage.sh': '/tmp/build-clean081-stage.sh',
}
for old, new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN08 build anchor missing: {old}')
    src = src.replace(old, new)

Path('/tmp/build-clean081-generated.sh').write_text(src)
PY

bash /tmp/build-clean081-generated.sh

test -f /tmp/Projectivy-CLEAN-08-1-CardBG-Watchlists.apk
mkdir -p /tmp/clean081-artifact
cp -f /tmp/Projectivy-CLEAN-08-1-CardBG-Watchlists.apk /tmp/clean081-artifact/
