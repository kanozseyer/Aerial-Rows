from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"pattern not found in {path}: {old[:120]!r}")
    if text.count(old) != 1:
        raise SystemExit(f"pattern not unique in {path}: {old[:120]!r} count={text.count(old)}")
    p.write_text(text.replace(old, new, 1))


# Make the Shield build unmistakably identifiable so a stale APK cannot masquerade
# as the Phase 6 follow-up while we verify the SAF document picker behavior.
replace_once(
    "app/build.gradle.kts",
    'versionCode = 8\n        versionName = "1.1.0-ar-dev"',
    'versionCode = 9\n        versionName = "1.1.1-ar-dev"',
)

# Settings -> Categories: reuse the existing row editor, which already writes the
# same per-category order model as the home screen. This gives one source of truth
# for membership + ordering instead of maintaining a second reorder implementation.
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt",
    '''    appsFor?.let { cat ->
        // Keep the dialog's category object in sync with the live config
        val liveCat = config.categories.firstOrNull { it.id == cat.id } ?: cat
        SectionAppsDialog(
            cat = liveCat,
            apps = apps,
            config = config,
            onToggle = { app ->
                scope.launch {
                    store.update { cfg ->
                        val current = AppRepository.sectionsOf(app, cfg)
                        val next = if (liveCat.id in current) current - liveCat.id
                        else current + liveCat.id
                        cfg.copy(sections = cfg.sections + (app.pkg to next))
                    }
                }
            },
            onDismiss = { appsFor = null },
        )
    }
''',
    '''    appsFor?.let { cat ->
        // Keep the editor's category object in sync with the live config. The
        // shared row editor supports both membership and explicit app ordering:
        // OK adds/removes, Left/Right moves an included app earlier/later.
        val liveCat = config.categories.firstOrNull { it.id == cat.id } ?: cat
        RowQuickEditor(
            title = liveCat.name,
            icon = "▦",
            category = liveCat,
            channel = null,
            isWatchNext = false,
            allApps = apps,
            config = config,
            store = store,
            onDismiss = { appsFor = null },
        )
    }
''',
)

# Carousel move mode: after every reorder, first scroll the moving semantic item
# into composition, then restore focus to it. This prevents an off-screen moved
# card from leaving focus visually behind.
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/LauncherScreen.kt",
    '''    val state = rememberLazyListState()
    // Pivot scrolling done RIGHT: instead of letting the default
''',
    '''    val state = rememberLazyListState()
    LaunchedEffect(movePkg, catApps) {
        val pkg = movePkg ?: return@LaunchedEffect
        val index = catApps.indexOfFirst { it.pkg == pkg }
        if (index < 0) return@LaunchedEffect
        state.scrollToItem(index)
        withFrameNanos { }
        runCatching { moveFocus.requestFocus() }
    }
    // Pivot scrolling done RIGHT: instead of letting the default
''',
)

# Sliding Rows uses its own LazyRow, so give it the same semantic move-follow
# behavior. This is the layout where the Shield checkpoint exposed the bug.
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/SlidingRows.kt",
    '''    val itemIds: List<HomeItemId> = remember(row.apps) { row.apps.map(::appHomeItemId) }
    val itemFocus = remember(rowId, itemIds) { itemIds.associateWith { FocusRequester() } }
    val horizontalState = rememberLazyListState()

    LaunchedEffect(restoreItemId, itemIds) {
''',
    '''    val itemIds: List<HomeItemId> = remember(row.apps) { row.apps.map(::appHomeItemId) }
    val itemFocus = remember(rowId, itemIds) { itemIds.associateWith { FocusRequester() } }
    val horizontalState = rememberLazyListState()

    LaunchedEffect(movePkg, row.apps) {
        val pkg = movePkg ?: return@LaunchedEffect
        val index = row.apps.indexOfFirst { it.pkg == pkg }
        if (index < 0) return@LaunchedEffect
        horizontalState.scrollToItem(index)
        withFrameNanos { }
        runCatching { moveFocus.requestFocus() }
    }

    LaunchedEffect(restoreItemId, itemIds) {
''',
)

