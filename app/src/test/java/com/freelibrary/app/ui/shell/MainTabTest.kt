package com.freelibrary.app.ui.shell

import com.freelibrary.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabTest {
    @Test
    fun `the tabs are home, explore and profile in that order`() {
        assertEquals(
            listOf(R.id.homeFragment, R.id.exploreFragment, R.id.profileFragment),
            MainTab.entries.map { it.destinationId },
        )
    }

    @Test
    fun `each tab is found from its own page`() {
        MainTab.entries.forEach { assertEquals(it, mainTabFor(it.destinationId)) }
    }

    @Test
    fun `screens that are not tabs have no tab`() {
        listOf(R.id.bookPageFragment, R.id.bookListFragment, R.id.searchFragment).forEach {
            assertNull(mainTabFor(it))
        }
    }

    @Test
    fun `no page at all has no tab`() {
        assertNull(mainTabFor(null))
    }

    @Test
    fun `the filter is dropped when the reader leaves Explore for another tab`() {
        assertTrue(shouldResetFilter(MainTab.EXPLORE, MainTab.HOME))
        assertTrue(shouldResetFilter(MainTab.EXPLORE, MainTab.PROFILE))
    }

    @Test
    fun `the filter stays when the reader stays in Explore or comes to it`() {
        assertFalse(shouldResetFilter(MainTab.EXPLORE, MainTab.EXPLORE))
        assertFalse(shouldResetFilter(MainTab.HOME, MainTab.EXPLORE))
        assertFalse(shouldResetFilter(MainTab.PROFILE, MainTab.HOME))
        assertFalse(shouldResetFilter(null, MainTab.HOME))
        assertFalse(shouldResetFilter(MainTab.EXPLORE, null))
    }
}
