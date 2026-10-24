package com.freelibrary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSizeTest {
    @Test
    fun `the sizes grow from small to large and large is the original size`() {
        assertTrue(AppSize.SMALL.scale < AppSize.MEDIUM.scale)
        assertTrue(AppSize.MEDIUM.scale < AppSize.LARGE.scale)
        assertEquals(1.0f, AppSize.LARGE.scale, 0f)
    }

    @Test
    fun `a narrow screen starts small and a roomy one starts medium`() {
        assertEquals(AppSize.SMALL, AppSize.forScreen(320))
        assertEquals(AppSize.SMALL, AppSize.forScreen(360))
        assertEquals(AppSize.SMALL, AppSize.forScreen(AppSize.ROOMY_SCREEN_DP - 1))
        assertEquals(AppSize.MEDIUM, AppSize.forScreen(AppSize.ROOMY_SCREEN_DP))
        assertEquals(AppSize.MEDIUM, AppSize.forScreen(600))
    }

    @Test
    fun `no screen starts large and an unknown width starts medium`() {
        assertEquals(AppSize.MEDIUM, AppSize.forScreen(0))
        assertTrue(listOf(0, 240, 360, 400, 600, 900).none { AppSize.forScreen(it) == AppSize.LARGE })
    }
}
