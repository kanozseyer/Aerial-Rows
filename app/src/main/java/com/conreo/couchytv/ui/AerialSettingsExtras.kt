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
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var channelRefresh by remember { mutableStateOf(0) }
    val channels by produceState(emptyList<TvChannelRow>(), channelRefresh) {
        value = TvContentRepository.discoverChannels(context)
    }

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
        val focus = extraInitialFocus()
        ExtraSettingRow(
            title = "Watch Next",
            subtitle = "Show the system Play Next / Watch Next row when available",
            checked = config.showWatchNext,
            modifier = Modifier.focusRequester(focus),
            onClick = {
                scope.launch { store.update { it.copy(showWatchNext = !it.showWatchNext) } }
            },
        )
        ExtraSettingRow(
            title = "App preview channels",
            subtitle = "Master switch for browsable channels published by installed TV apps",
            checked = config.showPreviewChannels,
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
                val hidden = channel.key in config.hiddenTvChannels
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
                                    cfg.copy(
                                        hiddenTvChannels = if (channel.key in cfg.hiddenTvChannels)
                                            cfg.hiddenTvChannels - channel.key
                                        else cfg.hiddenTvChannels + channel.key
                                    )
                                }
                            }
                        }
                    },
                )
                if (channel.browsable && !hidden && channel.programs.isNotEmpty()) {
                    val value = config.channelItemLimits[channel.key] ?: config.channelItemLimit
                    val index = limits.indices.minByOrNull { kotlin.math.abs(limits[it] - value) } ?: current
                    ExtraSelector(
                        title = "↳ ${channel.name}: cards shown",
                        value = limits[index].toString(),
                        steps = limits.size,
                        current = index,
                        onSelect = { i ->
                            scope.launch {
                                store.update { cfg ->
                                    cfg.copy(channelItemLimits = cfg.channelItemLimits + (channel.key to limits[i]))
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
fun PanelAppearanceSettings(
    config: LauncherConfig,
    store: ConfigStore,
) {
    val scope = rememberCoroutineScope()

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
        steps = margins.size,
        current = marginIndex,
        onSelect = { i -> scope.launch { store.update { it.copy(rowsTopMarginPercent = margins[i]) } } },
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
        steps = styles.size,
        current = config.panelStyle.coerceIn(0, 2),
        onSelect = { v -> scope.launch { store.update { it.copy(panelStyle = v) } } },
    )

    if (config.panelStyle != PANEL_CLEAR) {
        ExtraSelector(
            title = "Opacity",
            value = "${config.panelOpacity.coerceIn(0, 10) * 10}%",
            steps = 11,
            current = config.panelOpacity.coerceIn(0, 10),
            onSelect = { v -> scope.launch { store.update { it.copy(panelOpacity = v) } } },
        )

        val hueNames = listOf(
            "Neutral", "Red", "Orange", "Yellow", "Lime", "Green", "Aqua",
            "Cyan", "Azure", "Blue", "Violet", "Magenta", "Rose",
        )
        ExtraSelector(
            title = "Tint colour",
            value = hueNames[config.panelTintHue.coerceIn(0, 12)],
            steps = hueNames.size,
            current = config.panelTintHue.coerceIn(0, 12),
            onSelect = { v -> scope.launch { store.update { it.copy(panelTintHue = v) } } },
        )

        val satNames = listOf("Subtle", "Soft", "Medium", "Rich", "Vivid", "Maximum")
        ExtraSelector(
            title = "Tint saturation",
            value = satNames[config.panelTintSaturation.coerceIn(0, 5)],
            steps = satNames.size,
            current = config.panelTintSaturation.coerceIn(0, 5),
            onSelect = { v -> scope.launch { store.update { it.copy(panelTintSaturation = v) } } },
        )

        if (config.panelStyle == PANEL_LIQUID_GLASS) {
            val strengths = listOf("Off", "Very low", "Low", "Medium", "High", "Maximum")
            ExtraSelector(
                title = "Glass effect strength",
                subtitle = "Controls sheen, edge highlight and visual depth",
                value = strengths[config.panelEffectStrength.coerceIn(0, 5)],
                steps = strengths.size,
                current = config.panelEffectStrength.coerceIn(0, 5),
                onSelect = { v -> scope.launch { store.update { it.copy(panelEffectStrength = v) } } },
            )
        }

        AerialPanel(
            config = config,
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
) {
    ExtraListItem(
        modifier = modifier.onPreviewKeyEvent { event ->
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
        onClick = { onSelect((current + 1) % steps) },
        title = title,
        subtitle = subtitle,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("◄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value)
                Text("►", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
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
