package com.freelibrary.app.ui.home

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.BookListRepository
import com.freelibrary.app.data.repository.HomeRepository
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    private lateinit var database: FreeLibraryDatabase
    private var sourceId: Long = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FreeLibraryDatabase::class.java,
            ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        HomeViewModel(
            HomeRepository(
                BookListRepository(database.bookListDao()),
                ReadingStateRepository(database, database.bookShelfStateDao(), database.bookListDao(), TimeProvider { 0L }),
            ),
        )

    @Test
    fun `the screen starts in the loading state`() {
        assertEquals(HomeUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `once the sliders are known the screen shows them`() =
        runTest {
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = "1",
                    title = "Book 1",
                    issuedDate = "2026-09-01",
                    primaryLanguage = "en",
                    locc = null,
                    lastModified = "2026-10-04",
                ),
            )

            val state = viewModel().uiState.first { it is HomeUiState.Content }

            assertEquals(listOf("recently-added"), (state as HomeUiState.Content).sections.map { it.id })
        }
}
