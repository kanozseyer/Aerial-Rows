#!/usr/bin/env bash
set -euo pipefail

# CLEAN 07L is frozen CLEAN 07K plus presentation-only Red Light branding:
# real installed-addon channel logo + Red Light source heading.
python3 .github/clean07l/prepare-redlight-icon.py

python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean07k/build-clean07k.sh').read_text()
repls={
    '.github/clean07k/patch-native-multiselect.py':'.github/clean07l/patch-native-multiselect.py',
    '.github/clean07k/verify-clean07k.py':'.github/clean07l/verify-clean07l.py',
    '/tmp/verify-clean07k':'/tmp/verify-clean07l',
    '/tmp/unsigned-clean07k.apk':'/tmp/unsigned-clean07l.apk',
    '/tmp/Projectivy-CLEAN-07K-Exact-Visible-Sync.apk':'/tmp/Projectivy-CLEAN-07L-RedLight-Branding.apk',
    '/tmp/clean07k-artifact':'/tmp/clean07l-artifact',
    '/tmp/build-clean07k-wrapper.sh':'/tmp/build-clean07l-wrapper.sh',
}
for old,new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN07K build anchor missing: {old}')
    src=src.replace(old,new)
Path('/tmp/build-clean07l-generated.sh').write_text(src)
PY

bash /tmp/build-clean07l-generated.sh
