package com.freelibrary.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.freelibrary.app.domain.model.UserProfile
import com.freelibrary.app.domain.model.cleanProfileName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** The reader's name, birthday and picture marker, stored locally with Preferences DataStore. */
@Singleton
class ProfileRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /** The stored profile. A failed read counts as nothing stored. */
        val profile: Flow<UserProfile> =
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .map { preferences ->
                    UserProfile(
                        name = preferences[NAME_KEY],
                        birthdayUtcMillis = preferences[BIRTHDAY_KEY],
                        avatarVersion = preferences[AVATAR_VERSION_KEY],
                    )
                }

        /** Saves the name (cleaned first; a blank one removes it) and the birthday (null removes it). */
        suspend fun saveDetails(
            name: String?,
            birthdayUtcMillis: Long?,
        ) {
            val cleaned = cleanProfileName(name)
            dataStore.edit { preferences ->
                if (cleaned == null) preferences.remove(NAME_KEY) else preferences[NAME_KEY] = cleaned
                if (birthdayUtcMillis == null) preferences.remove(BIRTHDAY_KEY) else preferences[BIRTHDAY_KEY] = birthdayUtcMillis
            }
        }

        /** Records that a picture was saved (or, with null, removed). */
        suspend fun setAvatarVersion(version: Long?) {
            dataStore.edit { preferences ->
                if (version == null) preferences.remove(AVATAR_VERSION_KEY) else preferences[AVATAR_VERSION_KEY] = version
            }
        }

        private companion object {
            val NAME_KEY = stringPreferencesKey("profile_name")
            val BIRTHDAY_KEY = longPreferencesKey("profile_birthday_utc_millis")
            val AVATAR_VERSION_KEY = longPreferencesKey("profile_avatar_version")
        }
    }
