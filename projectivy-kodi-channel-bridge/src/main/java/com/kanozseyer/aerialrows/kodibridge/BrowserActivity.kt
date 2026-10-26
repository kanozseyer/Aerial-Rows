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
    val sourceUri: String by lazy { intent.getStringExtra(EXTRA_URI) ?: KodiClient.VIDEO_ADDONS_ROOT }
    val displayName: String by lazy { intent.getStringExtra(EXTRA_NAME) ?: "Video add-ons" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        host.sourceUri,
        "Kodi browser",
        null,
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        addFixedActions(actions)
        actions += GuidedAction.Builder(context).id(ACTION_LOADING).title("Loading Kodi folder…").infoOnly(true).build()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) { KodiClient.list(requireContext(), host.sourceUri) }
            if (loaded == null) {
                val actions = mutableListOf<GuidedAction>()
                addFixedActions(actions)
                actions += GuidedAction.Builder(context)
                    .id(ACTION_LOADING)
                    .title("Kodi folder unavailable")
                    .description("Open Kodi, return here, then reopen this folder.")
                    .infoOnly(true)
                    .build()
                setActions(actions)
                return@launch
            }
            entries = loaded.take(MAX_ENTRIES)
            val actions = mutableListOf<GuidedAction>()
            addFixedActions(actions)
            entries.forEachIndexed { index, item ->
                actions += GuidedAction.Builder(context)
                    .id(ENTRY_BASE + index)
                    .title(item.name)
                    .description(if (item.isDirectory) "Folder · ${item.uri}" else (item.mediaType ?: item.category ?: item.uri))
                    .build()
            }
            if (entries.isEmpty()) {
                actions += GuidedAction.Builder(context).id(ACTION_LOADING).title("No items returned by Kodi").infoOnly(true).build()
            }
            setActions(actions)
        }
    }

    private fun addFixedActions(actions: MutableList<GuidedAction>) {
        actions += GuidedAction.Builder(context)
            .id(ACTION_USE_FOLDER)
            .title("Use this folder as Projectivy channel")
            .description("Publish up to 60 items and keep them cached when Kodi closes")
            .build()
        actions += GuidedAction.Builder(context)
            .id(ACTION_OPEN_FOLDER)
            .title("Open this folder in Kodi")
            .description(host.sourceUri)
            .build()
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_USE_FOLDER -> addChannel()
            ACTION_OPEN_FOLDER -> KodiClient.open(requireContext(), host.sourceUri)
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
        val title = "Kodi · ${host.displayName}"
        val config = BridgePrefs.add(context, title, host.sourceUri)
        lifecycleScope.launch {
            val synced = withContext(Dispatchers.IO) { TvPublisher.sync(context.applicationContext, config) }
            Toast.makeText(
                context,
                if (synced) "$title added to Android TV channels" else "$title saved; open Kodi and refresh to populate it",
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
