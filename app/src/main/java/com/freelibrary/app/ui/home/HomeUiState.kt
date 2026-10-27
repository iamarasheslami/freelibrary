package com.freelibrary.app.ui.home

import com.freelibrary.app.domain.model.HomeSection

/** What the home screen shows: nothing while the lists load, then the sliders, or a message if the catalog cannot open. */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** The catalog database could not be opened, most often because there was no room to copy it on the first start. */
    data object Failed : HomeUiState

    data class Content(
        val sections: List<HomeSection>,
    ) : HomeUiState
}
