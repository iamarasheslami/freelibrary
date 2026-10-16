package com.freelibrary.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.CatalogSyncer
import com.freelibrary.app.data.repository.SettingsRepository
import com.freelibrary.app.domain.model.ThemeMode
import com.freelibrary.app.ui.cover.CoverCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Where the reader's own "Sync now" stands. */
sealed interface SyncStatus {
    data object Idle : SyncStatus

    data object Running : SyncStatus

    /** The sync ran; [booksChanged] books were added or updated (0 means the catalog was already up to date). */
    data class Done(
        val booksChanged: Int,
    ) : SyncStatus

    /** The sync could not run, normally because there is no connection. */
    data object Failed : SyncStatus
}

/** Everything the Settings screen shows. */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val trueBlack: Boolean = false,
    val wifiOnlySync: Boolean = true,
    val lastSyncAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val coverCacheBytes: Long = 0L,
)

/** The Settings screen: the stored choices, "Sync now" and the cover cache. */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val catalogSyncer: CatalogSyncer,
        private val coverCache: CoverCache,
    ) : ViewModel() {
        private val syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
        private val coverCacheBytes = MutableStateFlow(0L)

        private val stored =
            combine(
                settings.themeMode,
                settings.trueBlack,
                settings.wifiOnlySync,
                settings.lastCatalogSyncAt,
            ) { themeMode, trueBlack, wifiOnly, lastSyncAt ->
                SettingsState(themeMode = themeMode, trueBlack = trueBlack, wifiOnlySync = wifiOnly, lastSyncAt = lastSyncAt)
            }

        val state: StateFlow<SettingsState> =
            combine(stored, syncStatus, coverCacheBytes) { base, sync, cache ->
                base.copy(syncStatus = sync, coverCacheBytes = cache)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsState())

        init {
            refreshCoverCacheSize()
        }

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch { settings.setThemeMode(mode) }
        }

        fun setTrueBlack(enabled: Boolean) {
            viewModelScope.launch { settings.setTrueBlack(enabled) }
        }

        fun setWifiOnlySync(enabled: Boolean) {
            viewModelScope.launch { settings.setWifiOnlySync(enabled) }
        }

        /** Syncs the catalog now. A second tap while a sync is running is ignored. */
        fun syncNow() {
            if (syncStatus.value == SyncStatus.Running) return
            syncStatus.value = SyncStatus.Running
            viewModelScope.launch {
                val result =
                    try {
                        catalogSyncer.syncNow()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        null
                    }
                syncStatus.value = if (result == null) SyncStatus.Failed else SyncStatus.Done(result.added + result.updated)
            }
        }

        fun clearCoverCache() {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { coverCache.clear() }
                coverCacheBytes.value = withContext(Dispatchers.IO) { coverCache.sizeBytes() }
            }
        }

        private fun refreshCoverCacheSize() {
            viewModelScope.launch {
                coverCacheBytes.value = withContext(Dispatchers.IO) { coverCache.sizeBytes() }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
