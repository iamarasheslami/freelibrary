package com.freelibrary.app

import android.app.Application
import com.freelibrary.app.ui.theme.ThemeModeApplier
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation and set up the
 * application-level dependency container that all other components (activities,
 * fragments, ViewModels) will pull their dependencies from.
 *
 * It also applies the reader's chosen theme at every start, because AppCompat does not
 * remember it between launches.
 */
@HiltAndroidApp
class FreeLibraryApplication : Application() {
    @Inject
    lateinit var themeModeApplier: ThemeModeApplier

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        themeModeApplier.applyStored()
        applicationScope.launch { themeModeApplier.follow() }
    }
}
