package com.conreo.couchytv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewInstallPolicyTest {
    private val regularCategories = listOf("streaming", "games", "apps")

    @Test
    fun firstSectionPreservesExistingAssignmentBehavior() {
        assertEquals(
            setOf("streaming"),
            NewInstallAssignment.explicitSections(
                NewInstallPolicy.FIRST_SECTION,
                regularCategories,
            ),
        )
    }

    @Test
    fun autoCategoryLeavesNoExplicitOverride() {
        assertNull(
            NewInstallAssignment.explicitSections(
                NewInstallPolicy.AUTO_CATEGORY,
                regularCategories,
            ),
        )
    }

    @Test
    fun allAppsOnlyStoresExplicitEmptyAssignment() {
        assertEquals(
            emptySet<String>(),
            NewInstallAssignment.explicitSections(
                NewInstallPolicy.ALL_APPS_ONLY,
                regularCategories,
            ),
        )
    }

    @Test
    fun firstSectionWithoutRegularCategorySafelyFallsBackToEmptyAssignment() {
        assertEquals(
            emptySet<String>(),
            NewInstallAssignment.explicitSections(
                NewInstallPolicy.FIRST_SECTION,
                emptyList(),
            ),
        )
    }
}
