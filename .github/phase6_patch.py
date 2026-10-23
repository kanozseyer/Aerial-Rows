from pathlib import Path


def once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Missing patch anchor: {label}")
    return text.replace(old, new, 1)


def slice_section(text: str, start: str, end: str):
    a = text.index(start)
    b = text.index(end, a)
    return a, b, text[a:b]


settings_path = Path("app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt")
text = settings_path.read_text()
text = once(text, "import com.conreo.couchytv.core.settings.DisplaySettingDescriptors\n", "import com.conreo.couchytv.core.settings.AppSettingsIds\nimport com.conreo.couchytv.core.settings.CategorySettingsIds\nimport com.conreo.couchytv.core.settings.DisplaySettingDescriptors\n", "settings imports")
text = once(text, "import com.conreo.couchytv.data.ConfigStore\n", "import com.conreo.couchytv.data.ConfigDefaults\nimport com.conreo.couchytv.data.ConfigImportResult\nimport com.conreo.couchytv.data.ConfigStore\nimport com.conreo.couchytv.data.ConfigTransfer\n", "config imports")
text = text.replace("import kotlinx.serialization.json.Json\n", "")
text = text.replace("\nprivate val format = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }\n", "\n")

text = once(text, "                        config = config,\n                        onBack = { navigation = navigation.back() },\n                        onLaunch = { pkg ->", "                        config = config,\n                        rememberedFocus = navigation.rememberedFocus(SettingsRoutes.Apps),\n                        onFocus = { id -> navigation = navigation.rememberFocus(SettingsRoutes.Apps, id) },\n                        onBack = { navigation = navigation.back() },\n                        onLaunch = { pkg ->", "apps route")
text = once(text, "                    SettingsRoutes.Channels -> ChannelsSettingsScreen(\n                        config = config,\n                        store = store,\n                    )", "                    SettingsRoutes.Channels -> ChannelsSettingsScreen(\n                        config = config,\n                        store = store,\n                        rememberedFocus = navigation.rememberedFocus(SettingsRoutes.Channels),\n                        onFocus = { id -> navigation = navigation.rememberFocus(SettingsRoutes.Channels, id) },\n                    )", "channels route")
text = once(text, "                    SettingsRoutes.Categories -> CategoriesScreen(\n                        config = config,\n                        apps = apps,\n                        store = store,\n                        onBack = { navigation = navigation.back() },\n                    )", "                    SettingsRoutes.Categories -> CategoriesScreen(\n                        config = config,\n                        apps = apps,\n                        store = store,\n                        rememberedFocus = navigation.rememberedFocus(SettingsRoutes.Categories),\n                        onFocus = { id -> navigation = navigation.rememberFocus(SettingsRoutes.Categories, id) },\n                        onBack = { navigation = navigation.back() },\n                    )", "categories route")

a, b, apps = slice_section(text, "@Composable\nprivate fun AppsScreen(", "/** Small leading artwork")
apps = once(apps, "    config: LauncherConfig,\n    onBack: () -> Unit,\n", "    config: LauncherConfig,\n    rememberedFocus: SettingId?,\n    onFocus: (SettingId) -> Unit,\n    onBack: () -> Unit,\n", "apps signature")
apps = once(apps, "    val catNames = remember(config.categories) { config.categories.associate { it.id to it.name } }\n\n", "    val catNames = remember(config.categories) { config.categories.associate { it.id to it.name } }\n    val appFocusIds = remember(apps) { apps.map { AppSettingsIds.app(it.pkg, it.componentName) } }\n    val appFocus = appFocusIds.takeIf { it.isNotEmpty() }?.let { ids ->\n        rememberSettingsFocusBindings(\n            ids = ids,\n            rememberedFocus = rememberedFocus,\n            defaultFocus = ids.first(),\n            onFocused = onFocus,\n        )\n    }\n\n", "apps focus bindings")
apps = once(apps, "        val f = initialFocus()\n", "", "apps initial focus")
apps = once(apps, "                            modifier = if (i == 0) Modifier.focusRequester(f) else Modifier,\n", "                            modifier = appFocus?.modifier(AppSettingsIds.app(app.pkg, app.componentName)) ?: Modifier,\n", "apps row focus")
text = text[:a] + apps + text[b:]

