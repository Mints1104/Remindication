package com.mints.mobilehealthapplication

import com.mints.mobilehealthapplication.ui.HomeFragment
import org.junit.Assert.assertNotNull
import org.junit.Test

class SimpleHomeFragmentTest {

    @Test
    fun testHomeFragmentCanBeCreated() {
        val fragment = HomeFragment()
        assertNotNull(fragment)
    }
}