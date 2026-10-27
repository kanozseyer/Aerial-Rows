#!/usr/bin/env python3
from pathlib import Path
import hashlib
import struct
import sys
import zlib


def u2(b, o):
    return struct.unpack_from('<H', b, o)[0]


def u4(b, o):
    return struct.unpack_from('<I', b, o)[0]


def put_u2(b, o, v):
    struct.pack_into('<H', b, o, v)


def read_uleb(b, o):
    value = 0
    shift = 0
    while True:
        x = b[o]
        o += 1
        value |= (x & 0x7f) << shift
        if x < 0x80:
            return value, o
        shift += 7
        if shift > 35:
            raise ValueError('bad uleb128')


def read_ascii_string(b, o):
    _, o = read_uleb(b, o)
    end = b.index(0, o)
    return b[o:end].decode('utf-8', 'replace')


class Dex:
    def __init__(self, raw):
        self.b = raw
        if raw[:4] != b'dex\n':
            raise ValueError('not a DEX file')
        self.string_ids_size = u4(raw, 0x38)
        self.string_ids_off = u4(raw, 0x3c)
        self.type_ids_size = u4(raw, 0x40)
        self.type_ids_off = u4(raw, 0x44)
        self.proto_ids_size = u4(raw, 0x48)
        self.proto_ids_off = u4(raw, 0x4c)
        self.field_ids_size = u4(raw, 0x50)
        self.field_ids_off = u4(raw, 0x54)
        self.method_ids_size = u4(raw, 0x58)
        self.method_ids_off = u4(raw, 0x5c)
        self.class_defs_size = u4(raw, 0x60)
        self.class_defs_off = u4(raw, 0x64)
        self.strings = [read_ascii_string(raw, u4(raw, self.string_ids_off + i * 4)) for i in range(self.string_ids_size)]
        self.type_desc = [self.strings[u4(raw, self.type_ids_off + i * 4)] for i in range(self.type_ids_size)]

    def type_index(self, desc):
        hits = [i for i, x in enumerate(self.type_desc) if x == desc]
        if len(hits) != 1:
            raise AssertionError(f'expected one type {desc}, got {hits}')
        return hits[0]

    def proto(self, idx):
        o = self.proto_ids_off + idx * 12
        _shorty, ret, params = struct.unpack_from('<III', self.b, o)
        p = []
        if params:
            n = u4(self.b, params)
            p = [self.type_desc[u2(self.b, params + 4 + j * 2)] for j in range(n)]
        return '(' + ''.join(p) + ')' + self.type_desc[ret]

    def method(self, idx):
        o = self.method_ids_off + idx * 8
        cls, proto, name = struct.unpack_from('<HHI', self.b, o)
        return self.type_desc[cls], self.strings[name], self.proto(proto)

    def method_index(self, cls, name, proto):
        hits = [i for i in range(self.method_ids_size) if self.method(i) == (cls, name, proto)]
        if len(hits) != 1:
            raise AssertionError(f'expected one method {(cls,name,proto)}, got {hits}')
        return hits[0]

    def field(self, idx):
        o = self.field_ids_off + idx * 8
        cls, typ, name = struct.unpack_from('<HHI', self.b, o)
        return self.type_desc[cls], self.strings[name], self.type_desc[typ]

    def field_index(self, cls, name, typ):
        hits = [i for i in range(self.field_ids_size) if self.field(i) == (cls, name, typ)]
        if len(hits) != 1:
            raise AssertionError(f'expected one field {(cls,name,typ)}, got {hits}')
        return hits[0]

    def class_def(self, desc):
        tid = self.type_index(desc)
        hits = []
        for i in range(self.class_defs_size):
            vals = struct.unpack_from('<IIIIIIII', self.b, self.class_defs_off + i * 32)
            if vals[0] == tid:
                hits.append(vals)
        if len(hits) != 1:
            raise AssertionError(f'expected one class {desc}, got {len(hits)}')
        return hits[0]

    def class_methods(self, desc):
        vals = self.class_def(desc)
        o = vals[6]
        static_fields, o = read_uleb(self.b, o)
        instance_fields, o = read_uleb(self.b, o)
        direct_methods, o = read_uleb(self.b, o)
        virtual_methods, o = read_uleb(self.b, o)
        for count in (static_fields, instance_fields):
            idx = 0
            for _ in range(count):
                diff, o = read_uleb(self.b, o)
                idx += diff
                _flags, o = read_uleb(self.b, o)
        out = []
        for kind, count in (('direct', direct_methods), ('virtual', virtual_methods)):
            idx = 0
            for _ in range(count):
                diff, o = read_uleb(self.b, o)
                idx += diff
                flags, o = read_uleb(self.b, o)
                code_off, o = read_uleb(self.b, o)
                out.append((idx, self.method(idx), flags, code_off, kind))
        return out

    def code_words(self, code_off):
        if not code_off:
            return []
        n = u4(self.b, code_off + 12)
        return list(struct.unpack_from('<' + 'H' * n, self.b, code_off + 16))


