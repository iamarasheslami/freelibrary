package com.freelibrary.catalogtool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val SAMPLE_SCHEMA_JSON =
    """
    {
      "formatVersion": 1,
      "database": {
        "version": 9,
        "identityHash": "e8fc36ada50975fc80d66f821d141ad0",
        "entities": [
          {
            "tableName": "sources",
            "createSql": "CREATE TABLE IF NOT EXISTS `${'$'}{TABLE_NAME}` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `attribution` TEXT NOT NULL)",
            "fields": [],
            "primaryKey": { "autoGenerate": true, "columnNames": ["id"] }
          },
          {
            "tableName": "books",
            "createSql": "CREATE TABLE IF NOT EXISTS `${'$'}{TABLE_NAME}` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourceId` INTEGER NOT NULL, FOREIGN KEY(`sourceId`) REFERENCES `sources`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
            "fields": [],
            "primaryKey": { "autoGenerate": true, "columnNames": ["id"] },
            "indices": [
              {
                "name": "index_books_sourceId",
                "unique": false,
                "columnNames": ["sourceId"],
                "orders": [],
                "createSql": "CREATE INDEX IF NOT EXISTS `index_books_sourceId` ON `${'$'}{TABLE_NAME}` (`sourceId`)"
              }
            ],
            "foreignKeys": []
          }
        ],
        "setupQueries": [
          "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
          "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e8fc36ada50975fc80d66f821d141ad0')"
        ]
      }
    }
    """.trimIndent()

class SchemaLoaderTest {
    @Test
    fun `loadSchemaStatements substitutes the table name placeholder in create statements`() {
        val statements = loadSchemaStatements(SAMPLE_SCHEMA_JSON)

        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `sources`") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `books`") })
    }

    @Test
    fun `loadSchemaStatements includes index creation statements with the table name substituted`() {
        val statements = loadSchemaStatements(SAMPLE_SCHEMA_JSON)

        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_books_sourceId` ON `books`") })
    }

    @Test
    fun `loadSchemaStatements includes Room's setup queries for schema-identity validation`() {
        val statements = loadSchemaStatements(SAMPLE_SCHEMA_JSON)

        assertTrue(statements.any { it.contains("room_master_table") && it.contains("CREATE TABLE") })
        assertTrue(statements.any { it.contains("INSERT OR REPLACE INTO room_master_table") })
    }

    @Test
    fun `loadSchemaStatements preserves statement order - tables and indices before setup queries`() {
        val statements = loadSchemaStatements(SAMPLE_SCHEMA_JSON)

        val sourcesIndex = statements.indexOfFirst { it.contains("CREATE TABLE IF NOT EXISTS `sources`") }
        val setupIndex = statements.indexOfFirst { it.contains("room_master_table") }

        assertTrue(sourcesIndex < setupIndex)
        assertEquals(5, statements.size)
    }
}
