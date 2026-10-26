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
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, BridgeSettingsFragment(), android.R.id.content)
        }
    }
}

class BridgeSettingsFragment : GuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance = Guidance(
        getString(R.string.app_short_name),
        getString(R.string.app_description),
        "Projectivy channel integration",
        null,
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        val installed = KodiClient.isInstalled(context)
        val channels = BridgePrefs.channels(context)
        actions += GuidedAction.Builder(context)
            .id(ACTION_STATUS)
            .title(if (installed) "Kodi detected" else "Kodi not detected")
            .description("Package: ${context.kodiPackage} · ${channels.size} bridge channel(s)")
            .infoOnly(true)
            .build()
        actions += GuidedAction.Builder(context).id(ACTION_OPEN_KODI).title("Open Kodi").description("Start Kodi so add-on folders can be queried").build()
        actions += GuidedAction.Builder(context).id(ACTION_BROWSE).title("Browse Kodi and add channel").description("Video add-ons → choose a folder → publish it to Projectivy").build()
        actions += GuidedAction.Builder(context).id(ACTION_REFRESH).title("Refresh all Kodi channels").description("Keeps previous cards if Kodi is unavailable").build()
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
                val result = withContext(Dispatchers.IO) { TvPublisher.refreshAll(context.applicationContext) }
                Toast.makeText(context, "Refreshed ${result.first}/${result.second} Kodi channels", Toast.LENGTH_LONG).show()
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
