package com.freelibrary.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class OfficialHostHealthTest {
    @Test
    fun `the first call waits for the check and returns what it found`() {
        val health = OfficialHostHealth(probe = { false })

        assertFalse(health.reachable(waitMillis = 5_000))
    }

    @Test
    fun `a site that answers is reachable`() {
        val health = OfficialHostHealth(probe = { true })

        assertTrue(health.reachable(waitMillis = 5_000))
    }

    @Test
    fun `a check that blows up counts as not reachable`() {
        val health = OfficialHostHealth(probe = { error("no network") })

        assertFalse(health.reachable(waitMillis = 5_000))
    }

    @Test
    fun `when the check is slower than the wait the site is assumed to work`() {
        val release = CountDownLatch(1)
        val health = OfficialHostHealth(probe = { release.await(5, TimeUnit.SECONDS) })

        assertTrue(health.reachable(waitMillis = 20))
        release.countDown()
    }

    @Test
    fun `a known answer is returned at once`() {
        val health = OfficialHostHealth(probe = { true })

        health.reportFailure()
        assertFalse(health.reachable(waitMillis = 0))
        health.reportSuccess()
        assertTrue(health.reachable(waitMillis = 0))
    }

    @Test
    fun `an old answer is kept while the site is checked again in the background`() {
        var clock = 0L
        val checks = AtomicInteger()
        val checked = CountDownLatch(1)
        val health =
            OfficialHostHealth(
                probe = {
                    checks.incrementAndGet()
                    checked.countDown()
                    true
                },
                now = { clock },
            )
        health.reportFailure()
        clock += HEALTH_VALID_MILLIS

        assertFalse(health.reachable(waitMillis = 0))

        assertTrue(checked.await(5, TimeUnit.SECONDS))
        assertEquals(1, checks.get())
    }

    @Test
    fun `a fresh answer is not checked again`() {
        val checks = AtomicInteger()
        val health = OfficialHostHealth(probe = { checks.incrementAndGet() > 0 }, now = { 0L })
        health.reportSuccess()

        repeat(3) { health.reachable(waitMillis = 0) }
        Thread.sleep(100)

        assertEquals(0, checks.get())
    }
}
