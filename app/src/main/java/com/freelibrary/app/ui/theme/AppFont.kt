package com.freelibrary.app.ui.theme

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.annotation.FontRes
import com.freelibrary.app.R
import com.freelibrary.app.data.reader.ReaderFont
import com.freelibrary.app.data.reader.WebFont
import java.io.InputStream

/** One file of a font family as the EPUB web view loads it: [file] is its name in the server's font folder. */
class FontFace(
    val file: String,
    @FontRes val res: Int,
    val weight: String,
    val italic: Boolean,
)

/**
 * The fonts the reader can choose from, for plain text books and for EPUB books. [body] is the family (regular, italic and bold).
 * The names are the fonts' own names, so they are not translated.
 */
enum class AppFont(
    val id: String,
    val label: String,
    @FontRes val body: Int,
    private val faces: List<FontFace>,
) {
    LITERATA(
        "literata",
        "Literata",
        R.font.font_literata,
        variable(R.font.literata, R.font.literata_italic, "literata"),
    ),
    NOTO_SERIF(
        "noto_serif",
        "Noto Serif",
        R.font.font_notoserif,
        variable(R.font.notoserif, R.font.notoserif_italic, "notoserif"),
    ),
    LORA(
        "lora",
        "Lora",
        R.font.font_lora,
        variable(R.font.lora, R.font.lora_italic, "lora"),
    ),
    SOURCE_SERIF(
        "source_serif",
        "Source Serif 4",
        R.font.font_sourceserif4,
        variable(R.font.sourceserif4, R.font.sourceserif4_italic, "sourceserif4"),
    ),
    CRIMSON_PRO(
        "crimson_pro",
        "Crimson Pro",
        R.font.font_crimsonpro,
        variable(R.font.crimsonpro, R.font.crimsonpro_italic, "crimsonpro"),
    ),
    EB_GARAMOND(
        "eb_garamond",
        "EB Garamond",
        R.font.font_ebgaramond,
        variable(R.font.ebgaramond, R.font.ebgaramond_italic, "ebgaramond"),
    ),
    LIBRE_BASKERVILLE(
        "libre_baskerville",
        "Libre Baskerville",
        R.font.font_librebaskerville,
        variable(R.font.librebaskerville, R.font.librebaskerville_italic, "librebaskerville"),
    ),
    OPEN_SANS(
        "open_sans",
        "Open Sans",
        R.font.font_opensans,
        variable(R.font.opensans, R.font.opensans_italic, "opensans"),
    ),
    ATKINSON(
        "atkinson",
        "Atkinson Hyperlegible",
        R.font.font_atkinson,
        fixed(
            "atkinson",
            "ttf",
            R.font.atkinson_regular,
            R.font.atkinson_italic,
            R.font.atkinson_bold,
            R.font.atkinson_bold_italic,
        ),
    ),
    OPEN_DYSLEXIC(
        "open_dyslexic",
        "OpenDyslexic",
        R.font.font_opendyslexic,
        fixed(
            "opendyslexic",
            "otf",
            R.font.opendyslexic_regular,
            R.font.opendyslexic_italic,
            R.font.opendyslexic_bold,
            R.font.opendyslexic_bold_italic,
        ),
    ),
    ;

    /** The font as the EPUB style sheet names it. */
    fun forReader(): ReaderFont = ReaderFont(faces.map { WebFont(it.file, it.weight, it.italic) })

    /**
     * The bytes of the font file the style sheet asked for by [file], or null if this font has no such file. A font file in
     * res/font is a plain file that [Resources.openRawResource] can read, though lint expects a raw resource id there.
     */
    @SuppressLint("ResourceType")
    fun open(
        resources: Resources,
        file: String,
    ): InputStream? = faces.firstOrNull { it.file == file }?.let { resources.openRawResource(it.res) }

    companion object {
        /** The font with [id], or null for an unknown or missing id. */
        fun fromId(id: String?): AppFont? = entries.firstOrNull { it.id == id }
    }
}

private const val VARIABLE_WEIGHTS = "100 900"

/** A family made of two variable files, one upright and one italic, each covering every weight. */
private fun variable(
    @FontRes regular: Int,
    @FontRes italic: Int,
    name: String,
): List<FontFace> =
    listOf(
        FontFace("$name.ttf", regular, VARIABLE_WEIGHTS, italic = false),
        FontFace("${name}_italic.ttf", italic, VARIABLE_WEIGHTS, italic = true),
    )

/** A family made of four fixed files: regular, italic, bold and bold italic. */
private fun fixed(
    name: String,
    extension: String,
    @FontRes regular: Int,
    @FontRes italic: Int,
    @FontRes bold: Int,
    @FontRes boldItalic: Int,
): List<FontFace> =
    listOf(
        FontFace("${name}_regular.$extension", regular, "400", italic = false),
        FontFace("${name}_italic.$extension", italic, "400", italic = true),
        FontFace("${name}_bold.$extension", bold, "700", italic = false),
        FontFace("${name}_bold_italic.$extension", boldItalic, "700", italic = true),
    )
