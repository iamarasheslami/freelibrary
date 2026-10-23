package com.freelibrary.catalogtool

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

private const val BOOK_TYPE = "book"
private const val BUNDLE_ORIGINAL = "ORIGINAL"
private const val BUNDLE_THUMBNAIL = "THUMBNAIL"
private const val THEMA_PREFIX = "thema EDItEUR::"
private val WHITESPACE_RUN = Regex("\\s+")

/** Thrown when an OAI-PMH response cannot be read, or the server answers with an error. */
class OaiParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** One book taken from the feed, with the date it was last changed at the source (yyyy-MM-dd). */
data class OaiBook(
    val book: ParsedBook,
    val lastModified: String,
)

/**
 * One page of a ListRecords harvest. [skipped] counts the records that are not usable books (deleted,
 * not of type book, or without a direct PDF/EPUB link). [resumptionToken] is null on the last page.
 */
data class OaiPage(
    val books: List<OaiBook>,
    val skipped: Int,
    val resumptionToken: String?,
    val completeListSize: Int?,
)

/**
 * Parses one OAI-PMH ListRecords response in the xoai metadata format (as served by OAPEN) into books.
 * A record becomes a book only if it is not deleted, its dc.type is "book", it has a title and it has at
 * least one https PDF or EPUB file in its ORIGINAL bundle; every other record is only counted as skipped.
 */
fun parseOaiPage(xmlBytes: ByteArray): OaiPage {
    val document =
        try {
            buildOaiDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        } catch (e: Exception) {
            throw OaiParseException("Failed to parse OAI-PMH XML document", e)
        }

    val error = document.getElementsByTagName("error").item(0) as? Element
    if (error != null) {
        if (error.getAttribute("code") == "noRecordsMatch") {
            return OaiPage(books = emptyList(), skipped = 0, resumptionToken = null, completeListSize = null)
        }
        throw OaiParseException("The server answered with the OAI error ${error.getAttribute("code")}: ${error.textContent.trim()}")
    }

    val books = mutableListOf<OaiBook>()
    var skipped = 0
    val records = document.getElementsByTagName("record")
    for (index in 0 until records.length) {
        val book = parseRecord(records.item(index) as Element)
        if (book == null) skipped++ else books.add(book)
    }

    val tokenElement = document.getElementsByTagName("resumptionToken").item(0) as? Element
    return OaiPage(
        books = books,
        skipped = skipped,
        resumptionToken = tokenElement?.textContent?.trim()?.ifBlank { null },
        completeListSize = tokenElement?.getAttribute("completeListSize")?.toIntOrNull(),
    )
}

private fun buildOaiDocumentBuilder() =
    DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        isExpandEntityReferences = false
    }.newDocumentBuilder()

private fun parseRecord(record: Element): OaiBook? {
    val header = childElements(record).firstOrNull { it.tagName == "header" } ?: return null
    if (header.getAttribute("status") == "deleted") return null

    val identifier = childElements(header).firstOrNull { it.tagName == "identifier" }?.textContent?.trim() ?: return null
    val externalId = identifier.substringAfterLast("/").ifBlank { return null }
    val lastModified =
        childElements(header).firstOrNull { it.tagName == "datestamp" }?.textContent?.trim()?.substringBefore("T")
            ?.ifBlank { null } ?: return null

    val outer = childElements(record).firstOrNull { it.tagName == "metadata" } ?: return null
    val metadata = childElements(outer).firstOrNull { it.tagName == "metadata" } ?: return null
    val dc = xoai(metadata, "dc") ?: return null

    if (values(dc, "type").firstOrNull()?.lowercase() != BOOK_TYPE) return null
    val title = values(dc, "title").firstOrNull()?.let(::normalizeSpaces) ?: return null
    val formats = extractFormats(metadata)
    if (formats.isEmpty()) return null

    val authors = values(dc, "contributor", "author").ifEmpty { values(dc, "contributor", "editor") }

    return OaiBook(
        book =
            ParsedBook(
                externalId = externalId,
                title = title,
                issuedDate = values(dc, "date", "issued").firstOrNull(),
                language = values(dc, "language").firstNotNullOfOrNull { iso639ToTwoLetters(it) },
                locc = null,
                creators = authors.distinct().map { ParsedCreator(name = it, birthYear = null, deathYear = null) },
                subjects = emptyList(),
                bookshelves = extractBookshelves(dc),
                formats = formats,
                summary = values(dc, "description", "abstract").firstOrNull()?.let(::normalizeSpaces),
                coverUrl = extractCoverUrl(metadata),
            ),
        lastModified = lastModified,
    )
}

/** The broad subject area from each thema classification, e.g. "Society and Social Sciences". */
private fun extractBookshelves(dc: Element): List<String> =
    values(dc, "subject", "classification")
        .filter { it.startsWith(THEMA_PREFIX) }
        .mapNotNull { it.removePrefix(THEMA_PREFIX).split("::").getOrNull(0)?.substringAfter(" ", "")?.trim()?.ifBlank { null } }
        .distinct()

