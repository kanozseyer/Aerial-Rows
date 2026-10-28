#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean07f/build-clean07f.sh').read_text()
repls={
    '.github/clean07f/ChannelVisualBridge.java':'.github/clean07j/ChannelVisualBridge.java',
    '.github/clean07f/patch-native-multiselect.py':'.github/clean07j/patch-native-multiselect.py',
    '.github/clean07f/verify-clean07f.py':'.github/clean07j/verify-clean07j.py',
    '/tmp/verify-clean07f':'/tmp/verify-clean07j',
    '/tmp/unsigned-clean07f.apk':'/tmp/unsigned-clean07j.apk',
    '/tmp/Projectivy-CLEAN-07F-Stock-Native-MultiSelect.apk':'/tmp/Projectivy-CLEAN-07J-Live-Adapter-Resync.apk',
    '/tmp/clean07f-artifact':'/tmp/clean07j-artifact',
    '/tmp/build-clean07f-generated.sh':'/tmp/build-clean07j-generated.sh',
}
for old,new in repls.items():
    if old not in src: raise SystemExit(f'expected CLEAN07F anchor missing: {old}')
    src=src.replace(old,new)
Path('/tmp/build-clean07j-wrapper.sh').write_text(src)
PY
bash /tmp/build-clean07j-wrapper.sh
