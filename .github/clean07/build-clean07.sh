#!/usr/bin/env bash
set -euo pipefail

# CLEAN 07 deliberately inherits the proven CLEAN 06D packaging/signing pipeline.
# Generate a temporary build script by replacing only the CLEAN 07 stage-specific
# source/manifest/verifier/output names, then execute that generated script.
python3 - <<'PY'
from pathlib import Path
src=Path('.github/clean06/build-clean06.sh').read_text()
repls={
    'UNSIGNED=/tmp/unsigned-clean06.apk':'UNSIGNED=/tmp/unsigned-clean07.apk',
    'FINAL=/tmp/Projectivy-CLEAN-06-RedLight-One-Row.apk':'FINAL=/tmp/Projectivy-CLEAN-07-RedLight-All-Rows.apk',
    'ART=/tmp/clean06-artifact':'ART=/tmp/clean07-artifact',
    '.github/clean06/patch-manifest.py':'.github/clean07/patch-manifest.py',
    '.github/clean06/RedLightDiscoveryEngine.java':'.github/clean07/RedLightDiscoveryEngine.java',
    '.github/clean06/RedLightDiscoveryProvider.java':'.github/clean07/RedLightDiscoveryProvider.java',
    "'CLEAN06 SUCCESS'":"'CLEAN07 SUCCESS'",
    '/tmp/verify-clean06':'/tmp/verify-clean07',
    '.github/clean06/verify-clean06.py':'.github/clean07/verify-clean07.py',
}
for old,new in repls.items():
    if old not in src:
        raise SystemExit(f'expected CLEAN06 build anchor missing: {old}')
    src=src.replace(old,new)
Path('/tmp/build-clean07-generated.sh').write_text(src)
PY

bash /tmp/build-clean07-generated.sh
