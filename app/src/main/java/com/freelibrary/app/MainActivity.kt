package com.freelibrary.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.freelibrary.app.data.repository.CatalogSyncRunner
import com.freelibrary.app.data.repository.SettingsRepository
import com.freelibrary.app.databinding.ActivityMainBinding
import com.freelibrary.app.ui.theme.shouldUseTrueBlack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Single-activity host for the app's navigation graph. All actual screens are
 * fragments managed by the NavHostFragment declared in activity_main.xml.
 *
 * It also puts the true black overlay on its theme while dark mode is showing and the reader
 * chose true black, and recreates itself when that choice changes.
 * The first time it is created in a process it also starts the catalog sync in the background.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var catalogSyncRunner: CatalogSyncRunner
    private lateinit var binding: ActivityMainBinding
    private var usesTrueBlack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hilt injects the settings during super.onCreate, and the overlay has to be on the theme
        // before the layout is inflated. This is one small preferences read, like the one at app start.
        usesTrueBlack = shouldUseTrueBlack(darkIsShowing(), runBlocking { settingsRepository.trueBlack.first() })
        if (usesTrueBlack) theme.applyStyle(R.style.ThemeOverlay_FreeLibrary_TrueBlack, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        recreateWhenTrueBlackChanges()
        catalogSyncRunner.startOnce()
    }

    private fun recreateWhenTrueBlackChanges() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.trueBlack.collect { chosen ->
                    if (shouldUseTrueBlack(darkIsShowing(), chosen) != usesTrueBlack) recreate()
                }
            }
        }
    }

    private fun darkIsShowing(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
