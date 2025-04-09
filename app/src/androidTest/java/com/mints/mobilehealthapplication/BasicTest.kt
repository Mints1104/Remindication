package com.mints.mobilehealthapplication

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasicTest {
    @Test
    fun testIsRunning() {
        println("This test is running!")
        assert(true)
    }
}