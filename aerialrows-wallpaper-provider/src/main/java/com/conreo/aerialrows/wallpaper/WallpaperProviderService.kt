package com.conreo.aerialrows.wallpaper

import android.app.Service
import android.content.Intent
import android.os.IBinder
import tv.projectivy.plugin.wallpaperprovider.api.Event
import tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperDisplayMode
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperType

class WallpaperProviderService : Service() {
    override fun onCreate() {
        super.onCreate()
        AerialPreferences.init(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private val binder = object : IWallpaperProviderService.Stub() {
        override fun getWallpapers(event: Event?): List<Wallpaper> =
            AerialCatalogue.loadSelected(this@WallpaperProviderService, AerialPreferences.selectedSources)
                .map { item ->
                    Wallpaper(
                        uri = item.uri,
                        type = WallpaperType.VIDEO,
                        displayMode = WallpaperDisplayMode.DEFAULT,
                        title = item.title,
                        source = AerialCatalogue.sourceLabel(item.sourceId),
                        author = null,
                    )
                }

        override fun getPreferences(): String = AerialPreferences.export()

        override fun setPreferences(params: String?) {
            if (!params.isNullOrBlank()) {
                runCatching { AerialPreferences.import(params) }
            }
        }
    }
}
