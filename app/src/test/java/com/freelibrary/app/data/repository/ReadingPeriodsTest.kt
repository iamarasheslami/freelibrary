package com.freelibrary.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class ReadingPeriodsTest {
    private val utc = TimeZone.getTimeZone("UTC")

    private fun millis(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
    ): Long =
        GregorianCalendar(zone)
            .apply {
                clear()
                set(year, month, day, hour, 0, 0)
            }.timeInMillis

    @Test
    fun `a day in the middle of the year gives that year and month`() {
        val periods = readingPeriods(millis(utc, 2026, Calendar.MARCH, 15, 12), utc)

        assertEquals(2026, periods.year)
        assertEquals(millis(utc, 2026, Calendar.JANUARY, 1), periods.yearStart)
        assertEquals(millis(utc, 2027, Calendar.JANUARY, 1), periods.nextYearStart)
        assertEquals(millis(utc, 2026, Calendar.MARCH, 1), periods.monthStart)
        assertEquals(millis(utc, 2026, Calendar.APRIL, 1), periods.nextMonthStart)
    }

    @Test
    fun `December ends in January of the next year`() {
        val periods = readingPeriods(millis(utc, 2026, Calendar.DECEMBER, 31, 23), utc)

        assertEquals(millis(utc, 2026, Calendar.DECEMBER, 1), periods.monthStart)
        assertEquals(millis(utc, 2027, Calendar.JANUARY, 1), periods.nextMonthStart)
    }

    @Test
    fun `the first moment of a year belongs to that year`() {
        val start = millis(utc, 2026, Calendar.JANUARY, 1)

        val periods = readingPeriods(start, utc)

        assertEquals(2026, periods.year)
        assertEquals(start, periods.yearStart)
    }

    @Test
    fun `the phone's own time zone decides where the year starts`() {
        val tehran = TimeZone.getTimeZone("Asia/Tehran")
        // 2026-12-31 22:00 UTC is already 2027-01-01 in Tehran (UTC+3:30).
        val periods = readingPeriods(millis(utc, 2026, Calendar.DECEMBER, 31, 22), tehran)

        assertEquals(2027, periods.year)
        assertEquals(millis(tehran, 2027, Calendar.JANUARY, 1), periods.yearStart)
    }
}