# Persist per-channel hidden programme identities. This is additive/defaulted, so
# old configs continue decoding without a schema migration.
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/Config.kt",
    '''    /** Optional per-channel item limit, keyed by TvChannelRow.key. */
    val channelItemLimits: Map<String, Int> = emptyMap(),

    // ----- home row organization -----
''',
    '''    /** Optional per-channel item limit, keyed by TvChannelRow.key. */
    val channelItemLimits: Map<String, Int> = emptyMap(),
    /** Programme identities hidden by the user, keyed by stable TV channel key. */
    val hiddenTvPrograms: Map<String, Set<String>> = emptyMap(),

    // ----- home row organization -----
''',
)

# Normalize programme-hide maps so imported/legacy configs cannot retain blank
# keys or empty sets forever.
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/ConfigNormalizer.kt",
    '''        val excludedWatchNextProviders = config.excludedWatchNextProviders
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toCollection(linkedSetOf())

        return config.copy(
''',
    '''        val excludedWatchNextProviders = config.excludedWatchNextProviders
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toCollection(linkedSetOf())
        val hiddenTvPrograms = config.hiddenTvPrograms
            .asSequence()
            .mapNotNull { (channelKey, programKeys) ->
                val key = channelKey.trim()
                val values = programKeys.asSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toCollection(linkedSetOf())
                if (key.isEmpty() || values.isEmpty()) null else key to values
            }
            .toMap()

        return config.copy(
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/ConfigNormalizer.kt",
    '''            channelItemLimit = config.channelItemLimit.coerceIn(4, 30),
            channelItemLimits = config.channelItemLimits.mapValues { (_, value) -> value.coerceIn(4, 30) },
            homeRowOrder = config.homeRowOrder.distinct(),
''',
    '''            channelItemLimit = config.channelItemLimit.coerceIn(4, 30),
            channelItemLimits = config.channelItemLimits.mapValues { (_, value) -> value.coerceIn(4, 30) },
            hiddenTvPrograms = hiddenTvPrograms,
            homeRowOrder = config.homeRowOrder.distinct(),
''',
)

# Stable programme identity + pure filtering policy. Prefer the provider intent URI
# because provider row IDs can be recreated; fall back to the numeric row ID.
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/TvContentRepository.kt",
    '''data class WatchNextProviderPolicy(
    val maxItemsPerProvider: Int,
    val excludedPackages: Set<String>,
)

