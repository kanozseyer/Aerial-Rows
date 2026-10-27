from pathlib import Path

p = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
text = p.read_text()

# v7/v8/v9 must already be present; this patch only changes first-run profile application.
if 'restoreBundledChannelLayoutOnce' not in Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java').read_text():
    raise SystemExit('expected v9 delayed channel-layout restore before diagnosed profile patch')

resume_anchor = '        applyDefaultProfile(app);\n'
if text.count(resume_anchor) != 1:
    raise SystemExit(f'onLauncherResume applyDefaultProfile anchor mismatch: {text.count(resume_anchor)}')
text = text.replace(resume_anchor,
                    resume_anchor + '        ProfileRestoreBridge.onLauncherResume(context);\n', 1)

sig = '    static void applyDefaultProfile(Context c) {'
start = text.find(sig)
if start < 0:
    raise SystemExit('applyDefaultProfile method missing')
brace = text.find('{', start)
depth = 0
end = None
for i in range(brace, len(text)):
    ch = text[i]
    if ch == '{': depth += 1
    elif ch == '}':
        depth -= 1
        if depth == 0:
            end = i + 1
            break
if end is None:
    raise SystemExit('could not find applyDefaultProfile method end')
new_method = '''    static void applyDefaultProfile(Context c) {\n        // Bootstrap only safe early values. The full exported profile is applied on\n        // MainActivity resume through Projectivy's own typed importer/migration path.\n        ProfileRestoreBridge.bootstrap(c);\n    }'''
text = text[:start] + new_method + text[end:]
p.write_text(text)
print('patched v9 default profile to native typed restore on launcher resume')
