#!/usr/bin/env bash
set -euo pipefail

P_APK=/tmp/Projectivy-CLEAN-13-1P-Show-Date-Native-Checkbox.apk
APKTOOL=/tmp/apktool.jar
DECODED=/tmp/clean14-projectivy-decoded
OUT=/tmp/clean14-row-audit
REPORT="$OUT/CLEAN14-ROW-AUDIT.txt"

# Rebuild the exact user-approved P stack. Do not build from main or an older baseline.
bash .github/clean13_1p/build-clean13-1p.sh
test -s "$P_APK"
test -s "$APKTOOL"

rm -rf "$DECODED" "$OUT"
mkdir -p "$OUT"
sha256sum "$P_APK" > "$OUT/P-APK-SHA256.txt"
unzip -t "$P_APK" > "$OUT/P-APK-ZIP-TEST.txt"
java -jar "$APKTOOL" d -r -f "$P_APK" -o "$DECODED" > "$OUT/apktool-decode.txt" 2>&1

python3 - "$DECODED" "$REPORT" <<'PY'
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
report = Path(sys.argv[2])
smali_dirs = sorted(p for p in root.glob("smali*") if p.is_dir())
interesting = re.compile(
    r"HorizontalGridView|BaseGridView|GridLayoutManager|"
    r"setWindowAlignment|setWindowAlignmentOffset|setItemAlignment|"
    r"setFocusScrollStrategy|setSelectedPosition|setOnChildViewHolderSelectedListener|"
    r"setOnScrollListener|smoothScrollToPosition|scrollToPosition|smoothScrollBy|"
    r"scrollBy|requestChildFocus|focusSearch|onRequestChildFocus|"
    r"WindowAlignment|ItemAlignment|setExtraLayoutSpace|"
    r"onScroll|onTouch|onKey|onInterceptTouchEvent|focusSearch|"
    r"dispatchKeyEvent|setSelectedPositionSmooth|getSelectedPosition|"
    r"setOverScrollMode|EdgeEffect|translationX|ViewPropertyAnimator|"
    r"setOn[A-Za-z]+Listener|setOnTouchInterceptListener",
    re.I,
)
target_classes = {
    "jo4", "f23", "lu", "hb3", "ui2",
    "io4", "ho4", "nf0", "ku", "si2", "vi2",
    "bu", "j3", "jv", "rw1", "mi",
}
leanback_classes = {
    "HorizontalGridView", "BaseGridView", "GridLayoutManager",
    "WindowAlignment", "ItemAlignmentFacet", "GridLayoutManager$Grid",
}

out = []
def add(s=""):
    out.append(str(s))

add("CLEAN14 UNIVERSAL CAROUSEL — PROJECTIVY ROW/FOCUS AUDIT")
add("=" * 68)
add(f"Decoded APK: {root}")
add("Source baseline: rebuilt from frozen CLEAN13.1P")
add("Target package: com.spocky.projengmenu, v4.72 / code 97")
add()
add("SMALI DEX DIRECTORIES")
for d in smali_dirs:
    add(f"- {d.name}")
add()

# Exact target class files and all methods that contain relevant row/scroll calls.
add("PROJECTIVY ROW/PRESENTER CLASSES")
for d in smali_dirs:
    for cls in sorted(target_classes):
        for f in d.rglob(cls + ".smali"):
            lines = f.read_text(errors="replace").splitlines()
            add()
            add(f"### {f.relative_to(root)} ({len(lines)} lines)")
            method = []
            method_name = None
            method_start = 0
            selected = []
            for n, line in enumerate(lines, 1):
                if line.startswith(".method "):
                    method = [line]
                    method_name = line
                    method_start = n
                elif method:
                    method.append(line)
                    if line.strip() == ".end method":
                        if any(interesting.search(x) for x in method):
                            selected.append((method_start, method.copy()))
                        method = []
                        method_name = None
            add("Relevant method blocks:")
            if not selected:
                add("(No direct HorizontalGridView/scroll API references in this class.)")
            for start, block in selected:
                add(f"-- method begins line {start} --")
                add("\n".join(f"{start+i}: {line}" for i, line in enumerate(block)))
            # Keep the exact class header and field declarations for inheritance/context.
            add("Class header and fields:")
            for n, line in enumerate(lines, 1):
                if line.startswith(".class ") or line.startswith(".super ") or line.startswith(".implements ") or line.startswith(".field "):
                    add(f"{n}: {line}")

add()
add("LEANBACK GRID/ALIGNMENT IMPLEMENTATION CLASSES")
found = 0
for d in smali_dirs:
    for f in sorted(d.rglob("*.smali")):
        if f.stem not in leanback_classes:
            continue
        found += 1
        lines = f.read_text(errors="replace").splitlines()
        add()
        add(f"### {f.relative_to(root)} ({len(lines)} lines)")
        method = []
        start = 0
        selected = []
        for n, line in enumerate(lines, 1):
            if line.startswith(".method "):
                method = [line]
                start = n
            elif method:
                method.append(line)
                if line.strip() == ".end method":
                    if any(interesting.search(x) for x in method):
                        selected.append((start, method.copy()))
                    method = []
        if not selected:
            add("(No methods matched the row/scroll API filter.)")
        for start, block in selected:
            add(f"-- method begins line {start} --")
            add("\n".join(f"{start+i}: {line}" for i, line in enumerate(block)))
if not found:
    add("No un-obfuscated Leanback grid implementation classes were found by filename.")

add()
add("GLOBAL HORIZONTAL-GRID REFERENCES")
matches = []
for d in smali_dirs:
    for f in d.rglob("*.smali"):
        try:
            lines = f.read_text(errors="replace").splitlines()
        except Exception:
            continue
        for n, line in enumerate(lines, 1):
            if interesting.search(line):
                matches.append((str(f.relative_to(root)), n, line.strip()))
for path, n, line in matches[:600]:
    add(f"{path}:{n}: {line}")
add(f"Total matching smali lines: {len(matches)} (listed up to 600)")
add()

add("ROW PRESENTER SELECTION / CLASS INVENTORY")
for d in smali_dirs:
    for cls in ("ui2", "jo4", "f23", "lu", "hb3", "po2", "eb3", "oo2"):
        for f in d.rglob(cls + ".smali"):
            lines = f.read_text(errors="replace").splitlines()
            add(f"{f.relative_to(root)}: {len(lines)} lines")
            for n, line in enumerate(lines, 1):
                if line.startswith((".class ", ".super ", ".implements ")):
                    add(f"  {n}: {line}")
add()

add("AUDIT NOTES")
add("- No source changes or APK modifications were made by this audit.")
add("- The audit intentionally identifies the shared row presenter/grid path before choosing a patch point.")
add("- CLEAN14 must preserve Projectivy's vertical focus, card scaling, clicks, and existing Red Light behaviour.")
report.write_text("\n".join(out) + "\n")
print(f"Wrote {report} ({report.stat().st_size} bytes, {len(out)} output lines)")
PY

test -s "$REPORT"
wc -lc "$REPORT"
grep -E 'PROJECTIVY ROW/PRESENTER|LEANBACK GRID|GLOBAL HORIZONTAL|Total matching|jo4.smali|f23.smali|HorizontalGridView' "$REPORT" | head -100
