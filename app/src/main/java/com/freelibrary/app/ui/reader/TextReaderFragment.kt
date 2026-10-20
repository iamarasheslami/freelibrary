package com.freelibrary.app.ui.reader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.StringRes
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.data.reader.ReaderStyle
import com.freelibrary.app.data.reader.TextBook
import com.freelibrary.app.data.repository.BookmarkRepository
import com.freelibrary.app.databinding.FragmentTextReaderBinding
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import javax.inject.Inject

private const val TEXT_RESTORE_RETRY_MS = 60L
private const val TEXT_RESTORE_ATTEMPTS = 25
private const val TEXT_LABEL_REFRESH_MS = 300L
private const val TEXT_END_PROMPT_MS = 10_000

/** The address the pages are said to come from; nothing is ever fetched from it. */
private const val TEXT_BASE_URL = "https://text.invalid/"

/**
 * Reads a plain text book one part at a time in a web view, page by page or as a scroll. The text
 * is turned into a page of paragraphs in the app, so JavaScript, file access and network loads are
 * all off and no link can take the reader out of the book. In page view a swipe or a tap near an
 * edge turns the page, and turning past the end of a part opens the next one.
 */
@AndroidEntryPoint
class TextReaderFragment : Fragment() {
    @Inject lateinit var bookmarkRepository: BookmarkRepository

    private val viewModel: TextReaderViewModel by viewModels()

    private var _binding: FragmentTextReaderBinding? = null
    private val binding get() = _binding!!

    private var book: TextBook? = null
    private var baseStyle: ReaderStyle? = null
    private var pageStyle: ReaderStyle? = null
    private var started = false
    private var paged = true
    private var textPercent = ReaderStyle.DEFAULT_TEXT_PERCENT
    private var currentPart = 0
    private var pendingPermille = 0

