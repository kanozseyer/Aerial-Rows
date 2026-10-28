#!/usr/bin/env bash
set -euo pipefail

# CLEAN 08.1 is frozen CLEAN 07L + CLEAN 08 canonical layout + exactly two fixes:
# corrected opaque card background and Red Light Trakt Watchlist traversal.
python3 - <<'PY'
from pathlib import Path
src = Path('.github/clean07l/build-clean07l.sh').read_text()

old = 'python3 .github/clean07l/prepare-redlight-icon.py\n'
new = (
    'python3 .github/clean07l/prepare-redlight-icon.py\n'
    'python3 .github/clean08/prepare-canonical-layout-v2.py\n'
    'python3 .github/clean081/prepare-clean081.py\n'
)
if old not in src:
    raise SystemExit('CLEAN07L prepare anchor missing')
src = src.replace(old, new, 1)

repls = {
    '.github/clean07l/verify-clean07l.py': '.github/clean081/verify-clean081.py',
    '/tmp/verify-clean07l': '/tmp/verify-clean081',
    '/tmp/unsigned-clean07l.apk': '/tmp/unsigned-clean081.apk',
    '/tmp/Projectivy-CLEAN-07L-RedLight-Branding.apk': '/tmp/Projectivy-CLEAN-08-1-CardBG-Watchlists.apk',
    '/tmp/clean07l-artifact': '/tmp/clean081-artifact',
}
for old, new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN07L build anchor missing: {old}')
    src = src.replace(old, new)

Path('/tmp/build-clean081-stage.sh').write_text(src)
PY

bash /tmp/build-clean081-stage.sh

test -f /tmp/Projectivy-CLEAN-08-1-CardBG-Watchlists.apk
mkdir -p /tmp/clean081-artifact
cp -f /tmp/Projectivy-CLEAN-08-1-CardBG-Watchlists.apk /tmp/clean081-artifact/
