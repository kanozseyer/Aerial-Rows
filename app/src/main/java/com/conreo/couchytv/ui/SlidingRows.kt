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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.conreo.couchytv.core.launch.LaunchDispatcher
import com.conreo.couchytv.core.launch.LaunchOrigin
import com.conreo.couchytv.core.launch.LaunchRequest
import com.conreo.couchytv.core.launch.LaunchTarget
import com.conreo.couchytv.core.home.HomeFocusState
import com.conreo.couchytv.core.home.HomeFocusTarget
import com.conreo.couchytv.core.home.HomeRailState
import com.conreo.couchytv.core.home.HomeRowSnapshot
import com.conreo.couchytv.core.model.AppId
import com.conreo.couchytv.core.model.HomeItemId
import com.conreo.couchytv.core.model.RowId
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.ConfigStore
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.TvChannelRow
import com.conreo.couchytv.data.TvContentRepository
import com.conreo.couchytv.data.TvContentRows
import com.conreo.couchytv.data.TvProgramItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private sealed interface HomeRowModel {
    val id: String
    val title: String
    val icon: String
}

private data class WatchNextHomeRow(
    val programs: List<TvProgramItem>,
) : HomeRowModel {
    override val id: String = "watch_next"
    override val title: String = "Watch Next"
    override val icon: String = "▶"
}

private data class ChannelHomeRow(
    val channel: TvChannelRow,
) : HomeRowModel {
    override val id: String = "channel:${channel.key}"
    override val title: String = channel.name
    override val icon: String = "▣"
}

private data class CategoryHomeRow(
    val category: CategoryCfg,
    val apps: List<AppEntry>,
) : HomeRowModel {
    override val id: String = "category:${category.id}"
    override val title: String = category.name
    override val icon: String = categoryIcon(category)
}

private fun categoryIcon(category: CategoryCfg): String {
    val token = (category.id + " " + category.name).lowercase()
    return when {
        "stream" in token || "video" in token || "tv" in token -> "▶"
        "game" in token -> "◆"
        "music" in token || "audio" in token -> "♪"
        "favorite" in token || "favourite" in token -> "★"
        "tool" in token || "setting" in token -> "⚙"
        "all" in token || "app" in token -> "▦"
        else -> "●"
    }
}

private fun normalizeHomeOrder(
    saved: List<String>,
    natural: List<String>,
    aliases: Map<String, String> = emptyMap(),
): List<String> {
    val migrated = saved.map { aliases[it] ?: it }
    return migrated.filter { it in natural }.distinct() + natural.filterNot { it in migrated }
}

private fun appHomeItemId(app: AppEntry): HomeItemId =
    HomeItemId.App(
        AppId(
            packageName = app.pkg,
            componentName = app.componentName,
        ),
    )

private fun tvHomeItemId(item: TvProgramItem, ownerFallback: String): HomeItemId =
    HomeItemId.TvProgram(
        providerPackage = item.packageName.ifBlank { ownerFallback },
        programKey = item.id.toString(),
    )

private fun HomeRowModel.toFocusSnapshot(): HomeRowSnapshot = when (this) {
    is WatchNextHomeRow -> HomeRowSnapshot(
        id = RowId(id),
        itemIds = programs.map { tvHomeItemId(it, "watch_next") },
    )
    is ChannelHomeRow -> HomeRowSnapshot(
        id = RowId(id),
        itemIds = channel.programs.map {
            tvHomeItemId(it, channel.packageName.ifBlank { "channel:${channel.key}" })
        },
    )
    is CategoryHomeRow -> HomeRowSnapshot(
        id = RowId(id),
        itemIds = apps.map(::appHomeItemId),
    )
}

/**
 * Projectivy-inspired layout: vertically stacked sections, each one a normal
 * horizontally sliding LazyRow. Watch Next, Android TV channels and app
 * categories all participate in one user-reorderable row stack.
 */
