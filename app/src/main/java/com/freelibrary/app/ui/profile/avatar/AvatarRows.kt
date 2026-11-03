package com.freelibrary.app.ui.profile.avatar

import androidx.annotation.StringRes
import com.freelibrary.app.R
import com.freelibrary.app.domain.model.AvatarDesign
import com.freelibrary.app.domain.model.AvatarSlot

/** How a choice is shown: as a small avatar wearing it, or as a plain colour. */
enum class AvatarSectionKind { STYLE, COLOUR }

/** One group of choices under a heading, such as the hair styles. */
class AvatarSection(
    @StringRes val title: Int,
    val slot: AvatarSlot,
    val kind: AvatarSectionKind,
)

/** The tabs of the creator and the groups of choices each one holds. */
enum class AvatarTab(
    @StringRes val label: Int,
    val sections: List<AvatarSection>,
) {
    HEAD(
        R.string.avatar_tab_head,
        listOf(
            AvatarSection(R.string.avatar_section_skin, AvatarSlot.SKIN, AvatarSectionKind.COLOUR),
            AvatarSection(R.string.avatar_section_face_shape, AvatarSlot.FACE_SHAPE, AvatarSectionKind.STYLE),
            AvatarSection(R.string.avatar_section_nose, AvatarSlot.NOSE, AvatarSectionKind.STYLE),
        ),
    ),
    HAIR(
        R.string.avatar_tab_hair,
        listOf(
            AvatarSection(R.string.avatar_section_hair_style, AvatarSlot.HAIR, AvatarSectionKind.STYLE),
            AvatarSection(R.string.avatar_section_hair_colour, AvatarSlot.HAIR_COLOUR, AvatarSectionKind.COLOUR),
        ),
    ),
    FACE(
        R.string.avatar_tab_face,
        listOf(
            AvatarSection(R.string.avatar_section_eyes, AvatarSlot.EYES, AvatarSectionKind.STYLE),
            AvatarSection(R.string.avatar_section_eye_colour, AvatarSlot.EYE_COLOUR, AvatarSectionKind.COLOUR),
            AvatarSection(R.string.avatar_section_mouth, AvatarSlot.MOUTH, AvatarSectionKind.STYLE),
        ),
    ),
    EXTRAS(
        R.string.avatar_tab_extras,
        listOf(
            AvatarSection(R.string.avatar_section_glasses, AvatarSlot.GLASSES, AvatarSectionKind.STYLE),
            AvatarSection(R.string.avatar_section_hat, AvatarSlot.HAT, AvatarSectionKind.STYLE),
            AvatarSection(R.string.avatar_section_hat_colour, AvatarSlot.HAT_COLOUR, AvatarSectionKind.COLOUR),
            AvatarSection(R.string.avatar_section_earrings, AvatarSlot.EARRINGS, AvatarSectionKind.STYLE),
        ),
    ),
    CLOTHES(
        R.string.avatar_tab_clothes,
        listOf(
            AvatarSection(R.string.avatar_section_outfit, AvatarSlot.OUTFIT, AvatarSectionKind.STYLE),
            AvatarSection(R.string.avatar_section_outfit_colour, AvatarSlot.OUTFIT_COLOUR, AvatarSectionKind.COLOUR),
        ),
    ),
    BACKGROUND(
        R.string.avatar_tab_background,
        listOf(AvatarSection(R.string.avatar_section_background, AvatarSlot.BACKGROUND, AvatarSectionKind.COLOUR)),
    ),
}

/** One row of the creator's list of choices. */
sealed interface AvatarRow {
    data class Heading(
        @StringRes val title: Int,
    ) : AvatarRow

    /** A choice of [slot] shown as a small avatar ([preview]) that already wears it. */
    data class Style(
        @StringRes val title: Int,
        val slot: AvatarSlot,
        val choice: Int,
        val preview: AvatarDesign,
        val selected: Boolean,
    ) : AvatarRow

    /** A choice of [slot] shown as the plain colour [rgb]. */
    data class Colour(
        @StringRes val title: Int,
        val slot: AvatarSlot,
        val choice: Int,
        val rgb: Int,
        val selected: Boolean,
    ) : AvatarRow
}

private fun colours(slot: AvatarSlot): IntArray =
    when (slot) {
        AvatarSlot.SKIN -> AvatarArt.skinTones
        AvatarSlot.HAIR_COLOUR -> AvatarArt.hairColours
        AvatarSlot.EYE_COLOUR -> AvatarArt.eyeColours
        AvatarSlot.HAT_COLOUR -> AvatarArt.hatColours
        AvatarSlot.OUTFIT_COLOUR -> AvatarArt.outfitColours
        AvatarSlot.BACKGROUND -> AvatarArt.backgrounds
        else -> error("$slot is not a colour choice")
    }

/** The rows of [tab] for [design]: each group's heading, then its choices with the current one marked. */
fun avatarRows(
    tab: AvatarTab,
    design: AvatarDesign,
): List<AvatarRow> =
    tab.sections.flatMap { section ->
        val current = design[section.slot]
        listOf<AvatarRow>(AvatarRow.Heading(section.title)) +
            (0 until section.slot.count).map { choice ->
                when (section.kind) {
                    AvatarSectionKind.STYLE ->
                        AvatarRow.Style(section.title, section.slot, choice, design.with(section.slot, choice), choice == current)
                    AvatarSectionKind.COLOUR ->
                        AvatarRow.Colour(section.title, section.slot, choice, colours(section.slot)[choice], choice == current)
                }
            }
    }
