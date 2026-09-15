package com.freelibrary.catalogtool

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

/** Thrown when the output SQLite database cannot be created or written to. */
class CatalogDatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Opens (creating if necessary) a SQLite database at [outputPath] and
 * executes [schemaStatements] against it in order, inside a single
 * transaction. The caller is responsible for closing the returned
 * [Connection] once all writing is complete.
 */
fun createDatabase(
    outputPath: Path,
    schemaStatements: List<String>,
): Connection {
    val connection =
        try {
            DriverManager.getConnection("jdbc:sqlite:$outputPath")
        } catch (e: Exception) {
            throw CatalogDatabaseException("Failed to open SQLite connection at $outputPath", e)
        }

    try {
        connection.autoCommit = false
        connection.createStatement().use { statement ->
            for (sql in schemaStatements) {
                statement.execute(sql)
            }
        }
        connection.commit()
    } catch (e: Exception) {
        connection.rollback()
        connection.close()
        throw CatalogDatabaseException("Failed to apply schema to $outputPath", e)
    }

    return connection
}
