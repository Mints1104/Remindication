package com.mints.mobilehealthapplication.data

import android.content.Context
import android.content.SharedPreferences
import com.mints.mobilehealthapplication.R
import java.time.LocalDate

object MotivationManager {



    private val quotes = listOf(
        "Believe in yourself!",
        "Seize the day!",
        "Stay positive!",
    )

    private val images = listOf(R.drawable.mountain1,
        R.drawable.mountain2,
        R.drawable.mountain3,
        R.drawable.cat1,
        R.drawable.cat2,
        R.drawable.cat3,
        R.drawable.beach1,
        R.drawable.beach2,
        R.drawable.beach3

    )

    fun pickOrRetrieveContentForToday(context: Context): ContentOption {
        val sharedPref = context.getSharedPreferences("motivation_prefs", Context.MODE_PRIVATE)
        val todayDate = LocalDate.now().toString()
        val storedDate = sharedPref.getString("content_date", null)

        if (todayDate == storedDate) {
            val contentType = sharedPref.getString("content_type", null)
            return when (contentType) {
                "image" -> {
                    val imageId = sharedPref.getInt("content_id", 0)
                    ContentOption.Image(imageId)
                }
                "quote" -> {
                    val quoteText = sharedPref.getString("content_text", "Have a great day!")
                    ContentOption.Quote(quoteText ?: "Have a great day!")
                }
                else -> pickAndStoreNewContent(sharedPref, todayDate)
            }
        } else {
            return pickAndStoreNewContent(sharedPref, todayDate)
        }
    }

    private fun pickAndStoreNewContent(sharedPref: SharedPreferences, todayDate: String): ContentOption {
        val editor = sharedPref.edit()
        val content = pickContentForToday()

        when (content) {
            is ContentOption.Image -> {
                editor.putString("content_type", "image")
                editor.putInt("content_id", content.drawableId)
            }
            is ContentOption.Quote -> {
                editor.putString("content_type", "quote")
                editor.putString("content_text", content.text)
            }
        }

        editor.putString("content_date", todayDate)
        editor.apply()
        return content
    }

    private fun pickContentForToday(): ContentOption {
        val isImageDay = (0..1).random() == 0
        return if (isImageDay) {
            ContentOption.Image(images.random())
        } else {
            ContentOption.Quote(quotes.random())
        }
    }

    sealed class ContentOption {
        data class Image(val drawableId: Int) : ContentOption()
        data class Quote(val text: String) : ContentOption()
    }
}