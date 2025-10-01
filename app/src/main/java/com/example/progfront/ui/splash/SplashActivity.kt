package com.example.progfront.ui.splash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.progfront.databinding.ActivitySplashBinding
import com.example.progfront.ui.auth.login.LoginActivity
import com.example.progfront.ui.main.MainActivity
import com.example.progfront.utils.TokenManager
import com.example.progfront.data.model.Tokens
import com.example.progfront.data.remote.RetrofitClient
import com.example.progfront.data.model.HabitResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var tokenManager: TokenManager
    private var navigated = false

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
                Log.d(TAG, "No refresh token; validating existing access token via lightweight API call")
                validateAccessToken(accessToken)
            } else {
                Log.d(TAG, "No tokens at all – navigating to Login")
                goToLogin()
            }
            return
        }
        Log.d(TAG, "Attempting token refresh")
        RetrofitClient.instance.refreshTokens(refreshToken)
            .enqueue(object : Callback<Tokens> {
                override fun onResponse(call: Call<Tokens>, response: Response<Tokens>) {
                    if (response.isSuccessful) {
                        val tokens = response.body()
                        if (tokens != null) {
                            Log.d(TAG, "Refresh success: new tokens received")
                            tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                            goToMain()
                        } else {
                            Log.e(TAG, "Refresh success but body null – clearing & login")
                            tokenManager.clearTokens()
                            goToLogin()
                        }
                    } else {
                        Log.e(TAG, "Refresh failed code=${response.code()} err=${response.errorBody()?.string()} – trying to validate existing access token if present")
                        if (!accessToken.isNullOrBlank()) {
                            validateAccessToken(accessToken)
                        } else {
                            tokenManager.clearTokens()
                            goToLogin()
                        }
                    }
                }

                override fun onFailure(call: Call<Tokens>, t: Throwable) {
                    Log.e(TAG, "Refresh network error: ${t.message} – fallback to access token validation if possible", t)
                    if (!accessToken.isNullOrBlank()) {
                        validateAccessToken(accessToken)
                    } else {
                        goToLogin()
                    }
                }
            })
    }

    private fun validateAccessToken(accessToken: String) {
        val bearer = "Bearer $accessToken"
        RetrofitClient.instance.getHabits(bearer)
            .enqueue(object : Callback<List<HabitResponse>> {
                override fun onResponse(
                    call: Call<List<HabitResponse>>, response: Response<List<HabitResponse>>
                ) {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Access token validated via habits endpoint; proceeding to main")
                        goToMain()
                    } else {
                        Log.e(TAG, "Access token invalid (code=${response.code()}); clearing & login")
                        tokenManager.clearTokens()
                        goToLogin()
                    }
                }

                override fun onFailure(call: Call<List<HabitResponse>>, t: Throwable) {
                    Log.e(TAG, "Validation network error: ${t.message}; navigating to login to be safe", t)
                    goToLogin()
                }
            })
    }

    private fun goToMain() { if (navigated) return; navigated = true; startActivity(Intent(this, MainActivity::class.java)); finish() }
    private fun goToLogin() { if (navigated) return; navigated = true; startActivity(Intent(this, LoginActivity::class.java)); finish() }
}
