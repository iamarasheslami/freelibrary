package com.freelibrary.app.data.remote

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** How long an answer about the official site is trusted before it is checked again. */
internal const val HEALTH_VALID_MILLIS = 10L * 60 * 1000

/**
 * Whether gutenberg.org can be reached from where the phone is now, found out once by a small
 * request of its own and not by the covers that happen to be loading. A cover request is cancelled
 * whenever its picture is scrolled away or drawn again, so a blocked site would never be seen to fail
 * by the covers themselves; this check does not depend on them.
 *
 * The first time the answer is needed, [reachable] waits a few seconds for it, once. Later it
 * returns what is known at once, and asks again in the background when the answer is old.
 * [probe] returns whether the site answered at all, whatever the answer was.
 */
class OfficialHostHealth(
    private val probe: () -> Boolean,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val worker =
        Executors.newSingleThreadExecutor { task -> Thread(task, "official-host-probe").apply { isDaemon = true } }
    private val running = AtomicBoolean(false)
    private val firstAnswer = CountDownLatch(1)

    @Volatile
    private var answer: Boolean? = null

    @Volatile
    private var answeredAt = 0L

    /** Starts the check unless one is already running. It never blocks. */
    fun start() {
        if (!running.compareAndSet(false, true)) return
        worker.execute {
            try {
                record(runCatching(probe).getOrDefault(false))
            } finally {
                running.set(false)
            }
        }
    }

    /**
     * Whether to try the official site first. The very first call waits up to [waitMillis] for the
     * check; when there is still no answer it assumes the site works, which is the normal case.
     */
    fun reachable(waitMillis: Long): Boolean {
        if (answer == null) {
            start()
            firstAnswer.await(waitMillis, TimeUnit.MILLISECONDS)
        } else if (now() - answeredAt >= HEALTH_VALID_MILLIS) {
            start()
        }
        return answer ?: true
    }

    /** A real request to the official site could not connect. */
    fun reportFailure() = record(false)

    /** A real request to the official site was answered. */
    fun reportSuccess() = record(true)

    private fun record(reachable: Boolean) {
        answeredAt = now()
        answer = reachable
        firstAnswer.countDown()
    }
}
