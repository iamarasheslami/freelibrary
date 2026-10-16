package com.freelibrary.app.ui.settings

import androidx.annotation.IdRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.freelibrary.app.R
import com.freelibrary.app.domain.model.ThemeMode

/** What to say about the reader's own sync, decided apart from any view so it can be tested. */
internal sealed interface SyncMessage {
    /** Nothing is happening: say when the catalog last synced. */
    data object LastSync : SyncMessage

    data class Text(
        @StringRes val res: Int,
    ) : SyncMessage

    data class Plural(
        @PluralsRes val res: Int,
        val count: Int,
    ) : SyncMessage
}

internal object SettingsLabels {
    /** The theme button that stands for [mode]. */
    @IdRes
    fun themeButtonId(mode: ThemeMode): Int =
        when (mode) {
            ThemeMode.SYSTEM -> R.id.themeSystem
            ThemeMode.LIGHT -> R.id.themeLight
            ThemeMode.DARK -> R.id.themeDark
        }

    fun syncMessage(status: SyncStatus): SyncMessage =
        when (status) {
            SyncStatus.Idle -> SyncMessage.LastSync
            SyncStatus.Running -> SyncMessage.Text(R.string.sync_running)
            SyncStatus.Failed -> SyncMessage.Text(R.string.sync_failed)
            is SyncStatus.Done ->
                if (status.booksChanged == 0) {
                    SyncMessage.Text(R.string.sync_up_to_date)
                } else {
                    SyncMessage.Plural(R.plurals.sync_done, status.booksChanged)
                }
        }
}

/** Where the About section sends the reader. */
internal object AboutLinks {
    const val SOURCE = "https://github.com/iamarasheslami/freelibrary"
    const val NOTICES = "https://github.com/iamarasheslami/freelibrary/blob/main/THIRD_PARTY_NOTICES.md"
}
