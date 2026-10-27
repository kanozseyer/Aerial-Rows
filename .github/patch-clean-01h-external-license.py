#!/usr/bin/env python3
from pathlib import Path
import importlib.util
import sys

BASE = Path(__file__).with_name('patch-clean-01g-surgical-dex.py')
spec = importlib.util.spec_from_file_location('clean01g', BASE)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)


def patch_rc1_h(src, dst):
    original = Path(src).read_bytes()
    b = bytearray(original)
    d = mod.Dex(original)
    methods = [m for m in d.class_methods('Lrc1;') if m[1] == ('Lrc1;', 'h', '()V')]
    assert len(methods) == 1, methods
    code = methods[0][3]
    first = mod.u2(b, code + 16)
    assert first != 0x000e, 'rc1.h already starts with return-void'
    mod.put_u2(b, code + 16, 0x000e)
    mod.fix_header(b)
    mod.verify_header(b)
    Path(dst).write_bytes(b)
    changed = sum(a != c for a, c in zip(original, b))
    print(f'classes2.dex: no-op rc1.h external-license refresh; changed bytes incl header={changed}; size={len(b)}')
    assert changed <= 40, changed


def main():
    if len(sys.argv) != 5:
        raise SystemExit('usage: patch-clean-01h-external-license.py IN_CLASSES IN_CLASSES2 OUT_CLASSES OUT_CLASSES2')
    in1, in2, out1, out2 = sys.argv[1:]
    tmp2 = str(Path(out2).with_suffix('.01g.tmp.dex'))
    mod.patch_classes1(in1, out1)
    mod.patch_classes2(in2, tmp2)
    patch_rc1_h(tmp2, out2)
    Path(tmp2).unlink(missing_ok=True)


if __name__ == '__main__':
    main()
