from pathlib import Path

path = Path('app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt')
text = path.read_text()
old = 'context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->'
new = 'context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { writer ->'
if old not in text:
    raise SystemExit('SAF output mode anchor missing')
path.write_text(text.replace(old, new, 1))
