package com.conreo.couchytv.feature.wallpaper

/** Why a configured wallpaper candidate is temporarily unavailable. */
enum class WallpaperUnavailableReason {
    NETWORK,
    LOAD_FAILED,
    MISSING_ASSET,
    QUARANTINED,
}

/** One concrete candidate selected from a [WallpaperPlan]. */
data class WallpaperSelection(
    val candidate: WallpaperCandidate,
    val fallbackDepth: Int,
    val revision: Long,
)

/** Semantic events consumed by the wallpaper state machine. */
sealed interface WallpaperEvent {
    data class PlanChanged(val plan: WallpaperPlan) : WallpaperEvent

    data class SourceUnavailable(
        val candidate: WallpaperCandidate,
        val reason: WallpaperUnavailableReason,
    ) : WallpaperEvent

    data class SourceRecovered(val candidate: WallpaperCandidate) : WallpaperEvent

    /**
     * Requests a fresh value from the same logical source (for example next
     * Aerial clip or re-decoded photo) without changing source priority.
     */
    data class RefreshRequested(val candidate: WallpaperCandidate) : WallpaperEvent
}

/**
 * Pure wallpaper coordinator state. Android providers/renderers can feed it
 * semantic availability/refresh events while the UI only observes [selection].
 *
 * The final preset candidate is intentionally fail-safe: attempts to mark it
 * unavailable are ignored so HOME always has one renderable background choice.
 */
data class WallpaperEngineState(
    val plan: WallpaperPlan,
    val unavailable: Map<WallpaperCandidate, WallpaperUnavailableReason> = emptyMap(),
    val revisions: Map<WallpaperCandidate, Long> = emptyMap(),
) {
    val selection: WallpaperSelection
        get() {
            val chosenIndex = plan.candidates.indexOfFirst { candidate ->
                candidate is WallpaperCandidate.Preset || candidate !in unavailable
            }.let { if (it >= 0) it else plan.candidates.lastIndex }
            val candidate = plan.candidates[chosenIndex]
            return WallpaperSelection(
                candidate = candidate,
                fallbackDepth = chosenIndex,
                revision = revisions[candidate] ?: 0L,
            )
        }

    fun reduce(event: WallpaperEvent): WallpaperEngineState = when (event) {
        is WallpaperEvent.PlanChanged -> {
            val allowed = event.plan.candidates.toSet()
            copy(
                plan = event.plan,
                unavailable = unavailable.filterKeys { it in allowed && it !is WallpaperCandidate.Preset },
                revisions = revisions.filterKeys { it in allowed },
            )
        }

        is WallpaperEvent.SourceUnavailable -> {
            if (event.candidate !in plan.candidates || event.candidate is WallpaperCandidate.Preset) this
            else copy(unavailable = unavailable + (event.candidate to event.reason))
        }

        is WallpaperEvent.SourceRecovered -> {
            if (event.candidate !in unavailable) this
            else copy(unavailable = unavailable - event.candidate)
        }

        is WallpaperEvent.RefreshRequested -> {
            if (event.candidate !in plan.candidates) this
            else copy(
                revisions = revisions +
                    (event.candidate to ((revisions[event.candidate] ?: 0L) + 1L)),
            )
        }
    }
}
