package com.freelibrary.app.data

/** The current time, behind an interface so rules that depend on it can be tested. */
fun interface TimeProvider {
    fun nowMillis(): Long
}
