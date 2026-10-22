package com.conreo.couchytv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.conreo.couchytv.Actions
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.TvContentRepository
import com.conreo.couchytv.data.TvContentRows
import com.conreo.couchytv.data.TvProgramItem
import kotlinx.coroutines.launch

/**
 * Projectivy-inspired layout: vertically stacked sections, each one a normal
 * horizontally sliding LazyRow. Android TV Watch Next and preview channels are
 * injected above app sections when the device/provider exposes them.
 */
@Composable
fun SlidingRowsArea(
    modifier: Modifier = Modifier,
    categorized: List<Pair<CategoryCfg, List<AppEntry>>>,
    config: LauncherConfig,
    accent: Color,
    movePkg: String?,
    moveFocus: FocusRequester,
    cardWidth: Dp,
    gap: Dp,
    onLaunch: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onSettings: () -> Unit,
    onAndroidSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val verticalState = rememberLazyListState()
    var railVisible by remember { mutableStateOf(false) }
    var activeRow by remember { mutableIntStateOf(0) }
    val railFocus = remember { FocusRequester() }
    val firstCardFocus = remember(categorized.size) {
        List(categorized.size) { FocusRequester() }
    }

    val tvContent by produceState(
        TvContentRows(),
        config.showWatchNext,
        config.showPreviewChannels,
        config.channelItemLimit,
    ) {
        value = TvContentRepository.load(context, config)
    }
    val tvRowCount = (if (tvContent.watchNext.isNotEmpty()) 1 else 0) + tvContent.channels.size

    fun revealRail(): Boolean {
        if (!railVisible) railVisible = true
        else runCatching { railFocus.requestFocus() }
        return true
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = verticalState,
            modifier = Modifier
                .fillMaxSize()
                .focusGroup(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (tvContent.watchNext.isNotEmpty()) {
                item(key = "__watch_next__") {
                    TvContentRow(
                        title = "WATCH NEXT",
                        programs = tvContent.watchNext,
                        config = config,
                        accent = accent,
                        cardWidth = cardWidth,
                        gap = gap,
                        onFirstLeft = { revealRail() },
                        onLaunch = { item ->
                            Actions.launchTvContent(context, item.intentUri, item.packageName)
                        },
                    )
                }
            }

            itemsIndexed(
                items = tvContent.channels,
                key = { _, channel -> "channel:${channel.id}" },
            ) { _, channel ->
                TvContentRow(
                    title = channel.name.uppercase(),
                    programs = channel.programs,
                    config = config,
                    accent = accent,
                    cardWidth = cardWidth,
                    gap = gap,
                    onFirstLeft = { revealRail() },
                    onLaunch = { item ->
                        Actions.launchTvContent(
                            context,
                            item.intentUri,
                            item.packageName.ifBlank { channel.packageName },
                        )
                    },
                )
            }

            itemsIndexed(
                items = categorized,
                key = { _, pair -> pair.first.id },
            ) { rowIndex, (category, apps) ->
                AerialPanel(
                    config = config,
                    shape = SmoothCornerShape(LocalCornerRadius.current + 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        if (config.showCategoryNames) {
                            Text(
                                text = category.name.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.72f),
                                letterSpacing = 1.4.sp,
                                modifier = Modifier.padding(start = 24.dp, bottom = 10.dp),
                            )
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(start = 24.dp, end = 44.dp, top = 4.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            itemsIndexed(
                                items = apps,
                                key = { _, app -> "${category.id}:${app.pkg}" },
                            ) { appIndex, app ->
                                val firstInRow = appIndex == 0
                                var cardModifier: Modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) activeRow = rowIndex
                                }
                                if (firstInRow) {
                                    cardModifier = cardModifier
                                        .focusRequester(firstCardFocus[rowIndex])
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                                                revealRail()
                                            } else false
                                        }
                                }
                                if (movePkg == app.pkg) cardModifier = cardModifier.focusRequester(moveFocus)

                                AppCard(
                                    app = app,
                                    isMoving = movePkg == app.pkg,
                                    isHidden = app.pkg in config.hidden,
                                    accent = accent,
                                    cardWidth = cardWidth,
                                    showLabel = config.showAppLabels,
                                    modifier = cardModifier,
                                    onLaunch = { onLaunch(app) },
                                    onLongPress = { onMenu(app) },
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = railVisible,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Surface(
                shape = SmoothCornerShape(22.dp),
                colors = SurfaceDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionRight, Key.Back, Key.Escape -> {
                                railVisible = false
                                firstCardFocus.getOrNull(activeRow)?.let { runCatching { it.requestFocus() } }
                                true
                            }
                            else -> false
                        }
                    },
            ) {
                AerialPanel(
                    config = config.copy(
                        panelOpacity = (config.panelOpacity + 2).coerceAtMost(10),
                        panelEffectStrength = (config.panelEffectStrength + 1).coerceAtMost(5),
                    ),
                    shape = SmoothCornerShape(22.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.08f))
                            .padding(top = 42.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "AERIAL ROWS",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            letterSpacing = 1.6.sp,
                            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 12.dp),
                        )

                        categorized.forEachIndexed { index, (category, _) ->
                            RailItem(
                                label = category.name,
                                selected = index == activeRow,
                                modifier = if (index == activeRow) Modifier.focusRequester(railFocus) else Modifier,
                                onClick = {
                                    activeRow = index
                                    scope.launch {
                                        verticalState.animateScrollToItem(tvRowCount + index)
                                        railVisible = false
                                        firstCardFocus.getOrNull(index)?.let { runCatching { it.requestFocus() } }
                                    }
                                },
                            )
                        }

                        Box(Modifier.weight(1f))
                        RailItem(label = "Launcher settings", selected = false, onClick = onSettings)
                        RailItem(label = "Android settings", selected = false, onClick = onAndroidSettings)
                    }
                }
            }
        }
    }
}

@Composable
private fun TvContentRow(
    title: String,
    programs: List<TvProgramItem>,
    config: LauncherConfig,
    accent: Color,
    cardWidth: Dp,
    gap: Dp,
    onFirstLeft: () -> Boolean,
    onLaunch: (TvProgramItem) -> Unit,
) {
    AerialPanel(
        config = config,
        shape = SmoothCornerShape(LocalCornerRadius.current + 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.72f),
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(start = 24.dp, bottom = 10.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(start = 24.dp, end = 44.dp, top = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                itemsIndexed(programs, key = { _, item -> item.id }) { index, item ->
                    val nav = if (index == 0) {
                        Modifier.onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) onFirstLeft()
                            else false
                        }
                    } else Modifier
                    TvProgramCard(
                        item = item,
                        cardWidth = cardWidth,
                        accent = accent,
                        modifier = nav,
                        onClick = { onLaunch(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RailItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 10.dp)
            .onFocusChanged { focused = it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent,
            focusedContainerColor = Color(0xFFF2F3F5),
            contentColor = Color.White.copy(alpha = 0.86f),
            focusedContentColor = Color(0xFF15161A),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected && !focused) {
                Text("•", fontSize = 18.sp, color = Color.White.copy(alpha = 0.65f))
            }
        }
    }
}
