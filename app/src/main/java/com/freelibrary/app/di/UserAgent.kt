package com.freelibrary.app.di

import android.content.Context

private const val PROJECT_URL = "https://github.com/iamarasheslami/freelibrary"

/** What every request of the app says about itself: its name, its version and where its code lives. */
fun appUserAgent(context: Context): String {
    val version =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
            .ifBlank { "dev" }
    return "FreeLibrary/$version (+$PROJECT_URL)"
}
