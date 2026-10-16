package com.freelibrary.app.data

/** What the app needs to know about the connection, behind an interface so rules that depend on it can be tested. */
fun interface NetworkState {
    /** True when the current connection is metered (normally mobile data); also true when there is none. */
    fun isMetered(): Boolean
}
