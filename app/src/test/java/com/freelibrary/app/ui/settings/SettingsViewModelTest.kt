package com.freelibrary.app.ui.settings

import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.CatalogSyncer
import com.freelibrary.app.data.repository.InMemoryPreferences
import com.freelibrary.app.data.repository.ReadingHistory
import com.freelibrary.app.data.repository.SettingsRepository
import com.freelibrary.app.data.repository.SyncResult
import com.freelibrary.app.domain.model.ThemeMode
import com.freelibrary.app.ui.cover.CoverCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private class FakeSyncer(
        private val outcome: suspend () -> SyncResult?,
    ) : CatalogSyncer {
        var calls = 0

        override suspend fun syncNow(): SyncResult? {
            calls++
            return outcome()
        }
    }

    private class FakeCoverCache(
        var bytes: Long = 0L,
    ) : CoverCache {
        var clears = 0

        override fun sizeBytes(): Long = bytes

        override fun clear() {
            clears++
            bytes = 0L
        }
    }

    private class FakeHistory(
        private val failure: Exception? = null,
    ) : ReadingHistory {
        var clears = 0

        override suspend fun clear() {
            failure?.let { throw it }
            clears++
        }
    }

    private val settings = SettingsRepository(InMemoryPreferences())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private val created = mutableListOf<SettingsViewModel>()

    @After
    fun tearDown() {
        // Work still on its way (the cover cache size is read on another thread) must finish while the
        // main dispatcher is still set, or it fails in a later test.
        runBlocking { withTimeoutOrNull(5_000) { created.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } } }
        Dispatchers.resetMain()
    }

    /** A view model whose state is being watched, as the screen does, for the length of the test. */
    private fun TestScope.watched(
        syncer: CatalogSyncer = FakeSyncer { null },
        cache: CoverCache = FakeCoverCache(),
        history: ReadingHistory = FakeHistory(),
    ): SettingsViewModel {
        val viewModel = SettingsViewModel(settings, syncer, cache, history)
        created.add(viewModel)
        backgroundScope.launch { viewModel.state.collect {} }
        return viewModel
    }

    private fun result(
        added: Int = 0,
        updated: Int = 0,
    ) = SyncResult(added = added, updated = updated, failed = 0, failureSamples = emptyList())

    @Test
    fun `clearing the reading history clears it once and reports success`() =
        runTest {
            val history = FakeHistory()
            val viewModel = watched(history = history)

            viewModel.clearReadingHistory()

            assertEquals(1, history.clears)
            assertEquals(true, viewModel.historyCleared.first())
        }

    @Test
    fun `a reading history that could not be cleared is reported as failed`() =
        runTest {
            val viewModel = watched(history = FakeHistory(failure = IllegalStateException("disk full")))

            viewModel.clearReadingHistory()

            assertEquals(false, viewModel.historyCleared.first())
        }

    @Test
    fun `it starts dark, with Wi-Fi only off and no sync yet`() =
        runTest {
            val state = watched().state.value

            assertEquals(ThemeMode.DARK, state.themeMode)
            assertEquals(false, state.wifiOnlySync)
            assertNull(state.lastSyncAt)
        }

    @Test
    fun `the choices are saved and shown`() =
        runTest {
            val viewModel = watched()

            viewModel.setThemeMode(ThemeMode.LIGHT)
            viewModel.setWifiOnlySync(true)

            val state = viewModel.state.first { it.themeMode == ThemeMode.LIGHT && it.wifiOnlySync }
            assertEquals(ThemeMode.LIGHT, state.themeMode)
            assertEquals(ThemeMode.LIGHT, settings.themeMode.first())
        }

    @Test
    fun `the time of the last sync is shown`() =
        runTest {
            val viewModel = watched()

            settings.recordCatalogSync(5_000L)

            assertEquals(5_000L, viewModel.state.first { it.lastSyncAt != null }.lastSyncAt)
        }

    @Test
    fun `sync now shows running and then how many books changed`() =
        runTest {
            val gate = CompletableDeferred<SyncResult?>()
            val syncer = FakeSyncer { gate.await() }
            val viewModel = watched(syncer)

            viewModel.syncNow()
            viewModel.syncNow()

            assertEquals(SyncStatus.Running, viewModel.state.first { it.syncStatus == SyncStatus.Running }.syncStatus)
            assertEquals(1, syncer.calls)

            gate.complete(result(added = 2, updated = 1))

            assertEquals(SyncStatus.Done(3), viewModel.state.first { it.syncStatus is SyncStatus.Done }.syncStatus)
        }

    @Test
    fun `a sync that could not run is shown as failed`() =
        runTest {
            val viewModel = watched(FakeSyncer { null })

            viewModel.syncNow()

            assertEquals(SyncStatus.Failed, viewModel.state.first { it.syncStatus == SyncStatus.Failed }.syncStatus)
        }

    @Test
    fun `a sync that throws is shown as failed and does not crash`() =
        runTest {
            val viewModel = watched(FakeSyncer { error("boom") })

            viewModel.syncNow()

            assertEquals(SyncStatus.Failed, viewModel.state.first { it.syncStatus == SyncStatus.Failed }.syncStatus)
        }

    @Test
    fun `the cover cache size is shown and clearing empties it`() =
        runTest {
            val cache = FakeCoverCache(bytes = 4_096L)
            val viewModel = watched(cache = cache)

            assertEquals(4_096L, viewModel.state.first { it.coverCacheBytes == 4_096L }.coverCacheBytes)

            viewModel.clearCoverCache()

            assertEquals(0L, viewModel.state.first { it.coverCacheBytes == 0L }.coverCacheBytes)
            assertEquals(1, cache.clears)
        }
}
