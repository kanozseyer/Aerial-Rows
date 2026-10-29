#!/usr/bin/env python3
import hashlib
import sys
import zipfile

if len(sys.argv) != 3:
    raise SystemExit('usage: verify-clean13-1.py BASE CANDIDATE')
base, cand = sys.argv[1:]

def members(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename: z.read(i.filename) for i in z.infolist() if not i.filename.startswith('META-INF/')}

b = members(base)
c = members(cand)
if set(b) != set(c):
    raise SystemExit('archive member set changed')
changed = sorted(k for k in b if b[k] != c[k])
if changed != ['classes2.dex', 'classes4.dex']:
    raise SystemExit('unexpected changed members: ' + repr(changed))

# classes2: only native Status Bar/Clock page hooks.
d2 = c['classes2.dex']
for token in [
    b'WeatherStatusBarBridge',
    b'addWeatherParentAction',
    b'handleWeatherParentAction',
    b'populateWeatherPage',
    b'handleWeatherPageAction',
    b'isWeatherPage',
    b'StatusBarCustomizationBridge',
    b'addClockDateActions',
    b'handleClockDateAction',
]:
    if token not in d2:
        raise SystemExit('classes2 marker missing: ' + token.decode(errors='replace'))

# classes4: corrected weather implementation + full frozen stack.
d4 = c['classes4.dex']
required = [
    b'ProjectivyWeather',
    b'CLEAN13.1B Weather sibling added to Status Bar',
    b'CLEAN13.1B Weather native page opened',
    b'CLEAN13.1D Weather native page populated',
    b'CLEAN13.1B location dialog opened',
    b'CLEAN13.1B status order=settings/status icons > weather > date > clock',
    b'CLEAN13.1B idle pill watcher attached',
    b'CLEAN13.1B idle pill iconsCollapsed=',
    b'CLEAN13.1D show weather=',
    b'CLEAN13.1D manual refresh requested',
    b'CLEAN13.1D geocoded via',
    b'CLEAN13.1 weather refreshed',
    b'WeatherMenuDrawable',
    b'Weather location',
    b'Temperature unit',
    b'Open-Meteo',
    b'CC BY 4.0',
    b'api.postcodes.io/postcodes/',
    b'geocoding-api.open-meteo.com/v1/search',
    b'api.open-meteo.com/v1/forecast',
    b'temperature_2m,weather_code,is_day',
    b'CLEAN13D2 native clock/date actions added; unified default',
    b'Combined with status icons',
    b'CLEAN13C native clock APPLIED before=',
    b'CLEAN12.1 SUCCESS channels=',
    b'CLEAN12 discovery COMPLETE attempt=',
    b'CLEAN12 Kodi provider retention lease=',
    b'CLEAN10.3 browse clean uri=',
]
for token in required:
    if token not in d4:
        raise SystemExit('classes4 marker missing: ' + token.decode(errors='replace'))

# Weather remains manual city/postcode only.
for forbidden in [
    b'ACCESS_FINE_LOCATION',
    b'ACCESS_COARSE_LOCATION',
    b'LocationManager',
    b'FusedLocationProvider',
]:
    if forbidden in d4:
        raise SystemExit('forbidden location marker present: ' + forbidden.decode())

print('PASS: CLEAN13.1D scope exact: classes2.dex + classes4.dex only')
print('classes2 sha256', hashlib.sha256(d2).hexdigest())
print('classes4 sha256', hashlib.sha256(d4).hexdigest())