def fix_header(b):
    b[12:32] = hashlib.sha1(bytes(b[32:])).digest()
    struct.pack_into('<I', b, 8, zlib.adler32(bytes(b[12:])) & 0xffffffff)


def verify_header(b):
    assert u4(b, 0x20) == len(b), 'DEX file_size mismatch'
    assert bytes(b[12:32]) == hashlib.sha1(bytes(b[32:])).digest(), 'DEX SHA-1 mismatch'
    assert u4(b, 8) == (zlib.adler32(bytes(b[12:])) & 0xffffffff), 'DEX Adler32 mismatch'


def patch_classes1(src, dst):
    original = Path(src).read_bytes()
    b = bytearray(original)
    d = Dex(original)

    # 1) Lit1.g() is exclusively the BillingClient connection gate. Make its
    # first one-code-unit instruction return-void. All following original
    # instructions stay aligned and verifier-valid but are unreachable.
    methods = [m for m in d.class_methods('Lit1;') if m[1] == ('Lit1;', 'g', '()V')]
    assert len(methods) == 1, methods
    code = methods[0][3]
    first = u2(b, code + 16)
    assert first == 0x2012, f'unexpected Lit1.g first instruction 0x{first:04x}'  # const/4 v0, 0x2
    put_u2(b, code + 16, 0x000e)  # return-void

    # 2) Lw40.<clinit>: change only the actual About list A, not the earlier
    # four-item list z. Anchor on PremiumCardActivity.e0 and Lw40.A.
    cl = [m for m in d.class_methods('Lw40;') if m[1] == ('Lw40;', '<clinit>', '()V')]
    assert len(cl) == 1, cl
    code = cl[0][3]
    words = d.code_words(code)
    list_type = d.type_index('[Ld90;')
    field_a = d.field_index('Lw40;', 'A', 'Ljava/util/List;')
    premium_field = d.field_index(
        'Lcom/spocky/projengmenu/ui/guidedActions/activities/about/PremiumCardActivity;',
        'e0', 'Lp;'
    )
    sput_a = [i for i in range(len(words)-1) if (words[i] & 0xff) == 0x69 and words[i+1] == field_a]
    sget_premium = [i for i in range(len(words)-1) if (words[i] & 0xff) == 0x62 and words[i+1] == premium_field]
    assert len(sput_a) == 1 and len(sget_premium) == 1, (sput_a, sget_premium)

    pattern = [
        0x4023, list_type,       # new-array v0, v4, [Ld90;
        0x0c4d, 0x0b00,         # aput-object v12, v0, v11 (index 0)
        0x0d4d, 0x0100,         # aput-object v13, v0, v1  (Premium)
        0x0e4d, 0x0200,         # aput-object v14, v0, v2  (Device)
        0x0f4d, 0x0300,         # aput-object v15, v0, v3  (Background)
    ]
    starts = [i for i in range(len(words)-len(pattern)+1) if words[i:i+len(pattern)] == pattern]
    eligible = [i for i in starts if sget_premium[0] < i < sput_a[0]]
    assert len(eligible) == 1, (starts, sget_premium, sput_a, eligible)
    pc = eligible[0]
    base = code + 16 + pc * 2
    # array size register v4 -> v3 (3 items)
    put_u2(b, base + 0*2, 0x3023)
    # position 1 becomes Device (v14), position 2 becomes Background (v15)
    put_u2(b, base + 4*2, 0x0e4d)
    put_u2(b, base + 6*2, 0x0f4d)
    # old fourth write would address index 3 of a three-element array: replace
    # its two code units with two nops.
    put_u2(b, base + 8*2, 0x0000)
    put_u2(b, base + 9*2, 0x0000)

    # Premium card object is now unused. Replace its sget-object v19 with a
    # same-width const/16 v19, 0 so PremiumCardActivity is not resolved at init.
    ppc = sget_premium[0]
    pbase = code + 16 + ppc * 2
    assert u2(b, pbase) == 0x1362 and u2(b, pbase+2) == premium_field
    put_u2(b, pbase, 0x1313)  # const/16 v19, 0
    put_u2(b, pbase+2, 0x0000)

    fix_header(b)
    verify_header(b)
    Path(dst).write_bytes(b)
    changed = sum(a != c for a, c in zip(original, b))
    print(f'classes.dex: patched Lit1.g and Lw40.A; changed bytes incl header={changed}; size={len(b)}')
    assert changed <= 64, changed


