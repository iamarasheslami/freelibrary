package com.freelibrary.app.ui.reader

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.data.reader.PdfBook
import com.freelibrary.app.data.repository.BookmarkRepository
import com.freelibrary.app.databinding.FragmentPdfReaderBinding
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val END_PROMPT_MS = 10_000

/**
 * Reads a downloaded PDF one page at a time, with the phone's own PDF renderer. A page is drawn
 * whole when it is shown and drawn again sharper once the reader zooms in. Pages turn by swiping,
 * by tapping near an edge or with the Previous and Next buttons.
 */
@AndroidEntryPoint
class PdfReaderFragment : Fragment() {
    @Inject lateinit var bookmarkRepository: BookmarkRepository

    private val viewModel: PdfReaderViewModel by viewModels()

    private var _binding: FragmentPdfReaderBinding? = null
    private val binding get() = _binding!!

    private var book: PdfBook? = null
    private var currentPage = 0
    private var renderedLevel = 0
    private var renderJob: Job? = null
    private var pageDialog: AlertDialog? = null
    private var chrome: ReaderChrome? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPdfReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pdfReaderRoot.applySystemBarInsetsAsPadding()
        chrome = ReaderChrome(listOf(binding.pdfReaderToolbar, binding.pdfPageBar), ReaderChrome.screenReaderOn(requireContext()))
        applyPageColors()
        binding.pdfReaderToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.pdfReaderToolbar.inflateMenu(R.menu.menu_pdf_reader)
        binding.pdfReaderToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_go_to_page -> askForPage()
                R.id.action_add_bookmark -> addBookmark()
                R.id.action_bookmarks -> openBookmarkList()
            }
            true
        }
        binding.pdfPageView.onTurnPage = { forward -> goToPage(currentPage + if (forward) 1 else -1) }
        binding.pdfPageView.onTap = { chrome?.toggle() }
        binding.pdfPageView.onZoomSettled = ::onZoomSettled
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.endReached.collect { offerToFinish() } }
            }
        }
    }

    /** In a dark theme the white pages are redrawn in the theme's own paper and ink, so they do not glare. */
    private fun applyPageColors() {
        val view = binding.pdfPageView
        val paper = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface)
        val ink = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface)
        if (PdfNightColors.isDark(paper)) view.setNightColors(paper, ink) else view.setNightColors(null, null)
    }

    private fun render(state: PdfReaderUiState) {
        when (state) {
            PdfReaderUiState.Loading -> Unit
            is PdfReaderUiState.Failed -> {
                binding.pdfLoading.isVisible = false
                binding.pdfMessage.setText(failureMessage(state.reason))
                binding.pdfMessage.isVisible = true
                chrome?.show()
            }
            is PdfReaderUiState.Ready ->
                if (book == null) {
                    // Pages are drawn to fit the laid out view, so wait for it.
                    binding.pdfPageView.doOnLayout { if (book == null) startReading(state) }
                }
        }
    }

    private fun startReading(ready: PdfReaderUiState.Ready) {
        book = ready.book
        binding.pdfPageView.isVisible = true
        goToPage(viewModel.page, report = false)
    }

    /** Shows page [page] (from 0), if the book has it. [report] tells the view model the reader moved. */
    private fun goToPage(
        page: Int,
        report: Boolean = true,
    ) {
        val book = book ?: return
        if (page !in 0 until book.pageCount) return
        currentPage = page
        if (report) viewModel.onPageChanged(page)
        updatePageBar(book)
        draw(page, level = 1, keepView = false)
    }

    private fun updatePageBar(book: PdfBook) {
        binding.pdfPageLabel.text = getString(R.string.pdf_page_label, currentPage + 1, book.pageCount)
    }

    /** Draws [page] at [level] off the main thread and shows it, unless the reader has moved on meanwhile. */
    private fun draw(
        page: Int,
        level: Int,
        keepView: Boolean,
    ) {
        val book = book ?: return
        val view = binding.pdfPageView
        val width = view.width
        renderJob?.cancel()
        renderJob =
            viewLifecycleOwner.lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) { book.render(page, width, level) }
                if (page != currentPage) return@launch
                binding.pdfLoading.isVisible = false
                if (bitmap == null) {
                    binding.pdfMessage.setText(R.string.pdf_page_error)
                    binding.pdfMessage.isVisible = true
                } else {
                    binding.pdfMessage.isVisible = false
                    view.showPage(bitmap, keepView)
                    renderedLevel = level
                }
            }
    }

    /** A zoom has settled: draw the page sharper (or lighter again) if the new zoom calls for it. */
    private fun onZoomSettled(zoom: Float) {
        val level = PdfZoomMath.renderLevel(zoom)
        if (level != renderedLevel) draw(currentPage, level, keepView = true)
    }

    /** Asks for a note and bookmarks the page the reader is on. */
    private fun addBookmark() {
        if (book == null) return
        promptForBookmark(bookmarkRepository, ReaderPosition.Pdf(currentPage.coerceAtLeast(0)))
    }

    private fun askForPage() {
        val book = book ?: return
        val input =
            EditText(requireContext()).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                hint = getString(R.string.pdf_go_to_page_hint, book.pageCount)
                setSingleLine()
            }
        val padding = resources.getDimensionPixelSize(R.dimen.spacing_m)
        val container =
            FrameLayout(requireContext()).apply {
                setPadding(padding, padding / 2, padding, 0)
                addView(input)
            }
        pageDialog?.dismiss()
        pageDialog =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pdf_go_to_page)
                .setView(container)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    input.text.toString().trim().toIntOrNull()?.let { goToPage(it - 1) }
                }.setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun offerToFinish() {
        val bar = _binding?.pdfPageBar ?: return
        chrome?.show()
        Snackbar.make(binding.pdfReaderRoot, R.string.reader_end_reached, Snackbar.LENGTH_INDEFINITE)
            .setDuration(END_PROMPT_MS)
            .setAnchorView(bar)
            .setAction(R.string.reader_mark_finished) { viewModel.onMarkFinished() }
            .show()
    }

    @StringRes
    private fun failureMessage(reason: PdfFailure): Int =
        when (reason) {
            PdfFailure.NOT_DOWNLOADED -> R.string.reader_error_not_downloaded
            PdfFailure.NOT_A_PDF -> R.string.reader_error_not_pdf
            PdfFailure.PASSWORD_PROTECTED -> R.string.reader_error_pdf_password
            PdfFailure.DAMAGED -> R.string.reader_error_damaged
            PdfFailure.EMPTY -> R.string.reader_error_empty
        }

    override fun onStop() {
        viewModel.saveNow()
        super.onStop()
    }

    override fun onDestroyView() {
        renderJob?.cancel()
        renderJob = null
        pageDialog?.dismiss()
        pageDialog = null
        book = null
        chrome = null
        _binding?.pdfPageView?.let { view ->
            view.onTurnPage = null
            view.onTap = null
            view.onZoomSettled = null
        }
        super.onDestroyView()
        _binding = null
    }
}
