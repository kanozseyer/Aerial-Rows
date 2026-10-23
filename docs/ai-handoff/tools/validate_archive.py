#!/usr/bin/env python3
"""Validate all completed AI-handoff raw reports from repository manifests."""
from __future__ import annotations
import hashlib, json, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RAW=ROOT/'raw'

def sha256(data:bytes)->str: return hashlib.sha256(data).hexdigest()
def git_blob_sha1(data:bytes)->str:
    h=hashlib.sha1(); h.update(f"blob {len(data)}\0".encode()); h.update(data); return h.hexdigest()

def validate_manifest(path:Path)->list[str]:
    errors=[]
    m=json.loads(path.read_text(encoding='utf-8'))
    base=path.parent; rebuilt=[]
    for p in m.get('parts',[]):
        fp=base/p['path']
        if not fp.is_file(): errors.append(f"{path}: missing part {p['path']}"); continue
        b=fp.read_bytes(); rebuilt.append(b)
        if len(b)!=p['size_bytes']: errors.append(f"{path}: size mismatch {p['path']}")
        if sha256(b)!=p['sha256']: errors.append(f"{path}: sha256 mismatch {p['path']}")
        if git_blob_sha1(b)!=p['git_blob_sha1']: errors.append(f"{path}: git blob sha mismatch {p['path']}")
    whole=b''.join(rebuilt)
    if len(whole)!=m.get('source_size_bytes'): errors.append(f"{path}: reassembled size mismatch")
    if sha256(whole)!=m.get('source_sha256'): errors.append(f"{path}: reassembled sha256 mismatch")
    return errors

def main()->int:
    errors=[]; checked=0
    master=json.loads((RAW/'ARCHIVE_MANIFEST.json').read_text(encoding='utf-8'))
    for collection,entries in master['collections'].items():
        if not isinstance(entries,dict): continue
        for rid,item in entries.items():
            if not isinstance(item,dict) or item.get('status')!='complete': continue
            manifest=item.get('manifest')
            if not manifest: errors.append(f"master: {collection}/{rid} complete but has no manifest"); continue
            mp=RAW/manifest
            if not mp.is_file(): errors.append(f"master: missing manifest {manifest}"); continue
            errors.extend(validate_manifest(mp)); checked+=1
            m=json.loads(mp.read_text(encoding='utf-8'))
            if item.get('source_sha256') and item['source_sha256']!=m.get('source_sha256'):
                errors.append(f"master: source sha mismatch for {collection}/{rid}")
    if errors:
        print("AI handoff validation FAILED",file=sys.stderr)
        for e in errors: print(f"- {e}",file=sys.stderr)
        return 1
    print(f"AI handoff validation OK: {checked} completed report manifest(s)")
    return 0

if __name__=='__main__': raise SystemExit(main())
