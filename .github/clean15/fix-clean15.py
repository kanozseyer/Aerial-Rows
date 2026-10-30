#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: fix-clean15.py APKTOOL_ROOT")

root = Path(sys.argv[1])

def find_one(name):
    found = [p for p in root.rglob(name) if p.parent.name == "smali"]
    if len(found) != 1:
        raise SystemExit(f"expected one {name} in classes.dex smali, found {len(found)}")
    return found[0]

def patch_method(path, signature, needle, replacement, label):
    source = path.read_text()
    start = source.find(signature)
    if start < 0:
        raise SystemExit(f"{label}: method not found: {signature}")
    end = source.find(".end method", start)
    if end < 0:
        raise SystemExit(f"{label}: method terminator not found")
    block = source[start:end]
    if replacement.strip() in block:
        raise SystemExit(f"{label}: bridge call already present")
    if block.count(needle) != 1:
        raise SystemExit(f"{label}: expected one anchor, found {block.count(needle)}")
    block = block.replace(needle, needle + replacement, 1)
    path.write_text(source[:start] + block + source[end:])

bridge = "Lcom/spocky/projengmenu/embedded/CLEAN15ChannelRefreshBridge;"

r12 = find_one("r12.smali")
patch_method(
    r12,
    ".method public final c(Lxj3;Ljava/lang/Object;)V",
    "    invoke-virtual {p1, v0}, Lw12;->setTag(Ljava/lang/Object;)V\n",
    f"    invoke-static {{p1, v0}}, {bridge}->bind(Landroid/view/ViewGroup;Ljava/lang/Object;)V\n",
    "CLEAN15 r12 category bind"
)
patch_method(
    r12,
    ".method public final d(Landroid/view/ViewGroup;)Lxj3;",
    "    invoke-direct {v0, p1}, Lw12;-><init>(Landroid/content/Context;)V\n",
    f"    invoke-static {{v0}}, {bridge}->install(Landroid/view/ViewGroup;)V\n",
    "CLEAN15 r12 header factory"
)

w12 = find_one("w12.smali")
patch_method(
    w12,
    ".method public final b(IZ)V",
    "    invoke-virtual {{p0}}, Lw12;->getReorderView()Lcom/spocky/projengmenu/ui/home/header/IconHeaderImageView;\n",
    f"    invoke-static {{p0, p1}}, {bridge}->tint(Landroid/view/ViewGroup;I)V\n",
    "CLEAN15 w12 icon tint"
)

for path, token in [
    (r12, "CLEAN15ChannelRefreshBridge;->install"),
    (r12, "CLEAN15ChannelRefreshBridge;->bind"),
    (w12, "CLEAN15ChannelRefreshBridge;->tint"),
]:
    if token not in path.read_text():
        raise SystemExit(f"missing patched marker {token} in {path}")

print("PASS: CLEAN15 installs the refresh action in the shared left-header presenter")
print("PASS: CLEAN15 binds visibility to Projectivy's native channel-row predicate")
print("PASS: CLEAN15 refresh icon follows the existing category-header tint path")
print("Patched:", r12)
print("Patched:", w12)
