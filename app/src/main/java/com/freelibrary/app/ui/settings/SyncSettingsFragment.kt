package com.freelibrary.app.ui.settings

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentSettingsSyncBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The catalog sync page: Wi-Fi-only sync, "Sync now" and when the catalog last synced. */
@AndroidEntryPoint
class SyncSettingsFragment : Fragment() {
    private var _binding: FragmentSettingsSyncBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pageRoot.applySystemBarInsetsAsPadding()
        binding.pageToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.wifiOnlySwitch.setOnClickListener { viewModel.setWifiOnlySync(binding.wifiOnlySwitch.isChecked) }
        binding.wifiOnlyRow.setOnClickListener { binding.wifiOnlySwitch.performClick() }
        binding.syncNowButton.setOnClickListener { viewModel.syncNow() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: SettingsState) {
        if (binding.wifiOnlySwitch.isChecked != state.wifiOnlySync) binding.wifiOnlySwitch.isChecked = state.wifiOnlySync

        val running = state.syncStatus == SyncStatus.Running
        binding.syncNowButton.isEnabled = !running
        binding.syncProgress.visibility = if (running) View.VISIBLE else View.INVISIBLE
        binding.syncStatusText.text = syncText(state)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
