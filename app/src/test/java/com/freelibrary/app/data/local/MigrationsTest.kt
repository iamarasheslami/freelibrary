package com.freelibrary.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Stops a schema change from reaching a reader's phone without a migration. */
class MigrationsTest {
    private val schemas = File("schemas/com.freelibrary.app.data.local.FreeLibraryDatabase")

    private fun exportedVersions(): List<Int> {
        assertTrue("Schema folder not found: ${schemas.absolutePath}", schemas.isDirectory)
        return schemas.listFiles().orEmpty().mapNotNull { it.name.removeSuffix(".json").toIntOrNull() }.sorted()
    }

    @Test
    fun `the database version is the newest exported schema`() {
        assertEquals(DATABASE_VERSION, exportedVersions().max())
    }

    @Test
    fun `every version after the first released one has a migration from the version before it`() {
        for (version in FIRST_RELEASED_VERSION + 1..DATABASE_VERSION) {
            val steps = MIGRATIONS.filter { it.startVersion == version - 1 && it.endVersion == version }
            assertEquals("Exactly one migration from ${version - 1} to $version is needed", 1, steps.size)
        }
    }

    @Test
    fun `no migration starts before the first released version or goes past the current one`() {
        for (migration in MIGRATIONS) {
            assertTrue(migration.startVersion >= FIRST_RELEASED_VERSION)
            assertTrue(migration.endVersion <= DATABASE_VERSION)
        }
    }

    @Test
    fun `the schema of the first released version is kept`() {
        assertTrue(exportedVersions().contains(FIRST_RELEASED_VERSION))
    }
}
