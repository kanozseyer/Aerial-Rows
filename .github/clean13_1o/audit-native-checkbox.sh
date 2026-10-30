#!/usr/bin/env bash
set -euo pipefail

I_APK=/tmp/Projectivy-CLEAN-13-1I-Weather-Order-Only.apk
APKTOOL=/tmp/apktool.jar
DECODED=/tmp/pj-clean13-1o-audit
OUT=/tmp/clean13-1o-audit

# Rebuild the exact approved I artifact first. This ensures the audit is against
# the same frozen native Projectivy DEX baseline, not a stale downloaded APK.
bash .github/clean13_1i/build-clean13-1i.sh
test -s "$I_APK"
test -s "$APKTOOL"

rm -rf "$DECODED" "$OUT"
mkdir -p "$OUT/classes"
sha256sum "$I_APK" > "$OUT/I-apk-sha256.txt"

java -jar "$APKTOOL" d -r -f "$I_APK" -o "$DECODED" > "$OUT/apktool-decode.log" 2>&1

# Preserve the exact class files for independent inspection.
for cls in cb3 hv1 yh3 lu1 iv1 mu1 pg qg hj3 ta3; do
  src="$(find "$DECODED" -type f -name "$cls.smali" -print -quit)"
  if [ -n "$src" ]; then
    cp "$src" "$OUT/classes/$cls.smali"
  else
    printf 'NOT FOUND: %s.smali\n' "$cls" >> "$OUT/missing-classes.txt"
  fi
done

python3 - "$DECODED" "$OUT/native-checkbox-methods.txt" <<'PY'
from pathlib import Path
import re, sys

root = Path(sys.argv[1])
out = Path(sys.argv[2])
needles = (
    'Lhv1;', 'Lcb3;->q0', 'Lcb3;->r0', 'Lcb3;->z0',
    'Lcb3;->t0', 'Llu1;->h(Z)', 'Liv1;->c()',
    'Show date', 'Show Date', 'show_date'
)
class_names = {'cb3', 'hv1', 'yh3', 'lu1', 'iv1', 'mu1', 'pg', 'qg', 'hj3', 'ta3'}
chunks = []
refs = []
for path in sorted(root.rglob('*.smali')):
    name = path.stem
    text = path.read_text(errors='replace')
    lines = text.splitlines()
    if name in class_names:
        chunks.append(f'\n===== CLASS {name} : {path.relative_to(root)} =====\n')
        if name == 'hv1' or name == 'yh3':
            chunks.extend(lines)
        else:
            in_method = False
            block = []
            header = ''
            for line in lines:
                if line.startswith('.method '):
                    in_method = True
                    block = [line]
                    header = line
                    continue
                if in_method:
                    block.append(line)
                    if line.strip() == '.end method':
                        body = '\n'.join(block)
                        selected = (
                            name in {'cb3'} and any(x in header for x in
                                (' q0(', ' r0(', ' z0(', ' t0(', ' J0(', ' <init>('))
                        ) or (
                            name in {'lu1'} and any(x in header for x in
                                (' h(', ' <init>('))
                        ) or (
                            name in {'iv1'} and any(x in header for x in
                                (' c(', ' <init>('))
                        ) or (
                            name in {'pg', 'qg'} and any(x in body for x in needles)
                        ) or (
                            name in {'mu1', 'hj3', 'ta3'} and any(x in body for x in needles)
                        )
                        if selected:
                            chunks.extend(block)
                            chunks.append('')
                        in_method = False
            # Include class field declarations to identify backing fields.
            if name in {'hv1', 'yh3', 'lu1', 'iv1'}:
                fields = [x for x in lines if x.startswith('.field ')]
                chunks.extend(['-- fields --'] + fields)

    for i, line in enumerate(lines):
        if any(n in line for n in needles):
            start, end = max(0, i-12), min(len(lines), i+13)
            refs.append(f'\n===== REFERENCE {path.relative_to(root)}:{i+1} =====\n')
            refs.extend(f'{j+1}: {lines[j]}' for j in range(start, end))

chunks.append('\n===== ALL NATIVE CHECKBOX REFERENCES =====\n')
chunks.extend(refs)
out.write_text('\n'.join(chunks))
print(f'Wrote {out} ({out.stat().st_size} bytes; {len(refs)} reference lines)')
PY

# Small machine-readable inventory plus the focused smali excerpts.
find "$DECODED" -type f -name '*.smali' | wc -l > "$OUT/smali-file-count.txt"
grep -R -n -E 'new-instance .*Lhv1;|invoke-direct .*Lhv1;-><init>|Lcb3;->q0|Lcb3;->r0|Lcb3;->z0|Llu1;->h\(Z\)|Liv1;->c\(\)' "$DECODED" > "$OUT/native-checkbox-call-sites.txt" || true
test -s "$OUT/native-checkbox-methods.txt"

cd /tmp
zip -qr clean13-1o-audit.zip clean13-1o-audit
echo "AUDIT_ZIP=/tmp/clean13-1o-audit.zip"
