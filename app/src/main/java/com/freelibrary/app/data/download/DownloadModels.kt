package com.freelibrary.app.data.download

import java.io.File

/** Why a download did not happen, in terms the reader can act on. */
enum class DownloadError {
    /** No connection, or the connection broke part way. */
    OFFLINE,

    /** The site refused or limited the request (403, 429 or 451); trying later, or without a VPN, may help. */
    BLOCKED,

    /** The site has no such file. */
    NOT_FOUND,

    /** The site had an error of its own. */
    SERVER,

    /** What came back is not a book of the asked kind, for example a web page. */
    NOT_A_BOOK,

    /** The file is bigger than the app accepts. */
    TOO_LARGE,

    /** The phone has no room, or the file could not be written. */
    STORAGE,
}

/** How a download ended. */
sealed interface DownloadOutcome {
    data class Done(
        val file: File,
    ) : DownloadOutcome

    data class Failed(
        val error: DownloadError,
    ) : DownloadOutcome
}

/** How much of a file has arrived; [total] is null when the site did not say how big it is. */
data class DownloadProgress(
    val bytes: Long,
    val total: Long?,
)
