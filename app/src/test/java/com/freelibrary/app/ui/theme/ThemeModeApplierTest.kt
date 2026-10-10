package com.freelibrary.app.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.freelibrary.app.data.repository.InMemoryPreferences
import com.freelibrary.app.data.repository.SettingsRepository
import com.freelibrary.app.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeModeApplierTest {
    private val settings = SettingsRepository(InMemoryPreferences())
    private val applier = ThemeModeApplier(settings)

    @After
    fun restoreNightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    @Test
    fun `each theme maps to the matching AppCompat night mode`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeMode.SYSTEM.toNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeMode.LIGHT.toNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.DARK.toNightMode())
    }

    @Test
    fun `the stored theme is applied at startup`() {
        runBlocking { settings.setThemeMode(ThemeMode.DARK) }

        applier.applyStored()

        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun `startup follows the device when no theme was stored`() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        applier.applyStored()

        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.getDefaultNightMode())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a theme chosen later is applied while following`() =
        runTest {
            backgroundScope.launch { applier.follow() }
            runCurrent()

            settings.setThemeMode(ThemeMode.LIGHT)
            runCurrent()
            assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())

            settings.setThemeMode(ThemeMode.DARK)
            runCurrent()
            assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
        }
}
