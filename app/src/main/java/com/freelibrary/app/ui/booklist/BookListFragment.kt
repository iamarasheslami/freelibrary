package com.freelibrary.app.ui.booklist

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
import androidx.navigation.fragment.navArgs
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentBookListBinding
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The full list behind a "View more" button: a title bar with a back arrow above a grid of cards.
 * Which list it shows comes from its navigation arguments. A tap on a book opens its own page.
 */
@AndroidEntryPoint
class BookListFragment : Fragment() {
    @Inject lateinit var imageLoader: ImageLoader

    private val viewModel: BookListViewModel by viewModels()
    private val args: BookListFragmentArgs by navArgs()

    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBookListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.bookListRoot.applySystemBarInsetsAsPadding()
        binding.bookListToolbar.setTitle(args.titleRes)
        binding.bookListToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val cardWidth = resources.getDimensionPixelSize(R.dimen.book_card_width)
        val gap = resources.getDimensionPixelSize(R.dimen.spacing_s)
        val adapter = BookPagingAdapter(CoverTypefaces.from(requireContext()), imageLoader, onBookClick = ::openBook)
        binding.bookListGrid.layoutManager = AutoSpanGridLayoutManager(requireContext(), cardWidth, gap)
        binding.bookListGrid.addItemDecoration(CenteredCardDecoration(cardWidth, rowGapPx = gap * 2))
        binding.bookListGrid.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pages.collectLatest { adapter.submitData(it) }
            }
        }
    }

    /** Opens a book's own page. A second tap while the page is opening is ignored. */
    private fun openBook(book: BookCard) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.bookListFragment) return
        navController.navigate(BookListFragmentDirections.actionBookListFragmentToBookPageFragment(bookId = book.bookId))
    }

    override fun onDestroyView() {
        binding.bookListGrid.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
