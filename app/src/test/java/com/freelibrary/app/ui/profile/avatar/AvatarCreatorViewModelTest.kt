package com.freelibrary.app.ui.profile.avatar

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.profile.AvatarDesignStore
import com.freelibrary.app.domain.model.AvatarDesign
import com.freelibrary.app.domain.model.AvatarSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AvatarCreatorViewModelTest {
    private class FakeDrafts(
        private val fail: Boolean = false,
    ) : AvatarDrafts {
        val written = mutableListOf<AvatarDesign>()

        override suspend fun write(design: AvatarDesign): Uri {
            if (fail) throw IOException("disk full")
            written += design
            return Uri.parse("file:///cache/avatar-1.png")
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val created = mutableListOf<AvatarCreatorViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context.getSharedPreferences("avatar_design", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        runBlocking { withTimeoutOrNull(5_000) { created.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } } }
        Dispatchers.resetMain()
    }

    private fun viewModel(drafts: AvatarDrafts = FakeDrafts()) =
        AvatarCreatorViewModel(AvatarDesignStore(context), drafts).also { created += it }

    @Test
    fun `a reader with no saved avatar starts with some valid design`() {
        val design = viewModel().state.value.design

        assertEquals(design, AvatarDesign.decode(design.encode()))
    }

    @Test
    fun `the creator opens with the last saved avatar`() {
        val saved = AvatarDesign.DEFAULT.with(AvatarSlot.HAIR, 6).with(AvatarSlot.BACKGROUND, 5)
        AvatarDesignStore(context).save(saved)

        assertEquals(saved, viewModel().state.value.design)
    }

    @Test
    fun `picking changes only that choice and the tab can be changed`() {
        val viewModel = viewModel()
        val before = viewModel.state.value.design

        viewModel.pick(AvatarSlot.MOUTH, 2)
        viewModel.selectTab(AvatarTab.FACE)

        val state = viewModel.state.value
        assertEquals(2, state.design[AvatarSlot.MOUTH])
        assertEquals(before[AvatarSlot.EYES], state.design[AvatarSlot.EYES])
        assertEquals(AvatarTab.FACE, state.tab)
    }

    @Test
    fun `randomize gives a valid design`() {
        val viewModel = viewModel()

        repeat(20) {
            viewModel.randomize()
            val design = viewModel.state.value.design
            assertEquals(design, AvatarDesign.decode(design.encode()))
        }
    }

    @Test
    fun `saving writes the picture, remembers the avatar and hands the picture over`() {
        val drafts = FakeDrafts()
        val viewModel = viewModel(drafts)
        viewModel.pick(AvatarSlot.SKIN, 3)

        viewModel.save()

        val state = viewModel.state.value
        assertNotNull(state.picture)
        assertFalse(state.saving)
        assertEquals(listOf(state.design), drafts.written)
        assertEquals(state.design, AvatarDesignStore(context).load())
    }

    @Test
    fun `a second save after the first finished does nothing`() {
        val drafts = FakeDrafts()
        val viewModel = viewModel(drafts)

        viewModel.save()
        viewModel.save()

        assertEquals(1, drafts.written.size)
    }

    @Test
    fun `a picture that cannot be written is reported and nothing is remembered`() {
        val viewModel = viewModel(FakeDrafts(fail = true))

        viewModel.save()

        val state = viewModel.state.value
        assertTrue(state.failed)
        assertNull(state.picture)
        assertFalse(state.saving)
        assertNull(AvatarDesignStore(context).load())

        viewModel.failureShown()
        assertFalse(viewModel.state.value.failed)
    }
}
