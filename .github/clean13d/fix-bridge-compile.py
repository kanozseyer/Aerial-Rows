#!/usr/bin/env python3
from pathlib import Path

p=Path('.github/clean13d/StatusBarCustomizationBridge.java')
s=p.read_text()
old='''                    @Override public void run() {\n                        Activity a = ref.get();\n                        if (a != null && !a.isFinishing() && !a.isDestroyed()) applyVisual(a);\n                    }\n'''
new='''                    @Override public void run() {\n                        Activity a = ref.get();\n                        if (a == null || a.isFinishing() || a.isDestroyed()) return;\n                        try {\n                            applyVisual(a);\n                        } catch (Throwable t) {\n                            Log.e(TAG, "CLEAN13D delayed visual apply failed", t);\n                        }\n                    }\n'''
if s.count(old)!=1:
    raise SystemExit(f'CLEAN13D delayed visual anchor mismatch: {s.count(old)}')
p.write_text(s.replace(old,new,1))
print('PASS: CLEAN13D delayed visual refresh guarded')
