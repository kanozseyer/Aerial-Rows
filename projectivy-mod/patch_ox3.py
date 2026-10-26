#!/usr/bin/env python3
"""Patch Projectivy 4.72's existing rounded dock decoration into a glass-like treatment.

This script intentionally touches only ox3.smali, the RecyclerView ItemDecoration used by
Projectivy for eligible category dock backgrounds. It does not alter feature gates,
licensing, category eligibility, layout, focus, scrolling, or wallpaper behavior.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import struct
from pathlib import Path

DEFAULT_FILL_ALPHA = 0x60  # 37.6%: enough contrast over moving Aerial video without looking opaque.
DEFAULT_RIM_ALPHA = 0xB0   # 69.0%: visible glass edge on bright and dark wallpaper frames.
DEFAULT_RIM_LIGHTEN = 0.45 # Existing Projectivy rim is 0.20 toward white.
ORIGINAL_LIGHTEN_BITS = "0x3e4ccccd"


def float_bits(value: float) -> str:
    bits = struct.unpack(">I", struct.pack(">f", value))[0]
    return f"0x{bits:08x}"


def method_span(lines: list[str], signature: str) -> tuple[int, int]:
    starts = [i for i, line in enumerate(lines) if line.strip() == signature]
    if len(starts) != 1:
        raise RuntimeError(f"Expected exactly one {signature!r}, found {len(starts)}")
    start = starts[0]
    try:
        end = next(i for i in range(start + 1, len(lines)) if lines[i].strip() == ".end method")
    except StopIteration as exc:
        raise RuntimeError(f"Method {signature!r} has no .end method") from exc
    return start, end


def insert_alpha_after_setcolor(
    block: list[str],
    expected_calls: list[tuple[str, str, int]],
) -> list[str]:
    """Insert Paint.setAlpha after the two known Paint.setColor calls.

    expected_calls items are (paint_register, scratch_register, alpha).
    """
    call_indexes = [
        i for i, line in enumerate(block)
        if "Landroid/graphics/Paint;->setColor(I)V" in line
    ]
    if len(call_indexes) != len(expected_calls):
        raise RuntimeError(
            f"Expected {len(expected_calls)} Paint.setColor calls in method, found {len(call_indexes)}"
        )

    calls = iter(expected_calls)
    next_call = next(calls, None)
    call_number = 0
    output: list[str] = []
    for line in block:
        output.append(line)
        if "Landroid/graphics/Paint;->setColor(I)V" not in line:
            continue
        if next_call is None:
            raise RuntimeError("Unexpected extra setColor call")
        paint_register, scratch_register, alpha = next_call
        call_number += 1
        output.extend([
            "",
            f"    # Projectivy Liquid Glass: alpha pass {call_number}",
            f"    const/16 {scratch_register}, 0x{alpha:02x}",
            "",
            f"    invoke-virtual {{{paint_register}, {scratch_register}}}, Landroid/graphics/Paint;->setAlpha(I)V",
        ])
        next_call = next(calls, None)
    return output


def patch_smali(path: Path, fill_alpha: int, rim_alpha: int, rim_lighten: float) -> None:
    original = path.read_text(encoding="utf-8")
    if "Projectivy Liquid Glass:" in original:
        raise RuntimeError(f"{path} already appears to be patched")

    original_sha = hashlib.sha256(original.encode()).hexdigest()
    lines = original.splitlines()

    ctor_sig = ".method public constructor <init>(IFFF)V"
    update_sig = ".method public final e(I)V"
    ctor_start, ctor_end = method_span(lines, ctor_sig)
    update_start, update_end = method_span(lines, update_sig)

    ctor = lines[ctor_start:ctor_end + 1]
    update = lines[update_start:update_end + 1]

    # Projectivy 4.72 contains exactly one 0.20f rim-lighten literal in each of
    # these two methods. Refuse the patch if the fingerprint differs.
    if sum(ORIGINAL_LIGHTEN_BITS in line for line in ctor) != 1:
        raise RuntimeError("Constructor no longer matches the Projectivy 4.72 rim-color fingerprint")
    if sum(ORIGINAL_LIGHTEN_BITS in line for line in update) != 1:
        raise RuntimeError("e(int) no longer matches the Projectivy 4.72 rim-color fingerprint")

    new_bits = float_bits(rim_lighten)
    ctor = [line.replace(ORIGINAL_LIGHTEN_BITS, new_bits).replace("# 0.2f", f"# {rim_lighten:.2f}f") for line in ctor]
    update = [line.replace(ORIGINAL_LIGHTEN_BITS, new_bits).replace("# 0.2f", f"# {rim_lighten:.2f}f") for line in update]

    # Constructor register facts verified against the official 4.72 APK:
    # p2 is the Paint instance for both fill and rim; p3 is dead/reassigned
    # immediately after each setColor and is therefore a safe alpha scratch.
    ctor = insert_alpha_after_setcolor(
        ctor,
        [
            ("p2", "p3", fill_alpha),
            ("p2", "p3", rim_alpha),
        ],
    )

    # e(int) has .locals 2. v1 is free directly after the fill setColor and p1
    # is dead after the final rim setColor. No register-count change is needed.
    update = insert_alpha_after_setcolor(
        update,
        [
            ("v0", "v1", fill_alpha),
            ("p0", "p1", rim_alpha),
        ],
    )

    # Rebuild the file from the bottom method upward so original offsets remain valid.
    patched = lines[:]
    patched[update_start:update_end + 1] = update
    # update method is after constructor, so constructor indices are unchanged.
    patched[ctor_start:ctor_end + 1] = ctor
    result = "\n".join(patched) + ("\n" if original.endswith("\n") else "")

    if result.count("Projectivy Liquid Glass:") != 4:
        raise RuntimeError("Patch postcondition failed: expected four alpha markers")
    if result.count(new_bits) < 2:
        raise RuntimeError("Patch postcondition failed: rim-lighten constant was not applied twice")

    path.write_text(result, encoding="utf-8")
    new_sha = hashlib.sha256(result.encode()).hexdigest()
    print(f"Patched: {path}")
    print(f"ox3.smali SHA-256 before: {original_sha}")
    print(f"ox3.smali SHA-256 after : {new_sha}")
    print(f"fill alpha : {fill_alpha}/255 ({fill_alpha / 255:.1%})")
    print(f"rim alpha  : {rim_alpha}/255 ({rim_alpha / 255:.1%})")
    print(f"rim lighten: {rim_lighten:.2f} toward white")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("decoded_apk", type=Path, help="Directory produced by apktool d")
    parser.add_argument("--fill-alpha", type=int, default=DEFAULT_FILL_ALPHA)
    parser.add_argument("--rim-alpha", type=int, default=DEFAULT_RIM_ALPHA)
    parser.add_argument("--rim-lighten", type=float, default=DEFAULT_RIM_LIGHTEN)
    args = parser.parse_args()

    for name, value in (("fill-alpha", args.fill_alpha), ("rim-alpha", args.rim_alpha)):
        if not 0 <= value <= 255:
            parser.error(f"--{name} must be between 0 and 255")
    if not 0.0 <= args.rim_lighten <= 1.0:
        parser.error("--rim-lighten must be between 0.0 and 1.0")

    candidates = list(args.decoded_apk.rglob("ox3.smali"))
    if len(candidates) != 1:
        raise SystemExit(f"Expected exactly one ox3.smali below {args.decoded_apk}, found {len(candidates)}")
    patch_smali(candidates[0], args.fill_alpha, args.rim_alpha, args.rim_lighten)


if __name__ == "__main__":
    main()
