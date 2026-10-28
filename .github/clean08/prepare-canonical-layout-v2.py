#!/usr/bin/env python3
from pathlib import Path

src = Path('.github/clean08/prepare-canonical-layout.py').read_text()
needle = '''        if (n.isEmpty()) return "";\n        if ("in_progress_episodes".equals(n)) return "redlight:in_progress_episodes";'''
replacement = '''        if (n.isEmpty()) return "";\n        // The six canonical visible backup rows use explicit stable identities.\n        if ("trending_movies".equals(n)) return "redlight:shelf:trending:movies";\n        if ("trending_tv_shows".equals(n)) return "redlight:shelf:trending:shows";\n        if ("in_progress_episodes".equals(n)) return "redlight:in_progress_episodes";'''
if needle not in src:
    raise SystemExit('CLEAN08 explicit identity anchor missing')
src = src.replace(needle, replacement, 1)
exec(compile(src, '.github/clean08/prepare-canonical-layout.py', 'exec'), globals())
