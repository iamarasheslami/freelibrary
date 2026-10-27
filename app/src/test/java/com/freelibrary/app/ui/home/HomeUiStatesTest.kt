package com.freelibrary.app.ui.home

import com.freelibrary.app.domain.model.HomeSection
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class HomeUiStatesTest {
    @Test
    fun `the lists arrive after a loading state`() =
        runTest {
            val states = flowOf(emptyList<HomeSection>()).asHomeUiStates().toList()

            assertEquals(listOf(HomeUiState.Loading, HomeUiState.Content(emptyList())), states)
        }

    @Test
    fun `a failure to read the lists ends in the failed state instead of crashing`() =
        runTest {
            val states =
                flow<List<HomeSection>> {
                    throw IOException("Unable to copy database file")
                }.asHomeUiStates().toList()

            assertEquals(listOf(HomeUiState.Loading, HomeUiState.Failed), states)
        }
}
