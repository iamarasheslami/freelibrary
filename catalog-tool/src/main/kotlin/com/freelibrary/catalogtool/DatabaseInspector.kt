package com.freelibrary.catalogtool

import java.sql.Connection

/**
 * Reports per-table storage usage and row counts for [connection]'s
 * database, using SQLite's built-in dbstat virtual table. Diagnostic only -
 * not part of the actual catalog-generation pipeline, used to decide how to
 * reduce or restructure the bundled database's size.
 */
fun printDatabaseSizeReport(connection: Connection) {
    println("=== Database size report ===")

    connection.createStatement().use { statement ->
        val resultSet =
            statement.executeQuery(
                "SELECT name, SUM(pgsize) as totalBytes FROM dbstat GROUP BY name ORDER BY totalBytes DESC",
            )
        while (resultSet.next()) {
            val name = resultSet.getString("name")
            val bytes = resultSet.getLong("totalBytes")
            val megabytes = bytes / 1024.0 / 1024.0
            println("%-30s %10.2f MB".format(name, megabytes))
        }
    }

    println()
    println("=== Row counts ===")
    val tables =
        listOf(
            "books", "authors", "book_authors", "subjects", "book_subjects",
            "bookshelves", "book_bookshelves", "book_formats", "books_fts",
        )
    connection.createStatement().use { statement ->
        for (table in tables) {
            val resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM $table")
            resultSet.next()
            println("%-20s %d rows".format(table, resultSet.getInt("count")))
        }
    }

    println()
    println("=== Format type breakdown ===")
    connection.createStatement().use { statement ->
        val resultSet =
            statement.executeQuery(
                "SELECT formatType, COUNT(*) as count FROM book_formats GROUP BY formatType ORDER BY count DESC",
            )
        while (resultSet.next()) {
            println("%-20s %d".format(resultSet.getString("formatType"), resultSet.getInt("count")))
        }
    }
}
