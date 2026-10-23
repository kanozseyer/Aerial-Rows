package com.conreo.couchytv

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.conreo.couchytv.data.AppRepository
import com.conreo.couchytv.ui.LauncherApp
import java.util.Locale

class MainActivity : ComponentActivity() {

    /** Bumped after a relevant package change so app discovery refreshes once. */
    var rescanTick by mutableIntStateOf(0)
        private set

    private val packageRescanHandler = Handler(Looper.getMainLooper())
    private val packageRescanRunnable = Runnable { rescanTick++ }
    private val launcherApps: LauncherApps by lazy {
        @Suppress("DEPRECATION")
        getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    private val tvListingsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // TvContentRepository may have queried before permission was granted.
            // Recreate once so Watch Next / preview rows are queried immediately.
            recreate()
        }
    }

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            if (shouldRescan(packageName)) schedulePackageRescan()
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            // The package can no longer be queried, so always refresh to remove a
            // previously visible card and clear stale artwork on the next scan.
            schedulePackageRescan()
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            // Rescan when the package is launchable now OR was already present.
            // The second case matters when an update/disable makes an app stop
            // being launchable; checking only its current state would leave a
            // stale card behind indefinitely.
            if (shouldRescan(packageName)) schedulePackageRescan()
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) {
            if (packageNames.any(::shouldRescan)) schedulePackageRescan()
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) {
            val visible = AppRepository.memoryCache.orEmpty().asSequence().map { it.pkg }.toHashSet()
            if (packageNames.any(visible::contains)) schedulePackageRescan()
        }
    }

    private fun shouldRescan(packageName: String): Boolean {
        val alreadyVisible = AppRepository.memoryCache.orEmpty().any { it.pkg == packageName }
        if (alreadyVisible) return true
        return packageManager.getLeanbackLaunchIntentForPackage(packageName) != null ||
            packageManager.getLaunchIntentForPackage(packageName) != null
    }

    /** Coalesce install/update callback bursts into one expensive artwork scan. */
    private fun schedulePackageRescan() {
        packageRescanHandler.removeCallbacks(packageRescanRunnable)
        packageRescanHandler.postDelayed(packageRescanRunnable, PACKAGE_RESCAN_DEBOUNCE_MS)
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences(LOCALE_PREFS, MODE_PRIVATE)
            .getString(LOCALE_KEY, "").orEmpty()
        super.attachBaseContext(if (lang.isEmpty()) newBase else applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android's TvProvider exposes searchable channels / Watch Next entries
        // from other apps when READ_TV_LISTINGS is granted. Ask only once.
        maybeRequestTvListingsPermission()

        // A home screen never exits on Back.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op */ }
        })

        launcherApps.registerCallback(packageCallback)

        setContent {
            LauncherApp(rescanTick)
        }
    }

    private fun maybeRequestTvListingsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (checkSelfPermission(TV_LISTINGS_PERMISSION) == PackageManager.PERMISSION_GRANTED) return

        val prefs = getSharedPreferences(PERMISSION_PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(TV_LISTINGS_PROMPTED_KEY, false)) return
        prefs.edit().putBoolean(TV_LISTINGS_PROMPTED_KEY, true).apply()
        tvListingsPermissionLauncher.launch(TV_LISTINGS_PERMISSION)
    }

    override fun onDestroy() {
        packageRescanHandler.removeCallbacks(packageRescanRunnable)
        launcherApps.unregisterCallback(packageCallback)
        super.onDestroy()
    }

    companion object {
        private const val PACKAGE_RESCAN_DEBOUNCE_MS = 250L
        private const val LOCALE_PREFS = "locale"
        private const val LOCALE_KEY = "lang"
        private const val PERMISSION_PREFS = "permissions"
        private const val TV_LISTINGS_PROMPTED_KEY = "tv_listings_prompted"
        private const val TV_LISTINGS_PERMISSION = "android.permission.READ_TV_LISTINGS"

        /** Mirror the chosen language so attachBaseContext can read it synchronously. */
        fun persistLocale(context: Context, lang: String) {
            context.getSharedPreferences(LOCALE_PREFS, MODE_PRIVATE)
                .edit().putString(LOCALE_KEY, lang).apply()
        }

        fun currentLocalePref(context: Context): String =
            context.getSharedPreferences(LOCALE_PREFS, MODE_PRIVATE)
                .getString(LOCALE_KEY, "").orEmpty()

        fun applyLocale(context: Context, lang: String): Context {
            val locale = if (lang.contains('-')) {
                val parts = lang.split('-')
                Locale(parts[0], parts.getOrElse(1) { "" })
            } else Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }
    }
}
