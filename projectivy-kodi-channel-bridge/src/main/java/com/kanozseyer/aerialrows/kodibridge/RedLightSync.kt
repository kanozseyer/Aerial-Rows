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

            classify(node.entry, node.trail)?.let { section ->
                discovered.putIfAbsent(section.semanticKey, section)
                continue
            }

            if (node.depth >= MAX_DEPTH || !shouldTraverse(node.entry, node.trail)) continue
            containerScans++
            val children = KodiClient.list(context, nativeUri)
            if (children == null) {
                // Kodi, Red Light or an authenticated provider was temporarily busy.
                // Do not age-out previously working channels on a partial scan.
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
        discovered.values.forEach { section ->
            seenKeys += section.semanticKey
            val channel = BridgePrefs.upsert(
                context = context,
                semanticKey = section.semanticKey,
                title = section.title,
                sourceUri = KodiClient.rawUri(context, section.entry.uri),
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

    /**
     * Resolve by route semantics first, then human label. Route matching survives
     * user menu moves/renames; label matching gives us a fallback when Red Light
     * changes an internal route name in a future release.
     */
    private fun classify(entry: KodiEntry, trail: List<String>): DiscoveredSection? {
        val uri = runCatching { Uri.parse(entry.uri) }.getOrNull()
        val mode = uri?.getQueryParameter("mode").orEmpty()
        val action = uri?.getQueryParameter("action").orEmpty()
        val sort = uri?.getQueryParameter("nextep_sort").orEmpty()
        val anime = uri?.getQueryParameter("is_anime_list").equals("true", true)
        val label = normalize(entry.name)

        fun section(key: String, defaultTitle: String = entry.name.trim()): DiscoveredSection =
            DiscoveredSection("redlight:$key", "Red Light · $defaultTitle", entry)

        // Red Light single-episode / progress shelves.
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

        // Red Light's movie/show progress shelves.
        when (action) {
            "recent_watched_movies" -> return section("recently_watched:movies", "Recently Watched Movies")
            "recent_watched_tvshows" -> if (!anime) return section("recently_watched:shows", "Recently Watched TV Shows")
            "in_progress_movies" -> return section("in_progress:movies", "Movies In Progress")
            "in_progress_tvshows" -> if (!anime) return section("in_progress:shows", "TV Shows In Progress")
        }

        // Signed-in Trakt shelves. Media type is inferred from the build route so
        // the identity remains stable even when the visible label is customized.
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

        // Every personal Trakt list gets its own stable Projectivy channel. Prefer
        // Trakt list_id, then user/slug, then a deterministic label/trail hash.
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

        // Label fallback for future Red Light releases that rename an internal
        // route while retaining the user's visible shelf name.
        return when {
            label == "movies watchlist" -> section("trakt:watchlist:movies", "Movies Watchlist")
            label == "tv shows watchlist" || label == "tv show watchlist" -> section("trakt:watchlist:shows", "TV Shows Watchlist")
            label.contains("recommended movies") || label == "movies recommended" -> section("trakt:recommended:movies", "Recommended Movies")
            label.contains("recommended tv") || label == "tv shows recommended" -> section("trakt:recommended:shows", "Recommended TV Shows")
            else -> null
        }
    }

    /**
     * Only walk Red Light containers that can lead to useful media shelves. This
     * keeps discovery cheap and avoids invoking Tools/Search/Services menus or
     * hammering remote APIs simply to learn the menu tree.
     */
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

        // This is Red Light's live list-of-Trakt-lists container. Traversing it
        // discovers lists the user creates later without any bridge update.
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
