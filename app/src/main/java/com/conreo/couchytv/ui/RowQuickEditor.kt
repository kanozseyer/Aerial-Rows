package com.conreo.couchytv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.AppRepository
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.ConfigStore
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.TvChannelRow
import com.conreo.couchytv.data.tvProgramStableKey
import kotlinx.coroutines.launch

/**
 * Projectivy-style quick editor opened directly from the left rail.
 *
 * Category rows:
 *  - OK adds/removes an app from the row.
 *  - Left/Right moves an included app earlier/later in that row.
 *
 * TV channel rows:
 *  - Toggle visibility.
 *  - Set how many provider items that specific row shows.
 */
@Composable
fun RowQuickEditor(
    title: String,
    icon: String,
    category: CategoryCfg?,
    channel: TvChannelRow?,
    isWatchNext: Boolean,
    allApps: List<AppEntry>,
    config: LauncherConfig,
    store: ConfigStore,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.width(760.dp).height(590.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(start = 6.dp, bottom = 12.dp),
                ) {
                    Text(icon, style = MaterialTheme.typography.headlineSmall)
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            when {
                                category != null -> "OK: add/remove  •  Left/Right: reorder included apps"
                                channel != null -> "Choose whether this channel appears and how many cards it shows"
                                isWatchNext -> "Customize the system Watch Next row"
                                else -> "Row settings"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                when {
                    category != null -> CategoryContentsEditor(
                        category = category,
                        allApps = allApps,
                        config = config,
                        store = store,
                    )

                    channel != null -> ChannelContentsEditor(
                        channel = channel,
                        config = config,
                        store = store,
                    )

                    isWatchNext -> WatchNextEditor(config = config, store = store)
                }

                Box(Modifier.weight(1f))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun CategoryContentsEditor(
    category: CategoryCfg,
    allApps: List<AppEntry>,
    config: LauncherConfig,
    store: ConfigStore,
) {
    val scope = rememberCoroutineScope()
    val explicit = config.order[category.id].orEmpty()
    val included = allApps.filter { category.id in AppRepository.sectionsOf(it, config) }
    val includedIds = included.map { it.pkg }.toSet()
    val includedOrdered = included.sortedWith(
        compareBy<AppEntry> {
            val i = explicit.indexOf(it.pkg)
            if (i >= 0) i else Int.MAX_VALUE
        }.thenBy { it.label.lowercase() }
    )
    val excluded = allApps.filter { it.pkg !in includedIds }.sortedBy { it.label.lowercase() }
    val display = includedOrdered + excluded
    val first = androidx.compose.runtime.remember(category.id) { FocusRequester() }

    LazyColumn(
        Modifier
            .fillMaxWidth()
            .height(455.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(display, key = { _, app -> app.pkg }) { index, app ->
            val inRow = app.pkg in includedIds
            ListItem(
                selected = false,
                onClick = {
                    scope.launch {
                        store.update { cfg ->
                            val current = AppRepository.sectionsOf(app, cfg)
                            val next = if (category.id in current) current - category.id else current + category.id
                            val currentOrder = cfg.order[category.id].orEmpty().filter { it != app.pkg }
                            val nextOrder = if (category.id in next) currentOrder + app.pkg else currentOrder
                            cfg.copy(
                                sections = cfg.sections + (app.pkg to next),
                                order = cfg.order + (category.id to nextOrder),
                            )
                        }
                    }
                },
                modifier = (if (index == 0) Modifier.focusRequester(first) else Modifier)
                    .onPreviewKeyEvent { event ->
                        if (!inRow || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val delta = when (event.key) {
                            Key.DirectionLeft -> -1
                            Key.DirectionRight -> 1
                            else -> return@onPreviewKeyEvent false
                        }
                        scope.launch {
                            store.update { cfg ->
                                val currentApps = allApps.filter {
                                    category.id in AppRepository.sectionsOf(it, cfg)
                                }
                                val saved = cfg.order[category.id].orEmpty()
                                val normalized = saved.filter { pkg -> currentApps.any { it.pkg == pkg } } +
                                    currentApps.map { it.pkg }.filterNot { it in saved }
                                val from = normalized.indexOf(app.pkg)
                                val to = (from + delta).coerceIn(0, normalized.lastIndex)
                                if (from < 0 || from == to) cfg
                                else {
                                    val moved = normalized.toMutableList()
                                    val value = moved.removeAt(from)
                                    moved.add(to, value)
                                    cfg.copy(order = cfg.order + (category.id to moved))
                                }
                            }
                        }
                        true
                    }
                    .padding(horizontal = 4.dp),
                headlineContent = { Text(app.label) },
                supportingContent = {
                    Text(if (inRow) "Included in ${category.name}" else "Not in this row")
                },
                trailingContent = {
                    Text(
                        if (inRow) "◀   ✓   ▶" else "+",
                        color = if (inRow) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = androidx.tv.material3.ListItemDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                ),
            )
        }
    }
}

@Composable
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

@Composable
private fun WatchNextEditor(config: LauncherConfig, store: ConfigStore) {
    val scope = rememberCoroutineScope()
    val limits = listOf(4, 6, 8, 10, 12, 16, 20, 24, 30)
    val currentIndex = limits.indices.minByOrNull {
        kotlin.math.abs(limits[it] - config.channelItemLimit)
    } ?: 4
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        QuickToggleRow(
            title = "Show Watch Next",
            subtitle = "System Play Next / Watch Next recommendations",
            checked = config.showWatchNext,
            onClick = { scope.launch { store.update { it.copy(showWatchNext = !it.showWatchNext) } } },
        )
        QuickSelectorRow(
            title = "Cards shown",
            value = limits[currentIndex].toString(),
            current = currentIndex,
            steps = limits.size,
            onSelect = { i -> scope.launch { store.update { it.copy(channelItemLimit = limits[i]) } } },
        )
    }
}

@Composable
private fun QuickToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Box(
                Modifier
                    .background(
                        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(if (checked) "ON" else "OFF")
            }
        },
    )
}

@Composable
private fun QuickSelectorRow(
    title: String,
    value: String,
    current: Int,
    steps: Int,
    subtitle: String? = null,
    onSelect: (Int) -> Unit,
) {
    ListItem(
        selected = false,
        onClick = { onSelect((current + 1) % steps) },
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> {
                    onSelect((current - 1 + steps) % steps)
                    true
                }
                Key.DirectionRight -> {
                    onSelect((current + 1) % steps)
                    true
                }
                else -> false
            }
        },
        headlineContent = { Text(title) },
        supportingContent = if (subtitle == null) null else ({ Text(subtitle) }),
        trailingContent = { Text("◀   $value   ▶") },
    )
}
