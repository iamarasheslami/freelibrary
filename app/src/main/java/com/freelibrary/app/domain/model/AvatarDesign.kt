package com.freelibrary.app.domain.model

import kotlin.random.Random

/** What the avatar creator lets the reader choose, and how many choices each has. */
enum class AvatarSlot(
    val count: Int,
) {
    SKIN(6),
    FACE_SHAPE(5),
    NOSE(5),
    HAIR(16),
    HAIR_COLOUR(8),
    EYES(7),
    EYE_COLOUR(8),
    MOUTH(5),
    GLASSES(7),
    HAT(8),
    HAT_COLOUR(8),
    EARRINGS(8),
    OUTFIT(11),
    OUTFIT_COLOUR(8),
    BACKGROUND(8),
}

/**
 * An avatar the reader built: one choice number for every [AvatarSlot]. It is kept as a short text so the creator can open
 * again with the last avatar, and it never holds anything but numbers.
 */
class AvatarDesign private constructor(
    private val choices: List<Int>,
) {
    operator fun get(slot: AvatarSlot): Int = choices[slot.ordinal]

    /** A copy with [slot] set to [choice]; a choice that does not exist for the slot is refused. */
    fun with(
        slot: AvatarSlot,
        choice: Int,
    ): AvatarDesign {
        require(choice in 0 until slot.count) { "$slot has no choice $choice" }
        return AvatarDesign(choices.toMutableList().also { it[slot.ordinal] = choice })
    }

    /** The text form, such as `2:1.0.0.1.2.0.3.0.1.0.0.0.0.5.2`: the format version, then the choices in slot order. */
    fun encode(): String = "$VERSION$SEPARATOR_VERSION${choices.joinToString(SEPARATOR_CHOICE)}"

    override fun equals(other: Any?): Boolean = other is AvatarDesign && other.choices == choices

    override fun hashCode(): Int = choices.hashCode()

    override fun toString(): String = "AvatarDesign(${encode()})"

    companion object {
        private const val VERSION = "2"
        private const val SEPARATOR_VERSION = ":"
        private const val SEPARATOR_CHOICE = "."

        /** The plain sleeveless top: no collar, no print, so the first avatar is just a face in green cloth. */
        private const val PLAIN_OUTFIT = 10

        /** The avatar the creator starts with when the reader has not made one. */
        val DEFAULT: AvatarDesign =
            AvatarDesign(
                AvatarSlot.entries.map { 0 },
            ).with(AvatarSlot.SKIN, 1).with(AvatarSlot.OUTFIT, PLAIN_OUTFIT)

        /** An avatar with a random choice in every slot. */
        fun random(random: Random): AvatarDesign = AvatarDesign(AvatarSlot.entries.map { random.nextInt(it.count) })

        /** The avatar written by [encode], or null for anything else: another version, a wrong number of choices or one out of range. */
        fun decode(text: String?): AvatarDesign? {
            val prefix = "$VERSION$SEPARATOR_VERSION"
            if (text == null || !text.startsWith(prefix)) return null
            val body = text.removePrefix(prefix)
            val parts = body.split(SEPARATOR_CHOICE)
            if (parts.size != AvatarSlot.entries.size) return null
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            if (AvatarSlot.entries.any { numbers[it.ordinal] !in 0 until it.count }) return null
            return AvatarDesign(numbers)
        }
    }
}
