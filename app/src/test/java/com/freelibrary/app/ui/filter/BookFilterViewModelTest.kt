package com.freelibrary.app.ui.filter

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.BookFilterHolder
import com.freelibrary.app.data.repository.BookFilterOptionsRepository
import com.freelibrary.app.domain.model.BookFilter
import com.freelibrary.app.domain.model.FilterOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
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
class BookFilterViewModelTest {
    private lateinit var database: FreeLibraryDatabase
    private val holder = BookFilterHolder()

    @Before
    fun setUp() =
        runTest {
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

    private fun viewModel() = BookFilterViewModel(holder, BookFilterOptionsRepository(database.bookListDao()))

    @Test
    fun `the draft starts as the filter in force`() {
        holder.update(BookFilter(language = "fr", formats = setOf("epub")))

        assertEquals(BookFilter(language = "fr", formats = setOf("epub")), viewModel().draft.value)
    }

    @Test
    fun `choosing a language picks it and choosing it again clears it`() {
        val viewModel = viewModel()

        viewModel.chooseLanguage("fr")
        assertEquals("fr", viewModel.draft.value.language)
        viewModel.chooseLanguage("de")
        assertEquals("de", viewModel.draft.value.language)
        viewModel.chooseLanguage("de")
        assertEquals(null, viewModel.draft.value.language)
    }

    @Test
    fun `formats and genres toggle on and off`() {
        val viewModel = viewModel()

        viewModel.toggleFormat("epub")
        viewModel.toggleFormat("pdf")
        viewModel.toggleFormat("epub")
        viewModel.toggleGenre("Category: Fiction")

        assertEquals(setOf("pdf"), viewModel.draft.value.formats)
        assertEquals(setOf("Category: Fiction"), viewModel.draft.value.genres)
    }

    @Test
    fun `nothing reaches the shared filter until apply`() {
        val viewModel = viewModel()

        viewModel.chooseLanguage("fr")
        assertEquals(BookFilter(), holder.filter.value)

        viewModel.apply()
        assertEquals(BookFilter(language = "fr"), holder.filter.value)
    }

    @Test
    fun `reset clears the draft but not the filter in force until apply`() {
        holder.update(BookFilter(language = "fr"))
        val viewModel = viewModel()

        viewModel.reset()
        assertEquals(BookFilter(), viewModel.draft.value)
        assertEquals(BookFilter(language = "fr"), holder.filter.value)

        viewModel.apply()
        assertEquals(BookFilter(), holder.filter.value)
    }

    @Test
    fun `the options come from the catalog`() =
        runTest {
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = "1",
                    title = "Livre",
                    issuedDate = null,
                    primaryLanguage = "fr",
                    locc = null,
                    lastModified = "2026-10-16",
                ),
            )

            val options = viewModel().options.filterNotNull().first()

            assertEquals(listOf(FilterOption("fr", 1)), options.languages)
        }

    @Test
    fun `the language and the genres can be cleared one kind at a time`() {
        val viewModel = viewModel()
        viewModel.chooseLanguage("fr")
        viewModel.toggleGenre("Category: Fiction")
        viewModel.toggleFormat("epub")

        viewModel.clearLanguage()
        assertEquals(null, viewModel.draft.value.language)
        assertEquals(setOf("Category: Fiction"), viewModel.draft.value.genres)

        viewModel.clearGenres()
        assertEquals(emptySet<String>(), viewModel.draft.value.genres)
        assertEquals(setOf("epub"), viewModel.draft.value.formats)
    }
}
