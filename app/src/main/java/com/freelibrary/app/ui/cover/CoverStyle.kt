package com.freelibrary.app.ui.cover

/** The decoration drawn faintly behind the text of a generated cover. */
enum class CoverMotif { SPINE, ARCS, DOTS, LINES }

/**
 * The look of a generated cover: a book-cloth background color and a motif. Both come only from
 * the book's key, so a book looks the same on every phone and after every catalog update.
 */
data class CoverStyle(
    val backgroundColor: Int,
    val motif: CoverMotif,
) {
    companion object {
        /** Title and author text. It keeps at least 4.5 to 1 contrast on every background. */
        const val TEXT_COLOR: Int = 0xFFFAF8F4.toInt()

        /** The small gold rule above the title. */
        const val RULE_COLOR: Int = 0xFFEAC06E.toInt()

        /**
         * Twelve book-cloth colors, in this order: teal, oxblood, navy, forest, plum, umber, slate,
         * rust, olive, indigo, charcoal, mustard.
         */
        internal val BACKGROUNDS =
            intArrayOf(
                0xFF0B5E55.toInt(),
                0xFF7A2431.toInt(),
                0xFF1F3A5F.toInt(),
                0xFF2D5A3D.toInt(),
                0xFF5B2A5C.toInt(),
                0xFF6B4423.toInt(),
                0xFF3E4C59.toInt(),
                0xFF9A3F1D.toInt(),
                0xFF5E5B20.toInt(),
                0xFF3B3F8C.toInt(),
                0xFF2E2E2E.toInt(),
                0xFF7A5A00.toInt(),
            )

        /** The style for a book, from a stable key such as its Gutenberg id. */
        fun forKey(key: String): CoverStyle {
            val mixed = mix(key.hashCode())
            val motifs = CoverMotif.entries
            return CoverStyle(
                backgroundColor = BACKGROUNDS[mixed.mod(BACKGROUNDS.size)],
                motif = motifs[(mixed ushr 8).mod(motifs.size)],
            )
        }

        /** Spreads similar keys, such as neighbouring ids, over the whole range (MurmurHash3 finalizer). */
        private fun mix(value: Int): Int {
            var h = value
            h = h xor (h ushr 16)
            h *= 0x85EBCA6B.toInt()
            h = h xor (h ushr 13)
            h *= 0xC2B2AE35.toInt()
            return h xor (h ushr 16)
        }
    }
}
