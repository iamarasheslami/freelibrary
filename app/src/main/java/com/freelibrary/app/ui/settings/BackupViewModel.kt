package com.freelibrary.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.backup.BackupExporter
import com.freelibrary.app.data.backup.BackupFormatException
import com.freelibrary.app.data.backup.BackupImporter
import com.freelibrary.app.data.backup.BackupInfo
import com.freelibrary.app.data.backup.BackupPart
import com.freelibrary.app.data.backup.ExportSummary
import com.freelibrary.app.data.backup.ImportSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/** Why a backup or restore did not finish. The screen turns each into a message. */
enum class BackupFailure {
    /** The chosen place or file could not be opened. */
    CANNOT_OPEN,

    /** The file is not a backup this app can read. */
    INVALID_FILE,

    /** Anything else, such as a full disk. */
    FAILED,
}

sealed interface BackupStatus {
    data object Idle : BackupStatus

    data object Working : BackupStatus

    data class ExportDone(val summary: ExportSummary) : BackupStatus

    /** A file was read far enough to show the reader which parts it holds. */
    data class Inspected(val info: BackupInfo) : BackupStatus

    data class ImportDone(val summary: ImportSummary) : BackupStatus

    data class Failed(val reason: BackupFailure) : BackupStatus
}

/**
 * Runs one backup or restore at a time. The screen passes a function that opens the file the
 * reader picked, so this class never touches Android's file APIs.
 */
@HiltViewModel
class BackupViewModel
    @Inject
    constructor(
        private val exporter: BackupExporter,
        private val importer: BackupImporter,
    ) : ViewModel() {
        private val mutableStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
        val status: StateFlow<BackupStatus> = mutableStatus.asStateFlow()

        fun export(
            parts: Set<BackupPart>,
            openOutput: () -> OutputStream?,
        ) = run {
            val output = withContext(Dispatchers.IO) { openOutput() } ?: throw IOException("The file could not be opened")
            BackupStatus.ExportDone(output.use { exporter.export(parts, it) })
        }

        /** Reads only the manifest, to learn which parts the file holds. */
        fun inspect(openInput: () -> InputStream?) =
            run {
                val input = withContext(Dispatchers.IO) { openInput() } ?: throw IOException("The file could not be opened")
                BackupStatus.Inspected(input.use { importer.inspect(it) })
            }

        fun import(
            parts: Set<BackupPart>,
            openInput: () -> InputStream?,
        ) = run {
            val input = withContext(Dispatchers.IO) { openInput() } ?: throw IOException("The file could not be opened")
            BackupStatus.ImportDone(input.use { importer.import(it, parts) })
        }

        /** The screen calls this once it has shown a result, so the result is not shown again. */
        fun resultShown() {
            if (mutableStatus.value !is BackupStatus.Working) mutableStatus.value = BackupStatus.Idle
        }

        private fun run(block: suspend () -> BackupStatus) {
            if (mutableStatus.value is BackupStatus.Working) return
            mutableStatus.value = BackupStatus.Working
            viewModelScope.launch {
                mutableStatus.value =
                    try {
                        block()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: BackupFormatException) {
                        BackupStatus.Failed(BackupFailure.INVALID_FILE)
                    } catch (e: IOException) {
                        BackupStatus.Failed(BackupFailure.CANNOT_OPEN)
                    } catch (e: Exception) {
                        BackupStatus.Failed(BackupFailure.FAILED)
                    }
            }
        }
    }
