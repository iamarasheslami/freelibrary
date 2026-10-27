package com.freelibrary.app.ui.home

import com.freelibrary.app.domain.model.HomeSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * The home screen's states for these section lists: loading until the first list arrives, then the sliders.
 * If the lists cannot be read at all, which happens when the catalog database cannot be copied out of the app on
 * the first start because the phone is full, the flow ends with [HomeUiState.Failed] instead of crashing the app.
 */
fun Flow<List<HomeSection>>.asHomeUiStates(): Flow<HomeUiState> =
    map<List<HomeSection>, HomeUiState> { HomeUiState.Content(it) }
        .onStart { emit(HomeUiState.Loading) }
        .catch { emit(HomeUiState.Failed) }
