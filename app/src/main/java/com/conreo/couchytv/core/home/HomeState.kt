package com.conreo.couchytv.core.home

import com.conreo.couchytv.core.model.HomeItemId
import com.conreo.couchytv.core.model.RowId

/**
 * Lightweight semantic snapshot of one home row.
 *
 * Rendering payloads stay outside this core model; this layer owns only stable
 * identities needed for ordering, focus restoration and rail navigation.
 */
data class HomeRowSnapshot(
    val id: RowId,
    val itemIds: List<HomeItemId>,
) {
    init {
        require(itemIds.distinct().size == itemIds.size) {
            "Home row ${id.value} contains duplicate item IDs"
        }
    }
}

enum class HomeRailState {
    HIDDEN,
    REVEALED,
    FOCUSED,
}

data class HomeFocusTarget(
    val rowId: RowId,
    val itemId: HomeItemId?,
)

/**
 * Semantic home focus state. It deliberately stores IDs rather than list
 * positions so row/item reordering does not move focus to unrelated content.
 */
data class HomeFocusState(
    val activeRowId: RowId? = null,
    val lastItemByRow: Map<RowId, HomeItemId> = emptyMap(),
    val railState: HomeRailState = HomeRailState.HIDDEN,
) {
    fun onItemFocused(rowId: RowId, itemId: HomeItemId): HomeFocusState = copy(
        activeRowId = rowId,
        lastItemByRow = lastItemByRow + (rowId to itemId),
        railState = HomeRailState.HIDDEN,
    )

    fun selectRow(rowId: RowId): HomeFocusState = copy(activeRowId = rowId)

    fun revealRail(): HomeFocusState = copy(
        railState = when (railState) {
            HomeRailState.HIDDEN -> HomeRailState.REVEALED
            HomeRailState.REVEALED,
            HomeRailState.FOCUSED,
            -> railState
        },
    )

    fun focusRail(): HomeFocusState = copy(railState = HomeRailState.FOCUSED)

    fun hideRail(): HomeFocusState = copy(railState = HomeRailState.HIDDEN)

    /**
     * Reconcile stored semantic focus against the rows currently available.
     * Missing rows/items are removed, while surviving IDs retain focus even if
     * their positions changed.
     */
    fun reconcile(rows: List<HomeRowSnapshot>): HomeFocusState {
        if (rows.isEmpty()) {
            return copy(
                activeRowId = null,
                lastItemByRow = emptyMap(),
                railState = HomeRailState.HIDDEN,
            )
        }

        val rowsById = rows.associateBy { it.id }
        val reconciledItems = lastItemByRow.mapNotNull { (rowId, itemId) ->
            val row = rowsById[rowId] ?: return@mapNotNull null
            if (itemId in row.itemIds) rowId to itemId else null
        }.toMap()

        return copy(
            activeRowId = activeRowId?.takeIf(rowsById::containsKey) ?: rows.first().id,
            lastItemByRow = reconciledItems,
        )
    }

    /**
     * Exact target to restore when content regains focus after visiting the rail.
     * If the remembered card disappeared, fall back to the first current card in
     * that same semantic row; an empty row still yields its row ID.
     */
    fun restoreTarget(rows: List<HomeRowSnapshot>): HomeFocusTarget? {
        if (rows.isEmpty()) return null
        val row = rows.firstOrNull { it.id == activeRowId } ?: rows.first()
        val remembered = lastItemByRow[row.id]?.takeIf { it in row.itemIds }
        return HomeFocusTarget(
            rowId = row.id,
            itemId = remembered ?: row.itemIds.firstOrNull(),
        )
    }
}
