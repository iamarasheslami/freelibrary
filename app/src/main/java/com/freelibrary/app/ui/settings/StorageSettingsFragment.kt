package com.freelibrary.app.ui.settings

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentSettingsStorageBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The Storage page: a row with the number of books downloaded to this phone and the room they take, which opens
 * the Downloaded books page; the size of the cover cache with a button to clear it; and the button that clears
 * the reading history, after a confirmation.
 */
@AndroidEntryPoint
class StorageSettingsFragment : Fragment() {
    private var _binding: FragmentSettingsStorageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by activityViewModels()
    private val storageViewModel: StorageViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pageRoot.applySystemBarInsetsAsPadding()
        binding.pageToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.clearCacheButton.setOnClickListener { viewModel.clearCoverCache() }
        binding.clearHistoryButton.setOnClickListener { confirmClearHistory() }

        binding.downloadsRow.setOnClickListener {
            findNavController().navigate(R.id.action_storageSettingsFragment_to_downloadedBooksFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        binding.coverCacheSize.text = Formatter.formatShortFileSize(requireContext(), state.coverCacheBytes)
                        binding.clearCacheButton.isEnabled = state.coverCacheBytes > 0L
                    }
                }
                launch {
                    storageViewModel.state.collect { state ->
                        binding.downloadsSummary.text =
                            if (state.downloads.isEmpty()) {
                                getString(R.string.storage_downloads_empty)
                            } else {
                                val books = resources.getQuantityString(R.plurals.storage_books, state.bookCount, state.bookCount)
                                val size = Formatter.formatShortFileSize(requireContext(), state.totalBytes)
                                getString(R.string.storage_downloads_summary, books, size)
                            }
                    }
                }
                launch {
                    viewModel.historyCleared.collect { showMessage(if (it) R.string.history_cleared else R.string.history_clear_failed) }
                }
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

    private fun showMessage(message: Int) {
        Snackbar.make(binding.pageRoot, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
