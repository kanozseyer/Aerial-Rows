#!/usr/bin/env python3
from pathlib import Path

engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

# Shield's TV provider normalizes PreviewProgram.description back to empty for
# these rows. CLEAN12.1A proved every other compared field round-trips exactly.
# Therefore stop writing/comparing this non-persistent field so unchanged cards
# remain genuinely unchanged on later refreshes.
write_line = '            v.put("description", e.mediaType != null ? e.mediaType : "Red Light");\n'
compare_line = '        if (!same(old.description, v.getAsString("description"))) return false;\n'

if text.count(write_line) != 1:
    raise SystemExit(f'CLEAN12.1B description write anchor mismatch: {text.count(write_line)}')
if text.count(compare_line) != 1:
    raise SystemExit(f'CLEAN12.1B description compare anchor mismatch: {text.count(compare_line)}')

text = text.replace(write_line, '', 1)
text = text.replace(compare_line, '', 1)
engine.write_text(text)

check = engine.read_text()
if 'v.put("description", e.mediaType != null ? e.mediaType : "Red Light")' in check:
    raise SystemExit('CLEAN12.1B description write still active')
if 'same(old.description, v.getAsString("description"))' in check:
    raise SystemExit('CLEAN12.1B description comparison still active')
for token in [
    'CLEAN12.1 SUCCESS channels=',
    'loadExistingPrograms(c, channel)',
    'sameProgram(old, v)',
    'programIntent(c, e)',
    'CLEAN10.3 browse clean uri=',
]:
    if token not in check:
        raise SystemExit('CLEAN12.1B invariant missing: ' + token)

print('PASS: CLEAN12.1B ignores Shield-normalized PreviewProgram.description only')
