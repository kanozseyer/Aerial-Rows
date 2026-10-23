package com.conreo.couchytv.core.home

import com.conreo.couchytv.core.model.AppId
import com.conreo.couchytv.core.model.HomeItemId
import com.conreo.couchytv.core.model.RowId

/**
 * Layout-independent origin of a row. Visible titles and list positions are not
 * identity; each source carries the smallest stable semantic key it owns.
 */
sealed interface HomeRowSource {
    data class AppCategory(val categoryId: String) : HomeRowSource {
        init {
            require(categoryId.isNotBlank()) { "App category ID must not be blank" }
        }
    }

    data object WatchNext : HomeRowSource

    data class TvChannel(val channelKey: String) : HomeRowSource {
        init {
            require(channelKey.isNotBlank()) { "TV channel key must not be blank" }
        }
    }

    data object NowPlaying : HomeRowSource
    data object Inputs : HomeRowSource
    data object Shortcuts : HomeRowSource

    data class Folder(val folderId: String) : HomeRowSource {
        init {
            require(folderId.isNotBlank()) { "Folder ID must not be blank" }
        }
    }

    data object InternalActions : HomeRowSource
}

/**
 * Row-level presentation policy which is safe to persist/compare independently
 * from Compose. Pixel sizes, brushes and Android resources stay in the renderer.
 */
data class RowPresentation(
    val showTitle: Boolean = true,
    /** User-configured rows can stay addressable when their content is empty. */
    val keepVisibleWhenEmpty: Boolean = false,
)

/**
 * Generic home item identity. Rendering payload (AppEntry, bitmap, provider
 * artwork, MediaController, etc.) remains owned by the feature/platform layer.
 */
sealed interface HomeItem {
    val id: HomeItemId

    data class App(
        override val id: HomeItemId.App,
    ) : HomeItem {
        constructor(appId: AppId) : this(HomeItemId.App(appId))
    }

    data class TvProgram(
        override val id: HomeItemId.TvProgram,
    ) : HomeItem

    data class InternalAction(
        override val id: HomeItemId.InternalAction,
        val label: String,
    ) : HomeItem {
        init {
            require(label.isNotBlank()) { "Internal action label must not be blank" }
        }
    }

    /**
     * Concrete focusable item for a configured row whose ordinary content is
     * empty. The stable ID is derived from row identity, never from its label.
     */
    data class EmptyRowAction(
        val rowId: RowId,
        val label: String,
    ) : HomeItem {
        init {
            require(label.isNotBlank()) { "Empty-row action label must not be blank" }
        }

        override val id: HomeItemId = HomeItemId.InternalAction(
            actionId = "populate-row:${rowId.value}",
        )
    }
}

/**
 * Semantic row shared by future home sources and layouts. The renderer can join
 * each item's stable ID back to its richer feature payload without changing row
 * ordering/focus identity.
 */
data class HomeRow(
    val id: RowId,
    val source: HomeRowSource,
    val title: String,
    val presentation: RowPresentation = RowPresentation(),
    val items: List<HomeItem>,
) {
    init {
        require(title.isNotBlank()) { "Home row title must not be blank" }
        require(items.map { it.id }.distinct().size == items.size) {
            "Home row ${id.value} contains duplicate item IDs"
        }
        require(items.isNotEmpty() || !presentation.keepVisibleWhenEmpty) {
            "Configured empty row ${id.value} requires an EmptyRowAction"
        }
    }

    fun toFocusSnapshot(): HomeRowSnapshot = HomeRowSnapshot(
        id = id,
        itemIds = items.map { it.id },
    )
}
