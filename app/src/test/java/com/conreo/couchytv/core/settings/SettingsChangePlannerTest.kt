package com.conreo.couchytv.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsChangePlannerTest {
    @Test
    fun plannedChangeIsValidatedAndCarriesPolicy() {
        val descriptor = SettingDescriptor(
            id = SettingId("display.scale"),
            route = SettingsRoutes.Display,
            persistence = PersistencePolicy.DEBOUNCED,
            effect = SettingEffect.RECOMPOSE,
            exportPolicy = ExportPolicy.INCLUDE,
            validator = { value: Int -> value in 0..5 },
        )

        val change = descriptor.planChange(4)
        assertEquals(SettingId("display.scale"), change.id)
        assertEquals(4, change.value)
        assertEquals(PersistencePolicy.DEBOUNCED, change.persistence)
        assertTrue(SettingEffect.RECOMPOSE in change.effects)
        assertThrows(IllegalArgumentException::class.java) { descriptor.planChange(9) }
    }

    @Test
    fun noneEffectDoesNotPolluteBatch() {
        val batch = SettingEffectBatch()
            .plus(SettingEffect.NONE)
            .plus(SettingEffect.RELOAD_WALLPAPER)
            .plus(SettingEffect.RELOAD_APPS)
            .plus(SettingEffect.NONE)

        assertEquals(2, batch.effects.size)
        assertTrue(SettingEffect.RELOAD_WALLPAPER in batch)
        assertTrue(SettingEffect.RELOAD_APPS in batch)
        assertFalse(SettingEffect.NONE in batch)
    }

    @Test
    fun persistencePoliciesMapToExecutionDirectives() {
        assertEquals(PersistenceDirective.PersistNow, PersistencePolicy.IMMEDIATE.directive())
        assertEquals(PersistenceDirective.Debounce(220), PersistencePolicy.DEBOUNCED.directive())
        assertEquals(PersistenceDirective.AwaitConfirmation, PersistencePolicy.ON_CONFIRM.directive())
        assertEquals(PersistenceDirective.SessionOnly, PersistencePolicy.SESSION_ONLY.directive())
        assertThrows(IllegalArgumentException::class.java) {
            PersistencePolicy.DEBOUNCED.directive(debounceMs = 0)
        }
    }
}
