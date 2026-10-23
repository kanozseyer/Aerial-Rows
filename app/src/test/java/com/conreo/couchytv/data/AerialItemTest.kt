package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class AerialItemTest {
    @Test
    fun stableIdentityDoesNotDependOnVisibleTitle() {
        val first = aerialItem(
            sourceId = "A",
            title = "East Asia",
            uri = "https://example.test/aerial.mov",
        )
        val renamed = aerialItem(
            sourceId = "A",
            title = "East Asia at Night",
            uri = "https://example.test/aerial.mov",
        )

        assertNotNull(first)
        assertNotNull(renamed)
        assertEquals(first!!.id, renamed!!.id)
        assertEquals("A:https://example.test/aerial.mov", first.id)
    }

    @Test
    fun metadataIsTrimmedAndBlankTitleGetsFallback() {
        val item = aerialItem(
            sourceId = " C1 ",
            title = "   ",
            uri = " https://example.test/community.mp4 ",
        )

        assertNotNull(item)
        assertEquals("C1", item!!.sourceId)
        assertEquals("Aerial", item.title)
        assertEquals("https://example.test/community.mp4", item.uri)
    }

    @Test
    fun blankSourceOrUriIsRejected() {
        assertNull(aerialItem("", "Title", "https://example.test/a.mp4"))
        assertNull(aerialItem("A", "Title", "   "))
    }
}
