#!/usr/bin/env bash
set -euo pipefail

# CLEAN 08 is frozen CLEAN 07L plus canonical Home layout restoration and
# live Red Light semantic reconciliation. Generate a temporary 07L build with
# only CLEAN08 stage substitutions.
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean07l/build-clean07l.sh').read_text()
old='python3 .github/clean07l/prepare-redlight-icon.py\n'
new='python3 .github/clean07l/prepare-redlight-icon.py\npython3 .github/clean08/prepare-canonical-layout-v2.py\n'
if old not in src:
    raise SystemExit('CLEAN07L prepare anchor missing')
src=src.replace(old,new,1)
repls={
    '.github/clean07l/verify-clean07l.py':'.github/clean08/verify-clean08.py',
    '/tmp/verify-clean07l':'/tmp/verify-clean08',
    '/tmp/unsigned-clean07l.apk':'/tmp/unsigned-clean08.apk',
    '/tmp/Projectivy-CLEAN-07L-RedLight-Branding.apk':'/tmp/Projectivy-CLEAN-08-Canonical-Dynamic-Home-Layout.apk',
    '/tmp/clean07l-artifact':'/tmp/clean08-artifact',
    '/tmp/build-clean07l-wrapper.sh':'/tmp/build-clean08-wrapper.sh',
    '/tmp/build-clean07l-generated.sh':'/tmp/build-clean08-generated.sh',
}
for old,new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN07L build anchor missing: {old}')
    src=src.replace(old,new)
Path('/tmp/build-clean08-generated.sh').write_text(src)
PY

bash /tmp/build-clean08-generated.sh
