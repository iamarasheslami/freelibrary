package com.freelibrary.app.data.repository

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.Source
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExploreRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: ExploreRepository
    private var sourceId: Long = 0

    private suspend fun books(
        count: Int,
        language: String = "en",
        prefix: String = "b",
    ): List<Long> =
        (1..count).map { number ->
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = "$prefix$number",
                    title = "Book $prefix$number",
                    issuedDate = null,
                    primaryLanguage = language,
                    locc = null,
                    lastModified = "2026-10-07",
                ),
            )
        }

    private suspend fun shelf(
        name: String,
        bookIds: List<Long>,
    ) {
        val shelfId =
            database.bookshelfDao().findByName(name)?.id ?: database.bookshelfDao().insert(Bookshelf(name = name))
        bookIds.forEach { database.bookBookshelfDao().insert(BookBookshelf(bookId = it, bookshelfId = shelfId)) }
    }

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository = ExploreRepository(database.bookListDao())
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `sections come from shelves with at least ten English books, collections first`() =
        runTest {
            val english = books(12)
            val french = books(12, language = "fr", prefix = "f")
            shelf("Category: Poetry", english)
            shelf("Punch", english.take(11))
            shelf("Tiny", english.take(3))
            shelf("Category: French", french)
            shelf("Harvard Classics", english.take(10))

            val sections = repository.sections()

            assertEquals(
                listOf("collection:harvard-classics", "genre:Category: Poetry", "shelf:Punch"),
                sections.map { it.id },
            )
            assertEquals(listOf(10, 12, 11), sections.map { it.bookCount })
        }

    @Test
    fun `the page loads ten sections first and more as the reader scrolls`() =
        runTest {
            val english = books(10)
            (1..35).forEach { shelf("Shelf %02d".format(it), english) }

            val firstPage = repository.sectionPages().asSnapshot()
            val scrolled = repository.sectionPages().asSnapshot { scrollTo(index = 25) }

            assertEquals((1..10).map { "shelf:Shelf %02d".format(it) }, firstPage.map { it.id })
            assertTrue("loaded only ${scrolled.size} sections", scrolled.size > 25)
            assertEquals(
                (1..scrolled.size).map { "shelf:Shelf %02d".format(it) },
                scrolled.map { it.id },
            )
        }

    @Test
    fun `the section list is kept until it is invalidated`() =
        runTest {
            val english = books(10)
            shelf("Category: Poetry", english)
            assertEquals(1, repository.sections().size)

            shelf("Category: Drama", english)
            assertEquals(1, repository.sections().size)

            repository.invalidate()

            assertEquals(2, repository.sections().size)
        }
}
