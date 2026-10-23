package com.conreo.couchytv.data

import android.content.Context
import android.database.ContentObserver
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Immutable
data class TvProgramItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val artworkUri: String,
    val intentUri: String,
    val packageName: String,
    val durationMs: Long,
    val playbackPositionMs: Long,
)

@Immutable
data class TvChannelRow(
    val id: Long,
    val key: String,
    val name: String,
    val packageName: String,
    val programs: List<TvProgramItem>,
)

@Immutable
data class TvContentRows(
    val watchNext: List<TvProgramItem> = emptyList(),
    val channels: List<TvChannelRow> = emptyList(),
)

enum class TvContentObservation {
    WATCH_NEXT,
    CHANNELS,
    PREVIEW_PROGRAMS,
}

/** Pure demand-driven observation plan; useful for regression testing without Android provider state. */
fun tvContentObservationTargets(
    showWatchNext: Boolean,
    showPreviewChannels: Boolean,
): Set<TvContentObservation> = buildSet {
    if (showWatchNext) add(TvContentObservation.WATCH_NEXT)
    if (showPreviewChannels) {
        add(TvContentObservation.CHANNELS)
        add(TvContentObservation.PREVIEW_PROGRAMS)
    }
}

/** Reads and observes Android TV's system Watch Next row and app-published preview channels. */
object TvContentRepository {

