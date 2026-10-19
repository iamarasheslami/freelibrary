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
import com.freelibrary.app.data.reader.EpubBook
import com.freelibrary.app.data.reader.EpubPageServer
import com.freelibrary.app.data.reader.ReaderStyle
import com.freelibrary.app.databinding.FragmentEpubReaderBinding
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

private const val RESTORE_RETRY_MS = 60L
private const val RESTORE_ATTEMPTS = 25
private const val LABEL_REFRESH_MS = 300L
private const val END_PROMPT_MS = 10_000

/**
 * Reads an EPUB one chapter at a time in a web view, page by page or as a scroll. The book is never
 * unpacked and never touches the network: the web view asks [EpubPageServer], which answers from
 * inside the book file, and JavaScript, file access and network loads are all off. Links that stay
 * inside the book work; links out of it do nothing. The page is painted in the app's current theme.
 * In page view a swipe or a tap near an edge turns the page, and turning past the end of a chapter
 * opens the next one.
 */
@AndroidEntryPoint
class EpubReaderFragment : Fragment() {
    private val viewModel: EpubReaderViewModel by viewModels()

    private var _binding: FragmentEpubReaderBinding? = null
    private val binding get() = _binding!!

    @Volatile
    private var server: EpubPageServer? = null
    private var book: EpubBook? = null
    private var baseStyle: ReaderStyle? = null
    private var chapters: List<String> = emptyList()
    private var paged = true
    private var currentChapter = 0
    private var currentPagePath: String? = null
    private var pendingPermille = 0

