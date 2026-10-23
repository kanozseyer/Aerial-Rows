package com.conreo.couchytv.core.launch

import com.conreo.couchytv.core.model.AppId

sealed interface LaunchTarget {
    data class App(val appId: AppId) : LaunchTarget

    data class TvProgram(
        val intentUri: String,
        val ownerPackage: String,
    ) : LaunchTarget
}

enum class LaunchOrigin {
    HOME_CARD,
    CONTEXT_MENU,
    SETTINGS,
    STATUS_BAR,
    TV_CONTENT,
    REMOTE,
    STARTUP,
    NOTIFICATION,
}

data class LaunchRequest(
    val target: LaunchTarget,
    val origin: LaunchOrigin,
)

sealed interface LaunchDecision {
    data object Allow : LaunchDecision
    data class Deny(val reason: String? = null) : LaunchDecision
}

fun interface LaunchPolicy {
    fun evaluate(request: LaunchRequest): LaunchDecision
}

object AllowAllLaunchPolicy : LaunchPolicy {
    override fun evaluate(request: LaunchRequest): LaunchDecision = LaunchDecision.Allow
}

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class Blocked(val reason: String? = null) : LaunchResult
    data object NoHandler : LaunchResult
    data class Failure(val cause: Throwable? = null) : LaunchResult
}

fun interface LaunchExecutor {
    fun execute(target: LaunchTarget): LaunchResult
}

class LaunchDispatcher(
    private val policy: LaunchPolicy,
    private val executor: LaunchExecutor,
) {
    fun dispatch(request: LaunchRequest): LaunchResult = when (val decision = policy.evaluate(request)) {
        LaunchDecision.Allow -> executor.execute(request.target)
        is LaunchDecision.Deny -> LaunchResult.Blocked(decision.reason)
    }
}
