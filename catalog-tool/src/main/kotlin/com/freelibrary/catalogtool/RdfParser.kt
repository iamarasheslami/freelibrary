package com.freelibrary.catalogtool

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

private const val NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
private const val NS_DCTERMS = "http://purl.org/dc/terms/"
private const val NS_PGTERMS = "http://www.gutenberg.org/2009/pgterms/"
private const val NS_DCAM = "http://purl.org/dc/dcam/"

/** Thrown when an RDF document is malformed in a way that cannot be recovered from. */
class RdfParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Parses a single Project Gutenberg RDF file's raw bytes into a [ParsedBook].
 * Returns null (not an error) for entries that are not presentable as a real
 * book: explicitly non-"Text" types (audio, image, dataset, etc.), entries
 * missing a title entirely, or entries with zero downloadable formats - all
 * observed in the real catalog, and none of them something this app could
 * meaningfully show a user.
 */
fun parseRdf(xmlBytes: ByteArray): ParsedBook? {
    val document =
        try {
            buildSecureDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        } catch (e: Exception) {
            throw RdfParseException("Failed to parse RDF XML document", e)
        }

    val ebookElement =
        document.getElementsByTagNameNS(NS_PGTERMS, "ebook").item(0) as? Element
            ?: throw RdfParseException("RDF document has no pgterms:ebook root element")

    val declaredType = extractDescriptionValue(ebookElement, NS_DCTERMS, "type")?.first
    if (declaredType != null && declaredType != "Text") {
        return null
    }

    val externalId =
        ebookElement.getAttributeNS(NS_RDF, "about")
            .substringAfterLast("/")
            .ifBlank { throw RdfParseException("Could not determine book id from rdf:about attribute") }

    val title = firstElementText(ebookElement, NS_DCTERMS, "title") ?: return null
    val formats = extractFormats(ebookElement)
    if (formats.isEmpty()) return null

    return ParsedBook(
        externalId = externalId,
        title = title,
        issuedDate = firstElementText(ebookElement, NS_DCTERMS, "issued"),
        language = extractDescriptionValue(ebookElement, NS_DCTERMS, "language")?.first,
        locc = extractLocc(ebookElement),
        creators = extractCreators(ebookElement),
        subjects = extractSubjects(ebookElement),
        bookshelves = extractBookshelves(ebookElement),
        formats = formats,
    )
}

private fun buildSecureDocumentBuilder() =
    DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        isExpandEntityReferences = false
    }.newDocumentBuilder()

private fun firstElementText(
    parent: Element,
    namespaceUri: String,
    localName: String,
): String? {
    val node = parent.getElementsByTagNameNS(namespaceUri, localName).item(0)
    return node?.textContent?.trim()?.ifBlank { null }
}

/**
 * Extracts the (value, memberOfResource) pair from the common
 * <ns:localName><rdf:Description><rdf:value>...</rdf:value>
 * [<dcam:memberOf rdf:resource="..."/>]</rdf:Description></ns:localName>
 * pattern used throughout Gutenberg's RDF for subject, language, and bookshelf.
 */
private fun extractDescriptionValue(
    parent: Element,
    namespaceUri: String,
    localName: String,
): Pair<String, String?>? {
    val wrapper = parent.getElementsByTagNameNS(namespaceUri, localName).item(0) as? Element ?: return null
    val description = wrapper.getElementsByTagNameNS(NS_RDF, "Description").item(0) as? Element ?: return null
    val value = description.getElementsByTagNameNS(NS_RDF, "value").item(0)?.textContent?.trim() ?: return null
    val memberOf =
        (description.getElementsByTagNameNS(NS_DCAM, "memberOf").item(0) as? Element)
            ?.getAttributeNS(NS_RDF, "resource")
    return value to memberOf
}

