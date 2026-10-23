package com.conreo.couchytv.core.settings

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsEffectDispatcherTest {
    @Test
    fun externalEffectsIgnoreComposeOnlyEffectsAndUseStableOrder() {
        val batch = SettingEffectBatch(
            setOf(
                SettingEffect.RESTART_HOME_TASK,
                SettingEffect.RECOMPOSE,
                SettingEffect.RELOAD_WALLPAPER,
                SettingEffect.RELOAD_APPS,
                SettingEffect.NONE,
            ),
        )

        assertEquals(
            listOf(
                SettingEffect.RELOAD_APPS,
                SettingEffect.RELOAD_WALLPAPER,
                SettingEffect.RESTART_HOME_TASK,
            ),
            batch.externalEffects(),
        )
    }

    @Test
    fun dispatcherInvokesEachRequestedExternalEffectOnce() = runBlocking {
        val seen = mutableListOf<SettingEffect>()
        val dispatcher = SettingsEffectDispatcher { seen += it }

        dispatcher.dispatch(
            SettingEffectBatch(
                setOf(
                    SettingEffect.RELOAD_TV_CONTENT,
                    SettingEffect.RELOAD_TV_CONTENT,
                    SettingEffect.RECREATE_ACTIVITY,
                ),
            ),
        )

        assertEquals(
            listOf(
                SettingEffect.RELOAD_TV_CONTENT,
                SettingEffect.RECREATE_ACTIVITY,
            ),
            seen,
        )
    }
}
