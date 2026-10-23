package com.conreo.couchytv.feature.wallpaper

import android.content.Context
import com.conreo.couchytv.data.AerialFailureTracker
import com.conreo.couchytv.data.AerialItem
import com.conreo.couchytv.data.AerialShuffleBag
import com.conreo.couchytv.data.BuiltinAerials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Catalogue boundary kept separate so provider policy is JVM-testable. */
fun interface AerialCatalogue {
    suspend fun load(sourceIds: Set<String>): List<AerialItem>
}

sealed interface AerialProviderResult {
    data class Ready(val item: AerialItem) : AerialProviderResult
    data class RetryAfter(val delayMs: Long) : AerialProviderResult
    data object Empty : AerialProviderResult
}

/**
 * Built-in Aerial source provider. It owns catalogue selection policy rather
 * than exposing raw asset lists to Compose: load -> prune stale failure state ->
 * exclude quarantined clips -> fair shuffle -> typed result.
 */
class BuiltinAerialProvider(
    private val catalogue: AerialCatalogue,
    private val shuffleBag: AerialShuffleBag = AerialShuffleBag(),
    private val failures: AerialFailureTracker = AerialFailureTracker(),
) {
    suspend fun next(sourceIds: Set<String>): AerialProviderResult {
        val items = catalogue.load(sourceIds)
        failures.retain(items.mapTo(linkedSetOf()) { it.id })
        if (items.isEmpty()) return AerialProviderResult.Empty

        val eligible = failures.eligible(items)
        if (eligible.isEmpty()) {
            return failures.retryDelayMs(items)
                ?.let(AerialProviderResult::RetryAfter)
                ?: AerialProviderResult.Empty
        }

        return shuffleBag.next(eligible)
            ?.let(AerialProviderResult::Ready)
            ?: AerialProviderResult.Empty
    }

    fun recordFailure(itemId: String): Boolean = failures.recordFailure(itemId)

    fun recordSuccess(itemId: String) {
        failures.recordSuccess(itemId)
    }
}

/** Android adapter for the bundled compact `aerials.json` catalogue. */
fun createBuiltinAerialProvider(context: Context): BuiltinAerialProvider {
    val appContext = context.applicationContext
    return BuiltinAerialProvider(
        catalogue = AerialCatalogue { sourceIds ->
            withContext(Dispatchers.IO) {
                BuiltinAerials.loadItems(appContext, sourceIds)
            }
        },
    )
}
