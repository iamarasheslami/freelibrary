package com.freelibrary.app.ui.explore

import androidx.paging.LoadState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreLoadStateAdapterTest {
    private val adapter = ExploreLoadStateAdapter()

    @Test
    fun `the footer is shown while more categories are loading`() {
        assertTrue(adapter.displayLoadStateAsItem(LoadState.Loading))
    }

    @Test
    fun `the footer is not shown when loading is done or has failed`() {
        assertFalse(adapter.displayLoadStateAsItem(LoadState.NotLoading(endOfPaginationReached = false)))
        assertFalse(adapter.displayLoadStateAsItem(LoadState.NotLoading(endOfPaginationReached = true)))
        assertFalse(adapter.displayLoadStateAsItem(LoadState.Error(RuntimeException("offline"))))
    }
}
