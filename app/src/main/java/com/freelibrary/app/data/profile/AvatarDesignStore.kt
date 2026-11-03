package com.freelibrary.app.data.profile

import android.content.Context
import com.freelibrary.app.domain.model.AvatarDesign
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers the last avatar the reader built, so the creator opens with it again. It is a few numbers in a small private
 * preferences file; the picture itself is kept by [AvatarStore].
 */
@Singleton
class AvatarDesignStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

        /** The last saved avatar, or null if there is none or the stored text is not a valid one. */
        fun load(): AvatarDesign? = AvatarDesign.decode(preferences.getString(KEY, null))

        fun save(design: AvatarDesign) {
            preferences.edit().putString(KEY, design.encode()).apply()
        }

        private companion object {
            const val FILE = "avatar_design"
            const val KEY = "design"
        }
    }
