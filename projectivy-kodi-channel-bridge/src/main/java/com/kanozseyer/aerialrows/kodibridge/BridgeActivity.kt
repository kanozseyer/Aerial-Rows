package com.kanozseyer.aerialrows.kodibridge

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.kanozseyer.aerialrows.kodibridge.BridgePrefs.kodiPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BridgeActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RefreshScheduler.schedule(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { RedLightSync.discoverAndSync(applicationContext) }
        }
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, BridgeSettingsFragment(), android.R.id.content)
        }
    }
}

class BridgeSettingsFragment : GuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance = Guidance(
        "Red Light → Projectivy",
        "Mirrors Red Light and its Trakt-backed sections into Projectivy channels",
        "Kodi channel integration",
        null,
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        val installed = KodiClient.isInstalled(context)
        val channels = BridgePrefs.channels(context)
        actions += GuidedAction.Builder(context)
            .id(ACTION_STATUS)
            .title(if (installed) "Kodi detected" else "Kodi not detected")
            .description("Package: ${context.kodiPackage} · ${channels.size} Red Light channel(s)")
            .infoOnly(true)
            .build()
        actions += GuidedAction.Builder(context)
            .id(ACTION_OPEN_KODI)
            .title("Open Kodi")
            .description("Start Kodi if Red Light is not currently queryable")
            .build()
        actions += GuidedAction.Builder(context)
            .id(ACTION_BROWSE)
            .title("Browse Red Light sections")
            .description("Only Red Light is exposed; other video add-ons are ignored")
            .build()
        actions += GuidedAction.Builder(context)
            .id(ACTION_REFRESH)
            .title("Discover / refresh Red Light channels")
            .description("Auto-detects Red Light and expands Trakt/My Lists one level")
            .build()
        channels.forEachIndexed { index, channel ->
            actions += GuidedAction.Builder(context)
                .id(CHANNEL_BASE + index)
                .title(channel.title)
                .description(channel.sourceUri)
                .build()
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        val context = requireContext()
        when (action.id) {
            ACTION_OPEN_KODI -> KodiClient.open(context)
            ACTION_BROWSE -> startActivity(Intent(context, BrowserActivity::class.java))
            ACTION_REFRESH -> lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    val discovered = RedLightSync.discoverAndSync(context.applicationContext)
                    TvPublisher.refreshAll(context.applicationContext)
                    discovered
                }
                val message = when {
                    !result.found -> "Red Light not returned by Kodi. Open Kodi once, then retry."
                    result.discoveredSections == 0 -> "Red Light found, but no publishable sections were returned."
                    else -> "Red Light: ${result.syncedChannels}/${result.discoveredSections} channel(s) synced"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            else -> if (action.id >= CHANNEL_BASE) {
                val index = (action.id - CHANNEL_BASE).toInt()
                BridgePrefs.channels(context).getOrNull(index)?.let { channel ->
                    startActivity(Intent(context, ChannelActivity::class.java).putExtra(ChannelActivity.EXTRA_ID, channel.id))
                }
            }
        }
    }

    companion object {
        private const val ACTION_STATUS = 1L
        private const val ACTION_OPEN_KODI = 2L
        private const val ACTION_BROWSE = 3L
        private const val ACTION_REFRESH = 4L
        private const val CHANNEL_BASE = 1000L
    }
}
