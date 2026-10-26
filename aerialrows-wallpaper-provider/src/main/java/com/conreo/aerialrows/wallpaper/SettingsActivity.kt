package com.conreo.aerialrows.wallpaper

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperProviderContract

class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AerialPreferences.init(this)

        if (!packageManager.isApplicationInstalled(PROJECTIVY_PACKAGE_ID)) {
            Toast.makeText(this, R.string.projectivy_not_installed, Toast.LENGTH_LONG).show()
        }

        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, SettingsFragment(), android.R.id.content)
        }
    }

    fun requestWallpaperUpdate() {
        sendBroadcast(
            Intent(WallpaperProviderContract.ACTION_WALLPAPER_PROVIDER_UPDATED).apply {
                `package` = PROJECTIVY_PACKAGE_ID
                putExtra(WallpaperProviderContract.EXTRA_PROVIDER_ID, PROVIDER_UUID)
                putExtra(
                    WallpaperProviderContract.EXTRA_UPDATE_REASON,
                    WallpaperProviderContract.UpdateReason.PREFS_CHANGED,
                )
            },
        )
    }

    private fun PackageManager.isApplicationInstalled(packageName: String): Boolean =
        try {
            getApplicationInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    companion object {
        const val PROVIDER_UUID = "0d9fca82-7c69-4f26-b3cb-856602e90cbe"
        private const val PROJECTIVY_PACKAGE_ID = "com.spocky.projengmenu"
    }
}
