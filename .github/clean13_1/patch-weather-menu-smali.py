#!/usr/bin/env python3
from pathlib import Path
import re, sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-weather-menu-smali.py APKTOOL_ROOT')
root = Path(sys.argv[1])
qg = next(iter(root.rglob('qg.smali')), None)
pg = next(iter(root.rglob('pg.smali')), None)
if qg is None or pg is None:
    raise SystemExit(f'missing qg/pg: qg={qg} pg={pg}')


def method_span(text, sig):
    start = text.find(sig)
    if start < 0: raise SystemExit('method not found: ' + sig)
    end = text.find('.end method', start)
    if end < 0: raise SystemExit('method end not found: ' + sig)
    end += len('.end method')
    return start, end


def inject_after_locals(text, sig, injection, marker):
    start, end = method_span(text, sig)
    block = text[start:end]
    if marker in block: raise SystemExit('already patched: ' + marker)
    m = re.search(r'(^\s*\.locals\s+)(\d+)(\s*$)', block, re.M)
    if not m: raise SystemExit('locals not found: ' + sig)
    if int(m.group(2)) < 1:
        a,b = m.span(2)
        block = block[:a] + '1' + block[b:]
        m = re.search(r'(^\s*\.locals\s+\d+\s*$)', block, re.M)
    pos = m.end()
    block = block[:pos] + '\n\n' + injection + block[pos:]
    return text[:start] + block + text[end:]


def inject_before_every_return_void(text, sig, injection, marker):
    start, end = method_span(text, sig)
    block = text[start:end]
    if marker in block: raise SystemExit('already patched: ' + marker)
    needle = '    return-void'
    count = block.count(needle)
    if count < 1: raise SystemExit('return-void not found: ' + sig)
    block = block.replace(needle, injection + '\n' + needle)
    return text[:start] + block + text[end:], count

# qg = native Appearance > Status Bar page.
# CLEAN13.1B originally inserted only before qg.e0's last textual return. On Shield
# that return is not the runtime exit path, so Weather never appeared. Insert the
# sibling action before EVERY qg.e0 return instead: whichever branch Projectivy
# takes, the Weather row is added exactly once on that execution path.
s = qg.read_text()
s, qg_return_count = inject_before_every_return_void(
    s,
    '.method public final e0(Ljava/util/List;Landroid/os/Bundle;)V',
    '    invoke-static {p0, p1}, Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;->addWeatherParentAction(Ljava/lang/Object;Ljava/util/List;)V\n',
    'addWeatherParentAction')
s = inject_after_locals(
    s,
    '.method public final i0(Lmu1;)V',
    '''    invoke-static {p0, p1}, Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;->handleWeatherParentAction(Ljava/lang/Object;Ljava/lang/Object;)Z\n    move-result v0\n    if-eqz v0, :clean131_parent_continue\n    return-void\n\n    :clean131_parent_continue\n''',
    'handleWeatherParentAction')
qg.write_text(s)

# pg = native Clock page. Reuse the exact native page shell for Weather only when the
# bridge-marked pg instance was opened from the new Weather sibling action.
s = pg.read_text()
s = inject_after_locals(
    s,
    '.method public final e0(Ljava/util/List;Landroid/os/Bundle;)V',
    '''    invoke-static {p0, p1}, Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;->populateWeatherPage(Ljava/lang/Object;Ljava/util/List;)Z\n    move-result v0\n    if-eqz v0, :clean131_clock_population_continue\n    return-void\n\n    :clean131_clock_population_continue\n''',
    'populateWeatherPage')
s = inject_after_locals(
    s,
    '.method public final i0(Lmu1;)V',
    '''    invoke-static {p0, p1}, Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;->handleWeatherPageAction(Ljava/lang/Object;Ljava/lang/Object;)Z\n    move-result v0\n    if-eqz v0, :clean131_clock_handler_continue\n    return-void\n\n    :clean131_clock_handler_continue\n''',
    'handleWeatherPageAction')

if '.method public G0()Ljava/lang/String;' in s or '.method public final G0()Ljava/lang/String;' in s:
    raise SystemExit('pg unexpectedly already overrides G0')
weather_title = '''\n\n.method public G0()Ljava/lang/String;\n    .locals 1\n\n    invoke-static {p0}, Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;->isWeatherPage(Ljava/lang/Object;)Z\n    move-result v0\n    if-eqz v0, :clean131_normal_title\n\n    const-string v0, "Weather"\n    return-object v0\n\n    :clean131_normal_title\n    invoke-super {p0}, Lcb3;->G0()Ljava/lang/String;\n    move-result-object v0\n    return-object v0\n.end method\n'''
s = s.rstrip() + weather_title
pg.write_text(s)

qt = qg.read_text()
pt = pg.read_text()
if qt.count('addWeatherParentAction') != qg_return_count:
    raise SystemExit(f'qg: expected {qg_return_count} addWeatherParentAction hooks, got {qt.count("addWeatherParentAction")}')
if qt.count('handleWeatherParentAction') != 1:
    raise SystemExit('qg: parent handler hook count mismatch')
for token in ['populateWeatherPage', 'handleWeatherPageAction', 'isWeatherPage', 'const-string v0, "Weather"']:
    if pt.count(token) != 1:
        raise SystemExit(f'pg: expected exactly one {token}, got {pt.count(token)}')

print(f'PASS: CLEAN13.1C qg Weather sibling patched on all {qg_return_count} qg.e0 exit paths + pg native Weather page')
