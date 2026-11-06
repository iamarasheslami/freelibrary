package com.freelibrary.app.ui.profile.avatar

// The avatar artwork. Every shape is SVG path data in a 200 by 200 box, drawn by AvatarRenderer.
// The shapes are original drawings made for this app.

/** The colours a shape can ask for; AvatarRenderer turns each into a real colour for the chosen skin, hair and clothes. */
internal enum class ArtColour {
    INK,
    WHITE,
    SKIN,
    SKIN_SHADE,
    HAIR,
    HAIR_DARK,
    OUTFIT,
    OUTFIT_DARK,
    SCARF,
    BLUSH,
    TONGUE,
    IRIS,
    HAT,
    HAT_DARK,
    METAL,
}

/** One drawn shape: filled, or drawn as a line when [strokeWidth] is above 0. */
internal class ArtShape(
    val path: String,
    val colour: ArtColour,
    val strokeWidth: Float = 0f,
)

/** The shapes of one choice: those drawn [back] behind the head and those drawn in [front] of it or of the body. */
internal class ArtLayers(
    val back: List<ArtShape>,
    val front: List<ArtShape>,
)

internal object AvatarArt {
    const val BOX = 200f
    const val FIGURE_SCALE = 1.2f
    const val FIGURE_CENTRE_X = 100f
    const val FIGURE_CENTRE_Y = 125f

    /** The colour choices as plain RGB. */
    val skinTones = intArrayOf(0xF6D5BD, 0xEBBB98, 0xD9A079, 0xB97A57, 0x8D5A3C, 0x5E3A26)
    val hairColours = intArrayOf(0x231F20, 0x4A3326, 0x7A4B2A, 0xA5412D, 0xD9B26A, 0x8E8E93, 0xE9E5DC, 0x2F6B4F)
    val eyeColours = intArrayOf(0x3B2A20, 0x6B4423, 0x8A6A2F, 0x3F7F5A, 0x3F6FA8, 0x6F7F8C, 0x6B4F8F, 0x2B2430)
    val outfitColours = intArrayOf(0x7AA05A, 0x2D5A4A, 0x0D4C3C, 0xA0BDDB, 0x2F6B4F, 0xBFD8C2, 0xD98F4E, 0xC65D5D)
    val hatColours = intArrayOf(0x2D5A4A, 0xC65D5D, 0xD98F4E, 0xA0BDDB, 0xE9E5DC, 0x3A3438, 0x7A4B2A, 0xD9B26A)
    val backgrounds = intArrayOf(0xFFFFFF, 0xBFD7A6, 0xBFD8C2, 0xA0BDDB, 0x7AA05A, 0x2F6B4F, 0x12372A, 0x080813)
    const val INK_RGB = 0x2B2430

    const val BODY = "M22 200 C22 160 58 144 100 144 C142 144 178 160 178 200 Z"
    const val NECK = "M85 118 L115 118 L115 152 Q100 160 85 152 Z"
    const val EAR_LEFT = "M57 96 a7 8 0 1 0 14 0 a7 8 0 1 0 -14 0 Z"
    const val EAR_RIGHT = "M129 96 a7 8 0 1 0 14 0 a7 8 0 1 0 -14 0 Z"

    val heads: List<String> =
        listOf(
            "M64 92 a36 40 0 1 0 72 0 a36 40 0 1 0 -72 0 Z",
            "M60 92 a40 38 0 1 0 80 0 a40 38 0 1 0 -80 0 Z",
            "M68 92 a32 44 0 1 0 64 0 a32 44 0 1 0 -64 0 Z",
            "M64 84 C64 56 78 50 100 50 C122 50 136 56 136 84 L136 108 Q136 132 112 134 L88 134 Q64 132 64 108 Z",
            "M62 86 C62 58 78 51 100 51 C122 51 138 58 138 86 C138 108 118 130 100 136 C82 130 62 108 62 86 Z",
        )

