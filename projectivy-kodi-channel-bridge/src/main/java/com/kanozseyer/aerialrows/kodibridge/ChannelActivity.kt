package com.kanozseyer.aerialrows.kodibridge

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, ChannelFragment(), android.R.id.content)
        }
    }

    companion object { const val EXTRA_ID = "channel_id" }
}

class ChannelFragment : GuidedStepSupportFragment() {
    private val config: BridgeChannel?
        get() = activity?.intent?.getStringExtra(ChannelActivity.EXTRA_ID)?.let { BridgePrefs.find(requireContext(), it) }

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        val c = config
        return Guidance(c?.title ?: "Kodi channel", c?.sourceUri ?: "", "Channel actions", null)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions += GuidedAction.Builder(context).id(ACTION_REFRESH).title("Refresh channel now").build()
        actions += GuidedAction.Builder(context).id(ACTION_OPEN).title("Open source folder in Kodi").build()
        actions += GuidedAction.Builder(context).id(ACTION_DELETE).title("Delete bridge channel").description("Removes its cached Android TV programs too").build()
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        val c = config ?: return
        when (action.id) {
            ACTION_OPEN -> KodiClient.open(requireContext(), c.sourceUri)
            ACTION_REFRESH -> lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { TvPublisher.sync(requireContext().applicationContext, c) }
                Toast.makeText(requireContext(), if (ok) "Channel refreshed" else "Kodi unavailable; cached cards kept", Toast.LENGTH_LONG).show()
            }
            ACTION_DELETE -> lifecycleScope.launch {
                withContext(Dispatchers.IO) { TvPublisher.delete(requireContext().applicationContext, c) }
                Toast.makeText(requireContext(), "Channel deleted", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = 1L
        private const val ACTION_OPEN = 2L
        private const val ACTION_DELETE = 3L
    }
}
