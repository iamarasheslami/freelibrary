package com.freelibrary.app.ui.shell

import com.freelibrary.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
