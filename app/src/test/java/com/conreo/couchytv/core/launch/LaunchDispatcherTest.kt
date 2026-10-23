package com.conreo.couchytv.core.launch

import com.conreo.couchytv.core.model.AppId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchDispatcherTest {
    private val request = LaunchRequest(
        target = LaunchTarget.App(AppId("com.example.app")),
        origin = LaunchOrigin.HOME_CARD,
    )

    @Test
    fun allowedLaunchReachesExecutorExactlyOnce() {
        var calls = 0
        val dispatcher = LaunchDispatcher(
            policy = AllowAllLaunchPolicy,
            executor = LaunchExecutor {
                calls++
                LaunchResult.Success
            },
        )

        val result = dispatcher.dispatch(request)

        assertEquals(LaunchResult.Success, result)
        assertEquals(1, calls)
    }

    @Test
    fun deniedLaunchNeverReachesExecutor() {
        var executed = false
        val dispatcher = LaunchDispatcher(
            policy = LaunchPolicy { LaunchDecision.Deny("blocked") },
            executor = LaunchExecutor {
                executed = true
                LaunchResult.Success
            },
        )

        val result = dispatcher.dispatch(request)

        assertFalse(executed)
        assertTrue(result is LaunchResult.Blocked)
        assertEquals("blocked", (result as LaunchResult.Blocked).reason)
    }
}
