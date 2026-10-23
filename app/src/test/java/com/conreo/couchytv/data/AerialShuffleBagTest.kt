package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AerialShuffleBagTest {
    private fun item(id: String) = AerialItem(
        id = id,
        title = id,
        sourceId = "A",
        uri = "https://example.test/$id.mov",
    )

    @Test
    fun everyClipPlaysOnceBeforeBagRefills() {
        val items = listOf(item("a"), item("b"), item("c"))
        val bag = AerialShuffleBag { it }

        val firstCycle = List(3) { bag.next(items)!!.id }
        val fourth = bag.next(items)!!.id

        assertEquals(listOf("a", "b", "c"), firstCycle)
        assertEquals("a", fourth)
    }

    @Test
    fun refillNeverImmediatelyRepeatsPreviousClip() {
        val items = listOf(item("a"), item("b"), item("c"))
        var shuffleCount = 0
        val bag = AerialShuffleBag {
            shuffleCount++
            if (shuffleCount == 1) it else listOf(it[2], it[0], it[1])
        }

        repeat(2) { bag.next(items) }
        val lastOfFirstBag = bag.next(items)!!.id
        val firstOfSecondBag = bag.next(items)!!.id

        assertEquals("c", lastOfFirstBag)
        assertNotEquals(lastOfFirstBag, firstOfSecondBag)
    }

    @Test
    fun catalogueChangeStartsFreshBagWithoutRepeatingCurrentClip() {
        val a = item("a")
        val b = item("b")
        val bag = AerialShuffleBag { it }

        assertEquals("a", bag.next(listOf(a, b))!!.id)
        val afterChange = bag.next(listOf(a, item("d")))!!.id

        assertEquals("d", afterChange)
    }

    @Test
    fun metadataUpdatesAreResolvedFromCurrentCatalogue() {
        val a = item("a")
        val b = item("b")
        val bag = AerialShuffleBag { it }

        assertEquals("a", bag.next(listOf(a, b))!!.id)
        val renamedB = b.copy(title = "Updated title")

        assertEquals("Updated title", bag.next(listOf(a, renamedB))!!.title)
    }

    @Test
    fun duplicateIdsArePlayedOnlyOncePerBagAndEmptyCatalogueReturnsNull() {
        val a = item("a")
        val bag = AerialShuffleBag { it }

        assertEquals("a", bag.next(listOf(a, a.copy(title = "renamed")))!!.id)
        assertEquals("a", bag.next(listOf(a, a.copy(title = "renamed")))!!.id)
        assertNull(bag.next(emptyList()))
    }
}