a, b, launcher = slice_section(text, "@Composable\nprivate fun LauncherSettingsSubscreen(", "/* ------------------------------ language")
save_start = launcher.index("    fun saveConfig() {")
save_end = launcher.index("    val focus = rememberSettingsFocusBindings", save_start)
save_picker = '''    val savePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                            writer.write(ConfigTransfer.encode(config))
                        } ?: error("Could not open destination")
                    }.isSuccess
                }
                Actions.toast(
                    context,
                    context.getString(if (saved) R.string.toast_config_saved else R.string.toast_config_save_failed),
                )
            }
        }
    }

'''
launcher = launcher[:save_start] + save_picker + launcher[save_end:]
load_start = launcher.index("                val loaded = withContext(Dispatchers.IO) {")
load_end = launcher.index("            }\n        }\n    }\n\n    Column(", load_start)
load_body = '''                val imported = withContext(Dispatchers.IO) {
                    runCatching {
                        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: return@runCatching ConfigImportResult.Invalid
                        ConfigTransfer.decodeForRestore(
                            raw = raw,
                            fallbackCategories = ConfigDefaults.defaultCategories(context),
                            knownApps = config.knownApps,
                        )
                    }.getOrDefault(ConfigImportResult.Invalid)
                }
                when (imported) {
                    is ConfigImportResult.Success -> {
                        store.update { imported.config }
                        Actions.toast(context, context.getString(R.string.toast_config_loaded))
                    }
                    is ConfigImportResult.UnsupportedSchema ->
                        Actions.toast(context, context.getString(R.string.toast_config_newer))
                    ConfigImportResult.Invalid ->
                        Actions.toast(context, context.getString(R.string.toast_config_bad))
                }
'''
launcher = launcher[:load_start] + load_body + launcher[load_end:]
launcher = once(launcher, "            onClick = { saveConfig() },\n", "            onClick = {\n                runCatching { savePicker.launch(\"AerialRowsConfig.json\") }\n                    .onFailure { Actions.toast(context, context.getString(R.string.toast_no_picker)) }\n            },\n", "save click")
text = text[:a] + launcher + text[b:]

a, b, categories = slice_section(text, "@Composable\nprivate fun CategoriesScreen(", "/** Pencil dialog")
categories = once(categories, "    store: ConfigStore,\n    onBack: () -> Unit,\n", "    store: ConfigStore,\n    rememberedFocus: SettingId?,\n    onFocus: (SettingId) -> Unit,\n    onBack: () -> Unit,\n", "categories signature")
categories = once(categories, "    var appsFor by remember { mutableStateOf<CategoryCfg?>(null) }\n\n", "    var appsFor by remember { mutableStateOf<CategoryCfg?>(null) }\n    val categoryFocusIds = remember(config.categories) {\n        buildList {\n            add(CategorySettingsIds.AllApps)\n            config.categories.filter { it.id != AppRepository.ALL_APPS_ID }\n                .forEach { add(CategorySettingsIds.category(it.id)) }\n            add(CategorySettingsIds.AddCategory)\n        }\n    }\n    val categoryFocus = rememberSettingsFocusBindings(\n        ids = categoryFocusIds,\n        rememberedFocus = rememberedFocus,\n        defaultFocus = CategorySettingsIds.AllApps,\n        onFocused = onFocus,\n    )\n\n", "category focus bindings")
categories = once(categories, "        val f = initialFocus()\n", "", "category initial focus")
categories = once(categories, "                modifier = Modifier.focusRequester(f),\n", "                modifier = categoryFocus.modifier(CategorySettingsIds.AllApps),\n", "all apps focus")
categories = once(categories, "                        onClick = { appsFor = cat },\n                        headlineContent = { Text(cat.name) },\n", "                        onClick = { appsFor = cat },\n                        modifier = categoryFocus.modifier(CategorySettingsIds.category(cat.id)),\n                        headlineContent = { Text(cat.name) },\n", "category row focus")
categories = once(categories, "            headlineContent = { Text(stringResource(R.string.add_section)) },\n", "            modifier = categoryFocus.modifier(CategorySettingsIds.AddCategory),\n            headlineContent = { Text(stringResource(R.string.add_section)) },\n", "add category focus")
text = text[:a] + categories + text[b:]
settings_path.write_text(text)

