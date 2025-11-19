package com.example.progfront.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {
    private val parsePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    private val utcZone = TimeZone.getTimeZone("UTC")
    private val outputTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = utcZone }

    fun formatTime(raw: String): String {
        // Fast path: extract HH:mm after 'T'
        val tIndex = raw.indexOf('T')
        if (tIndex >= 0 && raw.length >= tIndex + 6) {
            val candidate = raw.substring(tIndex + 1, tIndex + 6)
            if (candidate.matches(Regex("\\d{2}:\\d{2}"))) return candidate
        }
        // Fallback parse
        parsePatterns.forEach { p ->
            try {
                val sdf = SimpleDateFormat(p, Locale.getDefault())
                if (p.contains("'Z'")) sdf.timeZone = utcZone
                val date = sdf.parse(raw)
                if (date != null) return outputTime.format(date)
            } catch (_: ParseException) {}
        }
        return raw.substringAfter('T').take(5)
    }
}
