package com.conreo.couchytv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.PANEL_CLEAR
import com.conreo.couchytv.data.PANEL_LIQUID_GLASS
import com.conreo.couchytv.data.PANEL_SOLID

/** Shared row/dock panel treatment used by Sliding Rows and Dock mode. */
@Composable
fun AerialPanel(
    config: LauncherConfig,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (config.panelStyle == PANEL_CLEAR) {
        Box(modifier = modifier) { content() }
        return
    }

    val tint = panelTint(config)
    val opacity = config.panelOpacity.coerceIn(0, 10) / 10f
    val strength = config.panelEffectStrength.coerceIn(0, 5) / 5f

    val styled = when (config.panelStyle) {
        PANEL_SOLID -> modifier
            .background(tint.copy(alpha = opacity), shape)
        PANEL_LIQUID_GLASS -> modifier
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f * strength),
                    0.22f to tint.copy(alpha = opacity * 0.62f),
                    0.72f to tint.copy(alpha = opacity * 0.48f),
                    1f to Color.Black.copy(alpha = opacity * (0.18f + 0.12f * strength)),
                ),
                shape,
            )
            .border(
                width = (1f + strength * 1.5f).dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.32f * strength),
                        tint.copy(alpha = 0.18f * strength),
                        Color.Black.copy(alpha = 0.22f * strength),
                    )
                ),
                shape = shape,
            )
        else -> modifier
    }

    Box(modifier = styled) { content() }
}

/**
 * TV-friendly tint control: 0 is a neutral charcoal; 1..12 are 30° hue steps.
 * Saturation remains separately adjustable so users can make the glass subtle
 * or strongly coloured without needing a pointer-based colour picker.
 */
fun panelTint(config: LauncherConfig): Color {
    if (config.panelTintHue <= 0) return Color(0xFF151820)
    val hue = ((config.panelTintHue - 1).coerceIn(0, 11) * 30f) % 360f
    val saturation = 0.10f + config.panelTintSaturation.coerceIn(0, 5) * 0.14f
    val hsv = floatArrayOf(hue, saturation.coerceAtMost(0.80f), 0.72f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}