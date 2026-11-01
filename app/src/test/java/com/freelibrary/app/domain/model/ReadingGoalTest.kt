package com.freelibrary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingGoalTest {
    @Test
    fun `a whole number inside the limits is a goal`() {
        assertEquals(12, parseYearlyGoal("12"))
        assertEquals(12, parseYearlyGoal("  12 "))
        assertEquals(MIN_YEARLY_GOAL, parseYearlyGoal("1"))
        assertEquals(MAX_YEARLY_GOAL, parseYearlyGoal("365"))
    }

    @Test
    fun `anything else is not a goal`() {
        assertNull(parseYearlyGoal(null))
        assertNull(parseYearlyGoal(""))
        assertNull(parseYearlyGoal("0"))
        assertNull(parseYearlyGoal("366"))
        assertNull(parseYearlyGoal("-3"))
        assertNull(parseYearlyGoal("2.5"))
        assertNull(parseYearlyGoal("twelve"))
    }

    @Test
    fun `progress is the share of the goal finished and never more than all of it`() {
        assertEquals(0.25f, ReadingSummary(finishedThisYear = 3, goal = 12).progress, 0.0001f)
        assertEquals(1f, ReadingSummary(finishedThisYear = 20, goal = 12).progress, 0.0001f)
        assertEquals(0f, ReadingSummary(finishedThisYear = 5).progress, 0.0001f)
    }

    @Test
    fun `the goal is reached at exactly the goal and not before`() {
        assertFalse(ReadingSummary(finishedThisYear = 11, goal = 12).goalReached)
        assertTrue(ReadingSummary(finishedThisYear = 12, goal = 12).goalReached)
        assertFalse(ReadingSummary(finishedThisYear = 99).goalReached)
    }
}
