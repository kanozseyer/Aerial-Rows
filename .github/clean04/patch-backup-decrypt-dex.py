#!/usr/bin/env python3
from pathlib import Path
import importlib.util
import sys

BASE = Path(__file__).parents[1] / 'patch-clean-01g-surgical-dex.py'
spec = importlib.util.spec_from_file_location('dexbase', BASE)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

TARGET_CLASS = 'Ls71;'
TARGET_METHOD = ('Ls71;', 'b', '([B)[B')
FIELD = ('Lcom/spocky/projengmenu/services/ProjectivyAccessibilityService;', 's0', 'B')


def contains_class(raw, desc):
    d = mod.Dex(raw)
    try:
        d.class_def(desc)
        return True
    except AssertionError:
        return False


def patch_one(raw):
    b = bytearray(raw)
    d = mod.Dex(raw)
    methods = [m for m in d.class_methods(TARGET_CLASS) if m[1] == TARGET_METHOD]
    assert len(methods) == 1, methods
    code = methods[0][3]
    words = d.code_words(code)
    field = d.field_index(*FIELD)

    hits = [i for i in range(len(words)-1) if (words[i] & 0xff) == 0x64 and words[i+1] == field]
    assert len(hits) == 1, hits
    pc = hits[0]
    dest = (words[pc] >> 8) & 0xff
    assert dest == 0, dest

    # Stock 4.72: sget-byte v0, s0 ; const/16 v1, 0x10 ; add-int/2addr v0,v1
    assert (words[pc+2] & 0xff) == 0x13, hex(words[pc+2])
    assert ((words[pc+2] >> 8) & 0xff) == 1, hex(words[pc+2])
    assert words[pc+3] == 0x0010, hex(words[pc+3])
    assert (words[pc+4] & 0xff) == 0xb0, hex(words[pc+4])

    off = code + 16 + pc * 2
    # Same-width replacement: const/16 v0, 0x10 (2 code units), keep the
    # existing const/16 v1,0x10, then nop the now-unwanted add-int/2addr.
    mod.put_u2(b, off, 0x0013)
    mod.put_u2(b, off + 2, 0x0010)
    mod.put_u2(b, off + 8, 0x0000)

    mod.fix_header(b)
    mod.verify_header(b)
    changed = sum(x != y for x, y in zip(raw, b))
    assert changed <= 48, changed
    return bytes(b), changed


def main():
    if len(sys.argv) != 5:
        raise SystemExit('usage: patch-backup-decrypt-dex.py IN_CLASSES IN_CLASSES2 OUT_CLASSES OUT_CLASSES2')
    paths = [Path(sys.argv[1]), Path(sys.argv[2])]
    outs = [Path(sys.argv[3]), Path(sys.argv[4])]
    raws = [p.read_bytes() for p in paths]
    where = [contains_class(r, TARGET_CLASS) for r in raws]
    assert sum(where) == 1, where
    for i, raw in enumerate(raws):
        if where[i]:
            patched, changed = patch_one(raw)
            outs[i].write_bytes(patched)
            print(f'PASS: CLEAN 04 patched {paths[i].name} s71.b to fixed 16-byte salt; changed bytes incl header={changed}; size={len(raw)}')
        else:
            outs[i].write_bytes(raw)
            print(f'PASS: preserved {paths[i].name} byte-for-byte; s71 not present')

if __name__ == '__main__':
    main()