class WatchNextProviderLimiter(policy: WatchNextProviderPolicy) {
''',
    '''data class WatchNextProviderPolicy(
    val maxItemsPerProvider: Int,
    val excludedPackages: Set<String>,
)

fun tvProgramStableKey(item: TvProgramItem, ownerFallback: String = ""): String {
    val owner = item.packageName.ifBlank { ownerFallback }.ifBlank { "unknown" }
    val identity = item.intentUri.trim().takeIf(String::isNotEmpty) ?: "id:${item.id}"
    return "$owner|$identity"
}

fun visibleTvPrograms(
    programs: List<TvProgramItem>,
    hiddenKeys: Set<String>,
    ownerFallback: String,
    limit: Int,
): List<TvProgramItem> = programs
    .asSequence()
    .filterNot { tvProgramStableKey(it, ownerFallback) in hiddenKeys }
    .take(limit.coerceIn(1, 30))
    .toList()

class WatchNextProviderLimiter(policy: WatchNextProviderPolicy) {
''',
)

# Home queries apply the saved per-channel programme hides. Discovery deliberately
# ignores them and fetches up to 30 items so Settings can show what is available.
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/TvContentRepository.kt",
    '''                    hidden = config.hiddenTvChannels,
                    perChannelLimits = config.channelItemLimits,
                    mode = TvChannelQueryMode.HOME,
''',
    '''                    hidden = config.hiddenTvChannels,
                    perChannelLimits = config.channelItemLimits,
                    hiddenPrograms = config.hiddenTvPrograms,
                    mode = TvChannelQueryMode.HOME,
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/TvContentRepository.kt",
    '''                defaultLimit = 4,
                hidden = emptySet(),
                perChannelLimits = emptyMap(),
                mode = TvChannelQueryMode.DISCOVERY,
''',
    '''                defaultLimit = 30,
                hidden = emptySet(),
                perChannelLimits = emptyMap(),
                hiddenPrograms = emptyMap(),
                mode = TvChannelQueryMode.DISCOVERY,
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/TvContentRepository.kt",
    '''        hidden: Set<String>,
        perChannelLimits: Map<String, Int>,
        mode: TvChannelQueryMode,
    ): List<TvChannelRow> {
''',
    '''        hidden: Set<String>,
        perChannelLimits: Map<String, Int>,
        hiddenPrograms: Map<String, Set<String>>,
        mode: TvChannelQueryMode,
    ): List<TvChannelRow> {
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/data/TvContentRepository.kt",
    '''                val programs = runCatching {
                    queryPrograms(
                        context = context,
                        uri = TvContract.buildPreviewProgramsUriForChannel(id),
                        limit = limit,
                        kind = TvProgramQueryKind.PREVIEW_CHANNEL,
                    )
                }.getOrDefault(emptyList())
                if (!shouldIncludeTvChannel(mode, browsable, programs.isNotEmpty())) continue
''',
    '''                val discoveredPrograms = runCatching {
                    queryPrograms(
                        context = context,
                        uri = TvContract.buildPreviewProgramsUriForChannel(id),
                        // Fetch enough candidates to refill the visible row after
                        // user-hidden cards are removed. Discovery also needs the
                        // fuller set for its content editor.
                        limit = 30,
                        kind = TvProgramQueryKind.PREVIEW_CHANNEL,
                    )
                }.getOrDefault(emptyList())
                val hiddenProgramKeys = hiddenPrograms[identity.key].orEmpty() +
                    hiddenPrograms[identity.legacyKey].orEmpty()
                val programs = visibleTvPrograms(
                    programs = discoveredPrograms,
                    hiddenKeys = if (mode == TvChannelQueryMode.HOME) hiddenProgramKeys else emptySet(),
                    ownerFallback = packageName,
                    limit = if (mode == TvChannelQueryMode.DISCOVERY) 30 else limit,
                )
                if (!shouldIncludeTvChannel(mode, browsable, programs.isNotEmpty())) continue
''',
)

# RowQuickEditor: a channel can now hide/unhide individual published cards as
# well as toggle the row and set its size. The provider still owns the raw feed
# and source ordering; Aerial Rows applies a user-side visibility policy.
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/RowQuickEditor.kt",
    '''import com.conreo.couchytv.data.TvChannelRow
import kotlinx.coroutines.launch
''',
    '''import com.conreo.couchytv.data.TvChannelRow
import com.conreo.couchytv.data.tvProgramStableKey
import kotlinx.coroutines.launch
''',
)
old_channel_editor = '''@Composable
private fun ChannelContentsEditor(
    channel: TvChannelRow,
    config: LauncherConfig,
    store: ConfigStore,
) {
    val scope = rememberCoroutineScope()
    val hidden = channel.key in config.hiddenTvChannels
    val limits = listOf(4, 6, 8, 10, 12, 16, 20, 24, 30)
    val currentLimit = config.channelItemLimits[channel.key] ?: config.channelItemLimit
    val currentIndex = limits.indices.minByOrNull { kotlin.math.abs(limits[it] - currentLimit) } ?: 4

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        QuickToggleRow(
            title = "Show this channel",
            subtitle = channel.packageName.ifBlank { "Android TV provider channel" },
            checked = !hidden,
            onClick = {
                scope.launch {
                    store.update { cfg ->
                        cfg.copy(
                            hiddenTvChannels = if (channel.key in cfg.hiddenTvChannels)
                                cfg.hiddenTvChannels - channel.key
                            else cfg.hiddenTvChannels + channel.key
                        )
                    }
                }
            },
        )
        QuickSelectorRow(
            title = "Cards shown",
            subtitle = "Provider order is preserved; this limits how much of the channel is displayed",
            value = limits[currentIndex].toString(),
            current = currentIndex,
            steps = limits.size,
            onSelect = { i ->
                scope.launch {
                    store.update { cfg ->
                        cfg.copy(channelItemLimits = cfg.channelItemLimits + (channel.key to limits[i]))
                    }
                }
            },
        )
        Text(
            "The provider app controls which programmes belong to this channel and their order.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp, top = 8.dp),
        )
    }
}
'''
new_channel_editor = '''@Composable
private fun ChannelContentsEditor(
    channel: TvChannelRow,
    config: LauncherConfig,
    store: ConfigStore,
) {
    val scope = rememberCoroutineScope()
    val hidden = channel.isHiddenBy(config.hiddenTvChannels)
    val limits = listOf(4, 6, 8, 10, 12, 16, 20, 24, 30)
    val currentLimit = channel.configuredItemLimit(config.channelItemLimits, config.channelItemLimit)
    val currentIndex = limits.indices.minByOrNull { kotlin.math.abs(limits[it] - currentLimit) } ?: 4
    val hiddenProgramKeys = config.hiddenTvPrograms[channel.key].orEmpty() +
        config.hiddenTvPrograms[channel.legacyKey].orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        QuickToggleRow(
            title = "Show this channel",
            subtitle = channel.packageName.ifBlank { "Android TV provider channel" },
            checked = !hidden,
            onClick = {
                scope.launch {
                    store.update { cfg ->
                        val aliases = setOf(channel.key, channel.legacyKey)
                        cfg.copy(
                            hiddenTvChannels = if (channel.isHiddenBy(cfg.hiddenTvChannels)) {
                                cfg.hiddenTvChannels - aliases
                            } else {
                                (cfg.hiddenTvChannels - channel.legacyKey) + channel.key
                            },
                        )
                    }
                }
            },
        )
        QuickSelectorRow(
            title = "Cards shown",
            subtitle = "Left/Right changes this row without creating a second focus stop in Channels settings",
            value = limits[currentIndex].toString(),
            current = currentIndex,
            steps = limits.size,
            onSelect = { i ->
                scope.launch {
                    store.update { cfg ->
                        cfg.copy(
                            channelItemLimits = (cfg.channelItemLimits - channel.legacyKey) +
                                (channel.key to limits[i]),
                        )
                    }
                }
            },
        )
        Text(
            "CHANNEL CONTENT  •  OK toggles an item",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp, top = 8.dp),
        )
        if (channel.programs.isEmpty()) {
            Text(
                "This provider is not currently publishing any readable programmes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxWidth().height(280.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                itemsIndexed(channel.programs, key = { _, item -> tvProgramStableKey(item, channel.packageName) }) { _, item ->
                    val programKey = tvProgramStableKey(item, channel.packageName)
                    val visible = programKey !in hiddenProgramKeys
                    ListItem(
                        selected = false,
                        onClick = {
                            scope.launch {
                                store.update { cfg ->
                                    val existing = cfg.hiddenTvPrograms[channel.key].orEmpty() +
                                        cfg.hiddenTvPrograms[channel.legacyKey].orEmpty()
                                    val next = if (programKey in existing) existing - programKey else existing + programKey
                                    val migrated = cfg.hiddenTvPrograms - channel.legacyKey - channel.key
                                    cfg.copy(
                                        hiddenTvPrograms = if (next.isEmpty()) migrated
                                        else migrated + (channel.key to next),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        headlineContent = { Text(item.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        supportingContent = if (item.subtitle.isBlank()) null else ({
                            Text(item.subtitle, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }),
                        trailingContent = { CheckMark(visible) },
                        colors = androidx.tv.material3.ListItemDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                        ),
                    )
                }
            }
        }
        Text(
            "The provider still controls which programmes it publishes and their source order; Aerial Rows can hide individual published cards.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp, top = 4.dp),
        )
    }
}
'''
replace_once("app/src/main/java/com/conreo/couchytv/ui/RowQuickEditor.kt", old_channel_editor, new_channel_editor)

