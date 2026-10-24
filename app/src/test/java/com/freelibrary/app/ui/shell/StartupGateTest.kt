package com.freelibrary.app.ui.shell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupGateTest {
    @Test
    fun `the gate is closed until the first page is ready`() {
        assertFalse(StartupGate().ready.value)
    }

    @Test
    fun `once ready it stays ready`() {
        val gate = StartupGate()

        gate.markReady()
        gate.markReady()

        assertTrue(gate.ready.value)
    }
}
