package com.conreo.couchytv.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class CategoryCfg(
    val id: String,
    val name: String,
)

@Immutable
@Serializable
data class LauncherConfig(
    val categories: List<CategoryCfg> = listOf(
        CategoryCfg("streaming", "Streaming"),
        CategoryCfg("games", "Games"),
        CategoryCfg("music", "Music"),
        CategoryCfg("apps", "Apps"),
    ),
    /** package -> section ids the user assigned (an app may be in several) */
    val sections: Map<String, Set<String>> = emptyMap(),
    /** category id -> explicit package order */
    val order: Map<String, List<String>> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    /** Packages seen on the last scan, used to identify genuinely new installs. */
    val knownApps: Set<String> = emptySet(),
    /** Assignment behavior for apps first observed after the initial baseline scan. */
    val newInstallPolicy: NewInstallPolicy = NewInstallPolicy.FIRST_SECTION,
    val wallpaper: Int = 1, // Aurora
    val useCustomWallpaper: Boolean = false,
    /** video wallpaper: persisted content-URI of a local video, looped muted */
    val videoUri: String = "",
    val useVideoWallpaper: Boolean = false,
    /** built-in aerials source */
    val useBuiltinAerials: Boolean = false,
    /** Legacy single-source value kept only so older Couchy backups still decode. */
    val builtinSource: Int = 0,
    /** Aerial collections enabled together. A=Apple, Z=Amazon, C1/C2=community. */
    val builtinSources: Set<String> = setOf("A", "Z", "C1", "C2"),
    /** video wallpaper playback speed: index into VIDEO_SPEEDS */
    val videoSpeed: Int = 3, // 1x
    val accent: Int = 0,
    val h24: Boolean = true,
    val showHidden: Boolean = false,
    val setupDone: Boolean = false,

    // ----- Android TV content rows -----
    /** System Watch Next / Play Next content, when the device exposes it. */
    val showWatchNext: Boolean = true,
    /** Maximum Watch Next cards accepted from any one provider package. */
    val watchNextPerProviderLimit: Int = 4,
    /** Provider packages excluded from the Watch Next row. */
    val excludedWatchNextProviders: Set<String> = emptySet(),
    /** App-published Android TV preview channels. */
    val showPreviewChannels: Boolean = true,
    /** Maximum cards loaded from each TV channel row unless overridden per channel. */
    val channelItemLimit: Int = 12,
    /** Stable channel keys hidden by the user. */
    val hiddenTvChannels: Set<String> = emptySet(),
    /** Optional per-channel item limit, keyed by TvChannelRow.key. */
    val channelItemLimits: Map<String, Int> = emptyMap(),
    /** Programme identities hidden by the user, keyed by stable TV channel key. */
    val hiddenTvPrograms: Map<String, Set<String>> = emptyMap(),

    // ----- home row organization -----
    /**
     * Ordered row ids. Unknown/new rows are appended automatically, so provider
     * channels can appear/disappear without corrupting the saved order.
     */
    val homeRowOrder: List<String> = emptyList(),
    /** Gap between the top of the screen and the first Sliding Rows row, percent. */
    val rowsTopMarginPercent: Int = 6,

    // ----- status bar -----
    val showStatusBar: Boolean = true,
    /** package of the app the VPN icon opens; empty = system VPN settings */
    val vpnApp: String = "",
    val showVpnButton: Boolean = true,
    /** wrap the status-bar icons in the same glass panel as dock mode */
    val statusBarGlass: Boolean = true,
    /** index into DATE_FORMATS; 0 = no date shown */
    val dateFormat: Int = 0,

    // ----- display options -----
    /** wallpaper dimming: 0 = top & bottom, 1 = top, 2 = bottom, 3 = full, 4 = off */
    val scrimMode: Int = 1, // Top
    val showCategoryNames: Boolean = true,
    val showAppLabels: Boolean = true,
    /** spacing step 0..4 (see GAP_SIZES) */
    val spacing: Int = 1, // Small
    /** icon size step 0..4 (see ICON_SIZES) */
    val iconScale: Int = 1, // Small
    /** icon/panel corner roundness step 0..4 (see CORNER_RADII) */
    val cornerRadius: Int = 3, // Large
    /** UI scale: 0 = Auto (compact high-DPI TVs), 1..5 = fixed (see UI_SCALES) */
    val uiScale: Int = 0, // Auto
    /** Row + dock panel: clear, solid tint, or layered liquid-glass effect. */
    val panelStyle: Int = PANEL_CLEAR,
    /** Panel opacity step 0..10, representing 0%..100%. */
    val panelOpacity: Int = 5,
    /** 0 = neutral; 1..12 = 30-degree hue steps. */
    val panelTintHue: Int = 0,
    /** Tint saturation step 0..5. */
    val panelTintSaturation: Int = 2,
    /** Liquid-glass highlight/border strength step 0..5. */
    val panelEffectStrength: Int = 3,
    /** language code (e.g. "fr", "de"); empty = system default */
    val language: String = "",
    /** 0 = carousel, 1 = grid, 2 = dock, 3 = Projectivy-style sliding rows */
    val layout: Int = LAYOUT_SLIDING_ROWS,
)

