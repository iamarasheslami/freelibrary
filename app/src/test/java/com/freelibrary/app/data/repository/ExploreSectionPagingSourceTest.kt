package com.freelibrary.app.data.repository

import androidx.paging.PagingSource
import com.freelibrary.app.domain.model.ExploreSection
import com.freelibrary.app.domain.model.ExploreSectionKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ExploreSectionPagingSourceTest {
    private fun section(name: String) =
        ExploreSection(
            id = "shelf:$name",
            bookshelfName = name,
            kind = ExploreSectionKind.SHELF,
            title = name,
            titleRes = null,
            bookCount = 10,
        )

    private val sections = List(12) { section("S$it") }

    private fun refresh(size: Int = 10) = PagingSource.LoadParams.Refresh<Int>(key = null, loadSize = size, placeholdersEnabled = false)

    @Test
    fun `results come back in the order of the sections whichever finishes first`() =
        runTest {
            val result =
                loadInParallel(sections.take(4)) { section ->
                    delay(if (section.bookshelfName == "S0") 50L else 5L)
                    section.bookshelfName
                }

            assertEquals(listOf("S0", "S1", "S2", "S3"), result)
        }

    @Test
    fun `no more than the limit run at the same time, and all of them run`() =
        runTest {
            val running = AtomicInteger()
            val highest = AtomicInteger()

            val result =
                loadInParallel(sections, limit = 3) { section ->
                    highest.accumulateAndGet(running.incrementAndGet()) { a, b -> maxOf(a, b) }
                    delay(10)
                    running.decrementAndGet()
                    section.bookshelfName
                }

            assertEquals(12, result.size)
            assertEquals(3, highest.get())
        }

    @Test
    fun `a page is cut from the list and turned into what the screen shows`() =
        runTest {
            val source = ExploreSectionPagingSource({ sections }) { page -> page.map { it.bookshelfName } }

            val first = source.load(refresh()) as PagingSource.LoadResult.Page
            val append = PagingSource.LoadParams.Append(key = first.nextKey!!, loadSize = 10, placeholdersEnabled = false)
            val second = source.load(append) as PagingSource.LoadResult.Page

            assertEquals(List(10) { "S$it" }, first.data)
            assertEquals(10, first.nextKey)
            assertEquals(listOf("S10", "S11"), second.data)
            assertEquals(null, second.nextKey)
        }

    @Test
    fun `a failing query becomes a load error and does not throw`() =
        runTest {
            val failure = IllegalStateException("shelf query failed")
            val source = ExploreSectionPagingSource<String>({ sections }) { throw failure }

            val result = source.load(refresh())

            assertTrue(result is PagingSource.LoadResult.Error)
            assertEquals(failure, (result as PagingSource.LoadResult.Error).throwable)
        }
}
