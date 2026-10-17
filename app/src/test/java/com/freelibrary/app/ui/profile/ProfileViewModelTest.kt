package com.freelibrary.app.ui.profile

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.InMemoryPreferences
import com.freelibrary.app.data.repository.ProfileRepository
import com.freelibrary.app.data.repository.ProfileStatsRepository
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
                ProfileStatsRepository(database.bookShelfStateDao(), database.bookmarkDao(), database.highlightDao()),
            )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `a new reader has no name no picture and no books`() =
        runTest {
            val state = viewModel.state.first { it.stats == ProfileState().stats }

            assertNull(state.name)
            assertNull(state.avatarVersion)
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
    fun `the counts follow the reader's shelves`() =
        runTest {
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            val bookId =
                database.bookDao().insert(
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
            database.bookShelfStateDao().upsert(BookShelfState(bookId, ShelfState.READING, updatedAt = 1L))

            val state = viewModel.state.first { it.stats.reading == 1 }

            assertEquals(0, state.stats.finished)
        }
}
