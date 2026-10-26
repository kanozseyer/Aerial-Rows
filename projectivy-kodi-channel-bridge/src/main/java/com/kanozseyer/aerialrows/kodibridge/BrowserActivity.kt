package com.kanozseyer.aerialrows.kodibridge

import android.content.Intent
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

class BrowserActivity : FragmentActivity() {
    var sourceUri: String? = null
    var displayName: String = KodiClient.RED_LIGHT_NAME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceUri = intent.getStringExtra(EXTRA_URI)
        displayName = intent.getStringExtra(EXTRA_NAME) ?: KodiClient.RED_LIGHT_NAME
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, KodiBrowserFragment(), android.R.id.content)
        }
    }

    companion object {
        const val EXTRA_URI = "uri"
        const val EXTRA_NAME = "name"
    }
}

class KodiBrowserFragment : GuidedStepSupportFragment() {
    private val host get() = requireActivity() as BrowserActivity
    private var entries: List<KodiEntry> = emptyList()

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance = Guidance(
        host.displayName,
        host.sourceUri ?: "Auto-detecting Red Light…",
        "Red Light browser",
        null,
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions += GuidedAction.Builder(context).id(ACTION_LOADING).title("Loading Red Light…").infoOnly(true).build()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val resolved = withContext(Dispatchers.IO) {
                val source = host.sourceUri ?: KodiClient.redLightRoot(requireContext())?.uri
                if (source == null) null else source to KodiClient.list(requireContext(), source)
            }

            if (resolved == null || resolved.second == null) {
                setActions(
                    listOf(
                        GuidedAction.Builder(context)
                            .id(ACTION_LOADING)
                            .title("Red Light unavailable")
                            .description("Open Kodi once and make sure Red Light is installed, then retry.")
                            .infoOnly(true)
                            .build(),
                    ),
                )
                return@launch
            }

            host.sourceUri = resolved.first
            entries = resolved.second.orEmpty().take(MAX_ENTRIES)
            val actions = mutableListOf<GuidedAction>()
            addFixedActions(actions)
            entries.forEachIndexed { index, item ->
                actions += GuidedAction.Builder(context)
                    .id(ENTRY_BASE + index)
                    .title(item.name)
                    .description(if (item.isDirectory) "Red Light folder" else (item.mediaType ?: item.category ?: "Red Light item"))
                    .build()
            }
            if (entries.isEmpty()) {
                actions += GuidedAction.Builder(context).id(ACTION_LOADING).title("No items returned by Red Light").infoOnly(true).build()
            }
            setActions(actions)
        }
    }

    private fun addFixedActions(actions: MutableList<GuidedAction>) {
        actions += GuidedAction.Builder(context)
            .id(ACTION_USE_FOLDER)
            .title("Use this Red Light section as Projectivy channel")
            .description("Publish up to 60 items and keep them cached when Kodi closes")
            .build()
        actions += GuidedAction.Builder(context)
            .id(ACTION_OPEN_FOLDER)
            .title("Open this section in Kodi")
            .description(host.sourceUri ?: "Red Light")
            .build()
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_USE_FOLDER -> addChannel()
            ACTION_OPEN_FOLDER -> host.sourceUri?.let { KodiClient.open(requireContext(), it) }
            else -> if (action.id >= ENTRY_BASE) {
                entries.getOrNull((action.id - ENTRY_BASE).toInt())?.let { item ->
                    if (item.isDirectory) {
                        startActivity(Intent(requireContext(), BrowserActivity::class.java)
                            .putExtra(BrowserActivity.EXTRA_URI, item.uri)
                            .putExtra(BrowserActivity.EXTRA_NAME, item.name))
                    } else {
                        KodiClient.open(requireContext(), item.uri)
                    }
                }
            }
        }
    }

    private fun addChannel() {
        val context = requireContext()
        val source = host.sourceUri ?: return
        val title = "Red Light · ${host.displayName}"
        val config = BridgePrefs.add(context, title, source)
        lifecycleScope.launch {
            val synced = withContext(Dispatchers.IO) { TvPublisher.sync(context.applicationContext, config) }
            Toast.makeText(
                context,
                if (synced) "$title added to Projectivy channels" else "$title saved; open Kodi and refresh to populate it",
                Toast.LENGTH_LONG,
            ).show()
            requireActivity().finish()
        }
    }

    companion object {
        private const val MAX_ENTRIES = 100
        private const val ACTION_USE_FOLDER = 1L
        private const val ACTION_OPEN_FOLDER = 2L
        private const val ACTION_LOADING = 3L
        private const val ENTRY_BASE = 1000L
    }
}
