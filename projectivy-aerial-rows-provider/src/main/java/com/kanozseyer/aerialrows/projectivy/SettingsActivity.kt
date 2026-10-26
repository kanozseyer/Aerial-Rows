package com.kanozseyer.aerialrows.projectivy

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperProviderContract

class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(this)

        if (!packageManager.isApplicationInstalled(PROJECTIVY_PACKAGE_ID)) {
            Toast.makeText(this, R.string.projectivy_not_installed, Toast.LENGTH_LONG).show()
        }

        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, SettingsFragment(), android.R.id.content)
        }
    }

    fun requestWallpaperUpdate() {
        sendBroadcast(Intent(WallpaperProviderContract.ACTION_WALLPAPER_PROVIDER_UPDATED).apply {
            `package` = PROJECTIVY_PACKAGE_ID
            putExtra(WallpaperProviderContract.EXTRA_PROVIDER_ID, getString(R.string.plugin_uuid))
            putExtra(
                WallpaperProviderContract.EXTRA_UPDATE_REASON,
                WallpaperProviderContract.UpdateReason.PREFS_CHANGED,
            )
        })
    }

    private fun PackageManager.isApplicationInstalled(packageName: String): Boolean = try {
        getApplicationInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        private const val PROJECTIVY_PACKAGE_ID = "com.spocky.projengmenu"
    }
}

