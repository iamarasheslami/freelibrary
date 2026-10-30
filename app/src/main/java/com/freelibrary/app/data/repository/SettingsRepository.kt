package com.freelibrary.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.freelibrary.app.data.reader.ReaderStyle
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

        /** The chosen theme; Dark until the reader picks one. */
        val themeMode: Flow<ThemeMode> =
            storedPreferences.map { preferences ->
                val stored = preferences[THEME_MODE_KEY]
                ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.DARK
            }

        /**
         * Whether the catalog syncs by itself only on a connection that is not metered (normally
         * Wi-Fi), so it never uses the reader's mobile data. Off until the reader turns it on.
         */
        val wifiOnlySync: Flow<Boolean> =
            storedPreferences.map { preferences -> preferences[WIFI_ONLY_SYNC_KEY] ?: false }

        /** When the catalog last synced successfully, in epoch milliseconds; null if it never has. */
        val lastCatalogSyncAt: Flow<Long?> =
            storedPreferences.map { preferences -> preferences[LAST_CATALOG_SYNC_KEY] }

        /**
         * Whether the book [bookId] is read page by page (true, the default) or as a scroll. The choice belongs
         * to the book: only the books that were switched to scrolling are stored.
         */
        fun readerPaged(bookId: Long): Flow<Boolean> =
            storedPreferences.map { preferences ->
                bookId.toString() !in preferences[READER_SCROLL_BOOKS_KEY].orEmpty()
            }

        suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode.name }
        }

        suspend fun setWifiOnlySync(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[WIFI_ONLY_SYNC_KEY] = enabled }
        }

        suspend fun recordCatalogSync(atMillis: Long) {
            dataStore.edit { preferences -> preferences[LAST_CATALOG_SYNC_KEY] = atMillis }
        }

        /** The reader's text size in percent of the book's own size; 100 until the reader changes it. */
        val readerTextPercent: Flow<Int> =
            storedPreferences.map { preferences ->
                (preferences[READER_TEXT_PERCENT_KEY] ?: ReaderStyle.DEFAULT_TEXT_PERCENT)
                    .coerceIn(ReaderStyle.MIN_TEXT_PERCENT, ReaderStyle.MAX_TEXT_PERCENT)
            }

        suspend fun setReaderTextPercent(percent: Int) {
            val kept = percent.coerceIn(ReaderStyle.MIN_TEXT_PERCENT, ReaderStyle.MAX_TEXT_PERCENT)
            dataStore.edit { preferences -> preferences[READER_TEXT_PERCENT_KEY] = kept }
        }

        suspend fun setReaderPaged(
            bookId: Long,
            paged: Boolean,
        ) {
            dataStore.edit { preferences ->
                val scrolled = preferences[READER_SCROLL_BOOKS_KEY].orEmpty()
                preferences[READER_SCROLL_BOOKS_KEY] = if (paged) scrolled - bookId.toString() else scrolled + bookId.toString()
            }
        }

        private companion object {
            val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
            val WIFI_ONLY_SYNC_KEY = booleanPreferencesKey("wifi_only_sync")
            val LAST_CATALOG_SYNC_KEY = longPreferencesKey("last_catalog_sync_at")
            val READER_SCROLL_BOOKS_KEY = stringSetPreferencesKey("reader_scroll_books")
            val READER_TEXT_PERCENT_KEY = intPreferencesKey("reader_text_percent")
        }
    }
