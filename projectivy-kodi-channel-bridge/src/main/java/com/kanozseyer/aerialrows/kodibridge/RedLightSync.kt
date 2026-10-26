package com.kanozseyer.aerialrows.kodibridge

import android.content.Context
import android.net.Uri
import java.security.MessageDigest
import java.util.ArrayDeque

data class RedLightSyncResult(
    val found: Boolean,
    val discoveredSections: Int,
    val syncedChannels: Int,
)

private data class MenuNode(
    val entry: KodiEntry,
    val trail: List<String>,
    val depth: Int,
)

private data class DiscoveredSection(
    val semanticKey: String,
    val title: String,
    val entry: KodiEntry,
)

/**
 * Runtime Red Light resolver.
 *
 * We intentionally do not persist hard-coded plugin:// paths. Each refresh starts
 * from Kodi's current Red Light add-on entry, walks only menu/container routes, and
 * resolves the current route for each semantic shelf. If Red Light moves a shelf,
 * rewrites a query string, or the user reorders the add-on menus, the existing
 * Android TV channel keeps its channelId and receives the newly resolved route.
 */
object RedLightSync {
    private const val MAX_DEPTH = 6
    private const val MAX_CONTAINER_SCANS = 48
    private const val MAX_CHILDREN_PER_CONTAINER = 80

    fun discoverAndSync(context: Context): RedLightSyncResult {
        val root = KodiClient.redLightRoot(context) ?: return RedLightSyncResult(false, 0, 0)
        val rootChildren = KodiClient.list(context, root.uri) ?: return RedLightSyncResult(true, 0, 0)

        val discovered = linkedMapOf<String, DiscoveredSection>()
        val queue = ArrayDeque<MenuNode>()
        rootChildren.asSequence()
            .filter { it.isDirectory }
            .take(MAX_CHILDREN_PER_CONTAINER)
            .forEach { queue.add(MenuNode(it, listOf(it.name.trim()), 1)) }

        val visited = mutableSetOf<String>()
        var containerScans = 0
        var completeDiscovery = true

        while (queue.isNotEmpty() && containerScans < MAX_CONTAINER_SCANS) {
            val node = queue.removeFirst()
            val nativeUri = KodiClient.rawUri(context, node.entry.uri)
            if (!visited.add(nativeUri)) continue

            val section = classify(node.entry, node.trail)
            if (section != null) {
                discovered.putIfAbsent(section.semanticKey, section)
                continue
            }

            if (node.depth >= MAX_DEPTH || !shouldTraverse(node.entry, node.trail)) continue
            containerScans++
            val children = KodiClient.list(context, nativeUri)
            if (children == null) {
                completeDiscovery = false
                continue
            }

            children.asSequence()
                .filter { it.isDirectory }
                .take(MAX_CHILDREN_PER_CONTAINER)
                .forEach { child ->
                    queue.add(MenuNode(child, node.trail + child.name.trim(), node.depth + 1))
                }
        }

        if (queue.isNotEmpty()) completeDiscovery = false

        var synced = 0
        val seenKeys = mutableSetOf<String>()
        discovered.values.forEach { item ->
            seenKeys += item.semanticKey
            val channel = BridgePrefs.upsert(
                context = context,
                semanticKey = item.semanticKey,
                title = item.title,
                sourceUri = KodiClient.rawUri(context, item.entry.uri),
            )
            if (TvPublisher.sync(context, channel)) synced++
        }

        if (completeDiscovery) {
            BridgePrefs.markMissingManaged(context, seenKeys).forEach { stale ->
                TvPublisher.delete(context, stale)
            }
        }

        return RedLightSyncResult(true, discovered.size, synced)
    }

