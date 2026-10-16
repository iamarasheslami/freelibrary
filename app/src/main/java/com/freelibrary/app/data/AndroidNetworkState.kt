package com.freelibrary.app.data

import android.content.Context
import android.net.ConnectivityManager

/** Asks Android whether the active connection is metered, which also covers a phone's Wi-Fi hotspot. */
class AndroidNetworkState(
    context: Context,
) : NetworkState {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override fun isMetered(): Boolean = manager?.isActiveNetworkMetered ?: true
}
