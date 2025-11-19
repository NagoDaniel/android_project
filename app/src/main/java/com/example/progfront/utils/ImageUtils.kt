package com.example.progfront.utils

import android.graphics.BitmapFactory
import android.util.Base64
import com.example.progfront.data.remote.RetrofitClient
import java.lang.reflect.Field

object ImageUtils {
    fun prepareImageUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw.startsWith("data:image") || raw.contains(";base64,")) {
            return raw // Return as is for base64 handling in UI
        }
        val url = normalizeImageUrl(raw)
        return url
    }

    private fun normalizeImageUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val base = getBaseUrl().trimEnd('/')
        val path = trimmed.trimStart('/')
        return "$base/$path"
    }

    private fun getBaseUrl(): String = try {
        val field: Field = RetrofitClient::class.java.getDeclaredField("BASE_URL")
        field.isAccessible = true
        (field.get(null) as? String) ?: "http://10.0.2.2:8080/"
    } catch (_: Exception) { "http://10.0.2.2:8080/" }

    fun decodeBase64ToBitmap(base64String: String): android.graphics.Bitmap? {
        return try {
            val base64Part = base64String.substringAfter(",", "")
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
