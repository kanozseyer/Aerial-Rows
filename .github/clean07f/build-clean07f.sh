#!/usr/bin/env bash
set -euo pipefail

# CLEAN 07F inherits the proven CLEAN 07D packaging/channel pipeline but replaces
# only the grouped-channel presentation bridge and patches Projectivy's existing
# Leanback selector (ie2/he2) to use its native multi adapter for Red Light
# visibility. No discovery, playback, boot, job or Kodi-start behaviour changes.
python3 - <<'PY'
from pathlib import Path
src = Path('.github/clean07/build-clean07.sh').read_text()
repls = {
    '.github/clean07/ChannelVisualBridge.java': '.github/clean07f/ChannelVisualBridge.java',
    '.github/clean07/verify-clean07.py': '.github/clean07f/verify-clean07f.py',
    '/tmp/verify-clean07': '/tmp/verify-clean07f',
    '/tmp/unsigned-clean07d.apk': '/tmp/unsigned-clean07f.apk',
    '/tmp/Projectivy-CLEAN-07D-RedLight-All-Rows-Checkbox-Picker.apk': '/tmp/Projectivy-CLEAN-07F-Native-Visibility-MultiSelect.apk',
    '/tmp/clean07d-artifact': '/tmp/clean07f-artifact',
}
for old, new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN07D build anchor missing: {old}')
    src = src.replace(old, new)

# Add the CLEAN07F native Leanback-dialog patch immediately after the existing
# grouped preference registration hook and before apktool rebuilds classes2.dex.
old = 'python3 .github/clean07/patch-grouped-channel-ui.py /tmp/pj\\n'
new = old + 'python3 .github/clean07f/patch-native-visibility-multiselect.py /tmp/pj\\n'
if old not in src:
    raise SystemExit('CLEAN07 grouped UI patch anchor missing')
src = src.replace(old, new, 1)

Path('/tmp/build-clean07f-generated.sh').write_text(src)
PY

bash /tmp/build-clean07f-generated.sh
