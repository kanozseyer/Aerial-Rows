#!/usr/bin/env python3
"""Reconstruct one archived research report to an exact Markdown file."""
from __future__ import annotations
import argparse,base64,gzip,hashlib,json
from pathlib import Path
def sha(b):return hashlib.sha256(b).hexdigest()
def main():
 ap=argparse.ArgumentParser();ap.add_argument('manifest',type=Path);ap.add_argument('--output',type=Path);a=ap.parse_args();m=json.loads(a.manifest.read_text());stored=b''.join((a.manifest.parent/p['path']).read_bytes() for p in m['parts']);fmt=m.get('storage_format','raw-utf8')
 source=stored if fmt=='raw-utf8' else gzip.decompress(base64.b64decode(stored,validate=True))
 if sha(source)!=m['source_sha256'] or len(source)!=m['source_size_bytes']:raise SystemExit('ERROR: reconstructed report failed integrity check')
 out=a.output or Path(m['source_filename']);out.write_bytes(source);print(f'wrote {out} ({len(source)} bytes, sha256 {sha(source)})')
if __name__=='__main__':main()
