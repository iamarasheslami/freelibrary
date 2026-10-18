package com.freelibrary.app.data.download

import javax.inject.Qualifier

/** Marks the HTTP client used for book downloads, so it is never mixed up with another one. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadClient
