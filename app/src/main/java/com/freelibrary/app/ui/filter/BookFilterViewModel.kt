package com.freelibrary.app.ui.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.BookFilterHolder
import com.freelibrary.app.data.repository.BookFilterOptionsRepository
import com.freelibrary.app.domain.model.BookFilter
import com.freelibrary.app.domain.model.BookFilterOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * The filter sheet. The reader edits a draft, which starts as the filter in force; nothing
 * changes in Explore or Search until [apply]. Closing the sheet without applying drops the draft.
 */
@HiltViewModel
class BookFilterViewModel
    @Inject
    constructor(
        private val holder: BookFilterHolder,
        optionsRepository: BookFilterOptionsRepository,
    ) : ViewModel() {
        private val _draft = MutableStateFlow(holder.filter.value)
        private val _options = MutableStateFlow<BookFilterOptions?>(null)

        val draft: StateFlow<BookFilter> = _draft.asStateFlow()

        /** The language in the draft as a set (empty for none), for the language picker. */
        val selectedLanguages: StateFlow<Set<String>> =
            draft.map { setOfNotNull(it.language) }.stateIn(viewModelScope, SharingStarted.Eagerly, setOfNotNull(draft.value.language))

        /** The genres in the draft, for the genre picker. */
        val selectedGenres: StateFlow<Set<String>> =
            draft.map { it.genres }.stateIn(viewModelScope, SharingStarted.Eagerly, draft.value.genres)

        /** The choices the catalog offers; null while they are still being read. */
        val options: StateFlow<BookFilterOptions?> = _options.asStateFlow()

        init {
            viewModelScope.launch {
                _options.value =
                    try {
                        optionsRepository.options()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        BookFilterOptions()
                    }
            }
        }

        /** Picks [code] as the language, or clears the language when it is already the one picked. */
        fun chooseLanguage(code: String) {
            _draft.update { it.copy(language = if (it.language == code) null else code) }
        }

        /** Takes the language choice back: Explore shows English again and Search every language. */
        fun clearLanguage() {
            _draft.update { it.copy(language = null) }
        }

        fun clearGenres() {
            _draft.update { it.copy(genres = emptySet()) }
        }

        fun toggleFormat(formatType: String) {
            _draft.update { it.copy(formats = it.formats.toggled(formatType)) }
        }

        fun toggleGenre(shelfName: String) {
            _draft.update { it.copy(genres = it.genres.toggled(shelfName)) }
        }

        /** Clears the draft. It reaches Explore and Search only when the reader taps Apply. */
        fun reset() {
            _draft.value = BookFilter()
        }

        fun apply() {
            holder.update(_draft.value)
        }

        private fun Set<String>.toggled(value: String): Set<String> = if (value in this) this - value else this + value
    }
