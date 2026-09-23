package com.freelibrary.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.CatalogSyncException
import com.freelibrary.app.data.repository.CatalogSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Temporary: triggers a real catalog sync on creation and exposes its
 * outcome as plain status text. This exists specifically to prove the full
 * Hilt -> Room (createFromAsset) -> Retrofit -> CatalogSyncRepository chain
 * actually works end to end on a real device, not just in unit tests. Real
 * home-screen UI (sliders, etc.) replaces this in Phase 2.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val syncRepository: CatalogSyncRepository,
    ) : ViewModel() {
        private val _syncStatus = MutableStateFlow("Starting catalog sync...")
        val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

        init {
            viewModelScope.launch {
                try {
                    val result = syncRepository.sync()
                    _syncStatus.value =
                        "Sync complete: ${result.added} added, ${result.updated} updated, ${result.failed} failed"
                } catch (e: CatalogSyncException) {
                    _syncStatus.value = "Sync failed: ${e.message}"
                }
            }
        }
    }
