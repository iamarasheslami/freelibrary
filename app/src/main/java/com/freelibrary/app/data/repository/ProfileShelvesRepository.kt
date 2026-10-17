package com.freelibrary.app.data.repository

import com.freelibrary.app.R
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.domain.model.BookSlider
import com.freelibrary.app.domain.model.CatalogDefaults
import com.freelibrary.app.domain.model.ProfileSection
import com.freelibrary.app.domain.model.ViewMoreTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The three sliders on the Profile page: Continue Reading, Want to Read and Finished. They are
 * always there, empty or not, and are rebuilt when the reader changes a shelf.
 */
@Singleton
class ProfileShelvesRepository
    @Inject
    constructor(
        private val readingStateRepository: ReadingStateRepository,
    ) {
        val sections: Flow<List<ProfileSection>> =
            combine(
                slider(ShelfState.READING),
                slider(ShelfState.WANT_TO_READ),
                slider(ShelfState.FINISHED),
            ) { reading, wantToRead, finished ->
                listOf(
                    section(
                        "continue-reading",
                        R.string.home_continue_reading,
                        R.string.profile_empty_reading,
                        reading,
                        ViewMoreTarget.ContinueReading,
                    ),
                    section(
                        "want-to-read",
                        R.string.home_want_to_read,
                        R.string.profile_empty_want_to_read,
                        wantToRead,
                        ViewMoreTarget.WantToRead,
                    ),
                    section("finished", R.string.profile_finished, R.string.profile_empty_finished, finished, ViewMoreTarget.Finished),
                )
            }.distinctUntilChanged()

        private fun slider(state: ShelfState): Flow<BookSlider> =
            combine(
                readingStateRepository.observeShelf(state, CatalogDefaults.BOOKS_PER_SLIDER),
                readingStateRepository.observeCount(state),
            ) { books, total -> BookSlider(books, total) }

        private fun section(
            id: String,
            titleRes: Int,
            emptyRes: Int,
            slider: BookSlider,
            viewMore: ViewMoreTarget,
        ) = ProfileSection(id, titleRes, emptyRes, slider.books, slider.hasMore, viewMore)
    }
