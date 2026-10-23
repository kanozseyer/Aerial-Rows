package com.conreo.couchytv.feature.wallpaper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Shared wallpaper HTTP client. Keep Android/OkHttp's normal certificate-chain
 * and hostname verification: decorative media is still untrusted network input
 * and must not weaken TLS for the video pipeline. One instance is reused for the
 * app lifetime because OkHttpDataSource does not own/close the client.
 */
private val wallpaperHttpClient: okhttp3.OkHttpClient by lazy {
    okhttp3.OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
}

/**
 * Shared live-video wallpaper renderer, muted and aspect-filled via ExoPlayer.
 * [loop] true replays the same clip (a user's own single video); false plays it
 * once then calls [onEnded] so Aerials can advance. The decoder is released while
 * the launcher is hidden and rebuilt on return.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoWallpaper(
    uri: String,
    speed: Float = 1f,
    loop: Boolean = true,
    coverBrush: Brush,
    onEnded: () -> Unit = {},
    onError: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // A clip is hidden until its first frame is actually rendered, then fades up.
    // On a source change the old clip fades out first, giving a symmetric fade.
    val alpha = remember { Animatable(0f) }
    var firstFrameGen by remember { mutableIntStateOf(0) }

    // Hold the scarce hardware decoder only while HOME is visible.
    var player by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }
    DisposableEffect(lifecycleOwner) {
        fun build(): androidx.media3.exoplayer.ExoPlayer {
            val renderers = androidx.media3.exoplayer.DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)
            val http = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(wallpaperHttpClient)
            val dataSource = androidx.media3.datasource.DefaultDataSource.Factory(context, http)
            val sourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSource)
            return androidx.media3.exoplayer.ExoPlayer.Builder(context)
                .setRenderersFactory(renderers)
                .setMediaSourceFactory(sourceFactory)
                // A muted, non-interactive wallpaper never seeks. Trim the default
                // buffer while retaining enough headroom for slower TV networking.
                .setLoadControl(
                    androidx.media3.exoplayer.DefaultLoadControl.Builder()
                        .setBufferDurationsMs(10_000, 15_000, 2_500, 5_000)
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .build()
                )
                .build().apply {
                    // Wallpaper video is always silent. Do not select/decode audio.
                    setTrackSelectionParameters(
                        trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                            .build()
                    )
                    repeatMode = if (loop) androidx.media3.common.Player.REPEAT_MODE_ONE
                    else androidx.media3.common.Player.REPEAT_MODE_OFF
                    volume = 0f
                    videoScalingMode =
                        androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    playWhenReady = true
                    setPlaybackSpeed(speed)
                }
        }

        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START ->
                    if (player == null) player = build()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    player?.let { runCatching { it.release() } }
                    player = null
                }
                else -> Unit
            }
        }

        if (
            player == null &&
            lifecycleOwner.lifecycle.currentState
                .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
        ) {
            player = build()
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.let { runCatching { it.release() } }
            player = null
        }
    }

    // Returning from another app starts covered, never as a black hole.
    LaunchedEffect(player == null) {
        if (player == null) alpha.snapTo(0f)
    }
    LaunchedEffect(player, speed) {
        player?.setPlaybackSpeed(speed)
    }

    DisposableEffect(player) {
        val p = player ?: return@DisposableEffect onDispose { }
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameGen++
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) onEnded()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.w(
                    "LiteTV",
                    "wallpaper play error ${error.errorCodeName}: $uri",
                )
                onError()
            }
        }
        p.addListener(listener)
        onDispose { p.removeListener(listener) }
    }

    // Keyed on player as well as URI so a fresh player re-prepares after HOME returns.
    LaunchedEffect(player, uri) {
        val p = player ?: return@LaunchedEffect
        if (alpha.value > 0f) alpha.animateTo(0f, tween(450))
        val genBefore = firstFrameGen
        runCatching {
            p.setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
            p.prepare()
        }

        // A dead/unreachable stream must not pin the cover indefinitely.
        delay(2_500L)
        val resumed = lifecycleOwner.lifecycle.currentState
            .isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
        if (firstFrameGen == genBefore && resumed) {
            android.util.Log.w(
                "LiteTV",
                "wallpaper stalled (no frame in 2.5s), skipping: $uri",
            )
            onError()
        }
    }

    LaunchedEffect(firstFrameGen) {
        if (firstFrameGen > 0) alpha.animateTo(1f, tween(1100))
    }

    // SurfaceView is composited by SurfaceFlinger rather than the Compose UI
    // thread, keeping wallpaper playback smooth during row/focus animation.
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> android.view.SurfaceView(ctx) },
            update = { view -> player?.setVideoSurfaceView(view) },
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = 1f - alpha.value }
                .background(coverBrush),
        )
    }
}