# Channels settings: one focus stop per channel. Left/Right changes the card count;
# OK opens the richer editor. This removes the nested selector row that made D-pad
# vertical navigation feel like focus was changing rows unpredictably.
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/AerialSettingsExtras.kt",
    '''    var channelRefresh by remember { mutableStateOf(0) }
    val channels by produceState(emptyList<TvChannelRow>(), channelRefresh) {
''',
    '''    var channelRefresh by remember { mutableStateOf(0) }
    var editingChannelKey by remember { mutableStateOf<String?>(null) }
    val channels by produceState(emptyList<TvChannelRow>(), channelRefresh) {
''',
)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/AerialSettingsExtras.kt",
    '''            channels.forEach { channel ->
                add(ChannelSettingsIds.channel(channel.key))
                if (channel.browsable && channel.programs.isNotEmpty()) add(ChannelSettingsIds.channelLimit(channel.key))
            }
''',
    '''            channels.forEach { channel ->
                add(ChannelSettingsIds.channel(channel.key))
            }
''',
)
old_channel_rows = '''            channels.forEach { channel ->
                val hidden = channel.isHiddenBy(config.hiddenTvChannels)
                val subtitle = when {
                    !channel.browsable -> {
                        val owner = channel.packageName.ifBlank { "Android TV channel" }
                        "$owner · Not approved for TV home — press OK to request"
                    }
                    channel.programs.isEmpty() -> {
                        val owner = channel.packageName.ifBlank { "Android TV channel" }
                        "$owner · No programmes currently published"
                    }
                    else -> channel.packageName.ifBlank { "Android TV channel" }
                }
                ExtraSettingRow(
                    title = channel.name,
                    subtitle = subtitle,
                    checked = channel.browsable && !hidden,
                    modifier = channelFocus.modifier(ChannelSettingsIds.channel(channel.key)),
                    onClick = {
                        if (!channel.browsable) {
                            if (TvContentRepository.requestChannelBrowsable(context, channel.id)) {
                                // The platform updates COLUMN_BROWSABLE asynchronously.
                                // Refresh once shortly after the system request so the
                                // Settings row reflects the approved state without the
                                // user having to leave/re-enter this screen.
                                scope.launch {
                                    delay(750L)
                                    channelRefresh++
                                }
                            }
                        } else {
                            scope.launch {
                                store.update { cfg ->
                                    val aliases = setOf(channel.key, channel.legacyKey)
                                    cfg.copy(
                                        hiddenTvChannels = if (channel.isHiddenBy(cfg.hiddenTvChannels)) {
                                            cfg.hiddenTvChannels - aliases
                                        } else {
                                            (cfg.hiddenTvChannels - channel.legacyKey) + channel.key
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
                if (channel.browsable && !hidden && channel.programs.isNotEmpty()) {
                    val value = channel.configuredItemLimit(
                        config.channelItemLimits,
                        config.channelItemLimit,
                    )
                    val index = limits.indices.minByOrNull { kotlin.math.abs(limits[it] - value) } ?: current
                    ExtraSelector(
                        title = "↳ ${channel.name}: cards shown",
                        value = limits[index].toString(),
                        steps = limits.size,
                        current = index,
                        modifier = channelFocus.modifier(ChannelSettingsIds.channelLimit(channel.key)),
                        onSelect = { i ->
                            scope.launch {
                                store.update { cfg ->
                                    cfg.copy(
                                        channelItemLimits = (cfg.channelItemLimits - channel.legacyKey) +
                                            (channel.key to limits[i]),
                                    )
                                }
                            }
                        },
                    )
                }
            }
'''
new_channel_rows = '''            channels.forEach { channel ->
                val hidden = channel.isHiddenBy(config.hiddenTvChannels)
                val value = channel.configuredItemLimit(config.channelItemLimits, config.channelItemLimit)
                val index = limits.indices.minByOrNull { kotlin.math.abs(limits[it] - value) } ?: current
                val owner = channel.packageName.ifBlank { "Android TV channel" }
                val subtitle = when {
                    !channel.browsable -> "$owner · Not approved for TV home — OK requests access"
                    channel.programs.isEmpty() -> "$owner · No programmes currently published · OK opens settings"
                    hidden -> "$owner · Hidden · OK customizes · Left/Right changes cards shown"
                    else -> "$owner · OK customizes content · Left/Right changes cards shown"
                }
                ExtraListItem(
                    title = channel.name,
                    subtitle = subtitle,
                    modifier = channelFocus.modifier(ChannelSettingsIds.channel(channel.key))
                        .onPreviewKeyEvent { event ->
                            if (!channel.browsable || channel.programs.isEmpty() || event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            val next = when (event.key) {
                                Key.DirectionLeft -> (index - 1).coerceAtLeast(0)
                                Key.DirectionRight -> (index + 1).coerceAtMost(limits.lastIndex)
                                else -> return@onPreviewKeyEvent false
                            }
                            if (next != index) {
                                scope.launch {
                                    store.update { cfg ->
                                        cfg.copy(
                                            channelItemLimits = (cfg.channelItemLimits - channel.legacyKey) +
                                                (channel.key to limits[next]),
                                        )
                                    }
                                }
                            }
                            true
                        },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExtraCheck(channel.browsable && !hidden)
                            if (channel.browsable && channel.programs.isNotEmpty()) {
                                Text("◄ ${limits[index]} ►", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    onClick = {
                        if (!channel.browsable) {
                            if (TvContentRepository.requestChannelBrowsable(context, channel.id)) {
                                scope.launch {
                                    delay(750L)
                                    channelRefresh++
                                }
                            }
                        } else {
                            editingChannelKey = channel.key
                        }
                    },
                )
            }
'''
replace_once("app/src/main/java/com/conreo/couchytv/ui/AerialSettingsExtras.kt", old_channel_rows, new_channel_rows)
replace_once(
    "app/src/main/java/com/conreo/couchytv/ui/AerialSettingsExtras.kt",
    '''        Text(
            "The provider app decides which programmes belong to a preview channel and their programme order. Aerial Rows controls which channel rows are visible and how many cards each row displays.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, end = 8.dp),
        )
    }
}
''',
    '''        Text(
            "Provider apps decide what they publish and the source order. Aerial Rows can show/hide channels, set row size, and hide individual published cards.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, end = 8.dp),
        )
    }

    channels.firstOrNull { it.key == editingChannelKey }?.let { channel ->
        RowQuickEditor(
            title = channel.name,
            icon = "▣",
            category = null,
            channel = channel,
            isWatchNext = false,
            allApps = emptyList(),
            config = config,
            store = store,
            onDismiss = { editingChannelKey = null },
        )
    }
}
''',
)

