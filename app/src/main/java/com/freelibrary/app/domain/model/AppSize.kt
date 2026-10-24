package com.freelibrary.app.domain.model

/**
 * How big the whole app is drawn: text, covers, buttons and spacing scale together by [scale].
 * [LARGE] is the size the app had before sizes existed.
 */
enum class AppSize(
    val scale: Float,
) {
    SMALL(0.85f),
    MEDIUM(0.92f),
    LARGE(1.0f),
    ;

    companion object {
        /** From this smallest screen width (in dp at the phone's own density) up, a screen is roomy enough for [MEDIUM]. */
        const val ROOMY_SCREEN_DP = 400

        /**
         * The size a new install starts with. Large is never chosen for the reader: it is only for
         * people who ask for big text and containers. An unknown width (0) gets [MEDIUM].
         */
        fun forScreen(smallestWidthDp: Int): AppSize =
            when {
                smallestWidthDp <= 0 -> MEDIUM
                smallestWidthDp < ROOMY_SCREEN_DP -> SMALL
                else -> MEDIUM
            }
    }
}
