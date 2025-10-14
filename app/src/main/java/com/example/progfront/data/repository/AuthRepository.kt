package com.example.progfront.data.repository

import com.example.progfront.data.Result
import com.example.progfront.data.model.LoginRequest
import com.example.progfront.data.model.RegisterRequest
import com.example.progfront.data.model.RegisterResponse
import com.example.progfront.data.model.Tokens
import com.example.progfront.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {

    suspend fun register(username: String, email: String, password: String): Result<RegisterResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterRequest(username, email, password)
                val response = RetrofitClient.instance.registerUser(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Registration failed: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun login(email: String, password: String): Result<RegisterResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(email, password)
                val response = RetrofitClient.instance.loginUser(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Login failed: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun refreshTokens(refreshToken: String): Result<Tokens> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.refreshTokens(refreshToken)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Token refresh failed: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.logout()
                if (response.isSuccessful) {
                    Result.Success(Unit)
                } else {
                    Result.Error("Logout failed: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
}

