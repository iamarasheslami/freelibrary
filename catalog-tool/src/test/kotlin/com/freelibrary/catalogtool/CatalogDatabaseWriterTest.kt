package com.freelibrary.catalogtool

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

private val SIMPLE_SCHEMA =
    listOf(
        "CREATE TABLE IF NOT EXISTS `sources` " +
            "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `attribution` TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `books` " +
            "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourceId` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
            "FOREIGN KEY(`sourceId`) REFERENCES `sources`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
        "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'test-hash')",
    )

class CatalogDatabaseWriterTest {
    private lateinit var tempDbPath: Path
    private var connection: Connection? = null

    @Before
    fun setUp() {
        tempDbPath = Files.createTempFile("catalog-test", ".sqlite")
        Files.delete(tempDbPath)
    }

    @After
    fun tearDown() {
        connection?.close()
        Files.deleteIfExists(tempDbPath)
    }

    @Test
    fun `createDatabase creates all tables from the schema statements`() {
        connection = createDatabase(tempDbPath, SIMPLE_SCHEMA)

        val tableNames = mutableListOf<String>()
        connection!!.metaData.getTables(null, null, "%", arrayOf("TABLE")).use { resultSet ->
            while (resultSet.next()) {
                tableNames.add(resultSet.getString("TABLE_NAME"))
            }
        }

        assertTrue(tableNames.contains("sources"))
        assertTrue(tableNames.contains("books"))
    }

    @Test
    fun `createDatabase applies Room's identity hash bookkeeping`() {
        connection = createDatabase(tempDbPath, SIMPLE_SCHEMA)

        connection!!.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT identity_hash FROM room_master_table")
            assertTrue(resultSet.next())
            assertEquals("test-hash", resultSet.getString("identity_hash"))
        }
    }

    @Test
    fun `createDatabase enforces the foreign key relationship between books and sources`() {
        connection = createDatabase(tempDbPath, SIMPLE_SCHEMA)

        connection!!.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("INSERT INTO sources (name, attribution) VALUES ('Test Source', 'Test Attribution')")
        }

        connection!!.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT id FROM sources")
            resultSet.next()
            val sourceId = resultSet.getLong("id")
            statement.execute("INSERT INTO books (sourceId, title) VALUES ($sourceId, 'Test Book')")
        }

        connection!!.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM books")
            resultSet.next()
            assertEquals(1, resultSet.getInt("count"))
        }
    }
}
