package com.freelibrary.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * The home screen's state. The live queries keep running for a few seconds after the screen goes
 * away, so turning the phone does not start them all again.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        homeRepository: HomeRepository,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            homeRepository.observeSections()
                .map<_, HomeUiState> { HomeUiState.Content(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Loading)
    }
