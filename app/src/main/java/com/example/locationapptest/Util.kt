package com.example.locationapptest

import android.os.Build
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

fun getTimeStringWithStamp(timestamp: Long): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.format(timestamp)
    } catch (e: Exception) {
        "null"
    }
}

fun getCurrentTm(): Long {
    val currentTm = Calendar.getInstance(
        Locale(
            Locale.SIMPLIFIED_CHINESE.language,
            Locale.CHINA.country
        )
    ).time.time
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val detonatedTmStr = format.format(currentTm)
    return format.parse(detonatedTmStr)!!.time
}