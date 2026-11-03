package com.freelibrary.app.data.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.domain.model.AvatarDesign
import com.freelibrary.app.domain.model.AvatarSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AvatarDesignStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clear() {
        context.getSharedPreferences("avatar_design", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `nothing is remembered until an avatar is saved`() {
        assertNull(AvatarDesignStore(context).load())
    }

    @Test
    fun `the last saved avatar comes back, even in a new store`() {
        val design = AvatarDesign.DEFAULT.with(AvatarSlot.OUTFIT, 4)

        AvatarDesignStore(context).save(design)

        assertEquals(design, AvatarDesignStore(context).load())
    }

    @Test
    fun `stored text that is not a design is ignored`() {
        context.getSharedPreferences("avatar_design", Context.MODE_PRIVATE).edit().putString("design", "garbage").commit()

        assertNull(AvatarDesignStore(context).load())
    }
}