    /** True once the page showing has loaded and been put back where the reader was. */
    private var pageReady = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEpubReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.readerRoot.applySystemBarInsetsAsPadding()
        binding.readerToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.readerToolbar.inflateMenu(R.menu.menu_reader)
        binding.readerToolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_toggle_paging && server != null) viewModel.setPaged(!paged)
            true
        }
        binding.readerPreviousButton.setOnClickListener { showChapter(currentChapter - 1, 0, report = true) }
        binding.readerNextButton.setOnClickListener { showChapter(currentChapter + 1, 0, report = true) }
        setUpWebView()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.paged.collect { onPagedChanged(it) } }
                launch { viewModel.endReached.collect { offerToFinish() } }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpWebView() {
        val web = binding.readerWebView
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
                ): WebResourceResponse = answer(request.url.toString())

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = handleLink(request.url.toString())

                override fun onPageFinished(
                    view: WebView,
                    url: String,
                ) = restoreScroll(0)
            }
        web.setOnScrollChangeListener { _, _, _, _, _ -> onScrolled() }
        web.onTurnPage = ::turnPage
        // The page margin in page view is padding around the web view; touches there still turn pages.
        binding.readerPageHost.setOnTouchListener { _, event -> forwardToWebView(event) }
    }

    private fun forwardToWebView(event: MotionEvent): Boolean {
        if (!paged) return false
        val web = binding.readerWebView
        val copy = MotionEvent.obtain(event)
        copy.offsetLocation(-web.left.toFloat(), -web.top.toFloat())
        web.dispatchTouchEvent(copy)
        copy.recycle()
        return true
    }

    private fun render(state: EpubReaderUiState) {
        when (state) {
            EpubReaderUiState.Loading -> Unit
            is EpubReaderUiState.Failed -> {
                binding.readerLoading.isVisible = false
                binding.readerMessage.setText(failureMessage(state.reason))
                binding.readerMessage.isVisible = true
            }
            is EpubReaderUiState.Ready ->
                if (server == null) {
                    // The page height is measured from the laid out view, so wait for it.
                    binding.readerPageHost.doOnLayout { if (server == null) startReading(state) }
                }
        }
    }

    private fun startReading(ready: EpubReaderUiState.Ready) {
        book = ready.book
        chapters = ready.book.chapters
        baseStyle = readerStyle()
        paged = viewModel.paged.value
        applyMode()
        binding.readerToolbar.title = ready.title.orEmpty()
        binding.readerLoading.isVisible = false
        binding.readerWebView.isVisible = true
        binding.readerChapterBar.isVisible = true
        val start = viewModel.position
        showChapter(start.chapter, start.permille, report = false)
    }

    private fun readerStyle(): ReaderStyle {
        val view = requireView()
        return ReaderStyle(
            textColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface),
            backgroundColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface),
            linkColor = MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary),
        )
    }

    /** Sets up the server, the page margin and the menu for the mode in [paged]. */
    private fun applyMode() {
        val book = book ?: return
        val style = (baseStyle ?: return).copy(paged = paged, pageHeight = pageHeightInCssPixels())
        server = EpubPageServer(book, style)
        val margin = if (paged) resources.getDimensionPixelSize(R.dimen.reader_page_margin) else 0
        binding.readerPageHost.setPadding(margin, 0, margin, 0)
        binding.readerPageHost.setBackgroundColor(style.backgroundColor)
        binding.readerWebView.setBackgroundColor(style.backgroundColor)
        binding.readerWebView.paged = paged
        binding.readerWebView.isVerticalScrollBarEnabled = !paged
        binding.readerWebView.isHorizontalScrollBarEnabled = false
        binding.readerToolbar.menu.findItem(R.id.action_toggle_paging)
            ?.setTitle(if (paged) R.string.reader_scroll_view else R.string.reader_page_view)
    }

    /** The height of the web view in the pixels the page is laid out in, or 0 before it is laid out. */
    private fun pageHeightInCssPixels(): Int {
        val height = binding.readerWebView.height
        if (height <= 0) return 0
        return (height / resources.displayMetrics.density).toInt() - 1
    }

    private fun onPagedChanged(newValue: Boolean) {
        if (server == null || newValue == paged) return
        paged = newValue
        applyMode()
        showChapter(currentChapter, viewModel.position.permille, report = false)
    }

    /** Shows chapter [chapter] and goes to [permille] of it. [report] tells the view model the reader moved. */
    private fun showChapter(
        chapter: Int,
        permille: Int,
        report: Boolean,
        address: String? = null,
    ) {
        val server = server ?: return
        if (chapter !in chapters.indices) return
        pageReady = false
        currentChapter = chapter
        currentPagePath = chapters[chapter]
        pendingPermille = permille
        if (report) viewModel.onPositionChanged(chapter, permille)
        updateChapterBar()
        binding.readerWebView.loadUrl(address ?: server.addressOf(chapters[chapter]))
    }

    /** A swipe or a tap at an edge asked for another page: turn it, or move to the next or previous chapter. */
    private fun turnPage(forward: Boolean) {
        val web = _binding?.readerWebView ?: return
        if (!pageReady) {
            web.snapToPage()
            return
        }
        if (web.turnPage(forward)) return
        val neighbour = currentChapter + if (forward) 1 else -1
        if (neighbour in chapters.indices) {
            showChapter(neighbour, if (forward) 0 else ReaderPosition.MAX_PERMILLE, report = true)
        } else {
            web.snapToPage()
        }
    }

    private fun updateChapterBar() {
        val web = _binding?.readerWebView ?: return
        binding.readerChapterLabel.text =
            if (paged && pageReady) {
                getString(R.string.reader_page_label, currentChapter + 1, chapters.size, web.currentPage() + 1, web.pageCount())
            } else {
                getString(R.string.reader_chapter_label, currentChapter + 1, chapters.size)
            }
        binding.readerPreviousButton.isEnabled = currentChapter > 0
        binding.readerNextButton.isEnabled = currentChapter < chapters.lastIndex
    }

    /** Called on a web view thread: the book's file for [url], or an empty not-found answer. */
    private fun answer(url: String): WebResourceResponse {
        val server = server
        val path = server?.pathOf(url)
        val served = if (server != null && path != null) runCatching { server.serve(path) }.getOrNull() else null
        return if (served != null) {
            WebResourceResponse(served.mimeType, served.encoding, served.stream)
        } else {
            WebResourceResponse("text/plain", "utf-8", 404, "Not found", emptyMap<String, String>(), ByteArrayInputStream(ByteArray(0)))
        }
    }

    /** Links inside the book open its pages; anything else is ignored, so the book never leaves the app. */
    private fun handleLink(url: String): Boolean {
        val server = server ?: return true
        val path = server.pathOf(url) ?: return true
        // A link to another place on the page showing is left to the web view.
        if (path == currentPagePath && url.contains('#') && !paged) return false
        val chapter = chapters.indexOf(path)
        if (chapter >= 0) {
            showChapter(chapter, 0, report = true, address = url)
        } else {
            // A page outside the reading order, such as a note: shown, but the reader's place stays in the chapter.
            pageReady = false
            currentPagePath = path
            pendingPermille = 0
            binding.readerWebView.loadUrl(url)
        }
        return true
    }

    /** Puts the page showing back where the reader was; the page may need a moment to be laid out. */
    private fun restoreScroll(attempt: Int) {
        val web = _binding?.readerWebView ?: return
        if (pageReady) return
        if (pendingPermille == 0 || web.scrollToPermille(pendingPermille)) {
            pageReady = true
            onScrolled()
            // The number of pages is only known once the page is laid out.
            web.postDelayed({ updateChapterBar() }, LABEL_REFRESH_MS)
        } else if (attempt < RESTORE_ATTEMPTS) {
            web.postDelayed({ restoreScroll(attempt + 1) }, RESTORE_RETRY_MS)
        } else {
            pageReady = true
        }
    }

    private fun onScrolled() {
        if (!pageReady) return
        val web = _binding?.readerWebView ?: return
        if (currentPagePath != chapters.getOrNull(currentChapter)) return
        viewModel.onPositionChanged(currentChapter, web.scrolledPermille())
        updateChapterBar()
    }

    private fun offerToFinish() {
        val bar = _binding?.readerChapterBar ?: return
        Snackbar.make(binding.readerRoot, R.string.reader_end_reached, Snackbar.LENGTH_INDEFINITE)
            .setDuration(END_PROMPT_MS)
            .setAnchorView(bar)
            .setAction(R.string.reader_mark_finished) { viewModel.onMarkFinished() }
            .show()
    }

    @StringRes
    private fun failureMessage(reason: ReaderFailure): Int =
        when (reason) {
            ReaderFailure.NOT_DOWNLOADED -> R.string.reader_error_not_downloaded
            ReaderFailure.NOT_AN_EPUB -> R.string.reader_error_not_epub
            ReaderFailure.DAMAGED -> R.string.reader_error_damaged
            ReaderFailure.EMPTY -> R.string.reader_error_empty
        }

    override fun onStop() {
        viewModel.saveNow()
        super.onStop()
    }

    override fun onDestroyView() {
        server = null
        book = null
        baseStyle = null
        chapters = emptyList()
        pageReady = false
        _binding?.readerWebView?.let { web ->
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
