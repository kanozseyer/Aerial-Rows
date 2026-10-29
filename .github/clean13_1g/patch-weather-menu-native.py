#!/usr/bin/env python3
from pathlib import Path
import re, sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-weather-menu-native.py APKTOOL_ROOT')
root = Path(sys.argv[1])
qg = next(iter(root.rglob('qg.smali')), None)
pg = next(iter(root.rglob('pg.smali')), None)
if qg is None or pg is None:
    raise SystemExit(f'missing qg/pg: qg={qg} pg={pg}')


def method_span(text, sig):
    start = text.find(sig)
    if start < 0:
        raise SystemExit('method not found: ' + sig)
    end = text.find('.end method', start)
    if end < 0:
        raise SystemExit('method end not found: ' + sig)
    end += len('.end method')
    return start, end


def inject_after_locals(text, sig, injection, marker):
    start, end = method_span(text, sig)
    block = text[start:end]
    if marker in block:
        raise SystemExit('already patched: ' + marker)
    m = re.search(r'(^\s*\.locals\s+)(\d+)(\s*$)', block, re.M)
    if not m:
        raise SystemExit('locals not found: ' + sig)
    if int(m.group(2)) < 1:
        a, b = m.span(2)
        block = block[:a] + '1' + block[b:]
        m = re.search(r'(^\s*\.locals\s+\d+\s*$)', block, re.M)
    pos = m.end()
    block = block[:pos] + '\n\n' + injection + block[pos:]
    return text[:start] + block + text[end:]


def patch_qg_population(text):
    sig = '.method public final e0(Ljava/util/List;Landroid/os/Bundle;)V'
    start, end = method_span(text, sig)
    block = text[start:end]
    if 'addWeatherParentAction' in block:
        raise SystemExit('qg.e0 already patched')
    m = re.search(r'(^\s*\.locals\s+)(\d+)(\s*$)', block, re.M)
    if not m:
        raise SystemExit('qg.e0 locals not found')
    original = int(m.group(2))
    if original != 6:
        raise SystemExit(f'qg.e0 unexpected locals={original}; frozen CLEAN13 expected 6')
    a, b = m.span(2)
    block = block[:a] + '7' + block[b:]
    m2 = re.search(r'(^\s*\.locals\s+7\s*$)', block, re.M)
    pos = m2.end()
    block = block[:pos] + '\n\n    move-object v6, p0\n' + block[pos:]
    needle = '    return-void'
    if block.count(needle) != 1:
        raise SystemExit(f'qg.e0 expected one return, got {block.count(needle)}')
    call = ('    invoke-static {v6, p1}, '
            'Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;'
            '->addWeatherParentAction(Ljava/lang/Object;Ljava/util/List;)V\n\n')
    block = block.replace(needle, call + needle, 1)
    return text[:start] + block + text[end:]


def patch_pg_population_at_end(text):
    sig = '.method public final e0(Ljava/util/List;Landroid/os/Bundle;)V'
    start, end = method_span(text, sig)
    block = text[start:end]
    if 'finishPopulateWeatherPage' in block:
        raise SystemExit('pg.e0 already patched')
    if 'populateWeatherPage' in block:
        raise SystemExit('pg.e0 contains failed early Weather population hook')

    # Frozen CLEAN13 pg.e0 has 11 locals and repurposes p0 as lu1 near the end.
    # Preserve the real pg fragment in v11, let the entire native method run, then
    # replace the generated Clock rows only for the bridge-marked Weather page.
    m = re.search(r'(^\s*\.locals\s+)(\d+)(\s*$)', block, re.M)
    if not m:
        raise SystemExit('pg.e0 locals not found')
    original = int(m.group(2))
    if original != 11:
        raise SystemExit(f'pg.e0 unexpected locals={original}; frozen CLEAN13 expected 11')
    a, b = m.span(2)
    block = block[:a] + '12' + block[b:]
    m2 = re.search(r'(^\s*\.locals\s+12\s*$)', block, re.M)
    pos = m2.end()
    block = block[:pos] + '\n\n    move-object v11, p0\n' + block[pos:]

    needle = '    return-void'
    if block.count(needle) != 1:
        raise SystemExit(f'pg.e0 expected one return, got {block.count(needle)}')
    call = ('    invoke-static {v11, p1}, '
            'Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;'
            '->finishPopulateWeatherPage(Ljava/lang/Object;Ljava/util/List;)V\n\n')
    block = block.replace(needle, call + needle, 1)

    # Structural guard: the Weather hook must be at the end, after native Clock
    # population, never immediately after .locals as in the failed F approach.
    if block.index('finishPopulateWeatherPage') < block.rfind('Lcb3;->z0(Llu1;)Lmu1;'):
        raise SystemExit('Weather population hook is not after native pg population')
    return text[:start] + block + text[end:]


s = qg.read_text()
s = patch_qg_population(s)
s = inject_after_locals(
    s,
    '.method public final i0(Lmu1;)V',
    '''    invoke-static {p0, p1}, Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;->handleWeatherParentAction(Ljava/lang/Object;Ljava/lang/Object;)Z\n    move-result v0\n    if-eqz v0, :clean131g_parent_continue\n    return-void\n\n    :clean131g_parent_continue\n''',
    'handleWeatherParentAction')
qg.write_text(s)

s = pg.read_text()
s = patch_pg_population_at_end(s)
s = inject_after_locals(
    s,
    '.method public final i0(Lmu1;)V',
    '''    invoke-static {p0, p1}, Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;->handleWeatherPageAction(Ljava/lang/Object;Ljava/lang/Object;)Z\n    move-result v0\n    if-eqz v0, :clean131g_clock_handler_continue\n    return-void\n\n    :clean131g_clock_handler_continue\n''',
    'handleWeatherPageAction')

if '.method public G0()Ljava/lang/String;' in s or '.method public final G0()Ljava/lang/String;' in s:
    raise SystemExit('pg unexpectedly already overrides G0')
weather_title = '''\n\n.method public G0()Ljava/lang/String;\n    .locals 1\n\n    invoke-static {p0}, Lcom/spocky/projengmenu/embedded/WeatherStatusBarBridge;->isWeatherPage(Ljava/lang/Object;)Z\n    move-result v0\n    if-eqz v0, :clean131g_normal_title\n\n    const-string v0, "Weather"\n    return-object v0\n\n    :clean131g_normal_title\n    invoke-super {p0}, Lcb3;->G0()Ljava/lang/String;\n    move-result-object v0\n    return-object v0\n.end method\n'''
s = s.rstrip() + weather_title
pg.write_text(s)

qt = qg.read_text()
pt = pg.read_text()
for token in ['move-object v6, p0', 'addWeatherParentAction', 'handleWeatherParentAction']:
    if qt.count(token) != 1:
        raise SystemExit(f'qg expected one {token}, got {qt.count(token)}')
for token in ['move-object v11, p0', 'finishPopulateWeatherPage', 'handleWeatherPageAction', 'isWeatherPage']:
    if pt.count(token) != 1:
        raise SystemExit(f'pg expected one {token}, got {pt.count(token)}')
if 'populateWeatherPage' in pt:
    raise SystemExit('failed early populateWeatherPage hook remains')

print('PASS: CLEAN13.1G Weather hooks installed; pg population runs to native final return first')
