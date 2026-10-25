package com.freelibrary.app.ui.settings

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.databinding.FragmentSettingsStorageBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The Storage page: the size of the cover cache and a button to clear it. */
@AndroidEntryPoint
class StorageSettingsFragment : Fragment() {
    private var _binding: FragmentSettingsStorageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pageRoot.applySystemBarInsetsAsPadding()
        binding.pageToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.clearCacheButton.setOnClickListener { viewModel.clearCoverCache() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.coverCacheSize.text = Formatter.formatShortFileSize(requireContext(), state.coverCacheBytes)
                    binding.clearCacheButton.isEnabled = state.coverCacheBytes > 0L
                }
            }
        }
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
