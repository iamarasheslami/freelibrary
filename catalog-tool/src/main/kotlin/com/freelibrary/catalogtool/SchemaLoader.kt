package com.freelibrary.catalogtool

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Thrown when Room's exported schema JSON cannot be parsed or has an unexpected shape. */
class SchemaParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Parses Room's exported schema JSON (e.g. app/schemas/.../9.json) into an
 * ordered list of raw SQL statements that, executed in order against an
 * empty SQLite database, produce a database Room will accept as matching its
 * expected schema exactly - including the room_master_table identity-hash
 * bookkeeping Room uses to validate a database on open.
 *
 * This deliberately does not hand-model each table's structure: it executes
 * Room's own generated SQL verbatim, so it stays correct automatically as
 * tables are added or changed, without this tool needing to be kept in sync
 * by hand.
 */
fun loadSchemaStatements(schemaJson: String): List<String> {
    val root =
        try {
            Json.parseToJsonElement(schemaJson).jsonObject
        } catch (e: Exception) {
            throw SchemaParseException("Failed to parse schema JSON", e)
        }

    val database =
        root["database"]?.jsonObject
            ?: throw SchemaParseException("Schema JSON has no top-level 'database' object")
    val entities =
        database["entities"]?.jsonArray
            ?: throw SchemaParseException("Schema JSON's 'database' object has no 'entities' array")

    val statements = mutableListOf<String>()

    for (entityElement in entities) {
        val entity = entityElement.jsonObject
        val tableName =
            entity["tableName"]?.jsonPrimitive?.content
                ?: throw SchemaParseException("An entity is missing 'tableName'")
        val createSql =
            entity["createSql"]?.jsonPrimitive?.content
                ?: throw SchemaParseException("Entity '$tableName' is missing 'createSql'")

        statements.add(substituteTableName(createSql, tableName))

        val indices = entity["indices"]?.jsonArray ?: emptyList()
        for (indexElement in indices) {
            val indexCreateSql =
                indexElement.jsonObject["createSql"]?.jsonPrimitive?.content
                    ?: throw SchemaParseException("An index on '$tableName' is missing 'createSql'")
            statements.add(substituteTableName(indexCreateSql, tableName))
        }
    }

    val setupQueries =
        database["setupQueries"]?.jsonArray
            ?: throw SchemaParseException("Schema JSON's 'database' object has no 'setupQueries' array")
    for (queryElement in setupQueries) {
        statements.add(queryElement.jsonPrimitive.content)
    }

    return statements
}

private fun substituteTableName(
    sql: String,
    tableName: String,
): String {
    return sql.replace("\${TABLE_NAME}", tableName)
}
