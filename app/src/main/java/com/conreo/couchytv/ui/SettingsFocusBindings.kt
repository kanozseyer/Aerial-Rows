package com.conreo.couchytv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.conreo.couchytv.core.settings.SettingId

/**
 * Stable FocusRequesters keyed by semantic SettingId, reusable by every settings
 * route. A restored target retries only until its node is attached, so quick D-pad
 * navigation is never yanked back by a delayed fixed-time focus request.
 */
internal class SettingsFocusBindings(
    private val requesters: Map<SettingId, FocusRequester>,
    private val onFocused: (SettingId) -> Unit,
) {
    fun modifier(id: SettingId): Modifier {
        val requester = requireNotNull(requesters[id]) { "Unknown settings focus id: $id" }
        return Modifier
            .focusRequester(requester)
            .onFocusChanged { state -> if (state.isFocused) onFocused(id) }
    }
}

@Composable
internal fun rememberSettingsFocusBindings(
    ids: List<SettingId>,
    rememberedFocus: SettingId?,
    defaultFocus: SettingId,
    onFocused: (SettingId) -> Unit,
): SettingsFocusBindings {
    require(ids.isNotEmpty()) { "Settings focus IDs must not be empty" }
    require(defaultFocus in ids) { "Default settings focus ID must be registered" }

    val requesters = remember(ids) { ids.associateWith { FocusRequester() } }
    val target = rememberedFocus?.takeIf { it in requesters } ?: defaultFocus

    LaunchedEffect(target, requesters) {
        repeat(30) {
            if (runCatching { requesters.getValue(target).requestFocus() }.isSuccess) {
                return@LaunchedEffect
            }
            withFrameNanos { }
        }
    }

    return SettingsFocusBindings(requesters, onFocused)
}
