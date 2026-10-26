from pathlib import Path

path = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = path.read_text()

call_anchor = '''        String route = norm(mode + " " + action + " " + q(u,"type") + " " + q(u,"list_type"));\n\n        if ("build_continue_watching".equals(mode) || "continue watching".equals(label))'''
call_replacement = '''        String route = norm(mode + " " + action + " " + q(u,"type") + " " + q(u,"list_type"));\n\n        // Red Light 2.9.x exposes its primary Movies / TV Shows shelves directly\n        // beneath navigator.main MovieList and TVShowList.  Preserve each exact\n        // route as its own channel instead of folding e.g. Trending Recent into\n        // Trending or Popular Today into Popular.\n        Section directMenuShelf = classifyDirectMenuShelf(e, trail, u, mode, action, anime);\n        if (directMenuShelf != null) return directMenuShelf;\n\n        if ("build_continue_watching".equals(mode) || "continue watching".equals(label))'''

if 'classifyDirectMenuShelf(e, trail, u, mode, action, anime)' not in text:
    if text.count(call_anchor) != 1:
        raise SystemExit(f'direct shelf call anchor mismatch: {text.count(call_anchor)}')
    text = text.replace(call_anchor, call_replacement, 1)

helper_anchor = '''    private static String inferMedia(Entry e, List<String> trail, Uri u, String mode, boolean anime) {'''
helper = r'''    private static Section classifyDirectMenuShelf(Entry e, List<String> trail, Uri u, String mode, String action, boolean anime) {
        if (trail == null || trail.size() != 2) return null;
        String parent = norm(trail.get(0));
        String media;
        if ("movies".equals(parent)) media = "movies";
        else if ("tv shows".equals(parent)) media = "shows";
        else if ("anime".equals(parent)) media = "anime";
        else return null;

        boolean directContent = "build_movie_list".equals(mode)
                || "build_tvshow_list".equals(mode)
                || "build_recently_watched_episode".equals(mode)
                || "build_in_progress_episode".equals(mode)
                || "build_next_episode".equals(mode)
                || "build_continue_watching".equals(mode);
        if (!directContent) return null;
        if ("movies".equals(media) && !"build_movie_list".equals(mode)) return null;
        if (!"movies".equals(media) && "build_movie_list".equals(mode)) return null;

        String identity = action == null || action.isEmpty() ? mode : action;
        String sort = q(u, "nextep_sort");
        if (!sort.isEmpty()) identity += ":" + norm(sort).replace(' ', '_');
        if (anime || "anime".equals(media)) identity += ":anime";

        String title = directShelfTitle(e.name, media, mode, action);
        return sec("menu:" + media + ":" + identity, title, e);
    }

    private static String directShelfTitle(String original, String media, String mode, String action) {
        String clean = original == null ? "" : original.replaceAll("\\[[^]]*]", "").trim();
        if (clean.isEmpty()) clean = "Red Light";

        // Keep Red Light's episode shelf names exactly as shown in Kodi.
        if ("build_recently_watched_episode".equals(mode)
                || "build_in_progress_episode".equals(mode)
                || "build_next_episode".equals(mode)
                || "build_continue_watching".equals(mode)) return clean;

        // Preserve the familiar wording already used by the launcher for these.
        if ("in_progress_movies".equals(action)) return "Movies In Progress";
        if ("in_progress_tvshows".equals(action)) return "TV Shows In Progress";
        if ("recent_watched_movies".equals(action)) return "Recently Watched Movies";
        if ("recent_watched_tvshows".equals(action)) return "Recently Watched TV Shows";

        String n = norm(clean);
        if ("movies".equals(media)) {
            if (!n.contains("movie")) return clean + " Movies";
        } else if ("shows".equals(media)) {
            if (!n.contains("tv show") && !n.contains("series")) return clean + " TV Shows";
        } else if ("anime".equals(media)) {
            if (!n.contains("anime")) return "Anime " + clean;
        }
        return clean;
    }

'''

if 'private static Section classifyDirectMenuShelf(' not in text:
    if text.count(helper_anchor) != 1:
        raise SystemExit(f'direct shelf helper anchor mismatch: {text.count(helper_anchor)}')
    text = text.replace(helper_anchor, helper + helper_anchor, 1)

path.write_text(text)
print('patched Red Light runtime for exact Movies/TV Shows shelves')
