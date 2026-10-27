#!/usr/bin/env python3
from pathlib import Path
import importlib.util
import sys

BASE = Path(__file__).with_name('patch-clean-01h-external-license.py')
spec = importlib.util.spec_from_file_location('clean01h', BASE)
h = importlib.util.module_from_spec(spec)
spec.loader.exec_module(h)
mod = h.mod


def nop_words(b, code_off, pc, n):
    off = code_off + 16 + pc * 2
    for i in range(n):
        mod.put_u2(b, off + i * 2, 0x0000)


def no_op_void_method(b, d, cls, name, proto):
    methods = [m for m in d.class_methods(cls) if m[1] == (cls, name, proto)]
    assert len(methods) == 1, (cls, name, proto, methods)
    code = methods[0][3]
    words = d.code_words(code)
    assert len(words) >= 2
    assert (words[0] & 0xff) == 0x62, (cls, name, hex(words[0]))
    mod.put_u2(b, code + 16, 0x000e)
    mod.put_u2(b, code + 18, 0x0000)
    return code


def nop_invoke_and_move_result(words, b, code, method_idx, opcode):
    hits = [i for i in range(len(words)-3) if (words[i] & 0xff) == opcode and words[i+1] == method_idx and (words[i+3] & 0xff) in (0x0a,0x0b,0x0c)]
    assert len(hits) == 1, (method_idx, opcode, hits)
    pc = hits[0]
    nop_words(b, code, pc, 4)
    return pc


def nop_all_invokes(words, b, code, method_idx, opcode):
    hits = [i for i in range(len(words)-2) if (words[i] & 0xff) == opcode and words[i+1] == method_idx]
    for pc in hits:
        nop_words(b, code, pc, 3)
    return hits


def patch_projectivy_telemetry(src, dst):
    original = Path(src).read_bytes()
    b = bytearray(original)
    d = mod.Dex(original)

    no_op_void_method(
        b, d,
        'Lcom/spocky/projengmenu/utils/PTUtils;',
        'e',
        '(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V'
    )

    no_op_void_method(
        b, d,
        'Lcom/spocky/projengmenu/PTApplication;',
        'e',
        '()V'
    )

    fa_get = d.method_index(
        'Lcom/google/firebase/analytics/FirebaseAnalytics;',
        'getInstance',
        '(Landroid/content/Context;)Lcom/google/firebase/analytics/FirebaseAnalytics;'
    )
    fa_prop = d.method_index(
        'Lcom/google/firebase/analytics/FirebaseAnalytics;',
        'a',
        '(Ljava/lang/String;Ljava/lang/String;)V'
    )
    obj_getclass = d.method_index('Ljava/lang/Object;', 'getClass', '()Ljava/lang/Class;')

    report = []

    methods = [m for m in d.class_methods('Lcom/spocky/projengmenu/displayProfiles/DisplayProfileManager;') if m[1] == ('Lcom/spocky/projengmenu/displayProfiles/DisplayProfileManager;', '<clinit>', '()V')]
    assert len(methods) == 1
    code = methods[0][3]; words = d.code_words(code)
    get_pc = nop_invoke_and_move_result(words, b, code, fa_get, 0x71)
    prop_hits = nop_all_invokes(words, b, code, fa_prop, 0x6e)
    assert len(prop_hits) == 1, prop_hits
    report.append(('DisplayProfileManager.<clinit>', get_pc, prop_hits))

    methods = [m for m in d.class_methods('Lkx3;') if m[1] == ('Lkx3;', 'b', '(Lzi0;)Ljava/lang/Object;')]
    assert len(methods) == 1
    code = methods[0][3]; words = d.code_words(code)
    get_pc = nop_invoke_and_move_result(words, b, code, fa_get, 0x71)
    prop_hits = nop_all_invokes(words, b, code, fa_prop, 0x6e)
    assert len(prop_hits) == 1, prop_hits
    report.append(('kx3.b', get_pc, prop_hits))

    # ya3's coroutine body is o(Object), not h(). Preserve all non-telemetry
    # startup state while removing only its direct FirebaseAnalytics calls.
    methods = [m for m in d.class_methods('Lya3;') if m[1] == ('Lya3;', 'o', '(Ljava/lang/Object;)Ljava/lang/Object;')]
    assert len(methods) == 1, methods
    code = methods[0][3]; words = d.code_words(code)
    get_pc = nop_invoke_and_move_result(words, b, code, fa_get, 0x71)
    getclass_hits = [i for i in range(get_pc+4, min(len(words)-2, get_pc+20)) if (words[i] & 0xff) == 0x6e and words[i+1] == obj_getclass]
    assert len(getclass_hits) == 1, getclass_hits
    nop_words(b, code, getclass_hits[0], 3)
    prop_hits = nop_all_invokes(words, b, code, fa_prop, 0x6e)
    assert len(prop_hits) == 5, prop_hits
    report.append(('ya3.o', get_pc, prop_hits))

    mod.fix_header(b)
    mod.verify_header(b)
    Path(dst).write_bytes(b)
    changed = sum(a != c for a, c in zip(original, b))
    print(f'classes.dex telemetry patch: changed bytes incl header={changed}; size={len(b)}')
    for row in report:
        print(' ', row)
    assert changed <= 160, changed


def main():
    if len(sys.argv) != 5:
        raise SystemExit('usage: patch-clean-01k-projectivy-telemetry.py IN_CLASSES IN_CLASSES2 OUT_CLASSES OUT_CLASSES2')
    in1, in2, out1, out2 = sys.argv[1:]
    tmp1 = str(Path(out1).with_suffix('.01h.tmp.dex'))
    tmp2a = str(Path(out2).with_suffix('.01g.tmp.dex'))
    tmp2b = str(Path(out2).with_suffix('.01h.tmp.dex'))

    mod.patch_classes1(in1, tmp1)
    mod.patch_classes2(in2, tmp2a)
    h.patch_rc1_h(tmp2a, tmp2b)

    patch_projectivy_telemetry(tmp1, out1)
    Path(tmp2b).replace(out2)
    Path(tmp1).unlink(missing_ok=True)
    Path(tmp2a).unlink(missing_ok=True)


if __name__ == '__main__':
    main()
