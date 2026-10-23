package com.conreo.couchytv.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IdsTest {
    @Test
    fun appStableKeyIncludesStrongestAvailableIdentity() {
        val id = AppId(
            packageName = "com.example.app",
            componentName = "com.example.app/.TvActivity",
            userSerial = 10L,
        )

        assertEquals(
            "com.example.app|com.example.app/.TvActivity|10",
            id.stableKey,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankRowIdIsRejected() {
        RowId("")
    }
}
