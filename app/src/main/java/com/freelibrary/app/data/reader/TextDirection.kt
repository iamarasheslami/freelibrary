package com.freelibrary.app.data.reader

/** Which way a language is written. */
internal object TextDirection {
    private val RIGHT_TO_LEFT =
        setOf("ar", "arc", "ckb", "dv", "fa", "he", "iw", "prs", "ps", "sd", "ug", "ur", "yi")

    /** True if [language] (a tag such as "ar" or "fa-IR"; case does not matter) is written from right to left. */
    fun isRightToLeft(language: String?): Boolean {
        val primary =
            language
                ?.trim()
                ?.lowercase()
                ?.split('-', '_')
                ?.firstOrNull()
                .orEmpty()
        return primary in RIGHT_TO_LEFT
    }
}