private fun extractLocc(ebookElement: Element): String? {
    val subjectNodes = ebookElement.getElementsByTagNameNS(NS_DCTERMS, "subject")
    for (i in 0 until subjectNodes.length) {
        val subjectElement = subjectNodes.item(i) as? Element ?: continue
        val description = subjectElement.getElementsByTagNameNS(NS_RDF, "Description").item(0) as? Element ?: continue
        val memberOf =
            (description.getElementsByTagNameNS(NS_DCAM, "memberOf").item(0) as? Element)
                ?.getAttributeNS(NS_RDF, "resource")
        if (memberOf?.endsWith("LCC") == true) {
            return description.getElementsByTagNameNS(NS_RDF, "value").item(0)?.textContent?.trim()
        }
    }
    return null
}

private fun extractSubjects(ebookElement: Element): List<String> {
    val subjects = mutableListOf<String>()
    val subjectNodes = ebookElement.getElementsByTagNameNS(NS_DCTERMS, "subject")
    for (i in 0 until subjectNodes.length) {
        val subjectElement = subjectNodes.item(i) as? Element ?: continue
        val description = subjectElement.getElementsByTagNameNS(NS_RDF, "Description").item(0) as? Element ?: continue
        val memberOf =
            (description.getElementsByTagNameNS(NS_DCAM, "memberOf").item(0) as? Element)
                ?.getAttributeNS(NS_RDF, "resource")
        if (memberOf?.endsWith("LCSH") == true) {
            description.getElementsByTagNameNS(NS_RDF, "value").item(0)?.textContent?.trim()?.let { subjects.add(it) }
        }
    }
    return subjects
}

private fun extractBookshelves(ebookElement: Element): List<String> {
    val bookshelves = mutableListOf<String>()
    val bookshelfNodes = ebookElement.getElementsByTagNameNS(NS_PGTERMS, "bookshelf")
    for (i in 0 until bookshelfNodes.length) {
        val bookshelfElement = bookshelfNodes.item(i) as? Element ?: continue
        val description = bookshelfElement.getElementsByTagNameNS(NS_RDF, "Description").item(0) as? Element ?: continue
        description.getElementsByTagNameNS(NS_RDF, "value").item(0)?.textContent?.trim()?.let { bookshelves.add(it) }
    }
    return bookshelves
}

private fun extractCreators(ebookElement: Element): List<ParsedCreator> {
    val creators = mutableListOf<ParsedCreator>()
    val agentNodes = ebookElement.getElementsByTagNameNS(NS_PGTERMS, "agent")
    for (i in 0 until agentNodes.length) {
        val agentElement = agentNodes.item(i) as? Element ?: continue
        val name = firstElementText(agentElement, NS_PGTERMS, "name") ?: continue
        val birthYear = firstElementText(agentElement, NS_PGTERMS, "birthdate")?.toIntOrNull()
        val deathYear = firstElementText(agentElement, NS_PGTERMS, "deathdate")?.toIntOrNull()
        creators.add(ParsedCreator(name = name, birthYear = birthYear, deathYear = deathYear))
    }
    return creators
}

private val SUPPORTED_FORMAT_MIME_MARKERS = listOf("epub", "text/plain", "application/pdf")

private fun extractFormats(ebookElement: Element): List<ParsedFormat> {
    val formats = mutableListOf<ParsedFormat>()
    val fileNodes = ebookElement.getElementsByTagNameNS(NS_PGTERMS, "file")
    for (i in 0 until fileNodes.length) {
        val fileElement = fileNodes.item(i) as? Element ?: continue
        val rawUrl = fileElement.getAttributeNS(NS_RDF, "about")
        if (rawUrl.isBlank()) continue
        val url = rawUrl
        val mimeType =
            extractDescriptionValue(fileElement, NS_DCTERMS, "format")?.first
                ?: firstElementText(fileElement, NS_DCTERMS, "format")
                ?: continue
        if (SUPPORTED_FORMAT_MIME_MARKERS.none { mimeType.contains(it) }) continue
        formats.add(ParsedFormat(url = url, mimeType = mimeType))
    }
    return formats
}
