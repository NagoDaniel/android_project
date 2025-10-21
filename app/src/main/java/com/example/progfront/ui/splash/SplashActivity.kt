package com.example.progfront.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.progfront.data.Result
import com.example.progfront.data.repository.AuthRepository
import com.example.progfront.data.repository.HabitRepository
import com.example.progfront.databinding.ActivitySplashBinding
import com.example.progfront.ui.main.MainActivity
import com.example.progfront.utils.TokenManager
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var tokenManager: TokenManager
    private var navigated = false

    private val authRepository = AuthRepository()
    private val habitRepository = HabitRepository()

    companion object { private const val TAG = "SplashActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)
        attemptAutoLogin()
    }

    private fun attemptAutoLogin() {
        val refreshToken = tokenManager.getRefreshToken()
        val accessToken = tokenManager.getAccessToken()
        Log.d(TAG, "Stored tokens => access=${accessToken?.take(10)}..., refresh=${refreshToken?.take(10)}...")

        if (refreshToken.isNullOrBlank()) {
            if (!accessToken.isNullOrBlank()) {
                Log.d(TAG, "No refresh token; validating existing access token")
                validateAccessToken(accessToken)
            } else {
                Log.d(TAG, "No tokens at all – navigating to MainActivity (will show Login)")
                goToMain()
            }
            return
        }

        Log.d(TAG, "Attempting token refresh")
        lifecycleScope.launch {
            val result = authRepository.refreshTokens(refreshToken)
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Refresh success: new tokens received")
                    tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                    goToMain()
                }
                is Result.Error -> {
                    Log.e(TAG, "Refresh failed: ${result.message} – trying to validate existing access token if present")
                    if (!accessToken.isNullOrBlank()) {
                        validateAccessToken(accessToken)
                    } else {
                        tokenManager.clearTokens()
                        goToMain()
                    }
                }
                is Result.Loading -> {
                    // Should not happen in this flow
                }
            }
        }
    }

    private fun validateAccessToken(accessToken: String) {
        lifecycleScope.launch {
            val result = habitRepository.getHabits()
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Access token validated via habits endpoint; proceeding to main")
                    goToMain()
                }
                is Result.Error -> {
                    Log.e(TAG, "Access token invalid: ${result.message}; clearing & showing login")
                    tokenManager.clearTokens()
                    goToMain()
                }
                is Result.Loading -> {
                    // Should not happen in this flow
                }
            }
        }
    }

    private fun goToMain() {
        if (navigated) return
        navigated = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
