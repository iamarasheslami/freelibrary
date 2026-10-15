package com.freelibrary.app.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentExploreBinding
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.ExploreSection
import com.freelibrary.app.domain.model.ViewMoreTarget
import com.freelibrary.app.ui.booklist.toArgs
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The Explore page: the curated collections first, then every genre and shelf of the catalog,
 * one slider each, loaded ten at a time as the reader scrolls. "View more" opens the full list.
 */
@AndroidEntryPoint
class ExploreFragment : Fragment() {
    @Inject lateinit var imageLoader: ImageLoader

    private val viewModel: ExploreViewModel by viewModels()

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.exploreRoot.applySystemBarInsetsAsPadding()
        binding.exploreToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.exploreToolbar.inflateMenu(R.menu.menu_explore)
        binding.exploreToolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_search) {
                openSearch()
                true
            } else {
                false
            }
        }

        val adapter =
            ExploreAdapter(
                typefaces = CoverTypefaces.from(requireContext()),
                imageLoader = imageLoader,
                onBookClick = ::openBook,
                onViewMore = ::openList,
            )
        binding.exploreList.adapter = adapter
        adapter.addLoadStateListener { states ->
            binding.exploreEmpty.isVisible = states.refresh is LoadState.NotLoading && adapter.itemCount == 0
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collectLatest { adapter.submitData(it) }
            }
        }
    }

    /** Opens the full list of a shelf. A second tap while the page is opening is ignored. */
    private fun openList(section: ExploreSection) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.exploreFragment) return
        val args = ViewMoreTarget.CatalogShelf(section.bookshelfName).toArgs()
        navController.navigate(
            ExploreFragmentDirections.actionExploreFragmentToBookListFragment(
                kind = args.kind,
                titleRes = section.titleRes ?: 0,
                bookshelfName = args.bookshelfName,
                title = if (section.titleRes == null) section.title else null,
            ),
        )
    }

    /** Opens the search screen. A second tap while the page is opening is ignored. */
    private fun openSearch() {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.exploreFragment) return
        navController.navigate(ExploreFragmentDirections.actionExploreFragmentToSearchFragment())
    }

    /** Opens a book's own page. A second tap while the page is opening is ignored. */
    private fun openBook(book: BookCard) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.exploreFragment) return
        navController.navigate(ExploreFragmentDirections.actionExploreFragmentToBookPageFragment(bookId = book.bookId))
    }

    override fun onDestroyView() {
        binding.exploreList.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
