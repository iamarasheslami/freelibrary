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
import com.freelibrary.app.data.download.BookFiles
import com.freelibrary.app.data.download.DownloadError
import com.freelibrary.app.data.download.DownloadProgress
import com.freelibrary.app.data.download.Transfer
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.databinding.FragmentBookPageBinding
import com.freelibrary.app.domain.model.BookFileFormat
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.cover.loadCover
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * A book's own page: cover, title, authors, language, where the book sits on the reader's shelves
 * with the buttons to change that, the book's files with the buttons to download, cancel and delete
 * them, and the catalog summary. Reading comes with the reader, so there is no Read button yet.
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

    private fun showFiles(files: BookFiles?) {
        binding.bookPageFiles.isVisible = files != null
        if (files == null) return
        val transfer = files.transfer
        val busy = files.busy

        val status = filesStatus(files)
        binding.bookPageFilesStatus.text = status
        binding.bookPageFilesStatus.isVisible = status.isNotEmpty()
        showProgress(transfer)

        val target =
            (transfer as? Transfer.Failed)?.format?.takeIf { it in files.toDownload }
                ?: files.toDownload.firstOrNull()
        val download = binding.bookPageDownloadButton
        download.isVisible = !busy && target != null
        if (target != null) {
            download.text =
                if (transfer is Transfer.Failed) {
                    getString(R.string.book_files_retry)
                } else {
                    getString(R.string.book_files_download, getString(formatLabel(target)))
                }
            download.setOnClickListener { viewModel.onDownload(target) }
        }

        binding.bookPageOtherFormatsButton.isVisible = !busy && files.toDownload.size > 1
        binding.bookPageOtherFormatsButton.setOnClickListener { chooseFormatToDownload(files.toDownload) }
        binding.bookPageCancelDownloadButton.isVisible = busy
        binding.bookPageCancelDownloadButton.setOnClickListener { viewModel.onCancelDownload() }
        binding.bookPageDeleteDownloadButton.isVisible = !busy && files.downloaded.isNotEmpty()
        binding.bookPageDeleteDownloadButton.setOnClickListener { deleteDownload(files.downloaded) }
    }

    private fun filesStatus(files: BookFiles): String {
        val transfer = files.transfer
        return when {
            transfer is Transfer.Waiting -> getString(R.string.book_files_waiting)
            transfer is Transfer.Running -> runningText(transfer)
            transfer is Transfer.Failed -> getString(errorMessage(transfer.error))
            files.downloaded.isNotEmpty() ->
                getString(
                    R.string.book_files_downloaded,
                    files.downloaded.joinToString(", ") { getString(formatLabel(it)) },
                )
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

    private fun chooseFormatToDownload(formats: List<BookFileFormat>) {
        val labels = formats.map { getString(formatLabel(it)) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.book_files_choose_title)
            .setItems(labels) { _, index -> viewModel.onDownload(formats[index]) }
            .show()
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
