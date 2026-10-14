package com.freelibrary.app.ui.bookpage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.databinding.FragmentBookPageBinding
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.cover.loadCover
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * A book's own page: cover, title, authors, language, where the book sits on the reader's shelves
 * with the buttons to change that, and the catalog summary. Reading and downloading come with the
 * reader, so there is no Read button yet.
 */
@AndroidEntryPoint
class BookPageFragment : Fragment() {
    @Inject lateinit var imageLoader: ImageLoader

    private val viewModel: BookPageViewModel by viewModels()

    private var _binding: FragmentBookPageBinding? = null
    private val binding get() = _binding!!

    /** The book whose fixed details (title, cover, summary) are already on screen. */
    private var shownBookId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBookPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.bookPageRoot.applySystemBarInsetsAsPadding()
        binding.bookPageToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        ViewCompat.setAccessibilityHeading(binding.bookPageTitle, true)
        ViewCompat.setAccessibilityHeading(binding.bookPageAboutHeading, true)

        val typefaces = CoverTypefaces.from(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it, typefaces) }
            }
        }
    }

    private fun render(
        state: BookPageUiState,
        typefaces: CoverTypefaces,
    ) {
        when (state) {
            BookPageUiState.Loading -> Unit
            BookPageUiState.NotFound -> {
                binding.bookPageScroll.isVisible = false
                binding.bookPageNotFound.isVisible = true
            }
            is BookPageUiState.Content -> showContent(state, typefaces)
        }
    }

    private fun showContent(
        content: BookPageUiState.Content,
        typefaces: CoverTypefaces,
    ) {
        binding.bookPageNotFound.isVisible = false
        binding.bookPageScroll.isVisible = true
        // The book's own details never change while the page is open; only the shelf buttons do.
        if (shownBookId != content.details.bookId) {
            shownBookId = content.details.bookId
            showDetails(content, typefaces)
        }
        showShelf(content)
    }

    private fun showDetails(
        content: BookPageUiState.Content,
        typefaces: CoverTypefaces,
    ) {
        val details = content.details
        binding.bookPageTitle.text = pageTitle(details.title)
        binding.bookPageAuthors.text = details.authors.joinToString("\n") { authorLine(it) }
        binding.bookPageAuthors.isVisible = details.authors.isNotEmpty()
        val language = languageName(details.language, Locale.getDefault())
        binding.bookPageLanguage.text = language
        binding.bookPageLanguage.isVisible = language != null
        binding.bookPageCover.loadCover(details.toCard(), typefaces, imageLoader)
        binding.bookPageSummary.text = details.summary
        binding.bookPageSummary.isVisible = details.summary != null
        binding.bookPageAboutHeading.isVisible = details.summary != null
    }

    private fun showShelf(content: BookPageUiState.Content) {
        val status = content.shelfState?.let(::statusLabel)
        binding.bookPageStatus.isVisible = status != null
        if (status != null) binding.bookPageStatus.setText(status)
        bindAction(binding.bookPagePrimaryButton, content.controls.primary)
        bindAction(binding.bookPageSecondaryButton, content.controls.secondary)
    }

    private fun bindAction(
        button: MaterialButton,
        action: ShelfAction?,
    ) {
        button.isVisible = action != null
        if (action == null) return
        button.setText(actionLabel(action))
        button.setOnClickListener { viewModel.onShelfAction(action) }
    }

    @StringRes
    private fun statusLabel(state: ShelfState): Int =
        when (state) {
            ShelfState.WANT_TO_READ -> R.string.book_page_status_want_to_read
            ShelfState.READING -> R.string.book_page_status_reading
            ShelfState.FINISHED -> R.string.book_page_status_finished
        }

    @StringRes
    private fun actionLabel(action: ShelfAction): Int =
        when (action) {
            ShelfAction.SAVE -> R.string.shelf_action_save
            ShelfAction.MARK_FINISHED -> R.string.shelf_action_mark_finished
            ShelfAction.REMOVE -> R.string.shelf_action_remove
        }

    override fun onDestroyView() {
        shownBookId = null
        super.onDestroyView()
        _binding = null
    }
}
