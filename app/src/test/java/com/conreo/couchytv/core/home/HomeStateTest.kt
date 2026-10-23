package com.conreo.couchytv.core.home

import com.conreo.couchytv.core.model.AppId
import com.conreo.couchytv.core.model.HomeItemId
import com.conreo.couchytv.core.model.RowId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeStateTest {
    private val rowA = RowId("category:apps")
    private val rowB = RowId("watch_next")
    private val appOne = HomeItemId.App(AppId("com.example.one"))
    private val appTwo = HomeItemId.App(AppId("com.example.two"))
    private val tvOne = HomeItemId.TvProgram("com.provider", "program-1")

    @Test
    fun rowAndItemReorderKeepsExactSemanticFocus() {
        val state = HomeFocusState()
            .onItemFocused(rowA, appTwo)
            .focusRail()

        val reordered = listOf(
            HomeRowSnapshot(rowB, listOf(tvOne)),
            HomeRowSnapshot(rowA, listOf(appTwo, appOne)),
        )

        val reconciled = state.reconcile(reordered)

        assertEquals(rowA, reconciled.activeRowId)
        assertEquals(appTwo, reconciled.restoreTarget(reordered)?.itemId)
        assertEquals(HomeRailState.FOCUSED, reconciled.railState)
    }

    @Test
    fun removedRememberedItemFallsBackInsideSameRow() {
        val state = HomeFocusState().onItemFocused(rowA, appTwo)
        val rows = listOf(HomeRowSnapshot(rowA, listOf(appOne)))

        val reconciled = state.reconcile(rows)

        assertEquals(rowA, reconciled.activeRowId)
        assertEquals(appOne, reconciled.restoreTarget(rows)?.itemId)
        assertNull(reconciled.lastItemByRow[rowA])
    }

    @Test
    fun removedActiveRowFallsBackToFirstCurrentRow() {
        val state = HomeFocusState().onItemFocused(rowA, appOne)
        val rows = listOf(HomeRowSnapshot(rowB, listOf(tvOne)))

        val reconciled = state.reconcile(rows)

        assertEquals(rowB, reconciled.activeRowId)
        assertEquals(tvOne, reconciled.restoreTarget(rows)?.itemId)
    }

    @Test
    fun railHasExplicitHiddenRevealedFocusedStates() {
        val hidden = HomeFocusState()
        val revealed = hidden.revealRail()
        val focused = revealed.focusRail()
        val restored = focused.hideRail()

        assertEquals(HomeRailState.HIDDEN, hidden.railState)
        assertEquals(HomeRailState.REVEALED, revealed.railState)
        assertEquals(HomeRailState.FOCUSED, focused.railState)
        assertEquals(HomeRailState.HIDDEN, restored.railState)
    }

    @Test
    fun emptyHomeClearsSemanticFocusAndRail() {
        val state = HomeFocusState()
            .onItemFocused(rowA, appOne)
            .focusRail()

        val reconciled = state.reconcile(emptyList())

        assertNull(reconciled.activeRowId)
        assertEquals(emptyMap<RowId, HomeItemId>(), reconciled.lastItemByRow)
        assertEquals(HomeRailState.HIDDEN, reconciled.railState)
        assertNull(reconciled.restoreTarget(emptyList()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateItemIdentityInsideRowIsRejected() {
        HomeRowSnapshot(rowA, listOf(appOne, appOne))
    }
}
