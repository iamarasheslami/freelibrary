package com.freelibrary.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.SearchRepository
import com.freelibrary.app.data.repository.SearchResult
import com.freelibrary.shared.SearchScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** What the search screen shows below the query field. */
sealed interface SearchState {
    /** Nothing typed yet. */
    data object Idle : SearchState

    /** A search is running; the previous results stay on screen meanwhile. */
    data object Loading : SearchState

    data object NoResults : SearchState

    data object Failed : SearchState

    data class Results(
        val results: List<SearchResult>,
    ) : SearchState
}

/**
 * Title and author search. The search starts [DEBOUNCE_MS] after the reader stops typing; a blank
 * query clears the results at once; switching between Title and Author searches again at once.
 * The query and the choice survive the screen being recreated.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _query = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
        private val _scope =
            MutableStateFlow(
                SearchScope.entries.firstOrNull { it.name == savedStateHandle.get<String>(KEY_SCOPE) } ?: SearchScope.TITLE,
            )

        val query: StateFlow<String> = _query
        val scope: StateFlow<SearchScope> = _scope

        val state: StateFlow<SearchState> =
            combine(
                _query.map { it.trim() }.distinctUntilChanged().debounce { if (it.isEmpty()) 0L else DEBOUNCE_MS },
                _scope,
            ) { text, scope -> text to scope }
                .flatMapLatest { (text, scope) -> searchFlow(text, scope) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState.Idle)

        fun setQuery(text: String) {
            _query.value = text
            savedStateHandle[KEY_QUERY] = text
        }

        fun setScope(scope: SearchScope) {
            _scope.value = scope
            savedStateHandle[KEY_SCOPE] = scope.name
        }

        private fun searchFlow(
            text: String,
            scope: SearchScope,
        ): Flow<SearchState> =
            flow {
                if (text.isEmpty()) {
                    emit(SearchState.Idle)
                    return@flow
                }
                emit(SearchState.Loading)
                val finished =
                    try {
                        val results = searchRepository.search(text, scope)
                        if (results.isEmpty()) SearchState.NoResults else SearchState.Results(results)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        SearchState.Failed
                    }
                emit(finished)
            }

        companion object {
            const val KEY_QUERY = "query"
            const val KEY_SCOPE = "scope"
            const val DEBOUNCE_MS = 300L
        }
    }
