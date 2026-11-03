package com.freelibrary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class ReadingStreakTest {
    private val today = 20_000L

    @Test
    fun `a reader who never read has no streak`() {
        assertEquals(ReadingStreak(0, 0, false), readingStreak(emptyList(), today))
    }

    @Test
    fun `days in a row up to today make the streak`() {
        assertEquals(ReadingStreak(3, 3, true), readingStreak(listOf(today - 2, today - 1, today), today))
    }

    @Test
    fun `the streak is still alive while today has not been read yet`() {
        assertEquals(ReadingStreak(2, 2, false), readingStreak(listOf(today - 2, today - 1), today))
    }

    @Test
    fun `a missed day ends the current streak but keeps the best one`() {
        assertEquals(ReadingStreak(0, 4, false), readingStreak(listOf(today - 8, today - 7, today - 6, today - 5, today - 2), today))
    }

    @Test
    fun `an older run does not count toward the current one`() {
        assertEquals(ReadingStreak(1, 3, true), readingStreak(listOf(today - 10, today - 9, today - 8, today), today))
    }

    @Test
    fun `order and repeated days do not matter`() {
        assertEquals(ReadingStreak(2, 2, true), readingStreak(listOf(today, today - 1, today, today - 1), today))
    }

    @Test
    fun `a day is counted on the phone's own calendar`() {
        val zone = TimeZone.getTimeZone("Asia/Tehran")
        val offset = zone.getOffset(0L)
        val midnightLocal = 100L * 86_400_000L - offset

        assertEquals(99L, localDay(midnightLocal - 1, zone))
        assertEquals(100L, localDay(midnightLocal, zone))
        assertEquals(100L, localDay(midnightLocal + 86_400_000L - 1, zone))
        assertEquals(101L, localDay(midnightLocal + 86_400_000L, zone))
    }
}
