package com.mints.mobilehealthapplication.data

object MotivationManager {

    private val quotes = listOf(
        "Believe in yourself!",
        "Great job keeping your streak!",
        "Every dose counts!",
        "Stay healthy, stay strong!",
        "Your dedication is inspiring!"
    )

    fun getRandomQuote(): String {
        return quotes.random()
    }


}