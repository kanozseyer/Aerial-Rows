#!/usr/bin/env python3
"""Validate AI-handoff archive integrity *and* expected-source coverage."""
from __future__ import annotations
import argparse, hashlib, json, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RAW=ROOT/'raw'

def sha256(data:bytes)->str: return hashlib.sha256(data).hexdigest()
def git_blob_sha1(data:bytes)->str:
    h=hashlib.sha1(); h.update(f"blob {len(data)}\0".encode()); h.update(data); return h.hexdigest()

def validate_manifest(path:Path, expected:dict)->list[str]:
    errors=[]
    try: m=json.loads(path.read_text(encoding='utf-8'))
    except Exception as e: return [f'{path}: invalid JSON: {e}']
    if m.get('source_sha256') != expected.get('source_sha256'): errors.append(f'{path}: source sha differs from SOURCE_CATALOG')
    if expected.get('source_filename') and m.get('source_filename') != expected.get('source_filename'): errors.append(f'{path}: source filename differs from SOURCE_CATALOG')
    base=path.parent; rebuilt=[]; parts=m.get('parts')
    if not isinstance(parts,list) or not parts: errors.append(f'{path}: missing/empty parts list'); return errors
    for p in parts:
        fp=base/p['path']
        if not fp.is_file(): errors.append(f'{path}: missing part {p["path"]}'); continue
        b=fp.read_bytes(); rebuilt.append(b)
        if len(b)!=p.get('size_bytes'): errors.append(f'{path}: size mismatch {p["path"]}')
        if sha256(b)!=p.get('sha256'): errors.append(f'{path}: sha256 mismatch {p["path"]}')
        if git_blob_sha1(b)!=p.get('git_blob_sha1'): errors.append(f'{path}: git blob sha mismatch {p["path"]}')
    whole=b''.join(rebuilt)
    if len(whole)!=m.get('source_size_bytes'): errors.append(f'{path}: reassembled size mismatch')
    if sha256(whole)!=m.get('source_sha256'): errors.append(f'{path}: reassembled sha256 mismatch')
    return errors

def main()->int:
    ap=argparse.ArgumentParser(); ap.add_argument('--require-complete',action='store_true'); args=ap.parse_args()
    errors=[]; checked=0; pending=[]
    catalog=json.loads((RAW/'SOURCE_CATALOG.json').read_text(encoding='utf-8'))
    master=json.loads((RAW/'ARCHIVE_MANIFEST.json').read_text(encoding='utf-8'))
    if master.get('catalog')!='SOURCE_CATALOG.json': errors.append('ARCHIVE_MANIFEST: catalog must be SOURCE_CATALOG.json')
    repo_root=ROOT.parents[1]
    for rel in catalog.get('required_context_files',[]):
        if not (repo_root/rel).is_file(): errors.append(f'catalog: missing required context file {rel}')
    ccols=catalog.get('collections',{}); mcols=master.get('collections',{})
    for collection,expected_entries in ccols.items():
        actual_entries=mcols.get(collection)
        if not isinstance(actual_entries,dict): errors.append(f'master: missing collection {collection}'); continue
        for rid in sorted(set(actual_entries)-set(expected_entries)): errors.append(f'master: unknown report/reference {collection}/{rid}')
        for rid,expected in expected_entries.items():
            item=actual_entries.get(rid)
            if not isinstance(item,dict): errors.append(f'master: missing catalog entry {collection}/{rid}'); continue
            if expected.get('reference_only'):
                if item.get('status')!='reference': errors.append(f'master: {collection}/{rid} must have status reference')
                if item.get('reference_sha256')!=expected.get('sha256'): errors.append(f'master: reference sha mismatch {collection}/{rid}')
                continue
            if item.get('source_sha256')!=expected.get('source_sha256'): errors.append(f'master: source sha mismatch {collection}/{rid}')
            status=item.get('status')
            if status=='complete':
                manifest=item.get('manifest')
                if not manifest: errors.append(f'master: {collection}/{rid} complete but no manifest'); continue
                mp=RAW/manifest
                if not mp.is_file(): errors.append(f'master: missing manifest {manifest}'); continue
                errors.extend(validate_manifest(mp,expected)); checked+=1
            elif status=='pending': pending.append(f'{collection}/{rid}')
            else: errors.append(f'master: invalid status {status!r} for {collection}/{rid}')
    if args.require_complete and pending: errors.append('required archive entries still pending: '+', '.join(pending))
    if errors:
        print('AI handoff validation FAILED',file=sys.stderr)
        for e in errors: print(f'- {e}',file=sys.stderr)
        return 1
    print(f'AI handoff validation OK: {checked} complete report(s), {len(pending)} pending report(s)')
    if pending: print('Next pending: '+pending[0])
    return 0
if __name__=='__main__': raise SystemExit(main())
