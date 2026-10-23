package com.conreo.couchytv.feature.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Sharp wallpaper texture plus the tiny upscaled pseudo-blur texture used by dock overlay. */
data class PhotoWallpaperAsset(
    val sharp: ImageBitmap,
    val blurred: ImageBitmap?,
)

/**
 * App-private custom-photo provider. Decoding stays off the main thread and keeps
 * Couchy's proven low-memory choices: RGB_565, ~1280px sharp texture and ~96px
 * pseudo-blur texture, with prepareToDraw() before either reaches composition.
 */
class LocalPhotoProvider(private val file: File) {
    suspend fun load(): PhotoWallpaperAsset? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        runCatching {
            val sharpBitmap = decodeDownsampled(file, maxWidth = 1280)
                ?: return@runCatching null
            sharpBitmap.prepareToDraw()

            val blurredBitmap = decodeDownsampled(file, maxWidth = 96)
            blurredBitmap?.prepareToDraw()

            PhotoWallpaperAsset(
                sharp = sharpBitmap.asImageBitmap(),
                blurred = blurredBitmap?.asImageBitmap(),
            )
        }.getOrNull()
    }
}

fun createLocalPhotoProvider(context: Context): LocalPhotoProvider =
    LocalPhotoProvider(File(context.applicationContext.filesDir, "wallpaper.jpg"))

/** Power-of-two inSampleSize matching the existing launcher decode behavior. */
internal fun imageSampleSize(sourceWidth: Int, maxWidth: Int): Int {
    require(maxWidth > 0)
    if (sourceWidth <= 0) return 1
    var sample = 1
    while (sourceWidth / (sample * 2) >= maxWidth) sample *= 2
    return sample
}

private fun decodeDownsampled(file: File, maxWidth: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0) return null

    val opts = BitmapFactory.Options().apply {
        inSampleSize = imageSampleSize(bounds.outWidth, maxWidth)
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeFile(file.absolutePath, opts)
}
