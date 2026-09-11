package com.freelibrary.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation and set up the
 * application-level dependency container that all other components (activities,
 * fragments, ViewModels) will pull their dependencies from.
 */
@HiltAndroidApp
class FreeLibraryApplication : Application()