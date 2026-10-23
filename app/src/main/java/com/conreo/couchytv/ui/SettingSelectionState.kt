package com.conreo.couchytv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.conreo.couchytv.core.settings.PersistencePolicy
import kotlinx.coroutines.delay

internal data class SettingSelectionState(
    val index: Int,
    val select: (Int) -> Unit,
)

/**
 * Local preview state for a D-pad selector.
 *
 * DEBOUNCED changes update the UI immediately but coalesce persistence into one
 * commit after the user pauses. A pending value is flushed if the row leaves
 * composition, so backing out of Settings cannot lose the final selection.
 */
@Composable
internal fun rememberSettingSelection(
    current: Int,
    persistence: PersistencePolicy,
    onCommit: (Int) -> Unit,
    debounceMs: Long = 220L,
): SettingSelectionState {
    require(debounceMs > 0L) { "debounceMs must be positive" }

    var preview by remember { mutableIntStateOf(current) }
    val latestCommit by rememberUpdatedState(onCommit)
    val latestCurrent by rememberUpdatedState(current)
    val latestPreview by rememberUpdatedState(preview)

    // External/persisted updates remain authoritative once they arrive. This
    // effect does not restart while only the local preview changes.
    LaunchedEffect(current) {
        preview = current
    }

    LaunchedEffect(preview, current, persistence, debounceMs) {
        if (persistence == PersistencePolicy.DEBOUNCED && preview != current) {
            delay(debounceMs)
            latestCommit(preview)
        }
    }

    DisposableEffect(persistence) {
        onDispose {
            if (
                persistence == PersistencePolicy.DEBOUNCED &&
                latestPreview != latestCurrent
            ) {
                latestCommit(latestPreview)
            }
        }
    }

    val visibleIndex = when (persistence) {
        PersistencePolicy.DEBOUNCED,
        PersistencePolicy.SESSION_ONLY,
        -> preview
        PersistencePolicy.IMMEDIATE,
        PersistencePolicy.ON_CONFIRM,
        -> current
    }

    val select: (Int) -> Unit = { next ->
        when (persistence) {
            PersistencePolicy.DEBOUNCED,
            PersistencePolicy.SESSION_ONLY,
            -> preview = next
            PersistencePolicy.IMMEDIATE,
            PersistencePolicy.ON_CONFIRM,
            -> latestCommit(next)
        }
    }

    return SettingSelectionState(
        index = visibleIndex,
        select = select,
    )
}
