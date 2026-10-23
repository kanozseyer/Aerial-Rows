package com.conreo.couchytv.core.settings

@JvmInline
value class SettingId(val value: String) {
    init {
        require(value.isNotBlank()) { "SettingId must not be blank" }
    }

    override fun toString(): String = value
}

@JvmInline
value class SettingsRoute(val value: String) {
    init {
        require(value.isNotBlank()) { "SettingsRoute must not be blank" }
    }

    override fun toString(): String = value

    companion object {
        val Root = SettingsRoute("root")
    }
}

enum class PersistencePolicy {
    IMMEDIATE,
    DEBOUNCED,
    ON_CONFIRM,
    SESSION_ONLY,
}

enum class SettingEffect {
    NONE,
    RECOMPOSE,
    RELOAD_APPS,
    RELOAD_TV_CONTENT,
    RELOAD_WALLPAPER,
    RECREATE_ACTIVITY,
    RESTART_HOME_TASK,
}

enum class ExportPolicy {
    INCLUDE,
    EXCLUDE,
}

/**
 * Renderer-independent contract for one setting. UI labels/controls are adapters;
 * identity, validation, persistence and side effects stay stable across surfaces.
 */
class SettingDescriptor<T>(
    val id: SettingId,
    val route: SettingsRoute,
    val persistence: PersistencePolicy = PersistencePolicy.IMMEDIATE,
    val effect: SettingEffect = SettingEffect.NONE,
    val exportPolicy: ExportPolicy = ExportPolicy.INCLUDE,
    private val validator: (T) -> Boolean = { true },
) {
    fun accepts(value: T): Boolean = validator(value)

    fun requireValid(value: T): T {
        require(accepts(value)) { "Invalid value for setting ${id.value}" }
        return value
    }
}

/**
 * Pure navigation/focus state for the side-panel settings UI. Routes and focused
 * controls use semantic IDs, so reordering labels or inserting rows cannot break
 * return-focus behavior.
 */
data class SettingsNavigationState(
    val routeStack: List<SettingsRoute> = listOf(SettingsRoute.Root),
    val lastFocusByRoute: Map<SettingsRoute, SettingId> = emptyMap(),
) {
    init {
        require(routeStack.isNotEmpty()) { "Settings route stack must not be empty" }
    }

    val currentRoute: SettingsRoute
        get() = routeStack.last()

    val canGoBack: Boolean
        get() = routeStack.size > 1

    fun open(route: SettingsRoute): SettingsNavigationState =
        if (route == currentRoute) this else copy(routeStack = routeStack + route)

    fun back(): SettingsNavigationState =
        if (!canGoBack) this else copy(routeStack = routeStack.dropLast(1))

    fun rememberFocus(
        route: SettingsRoute = currentRoute,
        settingId: SettingId,
    ): SettingsNavigationState = copy(
        lastFocusByRoute = lastFocusByRoute + (route to settingId),
    )

    fun rememberedFocus(route: SettingsRoute = currentRoute): SettingId? =
        lastFocusByRoute[route]
}
