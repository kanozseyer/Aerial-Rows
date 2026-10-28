#!/usr/bin/env bash
set -euo pipefail

# CLEAN 08.5 is frozen CLEAN 07L + CLEAN 08 + 08.1 + 08.2, with only a
# one-time full process reload added after the raw opaque appearance commit.
python3 - <<'PY'
from pathlib import Path
src = Path('.github/clean07l/build-clean07l.sh').read_text()

old = 'python3 .github/clean07l/prepare-redlight-icon.py\n'
new = (
    'python3 .github/clean07l/prepare-redlight-icon.py\n'
    'python3 .github/clean08/prepare-canonical-layout-v2.py\n'
    'python3 .github/clean081/prepare-clean081.py\n'
    'python3 .github/clean082/prepare-clean082.py\n'
    'python3 .github/clean085/prepare-clean085.py\n'
)
if old not in src:
    raise SystemExit('CLEAN07L prepare anchor missing')
src = src.replace(old, new, 1)

repls = {
    '.github/clean07l/verify-clean07l.py': '.github/clean085/verify-clean085.py',
    '/tmp/verify-clean07l': '/tmp/verify-clean085',
    '/tmp/unsigned-clean07l.apk': '/tmp/unsigned-clean085.apk',
    '/tmp/Projectivy-CLEAN-07L-RedLight-Branding.apk': '/tmp/Projectivy-CLEAN-08-5-Raw-Alpha-Process-Reload.apk',
    '/tmp/clean07l-artifact': '/tmp/clean085-artifact',
}
for old, new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN07L build anchor missing: {old}')
    src = src.replace(old, new)

Path('/tmp/build-clean085-stage.sh').write_text(src)
PY

bash /tmp/build-clean085-stage.sh

test -f /tmp/Projectivy-CLEAN-08-5-Raw-Alpha-Process-Reload.apk
mkdir -p /tmp/clean085-artifact
cp -f /tmp/Projectivy-CLEAN-08-5-Raw-Alpha-Process-Reload.apk /tmp/clean085-artifact/
