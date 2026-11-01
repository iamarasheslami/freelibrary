package com.freelibrary.app.data.reader

/** One font file the web view loads for a book: its name under the server's font folder, its weight range and whether it is italic. */
data class WebFont(
    val file: String,
    val weight: String,
    val italic: Boolean,
) {
    internal val format: String get() = if (file.endsWith(".otf", ignoreCase = true)) "opentype" else "truetype"
    internal val mimeType: String get() = if (file.endsWith(".otf", ignoreCase = true)) "font/otf" else "font/ttf"
}

/**
 * A font that replaces the one a book asks for. The web view has no JavaScript and no network, so the font files
 * are answered by [EpubPageServer] from inside the app, and the style sheet points at them.
 */
data class ReaderFont(
    val faces: List<WebFont>,
) {
    /** The rules that load [faces] under one family name and make the whole book use it, except code. */
    fun css(): String {
        val rules =
            faces.joinToString("\n") { face ->
                "@font-face { font-family: '$FAMILY'; font-style: ${if (face.italic) "italic" else "normal"}; " +
                    "font-weight: ${face.weight}; src: url('${EpubPageServer.FONT_URL}${face.file}') format('${face.format}'); }"
            }
        return rules + "\n" +
            "body, body * { font-family: '$FAMILY', serif !important; }\n" +
            "pre, pre *, code, kbd, samp, tt { font-family: monospace !important; }"
    }

    internal fun face(file: String): WebFont? = faces.firstOrNull { it.file == file }

    companion object {
        const val FAMILY = "FreeLibraryReader"
    }
}
