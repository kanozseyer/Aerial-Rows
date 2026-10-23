package com.conreo.couchytv.feature.wallpaper

/**
 * Small runtime owner for [WallpaperEngineState].
 *
 * Providers/renderers report semantic events here; callers observe [selection]
 * instead of reproducing fallback priority and revision bookkeeping themselves.
 * The coordinator owns no coroutine scope, service, player or polling loop.
 */
class WallpaperEngine(initialPlan: WallpaperPlan) {
    var state: WallpaperEngineState = WallpaperEngineState(initialPlan)
        private set

    val selection: WallpaperSelection
        get() = state.selection

    fun dispatch(event: WallpaperEvent): WallpaperSelection {
        state = state.reduce(event)
        return state.selection
    }

    fun updatePlan(plan: WallpaperPlan): WallpaperSelection =
        dispatch(WallpaperEvent.PlanChanged(plan))

    fun markUnavailable(
        candidate: WallpaperCandidate,
        reason: WallpaperUnavailableReason,
    ): WallpaperSelection =
        dispatch(WallpaperEvent.SourceUnavailable(candidate, reason))

    fun markRecovered(candidate: WallpaperCandidate): WallpaperSelection =
        dispatch(WallpaperEvent.SourceRecovered(candidate))

    fun requestRefresh(candidate: WallpaperCandidate): WallpaperSelection =
        dispatch(WallpaperEvent.RefreshRequested(candidate))
}