@Composable
fun SlidingRowsArea(
    modifier: Modifier = Modifier,
    categorized: List<Pair<CategoryCfg, List<AppEntry>>>,
    allApps: List<AppEntry>,
    config: LauncherConfig,
    store: ConfigStore,
    launchDispatcher: LaunchDispatcher,
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val verticalState = rememberLazyListState()
    var focusState by remember { mutableStateOf(HomeFocusState()) }
    var pendingRestore by remember { mutableStateOf<HomeFocusTarget?>(null) }
    var editingRowId by remember { mutableStateOf<String?>(null) }
    val railFocus = remember { FocusRequester() }
    val activeRowId = focusState.activeRowId?.value
    val railVisible = focusState.railState != HomeRailState.HIDDEN

    val tvContent by produceState(
        TvContentRows(),
        lifecycleOwner,
        config.showWatchNext,
        config.showPreviewChannels,
        config.watchNextPerProviderLimit,
        config.excludedWatchNextProviders,
        config.channelItemLimit,
        config.hiddenTvChannels,
        config.channelItemLimits,
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            TvContentRepository.observe(context, config).collect { rows ->
                value = rows
            }
        }
    }

    val naturalRows = buildList<HomeRowModel> {
        if (tvContent.watchNext.isNotEmpty()) add(WatchNextHomeRow(tvContent.watchNext))
        tvContent.channels.forEach { add(ChannelHomeRow(it)) }
        categorized.forEach { (category, apps) -> add(CategoryHomeRow(category, apps)) }
    }
    val naturalIds = naturalRows.map { it.id }
    val rowAliases = naturalRows
        .filterIsInstance<ChannelHomeRow>()
        .mapNotNull { row ->
            val legacyId = "channel:${row.channel.legacyKey}"
            if (legacyId == row.id) null else legacyId to row.id
        }
        .toMap()
    val normalizedOrder = normalizeHomeOrder(config.homeRowOrder, naturalIds, rowAliases)
    val rank = normalizedOrder.withIndex().associate { (index, id) -> id to index }
    val rows = naturalRows.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
    val rowSnapshots = remember(rows) { rows.map(HomeRowModel::toFocusSnapshot) }

    LaunchedEffect(rowSnapshots) {
        val reconciled = focusState.reconcile(rowSnapshots)
        focusState = reconciled
        pendingRestore = pendingRestore?.let { pending ->
            val row = rowSnapshots.firstOrNull { it.id == pending.rowId }
            if (row != null && pending.itemId in row.itemIds) pending
            else reconciled.restoreTarget(rowSnapshots)
        }
    }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val topPadding = (screenHeightDp * config.rowsTopMarginPercent.coerceIn(0, 50) / 100f).dp

    fun revealRail(): Boolean {
        if (focusState.railState == HomeRailState.HIDDEN) {
            focusState = focusState.revealRail()
        } else {
            focusState = focusState.focusRail()
            runCatching { railFocus.requestFocus() }
        }
        return true
    }

    fun moveActiveRow(delta: Int) {
        val id = activeRowId ?: return
        val ids = rows.map { it.id }
        val from = ids.indexOf(id)
        if (from < 0) return
        val to = (from + delta).coerceIn(0, ids.lastIndex)
        if (from == to) return
        scope.launch {
            store.update { cfg ->
                val order = normalizeHomeOrder(cfg.homeRowOrder, ids, rowAliases).toMutableList()
                val current = order.indexOf(id)
                if (current < 0) cfg
                else {
                    val target = (current + delta).coerceIn(0, order.lastIndex)
                    if (current == target) cfg
                    else {
                        val value = order.removeAt(current)
                        order.add(target, value)
                        cfg.copy(homeRowOrder = order)
                    }
                }
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = verticalState,
            modifier = Modifier
                .fillMaxSize()
                .focusGroup(),
            contentPadding = PaddingValues(top = topPadding, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            itemsIndexed(
                items = rows,
                key = { _, row -> row.id },
            ) { rowIndex, row ->
                when (row) {
                    is WatchNextHomeRow -> TvContentRow(
                        title = row.title,
                        icon = row.icon,
                        programs = row.programs,
                        config = config,
                        accent = accent,
                        cardWidth = cardWidth,
                        gap = gap,
                        rowId = RowId(row.id),
                        restoreItemId = pendingRestore?.takeIf { it.rowId.value == row.id }?.itemId,
                        onRestoreHandled = { target -> if (pendingRestore == target) pendingRestore = null },
                        onFocused = { itemId ->
                            focusState = focusState.onItemFocused(RowId(row.id), itemId)
                        },
                        onFirstLeft = { revealRail() },
                        onLaunch = { item ->
                            launchDispatcher.dispatch(
                                LaunchRequest(
                                    target = LaunchTarget.TvProgram(
                                        intentUri = item.intentUri,
                                        ownerPackage = item.packageName,
                                    ),
                                    origin = LaunchOrigin.TV_CONTENT,
                                ),
                            )
                        },
                    )

                    is ChannelHomeRow -> TvContentRow(
                        title = row.title,
                        icon = row.icon,
                        programs = row.channel.programs,
                        config = config,
                        accent = accent,
                        cardWidth = cardWidth,
                        gap = gap,
                        rowId = RowId(row.id),
                        programOwnerFallback = row.channel.packageName.ifBlank { "channel:${row.channel.key}" },
                        restoreItemId = pendingRestore?.takeIf { it.rowId.value == row.id }?.itemId,
                        onRestoreHandled = { target -> if (pendingRestore == target) pendingRestore = null },
                        onFocused = { itemId ->
                            focusState = focusState.onItemFocused(RowId(row.id), itemId)
                        },
                        onFirstLeft = { revealRail() },
                        onLaunch = { item ->
                            launchDispatcher.dispatch(
                                LaunchRequest(
                                    target = LaunchTarget.TvProgram(
                                        intentUri = item.intentUri,
                                        ownerPackage = item.packageName.ifBlank { row.channel.packageName },
                                    ),
                                    origin = LaunchOrigin.TV_CONTENT,
                                ),
                            )
                        },
                    )

                    is CategoryHomeRow -> AppCategoryRow(
                        row = row,
                        config = config,
                        accent = accent,
                        movePkg = movePkg,
                        moveFocus = moveFocus,
                        cardWidth = cardWidth,
                        gap = gap,
                        rowId = RowId(row.id),
                        restoreItemId = pendingRestore?.takeIf { it.rowId.value == row.id }?.itemId,
                        onRestoreHandled = { target -> if (pendingRestore == target) pendingRestore = null },
                        onFocused = { itemId ->
                            focusState = focusState.onItemFocused(RowId(row.id), itemId)
                        },
                        onFirstLeft = { revealRail() },
                        onLaunch = onLaunch,
                        onMenu = onMenu,
                    )
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
                    .width(285.dp)
                    .fillMaxHeight()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionRight, Key.Back, Key.Escape -> {
                                val next = focusState.hideRail()
                                focusState = next
                                pendingRestore = next.restoreTarget(rowSnapshots)
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
                            .padding(top = 38.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "AERIAL ROWS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.78f),
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 10.dp),
                        )

                        rows.forEachIndexed { index, row ->
                            RailItem(
                                icon = row.icon,
                                label = row.title,
                                selected = row.id == activeRowId,
                                modifier = if (row.id == activeRowId) Modifier.focusRequester(railFocus) else Modifier,
                                onClick = {
                                    val next = focusState.selectRow(RowId(row.id)).hideRail()
                                    focusState = next
                                    scope.launch {
                                        verticalState.animateScrollToItem(index)
                                        delay(60)
                                        pendingRestore = next.restoreTarget(rowSnapshots)
                                    }
                                },
                            )
                        }

                        Box(Modifier.weight(1f))
                        Text(
                            "ROW TOOLS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.padding(start = 18.dp, bottom = 4.dp),
                        )
                        RailItem(
                            icon = "⚙",
                            label = "Customize current row",
                            selected = false,
                            enabled = activeRowId != null,
                            onClick = { editingRowId = activeRowId },
                        )
                        RailItem(
                            icon = "↑",
                            label = "Move row up",
                            selected = false,
                            enabled = activeRowId != null && rows.indexOfFirst { it.id == activeRowId } > 0,
                            onClick = { moveActiveRow(-1) },
                        )
                        RailItem(
                            icon = "↓",
                            label = "Move row down",
                            selected = false,
                            enabled = activeRowId != null && rows.indexOfFirst { it.id == activeRowId } in 0 until rows.lastIndex,
                            onClick = { moveActiveRow(1) },
                        )
                        RailItem(icon = "⚙", label = "Launcher settings", selected = false, onClick = onSettings)
                        RailItem(icon = "◉", label = "Android settings", selected = false, onClick = onAndroidSettings)
                    }
                }
            }
        }
    }

    val editingRow = rows.firstOrNull { it.id == editingRowId }
    if (editingRow != null) {
        when (editingRow) {
            is CategoryHomeRow -> RowQuickEditor(
                title = editingRow.title,
                icon = editingRow.icon,
                category = editingRow.category,
                channel = null,
                isWatchNext = false,
                allApps = allApps,
                config = config,
                store = store,
                onDismiss = { editingRowId = null },
            )
            is ChannelHomeRow -> RowQuickEditor(
                title = editingRow.title,
                icon = editingRow.icon,
                category = null,
                channel = editingRow.channel,
                isWatchNext = false,
                allApps = allApps,
                config = config,
                store = store,
                onDismiss = { editingRowId = null },
            )
            is WatchNextHomeRow -> RowQuickEditor(
                title = editingRow.title,
                icon = editingRow.icon,
                category = null,
                channel = null,
                isWatchNext = true,
                allApps = allApps,
                config = config,
                store = store,
                onDismiss = { editingRowId = null },
            )
        }
    }
}

