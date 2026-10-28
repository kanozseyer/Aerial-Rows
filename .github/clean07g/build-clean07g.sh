#!/usr/bin/env bash
set -euo pipefail

# CLEAN07G inherits CLEAN07F's verified native multi-select implementation and
# changes only the he2 post-navigation state handling: after Projectivy opens a
# channel's customization page, the displayed Set is restored from real Visible
# flags instead of retaining the temporary native checkbox mutation.
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean07f/build-clean07f.sh').read_text()
repls={
    '.github/clean07f/patch-native-multiselect.py':'.github/clean07g/patch-native-multiselect.py',
    '.github/clean07f/verify-clean07f.py':'.github/clean07g/verify-clean07g.py',
    '/tmp/verify-clean07f':'/tmp/verify-clean07g',
    '/tmp/unsigned-clean07f.apk':'/tmp/unsigned-clean07g.apk',
    '/tmp/Projectivy-CLEAN-07F-Stock-Native-MultiSelect.apk':'/tmp/Projectivy-CLEAN-07G-Authoritative-Visibility.apk',
    '/tmp/clean07f-artifact':'/tmp/clean07g-artifact',
    '/tmp/build-clean07f-generated.sh':'/tmp/build-clean07g-generated.sh',
}
for old,new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN07F build anchor missing: {old}')
    src=src.replace(old,new)
Path('/tmp/build-clean07g-wrapper.sh').write_text(src)
PY

bash /tmp/build-clean07g-wrapper.sh
