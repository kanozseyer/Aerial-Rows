#!/usr/bin/env bash
set -euo pipefail

# CLEAN 07E inherits the proven CLEAN 07D pipeline and changes presentation only:
# compile the 07E bridge, use the 07E verifier/output names, and leave the
# CLEAN07 dynamic discovery/channel model untouched.
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean07/build-clean07.sh').read_text()
repls={
    '.github/clean07/ChannelVisualBridge.java':'.github/clean07e/ChannelVisualBridge.java',
    '.github/clean07/verify-clean07.py':'.github/clean07e/verify-clean07e.py',
    '/tmp/verify-clean07':'/tmp/verify-clean07e',
    '/tmp/unsigned-clean07d.apk':'/tmp/unsigned-clean07e.apk',
    '/tmp/Projectivy-CLEAN-07D-RedLight-All-Rows-Checkbox-Picker.apk':'/tmp/Projectivy-CLEAN-07E-Stock-Dialog-MultiSelect.apk',
    '/tmp/clean07d-artifact':'/tmp/clean07e-artifact',
}
for old,new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN07D build anchor missing: {old}')
    src=src.replace(old,new)
Path('/tmp/build-clean07e-generated.sh').write_text(src)
PY

bash /tmp/build-clean07e-generated.sh