extras_path = Path("app/src/main/java/com/conreo/couchytv/ui/AerialSettingsExtras.kt")
text = extras_path.read_text()
text = once(text, "import com.conreo.couchytv.core.settings.DisplaySettingDescriptors\n", "import com.conreo.couchytv.core.settings.ChannelSettingsIds\nimport com.conreo.couchytv.core.settings.DisplaySettingDescriptors\nimport com.conreo.couchytv.core.settings.SettingId\n", "channel imports")
text = once(text, "fun ChannelsSettingsScreen(\n    config: LauncherConfig,\n    store: ConfigStore,\n) {\n", "fun ChannelsSettingsScreen(\n    config: LauncherConfig,\n    store: ConfigStore,\n    rememberedFocus: SettingId?,\n    onFocus: (SettingId) -> Unit,\n) {\n", "channel signature")
text = once(text, "    val watchNextProviders by produceState(emptyList<com.conreo.couchytv.data.TvProviderInfo>(), channelRefresh) {\n        value = TvContentRepository.discoverWatchNextProviders(context)\n    }\n\n", "    val watchNextProviders by produceState(emptyList<com.conreo.couchytv.data.TvProviderInfo>(), channelRefresh) {\n        value = TvContentRepository.discoverWatchNextProviders(context)\n    }\n    val channelFocusIds = remember(config.showWatchNext, watchNextProviders, channels) {\n        buildList {\n            add(ChannelSettingsIds.WatchNext)\n            if (config.showWatchNext) {\n                add(ChannelSettingsIds.WatchNextLimit)\n                watchNextProviders.forEach { add(ChannelSettingsIds.watchNextProvider(it.packageName)) }\n            }\n            add(ChannelSettingsIds.PreviewChannels)\n            add(ChannelSettingsIds.DefaultChannelLimit)\n            channels.forEach { channel ->\n                add(ChannelSettingsIds.channel(channel.key))\n                if (channel.browsable && channel.programs.isNotEmpty()) add(ChannelSettingsIds.channelLimit(channel.key))\n            }\n        }\n    }\n    val channelFocus = rememberSettingsFocusBindings(\n        ids = channelFocusIds,\n        rememberedFocus = rememberedFocus,\n        defaultFocus = ChannelSettingsIds.WatchNext,\n        onFocused = onFocus,\n    )\n\n", "channel bindings")
text = once(text, "        val focus = extraInitialFocus()\n", "", "channel initial focus")
text = once(text, "            modifier = Modifier.focusRequester(focus),\n", "            modifier = channelFocus.modifier(ChannelSettingsIds.WatchNext),\n", "watch next focus")
text = once(text, "                current = capIndex,\n                onSelect = { i ->\n", "                current = capIndex,\n                modifier = channelFocus.modifier(ChannelSettingsIds.WatchNextLimit),\n                onSelect = { i ->\n", "watch next limit focus")
text = once(text, "                        checked = enabled,\n                        onClick = {\n", "                        checked = enabled,\n                        modifier = channelFocus.modifier(ChannelSettingsIds.watchNextProvider(provider.packageName)),\n                        onClick = {\n", "provider focus")
text = once(text, "            checked = config.showPreviewChannels,\n            onClick = {\n", "            checked = config.showPreviewChannels,\n            modifier = channelFocus.modifier(ChannelSettingsIds.PreviewChannels),\n            onClick = {\n", "preview focus")
text = once(text, "            current = current,\n            onSelect = { i -> scope.launch { store.update { it.copy(channelItemLimit = limits[i]) } } },\n", "            current = current,\n            modifier = channelFocus.modifier(ChannelSettingsIds.DefaultChannelLimit),\n            onSelect = { i -> scope.launch { store.update { it.copy(channelItemLimit = limits[i]) } } },\n", "default limit focus")
text = once(text, "                    checked = channel.browsable && !hidden,\n                    onClick = {\n", "                    checked = channel.browsable && !hidden,\n                    modifier = channelFocus.modifier(ChannelSettingsIds.channel(channel.key)),\n                    onClick = {\n", "channel focus")
text = once(text, "                        current = index,\n                        onSelect = { i ->\n", "                        current = index,\n                        modifier = channelFocus.modifier(ChannelSettingsIds.channelLimit(channel.key)),\n                        onSelect = { i ->\n", "channel limit focus")
extras_path.write_text(text)

descriptor_path = Path("app/src/main/java/com/conreo/couchytv/core/settings/SettingsDescriptors.kt")
descriptor_path.write_text(descriptor_path.read_text().replace("SettingEffect.RELOAD_WALLPAPER", "SettingEffect.RECOMPOSE"))

strings_path = Path("app/src/main/res/values/strings.xml")
text = strings_path.read_text()
text = once(text, "    <string name=\"toast_config_saved\">Configuration saved</string>\n    <string name=\"toast_config_loaded\">Configuration loaded — restart may be needed</string>\n    <string name=\"toast_config_bad\">Invalid or corrupted backup file</string>\n", "    <string name=\"toast_config_saved\">Configuration saved</string>\n    <string name=\"toast_config_save_failed\">Could not save configuration</string>\n    <string name=\"toast_config_loaded\">Configuration loaded</string>\n    <string name=\"toast_config_bad\">Invalid or corrupted backup file</string>\n    <string name=\"toast_config_newer\">This configuration was created by a newer version</string>\n", "config toast strings")
strings_path.write_text(text)
