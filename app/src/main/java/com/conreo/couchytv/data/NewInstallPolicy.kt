package com.conreo.couchytv.data

import kotlinx.serialization.Serializable

/**
 * Defines how a newly discovered app is assigned to user-managed sections.
 *
 * FIRST_SECTION preserves Couchy's existing behavior. AUTO_CATEGORY deliberately
 * returns no explicit override so AppRepository.sectionsOf() can keep using the
 * app's platform/known-package category. ALL_APPS_ONLY stores an explicit empty
 * assignment, which keeps the app out of manual categories while it can still
 * appear in the special All Apps row.
 */
@Serializable
enum class NewInstallPolicy {
    FIRST_SECTION,
    AUTO_CATEGORY,
    ALL_APPS_ONLY,
}

object NewInstallAssignment {
    /**
     * @return explicit section IDs to persist, or null when the app should keep
     * using automatic categorization with no stored section override.
     */
    fun explicitSections(
        policy: NewInstallPolicy,
        regularCategoryIds: List<String>,
    ): Set<String>? = when (policy) {
        NewInstallPolicy.FIRST_SECTION -> regularCategoryIds.firstOrNull()?.let(::setOf) ?: emptySet()
        NewInstallPolicy.AUTO_CATEGORY -> null
        NewInstallPolicy.ALL_APPS_ONLY -> emptySet()
    }
}