    /**
     * Emits an initial snapshot and then only reloads when one of the enabled TV
     * provider surfaces changes. Disabled features register no observer at all.
     */
    fun observe(context: Context, config: LauncherConfig): Flow<TvContentRows> {
        if (Build.VERSION.SDK_INT < 26) return flowOf(TvContentRows())

        val targets = tvContentObservationTargets(
            showWatchNext = config.showWatchNext,
            showPreviewChannels = config.showPreviewChannels,
        )
        if (targets.isEmpty()) return flowOf(TvContentRows())

        val appContext = context.applicationContext
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    trySend(Unit)
                }
            }

            fun register(uri: Uri) {
                runCatching {
                    appContext.contentResolver.registerContentObserver(
                        uri,
                        true,
                        observer,
                    )
                }
            }

            if (TvContentObservation.WATCH_NEXT in targets) {
                register(TvContract.WatchNextPrograms.CONTENT_URI)
            }
            if (TvContentObservation.CHANNELS in targets) {
                register(TvContract.Channels.CONTENT_URI)
            }
            if (TvContentObservation.PREVIEW_PROGRAMS in targets) {
                register(TvContract.PreviewPrograms.CONTENT_URI)
            }

            // Initial snapshot: the same path handles startup and later invalidations.
            trySend(Unit)

            awaitClose {
                runCatching { appContext.contentResolver.unregisterContentObserver(observer) }
            }
        }
            // Provider writes often arrive in small bursts; keep only the latest
            // invalidation while a query is already running.
            .conflate()
            .map { load(appContext, config) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    suspend fun load(context: Context, config: LauncherConfig): TvContentRows = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 26) return@withContext TvContentRows()

        val defaultLimit = config.channelItemLimit.coerceIn(4, 30)
        val watchNext = if (config.showWatchNext) {
            runCatching {
                queryPrograms(context, TvContract.WatchNextPrograms.CONTENT_URI, defaultLimit)
            }.getOrDefault(emptyList())
        } else emptyList()

        val channels = if (config.showPreviewChannels) {
            runCatching {
                queryChannels(
                    context = context,
                    defaultLimit = defaultLimit,
                    hidden = config.hiddenTvChannels,
                    perChannelLimits = config.channelItemLimits,
                )
            }.getOrDefault(emptyList())
        } else emptyList()

        TvContentRows(watchNext = watchNext, channels = channels)
    }

    /**
     * Settings uses this to show every readable channel, including channels the
     * user has hidden from the home screen. Only a few programs are loaded for
     * the preview so this remains cheap.
     */
    suspend fun discoverChannels(context: Context): List<TvChannelRow> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 26) return@withContext emptyList()
        runCatching {
            queryChannels(
                context = context,
                defaultLimit = 4,
                hidden = emptySet(),
                perChannelLimits = emptyMap(),
            )
        }.getOrDefault(emptyList())
    }

    fun channelKey(packageName: String, channelId: Long): String =
        "${packageName.ifBlank { "unknown" }}:$channelId"

    @RequiresApi(26)
    private fun queryChannels(
        context: Context,
        defaultLimit: Int,
        hidden: Set<String>,
        perChannelLimits: Map<String, Int>,
    ): List<TvChannelRow> {
        val projection = arrayOf(
            BaseColumns._ID,
            TvContract.Channels.COLUMN_DISPLAY_NAME,
            TvContract.Channels.COLUMN_BROWSABLE,
            TvContract.Channels.COLUMN_TYPE,
            "package_name",
        )
        val rows = mutableListOf<TvChannelRow>()
        context.contentResolver.query(TvContract.Channels.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndex(BaseColumns._ID)
            val nameCol = cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)
            val browsableCol = cursor.getColumnIndex(TvContract.Channels.COLUMN_BROWSABLE)
            val typeCol = cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE)
            val pkgCol = cursor.getColumnIndex("package_name")
            while (cursor.moveToNext()) {
                val id = if (idCol >= 0) cursor.getLong(idCol) else continue
                val type = if (typeCol >= 0) cursor.getString(typeCol).orEmpty() else ""
                if (type != TvContract.Channels.TYPE_PREVIEW) continue
                if (browsableCol >= 0 && cursor.getInt(browsableCol) == 0) continue

                val name = if (nameCol >= 0) {
                    cursor.getString(nameCol).orEmpty().ifBlank { "Channel" }
                } else "Channel"
                val packageName = if (pkgCol >= 0) cursor.getString(pkgCol).orEmpty() else ""
                val key = channelKey(packageName, id)
                if (key in hidden) continue

                val limit = (perChannelLimits[key] ?: defaultLimit).coerceIn(4, 30)
                val programs = runCatching {
                    queryPrograms(context, TvContract.buildPreviewProgramsUriForChannel(id), limit)
                }.getOrDefault(emptyList())
                if (programs.isEmpty()) continue

                rows += TvChannelRow(
                    id = id,
                    key = key,
                    name = name,
                    packageName = packageName,
                    programs = programs,
                )
            }
        }
        return rows
    }

    @RequiresApi(26)
    private fun queryPrograms(context: Context, uri: Uri, limit: Int): List<TvProgramItem> {
        // Shared preview/watch-next column names. Using literal provider names
        // avoids Kotlin/JVM inherited-static binding differences across SDKs.
        val projection = arrayOf(
            BaseColumns._ID,
            "title",
            "episode_title",
            "short_description",
            "poster_art_uri",
            "thumbnail_uri",
            "intent_uri",
            "package_name",
            "duration_millis",
            "last_playback_position_millis",
            "browsable",
        )
        val result = ArrayList<TvProgramItem>(limit)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            fun text(name: String): String {
                val i = cursor.getColumnIndex(name)
                return if (i >= 0 && !cursor.isNull(i)) cursor.getString(i).orEmpty() else ""
            }
            fun long(name: String): Long {
                val i = cursor.getColumnIndex(name)
                return if (i >= 0 && !cursor.isNull(i)) cursor.getLong(i) else 0L
            }
            val idCol = cursor.getColumnIndex(BaseColumns._ID)
            val browsableCol = cursor.getColumnIndex("browsable")
            while (cursor.moveToNext() && result.size < limit) {
                if (browsableCol >= 0 && cursor.getInt(browsableCol) == 0) continue
                val title = text("title")
                if (title.isBlank()) continue
                result += TvProgramItem(
                    id = if (idCol >= 0) cursor.getLong(idCol) else result.size.toLong(),
                    title = title,
                    subtitle = text("episode_title").ifBlank { text("short_description") },
                    artworkUri = text("poster_art_uri").ifBlank { text("thumbnail_uri") },
                    intentUri = text("intent_uri"),
                    packageName = text("package_name"),
                    durationMs = long("duration_millis"),
                    playbackPositionMs = long("last_playback_position_millis"),
                )
            }
        }
        return result
    }
}
