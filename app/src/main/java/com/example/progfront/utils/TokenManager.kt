package com.example.progfront.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {

    // could use encrypted one, bit it will do
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(ACCESS_TOKEN_KEY, accessToken)
            .putString(REFRESH_TOKEN_KEY, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString(ACCESS_TOKEN_KEY, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(REFRESH_TOKEN_KEY, null)
    }

    fun clearTokens() {
        prefs.edit()
            .remove(ACCESS_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .apply()
    }

    fun hasValidToken(): Boolean {
        return !getAccessToken().isNullOrEmpty()
    }

    fun getBearerToken(): String? {
        val token = getAccessToken()
        return if (token != null) "Bearer $token" else null
    }
}
