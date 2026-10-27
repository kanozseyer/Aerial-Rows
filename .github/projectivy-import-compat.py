from pathlib import Path

root = Path('/tmp/projectivy-decoded')
paths = list(root.rglob('ImportSettingsActivity.smali'))
if len(paths) != 1:
    raise SystemExit(f'expected one ImportSettingsActivity.smali, found {len(paths)}: {paths}')
path = paths[0]
text = path.read_text()
start = text.index('.method public onCreate(Landroid/os/Bundle;)V') if '.method public onCreate(Landroid/os/Bundle;)V' in text else text.index('.method public final onCreate(Landroid/os/Bundle;)V')
end = text.index('.end method', start)
method = text[start:end]

call = '    invoke-virtual {p0}, Lcom/spocky/projengmenu/ui/launcherActivities/ImportSettingsActivity;->v()V\n'
if method.count(call) != 1:
    raise SystemExit(f'ImportSettingsActivity v() call mismatch: {method.count(call)}')
replacement = '''    # Embedded: accept Projectivy .plbackup exports with a tolerant importer.\n    # If the intent is not a settings VIEW intent, fall back to stock behavior.\n    invoke-static {p0}, Lcom/spocky/projengmenu/embedded/EmbeddedRuntime;->handleManualImportActivity(Landroid/app/Activity;)Z\n    move-result p1\n    if-nez p1, :embedded_import_done\n\n''' + call + '''\n    :embedded_import_done\n'''
method = method.replace(call, replacement, 1)
text = text[:start] + method + text[end:]
path.write_text(text)

check = path.read_text()
if 'EmbeddedRuntime;->handleManualImportActivity' not in check:
    raise SystemExit('manual import compatibility hook verification failed')
print('patched Projectivy manual settings import compatibility hook')
