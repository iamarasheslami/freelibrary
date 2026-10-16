package com.freelibrary.app.data.repository

import com.freelibrary.app.domain.model.BookFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The filter the reader has chosen, shared by Explore and Search. It lives in memory only: it is
 * not saved, so every app start begins unfiltered.
 */
@Singleton
class BookFilterHolder
    @Inject
    constructor() {
        private val state = MutableStateFlow(BookFilter())

        val filter: StateFlow<BookFilter> = state.asStateFlow()

        fun update(filter: BookFilter) {
            state.value = filter
        }

        fun reset() {
            state.value = BookFilter()
        }
    }
