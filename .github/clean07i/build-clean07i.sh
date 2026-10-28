#!/usr/bin/env bash
set -euo pipefail

# CLEAN 07I inherits CLEAN 07F's verified build pipeline and swaps in the
# CLEAN 07I bridge/patch/verifier. Behavior = CLEAN 07H plus selector onResume
# resync from real Projectivy Visible flags.
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean07f/build-clean07f.sh').read_text()
repls={
    '.github/clean07f/ChannelVisualBridge.java':'.github/clean07i/ChannelVisualBridge.java',
    '.github/clean07f/patch-native-multiselect.py':'.github/clean07i/patch-native-multiselect.py',
    '.github/clean07f/verify-clean07f.py':'.github/clean07i/verify-clean07i.py',
    '/tmp/verify-clean07f':'/tmp/verify-clean07i',
    '/tmp/unsigned-clean07f.apk':'/tmp/unsigned-clean07i.apk',
    '/tmp/Projectivy-CLEAN-07F-Stock-Native-MultiSelect.apk':'/tmp/Projectivy-CLEAN-07I-Resume-Visibility-Resync.apk',
    '/tmp/clean07f-artifact':'/tmp/clean07i-artifact',
    '/tmp/build-clean07f-generated.sh':'/tmp/build-clean07i-generated.sh',
}
for old,new in repls.items():
    if old not in src: raise SystemExit(f'expected CLEAN07F anchor missing: {old}')
    src=src.replace(old,new)
Path('/tmp/build-clean07i-wrapper.sh').write_text(src)
PY

bash /tmp/build-clean07i-wrapper.sh
