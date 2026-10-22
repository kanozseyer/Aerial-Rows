package com.conreo.couchytv.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.conreo.couchytv.data.TvProgramItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val tvArtworkCache = object : LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(18) {}

@Composable
fun TvProgramCard(
    item: TvProgramItem,
    cardWidth: Dp,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val art by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = if (item.artworkUri.isBlank()) null else tvArtworkCache.get(item.artworkUri),
        item.artworkUri,
    ) {
        if (item.artworkUri.isBlank() || value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching { loadArtwork(context, item.artworkUri) }.getOrNull()
        }
        value?.let { tvArtworkCache.put(item.artworkUri, it) }
    }

    Surface(
        onClick = onClick,
        modifier = modifier.width(cardWidth),
        shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(LocalCornerRadius.current)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF20242A),
            focusedContainerColor = Color(0xFF20242A),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevationColor = accent.copy(alpha = 0.75f), elevation = 10.dp)
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(cardWidth * 9f / 16f)
        ) {
            if (art != null) {
                Image(
                    bitmap = art!!,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF303640), Color(0xFF171A1F))
                            )
                        )
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
            )

            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            )

            if (item.durationMs > 0L && item.playbackPositionMs > 0L) {
                val progress = (item.playbackPositionMs.toFloat() / item.durationMs.toFloat())
                    .coerceIn(0f, 1f)
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(accent)
                    )
                }
            }
        }
    }
}

private fun loadArtwork(context: android.content.Context, raw: String): androidx.compose.ui.graphics.ImageBitmap? {
    val uri = Uri.parse(raw)
    val stream = when (uri.scheme?.lowercase()) {
        "http", "https" -> {
            val connection = URL(raw).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 7000
            connection.instanceFollowRedirects = true
            connection.inputStream
        }
        else -> context.contentResolver.openInputStream(uri)
    } ?: return null

    val source = stream.use { BitmapFactory.decodeStream(it) } ?: return null
    val maxW = 480
    val maxH = 270
    val scale = minOf(maxW.toFloat() / source.width, maxH.toFloat() / source.height, 1f)
    val targetW = (source.width * scale).toInt().coerceAtLeast(1)
    val targetH = (source.height * scale).toInt().coerceAtLeast(1)
    val scaled: Bitmap = if (targetW != source.width || targetH != source.height) {
        Bitmap.createScaledBitmap(source, targetW, targetH, true).also { source.recycle() }
    } else source
    return scaled.asImageBitmap()
}