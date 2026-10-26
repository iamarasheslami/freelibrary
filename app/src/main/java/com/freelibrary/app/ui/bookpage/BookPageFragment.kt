package com.freelibrary.app.ui.bookpage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
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
import com.freelibrary.app.data.download.BookFiles
import com.freelibrary.app.data.download.DownloadError
import com.freelibrary.app.data.download.DownloadProgress
import com.freelibrary.app.data.download.Transfer
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.databinding.FragmentBookPageBinding
import com.freelibrary.app.databinding.ItemBookActionBinding
import com.freelibrary.app.domain.model.BookFileFormat
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.cover.loadCover
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * A book's own page: cover, title, authors, language, where the book sits on the reader's shelves,
 * the state of its files, a row of icon buttons (Read, Download, the shelf buttons, Bookmarks, Delete)
 * and the catalog summary. Tapping the cover of a downloaded book reads it.
 */
@AndroidEntryPoint
class BookPageFragment : Fragment() {
    @Inject lateinit var imageLoader: ImageLoader

    private val viewModel: BookPageViewModel by viewModels()

    private var _binding: FragmentBookPageBinding? = null
    private val binding get() = _binding!!

    /** The book whose fixed details (title, cover, summary) are already on screen. */
    private var shownBookId: Long? = null

    /** What the action buttons are built from, and what is on screen now, so they are rebuilt only when they change. */
    private var currentFiles: BookFiles? = null
    private var currentShelf: ShelfState? = null
    private var shownActions: List<BookActionState>? = null

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
                launch { viewModel.uiState.collect { render(it, typefaces) } }
                launch { viewModel.files.collect { showFiles(it) } }
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
        currentShelf = content.shelfState
        showActions()
    }

    private fun showFiles(files: BookFiles?) {
        currentFiles = files
        binding.bookPageFiles.isVisible = files != null
        showActions()
        if (files == null) return

        val status = filesStatus(files)
        binding.bookPageFilesStatus.text = status
        binding.bookPageFilesStatus.isVisible = status.isNotEmpty()
        showProgress(files.transfer)

        // Tapping the cover of a downloaded book reads it, like the Read button.
        val readable = files.downloaded
        binding.bookPageCover.isClickable = readable.isNotEmpty()
        binding.bookPageCover.setOnClickListener(if (readable.isEmpty()) null else View.OnClickListener { read(binding.bookPageCover) })
    }

    /** Draws the icon buttons: all of them always, each enabled or not and highlighted or not. */
    private fun showActions() {
        val actions = bookActionsFor(currentFiles, currentShelf)
        if (actions == shownActions) return
        shownActions = actions
        val row = binding.bookPageActions
        row.removeAllViews()
        for (state in actions) {
            val item = ItemBookActionBinding.inflate(layoutInflater, row, false)
            val label = getString(actionLabel(state))
            item.bookActionIcon.setImageResource(actionIcon(state.action))
            item.bookActionIcon.isSelected = state.selected
            item.bookActionLabel.text = label
            item.root.contentDescription = label
            item.root.isSelected = state.selected
            item.root.isEnabled = state.enabled
            item.root.alpha = if (state.enabled) 1f else DISABLED_ALPHA
            item.root.setOnClickListener { if (state.enabled) perform(state.action, item.root) }
            row.addView(item.root)
        }
    }

    private fun perform(
        action: BookAction,
        anchor: View,
    ) {
        when (action) {
            BookAction.READ -> read(anchor)
            BookAction.DOWNLOAD -> download(anchor)
            BookAction.CANCEL_DOWNLOAD -> viewModel.onCancelDownload()
            BookAction.SAVE -> viewModel.onToggleSaved()
            BookAction.FINISHED -> viewModel.onToggleFinished()
            BookAction.RESTART -> confirmRestart()
            BookAction.BOOKMARKS -> openBookmarks()
            BookAction.DELETE_DOWNLOAD -> currentFiles?.let { deleteDownload(it.downloaded) }
        }
    }

    private fun confirmRestart() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.book_restart_title)
            .setMessage(R.string.book_restart_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.book_action_restart) { _, _ -> viewModel.onRestart() }
            .show()
    }

    /** Opens the downloaded book; with more than one downloaded format the reader picks from a list. */
    private fun read(anchor: View) {
        val downloaded = currentFiles?.downloaded.orEmpty()
        when (downloaded.size) {
            0 -> Unit
            1 -> openReader(downloaded.first())
            else -> chooseFormat(anchor, downloaded) { openReader(it) }
        }
    }

    /**
     * Downloads the book. With one format on offer it starts at once; with several the reader picks from
     * all of them, and the ones already on the phone are greyed out with a mark after them.
     */
    private fun download(anchor: View) {
        val files = currentFiles ?: return
        val formats = files.available
        when {
            formats.size == 1 && files.toDownload.isNotEmpty() -> viewModel.onDownload(formats.first())
            formats.size > 1 -> {
                val menu = PopupMenu(requireContext(), anchor)
                formats.forEachIndexed { index, format ->
                    val label = getString(formatLabel(format))
                    val done = format in files.downloaded
                    val item = menu.menu.add(0, index, index, if (done) getString(R.string.book_format_downloaded, label) else label)
                    item.isEnabled = !done
                }
                menu.setOnMenuItemClickListener { item ->
                    viewModel.onDownload(formats[item.itemId])
                    true
                }
                menu.show()
            }
        }
    }

    private fun chooseFormat(
        anchor: View,
        formats: List<BookFileFormat>,
        onChosen: (BookFileFormat) -> Unit,
    ) {
        val menu = PopupMenu(requireContext(), anchor)
        formats.forEachIndexed { index, format -> menu.menu.add(0, index, index, getString(formatLabel(format))) }
        menu.setOnMenuItemClickListener { item ->
            onChosen(formats[item.itemId])
            true
        }
        menu.show()
    }

    private fun openReader(format: BookFileFormat) {
        val bookId = shownBookId ?: return
        val directions =
            if (format == BookFileFormat.TEXT) {
                BookPageFragmentDirections.actionBookPageFragmentToTextReaderFragment(bookId)
            } else if (format == BookFileFormat.PDF) {
                BookPageFragmentDirections.actionBookPageFragmentToPdfReaderFragment(bookId)
            } else {
                BookPageFragmentDirections.actionBookPageFragmentToEpubReaderFragment(bookId)
            }
        findNavController().navigate(directions)
    }

    private fun openBookmarks() {
        val bookId = shownBookId ?: return
        findNavController().navigate(R.id.action_bookPageFragment_to_bookmarksFragment, bundleOf(ARG_BOOK_ID to bookId))
    }

    private fun filesStatus(files: BookFiles): String {
        val transfer = files.transfer
        return when {
            transfer is Transfer.Waiting -> getString(R.string.book_files_waiting)
            transfer is Transfer.Running -> runningText(transfer)
            transfer is Transfer.Failed -> getString(errorMessage(transfer.error))
            files.downloaded.isNotEmpty() -> ""
            files.available.isEmpty() -> getString(R.string.book_files_none)
            else -> ""
        }
    }

    private fun runningText(transfer: Transfer.Running): String {
        val format = getString(formatLabel(transfer.format))
        val percent = percentOf(transfer.progress)
        return if (percent == null) {
            getString(R.string.book_files_downloading, format)
        } else {
            getString(R.string.book_files_downloading_percent, format, percent)
        }
    }

    private fun percentOf(progress: DownloadProgress): Int? {
        val total = progress.total
        if (total == null || total <= 0L) return null
        return (progress.bytes * 100 / total).toInt().coerceIn(0, 100)
    }

    private fun showProgress(transfer: Transfer?) {
        val bar = binding.bookPageDownloadProgress
        val running = transfer as? Transfer.Running
        bar.isVisible = transfer is Transfer.Waiting || running != null
        if (!bar.isVisible) return
        val percent = running?.let { percentOf(it.progress) }
        if (percent == null) {
            bar.isIndeterminate = true
        } else {
            bar.isIndeterminate = false
            bar.setProgressCompat(percent, true)
        }
    }

    private fun deleteDownload(downloaded: List<BookFileFormat>) {
        if (downloaded.size == 1) {
            viewModel.onDeleteDownload(downloaded.first())
            return
        }
        val labels = downloaded.map { getString(formatLabel(it)) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.book_files_delete_title)
            .setItems(labels) { _, index -> viewModel.onDeleteDownload(downloaded[index]) }
            .show()
    }

    @StringRes
    private fun formatLabel(format: BookFileFormat): Int =
        when (format) {
            BookFileFormat.EPUB -> R.string.format_epub
            BookFileFormat.PDF -> R.string.format_pdf
            BookFileFormat.TEXT -> R.string.format_txt
        }

    @StringRes
    private fun errorMessage(error: DownloadError): Int =
        when (error) {
            DownloadError.OFFLINE -> R.string.download_error_offline
            DownloadError.BLOCKED -> R.string.download_error_blocked
            DownloadError.NOT_FOUND -> R.string.download_error_not_found
            DownloadError.SERVER -> R.string.download_error_server
            DownloadError.NOT_A_BOOK -> R.string.download_error_not_a_book
            DownloadError.TOO_LARGE -> R.string.download_error_too_large
            DownloadError.STORAGE -> R.string.download_error_storage
        }

    @DrawableRes
    private fun actionIcon(action: BookAction): Int =
        when (action) {
            BookAction.READ -> R.drawable.ic_action_read
            BookAction.DOWNLOAD -> R.drawable.ic_action_download
            BookAction.CANCEL_DOWNLOAD -> R.drawable.ic_action_cancel
            BookAction.SAVE -> R.drawable.ic_action_save
            BookAction.FINISHED -> R.drawable.ic_action_finished
            BookAction.RESTART -> R.drawable.ic_action_restart
            BookAction.BOOKMARKS -> R.drawable.ic_action_bookmarks
            BookAction.DELETE_DOWNLOAD -> R.drawable.ic_action_delete
        }

    @StringRes
    private fun actionLabel(state: BookActionState): Int =
        when (state.action) {
            BookAction.READ -> R.string.book_files_read
            BookAction.DOWNLOAD -> R.string.book_action_download
            BookAction.CANCEL_DOWNLOAD -> R.string.book_files_cancel
            BookAction.SAVE -> if (state.selected) R.string.book_action_saved else R.string.book_action_save
            BookAction.FINISHED -> R.string.book_action_finished
            BookAction.RESTART -> R.string.book_action_restart
            BookAction.BOOKMARKS -> R.string.bookmarks_title
            BookAction.DELETE_DOWNLOAD -> R.string.book_action_delete
        }

    override fun onDestroyView() {
        shownBookId = null
        shownActions = null
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DISABLED_ALPHA = 0.38f
    }
}
