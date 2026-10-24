package com.freelibrary.app.ui.theme

import android.content.Context
import android.content.res.Configuration
import com.freelibrary.app.domain.model.AppSize
import kotlin.math.roundToInt

/**
 * Scales the whole app by changing the screen density the activity sees: every dp and sp then
 * grows or shrinks together. The choice lives in a small preferences file of its own, not in the
 * settings store, because it has to be known before the activity (and so before dependency
 * injection) exists, and because it belongs to this screen: a backup made on one phone must not
 * resize another.
 */
object AppScale {
    private const val PREFERENCES = "display"
    private const val KEY_APP_SIZE = "app_size"

    /**
     * [base] with the screen density scaled to the chosen size. On the first call ever it picks the
     * size from the screen and remembers it, so the choice never changes by itself later.
     */
    fun wrap(base: Context): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.densityDpi = scaledDensityDpi(configuration.densityDpi, current(base))
        return base.createConfigurationContext(configuration)
    }

    /** The size in use: the stored choice, or the one picked for this screen on first use. */
    fun current(context: Context): AppSize {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val stored = AppSize.entries.firstOrNull { it.name == preferences.getString(KEY_APP_SIZE, null) }
        if (stored != null) return stored
        val picked = AppSize.forScreen(context.resources.configuration.smallestScreenWidthDp)
        save(context, picked)
        return picked
    }

    fun save(
        context: Context,
        size: AppSize,
    ) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(KEY_APP_SIZE, size.name).apply()
    }
}

internal fun scaledDensityDpi(
    densityDpi: Int,
    size: AppSize,
): Int = (densityDpi * size.scale).roundToInt()