class SettingsFragment : GuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance = Guidance(
        getString(R.string.plugin_short_name),
        "v${BuildConfig.VERSION_NAME}\n\n${getString(R.string.plugin_description)}",
        getString(R.string.settings),
        ContextCompat.getDrawable(requireContext(), R.drawable.aerial_rows_banner),
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        PreferencesManager.init(requireContext())
        val enabled = PreferencesManager.enabledSources

        actions += sourceAction(ACTION_SOURCE_APPLE, R.string.source_apple, PreferencesManager.SOURCE_APPLE in enabled)
        actions += sourceAction(ACTION_SOURCE_AMAZON, R.string.source_amazon, PreferencesManager.SOURCE_AMAZON in enabled)
        actions += sourceAction(ACTION_SOURCE_COMMUNITY_1, R.string.source_community_1, PreferencesManager.SOURCE_COMMUNITY_1 in enabled)
        actions += sourceAction(ACTION_SOURCE_COMMUNITY_2, R.string.source_community_2, PreferencesManager.SOURCE_COMMUNITY_2 in enabled)
        actions += sourceAction(ACTION_SOURCE_CUSTOM, R.string.source_custom, PreferencesManager.SOURCE_CUSTOM in enabled)

        actions += GuidedAction.Builder(context)
            .id(ACTION_PREFER_4K)
            .title(getString(R.string.prefer_4k))
            .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
            .checked(PreferencesManager.prefer4k)
            .build()

        actions += GuidedAction.Builder(context)
            .id(ACTION_PREFER_HDR)
            .title(getString(R.string.prefer_hdr))
            .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
            .checked(PreferencesManager.preferHdr)
            .build()

        actions += GuidedAction.Builder(context)
            .id(ACTION_FALLBACK)
            .title(getString(R.string.quality_fallback))
            .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
            .checked(PreferencesManager.fallback)
            .build()

        actions += editableAction(
            ACTION_CUSTOM_URL,
            R.string.source_custom_url,
            PreferencesManager.customSourceUrl,
        )
        actions += editableAction(
            ACTION_CACHE_HOURS,
            R.string.cache_hours,
            PreferencesManager.cacheDurationHours.toString(),
        )
        actions += editableAction(
            ACTION_FILTER_KEYWORDS,
            R.string.filter_keywords,
            PreferencesManager.filterKeywords,
        )

        actions += GuidedAction.Builder(context)
            .id(ACTION_COUNT)
            .title(getString(R.string.wallpaper_count))
            .description(getString(R.string.loading))
            .infoOnly(true)
            .build()

        actions += GuidedAction.Builder(context)
            .id(ACTION_REFRESH)
            .title(getString(R.string.refresh_now))
            .description(getString(R.string.refresh_now_desc))
            .build()

        actions += GuidedAction.Builder(context)
            .id(ACTION_NOTE)
            .title(getString(R.string.source_note))
            .infoOnly(true)
            .build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateWallpaperCount()
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_SOURCE_APPLE -> setSource(PreferencesManager.SOURCE_APPLE, action.isChecked)
            ACTION_SOURCE_AMAZON -> setSource(PreferencesManager.SOURCE_AMAZON, action.isChecked)
            ACTION_SOURCE_COMMUNITY_1 -> setSource(PreferencesManager.SOURCE_COMMUNITY_1, action.isChecked)
            ACTION_SOURCE_COMMUNITY_2 -> setSource(PreferencesManager.SOURCE_COMMUNITY_2, action.isChecked)
            ACTION_SOURCE_CUSTOM -> setSource(PreferencesManager.SOURCE_CUSTOM, action.isChecked)
            ACTION_PREFER_4K -> {
                PreferencesManager.prefer4k = action.isChecked
                changed()
            }
            ACTION_PREFER_HDR -> {
                PreferencesManager.preferHdr = action.isChecked
                changed()
            }
            ACTION_FALLBACK -> {
                PreferencesManager.fallback = action.isChecked
                changed()
            }
            ACTION_CUSTOM_URL -> {
                PreferencesManager.customSourceUrl = action.editDescription?.toString().orEmpty()
                updateDescription(ACTION_CUSTOM_URL, PreferencesManager.customSourceUrl)
                changed()
            }
            ACTION_CACHE_HOURS -> {
                PreferencesManager.cacheDurationHours =
                    action.editDescription?.toString()?.toIntOrNull()
                        ?: PreferencesManager.DEFAULT_CACHE_DURATION_HOURS
                updateDescription(ACTION_CACHE_HOURS, PreferencesManager.cacheDurationHours.toString())
                changed()
            }
            ACTION_FILTER_KEYWORDS -> {
                PreferencesManager.filterKeywords = action.editDescription?.toString().orEmpty()
                updateDescription(ACTION_FILTER_KEYWORDS, PreferencesManager.filterKeywords)
                changed()
            }
            ACTION_REFRESH -> changed()
        }
    }

    private fun sourceAction(id: Long, titleRes: Int, checked: Boolean): GuidedAction =
        GuidedAction.Builder(context)
            .id(id)
            .title(getString(titleRes))
            .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
            .checked(checked)
            .build()

    private fun editableAction(id: Long, titleRes: Int, value: String): GuidedAction =
        GuidedAction.Builder(context)
            .id(id)
            .title(getString(titleRes))
            .description(value)
            .editDescription(value)
            .descriptionEditable(true)
            .build()

    private fun setSource(source: String, enabled: Boolean) {
        PreferencesManager.setSourceEnabled(source, enabled)
        changed()
    }

    private fun changed() {
        (activity as? SettingsActivity)?.requestWallpaperUpdate()
        updateWallpaperCount()
    }

    private fun updateDescription(id: Long, value: String) {
        findActionById(id)?.description = value
        val position = findActionPositionById(id)
        if (position >= 0) notifyActionChanged(position)
    }

    private fun updateWallpaperCount() {
        lifecycleScope.launch {
            val action = findActionById(ACTION_COUNT) ?: return@launch
            action.description = getString(R.string.loading)
            val position = findActionPositionById(ACTION_COUNT)
            if (position >= 0) notifyActionChanged(position)

            val count = AerialRowsCatalogue(requireContext()).load().size
            action.description = if (count >= AerialRowsCatalogue.MAX_WALLPAPERS) "$count+" else count.toString()
            if (position >= 0) notifyActionChanged(position)
        }
    }

    companion object {
        private const val ACTION_SOURCE_APPLE = 1L
        private const val ACTION_SOURCE_AMAZON = 2L
        private const val ACTION_SOURCE_COMMUNITY_1 = 3L
        private const val ACTION_SOURCE_COMMUNITY_2 = 4L
        private const val ACTION_SOURCE_CUSTOM = 5L
        private const val ACTION_PREFER_4K = 10L
        private const val ACTION_PREFER_HDR = 11L
        private const val ACTION_FALLBACK = 12L
        private const val ACTION_CUSTOM_URL = 20L
        private const val ACTION_CACHE_HOURS = 21L
        private const val ACTION_FILTER_KEYWORDS = 22L
        private const val ACTION_COUNT = 30L
        private const val ACTION_REFRESH = 31L
        private const val ACTION_NOTE = 32L
    }
}
