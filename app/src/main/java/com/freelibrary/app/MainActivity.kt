package com.freelibrary.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.freelibrary.app.data.repository.CatalogSyncRunner
import com.freelibrary.app.databinding.ActivityMainBinding
import com.freelibrary.app.ui.insets.withoutBottomSystemBar
import com.freelibrary.app.ui.shell.BottomBar
import com.freelibrary.app.ui.shell.MainTab
import com.freelibrary.app.ui.shell.mainTabFor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity host for the app's navigation graph. All actual screens are
 * fragments managed by the NavHostFragment declared in activity_main.xml. Below them sits the
 * bottom bar (Home, Explore, Profile), shown only on those three pages.
 *
 * It also puts the true black overlay on its theme while dark mode is showing and the reader
 * chose true black, and recreates itself when that choice changes.
 * The first time it is created in a process it also starts the catalog sync in the background.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var catalogSyncRunner: CatalogSyncRunner

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpBottomBar()
        catalogSyncRunner.startOnce()
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
}
