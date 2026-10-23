package com.conreo.couchytv.core.settings

/**
 * Executes non-Compose setting side effects in a deterministic order.
 *
 * RECOMPOSE is intentionally omitted: persisted/preview config state already
 * drives Compose. NONE is likewise a no-op. Keeping this policy pure makes it
 * easy for Android adapters to map reload/recreate/restart effects to callbacks
 * without embedding those decisions in individual settings rows.
 */
class SettingsEffectDispatcher(
    private val handler: suspend (SettingEffect) -> Unit,
) {
    suspend fun dispatch(batch: SettingEffectBatch) {
        batch.externalEffects().forEach { handler(it) }
    }
}

private val EXTERNAL_EFFECT_ORDER = listOf(
    SettingEffect.RELOAD_APPS,
    SettingEffect.RELOAD_TV_CONTENT,
    SettingEffect.RELOAD_WALLPAPER,
    SettingEffect.RECREATE_ACTIVITY,
    SettingEffect.RESTART_HOME_TASK,
)

fun SettingEffectBatch.externalEffects(): List<SettingEffect> =
    EXTERNAL_EFFECT_ORDER.filter { it in effects }
