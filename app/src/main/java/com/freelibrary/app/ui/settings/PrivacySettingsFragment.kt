package com.freelibrary.app.ui.settings

import android.os.Bundle
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
import com.freelibrary.app.databinding.FragmentSettingsPrivacyBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The Privacy page: clearing the reading history, after a confirmation. */
@AndroidEntryPoint
class PrivacySettingsFragment : Fragment() {
    private var _binding: FragmentSettingsPrivacyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pageRoot.applySystemBarInsetsAsPadding()
        binding.pageToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.clearHistoryButton.setOnClickListener { confirmClearHistory() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyCleared.collect { showHistoryResult(it) }
            }
        }
    }

    /** Asks first, because clearing the reading history cannot be undone. */
    private fun confirmClearHistory() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.history_clear_title)
            .setMessage(R.string.history_clear_message)
            .setPositiveButton(R.string.settings_history_clear) { _, _ -> viewModel.clearReadingHistory() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showHistoryResult(cleared: Boolean) {
        val message = if (cleared) R.string.history_cleared else R.string.history_clear_failed
        Snackbar.make(binding.pageRoot, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsPrivacyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
