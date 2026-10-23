package com.conreo.couchytv.feature.wallpaper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.conreo.couchytv.data.AerialItem
import com.conreo.couchytv.data.LauncherConfig
import kotlinx.coroutines.delay

private const val AERIAL_SAFETY_ROTATION_MS = 10 * 60_000L
private const val AERIAL_CONNECTED_RETRY_MS = 2_500L
private const val AERIAL_OFFLINE_RETRY_MS = 6_000L

/**
 * One runtime owner for wallpaper selection and rendering.
 *
 * [WallpaperEngineState] remains the pure policy brain; this composable is the
 * Android/Compose adapter that feeds source availability into it and renders the
 * selected candidate. Disabled sources do no provider work.
 */
@Composable
fun WallpaperHost(
    config: LauncherConfig,
    networkConnected: Boolean,
    photoRefreshToken: Int,
    dockExpanded: Boolean,
    presetBrush: Brush,
    videoSpeed: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val plan = remember(config) { WallpaperPlanResolver.from(config) }
    var engine by remember { mutableStateOf(WallpaperEngineState(plan)) }

    LaunchedEffect(plan) {
        engine = engine.reduce(WallpaperEvent.PlanChanged(plan))
    }

    val aerialCandidate = plan.candidates
        .filterIsInstance<WallpaperCandidate.BuiltinAerials>()
        .firstOrNull()
    val photoCandidate = WallpaperCandidate.CustomPhoto.takeIf { it in plan.candidates }

    val photoProvider = remember(context) { createLocalPhotoProvider(context) }
    var photoAsset by remember { mutableStateOf<PhotoWallpaperAsset?>(null) }
    val photoRevision = engine.revisions[WallpaperCandidate.CustomPhoto] ?: 0L

    LaunchedEffect(photoCandidate, photoRefreshToken, photoRevision) {
        if (photoCandidate == null) {
            photoAsset = null
            return@LaunchedEffect
        }

        val asset = photoProvider.load()
        photoAsset = asset
        engine = engine.reduce(
            if (asset == null) {
                WallpaperEvent.SourceUnavailable(
                    WallpaperCandidate.CustomPhoto,
                    WallpaperUnavailableReason.MISSING_ASSET,
                )
            } else {
                WallpaperEvent.SourceRecovered(WallpaperCandidate.CustomPhoto)
            }
        )
    }

    val aerialProvider = remember(context) { createBuiltinAerialProvider(context) }
    var aerialItem by remember { mutableStateOf<AerialItem?>(null) }
    var aerialFailureEpoch by remember { mutableIntStateOf(0) }
    val aerialRevision = aerialCandidate?.let { engine.revisions[it] ?: 0L } ?: 0L

    LaunchedEffect(aerialCandidate, networkConnected, aerialRevision) {
        val candidate = aerialCandidate ?: run {
            aerialItem = null
            return@LaunchedEffect
        }

        if (!networkConnected) {
            aerialItem = null
            engine = engine.reduce(
                WallpaperEvent.SourceUnavailable(
                    candidate,
                    WallpaperUnavailableReason.NETWORK,
                )
            )
            return@LaunchedEffect
        }

        engine = engine.reduce(WallpaperEvent.SourceRecovered(candidate))
        when (val result = aerialProvider.next(candidate.sourceIds)) {
            is AerialProviderResult.Ready -> {
                aerialItem = result.item
                engine = engine.reduce(WallpaperEvent.SourceRecovered(candidate))
                delay(AERIAL_SAFETY_ROTATION_MS)
                engine = engine.reduce(WallpaperEvent.RefreshRequested(candidate))
            }

            is AerialProviderResult.RetryAfter -> {
                aerialItem = null
                engine = engine.reduce(
                    WallpaperEvent.SourceUnavailable(
                        candidate,
                        WallpaperUnavailableReason.QUARANTINED,
                    )
                )
                delay(result.delayMs.coerceAtLeast(1_000L))
                engine = engine.reduce(WallpaperEvent.SourceRecovered(candidate))
                engine = engine.reduce(WallpaperEvent.RefreshRequested(candidate))
            }

            AerialProviderResult.Empty -> {
                aerialItem = null
                engine = engine.reduce(
                    WallpaperEvent.SourceUnavailable(
                        candidate,
                        WallpaperUnavailableReason.MISSING_ASSET,
                    )
                )
            }
        }
    }

    // A concrete stream failure temporarily removes the Aerial source so the
    // engine can immediately render the next configured source. Retry later;
    // reconnecting the network restarts the delay with the shorter path.
    LaunchedEffect(aerialFailureEpoch, networkConnected, aerialCandidate) {
        if (aerialFailureEpoch == 0) return@LaunchedEffect
        val candidate = aerialCandidate ?: return@LaunchedEffect

        delay(if (networkConnected) AERIAL_CONNECTED_RETRY_MS else AERIAL_OFFLINE_RETRY_MS)

        // Another effect may already have recovered the source after a network
        // change. Re-check after the delay so we never rotate a healthy clip just
        // because an older retry coroutine was still alive.
        val reason = engine.unavailable[candidate]
        if (
            reason != WallpaperUnavailableReason.LOAD_FAILED &&
            reason != WallpaperUnavailableReason.NETWORK
        ) return@LaunchedEffect

        if (networkConnected) {
            engine = engine.reduce(WallpaperEvent.SourceRecovered(candidate))
            engine = engine.reduce(WallpaperEvent.RefreshRequested(candidate))
        } else {
            engine = engine.reduce(
                WallpaperEvent.SourceUnavailable(
                    candidate,
                    WallpaperUnavailableReason.NETWORK,
                )
            )
        }
    }

    val selected = engine.selection.candidate

    Box(modifier.fillMaxSize()) {
        when (selected) {
            is WallpaperCandidate.BuiltinAerials -> {
                val item = aerialItem
                if (item != null && networkConnected) {
                    VideoWallpaper(
                        uri = item.uri,
                        speed = videoSpeed,
                        loop = false,
                        coverBrush = presetBrush,
                        onEnded = {
                            // Keep the final frame visible until the next URI is
                            // ready, matching the old no-flash Aerial transition.
                            aerialProvider.recordSuccess(item.id)
                            engine = engine.reduce(WallpaperEvent.RefreshRequested(selected))
                        },
                        onError = {
                            aerialProvider.recordFailure(item.id)
                            aerialItem = null
                            engine = engine.reduce(
                                WallpaperEvent.SourceUnavailable(
                                    selected,
                                    WallpaperUnavailableReason.LOAD_FAILED,
                                )
                            )
                            aerialFailureEpoch++
                        },
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(presetBrush))
                }
            }

            is WallpaperCandidate.LocalVideo -> {
                VideoWallpaper(
                    uri = selected.uri,
                    speed = videoSpeed,
                    loop = true,
                    coverBrush = presetBrush,
                    onError = {
                        // A persistently bad local URI stays failed for this plan.
                        // Choosing/toggling the source removes/re-adds the candidate,
                        // which clears the unavailable state via PlanChanged.
                        engine = engine.reduce(
                            WallpaperEvent.SourceUnavailable(
                                selected,
                                WallpaperUnavailableReason.LOAD_FAILED,
                            )
                        )
                    },
                )
            }

            WallpaperCandidate.CustomPhoto -> {
                val image = photoAsset?.sharp
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(presetBrush))
                }
            }

            is WallpaperCandidate.Preset -> {
                Box(Modifier.fillMaxSize().background(presetBrush))
            }
        }

        // Preserve Couchy's cheap dock-expanded pseudo-blur: the 96px photo
        // texture is upscaled by the GPU, followed by the same dim layer.
        val overlayAlpha by animateFloatAsState(
            targetValue = if (dockExpanded) 1f else 0f,
            animationSpec = tween(1_000),
            label = "dockOverlay",
        )
        if (overlayAlpha > 0.01f) {
            photoAsset?.blurred?.let { blurred ->
                Image(
                    bitmap = blurred,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = overlayAlpha },
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = overlayAlpha }
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }
    }
}