/** The https PDF and EPUB files of the book itself (not the extracted text, thumbnail or export files). */
private fun extractFormats(metadata: Element): List<ParsedFormat> =
    bitstreams(metadata, BUNDLE_ORIGINAL)
        .mapNotNull { bitstream ->
            val url = fieldValue(bitstream, "url")
            val mime = fieldValue(bitstream, "format")
            if (url != null && mime != null && url.startsWith("https://") && (mime == "application/pdf" || mime.contains("epub"))) {
                ParsedFormat(url = url, mimeType = mime)
            } else {
                null
            }
        }
        .distinctBy { it.url }

private fun extractCoverUrl(metadata: Element): String? =
    bitstreams(metadata, BUNDLE_THUMBNAIL)
        .firstNotNullOfOrNull { bitstream ->
            val url = fieldValue(bitstream, "url")
            val mime = fieldValue(bitstream, "format")
            if (url != null && url.startsWith("https://") && mime != null && mime.startsWith("image/")) url else null
        }

private fun bitstreams(
    metadata: Element,
    bundleName: String,
): List<Element> {
    val bundles = xoai(metadata, "bundles") ?: return emptyList()
    return childElements(bundles)
        .filter { fieldValue(it, "name") == bundleName }
        .flatMap { bundle ->
            val container = xoai(bundle, "bitstreams")
            if (container == null) emptyList() else childElements(container).filter { it.tagName == "element" }
        }
}

private fun childElements(parent: Element): List<Element> {
    val result = mutableListOf<Element>()
    var node: Node? = parent.firstChild
    while (node != null) {
        if (node.nodeType == Node.ELEMENT_NODE) result.add(node as Element)
        node = node.nextSibling
    }
    return result
}

private fun xoai(
    parent: Element,
    name: String,
): Element? = childElements(parent).firstOrNull { it.tagName == "element" && it.getAttribute("name") == name }

private fun fieldsNamed(
    element: Element,
    name: String,
): List<String> =
    childElements(element)
        .filter { it.tagName == "field" && it.getAttribute("name") == name }
        .map { it.textContent.trim() }
        .filter { it.isNotEmpty() }

private fun fieldValue(
    element: Element,
    name: String,
): String? = fieldsNamed(element, name).firstOrNull()

/** Every "value" below the element reached by [path], across all its language variants ("none", "en", ...). */
private fun values(
    root: Element,
    vararg path: String,
): List<String> {
    var current: Element? = root
    for (name in path) current = current?.let { xoai(it, name) }
    val target = current ?: return emptyList()
    return childElements(target).filter { it.tagName == "element" }.flatMap { fieldsNamed(it, "value") }
}

private fun normalizeSpaces(text: String): String = text.replace(WHITESPACE_RUN, " ").trim()

private val LANGUAGE_CODES =
    mapOf(
        "eng" to "en", "ger" to "de", "deu" to "de", "fre" to "fr", "fra" to "fr", "spa" to "es", "ita" to "it",
        "por" to "pt", "dut" to "nl", "nld" to "nl", "rus" to "ru", "ara" to "ar", "per" to "fa", "fas" to "fa",
        "chi" to "zh", "zho" to "zh", "jpn" to "ja", "kor" to "ko", "tur" to "tr", "pol" to "pl", "swe" to "sv",
        "dan" to "da", "nor" to "no", "fin" to "fi", "cze" to "cs", "ces" to "cs", "slo" to "sk", "slk" to "sk",
        "hun" to "hu", "gre" to "el", "ell" to "el", "heb" to "he", "hin" to "hi", "ind" to "id", "ukr" to "uk",
        "rum" to "ro", "ron" to "ro", "bul" to "bg", "hrv" to "hr", "srp" to "sr", "slv" to "sl", "cat" to "ca",
        "lat" to "la", "afr" to "af", "swa" to "sw", "tha" to "th", "vie" to "vi", "ben" to "bn", "urd" to "ur",
        "est" to "et", "lav" to "lv", "lit" to "lt", "ice" to "is", "isl" to "is", "wel" to "cy", "cym" to "cy",
        "baq" to "eu", "eus" to "eu", "glg" to "gl", "alb" to "sq", "sqi" to "sq", "mac" to "mk", "mkd" to "mk",
        "geo" to "ka", "kat" to "ka", "arm" to "hy", "hye" to "hy", "amh" to "am", "mal" to "ml", "tam" to "ta",
        "tel" to "te", "mar" to "mr", "nep" to "ne", "sin" to "si", "may" to "ms", "msa" to "ms", "tgl" to "tl",
        "mao" to "mi", "mri" to "mi", "bur" to "my", "mya" to "my", "khm" to "km", "lao" to "lo", "mon" to "mn",
        "yid" to "yi", "gle" to "ga", "ltz" to "lb", "bos" to "bs",
    )

/** Maps an ISO 639-2 three-letter language code to the two-letter code the app uses; null when unknown. */
fun iso639ToTwoLetters(code: String): String? = LANGUAGE_CODES[code.trim().lowercase()]
