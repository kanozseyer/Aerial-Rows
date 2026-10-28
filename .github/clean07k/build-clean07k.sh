#!/usr/bin/env bash
set -euo pipefail

# CLEAN 07K is CLEAN 07H plus one exact category_visible callback hook.
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean07f/build-clean07f.sh').read_text()
repls={
    '.github/clean07f/ChannelVisualBridge.java':'.github/clean07h/ChannelVisualBridge.java',
    '.github/clean07f/patch-native-multiselect.py':'.github/clean07k/patch-native-multiselect.py',
    '.github/clean07f/verify-clean07f.py':'.github/clean07k/verify-clean07k.py',
    '/tmp/verify-clean07f':'/tmp/verify-clean07k',
    '/tmp/unsigned-clean07f.apk':'/tmp/unsigned-clean07k.apk',
    '/tmp/Projectivy-CLEAN-07F-Stock-Native-MultiSelect.apk':'/tmp/Projectivy-CLEAN-07K-Exact-Visible-Sync.apk',
    '/tmp/clean07f-artifact':'/tmp/clean07k-artifact',
    '/tmp/build-clean07f-generated.sh':'/tmp/build-clean07k-generated.sh',
}
for old,new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN07F build anchor missing: {old}')
    src=src.replace(old,new)
Path('/tmp/build-clean07k-wrapper.sh').write_text(src)
PY

bash /tmp/build-clean07k-wrapper.sh
