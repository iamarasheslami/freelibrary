package com.freelibrary.app.data.repository

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * The calendar year and month that contain a moment, as start-inclusive, end-exclusive limits in
 * epoch milliseconds. The Gregorian calendar is used whatever the phone's language, because the
 * yearly goal is about the calendar year.
 */
data class ReadingPeriods(
    val year: Int,
    val yearStart: Long,
    val nextYearStart: Long,
    val monthStart: Long,
    val nextMonthStart: Long,
)

fun readingPeriods(
    nowMillis: Long,
    zone: TimeZone,
): ReadingPeriods {
    val now = GregorianCalendar(zone).apply { timeInMillis = nowMillis }
    val year = now.get(Calendar.YEAR)
    val month = now.get(Calendar.MONTH)
    val nextMonthYear = if (month == Calendar.DECEMBER) year + 1 else year
    val nextMonth = (month + 1) % MONTHS_IN_YEAR
    return ReadingPeriods(
        year = year,
        yearStart = startOf(year, Calendar.JANUARY, zone),
        nextYearStart = startOf(year + 1, Calendar.JANUARY, zone),
        monthStart = startOf(year, month, zone),
        nextMonthStart = startOf(nextMonthYear, nextMonth, zone),
    )
}

private const val MONTHS_IN_YEAR = 12

private fun startOf(
    year: Int,
    month: Int,
    zone: TimeZone,
): Long =
    GregorianCalendar(zone)
        .apply {
            clear()
            set(year, month, 1, 0, 0, 0)
        }.timeInMillis