# Pure regression tests for programme identity/filtering.
test_path = Path("app/src/test/java/com/conreo/couchytv/data/TvProgramCustomizationTest.kt")
test_path.write_text('''package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvProgramCustomizationTest {
    private fun item(
        id: Long,
        intent: String,
        owner: String = "provider.pkg",
        title: String = "Item $id",
    ) = TvProgramItem(
        id = id,
        title = title,
        subtitle = "",
        artworkUri = "",
        intentUri = intent,
        packageName = owner,
        durationMs = 0L,
        playbackPositionMs = 0L,
    )

    @Test
    fun stableKeyPrefersIntentUriOverProviderRowId() {
        val first = item(id = 1L, intent = "app://watch/episode-7")
        val recreated = item(id = 999L, intent = "app://watch/episode-7")
        assertEquals(tvProgramStableKey(first), tvProgramStableKey(recreated))
    }

    @Test
    fun stableKeyFallsBackToOwnerAndNumericId() {
        val value = item(id = 42L, intent = "", owner = "")
        assertEquals("fallback.pkg|id:42", tvProgramStableKey(value, "fallback.pkg"))
    }

    @Test
    fun visibleProgramsFiltersHiddenBeforeApplyingLimit() {
        val programs = (1L..6L).map { item(it, "app://watch/$it") }
        val hidden = setOf(tvProgramStableKey(programs[0]), tvProgramStableKey(programs[1]))
        val visible = visibleTvPrograms(programs, hidden, "provider.pkg", limit = 4)
        assertEquals(listOf(3L, 4L, 5L, 6L), visible.map { it.id })
        assertTrue(visible.none { tvProgramStableKey(it) in hidden })
    }
}
''')

print("Phase 6 Shield follow-up source materialized")
