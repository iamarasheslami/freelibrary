package com.freelibrary.app.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.profile.AvatarDecoder
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.data.repository.InMemoryPreferences
import com.freelibrary.app.data.repository.ProfileRepository
import com.freelibrary.app.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EditProfileViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var repository: ProfileRepository
    private lateinit var store: AvatarStore
    private var pictureReadable = true
    private val picture = Uri.parse("content://pictures/1")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = ProfileRepository(InMemoryPreferences())
        store =
            AvatarStore(
                File(temporaryFolder.root, "profile"),
                AvatarDecoder { _, _ -> if (pictureReadable) Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888) else null },
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = EditProfileViewModel(repository, store, TimeProvider { 5_000L })

    private suspend fun EditProfileViewModel.loaded(): EditProfileState = state.first { it.loaded }

    private suspend fun EditProfileViewModel.finished(): EditProfileState = state.first { it.saved || it.error != null }

    @Test
    fun `the saved profile is loaded and nothing is changed yet`() =
        runTest {
            repository.saveDetails("Arash", 1_000L)

            val state = viewModel().loaded()

            assertEquals("Arash", state.name)
            assertEquals(1_000L, state.birthdayUtcMillis)
            assertFalse(state.dirty)
        }

    @Test
    fun `typing a different name changes the form and typing the old one back does not`() =
        runTest {
            repository.saveDetails("Arash", null)
            val viewModel = viewModel()
            viewModel.loaded()

            viewModel.onNameChanged("Arash E")
            assertTrue(viewModel.state.value.dirty)

            viewModel.onNameChanged("Arash")
            assertFalse(viewModel.state.value.dirty)
        }

    @Test
    fun `saving stores the cleaned name and the birthday`() =
        runTest {
            val viewModel = viewModel()
            viewModel.loaded()

            viewModel.onNameChanged("  Arash   E ")
            viewModel.onBirthdayPicked(2_000L)
            viewModel.save()

            assertTrue(viewModel.finished().saved)
            assertEquals(UserProfile("Arash E", 2_000L, null), repository.profile.first())
        }

    @Test
    fun `a chosen picture is saved and its version is the time`() =
        runTest {
            val viewModel = viewModel()
            viewModel.loaded()

            viewModel.onPictureChosen(picture)
            viewModel.save()

            assertTrue(viewModel.finished().saved)
            assertNotNull(store.file())
            assertEquals(5_000L, repository.profile.first().avatarVersion)
        }

    @Test
    fun `a picture that cannot be read stops the whole save`() =
        runTest {
            pictureReadable = false
            val viewModel = viewModel()
            viewModel.loaded()

            viewModel.onNameChanged("New name")
            viewModel.onPictureChosen(picture)
            viewModel.save()

            assertEquals(EditProfileError.PICTURE_UNREADABLE, viewModel.finished().error)
            assertNull(repository.profile.first().name)
            assertNull(store.file())

            viewModel.errorShown()
            assertNull(viewModel.state.value.error)
        }

    @Test
    fun `removing the saved picture deletes it when saved`() =
        runTest {
            store.save(picture)
            repository.setAvatarVersion(1L)
            val viewModel = viewModel()
            viewModel.loaded()

            viewModel.onPictureRemoved()
            assertFalse(viewModel.state.value.hasPicture)
            assertTrue(viewModel.state.value.dirty)
            viewModel.save()

            assertTrue(viewModel.finished().saved)
            assertNull(store.file())
            assertNull(repository.profile.first().avatarVersion)
        }

    @Test
    fun `removing when there is no picture changes nothing`() =
        runTest {
            val viewModel = viewModel()
            viewModel.loaded()

            viewModel.onPictureRemoved()

            assertEquals(PictureDraft.Keep, viewModel.state.value.picture)
            assertFalse(viewModel.state.value.dirty)
        }

    @Test
    fun `a birthday in the future is ignored`() =
        runTest {
            val viewModel = viewModel()
            viewModel.loaded()

            viewModel.onBirthdayPicked(9_999_999L)

            assertNull(viewModel.state.value.birthdayUtcMillis)
            assertFalse(viewModel.state.value.dirty)
        }

    @Test
    fun `saving when nothing changed does nothing`() =
        runTest {
            val viewModel = viewModel()
            viewModel.loaded()

            viewModel.save()

            assertFalse(viewModel.state.value.saved)
            assertFalse(viewModel.state.value.saving)
        }
}
