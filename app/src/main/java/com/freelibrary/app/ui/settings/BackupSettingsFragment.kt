package com.freelibrary.app.ui.settings

import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.data.backup.BackupInfo
import com.freelibrary.app.data.backup.BackupPart
import com.freelibrary.app.data.backup.ExportSummary
import com.freelibrary.app.data.backup.ImportSummary
import com.freelibrary.app.databinding.FragmentSettingsBackupBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The Backup page: export and import of the reader's data, with the choice of parts. */
@AndroidEntryPoint
class BackupSettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBackupBinding? = null
    private val binding get() = _binding!!
    private val backupViewModel: BackupViewModel by viewModels()

    /** What the reader chose before the file picker opened, kept across a rotation or a process restart. */
    private var pendingExportParts: Set<BackupPart> = emptySet()
    private var pendingImportUri: Uri? = null

    private val createBackupFile =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { target ->
            if (target != null && pendingExportParts.isNotEmpty()) {
                val resolver = requireContext().applicationContext.contentResolver
                backupViewModel.export(pendingExportParts) { resolver.openOutputStream(target) }
            }
        }

    private val openBackupFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { source ->
            if (source != null) {
                pendingImportUri = source
                val resolver = requireContext().applicationContext.contentResolver
                backupViewModel.inspect { resolver.openInputStream(source) }
            }
        }

    private companion object {
        const val STATE_EXPORT_PARTS = "backup_export_parts"
        const val STATE_IMPORT_URI = "backup_import_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { saved ->
            pendingExportParts =
                saved.getStringArray(STATE_EXPORT_PARTS).orEmpty()
                    .mapNotNull { name -> BackupPart.entries.firstOrNull { it.name == name } }
                    .toSet()
            pendingImportUri = saved.getString(STATE_IMPORT_URI)?.let(Uri::parse)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(STATE_EXPORT_PARTS, pendingExportParts.map { it.name }.toTypedArray())
        outState.putString(STATE_IMPORT_URI, pendingImportUri?.toString())
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pageRoot.applySystemBarInsetsAsPadding()
        binding.pageToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.backupExportButton.setOnClickListener { chooseExportParts() }
        binding.backupImportButton.setOnClickListener { openBackupFile.launch(arrayOf("*/*")) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                backupViewModel.status.collect { renderBackup(it) }
            }
        }
    }

    private fun chooseExportParts() {
        chooseParts(getString(R.string.backup_export_title), R.string.backup_export_action, BackupPart.entries) { parts ->
            pendingExportParts = parts
            val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            createBackupFile.launch("freelibrary-backup-$day.zip")
        }
    }

    private fun askWhatToRestore(info: BackupInfo) {
        val source = pendingImportUri ?: return
        if (info.parts.isEmpty()) {
            showMessage(getString(R.string.backup_error_empty))
            return
        }
        val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR
        val made = DateUtils.formatDateTime(requireContext(), info.createdAt, flags)
        chooseParts(getString(R.string.backup_import_title, made), R.string.backup_import_action, info.parts) { parts ->
            val resolver = requireContext().applicationContext.contentResolver
            backupViewModel.import(parts) { resolver.openInputStream(source) }
        }
    }

    /** A list of the parts to tick, all ticked to begin with. The action button stays off while none is ticked. */
    private fun chooseParts(
        title: String,
        actionRes: Int,
        available: List<BackupPart>,
        onChosen: (Set<BackupPart>) -> Unit,
    ) {
        val labels = available.map { getString(partLabel(it)) }.toTypedArray()
        val checked = BooleanArray(available.size) { true }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMultiChoiceItems(labels, checked) { dialog, which, isChecked ->
                checked[which] = isChecked
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = checked.any { it }
            }
            .setPositiveButton(actionRes) { _, _ -> onChosen(available.filterIndexed { index, _ -> checked[index] }.toSet()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun partLabel(part: BackupPart): Int =
        when (part) {
            BackupPart.DATA -> R.string.backup_part_data
            BackupPart.SETTINGS -> R.string.backup_part_settings
            BackupPart.BOOKS -> R.string.backup_part_books
        }

    private fun renderBackup(status: BackupStatus) {
        val working = status is BackupStatus.Working
        binding.backupExportButton.isEnabled = !working
        binding.backupImportButton.isEnabled = !working
        binding.backupProgress.visibility = if (working) View.VISIBLE else View.INVISIBLE
        when (status) {
            BackupStatus.Idle, BackupStatus.Working -> return
            is BackupStatus.Inspected -> askWhatToRestore(status.info)
            is BackupStatus.ExportDone -> showMessage(exportMessage(status.summary))
            is BackupStatus.ImportDone -> showMessage(importMessage(status.summary))
            is BackupStatus.Failed -> showMessage(getString(failureMessage(status.reason)))
        }
        backupViewModel.resultShown()
    }

    private fun exportMessage(summary: ExportSummary): String {
        val done = getString(R.string.backup_export_done, summary.dataItems, summary.bookFiles)
        return if (summary.missingBookFiles > 0) {
            done + "\n\n" + getString(R.string.backup_export_missing, summary.missingBookFiles)
        } else {
            done
        }
    }

    private fun importMessage(summary: ImportSummary): String {
        val lines = mutableListOf(getString(R.string.backup_import_done, summary.restoredItems, summary.bookFilesRestored))
        if (summary.settingsRestored) lines += getString(R.string.backup_import_settings)
        val skipped = summary.skippedItems + summary.bookFilesSkipped
        if (skipped > 0) lines += getString(R.string.backup_import_skipped, skipped)
        return lines.joinToString("\n\n")
    }

    private fun failureMessage(reason: BackupFailure): Int =
        when (reason) {
            BackupFailure.CANNOT_OPEN -> R.string.backup_error_open
            BackupFailure.INVALID_FILE -> R.string.backup_error_invalid
            BackupFailure.FAILED -> R.string.backup_error_failed
        }

    private fun showMessage(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