def patch_classes2(src, dst):
    original = Path(src).read_bytes()
    b = bytearray(original)
    d = Dex(original)
    premium_type = d.type_index('Lcom/spocky/projengmenu/ui/guidedActions/activities/about/PremiumActivity;')
    start_activity = d.method_index('Landroid/app/Activity;', 'startActivity', '(Landroid/content/Intent;)V')
    targets = [
        'Lcom/spocky/projengmenu/ui/launcherActivities/ImportThemeActivity;',
        'Lcom/spocky/projengmenu/ui/launcherActivities/SetBackgroundActivity;',
        'Lcom/spocky/projengmenu/ui/launcherActivities/ToggleParentalControlActivity;',
        'Lgh;',
        'Lft;',
    ]
    report = []
    for desc in targets:
        found = []
        for _mid, method, _flags, code, _kind in d.class_methods(desc):
            if not code:
                continue
            words = d.code_words(code)
            premium_pcs = [i for i in range(len(words)-1) if (words[i] & 0xff) == 0x1c and words[i+1] == premium_type]
            for ppc in premium_pcs:
                for j in range(ppc + 2, min(len(words)-2, ppc + 120)):
                    if (words[j] & 0xff) == 0x6e and words[j+1] == start_activity:
                        found.append((method, code, ppc, j, words[j:j+3]))
                        break
        assert len(found) == 1, (desc, found)
        method, code, ppc, invoke_pc, old = found[0]
        off = code + 16 + invoke_pc * 2
        # invoke-virtual is three code units. Three nops preserve all branch
        # offsets and instruction alignment; existing finish/return/goto stays.
        for k in range(3):
            put_u2(b, off + k*2, 0x0000)
        report.append((desc, method[1], invoke_pc, old))

    fix_header(b)
    verify_header(b)
    Path(dst).write_bytes(b)
    changed = sum(a != c for a, c in zip(original, b))
    print(f'classes2.dex: neutralized {len(report)} PremiumActivity launches; changed bytes incl header={changed}; size={len(b)}')
    for row in report:
        print('  ', row)
    assert len(report) == 5
    assert changed <= 80, changed


def main():
    if len(sys.argv) != 5:
        raise SystemExit('usage: patch-clean-01g-surgical-dex.py IN_CLASSES IN_CLASSES2 OUT_CLASSES OUT_CLASSES2')
    patch_classes1(sys.argv[1], sys.argv[3])
    patch_classes2(sys.argv[2], sys.argv[4])


if __name__ == '__main__':
    main()
