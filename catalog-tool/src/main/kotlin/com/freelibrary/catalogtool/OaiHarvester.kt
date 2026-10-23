package com.freelibrary.catalogtool

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val OAPEN_OAI_URL = "https://library.oapen.org/oai/request"

/** The "Books" community of OAPEN (as listed by the ListSets verb); its book chapters are a separate community. */
const val OAPEN_BOOKS_SET = "com_20.500.12657_5"
const val CATALOG_TOOL_USER_AGENT = "FreeLibrary-catalog-tool (https://github.com/iamarasheslami/freelibrary)"

private const val MAX_ATTEMPTS = 3
private const val MAX_PAGES = 5000

/** Thrown when a page of the harvest cannot be fetched after several attempts, or the harvest never ends. */
class OaiHarvestException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Harvests every book of an OAI-PMH repository page by page (ListRecords, xoai format), following the
 * resumption token until the server stops sending one, and hands each parsed page to [onPage]. A failed
 * page is retried a few times before the harvest gives up. Pauses [pauseMillis] between pages so the
 * server is not hammered. Returns the number of pages read.
 */
fun harvestOapen(
    baseUrl: String = OAPEN_OAI_URL,
    set: String = OAPEN_BOOKS_SET,
    pauseMillis: Long = 500,
    retryPauseMillis: Long = 2000,
    fetch: (String) -> ByteArray = { fetchBytes(it, CATALOG_TOOL_USER_AGENT) },
    onPage: (OaiPage) -> Unit,
): Int {
    var url = "$baseUrl?verb=ListRecords&metadataPrefix=xoai&set=${encode(set)}"
    var pages = 0
    while (true) {
        val page = fetchPage(url, retryPauseMillis, fetch)
        pages++
        onPage(page)
        val token = page.resumptionToken ?: return pages
        if (pages >= MAX_PAGES) throw OaiHarvestException("The harvest did not end after $MAX_PAGES pages")
        url = "$baseUrl?verb=ListRecords&resumptionToken=${encode(token)}"
        if (pauseMillis > 0) Thread.sleep(pauseMillis)
    }
}

private fun fetchPage(
    url: String,
    retryPauseMillis: Long,
    fetch: (String) -> ByteArray,
): OaiPage {
    var lastError: Exception? = null
    for (attempt in 1..MAX_ATTEMPTS) {
        try {
            return parseOaiPage(fetch(url))
        } catch (e: Exception) {
            lastError = e
            if (attempt < MAX_ATTEMPTS && retryPauseMillis > 0) Thread.sleep(retryPauseMillis * attempt)
        }
    }
    throw OaiHarvestException("Could not read $url after $MAX_ATTEMPTS attempts: ${lastError?.message}", lastError)
}

private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
