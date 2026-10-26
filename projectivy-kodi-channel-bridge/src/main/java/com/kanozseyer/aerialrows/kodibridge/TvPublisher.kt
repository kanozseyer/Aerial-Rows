package com.kanozseyer.aerialrows.kodibridge

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat

@SuppressLint("RestrictedApi")
object TvPublisher {
    private const val MAX_PROGRAMS = 60

    fun refreshAll(context: Context): Pair<Int, Int> {
        var ok = 0
        val all = BridgePrefs.channels(context)
        all.forEach { if (sync(context, it)) ok++ }
        return ok to all.size
    }

    fun sync(context: Context, original: BridgeChannel): Boolean {
        val channel = ensureChannel(context, original) ?: return false
        val entries = KodiClient.list(context, channel.sourceUri) ?: return false

        val resolver = context.contentResolver
        runCatching {
            resolver.delete(TvContractCompat.buildPreviewProgramsUriForChannel(channel.channelId), null, null)
        }

        // A media shelf can occasionally contain navigation folders. Projectivy
        // should only receive actual content cards; Red Light handles the next
        // navigation/playback step from the card's native plugin:// URI.
        entries.asSequence()
            .filterNot { it.isDirectory }
            .take(MAX_PROGRAMS)
            .forEachIndexed { index, item ->
                val program = buildProgram(context, channel.channelId, item, index)
                runCatching {
                    resolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, program.toContentValues())
                }
            }
        return true
    }

    fun delete(context: Context, channel: BridgeChannel) {
        if (channel.channelId > 0L) {
            runCatching {
                context.contentResolver.delete(
                    TvContractCompat.buildPreviewProgramsUriForChannel(channel.channelId), null, null,
                )
            }
            runCatching {
                context.contentResolver.delete(TvContractCompat.buildChannelUri(channel.channelId), null, null)
            }
        }
        BridgePrefs.remove(context, channel.id)
    }

    private fun ensureChannel(context: Context, original: BridgeChannel): BridgeChannel? {
        // The channel app-link simply opens Kodi. Individual PreviewPrograms carry
        // the exact current Red Light plugin:// route via ACTION_VIEW.
        val channelValues = Channel.Builder()
            .setType(TvContractCompat.Channels.TYPE_PREVIEW)
            .setDisplayName(original.title)
            .setAppLinkIntent(KodiClient.intent(context))
            .setInternalProviderId(original.id)
            .build()
            .toContentValues()

        if (original.channelId > 0L && channelExists(context, original.channelId)) {
            // Update the visible title/app-link in place. Keeping channelId stable
            // preserves Projectivy/Android TV channel choices when Red Light moves
            // or renames the backing route.
            runCatching {
                context.contentResolver.update(
                    TvContractCompat.buildChannelUri(original.channelId),
                    channelValues,
                    null,
                    null,
                )
            }
            return original
        }

        val uri = runCatching {
            context.contentResolver.insert(TvContractCompat.Channels.CONTENT_URI, channelValues)
        }.getOrNull() ?: return null

        val withId = original.copy(channelId = ContentUris.parseId(uri))
        BridgePrefs.update(context, withId)
        runCatching { TvContractCompat.requestChannelBrowsable(context, withId.channelId) }
        return withId
    }

    private fun channelExists(context: Context, id: Long): Boolean = runCatching {
        context.contentResolver.query(TvContractCompat.buildChannelUri(id), arrayOf("_id"), null, null, null)
            ?.use { it.moveToFirst() } == true
    }.getOrDefault(false)

    private fun buildProgram(context: Context, channelId: Long, item: KodiEntry, index: Int): PreviewProgram {
        val type = when (item.mediaType?.lowercase()) {
            "movie" -> TvContractCompat.PreviewProgramColumns.TYPE_MOVIE
            "tvshow" -> TvContractCompat.PreviewProgramColumns.TYPE_TV_SERIES
            "episode" -> TvContractCompat.PreviewProgramColumns.TYPE_TV_EPISODE
            "album" -> TvContractCompat.PreviewProgramColumns.TYPE_ALBUM
            "song" -> TvContractCompat.PreviewProgramColumns.TYPE_TRACK
            "musicvideo" -> TvContractCompat.PreviewProgramColumns.TYPE_CLIP
            else -> TvContractCompat.PreviewProgramColumns.TYPE_CLIP
        }
        val poster = Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_kodi_bridge}")
        val nativeUri = KodiClient.rawUri(context, item.uri)
        return PreviewProgram.Builder()
            .setChannelId(channelId)
            .setTitle(item.name)
            .setDescription(item.mediaType ?: item.category ?: "Red Light")
            .setType(type)
            .setIntent(KodiClient.intent(context, nativeUri))
            .setPosterArtUri(poster)
            .setPosterArtAspectRatio(TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_16_9)
            .setInternalProviderId(nativeUri)
            .setWeight(MAX_PROGRAMS - index)
            .build()
    }
}