const val LAYOUT_CAROUSEL = 0
const val LAYOUT_GRID = 1
const val LAYOUT_DOCK = 2
const val LAYOUT_SLIDING_ROWS = 3

const val PANEL_CLEAR = 0
const val PANEL_SOLID = 1
const val PANEL_LIQUID_GLASS = 2

val ALL_AERIAL_SOURCES = setOf("A", "Z", "C1", "C2")

/** Status bar date formats (SimpleDateFormat patterns); index 0 = off. */
val DATE_FORMATS = listOf("", "EEE d", "EEE d MMM", "d MMM yyyy", "dd/MM", "MM/dd", "yyyy-MM-dd")

/** Sorted by user base — the display name is localized in the UI via string resource. */
val LANGUAGES = listOf(
    "",     // system default
    "zh", "es", "ja", "de", "fr",   // tier 1
    "pt", "ru", "ko", "ar", "it",   // tier 2
    "tr", "pl", "nl", "hi", "th", "in", "vi", // tier 3
)

private val Context.dataStore by preferencesDataStore(name = "launcher")
private val KEY_CONFIG = stringPreferencesKey("config")
private val KEY_LAST_KNOWN_GOOD = stringPreferencesKey("config_last_known_good")
private val KEY_CORRUPT_CONFIG = stringPreferencesKey("config_corrupt_backup")

class ConfigStore(private val context: Context) {

    private val fallbackCategories: List<CategoryCfg>
        get() = ConfigDefaults.defaultCategories(context)

    private fun decode(raw: String?): LauncherConfig? = when (val result = raw?.let(ConfigCodec::decode)) {
        is ConfigDecodeResult.Success -> result.envelope.launcherConfig
        else -> null
    }

    private fun normalize(config: LauncherConfig): LauncherConfig =
        ConfigNormalizer.normalize(config, fallbackCategories)

    val flow: Flow<LauncherConfig> = context.dataStore.data.map { prefs ->
        val loaded = decode(prefs[KEY_CONFIG])
            ?: decode(prefs[KEY_LAST_KNOWN_GOOD])
            ?: ConfigDefaults.create(context)
        normalize(loaded)
    }

    suspend fun update(transform: (LauncherConfig) -> LauncherConfig) {
        context.dataStore.edit { prefs ->
            val rawCurrent = prefs[KEY_CONFIG]
            val currentResult = rawCurrent?.let(ConfigCodec::decode)

            if (currentResult is ConfigDecodeResult.UnsupportedSchema) {
                error(
                    "Stored config schema ${currentResult.schemaVersion} is newer than " +
                        "supported schema $CURRENT_CONFIG_SCHEMA_VERSION",
                )
            }

            // Never silently destroy a malformed payload. Preserve the first
            // damaged value for diagnostics/manual recovery before writing a
            // repaired snapshot back to the primary key.
            if (rawCurrent != null && currentResult is ConfigDecodeResult.Failure && prefs[KEY_CORRUPT_CONFIG] == null) {
                prefs[KEY_CORRUPT_CONFIG] = rawCurrent
            }

            val current = (currentResult as? ConfigDecodeResult.Success)?.envelope?.launcherConfig
                ?: decode(prefs[KEY_LAST_KNOWN_GOOD])
                ?: ConfigDefaults.create(context)

            val next = normalize(transform(normalize(current)))
            val encoded = ConfigCodec.encode(next)

            check(ConfigCodec.decode(encoded) is ConfigDecodeResult.Success) {
                "Refusing to persist unreadable launcher config"
            }

            prefs[KEY_CONFIG] = encoded
            prefs[KEY_LAST_KNOWN_GOOD] = encoded
        }
    }
}
