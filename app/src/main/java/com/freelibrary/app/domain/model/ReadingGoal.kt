package com.freelibrary.app.domain.model

/** The fewest books a yearly goal can ask for. */
const val MIN_YEARLY_GOAL = 1

/** The most books a yearly goal can ask for: about one a day. */
const val MAX_YEARLY_GOAL = 365

/** A yearly goal typed by the reader: a whole number from [MIN_YEARLY_GOAL] to [MAX_YEARLY_GOAL], else null. */
fun parseYearlyGoal(raw: String?): Int? = raw?.trim()?.toIntOrNull()?.takeIf { it in MIN_YEARLY_GOAL..MAX_YEARLY_GOAL }

/**
 * What the Profile page shows about the reader's year: books finished this calendar [year] and
 * this month, and the yearly [goal] if the reader set one.
 */
data class ReadingSummary(
    val year: Int = 0,
    val finishedThisYear: Int = 0,
    val finishedThisMonth: Int = 0,
    val goal: Int? = null,
) {
    /** True once the reader has finished as many books this year as the goal asks for. */
    val goalReached: Boolean get() = goal != null && finishedThisYear >= goal

    /** How much of the goal is done, from 0 to 1; 0 when there is no goal. */
    val progress: Float
        get() = if (goal == null) 0f else (finishedThisYear.toFloat() / goal).coerceIn(0f, 1f)
}
