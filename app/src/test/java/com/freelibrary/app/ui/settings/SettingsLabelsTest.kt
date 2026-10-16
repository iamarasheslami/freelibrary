package com.freelibrary.app.ui.settings

import com.freelibrary.app.R
import com.freelibrary.app.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLabelsTest {
    @Test
    fun `every theme has its own button`() {
        val ids = ThemeMode.entries.map { SettingsLabels.themeButtonId(it) }

        assertEquals(ThemeMode.entries.size, ids.toSet().size)
        assertEquals(R.id.themeSystem, SettingsLabels.themeButtonId(ThemeMode.SYSTEM))
        assertEquals(R.id.themeLight, SettingsLabels.themeButtonId(ThemeMode.LIGHT))
        assertEquals(R.id.themeDark, SettingsLabels.themeButtonId(ThemeMode.DARK))
    }

    @Test
    fun `an idle sync says when the catalog last synced`() {
        assertEquals(SyncMessage.LastSync, SettingsLabels.syncMessage(SyncStatus.Idle))
    }

    @Test
    fun `a running sync and a failed one have their own words`() {
        assertEquals(SyncMessage.Text(R.string.sync_running), SettingsLabels.syncMessage(SyncStatus.Running))
        assertEquals(SyncMessage.Text(R.string.sync_failed), SettingsLabels.syncMessage(SyncStatus.Failed))
    }

    @Test
    fun `a sync that changed nothing says the catalog is up to date`() {
        assertEquals(SyncMessage.Text(R.string.sync_up_to_date), SettingsLabels.syncMessage(SyncStatus.Done(0)))
    }

    @Test
    fun `a sync that changed books says how many`() {
        assertEquals(SyncMessage.Plural(R.plurals.sync_done, 3), SettingsLabels.syncMessage(SyncStatus.Done(3)))
    }

    @Test
    fun `the About links point at the project`() {
        assertEquals(true, AboutLinks.SOURCE.startsWith("https://github.com/iamarasheslami/freelibrary"))
        assertEquals(true, AboutLinks.NOTICES.endsWith("THIRD_PARTY_NOTICES.md"))
    }
}
