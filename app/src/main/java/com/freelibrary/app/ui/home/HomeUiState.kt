package com.freelibrary.app.ui.home

import com.freelibrary.app.domain.model.HomeSection

/** What the home screen shows: nothing yet while the first lists load, then the sliders. */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val sections: List<HomeSection>,
    ) : HomeUiState
}