@Composable
private fun AppCategoryRow(
    row: CategoryHomeRow,
    config: LauncherConfig,
    accent: Color,
    movePkg: String?,
    moveFocus: FocusRequester,
    cardWidth: Dp,
    gap: Dp,
    rowId: RowId,
    restoreItemId: HomeItemId?,
    onRestoreHandled: (HomeFocusTarget) -> Unit,
    onFocused: (HomeItemId) -> Unit,
    onFirstLeft: () -> Boolean,
    onLaunch: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
) {
    val itemIds = remember(row.apps) { row.apps.map(::appHomeItemId) }
    val itemFocus = remember(rowId, itemIds) { itemIds.associateWith { FocusRequester() } }
    val horizontalState = rememberLazyListState()

    LaunchedEffect(restoreItemId, itemIds) {
        val target = restoreItemId ?: return@LaunchedEffect
        val index = itemIds.indexOf(target)
        if (index < 0) return@LaunchedEffect
        horizontalState.scrollToItem(index)
        withFrameNanos { }
        runCatching { itemFocus.getValue(target).requestFocus() }
        onRestoreHandled(HomeFocusTarget(rowId, target))
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 24.dp, bottom = 10.dp),
                ) {
                    Text(
                        text = row.icon,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.86f),
                    )
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.88f),
                        letterSpacing = 0.4.sp,
                    )
                }
            }

            LazyRow(
                state = horizontalState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 44.dp, top = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                itemsIndexed(
                    items = row.apps,
                    key = { _, app -> "${row.category.id}:${app.pkg}:${app.componentName.orEmpty()}" },
                ) { appIndex, app ->
                    val firstInRow = appIndex == 0
                    val itemId = itemIds[appIndex]
                    var cardModifier: Modifier = Modifier
                        .focusRequester(itemFocus.getValue(itemId))
                        .onFocusChanged {
                            if (it.hasFocus) onFocused(itemId)
                        }
                    if (firstInRow) {
                        cardModifier = cardModifier.onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                                onFirstLeft()
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

@Composable
private fun TvContentRow(
    title: String,
    icon: String,
    programs: List<TvProgramItem>,
    config: LauncherConfig,
    accent: Color,
    cardWidth: Dp,
    gap: Dp,
    rowId: RowId,
    programOwnerFallback: String = "watch_next",
    restoreItemId: HomeItemId?,
    onRestoreHandled: (HomeFocusTarget) -> Unit,
    onFocused: (HomeItemId) -> Unit,
    onFirstLeft: () -> Boolean,
    onLaunch: (TvProgramItem) -> Unit,
) {
    val itemIds = remember(programs, programOwnerFallback) {
        programs.map { tvHomeItemId(it, programOwnerFallback) }
    }
    val itemFocus = remember(rowId, itemIds) { itemIds.associateWith { FocusRequester() } }
    val horizontalState = rememberLazyListState()

    LaunchedEffect(restoreItemId, itemIds) {
        val target = restoreItemId ?: return@LaunchedEffect
        val index = itemIds.indexOf(target)
        if (index < 0) return@LaunchedEffect
        horizontalState.scrollToItem(index)
        withFrameNanos { }
        runCatching { itemFocus.getValue(target).requestFocus() }
        onRestoreHandled(HomeFocusTarget(rowId, target))
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 24.dp, bottom = 10.dp),
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.86f),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.88f),
                    letterSpacing = 0.4.sp,
                )
            }
            LazyRow(
                state = horizontalState,
                contentPadding = PaddingValues(start = 24.dp, end = 44.dp, top = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                itemsIndexed(programs, key = { _, item -> item.id }) { index, item ->
                    val itemId = itemIds[index]
                    var nav = Modifier
                        .focusRequester(itemFocus.getValue(itemId))
                        .onFocusChanged { if (it.hasFocus) onFocused(itemId) }
                    if (index == 0) {
                        nav = nav.onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) onFirstLeft()
                            else false
                        }
                    }
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
    icon: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .padding(horizontal = 9.dp)
            .onFocusChanged { focused = it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent,
            focusedContainerColor = Color(0xFFF2F3F5),
            contentColor = if (enabled) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.32f),
            focusedContentColor = Color(0xFF15161A),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(icon, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
