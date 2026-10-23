package com.conreo.couchytv.core.home

import com.conreo.couchytv.core.model.AppId
import com.conreo.couchytv.core.model.HomeItemId
import com.conreo.couchytv.core.model.RowId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HomeModelTest {
    @Test
    fun rowProjectsDirectlyToSemanticFocusSnapshot() {
        val appId = AppId(
            packageName = "com.example.tv",
            componentName = "com.example.tv/.MainActivity",
        )
        val row = HomeRow(
            id = RowId("apps"),
            source = HomeRowSource.AppCategory("apps"),
            title = "Apps",
            items = listOf(HomeItem.App(appId)),
        )

        assertEquals(
            HomeRowSnapshot(
                id = RowId("apps"),
                itemIds = listOf(HomeItemId.App(appId)),
            ),
            row.toFocusSnapshot(),
        )
    }

    @Test
    fun duplicateSemanticItemsAreRejectedEvenWhenPayloadWouldDiffer() {
        val id = HomeItemId.TvProgram(
            providerPackage = "com.example.provider",
            programKey = "episode-7",
        )

        assertThrows(IllegalArgumentException::class.java) {
            HomeRow(
                id = RowId("watch_next"),
                source = HomeRowSource.WatchNext,
                title = "Watch Next",
                items = listOf(HomeItem.TvProgram(id), HomeItem.TvProgram(id)),
            )
        }
    }

    @Test
    fun configuredEmptyRowUsesStableRowDerivedActionIdentity() {
        val rowId = RowId("category:favourites")
        val action = HomeItem.EmptyRowAction(
            rowId = rowId,
            label = "Add apps",
        )
        val row = HomeRow(
            id = rowId,
            source = HomeRowSource.AppCategory("favourites"),
            title = "Favourites",
            presentation = RowPresentation(keepVisibleWhenEmpty = true),
            items = listOf(action),
        )

        assertEquals(
            HomeItemId.InternalAction("populate-row:category:favourites"),
            action.id,
        )
        assertEquals(listOf(action.id), row.toFocusSnapshot().itemIds)
    }

    @Test
    fun configuredRowCannotRemainVisibleAsAnUnfocusableEmptyContainer() {
        assertThrows(IllegalArgumentException::class.java) {
            HomeRow(
                id = RowId("category:favourites"),
                source = HomeRowSource.AppCategory("favourites"),
                title = "Favourites",
                presentation = RowPresentation(keepVisibleWhenEmpty = true),
                items = emptyList(),
            )
        }
    }

    @Test
    fun ephemeralProviderRowMayBeEmptyBeforeRepositoryFiltersIt() {
        val row = HomeRow(
            id = RowId("watch_next"),
            source = HomeRowSource.WatchNext,
            title = "Watch Next",
            items = emptyList(),
        )

        assertEquals(emptyList<HomeItemId>(), row.toFocusSnapshot().itemIds)
    }
}
