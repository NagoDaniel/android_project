package com.example.progfront.ui.auth.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.RegisterResponse
import com.example.progfront.data.repository.AuthRepository
import kotlinx.coroutines.launch
import com.example.progfront.utils.TokenManager

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()

    private val _registerResult = MutableLiveData<Result<RegisterResponse>>()
    private val tokenManager = TokenManager(application.applicationContext)
    val registerResult: LiveData<Result<RegisterResponse>> = _registerResult

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _registerResult.value = Result.Loading
            val result = authRepository.register(username, email, password)

            // Save tokens in ViewModel so Fragment doesn't need to handle context-related token storage
            if (result is Result.Success) {
                try {
                    tokenManager.saveTokens(
                        result.data.tokens.accessToken,
                        result.data.tokens.refreshToken
                    )
                } catch (e: Exception) {
                    // Log or handle if token saving fails, but still propagate the result
                }
            }

            _registerResult.value = result
        }
    }
}
