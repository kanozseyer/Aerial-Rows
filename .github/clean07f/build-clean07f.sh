#!/usr/bin/env bash
set -euo pipefail

# CLEAN 07F inherits the proven CLEAN 07D channel engine and replaces only the
# grouped Red Light presentation/interaction layer. It keeps the stock Projectivy
# panel, removes OBJ text, and patches Projectivy's Leanback ie2/he2 path so that
# the bound Red Light selector uses native multi-choice behavior.
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean07/build-clean07.sh').read_text()
repls={
    '.github/clean07/ChannelVisualBridge.java':'.github/clean07f/ChannelVisualBridge.java',
    '.github/clean07/verify-clean07.py':'.github/clean07f/verify-clean07f.py',
    '/tmp/verify-clean07':'/tmp/verify-clean07f',
    '/tmp/unsigned-clean07d.apk':'/tmp/unsigned-clean07f.apk',
    '/tmp/Projectivy-CLEAN-07D-RedLight-All-Rows-Checkbox-Picker.apk':'/tmp/Projectivy-CLEAN-07F-Stock-Native-MultiSelect.apk',
    '/tmp/clean07d-artifact':'/tmp/clean07f-artifact',
}
for old,new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN07D build anchor missing: {old}')
    src=src.replace(old,new)

# clean07/build-clean07.sh itself generates the final build script, so inject
# CLEAN07F into that generator's existing grouped-UI hook rather than looking
# for the generated command directly.
old="src=src.replace(anchor, anchor+'python3 .github/clean07/patch-grouped-channel-ui.py /tmp/pj\\n',1)"
new="src=src.replace(anchor, anchor+'python3 .github/clean07/patch-grouped-channel-ui.py /tmp/pj\\npython3 .github/clean07f/patch-native-multiselect.py /tmp/pj\\n',1)"
if old not in src:
    raise SystemExit('CLEAN07 grouped UI generator anchor missing')
src=src.replace(old,new,1)

Path('/tmp/build-clean07f-generated.sh').write_text(src)
PY

bash /tmp/build-clean07f-generated.sh
