#!/usr/bin/env python3
"""Package one known raw research report from SOURCE_CATALOG into the archive.

The source SHA must match the catalog before any output is written. This tool emits
byte-exact parts + manifest and updates ARCHIVE_MANIFEST.json in a local checkout/staging tree.
Publishing to GitHub is intentionally separate and should use one atomic Git tree/commit.
"""
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path

HERE = Path(__file__).resolve()
HANDOFF = HERE.parents[1]
RAW = HANDOFF / 'raw'
CATALOG_PATH = RAW / 'SOURCE_CATALOG.json'
ARCHIVE_PATH = RAW / 'ARCHIVE_MANIFEST.json'

def sha256(data: bytes) -> str: return hashlib.sha256(data).hexdigest()
def git_blob_sha1(data: bytes) -> str:
    h=hashlib.sha1(); h.update(f"blob {len(data)}\0".encode('ascii')); h.update(data); return h.hexdigest()

def split_bytes(data: bytes, max_bytes: int) -> list[bytes]:
    if len(data) <= max_bytes: return [data]
    parts=[]; pos=0
    while len(data)-pos > max_bytes:
        limit=pos+max_bytes; floor=pos+max(1024,int(max_bytes*0.70))
        cut=data.rfind(b'\n', floor, limit+1)
        if cut < floor: cut=limit
        else: cut += 1
        if cut <= pos: cut=limit
        parts.append(data[pos:cut]); pos=cut
    parts.append(data[pos:]); return parts

def main() -> int:
    ap=argparse.ArgumentParser()
    ap.add_argument('source', type=Path)
    ap.add_argument('--collection', required=True)
    ap.add_argument('--report-id', required=True)
    ap.add_argument('--max-bytes', type=int, default=24000)
    args=ap.parse_args()
    catalog=json.loads(CATALOG_PATH.read_text(encoding='utf-8'))
    archive=json.loads(ARCHIVE_PATH.read_text(encoding='utf-8'))
    try: expected=catalog['collections'][args.collection][args.report_id]
    except KeyError: raise SystemExit(f'ERROR: unknown catalog entry {args.collection}/{args.report_id}')
    if expected.get('reference_only'): raise SystemExit('ERROR: reference-only entries are not packageable reports')
    data=args.source.read_bytes(); data.decode('utf-8')
    actual_sha=sha256(data)
    if actual_sha != expected['source_sha256']:
        raise SystemExit(f"ERROR: source SHA mismatch: got {actual_sha}, expected {expected['source_sha256']}")
    out_dir=RAW/args.collection; out_dir.mkdir(parents=True, exist_ok=True)
    chunks=split_bytes(data,args.max_bytes); width=max(2,len(str(len(chunks)-1)))
    entries=[]
    for i,chunk in enumerate(chunks):
        name=f'{args.report_id}.md' if len(chunks)==1 else f'{args.report_id}.part{i:0{width}d}.md'
        (out_dir/name).write_bytes(chunk)
        entries.append({'path':name,'size_bytes':len(chunk),'sha256':sha256(chunk),'git_blob_sha1':git_blob_sha1(chunk)})
    rebuilt=b''.join((out_dir/e['path']).read_bytes() for e in entries)
    if rebuilt != data: raise SystemExit('ERROR: byte-exact reassembly check failed')
    manifest={'schema_version':4,'report_id':args.report_id,'collection':args.collection,'catalog':'../SOURCE_CATALOG.json','source_filename':expected['source_filename'],'source_size_bytes':len(data),'source_sha256':actual_sha,'encoding':'utf-8','reassembly':'concatenate parts in listed order with no separator','verified_exact_reassembly':True,'parts':entries}
    manifest_path=out_dir/f'{args.report_id}.manifest.json'; manifest_path.write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    aentry=archive['collections'][args.collection][args.report_id]; aentry['status']='complete'; aentry['manifest']=f'{args.collection}/{args.report_id}.manifest.json'; aentry['source_sha256']=actual_sha
    ARCHIVE_PATH.write_text(json.dumps(archive,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(json.dumps(manifest,indent=2,ensure_ascii=False)); return 0
if __name__=='__main__': raise SystemExit(main())
