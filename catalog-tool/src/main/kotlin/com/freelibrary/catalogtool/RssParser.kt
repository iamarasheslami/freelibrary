package com.freelibrary.catalogtool

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/** Thrown when the RSS feed is malformed or cannot be parsed. */
class RssParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Extracts the external book IDs referenced by Project Gutenberg's
 * today.rss feed - eBooks posted or updated in the last 24 hours. Each
 * item's <link> is of the form "https://www.gutenberg.org/ebooks/<id>";
 * the id is what this function returns, in feed order, deduplicated.
 */
fun parseRssBookIds(rssBytes: ByteArray): List<String> {
    val document =
        try {
            buildSecureDocumentBuilder().parse(ByteArrayInputStream(rssBytes))
        } catch (e: Exception) {
            throw RssParseException("Failed to parse RSS feed", e)
        }

    val linkNodes = document.getElementsByTagName("link")
    val ids = LinkedHashSet<String>()

    for (i in 0 until linkNodes.length) {
        val linkText = linkNodes.item(i).textContent?.trim() ?: continue
        val id = linkText.substringAfterLast("/ebooks/", missingDelimiterValue = "")
        if (id.isNotBlank() && id.all { it.isDigit() }) {
            ids.add(id)
        }
    }

    return ids.toList()
}

private fun buildSecureDocumentBuilder() =
    DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        isExpandEntityReferences = false
    }.newDocumentBuilder()
