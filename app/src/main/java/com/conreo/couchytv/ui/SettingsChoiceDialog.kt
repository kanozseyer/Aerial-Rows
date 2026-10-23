package com.conreo.couchytv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

/**
 * TV-friendly named choice surface shared by every settings selector.
 *
 * It restores focus to the currently selected option instead of row 1, so OK
 * opens a predictable list and Back returns to the original selector without
 * changing the value.
 */
@Composable
internal fun SettingsChoiceDialog(
    title: String,
    options: List<String>,
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    require(options.isNotEmpty()) { "Settings choice dialog needs at least one option" }
    val selectedIndex = current.coerceIn(options.indices)
    val selectedFocus = remember(options, selectedIndex) { FocusRequester() }

    LaunchedEffect(selectedFocus) {
        repeat(30) {
            if (runCatching { selectedFocus.requestFocus() }.isSuccess) {
                return@LaunchedEffect
            }
            withFrameNanos { }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.width(360.dp),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                )
                options.forEachIndexed { index, option ->
                    SettingsItem(
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        headlineContent = { Text(option) },
                        trailingContent = { CheckMark(checked = index == selectedIndex) },
                        modifier = if (index == selectedIndex) {
                            Modifier.focusRequester(selectedFocus)
                        } else Modifier,
                    )
                }
            }
        }
    }
}
