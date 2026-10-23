package com.conreo.couchytv.core.settings

/**
 * Semantic focus identities for settings rows whose membership is discovered at runtime.
 * Values are treated as opaque keys; labels and list positions never participate in identity.
 */
object AppSettingsIds {
    fun app(packageName: String, componentName: String?): SettingId = SettingId(
        buildString {
            append("apps.app:")
            append(packageName)
            append(':')
            append(componentName.orEmpty())
        },
    )
}

object ChannelSettingsIds {
    val WatchNext = SettingId("channels.watch_next")
    val WatchNextLimit = SettingId("channels.watch_next_limit")
    val PreviewChannels = SettingId("channels.preview_channels")
    val DefaultChannelLimit = SettingId("channels.default_limit")

    fun watchNextProvider(packageName: String): SettingId =
        SettingId("channels.watch_next_provider:$packageName")

    fun channel(channelKey: String): SettingId =
        SettingId("channels.channel:$channelKey")

    fun channelLimit(channelKey: String): SettingId =
        SettingId("channels.channel_limit:$channelKey")
}

object CategorySettingsIds {
    val AllApps = SettingId("categories.all_apps")
    val AddCategory = SettingId("categories.add")

    fun category(categoryId: String): SettingId =
        SettingId("categories.category:$categoryId")
}
