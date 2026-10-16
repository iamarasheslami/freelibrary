package com.freelibrary.app.data.backup

import java.io.File

/** The folder that holds downloaded book files. The download manager will use the same folder. */
class BookStorage(
    val directory: File,
)
