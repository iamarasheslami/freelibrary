package com.freelibrary.app.domain.model

import java.util.TimeZone

private const val MILLIS_PER_DAY = 86_400_000L

/** The number of the calendar day that contains [millis] on the phone's calendar: whole days since 1 January 1970 in [zone]. */
fun localDay(
    millis: Long,
    zone: TimeZone,
): Long = Math.floorDiv(millis + zone.getOffset(millis), MILLIS_PER_DAY)

/**
 * The reader's run of reading days. [current] counts the days in a row up to today, or up to yesterday while today is still
 * to come, so a streak is never lost before the day is over; [longest] is the best run ever; [readToday] says whether today
 * is already counted.
 */
data class ReadingStreak(
    val current: Int = 0,
    val longest: Int = 0,
    val readToday: Boolean = false,
)

/** The streak for [days] (day numbers, any order, repeats allowed) as seen on day [today]. */
fun readingStreak(
    days: List<Long>,
    today: Long,
): ReadingStreak {
    val sorted = days.distinct().sorted()
    if (sorted.isEmpty()) return ReadingStreak()

    var longest = 1
    var run = 1
    for (i in 1 until sorted.size) {
        run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
        if (run > longest) longest = run
    }

    val last = sorted.last()
    val alive = last == today || last == today - 1
    return ReadingStreak(current = if (alive) run else 0, longest = longest, readToday = last == today)
}
