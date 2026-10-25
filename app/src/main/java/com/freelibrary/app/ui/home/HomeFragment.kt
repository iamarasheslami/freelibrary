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
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentHomeBinding
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.HomeSection
import com.freelibrary.app.ui.booklist.toArgs
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.freelibrary.app.ui.shell.StartupGate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The home screen: the reader's own sliders followed by the curated shelves.
 * Nothing is drawn until the first lists arrive. "View more" opens the full list; taps on a book
 * do nothing yet.
 *
 * The catalog sync does not start here: MainActivity starts it once per app start.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {
    @Inject lateinit var imageLoader: ImageLoader

    @Inject lateinit var startupGate: StartupGate

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
                onBookClick = ::openBook,
                onViewMore = ::openList,
            )
        binding.homeList.adapter = adapter
        binding.homeList.applySystemBarInsetsAsPadding()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        HomeUiState.Loading -> Unit
                        is HomeUiState.Content -> {
                            adapter.submitSections(state.sections)
                            // Tells the system the first screen is ready to use, so start-up time can be measured to this point.
                            binding.homeList.post {
                                activity?.reportFullyDrawn()
                                startupGate.markReady()
                            }
                        }
                    }
                }
            }
        }
    }

    /** Opens the full list of a section. A second tap while the page is opening is ignored. */
    private fun openList(section: HomeSection) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.homeFragment) return
        val args = section.viewMore.toArgs()
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToBookListFragment(
                kind = args.kind,
                titleRes = section.titleRes,
                bookshelfName = args.bookshelfName,
            ),
        )
    }

    /** Opens a book's own page. A second tap while the page is opening is ignored. */
    private fun openBook(book: BookCard) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.homeFragment) return
        navController.navigate(HomeFragmentDirections.actionHomeFragmentToBookPageFragment(bookId = book.bookId))
    }

    override fun onDestroyView() {
        binding.homeList.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
