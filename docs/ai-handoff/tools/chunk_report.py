#!/usr/bin/env python3
"""Split a UTF-8 research report into byte-exact Git-safe parts and emit a manifest.

This tool is intentionally dependency-free. It preserves every input byte and verifies
reassembly before returning success. Parts are cut at newline boundaries when practical,
but exact bytes—not line formatting—are authoritative.
"""
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git_blob_sha1(data: bytes) -> str:
    h = hashlib.sha1()
    h.update(f"blob {len(data)}\0".encode("ascii"))
    h.update(data)
    return h.hexdigest()


def split_bytes(data: bytes, max_bytes: int) -> list[bytes]:
    if max_bytes < 1024:
        raise ValueError("max_bytes must be at least 1024")
    parts=[]; pos=0
    while len(data)-pos > max_bytes:
        limit=pos+max_bytes
        floor=pos + int(max_bytes*0.75)
        cut=data.rfind(b"\n", floor, limit+1)
        if cut < floor:
            cut=limit
        else:
            cut += 1
        parts.append(data[pos:cut]); pos=cut
    parts.append(data[pos:])
    return parts


def main() -> int:
    ap=argparse.ArgumentParser()
    ap.add_argument("source", type=Path)
    ap.add_argument("--report-id", required=True)
    ap.add_argument("--collection", required=True)
    ap.add_argument("--out-dir", type=Path, required=True)
    ap.add_argument("--max-bytes", type=int, default=14000)
    args=ap.parse_args()

    data=args.source.read_bytes()
    data.decode("utf-8")
    args.out_dir.mkdir(parents=True, exist_ok=True)
    chunks=split_bytes(data,args.max_bytes)
    width=max(2,len(str(len(chunks)-1)))
    part_entries=[]; names=[]
    for i,chunk in enumerate(chunks):
        name=f"{args.report_id}.part{i:0{width}d}.md" if len(chunks)>1 else f"{args.report_id}.md"
        (args.out_dir/name).write_bytes(chunk); names.append(name)
        part_entries.append({"path":name,"size_bytes":len(chunk),"sha256":sha256(chunk),"git_blob_sha1":git_blob_sha1(chunk)})
    rebuilt=b"".join((args.out_dir/n).read_bytes() for n in names)
    if rebuilt != data:
        raise SystemExit("ERROR: reassembled bytes differ from source")
    manifest={"schema_version":3,"report_id":args.report_id,"collection":args.collection,"source_filename":args.source.name,
              "source_size_bytes":len(data),"source_sha256":sha256(data),"encoding":"utf-8",
              "reassembly":"concatenate parts in listed order with no separator","verified_exact_reassembly":True,
              "parts":part_entries}
    (args.out_dir/f"{args.report_id}.manifest.json").write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps(manifest,indent=2,ensure_ascii=False))
    return 0

if __name__ == "__main__": raise SystemExit(main())