    private fun classify(entry: KodiEntry, trail: List<String>): DiscoveredSection? {
        val uri = runCatching { Uri.parse(entry.uri) }.getOrNull()
        val mode = uri?.getQueryParameter("mode").orEmpty()
        val action = uri?.getQueryParameter("action").orEmpty()
        val sort = uri?.getQueryParameter("nextep_sort").orEmpty()
        val anime = uri?.getQueryParameter("is_anime_list").equals("true", true)
        val label = normalize(entry.name)

        fun section(key: String, defaultTitle: String = entry.name.trim()): DiscoveredSection =
            DiscoveredSection("redlight:$key", "Red Light · $defaultTitle", entry)

        if (mode == "build_continue_watching" || label == "continue watching")
            return section("continue_watching", "Continue Watching")
        if (mode == "build_next_episode" || label.startsWith("next episodes") || label.startsWith("anime next episodes")) {
            val suffix = when {
                anime -> "anime"
                sort == "last_played" -> "recently_watched"
                sort == "first_aired" -> "airdate"
                else -> "default"
            }
            return section("next_episodes:$suffix")
        }
        if (mode == "build_in_progress_episode" || label.contains("in progress episodes"))
            return section(if (anime) "in_progress_episodes:anime" else "in_progress_episodes")
        if (mode == "build_recently_watched_episode" || label.contains("recently watched episodes"))
            return section(if (anime) "recently_watched_episodes:anime" else "recently_watched_episodes")

        when (action) {
            "recent_watched_movies" -> return section("recently_watched:movies", "Recently Watched Movies")
            "recent_watched_tvshows" -> if (!anime) return section("recently_watched:shows", "Recently Watched TV Shows")
            "in_progress_movies" -> return section("in_progress:movies", "Movies In Progress")
            "in_progress_tvshows" -> if (!anime) return section("in_progress:shows", "TV Shows In Progress")
        }

        val media = when (mode) {
            "build_movie_list" -> "movies"
            "build_tvshow_list" -> if (anime) "anime" else "shows"
            else -> ""
        }
        when (action) {
            "trakt_watchlist" -> if (media.isNotBlank()) return section("trakt:watchlist:$media")
            "trakt_watchlist_lists" -> if (media.isNotBlank()) return section("trakt:watchlist_recent:$media")
            "trakt_recommendations" -> if (media.isNotBlank()) return section("trakt:recommended:$media")
            "trakt_collection" -> if (media.isNotBlank()) return section("trakt:library:$media")
            "trakt_collection_lists" -> if (media.isNotBlank()) return section("trakt:library_recent:$media")
            "trakt_favorites" -> if (media.isNotBlank()) return section("trakt:favorites:$media")
        }
        if (mode == "build_my_calendar" || label == "calendar")
            return section("trakt:calendar", "Trakt Calendar")

        if (mode == "trakt.list.build_trakt_list") {
            val listId = uri?.getQueryParameter("list_id").orEmpty()
            val user = uri?.getQueryParameter("user").orEmpty()
            val slug = uri?.getQueryParameter("slug").orEmpty()
            val identity = when {
                listId.isNotBlank() -> "id:$listId"
                user.isNotBlank() || slug.isNotBlank() -> "slug:${normalize(user)}:${normalize(slug)}"
                else -> "menu:${shortHash(trail.joinToString("/"))}"
            }
            return section("trakt:list:$identity")
        }

        return when {
            label == "movies watchlist" -> section("trakt:watchlist:movies", "Movies Watchlist")
            label == "tv shows watchlist" || label == "tv show watchlist" -> section("trakt:watchlist:shows", "TV Shows Watchlist")
            label.contains("recommended movies") || label == "movies recommended" -> section("trakt:recommended:movies", "Recommended Movies")
            label.contains("recommended tv") || label == "tv shows recommended" -> section("trakt:recommended:shows", "Recommended TV Shows")
            else -> null
        }
    }

    private fun shouldTraverse(entry: KodiEntry, trail: List<String>): Boolean {
        val uri = runCatching { Uri.parse(entry.uri) }.getOrNull()
        val mode = uri?.getQueryParameter("mode").orEmpty()
        val action = uri?.getQueryParameter("action").orEmpty()
        val label = normalize(entry.name)

        if (label in setOf("tools", "search", "people", "downloads", "my services", "discover", "random lists")) return false

        if (mode == "navigator.main") {
            return action in setOf("RootList", "MovieList", "TVShowList", "AnimeList") || trail.size <= 2
        }
        if (mode in setOf(
                "navigator.my_lists",
                "navigator.trakt_lists_personal",
                "navigator.trakt_watchlists",
                "navigator.trakt_collections",
                "navigator.trakt_favorites",
                "navigator.trakt_recommendations",
                "navigator.favorites",
            )
        ) return true

        if (mode == "trakt.list.get_trakt_lists" &&
            uri?.getQueryParameter("list_type") == "my_lists"
        ) return true

        return false
    }

    private fun normalize(value: String): String = value
        .lowercase()
        .replace(Regex("\\[[^]]*]"), "")
        .replace('&', ' ')
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun shortHash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}
