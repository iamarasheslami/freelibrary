package com.freelibrary.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentSettingsBinding
import com.freelibrary.app.domain.model.ThemeMode
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The Settings screen: appearance, catalog sync, storage and about. This class only shows
 * [SettingsViewModel]'s state and passes the reader's taps on. Every control reacts to the
 * reader's own taps only, so showing a stored value never writes it back.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private val viewModel: SettingsViewModel by viewModels()

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsRoot.applySystemBarInsetsAsPadding()
        binding.settingsToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.themeSystem.setOnClickListener { viewModel.setThemeMode(ThemeMode.SYSTEM) }
        binding.themeLight.setOnClickListener { viewModel.setThemeMode(ThemeMode.LIGHT) }
        binding.themeDark.setOnClickListener { viewModel.setThemeMode(ThemeMode.DARK) }

        binding.trueBlackSwitch.setOnClickListener { viewModel.setTrueBlack(binding.trueBlackSwitch.isChecked) }
        binding.trueBlackRow.setOnClickListener { binding.trueBlackSwitch.performClick() }
        binding.wifiOnlySwitch.setOnClickListener { viewModel.setWifiOnlySync(binding.wifiOnlySwitch.isChecked) }
        binding.wifiOnlyRow.setOnClickListener { binding.wifiOnlySwitch.performClick() }

        binding.syncNowButton.setOnClickListener { viewModel.syncNow() }
        binding.clearCacheButton.setOnClickListener { viewModel.clearCoverCache() }

        binding.aboutVersion.text = getString(R.string.about_version, versionName())
        binding.aboutSource.setOnClickListener { openLink(AboutLinks.SOURCE) }
        binding.aboutNotices.setOnClickListener { openLink(AboutLinks.NOTICES) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: SettingsState) {
        val themeButton = SettingsLabels.themeButtonId(state.themeMode)
        if (binding.themeGroup.checkedButtonId != themeButton) binding.themeGroup.check(themeButton)
        if (binding.trueBlackSwitch.isChecked != state.trueBlack) binding.trueBlackSwitch.isChecked = state.trueBlack
        if (binding.wifiOnlySwitch.isChecked != state.wifiOnlySync) binding.wifiOnlySwitch.isChecked = state.wifiOnlySync

        val running = state.syncStatus == SyncStatus.Running
        binding.syncNowButton.isEnabled = !running
        binding.syncProgress.visibility = if (running) View.VISIBLE else View.INVISIBLE
        binding.syncStatusText.text = syncText(state)

        binding.coverCacheSize.text = Formatter.formatShortFileSize(requireContext(), state.coverCacheBytes)
        binding.clearCacheButton.isEnabled = state.coverCacheBytes > 0L
    }

    private fun syncText(state: SettingsState): String =
        when (val message = SettingsLabels.syncMessage(state.syncStatus)) {
            is SyncMessage.Text -> getString(message.res)
            is SyncMessage.Plural -> resources.getQuantityString(message.res, message.count, message.count)
            SyncMessage.LastSync ->
                state.lastSyncAt?.let { lastSyncAt ->
                    val ago = DateUtils.getRelativeTimeSpanString(lastSyncAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                    getString(R.string.sync_last, ago)
                } ?: getString(R.string.sync_never)
        }

    @Suppress("DEPRECATION")
    private fun versionName(): String {
        val context = requireContext()
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.about_no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
