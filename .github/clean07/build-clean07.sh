#!/usr/bin/env bash
set -euo pipefail

# CLEAN 07B deliberately inherits the proven CLEAN 06D packaging/signing pipeline.
# Generate a temporary build script by replacing only CLEAN 07 stage-specific
# sources/manifest/verifier/output names, then add the presentation-only grouped
# channel hook before rebuilding Projectivy.
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean06/build-clean06.sh').read_text()
repls={
    'UNSIGNED=/tmp/unsigned-clean06.apk':'UNSIGNED=/tmp/unsigned-clean07b.apk',
    '/tmp/unsigned-clean06.apk':'/tmp/unsigned-clean07b.apk',
    'FINAL=/tmp/Projectivy-CLEAN-06-RedLight-One-Row.apk':'FINAL=/tmp/Projectivy-CLEAN-07B-RedLight-All-Rows-Channel-UI.apk',
    'ART=/tmp/clean06-artifact':'ART=/tmp/clean07b-artifact',
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

# Compile the tiny presentation bridge into the existing embedded classes4.dex.
anchor='.github/clean07/RedLightDiscoveryProvider.java\n'
if anchor not in src:
    raise SystemExit('CLEAN07 javac source anchor missing')
src=src.replace(anchor,'.github/clean07/RedLightDiscoveryProvider.java .github/clean07/ChannelVisualBridge.java\n',1)

Path('/tmp/build-clean07-generated.sh').write_text(src)
PY

bash /tmp/build-clean07-generated.sh
