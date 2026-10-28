#!/usr/bin/env bash
set -euo pipefail

# CLEAN 07D deliberately inherits the proven CLEAN 06D packaging/signing pipeline.
# Generate a temporary build script by replacing only CLEAN 07 stage-specific
# sources/manifest/verifier/output names, then add the presentation-only grouped
# channel hook before rebuilding Projectivy.
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean06/build-clean06.sh').read_text()
repls={
    'UNSIGNED=/tmp/unsigned-clean06.apk':'UNSIGNED=/tmp/unsigned-clean07d.apk',
    '/tmp/unsigned-clean06.apk':'/tmp/unsigned-clean07d.apk',
    'FINAL=/tmp/Projectivy-CLEAN-06-RedLight-One-Row.apk':'FINAL=/tmp/Projectivy-CLEAN-07D-RedLight-All-Rows-Checkbox-Picker.apk',
    'ART=/tmp/clean06-artifact':'ART=/tmp/clean07d-artifact',
    '.github/clean06/patch-manifest.py':'.github/clean07/patch-manifest.py',
    '.github/clean06/RedLightDiscoveryEngine.java':'.github/clean07/RedLightDiscoveryEngine.java',
    '.github/clean06/RedLightDiscoveryProvider.java':'.github/clean07/RedLightDiscoveryProvider.java',
    "'CLEAN06 SUCCESS'":"'CLEAN07 SUCCESS'",
    "'redlight:shelf:trending:movies'":"'redlight:shelf:'",
    '/tmp/verify-clean06':'/tmp/verify-clean07',
    '.github/clean06/verify-clean06.py':'.github/clean07/verify-clean07.py',
}
for old,new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN06 build anchor missing: {old}')
    src=src.replace(old,new)

# Hook the stock Projectivy grouped-channel preference after the decoded manifest
# patch but before apktool rebuild.
anchor='python3 .github/clean07/patch-manifest.py /tmp/pj\n'
if anchor not in src:
    raise SystemExit('CLEAN07 manifest patch anchor missing')
src=src.replace(anchor, anchor+'python3 .github/clean07/patch-grouped-channel-ui.py /tmp/pj\n',1)

# Compile the presentation bridge into the existing embedded classes4.dex.
anchor='.github/clean07/RedLightDiscoveryProvider.java\n'
if anchor not in src:
    raise SystemExit('CLEAN07 javac source anchor missing')
src=src.replace(anchor,'.github/clean07/RedLightDiscoveryProvider.java .github/clean07/ChannelVisualBridge.java\n',1)

# g70 lives in classes2.dex. Preserve apktool's rebuilt classes2 containing the
# one smali hook instead of discarding it for classes2-lean, then apply CLEAN04's
# fixed 16-byte backup decrypt patch to this rebuilt classes2.
old='unzip -p /tmp/rebuilt.apk classes.dex > /tmp/classes-clean06.dex; unzip -p /tmp/rebuilt.apk AndroidManifest.xml > /tmp/AndroidManifest.xml\n'
new='unzip -p /tmp/rebuilt.apk classes.dex > /tmp/classes-clean06.dex; unzip -p /tmp/rebuilt.apk classes2.dex > /tmp/classes2-clean07-ui.dex; unzip -p /tmp/rebuilt.apk AndroidManifest.xml > /tmp/AndroidManifest.xml\n'
if old not in src:
    raise SystemExit('rebuilt dex extraction anchor missing')
src=src.replace(old,new,1)
old='.github/clean04/patch-backup-decrypt-dex.py /tmp/classes-clean06.dex /tmp/classes2-lean.dex /tmp/classes-clean06-fixed.dex /tmp/classes2-clean06-fixed.dex'
new='.github/clean04/patch-backup-decrypt-dex.py /tmp/classes-clean06.dex /tmp/classes2-clean07-ui.dex /tmp/classes-clean06-fixed.dex /tmp/classes2-clean06-fixed.dex'
if old not in src:
    raise SystemExit('CLEAN04 classes2 input anchor missing')
src=src.replace(old,new,1)

Path('/tmp/build-clean07-generated.sh').write_text(src)
PY

bash /tmp/build-clean07-generated.sh
