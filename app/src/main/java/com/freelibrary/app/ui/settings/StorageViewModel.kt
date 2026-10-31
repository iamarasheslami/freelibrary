package com.freelibrary.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.download.DownloadsStorage
import com.freelibrary.app.data.download.StoredDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** The downloaded books of the Storage page: [downloads] newest first, how many books they belong to and the room they take. */
data class StorageState(
    val downloads: List<StoredDownload> = emptyList(),
) {
    val bookCount: Int get() = downloads.map { it.bookId }.distinct().size
    val totalBytes: Long get() = downloads.sumOf { it.sizeBytes }
}

/** The Storage page's list of downloaded books, and removing one. */
@HiltViewModel
class StorageViewModel
    @Inject
    constructor(
        private val storage: DownloadsStorage,
    ) : ViewModel() {
        private val failures = Channel<Unit>(Channel.CONFLATED)

        /** Emits once for every removal that failed. */
        val deleteFailed: Flow<Unit> = failures.receiveAsFlow()

        val state: StateFlow<StorageState> =
            storage
                .observe()
                .map { StorageState(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StorageState())

        /** Removes [download] from the phone for good. */
        fun delete(download: StoredDownload) {
            viewModelScope.launch {
                try {
                    storage.delete(download)
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    failures.trySend(Unit)
                }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
