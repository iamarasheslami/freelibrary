package com.freelibrary.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.freelibrary.app.data.repository.CatalogSyncRunner
import com.freelibrary.app.databinding.ActivityMainBinding
import com.freelibrary.app.ui.insets.withoutBottomSystemBar
import com.freelibrary.app.ui.shell.BottomBar
import com.freelibrary.app.ui.shell.MainTab
import com.freelibrary.app.ui.shell.SplashOverlay
import com.freelibrary.app.ui.shell.StartupGate
import com.freelibrary.app.ui.shell.mainTabFor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Single-activity host for the app's navigation graph. All actual screens are
 * fragments managed by the NavHostFragment declared in activity_main.xml. Below them sits the
 * bottom bar (Home, Explore, Profile), shown only on those three pages.
 *
 * Until Home has its first content a start-up screen with the logo covers the page, and then hands over
 * to it with a short animation. The first time it is created in a process it also starts the catalog sync in the background.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var catalogSyncRunner: CatalogSyncRunner

    @Inject
    lateinit var startupGate: StartupGate

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest gives the launch window its plain start-up theme; the real theme goes on before the layout exists.
        setTheme(R.style.Theme_FreeLibrary)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpBottomBar()
        setUpSplash()
        catalogSyncRunner.startOnce()
    }

    /** Keeps the start-up screen until Home is ready, but never longer than [SPLASH_TIMEOUT_MS]. */
    private fun setUpSplash() {
        val splash = SplashOverlay(binding.splashOverlay, binding.splashLogo, binding.appContent)
        if (startupGate.ready.value) {
            splash.hideNow()
            return
        }
        lifecycleScope.launch {
            withTimeoutOrNull(SPLASH_TIMEOUT_MS) { startupGate.ready.first { it } }
            splash.reveal()
        }
    }

    private fun setUpBottomBar() {
        val navController = (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).navController
        val bottomBar =
            BottomBar(
                bar = binding.bottomBar,
                tabs =
                    mapOf(
                        MainTab.HOME to binding.tabHome,
                        MainTab.EXPLORE to binding.tabExplore,
                        MainTab.PROFILE to binding.tabProfile,
                    ),
            ) { tab -> openTab(navController, tab) }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomBar.show(mainTabFor(destination.id))
            ViewCompat.requestApplyInsets(binding.root)
        }
        // The bar pads for the navigation bar itself, so while it shows the screens above it must not.
        val barPadding = binding.bottomBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.bottomBar.updatePadding(bottom = barPadding + bars.bottom)
            if (binding.bottomBar.isVisible) insets.withoutBottomSystemBar() else insets
        }
    }

    /** Switches to a tab's page, keeping what each tab had open. Tapping the tab already showing does nothing. */
    private fun openTab(
        navController: NavController,
        tab: MainTab,
    ) {
        if (navController.currentDestination?.id == tab.destinationId) return
        navController.navigate(
            tab.destinationId,
            null,
            navOptions {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            },
        )
    }

    private companion object {
        const val SPLASH_TIMEOUT_MS = 4_000L
    }
}
