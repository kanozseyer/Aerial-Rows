package com.conreo.couchytv.core.settings

/** Immutable, validated change request produced before any persistence/UI adapter runs. */
data class PlannedSettingChange<T>(
    val id: SettingId,
    val value: T,
    val persistence: PersistencePolicy,
    val effects: Set<SettingEffect>,
    val exportPolicy: ExportPolicy,
)

fun <T> SettingDescriptor<T>.planChange(value: T): PlannedSettingChange<T> =
    PlannedSettingChange(
        id = id,
        value = requireValid(value),
        persistence = persistence,
        effects = if (effect == SettingEffect.NONE) emptySet() else setOf(effect),
        exportPolicy = exportPolicy,
    )

/**
 * Accumulates side effects from a burst/batch without losing independent work.
 * NONE is never retained once a real effect is present.
 */
data class SettingEffectBatch(
    val effects: Set<SettingEffect> = emptySet(),
) {
    fun plus(effect: SettingEffect): SettingEffectBatch = when (effect) {
        SettingEffect.NONE -> this
        else -> copy(effects = effects + effect)
    }

    fun plus(change: PlannedSettingChange<*>): SettingEffectBatch =
        copy(effects = effects + change.effects)

    operator fun contains(effect: SettingEffect): Boolean = effect in effects
}

/**
 * Runtime adapters use this to decide when persistence happens while UI can
 * preview the validated value immediately.
 */
sealed interface PersistenceDirective {
    data object PersistNow : PersistenceDirective
    data class Debounce(val delayMs: Long) : PersistenceDirective {
        init {
            require(delayMs > 0L) { "Debounce delay must be positive" }
        }
    }
    data object AwaitConfirmation : PersistenceDirective
    data object SessionOnly : PersistenceDirective
}

fun PersistencePolicy.directive(debounceMs: Long = 220L): PersistenceDirective = when (this) {
    PersistencePolicy.IMMEDIATE -> PersistenceDirective.PersistNow
    PersistencePolicy.DEBOUNCED -> PersistenceDirective.Debounce(debounceMs)
    PersistencePolicy.ON_CONFIRM -> PersistenceDirective.AwaitConfirmation
    PersistencePolicy.SESSION_ONLY -> PersistenceDirective.SessionOnly
}
