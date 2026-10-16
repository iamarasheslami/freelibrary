package com.freelibrary.app.ui.shell

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BottomBarTest {
    private lateinit var bar: LinearLayout
    private lateinit var tabs: Map<MainTab, FrameLayout>

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        bar = LinearLayout(context)
        tabs = MainTab.entries.associateWith { FrameLayout(context).also { tab -> tab.addView(View(context)) } }
    }

    private fun bottomBar(onTabClick: (MainTab) -> Unit = {}) = BottomBar(bar, tabs, onTabClick)

    @Test
    fun `showing a tab selects only that tab`() {
        bottomBar().show(MainTab.EXPLORE)

        assertTrue(tabs.getValue(MainTab.EXPLORE).isSelected)
        assertFalse(tabs.getValue(MainTab.HOME).isSelected)
        assertFalse(tabs.getValue(MainTab.PROFILE).isSelected)
    }

    @Test
    fun `the selection reaches what is inside the tab`() {
        bottomBar().show(MainTab.HOME)

        assertTrue(tabs.getValue(MainTab.HOME).getChildAt(0).isSelected)
        assertFalse(tabs.getValue(MainTab.PROFILE).getChildAt(0).isSelected)
    }

    @Test
    fun `the bar is visible while a tab is showing`() {
        bar.isVisible = false

        bottomBar().show(MainTab.HOME)

        assertTrue(bar.isVisible)
    }

    @Test
    fun `the bar is hidden and nothing is selected on any other screen`() {
        val bottomBar = bottomBar()
        bottomBar.show(MainTab.HOME)

        bottomBar.show(null)

        assertFalse(bar.isVisible)
        assertTrue(tabs.values.none { it.isSelected })
    }

    @Test
    fun `a tap on a tab passes that tab to the callback`() {
        var tapped: MainTab? = null
        bottomBar { tapped = it }

        tabs.getValue(MainTab.PROFILE).performClick()

        assertEquals(MainTab.PROFILE, tapped)
    }
}
