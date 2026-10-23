package com.conreo.couchytv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.conreo.couchytv.core.settings.ChannelSettingsIds
import com.conreo.couchytv.core.settings.DisplaySettingDescriptors
import com.conreo.couchytv.core.settings.SettingId
import com.conreo.couchytv.core.settings.DisplaySettingIds
import com.conreo.couchytv.data.ConfigStore
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.PANEL_CLEAR
import com.conreo.couchytv.data.PANEL_LIQUID_GLASS
import com.conreo.couchytv.data.PANEL_SOLID
import com.conreo.couchytv.data.TvChannelRow
import com.conreo.couchytv.data.TvContentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChannelsSettingsScreen(
    config: LauncherConfig,
    store: ConfigStore,
    rememberedFocus: SettingId?,
    onFocus: (SettingId) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var channelRefresh by remember { mutableStateOf(0) }
    val channels by produceState(emptyList<TvChannelRow>(), channelRefresh) {
        value = TvContentRepository.discoverChannels(context)
    }
    val watchNextProviders by produceState(emptyList<com.conreo.couchytv.data.TvProviderInfo>(), channelRefresh) {
        value = TvContentRepository.discoverWatchNextProviders(context)
    }
    val channelFocusIds = remember(config.showWatchNext, watchNextProviders, channels) {
        buildList {
            add(ChannelSettingsIds.WatchNext)
            if (config.showWatchNext) {
                add(ChannelSettingsIds.WatchNextLimit)
                watchNextProviders.forEach { add(ChannelSettingsIds.watchNextProvider(it.packageName)) }
            }
            add(ChannelSettingsIds.PreviewChannels)
            add(ChannelSettingsIds.DefaultChannelLimit)
            channels.forEach { channel ->
                add(ChannelSettingsIds.channel(channel.key))
                if (channel.browsable && channel.programs.isNotEmpty()) add(ChannelSettingsIds.channelLimit(channel.key))
            }
        }
    }
    val channelFocus = rememberSettingsFocusBindings(
        ids = channelFocusIds,
        rememberedFocus = rememberedFocus,
        defaultFocus = ChannelSettingsIds.WatchNext,
        onFocused = onFocus,
    )

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Channels",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
        )
        Text(
            "ANDROID TV CONTENT",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
        )
        ExtraSettingRow(
            title = "Watch Next",
            subtitle = "Show the system Play Next / Watch Next row when available",
            checked = config.showWatchNext,
            modifier = channelFocus.modifier(ChannelSettingsIds.WatchNext),
            onClick = {
                scope.launch { store.update { it.copy(showWatchNext = !it.showWatchNext) } }
            },
        )
        if (config.showWatchNext) {
            val providerCaps = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 30)
            val capIndex = providerCaps.indices.minByOrNull {
                kotlin.math.abs(providerCaps[it] - config.watchNextPerProviderLimit)
            } ?: 3
            ExtraSelector(
                title = "Watch Next cards per app",
                subtitle = "Stops one provider from filling the entire Watch Next row",
                value = providerCaps[capIndex].toString(),
                steps = providerCaps.size,
                current = capIndex,
                modifier = channelFocus.modifier(ChannelSettingsIds.WatchNextLimit),
                onSelect = { i ->
                    scope.launch { store.update { it.copy(watchNextPerProviderLimit = providerCaps[i]) } }
                },
            )
            if (watchNextProviders.isNotEmpty()) {
                Text(
                    "WATCH NEXT APPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                )
                watchNextProviders.forEach { provider ->
                    val enabled = provider.packageName !in config.excludedWatchNextProviders
                    ExtraSettingRow(
                        title = provider.label,
                        subtitle = provider.packageName,
                        checked = enabled,
                        modifier = channelFocus.modifier(ChannelSettingsIds.watchNextProvider(provider.packageName)),
                        onClick = {
                            scope.launch {
                                store.update { cfg ->
                                    cfg.copy(
                                        excludedWatchNextProviders = if (provider.packageName in cfg.excludedWatchNextProviders)
                                            cfg.excludedWatchNextProviders - provider.packageName
                                        else cfg.excludedWatchNextProviders + provider.packageName
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }

        ExtraSettingRow(
            title = "App preview channels",
            subtitle = "Master switch for browsable channels published by installed TV apps",
            checked = config.showPreviewChannels,
            modifier = channelFocus.modifier(ChannelSettingsIds.PreviewChannels),
            onClick = {
                scope.launch { store.update { it.copy(showPreviewChannels = !it.showPreviewChannels) } }
            },
        )

        val limits = listOf(4, 6, 8, 10, 12, 16, 20, 24, 30)
        val current = limits.indices.minByOrNull { kotlin.math.abs(limits[it] - config.channelItemLimit) } ?: 4
        ExtraSelector(
            title = "Default cards per channel",
            subtitle = "Used unless a channel below has its own value",
            value = limits[current].toString(),
            steps = limits.size,
            current = current,
            modifier = channelFocus.modifier(ChannelSettingsIds.DefaultChannelLimit),
            onSelect = { i -> scope.launch { store.update { it.copy(channelItemLimit = limits[i]) } } },
        )

        Text(
            "INDIVIDUAL CHANNELS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
        )

        if (channels.isEmpty()) {
            Text(
                "No readable preview channels are currently exposed by Android TV apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
        } else {
            channels.forEach { channel ->
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
        }

        Text(
            "The provider app decides which programmes belong to a preview channel and their programme order. Aerial Rows controls which channel rows are visible and how many cards each row displays.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, end = 8.dp),
        )
    }
}

/** Extra Display settings shared by Sliding Rows and Couchy's Dock mode. */
@Composable
internal fun PanelAppearanceSettings(
    config: LauncherConfig,
    store: ConfigStore,
    debouncedWriter: DebouncedSettingsWriter,
    focus: SettingsFocusBindings,
) {
    val scope = rememberCoroutineScope()
    val previewConfig = debouncedWriter.preview(config)

    Text(
        "SLIDING ROWS POSITION",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, start = 8.dp, bottom = 4.dp),
    )
    val margins = listOf(0, 3, 6, 9, 12, 15, 20, 25, 30, 40, 50)
    val marginIndex = margins.indices.minByOrNull {
        kotlin.math.abs(margins[it] - config.rowsTopMarginPercent)
    } ?: 2
    ExtraSelector(
        title = "Top row margin",
        subtitle = "Space between the top of the screen and the first home row",
        value = "${margins[marginIndex]}%",
        options = margins.map { "$it%" },
        steps = margins.size,
        current = marginIndex,
        modifier = focus.modifier(DisplaySettingIds.RowsTopMargin),
        onSelect = { i ->
            val value = DisplaySettingDescriptors.RowsTopMargin.requireValid(margins[i])
            debouncedWriter.submit(DisplaySettingDescriptors.RowsTopMargin.id) {
                it.copy(rowsTopMarginPercent = value)
            }
        },
    )

    Text(
        "ROW & DOCK BACKGROUND",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, start = 8.dp, bottom = 4.dp),
    )

    val styles = listOf("Clear", "Solid tint", "Liquid glass")
    ExtraSelector(
        title = "Background style",
        subtitle = when (config.panelStyle) {
            PANEL_CLEAR -> "No row or dock panel — wallpaper remains fully visible"
            PANEL_SOLID -> "Flat custom tint behind rows and the dock"
            else -> "Layered translucent glass with highlight and edge depth"
        },
        value = styles[config.panelStyle.coerceIn(0, 2)],
        options = styles,
        steps = styles.size,
        current = config.panelStyle.coerceIn(0, 2),
        modifier = focus.modifier(DisplaySettingIds.PanelStyle),
        onSelect = { v ->
            val value = DisplaySettingDescriptors.PanelStyle.requireValid(v)
            scope.launch { store.update { it.copy(panelStyle = value) } }
        },
    )

    if (config.panelStyle != PANEL_CLEAR) {
        ExtraSelector(
            title = "Opacity",
            value = "${config.panelOpacity.coerceIn(0, 10) * 10}%",
            options = (0..10).map { "${it * 10}%" },
            steps = 11,
            current = config.panelOpacity.coerceIn(0, 10),
            modifier = focus.modifier(DisplaySettingIds.PanelOpacity),
            onSelect = { v ->
                val value = DisplaySettingDescriptors.PanelOpacity.requireValid(v)
                debouncedWriter.submit(DisplaySettingDescriptors.PanelOpacity.id) { it.copy(panelOpacity = value) }
            },
        )

        val hueNames = listOf(
            "Neutral", "Red", "Orange", "Yellow", "Lime", "Green", "Aqua",
            "Cyan", "Azure", "Blue", "Violet", "Magenta", "Rose",
        )
        ExtraSelector(
            title = "Tint colour",
            value = hueNames[config.panelTintHue.coerceIn(0, 12)],
            options = hueNames,
            steps = hueNames.size,
            current = config.panelTintHue.coerceIn(0, 12),
            modifier = focus.modifier(DisplaySettingIds.PanelTintHue),
            onSelect = { v ->
                val value = DisplaySettingDescriptors.PanelTintHue.requireValid(v)
                debouncedWriter.submit(DisplaySettingDescriptors.PanelTintHue.id) { it.copy(panelTintHue = value) }
            },
        )

        val satNames = listOf("Subtle", "Soft", "Medium", "Rich", "Vivid", "Maximum")
        ExtraSelector(
            title = "Tint saturation",
            value = satNames[config.panelTintSaturation.coerceIn(0, 5)],
            options = satNames,
            steps = satNames.size,
            current = config.panelTintSaturation.coerceIn(0, 5),
            modifier = focus.modifier(DisplaySettingIds.PanelTintSaturation),
            onSelect = { v ->
                val value = DisplaySettingDescriptors.PanelTintSaturation.requireValid(v)
                debouncedWriter.submit(DisplaySettingDescriptors.PanelTintSaturation.id) { it.copy(panelTintSaturation = value) }
            },
        )

        if (config.panelStyle == PANEL_LIQUID_GLASS) {
            val strengths = listOf("Off", "Very low", "Low", "Medium", "High", "Maximum")
            ExtraSelector(
                title = "Glass effect strength",
                subtitle = "Controls sheen, edge highlight and visual depth",
                value = strengths[config.panelEffectStrength.coerceIn(0, 5)],
                options = strengths,
                steps = strengths.size,
                current = config.panelEffectStrength.coerceIn(0, 5),
                modifier = focus.modifier(DisplaySettingIds.PanelEffectStrength),
                onSelect = { v ->
                    val value = DisplaySettingDescriptors.PanelEffectStrength.requireValid(v)
                    debouncedWriter.submit(DisplaySettingDescriptors.PanelEffectStrength.id) { it.copy(panelEffectStrength = value) }
                },
            )
        }

        AerialPanel(
            config = previewConfig,
            shape = SmoothCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                Text("Live panel preview", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ExtraSelector(
    title: String,
    value: String,
    steps: Int,
    current: Int,
    onSelect: (Int) -> Unit,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    options: List<String>? = null,
) {
    var optimisticCurrent by remember { mutableIntStateOf(current) }
    var showChoices by remember { mutableStateOf(false) }
    LaunchedEffect(current) { optimisticCurrent = current }
    val validOptions = options?.takeIf { it.size == steps }
    val displayedCurrent = optimisticCurrent.coerceIn(0, steps - 1)
    val displayedValue = validOptions?.getOrNull(displayedCurrent) ?: value
    fun select(index: Int) {
        optimisticCurrent = index
        onSelect(index)
    }
    ExtraListItem(
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> {
                    select((displayedCurrent - 1 + steps) % steps)
                    true
                }
                Key.DirectionRight -> {
                    select((displayedCurrent + 1) % steps)
                    true
                }
                else -> false
            }
        },
        onClick = {
            if (validOptions != null) showChoices = true
            else select((displayedCurrent + 1) % steps)
        },
        title = title,
        subtitle = subtitle,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("◄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(displayedValue)
                Text(if (validOptions != null) "▾" else "►", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
    if (showChoices && validOptions != null) {
        SettingsChoiceDialog(
            title = title,
            options = validOptions,
            current = displayedCurrent,
            onSelect = { index ->
                select(index)
                showChoices = false
            },
            onDismiss = { showChoices = false },
        )
    }
}

@Composable
private fun ExtraSettingRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ExtraListItem(
        modifier = modifier,
        onClick = onClick,
        title = title,
        subtitle = subtitle,
        trailing = { ExtraCheck(checked) },
    )
}

@Composable
private fun ExtraListItem(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { focused = it.hasFocus }
            .padding(horizontal = 8.dp)
            .background(
                if (focused) Color(0xFFF1F2F4) else Color.Transparent,
                RoundedCornerShape(12.dp),
            ),
        headlineContent = { Text(title) },
        supportingContent = if (subtitle == null) null else ({ Text(subtitle) }),
        trailingContent = trailing,
        colors = androidx.tv.material3.ListItemDefaults.colors(
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color(0xFF14151A),
        ),
    )
}

@Composable
private fun ExtraCheck(checked: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .background(
                if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(5.dp),
            )
            .border(
                2.dp,
                if (checked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                RoundedCornerShape(5.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Text("✓", color = Color.Black)
    }
}

@Composable
private fun extraInitialFocus(): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }
    return requester
}
