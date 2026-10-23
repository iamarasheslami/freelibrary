package com.freelibrary.catalogtool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

private fun pageXml(token: String): ByteArray =
    (
        """<OAI-PMH><ListRecords><resumptionToken completeListSize="2" cursor="0">$token</resumptionToken></ListRecords></OAI-PMH>"""
    ).toByteArray()

class OaiHarvesterTest {
    @Test
    fun `the harvest follows the resumption token until the last page`() {
        val requested = mutableListOf<String>()
        val answers = ArrayDeque(listOf(pageXml("a/b c"), pageXml("")))
        var pages = 0

        val count =
            harvestOapen(
                baseUrl = "https://example.org/oai",
                set = "col_1",
                pauseMillis = 0,
                retryPauseMillis = 0,
                fetch = {
                    requested.add(it)
                    answers.removeFirst()
                },
                onPage = { pages++ },
            )

        assertEquals(2, count)
        assertEquals(2, pages)
        assertEquals(
            listOf(
                "https://example.org/oai?verb=ListRecords&metadataPrefix=xoai&set=col_1",
                "https://example.org/oai?verb=ListRecords&resumptionToken=a%2Fb+c",
            ),
            requested,
        )
    }

    @Test
    fun `a failed page is tried again`() {
        var calls = 0

        val count =
            harvestOapen(
                pauseMillis = 0,
                retryPauseMillis = 0,
                fetch = {
                    calls++
                    if (calls == 1) throw HttpFetchException("boom") else pageXml("")
                },
                onPage = {},
            )

        assertEquals(1, count)
        assertEquals(2, calls)
    }

    @Test
    fun `a page that keeps failing ends the harvest with an error`() {
        assertThrows(OaiHarvestException::class.java) {
            harvestOapen(pauseMillis = 0, retryPauseMillis = 0, fetch = { throw HttpFetchException("down") }, onPage = {})
        }
    }
}
