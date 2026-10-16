package com.freelibrary.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.freelibrary.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The reader's app settings, stored locally with Preferences DataStore. The app language is
 * deliberately not stored here: Android's per-app language support keeps that choice itself.
 */
@Singleton
class SettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /** The stored settings. A failed read counts as nothing stored, so defaults apply. */
        private val storedPreferences: Flow<Preferences> =
            dataStore.data.catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

        /** The chosen theme; System until the reader picks one. */
        val themeMode: Flow<ThemeMode> =
            storedPreferences.map { preferences ->
                val stored = preferences[THEME_MODE_KEY]
                ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
            }

        /** Whether dark mode uses pure black surfaces, which saves power on OLED screens. Off until chosen. */
        val trueBlack: Flow<Boolean> =
            storedPreferences.map { preferences -> preferences[TRUE_BLACK_KEY] ?: false }

        /**
         * Whether the catalog syncs by itself only on a connection that is not metered (normally
         * Wi-Fi), so it never uses the reader's mobile data. On until the reader turns it off.
         */
        val wifiOnlySync: Flow<Boolean> =
            storedPreferences.map { preferences -> preferences[WIFI_ONLY_SYNC_KEY] ?: true }

        /** When the catalog last synced successfully, in epoch milliseconds; null if it never has. */
        val lastCatalogSyncAt: Flow<Long?> =
            storedPreferences.map { preferences -> preferences[LAST_CATALOG_SYNC_KEY] }

        suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode.name }
        }

        suspend fun setTrueBlack(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[TRUE_BLACK_KEY] = enabled }
        }

        suspend fun setWifiOnlySync(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[WIFI_ONLY_SYNC_KEY] = enabled }
        }

        suspend fun recordCatalogSync(atMillis: Long) {
            dataStore.edit { preferences -> preferences[LAST_CATALOG_SYNC_KEY] = atMillis }
        }

        private companion object {
            val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
            val TRUE_BLACK_KEY = booleanPreferencesKey("true_black")
            val WIFI_ONLY_SYNC_KEY = booleanPreferencesKey("wifi_only_sync")
            val LAST_CATALOG_SYNC_KEY = longPreferencesKey("last_catalog_sync_at")
        }
    }
