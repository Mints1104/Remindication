package com.mints.mobilehealthapplication

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

class CustomTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        println("Runner: Starting!")
        super.onCreate(arguments)
    }

    override fun onException(obj: Any?, e: Throwable?): Boolean {
        println("Runner: Crash detected!")
        e?.printStackTrace()
        return false // Let the parent handle it too
    }
}