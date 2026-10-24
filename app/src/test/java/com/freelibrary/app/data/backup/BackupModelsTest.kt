package com.freelibrary.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupModelsTest {
    private val book = BookRef(source = "Project Gutenberg", externalId = "1342")

    @Test
    fun `a manifest survives a round trip and names the format and version`() {
        val manifest = BackupManifest(createdAt = 1_000L, parts = listOf(BackupPart.DATA, BackupPart.BOOKS))

        val text = BackupJson.encodeToString(BackupManifest.serializer(), manifest)
        val back = BackupJson.decodeFromString(BackupManifest.serializer(), text)

        assertEquals(manifest, back)
        assertEquals(BACKUP_FORMAT, back.format)
        assertEquals(BACKUP_VERSION, back.version)
    }

    @Test
    fun `the parts are written by name`() {
        val text = BackupJson.encodeToString(BackupManifest.serializer(), BackupManifest(createdAt = 1L, parts = BackupPart.entries))

        assertEquals(true, text.contains("\"DATA\"") && text.contains("\"SETTINGS\"") && text.contains("\"BOOKS\""))
    }

    @Test
    fun `the reader's data survives a round trip`() {
        val data =
            DataBackup(
                shelfStates = listOf(ShelfStateBackup(book, "READING", updatedAt = 5L)),
                readingProgress = listOf(ReadingProgressBackup(book, "0.42", lastOpenedAt = 6L)),
                bookmarks = listOf(BookmarkBackup(book, "0.1", note = "nice", createdAt = 7L)),
                highlights = listOf(HighlightBackup(book, "0.2", "0.3", color = "yellow", createdAt = 8L)),
            )

        val back = BackupJson.decodeFromString(DataBackup.serializer(), BackupJson.encodeToString(DataBackup.serializer(), data))

        assertEquals(data, back)
    }

    @Test
    fun `keys this version does not know are ignored`() {
        val text = """{"format":"freelibrary-backup","version":1,"createdAt":9,"parts":["DATA"],"somethingNew":true}"""

        val manifest = BackupJson.decodeFromString(BackupManifest.serializer(), text)

        assertEquals(listOf(BackupPart.DATA), manifest.parts)
    }

    @Test
    fun `missing lists and optional fields have defaults`() {
        val data = BackupJson.decodeFromString(DataBackup.serializer(), "{}")
        val bookmark =
            BackupJson.decodeFromString(
                BookmarkBackup.serializer(),
                """{"book":{"source":"s","externalId":"1"},"position":"p","createdAt":1}""",
            )

        assertEquals(DataBackup(), data)
        assertNull(bookmark.note)
    }

    @Test
    fun `settings and the book list survive a round trip`() {
        val settings = SettingsBackup(themeMode = "DARK", wifiOnlySync = false)
        val books = BooksBackup(listOf(BookFileBackup(book, format = "epub", entry = "books/1", downloadedAt = 3L)))

        assertEquals(
            settings,
            BackupJson.decodeFromString(SettingsBackup.serializer(), BackupJson.encodeToString(SettingsBackup.serializer(), settings)),
        )
        assertEquals(
            books,
            BackupJson.decodeFromString(BooksBackup.serializer(), BackupJson.encodeToString(BooksBackup.serializer(), books)),
        )
    }
}
