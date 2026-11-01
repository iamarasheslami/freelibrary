package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.ReadingSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class ReadingGoalRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var repository: ReadingGoalRepository
    private var sourceId = 0L
    private val periods = readingPeriods(NOW, TimeZone.getDefault())

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            settings = SettingsRepository(InMemoryPreferences())
            repository = ReadingGoalRepository(database.bookShelfStateDao(), settings, TimeProvider { NOW })
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun finishBook(
        externalId: String,
        finishedAt: Long,
        state: ShelfState = ShelfState.FINISHED,
    ) {
        val bookId =
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = externalId,
                    title = "Book $externalId",
                    issuedDate = null,
                    primaryLanguage = "en",
                    locc = null,
                    lastModified = "2026-10-17",
                ),
            )
        database.bookShelfStateDao().upsert(BookShelfState(bookId, state, finishedAt, finishedAt))
    }

    @Test
    fun `a reader who finished nothing has an empty year and no goal`() =
        runTest {
            assertEquals(ReadingSummary(periods.year, 0, 0, null), repository.summary.first())
        }

    @Test
    fun `only books finished in the year and the month are counted`() =
        runTest {
            finishBook("1", periods.monthStart)
            finishBook("2", periods.yearStart)
            finishBook("3", periods.yearStart - 1)
            finishBook("4", periods.nextYearStart)
            finishBook("5", periods.monthStart + 1, state = ShelfState.READING)

            val summary = repository.summary.first()

            assertEquals(2, summary.finishedThisYear)
            assertEquals(1, summary.finishedThisMonth)
        }

    @Test
    fun `the goal is saved and removed`() =
        runTest {
            repository.setGoal(12)
            assertEquals(12, repository.summary.first().goal)

            repository.setGoal(null)
            assertEquals(null, repository.summary.first().goal)
        }

    private companion object {
        const val NOW = 1_790_000_000_000L
    }
}
