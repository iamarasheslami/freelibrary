package com.freelibrary.catalogtool

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * Streams each individual RDF file's raw bytes out of the archive, one at a
 * time, without ever extracting the whole archive to disk. The archive
 * contains tens of thousands of entries, so holding them all in memory or
 * writing them all out individually would be wasteful.
 *
 * [onRdfFile] is invoked once per RDF entry found, receiving its raw content
 * as a byte array. Non-RDF entries (directories, non-.rdf files) are skipped.
 */
fun readRdfEntriesFromArchive(
    archivePath: Path,
    onRdfFile: (entryName: String, content: ByteArray) -> Unit,
) {
    Files.newInputStream(archivePath).use { fileStream ->
        BufferedInputStream(fileStream).use { bufferedStream ->
            BZip2CompressorInputStream(bufferedStream).use { bzip2Stream ->
                TarArchiveInputStream(bzip2Stream).use { tarStream ->
                    processTarEntries(tarStream, onRdfFile)
                }
            }
        }
    }
}

private fun processTarEntries(
    tarStream: TarArchiveInputStream,
    onRdfFile: (entryName: String, content: ByteArray) -> Unit,
) {
    var entry = tarStream.nextEntry
    while (entry != null) {
        if (!entry.isDirectory && entry.name.endsWith(".rdf")) {
            onRdfFile(entry.name, tarStream.readAllBytes())
        }
        entry = tarStream.nextEntry
    }
}

/**
 * Convenience overload for tests and quick inspection: counts RDF entries
 * without needing a real archive file on disk.
 */
fun countRdfEntries(inputStream: InputStream): Int {
    var count = 0
    BZip2CompressorInputStream(inputStream).use { bzip2Stream ->
        TarArchiveInputStream(bzip2Stream).use { tarStream ->
            var entry = tarStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".rdf")) {
                    count++
                }
                entry = tarStream.nextEntry
            }
        }
    }
    return count
}
