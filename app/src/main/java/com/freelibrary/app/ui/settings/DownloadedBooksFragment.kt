package com.freelibrary.app.ui.settings

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.data.download.StoredDownload
import com.freelibrary.app.databinding.FragmentDownloadedBooksBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The Downloaded books page, opened from Storage: how many books are on this phone and how much room they take,
 * and every file with a button to delete it for good (after a confirmation).
 */
@AndroidEntryPoint
class DownloadedBooksFragment : Fragment() {
    private var _binding: FragmentDownloadedBooksBinding? = null
    private val binding get() = _binding!!
    private val storageViewModel: StorageViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pageRoot.applySystemBarInsetsAsPadding()
        binding.pageToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val adapter = StorageAdapter(::confirmDelete)
        binding.downloadsList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    storageViewModel.state.collect { state ->
                        adapter.submitList(state.downloads)
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
                launch { storageViewModel.deleteFailed.collect { showMessage(R.string.storage_delete_failed) } }
            }
        }
    }

    /** Asks first, because a deleted file is gone from the phone for good; the book can be downloaded again later. */
    private fun confirmDelete(download: StoredDownload) {
        val format = getString(StorageAdapter.formatName(download.format))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.storage_delete_title)
            .setMessage(getString(R.string.storage_delete_message, download.title, format))
            .setPositiveButton(R.string.storage_delete) { _, _ -> storageViewModel.delete(download) }
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
        _binding = FragmentDownloadedBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        binding.downloadsList.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
