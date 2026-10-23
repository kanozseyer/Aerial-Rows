#!/usr/bin/env python3
"""Package a cataloged research report for the AI handoff archive.

Supports raw UTF-8 parts and deterministic gzip+base64 transport parts. In both
formats the authoritative source is the original report bytes/SHA-256.
"""
from __future__ import annotations
import argparse, base64, gzip, hashlib, json
from pathlib import Path

HERE=Path(__file__).resolve()
HANDOFF=HERE.parents[1]
RAW=HANDOFF/'raw'
CATALOG_PATH=RAW/'SOURCE_CATALOG.json'
ARCHIVE_PATH=RAW/'ARCHIVE_MANIFEST.json'

def sha256(b:bytes)->str:return hashlib.sha256(b).hexdigest()
def git_blob_sha1(b:bytes)->str:
 h=hashlib.sha1();h.update(f'blob {len(b)}\0'.encode('ascii'));h.update(b);return h.hexdigest()
def split_bytes(data:bytes,max_bytes:int)->list[bytes]:
 return [data[i:i+max_bytes] for i in range(0,len(data),max_bytes)] or [b'']

def main()->int:
 ap=argparse.ArgumentParser()
 ap.add_argument('source',type=Path);ap.add_argument('--collection',required=True);ap.add_argument('--report-id',required=True)
 ap.add_argument('--storage',choices=['raw-utf8','gzip-base64'],default='gzip-base64')
 ap.add_argument('--max-bytes',type=int,default=4000)
 args=ap.parse_args()
 catalog=json.loads(CATALOG_PATH.read_text());archive=json.loads(ARCHIVE_PATH.read_text())
 expected=catalog['collections'][args.collection][args.report_id]
 if expected.get('reference_only'):raise SystemExit('ERROR: reference-only entry')
 source=args.source.read_bytes();source.decode('utf-8')
 if sha256(source)!=expected['source_sha256']:raise SystemExit(f"ERROR: source SHA mismatch: {sha256(source)} != {expected['source_sha256']}")
 if args.storage=='raw-utf8':
  payload=source; ext='md'; encoding='utf-8'; transform={'type':'none'}
 else:
  gz=gzip.compress(source,compresslevel=9,mtime=0);payload=base64.b64encode(gz);ext='md.gz.b64'
  encoding='ascii-base64';transform={'type':'gzip+base64','gzip_mtime':0,'gzip_sha256':sha256(gz),'encoded_sha256':sha256(payload)}
 chunks=split_bytes(payload,args.max_bytes)
 out=RAW/args.collection;out.mkdir(parents=True,exist_ok=True);width=max(2,len(str(len(chunks)-1)));parts=[]
 for i,ch in enumerate(chunks):
  name=(f'{args.report_id}.{ext}' if len(chunks)==1 else f'{args.report_id}.part{i:0{width}d}.{ext}')
  (out/name).write_bytes(ch);parts.append({'path':name,'size_bytes':len(ch),'sha256':sha256(ch),'git_blob_sha1':git_blob_sha1(ch)})
 stored=b''.join((out / p['path']).read_bytes() for p in parts)
 if stored!=payload:raise SystemExit('ERROR: stored payload reassembly mismatch')
 if args.storage=='gzip-base64': rebuilt=gzip.decompress(base64.b64decode(stored,validate=True))
 else:rebuilt=stored
 if rebuilt!=source:raise SystemExit('ERROR: decoded source does not exactly match input')
 manifest={'schema_version':5,'report_id':args.report_id,'collection':args.collection,'catalog':'../SOURCE_CATALOG.json','source_filename':expected['source_filename'],'source_size_bytes':len(source),'source_sha256':sha256(source),'source_encoding':'utf-8','storage_format':args.storage,'transform':transform,'reassembly':'concatenate parts in listed order with no separator, then apply storage_format decoding','verified_exact_reassembly':True,'parts':parts}
 (out/f'{args.report_id}.manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n')
 item=archive['collections'][args.collection][args.report_id];item.update(status='complete',manifest=f'{args.collection}/{args.report_id}.manifest.json',source_sha256=sha256(source))
 ARCHIVE_PATH.write_text(json.dumps(archive,indent=2,ensure_ascii=False)+'\n')
 print(json.dumps(manifest,indent=2,ensure_ascii=False));return 0
if __name__=='__main__':raise SystemExit(main())
