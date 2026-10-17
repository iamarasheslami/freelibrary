package com.freelibrary.app.data.repository

import com.freelibrary.app.domain.model.MAX_PROFILE_NAME_LENGTH
import com.freelibrary.app.domain.model.UserProfile
import com.freelibrary.app.domain.model.cleanProfileName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRepositoryTest {
    @Test
    fun `a new profile is empty`() =
        runTest {
            assertEquals(UserProfile(), ProfileRepository(InMemoryPreferences()).profile.first())
        }

    @Test
    fun `the name and birthday are stored`() =
        runTest {
            val repository = ProfileRepository(InMemoryPreferences())

            repository.saveDetails("  Arash   Eslami ", 631_152_000_000L)

            assertEquals(UserProfile("Arash Eslami", 631_152_000_000L, null), repository.profile.first())
        }

    @Test
    fun `a blank name and no birthday remove what was stored`() =
        runTest {
            val repository = ProfileRepository(InMemoryPreferences())
            repository.saveDetails("Arash", 1L)

            repository.saveDetails("   ", null)

            assertEquals(UserProfile(), repository.profile.first())
        }

    @Test
    fun `the picture marker is stored and removed`() =
        runTest {
            val repository = ProfileRepository(InMemoryPreferences())

            repository.setAvatarVersion(42L)
            assertEquals(42L, repository.profile.first().avatarVersion)

            repository.setAvatarVersion(null)
            assertNull(repository.profile.first().avatarVersion)
        }

    @Test
    fun `saving the details leaves the picture alone`() =
        runTest {
            val repository = ProfileRepository(InMemoryPreferences())
            repository.setAvatarVersion(7L)

            repository.saveDetails("Arash", null)

            assertEquals(7L, repository.profile.first().avatarVersion)
        }

    @Test
    fun `a name is cleaned and capped`() {
        assertEquals("A B", cleanProfileName("  A \n  B  "))
        assertNull(cleanProfileName("   "))
        assertNull(cleanProfileName(null))
        assertEquals(MAX_PROFILE_NAME_LENGTH, cleanProfileName("x".repeat(100))!!.length)
    }
}
