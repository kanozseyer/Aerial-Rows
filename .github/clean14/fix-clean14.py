#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: fix-clean14.py APKTOOL_ROOT")

root = Path(sys.argv[1])
candidates = [p for p in root.rglob("lu.smali")
              if p.parent.name == "smali"]
if len(candidates) != 1:
    raise SystemExit(f"expected one classes.dex lu.smali, found {len(candidates)}")

path = candidates[0]
source = path.read_text()
signature = ".method public h(Landroid/view/ViewGroup;)Lhy3;"
start = source.find(signature)
if start < 0:
    raise SystemExit("shared HorizontalGridView presenter factory lu.h not found")

end = source.find(".end method", start)
if end < 0:
    raise SystemExit("lu.h method terminator not found")

block = source[start:end]
marker = "Lcom/spocky/projengmenu/clean14/CLEAN14CarouselBridge;->attach(Landroid/view/ViewGroup;)V"
if marker in block:
    raise SystemExit("CLEAN14 bridge call already exists in lu.h")

# DEX audit: v3 is the HorizontalGridView returned by vi2.getGridView().
# It remains live through the final return-object v1 in this method.
required = [
    ".locals 6",
    "getGridView()Landroidx/leanback/widget/HorizontalGridView;",
    "invoke-virtual {v3, p1}, Lbu;->setWindowAlignment(I)V",
    "invoke-virtual {v3, p1}, Lbu;->setWindowAlignmentOffset(I)V",
    "return-object v1",
]
for token in required:
    if token not in block:
        raise SystemExit("lu.h audit anchor missing: " + token)

call = (
    "    invoke-static {v3}, "
    "Lcom/spocky/projengmenu/clean14/CLEAN14CarouselBridge;"
    "->attach(Landroid/view/ViewGroup;)V\n"
)
needle = "    return-object v1\n"
if block.count(needle) != 1:
    raise SystemExit(f"expected one lu.h return-object v1, found {block.count(needle)}")

patched_block = block.replace(needle, call + needle, 1)
patched = source[:start] + patched_block + source[end:]
path.write_text(patched)

# Confirm that the injection is unique and is inside the shared row factory.
if patched.count(marker) != 1:
    raise SystemExit("CLEAN14 bridge call count is not exactly one")
print("PASS: CLEAN14 shared carousel bridge attached in lu.h:", path)
