package com.freelibrary.app.ui.profile

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookmark
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.InMemoryPreferences
import com.freelibrary.app.data.repository.ProfileRepository
import com.freelibrary.app.data.repository.ProfileShelvesRepository
import com.freelibrary.app.data.repository.ProfileStatsRepository
import com.freelibrary.app.data.repository.ReadingStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProfileViewModelTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var profileRepository: ProfileRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FreeLibraryDatabase::class.java,
            ).allowMainThreadQueries().build()
        profileRepository = ProfileRepository(InMemoryPreferences())
        viewModel =
            ProfileViewModel(
                profileRepository,
                ProfileStatsRepository(database.bookmarkDao(), database.highlightDao()),
                ProfileShelvesRepository(
                    ReadingStateRepository(database, database.bookShelfStateDao(), database.bookListDao(), TimeProvider { 100L }),
                ),
            )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun addBook(): Long {
        val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        return database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = "1",
                title = "Book 1",
                issuedDate = null,
                primaryLanguage = "en",
                locc = null,
                lastModified = "2026-10-17",
            ),
        )
    }

    @Test
    fun `a new reader has no name no picture and three empty shelves`() =
        runTest {
            val state = viewModel.state.first { it.sections.isNotEmpty() }

            assertNull(state.name)
            assertNull(state.avatarVersion)
            assertEquals(0, state.counts.bookmarks)
            assertEquals(3, state.sections.size)
            assertEquals(true, state.sections.all { it.books.isEmpty() })
        }

    @Test
    fun `the name and picture marker follow what is saved`() =
        runTest {
            profileRepository.saveDetails("Arash", null)
            profileRepository.setAvatarVersion(9L)

            val state = viewModel.state.first { it.name != null && it.avatarVersion != null }

            assertEquals("Arash", state.name)
            assertEquals(9L, state.avatarVersion)
        }

    @Test
    fun `a saved book shows on the Want to Read shelf`() =
        runTest {
            val bookId = addBook()
            database.bookShelfStateDao().upsert(BookShelfState(bookId, ShelfState.WANT_TO_READ, updatedAt = 1L))

            val state = viewModel.state.first { s -> s.sections.any { it.id == "want-to-read" && it.books.isNotEmpty() } }

            assertEquals(listOf("Book 1"), state.sections.first { it.id == "want-to-read" }.books.map { it.title })
        }

    @Test
    fun `the bookmark total follows the reader's bookmarks`() =
        runTest {
            val bookId = addBook()
            database.bookmarkDao().insert(Bookmark(bookId = bookId, position = "0.1", note = null, createdAt = 1L))

            val state = viewModel.state.first { it.counts.bookmarks == 1 }

            assertEquals(0, state.counts.highlights)
        }
}
