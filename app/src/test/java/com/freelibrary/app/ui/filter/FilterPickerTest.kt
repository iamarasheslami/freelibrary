package com.freelibrary.app.ui.filter

import com.freelibrary.app.domain.model.FilterOption
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterPickerTest {
    private val options =
        listOf(
            FilterOption("Category: Fiction", 900),
            FilterOption("Category: Poetry", 400),
            FilterOption("Category: Science Fiction", 120),
        )

    private fun rows(
        query: String,
        selected: Set<String> = emptySet(),
    ) = filterPickerRows(options, query, { it.value.removePrefix("Category: ") }, selected)

    @Test
    fun `nothing typed shows every option in the given order`() {
        assertEquals(listOf("Fiction", "Poetry", "Science Fiction"), rows("").map { it.label })
        assertEquals(listOf("Fiction", "Poetry", "Science Fiction"), rows("   ").map { it.label })
    }

    @Test
    fun `typing keeps the options whose name contains the text, ignoring case and spaces`() {
        assertEquals(listOf("Fiction", "Science Fiction"), rows("  FICT ").map { it.label })
        assertEquals(listOf("Poetry"), rows("poe").map { it.label })
    }

    @Test
    fun `text that matches nothing leaves no rows`() {
        assertEquals(emptyList<PickerRow>(), rows("zzz"))
    }

    @Test
    fun `chosen options are marked and the count comes along`() {
        val row = rows("poetry", selected = setOf("Category: Poetry")).single()

        assertEquals(PickerRow("Category: Poetry", "Poetry", 400, true), row)
    }
}
