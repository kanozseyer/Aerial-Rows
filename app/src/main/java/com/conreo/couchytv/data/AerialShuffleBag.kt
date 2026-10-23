package com.conreo.couchytv.data

/**
 * Fair Aerial selector: every currently eligible clip is returned once before
 * the bag is refilled. Across bag boundaries the first clip is swapped when
 * necessary so the user does not see the same clip twice in a row.
 *
 * A catalogue/source change starts a fresh bag immediately while preserving the
 * previous item only as the no-immediate-repeat boundary. The bag stores IDs,
 * not item payloads, so metadata-only catalogue updates are reflected instantly.
 */
class AerialShuffleBag(
    private val shuffle: (List<AerialItem>) -> List<AerialItem> = { it.shuffled() },
) {
    private var catalogueIds: Set<String> = emptySet()
    private var remainingIds: MutableList<String> = mutableListOf()
    private var lastPlayedId: String? = null

    fun next(items: List<AerialItem>): AerialItem? {
        val catalogue = items.distinctBy(AerialItem::id)
        if (catalogue.isEmpty()) {
            catalogueIds = emptySet()
            remainingIds.clear()
            return null
        }

        val byId = catalogue.associateBy(AerialItem::id)
        val ids = catalogue.mapTo(linkedSetOf(), AerialItem::id)
        if (ids != catalogueIds) {
            catalogueIds = ids
            remainingIds = refill(catalogue)
        } else if (remainingIds.isEmpty()) {
            remainingIds = refill(catalogue)
        }

        val nextId = remainingIds.removeAt(0)
        val next = byId[nextId] ?: return next(catalogue)
        lastPlayedId = next.id
        return next
    }

    private fun refill(items: List<AerialItem>): MutableList<String> {
        val bag = shuffle(items)
            .distinctBy(AerialItem::id)
            .map(AerialItem::id)
            .toMutableList()
        val previous = lastPlayedId
        if (previous != null && bag.size > 1 && bag.first() == previous) {
            val replacement = bag.indexOfFirst { it != previous }
            if (replacement > 0) {
                val first = bag[0]
                bag[0] = bag[replacement]
                bag[replacement] = first
            }
        }
        return bag
    }
}
