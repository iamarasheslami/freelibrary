package com.freelibrary.app.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentBookmarksBinding
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.bookpage.ARG_BOOK_ID
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.freelibrary.app.ui.reader.ARG_START_POSITION
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The bookmarks of one book. Tapping one opens the book at that place; the X deletes it, with an Undo. */
@AndroidEntryPoint
class BookmarksFragment : Fragment() {
    private val viewModel: BookmarksViewModel by viewModels()

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.bookmarksRoot.applySystemBarInsetsAsPadding()
        binding.bookmarksToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        val adapter = BookmarkAdapter(onOpen = ::open, onDelete = ::delete)
        binding.bookmarksList.layoutManager = LinearLayoutManager(requireContext())
        binding.bookmarksList.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.items.collect { items ->
                        adapter.submitList(items.orEmpty())
                        binding.bookmarksEmpty.isVisible = items != null && items.isEmpty()
                    }
                }
                launch { viewModel.bookTitle.collect { binding.bookmarksToolbar.subtitle = it } }
            }
        }
    }

    private fun open(item: BookmarkItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val position = item.bookmark.position
            val opens = viewModel.canOpen(position)
            val root = _binding?.bookmarksRoot ?: return@launch
            if (!opens) {
                Snackbar.make(root, R.string.bookmark_cannot_open, Snackbar.LENGTH_LONG).show()
                return@launch
            }
            val navController = findNavController()
            // A second tap before the reader shows must not navigate again.
            if (navController.currentDestination?.id != R.id.bookmarksFragment) return@launch
            val action =
                when (position) {
                    is ReaderPosition.Epub -> R.id.action_bookmarksFragment_to_epubReaderFragment
                    is ReaderPosition.Pdf -> R.id.action_bookmarksFragment_to_pdfReaderFragment
                    is ReaderPosition.Text -> R.id.action_bookmarksFragment_to_textReaderFragment
                }
            navController.navigate(action, bundleOf(ARG_BOOK_ID to item.bookmark.bookId, ARG_START_POSITION to position.encode()))
        }
    }

    private fun delete(item: BookmarkItem) {
        viewModel.delete(item.bookmark)
        Snackbar.make(binding.bookmarksRoot, R.string.bookmark_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.bookmark_undo) { viewModel.restore(item.bookmark) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
