package com.mints.mobilehealthapplication.data

import android.content.Context
import android.content.SharedPreferences
import com.mints.mobilehealthapplication.R
import java.time.LocalDate

object MotivationManager {



    private val quotes = listOf(
        "Small steps lead to big victories. Great job taking care of yourself today!",
        "Self-care isn't selfish, it's necessary. You're doing great!",
        "Consistency builds strength. You're proving that one day at a time!",
        "Every dose is an act of self-care. Be proud of yourself today!",
        "Today's effort is tomorrow's strength. You're building a healthier future!",
        "Health is a journey, not a race. Keep moving forward at your own pace!",
        "Each small habit today makes a healthier you tomorrow. Keep stacking those wins!",
        "You showed up for yourself today, and that matters more than you know.",
        "Your well-being is worth the effort. Keep investing in yourself!",
        "You’re not alone in this journey. Every small victory counts—this is one of them!",
        "Healthy habits create a healthy life. You're on the right path!",
        "Each dose is a step toward better health. You took all your steps today!"
    )

    private val images = listOf(R.drawable.mountain1,
        R.drawable.mountain2,
        R.drawable.mountain3,
        R.drawable.cat1,
        R.drawable.cat2,
        R.drawable.cat3,
        R.drawable.beach1,
        R.drawable.beach2,
        R.drawable.beach3,
        R.drawable.dog1,
        R.drawable.dog2,
        R.drawable.dog3,
        R.drawable.bunny1,
        R.drawable.puppy1,
        R.drawable.sunset1,
        R.drawable.sunset2,
        R.drawable.sunset3

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
                    val quoteText = sharedPref.getString("content_text", "")
                    ContentOption.Quote(quoteText ?: "")
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