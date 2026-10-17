package com.freelibrary.app.domain.model

/** The longest name the profile keeps. */
const val MAX_PROFILE_NAME_LENGTH = 40

/**
 * What the reader has told the app about themselves. Everything is optional and stays on the
 * phone. [birthdayUtcMillis] is the picked day at midnight UTC, the form a date picker gives.
 * [avatarVersion] changes whenever the picture changes, so a cached copy of the old one is not shown.
 */
data class UserProfile(
    val name: String? = null,
    val birthdayUtcMillis: Long? = null,
    val avatarVersion: Long? = null,
)

/** The numbers shown on the Profile page. */
data class ProfileStats(
    val reading: Int,
    val wantToRead: Int,
    val finished: Int,
    val bookmarks: Int,
    val highlights: Int,
)

/** A name with its ends trimmed, runs of spaces made single and its length capped; null when nothing is left. */
fun cleanProfileName(raw: String?): String? =
    raw
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.take(MAX_PROFILE_NAME_LENGTH)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