    /** True once the page showing has loaded and been put back where the reader was. */
    private var pageReady = false

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
        binding.textReaderToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.textReaderToolbar.inflateMenu(R.menu.menu_text_reader)
        binding.textReaderToolbar.setOnMenuItemClickListener { item ->
            if (started) {
                when (item.itemId) {
                    R.id.action_toggle_paging -> viewModel.setPaged(!paged)
                    R.id.action_add_bookmark -> addBookmark()
                    R.id.action_larger_text -> viewModel.changeTextSize(larger = true)
                    R.id.action_smaller_text -> viewModel.changeTextSize(larger = false)
                }
            }
            true
        }
        binding.textPreviousButton.setOnClickListener { showPart(currentPart - 1, 0, report = true) }
        binding.textNextButton.setOnClickListener { showPart(currentPart + 1, 0, report = true) }
        setUpWebView()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.paged.collect { onPagedChanged(it) } }
                launch { viewModel.textPercent.collect { onTextPercentChanged(it) } }
                launch { viewModel.endReached.collect { offerToFinish() } }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpWebView() {
        val web = binding.textWebView
        web.settings.apply {
            javaScriptEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkLoads = true
            domStorageEnabled = false
            setSupportZoom(false)
        }
        web.webViewClient =
            object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse =
                    WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        404,
                        "Not found",
                        emptyMap<String, String>(),
                        ByteArrayInputStream(ByteArray(0)),
                    )

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = true

                override fun onPageFinished(
                    view: WebView,
                    url: String,
                ) = restoreScroll(0)
            }
        web.setOnScrollChangeListener { _, _, _, _, _ -> onScrolled() }
        web.onTurnPage = ::turnPage
        // The page margin in page view is padding around the web view; touches there still turn pages.
        binding.textPageHost.setOnTouchListener { _, event -> forwardToWebView(event) }
    }

    private fun forwardToWebView(event: MotionEvent): Boolean {
        if (!paged) return false
        val web = binding.textWebView
        val copy = MotionEvent.obtain(event)
        copy.offsetLocation(-web.left.toFloat(), -web.top.toFloat())
        web.dispatchTouchEvent(copy)
        copy.recycle()
        return true
    }

    private fun render(state: TextReaderUiState) {
        when (state) {
            TextReaderUiState.Loading -> Unit
            is TextReaderUiState.Failed -> {
                binding.textLoading.isVisible = false
                binding.textMessage.setText(failureMessage(state.reason))
                binding.textMessage.isVisible = true
            }
            is TextReaderUiState.Ready ->
                if (!started) {
                    // The page height is measured from the laid out view, so wait for it.
                    binding.textPageHost.doOnLayout { if (!started) startReading(state) }
                }
        }
    }

    private fun startReading(ready: TextReaderUiState.Ready) {
        started = true
        book = ready.book
        baseStyle = readerStyle()
        paged = viewModel.paged.value
        textPercent = viewModel.textPercent.value
        applyMode()
        binding.textReaderToolbar.title = ready.title.orEmpty()
        binding.textLoading.isVisible = false
        binding.textWebView.isVisible = true
        binding.textPartBar.isVisible = true
        val start = viewModel.position
        showPart(start.part, start.permille, report = false)
    }

    private fun readerStyle(): ReaderStyle {
        val view = requireView()
        return ReaderStyle(
            textColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface),
            backgroundColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface),
            linkColor = MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary),
        )
    }

    /** Sets up the page style, the page margin and the menu for the mode in [paged]. */
    private fun applyMode() {
        val style = (baseStyle ?: return).copy(paged = paged, textPercent = textPercent, pageHeight = pageHeightInCssPixels())
        pageStyle = style
        val margin = if (paged) resources.getDimensionPixelSize(R.dimen.reader_page_margin) else 0
        binding.textPageHost.setPadding(margin, 0, margin, 0)
        binding.textPageHost.setBackgroundColor(style.backgroundColor)
        binding.textWebView.setBackgroundColor(style.backgroundColor)
        binding.textWebView.paged = paged
        binding.textWebView.isVerticalScrollBarEnabled = !paged
        binding.textWebView.isHorizontalScrollBarEnabled = false
        binding.textReaderToolbar.menu.findItem(R.id.action_toggle_paging)
            ?.setTitle(if (paged) R.string.reader_scroll_view else R.string.reader_page_view)
    }

    /** The height of the web view in the pixels the page is laid out in, or 0 before it is laid out. */
    private fun pageHeightInCssPixels(): Int {
        val height = binding.textWebView.height
        if (height <= 0) return 0
        return (height / resources.displayMetrics.density).toInt() - 1
    }

    private fun onTextPercentChanged(newValue: Int) {
        if (!started || newValue == textPercent) return
        // Remember where the reader is, then lay the part out again at the new size.
        val permille = _binding?.textWebView?.takeIf { pageReady }?.scrolledPermille() ?: viewModel.position.permille
        textPercent = newValue
        applyMode()
        showPart(currentPart, permille, report = false)
    }

    private fun onPagedChanged(newValue: Boolean) {
        if (!started || newValue == paged) return
        paged = newValue
        applyMode()
        showPart(currentPart, viewModel.position.permille, report = false)
    }

    /** Shows part [part] and goes to [permille] of it. [report] tells the view model the reader moved. */
    private fun showPart(
        part: Int,
        permille: Int,
        report: Boolean,
    ) {
        val book = book ?: return
        val style = pageStyle ?: return
        if (part !in book.parts.indices) return
        pageReady = false
        currentPart = part
        pendingPermille = permille
        if (report) viewModel.onPositionChanged(part, permille)
        updatePartBar()
        binding.textWebView.loadDataWithBaseURL(TEXT_BASE_URL, book.html(part, style), "text/html", "utf-8", null)
    }

    /** A swipe or a tap at an edge asked for another page: turn it, or move to the next or previous part. */
    private fun turnPage(forward: Boolean) {
        val web = _binding?.textWebView ?: return
        val partCount = book?.parts?.size ?: return
        if (!pageReady) {
            web.snapToPage()
            return
        }
        if (web.turnPage(forward)) return
        val neighbour = currentPart + if (forward) 1 else -1
        if (neighbour in 0 until partCount) {
            showPart(neighbour, if (forward) 0 else ReaderPosition.MAX_PERMILLE, report = true)
        } else {
            web.snapToPage()
        }
    }

    /** Asks for a note and bookmarks the place the reader is at. */
    private fun addBookmark() {
        val book = book ?: return
        val place = viewModel.position
        promptForBookmark(bookmarkRepository, ReaderPosition.Text(book.offsetOf(place.part, place.permille)))
    }

    private fun updatePartBar() {
        val web = _binding?.textWebView ?: return
        val partCount = book?.parts?.size ?: return
        binding.textPartLabel.text =
            if (paged && pageReady) {
                getString(R.string.text_page_label, currentPart + 1, partCount, web.currentPage() + 1, web.pageCount())
            } else {
                getString(R.string.text_part_label, currentPart + 1, partCount)
            }
        binding.textPreviousButton.isEnabled = currentPart > 0
        binding.textNextButton.isEnabled = currentPart < partCount - 1
    }

    /** Puts the page showing back where the reader was; the page may need a moment to be laid out. */
    private fun restoreScroll(attempt: Int) {
        val web = _binding?.textWebView ?: return
        if (pageReady) return
        if (pendingPermille == 0 || web.scrollToPermille(pendingPermille)) {
            pageReady = true
            onScrolled()
            // The number of pages is only known once the page is laid out.
            web.postDelayed({ updatePartBar() }, TEXT_LABEL_REFRESH_MS)
        } else if (attempt < TEXT_RESTORE_ATTEMPTS) {
            web.postDelayed({ restoreScroll(attempt + 1) }, TEXT_RESTORE_RETRY_MS)
        } else {
            pageReady = true
        }
    }

    private fun onScrolled() {
        if (!pageReady) return
        val web = _binding?.textWebView ?: return
        viewModel.onPositionChanged(currentPart, web.scrolledPermille())
        updatePartBar()
    }

    private fun offerToFinish() {
        val bar = _binding?.textPartBar ?: return
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
        book = null
        baseStyle = null
        pageStyle = null
        pageReady = false
        _binding?.textWebView?.let { web ->
            web.onTurnPage = null
            web.stopLoading()
            web.webViewClient = WebViewClient()
            (web.parent as? ViewGroup)?.removeView(web)
            web.destroy()
        }
        super.onDestroyView()
        _binding = null
    }
}
