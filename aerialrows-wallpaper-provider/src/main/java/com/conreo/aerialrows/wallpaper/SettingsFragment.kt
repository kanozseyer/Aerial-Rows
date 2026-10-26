package com.conreo.aerialrows.wallpaper

import android.os.Bundle
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction

class SettingsFragment : GuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance =
        Guidance(
            getString(R.string.plugin_name),
            getString(R.string.plugin_description, BuildConfig.VERSION_NAME),
            getString(R.string.settings),
            null,
        )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        AerialPreferences.init(requireContext())
        val counts = AerialCatalogue.counts(requireContext())
        val selected = AerialPreferences.selectedSources

        AerialCatalogue.sources.forEachIndexed { index, source ->
            actions += GuidedAction.Builder(context)
                .id(SOURCE_ACTION_BASE + index)
                .title(source.label)
                .description(getString(R.string.video_count, counts[source.id] ?: 0))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(source.id in selected)
                .build()
        }

        actions += GuidedAction.Builder(context)
            .id(ACTION_SELECT_ALL)
            .title(getString(R.string.select_all))
            .build()

        actions += GuidedAction.Builder(context)
            .id(ACTION_CLEAR_ALL)
            .title(getString(R.string.clear_all))
            .build()

        actions += GuidedAction.Builder(context)
            .id(ACTION_SELECTED_COUNT)
            .title(getString(R.string.selected_wallpapers))
            .description(selectedCountDescription())
            .infoOnly(true)
            .build()
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when {
            action.id in SOURCE_ACTION_BASE until SOURCE_ACTION_BASE + AerialCatalogue.sources.size -> {
                val source = AerialCatalogue.sources[(action.id - SOURCE_ACTION_BASE).toInt()]
                val selected = AerialPreferences.selectedSources.toMutableSet()
                if (action.isChecked) selected += source.id else selected -= source.id
                AerialPreferences.selectedSources = selected
                notifyProviderChanged()
                updateSelectedCount()
            }

            action.id == ACTION_SELECT_ALL -> {
                AerialPreferences.selectedSources = AerialCatalogue.sourceIds
                syncSourceChecks()
                notifyProviderChanged()
                updateSelectedCount()
            }

            action.id == ACTION_CLEAR_ALL -> {
                AerialPreferences.selectedSources = emptySet()
                syncSourceChecks()
                notifyProviderChanged()
                updateSelectedCount()
            }
        }
    }

    private fun syncSourceChecks() {
        val selected = AerialPreferences.selectedSources
        AerialCatalogue.sources.forEachIndexed { index, source ->
            val id = SOURCE_ACTION_BASE + index
            findActionById(id)?.isChecked = source.id in selected
            notifyActionChanged(findActionPositionById(id))
        }
    }

    private fun updateSelectedCount() {
        findActionById(ACTION_SELECTED_COUNT)?.description = selectedCountDescription()
        notifyActionChanged(findActionPositionById(ACTION_SELECTED_COUNT))
    }

    private fun selectedCountDescription(): String {
        val count = AerialCatalogue.loadSelected(requireContext(), AerialPreferences.selectedSources).size
        return getString(R.string.video_count, count)
    }

    private fun notifyProviderChanged() {
        (activity as? SettingsActivity)?.requestWallpaperUpdate()
    }

    companion object {
        private const val SOURCE_ACTION_BASE = 100L
        private const val ACTION_SELECT_ALL = 200L
        private const val ACTION_CLEAR_ALL = 201L
        private const val ACTION_SELECTED_COUNT = 202L
    }
}
