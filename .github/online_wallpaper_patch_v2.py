from pathlib import Path

source = Path('.github/online_wallpaper_patch.py').read_text()
old = '''replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    ''' + "'''" + '''                                        useCustomWallpaper = false,\n                                        useVideoWallpaper = false,\n                                        useBuiltinAerials = false,\n''' + "'''" + ''',
    ''' + "'''" + '''                                        useCustomWallpaper = false,\n                                        useOnlineWallpaper = false,\n                                        useVideoWallpaper = false,\n                                        useBuiltinAerials = false,\n''' + "'''" + ''',
)'''
new = '''replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    ''' + "'''" + '''                        onPreset = { i ->\n                            scope.launch {\n                                store.update {\n                                    it.copy(\n                                        wallpaper = i,\n                                        useCustomWallpaper = false,\n                                        useVideoWallpaper = false,\n                                        useBuiltinAerials = false,\n''' + "'''" + ''',
    ''' + "'''" + '''                        onPreset = { i ->\n                            scope.launch {\n                                store.update {\n                                    it.copy(\n                                        wallpaper = i,\n                                        useCustomWallpaper = false,\n                                        useOnlineWallpaper = false,\n                                        useVideoWallpaper = false,\n                                        useBuiltinAerials = false,\n''' + "'''" + ''',
)'''
if old not in source:
    raise SystemExit('Expected ambiguous replacement block not found in materializer')
source = source.replace(old, new, 1)
exec(compile(source, '.github/online_wallpaper_patch.py', 'exec'))
