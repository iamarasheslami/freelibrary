package com.freelibrary.app.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.freelibrary.app.data.repository.SettingsRepository
import com.freelibrary.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the reader's chosen [ThemeMode] to the whole app through AppCompat's night mode.
 * AppCompat does not remember the mode between launches, so the stored choice has to be applied
 * again at every start.
 */
@Singleton
class ThemeModeApplier
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) {
        /**
         * Applies the stored theme before the first screen exists, so the app never opens in the
         * wrong theme and then recreates itself. It reads one small preferences file on the calling
         * thread on purpose: recreating a screen on a low-memory phone costs more than that read.
         */
        fun applyStored() {
            apply(runBlocking { settingsRepository.themeMode.first() })
        }

        /** Applies every later change of the stored theme. It collects forever, so run it in a scope. */
        suspend fun follow() {
            settingsRepository.themeMode.collect { apply(it) }
        }

        private fun apply(mode: ThemeMode) {
            AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
        }
    }

internal fun ThemeMode.toNightMode(): Int =
    when (this) {
        ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }
