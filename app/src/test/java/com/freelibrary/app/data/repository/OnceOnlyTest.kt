package com.freelibrary.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class OnceOnlyTest {
    @Test
    fun `the block runs the first time`() {
        var runs = 0

        OnceOnly().runFirstTime { runs++ }

        assertEquals(1, runs)
    }

    @Test
    fun `the block never runs again`() {
        val once = OnceOnly()
        var runs = 0

        repeat(3) { once.runFirstTime { runs++ } }

        assertEquals(1, runs)
    }

    @Test
    fun `each guard has its own first time`() {
        var runs = 0

        OnceOnly().runFirstTime { runs++ }
        OnceOnly().runFirstTime { runs++ }

        assertEquals(2, runs)
    }
}
