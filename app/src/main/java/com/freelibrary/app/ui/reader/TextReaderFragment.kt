package com.freelibrary.app.ui.reader

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.data.reader.BookSearch
import com.freelibrary.app.data.reader.ReaderStyle
import com.freelibrary.app.data.reader.SearchHit
import com.freelibrary.app.data.reader.TextBook
import com.freelibrary.app.data.repository.BookmarkRepository
import com.freelibrary.app.databinding.FragmentTextReaderBinding
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TEXT_END_PROMPT_MS = 10_000

/** The size of the text at 100 percent, in scaled pixels. */
private const val TEXT_BASE_SP = 17f

/**
 * Reads a plain text book one part at a time, page by page or as a scroll. The text is laid out and
 * drawn by the app itself, with no web view, so it needs little memory. In page view a swipe or a tap
 * near an edge turns the page, and turning past the end of a part opens the next one.
 */
@AndroidEntryPoint
class TextReaderFragment : Fragment() {
    @Inject lateinit var bookmarkRepository: BookmarkRepository

    private val viewModel: TextReaderViewModel by viewModels()

    private var _binding: FragmentTextReaderBinding? = null
    private val binding get() = _binding!!

    private var book: TextBook? = null
    private var started = false
    private var paged = true
    private var textPercent = ReaderStyle.DEFAULT_TEXT_PERCENT
    private var currentPart = 0
    private var chrome: ReaderChrome? = null
    private var searchJob: Job? = null
    private var searchDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTextReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.textReaderRoot.applySystemBarInsetsAsPadding()
        chrome = ReaderChrome(listOf(binding.textReaderToolbar, binding.textPartBar), ReaderChrome.screenReaderOn(requireContext()))
        binding.textReaderToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.textReaderToolbar.inflateMenu(R.menu.menu_text_reader)
        (binding.textReaderToolbar.menu.findItem(R.id.action_search)?.actionView as? SearchView)?.onBookSearch(::searchBook)
        binding.textReaderToolbar.setOnMenuItemClickListener { item ->
            if (started) {
                when (item.itemId) {
                    R.id.action_toggle_paging -> viewModel.setPaged(!paged)
                    R.id.action_add_bookmark -> addBookmark()
                    R.id.action_bookmarks -> openBookmarkList()
                    R.id.action_text_size ->
                        showTextSizeControl(
                            binding.textReaderToolbar,
                            current = { viewModel.textPercent.value },
                            onLarger = { viewModel.changeTextSize(larger = true) },
                            onSmaller = { viewModel.changeTextSize(larger = false) },
                            onSet = viewModel::setTextSize,
                        )
                }
            }
            true
        }
        binding.textPage.onTurnPage = ::turnPage
        binding.textPage.onTap = { chrome?.toggle() }
        binding.textPage.onMoved = ::onMoved
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.paged.collect { onPagedChanged(it) } }
                launch { viewModel.textPercent.collect { onTextPercentChanged(it) } }
                launch { viewModel.endReached.collect { offerToFinish() } }
            }
        }
    }

    private fun render(state: TextReaderUiState) {
        when (state) {
            TextReaderUiState.Loading -> Unit
            is TextReaderUiState.Failed -> {
                binding.textLoading.isVisible = false
                binding.textMessage.setText(failureMessage(state.reason))
                binding.textMessage.isVisible = true
                chrome?.show()
            }
            is TextReaderUiState.Ready ->
                if (!started) {
                    binding.textPage.doOnLayout { if (!started) startReading(state) }
                }
        }
    }

    private fun startReading(ready: TextReaderUiState.Ready) {
        started = true
        book = ready.book
        paged = viewModel.paged.value
        textPercent = viewModel.textPercent.value
        updateMenu()
        binding.textLoading.isVisible = false
        binding.textPage.visibility = View.VISIBLE
        val start = viewModel.position
        showPart(start.part, start.permille, report = false)
    }

    private fun textColor(): Int = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurface)

    private fun highlightColor(): Int = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorPrimaryContainer)

    private fun backgroundColor(): Int = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorSurface)

    private fun textSizePx(): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_BASE_SP * textPercent / 100f, resources.displayMetrics)

    private fun updateMenu() {
        binding.textReaderToolbar.menu.findItem(R.id.action_toggle_paging)?.apply {
            setTitle(if (paged) R.string.reader_scroll_view else R.string.reader_page_view)
            setIcon(if (paged) R.drawable.ic_view_scroll else R.drawable.ic_view_page)
        }
    }

    private fun onTextPercentChanged(newValue: Int) {
        if (!started || newValue == textPercent) return
        // Remember where the reader is, then lay the part out again at the new size.
        val permille = _binding?.textPage?.permille() ?: viewModel.position.permille
        textPercent = newValue
        showPart(currentPart, permille, report = false)
    }

    private fun onPagedChanged(newValue: Boolean) {
        if (!started || newValue == paged) return
        val permille = _binding?.textPage?.permille() ?: viewModel.position.permille
        paged = newValue
        updateMenu()
        showPart(currentPart, permille, report = false)
    }

    /** Shows part [part] and goes to [permille] of it. [report] tells the view model the reader moved. */
    private fun showPart(
        part: Int,
        permille: Int,
        report: Boolean,
        highlight: IntRange? = null,
    ) {
        val book = book ?: return
        if (part !in book.parts.indices) return
        currentPart = part
        val plain = book.plain(part)
        val place = if (highlight != null) TextPaging.permilleOf(highlight.first, plain.length) else permille
        if (report) viewModel.onPositionChanged(part, place)
        binding.textPage.show(plain, paged, textSizePx(), textColor(), backgroundColor(), place, highlight, highlightColor())
        updatePartBar()
    }

    /** Looks for [query] in every part, off the main thread, and lists the places found. */
    private fun searchBook(query: String) {
        val book = book ?: return
        searchJob?.cancel()
        searchJob =
            viewLifecycleOwner.lifecycleScope.launch {
                val results =
                    withContext(Dispatchers.Default) {
                        BookSearch.findAll(book.parts.size, query, checkActive = { ensureActive() }) { book.plain(it) }
                    }
                if (results.hits.isEmpty()) {
                    Snackbar.make(binding.textReaderRoot, getString(R.string.reader_search_none, query), Snackbar.LENGTH_LONG).show()
                } else {
                    searchDialog?.dismiss()
                    searchDialog =
                        showSearchResults(requireContext(), query, results, { getString(R.string.reader_search_part, it.unit + 1) }) {
                            openHit(it)
                        }
                }
            }
    }

    /** Goes to a search result: opens its part at the words, which are highlighted. */
    private fun openHit(hit: SearchHit) {
        binding.textReaderToolbar.menu.findItem(R.id.action_search)?.collapseActionView()
        showPart(hit.unit, 0, report = true, highlight = hit.offset until hit.offset + hit.length)
    }

    /** A swipe or a tap at an edge asked for another page: turn it, or move to the next or previous part. */
    private fun turnPage(forward: Boolean) {
        val page = _binding?.textPage ?: return
        val partCount = book?.parts?.size ?: return
        if (page.turnPage(forward)) return
        val neighbour = currentPart + if (forward) 1 else -1
        if (neighbour in 0 until partCount) {
            showPart(neighbour, if (forward) 0 else ReaderPosition.MAX_PERMILLE, report = true)
        }
    }

    private fun onMoved() {
        val page = _binding?.textPage ?: return
        if (!started) return
        viewModel.onPositionChanged(currentPart, page.permille())
        updatePartBar()
    }

    /** Asks for a note and bookmarks the place the reader is at. */
    private fun addBookmark() {
        val book = book ?: return
        val place = viewModel.position
        promptForBookmark(bookmarkRepository, ReaderPosition.Text(book.offsetOf(place.part, place.permille)))
    }

    private fun updatePartBar() {
        val page = _binding?.textPage ?: return
        val partCount = book?.parts?.size ?: return
        binding.textPartLabel.text = getString(R.string.text_part_label, currentPart + 1, partCount)
        binding.textPageLabel.text =
            if (paged) getString(R.string.reader_page_number, page.currentPage() + 1, page.pageCount()) else ""
    }

    private fun offerToFinish() {
        val bar = _binding?.textPartBar ?: return
        chrome?.show()
        Snackbar.make(binding.textReaderRoot, R.string.reader_end_reached, Snackbar.LENGTH_INDEFINITE)
            .setDuration(TEXT_END_PROMPT_MS)
            .setAnchorView(bar)
            .setAction(R.string.reader_mark_finished) { viewModel.onMarkFinished() }
            .show()
    }

    @StringRes
    private fun failureMessage(reason: TextFailure): Int =
        when (reason) {
            TextFailure.NOT_DOWNLOADED -> R.string.reader_error_not_downloaded
            TextFailure.UNREADABLE -> R.string.reader_error_damaged
            TextFailure.EMPTY -> R.string.reader_error_empty
            TextFailure.TOO_LARGE -> R.string.reader_error_text_too_large
        }

    override fun onStop() {
        viewModel.saveNow()
        super.onStop()
    }

    override fun onDestroyView() {
        started = false
        chrome = null
        searchJob?.cancel()
        searchJob = null
        searchDialog?.dismiss()
        searchDialog = null
        book = null
        _binding?.textPage?.let {
            it.onTurnPage = null
            it.onTap = null
            it.onMoved = null
        }
        super.onDestroyView()
        _binding = null
    }
}
