package com.kanozseyer.aerialrows.projectivy

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.runBlocking
import tv.projectivy.plugin.wallpaperprovider.api.Event
import tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper

class WallpaperProviderService : Service() {
    private lateinit var catalogue: AerialRowsCatalogue

    override fun onCreate() {
        super.onCreate()
        PreferencesManager.init(this)
        catalogue = AerialRowsCatalogue(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private val binder = object : IWallpaperProviderService.Stub() {
        override fun getWallpapers(event: Event?): List<Wallpaper> = runBlocking {
            catalogue.load()
        }

        override fun getPreferences(): String = PreferencesManager.export()

        override fun setPreferences(params: String) {
            PreferencesManager.import(params)
        }
    }
}