    val noses: List<List<ArtShape>> =
        listOf(
            listOf(
                ArtShape("M100 98 Q96 106 101 107", ArtColour.SKIN_SHADE, 2.4f),
            ),
            listOf(
                ArtShape("M96.5 104 Q100 108 103.5 104", ArtColour.SKIN_SHADE, 2.4f),
            ),
            listOf(
                ArtShape("M100 97 Q98 103 100 106", ArtColour.SKIN_SHADE, 2.2f),
                ArtShape("M92.5 107 Q100 111.5 107.5 107", ArtColour.SKIN_SHADE, 2.4f),
            ),
            listOf(
                ArtShape("M100 93 L96 107 L103.5 108", ArtColour.SKIN_SHADE, 2.4f),
            ),
            listOf(
                ArtShape("M94.8 105 a1.7 1.3 0 1 0 3.4 0 a1.7 1.3 0 1 0 -3.4 0 Z", ArtColour.SKIN_SHADE),
                ArtShape("M101.8 105 a1.7 1.3 0 1 0 3.4 0 a1.7 1.3 0 1 0 -3.4 0 Z", ArtColour.SKIN_SHADE),
            ),
        )

    val hair: List<ArtLayers> =
        listOf(
            ArtLayers(
                back = emptyList(),
                front = emptyList(),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape(
                            "M63 90 C58 56 80 44 100 44 C122 44 142 56 137 90 C132 76 120 68 100 68 C80 68 68 76 63 90 Z",
                            ArtColour.HAIR,
                        ),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape(
                            "M62 92 C54 54 84 40 106 42 C132 44 146 62 138 92 C136 78 128 70 116 66 C104 74 82 72 70 80 C66 " +
                                "84 64 88 62 92 Z",
                            ArtColour.HAIR,
                        ),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M56 92 C48 128 50 150 58 166 L142 166 C150 150 152 128 144 92 Z", ArtColour.HAIR),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M62 92 C56 54 82 42 100 42 C124 42 146 56 138 92 C132 74 118 66 100 66 C84 66 68 74 62 92 Z",
                            ArtColour.HAIR,
                        ),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M55 92 C50 124 52 140 62 146 L138 146 C148 140 150 124 145 92 Z", ArtColour.HAIR),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M60 94 C54 54 82 41 100 41 C126 41 148 56 140 94 C134 72 120 62 100 62 C82 62 66 72 60 94 Z",
                            ArtColour.HAIR,
                        ),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M54 86 a12 12 0 1 0 24 0 a12 12 0 1 0 -24 0 Z", ArtColour.HAIR),
                        ArtShape("M122 86 a12 12 0 1 0 24 0 a12 12 0 1 0 -24 0 Z", ArtColour.HAIR),
                    ),
                front =
                    listOf(
                        ArtShape("M60 70 a13 13 0 1 0 26 0 a13 13 0 1 0 -26 0 Z", ArtColour.HAIR),
                        ArtShape("M74 56 a15 15 0 1 0 30 0 a15 15 0 1 0 -30 0 Z", ArtColour.HAIR),
                        ArtShape("M96 56 a15 15 0 1 0 30 0 a15 15 0 1 0 -30 0 Z", ArtColour.HAIR),
                        ArtShape("M114 70 a13 13 0 1 0 26 0 a13 13 0 1 0 -26 0 Z", ArtColour.HAIR),
                        ArtShape("M64 84 C62 70 76 62 100 62 C124 62 138 70 136 84 C126 72 74 72 64 84 Z", ArtColour.HAIR),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M89 31 a11 11 0 1 0 22 0 a11 11 0 1 0 -22 0 Z", ArtColour.HAIR),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M63 90 C58 56 80 45 100 45 C122 45 142 56 137 90 C132 76 120 69 100 69 C80 69 68 76 63 90 Z",
                            ArtColour.HAIR,
                        ),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape(
                            "M65 86 C62 58 80 51 100 51 C120 51 138 58 135 86 C130 72 120 64 100 64 C80 64 70 72 65 86 Z",
                            ArtColour.HAIR,
                        ),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape(
                            "M62 90 C54 58 80 42 104 43 C128 44 144 62 138 90 C134 80 130 74 124 70 C112 64 96 66 84 62 C76 " +
                                "70 68 78 62 90 Z",
                            ArtColour.HAIR,
                        ),
                        ArtShape("M118 62 C128 64 136 72 138 84 C132 76 126 70 114 66 Z", ArtColour.HAIR),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M44 80 a56 54 0 1 0 112 0 a56 54 0 1 0 -112 0 Z", ArtColour.HAIR),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M63 88 C60 64 78 58 100 58 C122 58 140 64 137 88 C130 74 118 68 100 68 C82 68 70 74 63 88 Z",
                            ArtColour.HAIR,
                        ),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M122 58 C158 54 170 100 152 142 C148 150 138 148 142 138 C150 108 142 84 124 76 Z", ArtColour.HAIR),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M63 90 C58 56 80 44 100 44 C122 44 142 56 137 90 C132 76 120 68 100 68 C80 68 68 76 63 90 Z",
                            ArtColour.HAIR,
                        ),
                        ArtShape("M122 54 L134 62 L130 70 L118 62 Z", ArtColour.HAIR_DARK),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M64 78 C40 78 32 112 44 138 C52 132 54 114 66 102 Z", ArtColour.HAIR),
                        ArtShape("M136 78 C160 78 168 112 156 138 C148 132 146 114 134 102 Z", ArtColour.HAIR),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M63 90 C58 56 80 44 100 44 C122 44 142 56 137 90 C132 76 120 68 100 68 C80 68 68 76 63 90 Z",
                            ArtColour.HAIR,
                        ),
                        ArtShape("M57.5 80 a6.5 6.5 0 1 0 13 0 a6.5 6.5 0 1 0 -13 0 Z", ArtColour.HAIR_DARK),
                        ArtShape("M129.5 80 a6.5 6.5 0 1 0 13 0 a6.5 6.5 0 1 0 -13 0 Z", ArtColour.HAIR_DARK),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape(
                            "M54 90 C42 110 56 124 48 142 C44 154 54 164 62 168 L138 168 C146 164 156 154 152 142 C144 124 " +
                                "158 110 146 90 Z",
                            ArtColour.HAIR,
                        ),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M60 94 C52 54 84 40 104 42 C130 44 148 62 140 94 C136 80 128 70 114 64 C100 76 80 74 68 82 C64 " +
                                "86 62 90 60 94 Z",
                            ArtColour.HAIR,
                        ),
                        ArtShape("M60 94 C54 108 62 120 56 134 C68 128 70 112 66 96 Z", ArtColour.HAIR),
                        ArtShape("M140 94 C146 108 138 120 144 134 C132 128 130 112 134 96 Z", ArtColour.HAIR),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape(
                            "M62 92 C52 56 70 34 98 34 C128 32 148 54 138 92 C134 76 126 66 110 62 C96 58 76 62 66 78 C64 82 " +
                                "62 88 62 92 Z",
                            ArtColour.HAIR,
                        ),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M61 38 a13 13 0 1 0 26 0 a13 13 0 1 0 -26 0 Z", ArtColour.HAIR),
                        ArtShape("M113 38 a13 13 0 1 0 26 0 a13 13 0 1 0 -26 0 Z", ArtColour.HAIR),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M63 90 C58 56 80 45 100 45 C122 45 142 56 137 90 C132 76 120 69 100 69 C80 69 68 76 63 90 Z",
                            ArtColour.HAIR,
                        ),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M58 92 C52 112 58 130 50 152 L62 154 C70 132 66 112 68 96 Z", ArtColour.HAIR),
                        ArtShape("M142 92 C148 112 142 130 150 152 L138 154 C130 132 134 112 132 96 Z", ArtColour.HAIR),
                        ArtShape("M54 118 L66 122", ArtColour.HAIR_DARK, 2f),
                        ArtShape("M52 132 L64 136", ArtColour.HAIR_DARK, 2f),
                        ArtShape("M138 122 L150 118", ArtColour.HAIR_DARK, 2f),
                        ArtShape("M136 136 L148 132", ArtColour.HAIR_DARK, 2f),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M62 92 C56 54 82 42 100 42 C124 42 146 56 138 92 C132 74 118 66 100 66 C84 66 68 74 62 92 Z",
                            ArtColour.HAIR,
                        ),
                        ArtShape("M100 44 L100 62", ArtColour.HAIR_DARK, 2f),
                    ),
            ),
        )

    val eyes: List<List<ArtShape>> =
        listOf(
            listOf(
                ArtShape("M82 92 a3.8 3.8 0 1 0 7.6 0 a3.8 3.8 0 1 0 -7.6 0 Z", ArtColour.IRIS),
                ArtShape("M110.4 92 a3.8 3.8 0 1 0 7.6 0 a3.8 3.8 0 1 0 -7.6 0 Z", ArtColour.IRIS),
            ),
            listOf(
                ArtShape("M81 93 Q86 86 91 93", ArtColour.INK, 3f),
                ArtShape("M109 93 Q114 86 119 93", ArtColour.INK, 3f),
            ),
            listOf(
                ArtShape("M80 92 a6.5 7.5 0 1 0 13 0 a6.5 7.5 0 1 0 -13 0 Z", ArtColour.WHITE),
                ArtShape("M107 92 a6.5 7.5 0 1 0 13 0 a6.5 7.5 0 1 0 -13 0 Z", ArtColour.WHITE),
                ArtShape("M82.5 93 a4 4.6 0 1 0 8 0 a4 4.6 0 1 0 -8 0 Z", ArtColour.IRIS),
                ArtShape("M109.5 93 a4 4.6 0 1 0 8 0 a4 4.6 0 1 0 -8 0 Z", ArtColour.IRIS),
                ArtShape("M84.6 93.4 a1.9 2.3 0 1 0 3.8 0 a1.9 2.3 0 1 0 -3.8 0 Z", ArtColour.INK),
                ArtShape("M111.6 93.4 a1.9 2.3 0 1 0 3.8 0 a1.9 2.3 0 1 0 -3.8 0 Z", ArtColour.INK),
                ArtShape("M85.6 91 a1.4 1.4 0 1 0 2.8 0 a1.4 1.4 0 1 0 -2.8 0 Z", ArtColour.WHITE),
                ArtShape("M112.6 91 a1.4 1.4 0 1 0 2.8 0 a1.4 1.4 0 1 0 -2.8 0 Z", ArtColour.WHITE),
            ),
            listOf(
                ArtShape("M80 92 L92 92", ArtColour.INK, 3f),
                ArtShape("M108 92 L120 92", ArtColour.INK, 3f),
                ArtShape("M80 92 L77 89", ArtColour.INK, 2f),
                ArtShape("M120 92 L123 89", ArtColour.INK, 2f),
            ),
            listOf(
                ArtShape("M79.5 93 a6.5 5 0 1 0 13 0 a6.5 5 0 1 0 -13 0 Z", ArtColour.WHITE),
                ArtShape("M107.5 93 a6.5 5 0 1 0 13 0 a6.5 5 0 1 0 -13 0 Z", ArtColour.WHITE),
                ArtShape("M82.6 93.5 a3.4 3.4 0 1 0 6.8 0 a3.4 3.4 0 1 0 -6.8 0 Z", ArtColour.IRIS),
                ArtShape("M110.6 93.5 a3.4 3.4 0 1 0 6.8 0 a3.4 3.4 0 1 0 -6.8 0 Z", ArtColour.IRIS),
                ArtShape("M78 92.5 Q86 84.5 94 92.5 L94 90 Q86 82.5 78 90 Z", ArtColour.SKIN),
                ArtShape("M106 92.5 Q114 84.5 122 92.5 L122 90 Q114 82.5 106 90 Z", ArtColour.SKIN),
                ArtShape("M79 92 Q86 87 93 92", ArtColour.INK, 2.6f),
                ArtShape("M107 92 Q114 87 121 92", ArtColour.INK, 2.6f),
            ),
            listOf(
                ArtShape("M80.5 92.5 a6 6.5 0 1 0 12 0 a6 6.5 0 1 0 -12 0 Z", ArtColour.WHITE),
                ArtShape("M107.5 92.5 a6 6.5 0 1 0 12 0 a6 6.5 0 1 0 -12 0 Z", ArtColour.WHITE),
                ArtShape("M82.7 93 a3.8 4.2 0 1 0 7.6 0 a3.8 4.2 0 1 0 -7.6 0 Z", ArtColour.IRIS),
                ArtShape("M109.7 93 a3.8 4.2 0 1 0 7.6 0 a3.8 4.2 0 1 0 -7.6 0 Z", ArtColour.IRIS),
                ArtShape("M84.8 93 a1.7 1.7 0 1 0 3.4 0 a1.7 1.7 0 1 0 -3.4 0 Z", ArtColour.INK),
                ArtShape("M111.8 93 a1.7 1.7 0 1 0 3.4 0 a1.7 1.7 0 1 0 -3.4 0 Z", ArtColour.INK),
                ArtShape("M86.6 91.4 a1.2 1.2 0 1 0 2.4 0 a1.2 1.2 0 1 0 -2.4 0 Z", ArtColour.WHITE),
                ArtShape("M113.6 91.4 a1.2 1.2 0 1 0 2.4 0 a1.2 1.2 0 1 0 -2.4 0 Z", ArtColour.WHITE),
                ArtShape("M80 90 Q86 85.5 93 90", ArtColour.INK, 2.4f),
                ArtShape("M107 90 Q114 85.5 120 90", ArtColour.INK, 2.4f),
                ArtShape("M80.5 90 L76.5 87", ArtColour.INK, 2.2f),
                ArtShape("M119.5 90 L123.5 87", ArtColour.INK, 2.2f),
            ),
            listOf(
                ArtShape("M82 92 a3.8 3.8 0 1 0 7.6 0 a3.8 3.8 0 1 0 -7.6 0 Z", ArtColour.IRIS),
                ArtShape("M109 93 Q114 86 119 93", ArtColour.INK, 3f),
            ),
        )

    val mouths: List<List<ArtShape>> =
        listOf(
            listOf(
                ArtShape("M88 112 Q100 123 112 112", ArtColour.INK, 3f),
            ),
            listOf(
                ArtShape("M86 110 Q100 128 114 110 Z", ArtColour.INK),
                ArtShape("M90 111.5 Q100 114 110 111.5 L108.5 113.5 Q100 115.5 91.5 113.5 Z", ArtColour.WHITE),
            ),
            listOf(
                ArtShape("M91 114 L109 114", ArtColour.INK, 3f),
            ),
            listOf(
                ArtShape("M90 109 Q100 129 110 109 Z", ArtColour.INK),
                ArtShape("M94 117 Q100 122 106 117 Q100 114 94 117 Z", ArtColour.TONGUE),
            ),
            listOf(
                ArtShape("M90 114 Q102 117 111 110", ArtColour.INK, 3f),
            ),
        )

    val glasses: List<List<ArtShape>> =
        listOf(
            emptyList(),
            listOf(
                ArtShape("M76 92 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z", ArtColour.INK, 2.6f),
                ArtShape("M104 92 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z", ArtColour.INK, 2.6f),
                ArtShape("M96 91 Q100 88 104 91", ArtColour.INK, 2.6f),
                ArtShape("M76 90 L66 88", ArtColour.INK, 2.4f),
                ArtShape("M124 90 L134 88", ArtColour.INK, 2.4f),
            ),
            listOf(
                ArtShape("M76 85 L96 85 L96 99 Q96 101 94 101 L78 101 Q76 101 76 99 Z", ArtColour.INK, 2.6f),
                ArtShape("M104 85 L124 85 L124 99 Q124 101 122 101 L106 101 Q104 101 104 99 Z", ArtColour.INK, 2.6f),
                ArtShape("M96 90 L104 90", ArtColour.INK, 2.6f),
                ArtShape("M76 90 L66 88", ArtColour.INK, 2.4f),
                ArtShape("M124 90 L134 88", ArtColour.INK, 2.4f),
            ),
            listOf(
                ArtShape("M74 86 L96 88 Q96 101 85 101 Q74 100 74 86 Z", ArtColour.INK, 2.6f),
                ArtShape("M126 86 L104 88 Q104 101 115 101 Q126 100 126 86 Z", ArtColour.INK, 2.6f),
                ArtShape("M96 90 Q100 88 104 90", ArtColour.INK, 2.6f),
                ArtShape("M74 87 L66 85", ArtColour.INK, 2.4f),
                ArtShape("M126 87 L134 85", ArtColour.INK, 2.4f),
            ),
            listOf(
                ArtShape("M75 86 L96 86 L95 97 Q93 102 86 102 Q76 102 75 94 Z", ArtColour.INK),
                ArtShape("M125 86 L104 86 L105 97 Q107 102 114 102 Q124 102 125 94 Z", ArtColour.INK),
                ArtShape("M96 88 L104 88", ArtColour.INK, 2.6f),
                ArtShape("M76 90 L66 88", ArtColour.INK, 2.4f),
                ArtShape("M124 90 L134 88", ArtColour.INK, 2.4f),
                ArtShape("M79 90 L84 88", ArtColour.WHITE, 1.6f),
                ArtShape("M121 90 L116 88", ArtColour.WHITE, 1.6f),
            ),
            listOf(
                ArtShape("M75 92 a11 8 0 1 0 22 0 a11 8 0 1 0 -22 0 Z", ArtColour.INK, 2f),
                ArtShape("M103 92 a11 8 0 1 0 22 0 a11 8 0 1 0 -22 0 Z", ArtColour.INK, 2f),
                ArtShape("M97 91 Q100 89 103 91", ArtColour.INK, 2f),
                ArtShape("M75 91 L66 89", ArtColour.INK, 2f),
                ArtShape("M125 91 L134 89", ArtColour.INK, 2f),
            ),
            listOf(
                ArtShape("M78 92 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 Z", ArtColour.INK, 2f),
                ArtShape("M106 92 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 Z", ArtColour.INK, 2f),
                ArtShape("M94 91 Q100 87 106 91", ArtColour.INK, 2f),
                ArtShape("M78 91 L66 89", ArtColour.INK, 2f),
                ArtShape("M122 91 L134 89", ArtColour.INK, 2f),
            ),
        )

    val hats: List<ArtLayers> =
        listOf(
            ArtLayers(
                back = emptyList(),
                front = emptyList(),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M61 68 C58 44 80 35 100 35 C120 35 142 44 139 68 Z", ArtColour.HAT),
                        ArtShape("M59 60 Q100 56 141 60 L141 71 Q100 75 59 71 Z", ArtColour.HAT_DARK),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M61 68 C58 44 80 35 100 35 C120 35 142 44 139 68 Z", ArtColour.HAT),
                        ArtShape("M59 60 Q100 56 141 60 L141 71 Q100 75 59 71 Z", ArtColour.HAT_DARK),
                        ArtShape("M92 31 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 Z", ArtColour.HAT_DARK),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M62 70 C60 44 80 37 100 37 C120 37 140 44 138 70 Z", ArtColour.HAT),
                        ArtShape("M58 66 Q100 74 142 66 Q146 72 138 74 Q100 80 62 74 Q54 72 58 66 Z", ArtColour.HAT_DARK),
                        ArtShape("M97 38 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z", ArtColour.HAT_DARK),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M64 62 C62 42 82 36 100 36 C118 36 138 42 136 62 Z", ArtColour.HAT),
                        ArtShape("M64 56 Q100 51 136 56 L136 62 Q100 57 64 62 Z", ArtColour.HAT_DARK),
                        ArtShape("M44 62 Q100 50 156 62 Q160 70 150 71 Q100 62 50 71 Q40 70 44 62 Z", ArtColour.HAT),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M58 64 C54 44 82 33 108 36 C136 40 150 54 138 66 C120 74 76 74 58 64 Z", ArtColour.HAT),
                        ArtShape("M104 35 L108 29 L112 36 Z", ArtColour.HAT_DARK),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape(
                            "M52 84 C44 102 46 132 52 152 L148 152 C154 132 156 102 148 84 C146 54 124 40 100 40 C76 40 54 54 " +
                                "52 84 Z",
                            ArtColour.HAT,
                        ),
                    ),
                front =
                    listOf(
                        ArtShape(
                            "M63 96 C56 58 80 44 100 44 C120 44 144 58 137 96 C134 80 124 70 100 68 C76 70 66 80 63 96 Z",
                            ArtColour.HAT,
                        ),
                        ArtShape("M62 116 C74 148 126 148 138 116 L148 152 C126 170 74 170 52 152 Z", ArtColour.HAT),
                        ArtShape("M66 100 C70 82 84 74 100 74", ArtColour.HAT_DARK, 2f),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M30 62 Q60 76 100 74 Q140 76 170 62 Q164 54 150 58 Q100 50 50 58 Q36 54 30 62 Z", ArtColour.HAT),
                        ArtShape("M66 58 C64 36 74 30 84 34 Q100 40 116 34 C126 30 136 36 134 58 Z", ArtColour.HAT),
                        ArtShape("M66 52 Q100 58 134 52 L134 58 Q100 64 66 58 Z", ArtColour.HAT_DARK),
                    ),
            ),
        )

    val earrings: List<List<ArtShape>> =
        listOf(
            emptyList(),
            listOf(
                ArtShape("M60.6 104 a3.4 3.4 0 1 0 6.8 0 a3.4 3.4 0 1 0 -6.8 0 Z", ArtColour.METAL),
                ArtShape("M132.6 104 a3.4 3.4 0 1 0 6.8 0 a3.4 3.4 0 1 0 -6.8 0 Z", ArtColour.METAL),
            ),
            listOf(
                ArtShape("M60.6 104 a3.4 3.4 0 1 0 6.8 0 a3.4 3.4 0 1 0 -6.8 0 Z", ArtColour.METAL),
            ),
            listOf(
                ArtShape("M132.6 104 a3.4 3.4 0 1 0 6.8 0 a3.4 3.4 0 1 0 -6.8 0 Z", ArtColour.METAL),
            ),
            listOf(
                ArtShape("M58 111 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 Z", ArtColour.METAL, 2.2f),
                ArtShape("M130 111 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 Z", ArtColour.METAL, 2.2f),
            ),
            listOf(
                ArtShape("M58 111 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 Z", ArtColour.METAL, 2.2f),
            ),
            listOf(
                ArtShape("M130 111 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 Z", ArtColour.METAL, 2.2f),
            ),
            listOf(
                ArtShape("M64 104 L64 111", ArtColour.METAL, 1.8f),
                ArtShape("M60 115 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z", ArtColour.METAL),
                ArtShape("M136 104 L136 111", ArtColour.METAL, 1.8f),
                ArtShape("M132 115 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z", ArtColour.METAL),
            ),
        )

    val outfits: List<ArtLayers> =
        listOf(
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M85 144 Q100 160 115 144 Z", ArtColour.SKIN),
                    ),
            ),
            ArtLayers(
                back =
                    listOf(
                        ArtShape("M58 154 C56 120 76 112 100 112 C124 112 144 120 142 154 Z", ArtColour.OUTFIT),
                    ),
                front =
                    listOf(
                        ArtShape("M84 144 Q100 162 116 144 L110 143 Q100 152 90 143 Z", ArtColour.OUTFIT_DARK),
                        ArtShape("M94 150 L93 172", ArtColour.WHITE, 2.4f),
                        ArtShape("M106 150 L107 172", ArtColour.WHITE, 2.4f),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M83 144 L100 166 L117 144 Z", ArtColour.SKIN),
                        ArtShape("M80 142 L100 168 L92 140 Z", ArtColour.WHITE),
                        ArtShape("M120 142 L100 168 L108 140 Z", ArtColour.WHITE),
                        ArtShape("M100 168 L100 200", ArtColour.OUTFIT_DARK, 2.4f),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M70 139 Q100 160 130 139 L134 153 Q100 176 66 153 Z", ArtColour.SCARF),
                        ArtShape("M106 162 L124 160 L130 192 L110 190 Z", ArtColour.SCARF),
                        ArtShape("M110 176 L127 174", ArtColour.OUTFIT_DARK, 2f),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M80 144 L100 176 L120 144 Z", ArtColour.SKIN),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M84 144 Q100 160 116 144 Z", ArtColour.SKIN),
                        ArtShape("M82 143 Q100 164 118 143 L121 150 Q100 172 79 150 Z", ArtColour.OUTFIT_DARK),
                        ArtShape("M31 176 L169 176 L170 183 L30 183 Z", ArtColour.SCARF),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M82 144 L100 174 L118 144 Z", ArtColour.WHITE),
                        ArtShape("M87 144 L100 164 L113 144 Z", ArtColour.SKIN),
                        ArtShape("M78 142 L96 178 L82 186 L64 158 Z", ArtColour.OUTFIT_DARK),
                        ArtShape("M122 142 L104 178 L118 186 L136 158 Z", ArtColour.OUTFIT_DARK),
                        ArtShape("M100 178 L100 200", ArtColour.OUTFIT_DARK, 2.2f),
                        ArtShape("M98 186 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0 Z", ArtColour.WHITE),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M82 126 L118 126 L121 152 Q100 162 79 152 Z", ArtColour.OUTFIT_DARK),
                        ArtShape("M81 140 Q100 150 119 140", ArtColour.OUTFIT, 2f),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M22 200 C22 160 58 144 100 144 C142 144 178 160 178 200 Z", ArtColour.WHITE),
                        ArtShape("M85 144 Q100 160 115 144 Z", ArtColour.SKIN),
                        ArtShape("M72 200 L72 168 L128 168 L128 200 Z", ArtColour.OUTFIT),
                        ArtShape("M72 170 L68 152 L80 150 L84 168 Z", ArtColour.OUTFIT),
                        ArtShape("M128 170 L132 152 L120 150 L116 168 Z", ArtColour.OUTFIT),
                        ArtShape("M73.8 166 a2.2 2.2 0 1 0 4.4 0 a2.2 2.2 0 1 0 -4.4 0 Z", ArtColour.WHITE),
                        ArtShape("M121.8 166 a2.2 2.2 0 1 0 4.4 0 a2.2 2.2 0 1 0 -4.4 0 Z", ArtColour.WHITE),
                        ArtShape("M88 180 L112 180 L112 192 L88 192 Z", ArtColour.OUTFIT_DARK, 1.8f),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M86 144 L100 158 L114 144 Z", ArtColour.SKIN),
                        ArtShape("M78 142 L98 150 L92 162 L72 150 Z", ArtColour.OUTFIT_DARK),
                        ArtShape("M122 142 L102 150 L108 162 L128 150 Z", ArtColour.OUTFIT_DARK),
                        ArtShape("M100 158 L100 178", ArtColour.OUTFIT_DARK, 2f),
                        ArtShape("M98.4 166 a1.6 1.6 0 1 0 3.2 0 a1.6 1.6 0 1 0 -3.2 0 Z", ArtColour.WHITE),
                        ArtShape("M98.4 173 a1.6 1.6 0 1 0 3.2 0 a1.6 1.6 0 1 0 -3.2 0 Z", ArtColour.WHITE),
                    ),
            ),
            ArtLayers(
                back = emptyList(),
                front =
                    listOf(
                        ArtShape("M22 200 C22 160 58 144 100 144 C142 144 178 160 178 200 Z", ArtColour.SKIN),
                        ArtShape("M70 200 L70 158 Q72 146 82 144 Q100 166 118 144 Q128 146 130 158 L130 200 Z", ArtColour.OUTFIT),
                    ),
            ),
        )

    val brows: List<ArtShape> =
        listOf(
            ArtShape("M79 80 Q86 76 93 79", ArtColour.HAIR_DARK, 2.6f),
            ArtShape("M107 79 Q114 76 121 80", ArtColour.HAIR_DARK, 2.6f),
        )

    val blush: List<ArtShape> =
        listOf(
            ArtShape("M71 104 a7 4.5 0 1 0 14 0 a7 4.5 0 1 0 -14 0 Z", ArtColour.BLUSH),
            ArtShape("M115 104 a7 4.5 0 1 0 14 0 a7 4.5 0 1 0 -14 0 Z", ArtColour.BLUSH),
        )
}
