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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.LauncherConfig
import kotlinx.coroutines.launch

/**
 * Projectivy-inspired layout: vertically stacked sections, each one a normal
 * horizontally sliding LazyRow. Unlike Couchy's Carousel mode, the selected
 * card itself moves across the screen rather than remaining pinned at a pivot.
 *
 * The left navigation rail is hidden during normal browsing. Pressing LEFT on
 * the first card reveals it; pressing LEFT again transfers focus to the rail.
 * RIGHT from the rail collapses it and returns to the first card of the active
 * section.
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
    val scope = rememberCoroutineScope()
    val verticalState = rememberLazyListState()
    var railVisible by remember { mutableStateOf(false) }
    var activeRow by remember { mutableIntStateOf(0) }
    val railFocus = remember { FocusRequester() }
    val firstCardFocus = remember(categorized.size) {
        List(categorized.size) { FocusRequester() }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = verticalState,
            modifier = Modifier
                .fillMaxSize()
                .focusGroup(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            itemsIndexed(
                items = categorized,
                key = { _, pair -> pair.first.id },
            ) { rowIndex, (category, apps) ->
                Column(Modifier.fillMaxWidth()) {
                    if (config.showCategoryNames) {
                        Text(
                            text = category.name.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.72f),
                            letterSpacing = 1.4.sp,
                            modifier = Modifier.padding(start = 52.dp, bottom = 10.dp),
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 52.dp, end = 72.dp),
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
                                        if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                                            return@onPreviewKeyEvent false
                                        }
                                        if (!railVisible) {
                                            railVisible = true
                                        } else {
                                            runCatching { railFocus.requestFocus() }
                                        }
                                        true
                                    }
                            }
                            if (movePkg == app.pkg) {
                                cardModifier = cardModifier.focusRequester(moveFocus)
                            }

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

        AnimatedVisibility(
            visible = railVisible,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Surface(
                shape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp),
                colors = SurfaceDefaults.colors(containerColor = Color(0xE61A1C21)),
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
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.10f))
                        .padding(top = 42.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "AERIAL ROWS",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 1.6.sp,
                        modifier = Modifier.padding(horizontal = 22.dp, bottom = 12.dp),
                    )

                    categorized.forEachIndexed { index, (category, _) ->
                        RailItem(
                            label = category.name,
                            selected = index == activeRow,
                            modifier = if (index == activeRow) Modifier.focusRequester(railFocus) else Modifier,
                            onClick = {
                                activeRow = index
                                scope.launch {
                                    verticalState.animateScrollToItem(index)
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
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
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
