package com.freelibrary.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
        private val attempt = MutableStateFlow(0)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<HomeUiState> =
            attempt
                .flatMapLatest { homeRepository.observeSections().asHomeUiStates() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Loading)

        /** Reads the lists again after a failure, for example once the reader has freed some storage. */
        fun retry() {
            attempt.value++
        }
    }
