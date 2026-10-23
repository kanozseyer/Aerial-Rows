package com.conreo.couchytv.data

import android.content.Context
import com.conreo.couchytv.R

/**
 * Canonical factory for a brand-new launcher configuration.
 *
 * Keep context/localization concerns here so every persistence path starts from
 * exactly the same defaults instead of falling back to LauncherConfig() with
 * hard-coded English category names.
 */
object ConfigDefaults {
    fun create(context: Context): LauncherConfig = LauncherConfig(
        categories = defaultCategories(context),
    )

    fun defaultCategories(context: Context): List<CategoryCfg> = listOf(
        CategoryCfg("streaming", context.getString(R.string.cat_streaming)),
        CategoryCfg("games", context.getString(R.string.cat_games)),
        CategoryCfg("music", context.getString(R.string.cat_music)),
        CategoryCfg("apps", context.getString(R.string.cat_apps)),
    )
}
