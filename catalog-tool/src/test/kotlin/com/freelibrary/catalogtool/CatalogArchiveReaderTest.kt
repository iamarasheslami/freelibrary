package com.freelibrary.catalogtool

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CatalogArchiveReaderTest {
    private fun buildFakeArchive(entries: Map<String, String>): ByteArrayInputStream {
        val byteStream = ByteArrayOutputStream()
        BZip2CompressorOutputStream(byteStream).use { bzip2Out ->
            TarArchiveOutputStream(bzip2Out).use { tarOut ->
                for ((name, content) in entries) {
                    val contentBytes = content.toByteArray()
                    val entry = TarArchiveEntry(name)
                    entry.size = contentBytes.size.toLong()
                    tarOut.putArchiveEntry(entry)
                    tarOut.write(contentBytes)
                    tarOut.closeArchiveEntry()
                }
            }
        }
        return ByteArrayInputStream(byteStream.toByteArray())
    }

    @Test
    fun `countRdfEntries counts only files ending in rdf`() {
        val archive =
            buildFakeArchive(
                mapOf(
                    "cache/epub/1/pg1.rdf" to "<rdf>fake content one</rdf>",
                    "cache/epub/2/pg2.rdf" to "<rdf>fake content two</rdf>",
                    "cache/epub/2/pg2.txt" to "not an rdf file",
                ),
            )

        val count = countRdfEntries(archive)

        assertEquals(2, count)
    }

    @Test
    fun `countRdfEntries returns zero for an archive with no rdf files`() {
        val archive =
            buildFakeArchive(
                mapOf("cache/epub/1/pg1.txt" to "just text"),
            )

        val count = countRdfEntries(archive)

        assertEquals(0, count)
    }
}
