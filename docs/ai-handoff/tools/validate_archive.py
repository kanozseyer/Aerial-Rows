#!/usr/bin/env python3
"""Validate AI handoff integrity and catalog coverage (raw or gzip+base64 reports)."""
from __future__ import annotations
import argparse,base64,gzip,hashlib,json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];RAW=ROOT/'raw'
def sha256(b:bytes)->str:return hashlib.sha256(b).hexdigest()
def git_blob_sha1(b:bytes)->str:
 h=hashlib.sha1();h.update(f'blob {len(b)}\0'.encode());h.update(b);return h.hexdigest()
def validate_manifest(path:Path,expected:dict)->list[str]:
 e=[]
 try:m=json.loads(path.read_text())
 except Exception as x:return[f'{path}: invalid JSON: {x}']
 if m.get('source_sha256')!=expected.get('source_sha256'):e.append(f'{path}: source sha differs from catalog')
 if m.get('source_filename')!=expected.get('source_filename'):e.append(f'{path}: source filename differs from catalog')
 payload=[]
 for p in m.get('parts',[]):
  fp=path.parent/p['path']
  if not fp.is_file():e.append(f'{path}: missing part {p["path"]}');continue
  b=fp.read_bytes();payload.append(b)
  if len(b)!=p.get('size_bytes'):e.append(f'{path}: size mismatch {p["path"]}')
  if sha256(b)!=p.get('sha256'):e.append(f'{path}: sha mismatch {p["path"]}')
  if git_blob_sha1(b)!=p.get('git_blob_sha1'):e.append(f'{path}: blob sha mismatch {p["path"]}')
 stored=b''.join(payload);fmt=m.get('storage_format','raw-utf8')
 try:
  if fmt in ('raw-utf8','raw',''):source=stored
  elif fmt=='gzip-base64':
   tr=m.get('transform',{});gz=base64.b64decode(stored,validate=True)
   if tr.get('encoded_sha256') and sha256(stored)!=tr['encoded_sha256']:e.append(f'{path}: encoded payload sha mismatch')
   if tr.get('gzip_sha256') and sha256(gz)!=tr['gzip_sha256']:e.append(f'{path}: gzip sha mismatch')
   source=gzip.decompress(gz)
  else:e.append(f'{path}: unsupported storage_format {fmt}');return e
 except Exception as x:e.append(f'{path}: decode/decompress failed: {x}');return e
 if len(source)!=m.get('source_size_bytes'):e.append(f'{path}: source size mismatch')
 if sha256(source)!=m.get('source_sha256'):e.append(f'{path}: source sha mismatch after reconstruction')
 try:source.decode('utf-8')
 except UnicodeDecodeError:e.append(f'{path}: reconstructed source is not UTF-8')
 return e
def main()->int:
 ap=argparse.ArgumentParser();ap.add_argument('--require-complete',action='store_true');args=ap.parse_args();errors=[];checked=0;pending=[]
 catalog=json.loads((RAW/'SOURCE_CATALOG.json').read_text());master=json.loads((RAW/'ARCHIVE_MANIFEST.json').read_text())
 if master.get('catalog')!='SOURCE_CATALOG.json':errors.append('ARCHIVE_MANIFEST: wrong catalog')
 repo=ROOT.parents[1]
 for rel in catalog.get('required_context_files',[]):
  if not (repo/rel).is_file():errors.append(f'catalog: missing required context {rel}')
 for coll,expected_entries in catalog['collections'].items():
  actual=master.get('collections',{}).get(coll)
  if not isinstance(actual,dict):errors.append(f'master: missing collection {coll}');continue
  for rid in sorted(set(actual)-set(expected_entries)):errors.append(f'master: unknown entry {coll}/{rid}')
  for rid,expected in expected_entries.items():
   item=actual.get(rid)
   if not isinstance(item,dict):errors.append(f'master: missing {coll}/{rid}');continue
   if expected.get('reference_only'):
    if item.get('status')!='reference':errors.append(f'master: {coll}/{rid} must be reference')
    if item.get('reference_sha256')!=expected.get('sha256'):errors.append(f'master: reference sha mismatch {coll}/{rid}')
    continue
   if item.get('source_sha256')!=expected.get('source_sha256'):errors.append(f'master: source sha mismatch {coll}/{rid}')
   if item.get('status')=='complete':
    mp=RAW/item.get('manifest','')
    if not mp.is_file():errors.append(f'master: missing manifest for {coll}/{rid}')
    else:errors.extend(validate_manifest(mp,expected));checked+=1
   elif item.get('status')=='pending':pending.append(f'{coll}/{rid}')
   else:errors.append(f'master: invalid status for {coll}/{rid}: {item.get("status")!r}')
 if args.require_complete and pending:errors.append('required reports still pending: '+', '.join(pending))
 if errors:
  print('AI handoff validation FAILED',file=sys.stderr);[print('- '+x,file=sys.stderr) for x in errors];return 1
 print(f'AI handoff validation OK: {checked} complete report(s), {len(pending)} pending report(s)')
 if pending:print('Next pending: '+pending[0])
 return 0
if __name__=='__main__':raise SystemExit(main())
