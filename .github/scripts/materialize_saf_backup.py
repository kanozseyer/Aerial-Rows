from pathlib import Path

path = Path('app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt')
text = path.read_text()

old = '''    fun saveConfig() {
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    dir.mkdirs()
                    File(dir, "CouchyBackup.json").writeText(format.encodeToString(LauncherConfig.serializer(), config))
                }
            }
            Actions.toast(context, "Saved to Downloads/CouchyBackup.json")
        }
    }

    val focus = rememberSettingsFocusBindings(
'''
new = '''    val savePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        val payload = format.encodeToString(LauncherConfig.serializer(), config)
                        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                            writer.write(payload)
                        } ?: error("Could not open backup destination")
                    }.isSuccess
                }
                if (saved) {
                    Actions.toast(context, context.getString(R.string.toast_config_saved))
                } else {
                    Actions.toast(context, "Could not save configuration")
                }
            }
        }
    }

    val focus = rememberSettingsFocusBindings(
'''
if old not in text:
    raise SystemExit('saveConfig anchor missing')
text = text.replace(old, new, 1)

old = '''        SettingsItem(
            selected = false,
            onClick = { saveConfig() },
            modifier = focus.modifier(LauncherSettingIds.SaveConfig),
'''
new = '''        SettingsItem(
            selected = false,
            onClick = {
                runCatching { savePicker.launch("CouchyBackup.json") }
                    .onFailure { Actions.toast(context, context.getString(R.string.toast_no_picker)) }
            },
            modifier = focus.modifier(LauncherSettingIds.SaveConfig),
'''
if old not in text:
    raise SystemExit('save row anchor missing')
text = text.replace(old, new, 1)

path.write_text(text)
