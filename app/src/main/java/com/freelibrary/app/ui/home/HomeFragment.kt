package com.freelibrary.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.ImageLoader
import com.freelibrary.app.databinding.FragmentHomeBinding
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The home screen: a scrolling title followed by the reader's own sliders and the curated shelves.
 * Nothing is drawn until the first lists arrive. Taps do nothing yet; navigation comes next.
 *
 * The catalog sync does not start here: MainActivity starts it once per app start.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {
    @Inject lateinit var imageLoader: ImageLoader

    private val viewModel: HomeViewModel by viewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val adapter =
            HomeAdapter(
                typefaces = CoverTypefaces.from(requireContext()),
                imageLoader = imageLoader,
                onBookClick = {},
                onViewMore = {},
            )
        binding.homeList.adapter = adapter
        binding.homeList.applySystemBarInsetsAsPadding()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        HomeUiState.Loading -> Unit
                        is HomeUiState.Content -> adapter.submitSections(state.sections)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.homeList.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
