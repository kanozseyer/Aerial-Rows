#!/usr/bin/env bash
set -euo pipefail

# CLEAN 09 = frozen CLEAN 08.5 + movie/episode Red Light playback bridge only.
python3 - <<'PY'
from pathlib import Path
src = Path('.github/clean085/build-clean085.sh').read_text()

# CLEAN085 generates its stage from CLEAN07L and inserts all CLEAN08 prepares.
# Add CLEAN09 prepare immediately after CLEAN085 prepare so it patches the final
# frozen source/build pipeline without altering earlier checkpoints.
old = '    \'python3 .github/clean085/prepare-clean085.py\\n\'\n'
new = old + '    \'python3 .github/clean09/prepare-clean09.py\\n\'\n'
if old not in src:
    raise SystemExit('CLEAN085 prepare insertion anchor missing')
src = src.replace(old, new, 1)

repls = {
    '.github/clean085/verify-clean085.py': '.github/clean09/verify-clean09.py',
    '/tmp/verify-clean085': '/tmp/verify-clean09',
    '/tmp/unsigned-clean085.apk': '/tmp/unsigned-clean09.apk',
    '/tmp/Projectivy-CLEAN-08-5-Raw-Alpha-Process-Reload.apk': '/tmp/Projectivy-CLEAN-09-Movie-Episode-Playback.apk',
    '/tmp/clean085-artifact': '/tmp/clean09-artifact',
    '/tmp/build-clean085-stage.sh': '/tmp/build-clean09-stage.sh',
}
for old_value, new_value in repls.items():
    if old_value not in src:
        raise SystemExit(f'expected CLEAN085 build anchor missing: {old_value}')
    src = src.replace(old_value, new_value)

Path('/tmp/build-clean09-wrapper.sh').write_text(src)
PY

bash /tmp/build-clean09-wrapper.sh

test -f /tmp/Projectivy-CLEAN-09-Movie-Episode-Playback.apk
mkdir -p /tmp/clean09-artifact
cp -f /tmp/Projectivy-CLEAN-09-Movie-Episode-Playback.apk /tmp/clean09-artifact/
